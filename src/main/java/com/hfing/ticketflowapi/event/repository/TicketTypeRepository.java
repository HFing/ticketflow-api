package com.hfing.ticketflowapi.event.repository;

import com.hfing.ticketflowapi.event.entity.TicketType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketTypeRepository extends JpaRepository<TicketType, String> {
}
