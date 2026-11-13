package br.com.sanquim.subject;

import br.com.sanquim.attendance.AttendanceRepository;
import br.com.sanquim.assignment.AssignmentRepository;
import br.com.sanquim.grade.GradeRepository;
import br.com.sanquim.lesson.LessonRepository;
import br.com.sanquim.user.AppUserRepository;
import br.com.sanquim.user.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/subjects")
/** Gerencia matérias e matrículas com regras de acesso específicas por papel. */
public class SubjectController {
    private final SubjectRepository subjects;
    private final AppUserRepository users;
    private final SubjectAccessService subjectAccess;
    private final GradeRepository grades;
    private final AttendanceRepository attendance;
    private final LessonRepository lessons;
    private final AssignmentRepository assignments;

    public SubjectController(SubjectRepository subjects, AppUserRepository users,
                             SubjectAccessService subjectAccess, GradeRepository grades,
                             AttendanceRepository attendance, LessonRepository lessons,
                             AssignmentRepository assignments) {
        this.subjects = subjects;
        this.users = users;
        this.subjectAccess = subjectAccess;
        this.grades = grades;
        this.attendance = attendance;
        this.lessons = lessons;
        this.assignments = assignments;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<SubjectResponse> list(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = subjectAccess.currentUserId(jwt);
        List<Subject> visibleSubjects = switch (subjectAccess.currentRole(jwt)) {
            case SECRETARIA -> subjects.findAll();
            case PROFESSOR -> subjects.findDistinctByTeacher_Id(userId);
            case ALUNO -> subjects.findDistinctByStudents_Id(userId);
        };
        return visibleSubjects.stream().map(SubjectResponse::from).toList();
    }

    @GetMapping("/{subjectId}/students")
    @PreAuthorize("hasAnyRole('PROFESSOR', 'SECRETARIA')")
    @Transactional(readOnly = true)
    public List<StudentResponse> listStudents(@PathVariable UUID subjectId,
                                              @AuthenticationPrincipal Jwt jwt) {
        return subjectAccess.requireManageableSubject(subjectId, jwt).getStudents().stream()
            .map(StudentResponse::from)
            .sorted(Comparator.comparing(StudentResponse::name, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('SECRETARIA')")
    @Transactional
    public SubjectResponse create(@Valid @RequestBody CreateSubjectRequest request) {
        var teacher = users.findById(request.teacherId())
            .filter(user -> user.getRole() == UserRole.PROFESSOR)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "O professor informado não existe ou não tem o papel de professor."));
        return SubjectResponse.from(subjects.save(
            new Subject(request.name(), request.className(), teacher)));
    }

    @PutMapping("/{subjectId}")
    @PreAuthorize("hasRole('SECRETARIA')")
    @Transactional
    public SubjectResponse update(@PathVariable UUID subjectId,
                                  @Valid @RequestBody UpdateSubjectRequest request) {
        Subject subject = subjects.findById(subjectId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Matéria não encontrada."));
        subject.updateDetails(request.name(), request.className());
        return SubjectResponse.from(subject);
    }

    @PutMapping("/{subjectId}/teacher")
    @PreAuthorize("hasRole('SECRETARIA')")
    @Transactional
    public SubjectResponse assignTeacher(@PathVariable UUID subjectId,
                                         @Valid @RequestBody AssignTeacherRequest request) {
        Subject subject = subjects.findById(subjectId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Matéria não encontrada."));
        var teacher = users.findById(request.teacherId())
            .filter(user -> user.getRole() == UserRole.PROFESSOR)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "O usuário informado não existe ou não tem o papel de professor."));
        subject.setTeacher(teacher);
        return SubjectResponse.from(subject);
    }

    @PostMapping("/{subjectId}/students/{studentId}")
    @PreAuthorize("hasAnyRole('PROFESSOR', 'SECRETARIA')")
    @Transactional
    public SubjectResponse enrollStudent(@PathVariable UUID subjectId, @PathVariable UUID studentId,
                                         @AuthenticationPrincipal Jwt jwt) {
        Subject subject = subjectAccess.requireManageableSubject(subjectId, jwt);
        subject.getStudents().add(subjectAccess.requireStudent(studentId));
        return SubjectResponse.from(subject);
    }

    @DeleteMapping("/{subjectId}/students/{studentId}")
    @PreAuthorize("hasAnyRole('PROFESSOR', 'SECRETARIA')")
    @Transactional
    public void unenrollStudent(@PathVariable UUID subjectId, @PathVariable UUID studentId,
                                @AuthenticationPrincipal Jwt jwt) {
        Subject subject = subjectAccess.requireManageableSubject(subjectId, jwt);
        subject.getStudents().removeIf(student -> student.getId().equals(studentId));
    }

    @DeleteMapping("/{subjectId}")
    @PreAuthorize("hasRole('SECRETARIA')")
    @Transactional
    public void delete(@PathVariable UUID subjectId) {
        Subject subject = subjects.findById(subjectId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Matéria não encontrada."));
        // Mantém o histórico acadêmico; apagar a matéria com registros quebraria referências.
        boolean hasAcademicHistory = grades.countBySubject_Id(subjectId) > 0
            || attendance.countBySubject_Id(subjectId) > 0
            || lessons.countBySubject_Id(subjectId) > 0
            || assignments.countBySubject_Id(subjectId) > 0
            || subjects.countStoredMaterialsBySubjectId(subjectId) > 0;
        if (hasAcademicHistory) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "A matéria possui registros acadêmicos e não pode ser removida.");
        }
        subjects.delete(subject);
    }

    /** Dados de criação da matéria e atribuição inicial do professor. */
    public record CreateSubjectRequest(@NotBlank @Size(max = 120) String name,
                                       @NotBlank @Size(max = 40) String className,
                                       @NotNull UUID teacherId) {
    }

    /** Identifica o novo professor responsável pela matéria. */
    public record AssignTeacherRequest(@NotNull UUID teacherId) {
    }

    /** Campos editáveis da matéria sem alterar sua atribuição de professor. */
    public record UpdateSubjectRequest(@NotBlank @Size(max = 120) String name,
                                       @NotBlank @Size(max = 40) String className) {
    }

    /** Identificação mínima de aluno matriculado, sem dados de login. */
    public record StudentResponse(UUID id, String name) {
        static StudentResponse from(br.com.sanquim.user.AppUser user) {
            return new StudentResponse(user.getId(), user.getName());
        }
    }

    /** Representação da matéria e do professor responsável. */
    public record SubjectResponse(UUID id, String name, String className, UUID teacherId) {
        static SubjectResponse from(Subject subject) {
            return new SubjectResponse(subject.getId(), subject.getName(), subject.getClassName(),
                subject.getTeacher().getId());
        }
    }
}
