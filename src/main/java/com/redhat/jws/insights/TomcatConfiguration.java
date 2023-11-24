package com.redhat.jws.insights;

import com.redhat.insights.InsightsException;
import com.redhat.insights.config.EnvAndSysPropsInsightsConfiguration;

public class TomcatConfiguration extends EnvAndSysPropsInsightsConfiguration {

    @Override
    public String getIdentificationName() {
        try {
            return super.getIdentificationName();
        } catch (InsightsException e) {
            return "JWS";
        }
    }

}
