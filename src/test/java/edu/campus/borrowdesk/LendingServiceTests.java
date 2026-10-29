package edu.campus.borrowdesk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(FixedClockConfig.class)
class LendingServiceTests {

	private static final LocalDate TODAY = FixedClockConfig.TODAY;

	@Autowired
	LendingService service;

	@Autowired
	EquipmentRepository equipmentRepository;

	@Autowired
	LoanRepository loanRepository;

	Equipment projector;

	@BeforeEach
	void setUp() {
		loanRepository.deleteAll();
		equipmentRepository.deleteAll();
		projector = equipmentRepository.save(new Equipment("Test Projector", 1));
	}

	private long available() {
		return service.listStock().stream().filter(s -> s.id().equals(projector.getId())).findFirst().get().available();
	}

	@Test
	void issuesAvailableEquipment() {
		Loan loan = service.issue(" Asha ", " 21CS001 ", projector.getId(), TODAY.plusDays(3));

		assertThat(loan.getId()).isNotNull();
		assertThat(loan.getStudentName()).isEqualTo("Asha");
		assertThat(loan.getRegisterNumber()).isEqualTo("21CS001");
		assertThat(loan.getIssuedDate()).isEqualTo(TODAY);
		assertThat(loan.getDueDate()).isEqualTo(TODAY.plusDays(3));
		assertThat(loan.isActive()).isTrue();
		assertThat(available()).isZero();
		assertThat(service.activeLoanCount()).isEqualTo(1);
	}

	@Test
	void dueDateOfTodayIsAllowed() {
		assertThat(service.issue("Asha", "21CS001", projector.getId(), TODAY).getDueDate()).isEqualTo(TODAY);
	}

	@Test
	void rejectsIssueWhenStockIsExhausted() {
		service.issue("Asha", "21CS001", projector.getId(), TODAY);

		assertThatThrownBy(() -> service.issue("Ben", "21CS002", projector.getId(), TODAY))
			.isInstanceOf(LendingException.class)
			.hasMessageContaining("available");
		assertThat(service.activeLoanCount()).isEqualTo(1);
		assertThat(available()).isZero();
	}

	@Test
	void rejectsBlankStudentDetails() {
		LocalDate due = TODAY.plusDays(1);
		assertThatThrownBy(() -> service.issue("   ", "R1", projector.getId(), due))
			.isInstanceOf(LendingException.class)
			.hasMessageContaining("Student name");
		assertThatThrownBy(() -> service.issue(null, "R1", projector.getId(), due))
			.isInstanceOf(LendingException.class);
		assertThatThrownBy(() -> service.issue("Asha", " ", projector.getId(), due))
			.isInstanceOf(LendingException.class)
			.hasMessageContaining("Register number");
		assertThat(service.activeLoanCount()).isZero();
	}

	@Test
	void rejectsPastDueDate() {
		assertThatThrownBy(() -> service.issue("Asha", "R1", projector.getId(), TODAY.minusDays(1)))
			.isInstanceOf(LendingException.class)
			.hasMessageContaining("before today");
		assertThat(service.activeLoanCount()).isZero();
	}

	@Test
	void rejectsUnknownEquipment() {
		assertThatThrownBy(() -> service.issue("Asha", "R1", 999_999L, TODAY))
			.isInstanceOf(LendingException.class)
			.hasMessageContaining("does not exist");
	}

	@Test
	void returnRestoresAvailability() {
		Loan loan = service.issue("Asha", "21CS001", projector.getId(), TODAY.plusDays(2));
		assertThat(available()).isZero();

		Loan returned = service.returnLoan(loan.getId());

		assertThat(returned.getReturnedDate()).isEqualTo(TODAY);
		assertThat(available()).isEqualTo(1);
		assertThat(service.activeLoanCount()).isZero();
		assertThat(service.recentHistory()).hasSize(1);
	}

	@Test
	void repeatedReturnDoesNotIncreaseStockAgain() {
		Loan loan = service.issue("Asha", "21CS001", projector.getId(), TODAY);
		service.returnLoan(loan.getId());

		assertThatThrownBy(() -> service.returnLoan(loan.getId())).isInstanceOf(LendingException.class)
			.hasMessageContaining("already returned");

		assertThat(available()).isEqualTo(1);
		assertThat(equipmentRepository.findById(projector.getId()).get().getTotalQuantity()).isEqualTo(1);
	}

	@Test
	void returningUnknownLoanIsRejected() {
		assertThatThrownBy(() -> service.returnLoan(999_999L)).isInstanceOf(LendingException.class)
			.hasMessageContaining("does not exist");
	}
}
