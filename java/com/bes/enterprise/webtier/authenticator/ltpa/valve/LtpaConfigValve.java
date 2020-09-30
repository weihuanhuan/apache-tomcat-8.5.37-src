package com.bes.enterprise.webtier.authenticator.ltpa.valve;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.servlet.ServletException;

import org.apache.catalina.Authenticator;
import org.apache.catalina.Contained;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import com.bes.enterprise.webtier.authenticator.ltpa.utils.TokenFactory;

public class LtpaConfigValve extends ValveBase {

    private static final Log log = LogFactory.getLog(LtpaConfigValve.class);

    private TokenFactory tokenFactory;

    private boolean useLtpa = true;
    private boolean useDelegate = true;

    private String keyPassword;
    private String keysFile = "ltpa.keys";

    private String uidPrefix = "uid";
    private String cookieName = "LtpaToken2";
    private String cookieDomain;

    private boolean createToken = false;

    @Override
    protected void initInternal() throws LifecycleException {
        super.initInternal();
        if (!useLtpa || !(container instanceof Context)) {
            return;
        }
        initLtpaKeyUtils();
    }

    private void initLtpaKeyUtils() {
        try {
            Path keysFilePath;
            if (!Paths.get(keysFile).isAbsolute()) {
                keysFilePath = Paths.get(System.getProperty("catalina.home"), "conf", keysFile);
            } else {
                keysFilePath = Paths.get(keysFile);
            }

            tokenFactory = new TokenFactory(keysFilePath.toAbsolutePath().toString());
            if (keyPassword != null && !keyPassword.isEmpty()) {
                tokenFactory.setKeyPassword(keyPassword);
            }
            tokenFactory.isValid();
        } catch (Exception e) {
            useLtpa = false;
            log.error("Failed to config ltpa token keys, so we do not use ltpa token for authentication!", e);
        }
    }

    @Override
    protected synchronized void startInternal() throws LifecycleException {
        super.startInternal();
        if (!useLtpa || !(container instanceof Context)) {
            return;
        }
        configLtpaAuthenValve((Context) container);
    }

    private void configLtpaAuthenValve(Context context) {
        Pipeline pipeline = context.getPipeline();
        if (pipeline == null) {
            return;
        }

        Authenticator authenticator = context.getAuthenticator();
        if (authenticator == null) {
            return;
        }

        Valve ltpaAuthenticator = createLtpaAuthenticator(authenticator);
        if (authenticator instanceof Valve) {
            ltpaAuthenticator.setNext(((Valve) authenticator).getNext());
        }
        addLtpaAuthenValve(pipeline, ltpaAuthenticator);
    }

    private Valve createLtpaAuthenticator(Authenticator authenticator) {
        LtpaAuthenticator ltpaAuthenticator;
        if (useDelegate) {
            ltpaAuthenticator = new LtpaDelegateAuthenticator();
            ((LtpaDelegateAuthenticator) ltpaAuthenticator).setDelegate(authenticator);
        } else {
            ltpaAuthenticator = new LtpaAuthenticator();
        }

        ltpaAuthenticator.setTokenFactory(tokenFactory);
        ltpaAuthenticator.setUidPrefix(uidPrefix);
        ltpaAuthenticator.setCookieName(cookieName);
        ltpaAuthenticator.setCookieDomain(cookieDomain);
        ltpaAuthenticator.setCreateToken(createToken);
        return ltpaAuthenticator;
    }

    private void addLtpaAuthenValve(Pipeline pipeline, Valve ltpaAuthenticator) {
        Container container = pipeline.getContainer();
        if (ltpaAuthenticator instanceof Contained) {
            Contained.class.cast(ltpaAuthenticator).setContainer(container);
        }

        if (pipeline instanceof Lifecycle && ltpaAuthenticator instanceof Lifecycle
                && Lifecycle.class.cast(pipeline).getState().isAvailable()) {
            try {
                Lifecycle.class.cast(ltpaAuthenticator).start();
            } catch (LifecycleException e) {
                log.error("Failed to start ltpa authenticator!", e);
            }
        }

        setNext(ltpaAuthenticator);
        container.fireContainerEvent(Container.ADD_VALVE_EVENT, ltpaAuthenticator);
    }

    public void invoke(Request request, Response response) throws IOException, ServletException {
        getNext().invoke(request, response);
        if (container instanceof Context) {
            removeLtpaConfigValve((Context) container);
        }
    }

    private void removeLtpaConfigValve(Context container) {
        Pipeline pipeline = container.getPipeline();
        if (pipeline == null) {
            return;
        }
        pipeline.removeValve(this);
    }

    public void setUseLtpa(boolean useLtpa) {
        this.useLtpa = useLtpa;
    }

    public void setUseDelegate(boolean useDelegate) {
        this.useDelegate = useDelegate;
    }

    public void setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
    }

    public void setKeysFile(String keysFile) {
        this.keysFile = keysFile;
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
