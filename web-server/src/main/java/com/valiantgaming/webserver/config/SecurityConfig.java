package com.valiantgaming.webserver.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * spring-boot-starter-security (a root-level dependency, inherited here via spring-boot-starter-web)
 * locks every endpoint behind HTTP Basic by default. Registration has to be reachable by an
 * unauthenticated client by definition, so it's carved out here explicitly rather than disabling
 * security wholesale - anything added under /api/ later defaults back to authenticated.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig
{
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception
    {
        http
            .authorizeHttpRequests(auth -> auth
                // Spring MVC's validation-error handling (e.g. a failed @Valid on RegisterAccountRequest)
                // re-dispatches internally with DispatcherType.ERROR - without this, that re-dispatch gets
                // evaluated as a fresh request against a path that isn't /api/register, is denied, and the
                // caller sees a bare 403 instead of the intended 400 with the actual validation body.
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers("/api/register").permitAll()
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
