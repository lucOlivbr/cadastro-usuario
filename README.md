# Cadastro de usuários — API REST

API de estudo para cadastrar, consultar, atualizar e remover usuários por nome e email.

**Java 17 · Spring Boot 4.1.1 · Spring Data JPA · H2 · Maven**

## Executar e testar

Requer JDK 17+; o Maven Wrapper baixa o Maven e as dependências.

```powershell
.\mvnw.cmd verify
.\mvnw.cmd spring-boot:run
```

No Linux/macOS: `chmod +x mvnw`, `./mvnw verify` e `./mvnw spring-boot:run`.
Endereço local: http://localhost:8081.

## Contrato HTTP

| Método | Rota | Resultado |
| --- | --- | --- |
| POST | /usuario | 201; cria um novo usuário, ignorando ID enviado no corpo |
| GET | /usuario?email=ana@example.com | 200 com usuário; 404 se ausente |
| PUT | /usuario?id=1 | 200; mantém os campos omitidos |
| DELETE | /usuario?email=ana@example.com | 204; 404 se ausente |

Corpo de criação: `{"nome":"Ana","email":"ana@example.com"}`.
Atualização parcial: `{"nome":"Ana Maria"}`. O PUT parcial mantém o contrato do projeto original; uma futura versão pode usar PATCH.

Nome deve ter 1–120 caracteres após remover espaços externos. Email recebe validação básica de formato (não verifica existência da caixa postal), tem no máximo 254 caracteres e é convertido para minúsculas. Campos inválidos retornam 400; email duplicado retorna 409. Erros de negócio têm o formato `{"mensagem":"..."}`; erros de conversão HTTP usam a resposta padrão do Spring.

## Decisões

Controller → Service → Repository → H2. Validações e conflitos ficam no serviço; um advice traduz erros para HTTP sem expor detalhes do banco. A restrição única do H2 também protege contra cadastros concorrentes. Transações agrupam atualização e exclusão.

Criação sempre gera uma nova entidade: aceitar um ID do cliente poderia sobrescrever registros existentes. Atualizações validam todos os campos antes de modificar a entidade.

## Validação

`verify` executa oito testes de regras de negócio e o teste de inicialização Spring/H2. A automação do GitHub repete a verificação com Java 17. Os testes do serviço não substituem testes HTTP completos.

Roteiro manual: cadastrar → consultar → atualizar nome → consultar novamente → excluir → consultar e conferir 404. Repetir cadastro com mesmo email para conferir 409 e com nome vazio para conferir 400.

## Limites

Banco em memória: dados se perdem ao reiniciar. Não há autenticação, autorização, paginação nem migrações persistentes. O console H2 está habilitado para estudo local. Não expor esta aplicação como cadastro público de pessoas reais.

Próximo incremento: testes HTTP de ponta a ponta e persistência PostgreSQL com migrações, antes de uma implantação pública.
