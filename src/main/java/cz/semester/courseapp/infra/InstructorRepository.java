package cz.semester.courseapp.infra;

import cz.semester.courseapp.domain.Instructor;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstructorRepository extends JpaRepository<Instructor, Long> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<Instructor> findByEmailIgnoreCase(String email);
}
