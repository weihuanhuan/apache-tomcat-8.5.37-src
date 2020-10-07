package com.bes.enterprise.webtier.authenticator.ltpa.utils;

import org.apache.catalina.connector.Request;
import org.apache.catalina.realm.GenericPrincipal;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import javax.servlet.http.Cookie;
import java.security.Principal;

public class TokenService {

    private static final String EMPTY_COOKIE_VALUE = "";

    protected Log log = LogFactory.getLog(TokenService.class);

    private TokenFactory tokenFactory;
    private boolean interoperability;

    private String uidPrefix;
    private String cookieDomain;

    private String cookieUser;
    private long cookieExpire;

    public String getCheckUserUid(Request request) {
        return getUserUid(request, false);
    }

    public String getLoginUserUid(Request request) {
        return getUserUid(request, true);
    }

    private String getUserUid(Request request, boolean update) {
        UserMetadata userMetaData = getUserMetaData(request);
        if (userMetaData == null) {
            return null;
        }

        if (update) {
            cookieUser = userMetaData.getUser();
            cookieExpire = userMetaData.getExpire();
        }
        return getUserUidValue(userMetaData.getUser(), uidPrefix);
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
            TokenFactory.LTPA_VERSION ltpaVersion = TokenFactory.LTPA_VERSION.getVersionByName(cookie.getName());
            UserMetadata userMetadata = tokenFactory.decodeLtpaToken(cookie.getValue(), ltpaVersion);
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

    private String getUserUidValue(String user, String uidPrefix) {
        String userUidValue = null;
        int indexOf = user.indexOf(uidPrefix + "=");
        if (indexOf != -1) {
            userUidValue = user.substring(indexOf + (uidPrefix + "=").length(), user.indexOf(",", indexOf));
        }
        if (userUidValue == null) {
            log.warn(String.format("Cannot not found user uid value from LtpaToken user:%s with prefix:%s", user, uidPrefix));
        }
        return userUidValue;
    }

    public void createLtpaCookie(Request request) {
        if (cookieUser == null) {
            if (log.isDebugEnabled()) {
                log.debug("We cannot to create ltpa token due to cookieUser non-exist!");
            }
            return;
        }

        String userUid = getUserUidValue(cookieUser, uidPrefix);
        if (userUid == null || !userUid.equals(getUsername(request.getPrincipal()))) {
            if (log.isDebugEnabled()) {
                log.debug("We cannot to create ltpa token due to cookieUser and username mismatch!");
            }
            return;
        }

        createLtpaCookie(request, cookieUser, cookieExpire, TokenFactory.LTPA_VERSION.LTPA2);
        if (!interoperability) {
            return;
        }

        createLtpaCookie(request, cookieUser, cookieExpire, TokenFactory.LTPA_VERSION.LTPA);
    }

    private void createLtpaCookie(Request request, String cookieUser, long cookieExpire, TokenFactory.LTPA_VERSION ltpaVersion) {
        try {
            UserMetadata userMetadata = createUserMetadata(ltpaVersion, cookieUser, cookieExpire);
            String cookieValue = tokenFactory.encodeLTPAToken(userMetadata);
            addLtpaToken(request, ltpaVersion.getCookieName(), cookieValue, (int) userMetadata.getExpire());
        } catch (Exception e) {
            log.warn(String.format("Failed to encode LtpaToken for user:%s with version:%s ", cookieUser, ltpaVersion), e);
            return;
        }
    }

    private UserMetadata createUserMetadata(TokenFactory.LTPA_VERSION ltpaVersion, String user, long expire) {
        //user\:FederatedRealm/uid=tomcat,ou=people,dc=ltpa,dc=com
        UserMetadata userMetadata = new UserMetadata();
        userMetadata.setUser(user);
        userMetadata.setExpire(expire);
        userMetadata.setLtpaVersion(ltpaVersion);
        return userMetadata;
    }

    public void cleanLtpaToken(Request request) {
        cleanLtpaToken(request, TokenFactory.LTPA_VERSION.LTPA2);
        if (!interoperability) {
            return;
        }

        cleanLtpaToken(request, TokenFactory.LTPA_VERSION.LTPA);
    }

    private void cleanLtpaToken(Request request, TokenFactory.LTPA_VERSION ltpaVersion) {
        addLtpaToken(request, ltpaVersion.getCookieName(), EMPTY_COOKIE_VALUE, 0);
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

    public void setUidPrefix(String uidPrefix) {
        this.uidPrefix = uidPrefix;
    }

    public void setCookieDomain(String cookieDomain) {
        this.cookieDomain = cookieDomain;
    }
}
