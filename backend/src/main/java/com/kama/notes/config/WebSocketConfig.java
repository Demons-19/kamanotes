package com.kama.notes.config;

import com.kama.notes.websocket.MessageWebSocketServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;

/**
 * WebSocket 配置类
 */
@Configuration
public class WebSocketConfig implements ServletContextAware {

    @Bean
    public MessageWebSocketServer messageWebSocketServer() {
        return new MessageWebSocketServer();
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        ServerContainer serverContainer = (ServerContainer) servletContext.getAttribute(ServerContainer.class.getName());
        if (serverContainer != null) {
            try {
                serverContainer.addEndpoint(MessageWebSocketServer.class);
            } catch (DeploymentException e) {
                throw new RuntimeException("注册 WebSocket endpoint 失败", e);
            }
        }
    }
}
