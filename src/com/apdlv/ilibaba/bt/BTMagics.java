package com.apdlv.ilibaba.bt;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;

public class BTMagics
{
    public static boolean isPc(BluetoothDevice device)
    {
	BluetoothClass btc = device.getBluetoothClass();
	int mdc = btc.getMajorDeviceClass();
	return (mdc == BluetoothClass.Device.Major.COMPUTER);
    }

    public static boolean isUncategorized(BluetoothDevice device)
    {
	BluetoothClass btc = device.getBluetoothClass();
	int mdc = btc.getMajorDeviceClass();
	return (mdc == BluetoothClass.Device.Major.UNCATEGORIZED);
    }

    public static boolean isHC05(BluetoothDevice device)
    {
	String addr = device.getAddress();
	for (String s : HC05_ADDR_PREFIXES)
	{
	    if (addr.startsWith(s)) return true;
	}
	return false;
    }
    
    private static final String HC05_ADDR_PREFIXES[] = 
	{
	"20:13:01",
	"00:12:10",
	"00:12:11",
	};
}
