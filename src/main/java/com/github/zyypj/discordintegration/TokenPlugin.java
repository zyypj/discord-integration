package com.github.zyypj.discordintegration;

import com.github.zyypj.discordintegration.commands.LinkCommand;
import com.github.zyypj.discordintegration.listeners.ChatListener;
import com.github.zyypj.discordintegration.listeners.PlayerJoinListener;
import com.github.zyypj.discordintegration.manager.TokenManager;
import com.github.zyypj.discordintegration.messages.MessageManager;
import com.github.zyypj.discordintegration.websocket.TokenWebSocket;
import lombok.Getter;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

@Getter
public class TokenPlugin extends JavaPlugin {

    private TokenManager tokenManager;
    private MessageManager messageManager;
    private TokenWebSocket tokenWebSocket;
    private LuckPerms luckPerms;
    private FileConfiguration rolesConfig;
    private Economy econ;

    @Override
    public void onEnable() {

        long startTime = System.currentTimeMillis();
        log(" ");
        log("&eIniciando plugin para integração...");

        saveDefaultConfig();

        loadManagers();
        startWebSocket();
        registerCommands();
        registerListeners();

        this.luckPerms = LuckPermsProvider.get();

        loadRolesConfig();

        setupEconomy();

        long endTime = System.currentTimeMillis() - startTime;
        log(" ");
        log("&aIntegração concluida em " + endTime + "ms!");
    }

    // Carrega os gerenciadores (token and messages)
    private void loadManagers() {

        long startTime = System.currentTimeMillis();
        log(" ");
        log("&eIniciando gerenciadores...");

        this.tokenManager = new TokenManager();
        this.messageManager = new MessageManager(this);

        long endTime = System.currentTimeMillis() - startTime;
        log("&aGerenciadores iniciados em " + endTime + "ms!");

    }

    // Carrega o webSocket e o conecta na url destinada
    private void startWebSocket() {

        long startTime = System.currentTimeMillis();
        log(" ");

        String websocketUrl = getConfig().getString("websocket.url", "ws://localhost:8080");

        log("&eConectando WebSocket em " + websocketUrl);

        this.tokenWebSocket = new TokenWebSocket(this, websocketUrl);
        this.tokenWebSocket.start();

        long endTime = System.currentTimeMillis() - startTime;
        log("&aWebSocket conectado em " + endTime + "ms!");

    }

    // Registra os comandos
    private void registerCommands() {

        long startTime = System.currentTimeMillis();
        log(" ");
        log("&eRegistrando comandos...");

        getCommand("vincular").setExecutor(new LinkCommand(this));

        long endTime = System.currentTimeMillis() - startTime;
        log("&aComandos registrados em " + endTime + "ms!");
    }

    private void registerListeners() {

        long startTime = System.currentTimeMillis();
        log(" ");
        log("&eRegistrando listeners...");

        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(this), this);

        long endTime = System.currentTimeMillis() - startTime;
        log("&aListeners registrados em " + endTime + "ms!");
    }

    // Carrega as permissões do LuckPerms
    private void loadRolesConfig() {
        File rolesFile = new File(getDataFolder(), "roles.yml");

        // Carregar a configuração existente ou criar um novo arquivo se ele não existir
        if (!rolesFile.exists()) {
            try {
                rolesFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        rolesConfig = YamlConfiguration.loadConfiguration(rolesFile);

        // Obter todos os grupos do LuckPerms
        Collection<Group> groups = getLuckPerms().getGroupManager().getLoadedGroups();
        for (Group group : groups) {
            String groupName = group.getName();

            // Adiciona o grupo ao arquivo roles.yml se ainda não existir
            if (!rolesConfig.contains(groupName)) {
                rolesConfig.set(groupName + ".id", "default"); // Define o ID inicial como "default"
            }
        }

        // Salvar as atualizações no roles.yml
        try {
            rolesConfig.save(rolesFile);
        } catch (IOException e) {
            getLogger().severe("Não foi possível salvar o arquivo roles.yml!");
            e.printStackTrace();
        }
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return true;
    }

    private void log(String message) {
        Bukkit.getConsoleSender().sendMessage(message.replace("&", "§"));
    }
}