package com.savio.ecuapp_v100;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
//import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

//ELM327 v 2.1 ?
public class MainActivity extends AppCompatActivity {
    static String[] appPermissions = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
    };

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static final int PERMISSION_REQUEST_CODE = 1;
    final ArrayList devices = new ArrayList();
    ArrayList deviceStrs = new ArrayList();
    private TextView statusBluetoothTv, pairTv;
    Button onBtn, pairedBtn;
    BluetoothAdapter bluetoothAdapter;
    Object deviceHardwareAddress;
    public BluetoothSocket socket;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
//      Solicita permissões para acessar e conectar bluetooth caso ainda não foram cedidas

        if (checkPermissions()) {
//
        } else {
            showToast("Permissions is denied");
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusBluetoothTv = findViewById(R.id.statusBluetoothTv);
        pairTv = findViewById(R.id.pairTv);
        pairedBtn = findViewById(R.id.pairedBtn);

        startInterfaceBluetooth();

//      Ao clicar no botão GET PAIRED, mostrar a lista de dispositivos pareados e conetar no escolhido
        pairedBtn.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View v) {
                if (bluetoothAdapter.isEnabled()) {
                    pairTv.setText("Paired Device: ");
                    Set<BluetoothDevice> Pairedevices = bluetoothAdapter.getBondedDevices();
                    for (BluetoothDevice device : Pairedevices) {
                        deviceStrs.add(device.getName() + "\n" + device.getAddress());
                        devices.add(device.getAddress());
                    }
                    showPairedDevices();
                } else {
                    showToast("Turn on Bluetooth!!");
                }
            }
        });


        //ia code
//            BluetoothSocket socket = device.createRfcommSocketToServiceRecord(MY_UUID);
//            socket.connect();
//
//            // Envia o comando AT para o dispositivo ELM327
//            OutputStream outputStream = socket.getOutputStream();
//            outputStream.write("AT\r".getBytes());
//
//            // Lê a resposta do dispositivo ELM327
//            InputStream inputStream = socket.getInputStream();
//            byte[] buffer = new byte[1024];
//            int bytes = inputStream.read(buffer);
//            String response = new String(buffer, 0, bytes);
//            Toast.makeText(this, "Response: " + response, Toast.LENGTH_SHORT).show();
//
//            // Envia o comando para ler o RPM do motor
//            outputStream.write("01 0C\r".getBytes());
//            bytes = inputStream.read(buffer);
//            response = new String(buffer, 0, bytes);
//            Toast.makeText(this, "RPM: " + response, Toast.LENGTH_SHORT).show();
//
//            outputStream.close();
//            inputStream.close();
//            socket.close();
    }

    public boolean checkPermissions() {
        List<String> requiredPermissions = new ArrayList<>();
        for (String permission : appPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(permission);
            }
        }
        if (!requiredPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, requiredPermissions.toArray(
                            new String[requiredPermissions.size()]),
                    PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    ;

    private void showPairedDevices() {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.select_dialog_singlechoice,
                deviceStrs.toArray(new String[deviceStrs.size()]));

        alertDialog.setSingleChoiceItems(adapter, -1, new DialogInterface.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                int position = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                deviceHardwareAddress = devices.get(position);
                pairTv.append(deviceHardwareAddress.toString());
//                pairTv.setText(deviceHardwareAddress.toString());

//              Conect at choosed device from list
                connectChoosedDevice();
            }
        });
        alertDialog.setTitle("Choose Bluetooth device");
        alertDialog.show();
    }

    @SuppressLint("MissingPermission")
    private void connectChoosedDevice() {
        try {
            BluetoothDevice elm327 = bluetoothAdapter.getRemoteDevice(deviceHardwareAddress.toString());
            socket = elm327.createRfcommSocketToServiceRecord(MY_UUID);
            socket.connect();
        } catch (Exception e) {
            showToast("Error at connect a device");
            return;
        }

        try {
            // Envia o comando AT para o dispositivo ELM327
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write("AT@1\r".getBytes());

            // Lê a resposta do dispositivo ELM327
            InputStream inputStream = socket.getInputStream();
            byte[] buffer = new byte[1024];
            int bytes = inputStream.read(buffer);
            String response = new String(buffer, 0, bytes);
            showToast("Response: " + response);
//            Toast.makeText(this, "Response: " + response, Toast.LENGTH_SHORT).show();
            getRPM(outputStream, inputStream);
        } catch (Exception e) {
            showToast("Error at stablish comunication with remote device");
        }
    }

    private void startInterfaceBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            statusBluetoothTv.setText("Bluetooth is not available");
        } else {
            statusBluetoothTv.setText("Bluetooth available");
        }
    }

    public void getRPM(OutputStream outputStream, InputStream inputStream){
        try {
            outputStream.write("01 0C\r".getBytes());
            byte[] buffer = new byte[1024];
            int bytes = inputStream.read(buffer);
            String response = new String(buffer, 0, bytes);
            Toast.makeText(this, "RPM: " + response, Toast.LENGTH_LONG).show();

        }catch (Exception e){
            showToast("Failed to read RPM command!");
        }
    }

    private void showToast(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
