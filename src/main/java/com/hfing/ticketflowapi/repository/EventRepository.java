package com.hfing.ticketflowapi.repository;

import com.hfing.ticketflowapi.common.enums.EventStatus;
import com.hfing.ticketflowapi.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, String> {

    List<Event> findByStatusOrderByStartTimeAsc(EventStatus status);

    @Query("SELECT e FROM Event e WHERE e.status = 'PUBLISHED' OR e.organizer.id = :organizerId ORDER BY e.startTime ASC")
    List<Event> findPublishedOrOrganizerEvents(@Param("organizerId") String organizerId);

    List<Event> findAllByOrderByStartTimeAsc();
}
