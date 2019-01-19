package com.marcel.robotclient;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;

public class MainActivity extends Activity {

    private EditText ipInput;
    private EditText portInput;
    private Switch[] switches;
    private DatagramSocket socket;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ipInput = findViewById(R.id.address);
        portInput = findViewById(R.id.port);

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
    }
    class SendPacketTask extends AsyncTask<Pair<Integer, Integer>, Void, Void> {
        protected Void doInBackground(Pair<Integer, Integer>... requests) {
            byte[] payload = {
                    requests[0].first.byteValue(),
                    requests[0].second.byteValue()
            };

            try {
                InetAddress addr = InetAddress.getByName(ipInput.getText().toString());
                int port = Integer.parseInt(portInput.getText().toString());
                DatagramPacket sendPacket = new DatagramPacket(payload, 0, payload.length, addr, port);
                if (socket != null) {
                    socket.disconnect();
                    socket.close();
                }
                socket = new DatagramSocket(port);
                socket.send(sendPacket);
            } catch (UnknownHostException e) {
                Log.e("MainActivity sendPacket", "getByName failed");
            } catch (IOException e) {
                Log.e("MainActivity sendPacket", "send failed");
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
