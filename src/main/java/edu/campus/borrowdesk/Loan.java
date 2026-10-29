package edu.campus.borrowdesk;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/** One item issued to one student. A loan is active while returnedDate is null. */
@Entity
public class Loan {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "equipment_id")
	private Equipment equipment;

	private String studentName;

	private String registerNumber;

	private LocalDate issuedDate;

	private LocalDate dueDate;

	private LocalDate returnedDate;

	protected Loan() {
	}

	public Loan(Equipment equipment, String studentName, String registerNumber, LocalDate issuedDate,
			LocalDate dueDate) {
		this.equipment = equipment;
		this.studentName = studentName;
		this.registerNumber = registerNumber;
		this.issuedDate = issuedDate;
		this.dueDate = dueDate;
	}

	public Long getId() {
		return id;
	}

	public Equipment getEquipment() {
		return equipment;
	}

	public String getStudentName() {
		return studentName;
	}

	public String getRegisterNumber() {
		return registerNumber;
	}

	public LocalDate getIssuedDate() {
		return issuedDate;
	}

	public LocalDate getDueDate() {
		return dueDate;
	}

	public LocalDate getReturnedDate() {
		return returnedDate;
	}

	public void setReturnedDate(LocalDate returnedDate) {
		this.returnedDate = returnedDate;
	}

	public boolean isActive() {
		return returnedDate == null;
	}
}
