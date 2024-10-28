package com.github.zyypj.discordintegration.listeners;

import com.github.zyypj.discordintegration.TokenPlugin;
import com.github.zyypj.discordintegration.websocket.TokenWebSocket;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final TokenPlugin plugin;
    private final TokenWebSocket tokenWebSocket;

    public ChatListener(TokenPlugin plugin) {
        this.plugin = plugin;
        this.tokenWebSocket = plugin.getTokenWebSocket();
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        String world = player.getWorld().getName();
        String server = Bukkit.getServer().getName();  // Nome do servidor
        String skin = player.getName();  // Usando o nome do jogador como identificador de skin

        // Chama o método para enviar a mensagem pelo WebSocket
        tokenWebSocket.sendChatMessage(player, skin, message, world, server);
    }
}