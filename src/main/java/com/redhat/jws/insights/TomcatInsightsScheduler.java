/* Copyright (C) Red Hat 2023 */
package com.redhat.jws.insights;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.redhat.insights.InsightsScheduler;
import com.redhat.insights.config.InsightsConfiguration;
import com.redhat.insights.logging.InsightsLogger;

public class TomcatInsightsScheduler implements InsightsScheduler {

    private boolean active = true;
    private final ScheduledExecutorService utilityExecutor;
    private final InsightsLogger logger;
    private final InsightsConfiguration configuration;

    public TomcatInsightsScheduler(InsightsLogger logger,
            InsightsConfiguration configuration, ScheduledExecutorService utilityExecutor) {
        this.utilityExecutor = utilityExecutor;
        this.logger = logger;
        this.configuration = configuration;
    }

    @Override
    public boolean isShutdown() {
        return false;
    }

    @Override
    public ScheduledFuture<?> scheduleConnect(Runnable command) {
        if (!active) {
            throw new IllegalStateException("Not active");
        }
        return utilityExecutor.scheduleAtFixedRate(command,
                0, configuration.getConnectPeriod().getSeconds(), TimeUnit.SECONDS);
    }

    @Override
    public ScheduledFuture<?> scheduleJarUpdate(Runnable command) {
        if (!active) {
            throw new IllegalStateException("Not active");
        }
        return utilityExecutor.scheduleAtFixedRate(command,
                configuration.getUpdatePeriod().getSeconds(),
                configuration.getUpdatePeriod().getSeconds(),
                TimeUnit.SECONDS);
    }

    @Override
    public void shutdown() {
        active = false;
    }

    @Override
    public List<Runnable> shutdownNow() {
        return List.of();
    }

}
