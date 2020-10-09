package com.bes.enterprise.webtier.authenticator.ltpa.valve;

import java.io.IOException;
import java.security.Principal;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Authenticator;
import org.apache.catalina.Contained;
import org.apache.catalina.Container;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.authenticator.AuthenticatorBase;
import org.apache.catalina.connector.Request;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

public class LtpaDelegateAuthenticator extends LtpaAuthenticator {

    protected Log log = LogFactory.getLog(LtpaDelegateAuthenticator.class);

    private Authenticator delegate;

    private boolean createToken;

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

    //这个方法属于 AuthenticatorBase.isContinuationRequired ,其会在 AuthenticatorBase.invoke 中进行调用。
    //他的作用是用来判断那些两阶段认证（FORM）或者是特定的URI（j_security_check）是否处于一个认证的流程中，
    //这样子对于这些特殊的认证逻辑 AuthenticatorBase 可以识别出他们使其正常的进行认证的处理，否则可能导致认证失败。
    //具体参考
    //org.apache.catalina.authenticator.AuthenticatorBase.invoke
    //  判断是否当前请求处于特殊的认证流程中（form 等）
    //  org.apache.catalina.authenticator.AuthenticatorBase.isContinuationRequired
    //  如果当前请求不需要认证那么直接调用下一个 valve。
    //  org.apache.catalina.Valve.invoke
    //  识别出当前正处于需要认证的请求中由 AuthenticatorBase 发起实际的认证逻辑。
    //  另外这个识别不只会考虑上面而谈到的特殊情况，也会考虑当前请求的 url 是否符合应用配置的 <security-constraint>
    //  假若当前请求的 url 并不符合上面的 特殊情况，当时他符合了 <security-constraint> ，其依旧会进入认证逻辑的处理。
    //  org.apache.catalina.authenticator.AuthenticatorBase.doAuthenticate

    //比如在 JavaEE 标准的 login form 中，其认证的 url 是由规范中特定的 【j_security_check】，
    //当遇见这个请求的 url 时，就表明对于此次请求的处理， authenticator 需要进行认证处理，
    //如果我们忽略这个特定的 url，那么他将被当作一个普通的 url 来给予响应，而一般应用确实不存在这个 url 的映射，
    //因此 tomcat 就像普通的未能定位的 url 那样给 browser 返回一个 404 来标识资源不存在。
    //此时不仅认证没有正确的处理，而且还导致了页面访问到了专为 FORM 认证而虚拟的地址，并产生错误的 404 响应。
    @Override
    public boolean isContinuationRequired(Request request) {
        if (!(delegate instanceof AuthenticatorBase)) {
            return super.isContinuationRequired(request);
        }

        return AuthenticatorBase.class.cast(delegate).isContinuationRequired(request);
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
            Principal principal = request.getPrincipal();
            String username = tokenService.getUsername(principal);
            String password = tokenService.getPassword(principal);
            register(request, response, principal, getAuthMethod(), username, password);
            createLtpaCookie(request);
        }
        return authenticate;
    }

    @Override
    protected String getAuthMethod() {
        return "LTPAWithDelegate";
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
            register(request, request.getResponse(), request.getPrincipal(), getAuthMethod(), username, password);
            createLtpaCookie(request);
        }
    }

    private void createLtpaCookie(Request request) {
        if (!createToken) {
            if (log.isDebugEnabled()) {
                log.debug("Success to authentication with ltap delegate authenticator, but we don't create ltpa token!");
            }
            return;
        }

        tokenService.createLtpaCookie(request);
    }

    @Override
    public void logout(Request request) {
        super.logout(request);
        delegate.logout(request);
    }

    public void setDelegate(Authenticator delegate) {
        this.delegate = delegate;
    }

    public void setCreateToken(boolean createToken) {
        this.createToken = createToken;
    }
}
