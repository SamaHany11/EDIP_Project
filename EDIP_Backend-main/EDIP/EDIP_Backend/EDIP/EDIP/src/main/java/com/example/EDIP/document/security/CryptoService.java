package com.example.EDIP.document.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;

@Service
public class CryptoService {


    @Value("${app.encryption.key}")
    private String secretKey;

    private static final String ALGORITHM = "AES";

    // =========================
    // ENCRYPT
    // =========================
    public byte[] encrypt(byte[] data) {
        try {
            Key key = generateKey();

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);

            return cipher.doFinal(data);

        } catch (Exception e) {
            throw new RuntimeException("Error while encrypting file", e);
        }
    }


    public byte[] decrypt(byte[] encryptedData) {
        try {
            Key key = generateKey();

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);

            return cipher.doFinal(encryptedData);

        } catch (Exception e) {
            throw new RuntimeException("Error while decrypting file", e);
        }
    }

    // =========================
    // KEY GENERATION
    // =========================
    private Key generateKey() {

        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);

        byte[] key = new byte[16];
        System.arraycopy(keyBytes, 0, key, 0, Math.min(keyBytes.length, 16));

        return new SecretKeySpec(key, ALGORITHM);
    }
}
