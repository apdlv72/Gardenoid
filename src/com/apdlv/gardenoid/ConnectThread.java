package com.apdlv.gardenoid;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.apdlv.ilibaba.bt.BTMagics;
import com.apdlv.ilibaba.bt.SPPService;
import com.apdlv.utils.U;

//class TimeoutThread extends Thread
//{
//    private final String TAG = TimeoutThread.class.getSimpleName();
// 
//    private ConnectThread connectThread;
//
//    TimeoutThread(ConnectThread connectThread)
//    {
//	this.connectThread = connectThread;
//    }
//
//    public void run()
//    {
//	long start = now();
//	
//	while (now()-start<3*ConnectThread.THRESHOLD)
//	{
//	    if (connectThread.isConnected())
//	    {
//		Log.e(TAG, "ConnectThread connected, bailing out");
//		return;
//	    }
//	    else if (!connectThread.isAlive())
//	    {
//		Log.e(TAG, "ConnectThread dead, bailing out");
//		return;
//	    }
//	    else if (connectThread.isTimedout())
//	    {
//		Log.e(TAG, "ConnectThread timed out, calling handleTimeout");
//		connectThread.handleTimeout();
//	    }
//	    
//	    try { Thread.sleep(100); } catch (Exception e) {}
//	}
//	   
//    }
//
//    private static long now()
//    {
//	return Calendar.getInstance().getTimeInMillis();
//    }
//}

class ConnectThread extends Thread 
{
    // The standard Serial Port Profile UUID
    public static final UUID SPP_UUID =  UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    
    public final static long THRESHOLD = 5*1000L; // seconds

    public static boolean DUMP_SERVICES = false;


    public ConnectThread(GardenoidService gardenoidService, boolean linewise)
    {
	this(gardenoidService, linewise, 0);
    }
    
    
    public ConnectThread(GardenoidService gardenoidService, boolean linewise, int retry) 
    {
//	if (DUMP_SERVICES)
//	{
//	    logDeviceServices(device);
//	}
	super.setName(ConnectThread.class.getSimpleName());
	this.mRetry             = retry;
	this.mDeviceToConnectTo = null;
	this.mDeviceConnected   = null;
	this.mGardenoidService  = gardenoidService;
	this.mLinewise          = linewise;
	this.mCancelCommunication = false;
	setState(SPPService.STATE_NONE);
    }
    
    
    public boolean connectTo(BluetoothDevice device)
    {
	return connectTo(device, true /* do wait */);
    }
    
    public boolean connectTo(BluetoothDevice device, boolean wait)
    {
	if (!disconnect()) 
	{ 
	    return false; 
	}

	mDeviceToConnectTo=device;
	
	return wait ? waitUntilConnectedTo(mDeviceToConnectTo, "connect", 40) : true;
    }
    
    
    // @returns true when connection was successfully closed
    public boolean disconnect()
    {
	mDeviceToConnectTo=null;
	cancel();
	if (null!=mDeviceConnected)
	{
	    this.interrupt();
	}
	closeBluetoothSocket();
	return waitUntilConnectedTo(null, "disconnect", 40);
    }


    private boolean waitUntilConnectedTo(BluetoothDevice device, String title, int retries)
    {
	for (; retries>0; retries--)
	{
	    if (device==mDeviceConnected)
		break;
		
	    U.mSleep(250);
	    if (0==retries%4) 	    
	    {
		String name = null;
		try { name = mDeviceConnected.getName(); } catch (Exception e) {}
		Log.d(TAG, "" + title + ": waiting for device " + name);
	    }
	}
	return mDeviceConnected==device;
    }
    

    public boolean isConnected()
    {
	return null!=mDeviceConnected && mLastState==SPPService.STATE_CONNECTED;
    }
    

    public boolean shouldConnect()
    {
	return null!=mDeviceToConnectTo;
    }
    

