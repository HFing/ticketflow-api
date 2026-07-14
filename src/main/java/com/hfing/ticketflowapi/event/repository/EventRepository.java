package com.hfing.ticketflowapi.event.repository;

import com.hfing.ticketflowapi.event.entity.Event;
import com.hfing.ticketflowapi.event.enums.EventStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;



@Repository
public interface EventRepository extends JpaRepository<Event, String> {

    List<Event> findByStatusOrderByStartTimeAsc(EventStatus status);

    @Query("SELECT e FROM Event e WHERE e.status = 'PUBLISHED' OR e.organizer.id = :organizerId ORDER BY e.startTime ASC")
    List<Event> findPublishedOrOrganizerEvents(@Param("organizerId") String organizerId);

    List<Event> findAllByOrderByStartTimeAsc();
}