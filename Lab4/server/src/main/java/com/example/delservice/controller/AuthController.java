package com.example.delservice.controller;

import com.example.delservice.config.AuthRequest;
import com.example.delservice.config.AuthResponse;
import com.example.delservice.config.jwt.Exception.TokenRefreshException;
import com.example.delservice.config.jwt.JwtUtil;
import com.example.delservice.config.jwt.TokenRefreshRequest;
import com.example.delservice.model.RefreshToken;
import com.example.delservice.model.User;
import com.example.delservice.service.RefreshTokenService;
import com.example.delservice.service.UserService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtTokenUtil;

    @Autowired
    private RefreshTokenService refreshTokenService;


    @ApiOperation(
            value = "Регистрация пользователя",
            notes = "Позволяет зарегистрировать пользователя"
    )

    @PostMapping("/register")
    void register(
            @RequestParam("username") @ApiParam(value = "Имя пользователя", example = "vasya") String username,
            @RequestParam("password") @ApiParam(value = "Пароль", example = "3530901") String password) {

        boolean status = userService
                .saveUser(new User(username, password));

        if (!status) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already exist");
        }
    }

    @ApiOperation(
            value = "Валидация пользователя",
            notes = "Метод для получения клиента информации о правах текущего пользователя" +
                    "Возвращает boolean переменную. " +
                    "true = если у пользователя права ROLE_SELLER, " +
                    "false если права обычного пользователя"
    )
    @GetMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public AuthResponse login(@RequestBody AuthRequest authRequest) {
        Authentication authentication;

        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getName(), authRequest.getPassword()));

        } catch (BadCredentialsException bce) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Имя или пароль неправильны", bce);
        }
        // при создании токена в него кладется username как Subject claim и список authorities как кастомный claim
        String jwt = jwtTokenUtil.generateToken((UserDetails) authentication.getPrincipal());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(authRequest.getName());

        return new AuthResponse(jwt, refreshToken.getToken());
    }

    @PostMapping("/refreshtoken")
    public AuthResponse refreshToken(@RequestBody TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String jwt = jwtTokenUtil.generateToken(user);
                    return new AuthResponse(jwt, requestRefreshToken);
                })
                .orElseThrow(() -> new TokenRefreshException(requestRefreshToken,
                        "Refresh token is not in database"));
    }

}
