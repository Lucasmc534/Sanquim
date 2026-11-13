# Portal Escolar Sanquim

Portal escolar com API REST em Java/Spring Boot e interface React. O papel da conta autenticada determina as telas e as operações permitidas; a autorização é sempre conferida pela API.

## Organização do projeto

```text
backend/src/main/java/br/com/sanquim/
  assignment/   tarefas e entregas de alunos
  attendance/   presenças
  bootstrap/    criação inicial opcional da conta de secretaria
  error/        respostas padronizadas de erro da API
  event/        eventos escolares
  grade/        notas e avaliações
  health/       verificação de disponibilidade
  lesson/       cronograma de aulas
  security/     login, JWT e configuração de segurança
  subject/      matérias, matrículas e autorização por matéria
  user/         contas e perfil autenticado
backend/src/main/resources/
  application.yml
  db/migration/ migrações Flyway do banco
backend/src/test/java/br/com/sanquim/security/
  RoleAccessControllerTest.java
  SecurityConfigTest.java
frontend/src/
  api.js            cliente HTTP e tratamento de erros
  App.jsx           sessão, dados compartilhados e navegação
  components/       login e estrutura comum do portal
  pages/            páginas por área funcional
  style.css         estilos da aplicação
  api.test.js       testes do cliente HTTP
frontend/vite.config.js  runtime JSX automático do React
```

O Maven usa a raiz como diretório do projeto e configura os fontes Java em `backend/src`. O frontend ativo é `frontend`. `PI-colegio-sanquim` contém telas HTML antigas mantidas como referência; `front-sanquim` não é utilizado como aplicação.

## Tecnologias

- Java 21, Spring Boot 3.5, Spring MVC, Spring Security, Spring Data JPA e Maven.
- MySQL 8.4 e Flyway. O Hibernate valida o esquema existente, sem criar tabelas automaticamente.
- JWT Bearer (HS256) e senhas com BCrypt.
- React e Vite; o cliente usa `fetch` nativo, sem biblioteca HTTP adicional.

## Papéis e permissões

| Operação | Aluno | Professor | Secretaria |
|---|---:|---:|---:|
| Consultar matérias | Apenas matriculadas | Apenas ministradas | Todas |
| Criar/editar/remover matéria e atribuir professor | — | — | Sim |
| Matricular/remover aluno | — | Nas próprias matérias | Sim |
| Consultar notas e presenças | Apenas os próprios registros | Próprias matérias | Todas |
| Registrar/editar/remover notas e presenças | — | Próprias matérias | Sim |
| Consultar/administrar tarefas e cronograma | Consulta e entrega tarefas | Próprias matérias | Sim |
| Criar/editar/remover eventos escolares | Consulta | Consulta | Sim |
| Criar contas | — | — | Sim |

A interface esconde ações que não cabem ao perfil, mas isso não é uma fronteira de segurança. Os controladores também verificam o papel e, para professores e alunos, o vínculo da conta com a matéria. A secretaria pode administrar qualquer matéria.

## Requisitos

