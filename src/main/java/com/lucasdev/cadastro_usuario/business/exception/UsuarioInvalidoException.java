package com.lucasdev.cadastro_usuario.business.exception;

public class UsuarioInvalidoException extends RuntimeException {
    public UsuarioInvalidoException(String mensagem) {
        super(mensagem);
    }
}
