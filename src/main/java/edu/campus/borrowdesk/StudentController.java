package edu.campus.borrowdesk;

import java.security.Principal;
import java.time.Clock;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/student")
public class StudentController {

	public static class RequestForm {
		@NotNull(message = "Please select an equipment item.")
		private Long equipmentId;

		@NotNull(message = "Due date is required.")
		private LocalDate dueDate;

		@Size(max = 250, message = "Reason cannot exceed 250 characters.")
		private String reason;

		public Long getEquipmentId() {
			return equipmentId;
		}

		public void setEquipmentId(Long equipmentId) {
			this.equipmentId = equipmentId;
		}

		public LocalDate getDueDate() {
			return dueDate;
		}

		public void setDueDate(LocalDate dueDate) {
			this.dueDate = dueDate;
		}

		public String getReason() {
			return reason;
		}

		public void setReason(String reason) {
			this.reason = reason;
		}
	}

	private final LendingService lendingService;
	private final UserRepository userRepository;
	private final Clock clock;

	public StudentController(LendingService lendingService, UserRepository userRepository, Clock clock) {
		this.lendingService = lendingService;
		this.userRepository = userRepository;
		this.clock = clock;
	}

	private User getStudentUser(Principal principal) {
		if (principal == null) {
			throw new AccessDeniedException("Unauthenticated access.");
		}
		return userRepository.findByUsername(principal.getName())
			.or(() -> userRepository.findByRegisterNumber(principal.getName()))
			.orElseThrow(() -> new AccessDeniedException("Student account not found for: " + principal.getName()));
	}

	@GetMapping("/dashboard")
	public String dashboard(Principal principal, Model model) {
		User student = getStudentUser(principal);
		String reg = student.getRegisterNumber();

		model.addAttribute("student", student);
		model.addAttribute("availableTypesCount", lendingService.equipmentCount());
		model.addAttribute("activeLoansCount", lendingService.studentActiveLoanCount(reg));
		model.addAttribute("pendingRequestsCount", lendingService.studentPendingRequestCount(reg));
		model.addAttribute("completedReturnsCount", lendingService.studentCompletedReturnCount(reg));
		model.addAttribute("recentLoans", lendingService.studentActiveLoans(reg));
		model.addAttribute("today", LocalDate.now(clock));

		return "student/dashboard";
	}

	@GetMapping("/equipment")
	public String equipment(Principal principal, Model model) {
		User student = getStudentUser(principal);
		model.addAttribute("student", student);
		model.addAttribute("stock", lendingService.listStock());
		return "student/equipment";
	}

	@GetMapping("/request")
	public String requestForm(@RequestParam(value = "equipmentId", required = false) Long equipmentId,
			Principal principal, Model model) {
		User student = getStudentUser(principal);
		RequestForm form = new RequestForm();
		if (equipmentId != null) {
			form.setEquipmentId(equipmentId);
		}
		form.setDueDate(LocalDate.now(clock).plusDays(3));

		model.addAttribute("student", student);
		model.addAttribute("form", form);
		model.addAttribute("stock", lendingService.listStock());
		return "student/request";
	}

	@PostMapping("/request")
	public String submitRequest(@ModelAttribute("form") RequestForm form, BindingResult result,
			Principal principal, Model model, RedirectAttributes redirect) {
		User student = getStudentUser(principal);

		if (form.getDueDate() != null && form.getDueDate().isBefore(LocalDate.now(clock))) {
			result.rejectValue("dueDate", "date.past", "Due date cannot be before today.");
		}

		if (!result.hasErrors()) {
			try {
				BorrowRequest request = lendingService.submitRequest(student.getRegisterNumber(),
						form.getEquipmentId(), form.getDueDate(), form.getReason());
				redirect.addFlashAttribute("message", "Borrowing request for " + request.getEquipment().getName()
						+ " submitted successfully (Status: PENDING).");
				return "redirect:/student/requests";
			}
			catch (LendingException ex) {
				result.reject("lending", ex.getMessage());
			}
		}

		model.addAttribute("student", student);
		model.addAttribute("stock", lendingService.listStock());
		return "student/request";
	}

	@GetMapping("/requests")
	public String myRequests(Principal principal, Model model) {
		User student = getStudentUser(principal);
		model.addAttribute("student", student);
		model.addAttribute("requests", lendingService.studentRequests(student.getRegisterNumber()));
		return "student/requests";
	}

	@GetMapping("/loans")
	public String myLoans(Principal principal, Model model) {
		User student = getStudentUser(principal);
		model.addAttribute("student", student);
		model.addAttribute("loans", lendingService.studentActiveLoans(student.getRegisterNumber()));
		model.addAttribute("today", LocalDate.now(clock));
		return "student/loans";
	}

	@GetMapping("/history")
	public String myHistory(Principal principal, Model model) {
		User student = getStudentUser(principal);
		model.addAttribute("student", student);
		model.addAttribute("loans", lendingService.studentHistory(student.getRegisterNumber()));
		return "student/history";
	}
}
