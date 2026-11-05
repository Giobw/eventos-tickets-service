package cl.duoc.eventos.tickets.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/** Estadísticas de ventas y reservas de un evento. */
public record EventStatsResponse(
        String eventId,
        String eventName,
        long totalTickets,
        long reservedTickets,
        long soldTickets,
        BigDecimal salesRevenue,
        BigDecimal reservedAmount,
        long uniqueUsers,
        long ticketsInS3,
        Map<String, Long> ticketsByUser,
        LocalDateTime generatedAt) {
}
