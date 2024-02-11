package com.example.appblethermalprinterjava;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BluetoothDiscoveryActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private ArrayAdapter<String> deviceListAdapter;
    private ArrayList<BluetoothDevice> discoveredDevices;
    private ProgressBar progressBar;

    // UUID для SPP (Serial Port Profile)
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Коллекция для хранения выбранных MAC-адресов
    private Set<String> selectedDevicesMacAddresses = new HashSet<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_discovery);

        // Инициализация кнопки "Печать"
        Button printTestButton = findViewById(R.id.printTestButton);

        // Обработчик нажатия для кнопки "Печать"
        printTestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Печать текста "Эта печать работает X5" на выбранных устройствах
                for (String macAddress : selectedDevicesMacAddresses) {
                    BluetoothDevice selectedDevice = bluetoothAdapter.getRemoteDevice(macAddress);
                    if (selectedDevice != null) {
                        // Запуск асинхронной задачи для печати текста на каждом выбранном устройстве
                        new BluetoothPrintTask(selectedDevice, "Этот принтер подключается и работает\n").execute();
                        // Выводим лог в консоль
                        Log.d("PrintButton", "Print button clicked for device: " + selectedDevice.getName());
                    }
                }
            }
        });

        // Инициализация Bluetooth
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            showToast("Bluetooth is not supported on this device");
            finish();
            return;
        }

        // Инициализация списка устройств и адаптера
        discoveredDevices = new ArrayList<>();
        deviceListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        ListView deviceListView = findViewById(R.id.deviceListView);
        deviceListView.setAdapter(deviceListAdapter);

        // Инициализация ProgressBar
        progressBar = findViewById(R.id.progressBar);

        // Регистрация BroadcastReceiver для обнаружения устройств
        registerReceiver(discoveryReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        registerReceiver(discoveryReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));

        // Обработчик выбора устройства из списка
        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice selectedDevice = discoveredDevices.get(position);
                String selectedDeviceMacAddress = selectedDevice.getAddress();

                if (selectedDevicesMacAddresses.contains(selectedDeviceMacAddress)) {
                    showToast("Device already selected: " + selectedDevice.getName());
                } else {
                    selectedDevicesMacAddresses.add(selectedDeviceMacAddress);
                    boolean bluetoothAvailable = getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);

                    if (bluetoothAvailable)
                        showToast("Selected device: " + selectedDevice.getName() + "\nMAC Address: " + selectedDeviceMacAddress);
                    else {
                        showToast("Bluetooth is not supported on this device");
                    }

                    // Печать текста на выбранном устройстве
                    printText(selectedDevice, "Hello, this is a test print!");
                }
            }
        });

        // Начало поиска устройств
        startDiscovery();
    }

    // Метод для печати текста на Bluetooth-принтере
    @SuppressLint("MissingPermission")
    private void printText(BluetoothDevice device, String text) {
        // Вызываем асинхронную задачу для печати текста
        new BluetoothPrintTask(device, text).execute();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelDiscovery();
        unregisterReceiver(discoveryReceiver);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("MissingPermission")
    private void startDiscovery() {
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();
        progressBar.setVisibility(View.VISIBLE);
    }

    @SuppressLint("MissingPermission")
    private void cancelDiscovery() {
        bluetoothAdapter.cancelDiscovery();
    }

    private final BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && device.getName() != null) {
                    discoveredDevices.add(device);
                    deviceListAdapter.add(device.getName() + "\n" + device.getAddress());
                    deviceListAdapter.notifyDataSetChanged();
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                showToast("Discovery finished");
                progressBar.setVisibility(View.GONE);
            }
        }
    };

    //=======================================================================
    private class BluetoothPrintTask extends AsyncTask<Void, Void, Boolean> {
        private BluetoothDevice device;
        private String text;

        BluetoothPrintTask(BluetoothDevice device, String text) {
            this.device = device;
            this.text = text;
        }

        // Внутри метода doInBackground класса BluetoothPrintTask
        @Override
        protected Boolean doInBackground(Void... params) {
            BluetoothSocket socket = null;
            OutputStream outputStream = null;
            InputStream inputStream = null;

            try {
                BluetoothDevice remoteDevice = device;

                // Устанавливаем соединение с устройством
                socket = remoteDevice.createRfcommSocketToServiceRecord(MY_UUID);

                // Подключение к устройству
                socket.connect();
                if (socket.isConnected()) {
                    // Получение потока для записи данных в сокет
                    outputStream = socket.getOutputStream();
                    // Отправка текста в виде байтов
                    outputStream.write(text.getBytes());

                    // Важно: После отправки данных, дайте немного времени на их передачу
                    Thread.sleep(5000);

                    // Прочитайте данные из сокета, если необходимо
                    inputStream = socket.getInputStream();
                    byte[] buffer = new byte[1024];
                    int bytesRead;

                    // Прочитайте данные, пока не будет достигнут конец файла (EOF)
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        // Обработка прочитанных данных, если нужно
                    }

                    // Добавим задержку перед закрытием сокета
                    Thread.sleep(2000);

                    return true;
                }
            } catch (IOException | InterruptedException e) {
                Log.e("BluetoothPrintTask", "Error during data transfer", e);
            } finally {
                try {
                    // Закрытие потока вывода и сокета
                    if (outputStream != null) {
                        outputStream.close();
                    }
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    if (socket != null && socket.isConnected()) {
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return false;
        }
        //================================================================

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                showToast("Text sent to the printer");
            } else {
                showToast("Failed to send text to the printer");
            }
        }
    }
}
