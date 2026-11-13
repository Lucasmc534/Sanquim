CREATE TABLE app_users (
    id BINARY(16) NOT NULL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(190) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    CONSTRAINT chk_app_users_role CHECK (role IN ('SECRETARIA', 'PROFESSOR', 'ALUNO'))
);

CREATE TABLE subjects (
    id BINARY(16) NOT NULL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    class_name VARCHAR(40),
    teacher_id BINARY(16) NOT NULL,
    CONSTRAINT fk_subject_teacher FOREIGN KEY (teacher_id) REFERENCES app_users(id)
);

CREATE TABLE subject_students (
    subject_id BINARY(16) NOT NULL,
    student_id BINARY(16) NOT NULL,
    PRIMARY KEY (subject_id, student_id),
    CONSTRAINT fk_subject_students_subject FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    CONSTRAINT fk_subject_students_student FOREIGN KEY (student_id) REFERENCES app_users(id) ON DELETE CASCADE
);

CREATE TABLE school_events (
    id BINARY(16) NOT NULL PRIMARY KEY,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    starts_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NOT NULL,
    CONSTRAINT fk_school_events_creator FOREIGN KEY (created_by) REFERENCES app_users(id),
    INDEX idx_school_events_starts_at (starts_at)
);

CREATE TABLE attendance (
    id BINARY(16) NOT NULL PRIMARY KEY,
    subject_id BINARY(16) NOT NULL,
    student_id BINARY(16) NOT NULL,
    lesson_date DATE NOT NULL,
    present BOOLEAN NOT NULL,
    recorded_by BINARY(16) NOT NULL,
    CONSTRAINT fk_attendance_subject FOREIGN KEY (subject_id) REFERENCES subjects(id),
    CONSTRAINT fk_attendance_student FOREIGN KEY (student_id) REFERENCES app_users(id),
    CONSTRAINT fk_attendance_recorder FOREIGN KEY (recorded_by) REFERENCES app_users(id),
    UNIQUE KEY uk_attendance_student_lesson (subject_id, student_id, lesson_date)
);

CREATE TABLE grades (
    id BINARY(16) NOT NULL PRIMARY KEY,
    subject_id BINARY(16) NOT NULL,
    student_id BINARY(16) NOT NULL,
    title VARCHAR(160) NOT NULL,
    score DECIMAL(5,2) NOT NULL,
    graded_at DATETIME(6) NOT NULL,
    recorded_by BINARY(16) NOT NULL,
    CONSTRAINT fk_grades_subject FOREIGN KEY (subject_id) REFERENCES subjects(id),
    CONSTRAINT fk_grades_student FOREIGN KEY (student_id) REFERENCES app_users(id),
    CONSTRAINT fk_grades_recorder FOREIGN KEY (recorded_by) REFERENCES app_users(id),
    CONSTRAINT chk_grades_score CHECK (score >= 0 AND score <= 100)
);

CREATE TABLE lessons (
    id BINARY(16) NOT NULL PRIMARY KEY,
    subject_id BINARY(16) NOT NULL,
    title VARCHAR(160) NOT NULL,
    description TEXT,
    starts_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NOT NULL,
    CONSTRAINT fk_lessons_subject FOREIGN KEY (subject_id) REFERENCES subjects(id),
    CONSTRAINT fk_lessons_creator FOREIGN KEY (created_by) REFERENCES app_users(id)
);

CREATE TABLE materials (
    id BINARY(16) NOT NULL PRIMARY KEY,
    subject_id BINARY(16) NOT NULL,
    title VARCHAR(160) NOT NULL,
    description TEXT,
    storage_key VARCHAR(500) NOT NULL,
    created_by BINARY(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_materials_subject FOREIGN KEY (subject_id) REFERENCES subjects(id),
    CONSTRAINT fk_materials_creator FOREIGN KEY (created_by) REFERENCES app_users(id)
);

CREATE TABLE assignments (
    id BINARY(16) NOT NULL PRIMARY KEY,
    subject_id BINARY(16) NOT NULL,
    title VARCHAR(160) NOT NULL,
    description TEXT,
    due_at DATETIME(6),
    created_by BINARY(16) NOT NULL,
    CONSTRAINT fk_assignments_subject FOREIGN KEY (subject_id) REFERENCES subjects(id),
    CONSTRAINT fk_assignments_creator FOREIGN KEY (created_by) REFERENCES app_users(id)
);

CREATE TABLE assignment_submissions (
    id BINARY(16) NOT NULL PRIMARY KEY,
    assignment_id BINARY(16) NOT NULL,
    student_id BINARY(16) NOT NULL,
    submission_text TEXT,
    storage_key VARCHAR(500),
    submitted_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_submission_assignment FOREIGN KEY (assignment_id) REFERENCES assignments(id),
    CONSTRAINT fk_submission_student FOREIGN KEY (student_id) REFERENCES app_users(id),
    UNIQUE KEY uk_submission_assignment_student (assignment_id, student_id)
);
