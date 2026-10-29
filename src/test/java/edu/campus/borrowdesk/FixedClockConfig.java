package edu.campus.borrowdesk;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Freezes "today" for tests so date rules never depend on the real calendar. */
@TestConfiguration
public class FixedClockConfig {

	public static final LocalDate TODAY = LocalDate.of(2030, 6, 15);

	@Bean
	@Primary
	Clock fixedClock() {
		return Clock.fixed(Instant.parse("2030-06-15T10:00:00Z"), ZoneOffset.UTC);
	}
}
