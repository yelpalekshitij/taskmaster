package com.taskmaster.adminserver.config

import de.codecentric.boot.admin.server.config.AdminServerProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.userdetails.User
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler
import org.springframework.security.web.util.matcher.AntPathRequestMatcher

@Configuration
@EnableWebSecurity
class SecurityConfig(private val adminServer: AdminServerProperties) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        val successHandler = SavedRequestAwareAuthenticationSuccessHandler().apply {
            setTargetUrlParameter("redirectTo")
            setDefaultTargetUrl(adminServer.path("/"))
        }

        http
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(adminServer.path("/assets/**")).permitAll()
                    .requestMatchers(adminServer.path("/actuator/info")).permitAll()
                    .requestMatchers(adminServer.path("/actuator/health")).permitAll()
                    .requestMatchers(adminServer.path("/login")).permitAll()
                    .anyRequest().authenticated()
            }
            .formLogin { form ->
                form.loginPage(adminServer.path("/login"))
                    .successHandler(successHandler)
            }
            .logout { logout ->
                logout.logoutUrl(adminServer.path("/logout"))
            }
            // httpBasic lets the SBA JavaScript frontend authenticate API calls directly
            // without triggering a form-login redirect that would result in a 404 after login.
            .httpBasic { }
            .csrf { csrf ->
                // CookieCsrfTokenRepository makes the CSRF token readable by the SBA JS frontend.
                // Without this, background API calls from the UI fail CSRF checks, trigger a
                // re-login redirect, and after login the user lands on an API URL → 404.
                csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(CsrfTokenRequestAttributeHandler())
                    .ignoringRequestMatchers(
                        AntPathRequestMatcher(adminServer.path("/instances")),
                        AntPathRequestMatcher(adminServer.path("/instances/**")),
                        AntPathRequestMatcher(adminServer.path("/actuator/**"))
                    )
            }

        return http.build()
    }

    @Bean
    fun userDetailsService(): InMemoryUserDetailsManager {
        val admin = User.withDefaultPasswordEncoder()
            .username("admin")
            .password("admin")
            .roles("ADMIN")
            .build()
        return InMemoryUserDetailsManager(admin)
    }
}
