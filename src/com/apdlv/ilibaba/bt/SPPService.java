/*
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed timeout in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.apdlv.ilibaba.bt;


import java.util.HashSet;

import com.apdlv.utils.U;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.content.res.Configuration;

public class SPPService  extends Service
{
    public static final String KEY_DEVICE_NAME = "KEY_DEVICE_NAME";
    public static final String KEY_DEVICE_ADDR = "KEY_DEVICE_ADDR";

    // Constants that indicate the current connection state.
    public static final int STATE_NONE           =  0; // we're doing nothing
    public static final int STATE_CONNECTING     =  1; // now initiating an outgoing connection
    public static final int STATE_CONNECTED      =  2; // now connected timeout a remote device
    public static final int STATE_DISCONNECTED   =  3; // now connected timeout a remote device
    public static final int STATE_CONN_TIMEOUT   =  4; // now connected timeout a remote device
    public static final int STATE_LOST           =  5; // now listening for incoming connections
    public static final int STATE_FAILED         =  6; // now listening for incoming connections
    public static final int STATE_CANCELLED      =  7; // connection attempt was cancelled upon user request 
    
    // Message types sent from the BluetoothChatService handler.
    public static final int MESSAGE_HELLO        =  1; // sent to handler upon registration 
    public static final int MESSAGE_STATE_CHANGE =  2; // state changed
    public static final int MESSAGE_READ         =  3; // sending timeout handler a message received from the peer device
    public static final int MESSAGE_WRITE        =  4;
    public static final int MESSAGE_DEVICE_INFO  =  5;
    public static final int MESSAGE_TOAST        =  6;
    public static final int MESSAGE_READLINE     =  7;
    public static final int MESSAGE_DEBUG_MSG    =  8;
    public static final int MESSAGE_BYEBYE       =  9;
    public static final int MESSAGE_RESET_BT     = 10; // bring BT adapter down and up again in order to reset 

    public static String state2String(int state)
    {
        switch (state)
        {
        case STATE_NONE:         return "NONE";
        case STATE_FAILED:       return "FAILED";
        case STATE_CONNECTING:   return "CONNECTING";
        case STATE_CONNECTED:    return "CONNECTED";
        case STATE_DISCONNECTED: return "DISCONNECTED";
        case STATE_CONN_TIMEOUT: return "TIMEOUT";	
        case STATE_LOST:         return "LOST";	
        default:                 return "UNKNOWN";
        }
    }

    /*
    private static String message2String(int state)
    {
	switch (state)
	{
	case MESSAGE_HELLO:          return "HELLO";
	case MESSAGE_BYEBYE:         return "BYEBYE";
	case MESSAGE_STATE_CHANGE:   return "STATE_CHANGE";
	case MESSAGE_READ:           return "READ";
	case MESSAGE_WRITE:          return "WRITE";
	case MESSAGE_DEVICE_INFO:    return "DEVICE_INFO";
	case MESSAGE_TOAST:          return "TOAST";	
	case MESSAGE_READLINE:       return "READLINE";	
	case MESSAGE_DEBUG_MSG:      return "DEBUG_MSG";
	default:                     return "UNKNOWN";
	}
    }
    */

    public interface OnBluetoothListener
    {
	void onBTStateChanged(int state, String msg);
	void onBTDataReceived(byte data[], int len);
    }

    public class SPPBinder extends Binder 
    {
        public SPPBinder(SPPService service) { this.service = service; }
        public SPPService getService() 	     { return service; }
        private SPPService service;
    }

    public SPPService()
    {
	Log.d(TAG, "instantiated");
    }

    @Override
    public IBinder onBind(Intent intent) 
    {
	return new SPPBinder(this);
    }


    @Override
    public void onCreate() 
    {    
	super.onCreate();
    	Log.d(TAG, "onCreate");

    	IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
	registerReceiver(mReceiver, filter);
    };


    @Override
    public void onStart(Intent intent, int startId) 
    {
	super.onStart(intent, startId); 
	Log.d(TAG, "onStart");
    };
    

    @Override
    public boolean onUnbind(Intent intent) 
    {
	boolean rc = super.onUnbind(intent);
	Log.d(TAG, "onUnbind");
	disconnect();
	return rc;
    };

       
    @Override
    public void onConfigurationChanged(Configuration newConfig) 
    {
	super.onConfigurationChanged(newConfig); 
	Log.d(TAG, "onConfigurationChanged: " + newConfig);
    };

    
    @Override
    public void onRebind(Intent intent) 
    {
	super.onRebind(intent);
	Log.d(TAG, "onRebind");
    };

    
    @Override
    public void onDestroy() 
    {
	super.onDestroy();
	disconnect();
	
	Log.d(TAG, "onDestroy: Unregistering bluetooth broadcast receiver");
	unregisterReceiver(mReceiver);
    };

    
    public void setHandler(Handler handler, boolean linewise)
    {
	synchronized (this)
	{
	    if (null==handler)
	    {
		System.err.println(TAG + "ERROR: handler=null in setHandler");
	    }
	    else
	    {
		mHandler  = handler;
		mLinewise = linewise;
		mHandler.obtainMessage(MESSAGE_HELLO).sendToTarget();
	    }
	}
    }

    public void removeHandler(Handler handler)
    {
	synchronized (this)
	{
	    if (mHandler==handler)
	    {
		mHandler.obtainMessage(MESSAGE_BYEBYE).sendToTarget();
		mHandler=null;
	    }
	    else
	    {
		Log.e(TAG, "removeHandler: stale handler " + handler + ", current: " + mHandler);
	    }
	}
    }

    public int getState() 
    {
	synchronized (this)
	{
	    return mState;
	}
    }

    
    public boolean isConnected()
    {
        return STATE_CONNECTED==mState;
    }

    public boolean connect(BluetoothDevice device) 
    {
        synchronized (this)
        {
            /*
            if (checkTimeout(device))
            {
        	log("Detected former timeout connecting to " + device.getName());        	
        	mHandler.obtainMessage(MESSAGE_RESET_BT, device).sendToTarget();
        	return false;
            }            
            */
            
            log("SPPService.connect(" + device + ")");
            cancelThreads();
    
            // Start the thread timeout connect with the given device
            mConnectThread = new ConnectThread(this, device, mLinewise);
            mConnectThread.start();
    
            sendDevicenfo(device);
            setState(STATE_CONNECTING);
    	    return true;
        }
    }

    public void disconnect()
    {
	synchronized(this)
	{
	    Log.d(TAG, "disconnecting");
	    cancelThreads();
	    setState(STATE_DISCONNECTED);
	}
    }

    
    public void write(byte[] out) 
    {
	synchronized (this)
	{
	    if (mState!=STATE_CONNECTED) return;
	    try
	    {
		if (null!=mConnectThread)
		{
		    mConnectThread.write(out);
		}
	    }
	    catch (Exception e)
	    {
		Log.e(TAG, "write(byte[]): " + e);
	    }
	}	
    }
    

    void sendMessageBytes(ConnectThread connectThread, int messageRead, int bytes, byte[] buffer)
    {
	if (this.mConnectThread==connectThread)
	{
	    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
	}
	else
	{
	    Log.d(TAG, "Ignored bytes from wrong thread: " + bytes);
	}
    }

    void sendMessageString(ConnectThread connectThread, String msg)
    {
	if (this.mConnectThread==connectThread)
	{
	    mHandler.obtainMessage(MESSAGE_READLINE, msg).sendToTarget();
	}
	else
	{
	    Log.d(TAG, "Ignored string from wrong thread: " + msg);	    
	}
    }

    void sendDebug(ConnectThread connectThread, String msg)
    {
	if (this.mConnectThread==connectThread)
	{
	    if (null!=mHandler)
	    {
		mHandler.obtainMessage(MESSAGE_DEBUG_MSG, msg).sendToTarget();
	    }
	    else
	    {
		Log.d(TAG, "No handler (any more?) for debug msg: " + msg);
	    }
	}
	else
	{
	    Log.d(TAG, "Ignored string from wrong thread: " + msg);	    
	}
    }

    boolean cancelDiscovery(ConnectThread connectThread)
    {
	if (this.mConnectThread==connectThread)
	{
	    if (mAdapter.isDiscovering())
	    {
		mAdapter.cancelDiscovery();
	    }
	    return true;
	}
	else
	{
	    Log.d(TAG, "Ignored discovery cancellation from wrong thread: " + connectThread);
	    return false;
	}
    }

    public void startDiscovery()
    {
	mAdapter.startDiscovery();
    }

    void setState(ConnectThread connectThread, int state) 
    {
        synchronized (this)
        {
            if (mConnectThread!=connectThread)
            {
        	Log.d(TAG, "setState: Ignoring state change from wrong thread " + connectThread);
        	return;
            }
            if (D) Log.d(TAG, "setState() " + state2String(mState) + " -> " + state2String(state));
            mState = state;
    
            // Give the new state timeout the Handler so the UI Activity can update
            if (null!=mHandler)
            {
        	mHandler.obtainMessage(MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
            }
        }
    }

    //	/**
    //	 * Write timeout the ConnectedThread in an unsynchronized manner
    //	 * @param out The bytes timeout write
    //	 * @see ConnectedThread#write(byte[])
    //	 */
    //	public void write(byte[] out) 
    //	{
    //	// Create temporary object
    //	ConnectThread r;
    //
    //	// Synchronize a copy of the ConnectedThread
    //	synchronized (this) 
    //	{
    //	    if (mState != STATE_CONNECTED) return;
    //	    r = mConnectThread;
    //	}
    //	// Perform the write unsynchronized
    //	r.write(out);
    //    	}
    
    /**
     * Called by ConnectThread to indicate that the connection attempt failed and 
     * the UI Activity should be notified.
     * @param connectThread 
     */
    void connectionFailed(ConnectThread connectThread, String reason) 
    {
        synchronized (this)
        {
            setState(STATE_FAILED);
    
            // Send a failure message back timeout the Activity
            if (null!=mHandler)
            {
        	Message msg = mHandler.obtainMessage(MESSAGE_TOAST, reason);
        	//Bundle bundle = new Bundle();
        	mHandler.sendMessage(msg);
            }
        }
    }
    
    
    public void sendToast(String text)
    {
	sendToast(text, false);
    }
    
    public void sendToast(String text, boolean _long)
    {
	if (null==mHandler) return;
	Message msg = mHandler.obtainMessage(MESSAGE_TOAST, _long ? 1 : 0, -1, text);
	//Bundle bundle = new Bundle();
	mHandler.sendMessage(msg);	
    }
    

    public void scheduleRetry(ConnectThread connectThread)
    {
	if (mConnectThread==connectThread)
	{
	    int retry = connectThread.getRetry();
	    final int MAX = 4;
	    
	    if (retry>=MAX)
	    {		
		sendToast("Giving up");
		return;
	    }
	    else if (retry==2)
	    {
//		sendToast("Plaese switch BT off, then on and retry", true);
//		return;
		
		sendToast("Resetting bluetooth adapter");
		
		long start = mCounterBTOff;
		if (mAdapter.isEnabled())
		{		   
		    log("Resetting BT adapter");
		    mAdapter.disable();
		
		    while (start==mCounterBTOff)
		    {
			Log.d(TAG, "Waiting for adapter to come down, counter: " + mCounterBTOff);
			com.apdlv.utils.U.sleep(250);
		    }
		}		

		U.sleep(2500);
		
		start = mCounterBTOn;
		log("Bringing BT adapter up");
		mAdapter.enable();
		
		while (start==mCounterBTOn)
		{
		    Log.d(TAG, "Waiting for adapter to come up, counter: " + mCounterBTOn);
		    U.sleep(250);
		}

		mAdapter.startDiscovery();
		U.sleep(2500);
	    }
	    
	    U.sleep(1000);
	    retry++;
	    sendToast("Retrying (" + retry + " of  " + MAX + ")");
	    mConnectThread = new ConnectThread(this, connectThread.getDevice(), connectThread.getLinewise(), retry);
	    mConnectThread.start();
	}
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    void connectionLost(String reason) 
    {
        synchronized (this)
        {
            setState(STATE_LOST);
    
            // Send a failure message back timeout the Activity
            if (null!=mHandler)
            {
        	Message msg = mHandler.obtainMessage(MESSAGE_TOAST, "Device connection was lost: " + reason);
        	mHandler.sendMessage(msg);
            }
        }
    }

    private HashSet<ConnectThread> registeredThreads = new HashSet<ConnectThread>();
    
    public void registerConnectThread(ConnectThread connectThread)
    {
	synchronized (registeredThreads)
	{
	    registeredThreads.add(connectThread);
	}
    }    

    public boolean checkTimeout(BluetoothDevice targetDevice)
    {
	boolean rc = false;
	HashSet<ConnectThread> toBeRemoved = new HashSet<ConnectThread>();
	
	synchronized (registeredThreads)
        {
	    for (ConnectThread t : registeredThreads)
	    {
		BluetoothDevice device = t.getDevice();
		boolean same = (device.getAddress().equals(targetDevice.getAddress()));

		if (!t.isAlive()) // remove dead threads
		{
		    toBeRemoved.add(t);
		}
		else if (same && t.isTimedout())
		{
		    rc = true;
		    toBeRemoved.add(t);
		}
	    }

	    for (ConnectThread t : toBeRemoved)
	    {
		registeredThreads.remove(t);
	    }
        }
	return rc;
    }
    
    /*
    private class CancelThread extends Thread 
    {
        private long timeout;
    
        public CancelThread(long timeoutMillies)
        {
            this.timeout = timeoutMillies;
        }
    
        public void run() 
        {
            long left = timeout;
            while (null!=mConnectThread && !mConnectThread.isConnected() && left>0)
            {
        	try { Thread.sleep(left<250 ? left : 250); } catch (InterruptedException e) {}
        	left-=250;
            }
    
            if (null!=mConnectThread && !mConnectThread.isConnected())
            {
        	if (mConnectThread.cancelIfNotConnected())
        	{
        	    log("Cancelled connect thread after timeout");
        	    mConnectThread = null;
        	}
            }
        }
    }
     */
    
    private void sendDevicenfo(BluetoothDevice device)
    {
        if (null!=mHandler)
        {
            String name = device.getName();
            String addr = device.getAddress();
            Message msg;
    
            Bundle bundle = new Bundle();
            bundle.putString(KEY_DEVICE_ADDR, addr);
            bundle.putString(KEY_DEVICE_NAME, name);
            msg = mHandler.obtainMessage(MESSAGE_DEVICE_INFO, bundle);
            mHandler.sendMessage(msg);
        }
    }

    private void setState(int state) 
    {
        synchronized (this)
        {
            if (mState == state)
            {
        	return; // noc change
            }
            
            if (D) Log.d(TAG, "setState() " + state2String(mState) + " -> " + state2String(state));
            mState = state;
    
            // Give the new state timeout the Handler so the UI Activity can update
            if (null!=mHandler)
            {
        	mHandler.obtainMessage(MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
            }
        }
    }

    private void cancelThreads()
    {
        if (mConnectThread != null) 	    
        {
            try { mConnectThread.cancel();  } catch (Exception e) {} 
        }        
    }

    private void log(String msg)
    {
        synchronized (this)
        {
            if (null==mHandler) 
            {
        	Log.d(TAG, msg);
        	return;
            }
            mHandler.obtainMessage(MESSAGE_DEBUG_MSG, msg).sendToTarget();
        }
    }

    
    private volatile long mCounterBTOff = 0;
    private volatile long mCounterBTOn  = 0;
    
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() 
    {
	@Override
	public void onReceive(Context context, Intent intent) 
	{
	    final String action = intent.getAction();

	    if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) 
	    {
		final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
		
		switch (state) 
		{
		case BluetoothAdapter.STATE_OFF:
		    mCounterBTOff++;
		    break;
		case BluetoothAdapter.STATE_ON:
		    mCounterBTOn++;
		    break;
		}

	    }
	}
    };

    // Debugging
    private static final String  TAG = SPPService.class.getSimpleName();
    private static final boolean D   = true;
    // Member fields
    private BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
    private int           mState           = STATE_NONE;
    private boolean       mLinewise        = true;
    private Handler       mHandler         = null;
    //private ConnectThread mConnectingThread  = null;
    private ConnectThread mConnectThread = null;

}
