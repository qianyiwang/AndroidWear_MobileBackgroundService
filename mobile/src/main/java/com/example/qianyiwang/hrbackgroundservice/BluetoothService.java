package com.example.qianyiwang.hrbackgroundservice;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.Html;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.util.UUID;

public class BluetoothService extends Service {

    //Objects for Service **************************************************************************
    private final IBinder myBinder = new MyLocalBinder();
    private int NOTIFICATION_ID = 102;
    boolean adas_demo_on;

    //Objects for Bluetooth ************************************************************************
    long BSthisTime = 3500;
    long BSlastTime = 0;
    long HWthisTime = 3500;
    long HWlastTime = 0;
    long LKthisTime = 3500;
    long LKlastTime = 0;
    long SPthisTime = 3500;
    long SPlastTime = 0;
    long SCthisTime = 3500;
    long SClastTime = 0;

    long[] bsVibrate = {100, 100, 96, 96, 92, 92, 88, 88, 84, 84, 80, 80, 76, 76, 72, 72, 68, 68, 64, 64, 60, 60, 56, 56};
    // long[] spVibrate = {50,50,50,50,50,50,50,50};
    long[] spVibrate = {100, 200, 100, 200, 100, 200, 100, 200, 750, 200, 100, 200, 100, 200, 100, 200, 750, 200, 100, 200, 100, 200, 100, 200, 100, 0,0};
    long[] hwVibrate = {50,50,50,50,50,50,50,50,50,50,50,50,50,50,50,50};
    // long[] lkVibrate = {50,50,50,50,50,50,50,50};
    long[] lkVibrate = {55, 45, 55, 45, 55, 45, 55, 45, 55, 45, 55, 45, 55, 45, 55, 45, 55, 45, 55, 45, 55, 45, 55, 45, 55, 45, 55, 45, 55, 45, 55, 45, 55, 45, 55, 45, 55, 45, 55, 45, 55, 45, 55, 45};
    long[] scVibrate = {50,50,50,50,50,50,50,50};
    long[] tnVibrate = {50,50,50,50,50,50,50,50,50,50,50,50,50,50,50,50};


    BluetoothAdapter btAdapter = null;
    BroadcastReceiver deviceReceiver;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;

    // SPP UUID service - this should work for most devices
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    protected final int SUCCESS_CONNECT = 0;
    protected final int MESSAGE_READ = 1;
    protected final int FAIL_CONNECT = 2;
    DatagramPacket packet;

    //**************************** HANDLER *********************************************************
    android.os.Handler mHandler = new android.os.Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case SUCCESS_CONNECT:
                    //Do Something when connect occurs
                    Log.d("Steve", "Handler - SUCCESS_CONNECT");
                    Toast.makeText(getApplicationContext(), "Device CONNECTED", Toast.LENGTH_SHORT).show();
                    String s = "Successfully Connected";


                    break;

                case FAIL_CONNECT:
                    Toast.makeText(getApplicationContext(), "Device FAILED to Connect! RUN SIMULATOR FIRST", Toast.LENGTH_SHORT).show();
                    break;

                case MESSAGE_READ:
                    //Log.d("Steve", "Message Received");
                    byte[] readBuf = (byte[]) msg.obj;
                    long[] vibrate = {50, 100, 50, 100, 50, 100, 50, 100};
                    String title = "ADAS Alert";
                    String alertText = new String(readBuf);
                    //terminal.append(alertText);

                    if (alertText.contains("LaneKeeping")) {

                        LKthisTime = System.currentTimeMillis();

                        if (LKthisTime - LKlastTime > 2000) {
                            String LKstring = alertText.replaceAll("[^.0123456789]", "");
                            LKlastTime = System.currentTimeMillis();
//                            postNotifications(getApplicationContext(), lkVibrate, R.mipmap.ic_launcher, "Lane Keeping Alert", "", adas_demo_on);
                            SendUDP sendUDP = new SendUDP("ADAS_LaneKeeping");
                            sendUDP.execute();
                        }
                    }

                    if (alertText.contains("Headway")) {

                        HWthisTime = System.currentTimeMillis();

                        if (HWthisTime - HWlastTime > 1000) {
                            String HWstring = alertText.replaceAll("[^.0123456789]", "");
                            HWlastTime = System.currentTimeMillis();
//                            postNotifications(getApplicationContext(), hwVibrate, R.mipmap.ic_launcher, "Headway Alert", "", adas_demo_on);
                            SendUDP sendUDP = new SendUDP("ADAS_Headway");
                            sendUDP.execute();
                        }
                    }

