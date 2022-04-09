package com.example.delservice.config.jwt;

import io.jsonwebtoken.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class JwtUtil {

    @Value("${jwt.secret}")
    private String SECRET_KEY;

    @Value("${jwt.sessionTime}")
    private long sessionTime;

    private static final Logger logger = LogManager.getLogger(JwtUtil.class);


    // генерация токена (кладем в него имя пользователя и authorities)
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        String commaSeparatedListOfAuthorities = userDetails
                .getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.joining(","));
        claims.put("authorities", commaSeparatedListOfAuthorities);
        return createToken(claims, userDetails.getUsername());
    }

    //извлечение имени пользователя из токена (внутри валидация токена)
    public String extractUsername(String token) throws ExpiredJwtException,
            SignatureException,
            MalformedJwtException {
        return extractClaim(token, Claims::getSubject);
    }

    //извлечение authorities (внутри валидация токена)
    public String extractAuthorities(String token) {
        return extractClaim(token, claims -> (String) claims.get("authorities"));
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) throws ExpiredJwtException,
            SignatureException,
            MalformedJwtException {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) throws ExpiredJwtException,
            SignatureException,
            MalformedJwtException {
        return Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token).getBody();
    }


    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder().setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(expireTimeFromNow())
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY).compact();
    }

    private Date expireTimeFromNow() {
        return new Date(System.currentTimeMillis() + sessionTime);
    }
}
