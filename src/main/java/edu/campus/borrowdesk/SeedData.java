package edu.campus.borrowdesk;

import java.time.Clock;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SeedData {

	@Bean
	Clock clock() {
		return Clock.systemDefaultZone();
	}

	/** Seeds sample equipment only when the equipment table is empty. */
	@Bean
	CommandLineRunner seedEquipment(EquipmentRepository repository) {
		return args -> {
			if (repository.count() == 0) {
				repository.saveAll(List.of(
					new Equipment("Projector", 3),
					new Equipment("Scientific Calculator", 10),
					new Equipment("Arduino Uno Kit", 6),
					new Equipment("Raspberry Pi 4", 4),
					new Equipment("Digital Multimeter", 5)
				));
			}
		};
	}

	/** Seeds default demo admin and student accounts only when the user table is empty. */
	@Bean
	CommandLineRunner seedUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			if (userRepository.count() == 0) {
				String adminPass = System.getenv("BORROWDESK_ADMIN_PASSWORD");
				if (adminPass == null || adminPass.isBlank()) {
					adminPass = "admin123";
				}

				String studentPass = System.getenv("BORROWDESK_STUDENT_PASSWORD");
				if (studentPass == null || studentPass.isBlank()) {
					studentPass = "student123";
				}

				userRepository.saveAll(List.of(
					new User("admin", "Lab Administrator", "ADMIN001", passwordEncoder.encode(adminPass), "ROLE_ADMIN"),
					new User("student", "Akshitha Sharma", "21CS001", passwordEncoder.encode(studentPass), "ROLE_STUDENT"),
					new User("student2", "Rahul Verma", "21CS002", passwordEncoder.encode(studentPass), "ROLE_STUDENT")
				));
			}
		};
	}
}
