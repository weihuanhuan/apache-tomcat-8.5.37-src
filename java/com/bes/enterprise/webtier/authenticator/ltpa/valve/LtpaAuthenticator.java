package com.bes.enterprise.webtier.authenticator.ltpa.valve;

import java.security.Principal;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.Realm;
import org.apache.catalina.authenticator.AuthenticatorBase;
import org.apache.catalina.connector.Request;
import org.apache.catalina.realm.GenericPrincipal;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import com.bes.enterprise.webtier.authenticator.ltpa.utils.UserMetadata;
import com.bes.enterprise.webtier.authenticator.ltpa.utils.TokenFactory;

public class LtpaAuthenticator extends AuthenticatorBase {

    protected Log log = LogFactory.getLog(LtpaAuthenticator.class);

    private static final String EMPTY_STRING = "";

    private TokenFactory tokenFactory;

    private String uidPrefix;
    private String cookieName;
    private String cookieDomain;

    private boolean createToken;

    private String cookieUser;
    private long cookieExpire;

    @Override
    protected boolean doAuthenticate(Request request, HttpServletResponse response) {
        if (checkForCachedAuthentication(request, response, true)) {
            return true;
        }

        Principal principal = getLtpaPrincipal(request);
        if (principal == null) {
            if (log.isDebugEnabled()) {
                log.debug("Failed to authenticate for request uri:" + request.getRequestURI());
            }
            return false;
        }

        register(request, response, principal);
        if (log.isDebugEnabled()) {
            log.debug("Success to authenticate for request uri:" + request.getRequestURI());
        }
        return true;
    }

    private Principal getLtpaPrincipal(Request request) {
        Cookie ltpaTokenCookie = getLtpaTokenCookie(request);
        if (ltpaTokenCookie == null) {
            if (log.isDebugEnabled()) {
                log.debug("LtpaToken cookie not found!");
            }
            return null;
        }

        UserMetadata userMetadata;
        try {
            userMetadata = tokenFactory.decodeLtpaToken(ltpaTokenCookie.getValue(), TokenFactory.LTPA_VERSION.LTPA2);
            cookieUser = userMetadata.getUser();
            cookieExpire = userMetadata.getExpire();
        } catch (Exception e) {
            log.warn("LtpaToken decode failed!", e);
            return null;
        }

        String userUid = getUserUid(cookieUser, uidPrefix);
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

    private Cookie getLtpaTokenCookie(Request request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return null;
        }

        for (int i = 0; i < cookies.length; i++) {
            if (cookies[i].getName().equals(cookieName)) {
                return cookies[i];
            }
        }
        return null;
    }

    private void register(Request request, HttpServletResponse response, Principal principal) {
        String username = getUsername(principal);
        String password = getPassword(principal);
        register(request, response, principal, getAuthMethod(), username, password);
    }

    protected String getUsername(Principal principal) {
        if (principal == null) {
            return null;
        }
        return principal.getName();
    }

    protected String getPassword(Principal principal) {
        if (!(principal instanceof GenericPrincipal)) {
            return null;
        }
        return ((GenericPrincipal) principal).getPassword();
    }

    @Override
    protected String getAuthMethod() {
        return "LTPA";
    }

    @Override
    public void login(String username, String password, Request request) throws ServletException {
        super.login(username, password, request);
        if (log.isDebugEnabled()) {
            log.debug("Success to login for request uri:" + request.getRequestURI());
        }

        if (getAuthMethod().equals(request.getAuthType())) {
            return;
        }
        createLtpaCookie(request);
    }

    @Override
    protected Principal doLogin(Request request, String loginUsername, String loginPassword) throws ServletException {
        Principal principal = getLtpaPrincipal(request);
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
        if (username != null && !username.equals(getUsername(principal))) {
            return false;
        }

        if (password != null && !password.equals(getPassword(principal))) {
            return false;
        }
        return true;
    }

    protected void createLtpaCookie(Request request) {
        if (!createToken || cookieUser == null) {
            return;
        }
        String userUid = getUserUid(cookieUser, uidPrefix);
        if (userUid == null || !userUid.equals(getUsername(request.getPrincipal()))) {
            return;
        }

        //user\:FederatedRealm/uid=tomcat,ou=people,dc=ltpa,dc=com
        //username = "user\\:FederatedRealm/" + uidPrefix + "=" + username + ",ou=people,dc=ltpa,dc=com";
        String username = cookieUser;
        UserMetadata userMetadata = new UserMetadata();
        userMetadata.setExpire(cookieExpire);
        userMetadata.setUser(username);
        userMetadata.setLtpaVersion(TokenFactory.LTPA_VERSION.LTPA2);

        try {
            String token = tokenFactory.encodeLTPAToken(userMetadata, userMetadata.getLtpaVersion());
            addLtpaToken(request, token, (int) userMetadata.getExpire());
        } catch (Exception e) {
            log.warn("Failed to encode LtpaToken for user: " + username, e);
            return;
        }
    }

    private String getUserUid(String user, String uidPrefix) {
        String userUid = null;
        int indexOf = user.indexOf(uidPrefix + "=");
        if (indexOf != -1) {
            userUid = user.substring(indexOf + (uidPrefix + "=").length(), user.indexOf(",", indexOf));
        }
        return userUid;
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
        addLtpaToken(request, EMPTY_STRING, 0);
    }

    private void addLtpaToken(Request request, String value, int maxAge) {
        Cookie cookie = new Cookie(cookieName, value);
        cookie.setMaxAge(maxAge);
        cookie.setPath("/");
        cookie.setSecure(request.isSecure());
        cookie.setDomain(cookieDomain);
        if (request.getServletContext().getSessionCookieConfig().isHttpOnly() || request.getContext().getUseHttpOnly()) {
            cookie.setHttpOnly(true);
        }
        request.getResponse().addCookie(cookie);
    }

    public void setTokenFactory(TokenFactory tokenFactory) {
        this.tokenFactory = tokenFactory;
    }

    public void setUidPrefix(String uidPrefix) {
        this.uidPrefix = uidPrefix;
    }

    public void setCookieName(String cookieName) {
        this.cookieName = cookieName;
    }

    public void setCookieDomain(String cookieDomain) {
        this.cookieDomain = cookieDomain;
    }

    public void setCreateToken(boolean createToken) {
        this.createToken = createToken;
    }
}
