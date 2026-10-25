package com.lucasdev.cadastro_usuario.controller;

import com.lucasdev.cadastro_usuario.business.exception.UsuarioNaoEncontradoException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Maps known failures without exposing SQL, stack traces or submitted values. */
@RestControllerAdvice
public class ApiExceptionHandler {
    public record ApiError(int status, String codigo, String mensagem, String caminho) {}

    @ExceptionHandler(UsuarioNaoEncontradoException.class)
    ResponseEntity<ApiError> usuarioAusente(HttpServletRequest request) {
        return erro(HttpStatus.NOT_FOUND, "USUARIO_NAO_ENCONTRADO",
                "Usuário não encontrado.", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> conflito(HttpServletRequest request) {
        return erro(HttpStatus.CONFLICT, "CONFLITO_DADOS",
                "Os dados informados conflitam com um registro existente.", request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> jsonInvalido(HttpServletRequest request) {
        return erro(HttpStatus.BAD_REQUEST, "JSON_INVALIDO",
                "Informe um corpo JSON válido.", request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ApiError> parametroAusente(MissingServletRequestParameterException ex,
                                             HttpServletRequest request) {
        return erro(HttpStatus.BAD_REQUEST, "PARAMETRO_AUSENTE",
                "Parâmetro obrigatório ausente: " + ex.getParameterName() + ".", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> parametroInvalido(MethodArgumentTypeMismatchException ex,
                                              HttpServletRequest request) {
        return erro(HttpStatus.BAD_REQUEST, "PARAMETRO_INVALIDO",
                "Parâmetro inválido: " + ex.getName() + ".", request);
    }

    private ResponseEntity<ApiError> erro(HttpStatus status, String codigo,
                                          String mensagem, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(new ApiError(status.value(), codigo, mensagem, request.getRequestURI()));
    }
}

