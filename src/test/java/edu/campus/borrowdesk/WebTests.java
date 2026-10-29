package edu.campus.borrowdesk;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

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
class WebTests {

	@Autowired
	WebApplicationContext context;

	@Autowired
	UserRepository userRepository;

	@Autowired
	BorrowRequestRepository borrowRequestRepository;

	@Autowired
	LoanRepository loanRepository;

	@Autowired
	EquipmentRepository equipmentRepository;

	@Autowired
	org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

	MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
		borrowRequestRepository.deleteAll();
		loanRepository.deleteAll();
		equipmentRepository.deleteAll();
		userRepository.deleteAll();

		userRepository.save(new User("student", "Akshitha Sharma", "21CS001",
				passwordEncoder.encode("student123"), "ROLE_STUDENT"));
		userRepository.save(new User("admin", "Lab Administrator", "ADMIN001",
				passwordEncoder.encode("admin123"), "ROLE_ADMIN"));
	}

	@Test
	void healthReturnsMinimalStatusWithoutAuth() throws Exception {
		mvc.perform(get("/health"))
			.andExpect(status().isOk())
			.andExpect(content().json("{\"status\":\"UP\"}", true));
	}

	@Test
	void loginPageRendersWithoutAuth() throws Exception {
		mvc.perform(get("/login"))
			.andExpect(status().isOk())
			.andExpect(view().name("login"));
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void adminPagesRender() throws Exception {
		for (String path : new String[] { "/admin/dashboard", "/admin/equipment", "/admin/requests", "/admin/loans", "/admin/history", "/equipment", "/issue", "/loans", "/history" }) {
			mvc.perform(get(path)).andExpect(status().isOk());
		}
	}

	@Test
	@WithMockUser(username = "student", roles = "STUDENT")
	void studentPagesRender() throws Exception {
		for (String path : new String[] { "/student/dashboard", "/student/equipment", "/student/request", "/student/requests", "/student/loans", "/student/history" }) {
			mvc.perform(get(path)).andExpect(status().isOk());
		}
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void blankIssueFormShowsFriendlyErrors() throws Exception {
		mvc.perform(post("/issue").with(csrf()).param("studentName", "").param("registerNumber", ""))
			.andExpect(status().isOk())
			.andExpect(view().name("issue"))
			.andExpect(model().attributeHasFieldErrors("form", "studentName", "registerNumber", "equipmentId",
					"dueDate"));
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void returnRequiresPost() throws Exception {
		mvc.perform(get("/loans/1/return")).andExpect(status().is4xxClientError());
	}
}
