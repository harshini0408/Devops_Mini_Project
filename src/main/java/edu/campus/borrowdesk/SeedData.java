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

	/** Seeds default demo admin and student accounts. */
	@Bean
	CommandLineRunner seedUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			// Remove previously created demo student accounts if present
			userRepository.findByUsername("student").ifPresent(userRepository::delete);
			userRepository.findByUsername("student2").ifPresent(userRepository::delete);

			// Admin account remains the same
			if (userRepository.findByUsername("admin").isEmpty()) {
				String adminPass = System.getenv("BORROWDESK_ADMIN_PASSWORD");
				if (adminPass == null || adminPass.isBlank()) {
					adminPass = "admin123";
				}
				userRepository.save(new User("admin", "Lab Administrator", "ADMIN001", passwordEncoder.encode(adminPass), "ROLE_ADMIN"));
			}

			// Student accounts: akshita, abi, shiva, shri
			syncStudent(userRepository, passwordEncoder, "akshita", "akshita", "21CS001", "akshita123");
			syncStudent(userRepository, passwordEncoder, "abi", "abi", "21CS002", "abi123");
			syncStudent(userRepository, passwordEncoder, "shiva", "shiva", "21CS003", "shiva123");
			syncStudent(userRepository, passwordEncoder, "shri", "shri", "21CS004", "shri123");
		};
	}

	private void syncStudent(UserRepository userRepository, PasswordEncoder passwordEncoder,
			String username, String fullName, String registerNumber, String rawPassword) {
		User user = userRepository.findByUsername(username).orElse(null);
		if (user == null) {
			userRepository.save(new User(username, fullName, registerNumber, passwordEncoder.encode(rawPassword), "ROLE_STUDENT"));
		} else {
			user.setFullName(fullName);
			user.setRegisterNumber(registerNumber);
			user.setPasswordHash(passwordEncoder.encode(rawPassword));
			user.setRole("ROLE_STUDENT");
			user.setEnabled(true);
			userRepository.save(user);
		}
	}
}
