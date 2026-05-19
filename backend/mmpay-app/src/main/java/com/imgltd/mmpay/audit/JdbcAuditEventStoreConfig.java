package com.imgltd.mmpay.audit;

import java.time.Clock;
import org.springframework.jdbc.core.JdbcTemplate;

public record JdbcAuditEventStoreConfig(
    JdbcTemplate jdbcTemplate, AuditHasher hasher, AuditLock lock, Clock clock) {}
