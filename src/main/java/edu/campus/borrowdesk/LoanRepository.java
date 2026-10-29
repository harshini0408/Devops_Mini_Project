package edu.campus.borrowdesk;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoanRepository extends JpaRepository<Loan, Long> {

	long countByReturnedDateIsNull();

	long countByEquipmentAndReturnedDateIsNull(Equipment equipment);

	List<Loan> findByReturnedDateIsNullOrderByDueDateAscIdAsc();

	List<Loan> findTop50ByOrderByIdDesc();

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select l from Loan l where l.id = :id")
	Optional<Loan> findForUpdate(@Param("id") Long id);
}
