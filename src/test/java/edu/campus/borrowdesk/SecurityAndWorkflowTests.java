package edu.campus.borrowdesk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.logout;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Import(FixedClockConfig.class)
class SecurityAndWorkflowTests {

	private static final LocalDate TODAY = FixedClockConfig.TODAY;

	@Autowired
	WebApplicationContext context;

	@Autowired
	EquipmentRepository equipmentRepository;

	@Autowired
	LoanRepository loanRepository;

	@Autowired
	BorrowRequestRepository borrowRequestRepository;

	@Autowired
	UserRepository userRepository;

	@Autowired
	org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

	MockMvc mvc;
	Equipment projector;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

		borrowRequestRepository.deleteAll();
		loanRepository.deleteAll();
		equipmentRepository.deleteAll();
		userRepository.deleteAll();

		userRepository.save(new User("admin", "Lab Administrator", "ADMIN001",
				passwordEncoder.encode("admin123"), "ROLE_ADMIN"));
		userRepository.save(new User("akshita", "akshita", "21CS001",
				passwordEncoder.encode("akshita123"), "ROLE_STUDENT"));
		userRepository.save(new User("abi", "abi", "21CS002",
				passwordEncoder.encode("abi123"), "ROLE_STUDENT"));

		projector = equipmentRepository.save(new Equipment("Lab Projector", 2));
	}

	@Test
	void validStudentLoginRedirectsToStudentDashboard() throws Exception {
		mvc.perform(formLogin().user("akshita").password("akshita123"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/student/dashboard"));
	}

	@Test
	void validAdminLoginRedirectsToAdminDashboard() throws Exception {
		mvc.perform(formLogin().user("admin").password("admin123"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/admin/dashboard"));
	}

	@Test
	void invalidCredentialsRedirectsToLoginError() throws Exception {
		mvc.perform(formLogin().user("akshita").password("wrong-password"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/login?error"));
	}

	@Test
	void logoutInvalidatesSessionAndRedirectsToLogin() throws Exception {
		mvc.perform(logout())
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/login?logout"));
	}

	@Test
	void unauthenticatedAccessToProtectedRoutesRedirectsToLogin() throws Exception {
		mvc.perform(get("/admin/dashboard"))
			.andExpect(status().is3xxRedirection())
			.andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/login")));

		mvc.perform(get("/student/dashboard"))
			.andExpect(status().is3xxRedirection())
			.andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/login")));
	}

	@Test
	@WithMockUser(username = "akshita", roles = "STUDENT")
	void studentCannotAccessAdminRoutes() throws Exception {
		mvc.perform(get("/admin/dashboard")).andExpect(status().isForbidden());
		mvc.perform(get("/admin/equipment")).andExpect(status().isForbidden());
		mvc.perform(get("/admin/requests")).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "akshita", roles = "STUDENT")
	void studentCannotApproveOrRejectRequests() throws Exception {
		mvc.perform(post("/admin/requests/1/approve").with(csrf())).andExpect(status().isForbidden());
		mvc.perform(post("/admin/requests/1/reject").with(csrf())).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "akshita", roles = "STUDENT")
	void studentSubmitRequestWorkflow() throws Exception {
		// Valid request
		mvc.perform(post("/student/request")
				.with(csrf())
				.param("equipmentId", projector.getId().toString())
				.param("dueDate", TODAY.plusDays(3).toString())
				.param("reason", "Project demo"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/student/requests"));

		List<BorrowRequest> reqs = borrowRequestRepository.findByRegisterNumberOrderByRequestedDateDescIdDesc("21CS001");
		assertThat(reqs).hasSize(1);
		assertThat(reqs.get(0).getStatus()).isEqualTo(BorrowRequest.STATUS_PENDING);
		assertThat(reqs.get(0).getEquipment().getId()).isEqualTo(projector.getId());
	}

	@Test
	@WithMockUser(username = "akshita", roles = "STUDENT")
	void studentPastDueDateRequestRejectedWithFriendlyError() throws Exception {
		mvc.perform(post("/student/request")
				.with(csrf())
				.param("equipmentId", projector.getId().toString())
				.param("dueDate", TODAY.minusDays(1).toString()))
			.andExpect(status().isOk())
			.andExpect(model().attributeHasFieldErrors("form", "dueDate"));
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void adminApproveAndReturnWorkflow() throws Exception {
		BorrowRequest req = borrowRequestRepository.save(
			new BorrowRequest(projector, "akshita", "21CS001", TODAY, TODAY.plusDays(2), "Lab work")
		);

		// Admin approves
		mvc.perform(post("/admin/requests/" + req.getId() + "/approve").with(csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/admin/requests"));

		BorrowRequest updatedReq = borrowRequestRepository.findById(req.getId()).get();
		assertThat(updatedReq.getStatus()).isEqualTo(BorrowRequest.STATUS_APPROVED);

		List<Loan> active = loanRepository.findByReturnedDateIsNullOrderByDueDateAscIdAsc();
		assertThat(active).hasSize(1);
		Loan loan = active.get(0);

		// Admin returns loan
		mvc.perform(post("/admin/loans/" + loan.getId() + "/return").with(csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/admin/loans"));

		Loan returned = loanRepository.findById(loan.getId()).get();
		assertThat(returned.getReturnedDate()).isEqualTo(TODAY);
	}

	@Test
	@WithMockUser(username = "akshita", roles = "STUDENT")
	void studentOnlyViewsOwnData() throws Exception {
		// Create request for akshita (21CS001) and abi (21CS002)
		borrowRequestRepository.save(new BorrowRequest(projector, "akshita", "21CS001", TODAY, TODAY.plusDays(2), "Mine"));
		borrowRequestRepository.save(new BorrowRequest(projector, "abi", "21CS002", TODAY, TODAY.plusDays(2), "Other"));

		// student visits /student/requests
		mvc.perform(get("/student/requests"))
			.andExpect(status().isOk())
			.andExpect(model().attribute("requests", org.hamcrest.Matchers.hasSize(1)));
	}
}
