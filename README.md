# FlutterForge (Fase 1)

Gerenciador nativo de projetos Flutter para Android construído em Kotlin, Jetpack Compose, Material 3, Hilt, Retrofit 2, OkHttp, Kotlinx Serialization, Paging 3, Room, DataStore e EncryptedSharedPreferences.

## Recursos implementados na Fase 1

- **Autenticação GitHub OAuth Device Flow funcional** (`POST /login/device/code` + polling automático em `POST /login/oauth/access_token`) sem `client_secret` embutido no app. O usuário autoriza no GitHub enquanto o app continua aguardando a resposta, com:
  - Código de usuário em fonte monoespaçada, botão **Copiar código**, **Abrir GitHub** via Custom Tabs, countdown ao vivo e opção para cancelar/gerar outro código.
  - Tratamento completo de `authorization_pending`, `slow_down` (+5s), `expired_token` e `access_denied`.
  - Armazenamento criptografado do token com `EncryptedSharedPreferences` (`AES256_GCM`) e logout automático em HTTP `401`.
- **Navegação de Repositórios**:
  - Lista paginada com **Paging 3** (`GET /user/repos` e `GET /search/repositories`), busca com debounce, pull-to-refresh e filtros `[Todos]`, `[Só Flutter]` e `[Privados]`.
  - Detecção automática de projetos Flutter via `pubspec.yaml` com badge **FLUTTER**.
- **Detalhes do Repositório e Importação**:
  - Seletor de branch, árvore de arquivos navegável com breadcrumb e preview parseado do `pubspec.yaml` (nome, versão, SDK Dart e dependências).
  - Download em streaming com **OkHttp** dentro de um **Foreground Service** (`dataSync`) com barra de progresso na notificação e na UI.
  - Extração ZIP protegida contra **Zip Slip** (validação de canonical path) e validação pós-extração de `pubspec.yaml` e `lib/main.dart`, persistindo metadados no **Room**.
  - Botão **Gerar APK** preparado para a Fase 2 (exibe BottomSheet informativo).

---

## Como criar a OAuth App no GitHub e configurar o `GITHUB_CLIENT_ID`

1. Acesse o GitHub e vá em **Settings → Developer settings → OAuth Apps → New OAuth App**.
2. Preencha os dados básicos do aplicativo:
   - **Application name**: `FlutterForge`
   - **Homepage URL**: `https://github.com/seu-usuario/flutterforge` (ou outra URL válida)
   - **Authorization callback URL**: `https://localhost/callback` (exigido pelo formulário do GitHub, embora o Device Flow não utilize redirect URI)
3. Clique em **Register application**.
4. Na página do app criado, **marque obrigatoriamente a caixa `Enable Device Flow`** e salve as alterações.
5. Copie o **Client ID** gerado (não é necessário gerar nem usar `Client Secret`).

### Onde configurar o `GITHUB_CLIENT_ID`

O projeto utiliza o **Secrets Gradle Plugin** configurado para ler variáveis de ambiente / painel **Secrets** (`.env`, nunca commitado ao Git) e injetá-las em `BuildConfig.GITHUB_CLIENT_ID`:

- **No Google AI Studio**: adicione a chave `GITHUB_CLIENT_ID` no painel **Secrets** do ambiente.
- **Em build local**: crie um arquivo `.env` na raiz do projeto (já ignorado pelo `.gitignore`) baseado no `.env.example`:

```properties
GITHUB_CLIENT_ID=Ov23liXXXXXXXXXXXXXX
```

> **Nota:** Configure o `GITHUB_CLIENT_ID` para habilitar a autenticação com GitHub. Sem essa configuração, o botão de conexão permanece desativado.
