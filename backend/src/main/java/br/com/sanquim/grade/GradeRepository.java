package br.com.sanquim.grade;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Consultas de notas com escopo de matéria e, quando necessário, aluno. */
public interface GradeRepository extends JpaRepository<Grade, UUID> {
    List<Grade> findBySubject_IdOrderByGradedAtDesc(UUID subjectId);

    List<Grade> findBySubject_IdAndStudent_IdOrderByGradedAtDesc(UUID subjectId, UUID studentId);

    Optional<Grade> findByIdAndSubject_Id(UUID gradeId, UUID subjectId);

    long countBySubject_Id(UUID subjectId);
}
