package com.tutorial.ecommerce.sharedkernel.domain;

import java.time.Instant;

/**
 * 跨模組 Integration Event 的標記介面。
 * 模組內部的 Domain Event 不必實作這個 — 此介面僅用於 Spring Modulith 的 Event Publication Log。
 */
public interface DomainEvent {
    Instant occurredAt();
}
