package com.lucasdev.cadastro_usuario.business.exception;

public class EmailJaCadastradoException extends RuntimeException {
    public EmailJaCadastradoException() {
        super("E-mail já cadastrado.");
    }
}
