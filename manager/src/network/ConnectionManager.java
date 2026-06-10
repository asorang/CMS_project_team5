package src.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import src.agent.AgentConnection;
import src.agent.AgentManager;
import src.ui.ManagerUI;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.Map;

/**
 * Agent 연결 수립, 명령 전송, 수신 스레드 담당
 */
public class ConnectionManager {
    private AgentManager agentStore;
    private static final int PORT     = 10293;
    private static final int INTERVAL = 10;

    private final Map<String, AgentConnection> agents;
    private ManagerUI managerUI;

    public ConnectionManager(Map<String, AgentConnection> agents) {
        this.agents = agents;
    }

    public void setManagerUI(ManagerUI managerUI) {
        this.managerUI = managerUI;
    }

    public void connectAgent(String input) {
        String[] parts = input.split(" ");
        if (parts.length < 4) {
            System.out.println("형식 오류: CONNECT [IP] [PW] [ALIAS]");
            return;
        }
        String agentIP = parts[1];
        String agentPW = parts[2];
        String alias   = parts[3];

        try {
            Socket socket = new Socket(agentIP, PORT);
            socket.setSoTimeout(INTERVAL * 3000);
            BufferedReader in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter    out = new PrintWriter(socket.getOutputStream(), true);

            // 인증
            JsonObject auth = new JsonObject();
            auth.addProperty("password", agentPW);
            out.println(auth.toString());

            String response = in.readLine();
            JsonObject authResult = JsonParser.parseString(response).getAsJsonObject();
            if (!authResult.get("auth").getAsString().equals("AUTH_OK")) {
                System.out.println("[" + alias + "] 인증 실패");
                socket.close();
                putOffline(alias, agentIP, agentPW);
                return;
            }
            System.out.println("[" + alias + "] 인증 성공");

            AgentConnection agent = new AgentConnection(alias, agentIP, agentPW, socket, out, in, true);
            agents.put(alias, agent);
            agentStore.saveAgents();
            notifyUI();

            // 수신 스레드
            new Thread(() -> {
                try {
                    String json;
                    while ((json = agent.in.readLine()) != null) {
                        System.out.println("[" + alias + "] 수신: " + json);
                        if (managerUI != null) {
                            try {
                                JsonObject jsonObj = JsonParser.parseString(json).getAsJsonObject();
                                if (jsonObj.has("type") && jsonObj.get("type").getAsString().equals("ACK")) {
                                    managerUI.showCommandResult(alias, jsonObj);
                                } else {
                                    managerUI.updateRealtimeResource(agentIP, json);
                                }
                            } catch (Exception parseEx) {
                                System.err.println("JSON 파싱 에러: " + parseEx.getMessage());
                            }
                        }
                    }
                } catch (SocketTimeoutException e) {
                    System.out.println("[" + alias + "] 접속 시간 초과");
                } catch (Exception e) {
                    System.out.println("[" + alias + "] 수신 종료: " + e.getMessage());
                } finally {
                    putOffline(alias, agentIP, agentPW);
                }
            }).start();

        } catch (Exception e) {
            System.out.println("[" + alias + "] 연결 실패: " + e.getMessage());
            putOffline(alias, agentIP, agentPW);
        }
    }

    public void sendCommand(String input, String cmd) {
        String[] parts = input.split(" ");
        if (parts.length < 2) return;
        String alias = parts[1];

        AgentConnection agent = agents.get(alias);
        if (agent == null || !agent.ON_state) {
            System.out.println("[" + alias + "] 존재하지 않는 Agent입니다. ");
            return;
        }

        JsonObject json = new JsonObject();
        json.addProperty("cmd", cmd);
        if (cmd.equals("EXEC") && parts.length >= 3) {
            json.addProperty("index", Integer.parseInt(parts[2]));
        }

        String payload = json.toString();
        JsonObject wrapper = new JsonObject();
        wrapper.add("payload", json);
        wrapper.addProperty("checksum", checksum(payload));

        agent.out.println(wrapper.toString());
        System.out.println("[송신 → " + alias + "] " + wrapper);
    }

    private void putOffline(String alias, String ip, String pw) {
        if (!agents.containsKey(alias)) return;

        agents.put(alias, new AgentConnection(alias, ip, pw, null, null, null, false));
        System.out.println("[" + alias + "] 오프라인 전환");
        if (agentStore != null) agentStore.saveAgents();
        notifyUI();
    }

    private void notifyUI() {
        if (managerUI != null) {
            SwingUtilities.invokeLater(() -> managerUI.loadBackendPcList());
        }
    }

    private static int checksum(String json) {
        int sum = 0;
        for (char c : json.toCharArray()) sum += c;
        return sum;
    }

    public ManagerUI getManagerUI() { return managerUI; }
    public void setAgentStore(AgentManager agentStore){
        this.agentStore = agentStore;
    }

}
