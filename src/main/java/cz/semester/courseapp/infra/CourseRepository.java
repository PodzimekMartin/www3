package cz.semester.courseapp.infra;

import cz.semester.courseapp.domain.Course;
import cz.semester.courseapp.domain.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, Long> {

    @Query("""
            select c from Course c
            left join c.instructor i
            where (:query is null
                or lower(c.title) like lower(concat('%', :query, '%'))
                or lower(i.name) like lower(concat('%', :query, '%'))
                or lower(i.email) like lower(concat('%', :query, '%')))
            and (:status is null or c.status = :status)
            """)
    Page<Course> searchCourses(
            @Param("query") String query,
            @Param("status") CourseStatus status,
            Pageable pageable);
}
