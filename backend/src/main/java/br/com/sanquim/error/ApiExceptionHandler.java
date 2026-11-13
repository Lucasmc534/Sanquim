package br.com.sanquim.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
/** Converte falhas de autenticação, validação e integridade em respostas HTTP previsíveis. */
public class ApiExceptionHandler {
    @ExceptionHandler(BadCredentialsException.class)
    ProblemDetail handleBadCredentials() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Os dados enviados são inválidos.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataConflict() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
            "O registro conflita com dados existentes.");
    }
}
