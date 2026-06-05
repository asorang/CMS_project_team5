package ui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javax.swing.*;
import java.awt.*;

// 기본 패키지에 위치한 main.CmsManager 임포트 (패키지 구조에 따라 수정 필요)
import main.CmsManager;

public class ManagerUI {

    private final CmsManager backendEngine;
    private JList<PcAgentData> pcJList;
    private DefaultListModel<PcAgentData> listModel;
    private CardLayout rightCardLayout;
    private JPanel rightPanel;

    private JLabel mainTitleLabel;
    private JLabel osValueLabel;
    private JLabel cpuValueLabel;
    private JLabel ramValueLabel;
    private JLabel diskValueLabel;
    private JProgressBar memoryProgressBar;
    private JLabel memoryValueLabel;
    private JComboBox<String> systemActionCombo;
    private JLabel offlineTitleLabel;
    private JComboBox<String> shortcutCombo;
    private JButton runShortcutButton;

    public ManagerUI(main.CmsManager backendEngine) {
        this.backendEngine = backendEngine;
    }

    public void drawUI() {
        JFrame frame = new JFrame("Manager Central Management System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(950, 650);
        frame.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
    //TODO :지금 모든 패널을 한번에 그리고 있기 떄문에 새로 그리는 것이 아닌 안에 것들을 따로 그려줘야 함 즉 온라인 패널은 그대로 두고 PC화면만 새로 만들어서 넣어줘야 함
        mainPanel.add(createTopToolbar(frame), BorderLayout.NORTH);
        mainPanel.add(createCentralSplitLayout(frame), BorderLayout.CENTER);

        frame.add(mainPanel);
        frame.setVisible(true);

        loadBackendPcList();
    }

    //제일 위 아이콘들 관리
    private JPanel createTopToolbar(JFrame parentFrame) {
        JPanel toolbarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        toolbarPanel.setBackground(Color.WHITE);

        JButton addButton = new JButton("+");
        addButton.setFont(new Font("Monospaced", Font.BOLD, 22));
        addButton.setPreferredSize(new Dimension(35, 35));
        addButton.setFocusPainted(false);
        addButton.setBackground(new Color(224, 224, 224));
        addButton.setBorder(BorderFactory.createEmptyBorder());
        addButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addButton.addActionListener(e -> showAddAgentDialog(parentFrame));

        JButton refreshButton = new JButton("🔄");
        refreshButton.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        refreshButton.setPreferredSize(new Dimension(35, 35));
        refreshButton.setFocusPainted(false);
        refreshButton.setBackground(new Color(224, 224, 224));
        refreshButton.setBorder(BorderFactory.createEmptyBorder());
        refreshButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        refreshButton.addActionListener(e -> {
            // 1. 클릭 즉시 버튼 비활성화 및 로딩 아이콘 변경
            refreshButton.setEnabled(false);
            refreshButton.setText("⏳");

            // 2. 백그라운드 스레드에서 무거운 네트워크 작업 실행
            new Thread(() -> {
                backendEngine.reloadAgents(); // 타임아웃이 걸리더라도 백그라운드라 UI는 안 멈춤

                // 3. 작업이 끝나면 다시 UI 스레드로 돌아와서 화면 갱신
                SwingUtilities.invokeLater(() -> {
                    loadBackendPcList();
                    rightCardLayout.show(rightPanel, "BLANK_VIEW");
                    refreshButton.setText("🔄"); // 원상 복구
                    refreshButton.setEnabled(true);
                    JOptionPane.showMessageDialog(parentFrame, "최신 관제 목록을 백엔드 파일 저장소로부터 동기화함.", "완료", JOptionPane.INFORMATION_MESSAGE);
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
        pcJList = new JList<>(listModel);
        pcJList.setBackground(new Color(245, 245, 245));
        pcJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        pcJList.setCellRenderer(new PcListCellRenderer());

        pcJList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                PcAgentData selectedPc = pcJList.getSelectedValue();
                if (selectedPc != null) {
                    bindPcDataToRightDetailPanel(selectedPc);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(pcJList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        leftPanel.add(scrollPane, BorderLayout.CENTER);

        return leftPanel;
    }

    //PC목록
    private JPanel createRightDisplayArea() {
        rightCardLayout = new CardLayout();
        rightPanel = new JPanel(rightCardLayout);
        rightPanel.setBackground(Color.WHITE);
        rightPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));

        JPanel blankView = new JPanel(new BorderLayout());
        blankView.setBackground(Color.WHITE);
        JLabel defaultMessage = new JLabel("조회할 에이전트 PC를 좌측 목록에서 선택하십시오.", SwingConstants.CENTER);
        defaultMessage.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        defaultMessage.setForeground(Color.GRAY);
        blankView.add(defaultMessage, BorderLayout.CENTER);

        JPanel onlineView = createOnlineRoomLayout();
        JPanel offlineView = createOfflineRoomLayout();

        rightPanel.add(blankView, "BLANK_VIEW");
        rightPanel.add(onlineView, "ONLINE_VIEW");
        rightPanel.add(offlineView, "OFFLINE_VIEW");

        rightCardLayout.show(rightPanel, "BLANK_VIEW");
        return rightPanel;
    }

    //온라인 그리고 명령어 및 아래 컨트롤 창들
    private JPanel createOnlineRoomLayout() {//옆에 pc 상세 화면 나오게 하는거
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
        headerPanel.setPreferredSize(new Dimension(0, 40));

        mainTitleLabel = new JLabel("● PC-ONLINE");
        mainTitleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 15));

        JButton deleteButton = new JButton("삭제");
        deleteButton.addActionListener(e -> handleAgentDelete());

        headerPanel.add(mainTitleLabel, BorderLayout.WEST);
        headerPanel.add(deleteButton, BorderLayout.EAST);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 5, 10, 5));

        JLabel infoTitle = new JLabel("시스템 사양 정보");
        infoTitle.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        infoTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(infoTitle);
        contentPanel.add(Box.createVerticalStrut(10));

        JPanel sysGrid = new JPanel(new GridLayout(4, 2, 0, 8));
        sysGrid.setBackground(Color.WHITE);
        sysGrid.setAlignmentX(Component.LEFT_ALIGNMENT);

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

        // 하단 패널 구성을 가로 2칸으로 변경
        JPanel bottomRow = new JPanel(new GridLayout(1, 2, 20, 0));
        bottomRow.setBackground(Color.WHITE);
        bottomRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        bottomRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        // --- [여기서부터 수정] 1. 왼쪽: 빠른 실행 바로가기 (드롭다운 방식) ---
        JPanel shortcutWrapPanel = new JPanel(new BorderLayout());
        shortcutWrapPanel.setBackground(Color.WHITE);
        JLabel shortcutTitle = new JLabel("빠른 실행");
        shortcutTitle.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        shortcutWrapPanel.add(shortcutTitle, BorderLayout.NORTH);

        JPanel shortcutAction = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
        shortcutAction.setBackground(Color.WHITE);

        shortcutCombo = new JComboBox<>();
        shortcutCombo.setBackground(Color.WHITE);

        runShortcutButton = new JButton("실행");
        // 버튼 클릭 시 실행할 이벤트 연결 (4단계에서 만들 함수)
        runShortcutButton.addActionListener(e -> handleShortcutCommand());

        shortcutAction.add(shortcutCombo);
        shortcutAction.add(runShortcutButton);
        shortcutWrapPanel.add(shortcutAction, BorderLayout.CENTER);
        // --- [여기까지 수정] ---

        // 2. 오른쪽: 시스템 제어 (종료/재시작)
        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setBackground(Color.WHITE);
        JLabel controlTitle = new JLabel("시스템 제어");
        controlTitle.setFont(new Font("맑은 고딕", Font.BOLD, 13));
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

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(contentPanel, BorderLayout.CENTER);
        return panel;
    }


    // [새로 추가] 빠른 실행 전용 이벤트 처리기
    private void handleShortcutCommand() {
        PcAgentData selected = pcJList.getSelectedValue();
        int execIndex = shortcutCombo.getSelectedIndex();

        if (selected != null && execIndex >= 0) {
            String selectedProgram = (String) shortcutCombo.getSelectedItem();
            String fakeConsoleInput = "EXEC " + selected.getPcName() + " " + execIndex;

            // 백엔드로 명령어 전송
            backendEngine.sendCommand(fakeConsoleInput, "EXEC");

            JOptionPane.showMessageDialog(null,
                    selected.getPcName() + " 단말에서 [" + selectedProgram + "] 실행 명령을 전송했습니다.",
                    "빠른 실행 완료", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    //오프라인
    private JPanel createOfflineRoomLayout() {//오프라인 레이아웃
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
        headerPanel.setPreferredSize(new Dimension(0, 40));

        offlineTitleLabel = new JLabel("● PC-OFFLINE (연결 유실)");
        offlineTitleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 15));
        offlineTitleLabel.setForeground(Color.RED);
        JButton deleteButton = new JButton("삭제");
        deleteButton.addActionListener(e -> handleAgentDelete());

        headerPanel.add(offlineTitleLabel, BorderLayout.WEST);
        headerPanel.add(deleteButton, BorderLayout.EAST);

        JLabel errorLabel = new JLabel("해당 에이전트 단말기 네트워크와 소켓 세션 연결을 수립할 수 없습니다.", SwingConstants.CENTER);
        errorLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        errorLabel.setForeground(Color.RED);

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(errorLabel, BorderLayout.CENTER);
        return panel;
    }

    //새로 추가할때 창
    private void showAddAgentDialog(JFrame parentFrame) {
        JDialog dialog = new JDialog(parentFrame, "새로운 원격 에이전트 디바이스 추가", true);
        dialog.setSize(340, 220);
        dialog.setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JTextField ipField = new JTextField();
        JPasswordField passField = new JPasswordField();
        JTextField aliasField = new JTextField();

        formPanel.add(new JLabel("대상 에이전트 IP"));
        formPanel.add(ipField);
        formPanel.add(new JLabel("접속 보안 암호"));
        formPanel.add(passField);
        formPanel.add(new JLabel("단말기 식별 명칭"));
        formPanel.add(aliasField);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        JButton cancelButton = new JButton("취소");
        JButton connectButton = new JButton("에이전트 추가");

        cancelButton.addActionListener(e -> dialog.dispose());

        connectButton.addActionListener(e -> {
            String targetIp = ipField.getText();
            String targetPw = new String(passField.getPassword());
            String alias = aliasField.getText();
            if(!targetIp.isEmpty()) {
                String commandStr = "CONNECT " + targetIp + " " + targetPw + " " + (alias.isEmpty() ? "PC-AGENT" : alias);
                new Thread(() -> backendEngine.connectAgent(commandStr)).start();

                dialog.dispose();
                Timer timer = new Timer(1000, ev -> loadBackendPcList());
                timer.setRepeats(false);
                timer.start();
            }
        });

        buttonPanel.add(cancelButton);
        buttonPanel.add(connectButton);

        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setLocationRelativeTo(parentFrame);
        dialog.setVisible(true);
    }

    //pc 정보 불러오는거
    public void loadBackendPcList() {
        // 1. [추가] 리스트 초기화 전, 사용자가 현재 보고 있던 PC의 IP를 기억함
        PcAgentData currentSelected = pcJList.getSelectedValue();
        String selectedIp = (currentSelected != null) ? currentSelected.getIpAddress() : null;

        java.util.Map<String, PcAgentData> backupMap = new java.util.HashMap<>();
        for (int i = 0; i < listModel.size(); i++) {
            PcAgentData oldData = listModel.get(i);
            backupMap.put(oldData.getIpAddress(), oldData);
        }

        listModel.clear();

        CmsManager.agents.forEach((alias, agent) -> {
            PcAgentData newData = new PcAgentData(agent.alias, agent.ip, agent.ON_state);

            if (backupMap.containsKey(agent.ip)) {
                PcAgentData oldData = backupMap.get(agent.ip);
                // [수정됨] 맨 끝에 oldData.getShortcuts() 추가
                newData.setInitSpec(oldData.getOsInfo(), oldData.getCpuInfo(), oldData.getTotalMemory(), oldData.getTotalDisk(), oldData.getShortcuts());
                newData.setRealtimeUsage(oldData.getCurrentCpu(), oldData.getCurrentRamUsed(), oldData.getCurrentDiskUsed(), oldData.getUptime());
            }
            listModel.addElement(newData);
        });

        // 2. [추가] 리스트 재생성이 끝난 후, 아까 보고 있던 PC를 찾아서 다시 선택(Select) 상태로 만듦
        if (selectedIp != null) {
            for (int i = 0; i < listModel.size(); i++) {
                if (listModel.get(i).getIpAddress().equals(selectedIp)) {
                    pcJList.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    //정보 받고 일치하는 애들 찾아서 매칭//해결
    public void updateRealtimeResource(String senderIp, String jsonPacket) {
        SwingUtilities.invokeLater(() -> {
            try {
                JsonObject json = JsonParser.parseString(jsonPacket).getAsJsonObject();

                // 1. 현재 패킷을 보낸 IP와 일치하는 데이터 찾기
                PcAgentData targetData = null;
                for (int i = 0; i < listModel.size(); i++) {
                    if (listModel.get(i).getIpAddress().equals(senderIp)) {
                        targetData = listModel.get(i);
                        break;
                    }
                }

                // [추가된 핵심 방어 로직]
                // UI 리스트에 등록되기 전에 스펙 패킷이 먼저 도착한 경우, 강제로 즉시 동기화 실행
                if (targetData == null) {
                    loadBackendPcList();
                    for (int i = 0; i < listModel.size(); i++) {
                        if (listModel.get(i).getIpAddress().equals(senderIp)) {
                            targetData = listModel.get(i);
                            break;
                        }
                    }
                }

                // 즉시 갱신을 했는데도 없다면 정말 잘못된 패킷이므로 무시
                if (targetData == null) return;

                // 2. 초기 스펙 패킷 수신 ("os" 키가 존재할 경우)
                // [수정된 초기 스펙 패킷 수신 부분]
                if (json.has("os")) {
                    java.util.List<String> scList = new java.util.ArrayList<>();

                    // "shortcuts" 대신 "programs" 키워드 사용
                    if (json.has("programs")) {
                        com.google.gson.JsonArray scArray = json.getAsJsonArray("programs");
                        for(int j = 0; j < scArray.size(); j++) {
                            // 배열 안의 요소를 JsonObject로 추출
                            JsonObject prog = scArray.get(j).getAsJsonObject();
                            // "name" 속성만 가져와서 리스트에 추가 (버튼 이름으로 사용)
                            scList.add(prog.get("name").getAsString());
                        }
                    }

                    targetData.setInitSpec(
                            json.get("os").getAsString(),
                            json.get("processor").getAsString(),
                            json.get("totalMemory").getAsInt(),
                            json.get("diskTotal").getAsInt(),
                            scList
                    );
                }
                // 3. 실시간 사용량 패킷 수신 ("cpu" 키가 존재할 경우)
                else if (json.has("cpu")) {
                    targetData.setRealtimeUsage(
                            json.get("cpu").getAsInt(),
                            json.get("ramUsed").getAsInt(),
                            json.get("diskUsed").getAsInt(),
                            json.get("uptime").getAsLong()
                    );
                }

                // 4. 좌측 JList 갱신
                pcJList.repaint();

                // 5. 현재 사용자가 화면 우측에 이 PC를 띄워놓고 보고 있다면 우측 패널도 갱신
                PcAgentData selectedPc = pcJList.getSelectedValue();
                if (selectedPc != null && selectedPc.getIpAddress().equals(senderIp)) {
                    bindPcDataToRightDetailPanel(selectedPc);
                }

            } catch (Exception e) {
                System.err.println("파싱 오류 무시: " + e.getMessage());
            }
        });
    }

    //상세 정보 사용량 계산
    private void bindPcDataToRightDetailPanel(PcAgentData data) {
        if (data.isOnline()) {
            mainTitleLabel.setText("● " + data.getPcName() + " (" + data.getIpAddress() + ")");
            osValueLabel.setText(data.getOsInfo());

            // --- [수정된 빠른 실행 패널 동적 갱신 로직] ---
            shortcutCombo.removeAllItems(); // 기존 목록 비우기

            if (data.getTotalMemory() > 0) {
                java.util.List<String> list = data.getShortcuts();
                if (list == null || list.isEmpty()) {
                    shortcutCombo.addItem("등록된 바로가기 없음");
                    shortcutCombo.setEnabled(false);
                    runShortcutButton.setEnabled(false);
                } else {
                    for (String name : list) {
                        shortcutCombo.addItem(name);
                    }
                    shortcutCombo.setEnabled(true);
                    runShortcutButton.setEnabled(true);
                }
            } else {
                shortcutCombo.addItem("수신 대기 중...");
                shortcutCombo.setEnabled(false);
                runShortcutButton.setEnabled(false);
            }
            // --- [여기까지 새로 추가] ---

            // 초기 스펙 패킷이 도착해서 Total 값이 0보다 클 때만 퍼센트 계산 수행
            if (data.getTotalMemory() > 0) {
                int ramPercent = (int) Math.round((double) data.getCurrentRamUsed() / data.getTotalMemory() * 100.0);
                int diskPercent = (int) Math.round((double) data.getCurrentDiskUsed() / data.getTotalDisk() * 100.0);

                cpuValueLabel.setText(data.getCpuInfo());
                ramValueLabel.setText(data.getTotalMemory() + " GB (물리 메모리)");
                diskValueLabel.setText(data.getCurrentDiskUsed() + " GB / " + data.getTotalDisk() + " GB (" + diskPercent + "% 사용)");

                memoryProgressBar.setValue(ramPercent);
                memoryValueLabel.setText(data.getCurrentRamUsed() + " GB / " + data.getTotalMemory() + " GB (" + ramPercent + "%) - " + data.getUptime() + "s 가동");
            } else {
                // 아직 스펙 패킷이 안 온 상태
                cpuValueLabel.setText("시스템 정보 대기 중...");
                ramValueLabel.setText("대기 중...");
                diskValueLabel.setText("대기 중...");
                memoryProgressBar.setValue(0);
                memoryValueLabel.setText("실시간 OSHI 데이터 스트림 대기 중...");
            }

            rightCardLayout.show(rightPanel, "ONLINE_VIEW");
        } else {
            offlineTitleLabel.setText("● " + data.getPcName() + " (" + data.getIpAddress() + ") - 연결 끊김");
            rightCardLayout.show(rightPanel, "OFFLINE_VIEW");
        }
    }

    //제거 로직 json에서 특정 정보만 제거
    private void handleAgentDelete() {
        PcAgentData selected = pcJList.getSelectedValue();
        if (selected != null) {
            int confirm = JOptionPane.showConfirmDialog(null, selected.getPcName() + " 단말을 영구히 삭제하겠습니까?", "Warning", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                CmsManager.AgentConnection target = main.CmsManager.agents.get(selected.getPcName());
                if(target != null) {
                    try { target.socket.close(); } catch(Exception ignored){}
                    CmsManager.agents.remove(selected.getPcName());
                    backendEngine.saveAgents();
                }
                listModel.removeElement(selected);
                rightCardLayout.show(rightPanel, "BLANK_VIEW");
            }
        }
    }

    //명령 입력 및 보내는거
    private void handleSystemControlCommand() {
        PcAgentData selected = pcJList.getSelectedValue();
        String selectedCombo = (String) systemActionCombo.getSelectedItem();

        if (selected != null) {
            String cmdKeyword = "LOCK";
            if (selectedCombo.contains("종료")) cmdKeyword = "SHUTDOWN";
            else if (selectedCombo.contains("시작")) cmdKeyword = "REBOOT";

            String fakeConsoleInput = cmdKeyword + " " + selected.getPcName();
            backendEngine.sendCommand(fakeConsoleInput, cmdKeyword);
            /*
            JOptionPane.showMessageDialog(null,
                    selected.getPcName() + " 단말로 원격 명령어 패킷을 전달하였습니다: " + cmdKeyword,
                    "명령 패킷 송신 완료", JOptionPane.INFORMATION_MESSAGE);*/
        }
    }

    //pc정보 매칭 시킬 변수들 즉 틀임
    public static class PcAgentData {
        private String pcName;
        private String ipAddress;
        private boolean isOnline;

        // 초기 스펙 데이터 (고정값)
        private String osInfo = "대기 중...";
        private String cpuInfo = "대기 중...";
        private int totalMemory = 0; // GB
        private int totalDisk = 0;   // GB

        // [추가] 바로가기 목록을 담을 리스트
        private java.util.List<String> shortcuts = new java.util.ArrayList<>();

        // 실시간 변동 데이터
        private int currentCpu = 0;
        private int currentRamUsed = 0;
        private int currentDiskUsed = 0;
        private long uptime = 0;

        public PcAgentData(String pcName, String ipAddress, boolean isOnline) {
            this.pcName = pcName;
            this.ipAddress = ipAddress;
            this.isOnline = isOnline;
        }

        public String getPcName() { return pcName; }
        public String getIpAddress() { return ipAddress; }
        public boolean isOnline() { return isOnline; }

        // Getter
        public String getOsInfo() { return osInfo; }
        public String getCpuInfo() { return cpuInfo; }
        public int getTotalMemory() { return totalMemory; }
        public int getTotalDisk() { return totalDisk; }
        public int getCurrentCpu() { return currentCpu; }
        public int getCurrentRamUsed() { return currentRamUsed; }
        public int getCurrentDiskUsed() { return currentDiskUsed; }
        public long getUptime() { return uptime; }

        // [추가] Getter
        public java.util.List<String> getShortcuts() { return shortcuts; }

        // [수정] shortcuts 파라미터 추가
        public void setInitSpec(String os, String cpu, int totalMem, int totalDisk, java.util.List<String> shortcuts) {
            this.osInfo = os;
            this.cpuInfo = cpu;
            this.totalMemory = totalMem;
            this.totalDisk = totalDisk;
            this.shortcuts = (shortcuts != null) ? shortcuts : new java.util.ArrayList<>();
        }

        // Setter (실시간 사용량 갱신용)
        public void setRealtimeUsage(int cpu, int ramUsed, int diskUsed, long uptime) {
            this.currentCpu = cpu;
            this.currentRamUsed = ramUsed;
            this.currentDiskUsed = diskUsed;
            this.uptime = uptime;
        }
    }

    //왼쪽 패널
    private static class PcListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JPanel card = new JPanel(new GridLayout(2, 1, 0, 3));
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(224, 224, 224)),
                    BorderFactory.createEmptyBorder(12, 15, 12, 15)
            ));
            card.setBackground(isSelected ? new Color(210, 230, 245) : Color.WHITE);

            if (value instanceof PcAgentData) {
                PcAgentData data = (PcAgentData) value;
                String dotColor = data.isOnline() ? "green" : "red";
                JLabel titleLabel = new JLabel("<html><font color='" + dotColor + "'>●</font> <b>" + data.getPcName() + "</b></html>");
                titleLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 13));

                // ----------------------------------------------------
                // [이 부분이 변경됨] 퍼센트 계산하여 서브 라벨에 띄우기
                // ----------------------------------------------------
                String subText;
                if (data.isOnline()) {
                    if (data.getTotalMemory() > 0) {
                        int ramPercent = (int) Math.round((double) data.getCurrentRamUsed() / data.getTotalMemory() * 100.0);
                        subText = String.format("%s | CPU: %d%% | RAM: %d%%", data.getIpAddress(), data.getCurrentCpu(), ramPercent);
                    } else {
                        subText = data.getIpAddress() + " (시스템 스펙 수신 대기중...)";
                    }
                } else {
                    subText = "네트워크 오프라인";
                }

                JLabel subLabel = new JLabel(subText);
                subLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
                subLabel.setForeground(Color.GRAY);

                card.add(titleLabel);
                card.add(subLabel);
            }
            return card;
        }
    }

    //명령어 보낸거 확인 됐는지 확인하는 로직
    public void showCommandResult(String alias, JsonObject ackJson) {
        SwingUtilities.invokeLater(() -> {
            String cmd = ackJson.has("cmd") ? ackJson.get("cmd").getAsString() : "알 수 없음";
            String status = ackJson.has("status") ? ackJson.get("status").getAsString() : "UNKNOWN";
            String msg = ackJson.has("message") ? ackJson.get("message").getAsString() : "";

            int msgType = status.equals("SUCCESS") ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE;
            String title = status.equals("SUCCESS") ? "원격 명령 수행 성공" : "원격 명령 수행 실패";

            JOptionPane.showMessageDialog(null,
                    "[" + alias + "] 단말 응답 결과\n\n▶ 명령어: " + cmd + "\n▶ 처리 상태: " + status + "\n▶ 상세 메시지: " + msg,
                    title, msgType);
        });
    }
}