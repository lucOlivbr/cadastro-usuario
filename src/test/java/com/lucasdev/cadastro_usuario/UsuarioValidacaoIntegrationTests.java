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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:usuarios-testes;DB_CLOSE_DELAY=-1",
        "spring.h2.console.enabled=false"
})
@Transactional
class UsuarioValidacaoIntegrationTests {
    @Autowired WebApplicationContext context;
    @Autowired UsuarioRepository repository;
    private MockMvc api;

    @BeforeEach
    void setUp() {
        api = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void criacaoComIdDeOutroUsuarioNaoSobrescreveRegistro() throws Exception {
        Usuario original = repository.saveAndFlush(new Usuario(null, "original@example.test", "Original"));

        api.perform(post("/usuario").contentType(MediaType.APPLICATION_JSON).content("""
                        {"id":%d,"nome":"Novo","email":"novo@example.test"}
                        """.formatted(original.getId())))
                .andExpect(status().isOk()).andExpect(content().string(""));

        assertEquals(2, repository.count());
        Usuario novo = repository.findByEmail("novo@example.test").orElseThrow();
        assertNotEquals(original.getId(), novo.getId());
        Usuario preservado = repository.findById(original.getId()).orElseThrow();
        assertEquals("Original", preservado.getNome());
        assertEquals("original@example.test", preservado.getEmail());
    }

    @Test
    void cadastroNormalizaNomeEPreservaCaixaDoEmail() throws Exception {
        api.perform(post("/usuario").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nome":"  Ana Maria  ","email":"ANA@EXAMPLE.TEST"}
                        """))
                .andExpect(status().isOk());

        Usuario salvo = repository.findByEmail("ANA@EXAMPLE.TEST").orElseThrow();
        assertEquals("Ana Maria", salvo.getNome());
        assertEquals("ANA@EXAMPLE.TEST", salvo.getEmail());
        assertEquals(1, repository.count());
    }

    @Test
    void camposObrigatoriosOuFormatoInvalidoRetornam400SemPersistir() throws Exception {
        String[] invalidos = {
                "{\"email\":\"ana@example.test\"}",
                "{\"nome\":null,\"email\":\"ana@example.test\"}",
                "{\"nome\":\"   \",\"email\":\"ana@example.test\"}",
                "{\"nome\":\"Ana\"}",
                "{\"nome\":\"Ana\",\"email\":null}",
                "{\"nome\":\"Ana\",\"email\":\"   \"}",
                "{\"nome\":\"Ana\",\"email\":\"nao-email\"}",
                "{\"nome\":\"Ana\",\"email\":\"ana @example.test\"}",
                "{\"nome\":\"Ana\",\"email\":\" ana@example.test \"}"
        };
        for (String json : invalidos) {
            api.perform(post("/usuario").contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.codigo").value("DADOS_INVALIDOS"))
                    .andExpect(jsonPath("$.caminho").value("/usuario"))
                    .andExpect(jsonPath("$.trace").doesNotExist())
                    .andExpect(content().string(not(containsString("nao-email"))));
            assertEquals(0, repository.count(), json);
        }
    }

    @Test
    void limitesDeNomeEEmailSaoAceitosPeloContratoBasico() throws Exception {
        String nome = "A".repeat(120);
        String email = "a".repeat(241) + "@example.test";
        assertEquals(254, email.length());

        api.perform(post("/usuario").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nome":"%s","email":"%s"}
                        """.formatted(nome, email)))
                .andExpect(status().isOk());

        Usuario salvo = repository.findByEmail(email).orElseThrow();
        assertEquals(nome, salvo.getNome());
    }

