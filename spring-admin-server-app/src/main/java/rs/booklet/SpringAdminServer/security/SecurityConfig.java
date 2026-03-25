package rs.booklet.SpringAdminServer.security;

import de.codecentric.boot.admin.server.config.AdminServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import rs.booklet.SpringAdminServer.service.PasswordService;

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.POST;

@Configuration
public class SecurityConfig
{
    private final PasswordService passwordService;
    private final AdminServerProperties adminServer;

    public SecurityConfig(
        PasswordService passwordService,
        AdminServerProperties adminServer
    )
    {
        this.adminServer = adminServer;
        this.passwordService = passwordService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception
    {
        SavedRequestAwareAuthenticationSuccessHandler successHandler =
            new SavedRequestAwareAuthenticationSuccessHandler();
        successHandler.setTargetUrlParameter("redirectTo");
        successHandler.setDefaultTargetUrl(adminServer.path("/"));

        http
            .authorizeHttpRequests(
                auth -> auth
                .requestMatchers(
                    PathPatternRequestMatcher.withDefaults()
                        .matcher(adminServer.path("/assets/**"))
                )
                .permitAll()
                .requestMatchers(
                    PathPatternRequestMatcher.withDefaults()
                        .matcher(adminServer.path("/login"))
                )
                .permitAll()
                .requestMatchers(
                    PathPatternRequestMatcher.withDefaults()
                        .matcher(adminServer.path("/actuator/info"))
                )
                .permitAll()
                .requestMatchers(
                    PathPatternRequestMatcher.withDefaults()
                        .matcher(adminServer.path("/actuator/health"))
                )
                .permitAll()
                .requestMatchers(
                    PathPatternRequestMatcher.withDefaults()
                        .matcher(POST, adminServer.path("/instances"))
                )
                .permitAll()
                .anyRequest()
                .authenticated()
            )
            .formLogin(
                formLogin -> formLogin
                .loginPage(adminServer.path("/login"))
                .successHandler(successHandler)
            )
            .logout(
                logout -> logout
                .logoutUrl(adminServer.path("/logout"))
            )
            .httpBasic(Customizer.withDefaults());

        http.addFilterAfter(new CustomCsrfFilter(), BasicAuthenticationFilter.class)
            .csrf(
                csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                .ignoringRequestMatchers(
                    PathPatternRequestMatcher.withDefaults()
                        .matcher(POST, adminServer.path("/instances")),
                    PathPatternRequestMatcher.withDefaults()
                        .matcher(DELETE, adminServer.path("/instances/*")),
                    PathPatternRequestMatcher.withDefaults()
                        .matcher(adminServer.path("/actuator/**"))
                )
            );

        return http.build();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService()
    {
        String hashedPassword = "{bcrypt}" + passwordService.getHashedPassword();
        UserDetails admin = User.builder()
            .username("admin")
            .password(hashedPassword)
            .roles("ADMIN")
            .build();

        return new InMemoryUserDetailsManager(admin);
    }
}