package com.rivers.core.crypto;

import lombok.extern.slf4j.Slf4j;

/**
 * 本地加解密 CLI，用于生成写入配置中心的密文及回验明文。
 * 需先设置环境变量 CONFIG_CIPHER_KEY。
 */
@Slf4j
public final class CipherCli {

    private CipherCli() {
        throw new IllegalStateException("Utility class");
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            log.error("用法: CipherCli <encrypt|decrypt> <值>");
            System.exit(1);
            return;
        }
        String result = "encrypt".equals(args[0])
                ? AesGcmCipher.encrypt(args[1])
                : AesGcmCipher.decrypt(args[1]);
        System.out.println(result);
    }
}