    public void handleTimeout()
    {	
	// save this value, since cancle() below will set it for sure
	boolean wasCancelled = mCancelCommunication;
	
	cancel();
	
	if (!wasCancelled)
	{
	    Log.d(TAG, "handleTimeout: not cancelled, calling scheduleRetry");
	    mGardenoidService.scheduleRetry(this);
	}
	else
	{
	    Log.d(TAG, "handleTimeout: WAS cancelled, NOT calling scheduleRetry");	    
	}
	setState(SPPService.STATE_CONN_TIMEOUT);
    }

    public BluetoothDevice getDeviceConnected()
    {
	return mDeviceConnected;
    }

    public boolean isTimedout()
    {	
	return mLastState==SPPService.STATE_CONN_TIMEOUT && now()-mLastStateChange>THRESHOLD; 
    }

    
    public void terminate()
    {
	mDoTerminate = true;
	this.interrupt();
    }

    public void run() 
    {
	Log.e(TAG, "Entered main loop");
	while (!mDoTerminate)
	{
	    long waitTime = 0; 
	    while (null==mDeviceToConnectTo)
	    {
		U.mSleep(250);
		waitTime++;
		if (0==waitTime%40) 
		    System.err.println("Waiting for a device address to connect to. waitTime=" + waitTime);
	    }
	    
	    int rc = connectAndCommunicate();
	    System.err.println("run: connectAndCommunicate returned rc=" + rc);
	    
	    // avoid busy loop  unless  
	    if (SPPService.STATE_LOST==rc)
	    {
		U.mSleep(250);
	    }
	    else
	    {
		U.mSleep(1000);
	    }
	    mDeviceConnected = null;
	}
    }
    

    public int connectAndCommunicate()
    {
	mDeviceConnected = null;
	// socket.connect() may block forever, therefore start a thread that will monitor
	// whether the connection attempt takes too long. 
	//	TimeoutThread toThread = new TimeoutThread(this);
	//	toThread.start();

	// Register this thread with the service to allow detection of threads that
	// stalled while connecting to the remote device or even while closing the socket.
	mGardenoidService.registerConnectThread(this);

	log("BEGIN mConnectThread");
	setName("ConnectThread");
	setState(SPPService.STATE_CONNECTING);

	// Always cancel discovery because it will slow down a connection
	Log.d(TAG, "Cancelling discovery ...");
	if (!mGardenoidService.cancelDiscovery(this))
	{
	    setState(SPPService.STATE_FAILED);
	    return SPPService.STATE_FAILED;
	}

	if (mDoTerminate)
	{        
	    setState(SPPService.STATE_CANCELLED);
	    return SPPService.STATE_CANCELLED;
	}

	try
	{
	    log("Creating bluetooth socket ...");
	    setState(SPPService.STATE_CONN_TIMEOUT); // next method might block
	    mSocket = createSocket(mDeviceToConnectTo);
	    setState(SPPService.STATE_CONNECTING);            
	}
	catch (Exception e)
	{
	    log("Exception: "+ e);
	}

	if (null==mSocket) // connect was not successful
	{
	    log("Socket creation failed");
	    mGardenoidService.onConnectionFailed(this, "Socket creation failed", mDeviceToConnectTo);		
	    setState(SPPService.STATE_FAILED);            
	    return SPPService.STATE_FAILED;		
	}

	if (mDoTerminate)
	{
	    setState(SPPService.STATE_CONN_TIMEOUT); // next method might block
	    closeBluetoothSocket();
	    setState(SPPService.STATE_CANCELLED);
	    mGardenoidService.onConnectionFailed(this, "Connection cancelled", mDeviceToConnectTo);
	    return SPPService.STATE_CANCELLED;
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
	    mDeviceConnected = mDeviceToConnectTo;
	} 
	catch (IOException ioe)
	{
	    String msg = ioe.getMessage();

	    log("Connection failed (IOException): "+ msg);
	    mGardenoidService.onConnectionFailed(this, msg, mDeviceToConnectTo);

	    // Close the socket
	    setState(SPPService.STATE_CONN_TIMEOUT); // next method might block
	    closeBluetoothSocket();            
	    setState(SPPService.STATE_FAILED);

	    if (!mCancelCommunication)
	    {            
		if (msg.contains("discovery failed"))
		{
		    U.sleep(4);
		    mGardenoidService.scheduleRetry(this);
		}
		else 
		{
		    mGardenoidService.onConnectionFailed(this, ioe.getMessage(), mDeviceToConnectTo);
		}
	    }
	    return SPPService.STATE_FAILED;
	}
	catch (Exception e) 
	{
	    log("Connection failed (Exception): "+ e);
	    mGardenoidService.onConnectionFailed(this, "" + e, mDeviceToConnectTo);

	    // Close the socket
	    setState(SPPService.STATE_CONN_TIMEOUT); // next method might block
	    closeBluetoothSocket();    
	    setState(SPPService.STATE_FAILED);

	    mGardenoidService.onConnectionFailed(this, e.getMessage(), mDeviceToConnectTo);
	    return SPPService.STATE_FAILED;
	}

	if (mDoTerminate)
	{
	    setState(SPPService.STATE_CONN_TIMEOUT); // next method might block
	    closeBluetoothSocket();
	    setState(SPPService.STATE_FAILED);

	    mGardenoidService.onConnectionFailed(this, "Connection cancelled", mDeviceToConnectTo);
	    return SPPService.STATE_FAILED;
	}        

	// if we succeed until here, we're connected
	setState(SPPService.STATE_CONNECTED); 
	mGardenoidService.setState(this, SPPService.STATE_CONNECTED);

	setState(SPPService.STATE_CONN_TIMEOUT); // next method might block???
	if (!getSocketStreams())
	{
	    closeBluetoothSocket();
	    setState(SPPService.STATE_FAILED);
	    mGardenoidService.setState(this, SPPService.STATE_FAILED);
	    return SPPService.STATE_FAILED;
	}

	// set this back to connected state to prevent isTimedout() from firing
	setState(SPPService.STATE_CONNECTED); 
	mCancelCommunication = false;
	mGardenoidService.onConnectionEstablished(this, mDeviceToConnectTo);
	if (mLinewise)
	{
	    communicateLinewise();
	    System.err.println("communicateLinewise: returned");
	}
	else
	{
	    communicateBytewise();
	}

	if (mCancelCommunication)
	{
	    mGardenoidService.connectionLost("Connection cancelled");
	}

	closeBluetoothSocket();
	return SPPService.STATE_LOST;
    }
	


