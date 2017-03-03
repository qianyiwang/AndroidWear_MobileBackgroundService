package com.example.qianyiwang.hrbackgroundservice;

import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.IOException;
import java.io.PrintStream;
import java.net.DatagramPacket;

/**
 * Created by qianyiwang on 2/7/17.
 */

public class WatchListener extends WearableListenerService {
    public static String START_ACTIVITY_PATH = "/from-watch";
    public static final String BROADCAST_ACTION = "message_from_watch";
    Intent broadCastIntent;
    DatagramPacket packet;

    @Override
    public void onCreate() {
        super.onCreate();
        broadCastIntent = new Intent(BROADCAST_ACTION);
        Toast.makeText(this, "WatchListener Bind", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "WatchListener Unbind", Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equalsIgnoreCase(START_ACTIVITY_PATH)){
            String msg_watch = new String(messageEvent.getData());
            if(msg_watch.contains("hr:")){

                String hr = msgParsing(msg_watch);
                sendUdp(hr);
                broadCastIntent.putExtra("msg_watch", msg_watch);
                sendBroadcast(broadCastIntent);
            }
            else if (msg_watch.contains("SYNC RC_")){
                sendUdp(msg_watch);
            }

            Log.e("WatchListener",msg_watch);
        }
    }

    // **********send UDP socket**********************
    private void sendUdp(String msg){
        if(GlobalValues.udp_socket!=null){
            packet = new DatagramPacket( msg.getBytes(), msg.getBytes().length, GlobalValues.udpAddress, GlobalValues.udpPort );
            try {
                GlobalValues.udp_socket.send(packet);
                Log.e("UDP send", msg);
            } catch (IOException e) {
                Log.e("UDP error", "network not reachable");
            }
        }
    }
    // parse hr value
    private String msgParsing(String msg){
        int pos = msg.indexOf(':');
        return msg.substring(pos+1);
    }
}
