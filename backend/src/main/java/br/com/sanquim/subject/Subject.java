package br.com.sanquim.subject;

import br.com.sanquim.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "subjects")
/** Matéria, professor responsável e alunos matriculados. */
public class Subject {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 40)
    private String className;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private AppUser teacher;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "subject_students",
        joinColumns = @JoinColumn(name = "subject_id"),
        inverseJoinColumns = @JoinColumn(name = "student_id"))
    private Set<AppUser> students = new HashSet<>();

    protected Subject() {
    }

    public Subject(String name, String className, AppUser teacher) {
        this.name = name;
        this.className = className;
        this.teacher = teacher;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getClassName() {
        return className;
    }

    public AppUser getTeacher() {
        return teacher;
    }

    public void setTeacher(AppUser teacher) {
        this.teacher = teacher;
    }

    public void updateDetails(String name, String className) {
        this.name = name;
        this.className = className;
    }

    public Set<AppUser> getStudents() {
        return students;
    }
}
