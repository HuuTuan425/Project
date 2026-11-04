package com.vuhuutuan.Project.config;

import com.vuhuutuan.Project.service.AccountService;
import com.vuhuutuan.Project.repository.AppUserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AccountService accountService,
            AppUserRepository appUserRepository
    ) throws Exception {
        return http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/dang-nhap", "/khong-co-quyen", "/css/**", "/error").permitAll()
                        .requestMatchers("/tai-khoan", "/tai-khoan/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/nhap-hang/nha-cung-cap")
                        .hasAnyRole("ADMIN", "PURCHASING", "WAREHOUSE", "MANAGER")
                        .requestMatchers(HttpMethod.GET, "/nhap-hang/nha-cung-cap/*/sua")
                        .hasAnyRole("ADMIN", "PURCHASING")
                        .requestMatchers(HttpMethod.POST, "/nhap-hang/nha-cung-cap")
                        .hasAnyRole("ADMIN", "PURCHASING")
                        .requestMatchers(HttpMethod.POST, "/nhap-hang/nha-cung-cap/*/xoa")
                        .hasAnyRole("ADMIN", "PURCHASING")
                        .requestMatchers(HttpMethod.POST, "/nhap-hang/nha-cung-cap/de-xuat")
                        .hasRole("WAREHOUSE")
                        .requestMatchers(HttpMethod.POST,
                                "/nhap-hang/nha-cung-cap/de-xuat/*/duyet",
                                "/nhap-hang/nha-cung-cap/de-xuat/*/tu-choi")
                        .hasAnyRole("ADMIN", "PURCHASING")
                        .requestMatchers(HttpMethod.GET, "/nhap-hang/tao")
                        .hasAnyRole("ADMIN", "PURCHASING")
                        .requestMatchers(HttpMethod.POST, "/nhap-hang")
                        .hasAnyRole("ADMIN", "PURCHASING")
                        .requestMatchers(HttpMethod.POST, "/nhap-hang/*/nhan")
                        .hasAnyRole("ADMIN", "WAREHOUSE")
                        .requestMatchers(HttpMethod.GET, "/mat-hang")
                        .hasAnyRole("ADMIN", "WAREHOUSE", "PURCHASING")
                        .requestMatchers("/nhap-hang", "/nhap-hang/**")
                        .hasAnyRole("ADMIN", "PURCHASING", "WAREHOUSE", "MANAGER")
                        .requestMatchers("/hoa-don/bao-cao").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.GET, "/hoa-don").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.POST, "/hoa-don/*/huy").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.GET, "/mat-hang/tim-kiem")
                        .hasAnyRole("ADMIN", "CASHIER", "WAREHOUSE", "PURCHASING")
                        .requestMatchers(HttpMethod.GET, "/khach-hang/them").hasAnyRole("ADMIN", "CASHIER")
                        .requestMatchers(HttpMethod.POST, "/khach-hang/them").hasAnyRole("ADMIN", "CASHIER")
                        .requestMatchers("/khach-hang", "/khach-hang/**").hasRole("ADMIN")
                        .requestMatchers("/mat-hang", "/mat-hang/**").hasAnyRole("ADMIN", "WAREHOUSE")
                        .requestMatchers("/hoa-don/lap", "/hoa-don/lap/**").hasAnyRole("ADMIN", "CASHIER")
                        .requestMatchers(HttpMethod.GET, "/hoa-don/*", "/hoa-don/*/pdf")
                        .hasAnyRole("ADMIN", "CASHIER", "MANAGER")
                        .anyRequest().denyAll()
                )
                .formLogin(form -> form
                        .loginPage("/dang-nhap")
                        .loginProcessingUrl("/dang-nhap")
                        .successHandler((request, response, authentication) -> {
                            accountService.recordLogin(authentication.getName());
                            String destination = authentication.getAuthorities().stream()
                                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_WAREHOUSE"))
                                    ? "/nhap-hang"
                                    : authentication.getAuthorities().stream()
                                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_PURCHASING"))
                                    ? "/nhap-hang"
                                    : authentication.getAuthorities().stream()
                                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_MANAGER"))
                                    ? "/hoa-don/bao-cao" : "/hoa-don/lap";
                            response.sendRedirect(destination);
                        })
                        .failureUrl("/dang-nhap?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/dang-nhap?logout")
                        .permitAll()
                )
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler((request, response, denied) ->
                                response.sendRedirect("/khong-co-quyen"))
                )
                .addFilterAfter(new AccountStatusFilter(appUserRepository),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
