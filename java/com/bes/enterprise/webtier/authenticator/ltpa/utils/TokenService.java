package com.bes.enterprise.webtier.authenticator.ltpa.utils;

import org.apache.catalina.connector.Request;
import org.apache.catalina.realm.GenericPrincipal;
import org.apache.catalina.realm.LdapPrincipal;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import javax.servlet.http.Cookie;
import java.security.Principal;

public class TokenService {

    private static final String EMPTY_COOKIE_VALUE = "";

    protected Log log = LogFactory.getLog(TokenService.class);

    private TokenFactory tokenFactory;
    private boolean interoperability;

    private String realm;
    private String uidPrefix;
    private String cookieDomain;
    private long expiration;

    public String getUserUid(Request request) {
        UserMetadata userMetaData = getUserMetaData(request);
        if (userMetaData == null) {
            return null;
        }

        String user = TokenFieldHelper.unescape(userMetaData.getUser());
        return getUserUidValue(user);
    }

    private UserMetadata getUserMetaData(Request request) {
        Cookie cookie = getLtpaTokenCookie(request);
        if (cookie == null) {
            if (log.isDebugEnabled()) {
                log.debug("LtpaToken cookie not found!");
            }
            return null;
        }

        try {
            TokenFactory.LTPA_VERSION version = TokenFactory.LTPA_VERSION.getVersionByName(cookie.getName());
            UserMetadata userMetadata = tokenFactory.decodeLtpaToken(cookie.getValue(), version);
            return userMetadata;
        } catch (Exception e) {
            log.warn(String.format("LtpaToken decode failed with cookie:%s=%s !", cookie.getName(), cookie.getValue()), e);
            return null;
        }
    }

    private Cookie getLtpaTokenCookie(Request request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return null;
        }

        Cookie cookie = getCookie(cookies, TokenFactory.LTPA_VERSION.LTPA2.getCookieName());
        if (interoperability && cookie == null) {
            cookie = getCookie(cookies, TokenFactory.LTPA_VERSION.LTPA.getCookieName());
        }
        return cookie;
    }

    private Cookie getCookie(Cookie[] cookies, String cookieName) {
        for (int i = 0; i < cookies.length; i++) {
            if (cookies[i].getName().equals(cookieName)) {
                return cookies[i];
            }
        }
        return null;
    }

    private String getUserUidValue(String user) {
        String userUidValue = null;
        int indexOf = user.indexOf(uidPrefix + "=");
        if (indexOf != -1) {
            userUidValue = user.substring(indexOf + (uidPrefix + "=").length(), user.indexOf(",", indexOf));
        }
        if (userUidValue == null) {
            log.warn(String.format("Cannot not found user uid value from ltpa user:%s with prefix:%s", user, uidPrefix));
        }
        return userUidValue;
    }

    public void createLtpaCookie(Request request) {
        String userDn = getUserDn(request.getPrincipal());
        if (userDn == null || !userDn.startsWith(uidPrefix)) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("We cannot to create ltpa token due to userDn:%s and uidPrefix:%s mismatch!", userDn, uidPrefix));
            }
            return;
        }

        String user = generateLtpaUser(userDn);
        createLtpaCookie(request, user, TokenFactory.LTPA_VERSION.LTPA2);
        if (!interoperability) {
            return;
        }

        createLtpaCookie(request, user, TokenFactory.LTPA_VERSION.LTPA);
    }

    private String generateLtpaUser(String userDn) {
        String tokenRealm = TokenFieldHelper.escape(realm);
        if (tokenRealm == null || tokenRealm.isEmpty()) {
            tokenRealm = tokenFactory.getRealm();
        }

        //user\:FederatedRealm/uid=tomcat,ou=people,dc=ltpa,dc=com
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("user");
        stringBuffer.append(TokenFieldHelper.escape(":"));
        stringBuffer.append(tokenRealm);
        stringBuffer.append("/");
        stringBuffer.append(TokenFieldHelper.escape(userDn));
        return stringBuffer.toString();
    }

    private void createLtpaCookie(Request request, String user, TokenFactory.LTPA_VERSION version) {
        try {
            UserMetadata userMetadata = tokenFactory.createUserMetadata(version, expiration, user);
            String cookieValue = tokenFactory.encodeLTPAToken(userMetadata);
            addLtpaToken(request, version.getCookieName(), cookieValue, -1);
        } catch (Exception e) {
            log.warn(String.format("Failed to encode LtpaToken for user:%s with version:%s ", user, version), e);
            return;
        }
    }

    public void cleanLtpaToken(Request request) {
        cleanLtpaToken(request, TokenFactory.LTPA_VERSION.LTPA2);
        if (!interoperability) {
            return;
        }

        cleanLtpaToken(request, TokenFactory.LTPA_VERSION.LTPA);
    }

    private void cleanLtpaToken(Request request, TokenFactory.LTPA_VERSION version) {
        addLtpaToken(request, version.getCookieName(), EMPTY_COOKIE_VALUE, 0);
    }

    private void addLtpaToken(Request request, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(maxAge);
        cookie.setPath("/");
        cookie.setSecure(request.isSecure());
        cookie.setDomain(cookieDomain);
        if (request.getServletContext().getSessionCookieConfig().isHttpOnly() || request.getContext().getUseHttpOnly()) {
            cookie.setHttpOnly(true);
        }
        request.getResponse().addCookie(cookie);
    }

    public String getUserDn(Principal principal) {
        if (!(principal instanceof LdapPrincipal)) {
            return null;
        }
        return LdapPrincipal.class.cast(principal).getDn();
    }

    public String getUsername(Principal principal) {
        if (principal == null) {
            return null;
        }
        return principal.getName();
    }

    public String getPassword(Principal principal) {
        if (!(principal instanceof GenericPrincipal)) {
            return null;
        }
        return ((GenericPrincipal) principal).getPassword();
    }

    public void setTokenFactory(TokenFactory tokenFactory) {
        this.tokenFactory = tokenFactory;
    }

    public void setInteroperability(boolean interoperability) {
        this.interoperability = interoperability;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public void setUidPrefix(String uidPrefix) {
        this.uidPrefix = uidPrefix;
    }

    public void setCookieDomain(String cookieDomain) {
        this.cookieDomain = cookieDomain;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }
}
