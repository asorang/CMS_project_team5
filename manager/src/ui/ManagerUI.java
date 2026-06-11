package src.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import src.agent.AgentConnection;
import src.network.ConnectionManager;
import src.agent.AgentManager;
import src.CmsManager;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * CMS Manager 메인 UI
 */
public class ManagerUI {

    private final ConnectionManager connectionManager;
    private final AgentManager agentStore;

    private JList<PcAgentData> pcJList;
    private DefaultListModel<PcAgentData> listModel;
    private CardLayout rightCardLayout;
    private JPanel rightPanel;

    private JLabel mainTitleLabel;
    private JLabel osValueLabel;
    private JLabel cpuValueLabel;
    private JLabel ramValueLabel;
    private JLabel diskValueLabel;

    private JProgressBar cpuProgressBar;
    private JLabel cpuUsageLabel;

    private JProgressBar memoryProgressBar;
    private JLabel memoryValueLabel;

    private JComboBox<String> systemActionCombo;
    private JLabel offlineTitleLabel;
    private JLabel offlineCardLabel;
    private JComboBox<String> shortcutCombo;
    private JButton runShortcutButton;

    public ManagerUI(ConnectionManager connectionManager, AgentManager agentStore) {
        this.connectionManager = connectionManager;
        this.agentStore        = agentStore;
    }

    public void drawUI() {
        JFrame frame = new JFrame("JSimpleCMS Management System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(960, 640);
        frame.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.add(createTopToolbar(frame), BorderLayout.NORTH);
        mainPanel.add(createCentralSplitLayout(frame), BorderLayout.CENTER);

        frame.add(mainPanel);
        frame.setVisible(true);

        loadBackendPcList();
    }

    private JPanel createTopToolbar(JFrame parentFrame) {
        JPanel toolbarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15));
        toolbarPanel.setBackground(Color.WHITE);

        JButton addButton =  new JButton(loadIcon("/resource/icon_new.png", 18));
        addButton.setFont(CmsManager.baseFont.deriveFont(Font.BOLD, 16));
        addButton.setPreferredSize(new Dimension(35, 35));
        addButton.setFocusPainted(false);
        addButton.setBackground(new Color(224, 224, 224));
        addButton.setBorder(BorderFactory.createEmptyBorder());
        addButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addButton.addActionListener(e -> showAddAgentDialog(parentFrame));

        JButton refreshButton = new JButton(loadIcon("/resource/icon_refresh.png", 18));
        refreshButton.setFont(CmsManager.baseFont.deriveFont(Font.BOLD, 16));
        refreshButton.setPreferredSize(new Dimension(35, 35));
        refreshButton.setFocusPainted(false);
        refreshButton.setBackground(new Color(224, 224, 224));
        refreshButton.setBorder(BorderFactory.createEmptyBorder());
        refreshButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshButton.addActionListener(e -> {
            refreshButton.setEnabled(false);
            refreshButton.setIcon(loadIcon("/resource/icon_loading.png", 18));
            new Thread(() -> {
                agentStore.reloadAgents();
                SwingUtilities.invokeLater(() -> {
                    // loadBackendPcList() 제거 — reloadAgents() 내부에서 이미 처리됨
                    rightCardLayout.show(rightPanel, "BLANK_VIEW");
                    refreshButton.setIcon(loadIcon("/resource/icon_refresh.png", 18));
                    refreshButton.setEnabled(true);
                    JOptionPane.showMessageDialog(parentFrame,
                            "에이전트 리스트를 새로고침했습니다.",
                            "완료", JOptionPane.INFORMATION_MESSAGE);
                });
            }).start();
        });

