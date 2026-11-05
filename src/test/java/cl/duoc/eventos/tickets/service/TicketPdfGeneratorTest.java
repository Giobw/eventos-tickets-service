package cl.duoc.eventos.tickets.service;

import cl.duoc.eventos.tickets.model.Ticket;
import cl.duoc.eventos.tickets.model.TicketStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketPdfGeneratorTest {

    @Test
    void generaUnPdfValido() {
        Ticket t = new Ticket();
        t.setCode("abc-123");
        t.setEventId("evento123");
        t.setEventName("Concierto Andino");
        t.setUserId("usuarioX");
        t.setUserName("Ana Pérez");
        t.setSeat("A-12");
        t.setPrice(new BigDecimal("25000"));
        t.setStatus(TicketStatus.VENDIDO);

        byte[] pdf = new TicketPdfGenerator().generate(t);

        assertTrue(pdf.length > 500, "El PDF no debería estar vacío");
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }
}
