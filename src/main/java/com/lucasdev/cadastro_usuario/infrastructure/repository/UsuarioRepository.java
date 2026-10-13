package com.lucasdev.cadastro_usuario.infrastructure.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.lucasdev.cadastro_usuario.infrastructure.entitys.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByEmail(String email);

    @Transactional
    void deleteByEmail(String email);
}