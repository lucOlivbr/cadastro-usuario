package com.lucasdev.cadastro_usuario.business;

import org.springframework.stereotype.Service;
import com.lucasdev.cadastro_usuario.business.exception.UsuarioNaoEncontradoException;

import com.lucasdev.cadastro_usuario.infrastructure.entitys.Usuario;
import com.lucasdev.cadastro_usuario.infrastructure.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private final UsuarioRepository repository;

    public UsuarioService(UsuarioRepository repository) {
        this.repository = repository;
    }

    public void salvarUsuario(Usuario usuario) {
        repository.saveAndFlush(usuario);
    }

    public Usuario buscarUsuarioPorEmail(String email) {
        return repository.findByEmail(email)
                .orElseThrow(UsuarioNaoEncontradoException::new);
    }

    public void deletarUsuarioPorEmail(String email) {
        repository.deleteByEmail(email);
    }

    public void atualizarUsuarioPorId(Integer id, Usuario usuario) {
        Usuario existente = repository.findById(id)
                .orElseThrow(UsuarioNaoEncontradoException::new);

        if (usuario.getEmail() != null) {
            existente.setEmail(usuario.getEmail());
        }
        if (usuario.getNome() != null) {
            existente.setNome(usuario.getNome());
        }
        repository.saveAndFlush(existente);
    }
}

