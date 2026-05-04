package cz.semester.courseapp.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Entity
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String title;

    @Min(1)
    private int capacity;

    @Enumerated(EnumType.STRING)
    private CourseStatus status = CourseStatus.DRAFT;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private final List<CourseSession> sessions = new ArrayList<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private final List<Enrollment> enrollments = new ArrayList<>();

    protected Course() {
    }

    public Course(String title, int capacity) {
        this.title = title;
        this.capacity = capacity;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getCapacity() {
        return capacity;
    }

    public CourseStatus getStatus() {
        return status;
    }

    public List<CourseSession> getSessions() {
        return List.copyOf(sessions);
    }

    public List<Enrollment> getEnrollments() {
        return List.copyOf(enrollments);
    }

    public long activeEnrollmentCount() {
        return enrollments.stream()
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.ENROLLED)
                .count();
    }

    public long waitlistCount() {
        return enrollments.stream()
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.WAITLISTED)
                .count();
    }

    public boolean hasStudent(Long studentId) {
        return enrollments.stream()
                .anyMatch(enrollment -> enrollment.getStudent().getId().equals(studentId));
    }

    public void publish() {
        if (sessions.isEmpty()) {
            throw new DomainException("Kurz nelze publikovat bez terminu.");
        }
        status = CourseStatus.PUBLISHED;
    }

    public void addSession(CourseSession session) {
        sessions.add(session);
        session.assignCourse(this);
    }

    public Enrollment enroll(Student student) {
        if (status != CourseStatus.PUBLISHED) {
            throw new DomainException("Student se nemuze zapsat do nepublikovaneho kurzu.");
        }
        if (student.isBlocked()) {
            throw new DomainException("Blokovany student se nemuze zapsat do kurzu.");
        }
        if (hasStudent(student.getId())) {
            throw new DomainException("Student uz je v kurzu zapsan nebo je na cekaci listine.");
        }

        EnrollmentStatus nextStatus = activeEnrollmentCount() < capacity
                ? EnrollmentStatus.ENROLLED
                : EnrollmentStatus.WAITLISTED;
        Enrollment enrollment = new Enrollment(student, this, nextStatus);
        enrollments.add(enrollment);
        return enrollment;
    }

    public void cancelEnrollment(Long studentId) {
        Enrollment removed = enrollments.stream()
                .filter(enrollment -> enrollment.getStudent().getId().equals(studentId))
                .findFirst()
                .orElseThrow(() -> new DomainException("Zapis studenta v kurzu neexistuje."));
        boolean releasedSeat = removed.getStatus() == EnrollmentStatus.ENROLLED;
        enrollments.remove(removed);
        if (releasedSeat) {
            promoteFirstWaitlistedStudent();
        }
    }

    public void changeCapacity(int newCapacity) {
        if (newCapacity < 1) {
            throw new DomainException("Kapacita musi byt alespon 1.");
        }
        if (newCapacity < activeEnrollmentCount()) {
            throw new DomainException("Kapacita nesmi byt mensi nez pocet aktivnich zapisu.");
        }
        capacity = newCapacity;
        while (activeEnrollmentCount() < capacity && waitlistCount() > 0) {
            promoteFirstWaitlistedStudent();
        }
    }

    private void promoteFirstWaitlistedStudent() {
        enrollments.stream()
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.WAITLISTED)
                .min(Comparator.comparing(Enrollment::getCreatedAt))
                .ifPresent(Enrollment::promote);
    }
}
