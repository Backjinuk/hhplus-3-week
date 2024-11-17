package com.example.hh3week.common.config.Kafka;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import com.example.hh3week.adapter.out.messaging.kafka.dto.SeatReservationRequest;

@EnableKafka
@Configuration
public class KafkaProducerConfig {

	@Value("${spring.kafka.bootstrap-servers}")
	private String bootstrapServers;

	@Bean
	public ProducerFactory<String, SeatReservationRequest> producerFactory() {
		Map<String, Object> configProps = new HashMap<>();
		configProps.put(
			ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
			bootstrapServers);
		configProps.put(
			ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
			StringSerializer.class);
		configProps.put(
			ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
			JsonSerializer.class);
		return new DefaultKafkaProducerFactory<>(configProps);
	}

	// KafkaTemplate for SeatReservationRequest
	@Bean
	public KafkaTemplate<String, SeatReservationRequest> requestKafkaTemplate() {
		return new KafkaTemplate<>(producerFactory());
	}


}
