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
import org.apache.catalina.Valve;
import org.apache.catalina.Wrapper;
import org.apache.catalina.authenticator.BasicAuthenticator;
import org.apache.catalina.authenticator.DigestAuthenticator;
import org.apache.catalina.authenticator.NonLoginAuthenticator;
import org.apache.catalina.authenticator.SSLAuthenticator;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.deploy.SecurityCollection;
import org.apache.catalina.deploy.SecurityConstraint;

import java.beans.PropertyChangeListener;
import java.util.concurrent.ConcurrentMap;

// TODO: security?
public class ClusterMonitorContext extends StandardContext {
    private static final String CONTEXT_NAME = System.getProperty("rmannibucau.cluster.monitor.context", "/rmannibucau-tomcat-monitor");
    private static final String MAPPING = System.getProperty("rmannibucau.cluster.monitor.controller", "/controller");
    private static final String AUTH = System.getProperty("rmannibucau.cluster.monitor.auth", "NONE");
    private static final String REALM = System.getProperty("rmannibucau.cluster.monitor.realm", "");
    private static final String ROLE = System.getProperty("rmannibucau.cluster.monitor.role", "system");
    private static final String TRANSPORT_GUARANTEE = System.getProperty("rmannibucau.cluster.monitor.transport-guarantee", "NONE");
    private static final String VALVE = System.getProperty("rmannibucau.cluster.monitor.valve");

    private final ConcurrentMap<Cluster, DynamicMembershipInterceptor> clusters;

    public ClusterMonitorContext(final ConcurrentMap<Cluster, DynamicMembershipInterceptor> clusters) {
        setDocBase("");
        setParentClassLoader(ClusterMonitorContext.class.getClassLoader());
        setDelegate(true);
        setName(CONTEXT_NAME);
        setPath(CONTEXT_NAME);
        setLoader(new ServerClassLoaderLoader(this));
        setProcessTlds(false);

        this.clusters = clusters;

        // needed to let tomcat handle this context as started
        addLifecycleListener(new LifecycleListener() {
            @Override
            public void lifecycleEvent(final LifecycleEvent event) {
                if (!Context.class.isInstance(event.getSource())) {
                    return;
                }

                final Context context = Context.class.cast(event.getSource());
                if (event.getType().equals(Lifecycle.CONFIGURE_START_EVENT)) {
                    context.setConfigured(true);
                }
            }
        });

        // in TomEE exclude this context
        System.setProperty(CONTEXT_NAME.replaceFirst("^/", "") + ".tomcat-only", Boolean.TRUE.toString());

        // setup security
        if ("BASIC".equals(AUTH) || "DIGEST".equals(AUTH) || "CLIENT-CERT".equals(AUTH)) {
            final LoginConfig loginConfig = new LoginConfig();
            loginConfig.setAuthMethod(AUTH);
            loginConfig.setRealmName(REALM);
            setLoginConfig(loginConfig);

            //Setup a default Security Constraint
            for (final String role : ROLE.split(",")) {
                final SecurityCollection collection = new SecurityCollection();
                collection.addMethod("GET");
                collection.addMethod("POST");
                collection.addMethod("DELETE");
                collection.addMethod("PATCH");
                collection.addMethod("PUT");
                collection.addMethod("OPTIONS");
                collection.addMethod("TRACE");
                collection.addMethod("HEAD");
                collection.addMethod("CONNECT");
                collection.addPattern("/*");
                collection.setName(role);

                final SecurityConstraint sc = new SecurityConstraint();
                sc.addAuthRole("*");
                sc.addCollection(collection);
                sc.setAuthConstraint(true);
                sc.setUserConstraint(TRANSPORT_GUARANTEE);

                addConstraint(sc);
                addSecurityRole(role);
            }

            //Set the proper authenticator
            if ("BASIC".equals(AUTH)) {
                addValve(new BasicAuthenticator());
            } else if ("DIGEST".equals(AUTH)) {
                addValve(new DigestAuthenticator());
            } else if ("CLIENT-CERT".equals(AUTH)) {
                addValve(new SSLAuthenticator());
            } else if ("NONE".equals(AUTH)) {
                addValve(new NonLoginAuthenticator());
            }
        }

        if (VALVE != null) {
            for (final String valve : VALVE.split(",")) {
                try {
                    addValve(Valve.class.cast(getLoader().getClassLoader().loadClass(valve.trim())));
                } catch (final ClassNotFoundException e) {
                    getLogger().error(e.getMessage(), e);
                }
            }
        }
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
