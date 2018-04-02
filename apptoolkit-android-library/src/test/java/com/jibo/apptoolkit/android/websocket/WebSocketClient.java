package com.jibo.apptoolkit.android.websocket;

/**
 * Created by user on 27.12.17.
 */


import com.google.gson.Gson;
import com.jibo.apptoolkit.protocol.model.Acknowledgment;
import com.jibo.apptoolkit.protocol.model.Command;
import com.jibo.apptoolkit.protocol.model.Header;

import java.io.IOException;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/")
public class WebSocketClient {

    private static Gson gson = new Gson();
    @OnOpen
    public void onOpen(Session session) {
        System.out.println(session.getId() + " has opened a connection");
//        if (EchoServer.WELCOME_MESSAGE) {
//            try {
//                session.getBasicRemote().sendText("Connection Established. Echo mode = " + (EchoServer.ECHO_SERVER ? "on" : "off"));
//            } catch (IOException ex) {
//                ex.printStackTrace();
//            }
//        }
        EchoServer.addClient(session);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("[" + session.getId() + "]: " + message);

        Command command = gson.fromJson(message, Command.class);
        if (command.getCommand().getType().equals(Command.CommandType.StartSession)) {
            try {
                session.getBasicRemote().sendText(new Gson().toJson(new Acknowledgment(new Header.ResponseHeader(command.getClientHeader().getTransactionID() ),
                        new Acknowledgment.SessionResponse(new Acknowledgment.SessionResponse.SessionInfo("AAAA", "1.0")
                        ))));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else {
            try {
                session.getBasicRemote().sendText("");
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        }
//        if (EchoServer.ECHO_SERVER) {
//            try {
//                session.getBasicRemote().sendText(message);
//            } catch (IOException ex) {
//                ex.printStackTrace();
//            }
//        }
    }

    @OnClose
    public void onClose(Session session) {
        System.out.println("Session " + session.getId() + " has ended");
        EchoServer.removeClient(session);
    }

    @OnError
    public void onError(Throwable exception, Session session) {
        exception.printStackTrace();
        System.err.println("Error for client: " + session.getId());
    }
}