package br.com.sanquim.assignment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Consultas de entregas por tarefa e por aluno, usadas para filtrar dados pessoais. */
public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, UUID> {
    List<AssignmentSubmission> findByAssignment_IdOrderBySubmittedAtDesc(UUID assignmentId);

    Optional<AssignmentSubmission> findByAssignment_IdAndStudent_Id(UUID assignmentId, UUID studentId);

    List<AssignmentSubmission> findByAssignment_Subject_IdAndStudent_Id(UUID subjectId, UUID studentId);

    boolean existsByAssignment_Id(UUID assignmentId);
}
