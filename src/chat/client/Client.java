package chat.client;

import chat.Connection;
import chat.ConsoleHelper;
import chat.Message;
import chat.MessageType;

import java.io.IOException;
import java.net.Socket;

public class Client {
    public static void main(String[] args) throws InterruptedException {
        Client client = new Client();
        client.run();
    }

    protected Connection connection;
    private volatile boolean clientConnected = false;

    protected String getServerAddress() {
        return ConsoleHelper.readString();
    }

    protected int getServerPort() {
        return ConsoleHelper.readInt();
    }

    protected String getUserName() {
        return ConsoleHelper.readString();
    }

    protected boolean shouldSendTextFromConsole() {
        return true;
    }

    protected SocketThread getSocketThread() {
        return new SocketThread();
    }

    protected void sendTextMessage(String text) {
        try {
            connection.send(new Message(MessageType.TEXT, text));
        } catch(IOException e) {
            ConsoleHelper.writeMessage("Во время отправки сообщения произошла ошибка.");
            clientConnected = false;
        }
    }

    public synchronized void run() throws InterruptedException {
        SocketThread socketThread = getSocketThread();
        socketThread.setDaemon(true);
        socketThread.start();
        try {
            wait();
        } catch (InterruptedException e) {
            ConsoleHelper.writeMessage("Ошибка");
            return;
        }

        if (clientConnected) ConsoleHelper.writeMessage("Соединение установлено. Для выхода наберите команду 'exit'.");
        else ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента.");

        String text = "";
        while (clientConnected) {
            text = ConsoleHelper.readString();
            if (text.equals("exit")) break;
            else if (shouldSendTextFromConsole()) sendTextMessage(text);
        }
        notify();
    }

    public class SocketThread extends Thread {
        protected void processIncomingMessage(String message) {
            ConsoleHelper.writeMessage(message);
        }

        protected void informAboutAddingNewUser(String userName) {
            ConsoleHelper.writeMessage(userName + " подключился к чату.");
        }

        protected void informAboutDeletingNewUser(String userName) {
            ConsoleHelper.writeMessage(userName + " покинул чат.");
        }

        protected void notifyConnectionStatusChanged(boolean clientConnected) {
            Client.this.clientConnected = clientConnected;
            synchronized (Client.this) {
                Client.this.notify();
            }
        }

        protected void clientHandshake() throws IOException, ClassNotFoundException {
            Message message;
            while (true) {
                message = connection.receive();
                if (message.getType() == null) throw new IOException("Unexpected MessageType");
                switch (message.getType()) {
                    case NAME_REQUEST:
                        connection.send(new Message(MessageType.USER_NAME, getUserName()));
                        break;
                    case NAME_ACCEPTED:
                        notifyConnectionStatusChanged(true);
                        return;
                    default:
                        throw new IOException("Unexpected MessageType");
                }
            }
        }

        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            Message message;
            while (true) {
                message = connection.receive();
                if (message.getType() == null) throw new IOException("Unexpected MessageType");
                switch (message.getType()) {
                    case TEXT:
                        processIncomingMessage(message.getData());
                        break;
                    case USER_ADDED:
                        informAboutAddingNewUser(message.getData());
                        break;
                    case USER_REMOVED:
                        informAboutDeletingNewUser(message.getData());
                        break;
                    default:
                        throw new IOException("Unexpected MessageType");
                }
            }
        }

        @Override
        public void run() {
            String serverAddress = getServerAddress();
            int serverPort = getServerPort();

            Socket socket = null;
            Connection connection;
            try {
                socket = new Socket(serverAddress, serverPort);
                connection = new Connection(socket);
                clientHandshake();
                clientMainLoop();
            } catch (IOException | ClassNotFoundException e) {
                notifyConnectionStatusChanged(false);
            }
        }
    }
}