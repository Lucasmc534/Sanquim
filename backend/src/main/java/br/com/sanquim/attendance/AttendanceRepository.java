package br.com.sanquim.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Consultas de presença por matéria, aluno e data de aula. */
public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {
    List<Attendance> findBySubject_IdOrderByLessonDateDesc(UUID subjectId);

    List<Attendance> findBySubject_IdAndStudent_IdOrderByLessonDateDesc(UUID subjectId, UUID studentId);

    Optional<Attendance> findByIdAndSubject_Id(UUID attendanceId, UUID subjectId);

    boolean existsBySubject_IdAndStudent_IdAndLessonDate(UUID subjectId, UUID studentId, LocalDate lessonDate);

    boolean existsBySubject_IdAndStudent_IdAndLessonDateAndIdNot(
        UUID subjectId, UUID studentId, LocalDate lessonDate, UUID attendanceId);

    long countBySubject_Id(UUID subjectId);
}
