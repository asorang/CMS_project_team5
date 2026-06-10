package src;

import src.agent.AgentConnection;
import src.network.ConnectionManager;
import src.agent.AgentManager;
import src.ui.ManagerUI;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * CMS Manager 진입점
 *
 * 실행 순서:
 *   1. LookAndFeel + 폰트 설정
 *   2. ConnectionManager, AgentStore 초기화
 *   3. UI 초기화
 *   4. agents.json 로드 + 자동 접속
 */
public class CmsManager {

    public static Map<String, AgentConnection> agents = new HashMap<>();
    public static Font baseFont;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            baseFont = Font.createFont(
                    Font.TRUETYPE_FONT, CmsManager.class.getResourceAsStream("/resource/font.ttf")
            ).deriveFont(13f);

            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(baseFont);
            UIManager.put("Label.font", baseFont);
            UIManager.put("Button.font", baseFont);
            UIManager.put("TextField.font",baseFont);
            UIManager.put("ComboBox.font", baseFont);
            UIManager.put("List.font", baseFont);
            UIManager.put("OptionPane.messageFont", baseFont);
            UIManager.put("OptionPane.buttonFont", baseFont);
        } catch (Exception e) {
            System.err.println("UI 설정 실패: " + e.getMessage());
        }

        // 2. 백엔드 초기화
        ConnectionManager connectionManager = new ConnectionManager(agents);
        AgentManager agentStore = new AgentManager(agents, connectionManager);
        connectionManager.setAgentStore(agentStore);
        // 3. UI 초기화
        SwingUtilities.invokeLater(() -> {
            ManagerUI managerUI = new ManagerUI(connectionManager, agentStore);
            connectionManager.setManagerUI(managerUI);
            managerUI.drawUI();

            // 4. UI 초기화 완료 후 자동 접속
            new Thread(() -> agentStore.loadAgents()).start();
        });
    }
}
