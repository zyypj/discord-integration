package com.github.zyypj.discordintegration.commands;

import com.github.zyypj.discordintegration.TokenPlugin;
import com.github.zyypj.discordintegration.manager.TokenManager;
import com.github.zyypj.discordintegration.messages.MessageManager;
import com.github.zyypj.discordintegration.websocket.TokenWebSocket;
import com.google.gson.JsonObject;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class LinkCommand implements CommandExecutor {

    private final TokenManager tokenManager;
    private final TokenWebSocket tokenWebSocket;
    private final MessageManager messageManager;

    public LinkCommand(TokenPlugin plugin) {
        this.tokenManager = plugin.getTokenManager();
        this.tokenWebSocket = plugin.getTokenWebSocket();
        this.messageManager = plugin.getMessageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Este comando só pode ser usado por jogadores.");
            return false;
        }

        if (args.length != 1) {
            sender.sendMessage(messageManager.getMessage("token_command_incorrect_usage"));
            return false;
        }

        Player player = (Player) sender;
        String token = args[0];

        // Recupera os dados do token (id, name, nick) do TokenManager
        Map<String, String> discordData = tokenManager.validateToken(token);

        if (discordData == null) {
            player.sendMessage(messageManager.getMessage("token_invalid"));
            return false;
        }
        //

        // Obtém os dados de ID, nome e apelido do usuário no Discord
        String discordId = discordData.get("id");
        String discordName = discordData.get("name");
        String discordNick = discordData.get("nick");

        // Define os valores para os placeholders
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{USER_NAME}", discordName);
        placeholders.put("{USER_NICK}", discordNick);
        placeholders.put("{PLAYER_NICK}", player.getName());

        // Token válido: envia a confirmação de vinculação para o bot com UUID e Nickname
        JsonObject response = new JsonObject();
        response.addProperty("action", "playerLinked");
        response.addProperty("uuid", player.getUniqueId().toString());
        response.addProperty("discordId", discordId);
        response.addProperty("nickname", player.getName()); // Adiciona o nickname do jogador atual

        tokenWebSocket.broadcast(response.toString());
        player.sendMessage(messageManager.getMessage("account_linked", placeholders));
        return true;
    }
}