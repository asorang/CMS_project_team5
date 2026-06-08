package src.ui;

import src.AgentState;

import javax.swing.*;
import java.awt.*;

/**
 * 시스템 트레이 아이콘 초기화 및 관리
 * 애는 AWT임.
 */
public class TrayManager {

    public static void initTray(AgentSettingUI settingUI) {
        if (!SystemTray.isSupported()) {
            System.out.println("시스템 트레이 동작을 지원하지 않는 OS입니다.");
            return;
        }

        PopupMenu popup = new PopupMenu();
        AgentState.statusItem = new MenuItem("... Waiting for Manager ...");
        AgentState.statusItem.setEnabled(false);

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));

        popup.add(AgentState.statusItem);
        popup.addSeparator();
        popup.add(exitItem);

        AgentState.trayIcon = new TrayIcon(AgentState.iconOffline, "JSimpleCMS Agent", popup);
        AgentState.trayIcon.setImageAutoSize(true);

        // 더블클릭 → 설정 창 표시
        AgentState.trayIcon.addActionListener(e ->
                SwingUtilities.invokeLater(() -> {
                    settingUI.setVisible(true);
                    settingUI.toFront();
                })
        );

        try {
            SystemTray.getSystemTray().add(AgentState.trayIcon);
        } catch (AWTException e) {
            System.out.println("트레이 아이콘 등록 실패: " + e.getMessage());
        }
    }
}