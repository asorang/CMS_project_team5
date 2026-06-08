package src.agent;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import src.network.ConnectionManager;

import java.io.*;
import java.util.Map;

/**
 * agents.json 저장/로드 담당
 */
public class AgentStore {

    private static final String AGENT_FILE = "./agents.json";
    private final Map<String, AgentConnection> agents;
    private final ConnectionManager connectionManager;

    public AgentStore(Map<String, AgentConnection> agents, ConnectionManager connectionManager) {
        this.agents            = agents;
        this.connectionManager = connectionManager;
    }

    public void saveAgents() {
        JsonObject root = new JsonObject();
        agents.forEach((alias, agent) -> {
            JsonObject entry = new JsonObject();
            entry.addProperty("ip",       agent.ip);
            entry.addProperty("password", agent.pw);
            entry.addProperty("alias",    alias);
            root.add(alias, entry);
        });
        try (FileWriter fw = new FileWriter(AGENT_FILE)) {
            fw.write(root.toString());
        } catch (Exception e) {
            System.out.println("저장 실패: " + e.getMessage());
        }
    }

    public void loadAgents() {
        File file = new File(AGENT_FILE);
        if (!file.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            JsonObject root = JsonParser.parseReader(br).getAsJsonObject();
            root.entrySet().forEach(entry -> {
                JsonObject agent = entry.getValue().getAsJsonObject();
                String input = "CONNECT "
                        + agent.get("ip").getAsString() + " "
                        + agent.get("password").getAsString() + " "
                        + agent.get("alias").getAsString();
                connectionManager.connectAgent(input);
            });
        } catch (Exception e) {
            System.out.println("불러오기 실패: " + e.getMessage());
        }
    }

    public void reloadAgents() {
        File file = new File(AGENT_FILE);
        if (!file.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            JsonObject root = JsonParser.parseReader(br).getAsJsonObject();
            root.entrySet().forEach(entry -> {
                JsonObject agentData = entry.getValue().getAsJsonObject();
                String alias = agentData.get("alias").getAsString();

                AgentConnection existing = agents.get(alias);
                if (existing == null || !existing.ON_state) {
                    String input = "CONNECT "
                            + agentData.get("ip").getAsString() + " "
                            + agentData.get("password").getAsString() + " "
                            + alias;
                    connectionManager.connectAgent(input);
                }
            });
        } catch (Exception e) {
            System.out.println("불러오기 실패: " + e.getMessage());
        }
    }
}
