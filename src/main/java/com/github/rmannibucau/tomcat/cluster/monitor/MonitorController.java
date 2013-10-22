package com.github.rmannibucau.tomcat.cluster.monitor;

import com.github.rmannibucau.tomcat.cluster.interceptor.DynamicMembershipInterceptor;
import org.apache.catalina.Cluster;
import org.apache.catalina.ha.CatalinaCluster;
import org.apache.catalina.tribes.Channel;
import org.apache.catalina.tribes.Member;
import org.apache.catalina.tribes.group.ChannelCoordinator;
import org.apache.catalina.tribes.membership.StaticMember;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MonitorController extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(MonitorController.class.getName());

    private static final int ALIVE_TIME = Integer.getInteger("rmannibucau.cluster.monitor.aliveTime", 0);

    private final ConcurrentMap<Cluster, DynamicMembershipInterceptor> clusters;

    public MonitorController(final ConcurrentMap<Cluster, DynamicMembershipInterceptor> clusters) {
        this.clusters = clusters;
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        final Collection<Member> members = new LinkedList<Member>();

        try {
            final InputStream is = req.getInputStream();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    final String[] parts = line.split(":");
                    if (parts.length < 2) {
                        continue;
                    }
                    members.add(new StaticMember(parts[0], Integer.parseInt(parts[1]), ALIVE_TIME));
                }
            } finally {
                is.close();
            }

            for (final DynamicMembershipInterceptor interceptor : clusters.values()) {
                interceptor.update(members);
            }
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            resp.setStatus(HttpURLConnection.HTTP_BAD_REQUEST);
            resp.getWriter().write("{\"status\": \"KO\", \"message\":\"" + e.getMessage().replace("\"", " ") + "\"}");
            return;
        }

        resp.setStatus(HttpURLConnection.HTTP_OK);
        resp.getWriter().write("{\"status\": \"OK\"}");
    }
}
