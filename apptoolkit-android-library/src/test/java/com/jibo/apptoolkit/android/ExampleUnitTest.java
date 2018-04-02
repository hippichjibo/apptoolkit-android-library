package com.jibo.apptoolkit.android;

import com.jibo.apptoolkit.android.util.FlavourHelper;
import com.jibo.apptoolkit.android.websocket.EchoServer;
import com.jibo.apptoolkit.protocol.CommandLibrary;
import com.jibo.apptoolkit.protocol.OnConnectionListener;
import com.jibo.apptoolkit.protocol.model.EventMessage;

import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class ExampleUnitTest {

    private WebSocket mDefaultSocketImpl = new WebSocket() {
        @Override
        public Request request() {
            return null;
        }

        @Override
        public long queueSize() {
            return 0;
        }

        @Override
        public boolean send(String s) {
            return true;
        }

        @Override
        public boolean send(ByteString byteString) {
            return false;
        }

        @Override
        public boolean close(int i, String s) {
            return false;
        }

        @Override
        public void cancel() {

        }
    };
    private static CommandLibrary commandLibrary;
    static private int sErrorsCounter = 0;
    OnConnectionListener onConnectionListener = new OnConnectionListener() {
        @Override
        public void onConnected() {

        }

        @Override
        public void onSessionStarted(CommandLibrary romCommander) {
            sErrorsCounter++;
            romCommander.cancel("123", new CommandLibrary.OnCommandResponseListener() {
                @Override
                public void onSuccess(String transactionID) {
                    assertNotNull(transactionID);
                    assertNotEquals(0, transactionID.length());
                    sErrorsCounter--;
                }

                @Override
                public void onError(String transactionID, String errorMessage) {
                }

                @Override
                public void onEventError(String transactionID, EventMessage.ErrorEvent.ErrorData errorData) {
                }

                @Override
                public void onSocketError() {

                }

                @Override
                public void onEvent(String transactionID, EventMessage.BaseEvent event) {

                }

                @Override
                public void onPhoto(String transactionID, EventMessage.TakePhotoEvent event, InputStream inputStream) {

                }

                @Override
                public void onVideo(String transactionID, EventMessage.VideoReadyEvent event, InputStream inputStream) {

                }

                @Override
                public void onListen(@NotNull String transactionID, @NotNull String speech) {

                }

                @Override
                public void onParseError() {

                }
            });
        }

        @Override
        public void onConnectionFailed(Throwable throwable) {

        }

        @Override
        public void onDisconnected(int code) {

        }
    };

    @Before
    public void prepareValues() {

        if (commandLibrary != null) {
            commandLibrary.disconnect();
            commandLibrary = null;
        }
        sErrorsCounter = 0;
    }

    @After
    public void removeValues() {
        EchoServer.shutdown();
//        if (mWebSocket != null)
//            mWebSocket.close(1000, "");
//        if (mHttpClient != null) {
//            mHttpClient.dispatcher().executorService().shutdown();
//            mHttpClient = null;
//        }
    }

    @Test
    public void addition_isCorrect() throws Exception {

//        new Thread(new Runnable() {
//            @Override
//            public void run() {
                EchoServer.main(null);
//            }
//        }).start();
        Thread.sleep(1000);
        OkHttpClient mHttpClient = new OkHttpClient.Builder().build();
        Request request = new Request.Builder().url(new StringBuilder("ws://").append("localhost")
                .append(":").append(FlavourHelper.SOCKET_PORT).toString()).build();
        WebSocket mWebSocket = mHttpClient.newWebSocket(request, new TestWebsocketListener(onConnectionListener));
        Thread.sleep(5000);
        assertEquals(0, sErrorsCounter);
    }

    private static class TestWebsocketListener extends WebSocketListener {
        private OnConnectionListener onConnectionListener;

        public TestWebsocketListener(OnConnectionListener onConnectionListener) {
            this.onConnectionListener = onConnectionListener;
        }

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            System.out.println("Now socket open");
            commandLibrary = new CommandLibrary(null, webSocket, "", onConnectionListener);
            commandLibrary.startSession();
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            System.out.println("onMessage:" + text);
            if (commandLibrary != null) commandLibrary.parseJiboResponse(text);
            if (text == null || text.isEmpty()) {
                assertNotEquals(0, text.length());
            }
        }

        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
            System.out.println("onMessage");
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            System.out.println("onClosing");
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            System.out.println("onClosed");
            commandLibrary.disconnect();
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            System.out.println("onFailure ");
        }
    };
}