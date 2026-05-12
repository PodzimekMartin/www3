package cz.semester.courseapp.http;

import cz.semester.courseapp.app.CourseService.ApplicationState;
import java.util.List;

record StateResponse(
        List<StudentResponse> students,
        List<InstructorResponse> instructors,
        List<CourseResponse> courses) {

    static StateResponse from(ApplicationState state) {
        return new StateResponse(
                state.students().stream().map(StudentResponse::from).toList(),
                state.instructors().stream().map(InstructorResponse::from).toList(),
                state.courses().stream().map(CourseResponse::from).toList());
    }
}
