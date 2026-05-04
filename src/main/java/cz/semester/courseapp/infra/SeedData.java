package cz.semester.courseapp.infra;

import cz.semester.courseapp.app.CourseService;
import java.time.LocalDateTime;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.seed-data", havingValue = "true")
public class SeedData implements ApplicationRunner {

    private final CourseService courseService;
    private final StudentRepository studentRepository;

    public SeedData(CourseService courseService, StudentRepository studentRepository) {
        this.courseService = courseService;
        this.studentRepository = studentRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (studentRepository.count() > 0) {
            return;
        }

        var ada = courseService.createStudent("Ada Lovelace", "ada@example.test");
        var grace = courseService.createStudent("Grace Hopper", "grace@example.test");
        var linus = courseService.createStudent("Linus Torvalds", "linus@example.test");
        var course = courseService.createCourse("DevOps pipeline prakticky", 2);

        courseService.addSession(
                course.getId(),
                LocalDateTime.now().plusDays(7).withHour(10).withMinute(0).withSecond(0).withNano(0),
                LocalDateTime.now().plusDays(7).withHour(12).withMinute(0).withSecond(0).withNano(0));
        courseService.publishCourse(course.getId());
        courseService.enroll(course.getId(), ada.getId());
        courseService.enroll(course.getId(), grace.getId());
        courseService.enroll(course.getId(), linus.getId());
    }
}
