package com.duoc.adminrabbit.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Beans de topologia: un Queue (con sus argumentos de dead-lettering), su DLQ
 * y sus Bindings por cada caso de uso definidos en application.yml.
 *
 * Declararlos aqui hace que el broker se auto-repare: si alguien borra una
 * cola del sistema por la UI de RabbitMQ, al reconectar este servicio la vuelve
 * a crear con la configuracion correcta.
 */
@Configuration
@EnableConfigurationProperties(MensajeriaProperties.class)
public class RabbitTopologyConfig {

    /** Exchange principal: los eventos de solicitudes entran por aqui. */
    @Bean
    public TopicExchange exchangeSolicitudes(MensajeriaProperties holder) {
        return new TopicExchange(holder.getExchangeSolicitudes(), true, false);
    }

    /** Exchange de la muerte: destino de los mensajes que se NACKean sin requeue. */
    @Bean
    public DirectExchange exchangeMuerta(MensajeriaProperties holder) {
        return new DirectExchange(holder.getExchangeMuerta(), true, false);
    }

    // ------------------------------------------------------------------
    // Caso de uso: solicitud creada -> notificacion
    // ------------------------------------------------------------------

    @Bean
    public Queue colaNotificacionCreada(MensajeriaProperties holder) {
        return colaConDlq(holder.getSolicitudes().get("creada"), holder);
    }

    @Bean
    public Queue dlqNotificacionCreada(MensajeriaProperties holder) {
        return new Queue(holder.getSolicitudes().get("creada").getDlq(), true);
    }

    @Bean
    public Binding bindingNotificacionCreada(MensajeriaProperties holder,
                                             TopicExchange exchangeSolicitudes) {
        return BindingBuilder.bind(colaNotificacionCreada(holder))
                .to(exchangeSolicitudes)
                .with(holder.getSolicitudes().get("creada").getRoutingKey());
    }

    @Bean
    public Binding bindingDlqNotificacionCreada(MensajeriaProperties holder,
                                                DirectExchange exchangeMuerta) {
        return BindingBuilder.bind(dlqNotificacionCreada(holder))
                .to(exchangeMuerta)
                .with(holder.getSolicitudes().get("creada").getCola());
    }

    // ------------------------------------------------------------------
    // Caso de uso: solicitud evaluada -> notificacion
    // ------------------------------------------------------------------

    @Bean
    public Queue colaNotificacionEvaluada(MensajeriaProperties holder) {
        return colaConDlq(holder.getSolicitudes().get("evaluada"), holder);
    }

    @Bean
    public Queue dlqNotificacionEvaluada(MensajeriaProperties holder) {
        return new Queue(holder.getSolicitudes().get("evaluada").getDlq(), true);
    }

    @Bean
    public Binding bindingNotificacionEvaluada(MensajeriaProperties holder,
                                               TopicExchange exchangeSolicitudes) {
        return BindingBuilder.bind(colaNotificacionEvaluada(holder))
                .to(exchangeSolicitudes)
                .with(holder.getSolicitudes().get("evaluada").getRoutingKey());
    }

    @Bean
    public Binding bindingDlqNotificacionEvaluada(MensajeriaProperties holder,
                                                  DirectExchange exchangeMuerta) {
        return BindingBuilder.bind(dlqNotificacionEvaluada(holder))
                .to(exchangeMuerta)
                .with(holder.getSolicitudes().get("evaluada").getCola());
    }

    // ------------------------------------------------------------------
    // Caso de uso: toda solicitud ("solicitud.*") -> auditoria
    // ------------------------------------------------------------------

    @Bean
    public Queue colaAuditoria(MensajeriaProperties holder) {
        return colaConDlq(holder.getSolicitudes().get("auditoria"), holder);
    }

    @Bean
    public Queue dlqAuditoria(MensajeriaProperties holder) {
        return new Queue(holder.getSolicitudes().get("auditoria").getDlq(), true);
    }

    @Bean
    public Binding bindingAuditoria(MensajeriaProperties holder,
                                    TopicExchange exchangeSolicitudes) {
        return BindingBuilder.bind(colaAuditoria(holder))
                .to(exchangeSolicitudes)
                .with(holder.getSolicitudes().get("auditoria").getRoutingKey());
    }

    @Bean
    public Binding bindingDlqAuditoria(MensajeriaProperties holder,
                                       DirectExchange exchangeMuerta) {
        return BindingBuilder.bind(dlqAuditoria(holder))
                .to(exchangeMuerta)
                .with(holder.getSolicitudes().get("auditoria").getCola());
    }

    /** Cola durable que al fallar (NACK sin requeue) manda el mensaje a la DLX. */
    private Queue colaConDlq(MensajeriaProperties.Ruta ruta, MensajeriaProperties props) {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", props.getExchangeMuerta());
        args.put("x-dead-letter-routing-key", ruta.getCola());
        return new Queue(ruta.getCola(), true, false, false, args);
    }

    /** JSON en el broker, no Java serializado. */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
