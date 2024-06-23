package ru.geekbrains.chat.server;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ClientManager implements Runnable {

    private final Socket socket;            // Дескриптор для связи между клиентом и сервером по протоколу IP.
    private BufferedReader bufferedReader;  // Класс библиотеки Java, предназначенный для чтения данных из потока байтов.
    private BufferedWriter bufferedWriter;  // Класс библиотеки Java, используемый для записи данных в поток байтов.
    private String name;                    // Имя клиента.

    public final static ArrayList<ClientManager> clients = new ArrayList<>(); // Коллекция всех клиентов.

    public ClientManager(Socket socket) {
        this.socket = socket;
        try {
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            name = bufferedReader.readLine();
            clients.add(this);
            System.out.println(name + " подключился к чату.");
            broadcastMessage("Server: " + name + " подключился к чату.");
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }


    }

    @Override
    public void run() {
        String massageFromClient;

        while (socket.isConnected()) {
            try {
                massageFromClient = bufferedReader.readLine();
                /*if (massageFromClient == null){
                    // для  macOS
                    closeEverything(socket, bufferedReader, bufferedWriter);
                    break;
                }*/
                broadcastMessage(massageFromClient);
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
                break;
            }
        }
    }

    /**
     * Метод broadcastMessage реализует алгоритм действий по отправке сообщений клиентам.
     * Происходит перебор клиентов из списка и в случае, если имя клиента не соответствует имени, с которого поступило
     * сообщение, то ему пересылается это сообщение.
     * При возникновении ошибок, вызывается метод closeEverything.
     *
     * @param message - текст сообщения от клиента.
     */

    private void broadcastMessage(String message){

        for (ClientManager client: clients) {
            try {
                if (message.startsWith("@")) {
                    String[] parts = message.split("\\s+", 2);
                    String recipient = null;
                    String privateMessage = null;
                    if (parts.length == 2 && parts[0].startsWith("@")){
                        recipient = parts[0].substring(1);
                        privateMessage = parts[1];
                    }
                    if (client.name.equals(recipient)) {
                        client.bufferedWriter.write(privateMessage);
                        client.bufferedWriter.newLine();
                        client.bufferedWriter.flush();
                    }
                } else {
                    if (!client.name.equals(name)) {
                        client.bufferedWriter.write(message);
                        client.bufferedWriter.newLine();
                        client.bufferedWriter.flush();
                    }
                }
            }
            catch (IOException e){
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
    }


    /**
     * Метод closeEverything обеспечивает удаление клиента из коллекции, закрывает буферы на чтение и на запись,
     * а также клиентский сокет, в случае возникновения ошибки.
     *
     * @param socket         — дескриптор для связи между клиентом и сервером по протоколу IP.
     * @param bufferedReader — это класс из библиотеки Java, предназначенный для чтения данных из потока байтов.
     * @param bufferedWriter — это класс из библиотеки Java, который используется для записи данных в поток байтов.
     */
    private void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        // Удаление клиента из коллекции
        removeClient();
        try {
            // Завершаем работу буфера на чтение данных
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            // Завершаем работу буфера для записи данных
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            // Закрытие соединения с клиентским сокетом
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод removeClient выполняет удаление клиента из коллекции при возникновении ошибки.
     */
    private void removeClient() {
        clients.remove(this);
        System.out.println(name + " покинул чат.");
        broadcastMessage("Server: " + name + " покинул чат.");
    }

}
