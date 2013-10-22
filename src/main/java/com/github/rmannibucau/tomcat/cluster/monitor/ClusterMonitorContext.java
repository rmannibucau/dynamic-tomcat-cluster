package com.github.rmannibucau.tomcat.cluster.monitor;

import com.github.rmannibucau.tomcat.cluster.interceptor.DynamicMembershipInterceptor;
import org.apache.catalina.Cluster;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.StandardContext;

import java.beans.PropertyChangeListener;
import java.util.concurrent.ConcurrentMap;

// TODO: security?
public class ClusterMonitorContext extends StandardContext {
    private static final String CONTEXT_NAME = System.getProperty("rmannibucau.cluster.monitor.context", "/rmannibucau-tomcat-monitor");
    private static final String MAPPING = System.getProperty("rmannibucau.cluster.monitor.controller", "/controller");

    private final ConcurrentMap<Cluster, DynamicMembershipInterceptor> clusters;
    private Boolean configured = null;

    public ClusterMonitorContext(final ConcurrentMap<Cluster, DynamicMembershipInterceptor> clusters) {
        setDocBase("");
        setParentClassLoader(ClusterMonitorContext.class.getClassLoader());
        setDelegate(true);
        setName(CONTEXT_NAME);
        setPath(CONTEXT_NAME);
        setLoader(new ServerClassLoaderLoader(this));
        setProcessTlds(false);

        this.clusters = clusters;

        addLifecycleListener(new LifecycleListener() {
            @Override
            public void lifecycleEvent(final LifecycleEvent event) {
                try {
                    final Context context = Context.class.cast(event.getLifecycle());
                    if (event.getType().equals(Lifecycle.CONFIGURE_START_EVENT)) {
                        context.setConfigured(true);
                    }
                } catch (final ClassCastException e) {
                    // no-op
                }
            }
        });
    }

    @Override
    protected void initInternal() throws LifecycleException {
        super.initInternal();
        final Wrapper servlet = createWrapper();
        servlet.setName(MonitorController.class.getSimpleName());
        servlet.setServlet(new MonitorController(clusters));
        addChild(servlet);
        addServletMapping(MAPPING, MonitorController.class.getSimpleName());
    }

    private static class ServerClassLoaderLoader implements Loader {
        private static final String[] EMPTY_ARRAY = new String[0];

        private final Container container;

        private ServerClassLoaderLoader(final Container container) {
            this.container = container;
        }

        @Override
        public void backgroundProcess() {
            // no-op
        }

        @Override
        public ClassLoader getClassLoader() {
            return ClusterMonitorContext.class.getClassLoader();
        }

        @Override
        public Container getContainer() {
            return container;
        }

        @Override
        public void setContainer(final Container container) {
            // no-op
        }

        @Override
        public boolean getDelegate() {
            return true;
        }

        @Override
        public void setDelegate(final boolean delegate) {
            // no-op
        }

        @Override
        public String getInfo() {
            return ServerClassLoaderLoader.class.getName() + "/1.0";
        }

        @Override
        public boolean getReloadable() {
            return false;
        }

        @Override
        public void setReloadable(final boolean reloadable) {
            // no-op
        }

        @Override
        public void addPropertyChangeListener(final PropertyChangeListener listener) {
            // no-op
        }

        @Override
        public void addRepository(final String repository) {
            // no-op
        }

        @Override
        public String[] findRepositories() {
            return EMPTY_ARRAY;
        }

        @Override
        public boolean modified() {
            return false;
        }

        @Override
        public void removePropertyChangeListener(final PropertyChangeListener listener) {
            // no-op
        }
    }
}