- JDK 21 ([Eclipse Temurin](https://adoptium.net/)).
- Maven 3.9 ou superior ([instalação](https://maven.apache.org/install.html)).
- Node.js LTS e npm ([instalação](https://nodejs.org/)).
- Docker Desktop com Docker Compose ([instalação](https://www.docker.com/products/docker-desktop/)).

Depois de instalar, abra um PowerShell e confirme que os comandos estão disponíveis:

```powershell
java -version
mvn -version
node --version
npm --version
docker --version
docker compose version
```

## Configuração e execução local (PowerShell)

### Preparação (uma vez)

Abra o PowerShell na pasta do projeto. Se necessário, navegue até ela:

```powershell
Set-Location C:\Users\Lucas\Desktop\Java\Sanqim
```

Crie o `.env` local a partir do modelo e edite-o no VS Code ou no Bloco de Notas:

```powershell
Copy-Item .env.example .env
notepad .env
```

No `.env`, configure uma chave JWT aleatória com pelo menos 32 bytes codificados em Base64. O comando abaixo funciona no Windows PowerShell e no PowerShell 7; copie o resultado para `JWT_SECRET_BASE64`:

```powershell
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
$bytes = New-Object byte[] 32
$rng.GetBytes($bytes)
[Convert]::ToBase64String($bytes)
$rng.Dispose()
```

Mantenha `SANQUIM_DEMO_ACCOUNTS_ENABLED=false` se não precisar das contas de apresentação. Para criar a primeira conta de secretaria em um banco vazio, preencha `BOOTSTRAP_SECRETARIA_EMAIL`, `BOOTSTRAP_SECRETARIA_NAME` e `BOOTSTRAP_SECRETARIA_PASSWORD` temporariamente; remova os três valores após a criação. Não compartilhe nem versione o `.env`.

### Terminal 1 — banco de dados

Abra o Docker Desktop e espere ele iniciar. Em seguida, no PowerShell aberto na raiz do projeto, execute:

```powershell
docker compose --env-file .env up -d
docker compose ps
```

Espere o serviço `mysql` aparecer como `healthy`. O banco fica acessível localmente em `127.0.0.1:3307`.

### Terminal 2 — API Java

Abra um segundo PowerShell na raiz do projeto e inicie a API:

```powershell
mvn spring-boot:run
```

Na primeira inicialização, o Flyway cria/atualiza o esquema do banco. Aguarde a mensagem `Started SanquimApplication`. Se configurou o bootstrap de secretaria, remova os valores `BOOTSTRAP_SECRETARIA_*` do `.env` depois que a conta tiver sido criada.

### Terminal 3 — interface web

Abra um terceiro PowerShell na raiz do projeto. Instale as dependências (necessário na primeira execução ou após uma alteração no `package-lock.json`) e inicie o Vite:

```powershell
npm --prefix frontend ci
npm --prefix frontend run dev
```

Abra o endereço exibido pelo Vite, normalmente `http://localhost:5173`. A API fica em `http://localhost:8080`. Para encerrar, pressione `Ctrl+C` no terminal correspondente; o MySQL pode continuar ativo.

### Contas para apresentação local

Para criar contas fixas de demonstração, habilite `SANQUIM_DEMO_ACCOUNTS_ENABLED=true` no `.env` local e configure as três contas `SANQUIM_DEMO_*`. Os e-mails reservados são `aluno@sanquim.test`, `professor@sanquim.test` e `secretaria@sanquim.test`. As senhas precisam atender à regra vigente de pelo menos 12 caracteres e são gravadas no banco com BCrypt. Com o recurso ativado, as senhas configuradas são reaplicadas ao iniciar a API.

Use essa opção apenas para apresentações com dados fictícios e ambiente controlado. A conta de secretaria tem acesso administrativo. Não habilite contas/senhas conhecidas em um ambiente público ou com dados reais.

### Verificação rápida e problemas comuns

Com os três serviços em execução, abra `http://localhost:5173` e entre com uma conta já cadastrada. Para verificar a API sem abrir a interface:

```powershell
Invoke-RestMethod http://localhost:8080/api/health
```

- Se o MySQL não ficar `healthy`, confira se o Docker Desktop está aberto e se a porta `3307` não está sendo usada por outro serviço.
- Se a API não conectar ao banco, confirme `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` no `.env`; a URL padrão usa a porta `3307`.
- Se o login falhar, confirme se as contas foram cadastradas e se o e-mail/senha correspondem ao papel. A chave JWT é para assinatura de tokens e não é a senha de login.
- Se a página ficar em branco, confira os erros no terminal do Vite e no console do navegador, depois reinicie o Vite com `Ctrl+C` e `npm --prefix frontend run dev`.
- Não remova o volume Docker para tentar corrigir o banco sem antes confirmar que os dados podem ser apagados: o volume guarda o conteúdo persistente do MySQL.

## API

Envie `Authorization: Bearer <token>` em toda rota autenticada.

| Rota | Acesso |
|---|---|
| `GET /api/health` | Público; estado básico da API |
| `POST /api/auth/login` | Público; recebe e-mail e senha, retorna JWT e perfil |
| `GET /api/me` | Autenticado; perfil da sessão |
| `GET /api/users`, `POST /api/users` | Secretaria; listar e criar contas |
| `GET /api/users/students` | Professor/secretaria; IDs e nomes para operações escolares |
| `GET /api/subjects` | Autenticado; matérias filtradas pelo papel e vínculo |
| `/api/subjects/{subjectId}` | Secretaria altera/remove a matéria e atribui professor |
| `/api/subjects/{subjectId}/students/{studentId}` | Professor responsável/secretaria matricula ou remove aluno |
| `/api/subjects/{subjectId}/assignments` | Acesso à matéria; professor/secretaria administra tarefas, aluno envia ou atualiza a própria entrega |
| `/api/subjects/{subjectId}/grades` | Acesso à matéria; aluno consulta somente suas notas, professor/secretaria administra notas |
| `/api/subjects/{subjectId}/attendance` | Acesso à matéria; aluno consulta somente suas presenças, professor/secretaria administra registros |
| `/api/subjects/{subjectId}/lessons` | Acesso à matéria; professor/secretaria administra o cronograma |
| `/api/events` | Autenticado consulta; secretaria cria e remove eventos |

As rotas acadêmicas de escrita também têm operações `PUT` e `DELETE` por ID quando aplicável. A API valida, por exemplo, que o professor seja responsável pela matéria e que os alunos indicados estejam matriculados nela. A interface não possui formulário de edição para todas as operações `PUT`.

### Limites funcionais

- Entregas de tarefa aceitam texto simples de até 16 mil caracteres. Upload de arquivos não está implementado.
- A tabela `materials` existe no esquema, mas ainda não há tela nem API para administrar arquivos/materiais.
- A exclusão de matérias com histórico acadêmico é bloqueada; tarefas com entregas também não podem ser excluídas.
- Não existe cadastro público. O bootstrap de secretaria é opcional e deve ser removido após o primeiro uso.

## Testes e verificações

Execute na raiz:

```powershell
mvn clean test
npm --prefix frontend test
npm --prefix frontend run build
npm --prefix frontend audit
```

Os testes Spring MVC exercitam autorização e isolamento por perfil com repositórios simulados; eles não substituem um teste de integração com MySQL. Os testes Node validam o envio de token, tratamento de respostas e erros de rede. O build Vite valida a compilação de produção.

Para uma verificação local básica depois de iniciar os serviços:

```powershell
Invoke-RestMethod http://localhost:8080/api/health
```

## Segurança e produção

- O segredo JWT não tem valor padrão seguro; a API falha ao iniciar se estiver ausente, inválido ou tiver menos de 32 bytes.
- O token fica em `sessionStorage` no navegador e deve ser tratado como credencial. Use HTTPS em produção.
- CORS permite por padrão `http://localhost:5173`; configure `SANQUIM_CORS_ALLOWED_ORIGINS` com as origens exatas da implantação.
- O JWT assinado garante integridade, não criptografa seu conteúdo.
- Antes da produção, implemente/defina renovação e revogação de tokens, limitação de tentativas de login, auditoria, backups restauráveis e gestão privada de arquivos.
- Para dados de alunos, especialmente menores de idade, defina retenção, controles de acesso e tratamento compatíveis com a LGPD.

Para produção, use MySQL gerenciado em rede privada, TLS, backups e credenciais em um cofre de segredos. Não exponha a porta do banco à internet.
