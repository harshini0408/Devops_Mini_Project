package edu.campus.borrowdesk;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

	@GetMapping("/login")
	public String login(Authentication authentication) {
		if (authentication != null && authentication.isAuthenticated()
				&& !"anonymousUser".equals(authentication.getPrincipal())) {
			for (GrantedAuthority authority : authentication.getAuthorities()) {
				if ("ROLE_ADMIN".equals(authority.getAuthority())) {
					return "redirect:/admin/dashboard";
				}
				if ("ROLE_STUDENT".equals(authority.getAuthority())) {
					return "redirect:/student/dashboard";
				}
			}
		}
		return "login";
	}
}
