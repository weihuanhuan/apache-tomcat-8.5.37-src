package com.bes.enterprise.webtier.authenticator.ltpa.utils;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.KeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.util.Arrays;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.tomcat.util.codec.binary.Base64;

/**
 * Utility class witch handles LTPA Tokens
 */
public class TokenFactory {

    // Base64 encoder & decoder
    private final Base64 decoder = new Base64();
    private final Base64 encoder = new Base64();

    private String sharedKey;
    private String keyPassword;
    private String privateKey;

    private String realm;
    private long expiration;

    private byte[] sharedSecretKey;
    private byte[][] privateRawKey;

    /**
     * LTPA Versions
     */
    public enum LTPA_VERSION {
        LTPA("LtpaToken"),
        LTPA2("LtpaToken2"),
        UNKNOWN("Unknown");

        private final String cookieName;

        LTPA_VERSION(String cookieName) {
            this.cookieName = cookieName;
        }

        public String getCookieName() {
            return this.cookieName;
        }

        @Override
        public String toString() {
            return this.cookieName;
        }

        public static LTPA_VERSION getVersionByName(String cookieName) {
            for (LTPA_VERSION ltpaVersion : LTPA_VERSION.values()) {
                if (ltpaVersion.getCookieName().equals(cookieName)) {
                    return ltpaVersion;
                }
            }
            return UNKNOWN;
        }
    }

    /**
     * Types of cripting algorithms
     */
    private enum CRIPTING_ALGORITHM {
        AES_DECRIPTING_ALGORITHM("AES/CBC/PKCS5Padding"),
        DES_DECRIPTING_ALGORITHM("DESede/ECB/PKCS5Padding");

        private final String text;

        CRIPTING_ALGORITHM(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return this.text;
        }
    }

    /**
     * LTPA Token Factory constructor
     * Give as parameter a filename of a Properties file containing the following
     * attributes
     * com.ibm.websphere.ltpa.KeyPassword
     * com.ibm.websphere.ltpa.PrivateKey
     * com.ibm.websphere.ltpa.PublicKey
     * com.ibm.websphere.ltpa.3DESKey
     *
     * @param propertiesFileName
     * @throws Exception
     */
    public TokenFactory(String propertiesFileName) throws Exception {
        Properties prop = new Properties();
        prop.load(new FileInputStream(new File(propertiesFileName)));
        this.keyPassword = prop.getProperty("com.ibm.websphere.ltpa.KeyPassword");
        this.privateKey = prop.getProperty("com.ibm.websphere.ltpa.PrivateKey");
        this.sharedKey = prop.getProperty("com.ibm.websphere.ltpa.3DESKey");
        this.realm = prop.getProperty("com.ibm.websphere.ltpa.Realm");
    }

    public void setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getRealm() {
        return realm;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }

    public boolean isValidKeys() throws Exception {
        if (keyPassword == null || keyPassword.isEmpty()) {
            throw new Exception("Invalid key password");
        }
        if (sharedKey == null || sharedKey.isEmpty()) {
            throw new Exception("Invalid shared key");
        }
        if (privateKey == null || privateKey.isEmpty()) {
            throw new Exception("Invalid private key");
        }

        // lets start by recovering the private key, which is encrypted
        sharedSecretKey = getSecretKey(this.sharedKey, this.keyPassword);
        byte[] privateSecretKey = getSecretKey(this.privateKey, this.keyPassword);
        LTPAPrivateKey ltpaPrivateKey = new LTPAPrivateKey(privateSecretKey);
        privateRawKey = ltpaPrivateKey.getRawKey();
        setRSAKey(privateRawKey);

        // lets encode and decode a token to check keys
        UserMetadata userMetadata = new UserMetadata();
        userMetadata.setUser("bes-dummy-user");
        userMetadata.setLtpaVersion(LTPA_VERSION.LTPA2);

        String encodeLTPAToken = encodeLTPAToken(userMetadata, userMetadata.getLtpaVersion());
        UserMetadata decodedUserMetadata = decodeLtpaToken(encodeLTPAToken, userMetadata.getLtpaVersion());
        if (decodedUserMetadata.equals(userMetadata)) {
            return true;
        }
        throw new Exception("Invalid ltpa keys");
    }

