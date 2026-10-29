package edu.campus.borrowdesk;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** All lending rules and transactions live here. */
@Service
public class LendingService {

	/** Equipment with its derived availability. */
	public record Stock(Long id, String name, int totalQuantity, long available) {
	}

	private final EquipmentRepository equipmentRepository;

	private final LoanRepository loanRepository;

	private final Clock clock;

	public LendingService(EquipmentRepository equipmentRepository, LoanRepository loanRepository, Clock clock) {
		this.equipmentRepository = equipmentRepository;
		this.loanRepository = loanRepository;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public List<Stock> listStock() {
		return equipmentRepository.findAllByOrderByNameAsc().stream().map(this::toStock).toList();
	}

	@Transactional(readOnly = true)
	public long equipmentCount() {
		return equipmentRepository.count();
	}

	@Transactional(readOnly = true)
	public long activeLoanCount() {
		return loanRepository.countByReturnedDateIsNull();
	}

	@Transactional(readOnly = true)
	public List<Loan> activeLoans() {
		return loanRepository.findByReturnedDateIsNullOrderByDueDateAscIdAsc();
	}

	@Transactional(readOnly = true)
	public List<Loan> recentHistory() {
		return loanRepository.findTop50ByOrderByIdDesc();
	}

	@Transactional
	public Loan issue(String studentName, String registerNumber, Long equipmentId, LocalDate dueDate) {
		String name = studentName == null ? "" : studentName.trim();
		String register = registerNumber == null ? "" : registerNumber.trim();
		if (name.isEmpty()) {
			throw new LendingException("Student name is required.");
		}
		if (register.isEmpty()) {
			throw new LendingException("Register number is required.");
		}
		if (equipmentId == null) {
			throw new LendingException("Please choose an equipment item.");
		}
		LocalDate today = LocalDate.now(clock);
		if (dueDate == null) {
			throw new LendingException("Due date is required.");
		}
		if (dueDate.isBefore(today)) {
			throw new LendingException("Due date cannot be before today.");
		}
		Equipment equipment = equipmentRepository.findForUpdate(equipmentId)
			.orElseThrow(() -> new LendingException("That equipment item does not exist."));
		long active = loanRepository.countByEquipmentAndReturnedDateIsNull(equipment);
		if (active >= equipment.getTotalQuantity()) {
			throw new LendingException("No " + equipment.getName() + " is currently available.");
		}
		return loanRepository.save(new Loan(equipment, name, register, today, dueDate));
	}

	@Transactional
	public Loan returnLoan(Long loanId) {
		Loan loan = loanRepository.findForUpdate(loanId)
			.orElseThrow(() -> new LendingException("That loan does not exist."));
		if (!loan.isActive()) {
			throw new LendingException("This loan was already returned.");
		}
		loan.setReturnedDate(LocalDate.now(clock));
		return loan;
	}

	private Stock toStock(Equipment e) {
		long active = loanRepository.countByEquipmentAndReturnedDateIsNull(e);
		return new Stock(e.getId(), e.getName(), e.getTotalQuantity(), Math.max(0, e.getTotalQuantity() - active));
	}
}
