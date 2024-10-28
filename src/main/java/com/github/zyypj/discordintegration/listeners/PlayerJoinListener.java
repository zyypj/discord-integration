package com.github.zyypj.discordintegration.listeners;

import com.github.zyypj.discordintegration.TokenPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final TokenPlugin plugin;

    public PlayerJoinListener(TokenPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Atualiza o nickname no WebSocket quando o jogador entra no servidor
        plugin.getTokenWebSocket().updatePlayerNickname(event.getPlayer().getUniqueId(), event.getPlayer().getName());
    }

    //
}