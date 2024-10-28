# Discord-Integration Documentation

Este documento descreve as ações e o formato de comunicação via WebSocket entre o servidor Minecraft e o bot Discord usando o `TokenWebSocket`. O `TokenWebSocket` permite a sincronização de informações entre o jogador no Minecraft e o bot no Discord, como vinculação de tokens, sincronização de cargos e obtenção de informações de jogadores.

## Ações Disponíveis

Cada ação é identificada pelo campo `"action"` no JSON enviado para o WebSocket. Abaixo estão os detalhes de cada ação.

---

### 1. `registerToken`

**Descrição**: Registra um token para vincular uma conta do Discord a um jogador do Minecraft. Essa ação deve ser chamada pelo bot quando um novo token é criado para um jogador.

#### Requisição Exemplo:

```json
{
  "action": "registerToken",
  "token": "abc123xyz",       // Token gerado pelo bot
  "id": "123456789012345678", // Discord ID do usuário
  "name": "DiscordUser",      // Nome de usuário no Discord
  "nick": "DiscordNick"       // Apelido do usuário no Discord, se tiver
}
```

**Quando Usar**: Sempre que um novo token é criado para vinculação entre o Discord e o Minecraft.

---

### 2. `syncRoles`

**Descrição**: Sincroniza os cargos de um jogador no Minecraft com o bot no Discord. O bot pode chamar essa ação para atualizar os cargos de um jogador no Discord com base nas permissões dele no Minecraft.

#### Requisição Exemplo:

```json
{
  "action": "syncRoles",
  "uuid": "550e8400-e29b-41d4-a716-446655440000" // UUID do jogador no Minecraft
}
```

#### Resposta Exemplo (do Servidor para o Bot):

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

**Quando Usar**: Sempre que for necessário sincronizar os cargos de um jogador no Minecraft com os cargos no Discord.

---

### 3. `getPlayerInfo`

**Descrição**: Retorna o `UUID` e o `nickname` atual de um jogador no Minecraft a partir do cache. Útil para o bot garantir que está usando o nickname mais recente do jogador.

#### Requisição Exemplo:

```json
{
  "action": "getPlayerInfo",
  "uuid": "550e8400-e29b-41d4-a716-446655440000" // UUID do jogador no Minecraft
}
```

#### Resposta Exemplo (do Servidor para o Bot):

```json
{
  "action": "playerInfo",
  "uuid": "550e8400-e29b-41d4-a716-446655440000",
  "nickname": "PlayerNickname" // Nickname atual do jogador no Minecraft
}
```

**Quando Usar**: Quando o bot precisar obter informações atualizadas de um jogador específico.

---

### 4. `updatePlayerNickname`

**Descrição**: Atualiza o cache com o nickname do jogador. Esse método é chamado automaticamente pelo servidor quando um jogador entra no Minecraft para garantir que o cache contenha o nickname mais recente.

**Parâmetros**:

- `uuid`: UUID do jogador.
- `nickname`: Nickname atual do jogador no Minecraft.

**Observação**: Esta função é chamada pelo servidor internamente quando o jogador entra no Minecraft, portanto, o bot não precisa chamá-la diretamente.

---

## Resumo das Ações

| Ação            | Quando Chamar                        | Requisição Exemplo                                      |
|-----------------|-------------------------------------|--------------------------------------------------------|
| `registerToken` | Quando um novo token é criado       | Veja o exemplo acima em **registerToken**              |
| `syncRoles`     | Para sincronizar cargos do jogador  | Veja o exemplo acima em **syncRoles**                  |
| `getPlayerInfo` | Para obter informações de um jogador| Veja o exemplo acima em **getPlayerInfo**              |
| `updatePlayerNickname` | Atualização automática ao entrar no servidor | Chamada internamente pelo servidor                     |

---

## Contribuição

Para contribuir com o projeto ou sugerir melhorias, faça um *fork* e envie uma *pull request* com suas modificações.

---

## Licença

Este projeto está sob a licença MIT. Para mais detalhes, consulte o arquivo [LICENSE](LICENSE).
```

Este `README.md` fornece uma documentação completa das ações do WebSocket para o bot de integração com o servidor Minecraft, com exemplos claros de como utilizá-las.
