package com.bes.enterprise.webtier.authenticator.ltpa.valve;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.servlet.ServletException;

import com.bes.enterprise.webtier.authenticator.ltpa.utils.TokenService;
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

    private static final String DEFAULT_KEYS_FILE = "ltpa.keys";
    private static final String DEFAULT_UID_PREFIX = "uid";

    private Log log = LogFactory.getLog(LtpaConfigValve.class);

    private TokenFactory tokenFactory;

    private boolean useLtpa = true;
    private boolean useDelegate = true;
    private boolean createToken = false;
    private boolean interoperability = true;
    private boolean cleanTokenOnSessionInvalid = false;

    private String keysFile;
    private String keyPassword;

    private String uidPrefix;
    private String cookieDomain;

    @Override
    protected void initInternal() throws LifecycleException {
        super.initInternal();
        if (!useLtpa || !(container instanceof Context)) {
            return;
        }

        initLtpaKeyFactory();

        if (uidPrefix == null) {
            uidPrefix = DEFAULT_UID_PREFIX;
            log.info("No uidPrefix configured, so automatically use the application default value:" + uidPrefix);
        }

        if (cookieDomain == null) {
            String sessionCookieDomain = Context.class.cast(container).getSessionCookieDomain();
            cookieDomain = sessionCookieDomain == null ? "" : sessionCookieDomain;
            log.info("No cookieDomain configured, so automatically use the application default value:" + cookieDomain);
        }
    }

    private void initLtpaKeyFactory() {
        try {
            if (keysFile == null || keyPassword.isEmpty()) {
                keysFile = DEFAULT_KEYS_FILE;
                log.info("No keysFile configured, so automatically use the application default value:" + keysFile);
            }

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
        if (useDelegate && authenticator == null) {
            useDelegate = false;
            log.info("No authenticator configured, so automatically fix the useDelegate value to:" + useDelegate);
        }

        Valve ltpaAuthenticator = createLtpaAuthenticator(authenticator);
        if (authenticator instanceof Valve) {
            ltpaAuthenticator.setNext(((Valve) authenticator).getNext());
        }
        addLtpaAuthenValve(pipeline, ltpaAuthenticator);
    }

    private Valve createLtpaAuthenticator(Authenticator authenticator) {
        TokenService tokenService = new TokenService();
        tokenService.setTokenFactory(tokenFactory);
        tokenService.setUidPrefix(uidPrefix);
        tokenService.setCookieDomain(cookieDomain);
        tokenService.setInteroperability(interoperability);

        LtpaAuthenticator ltpaAuthenticator;
        if (!useDelegate) {
            ltpaAuthenticator = new LtpaAuthenticator();
        } else {
            ltpaAuthenticator = new LtpaDelegateAuthenticator();
            LtpaDelegateAuthenticator.class.cast(ltpaAuthenticator).setDelegate(authenticator);
            LtpaDelegateAuthenticator.class.cast(ltpaAuthenticator).setCreateToken(createToken);
        }
        ltpaAuthenticator.setTokenService(tokenService);
        ltpaAuthenticator.setCleanTokenOnSessionInvalid(cleanTokenOnSessionInvalid);
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

    public void setCreateToken(boolean createToken) {
        this.createToken = createToken;
    }

    public void setInteroperability(boolean interoperability) {
        this.interoperability = interoperability;
    }

    public void setCleanTokenOnSessionInvalid(boolean cleanTokenOnSessionInvalid) {
        this.cleanTokenOnSessionInvalid = cleanTokenOnSessionInvalid;
    }

    public void setKeysFile(String keysFile) {
        this.keysFile = keysFile;
    }

    public void setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
    }

    public void setUidPrefix(String uidPrefix) {
        this.uidPrefix = uidPrefix;
    }

    public void setCookieDomain(String cookieDomain) {
        this.cookieDomain = cookieDomain;
    }
}
