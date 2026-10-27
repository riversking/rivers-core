package com.rivers.core.crypto;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM 配置加解密工具。密钥来自环境变量 {@code CONFIG_CIPHER_KEY}，
 * 经 SHA-256 派生为 32 字节密钥，与 JwtUtil 的密钥外置约定一致。
 */
@Slf4j
public final class AesGcmCipher {

    public static final String PREFIX = "ENC(";
    public static final String SUFFIX = ")";
    public static final String KEY_ENV = "CONFIG_CIPHER_KEY";

    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private AesGcmCipher() {
        throw new IllegalStateException("Utility class");
    }

    public static String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return PREFIX + Base64.getEncoder().encodeToString(payload) + SUFFIX;
        } catch (Exception e) {
            throw new IllegalStateException("配置加密失败", e);
        }
    }

    public static String decrypt(String ciphertext) {
        String body = ciphertext;
        if (body.startsWith(PREFIX) && body.endsWith(SUFFIX)) {
            body = body.substring(PREFIX.length(), body.length() - SUFFIX.length());
        }
        try {
            byte[] payload = Base64.getDecoder().decode(body);
            if (payload.length <= IV_LENGTH) {
                throw new IllegalArgumentException("密文长度非法");
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec(),
                    new GCMParameterSpec(TAG_LENGTH_BITS, payload, 0, IV_LENGTH));
            byte[] decrypted = cipher.doFinal(payload, IV_LENGTH, payload.length - IV_LENGTH);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("配置解密失败，请确认 CONFIG_CIPHER_KEY 与加密时一致", e);
        }
    }

    public static boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX) && value.endsWith(SUFFIX);
    }

    private static SecretKeySpec keySpec() {
        String key = System.getenv(KEY_ENV);
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("环境变量 " + KEY_ENV + " 未设置，无法解密配置");
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(key.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(digest, "AES");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 算法不可用", e);
        }
    }
}
