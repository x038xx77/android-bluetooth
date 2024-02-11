package com.example.appblethermalprinterjava;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class BluetoothControllerActivity extends AppCompatActivity {

    private BluetoothController bluetoothController;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_controller);

        bluetoothController = new BluetoothController(null);

        Button enableBluetoothButton = findViewById(R.id.enableBluetoothButton);
        Button disableBluetoothButton = findViewById(R.id.disableBluetoothButton);

        enableBluetoothButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothController.enableBluetooth(BluetoothControllerActivity.this, 1);
            }
        });

        disableBluetoothButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothController.disableBluetooth();
            }
        });
    }
}
