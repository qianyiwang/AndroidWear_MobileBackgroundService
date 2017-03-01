package com.example.qianyiwang.hrbackgroundservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Date;

public class MainApp extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, View.OnClickListener{

    public static String START_ACTIVITY_PATH = "/from-phone";
    private Node mNode = null;
    private GoogleApiClient googleApiClient;
    Button startBt, stopBt, terminateBt;
    ConnectUdp connectUdp;
    BroadcastReceiver broadcastReceiver;
    TextView hr_text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_app);

        startBt = (Button)findViewById(R.id.startBt);
        startBt.setOnClickListener(this);
        stopBt = (Button)findViewById(R.id.stopBt);
        stopBt.setOnClickListener(this);
//        terminateBt = (Button)findViewById(R.id.terminateBt) ;
//        terminateBt.setOnClickListener(this);
        hr_text = (TextView)findViewById(R.id.hr_text);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        googleApiClient.connect();
        // get connected nodes
        Wearable.NodeApi.getConnectedNodes(googleApiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(@NonNull NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                for(Node node: getConnectedNodesResult.getNodes()){
                    mNode = node;
                    Log.v("Node", String.valueOf(mNode));
                }
            }
        });
        connectUdp = new ConnectUdp();
        connectUdp.execute();

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                receiveBroadcast(intent);
            }
        };
        registerReceiver(broadcastReceiver, new IntentFilter(WatchListener.BROADCAST_ACTION));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }

    private void receiveBroadcast(Intent intent) {
        String msg_watch = intent.getStringExtra("msg_watch");
        hr_text.setText(msg_watch);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d("GoogleApi", "onConnected "+ bundle);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("GoogleApi", "onConnectionSuspended:" + i);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d("GoogleApi", "onConnectionFailed:" + connectionResult);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.startBt:
                SendMessageToWatch sendMessageToWatch1 = new SendMessageToWatch();
                sendMessageToWatch1.execute("start");
                break;
            case R.id.stopBt:
                SendMessageToWatch sendMessageToWatch2 = new SendMessageToWatch();
                sendMessageToWatch2.execute("stop");
                break;
//            case R.id.terminateBt:
//                SendMessageToWatch sendMessageToWatch3 = new SendMessageToWatch();
//                sendMessageToWatch3.execute("terminate");
//                break;
        }
    }

    public class SendMessageToWatch extends AsyncTask<String, Void, Void>{

        @Override
        protected Void doInBackground(String... strings) {
            if(mNode!=null){
                for(String s: strings){
                    Wearable.MessageApi.sendMessage(googleApiClient, mNode.getId(), START_ACTIVITY_PATH, s.getBytes()).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                        if(!sendMessageResult.getStatus().isSuccess()){
                            Log.e("GoogleApi","Failed to send message with status code: "
                                    + sendMessageResult.getStatus().getStatusCode());
                        }
                        else{
                            Log.e("GoogleApi","success");
                        }
                        }
                    });
                }
            }
            return null;
        }
    }
    //************* connect UDP socket in AsyncTask*************
    public class ConnectUdp extends AsyncTask<Void, Void, Void>{

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                GlobalValues.udpAddress = InetAddress.getByName(GlobalValues.udp_address);
                GlobalValues.udp_socket = new DatagramSocket();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
