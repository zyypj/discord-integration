package com.github.zyypj.discordintegration.websocket;

import com.github.zyypj.discordintegration.TokenPlugin;
import com.github.zyypj.discordintegration.manager.TokenManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
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

public class TokenWebSocket extends WebSocketServer {

    private final TokenManager tokenManager;
    private final TokenPlugin plugin;
    private final Map<UUID, String> playerNicknameCache = new ConcurrentHashMap<>();

    public TokenWebSocket(TokenPlugin plugin, String websocketUrl) {
        super(parseWebSocketURI(websocketUrl));
        this.plugin = plugin;
        this.tokenManager = plugin.getTokenManager();
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
            default:
                JsonObject response = new JsonObject();
                response.addProperty("registerToken", "Registra um token");
                response.addProperty("syncRoles", "Recarrega os ID dos cargos");
                response.addProperty("getPlayerInfo", "Retorna informações do jogador");

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

    private void handleGetPlayerInfo(JsonObject json, WebSocket webSocket) {
        String uuidString = json.get("uuid").getAsString();
        UUID uuid = UUID.fromString(uuidString);
        String nickname = playerNicknameCache.get(uuid);

        JsonObject response = new JsonObject();
        response.addProperty("action", "playerInfo");
        response.addProperty("uuid", uuid.toString());
        response.addProperty("nickname", nickname != null ? nickname : "Desconhecido");

        webSocket.send(response.toString());
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

    //
}