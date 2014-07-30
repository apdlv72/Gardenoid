package com.apdlv.gardenoid;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.apdlv.utils.U;

import fi.iki.elonen.NanoHTTPD;

public class GardenoidActivity extends Activity implements OnCheckedChangeListener
{
    public static final String TAG = GardenoidActivity.class.getSimpleName();

    class HttpServiceConnection extends BroadcastReceiver implements ServiceConnection
    {
	private GardenoidService mService;

	public void onServiceConnected(ComponentName className, IBinder service) 
	{
	    // We've bound to LocalService, cast the IBinder and get LocalService instance
	    GardenoidService.Binder binder = (GardenoidService.Binder) service;
	    mService = binder.getService();
	    Log.v(TAG, "Got BT frotect serial service " + mService);
	    mService.setHandler(mHandler);
	    //Log.v(TAG, "Registered handler with BT service ");

	    mHttpServer = mService.getHttpServer();
	    mToggleButtonOnOff.setOnCheckedChangeListener(null);
	    mToggleButtonOnOff.setChecked(mHttpServer.isAlive());
	    mToggleButtonOnOff.setOnCheckedChangeListener(GardenoidActivity.this);

	    updateServiceLink();
	}


	public void onServiceDisconnected(ComponentName arg0) 
	{
	    synchronized (this)
	    {
		if (null!=mService)
		{
		    //mService.removeHandler(mHandler);
		    mService = null;
		}
	    }
	}


	public void unbind(Context applicationContext)
	{
	    if (null!=mService)
	    {
		mService.removeHandler(mHandler);
		mService = null;
	    }		
	    try
	    {
		applicationContext.unbindService(this);
	    }
	    catch (Exception e)
	    {
		Log.e(TAG, "unbind: " + e);
	    }
	}

	
	public void updateServiceLink()
	{	
	    if (null!=mHttpServer && mHttpServer.isAlive())
	    {
		if (null==mService)
		{
		    mTextViewLog.setText("Service unconnected");
		    return;
		}
		String addr = mService.getAddress();
		int    port = mHttpServer.getListeningPort();
		String link = "http://" + addr + ":" + port;
		mTextViewLog.setText(link);
	    }
	    else
	    {
		mTextViewLog.setText("Server not active");
	    }	
	}


	@Override
        public void onReceive(Context arg0, Intent arg1)
        {
	    try
	    {
		Log.d(TAG, "Connectivity changed");            
		updateServiceLink();
	    }
	    catch (Exception e)
	    {
		Log.e(TAG, "HttpServiceConnection.onReceive: " + U.asString(e));		
	    }
        }
    }
    
    
    protected HttpServiceConnection mConnection = new HttpServiceConnection();
    private AssetManager mAssetManager; 


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
	super.onCreate(savedInstanceState);
	Log.e(TAG, "******** CALLED: onCreate ********");

	setContentView(R.layout.activity_gardenoid);
	
//	Intent intent = new Intent("BIND", null, this, GardenoidService.class);
//	bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
	
	mToggleButtonOnOff = (ToggleButton) findViewById(R.id.toggleButtonOnOff);
	mToggleButtonOnOff.setOnCheckedChangeListener(this);
	
	mTextViewLog = (TextView) findViewById(R.id.textViewLog);
	
	// register HttpServiceConnection to receive events when network configuration chenges
	// such taht it will be able to update the service link displayed 
	IntentFilter intentFilter = new IntentFilter("android.net.wifi.WIFI_AP_STATE_CHANGED");
	intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
	registerReceiver(mConnection, intentFilter);
	
	
	SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
	boolean firstRun = p.getBoolean("PREFERENCE_FIRST_RUN", true);
	// commit AFTER we copied pages
	//p.edit().putBoolean("PREFERENCE_FIRST_RUN", false).commit();
	
