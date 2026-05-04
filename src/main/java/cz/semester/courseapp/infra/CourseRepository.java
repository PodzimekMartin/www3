package cz.semester.courseapp.infra;

import cz.semester.courseapp.domain.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {
}
