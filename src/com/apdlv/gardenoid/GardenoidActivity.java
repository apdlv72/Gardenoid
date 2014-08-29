package com.apdlv.gardenoid;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.apdlv.utils.U;

import fi.iki.elonen.NanoHTTPD_SSL;

public class GardenoidActivity extends Activity implements OnCheckedChangeListener, OnClickListener
{
    private static final String PREFERENCE_RAN_BEFORE = "PREFERENCE_RAN_BEFORE";

    public static final String TAG = GardenoidActivity.class.getSimpleName();
    
    private long mStartTime = U.millis();
	
    private String mServerProtocol = "https://";
    private String mServerAddr     = GardenoidService.getAddress();
    private int    mServerPort     = 8080;
    private String mServiceLink    = "undefined";

    
    private String makeLocalUrl()
    {
	try
	{	    
	    String sess = null==mConnection.mService ? "none" : mConnection.mService.createSessionInternally();
	    String link  = String.format("%s127.0.0.1:%d/mobile.html?session=%s&version=%x", mServerProtocol, mServerPort, sess, mStartTime);
	    return link;
	}
	catch (Exception e)
	{
	    System.err.println("makeLocalUrl: " + e);
	}
	return "https://127.0.0.1:" + GardenoidService.PORT_HTTPS;
    }

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
	    if (null!=mToggleButtonOnOff)
	    {
		mToggleButtonOnOff.setOnCheckedChangeListener(null);
		mToggleButtonOnOff.setChecked(mHttpServer.isAlive());
		mToggleButtonOnOff.setOnCheckedChangeListener(GardenoidActivity.this);
	    }

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
	    mServerAddr = GardenoidService.getAddress();
	    if (null!=mHttpServer && mHttpServer.isAlive())
	    {
		if (null==mService)
		{
		    mServiceLink = "Service unconnected";
		    updateTitle();
		    return;
		}
				
		mServiceLink = mServerProtocol + mServerAddr + ":" + mServerPort;
		
		String link  = makeLocalUrl();
		mWebView.loadUrl(link);
	    }
	    else
	    {
		mServiceLink = "Service stopped";
		mWebView.loadData("Service stopped", "text/html", "utf-8");
	    }	
	    updateTitle();
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

    private Button mButtonServiceStart;
    private Button mButtonServiceStop;

    private WebView mWebView;

    //private TextView mTextViewMask; 


    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
	super.onCreate(savedInstanceState);
	Log.e(TAG, "******** CALLED: onCreate ********");

	setContentView(R.layout.activity_gardenoid);
	
//	Intent intent = new Intent("BIND", null, this, GardenoidService.class);
//	bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
	
	mToggleButtonOnOff = null; //(ToggleButton) findViewById(R.id.toggleButtonOnOff);
	if (null!=mToggleButtonOnOff)
	{
	    mToggleButtonOnOff.setOnCheckedChangeListener(this);
	}
	
	/*
	mButtonServiceStart = (Button)findViewById(R.id.buttonServiceStart);
	mButtonServiceStop  = (Button)findViewById(R.id.buttonServiceStop);	
	mButtonServiceStart.setOnClickListener(this);
	mButtonServiceStop.setOnClickListener(this);
	*/
	
	//mTextViewLog = (TextView) findViewById(R.id.textViewLog);
	//mTextViewMask = (TextView) findViewById(R.id.textViewMask);

	mWebView = (WebView) findViewById(R.id.webView1);
	
	// enable java script
	WebSettings webSettings = mWebView.getSettings();
	webSettings.setJavaScriptEnabled(true);
	
