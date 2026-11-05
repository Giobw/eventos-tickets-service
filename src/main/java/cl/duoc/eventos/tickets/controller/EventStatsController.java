package cl.duoc.eventos.tickets.controller;

import cl.duoc.eventos.tickets.dto.EventStatsResponse;
import cl.duoc.eventos.tickets.service.TicketService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class EventStatsController {

    private final TicketService service;

    public EventStatsController(TicketService service) {
        this.service = service;
    }

    /** 6) Estadísticas de ventas y reservas por evento. */
    @GetMapping("/{eventId}/stats")
    public EventStatsResponse stats(@PathVariable String eventId) {
        return service.stats(eventId);
    }
}
