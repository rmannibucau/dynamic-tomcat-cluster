package com.github.rmannibucau.tomcat.cluster.listener;

import com.github.rmannibucau.tomcat.cluster.interceptor.DynamicMembershipInterceptor;
import com.github.rmannibucau.tomcat.cluster.monitor.ClusterMonitorContext;
import org.apache.catalina.Cluster;
import org.apache.catalina.Container;
import org.apache.catalina.ContainerEvent;
import org.apache.catalina.ContainerListener;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.ha.CatalinaCluster;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DynamicClusterListener implements LifecycleListener {
    private ConcurrentMap<Cluster, DynamicMembershipInterceptor> clusters = new ConcurrentHashMap<Cluster, DynamicMembershipInterceptor>();

    @Override
    public void lifecycleEvent(final LifecycleEvent event) {
        if (Lifecycle.BEFORE_INIT_EVENT.equals(event.getType()) && Server.class.isInstance(event.getSource())) {
            final Server server = Server.class.cast(event.getSource());
            final Service[] services = server.findServices();
            for (final Service service : services) {
                final Container container = service.getContainer();
                final Cluster cluster = container.getCluster();
                if (cluster != null && CatalinaCluster.class.isInstance(cluster)) {
                    final DynamicMembershipInterceptor newInstance = new DynamicMembershipInterceptor();
                    DynamicMembershipInterceptor interceptor = clusters.putIfAbsent(cluster, newInstance);
                    if (interceptor == null) {
                        interceptor = newInstance;
                    }
                    CatalinaCluster.class.cast(cluster).getChannel().addInterceptor(interceptor);
                    clusters.put(cluster, interceptor);

                    container.addContainerListener(new ContainerListener() {
                        @Override
                        public void containerEvent(final ContainerEvent event) {
                            if (event.getType().contains("stop") || event.getType().contains("destroy")) {
                                clusters.remove(cluster);
                            }
                        }
                    });
                }

                if (Engine.class.isInstance(container)) {
                    final Engine engine = Engine.class.cast(container);
                    final Container[] children = engine.findChildren();
                    for (final Container child : children) {
                        if (Host.class.isInstance(child)) {
                            final Host host = Host.class.cast(child);
                            host.addChild(new ClusterMonitorContext(clusters));
                        }
                    }
                }
            }
        }
    }
}
