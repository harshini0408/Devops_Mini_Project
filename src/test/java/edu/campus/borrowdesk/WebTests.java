package edu.campus.borrowdesk;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Import(FixedClockConfig.class)
class WebTests {

	@Autowired
	WebApplicationContext context;

	MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).build();
	}

	@Test
	void healthReturnsMinimalStatus() throws Exception {
		mvc.perform(get("/health")).andExpect(status().isOk()).andExpect(content().json("{\"status\":\"UP\"}", true));
	}

	@Test
	void pagesRender() throws Exception {
		for (String path : new String[] { "/", "/equipment", "/issue", "/loans", "/history" }) {
			mvc.perform(get(path)).andExpect(status().isOk());
		}
	}

	@Test
	void blankIssueFormShowsFriendlyErrors() throws Exception {
		mvc.perform(post("/issue").param("studentName", "").param("registerNumber", ""))
			.andExpect(status().isOk())
			.andExpect(view().name("issue"))
			.andExpect(model().attributeHasFieldErrors("form", "studentName", "registerNumber", "equipmentId",
					"dueDate"));
	}

	@Test
	void returnRequiresPost() throws Exception {
		mvc.perform(get("/loans/1/return")).andExpect(status().is4xxClientError());
	}
}
