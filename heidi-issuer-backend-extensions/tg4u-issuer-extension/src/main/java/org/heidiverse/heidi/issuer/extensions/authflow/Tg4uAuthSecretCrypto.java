package org.heidiverse.heidi.issuer.extensions.authflow;

import org.heidiverse.heidi.issuer.service.SessionEncryptionProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Reuses the issuer session master key to keep provider secrets encrypted at rest. */
@Component
public class Tg4uAuthSecretCrypto {
    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public Tg4uAuthSecretCrypto(SessionEncryptionProperties properties) {
        byte[] bytes = HexFormat.of().parseHex(properties.getEncryptionMasterKey());
        if (bytes.length != 32) {
            throw new IllegalArgumentException("issuer session encryption key must contain 32 bytes");
        }
        key = new SecretKeySpec(bytes, "AES");
    }

    public String encrypt(String value) {
        try {
            byte[] iv = new byte[12];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    ivAndCiphertext(iv, cipher.doFinal(value.getBytes(StandardCharsets.UTF_8))));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not encrypt TG4U auth-flow secret", exception);
        }
    }

    public String decrypt(String value) {
        try {
            byte[] encoded = Base64.getUrlDecoder().decode(value);
            if (encoded.length <= 12) throw new IllegalArgumentException("Invalid encrypted secret");
            byte[] iv = java.util.Arrays.copyOfRange(encoded, 0, 12);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(java.util.Arrays.copyOfRange(encoded, 12, encoded.length)), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not decrypt TG4U auth-flow secret", exception);
        }
    }

    private static byte[] ivAndCiphertext(byte[] iv, byte[] ciphertext) {
        byte[] result = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);
        return result;
    }
}
