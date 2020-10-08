package com.bes.enterprise.webtier.authenticator.ltpa.utils;

public class TokenFieldHelper {

    private static final char TOKEN_DELIM = '%';
    private static final char USER_DATA_DELIM = '$';
    private static final char USER_ATTRIB_DELIM = ':';

    /*
     * Remove the delimination of the String form.
     *
     * @param str The String form
     */
    public static final String escape(String str) {
        if (str == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder(str.length() * 2);
        int len = str.length();
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            switch (c) {
                case TOKEN_DELIM:
                case USER_DATA_DELIM:
                case USER_ATTRIB_DELIM:
                    sb.append('\\');
                    break;
                default:
                    break;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /*
     * Add the delimination to the String form.
     *
     * @param str The String form
     */
    public static final String unescape(String str) {
        StringBuilder sb = new StringBuilder(str.length() * 2);
        int len = str.length();
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            if ((c == '\\') && (i < len - 1)) {
                char d = str.charAt(i + 1);
                if (!((d == USER_DATA_DELIM) || (d == USER_ATTRIB_DELIM) || (d == TOKEN_DELIM))) {
                    // if next char is delim, skip this escape char
                    sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

}

