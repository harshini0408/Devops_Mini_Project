package edu.campus.borrowdesk;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByUsername(String username);

	Optional<User> findByRegisterNumber(String registerNumber);

	boolean existsByUsername(String username);

	boolean existsByRegisterNumber(String registerNumber);
}
