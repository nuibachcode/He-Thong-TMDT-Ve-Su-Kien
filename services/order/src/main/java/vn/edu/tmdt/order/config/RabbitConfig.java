package vn.edu.tmdt.order.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "ticketgo.events";

    @Bean
    public TopicExchange ticketgoExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }
}
