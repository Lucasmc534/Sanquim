package br.com.sanquim.subject;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Consultas de matérias por professor, aluno ou vínculo. */
public interface SubjectRepository extends JpaRepository<Subject, UUID> {
    List<Subject> findDistinctByTeacher_EmailIgnoreCaseOrStudents_EmailIgnoreCase(
        String teacherEmail, String studentEmail);

    List<Subject> findDistinctByTeacher_Id(UUID teacherId);

    List<Subject> findDistinctByStudents_Id(UUID studentId);

    Optional<Subject> findByIdAndTeacher_Id(UUID subjectId, UUID teacherId);

    @Query(value = "SELECT COUNT(*) FROM materials WHERE subject_id = :subjectId", nativeQuery = true)
    long countStoredMaterialsBySubjectId(@Param("subjectId") UUID subjectId);
}
