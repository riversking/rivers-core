package com.rivers.core.util;

import com.google.common.collect.Maps;
import com.rivers.core.constant.JwtConstant;
import com.rivers.core.entity.LoginUser;
import com.rivers.core.exception.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class JwtUtil {


    private static final SignatureAlgorithm signatureAlgorithm;

    private static final KeyPair keyPair;


    static {
        signatureAlgorithm = Jwts.SIG.RS256;
        try (InputStream is = JwtUtil.class.getClassLoader().getResourceAsStream("key/rivers.jks")) {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(is, JwtConstant.JWT_SECRET.toCharArray());
            PrivateKey riversPrivate = (PrivateKey) keyStore.getKey("siyao", JwtConstant.JWT_SECRET.toCharArray());
            PublicKey riversPub = keyStore.getCertificate("siyao").getPublicKey();
            keyPair = new KeyPair(riversPub, riversPrivate);
        } catch (Exception e) {
            throw new BusinessException(e);
        }
    }

    public static String createJwt(LoginUser loginUser, String key) {
        // 获取私钥和证书
        Map<String, Object> chaim = Maps.newHashMap();
        chaim.put("loginUser", loginUser);
        // 构建JWT
        return Jwts.builder()
                .id(key)
                .subject("subject")
                .issuer("issuer")
                .audience().add("audience")
                .and()
                .signWith(keyPair.getPrivate(), signatureAlgorithm)
                .claims(chaim)
                .compact();
    }

    public static Claims parseJwt(String token) {
        PublicKey publicKey = keyPair.getPublic();
        return Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(token).getPayload();
    }
}
