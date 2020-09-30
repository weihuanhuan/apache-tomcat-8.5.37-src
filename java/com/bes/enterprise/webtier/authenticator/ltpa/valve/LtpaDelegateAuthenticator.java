package com.bes.enterprise.webtier.authenticator.ltpa.valve;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.Authenticator;
import org.apache.catalina.Contained;
import org.apache.catalina.Container;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Request;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

public class LtpaDelegateAuthenticator extends LtpaAuthenticator {

    protected Log log = LogFactory.getLog(LtpaDelegateAuthenticator.class);

    private Authenticator delegate;

    @Override
    public void setContainer(Container container) {
        super.setContainer(container);
        if (!(delegate instanceof Contained)) {
            return;
        }
        Contained.class.cast(delegate).setContainer(container);
    }

    @Override
    protected void initInternal() throws LifecycleException {
        super.initInternal();
        if (!(delegate instanceof Lifecycle)) {
            return;
        }
        Lifecycle.class.cast(delegate).init();
    }

    @Override
    protected synchronized void startInternal() throws LifecycleException {
        super.startInternal();
        if (!(delegate instanceof Lifecycle)) {
            return;
        }
        Lifecycle.class.cast(delegate).start();
    }

    @Override
    protected synchronized void stopInternal() throws LifecycleException {
        super.stopInternal();
        if (!(delegate instanceof Lifecycle)) {
            return;
        }
        Lifecycle.class.cast(delegate).stop();
    }

    @Override
    protected void destroyInternal() throws LifecycleException {
        super.destroyInternal();
        if (!(delegate instanceof Lifecycle)) {
            return;
        }
        Lifecycle.class.cast(delegate).destroy();
    }

    @Override
    protected boolean doAuthenticate(Request request, HttpServletResponse response) {
        if (super.doAuthenticate(request, response)) {
            return true;
        }

        boolean authenticate = false;
        try {
            authenticate = delegate.authenticate(request, response);
        } catch (IOException e) {
            log.warn("Failed to authenticate with delegate authenticator! ", e);
        }
        if (authenticate) {
            super.createLtpaCookie(request);
        }
        return authenticate;
    }

    @Override
    public void login(String username, String password, Request request) throws ServletException {
        try {
            super.login(username, password, request);
        } catch (ServletException ex) {
            if (log.isDebugEnabled()) {
                log.debug("Failed to login with ltap authenticator, so we try it again with delegate authenticator!", ex);
            }
            delegate.login(username, password, request);
            super.createLtpaCookie(request);
        }
    }

    @Override
    public void logout(Request request) {
        super.logout(request);
        delegate.logout(request);
    }

    public void setDelegate(Authenticator delegate) {
        this.delegate = delegate;
    }

}
