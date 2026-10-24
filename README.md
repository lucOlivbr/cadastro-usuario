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

## Organização

```text
controller/UsuarioController.java       rotas HTTP
business/UsuarioService.java           operações de cadastro/consulta/atualização
infrastructure/repository/             acesso ao banco
infrastructure/entitys/                entidade persistida
src/main/resources/application.properties
```

## Teste existente

```powershell
.\mvnw.cmd test
```

A suíte atual contém um teste de carregamento do contexto (`contextLoads`). Isso não representa cobertura das regras de negócio ou dos endpoints. Os exemplos foram conferidos contra o código e a configuração; esta revisão de documentação não executou a API.

## Limites e próximos passos

- Não há autenticação ou autorização.
- Ainda faltam validação de entrada, tratamento padronizado de erros e respostas 404 específicas; os casos de consulta/atualização inexistentes lançam `RuntimeException`.
- Ainda não há testes dedicados de CRUD, paginação ou banco persistente.
- O console H2 está habilitado para estudo local. A configuração atual não foi preparada para exposição pública em produção.

Este repositório demonstra prática de desenvolvimento e organização de uma API. Não representa experiência profissional nem um serviço de produção.
