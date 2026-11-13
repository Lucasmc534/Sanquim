package br.com.sanquim.grade;

import br.com.sanquim.subject.Subject;
import br.com.sanquim.subject.SubjectAccessService;
import br.com.sanquim.user.AppUser;
import br.com.sanquim.user.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/subjects/{subjectId}/grades")
/** Administra avaliações e limita a leitura dos alunos às próprias notas. */
public class GradeController {
    private final GradeRepository grades;
    private final SubjectAccessService subjectAccess;

    public GradeController(GradeRepository grades, SubjectAccessService subjectAccess) {
        this.grades = grades;
        this.subjectAccess = subjectAccess;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<GradeResponse> list(@PathVariable UUID subjectId, @AuthenticationPrincipal Jwt jwt) {
        Subject subject = subjectAccess.requireReadableSubject(subjectId, jwt);
        // O filtro por studentId é obrigatório para impedir a leitura de notas de colegas.
        List<Grade> visibleGrades = subjectAccess.currentRole(jwt) == UserRole.ALUNO
            ? grades.findBySubject_IdAndStudent_IdOrderByGradedAtDesc(
                subjectId, subjectAccess.currentUserId(jwt))
            : grades.findBySubject_IdOrderByGradedAtDesc(subject.getId());
        return visibleGrades.stream().map(GradeResponse::from).toList();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PROFESSOR', 'SECRETARIA')")
    @Transactional
    public GradeResponse create(@PathVariable UUID subjectId,
                                @Valid @RequestBody CreateGradeRequest request,
                                @AuthenticationPrincipal Jwt jwt) {
        Subject subject = subjectAccess.requireManageableSubject(subjectId, jwt);
        AppUser student = subjectAccess.requireStudent(request.studentId());
        subjectAccess.requireStudentInSubject(subject, student.getId());
        Grade grade = grades.save(new Grade(subject, student, request.title(), request.score(),
            LocalDateTime.now(), subjectAccess.currentUserId(jwt)));
        return GradeResponse.from(grade);
    }

    @PutMapping("/{gradeId}")
    @PreAuthorize("hasAnyRole('PROFESSOR', 'SECRETARIA')")
    @Transactional
    public GradeResponse update(@PathVariable UUID subjectId, @PathVariable UUID gradeId,
                                @Valid @RequestBody CreateGradeRequest request,
                                @AuthenticationPrincipal Jwt jwt) {
        Subject subject = subjectAccess.requireManageableSubject(subjectId, jwt);
        AppUser student = subjectAccess.requireStudent(request.studentId());
        subjectAccess.requireStudentInSubject(subject, student.getId());
        Grade grade = grades.findByIdAndSubject_Id(gradeId, subjectId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nota não encontrada."));
        grade.update(student, request.title(), request.score(), LocalDateTime.now(),
            subjectAccess.currentUserId(jwt));
        return GradeResponse.from(grade);
    }

    @DeleteMapping("/{gradeId}")
    @PreAuthorize("hasAnyRole('PROFESSOR', 'SECRETARIA')")
    @Transactional
    public void delete(@PathVariable UUID subjectId, @PathVariable UUID gradeId,
                       @AuthenticationPrincipal Jwt jwt) {
        subjectAccess.requireManageableSubject(subjectId, jwt);
        Grade grade = grades.findByIdAndSubject_Id(gradeId, subjectId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nota não encontrada."));
        grades.delete(grade);
    }

    /** Dados validados para criar ou atualizar uma nota de aluno matriculado. */
    public record CreateGradeRequest(@NotNull UUID studentId,
                                     @NotBlank @Size(max = 160) String title,
                                     @NotNull @DecimalMin("0.00") @DecimalMax("100.00")
                                     @Digits(integer = 3, fraction = 2) BigDecimal score) {
    }

    /** Dados de nota devolvidos conforme o escopo do usuário autenticado. */
    public record GradeResponse(UUID id, UUID studentId, String studentName, String title,
                                BigDecimal score, LocalDateTime gradedAt) {
        static GradeResponse from(Grade grade) {
            return new GradeResponse(grade.getId(), grade.getStudent().getId(),
                grade.getStudent().getName(), grade.getTitle(), grade.getScore(), grade.getGradedAt());
        }
    }
}
