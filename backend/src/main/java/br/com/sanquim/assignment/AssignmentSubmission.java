package br.com.sanquim.assignment;

import br.com.sanquim.user.AppUser;
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
@Table(name = "assignment_submissions")
/** Entrega de texto de um aluno para uma tarefa; uploads não são processados nesta versão. */
public class AssignmentSubmission {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private AppUser student;

    @Column(columnDefinition = "TEXT")
    private String submissionText;

    @Column(name = "storage_key", length = 500)
    private String storageKey;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    protected AssignmentSubmission() {
    }

    public AssignmentSubmission(Assignment assignment, AppUser student,
                                String submissionText, LocalDateTime submittedAt) {
        this.assignment = assignment;
        this.student = student;
        this.submissionText = submissionText;
        this.submittedAt = submittedAt;
    }

    public UUID getId() {
        return id;
    }

    public Assignment getAssignment() {
        return assignment;
    }

    public AppUser getStudent() {
        return student;
    }

    public String getSubmissionText() {
        return submissionText;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void replaceText(String submissionText, LocalDateTime submittedAt) {
        this.submissionText = submissionText;
        this.submittedAt = submittedAt;
    }
}
