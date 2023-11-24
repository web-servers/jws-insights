package com.redhat.jws.insights;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;

import javax.net.ssl.SSLContext;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.redhat.insights.Filtering;
import com.redhat.insights.InsightsException;
import com.redhat.insights.InsightsReport;
import com.redhat.insights.InsightsReportController;
import com.redhat.insights.InsightsSubreport;
import com.redhat.insights.config.EnvAndSysPropsInsightsConfiguration;
import com.redhat.insights.config.InsightsConfiguration;
import com.redhat.insights.config.InsightsConfiguration.ProxyConfiguration;
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

public class InsightsLifecycleListener extends EnvAndSysPropsInsightsConfiguration implements LifecycleListener {

    private static final Log log = LogFactory.getLog(InsightsLifecycleListener.class);

    private InsightsReportController insightsReportController;
    private InsightsReport insightsReport;
    private InsightsLogger logger = new TomcatLogger();

    @Override
    public void lifecycleEvent(LifecycleEvent event) {

        if (Lifecycle.AFTER_START_EVENT.equals(event.getType())) {
            if (!(event.getLifecycle() instanceof Server)) {
                throw new IllegalArgumentException("Not associated with a server");
            }
            // Init Insights
            PEMSupport pemSupport = new PEMSupport(logger, this);
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
            insightsReport = TomcatReport.of(logger, this, subReports);

            Server server = (Server) event.getLifecycle();
            TomcatInsightsScheduler insightsScheduler =
                  new TomcatInsightsScheduler(logger, this, server.getUtilityExecutor());

            try {
               insightsReportController = InsightsReportController.of(logger, this, insightsReport,
                     () -> new InsightsMultiClient(logger,
                           new InsightsJdkHttpClient(logger, this, sslContextSupplier),
                           new InsightsFileWritingClient(logger, this)), insightsScheduler,
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

    // Tomcat style intropection configuration, in addition to EnvAndSysPropsInsightsConfiguration

    private String identificationName = null;

    @Override
    public String getIdentificationName() {
        if (identificationName != null) {
            return identificationName;
        }
        try {
            return super.getIdentificationName();
        } catch (InsightsException e) {
            return "JWS";
        }
    }

    public void setIdentificationName(String identificationName) {
        this.identificationName = identificationName;
    }

    private String machineIdFilePath = null;

    @Override
    public String getMachineIdFilePath() {
        if (machineIdFilePath != null) {
            return machineIdFilePath;
        }
        return super.getMachineIdFilePath();
    }

    public void setMachineIdFilePath(String machineIdFilePath) {
        this.machineIdFilePath = machineIdFilePath;
    }

    private String archiveUploadDir = null;

    @Override
    public String getArchiveUploadDir() {
        if (archiveUploadDir != null) {
            return archiveUploadDir;
        }
        return super.getArchiveUploadDir();
    }

    public void setArchiveUploadDir(String archiveUploadDir) {
        this.archiveUploadDir = archiveUploadDir;
    }

    private String certFilePath = null;

    @Override
    public String getCertFilePath() {
        if (certFilePath != null) {
            return certFilePath;
        }
        return super.getCertFilePath();
    }

    public void setCertFilePath(String certFilePath) {
        this.certFilePath = certFilePath;
    }

    private String certHelperBinary = null;

    @Override
    public String getCertHelperBinary() {
        if (certHelperBinary != null) {
            return certHelperBinary;
        }
        return super.getCertHelperBinary();
    }

    public void setCertHelperBinary(String certHelperBinary) {
        this.certHelperBinary = certHelperBinary;
    }

    private Duration connectPeriod = null;

    @Override
    public Duration getConnectPeriod() {
        if (connectPeriod != null) {
            return connectPeriod;
        }
        return super.getConnectPeriod();
    }

    public String getConnectPeriodValue() {
        return getConnectPeriod().toString();
    }

    public void setConnectPeriodValue(String connectPeriod) {
        this.connectPeriod = Duration.parse(connectPeriod);
    }

    private Double httpClientRetryBackoffFactor = null;

    @Override
    public double getHttpClientRetryBackoffFactor() {
        if (httpClientRetryBackoffFactor != null) {
            return httpClientRetryBackoffFactor;
        }
        return super.getHttpClientRetryBackoffFactor();
    }

    // Use a separate fake property since Tomcat does not support double directly
    public String getHttpClientRetryBackoffFactorValue() {
        return Double.toString(getHttpClientRetryBackoffFactor());
    }

    public void setHttpClientRetryBackoffFactorValue(String httpClientRetryBackoffFactor) {
        this.httpClientRetryBackoffFactor = Double.parseDouble(httpClientRetryBackoffFactor);
    }

    private Long httpClientRetryInitialDelay = null;

    @Override
    public long getHttpClientRetryInitialDelay() {
        if (httpClientRetryInitialDelay != null) {
            return httpClientRetryInitialDelay.longValue();
        }
        return super.getHttpClientRetryInitialDelay();
    }

    public void setHttpClientRetryInitialDelay(long httpClientRetryInitialDelay) {
        this.httpClientRetryInitialDelay = Long.valueOf(httpClientRetryInitialDelay);
    }

    private Integer httpClientRetryMaxAttempts = null;

    @Override
    public int getHttpClientRetryMaxAttempts() {
        if (httpClientRetryMaxAttempts != null) {
            return httpClientRetryMaxAttempts.intValue();
        }
        return super.getHttpClientRetryMaxAttempts();
    }

    public void setHttpClientRetryMaxAttempts(int httpClientRetryMaxAttempts) {
        this.httpClientRetryMaxAttempts = Integer.valueOf(httpClientRetryMaxAttempts);
    }

    private Duration httpClientTimeout = null;

    @Override
    public Duration getHttpClientTimeout() {
        if (httpClientTimeout != null) {
            return httpClientTimeout;
        }
        return super.getHttpClientTimeout();
    }

    public String getHttpClientTimeoutValue() {
        return getHttpClientTimeout().toString();
    }

    public void setHttpClientTimeoutValue(String httpClientTimeout) {
        this.httpClientTimeout = Duration.parse(httpClientTimeout);
    }

    private String keyFilePath = null;

    @Override
    public String getKeyFilePath() {
        if (keyFilePath != null) {
            return keyFilePath;
        }
        return super.getKeyFilePath();
    }

    public void setKeyFilePath(String keyFilePath) {
        this.keyFilePath = keyFilePath;
    }

    private String maybeAuthToken = null;

    @Override
    public Optional<String> getMaybeAuthToken() {
        if (maybeAuthToken != null) {
            return Optional.of(maybeAuthToken);
        }
        return super.getMaybeAuthToken();
    }

    public String getMaybeAuthTokenValue() {
        return getMaybeAuthToken().orElse(null);
    }

    public void setMaybeAuthTokenValue(String maybeAuthToken) {
        this.maybeAuthToken = maybeAuthToken;
    }

    private String proxyHost = null;
    private Integer proxyPort = null;

    @Override
    public Optional<ProxyConfiguration> getProxyConfiguration() {
        if (proxyHost != null && proxyPort != null) {
            return Optional.of(new ProxyConfiguration(proxyHost, Integer.valueOf(proxyPort)));
        }
        return super.getProxyConfiguration();
    }

    public String getProxyHost() {
        if (proxyHost != null) {
            return proxyHost;
        }
        ProxyConfiguration config = getProxyConfiguration().orElse(null);
        if (config != null) {
            return config.getHost();
        } else {
            return null;
        }
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public int getProxyPort() {
        if (proxyPort != null) {
            return proxyPort.intValue();
        }
        ProxyConfiguration config = getProxyConfiguration().orElse(null);
        if (config != null) {
            return config.getPort();
        } else {
            return -1;
        }
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = Integer.valueOf(proxyPort);
    }

    private Duration updatePeriod = null;

    @Override
    public Duration getUpdatePeriod() {
        if (updatePeriod != null) {
            return updatePeriod;
        }
        return super.getUpdatePeriod();
    }

    public String getUpdatePeriodValue() {
        return getUpdatePeriod().toString();
    }

    public void setUpdatePeriod(String updatePeriod) {
        this.updatePeriod = Duration.parse(updatePeriod);
    }

    private String uploadBaseURL = null;

    @Override
    public String getUploadBaseURL() {
        if (uploadBaseURL != null) {
            return uploadBaseURL;
        }
        return super.getUploadBaseURL();
    }

    public void setUploadBaseURL(String uploadBaseURL) {
        this.uploadBaseURL = uploadBaseURL;
    }

    private String uploadUri = null;

    @Override
    public String getUploadUri() {
        if (uploadUri != null) {
            return uploadUri;
        }
        return super.getUploadUri();
    }

    public void setUploadUri(String uploadUri) {
        this.uploadUri = uploadUri;
    }

    private Boolean optingOut = null;

    @Override
    public boolean isOptingOut() {
        if (optingOut != null) {
            return optingOut.booleanValue();
        }
        return super.isOptingOut();
    }

    public void setOptingOut(boolean optingOut) {
        this.optingOut = Boolean.valueOf(optingOut);
    }

}
