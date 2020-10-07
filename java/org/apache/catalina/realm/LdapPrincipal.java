package org.apache.catalina.realm;

import org.ietf.jgss.GSSCredential;

import javax.security.auth.login.LoginContext;
import java.io.Serializable;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;

public class LdapPrincipal extends GenericPrincipal {

    private static final long serialVersionUID = 1L;

    protected final String dn;

    public LdapPrincipal(String dn, String name, String password, List<String> roles) {
        this(dn, name, password, roles, null);
    }

    public LdapPrincipal(String dn, String name, String password, List<String> roles, Principal userPrincipal) {
        this(dn, name, password, roles, userPrincipal, null);
    }

    public LdapPrincipal(String dn, String name, String password, List<String> roles,
                         Principal userPrincipal, LoginContext loginContext) {
        this(dn, name, password, roles, userPrincipal, loginContext, null);
    }

    public LdapPrincipal(String dn, String name, String password, List<String> roles,
                         Principal userPrincipal, LoginContext loginContext,
                         GSSCredential gssCredential) {
        super(name, password, roles, userPrincipal, loginContext, gssCredential);
        this.dn = dn;
    }

    public String getDn() {
        return dn;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("LdapPrincipal[");
        sb.append(this.dn);
        sb.append("],");
        sb.append(super.toString());
        return sb.toString();
    }

    private Object writeReplace() {
        return new SerializableLdapPrincipal(dn, name, password, roles, userPrincipal);
    }

    private static class SerializableLdapPrincipal implements Serializable {

        private static final long serialVersionUID = 1L;

        private final String dn;
        private final String name;
        private final String password;
        private final String[] roles;
        private final Principal principal;

        public SerializableLdapPrincipal(String dn, String name, String password, String[] roles,
                                         Principal principal) {
            this.dn = dn;
            this.name = name;
            this.password = password;
            this.roles = roles;
            if (principal instanceof Serializable) {
                this.principal = principal;
            } else {
                this.principal = null;
            }
        }

        private Object readResolve() {
            return new LdapPrincipal(dn, name, password, Arrays.asList(roles), principal);
        }
    }
}
