package br.com.sanquim.lesson;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Consultas de aulas ordenadas por data dentro de uma matéria. */
public interface LessonRepository extends JpaRepository<Lesson, UUID> {
    List<Lesson> findBySubject_IdOrderByStartsAtAsc(UUID subjectId);

    Optional<Lesson> findByIdAndSubject_Id(UUID lessonId, UUID subjectId);

    long countBySubject_Id(UUID subjectId);
}
