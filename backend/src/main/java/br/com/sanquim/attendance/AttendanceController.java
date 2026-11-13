package br.com.sanquim.attendance;

import br.com.sanquim.subject.Subject;
import br.com.sanquim.subject.SubjectAccessService;
import br.com.sanquim.user.AppUser;
import br.com.sanquim.user.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/subjects/{subjectId}/attendance")
/** Registra presenças e restringe consultas de aluno aos próprios registros. */
public class AttendanceController {
    private final AttendanceRepository attendance;
    private final SubjectAccessService subjectAccess;

    public AttendanceController(AttendanceRepository attendance, SubjectAccessService subjectAccess) {
        this.attendance = attendance;
        this.subjectAccess = subjectAccess;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<AttendanceResponse> list(@PathVariable UUID subjectId, @AuthenticationPrincipal Jwt jwt) {
        Subject subject = subjectAccess.requireReadableSubject(subjectId, jwt);
        // Professores e secretaria veem a matéria; o aluno nunca recebe registros de colegas.
        List<Attendance> visibleAttendance = subjectAccess.currentRole(jwt) == UserRole.ALUNO
            ? attendance.findBySubject_IdAndStudent_IdOrderByLessonDateDesc(
                subjectId, subjectAccess.currentUserId(jwt))
            : attendance.findBySubject_IdOrderByLessonDateDesc(subject.getId());
        return visibleAttendance.stream().map(AttendanceResponse::from).toList();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PROFESSOR', 'SECRETARIA')")
    @Transactional
    public AttendanceResponse create(@PathVariable UUID subjectId,
                                     @Valid @RequestBody CreateAttendanceRequest request,
                                     @AuthenticationPrincipal Jwt jwt) {
        Subject subject = subjectAccess.requireManageableSubject(subjectId, jwt);
        AppUser student = subjectAccess.requireStudent(request.studentId());
        subjectAccess.requireStudentInSubject(subject, student.getId());
        if (attendance.existsBySubject_IdAndStudent_IdAndLessonDate(
            subjectId, student.getId(), request.lessonDate())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "A presença deste aluno nesta data já foi registrada.");
        }
        Attendance record = attendance.save(new Attendance(subject, student, request.lessonDate(),
            request.present(), subjectAccess.currentUserId(jwt)));
        return AttendanceResponse.from(record);
    }

    @PutMapping("/{attendanceId}")
    @PreAuthorize("hasAnyRole('PROFESSOR', 'SECRETARIA')")
    @Transactional
    public AttendanceResponse update(@PathVariable UUID subjectId, @PathVariable UUID attendanceId,
                                     @Valid @RequestBody CreateAttendanceRequest request,
                                     @AuthenticationPrincipal Jwt jwt) {
        Subject subject = subjectAccess.requireManageableSubject(subjectId, jwt);
        AppUser student = subjectAccess.requireStudent(request.studentId());
        subjectAccess.requireStudentInSubject(subject, student.getId());
        Attendance record = attendance.findByIdAndSubject_Id(attendanceId, subjectId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Registro de presença não encontrado."));
        if (attendance.existsBySubject_IdAndStudent_IdAndLessonDateAndIdNot(
            subjectId, student.getId(), request.lessonDate(), attendanceId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "A presença deste aluno nesta data já foi registrada.");
        }
        record.update(student, request.lessonDate(), request.present(),
            subjectAccess.currentUserId(jwt));
        return AttendanceResponse.from(record);
    }

    @DeleteMapping("/{attendanceId}")
    @PreAuthorize("hasAnyRole('PROFESSOR', 'SECRETARIA')")
    @Transactional
    public void delete(@PathVariable UUID subjectId, @PathVariable UUID attendanceId,
                       @AuthenticationPrincipal Jwt jwt) {
        subjectAccess.requireManageableSubject(subjectId, jwt);
        Attendance record = attendance.findByIdAndSubject_Id(attendanceId, subjectId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro de presença não encontrado."));
        attendance.delete(record);
    }

    /** Identifica o aluno e o resultado de presença que a equipe autorizada registrará. */
    public record CreateAttendanceRequest(@NotNull UUID studentId, @NotNull LocalDate lessonDate,
                                          @NotNull Boolean present) {
    }

    /** Dados de presença retornados à equipe ou ao próprio aluno. */
    public record AttendanceResponse(UUID id, UUID studentId, String studentName,
                                     LocalDate lessonDate, boolean present) {
        static AttendanceResponse from(Attendance record) {
            return new AttendanceResponse(record.getId(), record.getStudent().getId(),
                record.getStudent().getName(), record.getLessonDate(), record.isPresent());
        }
    }
}
