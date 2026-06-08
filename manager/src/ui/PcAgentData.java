package src.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * UI에서 PC 1대의 표시 데이터를 담는 클래스
 */
public class PcAgentData {
    private String pcName;
    private String ipAddress;
    private boolean isOnline;

    // 초기 스펙 데이터 (고정값)
    private String osInfo   = "대기 중...";
    private String cpuInfo  = "대기 중...";
    private int totalMemory = 0;
    private int totalDisk   = 0;
    private List<String> shortcuts = new ArrayList<>();

    // 실시간 변동 데이터
    private int currentCpu      = 0;
    private int currentRamUsed  = 0;
    private int currentDiskUsed = 0;
    private long uptime         = 0;

    public PcAgentData(String pcName, String ipAddress, boolean isOnline) {
        this.pcName    = pcName;
        this.ipAddress = ipAddress;
        this.isOnline  = isOnline;
    }

    public void setInitSpec(String os, String cpu, int totalMem, int totalDisk, List<String> shortcuts) {
        this.osInfo      = os;
        this.cpuInfo     = cpu;
        this.totalMemory = totalMem;
        this.totalDisk   = totalDisk;
        this.shortcuts   = (shortcuts != null) ? shortcuts : new ArrayList<>();
    }

    public void setRealtimeUsage(int cpu, int ramUsed, int diskUsed, long uptime) {
        this.currentCpu      = cpu;
        this.currentRamUsed  = ramUsed;
        this.currentDiskUsed = diskUsed;
        this.uptime          = uptime;
    }

    // Getter
    public String getPcName()        { return pcName; }
    public String getIpAddress()     { return ipAddress; }
    public boolean isOnline()        { return isOnline; }
    public String getOsInfo()        { return osInfo; }
    public String getCpuInfo()       { return cpuInfo; }
    public int getTotalMemory()      { return totalMemory; }
    public int getTotalDisk()        { return totalDisk; }
    public List<String> getShortcuts(){ return shortcuts; }
    public int getCurrentCpu()       { return currentCpu; }
    public int getCurrentRamUsed()   { return currentRamUsed; }
    public int getCurrentDiskUsed()  { return currentDiskUsed; }
    public long getUptime()          { return uptime; }
}
