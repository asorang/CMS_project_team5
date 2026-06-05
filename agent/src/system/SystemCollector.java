package src.system;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import src.AgentState;

import java.io.PrintWriter;

/**
 * 시스템 정보 수집 및 명령 실행기
 * 고정 시스템 정보는 한 번, 주기적 시스템 정보 (Heartbeat)는 주기마다 1회 전송함
 * 명령어를 수신하는 경우 시스템에서 해당 명령어 또는 프로그램 실행
 */
public class SystemCollector {

    // 주기적 시스템 상태 정보 반환 (CPU 사용량, 업타임, 램/디스크 사용량)
    public static String systemStatus() {
        double cpu = AgentState.CPU.getSystemCpuLoadBetweenTicks(AgentState.prevTicks) * 100;
        AgentState.prevTicks = AgentState.CPU.getSystemCpuLoadTicks();

        long uptime   = AgentState.OS.getSystemUptime();
        long ramTotal = AgentState.HAL.getMemory().getTotal();
        long ramUsed  = ramTotal - AgentState.HAL.getMemory().getAvailable();
        long diskUsed = 0;

        for (var fs : AgentState.OS.getFileSystem().getFileStores()) {
            diskUsed += fs.getTotalSpace() - fs.getUsableSpace();
        }

        JsonObject json = new JsonObject();
        json.addProperty("cpu",      (int) cpu);
        json.addProperty("uptime",   uptime);
        json.addProperty("ramUsed",  toGB(ramUsed));
        json.addProperty("diskUsed", toGB(diskUsed));

        if (AgentState.DEBUG_MODE) System.out.println("[송신] " + json);
        return json.toString();
    }

    // 최초 1회 시스템 정보 반환 (시스템 고정 정보 -- OS, CPU 정보, 디스크 총 용량 등등....)
    public static String systemInfo() {
        long diskTotal = 0;
        for (var fs : AgentState.OS.getFileSystem().getFileStores()) {
            diskTotal += fs.getTotalSpace();
        }

        JsonObject json = new JsonObject();
        json.addProperty("os",          AgentState.OS.toString());
        json.addProperty("processor",   AgentState.CPU.getProcessorIdentifier().getName().strip());
        json.addProperty("cores",       AgentState.CPU.getPhysicalProcessorCount());
        json.addProperty("threads",     AgentState.CPU.getLogicalProcessorCount());
        json.addProperty("totalMemory", toGB(AgentState.HAL.getMemory().getTotal()));
        json.addProperty("diskTotal",   toGB(diskTotal));

        JsonArray programs = new JsonArray();
        for (String[] p : AgentState.PROGRAMS) {
            JsonObject entry = new JsonObject();
            entry.addProperty("name", p[0]);
            entry.addProperty("path", p[1]);
            programs.add(entry);
        }
        json.add("programs", programs);

        if (AgentState.DEBUG_MODE) System.out.println("[송신] " + json);
        return json.toString();
    }

    // 명령어 처리
    public static void systemCommand(String json, PrintWriter out) {
        JsonObject ack = new JsonObject();
        ack.addProperty("type", "ACK");

        try {
            JsonObject wrapper = JsonParser.parseString(json).getAsJsonObject();
            String payload = wrapper.get("payload").toString();
            int received   = wrapper.get("checksum").getAsInt();

            if (checksum(payload) != received) {
                System.out.println("명령어 체크섬이 올바르지 않습니다. (Manager 체크섬 :" + received + " / 실제 체크섬 : " + checksum(payload));
                ack.addProperty("status", "FAILED");
                ack.addProperty("message", "Checksum Mismatch");
                return;
            } else {
                System.out.println("명령어 무결성 확인됨 (Manager 체크섬 :" + received + " / 실제 체크섬 : " + checksum(payload) + ")");
            }

            JsonObject cmdJson = wrapper.get("payload").getAsJsonObject();
            String cmd = cmdJson.get("cmd").getAsString();

            System.out.println("[수신] " + json);

            if (AgentState.DEBUG_MODE) {
                switch (cmd) {
                    case "SHUTDOWN" -> System.out.println("[CMD] Shutdown 명령 수신됨");
                    case "REBOOT"   -> System.out.println("[CMD] Reboot 명령 수신됨");
                    case "LOCK"     -> System.out.println("[CMD] Lock 명령 수신됨");
                    case "EXEC"     -> System.out.println("[CMD] EXEC 명령 수신됨, index: " + cmdJson.get("index").getAsInt());
                }
                ack.addProperty("status", "SUCCESS");
                ack.addProperty("message", "DEBUG: " + cmd + " executed (simulated)");
            } else {
                switch (cmd) {
                    case "SHUTDOWN" -> new ProcessBuilder("shutdown", "/s", "/t", "0").start();
                    case "REBOOT"   -> new ProcessBuilder("shutdown", "/r", "/t", "0").start();
                    case "LOCK"     -> new ProcessBuilder("rundll32.exe", "user32.dll,LockWorkStation").start();
                    case "EXEC"     -> {
                        int index = cmdJson.get("index").getAsInt();
                        if (index >= 0 && index < AgentState.PROGRAMS.size())
                            new ProcessBuilder(AgentState.PROGRAMS.get(index)[1]).start();
                    }
                }
                ack.addProperty("status", "SUCCESS");
                ack.addProperty("message", "Command delivered to system");
            }
        } catch (Exception e) {
            ack.addProperty("status", "FAIL");
            ack.addProperty("message", e.getMessage());
            System.out.println("명령 처리 오류: " + e.getMessage());
        }

        if (out != null) {
            out.println(ack.toString());
            if (AgentState.DEBUG_MODE) System.out.println("[송신] " + ack);
        }
    }

    private static long toMB(long bytes){
        return bytes / (1024 * 1024);
    }

    private static long toGB(long bytes) {
        return bytes / (1024 * 1024 * 1024);
    }

    static int checksum(String json) {
        int sum = 0;
        for (char c : json.toCharArray()) sum += c;
        return sum;
    }
}