package Server;

import java.net.*;
import java.io.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientConnection extends Thread {
    private Socket socket;
    private String name;
    private BufferedReader in; // 입력 스트림
    private PrintWriter out; // 출력 스트림
    private List<Map<String, PrintWriter>> clientData; // 채팅방 클라이언트 목록 (0-전체, 1-방1, 2-방2, ...)
    int roomNumber = -1; // 현재 클라이언트가 속한 채팅방 번호

    // Constructor
    public ClientConnection(Socket socket, List<Map<String, PrintWriter>> clientData) {
        this.socket = socket;
        this.clientData = clientData;

        try {
            // (1) 입출력 스트림 생성
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // (2) 닉네임 설정
            name = in.readLine();

            // clientData에 추가
            clientData.getFirst().put(name, out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        System.out.println(name + " 닉네임의 사용자가 연결했습니다. (" + socket.getInetAddress() + ")");
        out.println("\n명령어를 입력하여 원하는 메뉴를 선택할 수 있습니다.\n" +
                "방 목록 보기 : /list\n" +
                "방 생성 : /create\n" +
                "방 입장 : /join [방번호]\n" +
                "방 나가기 : /exit\n" +
                "접속 종료 : /bye\n");

        String msg = null;
        try {
            while ((msg = in.readLine()) != null) {
                if (msg.startsWith("/")) {
                    String menu = msg.trim().split(" ")[0];
                    // 방 목록 보기
                    if (menu.equals("/list")) {
                        if (clientData.size() == 1) {
                            out.println("현재 채팅방이 없습니다.\n");
                            continue;
                        }
                        var list = clientData.stream().skip(1).filter(m -> !m.isEmpty()).toList();
                        if (list.isEmpty())
                            out.println("현재 채팅방이 없습니다.\n");
                        else {
                            out.println("현재 채팅방 목록입니다.");
                            list.forEach(m -> {
                                out.println("방 번호 : " + clientData.indexOf(m) + ", 인원 : " + m.size());
                            });
                        }
                        out.println();
                    } // 방 생성하기
                    else if (menu.equals("/create")) {
                        clientData.add(Collections.synchronizedMap(new HashMap<>()));
                        roomNumber = clientData.size() - 1;
                        clientData.get(roomNumber).put(name, out);
                        out.println(roomNumber + "번 채팅방이 생성되었습니다.");
                        out.println(roomNumber + "번 채팅방에 입장했습니다.\n");

                    } // 방 입장하기
                    else if (menu.equals("/join")) {
                        try {
                            roomNumber = Integer.parseInt(msg.trim().split(" ")[1]);
                            if(clientData.get(roomNumber).isEmpty()) {
                                out.println(roomNumber + "번 채팅방이 없습니다.\n");
                                roomNumber = -1;
                            }
                            clientData.get(roomNumber).put(name, out);
                            multicast(name + "님이 " + roomNumber + "번 채팅방에 입장했습니다.\n", roomNumber);
                        } catch (Exception e) {
                            out.println("잘못된 방 번호입니다.\n");
                        }
                    } // 방 나가기
                    else if (menu.equals("/exit")) {
                        if (roomNumber == -1) {
                            out.println("아직 채팅방에 입장하지 않았습니다.");
                        } else {
                            clientData.get(roomNumber).remove(name);
                            out.println(roomNumber + "번 채팅방에서 나왔습니다.");
                            if(clientData.get(roomNumber).isEmpty())
                                out.println(roomNumber + "번 채팅방이 삭제되었습니다.");
                            else
                                multicast(name + "님이 채팅방을 나갔습니다." , roomNumber);
                            roomNumber = -1;
                        }
                        out.println();
                    } // 프로그램 종료하기
                    else if (menu.equals("/bye")) {
                        System.out.println(name + " 닉네임의 사용자가 연결을 끊었습니다. ");
                        if (roomNumber != -1) {
                            clientData.get(roomNumber).remove(name);
                        }
                        break;
                    }
                    else {
                        out.println("잘못된 명령어입니다.\n");
                    }
                } else {
                    if (roomNumber == -1) {
                        out.println("채팅방에 입장해야 채팅을 할 수 있습니다.\n");
                    } else {
                        multicast("[" + name + "] " + msg, roomNumber);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            clientData.getFirst().remove(name);

            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void multicast(String msg, int room) {
        var clients = clientData.get(room);
        for (PrintWriter out : clients.values()) {
            out.println(msg);
        }
    }
}
