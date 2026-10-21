package com.bonsaimarket.backend.adminai;

import java.time.Instant;
import java.util.List;

public final class AdminAiDtos {
    private AdminAiDtos() {}
    public record Metric(String id, String name, long value, String definition, String scope, String sourceId) {}
    public record Source(String id, String label, String path) {}
    public record Row(long id, String name, Long count, List<String> warnings, String path) {}
    public record Finding(String id, String text, List<String> metricIds, String nextCheck) {}
    public record Snapshot(String capability, String title, Instant measuredAt, String timezone,
                           String dateRange, List<Metric> metrics, List<Row> rows,
                           List<Source> sources, List<String> warnings, List<Finding> findings) {}
    public record Response(String requestId, String capability, String answer, Instant measuredAt,
                           String timezone, String dateRange, List<Metric> metrics, List<Row> rows,
                           List<Source> sources, List<String> warnings, List<Finding> findings) {}
    public record Capability(String id, String label, String suggestedPrompt) {}
    public record Capabilities(boolean providerConfigured, String providerStatus, String message,
                               boolean readOnly, List<Capability> capabilities, List<String> unsupported) {}
}
