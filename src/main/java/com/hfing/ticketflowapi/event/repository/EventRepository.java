package com.hfing.ticketflowapi.event.repository;

import com.hfing.ticketflowapi.event.entity.Event;
import com.hfing.ticketflowapi.event.enums.EventStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends JpaRepository<Event, String> {

    boolean existsByName(String name);

    @Query("SELECT e FROM Event e WHERE e.status = :status ORDER BY (SELECT MIN(s.startTime) FROM EventShow s WHERE s.event = e) ASC NULLS LAST")
    Page<Event> findByStatusOrderByEarliestShowStartTime(
            @Param("status") EventStatus status,
            Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.status = 'PUBLISHED' OR e.organizer.id = :organizerId ORDER BY (SELECT MIN(s.startTime) FROM EventShow s WHERE s.event = e) ASC NULLS LAST")
    List<Event> findPublishedOrOrganizerEvents(@Param("organizerId") String organizerId);

    @Query("SELECT e FROM Event e ORDER BY (SELECT MIN(s.startTime) FROM EventShow s WHERE s.event = e) ASC NULLS LAST")
    Page<Event> findAllOrderByEarliestShowStartTime(Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.id = :id AND e.status = 'PUBLISHED'")
    Optional<Event> findByIdAndStatusPublished(@Param("id") String id);

    @Query(value = """
        SELECT e
        FROM Event e
        JOIN e.shows es
        WHERE e.status = :status
          AND es.startTime > :now
        GROUP BY e
        ORDER BY MIN(es.startTime) ASC
        """, countQuery = """
        SELECT COUNT(DISTINCT e)
        FROM Event e
        JOIN e.shows es
        WHERE e.status = :status
          AND es.startTime > :now
        """)
    Page<Event> findPublishedEventsWithUpcomingShows(
            @Param("status") EventStatus status,
            @Param("now") LocalDateTime now,
            Pageable pageable);
}
