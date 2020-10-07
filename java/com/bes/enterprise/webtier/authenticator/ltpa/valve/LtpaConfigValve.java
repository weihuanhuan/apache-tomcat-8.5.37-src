package com.bes.enterprise.webtier.authenticator.ltpa.valve;

import com.bes.enterprise.webtier.authenticator.ltpa.utils.TokenFactory;
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

import javax.servlet.ServletException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LtpaConfigValve extends ValveBase {

    private static final String DEFAULT_KEYS_FILE = "ltpa.keys";
    private static final String DEFAULT_UID_PREFIX = "uid";
    private static final String DEFAULT_EXPIRATION = "7200";

    private Log log = LogFactory.getLog(LtpaConfigValve.class);

    private TokenFactory tokenFactory;

    private boolean useLtpa = true;
    private String keysFile;
    private String keyPassword;
    private String uidPrefix;
    private String cookieDomain;

    private boolean createToken = true;
    private String realm;
    private String expiration;

    private boolean useDelegate = true;
    private boolean interoperability = true;
    private boolean cleanTokenOnSessionInvalid = false;

    @Override
    protected void initInternal() throws LifecycleException {
        super.initInternal();
        if (!useLtpa || !(container instanceof Context)) {
            return;
        }

        if (keysFile == null || keyPassword.isEmpty()) {
            keysFile = DEFAULT_KEYS_FILE;
            log.info("No keysFile configured, so automatically use the application default value:" + keysFile);
        }

        if (uidPrefix == null) {
            uidPrefix = DEFAULT_UID_PREFIX;
            log.info("No uidPrefix configured, so automatically use the application default value:" + uidPrefix);
        }

        if (cookieDomain == null) {
            String sessionCookieDomain = Context.class.cast(container).getSessionCookieDomain();
            cookieDomain = sessionCookieDomain == null ? "" : sessionCookieDomain;
            log.info("No cookieDomain configured, so automatically use the application default value:" + cookieDomain);
        }

        try {
            Long.parseLong(expiration);
        } catch (Exception ignored) {
            expiration = DEFAULT_EXPIRATION;
            log.info("Invalid expiration configured, so automatically use the application default value:" + expiration);
        }

        initTokenFactory();
    }

    private void initTokenFactory() {
        try {
            Path keysFilePath;
            if (!Paths.get(keysFile).isAbsolute()) {
                keysFilePath = Paths.get(System.getProperty("catalina.home"), "conf", keysFile);
            } else {
                keysFilePath = Paths.get(keysFile);
            }

            tokenFactory = new TokenFactory(keysFilePath.toAbsolutePath().toString());
            tokenFactory.setExpiration(Long.parseLong(expiration));

            if (keyPassword != null && !keyPassword.isEmpty()) {
                tokenFactory.setKeyPassword(keyPassword);
            }

            if (realm != null && !realm.isEmpty()) {
                tokenFactory.setRealm(realm);
            }

            tokenFactory.isValidKeys();
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
        tokenService.setInteroperability(interoperability);
        tokenService.setUidPrefix(uidPrefix);
        tokenService.setCookieDomain(cookieDomain);

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

    public void setCreateToken(boolean createToken) {
        this.createToken = createToken;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public void setExpiration(String expiration) {
        this.expiration = expiration;
    }

    public void setUseDelegate(boolean useDelegate) {
        this.useDelegate = useDelegate;
    }

    public void setInteroperability(boolean interoperability) {
        this.interoperability = interoperability;
    }

    public void setCleanTokenOnSessionInvalid(boolean cleanTokenOnSessionInvalid) {
        this.cleanTokenOnSessionInvalid = cleanTokenOnSessionInvalid;
    }
}
