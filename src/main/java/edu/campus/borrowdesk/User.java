package edu.campus.borrowdesk;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** User account with role-based access control. */
@Entity
@Table(name = "app_user")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 60)
	private String username;

	@Column(nullable = false, length = 100)
	private String fullName;

	@Column(unique = true, length = 40)
	private String registerNumber;

	@Column(nullable = false)
	private String passwordHash;

	@Column(nullable = false, length = 30)
	private String role;

	@Column(nullable = false)
	private boolean enabled = true;

	protected User() {
	}

	public User(String username, String fullName, String registerNumber, String passwordHash, String role) {
		this.username = username;
		this.fullName = fullName;
		this.registerNumber = registerNumber;
		this.passwordHash = passwordHash;
		this.role = role;
		this.enabled = true;
	}

	public Long getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getRegisterNumber() {
		return registerNumber;
	}

	public void setRegisterNumber(String registerNumber) {
		this.registerNumber = registerNumber;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
