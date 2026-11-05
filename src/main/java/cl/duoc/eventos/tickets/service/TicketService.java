package cl.duoc.eventos.tickets.service;

import cl.duoc.eventos.tickets.dto.EventStatsResponse;
import cl.duoc.eventos.tickets.dto.TicketRequest;
import cl.duoc.eventos.tickets.dto.TicketUpdateRequest;
import cl.duoc.eventos.tickets.exception.NotFoundException;
import cl.duoc.eventos.tickets.exception.StorageException;
import cl.duoc.eventos.tickets.model.Ticket;
import cl.duoc.eventos.tickets.model.TicketStatus;
import cl.duoc.eventos.tickets.repository.TicketRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    /** Archivo listo para descargar. */
    public record FilePayload(String fileName, byte[] content) {
    }

    private final TicketRepository repository;
    private final EfsStorageService efs;
    private final S3StorageService s3;
    private final TicketPdfGenerator pdfGenerator;
    private final boolean autoUpload;

    public TicketService(TicketRepository repository,
                         EfsStorageService efs,
                         S3StorageService s3,
                         TicketPdfGenerator pdfGenerator,
                         @Value("${app.s3.auto-upload}") boolean autoUpload) {
        this.repository = repository;
        this.efs = efs;
        this.s3 = s3;
        this.pdfGenerator = pdfGenerator;
        this.autoUpload = autoUpload;
    }

    // ------------------------------------------------------------------ Generar

    /** Genera el PDF en EFS y, si está habilitado, lo sube automáticamente a S3. */
    @Transactional
    public Ticket create(TicketRequest req) {
        Ticket t = new Ticket();
        t.setCode(UUID.randomUUID().toString());
        t.setEventId(req.eventId());
        t.setEventName(req.eventName());
        t.setUserId(req.userId());
        t.setUserName(req.userName());
        t.setSeat(req.seat());
        t.setPrice(req.price());
        t.setStatus(req.status() == null ? TicketStatus.RESERVADO : req.status());
        t = repository.save(t); // obtiene id y createdAt

        t.setFileName(String.format("ticket%05d.pdf", t.getId()));
        t.setEfsPath(t.getEventId() + "/" + t.getUserId() + "/" + t.getFileName());
        t.setS3Key(t.getEfsPath());

        efs.write(t.getEfsPath(), pdfGenerator.generate(t));

        if (autoUpload) {
            try {
                s3.upload(t.getS3Key(), efs.read(t.getEfsPath()));
                t.setUploadedToS3(true);
            } catch (StorageException e) {
                // El ticket queda en EFS y puede subirse luego con POST /api/tickets/{id}/upload
                log.warn("Subida automática a S3 falló para el ticket {}: {}", t.getId(), e.getMessage());
            }
        }
        return repository.save(t);
    }

    // ------------------------------------------------------------------ Subir

    /** Sube (o vuelve a subir) a S3 el archivo que está en EFS. */
    @Transactional
    public Ticket uploadToS3(Long id) {
        Ticket t = get(id);
        s3.upload(t.getS3Key(), efs.read(t.getEfsPath()));
        t.setUploadedToS3(true);
        return repository.save(t);
    }

    // ------------------------------------------------------------------ Consultar

    @Transactional(readOnly = true)
    public Ticket get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("No existe el ticket con id " + id));
    }

    /** Historial de tickets generados, filtrable por evento, usuario, estado y rango de fechas. */
    @Transactional(readOnly = true)
    public List<Ticket> search(String eventId, String userId, TicketStatus status,
                               LocalDate from, LocalDate to) {
        Specification<Ticket> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (eventId != null && !eventId.isBlank()) {
                predicates.add(cb.equal(root.get("eventId"), eventId));
            }
            if (userId != null && !userId.isBlank()) {
                predicates.add(cb.equal(root.get("userId"), userId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("createdAt"), from.atStartOfDay()));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.<LocalDateTime>get("createdAt"), to.plusDays(1).atStartOfDay()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return repository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    // ------------------------------------------------------------------ Descargar

    /** Descarga un ticket: desde S3 si ya fue subido; si no, desde EFS. */
    @Transactional(readOnly = true)
    public FilePayload download(Long id) {
        Ticket t = get(id);
        return new FilePayload(t.getFileName(), readContent(t));
    }

    /** Descarga en ZIP todos los tickets de un usuario (opcionalmente de un evento). */
    @Transactional(readOnly = true)
    public FilePayload downloadByUser(String userId, String eventId) {
        List<Ticket> tickets = search(eventId, userId, null, null, null);
        if (tickets.isEmpty()) {
            throw new NotFoundException("El usuario " + userId + " no tiene tickets"
                    + (eventId == null ? "" : " en el evento " + eventId));
        }
        return new FilePayload("tickets-usuario-" + userId + ".zip", zip(tickets));
    }

    /** Descarga en ZIP todos los tickets de un evento. */
    @Transactional(readOnly = true)
    public FilePayload downloadByEvent(String eventId) {
        List<Ticket> tickets = search(eventId, null, null, null, null);
        if (tickets.isEmpty()) {
            throw new NotFoundException("El evento " + eventId + " no tiene tickets");
        }
        return new FilePayload("tickets-evento-" + eventId + ".zip", zip(tickets));
    }

    // ------------------------------------------------------------------ Modificar

    /** Modifica los detalles del ticket, regenera el PDF y actualiza el objeto en S3. */
    @Transactional
    public Ticket update(Long id, TicketUpdateRequest req) {
        Ticket t = get(id);
        if (req.eventName() != null && !req.eventName().isBlank()) {
            t.setEventName(req.eventName());
        }
        if (req.userName() != null && !req.userName().isBlank()) {
            t.setUserName(req.userName());
        }
        if (req.seat() != null) {
            t.setSeat(req.seat());
        }
        if (req.price() != null) {
            t.setPrice(req.price());
        }
        if (req.status() != null) {
            t.setStatus(req.status());
        }

        byte[] pdf = pdfGenerator.generate(t);
        efs.write(t.getEfsPath(), pdf);

        if (t.isUploadedToS3()) {
            s3.upload(t.getS3Key(), pdf); // sobrescribe el objeto existente
        } else if (autoUpload) {
            try {
                s3.upload(t.getS3Key(), pdf);
                t.setUploadedToS3(true);
            } catch (StorageException e) {
                log.warn("No se pudo subir a S3 el ticket {} tras modificarlo: {}", id, e.getMessage());
            }
        }
        return repository.save(t);
    }

    // ------------------------------------------------------------------ Eliminar

    @Transactional
    public void delete(Long id) {
        Ticket t = get(id);
        if (t.isUploadedToS3()) {
            s3.delete(t.getS3Key());
        }
        efs.delete(t.getEfsPath());
        repository.delete(t);
    }

    // ------------------------------------------------------------------ Estadísticas

    @Transactional(readOnly = true)
    public EventStatsResponse stats(String eventId) {
        List<Ticket> tickets = repository.findByEventId(eventId);
        if (tickets.isEmpty()) {
            throw new NotFoundException("El evento " + eventId + " no tiene tickets");
        }
        long reserved = 0;
        long sold = 0;
        long inS3 = 0;
        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal reservedAmount = BigDecimal.ZERO;
        Map<String, Long> byUser = new LinkedHashMap<>();

        for (Ticket t : tickets) {
            if (t.getStatus() == TicketStatus.VENDIDO) {
                sold++;
                revenue = revenue.add(t.getPrice());
            } else {
                reserved++;
                reservedAmount = reservedAmount.add(t.getPrice());
            }
            if (t.isUploadedToS3()) {
                inS3++;
            }
            byUser.merge(t.getUserId(), 1L, Long::sum);
        }
        return new EventStatsResponse(
                eventId, tickets.get(0).getEventName(), tickets.size(), reserved, sold,
                revenue, reservedAmount, byUser.size(), inS3, byUser, LocalDateTime.now());
    }

    // ------------------------------------------------------------------ Internos

    private byte[] readContent(Ticket t) {
        return t.isUploadedToS3() ? s3.download(t.getS3Key()) : efs.read(t.getEfsPath());
    }

    private byte[] zip(List<Ticket> tickets) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(bos)) {
            for (Ticket t : tickets) {
                zos.putNextEntry(new ZipEntry(t.getEfsPath()));
                zos.write(readContent(t));
                zos.closeEntry();
            }
            zos.finish();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new StorageException("No se pudo generar el archivo ZIP", e);
        }
    }
}
