package com.redhat.jws.insights;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.apache.catalina.manager.StatusTransformer;
import org.apache.tomcat.util.modeler.Registry;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.redhat.insights.InsightsSubreport;

public class TomcatSubreportSerializer extends JsonSerializer<InsightsSubreport> implements NotificationListener {

    /**
     * MBean server.
     */
    protected MBeanServer mBeanServer = null;


    /**
     * Vector of thread pools object names.
     */
    protected final List<ObjectName> threadPools = Collections.synchronizedList(new ArrayList<>());


    /**
     * Vector of request processors object names.
     */
    protected final List<ObjectName> requestProcessors = Collections.synchronizedList(new ArrayList<>());


    /**
     * Vector of global request processors object names.
     */
    protected final List<ObjectName> globalRequestProcessors = Collections.synchronizedList(new ArrayList<>());


    TomcatSubreportSerializer() {
        // Retrieve the MBean server
        mBeanServer = Registry.getRegistry(null, null).getMBeanServer();

        try {

            // Query Thread Pools
            String onStr = "*:type=ThreadPool,*";
            ObjectName objectName = new ObjectName(onStr);
            Set<ObjectInstance> set = mBeanServer.queryMBeans(objectName, null);
            Iterator<ObjectInstance> iterator = set.iterator();
            while (iterator.hasNext()) {
                ObjectInstance oi = iterator.next();
                threadPools.add(oi.getObjectName());
            }

            // Query Global Request Processors
            onStr = "*:type=GlobalRequestProcessor,*";
            objectName = new ObjectName(onStr);
            set = mBeanServer.queryMBeans(objectName, null);
            iterator = set.iterator();
            while (iterator.hasNext()) {
                ObjectInstance oi = iterator.next();
                globalRequestProcessors.add(oi.getObjectName());
            }

            // Query Request Processors
            onStr = "*:type=RequestProcessor,*";
            objectName = new ObjectName(onStr);
            set = mBeanServer.queryMBeans(objectName, null);
            iterator = set.iterator();
            while (iterator.hasNext()) {
                ObjectInstance oi = iterator.next();
                requestProcessors.add(oi.getObjectName());
            }

            // Register with MBean server
            onStr = "JMImplementation:type=MBeanServerDelegate";
            objectName = new ObjectName(onStr);
            mBeanServer.addNotificationListener(objectName, this, null, null);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void handleNotification(Notification notification, Object handback) {

        if (notification instanceof MBeanServerNotification) {
            ObjectName objectName = ((MBeanServerNotification) notification).getMBeanName();
            if (notification.getType().equals(MBeanServerNotification.REGISTRATION_NOTIFICATION)) {
                String type = objectName.getKeyProperty("type");
                if (type != null) {
                    if (type.equals("ThreadPool")) {
                        threadPools.add(objectName);
                    } else if (type.equals("GlobalRequestProcessor")) {
                        globalRequestProcessors.add(objectName);
                    } else if (type.equals("RequestProcessor")) {
                        requestProcessors.add(objectName);
                    }
                }
            } else if (notification.getType().equals(MBeanServerNotification.UNREGISTRATION_NOTIFICATION)) {
                String type = objectName.getKeyProperty("type");
                if (type != null) {
                    if (type.equals("ThreadPool")) {
                        threadPools.remove(objectName);
                    } else if (type.equals("GlobalRequestProcessor")) {
                        globalRequestProcessors.remove(objectName);
                    } else if (type.equals("RequestProcessor")) {
                        requestProcessors.remove(objectName);
                    }
                }
            }
        }
    }

    @Override
    public void serialize(InsightsSubreport subreport, JsonGenerator generator, SerializerProvider serializerProvider) throws IOException {
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        StatusTransformer.writeHeader(writer, null, 2);
        try {
            StatusTransformer.writeVMState(writer, 2, null);
            StatusTransformer.writeConnectorsState(writer, mBeanServer, threadPools, globalRequestProcessors,
                    requestProcessors, 2, null);
            StatusTransformer.writeDetailedState(writer, mBeanServer, 2);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        StatusTransformer.writeFooter(writer, 2);
        writer.flush();
        generator.writeRaw(stringWriter.toString());
    }

}
