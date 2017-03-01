package com.example.qianyiwang.hrbackgroundservice;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class HeartRateService extends Service implements SensorEventListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    Sensor mHeartRateSensor;
    SensorManager mSensorManager;
    Timer timer;
    TimerTask timerTask;
    int hrVal;

    private GoogleApiClient googleApiClient;
    public static String START_ACTIVITY_PATH = "/from-watch";
    private Node mNode = null;

    public HeartRateService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // start heart rate sensor
        mSensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));
        mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        if (mHeartRateSensor == null) {
            List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
            for (Sensor sensor1 : sensors) {
                Log.i("Sensor Type", sensor1.getName() + ": " + sensor1.getType());
            }
        }
        mSensorManager.registerListener(this, mHeartRateSensor, SensorManager.SENSOR_DELAY_FASTEST);//define frequency
        Toast.makeText(this, "HR Service Started", Toast.LENGTH_SHORT).show();
        startTimer();
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
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
    }

    public void disconnect() {
        if (null != googleApiClient && googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSensorManager.unregisterListener(this);
        stopTimerTask();
        disconnect();
        Toast.makeText(this, "HR Service Stopped", Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
            hrVal = (int) event.values[0];
            Log.e("Sensor:", hrVal+"");

        } else
            Log.d("Sensor:", "Unknown sensor type");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void startTimer() {
        //set a new Timer
        timer = new Timer();
        //initialize the TimerTask's job
        initializeTimerTask();
        //schedule the timer, after the first 0ms the TimerTask will run every 5000ms
        timer.schedule(timerTask, 0, 1000); //
    }

    public void stopTimerTask() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {
                // send hr
                SendMessageToPhone sendMessageToPhone = new SendMessageToPhone();
                sendMessageToPhone.execute("hr:"+hrVal);
            }
        };
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

    public class SendMessageToPhone extends AsyncTask<String, Void, Void>{

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
}
