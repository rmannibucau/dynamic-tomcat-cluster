package com.github.rmannibucau.tomcat.cluster.interceptor;

import org.apache.catalina.tribes.Channel;
import org.apache.catalina.tribes.ChannelException;
import org.apache.catalina.tribes.Member;
import org.apache.catalina.tribes.group.interceptors.StaticMembershipInterceptor;

import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Logger;

public class DynamicMembershipInterceptor extends StaticMembershipInterceptor {
    private static final Logger LOGGER = Logger.getLogger(DynamicMembershipInterceptor.class.getName());

    public void update(final Collection<Member> newMemberList) {
        final Member local = getLocalMember(false);

        synchronized (members) {
            final Iterator<Member> it = members.iterator();
            while (it.hasNext()) {
                final Member old = it.next();

                if (!newMemberList.contains(old)) {
                    it.remove();
                    memberDisappeared(old);
                    LOGGER.info("Removed member: " + old);
                }
            }
            for (final Member diff : newMemberList) {
                if (!members.contains(diff) && !diff.equals(local)) {
                    members.add(diff);
                    memberAdded(diff);
                    LOGGER.info("Added member: " + diff);
                }
            }
        }
    }

    @Override
    public void start(int svc) throws ChannelException {
        super.start(svc  & (~Channel.MBR_TX_SEQ));
    }
}
