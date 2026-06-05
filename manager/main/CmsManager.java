package main;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import ui.ManagerUI;

public class CmsManager {
    static final int PORT = 10293;
    static final int INTERVAL = 10;
    static final String AGENT_FILE = "./agents.json";
    public static Map<String, AgentConnection> agents = new HashMap<>();
    public static ManagerUI managerUI;

    public static class AgentConnection {//맵 용 틀
        public String alias;
        public String ip;
        public String pw;
        public Socket socket;
        public PrintWriter out;
        public BufferedReader in;
        public boolean ON_state = false;

        AgentConnection(String alias, String ip, String pw, Socket socket, PrintWriter out, BufferedReader in, boolean ON_state) {
            this.alias = alias;
            this.ip = ip;
            this.pw = pw;
            this.socket = socket;
            this.out = out;
            this.in = in;
            this.ON_state = ON_state;
        }
    }

    public static void main(String[] args) {
        CmsManager backendEngine = new CmsManager();
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("시스템 Look and Feel 설정 실패: " + e.getMessage());
        }

        //Scanner scanner = new Scanner(System.in);
        //System.out.println("명령어 입력 (CONNECT / SHUTDOWN / REBOOT / LOCK)");
        //-------------------------------------------------------------------------------ui
        SwingUtilities.invokeLater(() -> {
            managerUI = new ManagerUI(backendEngine);
            managerUI.drawUI();
        });
        //순서상 이거 뒤에 하는게 더 나아서 이렇게 함
        backendEngine.loadAgents();
        /*
        while (true) {
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;

            if      (input.startsWith("CONNECT"))   backendEngine.connectAgent(input);
            else if (input.startsWith("SHUTDOWN"))  backendEngine.sendCommand(input, "SHUTDOWN");
            else if (input.startsWith("REBOOT"))    backendEngine.sendCommand(input, "REBOOT");
            else if (input.startsWith("LOCK"))      backendEngine.sendCommand(input, "LOCK");
            else if (input.startsWith("EXEC"))      backendEngine.sendCommand(input, "EXEC");
            else System.out.println("알 수 없는 명령어: " + input);
        }*/
    }

    public void connectAgent(String input) {
        String[] parts = input.split(" ");
        if (parts.length < 4) {
            System.out.println("형식 오류: CONNECT [IP] [PW] [ALIAS]");
            return;
        }
        String agentIP = parts[1];
        String agentPW = parts[2];
        String alias = parts[3];

        try {//일단 소켓 연결을 시도 해!
            Socket socket = new Socket(agentIP, PORT);
            //socket.connect(new InetSocketAddress(agentIP, PORT), 3000); // 연결 타임아웃 3초
            socket.setSoTimeout(INTERVAL * 3000);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            JsonObject auth = new JsonObject();
            auth.addProperty("password", agentPW);
            out.println(auth.toString());

            String response = in.readLine();
            JsonObject authResult = JsonParser.parseString(response).getAsJsonObject();
            if (!authResult.get("auth").getAsString().equals("AUTH_OK")) {
                System.out.println("인증 실패");
                socket.close();
                //인증 실패시 오프라인 상태로 ui로 넘김
                AgentConnection offlineAgent = new AgentConnection(alias, agentIP, agentPW, null, null, null, false);
                agents.put(alias, offlineAgent);
                if (managerUI != null) {
                    SwingUtilities.invokeLater(() -> managerUI.loadBackendPcList());
                }
                return;
            }
            System.out.println("인증 성공");

            AgentConnection agent = new AgentConnection(alias, agentIP, agentPW, socket, out, in, true);
            agents.put(alias, agent);
            saveAgents();

            new Thread(() -> {
                try {
                    String json;
                    while ((json = agent.in.readLine()) != null) {
                        System.out.println("[" + alias + "] 수신: " + json);
                        if (managerUI != null) {
                            try {
                                JsonObject jsonObj = JsonParser.parseString(json).getAsJsonObject();

                                // 1. 패킷 타입이 명령어 응답(ACK)인 경우
                                if (jsonObj.has("type") && jsonObj.get("type").getAsString().equals("ACK")) {
                                    managerUI.showCommandResult(alias, jsonObj);
                                }
                                // 2. 그 외의 경우 (기존 실시간 리소스 OSHI 데이터)
                                else {
                                    managerUI.updateRealtimeResource(agentIP, json);
                                }
                            } catch (Exception parseEx) {
                                System.err.println("JSON 파싱 에러: " + parseEx.getMessage());
                            }
                        }
                    }
                } catch (SocketTimeoutException e) {//타임아웃 처리
                    System.out.println("[" + alias + "] 접속 시간 초과 - 연결 해제");
                } catch (Exception e) {//연결해제
                    System.out.println("[" + alias + "] 수신 종료: " + e.getMessage());
                } finally {
                    // 완전히 제거하는 대신 오프라인 상태로 전환하여 UI에 유지
                    AgentConnection offlineAgent = new AgentConnection(alias, agentIP, agentPW, null, null, null, false);
                    agents.put(alias, offlineAgent);
                    System.out.println("[" + alias + "] 오프라인 상태로 전환됨");
                    if (managerUI != null) {
                        SwingUtilities.invokeLater(() -> managerUI.loadBackendPcList());
                    }
                }
            }).start();

        } catch (Exception e) {
            System.out.println("연결 실패 (" + alias + "): " + e.getMessage());
            // 소켓 연결 실패 시 오프라인 상태로 Map에 저장 후 UI 갱신
            AgentConnection offlineAgent = new AgentConnection(alias, agentIP, agentPW, null, null, null, false);
            agents.put(alias, offlineAgent);
            saveAgents();
            if (managerUI != null) {
                SwingUtilities.invokeLater(() -> managerUI.loadBackendPcList());
            }
        }
    }

    public void sendCommand(String input, String cmd) {
        String[] parts = input.split(" ");
        if (parts.length < 2) return;
        String alias = parts[1];

        AgentConnection agent = agents.get(alias);
        if (agent == null) {
            System.out.println("존재하지 않는 Agent 별칭입니다: " + alias);
            return;
        }

        JsonObject json = new JsonObject();
        json.addProperty("cmd", cmd);

        if (cmd.equals("EXEC") && parts.length >= 3) {
            json.addProperty("index", Integer.parseInt(parts[2]));
        }

        agent.out.println(json.toString());
        System.out.println("[송신 → " + alias + "] " + json);
    }

    public void saveAgents() {
        JsonObject root = new JsonObject();
        agents.forEach((alias, agent) -> {
            JsonObject entry = new JsonObject();
            entry.addProperty("ip", agent.ip);
            entry.addProperty("password", agent.pw);
            entry.addProperty("alias", alias);
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
                String input = "CONNECT " + agent.get("ip").getAsString() + " " + agent.get("password").getAsString() + " " + agent.get("alias").getAsString();
                connectAgent(input);
            });
        } catch (Exception e) {
            System.out.println("불러오기 실패: " + e.getMessage());
        }
    }
    public void reloadAgents() { // 새로고침: offline 상태이거나 새로 추가된 에이전트만 연결 시도
        File file = new File(AGENT_FILE);
        if (!file.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            JsonObject root = JsonParser.parseReader(br).getAsJsonObject();

            root.entrySet().forEach(entry -> {
                JsonObject agentData = entry.getValue().getAsJsonObject();
                String alias = agentData.get("alias").getAsString();

                // 현재 CmsManager의 메모리(Map)에 등록된 에이전트 상태 확인
                AgentConnection existingAgent = agents.get(alias);

                // 1. 맵에 존재하지 않음 (프로그램 실행 중 새로 추가됨)
                // 2. 맵에 존재하지만 오프라인 상태임 (ON_state == false)
                // 위 두 가지 경우에만 재연결 시도
                if (existingAgent == null || !existingAgent.ON_state) {
                    String input = "CONNECT "
                            + agentData.get("ip").getAsString() + " "
                            + agentData.get("password").getAsString() + " "
                            + alias;
                    connectAgent(input);
                }
            });
        } catch (Exception e) {
            System.out.println("불러오기 실패: " + e.getMessage());
        }
    }
}
