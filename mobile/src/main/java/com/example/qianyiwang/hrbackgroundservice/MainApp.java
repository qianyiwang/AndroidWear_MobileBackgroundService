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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MainApp extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, View.OnClickListener{

    public static String START_ACTIVITY_PATH = "/from-phone";
    private Node mNode = null;
    private GoogleApiClient googleApiClient;
    Button startBt, stopBt, adasBt, mapIssue, mapExe, addressIssue, addressExe, parkIssue, parkExe;
    ConnectUdp connectUdp;
    BroadcastReceiver broadcastReceiver;
    TextView hr_text;
    DatagramPacket packet;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_app);

        startBt = (Button)findViewById(R.id.startBt);
        startBt.setOnClickListener(this);
        stopBt = (Button)findViewById(R.id.stopBt);
        stopBt.setOnClickListener(this);
        adasBt = (Button)findViewById(R.id.bluetoothBt) ;
        adasBt.setOnClickListener(this);
        mapIssue = (Button)findViewById(R.id.mapCmdIssued);
        mapIssue.setOnClickListener(this);
        mapExe = (Button)findViewById(R.id.mapCmdExeCuted);
        mapExe.setOnClickListener(this);
        addressIssue = (Button) findViewById(R.id.addressCmdIssued);
        addressIssue.setOnClickListener(this);
        addressExe = (Button) findViewById(R.id.addressCmdExecuted);
        addressExe.setOnClickListener(this);
        parkIssue = (Button)findViewById(R.id.parkCmdIssued);
        parkIssue.setOnClickListener(this);
        parkExe = (Button)findViewById(R.id.parkCmdExecuted);
        parkExe.setOnClickListener(this);
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
        SendMessageToWatch sendMessageToWatch2 = new SendMessageToWatch();
        sendMessageToWatch2.execute("stop");
        stopService(new Intent(getBaseContext(), BluetoothService.class));
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
        SendUDP sendUDP = new SendUDP();
        switch (view.getId()){
            case R.id.startBt:
                SendMessageToWatch sendMessageToWatch1 = new SendMessageToWatch();
                sendMessageToWatch1.execute("start");

                break;
            case R.id.stopBt:
                SendMessageToWatch sendMessageToWatch2 = new SendMessageToWatch();
                sendMessageToWatch2.execute("stop");
                stopService(new Intent(getBaseContext(), BluetoothService.class));
                break;
            case R.id.bluetoothBt:
                startService(new Intent(getBaseContext(), BluetoothService.class));
                break;

            case R.id.mapCmdIssued:
                sendUDP.execute("Human_GoogleMapCmdIssued");
                break;
            case R.id.mapCmdExeCuted:
                sendUDP.execute("Human_GoogleMapCmdExecuted");
                break;
            case R.id.addressCmdIssued:
                sendUDP.execute("Human_EnterAddressIssued");
                break;
            case R.id.addressCmdExecuted:
                sendUDP.execute("Human_EnterAddressExecuted");
                break;
            case R.id.parkCmdIssued:
                sendUDP.execute("Human_parkopediaIssued");
                break;
            case R.id.parkCmdExecuted:
                sendUDP.execute("Human_parkopediaExecuted");
                break;
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
    // **********send UDP socket**********************
    public class SendUDP extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... strings) {
            if(GlobalValues.udp_socket!=null){
                for(String s: strings){
                    packet = new DatagramPacket( s.getBytes(), s.getBytes().length, GlobalValues.udpAddress, GlobalValues.udpPort );
                    try {
                        GlobalValues.udp_socket.send(packet);
                    } catch (IOException e) {
                        Log.e("UDP error", "network not reachable");
                    }
                }
            }
            return null;
        }
    }
}
