package com.example.appblethermalprinterjava;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private boolean btPermission = false;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket socket;
    private BluetoothDevice bluetoothDevice;
    private OutputStream outputStream;
    private InputStream inputStream;
    private Thread workerThread;
    private byte[] readBuffer;
    private int readBufferPosition = 0;

    private volatile boolean stopWorker = false;
    private String value = "";
    private ConnectionClass connectionClass = new ConnectionClass();
    private BluetoothDevice selectedDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void scanBt(View view) {
        checkPermission();
    }

    public void print(View view) {
        if (btPermission) {
            print_inv();
        } else {
            checkPermission();
        }
    }

    private void checkPermission() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null) {
            // Bluetooth не поддерживается на устройстве
        } else {
            if (bluetoothAdapter.isEnabled()) {
                // Bluetooth включен, продолжайте с вашей логикой
                // например, вызов btScan()
                btScan();
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                btActivityResultLauncher.launch(enableBtIntent);
            }
        }
    }

    private volatile ActivityResultLauncher<String> bluetoothPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean isGranted) {
                    if (isGranted) {
                        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
                        btPermission = true;

                        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
                            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            btActivityResultLauncher.launch(enableBtIntent);
                        }
                    } else {
                        // Действия при отказе в получении разрешения Bluetooth
                        btScan();
                    }
                }
            }
    );

    private ActivityResultLauncher<Intent> btActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    btScan();
                }
            }
    );

    @SuppressLint("MissingPermission")
    private void btScan() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            // Bluetooth выключен или отсутствует
            // Вы можете добавить обработку включения Bluetooth здесь
            return;
        }

        Intent discoverDevicesIntent = new Intent(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        sendBroadcast(discoverDevicesIntent);

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String deviceName = device.getName();
                    String deviceAddress = device.getAddress();
                    Log.d("Bluetooth", "Device Found: " + deviceName + " (" + deviceAddress + ")");
                    // Отобразите найденные устройства в списке
                    displayDevice(device);
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    unregisterReceiver(this);
                }
            }
        };

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver, filter);

        bluetoothAdapter.startDiscovery();
    }

    private void displayDevice(final BluetoothDevice device) {
        // Отобразите найденные устройства в списке
        ListAdapter adapter = new ArrayAdapter<>(this, R.layout.item_list, R.id.item_name,
                new String[]{device.getName()});
        ListView listView = findViewById(R.id.bt_lst);
        listView.setAdapter(adapter);

        // Добавьте листенер для элементов списка
        listView.setOnItemClickListener((parent, view, position, id) -> {
            selectedDevice = device;
            EditText deviceNameEditText = findViewById(R.id.deviceName);
            deviceNameEditText.setText(device.getName());
        });
    }

    private void beginListenForData() {
        try {
            stopWorker = false;
            readBufferPosition = 0;
            byte[] readBuffer = new byte[1024];

            workerThread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                    try {
                        int bytesAvailable = inputStream.available();
                        if (bytesAvailable > 0) {
                            // Ваш код для обработки доступных байтов
                        }
                    } catch (IOException e) {
                        stopWorker = true;
                    }
                }
            });

            workerThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initPrinter() {
        try {
            String prname = connectionClass.getPrinterName();
            BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
            BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                btActivityResultLauncher.launch(enableBluetooth);
            }

            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            if (pairedDevices != null && pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    if (device.getName().equals(prname)) {
                        bluetoothDevice = device;
                        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
                        Method m = bluetoothDevice.getClass().getMethod("createRfcommSocket", int.class);
                        socket = (BluetoothSocket) m.invoke(bluetoothDevice, 1);
                        bluetoothAdapter.cancelDiscovery();
                        socket.connect();
                        outputStream = socket.getOutputStream();
                        inputStream = socket.getInputStream();
                        beginListenForData();
                        break;
                    }
                }
            } else {
                value = "No Devices found";
                Toast.makeText(this, value, Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Bluetooth Printer Not Connected", Toast.LENGTH_LONG).show();
            socket = null;
        }
    }

    private void print_inv() {
        try {
            String invhdr = "Tax Invoice";
            StringBuilder textData = new StringBuilder();

            if (!invhdr.isEmpty()) {
                textData.append(invhdr);
            }
            intentPrint(String.valueOf(textData));

        } catch (Exception e) {
            Toast.makeText(this, "Error printing invoice", Toast.LENGTH_LONG).show();
        }
    }

    private void intentPrint(String txtValue) {
        if (!connectionClass.getPrinterName().trim().isEmpty()) {
            initPrintProcess();
        }
    }

    private void initPrintProcess() {
        try {
            initPrinter();

            byte[] printHeader = new byte[]{(byte) 0xAA, 0, 0, 0};

            if (printHeader.length > 128) {
                value += "\nValue is more than 128 size\n";
                Toast.makeText(this, "Error printing invoice", Toast.LENGTH_LONG).show();
            } else {
                outputStream.write("Text TEST Print".getBytes());
                outputStream.flush();
                outputStream.close();
                socket.close();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error printing invoice", Toast.LENGTH_LONG).show();
        }
    }
}
