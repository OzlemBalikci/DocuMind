package com.documind.common.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

@AutoConfiguration(before = OAuth2ResourceServerAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(name = "app.security.resource-server.enabled", matchIfMissing = true)
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({ResourceServerProperties.class, OAuth2ResourceServerProperties.class})
@Import({JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class})
public class ResourceServerSecurityConfig {

	@Bean
	JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());
		converter.setPrincipalClaimName("sub");
		return converter;
	}

	@Bean
	@ConditionalOnMissingBean
	JwtDecoder jwtDecoder(OAuth2ResourceServerProperties oauth2, ResourceServerProperties security) {
		OAuth2ResourceServerProperties.Jwt jwt = oauth2.getJwt();
		return ResourceServerJwtDecoderFactory.fromIssuerOrJwkSet(
				jwt.getIssuerUri(),
				jwt.getJwkSetUri(),
				security.getAudience());
	}

	@Bean
	SecurityFilterChain resourceServerSecurityFilterChain(
			HttpSecurity http,
			JwtAuthenticationConverter jwtAuthenticationConverter,
			JsonAuthenticationEntryPoint authenticationEntryPoint,
			JsonAccessDeniedHandler accessDeniedHandler,
			ResourceServerProperties properties,
			org.springframework.beans.factory.ObjectProvider<CorsConfigurationSource> cors)
			throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> {
					auth.requestMatchers(
							"/actuator/health",
							"/actuator/health/**",
							"/actuator/prometheus",
							"/error").permitAll();
					auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
					if (properties.isProtectInternal()) {
						auth.requestMatchers("/internal/**").hasRole("SERVICE");
					}
					auth.anyRequest().authenticated();
				})
				.oauth2ResourceServer(oauth2 -> oauth2
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler)
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler));
		CorsConfigurationSource corsSource = cors.orderedStream()
				.filter(source -> !(source instanceof HandlerMappingIntrospector))
				.findFirst()
				.orElse(null);
		if (corsSource != null) {
			http.cors(c -> c.configurationSource(corsSource));
		} else {
			http.cors(AbstractHttpConfigurer::disable);
		}
		return http.build();
	}
}
