package com.lucasdev.cadastro_usuario.controller;

import com.lucasdev.cadastro_usuario.business.UsuarioException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    public record Erro(String mensagem) {}
    @ExceptionHandler(UsuarioException.class)
    public ResponseEntity<Erro> tratarUsuario(UsuarioException erro) {
        HttpStatus status = switch (erro.getTipo()) {
            case INVALIDO -> HttpStatus.BAD_REQUEST;
            case NAO_ENCONTRADO -> HttpStatus.NOT_FOUND;
            case CONFLITO -> HttpStatus.CONFLICT;
        };
        return ResponseEntity.status(status).body(new Erro(erro.getMessage()));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Erro> tratarConflito(DataIntegrityViolationException erro) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new Erro("Conflito com os dados existentes."));
    }
}
