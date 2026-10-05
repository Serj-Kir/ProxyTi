package proxyti.net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * The cryptography Minecraft uses during login: a 1024 bit RSA key exchange for
 * a shared AES secret, plus the SHA-1 "server hash" used when talking to the
 * Mojang session server.
 */
public final class Crypto {
    private static final SecureRandom RANDOM = new SecureRandom();

    private Crypto() {
    }

    public static KeyPair generateServerKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(1024, RANDOM);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to generate RSA key pair", e);
        }
    }

    /** DER/X.509 encoded public key, as expected in the Encryption Request packet. */
    public static byte[] encodePublicKey(PublicKey key) {
        return key.getEncoded();
    }

    public static PublicKey decodePublicKey(byte[] der) throws IOException {
        try {
            return java.security.KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IOException("Invalid public key", e);
        }
    }

    public static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    /** Decrypts the shared secret sent by the client with RSA/PKCS#1 v1.5. */
    public static byte[] decryptRsa(PrivateKey privateKey, byte[] encrypted) throws IOException {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return cipher.doFinal(encrypted);
        } catch (Exception e) {
            throw new IOException("Unable to decrypt shared secret", e);
        }
    }

    public static Cipher aesCipher(int mode, byte[] secret) throws IOException {
        try {
            SecretKeySpec key = new SecretKeySpec(secret, "AES");
            IvParameterSpec iv = new IvParameterSpec(secret);
            Cipher cipher = Cipher.getInstance("AES/CFB8/NoPadding");
            cipher.init(mode, key, iv);
            return cipher;
        } catch (Exception e) {
            throw new IOException("Unable to initialise AES cipher", e);
        }
    }

    /**
     * The non-standard "server hash" Minecraft uses for authentication: SHA-1
     * over the server id, the shared secret and the public key, rendered as a
     * signed two's-complement hex string.
     */
    public static String serverHash(String serverId, byte[] sharedSecret, byte[] publicKey) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(serverId.getBytes(StandardCharsets.ISO_8859_1));
            digest.update(sharedSecret);
            digest.update(publicKey);
            byte[] hash = digest.digest();
            // This intentionally has no zero-padding: Mojang's protocol uses
            // BigInteger's signed hexadecimal representation verbatim.
            return new BigInteger(hash).toString(16);
        } catch (Exception e) {
            throw new IOException("Unable to compute server hash", e);
        }
    }

    public static byte[] hmacSha256(byte[] key, byte[] data) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to compute HMAC", e);
        }
    }

    public static byte[] concat(byte[]... arrays) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] array : arrays) {
            out.write(array, 0, array.length);
        }
        return out.toByteArray();
    }

    public static byte[] base64Decode(String value) {
        return Base64.getDecoder().decode(value);
    }
}
