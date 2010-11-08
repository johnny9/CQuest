package edu.illinois.CS598rhk;

import android.bluetooth.BluetoothAdapter;


public class BluetoothController {
    //Singleton pattern
    private volatile static BluetoothController INSTANCE = new BluetoothController();
    
    private BluetoothAdapter bluetoothAdapter;
    
    private BluetoothController() {
        
    }
    
    @Override
    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }
    
    public static BluetoothController getInstance() {
        return INSTANCE;
    }
}
