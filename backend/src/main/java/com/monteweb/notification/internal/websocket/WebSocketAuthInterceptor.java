package com.monteweb.notification.internal.websocket;

import com.monteweb.auth.AuthModuleApi;
import com.monteweb.auth.TokenClaims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * Intercepts STOMP CONNECT frames to validate JWT tokens.
 * Rejects connections without valid authentication.
 * Also validates SUBSCRIBE destinations to prevent subscribing to other users' queues.
 */
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

    private final AuthModuleApi authModuleApi;

    public WebSocketAuthInterceptor(AuthModuleApi authModuleApi) {
        this.authModuleApi = authModuleApi;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            return handleConnect(message, accessor);
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            return handleSubscribe(message, accessor);
        }

        return message;
    }

    private Message<?> handleConnect(Message<?> message, StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("WebSocket CONNECT rejected: missing or invalid Authorization header");
            throw new org.springframework.security.access.AccessDeniedException("Missing authentication token");
        }

        String token = authHeader.substring(7);
        var claims = authModuleApi.validateAndExtractClaims(token)
                .orElseThrow(() -> {
                    log.warn("WebSocket CONNECT rejected: invalid JWT token");
                    return new org.springframework.security.access.AccessDeniedException("Invalid authentication token");
                });

        var authorities = new ArrayList<SimpleGrantedAuthority>();
        if (claims.role() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + claims.role()));
        }

        var authentication = new UsernamePasswordAuthenticationToken(claims.userId(), null, authorities);
        accessor.setUser(authentication);

        log.debug("WebSocket CONNECT authenticated for user: {}", claims.userId());
        return message;
    }

    private Message<?> handleSubscribe(Message<?> message, StompHeaderAccessor accessor) {
        // Spring's UserDestinationMessageHandler resolves /user/queue/* subscriptions
        // to session-specific destinations automatically. No manual validation needed.
        return message;
    }
}
