package com.example.delservice.config.filter;

import com.example.delservice.config.jwt.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Component
public class JwtTokenFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            //если подпись не совпадает с вычисленной, то SignatureException
            //если подпись некорректная (не парсится), то MalformedJwtException
            //если время подписи истекло, то ExpiredJwtException
            try {
                username = jwtUtil.extractUsername(jwt);
            } catch (ExpiredJwtException expiredJwtException) {
                logger.error("Token expired", expiredJwtException);
            } catch (SignatureException signatureException) {
                logger.error("Signature does not match", signatureException);
            } catch (MalformedJwtException malformedJwtException) {
                logger.error("The signature is incorrect (not parsed)", malformedJwtException);
            }
            if (username == null) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.getWriter().write("The token is not valid");
                return;
            }
        }
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            String commaSeparatedListOfAuthorities = jwtUtil.extractAuthorities(jwt);
            List<GrantedAuthority> authorities =
                    AuthorityUtils.commaSeparatedStringToAuthorityList(commaSeparatedListOfAuthorities);
            UsernamePasswordAuthenticationToken upaat = new UsernamePasswordAuthenticationToken(
                    username, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(upaat);
        }
        filterChain.doFilter(request, response);
    }
}
