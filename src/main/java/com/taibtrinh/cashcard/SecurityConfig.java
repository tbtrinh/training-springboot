package com.taibtrinh.cashcard;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
public class SecurityConfig
{
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception
    {
        http.authorizeHttpRequests()
                .requestMatchers("/cashcards/**")
                .hasRole("CARD-OWNER")
                .and()
                .csrf()
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                .and()
                .httpBasic()
                .and()
                .addFilterAfter(new OncePerRequestFilter() {
                        @Override
                        protected void doFilterInternal(HttpServletRequest request,
                                                        HttpServletResponse response,
                                                        FilterChain filterChain) throws ServletException, IOException
                        {
                            CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
                            token.getToken();
                            filterChain.doFilter(request, response);
                        }
                    }, CsrfFilter.class
                );
        return http.build();
    }

    @Bean
    public UserDetailsService testOnlyUsers(PasswordEncoder passwordEncoder)
    {
        User.UserBuilder users = User.builder();
        UserDetails sarah = users
                .username("sarah1")
                .password(passwordEncoder.encode("abc123"))
                .roles("CARD-OWNER")
                .build();
        UserDetails hankOwnsNoCards = users
                .username("hank-owns-no-cards")
                .password(passwordEncoder.encode("qrs456"))
                .roles("NON-OWNER") // new role
                .build();
        UserDetails kumar = users
                .username("kumar2")
                .password(passwordEncoder.encode("xyz789"))
                .roles("CARD-OWNER")
                .build();
        return new InMemoryUserDetailsManager(sarah, hankOwnsNoCards, kumar);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