    @Test
    void nomeAcimaDoLimiteRetorna400() throws Exception {
        api.perform(post("/usuario").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nome":"%s","email":"ana@example.test"}
                        """.formatted("A".repeat(121))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DADOS_INVALIDOS"));
        assertEquals(0, repository.count());
    }

    @Test
    void emailAcimaDoLimiteRetorna400() throws Exception {
        api.perform(post("/usuario").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nome":"Ana","email":"%s"}
                        """.formatted("a".repeat(242) + "@example.test")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DADOS_INVALIDOS"));
        assertEquals(0, repository.count());
    }

    @Test
    void emailsComCaixaDiferenteContinuamDistintosNoContratoAtual() throws Exception {
        repository.saveAndFlush(new Usuario(null, "ana@example.test", "Original"));

        api.perform(post("/usuario").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nome":"Outro","email":"ANA@EXAMPLE.TEST"}
                        """))
                .andExpect(status().isOk());
        assertEquals(2, repository.count());
        assertEquals("Original", repository.findByEmail("ana@example.test").orElseThrow().getNome());
        assertEquals("Outro", repository.findByEmail("ANA@EXAMPLE.TEST").orElseThrow().getNome());
    }

    @Test
    void atualizacaoComNomeInvalidoPreservaNomeEEmail() throws Exception {
        Usuario salvo = repository.saveAndFlush(new Usuario(null, "original@example.test", "Original"));

        api.perform(put("/usuario").param("id", salvo.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nome":"   ","email":"novo@example.test"}
                                """))
                .andExpect(status().isBadRequest());

        assertEquals("Original", salvo.getNome());
        assertEquals("original@example.test", salvo.getEmail());
        assertEquals(1, repository.count());
        assertTrue(repository.findByEmail("novo@example.test").isEmpty());
    }

    @Test
    void atualizacaoComEmailInvalidoPreservaNomeEEmail() throws Exception {
        Usuario salvo = repository.saveAndFlush(new Usuario(null, "original@example.test", "Original"));

        api.perform(put("/usuario").param("id", salvo.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nome":"Novo","email":"invalido"}
                                """))
                .andExpect(status().isBadRequest());

        assertEquals("Original", salvo.getNome());
        assertEquals("original@example.test", salvo.getEmail());
        assertEquals(1, repository.count());
    }

    @Test
    void atualizacaoParaEmailOcupadoNaoModificaAEntidade() throws Exception {
        repository.saveAndFlush(new Usuario(null, "ocupado@example.test", "Primeiro"));
        Usuario segundo = repository.saveAndFlush(new Usuario(null, "segundo@example.test", "Segundo"));

        api.perform(put("/usuario").param("id", segundo.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nome":"Novo","email":"ocupado@example.test"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_DADOS"));

        assertEquals("Segundo", segundo.getNome());
        assertEquals("segundo@example.test", segundo.getEmail());
        assertEquals(2, repository.count());
    }

    @Test
    void atualizarComOProprioEmailEIdNoCorpoPreservaIdentidade() throws Exception {
        Usuario salvo = repository.saveAndFlush(new Usuario(null, "ana@example.test", "Ana"));

        api.perform(put("/usuario").param("id", salvo.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"id":999,"nome":" Nova Ana ","email":"ana@example.test"}
                                """))
                .andExpect(status().isOk());

        assertEquals(1, repository.count());
        Usuario atualizado = repository.findByEmail("ana@example.test").orElseThrow();
        assertEquals(salvo.getId(), atualizado.getId());
        assertEquals("Nova Ana", atualizado.getNome());
    }

    @Test
    void consultaEExclusaoPreservamBuscaExataDoEmail() throws Exception {
        api.perform(post("/usuario").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nome":"Ana","email":"ANA@EXAMPLE.TEST"}
                        """))
                .andExpect(status().isOk());
        api.perform(get("/usuario").param("email", "ANA@EXAMPLE.TEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ANA@EXAMPLE.TEST"));
        api.perform(get("/usuario").param("email", "ana@example.test"))
                .andExpect(status().isNotFound());
        api.perform(delete("/usuario").param("email", "ana@example.test"))
                .andExpect(status().isOk());
        assertEquals(1, repository.count());
        api.perform(delete("/usuario").param("email", "ANA@EXAMPLE.TEST"))
                .andExpect(status().isOk()).andExpect(content().string(""));
        assertEquals(0, repository.count());
        api.perform(delete("/usuario").param("email", "ANA@EXAMPLE.TEST"))
                .andExpect(status().isOk());
    }

    @Test
    void consultaEExclusaoSemCorrespondenciaPreservamContratoAnterior() throws Exception {
        repository.saveAndFlush(new Usuario(null, "ana@example.test", "Ana"));
        api.perform(get("/usuario").param("email", "invalido"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("USUARIO_NAO_ENCONTRADO"));
        api.perform(delete("/usuario").param("email", "invalido"))
                .andExpect(status().isOk());
        assertEquals(1, repository.count());
    }
}
