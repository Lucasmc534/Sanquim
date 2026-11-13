package br.com.sanquim.grade;

import br.com.sanquim.subject.Subject;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "grades")
/** Nota de um aluno em uma matéria, com autoria do registro preservada. */
public class Grade {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private AppUser student;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "graded_at", nullable = false)
    private LocalDateTime gradedAt;

    @Column(name = "recorded_by", nullable = false)
    private UUID recordedBy;

    protected Grade() {
    }

    public Grade(Subject subject, AppUser student, String title, BigDecimal score,
                 LocalDateTime gradedAt, UUID recordedBy) {
        this.subject = subject;
        this.student = student;
        this.title = title;
        this.score = score;
        this.gradedAt = gradedAt;
        this.recordedBy = recordedBy;
    }

    public UUID getId() {
        return id;
    }

    public Subject getSubject() {
        return subject;
    }

    public AppUser getStudent() {
        return student;
    }

    public String getTitle() {
        return title;
    }

    public BigDecimal getScore() {
        return score;
    }

    public LocalDateTime getGradedAt() {
        return gradedAt;
    }

    public void update(AppUser student, String title, BigDecimal score,
                       LocalDateTime gradedAt, UUID recordedBy) {
        this.student = student;
        this.title = title;
        this.score = score;
        this.gradedAt = gradedAt;
        this.recordedBy = recordedBy;
    }
}
