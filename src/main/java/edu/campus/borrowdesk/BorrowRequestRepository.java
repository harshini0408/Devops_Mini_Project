package edu.campus.borrowdesk;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BorrowRequestRepository extends JpaRepository<BorrowRequest, Long> {

	List<BorrowRequest> findByStatusOrderByRequestedDateDescIdDesc(String status);

	List<BorrowRequest> findByRegisterNumberOrderByRequestedDateDescIdDesc(String registerNumber);

	long countByStatus(String status);

	long countByRegisterNumberAndStatus(String registerNumber, String status);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select r from BorrowRequest r where r.id = :id")
	Optional<BorrowRequest> findForUpdate(@Param("id") Long id);
}
