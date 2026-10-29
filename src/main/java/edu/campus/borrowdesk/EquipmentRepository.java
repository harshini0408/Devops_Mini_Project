package edu.campus.borrowdesk;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

	List<Equipment> findAllByOrderByNameAsc();

	/** Row lock so two concurrent issues cannot both take the last item. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select e from Equipment e where e.id = :id")
	Optional<Equipment> findForUpdate(@Param("id") Long id);
}
