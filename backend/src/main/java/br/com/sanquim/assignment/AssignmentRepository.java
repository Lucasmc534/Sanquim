package br.com.sanquim.assignment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Consultas de tarefas ordenadas e sempre restringidas à matéria informada. */
public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {
    List<Assignment> findBySubject_IdOrderByDueAtAsc(UUID subjectId);

    Optional<Assignment> findByIdAndSubject_Id(UUID assignmentId, UUID subjectId);

    long countBySubject_Id(UUID subjectId);
}
