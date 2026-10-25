# Cadastro de Usuário

Projeto de estudo em Java e Spring Boot: uma API REST para cadastrar, consultar, atualizar e remover usuários por nome e e-mail.

## O que está implementado

- Cadastro com `POST /usuario`.
- Consulta por e-mail, atualização por ID e exclusão por e-mail.
- Separação em controller, service e repository, com injeção de dependências pelo construtor.
- Persistência via Spring Data JPA em H2 em memória.

## Tecnologias

Java 17, Spring Boot 4.1.1 (versão definida no `pom.xml`), Spring Web MVC, Spring Data JPA, H2, Lombok e Maven Wrapper.

## Executar localmente

Pré-requisito: JDK 17 configurado e acesso à internet na primeira execução para baixar as dependências.

Na pasta do projeto, execute:

```powershell
# Windows / PowerShell
.\mvnw.cmd spring-boot:run
```

```sh
# Linux / macOS
sh ./mvnw spring-boot:run
```

A configuração usa `http://localhost:8081`. O banco H2 é temporário: os registros não são preservados ao encerrar a aplicação.

## Experimentar a API

Com a aplicação em execução, abra outro PowerShell. Os comandos abaixo usam dados fictícios; copie o `id` retornado na consulta para atualizar o registro.

```powershell
$baseUrl = 'http://localhost:8081/usuario'

# Criar
$novo = @{ nome = 'Ana'; email = 'ana@example.com' } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri $baseUrl -ContentType 'application/json' -Body $novo

# Consultar e obter o ID
$usuario = Invoke-RestMethod -Uri ($baseUrl + '?email=ana%40example.com')
$usuario

# Atualizar o nome, mantendo o e-mail existente
$alteracao = @{ nome = 'Ana Maria' } | ConvertTo-Json
Invoke-RestMethod -Method Put -Uri ($baseUrl + '?id=' + $usuario.id) -ContentType 'application/json' -Body $alteracao

# Excluir
Invoke-RestMethod -Method Delete -Uri ($baseUrl + '?email=ana%40example.com')
```

### Contrato atual

| Método | Rota | Entrada | Resposta de sucesso definida no controller |
| --- | --- | --- | --- |
| POST | `/usuario` | JSON com nome/e-mail | HTTP 200, sem corpo |
| GET | `/usuario?email=...` | E-mail na query | HTTP 200, usuário em JSON |
| PUT | `/usuario?id=...` | ID na query; campos não nulos no JSON | HTTP 200, sem corpo |
| DELETE | `/usuario?email=...` | E-mail na query | HTTP 200, sem corpo |

No PUT, o service altera somente nome/e-mail recebidos como não nulos. Apesar dessa atualização parcial, a rota atual usa PUT; não há endpoint PATCH.

### Erros conhecidos

Consultas/atualizações de usuário ausente e falhas conhecidas de entrada/persistência usam um corpo JSON estável:

```json
{
  "status": 404,
  "codigo": "USUARIO_NAO_ENCONTRADO",
  "mensagem": "Usuário não encontrado.",
  "caminho": "/usuario"
}
```

| Situação | HTTP | Código |
| --- | --- | --- |
| Consulta ou atualização de usuário inexistente | 404 | `USUARIO_NAO_ENCONTRADO` |
| Conflito de integridade no banco, como e-mail duplicado | 409 | `CONFLITO_DADOS` |
| JSON malformado ou corpo obrigatório ausente | 400 | `JSON_INVALIDO` |
| Parâmetro obrigatório ausente | 400 | `PARAMETRO_AUSENTE` |
| Tipo de parâmetro inválido, como ID não numérico | 400 | `PARAMETRO_INVALIDO` |

O handler não devolve SQL, stack trace ou o e-mail consultado no corpo dos erros. A exclusão de usuário já ausente permanece idempotente (HTTP 200 sem corpo). Erros inesperados não são convertidos indevidamente em 404.

## Organização

```text
controller/UsuarioController.java       rotas HTTP
business/UsuarioService.java           operações de cadastro/consulta/atualização
infrastructure/repository/             acesso ao banco
infrastructure/entitys/                entidade persistida
src/main/resources/application.properties
```

## Testes automatizados

```powershell
.\mvnw.cmd test
```

Verificação local em 25/10/2026 com JDK 17: **15 testes executados, zero falhas, zero erros e zero ignorados** (`BUILD SUCCESS`).

Além de `contextLoads`, quatorze testes de integração exercitam o controller, o service e o repositório H2 reais via MockMvc, sem substituir o banco por mocks:

- Criação e consulta por e-mail, incluindo o ID persistido.
- Atualização de nome/e-mail e preservação de campos omitidos.
- Exclusão e verificação da remoção no banco.
- Consulta e atualização inexistentes retornando 404, sem criar registros.
- Restrição de unicidade no cadastro e na atualização retornando 409, sem expor detalhes SQL.
- JSON malformado, corpo ausente e parâmetros ausentes ou inválidos retornando 400 com código estável.
- Exclusão idempotente de usuário já ausente.

Os testes usam banco H2 separado em memória e rollback por transação. Eles verificam o comportamento atual; não implicam validação completa nem cobertura percentual medida.

## Limites e próximos passos

- Não há autenticação ou autorização.
- Ainda faltam validação completa dos campos (formato de e-mail, nomes em branco e limites de tamanho) e DTOs específicos de entrada. Os erros conhecidos já são tratados pelo `ApiExceptionHandler`; isso não equivale a validar todos os dados de negócio.
- Paginação e banco persistente ainda não foram implementados.
- O console H2 está habilitado para estudo local. A configuração atual não foi preparada para exposição pública em produção.

Este repositório demonstra prática de desenvolvimento e organização de uma API. Não representa experiência profissional nem um serviço de produção.


