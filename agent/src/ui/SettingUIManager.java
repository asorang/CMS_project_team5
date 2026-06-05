package src.ui;

import src.AgentState;
import src.config.ConfigManager;
import src.network.SocketManager;

import javax.swing.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;

/**
 * AgentSettingUI 초기화 및 버튼 이벤트 연결 담당
 */

public class SettingUIManager {

    public static AgentSettingUI createAndSetup() {
        AgentSettingUI settingUI = new AgentSettingUI();
        setupUI(settingUI);
        return settingUI;
    }

    private static void setupUI(AgentSettingUI settingUI) {
        // 설정값 주입
        String[] names = new String[3];
        String[] paths = new String[3];
        for (int i = 0; i < Math.min(AgentState.PROGRAMS.size(), 3); i++) {
            names[i] = AgentState.PROGRAMS.get(i)[0];
            paths[i] = AgentState.PROGRAMS.get(i)[1];
        }
        settingUI.setInitialData(String.valueOf(AgentState.PORT), AgentState.PASSWORD, names, paths);
        settingUI.setStatusIp(getLocalIp());

        // 저장 버튼
        settingUI.getSaveButton().addActionListener(e -> {
            if (!settingUI.isModified()) {
                JOptionPane.showMessageDialog(
                        settingUI,
                        "변경 사항이 없습니다.",
                        "저장 실패",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            AgentState.PASSWORD = settingUI.getPassword();
            AgentState.PROGRAMS.clear();
            for (int i = 0; i < 3; i++) {
                String name = settingUI.getShortcutName(i);
                String path = settingUI.getShortcutPath(i);
                if (name != null && !name.isBlank())
                    AgentState.PROGRAMS.add(new String[]{name, path});
            }
            ConfigManager.saveConfig();
            SocketManager.restartSocket();
            settingUI.setVisible(false);
        });

        // 취소 버튼
        settingUI.getCancelButton().addActionListener(e -> settingUI.setVisible(false));
    }

    private static String getLocalIp() {
        try {
            // 루프백 제외한 첫 번째 IPv4 반환
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                    if (!addr.isLoopbackAddress() && addr.getHostAddress().contains(".")) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            return "IP 확인 불가";
        }
        return "IP 확인 불가";
    }
}
