package com.lucasdev.cadastro_usuario.infrastructure.entitys;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Table(name = "usuario")
@Entity
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "nome")
    private String nome;

    public Usuario() {
    }

    public Usuario(Integer id, String email, String nome) {
        this.id = id;
        this.email = email;
        this.nome = nome;
    }
}