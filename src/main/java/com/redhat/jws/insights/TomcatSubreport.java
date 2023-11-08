package com.redhat.jws.insights;

import com.fasterxml.jackson.databind.JsonSerializer;
import com.redhat.insights.InsightsSubreport;

public class TomcatSubreport implements InsightsSubreport {

    private final TomcatSubreportSerializer serializer;

    public TomcatSubreport() {
        serializer = new TomcatSubreportSerializer();
    }

    @Override
    public void generateReport() {
        // The serializer will generate the report
    }

    @Override
    public JsonSerializer<InsightsSubreport> getSerializer() {
        return serializer;
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

}