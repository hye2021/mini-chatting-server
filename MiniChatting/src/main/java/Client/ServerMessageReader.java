package Client;

import java.io.BufferedReader;
import java.io.IOException;

public class ServerMessageReader implements Runnable {
    private BufferedReader in;

    public ServerMessageReader(BufferedReader in) {
        this.in = in;
    }

    @Override
    public void run() {
        try {
            String serverLine;
            // 서버로부터 받은 메시지가 null이라면
            // 서버와의 연결이 끊어진 것으로 판단하여 종료함
            while ((serverLine = in.readLine()) != null) {
                System.out.println(serverLine); // 서버로부터 받은 메시지를 출력
            }
        } catch (IOException e) {
            System.out.println("Server connection was closed.");
        }
    }
}