    public UserMetadata createUserMetadata(LTPA_VERSION ltpaVersion, String user) {
        long expirationInMilliseconds = System.currentTimeMillis() + expiration * 1000;

        UserMetadata userMetadata = new UserMetadata();
        userMetadata.setUser(user);
        userMetadata.setExpire(expirationInMilliseconds);
        userMetadata.setLtpaVersion(ltpaVersion);
        return userMetadata;
    }

    /**
     * Prepare an initialization vector for the cryptografic algorithm
     *
     * @param key  of the crypt algorithm
     * @param size of the vector
     * @return
     */
    private IvParameterSpec generateIvParameterSpec(byte key[], int size) {
        byte[] row = new byte[size];
        for (int i = 0; i < size; i++) {
            row[i] = key[i];
        } // for        
        return new IvParameterSpec(row);
    }

    /**
     * Helper function which do the hard work of encrypting and decrypting
     *
     * @param target    data to be [en/de]crypted
     * @param key       DES key
     * @param algorithm Algorithm used during the crypting process
     * @param mode      1 for decrypting and 2 for encrypting
     * @return dados [en/de]crypted data
     * @throws Exception
     */
    private byte[] crypt(byte[] target, byte[] key, CRIPTING_ALGORITHM algorithm, int mode) throws Exception {
        SecretKey sKey = null;

        if (algorithm.name().indexOf("AES") != -1) {
            sKey = new SecretKeySpec(key, 0, 16, "AES");
        } else {
            DESedeKeySpec kSpec = new DESedeKeySpec(key);
            SecretKeyFactory kFact = SecretKeyFactory.getInstance("DESede");
            sKey = kFact.generateSecret(kSpec);
        } // else
        Cipher cipher = Cipher.getInstance(algorithm.text);

        if (algorithm.name().indexOf("ECB") == -1) {
            if (algorithm.name().indexOf("AES") != -1) {
                IvParameterSpec ivs16 = generateIvParameterSpec(key, 16);
                cipher.init(mode, sKey, ivs16);
            } else {
                cipher.init(mode, sKey);
            } // else
        } else {
            cipher.init(mode, sKey);
        } // else
        return cipher.doFinal(target);
    }

