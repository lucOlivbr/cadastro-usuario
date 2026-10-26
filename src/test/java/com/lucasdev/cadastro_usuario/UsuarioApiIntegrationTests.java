package com.lucasdev.cadastro_usuario;

import com.lucasdev.cadastro_usuario.infrastructure.entitys.Usuario;
import com.lucasdev.cadastro_usuario.infrastructure.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

/** Exercises the actual controller, service and H2 repository together. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:usuarios-testes;DB_CLOSE_DELAY=-1",
        "spring.h2.console.enabled=false"
})
@Transactional
class UsuarioApiIntegrationTests {
    @Autowired WebApplicationContext context;
    @Autowired UsuarioRepository repository;
    private MockMvc api;

    @BeforeEach
    void setUp() {
        api = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void cadastroPodeSerConsultadoPorEmail() throws Exception {
        api.perform(post("/usuario").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nome":"Ana","email":"ana@example.test"}
                        """))
                .andExpect(status().isOk()).andExpect(content().string(""));

        Usuario salvo = repository.findByEmail("ana@example.test").orElseThrow();
        api.perform(get("/usuario").param("email", "ana@example.test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(salvo.getId()))
                .andExpect(jsonPath("$.nome").value("Ana"))
                .andExpect(jsonPath("$.email").value("ana@example.test"));
    }

    @Test
    void atualizacaoMudaNomeEEmailDoMesmoUsuario() throws Exception {
        Usuario salvo = cadastrar("antigo@example.test", "Nome antigo");

        api.perform(put("/usuario").param("id", salvo.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nome":"Nome novo","email":"novo@example.test"}
                        """))
                .andExpect(status().isOk());

        assertTrue(repository.findByEmail("antigo@example.test").isEmpty());
        Usuario atualizado = repository.findByEmail("novo@example.test").orElseThrow();
        assertEquals(salvo.getId(), atualizado.getId());
        assertEquals("Nome novo", atualizado.getNome());
    }

    @Test
    void camposOmitidosNaoApagamDadosNaAtualizacao() throws Exception {
        Usuario salvo = cadastrar("mantido@example.test", "Nome antigo");

        api.perform(put("/usuario").param("id", salvo.getId().toString())
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"nome":"Novo"}
                        """))
                .andExpect(status().isOk());

        Usuario atualizado = repository.findById(salvo.getId()).orElseThrow();
        assertEquals("mantido@example.test", atualizado.getEmail());
        assertEquals("Novo", atualizado.getNome());
    }

    @Test
    void exclusaoRemoveUsuarioDoBanco() throws Exception {
        Usuario salvo = cadastrar("excluir@example.test", "Excluir");

        api.perform(delete("/usuario").param("email", "excluir@example.test"))
                .andExpect(status().isOk()).andExpect(content().string(""));

        assertFalse(repository.existsById(salvo.getId()));
    }

    @Test
    void consultaInexistenteRetorna404SemExporEmailConsultado() throws Exception {
        api.perform(get("/usuario").param("email", "ausente@example.test"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.codigo").value("USUARIO_NAO_ENCONTRADO"))
                .andExpect(jsonPath("$.caminho").value("/usuario"))
                .andExpect(content().string(not(containsString("ausente@example.test"))))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void atualizacaoInexistenteNaoCriaRegistro() throws Exception {
        long quantidade = repository.count();
        api.perform(put("/usuario").param("id", "-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Ausente"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("USUARIO_NAO_ENCONTRADO"));
        assertEquals(quantidade, repository.count());
    }

    @Test
    void emailDuplicadoRetorna409SemExporDetalhesDoBanco() throws Exception {
        cadastrar("unico@example.test", "Primeiro");
        api.perform(post("/usuario").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Segundo","email":"unico@example.test"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.codigo").value("CONFLITO_DADOS"))
                .andExpect(content().string(not(containsString("unico@example.test"))))
                .andExpect(content().string(not(containsString("insert into"))))
                .andExpect(content().string(not(containsString("JdbcSQL"))))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void jsonInvalidoRetorna400SemPersistirUsuario() throws Exception {
        long quantidade = repository.count();

        api.perform(post("/usuario").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("JSON_INVALIDO"));

        assertEquals(quantidade, repository.count());
    }

    @Test
    void consultaSemParametroEmailRetorna400() throws Exception {
        api.perform(get("/usuario")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PARAMETRO_AUSENTE"));
    }

    @Test
    void idNaoNumericoRetorna400SemCriarRegistro() throws Exception {
        api.perform(put("/usuario").param("id", "abc")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PARAMETRO_INVALIDO"));
        assertEquals(0, repository.count());
    }

    @Test
    void atualizacaoSemIdRetorna400() throws Exception {
        api.perform(put("/usuario").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PARAMETRO_AUSENTE"));
    }

    @Test
    void cadastroSemCorpoRetorna400() throws Exception {
        api.perform(post("/usuario").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("JSON_INVALIDO"));
        assertEquals(0, repository.count());
    }

    @Test
    void atualizacaoParaEmailDeOutroUsuarioRetorna409() throws Exception {
        cadastrar("ocupado@example.test", "Primeiro");
        Usuario segundo = cadastrar("segundo@example.test", "Segundo");
        api.perform(put("/usuario").param("id", segundo.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ocupado@example.test"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_DADOS"));
    }

    @Test
    void excluirUsuarioAusenteContinuaIdempotente() throws Exception {
        api.perform(delete("/usuario").param("email", "ausente@example.test"))
                .andExpect(status().isOk()).andExpect(content().string(""));
        assertEquals(0, repository.count());
    }

    private Usuario cadastrar(String email, String nome) {
        return repository.saveAndFlush(new Usuario(null, email, nome));
    }
}


