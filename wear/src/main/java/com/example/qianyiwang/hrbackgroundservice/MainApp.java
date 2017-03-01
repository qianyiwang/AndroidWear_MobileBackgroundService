package com.example.qianyiwang.hrbackgroundservice;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

public class MainApp extends WearableListenerService {

    public static String START_ACTIVITY_PATH = "/from-phone";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equalsIgnoreCase(START_ACTIVITY_PATH)){
            String msg_watch = new String(messageEvent.getData());
            Log.e("MainApp","CMD from a phone:"+msg_watch);
            if(msg_watch.equals("start")){
                startService(new Intent(getBaseContext(), HeartRateService.class));
            }
            else if(msg_watch.equals("stop")){
                stopService(new Intent(getBaseContext(), HeartRateService.class));
            }
        }
    }

}
