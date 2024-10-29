# Discord Integration - WebSocket Documentation

Este documento descreve a comunicação via WebSocket entre o servidor Minecraft e o bot Discord, implementada no `TokenWebSocket`. Este sistema facilita a integração entre as contas de jogadores no Minecraft e seus perfis no Discord, permitindo a sincronização de cargos, envio de mensagens diretas e atualização de informações dos jogadores.

## Funcionalidades

O `TokenWebSocket` permite que o bot Discord e o servidor Minecraft se comuniquem através de ações específicas. Cada ação é identificada pelo campo `"action"` no JSON enviado via WebSocket, especificando qual operação deve ser executada. Abaixo estão os detalhes e exemplos de uso para cada uma das ações disponíveis.

---

### 1. `registerToken`

**Descrição**: Registra um token que vincula uma conta do Discord a um jogador no Minecraft. Esta ação é chamada pelo bot quando um novo token é criado para um jogador, associando a conta do Discord à conta do Minecraft.

**Requisição Exemplo (do Bot para o Plugin)**:

```json
{
  "action": "registerToken",
  "apiToken": "seuTokenDeAcessoAqui",
  "token": "abc123xyz",       // Token gerado pelo bot
  "id": "123456789012345678", // Discord ID do usuário
  "name": "DiscordUser",      // Nome de usuário no Discord
  "nick": "DiscordNick"       // Apelido do usuário no Discord, se tiver
}
```

**Quando Usar**: Sempre que um novo token de vinculação é criado para conectar uma conta do Discord a uma conta Minecraft.

---

### 2. `syncRoles`

**Descrição**: Sincroniza os cargos de um jogador no Minecraft com o bot Discord. O bot chama esta ação para atualizar os cargos de um jogador no Discord com base em suas permissões no Minecraft.

**Requisição Exemplo (do Bot para o Plugin)**:

```json
{
  "action": "syncRoles",
  "apiToken": "seuTokenDeAcessoAqui",
  "uuid": "550e8400-e29b-41d4-a716-446655440000" // UUID do jogador no Minecraft
}
```

**Resposta Exemplo (do Plugin para o Bot)**:

```json
{
  "action": "playerRoles",
  "uuid": "550e8400-e29b-41d4-a716-446655440000",
  "roles": {
    "admin": "123456789012345678",     // ID do cargo no Discord para 'admin'
    "member": "987654321098765432"     // ID do cargo no Discord para 'member'
  }
}
```

**Quando Usar**: Sempre que for necessário sincronizar os cargos de um jogador no Minecraft com seus cargos no Discord.

---

### 3. `getPlayerInfo`

**Descrição**: Retorna informações detalhadas sobre o jogador, como nickname, UUID, saldo, habilidades e mana. Esta ação pode ser chamada usando o nome do jogador.

**Requisição Exemplo (do Bot para o Plugin)**:

```json
{
  "action": "getPlayerInfo",
  "apiToken": "seuTokenDeAcessoAqui",
  "playerName": "Tadeu" // Nome do jogador no Minecraft
}
```

**Resposta Exemplo (do Plugin para o Bot)**:

```json
{
  "action": "playerInfo",
  "playerData": {
    "nickname": "Tadeu",
    "uuid": "550e8400-e29b-41d4-a716-446655440000",
    "firstJoinDate": "10-10-2023 15:30:25",
    "isPremium": true,
    "isOnline": true,
    "world": "world_nether",
    "server": "MinecraftServer",
    "balance": 2500.75,
    "farmingLevel": 5,
    "farmingXp": 125.0,
    "currentMana": 50.0,
    "maxMana": 100.0
  }
}
```

**Quando Usar**: Sempre que o bot precisar obter informações detalhadas de um jogador específico.

---

### 4. `getPlayerUUID`

**Descrição**: Retorna o UUID de um jogador a partir de seu nome no Minecraft.

**Requisição Exemplo (do Bot para o Plugin)**:

```json
{
  "action": "getPlayerUUID",
  "apiToken": "seuTokenDeAcessoAqui",
  "playerName": "Tadeu" // Nome do jogador no Minecraft
}
```

**Resposta Exemplo (do Plugin para o Bot)**:

```json
{
  "action": "getPlayerUUID",
  "playerName": "Tadeu",
  "uuid": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Quando Usar**: Sempre que for necessário obter o UUID de um jogador a partir de seu nome no Minecraft.

---

### 5. `sendDirectMessage`

**Descrição**: Envia uma mensagem direta para um usuário no Discord. Esta ação é utilizada pelo plugin para comunicar informações importantes diretamente ao usuário no Discord, como confirmações de vinculação.

**Requisição Exemplo (do Plugin para o Bot)**:

```json
{
  "action": "sendDirectMessage",
  "apiToken": "seuTokenDeAcessoAqui",
  "discordId": "123456789012345678",
  "message": "Sua conta foi vinculada com sucesso!"
}
```

**Quando Usar**: Sempre que o plugin precisar enviar uma notificação direta a um usuário do Discord.

---

### 6. `chatMessage`

**Descrição**: Transmite uma mensagem do chat do jogador para o bot, que deve redirecioná-la ao canal apropriado no Discord. Isso permite integração de chat em tempo real entre Minecraft e Discord.

**Requisição Exemplo (do Plugin para o Bot)**:

```json
{
  "action": "chatMessage",
  "apiToken": "seuTokenDeAcessoAqui",
  "skin": "PlayerSkin",
  "player": "PlayerName",
  "message": "Olá, mundo!",
  "world": "world_nether",
  "server": "MinecraftServer"
}
```

**Quando Usar**: Sempre que uma mensagem no Minecraft precisa ser exibida em um canal de chat no Discord.

---

## Sumário das Ações

| Ação                | Descrição                                                                                                 | Exemplo de Requisição                         |
|---------------------|-----------------------------------------------------------------------------------------------------------|-----------------------------------------------|
| `registerToken`     | Vincula uma conta Discord a uma conta Minecraft.                                                          | Veja o exemplo em **registerToken**           |
| `syncRoles`         | Sincroniza cargos do jogador entre Minecraft e Discord.                                                   | Veja o exemplo em **syncRoles**               |
| `getPlayerInfo`     | Obtém informações detalhadas de um jogador (nickname, saldo, habilidades, mana).                          | Veja o exemplo em **getPlayerInfo**           |
| `getPlayerUUID`     | Retorna o UUID de um jogador a partir de seu nome.                                                        | Veja o exemplo em **getPlayerUUID**           |
| `sendDirectMessage` | Envia uma mensagem direta a um usuário do Discord.                                                        | Veja o exemplo em **sendDirectMessage**       |
| `chatMessage`       | Transmite uma mensagem do jogador no Minecraft para o Discord.                                           | Veja o exemplo em **chatMessage**             |

---

## Como Contribuir

Se você deseja contribuir com este projeto, faça um *fork* do repositório, crie uma nova *branch* com suas modificações e envie uma *pull request* para revisão. Antes de enviar sua contribuição, verifique se o código está documentado e atende aos padrões de qualidade do projeto.

---

Este documento fornece uma visão geral das ações WebSocket para integração entre o servidor Minecraft e o bot Discord, facilitando a comunicação e sincronização de dados entre as duas plataformas.
