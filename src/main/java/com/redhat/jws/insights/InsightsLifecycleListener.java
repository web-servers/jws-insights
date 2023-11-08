package com.redhat.jws.insights;

import com.redhat.insights.config.EnvAndSysPropsInsightsConfiguration;
import com.redhat.insights.config.InsightsConfiguration;
import com.redhat.insights.logging.InsightsLogger;
import com.redhat.insights.logging.JulLogger;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;

public class InsightsLifecycleListener implements LifecycleListener {

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        InsightsLogger logger = new JulLogger(InsightsLifecycleListener.class.getName());
        InsightsConfiguration configuration = new EnvAndSysPropsInsightsConfiguration();
        if (Lifecycle.BEFORE_INIT_EVENT.equals(event.getType())) {
            
        } else if (Lifecycle.AFTER_DESTROY_EVENT.equals(event.getType())) {
            
        } else if (Lifecycle.PERIODIC_EVENT.equals(event.getType())) {
            
        }
    }

}
