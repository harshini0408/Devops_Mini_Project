package edu.campus.borrowdesk;

import java.security.Principal;
import java.time.Clock;
import java.time.LocalDate;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

	private final LendingService lendingService;
	private final UserRepository userRepository;
	private final Clock clock;

	public AdminController(LendingService lendingService, UserRepository userRepository, Clock clock) {
		this.lendingService = lendingService;
		this.userRepository = userRepository;
		this.clock = clock;
	}

	@GetMapping("/dashboard")
	public String dashboard(Principal principal, Model model) {
		model.addAttribute("adminUser", principal != null ? principal.getName() : "Admin");
		model.addAttribute("equipmentCount", lendingService.equipmentCount());
		model.addAttribute("totalUnits", lendingService.totalEquipmentUnits());
		model.addAttribute("availableUnits", lendingService.totalAvailableUnits());
		model.addAttribute("activeLoanCount", lendingService.activeLoanCount());
		model.addAttribute("pendingRequestCount", lendingService.pendingRequestCount());
		model.addAttribute("overdueCount", lendingService.overdueLoanCount());
		model.addAttribute("pendingRequests", lendingService.pendingRequests());
		model.addAttribute("activeLoans", lendingService.activeLoans());
		model.addAttribute("today", LocalDate.now(clock));
		return "admin/dashboard";
	}

	@GetMapping("/equipment")
	public String equipment(Model model) {
		model.addAttribute("stock", lendingService.listStock());
		return "admin/equipment";
	}

	@PostMapping("/equipment")
	public String addEquipment(@RequestParam("name") String name,
			@RequestParam("totalQuantity") int totalQuantity,
			RedirectAttributes redirect) {
		try {
			Equipment eq = lendingService.addEquipment(name, totalQuantity);
			redirect.addFlashAttribute("message", "Equipment '" + eq.getName() + "' added successfully ("
					+ eq.getTotalQuantity() + " units).");
		}
		catch (LendingException ex) {
			redirect.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/admin/equipment";
	}

	@PostMapping("/equipment/{id}/update")
	public String updateEquipment(@PathVariable("id") Long id,
			@RequestParam("name") String name,
			@RequestParam("totalQuantity") int totalQuantity,
			RedirectAttributes redirect) {
		try {
			Equipment eq = lendingService.updateEquipment(id, name, totalQuantity);
			redirect.addFlashAttribute("message", "Equipment '" + eq.getName() + "' updated successfully ("
					+ eq.getTotalQuantity() + " total units).");
		}
		catch (LendingException ex) {
			redirect.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/admin/equipment";
	}

	@GetMapping("/requests")
	public String requests(Model model) {
		model.addAttribute("requests", lendingService.pendingRequests());
		model.addAttribute("today", LocalDate.now(clock));
		return "admin/requests";
	}

	@PostMapping("/requests/{id}/approve")
	public String approveRequest(@PathVariable("id") Long id, RedirectAttributes redirect) {
		try {
			Loan loan = lendingService.approveRequest(id);
			redirect.addFlashAttribute("message", "Request approved. " + loan.getEquipment().getName()
					+ " issued to " + loan.getStudentName() + " (" + loan.getRegisterNumber() + ").");
		}
		catch (LendingException ex) {
			redirect.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/admin/requests";
	}

	@PostMapping("/requests/{id}/reject")
	public String rejectRequest(@PathVariable("id") Long id,
			@RequestParam(value = "reason", required = false) String reason,
			RedirectAttributes redirect) {
		try {
			BorrowRequest req = lendingService.rejectRequest(id, reason);
			redirect.addFlashAttribute("message", "Request #" + id + " for " + req.getEquipment().getName()
					+ " by " + req.getStudentName() + " was rejected.");
		}
		catch (LendingException ex) {
			redirect.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/admin/requests";
	}

	@GetMapping("/loans")
	public String activeLoans(Model model) {
		model.addAttribute("loans", lendingService.activeLoans());
		model.addAttribute("today", LocalDate.now(clock));
		return "admin/loans";
	}

	@PostMapping("/loans/{id}/return")
	public String returnLoan(@PathVariable("id") Long id, RedirectAttributes redirect) {
		try {
			Loan loan = lendingService.returnLoan(id);
			redirect.addFlashAttribute("message", loan.getEquipment().getName() + " returned by "
					+ loan.getStudentName() + ".");
		}
		catch (LendingException ex) {
			redirect.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/admin/loans";
	}

	@GetMapping("/history")
	public String history(Model model) {
		model.addAttribute("loans", lendingService.recentHistory());
		return "admin/history";
	}
}
