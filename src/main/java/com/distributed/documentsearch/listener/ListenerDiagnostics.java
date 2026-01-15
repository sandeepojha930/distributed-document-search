package com.distributed.documentsearch.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ListenerDiagnostics {

    private final RabbitListenerEndpointRegistry registry;

    @EventListener(ContextRefreshedEvent.class)
    public void onContextRefreshed() {
        java.util.Collection<org.springframework.amqp.rabbit.listener.MessageListenerContainer> containers = registry.getListenerContainers();
        int count = containers != null ? containers.size() : 0;
        log.info("Rabbit listener container count: {}", count);
        if (containers != null) {
            for (org.springframework.amqp.rabbit.listener.MessageListenerContainer c : containers) {
                if (c instanceof SimpleMessageListenerContainer) {
                    SimpleMessageListenerContainer smlc = (SimpleMessageListenerContainer) c;
                    String id = smlc.getListenerId() != null ? smlc.getListenerId() : "<unknown>";
                    log.info("Listener container id='{}' active={}, consumers={}", id, smlc.isActive(), smlc.getActiveConsumerCount());
                } else {
                    log.info("Listener container class={}", c.getClass().getName());
                }
            }
        }
    }
}

