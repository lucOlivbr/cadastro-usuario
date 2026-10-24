package com.lucasdev.cadastro_usuario;

import com.lucasdev.cadastro_usuario.business.*;
import com.lucasdev.cadastro_usuario.controller.ApiExceptionHandler;
import com.lucasdev.cadastro_usuario.infrastructure.entitys.Usuario;
import com.lucasdev.cadastro_usuario.infrastructure.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsuarioServiceTests {
    private final UsuarioRepository repository = mock(UsuarioRepository.class);
    private final UsuarioService service = new UsuarioService(repository);

    @Test void criacaoNormalizaEIgnoraIdEnviado() {
        service.salvarUsuario(new Usuario(99, " ANA@EXAMPLE.COM ", " Ana "));
        verify(repository).saveAndFlush(argThat(u -> u.getId() == null
                && u.getEmail().equals("ana@example.com") && u.getNome().equals("Ana")));
    }
    @Test void rejeitaNomeVazioSemSalvar() {
        assertThrows(UsuarioException.class, () -> service.salvarUsuario(new Usuario(null, "a@example.com", " ")));
        verify(repository, never()).saveAndFlush(any());
    }
    @Test void rejeitaEmailInvalidoSemSalvar() {
        assertThrows(UsuarioException.class, () -> service.salvarUsuario(new Usuario(null, "invalido", "Ana")));
        verifyNoInteractions(repository);
    }
    @Test void emailDuplicadoRetornaConflito() {
        when(repository.findByEmail("a@example.com")).thenReturn(Optional.of(new Usuario(1, "a@example.com", "A")));
        UsuarioException erro = assertThrows(UsuarioException.class,
                () -> service.salvarUsuario(new Usuario(null, "a@example.com", "B")));
        assertEquals(409, new ApiExceptionHandler().tratarUsuario(erro).getStatusCode().value());
        verify(repository, never()).saveAndFlush(any());
    }
    @Test void buscaAusenteRetorna404() {
        UsuarioException erro = assertThrows(UsuarioException.class, () -> service.buscarUsuarioPorEmail("a@example.com"));
        assertEquals(404, new ApiExceptionHandler().tratarUsuario(erro).getStatusCode().value());
    }
    @Test void atualizacaoParcialPreservaEmailEId() {
        Usuario salvo = new Usuario(1, "a@example.com", "A");
        when(repository.findById(1)).thenReturn(Optional.of(salvo));
        service.atualizarUsuarioPorId(1, new Usuario(999, null, " Novo "));
        assertEquals("a@example.com", salvo.getEmail());
        assertEquals(1, salvo.getId());
        assertEquals("Novo", salvo.getNome());
        verify(repository).saveAndFlush(salvo);
    }
    @Test void atualizacaoInvalidaNaoAlteraEntidade() {
        Usuario salvo = new Usuario(1, "a@example.com", "A");
        when(repository.findById(1)).thenReturn(Optional.of(salvo));
        assertThrows(UsuarioException.class, () -> service.atualizarUsuarioPorId(1, new Usuario(null, "b@example.com", " ")));
        assertEquals("a@example.com", salvo.getEmail());
        verify(repository, never()).saveAndFlush(any());
    }
    @Test void exclusaoAusenteNaoApagaNada() {
        assertThrows(UsuarioException.class, () -> service.deletarUsuarioPorEmail("a@example.com"));
        verify(repository, never()).delete(any());
    }
}
