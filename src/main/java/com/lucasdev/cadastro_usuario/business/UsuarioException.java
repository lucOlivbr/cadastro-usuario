package com.lucasdev.cadastro_usuario.business;

public class UsuarioException extends RuntimeException {
    public enum Tipo { INVALIDO, NAO_ENCONTRADO, CONFLITO }
    private final Tipo tipo;
    public UsuarioException(Tipo tipo, String mensagem) {
        super(mensagem);
        this.tipo = tipo;
    }
    public Tipo getTipo() { return tipo; }
}
