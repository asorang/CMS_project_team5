package src.agent;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import src.network.ConnectionManager;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.SwingUtilities;

/**
 * agents.json 저장/로드 담당
 */
public class AgentManager {

    private static final String AGENT_FILE = "./agents.json";
    private final Map<String, AgentConnection> agents;
    private final ConnectionManager connectionManager;

    public AgentManager(Map<String, AgentConnection> agents, ConnectionManager connectionManager) {
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
        if (!file.exists()) {
            try (FileWriter fw = new FileWriter(file)) {
                fw.write("{}");
                System.out.println("agents.json 생성됨");
            } catch (Exception e) {
                System.out.println("agents.json 생성 실패: " + e.getMessage());
            }
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            JsonObject root = JsonParser.parseReader(br).getAsJsonObject();

            // 전부 오프라인으로 Map에 추가 + 연결 입력 목록 동시 구성
            List<String> connectInputs = new ArrayList<>();
            root.entrySet().forEach(entry -> {
                JsonObject agent = entry.getValue().getAsJsonObject();
                String alias = agent.get("alias").getAsString();
                String ip    = agent.get("ip").getAsString();
                String pw    = agent.get("password").getAsString();
                agents.put(alias, new AgentConnection(alias, ip, pw, null, null, null, false));
                connectInputs.add("CONNECT " + ip + " " + pw + " " + alias);
            });

            // UI 즉시 갱신
            if (connectionManager.getManagerUI() != null) {
                SwingUtilities.invokeLater(() -> connectionManager.getManagerUI().loadBackendPcList());
            }

            // 실제 연결 시도
            connectInputs.forEach(input ->
                    new Thread(() -> connectionManager.connectAgent(input)).start()
            );

        } catch (Exception e) {
            System.out.println("불러오기 실패: " + e.getMessage());
        }
    }

    public void reloadAgents() {
        File file = new File(AGENT_FILE);
        if (!file.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            JsonObject root = JsonParser.parseReader(br).getAsJsonObject();

            // 오프라인이거나 없는 항목만 오프라인으로 추가 후 연결 시도
            List<String> connectInputs = new ArrayList<>();
            root.entrySet().forEach(entry -> {
                JsonObject agentData = entry.getValue().getAsJsonObject();
                String alias = agentData.get("alias").getAsString();
                String ip = agentData.get("ip").getAsString();
                String pw = agentData.get("password").getAsString();

                AgentConnection existing = agents.get(alias);
                if (existing == null || !existing.ON_state) {
                    agents.put(alias, new AgentConnection(alias, ip, pw, null, null, null, false));
                    connectInputs.add("CONNECT " + ip + " " + pw + " " + alias);
                }
            });

            // UI 갱신
            if (connectionManager.getManagerUI() != null) {
                SwingUtilities.invokeLater(() -> connectionManager.getManagerUI().loadBackendPcList());
            }

            // 백그라운드에서 연결 시도
            connectInputs.forEach(input ->
                    new Thread(() -> connectionManager.connectAgent(input)).start()
            );

        } catch (Exception e) {
            System.out.println("불러오기 실패: " + e.getMessage());
        }
    }
}