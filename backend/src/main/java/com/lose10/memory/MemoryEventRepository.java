package com.lose10.memory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MemoryEventRepository extends JpaRepository<MemoryEvent, UUID> {
}
