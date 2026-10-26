# Cadastro de Usuário

[![Java tests](https://github.com/lucOlivbr/cadastro-usuario/actions/workflows/java-tests.yml/badge.svg)](https://github.com/lucOlivbr/cadastro-usuario/actions/workflows/java-tests.yml)

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
| POST | `/usuario` | JSON com nome/e-mail; ID do corpo ignorado | HTTP 200, sem corpo |
| GET | `/usuario?email=...` | E-mail na query | HTTP 200, usuário em JSON |
| PUT | `/usuario?id=...` | ID na query; campos não nulos no JSON | HTTP 200, sem corpo |
| DELETE | `/usuario?email=...` | E-mail na query | HTTP 200, sem corpo |

No PUT, o service altera somente nome/e-mail recebidos como não nulos. Apesar dessa atualização parcial, a rota atual usa PUT; não há endpoint PATCH.

### Validação dos dados

- Nome obrigatório no cadastro, entre 1 e 120 caracteres após remover espaços externos.
- E-mail obrigatório no cadastro, com formato básico `parte@dominio.sufixo`, sem espaços e com até 254 caracteres. Essa validação não verifica existência da caixa postal, DNS ou conformidade completa com RFCs.
- E-mails mantêm maiúsculas/minúsculas e não recebem trim. Consultas, exclusões e a restrição de unicidade preservam a comparação exata do projeto; transformar essa regra em comparação sem distinção de caixa exigiria uma decisão e tratamento dos dados já existentes.
- Na atualização, campos não nulos são validados antes de alterar a entidade. Erro de validação ou e-mail já ocupado preserva nome e e-mail anteriores.
- A criação constrói uma entidade nova, sem reaproveitar um ID recebido do cliente. Isso impede que um POST sobrescreva um usuário existente; o ID da atualização continua vindo da query.

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
| Nome ou e-mail inválido no cadastro/atualização | 400 | `DADOS_INVALIDOS` |

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

Verificação local em 25/10/2026 com JDK 21.0.12 e compilação para Java 17: **28 testes executados, zero falhas, zero erros e zero ignorados** (`BUILD SUCCESS`). A automação do GitHub usa JDK 17.

Além de `contextLoads`, 27 testes de integração exercitam o controller, o service e o repositório H2 reais via MockMvc, sem substituir o banco por mocks:

- Criação e consulta por e-mail, incluindo o ID persistido.
- Atualização de nome/e-mail e preservação de campos omitidos.
- Exclusão e verificação da remoção no banco.
- Consulta e atualização inexistentes retornando 404, sem criar registros.
- Restrição de unicidade no cadastro e na atualização retornando 409, sem expor detalhes SQL.
- JSON malformado, corpo ausente e parâmetros ausentes ou inválidos retornando 400 com código estável.
- Exclusão idempotente de usuário já ausente.
- Proteção contra sobrescrita por ID no POST e preservação da identidade no PUT.
- Campos obrigatórios, formato básico de e-mail e limites de tamanho, incluindo valores no limite.
- Nome com espaços externos, preservação da caixa do e-mail e comparação exata em consultas/exclusões.
- Atualizações inválidas ou conflitantes sem modificar a entidade.

Os testes usam banco H2 separado em memória e rollback por transação. Eles verificam o comportamento atual; não implicam validação completa nem cobertura percentual medida.

### Verificação no GitHub

O workflow `Java tests` executa os testes unitários e de integração com Java 17 e o Maven Wrapper em cada push para `main`, nas pull requests e quando iniciado manualmente. Os resultados ficam na aba Actions e no indicador acima. A configuração usa banco H2 em memória, ações oficiais fixadas por commit e permissão somente de leitura do repositório.

## Limites e próximos passos

- Não há autenticação ou autorização.
- A validação de e-mail é básica e não verifica entregabilidade. DTOs específicos de entrada e uma política explícita de comparação de e-mails são próximos incrementos; não há cobertura percentual medida.
- Paginação e banco persistente ainda não foram implementados.
- O console H2 está habilitado para estudo local. A configuração atual não foi preparada para exposição pública em produção.

Este repositório demonstra prática de desenvolvimento e organização de uma API. Não representa experiência profissional nem um serviço de produção.