    /**
     * Decrypt a given encoded key, using a DES algorithm
     *
     * @param key
     * @param keyPassword
     * @return
     * @throws Exception
     */
    private byte[] getSecretKey(String key, String keyPassword) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA");
        md.update(keyPassword.getBytes());
        byte[] hash3DES = new byte[24];
        System.arraycopy(md.digest(), 0, hash3DES, 0, 20);
        Arrays.fill(hash3DES, 20, 24, (byte) 0);
        final Cipher cipher = Cipher.getInstance(CRIPTING_ALGORITHM.DES_DECRIPTING_ALGORITHM.text);
        final KeySpec keySpec = new DESedeKeySpec(hash3DES);
        final Key secretKey = SecretKeyFactory.getInstance("DESede").generateSecret(keySpec);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] secret = cipher.doFinal(decoder.decode(key));
        return secret;
    }

    /**
     * Compute the final structure of the RSA rawKey
     *
     * @param rawKey
     */
    private void setRSAKey(byte rawKey[][]) {
        BigInteger abiginteger[] = new BigInteger[8];
        for (int i = 0; i < 8; i++) {
            if (rawKey[i] != null) {
                abiginteger[i] = new BigInteger(1, rawKey[i]);
            } // if
        } // for

        if (abiginteger[3].compareTo(abiginteger[4]) < 0) {
            BigInteger biginteger = abiginteger[3];
            abiginteger[3] = abiginteger[4];
            abiginteger[4] = biginteger;
            biginteger = abiginteger[5];
            abiginteger[5] = abiginteger[6];
            abiginteger[6] = biginteger;
            abiginteger[7] = null;
        } // if

        if (abiginteger[7] == null)
            abiginteger[7] = abiginteger[4].modInverse(abiginteger[3]);
        if (abiginteger[0] == null)
            abiginteger[0] = abiginteger[3].multiply(abiginteger[4]);
        if (abiginteger[1] == null)
            abiginteger[1] = abiginteger[2].modInverse(abiginteger[3].subtract(BigInteger.valueOf(1L)).multiply(abiginteger[4].subtract(BigInteger.valueOf(1L))));
        if (abiginteger[5] == null)
            abiginteger[5] = abiginteger[1].remainder(abiginteger[3].subtract(BigInteger.valueOf(1L)));
        if (abiginteger[6] == null)
            abiginteger[6] = abiginteger[1].remainder(abiginteger[4].subtract(BigInteger.valueOf(1L)));

        for (int j = 0; j < 8; j++) {
            rawKey[j] = abiginteger[j].toByteArray();
        } // for
    }

    /**
     * ISO Padding algorithm for RSA
     *
     * @param data
     * @param k
     * @return
     */
    private byte[] padISO9796(byte data[], int k) {
        byte padded[] = null;
        k--;
        if (data.length * 16 > k + 3) {
            return null;
        } // if
        padded = new byte[(k + 7) / 8];
        for (int l = 0; l < padded.length / 2; l++) {
            padded[padded.length - 1 - 2 * l] = data[data.length - 1 - l % data.length];
        } // for
        if ((padded.length & 1) != 0) {
            padded[0] = data[data.length - 1 - (padded.length / 2) % data.length];
        } // if
        long l1 = 0x1ca76bd0f249853eL;
        for (int i1 = 0; i1 < padded.length / 2; i1++) {
            int k1 = padded.length - 1 - 2 * i1;
            padded[k1 - 1] = (byte) (int) ((l1 >> (padded[k1] >>> 2 & 0x3c) & 15L) << 4 | l1 >> ((padded[k1] & 0xf) << 2) & 15L);
        } // for
        padded[padded.length - 2 * data.length] ^= 1;
        int j1 = k % 8;
        padded[0] &= (byte) ((1 << j1) - 1);
        padded[0] |= 1 << ((j1 - 1) + 8) % 8;
        padded[padded.length - 1] = (byte) (padded[padded.length - 1] << 4 | 6);
        return padded;
    }

    /**
     * Basic RSA encrypting algorithm
     *
     * @param rawKey
     * @param data
     * @return
     */
    private byte[] rsa(byte[][] rawKey, byte[] data) {
        byte encoded[] = null;
        int rawKeyLength = rawKey.length;
        BigInteger abiginteger[] = new BigInteger[rawKeyLength];
        int l;
        int k1;
        if (rawKeyLength == 8) {
            l = 3;
            k1 = rawKeyLength;
            abiginteger[0] = new BigInteger(rawKey[0]);
        } else {
            l = 0;
            k1 = 2;
        } // else

        do {
            abiginteger[l] = new BigInteger(rawKey[l]);
        } while (++l < k1);

        int l2 = l != 2 ? abiginteger[3].bitLength() + abiginteger[4].bitLength() : abiginteger[0].bitLength();
        int j2 = (l2 + 7) / 8;
        BigInteger biginteger;
        byte abyte4[];
        if ((abyte4 = padISO9796(data, l2)) == null) {
            return null;
        } // if
        biginteger = new BigInteger(1, abyte4);

        if (rawKeyLength > 3) {
            BigInteger biginteger1 = biginteger.remainder(abiginteger[3]).modPow(abiginteger[5], abiginteger[3]);
            BigInteger biginteger2 = biginteger.remainder(abiginteger[4]).modPow(abiginteger[6], abiginteger[4]);
            biginteger = biginteger1.add(abiginteger[3]).subtract(biginteger2).multiply(abiginteger[7]).remainder(abiginteger[3]).multiply(abiginteger[4]).add(biginteger2);
        } else {
            biginteger = biginteger.modPow(abiginteger[1], abiginteger[0]);
        } // else

        if (biginteger.multiply(BigInteger.valueOf(2L)).compareTo(abiginteger[0]) == 1) {
            biginteger = abiginteger[0].subtract(biginteger);
        } // if

        byte abyte5[] = biginteger.toByteArray();
        rawKeyLength = 0;
        l = abyte5.length;
        k1 = j2;
        if ((l -= k1) == 0) {
            return abyte5;
        } // if
        if (l < 0) {
            rawKeyLength = -l;
            l = 0;
        } // if
        encoded = new byte[k1];
        System.arraycopy(abyte5, l, encoded, rawKeyLength, k1 - rawKeyLength);
        return encoded;
    }

    /**
     * Decode a LTPA v1 or v2 token
     *
     * @param tokenLTPA
     * @param version
     * @return
     * @throws Exception
     */
    public UserMetadata decodeLtpaToken(String tokenLTPA, LTPA_VERSION version) throws Exception {
        // lets get the shared key
        byte[] sharedKey = sharedSecretKey;
        // and decode from base64 to bytes the given token
        byte[] encryptedBytes = decoder.decode(tokenLTPA);
        // to get the plain decrypted token after applying the decrypting algorithm
        String plainToken = new String(crypt(encryptedBytes, sharedKey, version.equals(LTPA_VERSION.LTPA2) ?
                CRIPTING_ALGORITHM.AES_DECRIPTING_ALGORITHM : CRIPTING_ALGORITHM.DES_DECRIPTING_ALGORITHM, Cipher.DECRYPT_MODE));
        // finally, lets parse the decrypted token into the user metadata
        return new UserMetadata(plainToken, version);
    }

    public String encodeLTPAToken(UserMetadata userMetadata) throws Exception {
        return encodeLTPAToken(userMetadata, userMetadata.getLtpaVersion());
    }

    /**
     * Encode a given usermetadata object into a LTPA Token v1 or v2
     *
     * @param userMetadata
     * @param version
     * @return
     * @throws Exception
     */
    public String encodeLTPAToken(UserMetadata userMetadata, LTPA_VERSION version) throws Exception {
        byte[][] rawKey = privateRawKey;

        // new lets prepare to prepare the signature
        MessageDigest md1JCE = MessageDigest.getInstance("SHA");
        byte[] plainUserDataBytes = md1JCE.digest(userMetadata.getPlainUserMetadata().getBytes());

        // lets sign the hash created previously with the private key
        byte[] encodedSignatureBytes = null;
        if (version.equals(LTPA_VERSION.LTPA2)) {
            BigInteger biginteger = new BigInteger(rawKey[0]);
            BigInteger biginteger1 = new BigInteger(rawKey[2]);
            BigInteger biginteger2 = new BigInteger(rawKey[3]);
            BigInteger biginteger3 = new BigInteger(rawKey[4]);
            BigInteger biginteger4 = biginteger1.modInverse(biginteger2.subtract(BigInteger.ONE).multiply(biginteger3.subtract(BigInteger.ONE)));
            KeyFactory keyfactory = KeyFactory.getInstance("RSA");

            RSAPrivateKeySpec rsaprivatekeyspec = new RSAPrivateKeySpec(biginteger, biginteger4);
            PrivateKey privatekey = keyfactory.generatePrivate(rsaprivatekeyspec);
            Signature signer = Signature.getInstance("SHA1withRSA");
            signer.initSign(privatekey);
            signer.update(plainUserDataBytes, 0, plainUserDataBytes.length);

            // signing the hash
            encodedSignatureBytes = signer.sign();
        } else {
            // signing the hash
            encodedSignatureBytes = rsa(rawKey, plainUserDataBytes);
        } // else

        // ok. lets encode the signature with Base64
        String base64Signature = encoder.encodeAsString(encodedSignatureBytes).replaceAll("[\r\n]", "");

        // now, lets create the plain text version of the token
        StringBuffer token = new StringBuffer();
        token.append(userMetadata.getPlainUserMetadata()).append("%");
        token.append(userMetadata.getExpire()).append("%");
        token.append(base64Signature);

        // finally lets crypt everything with the private key and then
        // to apply a base64 encoding
        byte[] tokenBytes = token.toString().getBytes("UTF8");
        byte[] encryptedBytes = crypt(tokenBytes, sharedSecretKey, version.equals(LTPA_VERSION.LTPA2) ?
                CRIPTING_ALGORITHM.AES_DECRIPTING_ALGORITHM : CRIPTING_ALGORITHM.DES_DECRIPTING_ALGORITHM, Cipher.ENCRYPT_MODE);
        return encoder.encodeAsString(encryptedBytes).replaceAll("[\r\n]", "");
    }

}
