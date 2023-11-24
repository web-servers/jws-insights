package com.redhat.jws.insights;

import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.json.JSONParser;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.redhat.insights.Filtering;
import com.redhat.insights.InsightsReport;
import com.redhat.insights.InsightsSubreport;
import com.redhat.insights.config.InsightsConfiguration;
import com.redhat.insights.jars.ClasspathJarInfoSubreport;
import com.redhat.insights.logging.InsightsLogger;
import com.redhat.jws.insights.InsightsLifecycleListener;
import com.redhat.jws.insights.TomcatConfiguration;
import com.redhat.jws.insights.TomcatLogger;
import com.redhat.jws.insights.TomcatReport;
import com.redhat.jws.insights.TomcatSubreport;
import com.redhat.jws.insights.TomcatSubreportSerializer;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class TomcatInsightsTest extends TestCase {

    private static final Log log = LogFactory.getLog(TomcatSubreportSerializer.class);

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public TomcatInsightsTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(TomcatInsightsTest.class);
    }

    public void testApp() throws Exception {
        Tomcat tomcat = new Tomcat();
        Connector connector = new Connector("HTTP/1.1");
        connector.setPort(0);
        tomcat.getService().addConnector(connector);
        tomcat.setConnector(connector);
        tomcat.getServer().addLifecycleListener(new InsightsLifecycleListener());
        tomcat.start();

        InsightsLogger logger = new TomcatLogger();
        Map<String, InsightsSubreport> subReports = new LinkedHashMap<>(2);
        TomcatSubreport tomcatSubreport = new TomcatSubreport();
        ClasspathJarInfoSubreport jarsSubreport = new ClasspathJarInfoSubreport(logger);
        subReports.put("jars", jarsSubreport);
        subReports.put("tomcat", tomcatSubreport);
        InsightsConfiguration configuration = new TomcatConfiguration();
        InsightsReport insightsReport = TomcatReport.of(logger, configuration, subReports);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                JsonGenerator generator = createFor(insightsReport).writerWithDefaultPrettyPrinter().createGenerator(out)) {
            insightsReport.generateReport(Filtering.DEFAULT);
            insightsReport.getSerializer().serialize(insightsReport, generator, null);
            generator.flush();
            byte[] report = out.toByteArray();
            String result = (new JSONParser(new String(report, "UTF-8"))).parse().toString();
            log.info("Report: " + result);
            assertTrue(result.indexOf("http-nio-auto") > 0);
            assertTrue(result.indexOf("java.version") > 0);
        }
    }

    private static ObjectMapper createFor(InsightsReport insightsReport) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        SimpleModule simpleModule =
            new SimpleModule(
                "SimpleModule", new Version(1, 0, 0, null, "com.redhat.insights", "runtimes-java"));
        simpleModule.addSerializer(InsightsReport.class, insightsReport.getSerializer());
        for (InsightsSubreport subreport : insightsReport.getSubreports().values()) {
          simpleModule.addSerializer(subreport.getClass(), subreport.getSerializer());
        }
        mapper.registerModule(simpleModule);
        return mapper;
      }

}
