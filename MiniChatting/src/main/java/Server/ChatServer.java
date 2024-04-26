package Server;

import java.net.*; // network
import java.io.*; // I/O stream
import java.util.*;

// Lobby
public class ChatServer {
    public static void main(String[] args) {
        ServerSocket serverSocket = null;
        List<Map<String, PrintWriter>> clientData = Collections.synchronizedList(new ArrayList<>()); // 채팅방 클라이언트 목록
        clientData.add(Collections.synchronizedMap(new HashMap<>()));

        try {
            // (1) 서버 소켓 생성
            serverSocket = new ServerSocket(12345);
            System.out.println("start mini chatting program!!");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // (2) 클라이언트의 연결 요청 대기
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                // (3) 연결하는 클라이언트마다 전송받는 데이터를 기다려야 하므로 스레드 생성
                new ClientConnection(socket, clientData).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
