package kwonjh0406.joondrive.config;

import jakarta.servlet.http.HttpServletResponse;
import kwonjh0406.joondrive.auth.service.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableConfigurationProperties(CorsProperties.class)
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final CorsProperties corsProperties;

    /**
     * 스프링 시큐리티 메인 설정
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 비활성화
            .csrf(AbstractHttpConfigurer::disable)

            // CORS 설정 활성화 (아래 Bean 사용)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // 인증/인가 설정
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().authenticated()
            )

            // 로그인 처리
            .formLogin(login -> login
                .loginProcessingUrl("/api/auth/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler((_, res, _) ->
                    res.setStatus(HttpServletResponse.SC_OK))
                .failureHandler((_, res, _) ->
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED))
            )

            // 자동 로그인
            .rememberMe(r -> r
                .key("secure-key")
                .rememberMeParameter("rememberMe")
                .tokenValiditySeconds(60 * 60 * 24 * 7)
                .userDetailsService(userDetailsService)
            )

            // 로그아웃 처리
            .logout(l -> l
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((_, res, _) ->
                    res.setStatus(HttpServletResponse.SC_OK))
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            );

        return http.build();
    }

    /**
     * 비밀번호 암호화
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * CORS 설정
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // 쿠키(JSESSIONID) 사용 시 필수
        config.setAllowCredentials(true);

        // application.yml의 List 그대로 사용
        config.setAllowedOrigins(corsProperties.getAllowedOrigins());

        config.setAllowedMethods(
            List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")
        );

        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Set-Cookie"));

        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
