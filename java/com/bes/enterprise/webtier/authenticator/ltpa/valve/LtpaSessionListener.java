package com.bes.enterprise.webtier.authenticator.ltpa.valve;

import org.apache.catalina.Authenticator;
import org.apache.catalina.Context;
import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.SessionEvent;
import org.apache.catalina.SessionListener;

public class LtpaSessionListener implements SessionListener {

    @Override
    public void sessionEvent(SessionEvent event) {
        if (!Session.SESSION_DESTROYED_EVENT.equals(event.getType())) {
            return;
        }

        Session session = event.getSession();
        Manager manager = session.getManager();
        if (manager == null) {
            return;
        }

        Context context = manager.getContext();
        Authenticator authenticator = context.getAuthenticator();
        if (!(authenticator instanceof LtpaAuthenticator)) {
            return;
        }

        LtpaAuthenticator.class.cast(authenticator).sessionDestroyed(session);
    }
}
