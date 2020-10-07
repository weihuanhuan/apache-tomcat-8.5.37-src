package com.bes.enterprise.webtier.authenticator.ltpa.valve;

import com.bes.enterprise.webtier.authenticator.ltpa.utils.TokenService;
import org.apache.catalina.Realm;
import org.apache.catalina.Session;
import org.apache.catalina.authenticator.AuthenticatorBase;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;

public class LtpaAuthenticator extends AuthenticatorBase {

    protected Log log = LogFactory.getLog(LtpaAuthenticator.class);

    protected TokenService tokenService;

    private boolean cleanTokenOnSessionInvalid;

    private ThreadLocal<Boolean> tokenDestroy = new InheritableThreadLocal<>();

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        try {
            super.invoke(request, response);
        } finally {
            if (!cleanTokenOnSessionInvalid) {
                return;
            }

            Boolean destroy = tokenDestroy.get();
            if (destroy == null) {
                return;
            }

            if (destroy) {
                cleanLtpaToken(request);
            }
            tokenDestroy.remove();
        }
    }

    @Override
    protected boolean doAuthenticate(Request request, HttpServletResponse response) {
        if (checkForCachedAuthentication(request, response, true)) {
            return true;
        }

        Principal principal = getPrincipal(request);
        if (principal == null) {
            if (log.isDebugEnabled()) {
                log.debug("Failed to authenticate for request uri:" + request.getRequestURI());
            }
            return false;
        }

        String username = tokenService.getUsername(principal);
        String password = tokenService.getPassword(principal);
        register(request, response, principal, getAuthMethod(), username, password);
        if (log.isDebugEnabled()) {
            log.debug("Success to authenticate for request uri:" + request.getRequestURI());
        }
        return true;
    }

    @Override
    protected boolean checkForCachedAuthentication(Request request, HttpServletResponse response, boolean useSSO) {
        if (!super.checkForCachedAuthentication(request, response, useSSO)) {
            return false;
        }

        String ltpaUserUid = tokenService.getCheckUserUid(request);
        if (ltpaUserUid == null || ltpaUserUid.isEmpty()) {
            register(request, response, null, null, null, null);
            return false;
        }

        String username = tokenService.getUsername(request.getPrincipal());
        boolean match = ltpaUserUid.equals(username);
        if (!match) {
            register(request, response, null, null, null, null);
        }
        return match;
    }

    private Principal getPrincipal(Request request) {
        String userUid = tokenService.getLoginUserUid(request);
        if (userUid == null || userUid.isEmpty()) {
            return null;
        }

        Realm realm = request.getContext().getRealm();
        if (realm == null) {
            return null;
        }

        Principal principal = realm.authenticate(userUid);
        if (principal == null) {
            return null;
        }
        return principal;
    }

    @Override
    protected String getAuthMethod() {
        return "LTPA";
    }

    @Override
    protected Principal doLogin(Request request, String loginUsername, String loginPassword) throws ServletException {
        Principal principal = getPrincipal(request);
        if (isLegalLogin(principal, loginUsername, loginPassword)) {
            if (log.isDebugEnabled()) {
                log.debug("User's login info not exist or not match!");
            }
            return principal;
        }

        throw new ServletException(sm.getString("authenticator.loginFail"));
    }

    private boolean isLegalLogin(Principal principal, String username, String password) {
        if (principal == null) {
            return false;
        }
        if (username != null && !username.equals(tokenService.getUsername(principal))) {
            return false;
        }

        if (password != null && !password.equals(tokenService.getPassword(principal))) {
            return false;
        }
        return true;
    }

    @Override
    public void register(Request request, HttpServletResponse response, Principal principal, String authType, String username, String password) {
        super.register(request, response, principal, authType, username, password);

        if (!cleanTokenOnSessionInvalid) {
            return;
        }

        if (principal == null) {
            return;
        }
        authenticated();

        Session sessionInternal = request.getSessionInternal(false);
        if (sessionInternal == null) {
            return;
        }
        sessionInternal.addSessionListener(new LtpaSessionListener());
    }

    @Override
    public void logout(Request request) {
        super.logout(request);
        if (log.isDebugEnabled()) {
            log.debug("Success to logout for request uri:" + request.getRequestURI());
        }

        cleanLtpaToken(request);
    }

    private void cleanLtpaToken(Request request) {
        tokenService.cleanLtpaToken(request);
    }

    private void authenticated() {
        tokenDestroy.set(false);
    }

    public void sessionDestroyed(Session session) {
        if (!cleanTokenOnSessionInvalid) {
            return;
        }
        tokenDestroy.set(true);
    }

    public void setTokenService(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    public void setCleanTokenOnSessionInvalid(boolean cleanTokenOnSessionInvalid) {
        this.cleanTokenOnSessionInvalid = cleanTokenOnSessionInvalid;
    }
}
