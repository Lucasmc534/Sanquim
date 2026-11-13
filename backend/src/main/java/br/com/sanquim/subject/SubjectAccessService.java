package br.com.sanquim.subject;

import br.com.sanquim.user.AppUser;
import br.com.sanquim.user.AppUserRepository;
import br.com.sanquim.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.NoSuchElementException;

@Service
/** Centraliza a autorização por papel, identidade e vínculo real com a matéria. */
public class SubjectAccessService {
    private final SubjectRepository subjects;
    private final AppUserRepository users;

    public SubjectAccessService(SubjectRepository subjects, AppUserRepository users) {
        this.subjects = subjects;
        this.users = users;
    }

    /** Exige vínculo de leitura: secretaria global, professor responsável ou aluno matriculado. */
    public Subject requireReadableSubject(UUID subjectId, Jwt jwt) {
        Subject subject = findSubject(subjectId);
        UUID userId = currentUserId(jwt);
        UserRole role = currentRole(jwt);
        // Para professor e aluno, o vínculo efetivo com a matéria define o acesso.
        boolean canRead = role == UserRole.SECRETARIA
            || role == UserRole.PROFESSOR && subject.getTeacher().getId().equals(userId)
            || role == UserRole.ALUNO && subject.getStudents().stream()
                .anyMatch(student -> student.getId().equals(userId));
        if (!canRead) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não tem acesso a esta matéria.");
        }
        return subject;
    }

    /** Exige permissão de gestão, sem permitir que professor troque o responsável da matéria. */
    public Subject requireManageableSubject(UUID subjectId, Jwt jwt) {
        Subject subject = findSubject(subjectId);
        UserRole role = currentRole(jwt);
        if (role != UserRole.SECRETARIA
            && (role != UserRole.PROFESSOR
                || !subject.getTeacher().getId().equals(currentUserId(jwt)))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não pode alterar esta matéria.");
        }
        return subject;
    }

    public AppUser requireStudent(UUID studentId) {
        return users.findById(studentId)
            .filter(user -> user.getRole() == UserRole.ALUNO)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "O usuário informado não existe ou não tem o papel de aluno."));
    }

    public void requireStudentInSubject(Subject subject, UUID studentId) {
        boolean enrolled = subject.getStudents().stream()
            .anyMatch(student -> student.getId().equals(studentId));
        if (!enrolled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "O aluno não está matriculado nesta matéria.");
        }
    }

    public UUID currentUserId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getClaimAsString("userId"));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token de acesso inválido.");
        }
    }

    public UserRole currentRole(Jwt jwt) {
        try {
            return UserRole.valueOf(jwt.getClaimAsStringList("roles").getFirst());
        } catch (IllegalArgumentException | NullPointerException | NoSuchElementException exception) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "O token não contém um perfil válido.");
        }
    }

    private Subject findSubject(UUID subjectId) {
        return subjects.findById(subjectId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Matéria não encontrada."));
    }
}
