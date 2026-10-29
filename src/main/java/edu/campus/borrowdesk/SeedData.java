package edu.campus.borrowdesk;

import java.time.Clock;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SeedData {

	@Bean
	Clock clock() {
		return Clock.systemDefaultZone();
	}

	/** Seeds sample equipment only when the table is empty. */
	@Bean
	CommandLineRunner seedEquipment(EquipmentRepository repository) {
		return args -> {
			if (repository.count() == 0) {
				repository.saveAll(List.of(new Equipment("Projector", 3), new Equipment("Scientific Calculator", 10),
						new Equipment("Arduino Uno Kit", 6), new Equipment("Raspberry Pi 4", 4),
						new Equipment("Digital Multimeter", 5)));
			}
		};
	}
}
