package com.github.zyypj.discordintegration.websocket;

import com.github.zyypj.discordintegration.TokenPlugin;
import com.github.zyypj.discordintegration.manager.TokenManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.skill.Skills;
import dev.aurelium.auraskills.api.user.SkillsUser;
import lombok.Getter;
import me.qKing12.RoyaleEconomy.API.Balance;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Getter
public class TokenWebSocket extends WebSocketServer {

    private final String apiToken;

    private final TokenManager tokenManager;
    private final TokenPlugin plugin;
    private final Map<UUID, String> playerNicknameCache = new ConcurrentHashMap<>();

    AuraSkillsApi auraSkills = AuraSkillsApi.get();

    public TokenWebSocket(TokenPlugin plugin, String websocketUrl) {
        super(parseWebSocketURI(websocketUrl));
        this.plugin = plugin;
        this.tokenManager = plugin.getTokenManager();

        FileConfiguration config = plugin.getConfig();
        this.apiToken = config.getString("websocket.apiToken");
    }

    private static InetSocketAddress parseWebSocketURI(String websocketUrl) {
        try {
            URI uri = new URI(websocketUrl);
            return new InetSocketAddress(uri.getHost(), uri.getPort());
        } catch (Exception e) {
            throw new RuntimeException("URL inválida para o WebSocket: " + websocketUrl, e);
        }
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake handshake) {
        plugin.getLogger().log(Level.INFO, "Nova conexão WebSocket aberta: " + webSocket.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket webSocket, int code, String reason, boolean remote) {
        plugin.getLogger().log(Level.INFO, "Conexão WebSocket fechada: " + webSocket.getRemoteSocketAddress() + " Motivo: " + reason);
    }

    @Override
    public void onMessage(WebSocket webSocket, String message) {
        JsonObject json = new JsonParser().parse(message).getAsJsonObject();

        // Verifica o token de acesso
        if (!json.has("apiToken") || !json.get("apiToken").getAsString().equals(apiToken)) {
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Acesso negado: Token inválido ou ausente.");
            webSocket.send(errorResponse.toString());
            plugin.getLogger().warning("Tentativa de acesso com token inválido ou ausente de: " + webSocket.getRemoteSocketAddress());
            return;
        }

        String action = json.get("action").getAsString();

        switch (action) {
            case "registerToken":
                handleRegisterToken(json);
                break;
            case "syncRoles":
                handleSyncRoles(json, webSocket);
                break;
            case "getPlayerInfo":
                handleGetPlayerInfo(json, webSocket);
                break;
            case "getPlayerUUID":
                if (json.has("playerName")) {
                    String playerName = json.get("playerName").getAsString();
                    UUID playerUUID = getUUIDFromPlayerName(playerName);

                    JsonObject uuidResponse = new JsonObject();
                    uuidResponse.addProperty("action", "getPlayerUUID");
                    uuidResponse.addProperty("playerName", playerName);
                    if (playerUUID != null) {
                        uuidResponse.addProperty("uuid", playerUUID.toString());
                    } else {
                        uuidResponse.addProperty("error", "Jogador não encontrado.");
                    }
                    webSocket.send(uuidResponse.toString());
                } else {
                    JsonObject errorResponse = new JsonObject();
                    errorResponse.addProperty("error", "Parâmetro 'playerName' ausente.");
                    webSocket.send(errorResponse.toString());
                }
                break;
            default:
                JsonObject response = new JsonObject();
                response.addProperty("registerToken", "Registra um token");
                response.addProperty("syncRoles", "Recarrega os ID dos cargos");
                response.addProperty("getPlayerInfo", "Retorna informações do jogador");
                response.addProperty("getPlayerUUID", "Retorna o UUID de um jogador pelo nome");

                webSocket.send(response.toString());
        }
    }

    private void handleRegisterToken(JsonObject json) {
        if (json.has("token") && json.has("id") && json.has("name") && json.has("nick")) {
            String token = json.get("token").getAsString();
            String discordId = json.get("id").getAsString();
            String discordName = json.get("name").getAsString();
            String discordNick = json.get("nick").getAsString();

            tokenManager.storeToken(token, discordId, discordName, discordNick);
            plugin.getLogger().log(Level.INFO, "Token registrado para Discord ID: " + discordId);
        } else {
            plugin.getLogger().log(Level.WARNING, "Registro de token falhou: campos obrigatórios ausentes.");
        }
    }

    private void handleSyncRoles(JsonObject json, WebSocket webSocket) {
        String uuid = json.get("uuid").getAsString();
        Player player = plugin.getServer().getPlayer(UUID.fromString(uuid));

        if (player != null && player.isOnline()) {
            sendPlayerRoles(player, webSocket);
        } else {
            plugin.getLogger().log(Level.WARNING, "Jogador não encontrado ou offline: UUID=" + uuid);
        }
    }

    public void sendPlayerRoles(Player player, WebSocket webSocket) {
        // Obtém o usuário do LuckPerms
        User user = plugin.getLuckPerms().getUserManager().getUser(player.getUniqueId());

        // Cria o objeto de resposta JSON
        JsonObject response = new JsonObject();
        response.addProperty("action", "playerRoles");
        response.addProperty("uuid", player.getUniqueId().toString());

        // Carrega a configuração de cargos (roles.yml)
        FileConfiguration rolesConfig = plugin.getRolesConfig();
        JsonObject rolesJson = new JsonObject();

        // Obtém os grupos do jogador do LuckPerms e mapeia para os IDs dos cargos do Discord
        if (user != null) {
            List<String> playerGroups = user.getInheritedGroups((QueryOptions) plugin.getLuckPerms().getContextManager().getStaticContext()).stream()
                    .map(Group::getName)
                    .collect(Collectors.toList());

            for (String group : playerGroups) {
                if (rolesConfig.contains(group)) {
                    String roleId = rolesConfig.getString(group + ".id", "default");
                    rolesJson.addProperty(group, roleId);
                }
            }
        }

        // Adiciona os cargos ao JSON de resposta e envia ao WebSocket
        response.add("roles", rolesJson);
        webSocket.send(response.toString());
    }

    public UUID getUUIDFromPlayerName(String playerName) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (offlinePlayer != null && offlinePlayer.hasPlayedBefore()) {
            return offlinePlayer.getUniqueId();
        } else {
            plugin.getLogger().warning("Jogador não encontrado: " + playerName);
            return null;
        }
    }

