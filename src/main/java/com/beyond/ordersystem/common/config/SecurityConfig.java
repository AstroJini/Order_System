package com.beyond.ordersystem.common.config;

import com.beyond.ordersystem.common.auth.JwtAuthenticationHandler;
import com.beyond.ordersystem.common.auth.JwtAuthorizationHandler;
import com.beyond.ordersystem.common.auth.JwtTokenFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@RequiredArgsConstructor
//PreAuthorize 어노테이션 사용하기 위한 설정
@EnableMethodSecurity
public class SecurityConfig {
    private final JwtTokenFilter jwtTokenFilter;
    private final JwtAuthenticationHandler jwtAuthenticationHandler;
    private final JwtAuthorizationHandler jwtAuthorizationHandler;
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .cors(c->c.configurationSource(corsConfiguration()))
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(e->
                        e.authenticationEntryPoint(jwtAuthenticationHandler) // 401의 경우
                                .accessDeniedHandler(jwtAuthorizationHandler) // 403의 경우
                )
                .authorizeHttpRequests(a->a.requestMatchers("/member/create", "/member/doLogin", "/member/refresh-at", "/product/list").permitAll().anyRequest().authenticated())
                .build();
    }
    private CorsConfigurationSource corsConfiguration(){
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("*")); // 모든 HTTP(get, post 등) 메서드 허용
        configuration.setAllowedHeaders(Arrays.asList("*")); // 모든 헤더요소(Authorization 등) 허용
        configuration.setAllowCredentials(true); // 자격 증명 허용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); //모든 url패턴에 대해 cors설정 적용
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

}
