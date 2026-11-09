package com.nilspath.api;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;
import java.util.*;
@Configuration public class SecurityConfig {
 @Bean PasswordEncoder encoder(){return new BCryptPasswordEncoder(){
  @Override public boolean matches(CharSequence rawPassword,String encodedPassword){
   return rawPassword!=null&&rawPassword.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length<=72&&super.matches(rawPassword,encodedPassword);
  }
 };}
 @Bean UserDetailsService users(AccountDirectory directory){return username->{Account a=directory.forLogin(username);return User.withUsername(directory.loginName(a)).password(a.password).roles(a.role).disabled(!a.approved).build();};}
 @Bean SecurityFilterChain filter(HttpSecurity h, @org.springframework.beans.factory.annotation.Qualifier("cors") CorsConfigurationSource corsSource) throws Exception {return h.sessionManagement(s->s.sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS)).requestCache(c->c.disable()).csrf(c->c.disable()).cors(c->c.configurationSource(corsSource)).authorizeHttpRequests(a->a.dispatcherTypeMatchers(jakarta.servlet.DispatcherType.FORWARD).permitAll().requestMatchers(org.springframework.http.HttpMethod.GET,"/","/index.html","/assets/**","/favicon.ico","/health","/candidate/login","/candidate/register","/hr/login","/hr/register","/admin","/admin/login").permitAll().requestMatchers("/api/auth/register","/api/auth/login").permitAll().requestMatchers("/api/admin/**").hasRole("ADMIN").requestMatchers("/api/hr/**").hasRole("HR").requestMatchers("/api/candidate/**").hasRole("CANDIDATE").requestMatchers("/api/auth/me","/api/jobs/**").authenticated().anyRequest().denyAll()).httpBasic(b->{}).build();}
 @Bean CorsConfigurationSource cors(@org.springframework.beans.factory.annotation.Value("${app.cors-origin}") String origin){CorsConfiguration c=new CorsConfiguration();c.setAllowedOrigins(List.of(origin));c.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));c.setAllowedHeaders(List.of("Authorization","Content-Type"));UrlBasedCorsConfigurationSource s=new UrlBasedCorsConfigurationSource();s.registerCorsConfiguration("/**",c);return s;}
}
