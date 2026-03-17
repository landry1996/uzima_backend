package com.uzima.bootstrap.config;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import com.uzima.application.notification.WebSocketNotifierPort;
import com.uzima.bootstrap.adapter.websocket.SocketIOConnectionHandler;
import com.uzima.bootstrap.adapter.websocket.SocketIONotificationAdapter;
import com.uzima.infrastructure.notification.LoggingNotificationAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * Configuration Socket.IO (Netty) — profil {@code prod} uniquement.
 * <p>
 * En local (profil {@code !prod}), {@link WebSocketNotifierPort} est implémenté
 * par {@link LoggingNotificationAdapter} (bean déclaré dans {@link InfrastructureConfiguration}).
 * <p>
 * En prod, cette classe crée :
 * <ul>
 *   <li>Le {@link SocketIOServer} (serveur Netty, port configurable)</li>
 *   <li>Le {@link SocketIONotificationAdapter} (implémentation de WebSocketNotifierPort)</li>
 *   <li>Le {@link SocketIOConnectionHandler} (gestion des connexions/rooms)</li>
 * </ul>
 */
@org.springframework.context.annotation.Configuration
@Profile("prod")
public class SocketIOConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SocketIOConfiguration.class);

    private boolean serverRunning = false;

    @Value("${uzima.websocket.host:0.0.0.0}")
    private String host;

    @Value("${uzima.websocket.port:9092}")
    private int port;

    @Value("${uzima.websocket.ping-interval:25000}")
    private int pingInterval;

    @Value("${uzima.websocket.ping-timeout:60000}")
    private int pingTimeout;

    @Bean
    public SocketIOServer socketIOServer() {
        Configuration config = new Configuration();
        config.setHostname(host);
        config.setPort(port);
        config.setPingInterval(pingInterval);
        config.setPingTimeout(pingTimeout);
        // Désactive la compatibilité XHR polling pour forcer WebSocket pur en prod.
        // Retirer cette ligne si le client doit fonctionner derrière un proxy sans WS.
        // config.setTransports(Transport.WEBSOCKET);

        SocketIOServer server = new SocketIOServer(config);
        log.info("[SOCKET.IO] Serveur configuré → {}:{}", host, port);
        return server;
    }

    @Bean
    public WebSocketNotifierPort webSocketNotifierPort(SocketIOServer server) {
        return new SocketIONotificationAdapter(server);
    }

    @Bean
    public SocketIOConnectionHandler socketIOConnectionHandler(SocketIOServer server) {
        return new SocketIOConnectionHandler(server);
    }

    /** Démarre le serveur après que le contexte Spring est complètement initialisé. */
    @EventListener(ContextRefreshedEvent.class)
    public void startSocketIOServer(ContextRefreshedEvent event) {
        // Guard : ne démarre qu'une seule fois (ContextRefreshedEvent peut être émis plusieurs fois)
        if (!serverRunning) {
            SocketIOServer server = event.getApplicationContext().getBean(SocketIOServer.class);
            server.start();
            serverRunning = true;
            log.info("[SOCKET.IO] Serveur démarré sur le port {}", port);
        }
    }

    /** Arrête le serveur proprement à la fermeture du contexte Spring. */
    @EventListener(ContextClosedEvent.class)
    public void stopSocketIOServer(ContextClosedEvent event) {
        if (serverRunning) {
            SocketIOServer server = event.getApplicationContext().getBean(SocketIOServer.class);
            server.stop();
            serverRunning = false;
            log.info("[SOCKET.IO] Serveur arrêté proprement");
        }
    }
}
