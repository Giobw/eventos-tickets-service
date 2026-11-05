package cl.duoc.eventos.tickets.dto;

import cl.duoc.eventos.tickets.model.Ticket;
import cl.duoc.eventos.tickets.model.TicketStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TicketResponse(
        Long id,
        String code,
        String eventId,
        String eventName,
        String userId,
        String userName,
        String seat,
        BigDecimal price,
        TicketStatus status,
        String fileName,
        String efsPath,
        String s3Key,
        boolean uploadedToS3,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static TicketResponse from(Ticket t) {
        return new TicketResponse(
                t.getId(), t.getCode(), t.getEventId(), t.getEventName(),
                t.getUserId(), t.getUserName(), t.getSeat(), t.getPrice(),
                t.getStatus(), t.getFileName(), t.getEfsPath(), t.getS3Key(),
                t.isUploadedToS3(), t.getCreatedAt(), t.getUpdatedAt());
    }
}
