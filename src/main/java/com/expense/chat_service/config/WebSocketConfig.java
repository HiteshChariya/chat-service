package com.expense.chat_service.config;

import com.expense.chat_service.request.Principle;
import com.expense.chat_service.security.TokenValidation;
import com.expense.chat_service.security.ValidationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Collections;
import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final TokenValidation tokenValidation;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:5173", "http://localhost:3000")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null) return message;

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String token = accessor.getFirstNativeHeader("Authorization");
                    if (StringUtils.isBlank(token)) {
                        token = accessor.getFirstNativeHeader("token");
                    }
                    if (StringUtils.isBlank(token)) {
                        throw new IllegalArgumentException("Missing Authorization token");
                    }
                    if (token.startsWith("Bearer ")) {
                        token = token.substring(7).trim();
                    }
                    ValidationResponse validationResponse = tokenValidation.handleRequest("Bearer " + token);
                    if (!Boolean.TRUE.equals(validationResponse.getTokenValidated()) || validationResponse.getUser() == null) {
                        throw new IllegalArgumentException("Invalid Authorization token");
                    }
                    Principle principle = validationResponse.getUser();
                    String role = principle.getUserType() != null ? principle.getUserType() : "USER";
                    List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                            new SimpleGrantedAuthority("ROLE_" + role));
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            principle, null, authorities);
                    accessor.setUser(auth);
                    log.debug("WebSocket authenticated user: {} role: {}", principle.getEmail(), role);
                }

                return message;
            }
        });
    }
}
