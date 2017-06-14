package com.example.wesley.stressmeasure;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScreenMeasure extends Service {
    private static final String TAG = "ScreenMeasure";

    private boolean isRunning = false;
    int time = 0;
    NotificationManager mNotificationManager;
    private CountDownTimer timer;
    static public int minutes;
    private Context context;

    //session fields
    private int unlockedSeconds = 0;
    private int unlockedAmount = 0;
    private int sessionPaused = 0;
    private boolean sessionStarted = false;


    static public int getMinutes(){
        return minutes;
    }

    @Override
    public void onCreate() {
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Log.i(TAG, "Service onCreate");
        if(isRunning){
            return;
        }

        context = this.getApplicationContext();

        isRunning = true;
        minutes = 60 * 5;

        final Handler mhandler = new Handler();
        mhandler.postDelayed(new Runnable(){
            public void run(){
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                boolean isScreenOn = pm.isScreenOn();
                //Log.d("wesley_d", "" + isScreenOn);
                if(isScreenOn){
                    //Log.d("wesley_d", "Secondes over: " + minutes);
                    //Log.d("wesley_d", "Minuten over: " + (int) Math.ceil(minutes/60));
                }
                if(!isScreenOn){
                    minutes--;
                }
                if(MainActivity.isAirplaneModeOn(context) && !sessionStarted){
                    sessionStarted = true;
                }
                if(!sessionStarted){
                    Log.d("Wesley_d", "sending signal");
                    updateSession(context, ""+MainActivity.code.getText());
                }

                if(!MainActivity.isAirplaneModeOn(context) && sessionStarted){
                    //post call id, user disabled airplane mode
                    updateSession(context, ""+MainActivity.code.getText());
                    createNotification("application closing in " + (30 - sessionPaused));
                    sessionPaused++;
                }
                else{
                    sessionPaused = 0;
                }

                if(sessionPaused == 30){
                    endSession(context, ""+MainActivity.code.getText());
                }
                mhandler.postDelayed(this, 1000);
            }
        }, 1000);

    }
    void endSession(Context context, String code) {
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(context);
        String url = "http://192.168.1.106:8000/checkConnectionCode?end=true&code=" + code;

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("wesley_d", response + MainActivity.code);

                        if(response.equals("false")){
                            //connection doesn't exist

                        }
                        if(response.equals("true")){
                            createNotification("Your session has stopped.");
                            System.exit(0);
                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("wesley_d", ""+error);
            }
        });
// Add the request to the RequestQueue.
        queue.add(stringRequest);
    }


    void updateSession(Context context, String code) {
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(context);
        String url = "http://192.168.1.106:8000/checkConnectionCode?end=false&code=" + code;

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("wesley_d", response + MainActivity.code);

                        if(response.equals("false")){
                            //connection doesn't exist

                        }
                        if(response.equals("true")){

                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("wesley_d", ""+error);
            }
        });
// Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    private void deleteNotification() {
        mNotificationManager.cancel(142543532);
    }


    private void createNotification(String message) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.notification_icon)
                        .setContentTitle("Stress Measure Update")
                        .setContentText(message);
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, MainActivity.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        // mId allows you to update the notification later on.
        int mId = 142543532;
        mNotificationManager.notify(mId, mBuilder.build());
    }


    @Override
    public IBinder onBind(Intent arg0) {
        Log.i(TAG, "Service onBind");
        return null;
    }

    @Override
    public void onDestroy() {

        isRunning = false;

        Log.i(TAG, "Service onDestroy");
    }
}
