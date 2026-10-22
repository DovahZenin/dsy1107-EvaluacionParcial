package com.duoc.backend.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion de mensajeria del backend: SOLO publicacion.
 *
 * El backend no tiene consumers ni colas: declara el exchange de eventos y el
 * conversor JSON, y el SolicitudEventPublisher se encarga del resto. Si en el
 * entorno no hay broker (por ejemplo en ECS), el negocio sigue funcionando:
 * la publicacion fallida solo genera un warning.
 */
@Configuration
@EnableConfigurationProperties(MensajeriaProperties.class)
public class RabbitPublicacionConfig {

    @Bean
    public TopicExchange exchangeSolicitudes(MensajeriaProperties props) {
        return new TopicExchange(props.getExchangeSolicitudes(), true, false);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
