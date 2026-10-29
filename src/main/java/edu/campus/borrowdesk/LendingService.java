package edu.campus.borrowdesk;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** All lending, equipment management, and request approval rules live here. */
@Service
public class LendingService {

	/** Equipment with its derived availability. */
	public record Stock(Long id, String name, int totalQuantity, long available) {
	}

	private final EquipmentRepository equipmentRepository;

	private final LoanRepository loanRepository;

	private final BorrowRequestRepository borrowRequestRepository;

	private final UserRepository userRepository;

	private final Clock clock;

	public LendingService(EquipmentRepository equipmentRepository, LoanRepository loanRepository,
			BorrowRequestRepository borrowRequestRepository, UserRepository userRepository, Clock clock) {
		this.equipmentRepository = equipmentRepository;
		this.loanRepository = loanRepository;
		this.borrowRequestRepository = borrowRequestRepository;
		this.userRepository = userRepository;
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
	public long totalEquipmentUnits() {
		return equipmentRepository.findAll().stream().mapToInt(Equipment::getTotalQuantity).sum();
	}

	@Transactional(readOnly = true)
	public long totalAvailableUnits() {
		return listStock().stream().mapToLong(Stock::available).sum();
	}

	@Transactional(readOnly = true)
	public long activeLoanCount() {
		return loanRepository.countByReturnedDateIsNull();
	}

	@Transactional(readOnly = true)
	public long overdueLoanCount() {
		return loanRepository.countByReturnedDateIsNullAndDueDateBefore(LocalDate.now(clock));
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

	@Transactional
	public Equipment addEquipment(String name, int totalQuantity) {
		String trimmed = name == null ? "" : name.trim();
		if (trimmed.isEmpty()) {
			throw new LendingException("Equipment name is required.");
		}
		if (totalQuantity <= 0) {
			throw new LendingException("Total quantity must be at least 1.");
		}
		if (equipmentRepository.findByNameIgnoreCase(trimmed).isPresent()) {
			throw new LendingException("Equipment with name '" + trimmed + "' already exists.");
		}
		return equipmentRepository.save(new Equipment(trimmed, totalQuantity));
	}

	@Transactional
	public Equipment updateEquipment(Long id, String name, int totalQuantity) {
		if (id == null) {
			throw new LendingException("Equipment ID is required.");
		}
		String trimmed = name == null ? "" : name.trim();
		if (trimmed.isEmpty()) {
			throw new LendingException("Equipment name is required.");
		}
		Equipment equipment = equipmentRepository.findForUpdate(id)
			.orElseThrow(() -> new LendingException("That equipment item does not exist."));

		equipmentRepository.findByNameIgnoreCase(trimmed)
			.filter(existing -> !existing.getId().equals(id))
			.ifPresent(existing -> {
				throw new LendingException("Another equipment item already uses name '" + trimmed + "'.");
			});

		long active = loanRepository.countByEquipmentAndReturnedDateIsNull(equipment);
		if (totalQuantity < active) {
			throw new LendingException("Total quantity (" + totalQuantity
					+ ") cannot be reduced below currently active loans (" + active + ").");
		}
		if (totalQuantity < 0) {
			throw new LendingException("Total quantity cannot be negative.");
		}

		equipment.setName(trimmed);
		equipment.setTotalQuantity(totalQuantity);
		return equipment;
	}

	@Transactional
	public BorrowRequest submitRequest(String registerNumber, Long equipmentId, LocalDate dueDate, String reason) {
		String reg = registerNumber == null ? "" : registerNumber.trim();
		if (reg.isEmpty()) {
			throw new LendingException("Register number is required.");
		}
		User student = userRepository.findByRegisterNumber(reg)
			.orElseThrow(() -> new LendingException("Student account with register number '" + reg + "' not found."));

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

		Equipment equipment = equipmentRepository.findById(equipmentId)
			.orElseThrow(() -> new LendingException("That equipment item does not exist."));

		long active = loanRepository.countByEquipmentAndReturnedDateIsNull(equipment);
		if (active >= equipment.getTotalQuantity()) {
			throw new LendingException("No " + equipment.getName() + " is currently available for borrowing.");
		}

		String cleanReason = reason == null ? "" : reason.trim();
		BorrowRequest request = new BorrowRequest(equipment, student.getFullName(), student.getRegisterNumber(),
				today, dueDate, cleanReason);
		return borrowRequestRepository.save(request);
	}

	@Transactional
	public Loan approveRequest(Long requestId) {
		BorrowRequest request = borrowRequestRepository.findForUpdate(requestId)
			.orElseThrow(() -> new LendingException("Borrow request does not exist."));

		if (!request.isPending()) {
			throw new LendingException("Borrow request #" + requestId + " has already been processed ("
					+ request.getStatus() + ").");
		}

		Equipment equipment = equipmentRepository.findForUpdate(request.getEquipment().getId())
			.orElseThrow(() -> new LendingException("Equipment item does not exist."));

		long active = loanRepository.countByEquipmentAndReturnedDateIsNull(equipment);
		if (active >= equipment.getTotalQuantity()) {
			throw new LendingException("Cannot approve: No " + equipment.getName() + " is currently available.");
		}

		LocalDate today = LocalDate.now(clock);
		Loan loan = loanRepository.save(new Loan(equipment, request.getStudentName(), request.getRegisterNumber(),
				today, request.getDueDate()));

		request.setStatus(BorrowRequest.STATUS_APPROVED);
		request.setDecidedAt(today);
		request.setAdminDecision("Approved and equipment issued.");

		return loan;
	}

	@Transactional
	public BorrowRequest rejectRequest(Long requestId, String reason) {
		BorrowRequest request = borrowRequestRepository.findForUpdate(requestId)
			.orElseThrow(() -> new LendingException("Borrow request does not exist."));

		if (!request.isPending()) {
			throw new LendingException("Borrow request #" + requestId + " has already been processed ("
					+ request.getStatus() + ").");
		}

		request.setStatus(BorrowRequest.STATUS_REJECTED);
		request.setDecidedAt(LocalDate.now(clock));
		request.setAdminDecision(reason == null || reason.isBlank() ? "Rejected by lab administrator." : reason.trim());

		return request;
	}

	@Transactional(readOnly = true)
	public List<BorrowRequest> pendingRequests() {
		return borrowRequestRepository.findByStatusOrderByRequestedDateDescIdDesc(BorrowRequest.STATUS_PENDING);
	}

	@Transactional(readOnly = true)
	public long pendingRequestCount() {
		return borrowRequestRepository.countByStatus(BorrowRequest.STATUS_PENDING);
	}

	@Transactional(readOnly = true)
	public List<BorrowRequest> studentRequests(String registerNumber) {
		return borrowRequestRepository.findByRegisterNumberOrderByRequestedDateDescIdDesc(registerNumber);
	}

	@Transactional(readOnly = true)
	public List<Loan> studentActiveLoans(String registerNumber) {
		return loanRepository.findByRegisterNumberAndReturnedDateIsNullOrderByDueDateAsc(registerNumber);
	}

	@Transactional(readOnly = true)
	public List<Loan> studentHistory(String registerNumber) {
		return loanRepository.findByRegisterNumberAndReturnedDateIsNotNullOrderByReturnedDateDesc(registerNumber);
	}

	@Transactional(readOnly = true)
	public long studentActiveLoanCount(String registerNumber) {
		return loanRepository.countByRegisterNumberAndReturnedDateIsNull(registerNumber);
	}

	@Transactional(readOnly = true)
	public long studentPendingRequestCount(String registerNumber) {
		return borrowRequestRepository.countByRegisterNumberAndStatus(registerNumber, BorrowRequest.STATUS_PENDING);
	}

	@Transactional(readOnly = true)
	public long studentCompletedReturnCount(String registerNumber) {
		return loanRepository.countByRegisterNumberAndReturnedDateIsNotNull(registerNumber);
	}

	private Stock toStock(Equipment e) {
		long active = loanRepository.countByEquipmentAndReturnedDateIsNull(e);
		return new Stock(e.getId(), e.getName(), e.getTotalQuantity(), Math.max(0, e.getTotalQuantity() - active));
	}
}
