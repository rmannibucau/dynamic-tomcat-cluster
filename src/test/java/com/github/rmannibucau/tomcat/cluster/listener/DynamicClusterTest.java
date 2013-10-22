package com.github.rmannibucau.tomcat.cluster.listener;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.ha.CatalinaCluster;
import org.apache.catalina.ha.tcp.SimpleTcpCluster;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.tribes.Member;
import org.apache.coyote.http11.Http11Protocol;
import org.junit.Test;

import java.io.File;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class DynamicClusterTest {
    @Test
    public void test() throws Exception {
        final Tomcat tomcat1 = newTomcat("tomcat1", 1234);
        final Tomcat tomcat2 = newTomcat("tomcat2", 5678);

        try {
            tomcat1.start();
            tomcat2.start();

            final String members = "localhost:1234\nlocalhost:5678\nlocalhost:9123";
            doPost("http://" + tomcat1.getHost().getName() + ":" + tomcat1.getConnector().getPort() + "/rmannibucau-tomcat-monitor/controller", members);
            doPost("http://" + tomcat1.getHost().getName() + ":" + tomcat2.getConnector().getPort() + "/rmannibucau-tomcat-monitor/controller", members);

            final CatalinaCluster cluster1 = CatalinaCluster.class.cast(tomcat1.getEngine().getCluster());
            assertEquals(3, cluster1.getMembers().length);

            final Member[] cluster2 = CatalinaCluster.class.cast(tomcat2.getEngine().getCluster()).getMembers();
            assertEquals(3, cluster2.length);
        } finally {
            destroy(tomcat1);
            destroy(tomcat2);
        }
    }

    private static void doPost(final String target, final String payload) throws Exception {
        final URL url = new URL(target);

        final HttpURLConnection connection = HttpURLConnection.class.cast(url.openConnection());
        connection.setRequestMethod("POST");
        connection.setUseCaches(false);
        connection.setDoInput(true);
        connection.setDoOutput(true);

        try {
            final OutputStream output = connection.getOutputStream();
            try {
                output.write(payload.getBytes());
                output.flush();

                final int status = connection.getResponseCode();
                if (status / 100 != 2) {
                    fail(Integer.toString(status));
                }
            } finally {
                if (output != null) {
                    output.close();
                }
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static void destroy(final Tomcat tomcat1) throws LifecycleException {
        tomcat1.stop();
        tomcat1.destroy();
    }

    private static Tomcat newTomcat(final String base, final int port) throws LifecycleException {
        final Connector connector = new Connector(Http11Protocol.class.getName());
        connector.setPort(port);

        final Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(mkdirs("target/" + base + "/conf"));
        tomcat.getHost().setAppBase(mkdirs("target/" + base + "/webapps"));
        tomcat.getEngine().setName(base);
        tomcat.getEngine().setCluster(new SimpleTcpCluster());
        tomcat.getServer().addLifecycleListener(new DynamicClusterListener());
        tomcat.getService().addConnector(connector);
        tomcat.setConnector(connector);

        return tomcat;
    }

    private static String mkdirs(String base) throws LifecycleException {
        final File file = new File(base);
        if (!file.exists() && !file.mkdirs()) {
            throw new LifecycleException("Can't create workdir");
        }
        return file.getAbsolutePath();
    }
}