    private void handleGetPlayerInfo(JsonObject json, WebSocket webSocket) {
        // Obter o nome do jogador a partir do JSON
        String playerName = json.get("playerName").getAsString();
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);

        if (offlinePlayer == null) {
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Jogador não encontrado: " + playerName);
            webSocket.send(errorResponse.toString());
            plugin.getLogger().warning("Tentativa de acesso para jogador não encontrado: " + playerName);
            return;
        }

        boolean isOnline = offlinePlayer.isOnline();
        Player player = isOnline ? Bukkit.getPlayer(offlinePlayer.getUniqueId()) : null;

        JsonObject playerInfo = new JsonObject();

        // Adicionar informações básicas
        playerInfo.addProperty("nickname", offlinePlayer.getName());
        playerInfo.addProperty("uuid", offlinePlayer.getUniqueId().toString());

        long firstJoinTime = offlinePlayer.getFirstPlayed();
        String firstJoinDate = firstJoinTime > 0 ? new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new java.util.Date(firstJoinTime)) : "Desconhecido";
        playerInfo.addProperty("firstJoinDate", firstJoinDate);
        playerInfo.addProperty("isPremium", offlinePlayer.hasPlayedBefore());
        playerInfo.addProperty("isOnline", isOnline);

        if (isOnline && player != null) {
            playerInfo.addProperty("world", player.getWorld().getName());
            playerInfo.addProperty("server", Bukkit.getServer().getName());
        }

        // Obter o saldo do jogador usando a API RoyaleEconomy
        Balance balanceAPI = new Balance();
        double moneyBalance = balanceAPI.getBalance(offlinePlayer.getName());
        playerInfo.addProperty("balance", moneyBalance);

        // Obter habilidades e mana usando a API AuraSkills
        if (player != null) {
            SkillsUser user = auraSkills.getUser(player.getUniqueId());
            if (user != null) {
                int farmingLevel = user.getSkillLevel(Skills.FARMING);
                double farmingXp = user.getSkillXp(Skills.FARMING);
                playerInfo.addProperty("farmingLevel", farmingLevel);
                playerInfo.addProperty("farmingXp", farmingXp);

                double currentMana = user.getMana();
                double maxMana = user.getMaxMana();
                playerInfo.addProperty("currentMana", currentMana);
                playerInfo.addProperty("maxMana", maxMana);
            } else {
                plugin.getLogger().warning("Usuário não encontrado para o nome do jogador: " + playerName);
            }
        }

        // Envia as informações do jogador de volta ao WebSocket
        JsonObject response = new JsonObject();
        response.addProperty("action", "playerInfo");
        response.add("playerData", playerInfo);

        webSocket.send(response.toString());
    }

    public void sendDirectMessage(String discordId, String message) {
        // Cria o objeto JSON com as informações necessárias
        JsonObject json = new JsonObject();
        json.addProperty("action", "sendDirectMessage");
        json.addProperty("discordId", discordId);
        json.addProperty("message", message);

        // Envia o objeto JSON para todos os clientes WebSocket conectados
        for (WebSocket conn : getConnections()) {
            conn.send(json.toString());
        }
    }

    // Método para enviar mensagens de chat pelo WebSocket
    public void sendChatMessage(Player player, String skin, String message, String world, String server) {
        JsonObject chatMessage = new JsonObject();
        chatMessage.addProperty("action", "chatMessage");
        chatMessage.addProperty("skin", skin);
        chatMessage.addProperty("player", player.getDisplayName());
        chatMessage.addProperty("message", message);
        chatMessage.addProperty("world", world);
        chatMessage.addProperty("server", server);

        // Envia a mensagem para todos os clientes WebSocket conectados
        for (WebSocket conn : getConnections()) {
            conn.send(chatMessage.toString());
        }
    }

    /**
     * Atualiza e armazena o nickname do jogador no cache.
     *
     * @param uuid     UUID do jogador
     * @param nickname Novo nickname do jogador
     */
    public void updatePlayerNickname(UUID uuid, String nickname) {
        playerNicknameCache.put(uuid, nickname);
        plugin.getLogger().log(Level.INFO, "Nickname atualizado no cache: " + nickname);
    }

    @Override
    public void onError(WebSocket webSocket, Exception ex) {
        plugin.getLogger().log(Level.SEVERE, "Erro no WebSocket: " + (webSocket != null ? webSocket.getRemoteSocketAddress() : "desconhecido"), ex);
    }

    @Override
    public void onStart() {
        plugin.getLogger().log(Level.INFO, "Servidor WebSocket iniciado com sucesso.");
    }
}