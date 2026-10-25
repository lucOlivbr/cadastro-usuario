package com.lucasdev.cadastro_usuario.business.exception;

public class UsuarioNaoEncontradoException extends RuntimeException {
    public UsuarioNaoEncontradoException() {
        super("Usuário não encontrado.");
    }
}