                    if (alertText.contains("BlindSpot")) {

                        BSthisTime = System.currentTimeMillis();

                        if (BSthisTime - BSlastTime > 1000) {
                            String BSstring = alertText.replaceAll("[^.0123456789]", "");
                            BSlastTime = System.currentTimeMillis();
//                            postNotifications(getApplicationContext(), bsVibrate, R.mipmap.ic_launcher, "Blind Spot Warning", "", adas_demo_on);
                            SendUDP sendUDP = new SendUDP("ADAS_BlindSpot");
                            sendUDP.execute();
                        }
                    }

                    if (alertText.contains("Speed")) {

                        SPthisTime = System.currentTimeMillis();

                        if (SPthisTime - SPlastTime > 60000) {
                            String SPstring = alertText.replaceAll("[^.0123456789]", "");
                            SPlastTime = System.currentTimeMillis();
//                            postNotifications(getApplicationContext(), spVibrate, R.mipmap.ic_launcher, "Speed Warning", "", adas_demo_on);
                            SendUDP sendUDP = new SendUDP("ADAS_Speeding");
                            sendUDP.execute();
                        }
                    }

                    if (alertText.contains("SharpCurve")) {

                        SCthisTime = System.currentTimeMillis();

                        if (SCthisTime - SClastTime > 3000) {
                            String SCstring = alertText.replaceAll("[^.0123456789]", "");
                            SClastTime = System.currentTimeMillis();
//                            postNotifications(getApplicationContext(), scVibrate, R.mipmap.ic_launcher, "Sharp Curve Ahead", "", adas_demo_on);
                        }
                    }

                    break;
            }
        }
    };
    //************************************ END HANDLER *********************************************


    @Override
    public IBinder onBind(Intent intent) {
        return myBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("BT SERVICE", "SERVICE CREATED");

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("BT SERVICE", "SERVICE STARTED");
        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        discoverDevice();
        deviceReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                receiveBluetoothDevice(intent);
            }
        };
        registerReceiver(deviceReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        Toast.makeText(getBaseContext(), "Start ADAS service", 0).show();
        return super.onStartCommand(intent, flags, startId);
    }


    public class MyLocalBinder extends Binder {
        BluetoothService getService() {
            //Return instance of Bluetooth Service so that MainActivity can access public mehtods
            return BluetoothService.this;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
        if (connectedThread != null) {
            connectedThread.cancel();
        }
        if (connectThread != null) {
            connectThread.cancel();
        }
        Log.e("SERVICE", "onDestroy");

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Log.e("Service", "Cancelling notification");
        notificationManager.cancel(NOTIFICATION_ID);
        unregisterReceiver(deviceReceiver);
        Toast.makeText(getBaseContext(), "Stop ADAS service", 0).show();
    }


    //**********************************************************************************************
    //*************************** BLUETOOTH METHODS ************************************************
    //**********************************************************************************************

    private void discoverDevice() {
        if(btAdapter.isDiscovering()){
            btAdapter.cancelDiscovery();
        }
        btAdapter.startDiscovery();
    }

    private void receiveBluetoothDevice(Intent intent) {
        // When discovery finds a device
        String action = intent.getAction();
        Log.e("TEST", "TEST");
        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            // Get the BluetoothDevice object from the Intent
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            Log.v("Bluetooth Device", device.getName()+device.getAddress());

            if(device.getAddress().equals(GlobalValues.DEVICE_ADDRESS))
            {
                Toast.makeText(getApplicationContext(), "Device found!!!", Toast.LENGTH_SHORT).show();
                btAdapter.cancelDiscovery();
                connectDevice(device);
            }
        }
    }

    public void connectDevice(BluetoothDevice device) {
        connectThread = new ConnectThread(device);
        connectThread.start();
    }

    public void disconnectAll() {
        mHandler.removeCallbacksAndMessages(null);
        if (connectedThread != null) {
            connectedThread.cancel();
        }
        if (connectThread != null) {
            connectThread.cancel();
        }
    }


    //********************************* CONNECT THREAD *********************************************
    public class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(BTMODULEUUID);
            } catch (IOException e) {
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            btAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
                Log.v("SOCKET","SOCKET GOOD");
//                editor = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
//                editor.putInt("bluetooth_status", 1);
                // Do work to manage the connection (in a separate thread)
                mHandler.obtainMessage(SUCCESS_CONNECT, mmSocket).sendToTarget();
                connectedThread = new ConnectedThread(mmSocket);
                connectedThread.run();

            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
//                Toast.makeText(getApplicationContext(), "Device Failed to Connect", Toast.LENGTH_SHORT).show();
                Log.d("Steve", String.valueOf(connectException));
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                }
                // Do work to manage the connection (in a separate thread)
                mHandler.obtainMessage(FAIL_CONNECT, mmSocket).sendToTarget();
                return;
            }

