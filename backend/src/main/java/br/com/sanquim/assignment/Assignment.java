package br.com.sanquim.assignment;

import br.com.sanquim.subject.Subject;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "assignments")
/** Tarefa escolar vinculada a uma única matéria. */
public class Assignment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    protected Assignment() {
    }

    public Assignment(Subject subject, String title, String description,
                      LocalDateTime dueAt, UUID createdBy) {
        this.subject = subject;
        this.title = title;
        this.description = description;
        this.dueAt = dueAt;
        this.createdBy = createdBy;
    }

    public UUID getId() {
        return id;
    }

    public Subject getSubject() {
        return subject;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public void update(String title, String description, LocalDateTime dueAt) {
        this.title = title;
        this.description = description;
        this.dueAt = dueAt;
    }
}
