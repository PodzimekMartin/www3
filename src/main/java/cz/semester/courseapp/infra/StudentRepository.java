package cz.semester.courseapp.infra;

import cz.semester.courseapp.domain.Student;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<Student> findByEmailIgnoreCase(String email);
}
