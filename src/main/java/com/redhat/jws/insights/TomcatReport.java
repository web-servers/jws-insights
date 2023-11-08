package com.redhat.jws.insights;

import java.util.Collections;
import java.util.Map;

import com.redhat.insights.AbstractTopLevelReportBase;
import com.redhat.insights.InsightsReport;
import com.redhat.insights.InsightsSubreport;
import com.redhat.insights.config.InsightsConfiguration;
import com.redhat.insights.logging.InsightsLogger;

public class TomcatReport extends AbstractTopLevelReportBase implements InsightsReport {
    private TomcatReport(
            InsightsLogger logger,
            InsightsConfiguration config,
            Map<String, InsightsSubreport> subReports) {
        super(logger, config, subReports);
    }

    public static TomcatReport of(InsightsLogger logger, InsightsConfiguration configuration) {
        return new TomcatReport(logger, configuration, Collections.emptyMap());
    }

    @Override
    protected long getProcessPID() {
        return Long.parseLong(
                java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
    }

    @Override
    protected Package[] getPackages() {
        return Package.getPackages();
    }

}