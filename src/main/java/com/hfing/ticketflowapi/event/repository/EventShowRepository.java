package com.hfing.ticketflowapi.event.repository;

import com.hfing.ticketflowapi.event.entity.EventShow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventShowRepository extends JpaRepository<EventShow, String> {
}
