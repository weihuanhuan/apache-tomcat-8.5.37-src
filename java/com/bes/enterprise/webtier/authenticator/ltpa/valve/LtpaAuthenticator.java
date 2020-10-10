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

        String ltpaUserUid = tokenService.getUserUid(request);
        if (ltpaUserUid == null || ltpaUserUid.isEmpty()) {
            //当存在本应用的缓冲认证记录，却没有对应的 ltpa token cookie 存在时判定登录状态为无效的状态，此时需要重新执行登录。
            //为了防止其后的 authenticator 仍旧从 cache 中获取认证信息，进而错误的认为此时应用处于登录状态，所以我们要清除认证状态

            //但是，当进行真实的表单登录时，其存在 login 阶段和 restore 原始请求的恢复阶段，这俩个阶段处理并都会触发 context valve 的调用。
            //所以这里再清除登录状态的时候要区分这种情况，防止 ltpa 意外的清除正常 FORM 登录过程中 login 阶段设置的登录信息，使得 restore 阶段无法进行。
            //最终导致 FORM 认证无法正确的处理。
            //新版 tomcat 会有上面问题，因为其 FROM 认证的 login 阶段会对 principal 进行 register 处理。
            //参考
            //org.apache.catalina.authenticator.FormAuthenticator.doAuthenticate
            //  login 转向 restore 阶段
            //  org.apache.catalina.connector.Response.sendRedirect(java.lang.String, int)
            //  确认 restore 阶段的 uri 并执行 restore，这里避免清除的登录状态就是 matchRequest 时需要的。
            //  org.apache.catalina.authenticator.FormAuthenticator.matchRequest
            //  org.apache.catalina.authenticator.FormAuthenticator.restoreRequest

            //由于我们的 ltpa 认证实现不论原始认证是什么，我们自身都会进行主动 register ，其会刷新认证方法为 ltps 的方法，
            //因此我们可以通过 Request.getAuthType 来避免对其他原始 authenticator 的认证过程干扰。
            if (getAuthMethod().equals(request.getAuthType())) {
                register(request, response, null, null, null, null);
            }
            return false;
        }

        String username = tokenService.getUsername(request.getPrincipal());
        boolean match = ltpaUserUid.equals(username);
        if (!match && getAuthMethod().equals(request.getAuthType())) {
            register(request, response, null, null, null, null);
        }
        return match;
    }

    private Principal getPrincipal(Request request) {
        String userUid = tokenService.getUserUid(request);
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
            return principal;
        }
        if (log.isDebugEnabled()) {
            log.debug("Ltpa token cookie not exist or not match with user provided info!");
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
