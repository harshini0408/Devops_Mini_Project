package edu.campus.borrowdesk;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class RoleBasedSuccessHandler implements AuthenticationSuccessHandler {

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		for (GrantedAuthority authority : authentication.getAuthorities()) {
			String role = authority.getAuthority();
			if ("ROLE_ADMIN".equals(role)) {
				response.sendRedirect("/admin/dashboard");
				return;
			}
			if ("ROLE_STUDENT".equals(role)) {
				response.sendRedirect("/student/dashboard");
				return;
			}
		}
		response.sendRedirect("/");
	}
}
