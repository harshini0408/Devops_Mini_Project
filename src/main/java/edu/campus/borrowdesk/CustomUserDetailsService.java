package edu.campus.borrowdesk;

import java.util.Collections;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	public CustomUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String usernameOrRegister) throws UsernameNotFoundException {
		User user = userRepository.findByUsername(usernameOrRegister)
			.or(() -> userRepository.findByRegisterNumber(usernameOrRegister))
			.orElseThrow(() -> new UsernameNotFoundException("User not found with identifier: " + usernameOrRegister));

		return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPasswordHash(),
				user.isEnabled(), true, true, true,
				Collections.singletonList(new SimpleGrantedAuthority(user.getRole())));
	}
}
