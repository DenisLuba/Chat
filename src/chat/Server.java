package chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    public static void sendBroadcastMessage(Message message) throws IOException {
        for (Connection connection : connectionMap.values()) {
            try {
                connection.send(message);
            } catch (IOException e) {
                ConsoleHelper.writeMessage("Не удалось отправить сообщение.");
            }
        }
    }

    public static void main(String[] args) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(ConsoleHelper.readInt())) {
            ConsoleHelper.writeMessage("Сервер запущен.");
            while (true) {
                new Handler(serverSocket.accept()).start();
            }
        } catch (Exception e) {
            ConsoleHelper.writeMessage(e.getMessage());
        }
    }

    private static class Handler extends Thread {
        private Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException {
            Message message;
            while (true) {
                connection.send(new Message(MessageType.NAME_REQUEST));
                message = connection.receive();
                if (message.getType() == MessageType.USER_NAME && message.getData() != null && !message.getData().equals("") && !connectionMap.containsKey(message.getData())) break;
            }
            connectionMap.put(message.getData(), connection);
            connection.send(new Message(MessageType.NAME_ACCEPTED));
            return message.getData();
        }

        private void notifyUsers(Connection connection, String userName) throws IOException {
            for (String name : connectionMap.keySet()) {
                if (name.equals(userName)) continue;
                connection.send(new Message(MessageType.USER_ADDED, name));
            }
        }

        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException {
            while(true) {
                Message message = connection.receive();
                if (message.getType() == MessageType.TEXT) {
                    String text = String.format("%s: %s", userName, message.getData());
                    sendBroadcastMessage(new Message(MessageType.TEXT, text));
                } else ConsoleHelper.writeMessage("Ошибка! Данное сообщение не является текстом. Попробуйте снова, пожалуйста.");
            }
        }

        @Override
        public void run() {
            String address = socket.getRemoteSocketAddress().toString();
            ConsoleHelper.writeMessage("Установлено новое соединение с удаленным адресом " + address);
            String userName = "";
            try (Connection connection = new Connection(socket)) {
                userName = serverHandshake(connection);
                sendBroadcastMessage(new Message(MessageType.USER_ADDED, userName));
                notifyUsers(connection, userName);
                serverMainLoop(connection, userName);
                connectionMap.remove(userName);
                sendBroadcastMessage(new Message(MessageType.USER_REMOVED, userName));
                ConsoleHelper.writeMessage("Соединение с удаленным адресом закрыто.");
            } catch (IOException | ClassNotFoundException e) {
                ConsoleHelper.writeMessage("Произошла ошибка при обмене данными с удалённым пользователем.");
            }
        }
    }
}
