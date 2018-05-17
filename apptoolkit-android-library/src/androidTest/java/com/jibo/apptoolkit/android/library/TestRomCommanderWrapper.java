package com.jibo.apptoolkit.android.library;

import com.jibo.apptoolkit.protocol.CommandLibrary;
import com.jibo.apptoolkit.protocol.OnConnectionListener;
import com.jibo.apptoolkit.protocol.model.Command;
import com.jibo.apptoolkit.protocol.model.EventMessage;

import org.jetbrains.annotations.NotNull;

import java.io.InputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * Created by dacuesta on 20/02/2018.
 */

public class TestRomCommanderWrapper extends WebSocketListener
        implements OnConnectionListener, CommandLibrary.OnCommandResponseListener {

    // attributes

    private CommandLibrary mCommandLibrary;
    private WebSocket    mWebSocket;
    private String       mLatestCommandId;

    private boolean mIsConnected  = false;
    private int     mErrorCounter = 0;

    // constructor

    public TestRomCommanderWrapper() {
    }

    // WebSocketListener functions

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        System.out.println("Socket :: onOpen");

        mCommandLibrary = new CommandLibrary(null, webSocket, "", this);
        mCommandLibrary.startSession();
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        System.out.println("Socket :: onMessage: " + text);

        if (mCommandLibrary != null) {
            mCommandLibrary.parseJiboResponse(text);
        }
    }

    @Override
    public void onMessage(WebSocket webSocket, ByteString bytes) {
        System.out.println("Socket :: onMessage");
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        System.out.println("Socket :: onClosing");
    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        System.out.println("Socket :: onClosed");

        if (mCommandLibrary != null) {
            mCommandLibrary.disconnect();
        }
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        System.out.println("Socket :: onFailure");

        mErrorCounter++;
    }

    // JiboCommandControl.OnConnectionListener functions

    @Override
    public void onConnected() {
        System.out.println("JiboConnection :: onConnected");

        mIsConnected = true;
    }

    @Override
    public void onSessionStarted(@NotNull CommandLibrary romCommander) {
        System.out.println("JiboConnection :: onSessionStarted");

        mIsConnected = true;
    }

    @Override
    public void onConnectionFailed(@NotNull Throwable throwable) {
        System.out.println("JiboConnection :: onConnectionFailed");

        mErrorCounter++;
    }

    @Override
    public void onDisconnected(int code) {
        System.out.println("JiboConnection :: onDisconnected");
    }

    @Override
    public void onSuccess(@NotNull String transactionID) {
        System.out.println("RomOnCommand :: onSuccess");
    }

    @Override
    public void onError(@NotNull String transactionID, @NotNull String errorMessage) {
        System.out.println("RomOnCommand :: onError");

        mErrorCounter++;
    }

    @Override
    public void onEventError(@NotNull String transactionID, @NotNull EventMessage.ErrorEvent.ErrorData errorData) {
        System.out.println("RomOnCommand :: onEventError");

        mErrorCounter++;
    }

    @Override
    public void onSocketError() {
        System.out.println("RomOnCommand :: onSocketError");

        mErrorCounter++;
    }

    @Override
    public void onEvent(@NotNull String transactionID, @NotNull EventMessage.BaseEvent event) {
        System.out.println("RomOnCommand :: onEvent");
    }

    @Override
    public void onPhoto(@NotNull String transactionID, @NotNull EventMessage.TakePhotoEvent event, @NotNull InputStream inputStream) {
        System.out.println("RomOnCommand :: onPhoto");
    }

    @Override
    public void onVideo(@NotNull String transactionID, @NotNull EventMessage.VideoReadyEvent event, @NotNull InputStream inputStream) {
        System.out.println("RomOnCommand :: onVideo");
    }

    @Override
    public void onListen(@NotNull String transactionID, @NotNull String speech) {
        System.out.println("RomOnCommand :: onListen=" + speech);
    }

    @Override
    public void onParseError() {
        System.out.println("RomOnCommand :: onParseError");

        mErrorCounter++;
    }

    // public functions

    public void connect() {
        OkHttpClient mHttpClient = new OkHttpClient.Builder().build();
        Request      request     = new Request.Builder().url(TestHelper.getJiboURL()).build();
        mWebSocket = mHttpClient.newWebSocket(request, this);
    }

    public void disconnect() {
        mWebSocket.close(1000, "");
    }

    public void takePhoto() {
        if (mCommandLibrary != null) {
            mLatestCommandId = mCommandLibrary.takePhoto(
                    Command.TakePhotoRequest.Camera.Left,
                    Command.TakePhotoRequest.CameraResolution.MedRes,
                    false,
                    this);
        }
    }

    public void video() {
        if (mCommandLibrary != null) {
            mLatestCommandId = mCommandLibrary.video(
                    Command.VideoRequest.VideoType.Debug,
                    0,
                    this);
        }
    }

    public void cancel() {
        if (mCommandLibrary != null && mLatestCommandId != null) {
            mCommandLibrary.cancel(mLatestCommandId, this);
        }
    }

    public void say() {
        if (mCommandLibrary != null) {
            String text = "<pitch mult=\"1\">Let's go!</pitch>";
            mCommandLibrary.say(text, this);
        }
    }

    public void lookAt(int target) {
        if (mCommandLibrary != null) {
            switch (target) {
                case 0:
                    lookAtPosition();
                    break;
                case 1:
                    lookAtAngle();
                    break;
                case 2:
                    lookAtEntity();
                    break;
                case 3:
                    lookAtCamera();
                    break;
            }
        }
    }

    public boolean isConnected() {
        return mIsConnected;
    }

    public int getErrorCounter() {
        return mErrorCounter;
    }

    // private functions

    private void lookAtPosition() {
        int[] position = {10, 10, 10};
        mCommandLibrary.lookAt(new Command.LookAtRequest.PositionTarget(position), this);
    }

    private void lookAtAngle() {
        int[] angle = {10, 10};
        mCommandLibrary.lookAt(new Command.LookAtRequest.AngleTarget(angle), this);
    }

    private void lookAtEntity() {
        mCommandLibrary.lookAt(new Command.LookAtRequest.EntityTarget(10L), this);
    }

    private void lookAtCamera() {
        int[] coordinates = {10, 10};
        mCommandLibrary.lookAt(new Command.LookAtRequest.CameraTarget(coordinates), this);
    }

}
