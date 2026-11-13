package br.com.sanquim.event;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
/** Lista eventos e restringe publicação e remoção à secretaria. */
public class SchoolEventController {
    private final SchoolEventRepository events;

    public SchoolEventController(SchoolEventRepository events) {
        this.events = events;
    }

    @GetMapping
    public List<EventResponse> list() {
        return events.findAllByOrderByStartsAtAsc().stream().map(EventResponse::from).toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('SECRETARIA')")
    public EventResponse create(@Valid @RequestBody CreateEventRequest request,
                                @AuthenticationPrincipal Jwt jwt) {
        SchoolEvent event = events.save(new SchoolEvent(
            request.title(), request.description(), request.startsAt(),
            UUID.fromString(jwt.getClaimAsString("userId"))));
        return EventResponse.from(event);
    }

    @DeleteMapping("/{eventId}")
    @PreAuthorize("hasRole('SECRETARIA')")
    public void delete(@PathVariable UUID eventId) {
        SchoolEvent event = events.findById(eventId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento não encontrado."));
        events.delete(event);
    }

    /** Campos validados para publicar um evento escolar. */
    public record CreateEventRequest(@NotBlank @Size(max = 160) String title,
                                     @NotBlank @Size(max = 4000) String description,
                                     @NotNull LocalDateTime startsAt) {
    }

    /** Representação de evento visível aos usuários autenticados. */
    public record EventResponse(UUID id, String title, String description, LocalDateTime startsAt) {
        static EventResponse from(SchoolEvent event) {
            return new EventResponse(event.getId(), event.getTitle(), event.getDescription(), event.getStartsAt());
        }
    }
}
