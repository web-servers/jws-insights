package com.redhat.jws.insights;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;

import javax.net.ssl.SSLContext;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.redhat.insights.Filtering;
import com.redhat.insights.InsightsReport;
import com.redhat.insights.InsightsReportController;
import com.redhat.insights.InsightsSubreport;
import com.redhat.insights.config.InsightsConfiguration;
import com.redhat.insights.core.httpclient.InsightsJdkHttpClient;
import com.redhat.insights.http.InsightsFileWritingClient;
import com.redhat.insights.http.InsightsMultiClient;
import com.redhat.insights.jars.ClasspathJarInfoSubreport;
import com.redhat.insights.logging.InsightsLogger;
import com.redhat.insights.tls.PEMSupport;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Server;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.json.JSONParser;
import org.apache.tomcat.util.json.ParseException;

public class InsightsLifecycleListener implements LifecycleListener {

    private static final Log log = LogFactory.getLog(InsightsLifecycleListener.class);

    private InsightsReportController insightsReportController;
    private InsightsReport insightsReport;
    private InsightsLogger logger = new TomcatLogger();
    private InsightsConfiguration configuration = new TomcatConfiguration();

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
            subReports.put("jars", jarsSubreport);
            subReports.put("tomcat", tomcatSubreport);
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

        } else if (Lifecycle.STOP_EVENT.equals(event.getType())) {
            if (insightsReportController != null) {
                insightsReportController.shutdown();
            }
        } else if (Lifecycle.PERIODIC_EVENT.equals(event.getType())) {
        }

    }

}
