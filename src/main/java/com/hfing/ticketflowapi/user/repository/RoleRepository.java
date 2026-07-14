package com.hfing.ticketflowapi.user.repository;

import com.hfing.ticketflowapi.user.entity.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;



@Repository
public interface RoleRepository extends JpaRepository<Role, String> {
    Optional<Role> findByName(String roleName);

    boolean existsByName(String name);
}