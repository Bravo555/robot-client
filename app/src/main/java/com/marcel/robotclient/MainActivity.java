package com.marcel.robotclient;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.widget.VideoView;

import com.jmedeisis.bugstick.Joystick;
import com.jmedeisis.bugstick.JoystickListener;

public class MainActivity extends Activity {

    private String host;
    private int port;
    private DatagramSocket socket;
    private VideoView videoView;

    private static final String TAG = "MainActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo info = wifiManager.getConnectionInfo();
        String ssid = info.getSSID();
        String desiredSsid = getString(R.string.ssid);
        Log.i(TAG, desiredSsid);

        if(!ssid.equals(desiredSsid) && !desiredSsid.equals("*")) {
            exitDialog("Wrong SSID", "The SSID of the WIFI network is wrong. Desired SSID: " + desiredSsid);
        }

        host = getString(R.string.host);
        port = Integer.parseInt(getString(R.string.port));

        //new CheckConnectivity().execute();

        videoView = findViewById(R.id.videoView);
        Uri videoUri = Uri.parse(getString(R.string.video_uri));
        videoView.setVideoURI(videoUri);
        videoView.start();

        Joystick left = findViewById(R.id.joystick_left);
        Joystick right = findViewById(R.id.joystick_right);

        left.setJoystickListener(new JoystickListener() {
            @Override
            public void onDown() {
                log("down");
            }

            @Override
            public void onDrag(float degrees, float offset) {
                log("deg: " + degrees + ", off: " + offset);
                float upValue = (float) Math.sin(Math.toRadians(degrees)) * offset;
                log("" + upValue);

                int intValue = normalisedFloatToByte(upValue);
                new UpdateServo().execute(Pair.create(0xff, intValue));
            }

            @Override
            public void onUp() {
                log("up");
            }
        });

        right.setJoystickListener(new JoystickListener() {
            @Override
            public void onDown() {
                log("down");
            }

            @Override
            public void onDrag(float degrees, float offset) {
                log("deg: " + degrees + ", off: " + offset);
                float upValue = (float) Math.sin(Math.toRadians(degrees)) * offset;
                log("" + upValue);

                int intValue = normalisedFloatToByte(upValue);
                new UpdateServo().execute(Pair.create(0xfe, intValue));
            }

            @Override
            public void onUp() {
                log("up");
            }
        });
    }

    private int normalisedFloatToByte(float value) {
        if(value < -1.0 || value > 1.0) {
            return 0;
        }
        return 127 + Math.round(value * 127);
    }

    private void log(String text) {
        Log.d(TAG, text);
    }

    private void exitDialog(String title, String text) {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle(title)
                .setMessage(text)
                .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                })
                .show();
    }

    class CheckConnectivity extends AsyncTask<Void, Void, Boolean> {
        protected Boolean doInBackground(Void ...params) {
            try {
                if(!InetAddress.getByName(host).isReachable(2000)) {
                    return false;
                }
            } catch (UnknownHostException e) {
                return false;
            } catch (IOException e) {
                return false;
            }
            return true;
        }

        protected void onPostExecute(Boolean result) {
            if(result == false) {
                exitDialog("Can't connect", "Can't connect with host " + host);
            }
        }
    }

    class UpdateServo extends AsyncTask<Pair<Integer, Integer>, Void, Void> {
        protected Void doInBackground(Pair<Integer, Integer>... requests) {
            byte[] payload = {
                    requests[0].first.byteValue(),
                    requests[0].second.byteValue()
            };

            try {
                InetAddress addr = InetAddress.getByName(host);

                DatagramPacket sendPacket = new DatagramPacket(payload, 0, payload.length, addr, port);
                if (socket != null) {
                    socket.disconnect();
                    socket.close();
                }
                socket = new DatagramSocket(port);
                socket.send(sendPacket);
            } catch (UnknownHostException e) {
                Log.e(TAG, "getByName failed");
            } catch (IOException e) {
                Log.e(TAG, "send failed");
            }
            return null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
