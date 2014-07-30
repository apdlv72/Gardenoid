package com.apdlv.ilibaba.bt;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.UUID;

import com.apdlv.utils.U;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

class TimeoutThread extends Thread
{
    private final String TAG = TimeoutThread.class.getSimpleName();
 
    private ConnectThread connectThread;

    TimeoutThread(ConnectThread connectThread)
    {
	this.connectThread = connectThread;
    }

    public void run()
    {
	long start = now();
	
	while (now()-start<3*ConnectThread.THRESHOLD)
	{
	    if (connectThread.isConnected())
	    {
		Log.e(TAG, "ConnectThread connected, bailing out");
		return;
	    }
	    else if (!connectThread.isAlive())
	    {
		Log.e(TAG, "ConnectThread dead, bailing out");
		return;
	    }
	    else if (connectThread.isTimedout())
	    {
		Log.e(TAG, "ConnectThread timed out, calling handleTimeout");
		connectThread.handleTimeout();
	    }
	    
	    try { Thread.sleep(100); } catch (Exception e) {}
	}
	   
    }

    private static long now()
    {
	return Calendar.getInstance().getTimeInMillis();
    }
}

class SocketCloseThread extends Thread
{
    private final String TAG = SocketCloseThread.class.getSimpleName();

    private BluetoothSocket mSocket;

    SocketCloseThread(BluetoothSocket socket)
    {
	this.mSocket = socket;
    }

    public void run()
    {
	try
	{
	    Log.e(TAG, "Closing BT socket " + mSocket + " (in the background)");
	    mSocket.close();
	    Log.e(TAG, "BT socket " + mSocket + " closed");
	}
	catch (Exception e)
	{
	    Log.e(TAG, "" + e);
	}
    }
}


class ConnectThread extends Thread 
{
    // The standard Serial Port Profile UUID
    public static final UUID SPP_UUID =  UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    
    public final static long THRESHOLD = 5*1000L; // seconds

    public static boolean DUMP_SERVICES = false;


    public ConnectThread(SPPService sppService, BluetoothDevice device, boolean linewise)
    {
	this(sppService, device, linewise, 0);
    }
    
    
    public ConnectThread(SPPService sppService, BluetoothDevice device, boolean linewise, int retry) 
    {
	if (DUMP_SERVICES)
	{
	    logDeviceServices(device);
	}

	this.mRetry      = retry;
	this.mDevice     = device;
	this.mSPPService = sppService;
	this.mLinewise   = linewise;
	this.mDoCancel   = false;
	setState(SPPService.STATE_NONE);
    }

    public boolean isConnected()
    {
	return mLastState==SPPService.STATE_CONNECTED;
    }

    public void handleTimeout()
    {	// save this value, since cancle() belo will set it for sure
	boolean wasCancelled = mDoCancel;
	
	cancel();
	
	if (!wasCancelled)
	{
	    Log.d(TAG, "handleTimeout: not cancelled, calling scheduleRetry");
	    mSPPService.scheduleRetry(this);
	}
	else
	{
	    Log.d(TAG, "handleTimeout: WAS cancelled, NOT calling scheduleRetry");	    
	}
	setState(SPPService.STATE_CONN_TIMEOUT);
    }

    public BluetoothDevice getDevice()
    {
	return mDevice;
    }

    public boolean isTimedout()
    {	
	return mLastState==SPPService.STATE_CONN_TIMEOUT && now()-mLastStateChange>THRESHOLD; 
    }


