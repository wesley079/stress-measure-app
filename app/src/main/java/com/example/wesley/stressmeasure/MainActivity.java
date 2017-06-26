package com.example.wesley.stressmeasure;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import java.net.HttpURLConnection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {

    private Context context;
    private HttpURLConnection mHttpUrl;
    static ProgressDialog connectionDialog;
    static TextView errorMessage;
    static TextView code;
    static boolean connected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_code);
        context = this.getApplicationContext();

        //get fields
        errorMessage = (TextView)findViewById(R.id.errorTextView);


        Log.d("wesley_d", "airplanemode" + isAirplaneModeOn(this.getApplicationContext()));
    }

    public void findConnection(View v){
        connectionDialog = ProgressDialog.show(this, "Finding connection", "Please wait while loading");

        code = (TextView)findViewById(R.id.InputId);
        PostData(context, "" + code.getText());
    }

    /**
     * Open settings for the user to enable airplane mode
     * Start watching if airplane mode enabled
     */
    public void openSettings(View v){
        startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), 0);

        //start watching for changes
        Runnable screenCheck = new Runnable() {
            public void run() {

            }
        };

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(screenCheck, 0, 5, TimeUnit.SECONDS);
    }

    public void startService(){
        Intent intent = new Intent(this, ScreenMeasure.class);

        //manage how many minutes the phone should be locked
        intent.putExtra("SCREEN_LOCK_GOAL", 500);
        startService(intent);
    }

    /**
     * Gets the state of Airplane Mode.
     * @param context
     * @return true if enabled.
     */
    static boolean isAirplaneModeOn(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0) != 0;
    }

    @Override
    public void onStart() {
        super.onStart();

        if(isAirplaneModeOn(context) && connected){
            setContentView(R.layout.activity_running);
        }
        else{

            Toast toast = Toast.makeText(context, "Airplane mode is not enabled, please do so to start relaxing", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }


    private void sendNotification(String Title, String Message){
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_track_changes_black_24dp)
                        .setContentTitle(Title)
                        .setContentText(Message);
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
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
// mId allows you to update the notification later on.
        int mId = 142543532;
        mNotificationManager.notify(mId, mBuilder.build());
    }

    void PostData(Context context, String code) {
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(context);
        String url = "http://prototype3-devthis.wesleykroon.nl/checkConnectionCode?code=" + code;

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if(response.equals("false")){
                            //connection doesn't exist
                            connected = false;
                            errorMessage.setText("Wrong connection ID, please try again.");

                        }
                        if(response.equals("true")){
                            //connection exists
                            setContentView(R.layout.activity_main);
                            startService();
                            connected = true;
                        }

                        //close loading handler
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                connectionDialog.dismiss();
                            }
                        }, 2500);

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                errorMessage.setText("Something went wrong, do you have internet access?");
                connectionDialog.dismiss();
            }
        });
// Add the request to the RequestQueue.
        queue.add(stringRequest);
    }


}
