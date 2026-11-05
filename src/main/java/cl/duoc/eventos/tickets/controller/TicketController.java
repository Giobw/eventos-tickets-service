package cl.duoc.eventos.tickets.controller;

import cl.duoc.eventos.tickets.dto.TicketRequest;
import cl.duoc.eventos.tickets.dto.TicketResponse;
import cl.duoc.eventos.tickets.dto.TicketUpdateRequest;
import cl.duoc.eventos.tickets.model.TicketStatus;
import cl.duoc.eventos.tickets.service.TicketService;
import cl.duoc.eventos.tickets.service.TicketService.FilePayload;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService service;

    public TicketController(TicketService service) {
        this.service = service;
    }

    /** 1) Generar ticket (PDF en EFS + subida automática a S3). */
    @PostMapping
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody TicketRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TicketResponse.from(service.create(request)));
    }

    /** 2) Subir (o resubir) el ticket a S3. */
    @PostMapping("/{id}/upload")
    public TicketResponse upload(@PathVariable Long id) {
        return TicketResponse.from(service.uploadToS3(id));
    }

    /** Historial de tickets generados con filtros opcionales. */
    @GetMapping
    public List<TicketResponse> history(
            @RequestParam(required = false) String eventId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.search(eventId, userId, status, from, to).stream()
                .map(TicketResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public TicketResponse get(@PathVariable Long id) {
        return TicketResponse.from(service.get(id));
    }

    /** 3) Descargar un ticket (PDF). */
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        return file(service.download(id), MediaType.APPLICATION_PDF);
    }

    /** 3) Descargar todos los tickets de un usuario (ZIP). */
    @GetMapping("/user/{userId}/download")
    public ResponseEntity<byte[]> downloadByUser(@PathVariable String userId,
                                                 @RequestParam(required = false) String eventId) {
        return file(service.downloadByUser(userId, eventId), MediaType.parseMediaType("application/zip"));
    }

    /** 3) Descargar todos los tickets de un evento (ZIP). */
    @GetMapping("/event/{eventId}/download")
    public ResponseEntity<byte[]> downloadByEvent(@PathVariable String eventId) {
        return file(service.downloadByEvent(eventId), MediaType.parseMediaType("application/zip"));
    }

    /** 4) Modificar detalles del ticket (regenera PDF y actualiza S3). */
    @PutMapping("/{id}")
    public TicketResponse update(@PathVariable Long id, @Valid @RequestBody TicketUpdateRequest request) {
        return TicketResponse.from(service.update(id, request));
    }

    /** 5) Eliminar un ticket específico (S3 + EFS + metadatos). */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<byte[]> file(FilePayload payload, MediaType type) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(payload.fileName()).build().toString())
                .contentType(type)
                .body(payload.content());
    }
}
