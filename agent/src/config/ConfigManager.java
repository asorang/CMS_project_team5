package src.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import src.AgentState;

import javax.swing.*;
import java.io.*;

/**
 * Config Manager (설정 파일 매니저)
 * Config 파일 관련한 동작 (불러오기, 저장하기) 담당
 */
public class ConfigManager {
    // 설정 파일 로드
    public static void loadConfig() {
        File file = new File(AgentState.CONFIG_FILE);
        if (!file.exists()) {
            // 설정파일을 찾을 수 없으면 기본 설정파일을 생성 및 로드한다.
            defaultConfig();
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(
                            null,
                            "기본 설정 파일이 생성되었습니다.\n설정 창에서 기본값을 수정 후 사용해주세요.",
                            "알림",
                            JOptionPane.INFORMATION_MESSAGE
                    )
            );
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            JsonObject root = JsonParser.parseReader(br).getAsJsonObject();

            if (root.has("password")) AgentState.PASSWORD = root.get("password").getAsString();
            if (root.has("port"))     AgentState.PORT     = root.get("port").getAsInt();

            AgentState.PROGRAMS.clear();
            root.get("programs").getAsJsonArray().forEach(el -> {
                JsonObject p = el.getAsJsonObject();
                AgentState.PROGRAMS.add(new String[]{
                        p.get("name").getAsString(),
                        p.get("path").getAsString()
                });
            });
            System.out.println("설정 로드 완료. 프로그램 " + AgentState.PROGRAMS.size() + "개");
        } catch (Exception e) {
            System.out.println("설정 로드 실패: " + e.getMessage());
        }
    }
    // 설정 파일 신규 작성 (없는 경우)
    static void defaultConfig() {
        JsonObject root = new JsonObject();
        root.addProperty("password", "1234");
        root.addProperty("port", 10293);
        root.add("programs", new JsonArray());

        try (FileWriter fw = new FileWriter(AgentState.CONFIG_FILE)) {
            fw.write(root.toString());
            System.out.println("기본 설정 파일 생성됨");
        } catch (Exception e) {
            System.out.println("기본 설정 저장 실패: " + e.getMessage());
        }
    }


    // 설정 파일 저장
    public static void saveConfig() {
        JsonObject root = new JsonObject();
        root.addProperty("password", AgentState.PASSWORD);
        root.addProperty("port",     AgentState.PORT);

        JsonArray programs = new JsonArray();
        for (String[] p : AgentState.PROGRAMS) {
            JsonObject entry = new JsonObject();
            entry.addProperty("name", p[0]);
            entry.addProperty("path", p[1]);
            programs.add(entry);
        }
        root.add("programs", programs);

        try (FileWriter fw = new FileWriter(AgentState.CONFIG_FILE)) {
            fw.write(root.toString());
            System.out.println("설정 저장 완료");
        } catch (Exception e) {
            System.out.println("설정 저장 실패: " + e.getMessage());
        }
    }
}
