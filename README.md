# Cadastro de Usuário

API REST simples para cadastrar, consultar, atualizar e remover usuários (nome e email).

## Tecnologias
- Java 17
- Spring Boot
- Spring Data JPA
- Banco H2 (em memória)
- Maven

## Como funciona
Controller (recebe o pedido) → Service (regras) → Repository (acesso ao banco) → H2.

## Como rodar
1. Abra o projeto no VS Code com o JDK 17 instalado.
2. Rode a classe `CadastroUsuarioApplication`.
3. A API sobe em `http://localhost:8081`.

## Endpoints
- `POST /usuario` — cadastra (body: `{"nome":"Ana","email":"ana@email.com"}`)
- `GET /usuario?email=ana@email.com` — busca por email
- `PUT /usuario?id=1` — atualiza (body: `{"nome":"Ana Maria"}`)
- `DELETE /usuario?email=ana@email.com` — remove