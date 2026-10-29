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

	@Autowired
	BorrowRequestRepository borrowRequestRepository;

	@Autowired
	UserRepository userRepository;

	Equipment projector;
	User studentUser;

	@BeforeEach
	void setUp() {
		borrowRequestRepository.deleteAll();
		loanRepository.deleteAll();
		equipmentRepository.deleteAll();
		userRepository.deleteAll();

		projector = equipmentRepository.save(new Equipment("Test Projector", 1));
		studentUser = userRepository.save(new User("asha", "Asha Sharma", "21CS001", "hash", "ROLE_STUDENT"));
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

	@Test
	void adminCanAddAndEditEquipment() {
		Equipment eq = service.addEquipment("Digital Oscilloscope", 5);
		assertThat(eq.getId()).isNotNull();
		assertThat(eq.getName()).isEqualTo("Digital Oscilloscope");
		assertThat(eq.getTotalQuantity()).isEqualTo(5);

		// Duplicate equipment name rejection
		assertThatThrownBy(() -> service.addEquipment("digital oscilloscope", 2))
			.isInstanceOf(LendingException.class)
			.hasMessageContaining("already exists");

		// Edit equipment
		Equipment updated = service.updateEquipment(eq.getId(), "Digital Oscilloscope Pro", 8);
		assertThat(updated.getName()).isEqualTo("Digital Oscilloscope Pro");
		assertThat(updated.getTotalQuantity()).isEqualTo(8);
	}

	@Test
	void cannotReduceTotalQuantityBelowActiveLoans() {
		Equipment mult = service.addEquipment("Multimeter", 2);
		service.issue("Asha", "21CS001", mult.getId(), TODAY.plusDays(2));
		service.issue("Ben", "21CS002", mult.getId(), TODAY.plusDays(2));

		assertThatThrownBy(() -> service.updateEquipment(mult.getId(), "Multimeter", 1))
			.isInstanceOf(LendingException.class)
			.hasMessageContaining("active loans");
	}

	@Test
	void studentBorrowRequestLifecycle() {
		// 1. Submit request
		BorrowRequest req = service.submitRequest("21CS001", projector.getId(), TODAY.plusDays(3), "Lab experiment");
		assertThat(req.getId()).isNotNull();
		assertThat(req.getStatus()).isEqualTo(BorrowRequest.STATUS_PENDING);
		assertThat(service.pendingRequestCount()).isEqualTo(1);
		// Pending request does NOT reduce availability until approved
		assertThat(available()).isEqualTo(1);

		// 2. Approve request
		Loan loan = service.approveRequest(req.getId());
		assertThat(loan.getId()).isNotNull();
		assertThat(loan.getEquipment().getId()).isEqualTo(projector.getId());
		assertThat(available()).isZero();
		assertThat(service.pendingRequestCount()).isZero();

		// Duplicate approval rejected
		assertThatThrownBy(() -> service.approveRequest(req.getId()))
			.isInstanceOf(LendingException.class)
			.hasMessageContaining("already been processed");

		// 3. Return loan restores availability
		service.returnLoan(loan.getId());
		assertThat(available()).isEqualTo(1);
	}

	@Test
	void adminRejectionDoesNotAlterAvailability() {
		BorrowRequest req = service.submitRequest("21CS001", projector.getId(), TODAY.plusDays(2), "Project work");
		assertThat(available()).isEqualTo(1);

		BorrowRequest rejected = service.rejectRequest(req.getId(), "Reserved for semester exams");
		assertThat(rejected.getStatus()).isEqualTo(BorrowRequest.STATUS_REJECTED);
		assertThat(rejected.getAdminDecision()).isEqualTo("Reserved for semester exams");
		assertThat(available()).isEqualTo(1);
		assertThat(service.activeLoanCount()).isZero();
	}

	@Test
	void cannotApproveWhenStockBecomesExhausted() {
		// Create request when available
		BorrowRequest req = service.submitRequest("21CS001", projector.getId(), TODAY.plusDays(2), "Class demo");

		// Stock gets taken by direct issue or another approval
		service.issue("Other Student", "21CS999", projector.getId(), TODAY.plusDays(1));
		assertThat(available()).isZero();

		// Approving the pending request must fail
		assertThatThrownBy(() -> service.approveRequest(req.getId()))
			.isInstanceOf(LendingException.class)
			.hasMessageContaining("available");
	}
}
