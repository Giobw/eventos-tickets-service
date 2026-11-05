package cl.duoc.eventos.tickets.dto;

import cl.duoc.eventos.tickets.model.TicketStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Datos para generar un ticket. eventId y userId forman la carpeta en EFS/S3. */
public record TicketRequest(
        @NotBlank(message = "eventId es obligatorio")
        @Pattern(regexp = "^[A-Za-z0-9_-]{1,50}$",
                message = "eventId solo admite letras, números, guion y guion bajo (máx. 50)")
        String eventId,

        @NotBlank(message = "eventName es obligatorio")
        @Size(max = 120, message = "eventName admite máximo 120 caracteres")
        String eventName,

        @NotBlank(message = "userId es obligatorio")
        @Pattern(regexp = "^[A-Za-z0-9_-]{1,50}$",
                message = "userId solo admite letras, números, guion y guion bajo (máx. 50)")
        String userId,

        @NotBlank(message = "userName es obligatorio")
        @Size(max = 120, message = "userName admite máximo 120 caracteres")
        String userName,

        @Size(max = 20, message = "seat admite máximo 20 caracteres")
        String seat,

        @NotNull(message = "price es obligatorio")
        @DecimalMin(value = "0.0", message = "price no puede ser negativo")
        BigDecimal price,

        /** Opcional: por defecto RESERVADO. */
        TicketStatus status) {
}
