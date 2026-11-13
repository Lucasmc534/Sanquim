package br.com.sanquim.assignment;

import br.com.sanquim.subject.Subject;
import br.com.sanquim.subject.SubjectAccessService;
import br.com.sanquim.user.AppUser;
import br.com.sanquim.user.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/subjects/{subjectId}/assignments")
/** Administra tarefas e limita cada entrega ao aluno dono ou à equipe autorizada. */
public class AssignmentController {
    private final AssignmentRepository assignments;
    private final AssignmentSubmissionRepository submissions;
    private final SubjectAccessService subjectAccess;

    public AssignmentController(AssignmentRepository assignments,
                                AssignmentSubmissionRepository submissions,
                                SubjectAccessService subjectAccess) {
        this.assignments = assignments;
        this.submissions = submissions;
        this.subjectAccess = subjectAccess;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<AssignmentResponse> list(@PathVariable UUID subjectId,
                                         @AuthenticationPrincipal Jwt jwt) {
        subjectAccess.requireReadableSubject(subjectId, jwt);
        UUID userId = subjectAccess.currentUserId(jwt);
        boolean isStudent = subjectAccess.currentRole(jwt) == UserRole.ALUNO;
        // A resposta informa se o aluno já entregou, sem expor o texto da entrega na listagem.
        Set<UUID> submittedAssignmentIds = isStudent
            ? submissions.findByAssignment_Subject_IdAndStudent_Id(subjectId, userId).stream()
                .map(submission -> submission.getAssignment().getId())
                .collect(Collectors.toSet())
            : Set.of();
        return assignments.findBySubject_IdOrderByDueAtAsc(subjectId).stream()
            .map(assignment -> AssignmentResponse.from(assignment,
                isStudent ? submittedAssignmentIds.contains(assignment.getId()) : null))
            .toList();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PROFESSOR', 'SECRETARIA')")
    @Transactional
    public AssignmentResponse create(@PathVariable UUID subjectId,
                                     @Valid @RequestBody AssignmentRequest request,
                                     @AuthenticationPrincipal Jwt jwt) {
        Subject subject = subjectAccess.requireManageableSubject(subjectId, jwt);
        Assignment assignment = assignments.save(new Assignment(subject, request.title(),
            request.description(), request.dueAt(), subjectAccess.currentUserId(jwt)));
        return AssignmentResponse.from(assignment, false);
    }

    @PutMapping("/{assignmentId}")
    @PreAuthorize("hasAnyRole('PROFESSOR', 'SECRETARIA')")
    @Transactional
    public AssignmentResponse update(@PathVariable UUID subjectId, @PathVariable UUID assignmentId,
                                     @Valid @RequestBody AssignmentRequest request,
                                     @AuthenticationPrincipal Jwt jwt) {
        subjectAccess.requireManageableSubject(subjectId, jwt);
        Assignment assignment = findAssignment(subjectId, assignmentId);
        assignment.update(request.title(), request.description(), request.dueAt());
        return AssignmentResponse.from(assignment, false);
    }

    @DeleteMapping("/{assignmentId}")
    @PreAuthorize("hasAnyRole('PROFESSOR', 'SECRETARIA')")
    @Transactional
    public void delete(@PathVariable UUID subjectId, @PathVariable UUID assignmentId,
                       @AuthenticationPrincipal Jwt jwt) {
        subjectAccess.requireManageableSubject(subjectId, jwt);
        Assignment assignment = findAssignment(subjectId, assignmentId);
        if (submissions.existsByAssignment_Id(assignmentId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "A tarefa já possui entregas e não pode ser removida.");
        }
        assignments.delete(assignment);
    }

    @GetMapping("/{assignmentId}/submissions")
    @Transactional(readOnly = true)
    public List<SubmissionResponse> listSubmissions(@PathVariable UUID subjectId,
                                                    @PathVariable UUID assignmentId,
                                                    @AuthenticationPrincipal Jwt jwt) {
        subjectAccess.requireReadableSubject(subjectId, jwt);
        findAssignment(subjectId, assignmentId);
        List<AssignmentSubmission> visibleSubmissions;
        if (subjectAccess.currentRole(jwt) == UserRole.ALUNO) {
            visibleSubmissions = submissions.findByAssignment_IdAndStudent_Id(
                assignmentId, subjectAccess.currentUserId(jwt)).stream().toList();
        } else {
            visibleSubmissions = submissions.findByAssignment_IdOrderBySubmittedAtDesc(assignmentId);
        }
        return visibleSubmissions.stream().map(SubmissionResponse::from).toList();
    }

    @PostMapping("/{assignmentId}/submissions")
    @PreAuthorize("hasRole('ALUNO')")
    @Transactional
    public SubmissionResponse submit(@PathVariable UUID subjectId, @PathVariable UUID assignmentId,
                                     @Valid @RequestBody SubmitAssignmentRequest request,
                                     @AuthenticationPrincipal Jwt jwt) {
        Subject subject = subjectAccess.requireReadableSubject(subjectId, jwt);
        Assignment assignment = findAssignment(subjectId, assignmentId);
        UUID studentId = subjectAccess.currentUserId(jwt);
        subjectAccess.requireStudentInSubject(subject, studentId);
        AppUser student = subjectAccess.requireStudent(studentId);
        LocalDateTime submittedAt = LocalDateTime.now();
        AssignmentSubmission submission = submissions
            .findByAssignment_IdAndStudent_Id(assignmentId, studentId)
            .map(existing -> {
                existing.replaceText(request.text(), submittedAt);
                return existing;
            })
            .orElseGet(() -> new AssignmentSubmission(assignment, student, request.text(), submittedAt));
        return SubmissionResponse.from(submissions.save(submission));
    }

    private Assignment findAssignment(UUID subjectId, UUID assignmentId) {
        return assignments.findByIdAndSubject_Id(assignmentId, subjectId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarefa não encontrada."));
    }

    /** Campos aceitos ao criar ou atualizar uma tarefa. */
    public record AssignmentRequest(@NotBlank @Size(max = 160) String title,
                                    @Size(max = 16000) String description,
                                    LocalDateTime dueAt) {
    }

    /** Conteúdo textual enviado pelo aluno para a própria tarefa. */
    public record SubmitAssignmentRequest(@NotBlank @Size(max = 16000) String text) {
    }

    /** Representação pública da tarefa; o indicador de entrega só é preenchido para alunos. */
    public record AssignmentResponse(UUID id, String title, String description,
                                     LocalDateTime dueAt, Boolean submitted) {
        static AssignmentResponse from(Assignment assignment, Boolean submitted) {
            return new AssignmentResponse(assignment.getId(), assignment.getTitle(),
                assignment.getDescription(), assignment.getDueAt(), submitted);
        }
    }

    /** Representação de uma entrega; somente o aluno dono e a equipe autorizada podem consultá-la. */
    public record SubmissionResponse(UUID id, UUID studentId, String studentName,
                                     String text, LocalDateTime submittedAt) {
        static SubmissionResponse from(AssignmentSubmission submission) {
            return new SubmissionResponse(submission.getId(), submission.getStudent().getId(),
                submission.getStudent().getName(), submission.getSubmissionText(), submission.getSubmittedAt());
        }
    }
}