    public void cancel() 
    {	    
        synchronized(this) 
        {
            try  
            {
        	mCancelCommunication = true;
        	this.interrupt();
//        	if (null!=mSocket)
//        	{
//        	    (new SocketCloseThread(mSocket)).start();        	    
//        	} 
        	//this.interrupt();
        	//this.join(100);
        	
        	//if (this.isAlive())
        	//{
//        	    Log.e(TAG, "ConnectThread still alive after being interrupted...");
//        	    Throwable throwable = new RuntimeException("Thread cancelled");
//		    this.stop(throwable);
        	//}
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

    /*
    private MyJson logDeviceServices(BluetoothDevice device)
    {
        if (null==device) return null;
        try
        {
            MyJson j = json("name", device.getName()).add("addr", device.getAddress());
            BluetoothClass btc = device.getBluetoothClass();
            
            if (null!=btc)
            {
        	int dc  = btc.getDeviceClass();
        	int mdc = btc.getMajorDeviceClass();
        	
        	j.add("dc",dc).add("mdc",mdc);    
            }

            JArray a = j.array("categories");        	
            if (BTMagics.isPc(device)) a.add("PC");
            if (BTMagics.isUncategorized(device)) a.add("UNCAT");
            if (BTMagics.isHC05(device)) a.add("HC05");
            a.done();
    
            a = j.array("services");        
            for (int i=0; i<=65536; i++)
            {
        	if (btc.hasService(i)) a.add(i);
            }
            a.done();

            a = j.array("capibilities");
            if (btc.hasService(BluetoothClass.Service.AUDIO)) a.add("audio");
            if (btc.hasService(BluetoothClass.Service.CAPTURE)) a.add("capture");
            if (btc.hasService(BluetoothClass.Service.INFORMATION)) a.add("info");
            if (btc.hasService(BluetoothClass.Service.LIMITED_DISCOVERABILITY)) a.add("lim.disc");
            if (btc.hasService(BluetoothClass.Service.NETWORKING)) a.add("netw");
            if (btc.hasService(BluetoothClass.Service.OBJECT_TRANSFER)) a.add("obex");
            if (btc.hasService(BluetoothClass.Service.POSITIONING)) a.add("posit");
            if (btc.hasService(BluetoothClass.Service.RENDER)) a.add("render");
            if (btc.hasService(BluetoothClass.Service.TELEPHONY)) a.add("tel");
            a.done();
        	
            return j;
        }
        catch (Exception e)
        {
            log("Exception: " + e);
            return json("exception", ""+e);
        }
    }
    */

    private void log(String msg)
    {
	synchronized (this)
	{
	    Log.d(TAG, msg);
	    
	    if (null==mGardenoidService) 
	    {
		return;
	    }
	    mGardenoidService.sendDebug(this, msg);
	}
    }

    private void communicateLinewise() 
    {
	log("linewise communicate starting");

	InterruptableBufferedReader br = new InterruptableBufferedReader(new InputStreamReader(mInputStream));

	// Keep listening timeout the InputStream while connected
	while (!mCancelCommunication && null!=mInputStream  && mDeviceConnected==mDeviceToConnectTo) 
	{
	    try 
	    {
		String line = br.readLine();
		//log("ConnectedThread: read line: " + line);
		if (null!=mGardenoidService)
		{
		    if (null!=line)
		    {
			mGardenoidService.onBluetoothCommandReceived(this, line);
		    }
		}
		else
		{
		    Log.d(TAG, "Dropped (no handler): " + line);
		}
	    } 
	    catch (IOException e) 
	    {
		log("ConnectedThread: communicateLinewise: " + e);
		if (!mCancelCommunication)
		{
		    mGardenoidService.connectionLost(e.getMessage());
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
	while (!mCancelCommunication) 
	{
	    try 
	    {
		// Read from the InputStream
		bytes = mInputStream.read(buffer);
		log("ConnectedThread: read " + bytes + " bytes)");

		if (null!=mGardenoidService)
		{
		    mGardenoidService.sendMessageBytes(this, SPPService.MESSAGE_READ, bytes, buffer);
		}
	    } 
	    catch (IOException e) 
	    {
		log("ConnectedThread: communicateBytewise: " + e);
		if (!mCancelCommunication)
		{
		    mGardenoidService.connectionLost(e.getMessage());
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
        	tmp = (BluetoothSocket) m.invoke(mDeviceConnected, port);
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
            while (!mCancelCommunication && mDeviceConnected==mDeviceToConnectTo);
            
            Log.e(TAG, "InterruptableBufferedReader: mCancelCommunication=" + mCancelCommunication);
    
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

    private volatile boolean mDoTerminate = false;
    private volatile BluetoothDevice mDeviceToConnectTo;
    private          BluetoothDevice mDeviceConnected;
    private BluetoothSocket  mSocket;
    private OutputStream     mOutStream;
    private InputStream      mInputStream;
    private volatile boolean mCancelCommunication;
    private boolean          mLinewise;
    private GardenoidService mGardenoidService;
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


//class SocketCloseThread extends Thread
//{
//    private final String TAG = SocketCloseThread.class.getSimpleName();
//
//    private BluetoothSocket mSocket;
//
//    SocketCloseThread(BluetoothSocket socket)
//    {
//	this.mSocket = socket;
//    }
//
//    public void run()
//    {
//	try
//	{
//	    Log.e(TAG, "Closing BT socket " + mSocket + " (in the background)");
//	    mSocket.close();
//	    Log.e(TAG, "BT socket " + mSocket + " closed");
//	}
//	catch (Exception e)
//	{
//	    Log.e(TAG, "" + e);
//	}
//    }
//}