package com.lucasdev.cadastro_usuario.business;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.lucasdev.cadastro_usuario.business.exception.EmailJaCadastradoException;
import com.lucasdev.cadastro_usuario.business.exception.UsuarioInvalidoException;
import com.lucasdev.cadastro_usuario.business.exception.UsuarioNaoEncontradoException;

import java.util.regex.Pattern;

import com.lucasdev.cadastro_usuario.infrastructure.entitys.Usuario;
import com.lucasdev.cadastro_usuario.infrastructure.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private static final Pattern EMAIL = Pattern.compile("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");

    private final UsuarioRepository repository;

    public UsuarioService(UsuarioRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void salvarUsuario(Usuario usuario) {
        String email = validarEmail(usuario.getEmail());
        String nome = validarNome(usuario.getNome());
        verificarEmailDisponivel(email, null);
        // Creation never reuses an ID supplied by the client.
        repository.saveAndFlush(new Usuario(null, email, nome));
    }

    public Usuario buscarUsuarioPorEmail(String email) {
        return repository.findByEmail(email)
                .orElseThrow(UsuarioNaoEncontradoException::new);
    }

    public void deletarUsuarioPorEmail(String email) {
        repository.deleteByEmail(email);
    }

    @Transactional
    public void atualizarUsuarioPorId(Integer id, Usuario usuario) {
        Usuario existente = repository.findById(id)
                .orElseThrow(UsuarioNaoEncontradoException::new);

        String email = usuario.getEmail() == null ? existente.getEmail() : validarEmail(usuario.getEmail());
        String nome = usuario.getNome() == null ? existente.getNome() : validarNome(usuario.getNome());
        if (usuario.getEmail() != null) {
            verificarEmailDisponivel(email, id);
        }
        // Validate the whole request before changing a managed entity.
        existente.setEmail(email);
        existente.setNome(nome);
        repository.saveAndFlush(existente);
    }

    private void verificarEmailDisponivel(String email, Integer idAtual) {
        repository.findByEmail(email).ifPresent(outro -> {
            if (idAtual == null || !idAtual.equals(outro.getId())) {
                throw new EmailJaCadastradoException();
            }
        });
    }

    private String validarEmail(String email) {
        if (email == null) {
            throw new UsuarioInvalidoException("E-mail é obrigatório.");
        }
        if (email.length() > 254 || !EMAIL.matcher(email).matches()) {
            throw new UsuarioInvalidoException("Informe um e-mail válido com até 254 caracteres.");
        }
        return email;
    }

    private String validarNome(String nome) {
        if (nome == null || nome.isBlank() || nome.strip().length() > 120) {
            throw new UsuarioInvalidoException("Nome deve ter entre 1 e 120 caracteres.");
        }
        return nome.strip();
    }
}
