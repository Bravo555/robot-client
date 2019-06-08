package com.marcel.robotclient;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends Activity {

    private String host;
    private int port;
    private Switch[] switches;
    private SeekBar[] servoSliders;
    private DatagramSocket socket;
    private TextView logArea;

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
        logArea = findViewById(R.id.log);

        new CheckConnectivity().execute();

        switches = new Switch[] {
                findViewById(R.id.switch1),
                findViewById(R.id.switch2),
                findViewById(R.id.switch3)
        };

        for(int i = 0; i < 3; i++) {
            final int it = i;
            switches[i].setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    final Pair<Integer, Integer> params = Pair.create(it, isChecked ? 1 : 0);
                    new SendPacketTask().execute(params);
                }
            });
        }

        servoSliders = new SeekBar[] {
                findViewById(R.id.servo0),
                findViewById(R.id.servo1),
                findViewById(R.id.servo2),
                findViewById(R.id.servo3),
                findViewById(R.id.servo4),
                findViewById(R.id.servo5),
                findViewById(R.id.servo6),
                findViewById(R.id.servo7)
        };
        for(int i = 0; i < 8; i++) {
            final int it = i;
            servoSliders[i].setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    new SendPacketTask().execute(Pair.create(0xff - it, progress));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
        }
    }

    private void log(String text) {
        logArea.append(text + '\n');
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

    class SendPacketTask extends AsyncTask<Pair<Integer, Integer>, Void, Void> {
        protected Void doInBackground(Pair<Integer, Integer>... requests) {
            byte[] payload = {
                    requests[0].first.byteValue(),
                    requests[0].second.byteValue()
            };

            try {
                InetAddress addr = InetAddress.getByName(host);

                if(!addr.isReachable(2000)) {
                    log("Host " + host + " unreachable!");
                } else {
                    log("connected!");
                }

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
        socket.disconnect();
        socket.close();
    }
}
