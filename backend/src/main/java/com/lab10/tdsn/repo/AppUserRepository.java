package com.lab10.tdsn.repo;

import com.lab10.tdsn.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, String> {
}
