package com.example.btharman;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends Activity {

    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private static final int REQUEST_DISCOVERABLE_BLUETOOTH = 2;
    private static final int PERMISSION_REQUEST_CODE = 3;


    private Switch switchBluetooth;
    private ToggleButton toggleButton;
    private ToggleButton a2dpButton;
    public String deviceAddress;
    private String deviceName;
    private TextView connectionStatus, a2dpStatus;
    private Button buttonDiscoverable;
    private BluetoothProfile.ServiceListener serviceListener;
    private ListView listViewPairedDevices;
    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<String> pairedDeviceList;
    private ArrayAdapter<String> pairedDeviceAdapter;
    private BluetoothHeadset bluetoothHeadset;

    private BluetoothA2dp bluetoothA2dp;


    private int previousState = BluetoothHeadset.STATE_DISCONNECTED;
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH}, 0);
                } else {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, 0);
                    } else {
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        if (device != null) {
                            String deviceName = device.getName();
                            String deviceAddress = device.getAddress();

                            // Add the discovered device to the list
                            if (deviceName != null && deviceAddress != null) {
                                pairedDeviceList.add(deviceName + "\n" + deviceAddress);
                                pairedDeviceAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                    if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                        int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                        if (state == BluetoothAdapter.STATE_ON) {
                            Toast.makeText(MainActivity.this, "Bluetooth enabled", Toast.LENGTH_SHORT).show();
                        } else if (state == BluetoothAdapter.STATE_OFF) {
                            Toast.makeText(MainActivity.this, "Bluetooth disabled", Toast.LENGTH_SHORT).show();
                        }

                    }
                }
            }
        }

    };
    private final BroadcastReceiver headsetBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {
                    Log.d("HFP Broadcast", "HFP onReceive is running ");
                    Toast.makeText(MainActivity.this, "HFP is connected", Toast.LENGTH_SHORT).show();
                    int prevState = intent.getIntExtra(BluetoothHeadset.EXTRA_PREVIOUS_STATE, BluetoothHeadset.STATE_DISCONNECTED);
                    int newState = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_CONNECTING);

                    handleBluetoothHeadsetState(prevState, newState);
                }
            }
        }
    };

    private void handleBluetoothHeadsetState(int prevState, int newState) {
        // Display previous state and current state in the text view
        String prevStateText = getStateText(prevState);
        String newStateText = getStateText(newState);
        connectionStatus.setText(prevStateText + " -> " + newStateText);

        previousState = newState;
    }


    private final BroadcastReceiver a2dpBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)) {
                    Log.d("A2DP Broadcast", "A2DP onReceive is running ");
                    int prevState = intent.getIntExtra(BluetoothA2dp.EXTRA_PREVIOUS_STATE, BluetoothA2dp.STATE_DISCONNECTED);
                    int newState = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, BluetoothA2dp.STATE_CONNECTING);

                    handleBluetoothA2dpState(prevState, newState);
                }
            }
        }
    };

    private void handleBluetoothA2dpState(int prevState, int newState) {
        // Display previous state and current state in the text view
        String prevStateText = getStateText(prevState);
        String newStateText = getStateText(newState);
        a2dpStatus.setText(prevStateText + " -> " + newStateText);

        previousState = newState;
        Log.d("states", "A2dp states ");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toggleButton = findViewById(R.id.toggleButton);
        a2dpButton = findViewById(R.id.a2dpButton);
        a2dpStatus = findViewById(R.id.a2dpStatus);
        connectionStatus = findViewById(R.id.connectionStatus);
        switchBluetooth = findViewById(R.id.switchBluetooth);
        buttonDiscoverable = findViewById(R.id.buttonDiscoverable);
        listViewPairedDevices = findViewById(R.id.listViewPairedDevices);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        pairedDeviceList = new ArrayList<>();
        pairedDeviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, pairedDeviceList);
        listViewPairedDevices.setAdapter(pairedDeviceAdapter);

        // Register the broadcast receiver for Bluetooth discovery
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(broadcastReceiver, filter);

        IntentFilter filter1 = new IntentFilter();
        filter1.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        registerReceiver(headsetBroadcastReceiver, filter1);


        filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        registerReceiver(a2dpBroadcastReceiver, filter);


        // Check if Bluetooth is supported on the device
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show();
            switchBluetooth.setEnabled(false);
            buttonDiscoverable.setEnabled(false);
        } else {
            // Set the initial state of the switch button
            switchBluetooth.setChecked(bluetoothAdapter.isEnabled());

            switchBluetooth.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    // Enable Bluetooth
                    enableBluetooth();
                } else {
                    // Disable Bluetooth
                    disableBluetooth();
                }
            });

            buttonDiscoverable.setOnClickListener(v -> {
                // Make the device discoverable
                makeDiscoverable();
            });


            listViewPairedDevices.setOnItemClickListener((parent, view, position, id) -> {
                String item = (String) parent.getItemAtPosition(position);
                Toast.makeText(MainActivity.this, "Selected Device: " + item, Toast.LENGTH_SHORT).show();
            });
        }
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    enableHFP();
                } else {
                    disableHFP();
                }
            }
        });
        a2dpButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    enableA2DP();
                } else {
                    disableA2DP();
                }
            }
        });

        // Request the necessary permissions if not granted
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    0);
        }
    }


    private void enableA2DP() {
        bluetoothAdapter.getProfileProxy(MainActivity.this, new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                if (profile == BluetoothProfile.A2DP) {
                    bluetoothA2dp = (BluetoothA2dp) proxy;
                    handleBluetoothA2dpState(BluetoothA2dp.STATE_CONNECTING, BluetoothA2dp.STATE_CONNECTED);
                    Log.d("A2DP", "A2DP onServiceConnected is running ");
                }
            }

            @Override
            public void onServiceDisconnected(int profile) {
                if (profile == BluetoothProfile.A2DP) {
                    bluetoothA2dp = null;
                    handleBluetoothA2dpState(BluetoothA2dp.STATE_CONNECTED, BluetoothA2dp.STATE_DISCONNECTING);
                    Log.d("A2DP", "A2DP onServiceDisconnected is running ");
                }
            }
        }, BluetoothProfile.A2DP);
    }

    private void disableA2DP() {
        if (bluetoothA2dp != null) {
            bluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, bluetoothHeadset);
            bluetoothA2dp = null;
            handleBluetoothA2dpState(BluetoothHeadset.STATE_DISCONNECTING, BluetoothHeadset.STATE_DISCONNECTED);
        }
    }


    private void enableHFP() {
        connectHFP();
    }

    private void connectHFP() {
        bluetoothAdapter.getProfileProxy(MainActivity.this, new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                if (profile == BluetoothProfile.HEADSET) {
                    bluetoothHeadset = (BluetoothHeadset) proxy;
                    handleBluetoothHeadsetState(BluetoothHeadset.STATE_CONNECTING, BluetoothHeadset.STATE_CONNECTED);}


                    Log.d("HFP", "HFP onServiceConnected is running ");
                }
         //   }


            @Override
            public void onServiceDisconnected(int profile) {
                if (profile == BluetoothProfile.HEADSET) {
                    bluetoothHeadset = null;
                    handleBluetoothHeadsetState(BluetoothHeadset.STATE_CONNECTED, BluetoothHeadset.STATE_DISCONNECTING);
                    Log.d("HFP", "HFP onServiceDisconnected is running ");

                }
            }
        }, BluetoothProfile.HEADSET);


    }

    private void disableHFP() {
        if (bluetoothHeadset != null) {
            bluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, bluetoothHeadset);
            bluetoothHeadset = null;
            handleBluetoothHeadsetState(BluetoothHeadset.STATE_DISCONNECTING, BluetoothHeadset.STATE_DISCONNECTED);
        }
    }


    private void enableBluetooth() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH}, 0);
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH);
                Log.d("BT", "BT  is running ");
            }
        }
    }

    private void disableBluetooth() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, 0);
        } else {
            if (bluetoothAdapter.isEnabled()) {
                bluetoothAdapter.disable();
                pairedDeviceList.clear();
                pairedDeviceAdapter.notifyDataSetChanged();
            }
        }
    }

    private void makeDiscoverable() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH}, REQUEST_DISCOVERABLE_BLUETOOTH);
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, REQUEST_DISCOVERABLE_BLUETOOTH);
            } else {
                if (bluetoothAdapter != null) {
                    Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                    startActivityForResult(discoverableIntent, REQUEST_DISCOVERABLE_BLUETOOTH);
                }
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show();
            } else {
                switchBluetooth.setChecked(false);
                Toast.makeText(this, "Bluetooth enabling canceled", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_DISCOVERABLE_BLUETOOTH) {
            if (resultCode == 300) {
                Toast.makeText(this, "Device is discoverable for 5 minutes", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Device is not discoverable", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH}, 0);
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, 0);
            } else {
                // Refresh the paired device list
                pairedDeviceList.clear();
                if (bluetoothAdapter != null) {
                    Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                    if (pairedDevices != null && pairedDevices.size() > 0) {
                        for (BluetoothDevice device : pairedDevices) {
                            pairedDeviceList.add(device.getName() + "\n" + device.getAddress());
                        }
                    }
                }
                pairedDeviceAdapter.notifyDataSetChanged();
            }
        }
    }

    private String getStateText(int state) {
        switch (state) {
            case BluetoothHeadset.STATE_CONNECTING:
                return "Connecting";
            case BluetoothHeadset.STATE_CONNECTED:
                return "Connected";
            case BluetoothHeadset.STATE_DISCONNECTING:
                return "Disconnecting";
            case BluetoothHeadset.STATE_DISCONNECTED:
                return "Disconnected";
            default:
                return "Unknown";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unregister the broadcast receiver
        unregisterReceiver(broadcastReceiver);
        unregisterReceiver(headsetBroadcastReceiver);
        unregisterReceiver(a2dpBroadcastReceiver);
    }
}


