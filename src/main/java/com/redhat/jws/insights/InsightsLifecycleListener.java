package com.redhat.jws.insights;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.redhat.insights.InsightsReport;
import com.redhat.insights.InsightsReportController;
import com.redhat.insights.InsightsSubreport;
import com.redhat.insights.config.EnvAndSysPropsInsightsConfiguration;
import com.redhat.insights.config.InsightsConfiguration;
import com.redhat.insights.core.httpclient.InsightsJdkHttpClient;
import com.redhat.insights.http.InsightsFileWritingClient;
import com.redhat.insights.http.InsightsMultiClient;
import com.redhat.insights.jars.ClasspathJarInfoSubreport;
import com.redhat.insights.logging.InsightsLogger;
import com.redhat.insights.tls.PEMSupport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;

import javax.net.ssl.SSLContext;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Server;

public class InsightsLifecycleListener implements LifecycleListener {

    private InsightsReportController insightsReportController;
    private InsightsReport insightsReport;
    private InsightsLogger logger = new TomcatLogger();
    private InsightsConfiguration configuration = new EnvAndSysPropsInsightsConfiguration();

    @Override
    public void lifecycleEvent(LifecycleEvent event) {

        if (Lifecycle.AFTER_START_EVENT.equals(event.getType())) {
            if (!(event.getLifecycle() instanceof Server)) {
                throw new IllegalArgumentException("Not associated with a server");
            }
            // Init Insights
            PEMSupport pemSupport = new PEMSupport(logger, configuration);
            Supplier<SSLContext> sslContextSupplier = () -> {
               try {
                  return pemSupport.createTLSContext();
               } catch (Throwable e) {
                  throw new IllegalStateException("Error setting TLS", e);
               }
            };

            Map<String, InsightsSubreport> subReports = new LinkedHashMap<>(2);
            TomcatSubreport tomcatSubreport = new TomcatSubreport();
            ClasspathJarInfoSubreport jarsSubreport = new ClasspathJarInfoSubreport(logger);
            subReports.put("tomcat", tomcatSubreport);
            subReports.put("jars", jarsSubreport);
            insightsReport = TomcatReport.of(logger, configuration, subReports);

            Server server = (Server) event.getLifecycle();
            TomcatInsightsScheduler insightsScheduler =
                  new TomcatInsightsScheduler(logger, configuration, server.getUtilityExecutor());

            try {
               insightsReportController = InsightsReportController.of(logger, configuration, insightsReport,
                     () -> new InsightsMultiClient(logger,
                           new InsightsJdkHttpClient(logger, configuration, sslContextSupplier),
                           new InsightsFileWritingClient(logger, configuration)), insightsScheduler,
                     new LinkedBlockingQueue<>());
               insightsReportController.generate();
            } catch (Throwable e) {
               throw new IllegalStateException("Insights init failure", e);
            }
            try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                    JsonGenerator generator = createFor(insightsReport).writerWithDefaultPrettyPrinter().createGenerator(out)) {
                insightsReport.getSerializer().serialize(insightsReport, generator, null);
                generator.flush();
                byte[] report = out.toByteArray();
                System.out.println("Report: " + new String(report, "UTF-8"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (Lifecycle.STOP_EVENT.equals(event.getType())) {
            if (insightsReportController != null) {
                insightsReportController.shutdown();
            }
        } else if (Lifecycle.PERIODIC_EVENT.equals(event.getType())) {
        }

    }

    public static ObjectMapper createFor(InsightsReport insightsReport) {
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