	mAssetManager = getAssets();
	try
	{	    
	    //if (firstRun)
	    {
		copyWebPagesToSDCard(TemplateEngine.DIR_PREFIX_WEBPAGES);
		p.edit().putBoolean("PREFERENCE_FIRST_RUN", false).commit();
	    }
	}
	catch (Exception e)
	{
	    Log.e(TAG, "copyWebPagesToSDCard: " + e);
	}
	
	Log.e(TAG, "******** DONE: onCreate ********");
    }
    
    
    
    private void copyWebPagesToSDCard(String prefix) throws IOException
    {
	File dir = getExternalFilesDir(prefix);
	if (null==dir) 
	{
	    throw new RuntimeException("Cannot access external dir (SD card locked?)");
	}
	
	String[] list = mAssetManager.list(prefix);
	for (String path : list)
	{
	    String fullPath = prefix + "/" + path;
	    InputStream is = null;
	    try
	    {
		is = mAssetManager.open(fullPath);
	    }
	    catch (FileNotFoundException e) 
	    {
		// resource is a direcory
		Log.e(TAG, "copyWebPagesToSDCard: " + fullPath + ": " + e + ", assuming its a directory");
		copyWebPagesToSDCard(fullPath);
		continue;
	    }
		
	    dir.mkdir();
	    File file = new File(dir, path);
	    FileOutputStream os = new FileOutputStream(file);
	    copyStream(is, os);
	    os.close();
	    is.close();
	}
    }


    private void copyStream(InputStream input, FileOutputStream output) throws IOException
    {
	byte [] buffer = new byte[256];
	int bytesRead = 0;
	while((bytesRead = input.read(buffer)) != -1)
	{
	    output.write(buffer, 0, bytesRead);
	}    
    }


    private Handler mHandler = new Handler()
    {
	@Override
	public void handleMessage(Message msg)
	{
	    // TODO Auto-generated method stub
	    super.handleMessage(msg);
	    Log.v(TAG, "handleMessage: " + msg);
	    
	    switch (msg.what)
	    {
	    case GardenoidService.MSG_STATUS:
		Log.v(TAG, "MSG_STATUS: " + msg.replyTo);
		break;
	    case GardenoidService.MSG_SRVADDR:
		String addr = (String) msg.obj;
		int port = null==mHttpServer ? -1 : mHttpServer.getListeningPort();
		String link = "http://" + addr + ":" + port; 
		mTextViewLog.setText(link);
	    }
	}
    };
    
    
    @Override
    protected void onStart() 
    {
	super.onStart();
	Log.e(TAG, "******** CALLED: onStart ********");
	
	// Start Service On Boot Start Up
	Intent intent1 = new Intent(this, GardenoidService.class);
	startService(intent1);

	Intent intent2 = new Intent("BIND", null, this, GardenoidService.class);
	bindService(intent2, mConnection, Context.BIND_AUTO_CREATE);
	
	Log.e(TAG, "******** DONE: onStart ********");
    };

    
    @Override
    protected void onStop() 
    {
	super.onStop();
	Log.e(TAG, "******** CALLED: onStop ********");
	Log.v(TAG, "onStop: Unbinding from service");
	if (null!=mConnection) 
	{
	    mConnection.unbind(this);
	}
	Log.e(TAG, "******** DONE: onStop ********");
    };
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
	// Inflate the menu; this adds items to the action bar if it is present.
	getMenuInflater().inflate(R.menu.gardenoid, menu);
	return true;
    }


    @Override
    public void onCheckedChanged(CompoundButton button, boolean isChecked)
    {
	try
	{
	    if (mToggleButtonOnOff==button)
	    {
		if (null!=mConnection && null!=mConnection.mService)
		{
		    if (isChecked)
		    {
			mConnection.mService.startHttp();
		    }
		    else
		    {
			mConnection.mService.stopHttp();
		    }
		}
	    }
	}
	catch (Exception e)
	{
	    Log.e(TAG, "onCheckedChanged: " + e);
	}
    }

    
    
    private NanoHTTPD mHttpServer;
    private TextView mTextViewLog;
    private ToggleButton mToggleButtonOnOff;

}