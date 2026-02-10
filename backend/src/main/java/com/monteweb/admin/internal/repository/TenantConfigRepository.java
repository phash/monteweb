package com.monteweb.admin.internal.repository;

import com.monteweb.admin.internal.model.TenantConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TenantConfigRepository extends JpaRepository<TenantConfig, UUID> {
}
