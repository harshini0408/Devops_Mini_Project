package edu.campus.borrowdesk;

import java.time.Clock;
import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class BorrowDeskController {

	/** Form-backing object for the issue page. */
	public static class IssueForm {

		@NotBlank(message = "Student name is required.")
		@Size(max = 100, message = "Student name is too long.")
		private String studentName;

		@NotBlank(message = "Register number is required.")
		@Size(max = 30, message = "Register number is too long.")
		private String registerNumber;

		@NotNull(message = "Please choose an equipment item.")
		private Long equipmentId;

		@NotNull(message = "Due date is required.")
		private LocalDate dueDate;

		public String getStudentName() {
			return studentName;
		}

		public void setStudentName(String studentName) {
			this.studentName = studentName;
		}

		public String getRegisterNumber() {
			return registerNumber;
		}

		public void setRegisterNumber(String registerNumber) {
			this.registerNumber = registerNumber;
		}

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
	}

	private final LendingService lendingService;

	private final Clock clock;

	public BorrowDeskController(LendingService lendingService, Clock clock) {
		this.lendingService = lendingService;
		this.clock = clock;
	}

	@GetMapping("/")
	public String dashboard(Model model) {
		model.addAttribute("equipmentCount", lendingService.equipmentCount());
		model.addAttribute("activeLoanCount", lendingService.activeLoanCount());
		return "dashboard";
	}

	@GetMapping("/equipment")
	public String equipment(Model model) {
		model.addAttribute("stock", lendingService.listStock());
		return "equipment";
	}

	@GetMapping("/issue")
	public String issueForm(@ModelAttribute("form") IssueForm form, Model model) {
		model.addAttribute("stock", lendingService.listStock());
		return "issue";
	}

	@PostMapping("/issue")
	public String issue(@Valid @ModelAttribute("form") IssueForm form, BindingResult result, Model model,
			RedirectAttributes redirect) {
		if (!result.hasErrors()) {
			try {
				Loan loan = lendingService.issue(form.getStudentName(), form.getRegisterNumber(),
						form.getEquipmentId(), form.getDueDate());
				redirect.addFlashAttribute("message",
						loan.getEquipment().getName() + " issued to " + loan.getStudentName() + ".");
				return "redirect:/loans";
			}
			catch (LendingException ex) {
				result.reject("lending", ex.getMessage());
			}
		}
		model.addAttribute("stock", lendingService.listStock());
		return "issue";
	}

	@GetMapping("/loans")
	public String loans(Model model) {
		model.addAttribute("loans", lendingService.activeLoans());
		model.addAttribute("today", LocalDate.now(clock));
		return "loans";
	}

	@PostMapping("/loans/{id}/return")
	public String returnLoan(@PathVariable Long id, RedirectAttributes redirect) {
		try {
			Loan loan = lendingService.returnLoan(id);
			redirect.addFlashAttribute("message", loan.getEquipment().getName() + " returned by "
					+ loan.getStudentName() + ".");
		}
		catch (LendingException ex) {
			redirect.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/loans";
	}

	@GetMapping("/history")
	public String history(Model model) {
		model.addAttribute("loans", lendingService.recentHistory());
		return "history";
	}
}
