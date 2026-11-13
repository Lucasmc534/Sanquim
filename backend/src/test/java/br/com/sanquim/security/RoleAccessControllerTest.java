package br.com.sanquim.security;

import br.com.sanquim.attendance.AttendanceRepository;
import br.com.sanquim.assignment.Assignment;
import br.com.sanquim.assignment.AssignmentRepository;
import br.com.sanquim.assignment.AssignmentSubmissionRepository;
import br.com.sanquim.event.SchoolEventRepository;
import br.com.sanquim.grade.GradeRepository;
import br.com.sanquim.lesson.LessonRepository;
import br.com.sanquim.subject.Subject;
import br.com.sanquim.subject.SubjectAccessService;
import br.com.sanquim.subject.SubjectRepository;
import br.com.sanquim.user.AppUser;
import br.com.sanquim.user.AppUserRepository;
import br.com.sanquim.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import({SecurityConfig.class, SubjectAccessService.class})
@org.springframework.test.context.TestPropertySource(properties =
    "sanquim.security.jwt-secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
/** Testes HTTP de autorização por papel e escopo de matéria. */
class RoleAccessControllerTest {
    private static final UUID SECRETARY_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TEACHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID OTHER_TEACHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID STUDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID SUBJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID OTHER_SUBJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000006");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AppUserRepository users;

    @MockitoBean
    private SubjectRepository subjects;

    @MockitoBean
    private SchoolEventRepository events;

    @MockitoBean
    private GradeRepository grades;

    @MockitoBean
    private AttendanceRepository attendance;

    @MockitoBean
    private LessonRepository lessons;

    @MockitoBean
    private AssignmentRepository assignments;

    @MockitoBean
    private AssignmentSubmissionRepository submissions;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private AuthService authService;

    private AppUser teacher;
    private AppUser otherTeacher;
    private AppUser student;
    private Subject teacherSubject;
    private Subject otherSubject;

    @BeforeEach
    void setUp() {
        teacher = user(TEACHER_ID, UserRole.PROFESSOR, "teacher@example.com");
        otherTeacher = user(OTHER_TEACHER_ID, UserRole.PROFESSOR, "other@example.com");
        student = user(STUDENT_ID, UserRole.ALUNO, "student@example.com");
        teacherSubject = new Subject("Matemática", "9º A", teacher);
        teacherSubject.getStudents().add(student);
        otherSubject = new Subject("Português", "9º B", otherTeacher);
        when(subjects.findById(SUBJECT_ID)).thenReturn(Optional.of(teacherSubject));
        when(subjects.findById(OTHER_SUBJECT_ID)).thenReturn(Optional.of(otherSubject));
    }

    @Test
    void anonymousRequestsCannotReadPrivateData() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/subjects")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/events")).andExpect(status().isUnauthorized());
    }

    @Test
    void studentsCannotUseSecretaryOrTeacherWriteOperations() throws Exception {
        mvc.perform(get("/api/users").with(asRole(UserRole.ALUNO, STUDENT_ID)))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/users/students").with(asRole(UserRole.ALUNO, STUDENT_ID)))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/users").contentType(APPLICATION_JSON)
                .content(validUserJson())
                .with(asRole(UserRole.ALUNO, STUDENT_ID)))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/subjects").contentType(APPLICATION_JSON)
                .content(validSubjectJson())
                .with(asRole(UserRole.ALUNO, STUDENT_ID)))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/subjects/{id}/assignments", SUBJECT_ID)
                .contentType(APPLICATION_JSON).content("{\"title\":\"Tarefa\"}")
                .with(asRole(UserRole.ALUNO, STUDENT_ID)))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/subjects/{id}/grades", SUBJECT_ID)
                .contentType(APPLICATION_JSON)
                .content("{\"studentId\":\"" + STUDENT_ID + "\",\"title\":\"Prova\",\"score\":90}")
                .with(asRole(UserRole.ALUNO, STUDENT_ID)))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/subjects/{id}/attendance", SUBJECT_ID)
                .contentType(APPLICATION_JSON)
                .content("{\"studentId\":\"" + STUDENT_ID
                    + "\",\"lessonDate\":\"2026-01-15\",\"present\":true}")
                .with(asRole(UserRole.ALUNO, STUDENT_ID)))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/subjects/{id}/lessons", SUBJECT_ID)
                .contentType(APPLICATION_JSON)
                .content("{\"title\":\"Aula\",\"description\":\"Conteúdo\","
                    + "\"startsAt\":\"2026-01-15T10:00:00\"}")
                .with(asRole(UserRole.ALUNO, STUDENT_ID)))
            .andExpect(status().isForbidden());
    }

    @Test
    void studentsCannotSubmitAssignmentsForAnotherSubject() throws Exception {
        mvc.perform(post("/api/subjects/{subjectId}/assignments/{assignmentId}/submissions",
                        OTHER_SUBJECT_ID, UUID.randomUUID())
                .contentType(APPLICATION_JSON)
                .content("{\"text\":\"Minha resposta\"}")
                .with(asRole(UserRole.ALUNO, STUDENT_ID)))
            .andExpect(status().isForbidden());
    }

    @Test
    void teachersCanManageTheirOwnSubjectsButNotAnotherTeachersSubject() throws Exception {
        when(assignments.save(any(Assignment.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        mvc.perform(post("/api/subjects/{id}/assignments", SUBJECT_ID)
                .contentType(APPLICATION_JSON)
                .content("{\"title\":\"Lista 1\",\"description\":\"Exercícios\",\"dueAt\":\"2026-12-01T10:00:00\"}")
                .with(asRole(UserRole.PROFESSOR, TEACHER_ID)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Lista 1"));

        mvc.perform(post("/api/subjects/{id}/assignments", OTHER_SUBJECT_ID)
                .contentType(APPLICATION_JSON)
                .content("{\"title\":\"Lista 2\"}")
                .with(asRole(UserRole.PROFESSOR, TEACHER_ID)))
            .andExpect(status().isForbidden());

        mvc.perform(put("/api/subjects/{id}", SUBJECT_ID)
                .contentType(APPLICATION_JSON)
                .content("{\"name\":\"Outro nome\",\"className\":\"9º A\"}")
                .with(asRole(UserRole.PROFESSOR, TEACHER_ID)))
            .andExpect(status().isForbidden());

        mvc.perform(put("/api/subjects/{id}/teacher", SUBJECT_ID)
                .contentType(APPLICATION_JSON)
                .content("{\"teacherId\":\"" + OTHER_TEACHER_ID + "\"}")
                .with(asRole(UserRole.PROFESSOR, TEACHER_ID)))
            .andExpect(status().isForbidden());

        mvc.perform(post("/api/events").contentType(APPLICATION_JSON)
                .content("{\"title\":\"Evento\",\"description\":\"Teste\",\"startsAt\":\"2026-12-01T10:00:00\"}")
                .with(asRole(UserRole.PROFESSOR, TEACHER_ID)))
            .andExpect(status().isForbidden());
    }

    @Test
    void studentsReadOnlyTheirOwnGradesAndCannotReadOtherSubjects() throws Exception {
        mvc.perform(get("/api/subjects/{id}/grades", SUBJECT_ID)
                .with(asRole(UserRole.ALUNO, STUDENT_ID)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
        verify(grades).findBySubject_IdAndStudent_IdOrderByGradedAtDesc(SUBJECT_ID, STUDENT_ID);

        mvc.perform(get("/api/subjects/{id}/grades", OTHER_SUBJECT_ID)
                .with(asRole(UserRole.ALUNO, STUDENT_ID)))
            .andExpect(status().isForbidden());
    }

    @Test
    void secretaryCanCreateSubjectsAndCanAccessAdministrativeRoutes() throws Exception {
        when(users.findById(TEACHER_ID)).thenReturn(Optional.of(teacher));
        when(subjects.save(any(Subject.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mvc.perform(post("/api/subjects").contentType(APPLICATION_JSON)
                .content(validSubjectJson())
                .with(asRole(UserRole.SECRETARIA, SECRETARY_ID)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Ciências"));
        mvc.perform(get("/api/users").with(asRole(UserRole.SECRETARIA, SECRETARY_ID)))
            .andExpect(status().isOk());
    }

    private static AppUser user(UUID id, UserRole role, String email) {
        AppUser user = new AppUser("Test User", email, "not-a-real-password-hash", role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static JwtRequestPostProcessor asRole(UserRole role, UUID userId) {
        String roleName = role.name();
        return jwt().jwt(token -> token
                .subject(roleName.toLowerCase() + "@example.com")
                .claim("roles", List.of(roleName))
                .claim("userId", userId.toString()))
            .authorities(new SimpleGrantedAuthority("ROLE_" + roleName));
    }

    private static String validUserJson() {
        return "{\"name\":\"New User\",\"email\":\"new@example.com\","
            + "\"password\":\"valid-password-123\",\"role\":\"ALUNO\"}";
    }

    private static String validSubjectJson() {
        return "{\"name\":\"Ciências\",\"className\":\"8º A\",\"teacherId\":\""
            + TEACHER_ID + "\"}";
    }
}
