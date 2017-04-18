package fr.eove.android.bluetooth.app;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import fr.eove.android.bluetooth.service.BluetoothService;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startBluetoothService();
    }

    private void startBluetoothService() {

        Intent i = new Intent(this, BluetoothService.class);
        startService(i);
    }
}