    public void run() 
    {
	// socket.connect() may block forever, therefore start a thread that will monitor
	// whether the connection attempt takes too long. 
	TimeoutThread toThread = new TimeoutThread(this);
	toThread.start();
	
	// Register this thread with the service to allow detection of threads that
	// stalled while connecting to the remote device or even while closing the socket.
        mSPPService.registerConnectThread(this);
	
        log("BEGIN mConnectThread");
        setName("ConnectThread");
        setState(SPPService.STATE_CONNECTING);
    
        // Always cancel discovery because it will slow down a connection
        Log.d(TAG, "Cancelling discovery ...");
        if (!mSPPService.cancelDiscovery(this))
        {
            setState(SPPService.STATE_FAILED);
            return;
        }
    
        if (mDoCancel)
        {        
            setState(SPPService.STATE_CANCELLED);
            return;
        }
        
        try
        {
            log("Creating bluetooth socket ...");
            setState(SPPService.STATE_CONN_TIMEOUT); // next method might block
            mSocket = createSocket(mDevice);
            setState(SPPService.STATE_CONNECTING);            
        }
        catch (Exception e)
        {
            log("Exception: "+ e);
        }
    
        if (null==mSocket) // connect was not successful
        {
            log("Socket creation failed");
            mSPPService.connectionFailed(this, "Socket creation failed");		
            setState(SPPService.STATE_FAILED);            
            return;		
        }

        if (mDoCancel)
        {
            setState(SPPService.STATE_CONN_TIMEOUT); // next method might block
            closeBluetoothSocket();
            setState(SPPService.STATE_CANCELLED);
            mSPPService.connectionFailed(this, "Connection cancelled");
            return;
        }        

        // Make a connection timeout the BluetoothSocket
        try 
        {
            // This is a blocking call and will only return on a successful connection or an exception
            log("Connecting socket ... BT discovering: " + BluetoothAdapter.getDefaultAdapter().isDiscovering());
            
            setState(SPPService.STATE_CONN_TIMEOUT); // next method might block
            mSocket.connect(); 
            setState(SPPService.STATE_CONNECTED);
            
            log("Socket connected! (retry: " + mRetry + ")");
        } 
        catch (IOException ioe)
        {
            String msg = ioe.getMessage();
            
            log("Connection failed (IOException): "+ msg);
            mSPPService.connectionFailed(this, msg);
            
            // Close the socket
            setState(SPPService.STATE_CONN_TIMEOUT); // next method might block
            closeBluetoothSocket();            
            setState(SPPService.STATE_FAILED);

            if (!mDoCancel)
            {            
        	if (msg.contains("discovery failed"))
        	{
        	    U.sleep(2000);
        	    mSPPService.scheduleRetry(this);
        	}
        	else 
        	{
        	    mSPPService.connectionFailed(this, ioe.getMessage());
        	}
            }
            return;
        }
        catch (Exception e) 
        {
            log("Connection failed (Exception): "+ e);
            mSPPService.connectionFailed(this, "" + e);
            
            // Close the socket
            setState(SPPService.STATE_CONN_TIMEOUT); // next method might block
            closeBluetoothSocket();    
            setState(SPPService.STATE_FAILED);
            
            mSPPService.connectionFailed(this, e.getMessage());
            return;
        }
    
        if (mDoCancel)
        {
            setState(SPPService.STATE_CONN_TIMEOUT); // next method might block
            closeBluetoothSocket();
            setState(SPPService.STATE_FAILED);
            
            mSPPService.connectionFailed(this, "Connection cancelled");
            return;
        }        

        // if we succeed until here, we're connected
        setState(SPPService.STATE_CONNECTED); 
        mSPPService.setState(this, SPPService.STATE_CONNECTED);
    
        setState(SPPService.STATE_CONN_TIMEOUT); // next method might block???
        if (!getSocketStreams())
        {
            closeBluetoothSocket();
            setState(SPPService.STATE_FAILED);
            mSPPService.setState(this, SPPService.STATE_FAILED);
            return;
        }
        
        // set this back to connected state to prevent isTimedout() from firing
        setState(SPPService.STATE_CONNECTED); 
        if (mLinewise)
        {
            communicateLinewise();
        }
        else
        {
            communicateBytewise();
        }
        
        if (mDoCancel)
	{
	    mSPPService.connectionLost("Connection cancelled");
	}

	closeBluetoothSocket();
	log("ConnectedThread terminating");
    }


    public void cancel() 
    {	    
        synchronized(this) 
        {
            try  
            {
        	mDoCancel = true;
        	this.interrupt();
        	if (null!=mSocket)
        	{
        	    (new SocketCloseThread(mSocket)).start();        	    
        	} 
        	this.interrupt();
        	this.join(100);
        	
        	if (this.isAlive())
        	{
        	    Log.e(TAG, "ConnectThread still alive after being interrupted...");
//        	    Throwable throwable = new RuntimeException("Thread cancelled");
//		    this.stop(throwable);
        	}
            } 
            catch (Exception e) 
            {
        	Log.e(TAG, "cancel(): ", e);
            }
    
            /* Do not attempt to close the bt socket here,since "cancle()" will run in the context 
             * of the calling thread and therefore might break the UI activity. e it up to the thread
             * itself to detect the cancellation and take appropriate action.  
             */
            /*
            try
            {
        	if (null==mSocket)
        	{
        	    Log.e(TAG, "no (more) socket timeout close");		    
        	}
        	else
        	{
        	    Log.e(TAG, "closing socket " + mSocket);
    
        	    //DisconnectThread dt = new DisconnectThread(mmSocket);
        	    //dt.start();
        	    closeBluetoothSocket();
        	}
            } 
            catch (Exception e) 
            {
        	Log.e(TAG, "close() of connect socket failed", e);
            }
            mSPPService.setState(this, SPPService.STATE_DISCONNECTED);
            */
        }
    }


