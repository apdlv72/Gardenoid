package com.apdlv.ilibaba.bt;

import java.util.HashSet;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class SPPDataHandler extends Handler
{
    private boolean mConnected;

    private Device device;
    
    final static String TAG = SPPDataHandler.class.getSimpleName();
    
    public void addDataListener(SPPDataListener l)
    {
        if (null!=l)
        {
            dataListeners.add(l);
        }
    }

    public void removeDataListener(SPPDataListener l)
    {
        if (null!=l)
        {
            dataListeners.remove(l);
        }
    }

    public void addStatusListener(SPPStatusListener l)
    {
        if (null!=l)
        {
            statusListeners.add(l);
            l.setStatus(mConnected);
        }
    }

    public void removeStatusListener(SPPStatusListener l)
    {
        if (null!=l)
        {
            statusListeners.remove(l);
        }
    }

    @Override
    public void handleMessage(Message msg) 
    {    
        int what = msg.what;
        //System.err.println(TAG + ".handleMessage: Got message " + what + "(" + SPPService.message2String(what) + ")");
        switch (what) 
        {

        case SPPService.MESSAGE_HELLO:
        	onServiceConnected();
        	break;

        case SPPService.MESSAGE_DEBUG_MSG:
        	onDebugMessage((String) msg.obj);
        	break;

        case SPPService.MESSAGE_STATE_CHANGE:
            //onDebugMessage("MESSAGE_STATE_CHANGE: " + msg.arg1 + "(" + SPPService.state2String(msg.arg1) + ")");
            
            Log.e(TAG, "MESSAGE_STATE_CHANGE: " + SPPService.state2String(msg.arg1));
            
            switch (msg.arg1) 
            {
            case SPPService.STATE_CONNECTING:
                onConnectingDevice();
                break;
                
            case SPPService.STATE_CONNECTED:
                mConnected = true;
                dispatchDeviceConnect(device);
                break;
                
            case SPPService.STATE_DISCONNECTED:
                mConnected = false;
                dispatchDeviceDisconnect(device);
                break;
                
            case SPPService.STATE_CONN_TIMEOUT:
                mConnected = false;
                onTimeout();
                break;
                
            case SPPService.STATE_FAILED:
            case SPPService.STATE_LOST:
        	mConnected = false;
        	dispatchDeviceDisconnect(device);
        	break;
        	
            case SPPService.STATE_NONE:
                onIdle();
                break;
            }
            break;
    	
        case SPPService.MESSAGE_READLINE:
        	dispatchLine((String) msg.obj);
        	break;

        case SPPService.MESSAGE_READ:                
        	onDebugMessage("Got MESSAGE_READ "+ msg);
        	onDataReceived((byte[]) msg.obj, msg.arg1);
        	break;

        case SPPService.MESSAGE_DEVICE_INFO:
        	// save the connected device's name and address
            	Bundle bundle = (Bundle)msg.obj;
            	try
            	{
            	    device = new Device(bundle.getString(SPPService.KEY_DEVICE_NAME), bundle.getString(SPPService.KEY_DEVICE_ADDR));
            	    onDeviceInfo(device);
            	}
            	catch (Exception e)
            	{
            	    System.err.println(TAG + ".handleMessage: " + e);
            	}
        	break;

        case SPPService.MESSAGE_TOAST:
        	onToast((String) msg.obj, msg.arg1==1);
        	break;
        	
        case SPPService.MESSAGE_RESET_BT:
            {
                BluetoothDevice device = (BluetoothDevice) msg.obj;
                onBluetoothReset(device);
            }
            break;
        }
    }
    
    public class Device
    {
	private String name;
	private String addr;

	public Device(String name, String addr)
	{
	    this.name = name; 
	    this.addr = addr;
	}
	
	public String getName()
	{
	    return name;
	}
	
	public String getAddr()
	{
	    return addr;
	}
	
	public String toString()
	{
	    return "" + name + "[" + addr + "]";
	}
    }
    
    private void dispatchDeviceConnect(Device device)
    {
        onDeviceConnected();
        
        for (SPPStatusListener l : statusListeners)
        {
            l.onConnect(device);
        }
    }

    private void dispatchDeviceDisconnect(Device device)
    {
        onDeviceDisconnected();
        
        for (SPPStatusListener l : statusListeners)
        {
            l.onDisconnect(device);
        }
    }

    private void dispatchLine(String receivedLine)
    {
	// subclass first
        onLineReceived(receivedLine);
        
        // then the listener
        for (SPPDataListener l : dataListeners)
        {
            l.onLineReceived(receivedLine);
        }
    }

    // methods to be overridden by subclasses in order to receive various events:
    protected void onServiceConnected() {}
    protected void onDeviceInfo(Device device) {}
    protected void onConnectingDevice() {}
    protected void onDeviceConnected() {}
    protected void onDeviceDisconnected() {}
    protected void onTimeout() {}
    protected void onIdle() {}
    protected void onDataReceived(byte[] readBuf, int len) {}
    protected void onLineReceived(String receivedLine) {}
    protected void onToast(String msg, boolean _long) {}
    protected void onDebugMessage(String msg) {}
    protected void onBluetoothReset(BluetoothDevice device) {}


    
    private HashSet<SPPDataListener>   dataListeners = new HashSet<SPPDataListener>();
    private HashSet<SPPStatusListener> statusListeners = new HashSet<SPPStatusListener>();
    
}