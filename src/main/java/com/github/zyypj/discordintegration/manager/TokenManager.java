package com.github.zyypj.discordintegration.manager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TokenManager {

    // Armazena dados de token: chave é o token, e valor é um mapa com informações do Discord
    private final Map<String, Map<String, String>> tokens = new ConcurrentHashMap<>();

    /**
     * Armazena o token e os dados do usuário Discord (ID, nome, apelido) temporariamente.
     */
    public void storeToken(String token, String discordId, String discordName, String discordNick) {
        Map<String, String> discordData = new ConcurrentHashMap<>();
        discordData.put("id", discordId);
        discordData.put("name", discordName);
        discordData.put("nick", discordNick);

        tokens.put(token, discordData);
    }

    /**
     * Valida o token e retorna um mapa com os dados associados ao token,
     * ou null se o token for inválido.
     */
    public Map<String, String> validateToken(String token) {
        return tokens.remove(token); // Remove o token após validação para impedir reuso
    }
}