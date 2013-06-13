package android.netinf.node.services.visualization;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.io.IOUtils;

import android.netinf.messages.Get;
import android.netinf.messages.GetResponse;
import android.netinf.messages.Publish;
import android.netinf.messages.PublishResponse;
import android.netinf.messages.Response;
import android.netinf.messages.Search;
import android.netinf.messages.SearchResponse;
import android.netinf.node.SettingsActivity;
import android.netinf.node.logging.LogEntry;
import android.netinf.node.logging.LogService;
import android.util.Log;

public class VisualizationService implements LogService {

    public static final String TAG = VisualizationService.class.getSimpleName();

    private Socket mSocket;
    private PrintWriter mOut;
    private BufferedReader mIn;
    private Timer mPingPongTimer;

    @Override
    public void start() {
        mPingPongTimer = new Timer();
        mPingPongTimer.schedule(new PingPong(), 0, 2000);
    }

    private String getId() {
        return SettingsActivity.getPreference("pref_key_visualization_id");
    }

    private String getIp() {
        return SettingsActivity.getPreference("pref_key_visualization_ip");
    }

    private int getPort() {
        return SettingsActivity.getPreferenceAsInt("pref_key_visualization_port");
    }

    private void restartSocket() {

        Log.i(TAG, "Restarting socket");

        IOUtils.closeQuietly(mIn);
        IOUtils.closeQuietly(mOut);
        IOUtils.closeQuietly(mSocket);

        try {
            mSocket = new Socket(getIp(), getPort());
            mSocket.setSoTimeout(1000);
            mIn = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
            mOut = new PrintWriter(mSocket.getOutputStream(), true);
        } catch (IOException e) {
            Log.e(TAG, "Failed to restart socket", e);
        }

    }


    // Client ID = "NRS", "Android 1", etc.
    // message type = "GET", "PUBLISH", "SEARCH", "RESP"
    // direction = "IN", "OUT"

    private void send(String clientId, String messageId, String messageType, String messageDirection, String ndoUri) {

        try {
            StringBuilder builder = new StringBuilder();
            builder.append(clientId);
            builder.append(" ");
            builder.append(messageId);
            builder.append(" ");
            builder.append(messageType);
            builder.append(" ");
            builder.append(messageDirection);
            builder.append(" ");
            builder.append(ndoUri);
            send(builder.toString());
        } catch (IOException e) {
            Log.e(TAG, "Failed to send message to Visualization Server", e);
        }

    }

    private synchronized void send(String message) throws IOException {
        try {
            mOut.println(message);
        } catch (Exception e) {
            throw new IOException("Failed to send message to Visualization Server", e);
        }
    }

    private class PingPong extends TimerTask {

        private String mMessage = "Notification Service initialization for ";

        @Override
        public void run() {
            try {
                ping();
                // Log.i(TAG, "Visualization (Ping)");
                pong();
                // Log.i(TAG, "Visualization (Pong)");
            } catch (IOException e) {
                Log.e(TAG, "Communication with Visualization Server failed", e);
                restartSocket();
            }
        }

        private void ping() throws IOException {
            if (mSocket == null) {
                restartSocket();
            }
            send(mMessage + getId());
        }

        private void pong() throws IOException {
            String inLine = null;
            while ((inLine = mIn.readLine()) != null) {
                if (inLine.startsWith("Notification")) {
                    if (!inLine.equals(mMessage + getId())) {
                        throw new IOException("Read '" + inLine + "', expected '" + mMessage + "'");
                    } else {
                        break;
                    }
                }
                // Log.d(TAG, "pong() read: " + inLine);
            }
        }
    }

    public String getDirection(LogEntry logEntry) {
        if (logEntry.isIncoming()) {
            return "IN";
        } else {
            return "OUT";
        }
    }

    public String getStatus(Response response) {
        if (response.getStatus().isSuccess()) {
            return "RESP-OK";
        } else {
            return "RESP-FAILED";
        }
    }

    @Override
    public void log(LogEntry logEntry, Publish publish) {
        send(getId(),
                publish.getId(),
                "PUBLISH",
                getDirection(logEntry),
                publish.getNdo().getUri());
    }

    @Override
    public void log(LogEntry logEntry, PublishResponse publishResponse) {
        send(getId(),
                publishResponse.getId(),
                getStatus(publishResponse),
                getDirection(logEntry),
                "?");
    }

    @Override
    public void log(LogEntry logEntry, Get get) {
        send(getId(),
                get.getId(),
                "GET",
                getDirection(logEntry),
                get.getNdo().getUri());
    }

    @Override
    public void log(LogEntry logEntry, GetResponse getResponse) {
        send(getId(),
                getResponse.getId(),
                getStatus(getResponse),
                getDirection(logEntry),
                "?");
    }

    @Override
    public void log(LogEntry logEntry, Search search) {
        send(getId(),
                search.getId(),
                "SEARCH",
                getDirection(logEntry),
                search.getTokens().toString().replace(" ", ""));
    }

    @Override
    public void log(LogEntry logEntry, SearchResponse searchResponse) {
        send(getId(),
                searchResponse.getId(),
                getStatus(searchResponse),
                getDirection(logEntry),
                "?");
    }

}
