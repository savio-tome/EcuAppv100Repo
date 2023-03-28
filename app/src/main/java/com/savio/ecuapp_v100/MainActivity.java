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
import android.content.DialogInterface;

import android.content.pm.PackageManager;
import android.os.Bundle;

import android.os.Handler;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.enums.ObdProtocols;
import com.github.pires.obd.exceptions.UnableToConnectException;
import com.github.pires.obd.exceptions.UnsupportedCommandException;
//import com.github.pires.obd.reader.config.ObdConfig;
//import com.github.pires.obd.reader.io.ObdCommandJob;
//import com.github.pires.obd.reader.io.ObdGatewayService;
//import com.github.pires.obd.reader.io.ObdProgressListener;
//import com.github.pires.obd.reader.net.ObdReading;

//ELM327 v 2.1 ?
public class MainActivity extends AppCompatActivity{
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
    Button sendCommandBtn, pairedBtn;
    EditText commandEt;
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
        sendCommandBtn = findViewById(R.id.sendBtn);
        commandEt = findViewById(R.id.commandEt);

        startInterfaceBluetooth();
        sendCommandBtn.setEnabled(false);

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

        sendCommandBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String command = commandEt.getText().toString();
//                    OutputStream outputStream = socket.getOutputStream();
//                    InputStream inputStream = socket.getInputStream();
                sendCommand(command);
            }
        });
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
            showToast("Device connected!");
            sendCommandBtn.setEnabled(true);
        } catch (Exception e) {
            showToast("Error at connect a device");
            return;
        }

//        try {
//            // Envia o comando AT para o dispositivo ELM327
//            OutputStream outputStream = socket.getOutputStream();
//            outputStream.write("AT\r".getBytes());
//
//            // Lê a resposta do dispositivo ELM327
//            InputStream inputStream = socket.getInputStream();
//            byte[] buffer = new byte[1024];
//            int bytes = inputStream.read(buffer);
//            String response = new String(buffer, 0, bytes);
//            showToast("Response: " + response);
//
//            outputStream.close();
//            inputStream.close();
//
//        } catch (Exception e) {
//            showToast("Error at stablish comunication with remote device");
//        }
    }

    private void startInterfaceBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            statusBluetoothTv.setText("Bluetooth is not available");
        } else {
            statusBluetoothTv.setText("Bluetooth available");
        }
    }

    public void getRPM(OutputStream outputStream, InputStream inputStream) {
        try {
            outputStream.write("01 0C\r".getBytes());
            byte[] buffer = new byte[1024];
            int bytes = inputStream.read(buffer);
            String response = new String(buffer, 0, bytes);
            Toast.makeText(this, "RPM: " + response, Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            showToast("Failed to read RPM command!");
        }
    }

    @SuppressLint("MissingPermission")
    public void sendCommand(String command) {
//        if (!socket.isConnected()) {
//            try {
//                BluetoothDevice elm327 = bluetoothAdapter.getRemoteDevice(deviceHardwareAddress.toString());
//                socket = elm327.createRfcommSocketToServiceRecord(MY_UUID);
//                socket.connect();
//            } catch (IOException e) {
//                showToast("canot connect again");
//            }
//        }
        try {
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(command.getBytes());
            TimeUnit.SECONDS.sleep(1);
            InputStream inputStream = socket.getInputStream();
            byte[] buffer = new byte[1024];
            int bytes = inputStream.read(buffer);
            String response = new String(buffer, 0, bytes);
            pairTv.setText(response);

//            outputStream.close();
//            inputStream.close();

        } catch (IOException e) {
            showToast("canot connect to module");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    private void showToast(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

//    private class ObdService extends Thread{
//
//    }
}


//    private void getDiagnostics(){
//        new EchoOffObdCommand().run(socket.getInputStream(), socket.getOutputStream());
//
//        new LineFeedOffObdCommand().run(socket.getInputStream(), socket.getOutputStream());
//
//        new TimeoutObdCommand().run(socket.getInputStream(), socket.getOutputStream());
//
//        new SelectProtocolObdCommand(ObdProtocols.AUTO).run(socket.getInputStream(), socket.getOutputStream());
//    }