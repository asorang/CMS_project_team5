package src.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import src.AgentState;
import src.system.SystemCollector;

import java.io.*;
import java.net.*;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * ServerSocket 시작/재시작 및 연결 처리 담당
 */
public class SocketManager {

    // 소켓 시작
    public static void startSocket() {
        new Thread(() -> {
            try {
                AgentState.serverSocket = new ServerSocket(AgentState.PORT);
                System.out.println("소켓 수신 시작: " + AgentState.PORT);
                while (true) {
                    Socket socket = AgentState.serverSocket.accept();
                    handleConnection(socket);
                }
            } catch (BindException e) {
                // 포트번호가 겹치면 중복 실행으로 감지하고 프로그램 종료
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(
                            null,
                            "Agent 프로그램이 이미 실행 중입니다.\n실행 중이 아니라면, 다른 프로그램이 포트 " + AgentState.PORT + "(을)를 사용 중입니다.",
                            "Agent 중복 감지됨",
                            JOptionPane.ERROR_MESSAGE
                    );
                    System.exit(0);
                });
            } catch (Exception e) {
                System.out.println("소켓 종료: " + e.getMessage());
            }
        }).start();
    }

    // 소켓 재시작
    public static void restartSocket() {
        try {
            if (AgentState.serverSocket != null) AgentState.serverSocket.close();
        } catch (Exception e) {
            System.out.println("소켓 종료 오류: " + e.getMessage());
        }
        startSocket();
    }

    // 연결 처리
    public static void handleConnection(Socket socket) {
        try {
            BufferedReader in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter    out = new PrintWriter(socket.getOutputStream(), true);

            // 인증
            String authString = in.readLine();
            JsonObject authJson = JsonParser.parseString(authString).getAsJsonObject();

            if (!authJson.has("password") || !authJson.get("password").getAsString().equals(AgentState.PASSWORD)) {
                out.println("{\"auth\":\"AUTH_NG\"}");
                socket.close();
                System.out.println("매니저 인증 실패");
                return;
            }

            out.println("{\"auth\":\"AUTH_OK\"}");
            System.out.println("매니저 인증 성공");

            // 트레이 아이콘 온라인으로 변경
            AgentState.trayIcon.setImage(AgentState.iconOnline);
            AgentState.statusItem.setLabel("Connected: " + socket.getInetAddress().getHostAddress());

            // 최초 시스템 정보 전송
            out.println(SystemCollector.systemInfo());

            // 명령 수신 스레드
            new Thread(() -> {
                try {
                    String cmd;
                    while ((cmd = in.readLine()) != null) {
                        try {
                            JsonObject json = JsonParser.parseString(cmd).getAsJsonObject();
                            if (json.has("cmd") && "DISCONNECT".equals(json.get("cmd").getAsString())) {
                                System.out.println("Manager에서 연결을 종료함");
                                socket.close();
                                return;
                            }
                        } catch (Exception ignored) {}
                        SystemCollector.systemCommand(cmd, out);
                    }
                } catch (Exception e) {
                    System.out.println("명령 수신 종료: " + e.getMessage());
                }
            }).start();

            // 주기적 상태 전송
            while (!out.checkError()) {
                out.println(SystemCollector.systemStatus());
                Thread.sleep(AgentState.INTERVAL * 1000);
            }

        } catch (Exception e) {
            System.out.println("매니저 연결 종료: " + e.getMessage());
        } finally {
            AgentState.trayIcon.setImage(AgentState.iconOffline);
            AgentState.statusItem.setLabel("... Waiting for Manager ...");
        }
    }
}