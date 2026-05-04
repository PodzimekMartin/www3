package cz.semester.courseapp.http;

import cz.semester.courseapp.domain.Student;

record StudentResponse(Long id, String name, String email, boolean blocked) {

    static StudentResponse from(Student student) {
        return new StudentResponse(student.getId(), student.getName(), student.getEmail(), student.isBlocked());
    }
}
