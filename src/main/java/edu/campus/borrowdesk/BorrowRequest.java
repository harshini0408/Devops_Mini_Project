package edu.campus.borrowdesk;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A student's request to borrow an equipment item. */
@Entity
@Table(name = "borrow_request")
public class BorrowRequest {

	public static final String STATUS_PENDING = "PENDING";
	public static final String STATUS_APPROVED = "APPROVED";
	public static final String STATUS_REJECTED = "REJECTED";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "equipment_id")
	private Equipment equipment;

	@Column(nullable = false, length = 100)
	private String studentName;

	@Column(nullable = false, length = 40)
	private String registerNumber;

	@Column(nullable = false)
	private LocalDate requestedDate;

	@Column(nullable = false)
	private LocalDate dueDate;

	@Column(length = 250)
	private String reason;

	@Column(nullable = false, length = 20)
	private String status = STATUS_PENDING;

	@Column(length = 250)
	private String adminDecision;

	private LocalDate decidedAt;

	protected BorrowRequest() {
	}

	public BorrowRequest(Equipment equipment, String studentName, String registerNumber, LocalDate requestedDate,
			LocalDate dueDate, String reason) {
		this.equipment = equipment;
		this.studentName = studentName;
		this.registerNumber = registerNumber;
		this.requestedDate = requestedDate;
		this.dueDate = dueDate;
		this.reason = reason;
		this.status = STATUS_PENDING;
	}

	public Long getId() {
		return id;
	}

	public Equipment getEquipment() {
		return equipment;
	}

	public void setEquipment(Equipment equipment) {
		this.equipment = equipment;
	}

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

	public LocalDate getRequestedDate() {
		return requestedDate;
	}

	public void setRequestedDate(LocalDate requestedDate) {
		this.requestedDate = requestedDate;
	}

	public LocalDate getDueDate() {
		return dueDate;
	}

	public void setDueDate(LocalDate dueDate) {
		this.dueDate = dueDate;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getAdminDecision() {
		return adminDecision;
	}

	public void setAdminDecision(String adminDecision) {
		this.adminDecision = adminDecision;
	}

	public LocalDate getDecidedAt() {
		return decidedAt;
	}

	public void setDecidedAt(LocalDate decidedAt) {
		this.decidedAt = decidedAt;
	}

	public boolean isPending() {
		return STATUS_PENDING.equals(status);
	}

	public boolean isApproved() {
		return STATUS_APPROVED.equals(status);
	}

	public boolean isRejected() {
		return STATUS_REJECTED.equals(status);
	}
}
