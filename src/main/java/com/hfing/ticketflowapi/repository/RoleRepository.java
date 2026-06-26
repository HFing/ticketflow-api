package com.hfing.ticketflowapi.repository;

import com.hfing.ticketflowapi.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, String> {
    Optional<Role> findByName(String roleName);

    boolean existsByName(String name);
}