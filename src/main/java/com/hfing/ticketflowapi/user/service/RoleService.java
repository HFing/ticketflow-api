package com.hfing.ticketflowapi.user.service;

import com.hfing.ticketflowapi.user.entity.Role;


public interface RoleService {
    Role getRoleByName(String name);
}