        toolbarPanel.add(addButton);
        toolbarPanel.add(refreshButton);
        return toolbarPanel;
    }

    private JPanel createCentralSplitLayout(JFrame parentFrame) {
        JPanel splitPanel = new JPanel(new BorderLayout(15, 0));
        splitPanel.setBackground(Color.WHITE);
        splitPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 15, 15));
        splitPanel.add(createLeftNavigationArea(), BorderLayout.WEST);
        splitPanel.add(createRightDisplayArea(), BorderLayout.CENTER);
        return splitPanel;
    }

    private JPanel createLeftNavigationArea() {
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(260, 0));
        leftPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));

        listModel = new DefaultListModel<>();
        pcJList   = new JList<>(listModel);
        pcJList.setBackground(new Color(245, 245, 245));
        pcJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        pcJList.setCellRenderer(new PcListCellRenderer());

        pcJList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                PcAgentData selected = pcJList.getSelectedValue();
                if (selected != null) bindPcDataToRightDetailPanel(selected);
            }
        });

        JScrollPane scrollPane = new JScrollPane(pcJList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        leftPanel.add(scrollPane, BorderLayout.CENTER);
        return leftPanel;
    }

    private JPanel createRightDisplayArea() {
        rightCardLayout = new CardLayout();
        rightPanel      = new JPanel(rightCardLayout);
        rightPanel.setBackground(Color.WHITE);
        rightPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));

        JPanel blankView = new JPanel(new BorderLayout());
        blankView.setBackground(Color.WHITE);
        JLabel defaultMessage = new JLabel("<html>[+]버튼을 눌러 새로운 에이전트 PC를 추가하거나,<br>조회할 에이전트 PC를 좌측 목록에서 선택하십시오.<html>", SwingConstants.CENTER);
        defaultMessage.setFont(CmsManager.baseFont.deriveFont(Font.PLAIN, 15));
        defaultMessage.setForeground(Color.GRAY);
        blankView.add(defaultMessage, BorderLayout.CENTER);

        rightPanel.add(blankView,             "BLANK_VIEW");
        rightPanel.add(createOnlineRoomLayout(),  "ONLINE_VIEW");
        rightPanel.add(createOfflineRoomLayout(), "OFFLINE_VIEW");

        rightCardLayout.show(rightPanel, "BLANK_VIEW");
        return rightPanel;
    }

    private JPanel createOnlineRoomLayout() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
        headerPanel.setPreferredSize(new Dimension(0, 40));

        mainTitleLabel = new JLabel("● PC-ONLINE");
        mainTitleLabel.setFont(CmsManager.baseFont.deriveFont( Font.BOLD, 15));

        JButton editButton   = new JButton("수정");
        JButton deleteButton = new JButton("삭제");
        editButton.addActionListener(e -> {
            PcAgentData selected = pcJList.getSelectedValue();
            if (selected != null) showEditAgentDialog(null, selected);
        });
        deleteButton.addActionListener(e -> handleAgentDelete());

        JPanel buttonGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonGroup.setBackground(Color.WHITE);
        buttonGroup.add(editButton);
        buttonGroup.add(deleteButton);

        headerPanel.add(mainTitleLabel, BorderLayout.WEST);
        headerPanel.add(buttonGroup,    BorderLayout.EAST);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 5, 10, 5));

        JLabel infoTitle = new JLabel("시스템 사양 정보");
        infoTitle.setFont(CmsManager.baseFont.deriveFont( Font.BOLD, 16));
        infoTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(infoTitle);
        contentPanel.add(Box.createVerticalStrut(10));

        JPanel sysGrid = new JPanel(new GridLayout(4, 2, 0, 8));
        sysGrid.setBackground(Color.WHITE);
        sysGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        sysGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        sysGrid.add(new JLabel("운영체제 (OS)"));
        osValueLabel = new JLabel("-", SwingConstants.RIGHT);
        sysGrid.add(osValueLabel);

        sysGrid.add(new JLabel("프로세서 (CPU)"));
        cpuValueLabel = new JLabel("-", SwingConstants.RIGHT);
        sysGrid.add(cpuValueLabel);

        sysGrid.add(new JLabel("설치된 물리 메모리"));
        ramValueLabel = new JLabel("-", SwingConstants.RIGHT);
        sysGrid.add(ramValueLabel);

        sysGrid.add(new JLabel("디스크 저장 용량"));
        diskValueLabel = new JLabel("-", SwingConstants.RIGHT);
        sysGrid.add(diskValueLabel);

        contentPanel.add(sysGrid);
        contentPanel.add(Box.createVerticalStrut(25));

        // CPU
        JPanel cpuHeader = new JPanel(new BorderLayout());
        cpuHeader.setBackground(Color.WHITE);
        cpuHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        cpuHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        cpuHeader.add(new JLabel("CPU 사용률"), BorderLayout.WEST);
        cpuUsageLabel = new JLabel("0 %", SwingConstants.RIGHT);
        cpuHeader.add(cpuUsageLabel, BorderLayout.EAST);
        contentPanel.add(cpuHeader);
        contentPanel.add(Box.createVerticalStrut(10));

        cpuProgressBar = new JProgressBar(0, 100);
        cpuProgressBar.setStringPainted(true);
        cpuProgressBar.setBackground(new Color(235, 235, 235));
        cpuProgressBar.setForeground(new Color(255, 140, 0));  // 주황색으로 구분
        cpuProgressBar.setBorderPainted(false);
        cpuProgressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        cpuProgressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        contentPanel.add(cpuProgressBar);

        contentPanel.add(Box.createVerticalStrut(35));

        // 메모리
        JPanel memHeader = new JPanel(new BorderLayout());
        memHeader.setBackground(Color.WHITE);
        memHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        memHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        memHeader.add(new JLabel("메모리 사용량 (PC-사용시간)"), BorderLayout.WEST);
        memoryValueLabel = new JLabel("0 GB Used", SwingConstants.RIGHT);
        memHeader.add(memoryValueLabel, BorderLayout.EAST);
        contentPanel.add(memHeader);
        contentPanel.add(Box.createVerticalStrut(10));

        memoryProgressBar = new JProgressBar(0, 100);
        memoryProgressBar.setStringPainted(true);
        memoryProgressBar.setBackground(new Color(235, 235, 235));
        memoryProgressBar.setForeground(new Color(130, 180, 220));
        memoryProgressBar.setBorderPainted(false);
        memoryProgressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        memoryProgressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        contentPanel.add(memoryProgressBar);
        contentPanel.add(Box.createVerticalStrut(35));

        JPanel bottomRow = new JPanel(new GridLayout(1, 2, 20, 0));
        bottomRow.setBackground(Color.WHITE);
        bottomRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        bottomRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        // 빠른 실행
        JPanel shortcutWrapPanel = new JPanel(new BorderLayout());
        shortcutWrapPanel.setBackground(Color.WHITE);
        JLabel shortcutTitle = new JLabel("빠른 실행");
        shortcutTitle.setFont(CmsManager.baseFont.deriveFont( Font.BOLD, 16));
        shortcutWrapPanel.add(shortcutTitle, BorderLayout.NORTH);

        JPanel shortcutAction = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
        shortcutAction.setBackground(Color.WHITE);
        shortcutCombo = new JComboBox<>();
        shortcutCombo.setBackground(Color.WHITE);
        runShortcutButton = new JButton("실행");
        runShortcutButton.addActionListener(e -> handleShortcutCommand());
        shortcutAction.add(shortcutCombo);
        shortcutAction.add(runShortcutButton);
        shortcutWrapPanel.add(shortcutAction, BorderLayout.CENTER);

        // 시스템 제어
        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setBackground(Color.WHITE);
        JLabel controlTitle = new JLabel("시스템 제어");
        controlTitle.setFont(CmsManager.baseFont.deriveFont( Font.BOLD, 16));
        controlPanel.add(controlTitle, BorderLayout.NORTH);

        JPanel controlAction = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
        controlAction.setBackground(Color.WHITE);
        String[] actions = {"종료 (Shutdown)", "재시작 (Reboot)", "컴퓨터 잠금"};
        systemActionCombo = new JComboBox<>(actions);
        systemActionCombo.setBackground(Color.WHITE);
        JButton runButton = new JButton("명령 실행");
        runButton.addActionListener(e -> handleSystemControlCommand());
        controlAction.add(systemActionCombo);
        controlAction.add(runButton);
        controlPanel.add(controlAction, BorderLayout.CENTER);

        bottomRow.add(shortcutWrapPanel);
        bottomRow.add(controlPanel);
        contentPanel.add(bottomRow);

        panel.add(headerPanel,  BorderLayout.NORTH);
        panel.add(contentPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createOfflineRoomLayout() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
        headerPanel.setPreferredSize(new Dimension(0, 40));

        offlineTitleLabel = new JLabel("<html><font color='red'>●</font> <b>PC-OFFLINE</b> - 연결 실패</html>");
        offlineTitleLabel.setFont(CmsManager.baseFont.deriveFont( Font.BOLD, 15));

        JButton editButton   = new JButton("수정");
        JButton deleteButton = new JButton("삭제");
        editButton.addActionListener(e -> {
            PcAgentData selected = pcJList.getSelectedValue();
            if (selected != null) showEditAgentDialog(null, selected);
        });
        deleteButton.addActionListener(e -> handleAgentDelete());

        JPanel buttonGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonGroup.setBackground(Color.WHITE);
        buttonGroup.add(editButton);
        buttonGroup.add(deleteButton);

        headerPanel.add(offlineTitleLabel, BorderLayout.WEST);
        headerPanel.add(buttonGroup,       BorderLayout.EAST);

        offlineCardLabel = new JLabel("해당 에이전트와 통신할 수 없습니다. 프로그램 실행 상태를 확인하세요.", SwingConstants.CENTER);
        offlineCardLabel.setFont(CmsManager.baseFont.deriveFont( Font.PLAIN, 15));

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(offlineCardLabel,  BorderLayout.CENTER);
        return panel;
    }

    private void showAddAgentDialog(JFrame parentFrame) {
        JDialog dialog = new JDialog(parentFrame, "새로운 원격 에이전트 디바이스 추가", true);
        dialog.setSize(340, 220);
        dialog.setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JTextField ipField    = new JTextField();
        JPasswordField passField = new JPasswordField();
        JTextField aliasField = new JTextField();

        formPanel.add(new JLabel("대상 에이전트 IP"));   formPanel.add(ipField);
        formPanel.add(new JLabel("접속 보안 암호"));     formPanel.add(passField);
        formPanel.add(new JLabel("단말기 식별 명칭"));   formPanel.add(aliasField);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        JButton cancelButton  = new JButton("취소");
        JButton connectButton = new JButton("에이전트 추가");

        cancelButton.addActionListener(e -> dialog.dispose());
        connectButton.addActionListener(e -> {
            String targetIp = ipField.getText();
            String targetPw = new String(passField.getPassword());
            String alias    = aliasField.getText().isEmpty() ? "PC-AGENT" : aliasField.getText();

            if (CmsManager.agents.containsKey(alias)) {
                JOptionPane.showMessageDialog(dialog, "이미 사용 중인 별칭입니다.", "오류", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!targetIp.isEmpty()) {
                CmsManager.agents.put(alias,
                        new AgentConnection(alias, targetIp, targetPw, null, null, null, AgentConnection.Status.OFFLINE));
                agentStore.saveAgents();
                loadBackendPcList();

                String cmd = "CONNECT " + targetIp + " " + targetPw + " " + alias;
                new Thread(() -> connectionManager.connectAgent(cmd)).start();

                dialog.dispose();
            }
        });

        buttonPanel.add(cancelButton);
        buttonPanel.add(connectButton);
        dialog.add(formPanel,    BorderLayout.CENTER);
        dialog.add(buttonPanel,  BorderLayout.SOUTH);
        dialog.setLocationRelativeTo(parentFrame);
        dialog.setVisible(true);
    }

    private void showEditAgentDialog(JFrame parentFrame, PcAgentData selected) {
        JDialog dialog = new JDialog(parentFrame, "에이전트 정보 수정", true);
        dialog.setSize(340, 220);
        dialog.setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JTextField ipField = new JTextField(selected.getIpAddress());
        JPasswordField passField = new JPasswordField();
        JTextField aliasField = new JTextField(selected.getPcName());

        formPanel.add(new JLabel("대상 에이전트 IP"));   formPanel.add(ipField);
        formPanel.add(new JLabel("접속 보안 암호"));     formPanel.add(passField);
        passField.setText(CmsManager.agents.get(selected.getPcName()).pw);
        formPanel.add(new JLabel("단말기 식별 명칭"));   formPanel.add(aliasField);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        JButton cancelButton = new JButton("취소");
        JButton saveButton   = new JButton("저장");

        cancelButton.addActionListener(e -> dialog.dispose());
        saveButton.addActionListener(e -> {
            String newIp    = ipField.getText();
            String newPw    = new String(passField.getPassword());
            String newAlias = aliasField.getText().isEmpty() ? selected.getPcName() : aliasField.getText();

            if (!newAlias.equals(selected.getPcName()) && CmsManager.agents.containsKey(newAlias)) {
                JOptionPane.showMessageDialog(dialog, "이미 사용 중인 별칭입니다.", "오류", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!newIp.isEmpty()) {
                // 기존 소켓 닫기 + Map에서 제거
                AgentConnection old = CmsManager.agents.get(selected.getPcName());
                if (old != null && old.socket != null) {
                    try {
                        connectionManager.killAgent(selected.getPcName());
                    } catch (Exception ignored) {}
                }
                CmsManager.agents.remove(selected.getPcName());

                // 새 정보로 오프라인 추가 + 저장
                CmsManager.agents.put(newAlias,
                        new AgentConnection(newAlias, newIp, newPw, null, null, null, AgentConnection.Status.OFFLINE));
                agentStore.saveAgents();
                loadBackendPcList();

                // 백그라운드에서 재연결
                String cmd = "CONNECT " + newIp + " " + newPw + " " + newAlias;
                new Thread(() -> connectionManager.connectAgent(cmd)).start();

                dialog.dispose();
            }
        });

        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);
        dialog.add(formPanel,   BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setLocationRelativeTo(parentFrame);
        dialog.setVisible(true);
    }

    public void loadBackendPcList() {
        PcAgentData currentSelected = pcJList.getSelectedValue();
        String selectedIp = (currentSelected != null) ? currentSelected.getIpAddress() : null;

        java.util.Map<String, PcAgentData> backupMap = new HashMap<>();
        for (int i = 0; i < listModel.size(); i++) {
            PcAgentData old = listModel.get(i);
            backupMap.put(old.getIpAddress(), old);
        }

        listModel.clear();
        CmsManager.agents.forEach((alias, agent) -> {
            PcAgentData newData = new PcAgentData(agent.alias, agent.ip, agent.status);
            if (backupMap.containsKey(agent.ip)) {
                PcAgentData old = backupMap.get(agent.ip);
                newData.setInitSpec(old.getOsInfo(), old.getCpuInfo(),
                        old.getTotalMemory(), old.getTotalDisk(), old.getShortcuts());
                newData.setRealtimeUsage(old.getCurrentCpu(), old.getCurrentRamUsed(),
                        old.getCurrentDiskUsed(), old.getUptime());
            }
            listModel.addElement(newData);
        });

        if (selectedIp != null) {
            for (int i = 0; i < listModel.size(); i++) {
                if (listModel.get(i).getIpAddress().equals(selectedIp)) {
                    pcJList.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    public void updateRealtimeResource(String senderIp, String jsonPacket) {
        SwingUtilities.invokeLater(() -> {
            try {
                JsonObject json = JsonParser.parseString(jsonPacket).getAsJsonObject();

                PcAgentData targetData = null;
                for (int i = 0; i < listModel.size(); i++) {
                    if (listModel.get(i).getIpAddress().equals(senderIp)) {
                        targetData = listModel.get(i);
                        break;
                    }
                }

                if (targetData == null) {
                    loadBackendPcList();
                    for (int i = 0; i < listModel.size(); i++) {
                        if (listModel.get(i).getIpAddress().equals(senderIp)) {
                            targetData = listModel.get(i);
                            break;
                        }
                    }
                }
                if (targetData == null) return;

                if (json.has("os")) {
                    List<String> scList = new ArrayList<>();
                    if (json.has("programs")) {
                        JsonArray scArray = json.getAsJsonArray("programs");
                        for (int j = 0; j < scArray.size(); j++) {
                            scList.add(scArray.get(j).getAsJsonObject().get("name").getAsString());
                        }
                    }
                    targetData.setInitSpec(
                            json.get("os").getAsString(),
                            json.get("processor").getAsString(),
                            json.get("totalMemory").getAsInt(),
                            json.get("diskTotal").getAsInt(),
                            scList);
                } else if (json.has("cpu")) {
                    targetData.setRealtimeUsage(
                            json.get("cpu").getAsInt(),
                            json.get("ramUsed").getAsInt(),
                            json.get("diskUsed").getAsInt(),
                            json.get("uptime").getAsLong());
                }

                pcJList.repaint();

                PcAgentData selectedPc = pcJList.getSelectedValue();
                if (selectedPc != null && selectedPc.getIpAddress().equals(senderIp)) {
                    bindPcDataToRightDetailPanel(selectedPc);
                }

            } catch (Exception e) {
                System.err.println("파싱 오류 무시: " + e.getMessage());
            }
        });
    }

    private void bindPcDataToRightDetailPanel(PcAgentData data) {
        switch (data.getStatus()) {
            case ONLINE -> {
                mainTitleLabel.setText("<html><font color='green'>●</font> <b>" + data.getPcName() + " (" + data.getIpAddress() + ")");
                osValueLabel.setText(data.getOsInfo());

                shortcutCombo.removeAllItems();
                if (data.getTotalMemory() > 0) {
                    List<String> list = data.getShortcuts();
                    if (list == null || list.isEmpty()) {
                        shortcutCombo.addItem("등록된 바로가기 없음");
                        shortcutCombo.setEnabled(false);
                        runShortcutButton.setEnabled(false);
                    } else {
                        for (String name : list) shortcutCombo.addItem(name);
                        shortcutCombo.setEnabled(true);
                        runShortcutButton.setEnabled(true);
                    }
                } else {
                    shortcutCombo.addItem("수신 대기 중...");
                    shortcutCombo.setEnabled(false);
                    runShortcutButton.setEnabled(false);
                }

                if (data.getTotalMemory() > 0) {
                    int ramPercent = (int) Math.round((double) data.getCurrentRamUsed() / data.getTotalMemory() * 100.0);
                    int diskPercent = (int) Math.round((double) data.getCurrentDiskUsed() / data.getTotalDisk() * 100.0);

                    cpuValueLabel.setText(data.getCpuInfo());
                    ramValueLabel.setText(data.getTotalMemory() + " GB (물리 메모리)");
                    diskValueLabel.setText(data.getCurrentDiskUsed() + " GB / " + data.getTotalDisk()
                            + " GB (" + diskPercent + "% 사용)");
                    memoryProgressBar.setValue(ramPercent);
                    memoryValueLabel.setText(data.getCurrentRamUsed() + " GB / " + data.getTotalMemory()
                            + " GB (" + ramPercent + "%) - " + data.getUptime() + "s 가동");

                    int cpuPercent = data.getCurrentCpu();
                    cpuProgressBar.setValue(cpuPercent);
                    cpuUsageLabel.setText(cpuPercent + " %");

                } else {
                    cpuValueLabel.setText("시스템 정보 대기 중...");
                    ramValueLabel.setText("대기 중...");
                    diskValueLabel.setText("대기 중...");
                    memoryProgressBar.setValue(0);
                    memoryValueLabel.setText("실시간 OSHI 데이터 스트림 대기 중...");
                    cpuProgressBar.setValue(0);
                    cpuUsageLabel.setText("0 %");
                }

                rightCardLayout.show(rightPanel, "ONLINE_VIEW");
            }
            case OFFLINE -> {
                offlineTitleLabel.setText("<html><font color='red'>●</font> <b>" + data.getPcName() + "</b> (" + data.getIpAddress() + ")</html>");

                offlineCardLabel.setText("해당 에이전트와 통신할 수 없습니다. 프로그램 실행 상태를 확인하세요.");
                offlineCardLabel.setForeground(Color.RED);
                rightCardLayout.show(rightPanel, "OFFLINE_VIEW");
            }
            case CONNECTING -> {
                offlineTitleLabel.setText("<html><font color='orange'>●</font> <b>" + data.getPcName() + "</b> (" + data.getIpAddress() + ")</html>");

                offlineCardLabel.setText("에이전트와 연결을 시도하고 있습니다...");
                offlineCardLabel.setForeground(Color.GRAY);
                rightCardLayout.show(rightPanel, "OFFLINE_VIEW");
            }
        }
    }

    private void handleAgentDelete() {
        PcAgentData selected = pcJList.getSelectedValue();
        if (selected != null) {
            int confirm = JOptionPane.showConfirmDialog(null,
                    selected.getPcName() + " 단말을 영구히 삭제하겠습니까?",
                    "Warning", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                AgentConnection target = CmsManager.agents.get(selected.getPcName());
                if (target != null) {
                    if (target.socket != null) {
                        try {
                            connectionManager.killAgent(selected.getPcName());
                        } catch (Exception ignored) {}
                    }
                    CmsManager.agents.remove(selected.getPcName());
                    agentStore.saveAgents();
                }
                listModel.removeElement(selected);
                rightCardLayout.show(rightPanel, "BLANK_VIEW");
            }
        }
    }

    private void handleShortcutCommand() {
        PcAgentData selected = pcJList.getSelectedValue();
        int execIndex = shortcutCombo.getSelectedIndex();
        if (selected != null && execIndex >= 0) {
            String selectedProgram = (String) shortcutCombo.getSelectedItem();
            connectionManager.sendCommand("EXEC " + selected.getPcName() + " " + execIndex, "EXEC");
            JOptionPane.showMessageDialog(null,
                    selected.getPcName() + " 단말에서 [" + selectedProgram + "] 실행 명령을 전송했습니다.",
                    "빠른 실행 완료", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void handleSystemControlCommand() {
        PcAgentData selected      = pcJList.getSelectedValue();
        String selectedCombo = (String) systemActionCombo.getSelectedItem();
        if (selected != null) {
            String cmdKeyword = "LOCK";
            if (selectedCombo.contains("종료"))   cmdKeyword = "SHUTDOWN";
            else if (selectedCombo.contains("시작")) cmdKeyword = "REBOOT";
            connectionManager.sendCommand(cmdKeyword + " " + selected.getPcName(), cmdKeyword);
        }
    }

    public void showCommandResult(String alias, JsonObject ackJson) {
        SwingUtilities.invokeLater(() -> {
            String status  = ackJson.has("status")  ? ackJson.get("status").getAsString()  : "UNKNOWN";
            String msg     = ackJson.has("message") ? ackJson.get("message").getAsString() : "";
            int msgType    = status.equals("SUCCESS") ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE;
            String title   = status.equals("SUCCESS") ? "원격 명령 수행 성공" : "원격 명령 수행 실패";
            JOptionPane.showMessageDialog(null,
                    "[" + alias + "] 단말 응답 결과\n\n▶ 처리 상태: " + status + "\n▶ 상세 메시지: " + msg,
                    title, msgType);
        });
    }

    private ImageIcon loadIcon(String path, int size) {
        try {
            Image img = new ImageIcon(CmsManager.class.getResource(path))
                    .getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
            return new ImageIcon(img);
        } catch (Exception e) {
            return null;
        }
    }

    // 왼쪽 목록 셀 렌더러
    private static class PcListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            JPanel card = new JPanel(new GridLayout(2, 1, 0, 3));
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(224, 224, 224)),
                    BorderFactory.createEmptyBorder(12, 15, 12, 15)));
            card.setBackground(isSelected ? new Color(210, 230, 245) : Color.WHITE);

            if (value instanceof PcAgentData data) {
                String dotColor = switch (data.getStatus()) {
                    case ONLINE     -> "green";
                    case OFFLINE    -> "red";
                    case CONNECTING -> "orange";
                };

                JLabel titleLabel = new JLabel(
                        "<html><font color='" + dotColor + "'>●</font> <b>" + data.getPcName() + "</b></html>");
                titleLabel.setFont(CmsManager.baseFont.deriveFont(Font.PLAIN, 13));

                String subText;
                if (data.getStatus() == AgentConnection.Status.ONLINE) {
                    if (data.getTotalMemory() > 0) {
                        int ramPercent = (int) Math.round(
                                (double) data.getCurrentRamUsed() / data.getTotalMemory() * 100.0);
                        subText = String.format("%s | CPU: %d%% | RAM: %d%%",
                                data.getIpAddress(), data.getCurrentCpu(), ramPercent);
                    } else {
                        subText = data.getIpAddress() + " (시스템 스펙 수신 대기중...)";
                    }
                } else if (data.getStatus() == AgentConnection.Status.CONNECTING) {
                    subText = "연결 중...";
                } else {
                    subText = "네트워크 오프라인";
                }

                JLabel subLabel = new JLabel(subText);
                subLabel.setFont(CmsManager.baseFont.deriveFont(Font.PLAIN, 11));
                subLabel.setForeground(Color.GRAY);

                card.add(titleLabel);
                card.add(subLabel);
            }
            return card;
        }
    }
}
