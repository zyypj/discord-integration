package com.github.zyypj.discordintegration.messages;

import com.github.zyypj.discordintegration.TokenPlugin;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Map;

public class MessageManager {

    private final TokenPlugin plugin;
    private FileConfiguration messagesConfig;

    public MessageManager(TokenPlugin plugin) {
        this.plugin = plugin;
        loadMessagesConfig();
    }

    private void loadMessagesConfig() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false); // Cria o arquivo se ele não existir
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    /**
     * Retorna a mensagem formatada, substituindo as variáveis, se aplicável.
     *
     * @param path   O caminho da mensagem no arquivo messages.yml
     * @param placeholders Um mapa de variáveis para substituir na mensagem
     * @return A mensagem formatada
     */
    public String getMessage(String path, Map<String, String> placeholders) {
        String message = messagesConfig.getString("messages." + path, "Mensagem não encontrada: " + path);
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Método sobrecarregado para obter uma mensagem sem placeholders.
     *
     * @param path O caminho da mensagem no arquivo messages.yml
     * @return A mensagem formatada
     */
    public String getMessage(String path) {
        return getMessage(path, null);
    }
}
