package br.com.sanquim.event;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Consultas de eventos em ordem cronológica. */
public interface SchoolEventRepository extends JpaRepository<SchoolEvent, UUID> {
    List<SchoolEvent> findAllByOrderByStartsAtAsc();
}
