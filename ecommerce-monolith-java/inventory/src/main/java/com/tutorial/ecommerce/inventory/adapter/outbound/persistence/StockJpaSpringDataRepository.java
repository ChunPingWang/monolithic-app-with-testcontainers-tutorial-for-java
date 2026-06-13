package com.tutorial.ecommerce.inventory.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface StockJpaSpringDataRepository extends JpaRepository<StockJpaEntity, UUID> {}
