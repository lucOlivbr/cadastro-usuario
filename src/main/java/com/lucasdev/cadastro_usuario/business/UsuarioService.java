package com.lucasdev.cadastro_usuario.business;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;
import static com.lucasdev.cadastro_usuario.business.UsuarioException.Tipo.*;
import com.lucasdev.cadastro_usuario.infrastructure.entitys.Usuario;
import com.lucasdev.cadastro_usuario.infrastructure.repository.UsuarioRepository;

@Service
public class UsuarioService {
    private final UsuarioRepository repository;
    public UsuarioService(UsuarioRepository repository) { this.repository = repository; }

    @Transactional
    public void salvarUsuario(Usuario usuario) {
        String email = validarEmail(usuario.getEmail());
        String nome = validarNome(usuario.getNome());
        verificarEmailDisponivel(email, null);
        // A client-supplied ID must never overwrite another user on creation.
        repository.saveAndFlush(new Usuario(null, email, nome));
    }

    public Usuario buscarUsuarioPorEmail(String email) {
        return repository.findByEmail(validarEmail(email))
                .orElseThrow(() -> new UsuarioException(NAO_ENCONTRADO, "Usuário não encontrado"));
    }

    @Transactional
    public void deletarUsuarioPorEmail(String email) {
        repository.delete(buscarUsuarioPorEmail(email));
    }

    @Transactional
    public void atualizarUsuarioPorId(Integer id, Usuario usuario) {
        if (id == null || id <= 0) throw new UsuarioException(INVALIDO, "ID deve ser positivo");
        Usuario existente = repository.findById(id)
                .orElseThrow(() -> new UsuarioException(NAO_ENCONTRADO, "Usuário não encontrado"));
        String email = usuario.getEmail() == null ? existente.getEmail() : validarEmail(usuario.getEmail());
        String nome = usuario.getNome() == null ? existente.getNome() : validarNome(usuario.getNome());
        if (usuario.getEmail() != null) verificarEmailDisponivel(email, id);
        existente.setEmail(email);
        existente.setNome(nome);
        repository.saveAndFlush(existente);
    }

    private void verificarEmailDisponivel(String email, Integer idAtual) {
        repository.findByEmail(email).ifPresent(outro -> {
            if (idAtual == null || !idAtual.equals(outro.getId()))
                throw new UsuarioException(CONFLITO, "Email já cadastrado");
        });
    }

    private String validarEmail(String email) {
        if (email == null) throw new UsuarioException(INVALIDO, "Email é obrigatório");
        String normalizado = email.strip().toLowerCase(Locale.ROOT);
        if (normalizado.length() > 254 || !normalizado.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            throw new UsuarioException(INVALIDO, "Email inválido");
        return normalizado;
    }

    private String validarNome(String nome) {
        if (nome == null || nome.isBlank() || nome.strip().length() > 120)
            throw new UsuarioException(INVALIDO, "Nome deve ter entre 1 e 120 caracteres");
        return nome.strip();
    }
}
