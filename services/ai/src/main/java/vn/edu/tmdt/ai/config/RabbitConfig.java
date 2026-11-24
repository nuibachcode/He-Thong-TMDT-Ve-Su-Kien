package vn.edu.tmdt.ai.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "ticketgo.events";
    public static final String AI_QUEUE = "ai.catalog.changed";

    @Bean
    public TopicExchange ticketgoExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue aiCatalogQueue() {
        return QueueBuilder.durable(AI_QUEUE).build();
    }

    @Bean
    public Binding aiCatalogBinding(Queue aiCatalogQueue, TopicExchange ticketgoExchange) {
        return BindingBuilder.bind(aiCatalogQueue).to(ticketgoExchange).with("catalog.#");
    }
}
