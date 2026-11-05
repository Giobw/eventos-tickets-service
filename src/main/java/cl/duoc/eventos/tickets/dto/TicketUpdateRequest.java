package cl.duoc.eventos.tickets.dto;

import cl.duoc.eventos.tickets.model.TicketStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Campos modificables de un ticket; los que vengan nulos no se alteran. */
public record TicketUpdateRequest(
        @Size(max = 120, message = "eventName admite máximo 120 caracteres")
        String eventName,

        @Size(max = 120, message = "userName admite máximo 120 caracteres")
        String userName,

        @Size(max = 20, message = "seat admite máximo 20 caracteres")
        String seat,

        @DecimalMin(value = "0.0", message = "price no puede ser negativo")
        BigDecimal price,

        TicketStatus status) {
}
