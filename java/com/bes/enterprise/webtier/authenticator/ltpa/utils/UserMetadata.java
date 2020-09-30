package com.bes.enterprise.webtier.authenticator.ltpa.utils;

import org.apache.tomcat.util.codec.binary.Base64;

/**
 * User Metadata, used to build the LTPA Token
 */
public class UserMetadata {
    private long expire = 0l;
    // u
    private String user = null;
    private String host = null;
    private int port = 0;
    // java.naming.provider.url
    private String namingProvider = null;
    // process.serverName
    private String serverName = null;
    // security.authMechOID
    private String authenticationMethod = null;
    private String type = null;
    private byte[] signature = null;
    private TokenFactory.LTPA_VERSION ltpaVersion = TokenFactory.LTPA_VERSION.LTPA2;

    private final Base64 decoder = new Base64();

    public UserMetadata() {
    }

    /**
     * Basic constructor
     *
     * @param plainToken
     * @param ltpaVersion
     * @throws Exception
     */
    public UserMetadata(String plainToken, TokenFactory.LTPA_VERSION ltpaVersion) throws Exception {
        this.ltpaVersion = ltpaVersion;

        String[] parts = plainToken.split("\\%", 2);
        this.expire = Long.parseLong(parts[1].split("\\%")[0]);
        String[] tokens = parts[0].split("\\$");

        for (int i = 0; i < tokens.length; ++i) {
            String nameValue[] = tokens[i].split(":", 2);
            if ("expire".equals(nameValue[0])) {
                this.expire = Long.parseLong(nameValue[1]);
            } else if ("host".equals(nameValue[0])) {
                this.host = nameValue[1];
            } else if ("java.naming.provider.url".equals(nameValue[0])) {
                this.namingProvider = nameValue[1];
            } else if ("port".equals(nameValue[0])) {
                this.port = Integer.parseInt(nameValue[1]);
            } else if ("process.serverName".equals(nameValue[0])) {
                this.serverName = nameValue[1];
            } else if ("security.authMechOID".equals(nameValue[0])) {
                this.authenticationMethod = nameValue[1];
            } else if ("type".equals(nameValue[0])) {
                this.type = nameValue[1];
            } else if ("u".equals(nameValue[0])) {
                this.user = nameValue[1];
            } // else if
        } // for
        this.signature = decoder.decode(plainToken.split("%")[2]);
    }

    /**
     * Encode the user data to a plain LTPA token string
     *
     * @return
     */
    public String getPlainUserMetadata() {
        StringBuilder str = new StringBuilder();
        if (ltpaVersion.equals(TokenFactory.LTPA_VERSION.LTPA2)) {
            str.append("expire:").append(this.expire).append("$");
        } //
        str.append("u:").append(this.user);
        if (this.host != null) {
            str.append("$").append("host:").append(this.host);
        } // if
        if (this.namingProvider != null) {
            str.append("$").append("java.naming.provider.url:").append(this.namingProvider);
        } // if
        if (this.port > 0) {
            str.append("$").append("port:").append(this.port);
        } // if
        if (this.serverName != null) {
            str.append("$").append("process.serverName:").append(this.serverName);
        } // if
        if (this.authenticationMethod != null) {
            str.append("$").append("security.authMechOID:").append(this.authenticationMethod);
        } // if
        if (this.type != null) {
            str.append("$").append("type:").append(this.type);
        } // if
        return str.toString();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuilder str = new StringBuilder(getPlainUserMetadata());
        Base64 encoder = new Base64();
        str.append("%").append(this.expire).append("%");
        str.append(encoder.encodeAsString(this.signature).replaceAll("[\r\n]", ""));
        return str.toString();
    }

    /**
     * @return the expire
     */
    public long getExpire() {
        return expire;
    }

    /**
     * @param expire the expire to set
     */
    public void setExpire(long expire) {
        this.expire = expire;
    }

    /**
     * @return the user
     */
    public String getUser() {
        return user;
    }

    /**
     * @param user the user to set
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * @param host the host to set
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * @return the namingProvider
     */
    public String getNamingProvider() {
        return namingProvider;
    }

    /**
     * @param namingProvider the namingProvider to set
     */
    public void setNamingProvider(String namingProvider) {
        this.namingProvider = namingProvider;
    }

    /**
     * @return the serverName
     */
    public String getServerName() {
        return serverName;
    }

    /**
     * @param serverName the serverName to set
     */
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    /**
     * @return the authenticationMethod
     */
    public String getAuthenticationMethod() {
        return authenticationMethod;
    }

    /**
     * @param authenticationMethod the authenticationMethod to set
     */
    public void setAuthenticationMethod(String authenticationMethod) {
        this.authenticationMethod = authenticationMethod;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the signature
     */
    public byte[] getSignature() {
        return signature.clone();
    }

    /**
     * @param signature the signature to set
     */
    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    /**
     * @return the ltpaVersion
     */
    public TokenFactory.LTPA_VERSION getLtpaVersion() {
        return ltpaVersion;
    }

    /**
     * @param ltpaVersion the ltpaVersion to set
     */
    public void setLtpaVersion(TokenFactory.LTPA_VERSION ltpaVersion) {
        this.ltpaVersion = ltpaVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserMetadata)) return false;

        UserMetadata that = (UserMetadata) o;

        if (expire != that.expire) return false;
        if (port != that.port) return false;
        if (user != null ? !user.equals(that.user) : that.user != null) return false;
        if (host != null ? !host.equals(that.host) : that.host != null) return false;
        if (namingProvider != null ? !namingProvider.equals(that.namingProvider) : that.namingProvider != null)
            return false;
        if (serverName != null ? !serverName.equals(that.serverName) : that.serverName != null) return false;
        if (authenticationMethod != null ? !authenticationMethod.equals(that.authenticationMethod) : that.authenticationMethod != null)
            return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;
//        if (!Arrays.equals(signature, that.signature)) return false;
        return ltpaVersion == that.ltpaVersion;
    }

    @Override
    public int hashCode() {
        int result = (int) (expire ^ (expire >>> 32));
        result = 31 * result + (user != null ? user.hashCode() : 0);
        result = 31 * result + (host != null ? host.hashCode() : 0);
        result = 31 * result + port;
        result = 31 * result + (namingProvider != null ? namingProvider.hashCode() : 0);
        result = 31 * result + (serverName != null ? serverName.hashCode() : 0);
        result = 31 * result + (authenticationMethod != null ? authenticationMethod.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
//        result = 31 * result + Arrays.hashCode(signature);
        result = 31 * result + (ltpaVersion != null ? ltpaVersion.hashCode() : 0);
        return result;
    }
}

