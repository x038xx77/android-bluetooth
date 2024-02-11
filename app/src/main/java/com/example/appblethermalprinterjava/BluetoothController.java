package com.example.appblethermalprinterjava;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothController {

    private static final String TAG = "BluetoothController";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice connectedDevice;
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private Handler handler;

    public BluetoothController(Handler handler) {
        this.handler = handler;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    @SuppressLint("MissingPermission")
    public void enableBluetooth(Activity activity, int requestCode) {
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, requestCode);
        }
    }

    @SuppressLint("MissingPermission")
    public void disableBluetooth() {
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.disable();
        }
    }

    @SuppressLint("MissingPermission")
    public void connectToDevice(BluetoothDevice device) {
        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            bluetoothSocket.connect();
            connectedDevice = device;
            inputStream = bluetoothSocket.getInputStream();
            outputStream = bluetoothSocket.getOutputStream();
            startListening();
        } catch (IOException e) {
            Log.e(TAG, "Error connecting to device: " + e.getMessage());
            sendMessageToHandler("Connection failed");
        }
    }

    public void disconnect() {
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
                connectedDevice = null;
                sendMessageToHandler("Disconnected");
            } catch (IOException e) {
                Log.e(TAG, "Error closing connection: " + e.getMessage());
                sendMessageToHandler("Error disconnecting");
            }
        }
    }

    public void sendMessage(String message) {
        if (outputStream != null) {
            try {
                outputStream.write(message.getBytes());
                sendMessageToHandler("Sent: " + message);
            } catch (IOException e) {
                Log.e(TAG, "Error sending message: " + e.getMessage());
                sendMessageToHandler("Error sending message");
            }
        }
    }

    private void startListening() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[1024];
                int bytes;

                while (true) {
                    try {
                        bytes = inputStream.read(buffer);
                        String receivedMessage = new String(buffer, 0, bytes);
                        sendMessageToHandler("Received: " + receivedMessage);
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading from input stream: " + e.getMessage());
                        sendMessageToHandler("Error reading from input stream");
                        break;
                    }
                }
            }
        }).start();
    }

    private void sendMessageToHandler(String message) {
        if (handler != null) {
            Message msg = handler.obtainMessage();
            msg.obj = message;
            handler.sendMessage(msg);
        }
    }
}