    public void write(byte[] buffer)
    {
        try 
        {
            if (null!=mOutStream)
            {
        	mOutStream.write(buffer);
        	mOutStream.flush();
            }
        } 
        catch (IOException e) 
        {
            Log.e(TAG, "Exception during write", e);
        }
    }


    private void setState(int state)
    {
	this.mLastState       = state;
	this.mLastStateChange = Calendar.getInstance().getTimeInMillis();
    }


    private void logDeviceServices(BluetoothDevice device)
    {
        if (null==device) return;
        try
        {
            BluetoothClass btc = device.getBluetoothClass();
            if (null!=btc)
            {
        	int dc  = btc.getDeviceClass();
        	int mdc = btc.getMajorDeviceClass();
        	log(String.format("DC/MDC: 0x%04x/0x%04x", dc, mdc));
    
        	StringBuilder sb = new StringBuilder();
        	sb.append("Categories: ");
        	if (BTMagics.isPc(device)) sb.append("PC ");
        	if (BTMagics.isUncategorized(device)) sb.append("UNCAT ");
        	if (BTMagics.isUncategorized(device)) sb.append("HC05 ");
        	log(sb.toString());
    
        	sb = new StringBuilder();        	   
        	for (int i=0; i<=65536; i++)
        	{
        	    if (btc.hasService(i)) sb.append("srv" + i + " ");
        	}
        	if (btc.hasService(BluetoothClass.Service.AUDIO)) sb.append("audio ");
        	if (btc.hasService(BluetoothClass.Service.CAPTURE)) sb.append("capture ");
        	if (btc.hasService(BluetoothClass.Service.INFORMATION)) sb.append("info ");
        	if (btc.hasService(BluetoothClass.Service.LIMITED_DISCOVERABILITY)) sb.append("lim.disc ");
        	if (btc.hasService(BluetoothClass.Service.NETWORKING)) sb.append("netw ");
        	if (btc.hasService(BluetoothClass.Service.OBJECT_TRANSFER)) sb.append("obex ");
        	if (btc.hasService(BluetoothClass.Service.POSITIONING)) sb.append("posit ");
        	if (btc.hasService(BluetoothClass.Service.RENDER)) sb.append("render ");
        	if (btc.hasService(BluetoothClass.Service.TELEPHONY)) sb.append("tel ");
            }
            else
            {
        	log("no BT class for device " + device);
            }
        }
        catch (Exception e)
        {
            log("Exception: " + e);
        }
    }


    private void log(String msg)
    {
	synchronized (this)
	{
	    Log.d(TAG, msg);
	    
	    if (null==mSPPService) 
	    {
		return;
	    }
	    mSPPService.sendDebug(this, msg);
	}
    }

    private void communicateLinewise() 
    {
	log("linewise communicate starting");

	InterruptableBufferedReader br = new InterruptableBufferedReader(new InputStreamReader(mInputStream));

	// Keep listening timeout the InputStream while connected
	while (!mDoCancel && null!=mInputStream) 
	{
	    try 
	    {
		String line = br.readLine();
		//log("ConnectedThread: read line: " + line);
		if (null!=mSPPService)
		{
		    mSPPService.sendMessageString(this, line);
		}
		else
		{
		    Log.d(TAG, "Dropped (no handler): " + line);
		}
	    } 
	    catch (IOException e) 
	    {
		log("ConnectedThread: communicateLinewise: " + e);
		if (!mDoCancel)
		{
		    mSPPService.connectionLost(e.getMessage());
		}
		break;
	    }
	}

	try 
	{
	    br.close();
	}
	catch (IOException e)
	{
	    log("ConnectedThread: closing reader: " + e);
	}
    }


