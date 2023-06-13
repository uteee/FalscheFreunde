package com.example.falschefreunde;

import android.app.ActionBar;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * Activity for the functionality of activity_menu
 */
public class MenuActivity extends AppCompatActivity {

    /**
     * Create instances of SwitchCompat
     */
    SwitchCompat soundtoggle;

    /**
     * Create an instance of MediaPlayer
     */
    MediaPlayer mediaPlayer;

    /**
     * Create an instance of DatabankAcess
     */
    DatabaseAccess dbAccess;

    /**
     * Create an instance of Cursor
     */
    Cursor cursor;

    /**
     * Create an instance of ListView
     */
    ListView list;

    /**
     * Create an instance of SimpleCursorAdapter
     */
    SimpleCursorAdapter adapter;

    private static final String sub_topic = "sensor/data";
    private static final String pub_topic = "sensehat/message";
    private int qos = 0; // MQTT quality of service
    private String data;
    private String clientId;
    private MemoryPersistence persistence = new MemoryPersistence();
    private MqttClient client;
    private String TAG = MenuActivity.class.getSimpleName();
    private String BROKER = "tcp://192.168.137.2:1883";

    /**
     * Called when MenuActivity is first created.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);


        mediaPlayer = MediaPlayer.create(this, R.raw.musik);
        soundtoggle = findViewById(R.id.soundtoggle);
        soundtoggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            /**
             * onCheckedChanged checks if the switch in the menu got changed. Depending on it
             * the music for the app starts or stops
             *
             * @param compoundButton    button that is used
             * @param isChecked         value is switch is checked or nor
             */
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                //if the switch is checked, the music starts
                if (isChecked)
                {
                    /**
                     * From the book
                     * Android Der schnelle und einfache Einstieg in die Programmierung und Entwicklungsumgebung Auflage: 2. Auflage
                     * Chapter 14 S. 288/289
                     * The example got modified
                     */
                    AssetFileDescriptor fd = getResources().openRawResourceFd(R.raw.musik);
                    mediaPlayer.reset();

                    try {
                        mediaPlayer.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
                        mediaPlayer.prepare();
                        mediaPlayer.start();
                        /**
                         * When the track is over, is plays again
                         */
                        mediaPlayer.setLooping(true);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                /**
                 * If the switch is not checked, the music stops to play
                 */
                else
                {
                    mediaPlayer.stop();
                }
            }
        });
    }

    /**
     * startGame changes to GameActivity and starts the game, when the button from
     * activity_menu gets clicked
     *
     * @param view
     */
    public void startGame(View view){
        Intent intent = new Intent(this, GameActivity.class);
        startActivity(intent);
    }

    /**
     * showLeaderboard changes to LeaderboardActivity with its view activity_leaderboard, when
     * the button from activity_menu gets clicked
     *
     * @param view
     */
    public void showLeaderboard(View view){
        Intent intent = new Intent (this, LeaderboardActivity.class);
        startActivity(intent);
    }

    /**
     * Connect with MQTT
     * @param view
     */
    public void connectMQTT(View view){
        EditText iptext = findViewById(R.id.mqttIPtext);
        String ip = iptext.getText().toString();
        connect(ip);
    }

    /**
     * Disconnect with MQTT
     * @param view
     */
    public void disconnectMQTT(View view){
        disconnect();
    }

    /**
     * When this activity is finished and gets destroyed,
     */
    @Override
    protected void onDestroy(){
        if(cursor != null && !cursor.isClosed()){
            cursor.close();
        }
        if(dbAccess != null){
            dbAccess.close();
        }
        super.onDestroy();
    }

    @Override
    protected void onResume(){
        super.onResume();

        //connect(broker);
        //subscribe(sub_topic);
    }

    @Override
    protected void onPause(){
        super.onPause();

        //disconnect();
    }

    /**
     * Connect to broker and
     * @param broker Broker to connect to
     */
    private void connect (String broker) {
        try {
            clientId = MqttClient.generateClientId();
            client = new MqttClient(broker, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            Log.d(TAG, "Connecting to broker: " + broker);
            client.connect(connOpts);
            Log.d(TAG, "Connected with broker: " + broker);
        } catch (MqttException me) {
            Log.e(TAG, "Reason: " + me.getReasonCode());
            Log.e(TAG, "Message: " + me.getMessage());
            Log.e(TAG, "localizedMsg: " + me.getLocalizedMessage());
            Log.e(TAG, "cause: " + me.getCause());
            Log.e(TAG, "exception: " + me);
        }
    }

    /**
     * Subscribes to a given topic
     * @param topic Topic to subscribe to
     */
    private void subscribe(String topic) {
        try {
            client.subscribe(topic, qos, new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage msg) throws Exception {
                    String message = new String(msg.getPayload());
                    Log.d(TAG, "Message with topic " + topic + " arrived: " + message);
                }
            });
            Log.d(TAG, "subscribed to topic " + topic);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * Publishes a message via MQTT (with fixed topic)
     * @param topic topic to publish with
     * @param msg message to publish with publish topic
     */
    private void publish(String topic, String msg) {
        MqttMessage message = new MqttMessage(msg.getBytes());
        message.setQos(qos);
        try {
            client.publish(topic, message);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * Unsubscribe from default topic (please unsubscribe from further
     * topics prior to calling this function)
     */
    private void disconnect() {
        try {
            client.unsubscribe(sub_topic);
        } catch (MqttException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }
        try {
            Log.d(TAG, "Disconnecting from broker");
            client.disconnect();
            Log.d(TAG, "Disconnected.");
        } catch (MqttException me) {
            Log.e(TAG, me.getMessage());
        }
    }

}
