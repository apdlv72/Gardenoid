package com.apdlv.ilibaba.bt;

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.apdlv.ilibaba.bt.SPPService.SPPBinder;


public class SPPConnection implements ServiceConnection
{
    private Handler    mHandler;
    private SPPService mService = null;

    final static String TAG = SPPConnection.class.getSimpleName();
    
    public SPPConnection(Handler handler)
    {
	super();
	this.mHandler = handler;
    }
    
    
    public void onServiceConnected(ComponentName className, IBinder service) 
    {
	// We've bound to LocalService, cast the IBinder and get LocalService instance
	SPPBinder  btBinder   = (SPPBinder) service;
	mService = btBinder.getService();
	Log.d(TAG, "Got BT frotect serial service " + mService);
	setHandler(mHandler);
	Log.d(TAG, "Registered handler with BT service ");
    }


    public void setHandler(Handler handler)
    {
	mService.setHandler(handler, true);
    }
    

    public void onServiceDisconnected(ComponentName arg0) 
    {
	synchronized (this)
	{
	    if (null!=mService)
	    {
		mService.removeHandler(mHandler);
		mService = null;
	    }
	}
    }

    public void sendLine(String s)
    {
	synchronized (this)
	{
	    if (null!=mService)
	    {
		s = s + "\n";
		Log.d(TAG, "Sending string '" + s + "'");
		mService.write(s.getBytes());
	    }
	}
    }

    public void connect(BluetoothDevice device)
    {
	if (null!=mService)
	{
	    mService.connect(device);
	}
    }

    public void disconnect()
    {
	if (null!=mService)
	{
	    mService.disconnect();
	}	
    }

    
    public void unbind(Context applicationContext)
    {
	synchronized (this)
	{
	    if (null!=mService)
	    {
		Log.i(TAG, "unbind: removing handler and unbinding from service");
		mService.removeHandler(mHandler);
		applicationContext.unbindService(this);
		mService = null;
	    }
	    else
	    {
		Log.i(TAG, "unbind: no service available (any more?)");
	    }
	}
    }


    public int getState()
    {
	return mService.getState();
    }
 
    
    public boolean isConnected()
    {
	return null!=mService && mService.isConnected();
    }


    public void startDiscovery()
    {
	if (null!=mService)
	{
	    mService.startDiscovery();
	}
	
    }
}