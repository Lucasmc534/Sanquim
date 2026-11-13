package br.com.sanquim.lesson;

import br.com.sanquim.subject.Subject;
import br.com.sanquim.subject.SubjectAccessService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/subjects/{subjectId}/lessons")
/** Publica e consulta o cronograma somente para usuários autorizados na matéria. */
public class LessonController {
    private final LessonRepository lessons;
    private final SubjectAccessService subjectAccess;

    public LessonController(LessonRepository lessons, SubjectAccessService subjectAccess) {
        this.lessons = lessons;
        this.subjectAccess = subjectAccess;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<LessonResponse> list(@PathVariable UUID subjectId, @AuthenticationPrincipal Jwt jwt) {
        subjectAccess.requireReadableSubject(subjectId, jwt);
        return lessons.findBySubject_IdOrderByStartsAtAsc(subjectId).stream()
            .map(LessonResponse::from).toList();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PROFESSOR', 'SECRETARIA')")
    @Transactional
    public LessonResponse create(@PathVariable UUID subjectId,
                                 @Valid @RequestBody CreateLessonRequest request,
                                 @AuthenticationPrincipal Jwt jwt) {
        Subject subject = subjectAccess.requireManageableSubject(subjectId, jwt);
        Lesson lesson = lessons.save(new Lesson(subject, request.title(), request.description(),
            request.startsAt(), subjectAccess.currentUserId(jwt)));
        return LessonResponse.from(lesson);
    }

    @PutMapping("/{lessonId}")
    @PreAuthorize("hasAnyRole('PROFESSOR', 'SECRETARIA')")
    @Transactional
    public LessonResponse update(@PathVariable UUID subjectId, @PathVariable UUID lessonId,
                                 @Valid @RequestBody CreateLessonRequest request,
                                 @AuthenticationPrincipal Jwt jwt) {
        subjectAccess.requireManageableSubject(subjectId, jwt);
        Lesson lesson = lessons.findByIdAndSubject_Id(lessonId, subjectId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aula não encontrada."));
        lesson.update(request.title(), request.description(), request.startsAt());
        return LessonResponse.from(lesson);
    }

    @DeleteMapping("/{lessonId}")
    @PreAuthorize("hasAnyRole('PROFESSOR', 'SECRETARIA')")
    @Transactional
    public void delete(@PathVariable UUID subjectId, @PathVariable UUID lessonId,
                       @AuthenticationPrincipal Jwt jwt) {
        subjectAccess.requireManageableSubject(subjectId, jwt);
        Lesson lesson = lessons.findByIdAndSubject_Id(lessonId, subjectId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aula não encontrada."));
        lessons.delete(lesson);
    }

    /** Dados validados para criar ou atualizar um item do cronograma. */
    public record CreateLessonRequest(@NotBlank @Size(max = 160) String title,
                                     @Size(max = 16000) String description,
                                     @NotNull LocalDateTime startsAt) {
    }

    /** Representação de um item de cronograma visível aos usuários da matéria. */
    public record LessonResponse(UUID id, String title, String description, LocalDateTime startsAt) {
        static LessonResponse from(Lesson lesson) {
            return new LessonResponse(lesson.getId(), lesson.getTitle(),
                lesson.getDescription(), lesson.getStartsAt());
        }
    }
}
