
package src;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 에이전트 프로그램 내에서 전역으로 사용하는 전역 공유 변수
 * OSHI라던가, 트레이 아이콘이라던가 Main에서 일일히 붙여서 공유하던 걸 분리함
 */

public class AgentState {
    // 설정 변수
    public static int PORT = 10293;
    public static String PASSWORD = "1234";
    public static final int INTERVAL = 10;
    public static final String CONFIG_FILE = "config.json";
    public static final List<String[]> PROGRAMS = new ArrayList<>();
    public static final boolean DEBUG_MODE = false;

    // 네트워크 소켓
    public static java.net.ServerSocket serverSocket;

    // 트레이 관련
    public static TrayIcon trayIcon;
    public static MenuItem statusItem;
    public static Image iconOnline  = Toolkit.getDefaultToolkit().getImage(AgentState.class.getResource("/resource/tray_online.png"));
    public static Image iconOffline = Toolkit.getDefaultToolkit().getImage(AgentState.class.getResource("/resource/tray_offline.png"));

    // OSHI
    public static final SystemInfo SI = new SystemInfo();
    public static final HardwareAbstractionLayer HAL = SI.getHardware();
    public static final CentralProcessor CPU = HAL.getProcessor();
    public static long[] prevTicks = CPU.getSystemCpuLoadTicks();
    public static final OperatingSystem OS = SI.getOperatingSystem();
}