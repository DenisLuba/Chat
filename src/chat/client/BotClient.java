package chat.client;

import chat.ConsoleHelper;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

public class BotClient extends Client {
    public static void main(String[] args) throws InterruptedException {
        BotClient botClient = new BotClient();
        botClient.run();
    }

    @Override
    protected SocketThread getSocketThread() {
        return new BotSocketThread();
    }

    @Override
    protected boolean shouldSendTextFromConsole() {
        return false;
    }

    @Override
    protected String getUserName() {
        return String.format("date_bot_%d", (int) (Math.random() * 100));
    }

    public class BotSocketThread extends SocketThread {
        @Override
        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            BotClient.this.sendTextMessage("Приветик чатику. Я бот. Понимаю команды: дата, день, месяц, год, время, час, минуты, секунды.");
            super.clientMainLoop();
        }

        @Override
        protected void processIncomingMessage(String message) {
            ConsoleHelper.writeMessage(message);
            if (!message.contains(":")) return;
            String name = message.replaceFirst("(.+): (.+)", "$1");
            String text = message.replaceFirst("(.+): (.+)", "$2");
            String pattern = "";
            switch (text) {
                case "дата":
                    pattern = "d.MM.yyyy";
                    break;
                case "день":
                    pattern = "d";
                    break;
                case "месяц":
                    pattern = "MMMM";
                    break;
                case "год":
                    pattern = "yyyy";
                    break;
                case "время":
                    pattern = "H:mm:ss";
                    break;
                case "час":
                    pattern = "H";
                    break;
                case "минуты":
                    pattern = "m";
                    break;
                case "секунды":
                    pattern = "s";
                    break;
                default:
                    return;
            }
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
            String time = simpleDateFormat.format(new GregorianCalendar().getTime());
            String answer = String.format("Информация для %s: %s", name, time);
            sendTextMessage(answer);
        }
    }
}
