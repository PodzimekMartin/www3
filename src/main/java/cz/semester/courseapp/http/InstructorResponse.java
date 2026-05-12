package cz.semester.courseapp.http;

import cz.semester.courseapp.domain.Instructor;

record InstructorResponse(Long id, String name, String email) {

    static InstructorResponse from(Instructor instructor) {
        return new InstructorResponse(instructor.getId(), instructor.getName(), instructor.getEmail());
    }
}
