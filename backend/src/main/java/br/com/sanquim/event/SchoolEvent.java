package br.com.sanquim.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "school_events")
/** Evento escolar visível aos usuários autenticados. */
public class SchoolEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 4000)
    private String description;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    protected SchoolEvent() {
    }

    public SchoolEvent(String title, String description, LocalDateTime startsAt, UUID createdBy) {
        this.title = title;
        this.description = description;
        this.startsAt = startsAt;
        this.createdBy = createdBy;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getStartsAt() {
        return startsAt;
    }
}
