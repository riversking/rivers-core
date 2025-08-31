package com.rivers.core.util;

import com.google.common.collect.Maps;
import com.rivers.core.entity.LoginUser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.util.Map;

@Slf4j
public class JwtUtil {

    private JwtUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static String createJwt(LoginUser loginUser) {
        // 获取私钥和证书
        SignatureAlgorithm signatureAlgorithm = Jwts.SIG.RS256;
        KeyPair keyPair = signatureAlgorithm.keyPair().build();
        Map<String, Object> chaim = Maps.newHashMap();
        chaim.put("loginUser", loginUser);
        // 构建JWT
        return Jwts.builder()
                .subject("subject")
                .issuer("issuer")
                .audience().add("audience")
                .and()
                .signWith(keyPair.getPrivate(), signatureAlgorithm)
                .claims(chaim)
                .compact();
    }
}
