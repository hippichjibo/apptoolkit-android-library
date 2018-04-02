package com.jibo.apptoolkit.android.websocket;

/**
 * Created by user on 27.12.17.
 */


import com.jibo.apptoolkit.android.util.FlavourHelper;

import org.glassfish.tyrus.server.Server;

import java.io.IOException;
import java.util.ArrayList;

import javax.websocket.DeploymentException;
import javax.websocket.Session;

public class EchoServer {
    protected static boolean WELCOME_MESSAGE = true;
    protected static boolean ECHO_SERVER = true;

    private static ArrayList<Session> clients = new ArrayList<Session>();
    private static int PORT = Integer.valueOf(FlavourHelper.SOCKET_PORT);
    private static boolean shutDown = false;

    private enum Commands {
        DEFAULT,
        HELP,
        PRINT,
        PRIVATE,
        EXIT
    }

    private static Server server;
    public static void main(String[] args) {
        runServer();
    }

    public static void shutdown() {
//        shutDown = true;
        server.stop();
    }

    private static void runServer() {
        server = new Server("localhost", EchoServer.PORT, "/", WebSocketClient.class);

        try {
            server.start();
//            System.out.println("Now you can visit the website http://websocket.org/echo.html and begin to chatting!\n - For help, write the HELP command - ");
//
//            int i = 0;
//            while (true) {
//                Thread.sleep(10);
//                if (shutDown) {
//                    System.out.println("The process ends...");
//                    break;
//                }
//                i++;
//                if (i > 10000) shutDown = true;
//            }
        } catch (DeploymentException e) {
            throw new RuntimeException(e);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
        } finally {
//            server.stop();
        }
    }

    protected static void addClient(Session session) {
        EchoServer.clients.add(session);
    }

    protected static ArrayList<Session> getClients() {
        return EchoServer.clients;
    }

    protected static void removeClient(Session session) {
        EchoServer.clients.remove(session);
    }

    private static void sendMessage(String message) {
        sendMessage(message, null);
    }

    private static void sendMessage(String message, String sessionId) {
        if (EchoServer.getClients().size() > 0) {
            try {
                int count = 0;
                for (Session session : EchoServer.getClients()) {
                    if (sessionId != null) {
                        if (session.getId().equals(sessionId)) {
                            session.getBasicRemote().sendText(message);
                            count++;
                            break;
                        }
                    } else {
                        session.getBasicRemote().sendText(message);
                        count++;
                    }
                }
                System.out.println("Has been sent to " + count + " client" + (count > 1 ? "s" : ""));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("No connected clients");
        }
    }
}