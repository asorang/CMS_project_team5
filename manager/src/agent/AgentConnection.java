package src.agent;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Agent 1대의 연결 정보를 담는 데이터 클래스
 */
public class AgentConnection {
    public enum Status {
        ONLINE, OFFLINE, CONNECTING
    }

    public String alias;
    public String ip;
    public String pw;
    public Socket socket;
    public PrintWriter out;
    public BufferedReader in;
    public Status status;

    public AgentConnection(String alias, String ip, String pw,
                           Socket socket, PrintWriter out, BufferedReader in,
                           Status status) {
        this.alias    = alias;
        this.ip       = ip;
        this.pw       = pw;
        this.socket   = socket;
        this.out      = out;
        this.in       = in;
        this.status   = status;
    }
}