    private void communicateBytewise() 
    {
	log("byte wise communicate starting");

	byte[] buffer = new byte[1024];
	int bytes;

	// Keep listening timeout the InputStream while connected
	while (!mDoCancel) 
	{
	    try 
	    {
		// Read from the InputStream
		bytes = mInputStream.read(buffer);
		log("ConnectedThread: read " + bytes + " bytes)");

		if (null!=mSPPService)
		{
		    mSPPService.sendMessageBytes(this, SPPService.MESSAGE_READ, bytes, buffer);
		}
	    } 
	    catch (IOException e) 
	    {
		log("ConnectedThread: communicateBytewise: " + e);
		if (!mDoCancel)
		{
		    mSPPService.connectionLost(e.getMessage());
		}
		break;
	    }
	}
    }

    
    private BluetoothSocket createSocket(BluetoothDevice device)
    {	    
        BluetoothSocket tmp = null;	
    
        // Try timeout connect with HC-05 device
        if (BTMagics.isHC05(device) || BTMagics.isUncategorized(device))
        {
            log("ConnectThread: creating socket via createRfcommSocket");
            try 
            {
        	Method m = device.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
        	int port = 1;
        	tmp = (BluetoothSocket) m.invoke(mDevice, port);
            } 
            catch (Exception e) 
            {
        	log ("createRfcommSocket: Exception: " + e);
            } 
            if (null==tmp)
            {
        	Log.e(TAG, "ConnectThread: createRfcommSocket failed");
            }
        }
    
        // Try timeout connect timeout regular rfcomm device, e.g. a PC
        if (null==tmp)
        {
            // Get a BluetoothSocket for a connection with the given BluetoothDevice
            try 
            {
        	log("ConnectThread: creating socket via createRfcommSocketToServiceRecord");
        	tmp = device.createRfcommSocketToServiceRecord(SPP_UUID);
            } 
            catch (IOException e) 
            {
        	log ("createRfcommSocketToServiceRecord: Exception: " + e);
        	tmp = null;
            }
        }
    
        if (null==tmp)
        {
            log("ConnectThread: socket creation failed");
        }
    
        return tmp;
    }


    private boolean getSocketStreams()
    {
	BluetoothSocket socket = mSocket;
	InputStream tmpIn   = null;
	OutputStream tmpOut = null;

	// Get the BluetoothSocket input and output streams
	try 
	{
	    tmpIn  = socket.getInputStream();
	    tmpOut = socket.getOutputStream();
	} 
	catch (IOException e) 
	{
	    log("ConnectedThread: Exception getting socket streams: " + e);
	    return false;
	}

	mInputStream  = tmpIn;
	mOutStream = tmpOut;
	return true;
    }


    private void closeBluetoothSocket()
    {
        synchronized (this)
        {	            
            BluetoothSocket socket = mSocket;
            mSocket       = null;
            mInputStream  = null;
            mOutStream    = null;
    
            if (null==socket)
            {
        	Log.d(TAG, "closeBluetoothSocket: socket==null, returning");
        	return;
            }
    
            try 
            {
        	InputStream in = socket.getInputStream();
        	Log.d(TAG, "closeBluetoothSocket: closing input stream");
        	in.close();
            }
            catch (IOException ioex) 
            {
        	log("unable to close streams on BT socket: " + ioex);
            }
            try 
            {
        	OutputStream out = socket.getOutputStream();
        	Log.d(TAG, "closeBluetoothSocket: closing output stream");
        	out.flush();
        	out.close();
            }
            catch (IOException ioex) 
            {
        	log("unable to close streams on BT socket: " + ioex);
            }
            try 
            {
        	Log.d(TAG, "closeBluetoothSocket: closing socket");
        	socket.close();
        	Log.d(TAG, "closeBluetoothSocket: socket closed");
            }
            catch (IOException ioex) 
            {
        	log("unable to close BT socket: " + ioex);
            }
        }
    }


    // a replacement for the "normal" BufferedReader, however this one is aware 
    // of whether the connect thread was cancelled
    private class InterruptableBufferedReader
    {
        private InputStreamReader reader;
    
        InterruptableBufferedReader(InputStreamReader inputStreamReader)
        {
            this.reader = inputStreamReader;
        }
    
        String readLine() throws IOException
        {
            StringBuilder sb = new StringBuilder();
            do
            {
        	// TODO: optimize this: read in chunks rather than char by char
        	int i = reader.read();
        	if (i<0) return null; // EOF
    
        	char c = (char)i;
        	switch (c)
        	{
        	case '\r':
        	case '\n':
        	    return sb.toString();
        	default:
        	    sb.append(c);
        	}		
            }
            while (!mDoCancel);
    
            return null;
        }
    
        void close() throws IOException
        {
            reader.skip(100000);
            reader.close();	    
        }
    }

    private static long now()
    {
	return Calendar.getInstance().getTimeInMillis();
    }
    

    private final String TAG = ConnectThread.class.getSimpleName();

    private BluetoothSocket  mSocket;
    private OutputStream     mOutStream;
    private InputStream      mInputStream;
    private volatile boolean mDoCancel;
    private boolean          mLinewise;
    private BluetoothDevice  mDevice;
    private SPPService       mSPPService;
    private int              mLastState;
    private long             mLastStateChange;
    private int 	     mRetry;

    
    public int getRetry()
    {
	return mRetry;
    }

    public boolean getLinewise()
    {
	return mLinewise;
    }

} 
// ConnectThread