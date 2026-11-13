package br.com.sanquim.attendance;

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

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "attendance")
/** Registro único de presença de um aluno em uma matéria e data. */
public class Attendance {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private AppUser student;

    @Column(name = "lesson_date", nullable = false)
    private LocalDate lessonDate;

    @Column(nullable = false)
    private boolean present;

    @Column(name = "recorded_by", nullable = false)
    private UUID recordedBy;

    protected Attendance() {
    }

    public Attendance(Subject subject, AppUser student, LocalDate lessonDate,
                      boolean present, UUID recordedBy) {
        this.subject = subject;
        this.student = student;
        this.lessonDate = lessonDate;
        this.present = present;
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

    public LocalDate getLessonDate() {
        return lessonDate;
    }

    public boolean isPresent() {
        return present;
    }

    public void update(AppUser student, LocalDate lessonDate, boolean present, UUID recordedBy) {
        this.student = student;
        this.lessonDate = lessonDate;
        this.present = present;
        this.recordedBy = recordedBy;
    }
}