//            // Do work to manage the connection (in a separate thread)
//            mHandler.obtainMessage(SUCCESS_CONNECT, mmSocket).sendToTarget();
//            connectedThread = new ConnectedThread(mmSocket);
//            connectedThread.run();
        }


        /**
         * Will cancel an in-progress connection, and close the socket
         */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }
    //********************************* END CONNECT THREAD *****************************************

    //******************************** CONNECTED THREAD ********************************************
    public class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {

            byte[] buffer;  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    //Log.d("Steve", "Receiving...");
                    buffer = new byte[256];
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();

                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }
    //***************************** END CONNECTED THREAD *******************************************


    //**********************************************************************************************
    //******************************** NOTIFICATION CODE *******************************************
    //**********************************************************************************************
    public static void postNotifications(Context context, long[] vibration, int image, String title, String text, boolean adas) {
        NotificationTest test = new NotificationTest(adas);
        Notification[] notifications = test.buildNotifications(context, vibration, image, title, text);
        for (int i = 0; i < notifications.length; i++) {
            Notification not = notifications[i];
            NotificationManagerCompat.from(context).notify(i, not);
        }
    }

    private static class NotificationTest {

        boolean adas_demo_on;

        public NotificationTest(boolean adas)
        {
            this.adas_demo_on = adas;
            Log.v("constructor", String.valueOf(adas_demo_on));
        }

        public Notification[] buildNotifications(Context context, long[] vibration, int image, String title, String text) {


            String colorStr = "<font color=\"red\"><b>"+title+"</b></font>";

            if(!adas_demo_on)
            {
                NotificationCompat.Builder summaryBuilder = new
                        NotificationCompat.Builder(context)
                        .setContentTitle(Html.fromHtml(colorStr))
                        .setContentText(text)
                        .setSmallIcon(image)
                        .setVibrate(vibration);
                return new Notification[]{summaryBuilder.build()};
            }
            else
            {
                // Summary
                NotificationCompat.Builder summaryBuilder = new
                        NotificationCompat.Builder(context)
                        .setGroup("Group 1")
                        .setGroupSummary(true)
                        .setContentTitle(Html.fromHtml(colorStr))
                        .setContentText(text)
                        .setSmallIcon(image)
                        .setVibrate(vibration)
                        .extend(new NotificationCompat.WearableExtender().setBackground(BitmapFactory.decodeResource(context.getResources(), image)));

                // Child 1
                NotificationCompat.Builder childBuilder1 = new
                        NotificationCompat.Builder(context)
                        .setContentTitle(Html.fromHtml(colorStr))
                        .setContentText(text)
                        .setSmallIcon(image)
                        .setLocalOnly(false)
                        .setGroup("Group 1")
                        .extend(new NotificationCompat.WearableExtender().setBackground(BitmapFactory.decodeResource(context.getResources(), image)));

                return new Notification[]{summaryBuilder.build(), childBuilder1.build()};
            }
        }
    }

    // **********send UDP socket**********************
    public class SendUDP extends AsyncTask<Void, Void, Void> {

        String msg;

        SendUDP(String msg){
            this.msg = msg;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if(GlobalValues.udp_socket!=null){
                packet = new DatagramPacket( msg.getBytes(), msg.getBytes().length, GlobalValues.udpAddress, GlobalValues.udpPort );
                try {
                    GlobalValues.udp_socket.send(packet);
                } catch (IOException e) {
                    Log.e("UDP error", "network not reachable");
                }
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
    }

}