	// make links open in the web view not in default browser:
	mWebView.setWebViewClient(new WebViewClient() 
	{
	    @Override
	    public boolean shouldOverrideUrlLoading(WebView view, String url)
	    {
	      view.loadUrl(url);
	      return true;
	    }
	    @Override
	    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error)
	    {
	        //super.onReceivedSslError(view, handler, error);
		handler.proceed();
	    }
	});
	
	// make alert() work
	mWebView.setWebChromeClient(new WebChromeClient() {
		@Override
		public boolean onJsAlert(WebView view, String url, String message, android.webkit.JsResult result) {
    		    //Required functionality here
    		    return super.onJsAlert(view, url, message, result);
	       }
		@Override
		public boolean onJsConfirm(WebView view, String url, String message, JsResult result)
		{
		    return super.onJsConfirm(view, url, message, result);
		}
		@Override
		public void onCloseWindow(WebView window)
		{
		    String link  = makeLocalUrl();
		    window.loadUrl(link);
		}
	});
	
	// register HttpServiceConnection to receive events when network configuration chenges
	// such taht it will be able to update the service link displayed 
	IntentFilter intentFilter = new IntentFilter("android.net.wifi.WIFI_AP_STATE_CHANGED");
	intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
	registerReceiver(mConnection, intentFilter);
	
	mAssetManager = getAssets();
	// Reading from assets seems to be even faster than sdcard.
	// So no need to copy pages there. Makes things mor complicated
	// e.g. need to delete the pages after updates.
	/*
	try
	{	    
	    SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
	    boolean ranBefore = p.getBoolean(PREFERENCE_RAN_BEFORE, false);
	    //if (!ranBefore) 
	    {
		File externalDir = getExternalFilesDir(TemplateEngine.DIR_PREFIX_CHECKSUMS);
		TemplateEngine te = new TemplateEngine(mAssetManager, externalDir);

		// during development:
		boolean needUpdate = true;	    

		long s1,s2,e1,e2, sum1=0, sum2=0;
		String md5Asset = null;
		String md5Ext   = null;
		
		for (int i=0; i<100; i++)
		{
		    s1 = U.millis();
		    md5Asset = te.getAsset(TemplateEngine.DIR_PREFIX_CHECKSUMS + "/webpages.md5");
		    //md5Asset = te.getAsset(TemplateEngine.DIR_PREFIX_WEBPAGES + "/js/gardenoid.js");
		    e1 = U.millis();
		    sum1 += (e1-s1);

		    s2 = U.millis();
		    md5Ext = te.readExternalFile("webpages.md5");
		    //md5Ext = te.readExternalFile("../webpages/js/gardenoid.js");
		    e2 = U.millis();
		    sum2 += (e2-s2);
		}
		System.out.println("asset: " + sum1 + "ms, external: " + sum2 + " ms");		
		
		if (null!=md5Ext && md5Ext.matches(md5Asset))
		{
		    needUpdate = false;
		}	    

		// prevent "dead code" warning:
		boolean doIt = needUpdate; // && firstRun;
		if (doIt)
		{
		    copyWebPagesToSDCard(TemplateEngine.DIR_PREFIX_WEBPAGES);
		    copyWebPagesToSDCard(TemplateEngine.DIR_PREFIX_CHECKSUMS);
		    // commit AFTER we copied pages
		}

		p.edit().putBoolean(PREFERENCE_RAN_BEFORE, true).commit();
	    }
	}
	catch (Exception e)
	{
	    Log.e(TAG, "copyWebPagesToSDCard: " + e);
	}
	*/
	Log.e(TAG, "******** DONE: onCreate ********");
    }
    
    /*    
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
    */

    /*
    private void copyStream(InputStream input, FileOutputStream output) throws IOException
    {
	byte [] buffer = new byte[256];
	int bytesRead = 0;
	while((bytesRead = input.read(buffer)) != -1)
	{
	    output.write(buffer, 0, bytesRead);
	}    
    }
    */

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
		mServerProtocol = (String) msg.obj;
		//mServerAddr     = msg.arg1;
		mServerPort     = msg.arg2;
		//int port = null==mHttpServer ? -1 : mHttpServer.getListeningPort();
		mServiceLink = mServerProtocol + mServerAddr + ":" + mServerPort; 
		updateTitle();
		break;
	    case GardenoidService.MSG_MASK:
		mStrandMask = (Integer)msg.obj;
		updateTitle();
		break;
	    }
	}
    };
    
    private int mStrandMask = 0;
    
    private void updateTitle()
    {
	String sMask = "";
	
	if (mStrandMask>0)
	{
	    sMask = Integer.toBinaryString(mStrandMask);
	    while (sMask.length()<8) sMask="0"+sMask;
	}
	
	String title = sMask + " Gardenoid " + " " + mServiceLink.replaceAll("http://", "");
	setTitle(title);	
    }
    
    @Override
    protected void onStart() 
    {
	super.onStart();
	Log.e(TAG, "******** CALLED: onStart ********");
	Intent startIntent = getIntent();
	
	String action = (null==startIntent) ? "none(no intent)" : startIntent.getAction();
	
	if (GardenoidService.ACTION_NOTIFICATION.equals(action))
	{
	    Log.d(TAG, "Notification clicked. No need to start service again.");
	}	
	else	   
	{	
	    Toast.makeText(this, "GardenoidActivity.onStart: action=" + action, Toast.LENGTH_LONG).show();
	    // Start service on start up
	    startGardenoidService();
	}

	bindToGardenoidService();
	
	Log.e(TAG, "******** DONE: onStart ********");
    }



    /**
     * 
     */
    private void bindToGardenoidService()
    {
	Intent intent2 = new Intent("BIND", null, this, GardenoidService.class);
	bindService(intent2, mConnection, Context.BIND_AUTO_CREATE);
    }



    /**
     * 
     */
    private void startGardenoidService()
    {
	Intent intent1 = new Intent(this, GardenoidService.class);
	intent1.setAction("GardenoidActivity.start");
	startService(intent1);
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

    
    
    private NanoHTTPD_SSL mHttpServer;
    //private TextView mTextViewLog;
    private ToggleButton mToggleButtonOnOff;


    @Override
    public void onClick(View v)
    {
	if (v==mButtonServiceStart)
	{
	}
	else if (v==mButtonServiceStop)
	{
	    stopBackgroundService();
	}
    }

    @Override
    public boolean onMenuItemSelected(int featureId, android.view.MenuItem item) 
    {
	if (item.getItemId()==R.id.menuStartService)
	{
	    startBackgroundService();
	}
	else if (item.getItemId()==R.id.menuStopService)
	{
	    stopBackgroundService();
	} 
	else if (item.getItemId()==R.id.menuOpenBrowser)
	{
	    if (null!=mHttpServer && mHttpServer.isAlive())
	    {
		String link = makeLocalUrl();
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(link));
		startActivity(intent);
	    }
	} 
	else if (item.getItemId()==R.id.menuReload)
	{
	    String link  = makeLocalUrl();
	    mWebView.loadUrl(link);
	    //mWebView.reload();
	} 
	return false;
    };

    private void stopBackgroundService()
    {
	GardenoidService s = null==mConnection ? null : mConnection.mService;
	if (null!=s)
	{
	mConnection.unbind(this);
	//unbindService(mConnection);		
	
	Intent i = new Intent("User.stop");
	s.stopService(i);	    		
	}
	mConnection.updateServiceLink();
    }


    private void startBackgroundService()
    {
	startGardenoidService();
	bindToGardenoidService();	   
	mConnection.updateServiceLink();
    }

}
