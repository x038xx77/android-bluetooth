package com.example.appblethermalprinterjava;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private BluetoothController bluetoothController;

    private final Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            showToast((String) msg.obj);
            return true;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothController = new BluetoothController(handler);
        Button onOffButton = findViewById(R.id.onOffButton);
        Button searchButton = findViewById(R.id.searchButton);
        Button connectButton = findViewById(R.id.connectButton);
        Button startButton = findViewById(R.id.startButton);

        onOffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bluetoothController.isBluetoothEnabled()) {
                    // Запускаем BluetoothController при нажатии на кнопку поиска
                    Intent bluetoothIntent = new Intent(MainActivity.this, BluetoothControllerActivity.class);
                    startActivity(bluetoothIntent);
                } else {
                    showToast("Bluetooth is not enabled");
                }
            }
        });

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bluetoothController.isBluetoothEnabled()) {
                    // Запускаем BluetoothDiscoveryActivity при нажатии на кнопку поиска
                    Intent discoveryIntent = new Intent(MainActivity.this, BluetoothDiscoveryActivity.class);
                    startActivity(discoveryIntent);
                } else {
                    showToast("Bluetooth is not enabled");
                }
            }
        });


        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Connect button clicked");
                // Дополнительный код для кнопки "Подключить"
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Start button clicked");
                // Дополнительный код для кнопки "Старт"
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}

