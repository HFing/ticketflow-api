package com.hfing.ticketflowapi.event.repository;

import com.hfing.ticketflowapi.event.entity.TicketType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;

@Repository
public interface TicketTypeRepository extends JpaRepository<TicketType, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<TicketType> findAllByIdInOrderByIdAsc(Collection<String> ids);
}
