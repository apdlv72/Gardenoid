package com.apdlv.gardenoid;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;

import org.apache.http.client.protocol.RequestAddCookies;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.apdlv.gardenoid.db.DAO;
import com.apdlv.gardenoid.db.Event;
import com.apdlv.gardenoid.db.Schedule;
import com.apdlv.gardenoid.db.Weather;
import com.apdlv.utils.U;
import com.apdlv.yahooweather.Forecast;
import com.apdlv.yahooweather.ForecastProvider;
import com.apdlv.yahooweather.WeatherConditions;

import fi.iki.elonen.NanoHTTPD_SSL;
import fi.iki.elonen.NanoHTTPD_SSL.Response.Status;
//import fi.iki.elonen.NanoHTTPD_SSL;
//import com.apdlv.gardenoid.MyJson.JArray;



/**
 * A service running in the background and implementing kind of a HTTP to bluetooth gateway.
 * The HTTP server allows to send commands to and receive replies from the BT device.
 * @author art
 *
 */
public class GardenoidService extends Service 
{
    // TODO: make PASSWORD configurable
    private static final char[] KEYSTORE_PASSWORD = "test99".toCharArray();
    
    public static final int PORT_HTTP  = 8080;
    public static final int PORT_HTTPS = 8080;

    public static final String ACTION_NOTIFICATION = "GardenoidService.Notification.clicked";

    private static final String PREFKEY_BT_DEVICE_ADDR = "BT_DEVICE_ADDR";
    private static final String PREFKEY_ADMIN_PASSWORD = "ADMIN_PASSWORD";
    
    public static final int MSG_STATUS  = 1;
    public static final int MSG_SRVADDR = 2;
    public static final int MSG_MASK    = 3;

    private static final boolean DEBUG_ONETIME_CONTAINER = false; // debug

    public static final String TAG = GardenoidService.class.getSimpleName();

    public final static String RFC1123_PATTERN = "EEE, dd MMM yyyy HH:mm:ss z";

    public final static Locale LOCALE_US = Locale.US;
    public final static SimpleDateFormat RFC1123FORMAT = new SimpleDateFormat(RFC1123_PATTERN, LOCALE_US);
    public final static TimeZone GMT_ZONE = TimeZone.getTimeZone("GMT");

    private String mProtocol = "undefined";
    @SuppressWarnings("unused")
    private String mAddress  = "undefined";
    private int    mPort     = -1;		

    
    private CommandQueue.Command sendEffectiveMaskToControllerAndActivity(int mask)
    {
	String command = String.format("M%x", mask);
	CommandQueue.Command cmd = mCmdQueue.send(command, false /* do NOT wait */);
	if (null!=mHandler)
	{
	    mHandler.obtainMessage(MSG_MASK, mask).sendToTarget();
	}
	return cmd;
    }

    public GardenoidService() 
    {
	//System.out.println("GardenoidService");
	mPairedDevices    = new HashSet<BluetoothDevice>();
	mVisibleDevices   = new HashSet<BluetoothDevice>();
	mVisibleDevicesDiscovering =  new HashSet<BluetoothDevice>();
	mForecastProvider = new ForecastProvider();
    }
    
    private static class Cookie
    {
	private long    mExpires;
	private boolean mAuthorized;
	private boolean mSession;
	private boolean mMobile;
	private String  mName;

	public Cookie(String name, boolean session, boolean mobile)
	{
	    mName       = name;
	    mExpires    = -1;
	    mAuthorized = false;
	    mSession    = session;
	    mMobile     = mobile;
	}
	
	public boolean isMobile()
	{
	    return mMobile;
	}

	public void setMobile(boolean m)
	{
	    mMobile = m;
	}

	public boolean isExpired() 
	{
	    return U.millis()>mExpires;
	}

	public boolean isAuthorized()
	{
	    return mAuthorized;
	}

	public void setAuthorized(boolean authorized)
	{
	    mAuthorized = authorized;
	}

	public void prolong(int secs)
	{
	    mExpires = U.millis()+1000*secs;
	}

	public String getName()
	{
	    return mName;
	}

	public String toString()
	{
	    long now = U.millis();
	    long ttl = mExpires<=now ? 0 : mExpires-now;
	    return "Cookie[name=" + mName + ",auth=" + mAuthorized + ",ttl=" + ttl + ",sess=" + mSession + ",mobile=" + mMobile + "]";
	}

//	public void setSession(boolean s)
//        {
//	    mSession = s; 
//        }

	public static boolean isAuthorized(Cookie session)
        {
	    return null!=session && session.isAuthorized();
        }
	
	public static boolean isMobile(Cookie cookie)
	{
	    return null!=cookie && cookie.isMobile();
	}
    }

    public String createSessionInternally()
    {
	synchronized (mCookies)
	{	    
	    Cookie c = createCookie(true /* session */, true /* mobile */);
	    c.setAuthorized(true);
	    return c.getName();	    
	}
    }

    private Map<String,Cookie> mCookies = new HashMap<String,Cookie>();
	
    private Cookie createCookie(boolean session, boolean mobile)
    {
	synchronized (mCookies)
	{	    
	    // create a list of cookies that expired
	    HashSet<String> removed = new HashSet<String>();
	    for (Cookie c :mCookies.values())
	    {
		if (c.isExpired()) removed.add(c.getName());
	    }

	    // and clean them up
	    for (String name : removed)
	    {
		mCookies.remove(name);
	    }

	    // before trying to create a new key that does not yet exist
	    String name = null;
	    do
	    {
		name = (session ? "session_" : "auth_") + (""+Math.random()).replace(".", "");
	    }
	    while (mCookies.containsKey(name));

	    Cookie cookie = new Cookie(name, session, mobile);
	    mCookies.put(name, cookie);
	    return cookie;
	}
    }


    private class OneTimeSchedule
    {
	public OneTimeSchedule(int strandNo, long endtimeUnix)
	{
	    this.strandNo = strandNo;
	    this.endtimeUnix = endtimeUnix;
	}

	public boolean isActiveX(long nowUnixtime)
	{
	    return endtimeUnix>0 && nowUnixtime<endtimeUnix;
	}

	public long getSecondsLeftX(long nowUnixtime)
	{
	    long left = endtimeUnix-nowUnixtime;
	    return left<0 ? 0 : left;
	}

	int  strandNo;
	long endtimeUnix;

	public OneTimeSchedule addTimeX(int secs)
	{
	    long e = endtimeUnix; 	    
	    long nowUnixtime = DAO.nowUnixtime();	    
	    endtimeUnix = (e>nowUnixtime) ? e+secs : nowUnixtime+secs;
	    return this;
	}

	public OneTimeSchedule setTimeX(int secs)
	{
	    long nowUnixtime = DAO.nowUnixtime();	    
	    endtimeUnix = nowUnixtime+secs;
	    return this;
	}

	public OneTimeSchedule stopX()
	{
	    endtimeUnix = -1;
	    return this;
	}

	public String toJson()
	{
	    long now  = DAO.nowUnixtime();
	    long left = getSecondsLeftX(now);
	    long end  = endtimeUnix>now ? 0 : endtimeUnix; 
	    try
	    {
		return U.toJson(
			"no",     strandNo, 
			"end",    end, 
			"left",   left, 
			"active", left>0);
	    }
	    catch (Exception e)
	    {
		e.printStackTrace();
		return ""+e;
	    }
	}
    }


    class OneTimeContainer extends Thread
    {
	private OneTimeSchedule oneTimeSchedules[] = new OneTimeSchedule[]
		{
		new OneTimeSchedule(1, -1),
		new OneTimeSchedule(2, -1),
		new OneTimeSchedule(3, -1),
		new OneTimeSchedule(4, -1),
		new OneTimeSchedule(5, -1),
		new OneTimeSchedule(6, -1),
		new OneTimeSchedule(7, -1),
		new OneTimeSchedule(8, -1),
		};

	public OneTimeContainer()
	{		
	    mMask = 0;
	}

	public volatile boolean mShutdownRequested = false;

	@Override
	public void run() 
	{
	    try
	    {
		while (true) //(!mShutdownRequested )
		{
		    long millis = (Calendar.getInstance().getTimeInMillis()%1000);
		    long sleeptime = 1000-millis;
		    try { Thread.sleep(sleeptime); } catch (InterruptedException e) {}
		    //if (DEBUG_ONETIME_CONTAINER) System.out.println("oneTimeSchedules: calling updateMask");
		    boolean rc = updateMask();
		    //if (DEBUG_ONETIME_CONTAINER) System.out.println("oneTimeSchedules: updateMask returned " + rc);
		}
	    }
	    catch (Exception e)
	    {
		System.err.println("oneTimeSchedules: " + e);
	    }
	    //System.err.println("oneTimeSchedules: *************  at end of run() ************* ");
	};

	public OneTimeContainer addTime(int no, int secs)
	{
	    synchronized (this)
	    {	            
		oneTimeSchedules[no-1].addTimeX(secs);
		//if (DEBUG_ONETIME_CONTAINER) System.out.println("oneTimeSchedules: addTime(" + no + "," + secs + ")");
		updateMask();
		updateChangeTime(); // for sure - something changed. updateMask() will do it only if mask changed
		return this;
	    }
	}

	public OneTimeContainer setTime(int no, int secs)
	{
	    synchronized (this)
	    {	            
		oneTimeSchedules[no-1].setTimeX(secs);
		//if (DEBUG_ONETIME_CONTAINER) System.out.println("oneTimeSchedules: setTime(" + no + "," + secs + ")");
		updateMask();
		updateChangeTime();
		return this;
	    }
	}

	private long now()
	{
	    return Calendar.getInstance().getTimeInMillis();
	}

	private boolean updateMask()
	{
	    boolean notify  = false;
	    int     newMask = 0;

	    synchronized (this)
	    {
		int oldMask = mMask;
		newMask = computeMask();

		if ((notify = (oldMask!=newMask)))        	
		{
		    //System.out.println("oneTimeSchedules: mask changed " + oldMask  + " -> " + newMask + ", this=" + this);
		    updateChangeTime();
		}
		mMask = newMask;
	    }

	    // do this outside the synchronized area to avoid lockup when
	    // onOnetimeMaskChange needs longer than normal for execution
	    if (notify)
	    {
		GardenoidService.this.onOnetimeMaskChange(newMask);        	    
	    }

	    return notify;
	}

	private void updateChangeTime()
	{
	    mLastChange = now();
	}

	public int getMask()
	{
	    return mMask;
	}

	private int computeMask()
	{
	    int  mask = 0;	        
	    long nowUnixtime = DAO.nowUnixtime();
	    for (int i=7; i>=0; i--)
	    {
		mask<<=1;
		if (oneTimeSchedules[i].isActiveX(nowUnixtime))
		{
		    mask|=1;
		}
	    }

	    return mask;
	}

	public OneTimeContainer stop(int no)
	{
	    synchronized(this)
	    {
		oneTimeSchedules[no-1].stopX();
		//if (DEBUG_ONETIME_CONTAINER) System.out.println("oneTimeSchedules: stop(" + no + ")");
		updateMask();
		return this;
	    }
	}

	public String toJson(int no)
	{
	    return oneTimeSchedules[no-1].toJson();
	}

	public String toJson(long nowUnixtime)
	{
	    StringBuilder sb = new StringBuilder();
	    sb.append("[\n");
	    try
	    {		    
		for (int no=1; no<=mOneTimeContainer.size(); no++)
		{
		    sb
		    .append(1==no ? "" : ",\n")
		    .append("{ \"no\":").append(no)
		    .append(",\"left\":").append(getSecondsLeft(no, nowUnixtime))
		    .append(",\"end\":").append(getEndtimeUnix(no))
		    .append(",\"active\":").append(isActive(no, nowUnixtime))
		    .append("}");
		}
	    }
	    catch (Exception e)
	    {
		e.printStackTrace();
	    }
	    sb.append("\n]");
	    return sb.toString();
	}


	private Object isActive(int no, long nowUnixtime)
	{
	    return oneTimeSchedules[no-1].isActiveX(nowUnixtime);
	}

	public int size()
	{
	    return oneTimeSchedules.length;
	}

	public long getSecondsLeft(int no, long nowUnixtime)
	{	        
	    return oneTimeSchedules[no-1].getSecondsLeftX(nowUnixtime);
	}

	public long getEndtimeUnix(int no)
	{
	    return oneTimeSchedules[no-1].endtimeUnix;
	}

	public long getLastChangeTime()
	{
	    //System.out.println("oneTimeSchedules: getLastChangeTime: mask=" + mMask);
	    return mLastChange;
	}

	// volatile: mMask is set by one thread but read by another one
	private volatile int mMask = 0;
	// same for last change date
	private volatile long mLastChange = -1;
    }


    private OneTimeContainer mOneTimeContainer = new OneTimeContainer();
    private AssetManager     mAssetManager;
    private TemplateEngine   mTemplateEngine;
    private ForecastProvider mForecastProvider;
    private ConditionChecker mConditionChecker;
    private String           mServiceVersion;


    @Override
    public IBinder onBind(Intent intent) 
    {
	String action = intent.getAction();	
	Log.v(TAG, "onBind: intent=" + intent + ", action=" + action);	
	return new Binder();
    }

    public void onOnetimeMaskChange(int onetimeMask)
    {
	int  mask = onetimeMask|mActiveStrandsMask;

	long t1 = Calendar.getInstance().getTimeInMillis();    		
	sendEffectiveMaskToControllerAndActivity(mask);	
	long t2 = Calendar.getInstance().getTimeInMillis();
	
	long delta = t2-t1;

	if (delta>500)
	{
	    //if (DEBUG_ONETIME_CONTAINER) System.out.println("sendEffectiveMaskToControllerAndActivity took " + delta + " ms");
	}	
    }


    @Override
    public boolean onUnbind(Intent intent) 
    {
	boolean rc = super.onUnbind(intent);
	mDao.addEvent(new Event(mActiveStrandsMask, "onUnbind"));
	Toast.makeText(getApplicationContext(), "GardenoidService: onUnbind",Toast.LENGTH_SHORT).show();
	return rc;
    };

    @Override
    public void onRebind(Intent intent) 
    {
	try
	{
	    super.onRebind(intent);
	    mDao.addEvent(new Event(mActiveStrandsMask, "onRebind"));
	    Toast.makeText(getApplicationContext(), "GardenoidService: onRebind",Toast.LENGTH_SHORT).show();
	}
	catch (Exception e)
	{
	    log(e);
	}
    };

    @Override
    public void onLowMemory() 
    {
	try
	{
	    super.onLowMemory();
	    mDao.addEvent(new Event(mActiveStrandsMask, "onLowMemory"));
	    Toast.makeText(getApplicationContext(), "GardenoidService: onLowMemory",Toast.LENGTH_SHORT).show();
	}
	catch (Exception e)
	{
	    log(e);
	}
    };

    private boolean log(Throwable t)    
    {
	try
	{
	    t.printStackTrace();
	    if (null!=mDao)
	    {
		mDao.logException(t);
		return true;
	    }
	}
	catch (Exception e) 
	{ 
	    e.printStackTrace(); 
	}
	return false;
    }

    @Override
    public void onCreate() 
    {
	try
	{
	    super.onCreate();

	    SimpleDateFormat YYYYMMDD_hhmmss = new  SimpleDateFormat("yyyyMMdd_HHmmss");	    
	    mServiceVersion = YYYYMMDD_hhmmss.format(Calendar.getInstance().getTime());

	    //Toast.makeText(getApplicationContext(), "Service Created",Toast.LENGTH_SHORT).show();
	    initBluetooth();

	    // important: database must be initialized before HTTP server,
	    // otherwise race condition may occur if 1st request hits the server
	    // before database setup completed
	    mDao = new DAO(this);
	    mAssetManager = getAssets();

	    File externalDir = getExternalFilesDir(TemplateEngine.DIR_PREFIX_WEBPAGES);
	    mTemplateEngine = new TemplateEngine(mAssetManager, externalDir);

	    SSLServerSocketFactory socketFactory = null;
	    // http://stackoverflow.com/questions/21077273/nanohttpd-and-ssl
	    try 
	    {
		//String type = KeyStore.getDefaultType(); // "BKS"
		String type = "BKS";
		//String type = "PKCS12";
		
		InputStream certFile = getAssets().open("ssl/gardenoid." + type);
	        if (certFile != null) 
	        {
	            //String type = "BKS"; // KeyStore.getDefaultType();
	            KeyStore keyStore = KeyStore.getInstance(type);
	            //KeyStore keyStore = KeyStore.getInstance(type);
	            keyStore.load(certFile, KEYSTORE_PASSWORD);
	            
	            KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
	            factory.init(keyStore, KEYSTORE_PASSWORD);
	            
	            SSLContext context = SSLContext.getInstance("SSL");
	            context.init(factory.getKeyManagers(), null, new SecureRandom());
	            socketFactory = context.getServerSocketFactory();
	        }
	    }
	    catch (Exception e)
	    {
		System.err.println("onCreate: " + e);
	    }
	    
	    if (socketFactory!=null)
	    {
		mProtocol   = "https://";
		mAddress    = getAddress();
		mPort       = PORT_HTTP;
		mHttpServer = new MyHTTPD(mPort);
		mHttpServer.makeSecure(socketFactory);
	    }
	    else
	    {
		mProtocol   = "http://";
		mAddress    = getAddress();
		mPort       = PORT_HTTPS;		
		mHttpServer = new MyHTTPD(mPort);
	    }
	    
	    mHttpServer.start();
	    mConnectThread = new ConnectThread(GardenoidService.this, true);
	    mConnectThread.start();
	    mConditionChecker = new ConditionChecker(mDao);

	    registerReceiver(mTickReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));

	    Event e = new Event(mActiveStrandsMask, "onCreate");
	    mDao.addEvent(e);
	    connectToLastDevice();
	} 
	catch (IOException e)
	{
	    log(e);
	}
    }

    private void connectToLastDevice()
    {
	try
	{ 
	    System.err.println("===================");
	    System.err.println("connectToLastDevice");
	    System.err.println("===================");

	    if (null==mConnectThread || mConnectThread.isConnected())
	    {
		return;
	    }
	    SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
	    String address = null==p ? null : p.getString(PREFKEY_BT_DEVICE_ADDR, null);
	    System.err.println("address: " + address);

	    //System.out.println(PREFKEY_BT_DEVICE_ADDR + ": " + address);	    
	    BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
	    if (null!=address)
	    {	    
		if (!adapter.isEnabled())
		{
		    adapter.enable();
		}
		BluetoothDevice device = adapter.getRemoteDevice(address);	
		mConnectThread.connectTo(device, false /* do NOT wait here */);
	    }
	}
	catch (Exception e)
	{
	    log(e);
	}
    }
    
    
    private String getAdminPassword()
    {
	SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
	String password = null==p ? null : p.getString(PREFKEY_ADMIN_PASSWORD, null);
	return password;
    }
    

    private boolean setAdminPassword(String password)
    {
	try
	{
	    SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
	    p.edit().putString(PREFKEY_ADMIN_PASSWORD, password).commit();
	    return true;	   
	}
	catch (Exception e)
	{
	    System.err.println("setAdminPassword: " + e);
	}
	return false;
    }
    

    private BroadcastReceiver mTickReceiver = new BroadcastReceiver()
    {
	@Override
	public void onReceive(Context context, Intent intent)
	{
	    if (intent.getAction().compareTo(Intent.ACTION_TIME_TICK) == 0)
	    {
		Log.w(TAG, "**** ACTION_TIME_TICK ****");
		updateWeatherForecast();
		checkSchedules();
	    } 
	}

    };

    private int mActiveStrandsMask = -1;
    private ScheduleCheckThread mScheduleCheckThread;
    private TreeSet<Long> mPoweredOnSchedules = null;

    public TreeSet<Long> getPoweredOnSchedules()
    {
	TreeSet<Long> set = new TreeSet<Long>();
	synchronized (this)
	{
	    if (null!=this.mPoweredOnSchedules )
	    {
		set.addAll(mPoweredOnSchedules);
	    }
	}
	return set;
    }

    private boolean setPoweredOnSchedules(List<Schedule> on)
    {
	boolean change = false;

	synchronized (this)
	{
	    if (null==on && null==mPoweredOnSchedules)
	    {
		return false; // both null => no change
	    }

	    if (null==on || null==mPoweredOnSchedules || on.size()!=mPoweredOnSchedules.size())
	    {
		// only one null or different number of elements => change
		mPoweredOnSchedules = new TreeSet<Long>();
		if (null!=on)
		{
		    for (Schedule s : on) mPoweredOnSchedules.add(s.getId());
		}
		return true;
	    }

	    // assertion here: neither one out of "on" and "mPoweredOnSchedules" is null.
	    for (Schedule s : on)
	    {
		if (!mPoweredOnSchedules.contains(s.getId())) 
		{
		    change = true;
		    break;
		}
	    }

	    if (change)
	    {
		mPoweredOnSchedules.clear();	    
		for (Schedule s : on) mPoweredOnSchedules.add(s.getId());
	    }
	}

	return change;
    }	


    private void checkSchedules()
    {
	(mScheduleCheckThread = new ScheduleCheckThread(this)).start();
    }

    class ScheduleCheckThread extends Thread
    {
	private GardenoidService mService;

	public ScheduleCheckThread(GardenoidService service)
	{
	    this.mService = service;
	}

	public void run()
	{
	    try
	    {
		List<Schedule> all = getActiveSchedulesWithRetry();
		if (null==all) return;

		List<Schedule> on  = new LinkedList<Schedule>();
		List<Schedule> off = new LinkedList<Schedule>();

		int newMask = 0;
		for (Schedule s : all)
		{
		    boolean doWater = mConditionChecker.isFullFilled(s);
		    if (doWater)
		    {	    
			Log.w(TAG, "checkSchedules: TRUE: " + s);
			int oneMask = s.getStrandMask();
			newMask |= oneMask;
			on.add(s);
		    }
		    else
		    {
			Log.w(TAG, "checkSchedules: FALSE: " + s);
			off.add(s);
		    }
		}

		int affectedRows = mDao.setSchedulePower(on);
		//System.out.println("checkSchedules: Updated strand status: affectedRows=" + affectedRows);

		mActiveStrandsMask = newMask;
		//System.out.println("checkSchedules: mActiveStrandsMask=" + mActiveStrandsMask);

		CommandQueue.Command cmd = null;

		try	    
		{
		    int mask = mOneTimeContainer.getMask()|mActiveStrandsMask;			    
		    cmd = sendEffectiveMaskToControllerAndActivity(mask);
		}
		catch (Exception e)
		{
		    Log.e(TAG, "Exception sending command " + e);
		}

		// while the command is executing (see no wait above), update the database 
		boolean change = setPoweredOnSchedules(on); 
		if (change)
		{
		    onScheduleChange(on, newMask);
		}

		// now wait until the arduino processed the command and returned his ack
		if (null!=cmd)
		{
		    @SuppressWarnings("unused")
		    boolean ready = cmd.waitReady();
		    if (!cmd.ready)
		    {
			Log.e(TAG, "Timeout sending command: " + cmd.getString());
		    }
		    else if (cmd.isError())
		    {
			Log.e(TAG, "Error sending command: " + cmd.getError());
		    }
		}
	    }
	    catch (Exception e)
	    {
		log(e);
	    }
	    finally
	    {
		mService.onScheduleCheckThreadDone(this);
	    }
	}

        private void onScheduleChange(List<Schedule> on, int newMask)
        {
	    //System.out.println("checkSchedules: Schedules status changed: " + mPoweredOnSchedules);

	    if (mActiveStrandsMask!=newMask)
	    {
		//System.out.println("checkSchedules: Strand status changed: mask=" + newMask);			
	    }
	    else
	    {
		//System.out.println("checkSchedules: Strand status remains the same: mask=" + newMask);
	    }

	    StringBuilder sb = new StringBuilder();
	    for (Schedule s : on)
	    {
	    sb.append(sb.length()>0 ? "," : "").append(s.toJson());
	    }		    
	    String json = "{\"on\" : [" + sb.toString() + "]}";
	    		    
	    Event event = new Event(newMask, "onScheduleChange", json);
	    mDao.addEvent(event);
        }

	private List<Schedule> getActiveSchedulesWithRetry()
	{
	    Date now = Calendar.getInstance().getTime();
	    List<Schedule> all = null;
	    for (int trial=0; trial<10; trial++)
	    {
		try
		{
		    all = mDao.getActiveSchedules(now);
		}
		catch (IllegalStateException e)
		{
		    try { U.mSleep(20*trial); } catch (Exception ex) {}
		}
	    }

	    if (null==all)
	    {
		log(new RuntimeException("Failed to get active schedules"));
	    }
	    return all;
	}

	/*
	private String toIdString(List<Schedule> on)
	{
	    StringBuilder sb = new StringBuilder();
	    for (Schedule s : on) sb.append(sb.length()>0 ? "," : "").append(s.getId());
	    return sb.toString();
	}
	 */
    }


    private void updateWeatherForecast() 
    {
	long now = DAO.nowUnixtime();
	if (now-lastForecastUpdate < 5*60) // start update thread at most every 5 minutes
	{
	    return;  
	}
	if (null!=mUpdateThread)
	{
	    Log.e(TAG, "update: mUpdateThread: mUpdateThread!=null");
	    return;
	}

	(mUpdateThread = new WeatherUpdateThread(this, mForecastProvider, mDao)).start();
    }


    public void onScheduleCheckThreadDone(ScheduleCheckThread thread)
    {
	if (thread==mScheduleCheckThread)
	{
	    mScheduleCheckThread = null;
	}
    }

    public void onUpdateThreadDone(WeatherUpdateThread thread)
    {
	if (thread==mUpdateThread)
	{
	    mUpdateThread = null;
	    lastForecastUpdate = Calendar.getInstance().getTimeInMillis()/1000;
	}
    }


    private void initBluetooth()
    {
	// Register for broadcasts when a device is discovered
	IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
	this.registerReceiver(mReceiver, filter);

	// Register for broadcasts when discovery has finished
	filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
	this.registerReceiver(mReceiver, filter);

	// Get the local Bluetooth adapter
	mBtAdapter = BluetoothAdapter.getDefaultAdapter();

	// Get a set of currently paired devices
	mPairedDevices = (null==mBtAdapter) ? new HashSet<BluetoothDevice>() : mBtAdapter.getBondedDevices();
    }


    public boolean isConnected()
    {
	return null!=mConnectThread && mConnectThread.isConnected();
    }

    public BluetoothDevice getConnectedDevice()
    {
	return (null==mConnectThread) ? null : mConnectThread.getDeviceConnected();
    }

    public String[] getConnectedNameAndAddr()
    {
	BluetoothDevice d = getConnectedDevice();
	if (null==d) return null;
	String rv[] = { d.getName(), d.getAddress() };
	return rv;
    }

    @Override
    public void onDestroy() 
    {
	try
	{
	    super.onDestroy();
	    mDao.addEvent(new Event(mActiveStrandsMask, "onDestroy"));
	    Toast.makeText(getApplicationContext(), "Service Destroy", Toast.LENGTH_SHORT).show();
	    mConnectThread.disconnect();
	}
	catch (Exception e)
	{
	    log(e);
	}
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) 
    {
	int rc = START_STICKY;
	try
	{
	    rc = super.onStartCommand(intent, flags, startId);

	    String action = (null==intent) ? "none(no intent)" : intent.getAction();

	    // TODO Auto-generated method stub
	    Toast.makeText(getApplicationContext(), "Service Working, action=" + action, Toast.LENGTH_LONG).show();

	    Notification notification = getCompatNotification();
	    startForeground(4711, notification);

	    try
	    {
		DAO dao = mDao;
		if (null==dao) 
		{
		    System.err.println("No DAO available in onStartCommand");
		    dao = new DAO(this);
		}
		dao.addEvent(new Event(mActiveStrandsMask, "onStartCommand", "id", startId, "action", action));
	    }
	    catch (Exception e)
	    {
		System.err.println("onStartCommand: " + e);
	    }

	    updateWeatherForecast();
	    checkSchedules();	
	    connectToLastDevice();

	    // start the thread that monitors one time schedules with per second precision
	    if (!mOneTimeContainer.isAlive())
	    {
		mOneTimeContainer.start();
	    }
	}
	catch (Exception e)
	{
	    log(e);
	}
	// http://developer.android.com/guide/components/services.html
	// rc = START_REDELIVER_INTENT;
	rc = START_STICKY;
	return rc;
    }


    private Notification getCompatNotification()
    {
	NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
	builder.setSmallIcon(R.drawable.ic_launcher)
	.setContentTitle("Gardenoid service started")
	.setTicker("Service active")
	.setWhen(System.currentTimeMillis());
	Intent startIntent = new Intent(getApplicationContext(), GardenoidActivity.class);
	startIntent.setAction(ACTION_NOTIFICATION);

	PendingIntent contentIntent = PendingIntent.getActivity(this, 4718, startIntent, 0);


	builder.setContentIntent(contentIntent);
	Notification notification = builder.build();
	return notification;
    }

    public void setHandler(Handler handler)
    {
	mHandler = handler;
	//mHandler.obtainMessage(MSG_STATUS, -1, -1, "Hello!").sendToTarget();
	int mask = mOneTimeContainer.getMask()|mActiveStrandsMask;			    
	sendEffectiveMaskToControllerAndActivity(mask);
	
	int addr = -1;
	mHandler.obtainMessage(MSG_SRVADDR, addr, mPort, mProtocol).sendToTarget();
    }


    public void removeHandler(Handler handler)
    {
	if (mHandler==handler)
	{
	    mHandler = null;
	}
    }

    public NanoHTTPD_SSL getHttpServer()
    {
	return mHttpServer;
    }    


    public boolean startHttp()
    {
	try
	{
	    Log.v(TAG, "Starting HTTP server ...");
	    mHttpServer.start();
	    return awaitAlive(true);
	} 
	catch (IOException e)
	{
	    log(e);
	}	
	return false;
    }


    public boolean stopHttp()
    {
	try
	{
	    Log.v(TAG, "Stopping HTTP server ...");
	    mHttpServer.stop();	
	    return awaitAlive(false);
	}
	catch (Exception e)
	{
	    log(e);
	}
	return false;
    }


    public void scheduleRetry(ConnectThread connectThread)
    {
	// TODO Auto-generated method stub	
    }


    public void registerConnectThread(ConnectThread connectThread)
    {
	// TODO Auto-generated method stub

    }


    public boolean cancelDiscovery(ConnectThread connectThread)
    {
	cancelDiscovery();
	return true;
    }


    public void onConnectionEstablished(ConnectThread connectThread, BluetoothDevice device)
    {
	try
	{
	    String name = null==device ? "?" : device.getName();
	    String addr = null==device ? "?" : device.getAddress();
	    Log.e(TAG, "onConnectionEstablished: (device: " + name + "/" + addr + ")");

	    mDao.addEvent(new Event(mActiveStrandsMask, "onConnectionEstablished",  "name", name, "addr", addr));
	}
	catch (Exception e)
	{
	    log(e);
	}
    }

    private static final boolean ADD_DEBUG_EVENTS = false;

    public void onConnectionFailed(ConnectThread connectThread, String string, BluetoothDevice device)
    {
	try
	{
	    String name = null==device ? "?" : device.getName();
	    String addr = null==device ? "?" : device.getAddress();
	    Log.e(TAG, "connectionFailed: " + string + " (device: " + name + "/" + addr + ")");	

	    if (string.contains("discovery failed"))
	    {
		// do not log more often than every 5 minutes
		if (mLastServiceDiscoveryFailureEventLogged<1 || DAO.nowUnixtime()-mLastServiceDiscoveryFailureEventLogged<300)
		{
		    mLastServiceDiscoveryFailureEventLogged = DAO.nowUnixtime(); 
		    if (ADD_DEBUG_EVENTS) mDao.addEvent(new Event(mActiveStrandsMask, "onConnectionFailed", "name", name, "addr", addr, "error", string));
		}
	    }
	}
	catch (Exception e)
	{
	    log(e);
	}
    }


    public void setState(ConnectThread connectThread, int stateConnected)
    {
	Log.e(TAG, "setState: stateConnected" + stateConnected);	
    }


    public void connectionLost(String string)
    {
	// TODO Auto-generated method stub
	mDao.addEvent(new Event("connectionLost", "msg", string));
    }


    public void sendDebug(ConnectThread connectThread, String msg)
    {
	Log.d(TAG, "sendDebug: " + msg);	
    }


    // method invoked by the connect thread whenever a new line was received
    public void onBluetoothCommandReceived(ConnectThread connectThread, String line)
    {
	try
	{
	    // first:   '^12345:payload'
	    // success: '+12345:payload'
	    // error:   '-12345:payload'
	    // last:   ' $12345:ignored'
	    if (null==line || line.length()<1)
	    {
		//System.err.println("ERROR: sendMessageString: empty line");
		return;
	    }
	    char first = line.charAt(0);
	    switch (first)
	    {
	    case '+': // success line
	    case '-': // error line
	    case '$': // last line	    
		System.err.println("REPLY: " + line);

		// extract the command reference (after the first char, before the colon)
		String ref     = line.substring(1).replaceAll("[:$].*", "");
		String payload = line.substring(1).replaceAll("^" + ref + ":", "");

		switch (first)
		{
		case '+':
		    mCmdQueue.addSuccess(ref, payload); 
		    mCmdQueue.addSuccess(ref, "\n"); 
		    break;
		case '-':
		    mCmdQueue.addError(ref, payload);
		    mCmdQueue.addError(ref, "\n");
		    break;
		case '$':
		    mCmdQueue.addSuccess(ref, payload);
		    mCmdQueue.setReady(ref);
		    break;
		}
		break;
	    default:
		System.err.println("ERROR: Invalid line: " + line);
	    }

	    mCmdQueue.cleanupExpired();
	}
	catch (Exception e)
	{
	    log(e);
	}
	// TODO Auto-generated method stub	
    }

    public void sendMessageBytes(ConnectThread connectThread, int messageRead, int bytes, byte[] buffer)
    {
	// not needed since using line-wise communication only
    }

    public static String getAddress()
    {
	try 
	{
	    for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) 
	    {
		NetworkInterface intf = en.nextElement();
		for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) 
		{
		    InetAddress inetAddress = enumIpAddr.nextElement();
		    if (!inetAddress.isLoopbackAddress()) 
		    {
			Log.e(TAG, "Got addr: " + inetAddress);
			if (inetAddress instanceof Inet4Address)
			{
			    String rv = inetAddress.getHostAddress().toString();
			    return rv;
			}
		    }
		}
	    }
	} 
	catch (SocketException ex) 
	{
	    System.err.println(""+ex);
	    return "error";
	}
	return "127.0.0.1";   
    }


    boolean isDiscovering()
    {
	return null!=mBtAdapter && mBtAdapter.isDiscovering();
    }

    void startDiscovery()
    {
	if (null!=mBtAdapter) mBtAdapter.startDiscovery();
    }

    void cancelDiscovery()
    {
	if (null!=mBtAdapter) mBtAdapter.cancelDiscovery();
    }


    boolean toggleDiscovery()
    {
	boolean d = isDiscovering();
	if (d)
	{
	    mDao.addEvent(new Event(mActiveStrandsMask, "toggleDiscovery", "action","cancel"));
	    cancelDiscovery();
	}
	else
	{
	    mDao.addEvent(new Event(mActiveStrandsMask, "toggleDiscovery", "action", "start"));
	    startDiscovery();
	}
	return !d;
    }


    private boolean awaitAlive(boolean expectedStatus)
    {
	for (int i=1; i<10; i++)
	{
	    if (mHttpServer.isAlive()==expectedStatus) return true;
	    Log.v(TAG, "HTTP server not " + (expectedStatus ? "UP" : "DOWN") + ", waiting ...");
	    try { Thread.sleep(10); } catch (InterruptedException e) {}
	}
	return false;
    }


    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver()
    {

	@Override
	public void onReceive(Context context, Intent intent) 
	{
	    try
	    {
		String action = intent.getAction();

		// When discovery finds a device
		if (BluetoothDevice.ACTION_FOUND.equals(action)) 
		{
		    // Get the BluetoothDevice object from the Intent
		    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		    System.out.println("Discovered device: " + device);
		    // If it's already paired, skip it, because it's been listed already
		    //if (device.getBondState() != BluetoothDevice.BOND_BONDED) 
		    {
			String addr = device.getAddress();
			BluetoothDevice device2 = mBtAdapter.getRemoteDevice(addr);
			mVisibleDevicesDiscovering.add(device2);
			mVisibleDevices.add(device2);
		    }
		    // When discovery is finished, change the Activity title
		} 
		else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) 
		{
		    // replace the list of visible devices by the newly discovered ones
		    mVisibleDevices = mVisibleDevicesDiscovering;
		    mVisibleDevicesDiscovering = new HashSet<BluetoothDevice>();
		    //setProgressBarIndeterminateVisibility(false);
		    //setTitle(R.string.select_device);                
		    //                if (mNewDevicesArrayAdapter.getCount() == 0) {
		    //                    String noDevices = getResources().getText(R.string.none_found).toString();
		    //                    mNewDevicesArrayAdapter.add(noDevices);
		    //                }

		}
	    }
	    catch (Exception e)
	    {
		log(e);
	    }
	}
    };

    // remote handler the activity registered with this service
    private Handler mHandler    = null;
    private MyHTTPD mHttpServer = null;
    private BluetoothAdapter     mBtAdapter;
    private Set<BluetoothDevice> mPairedDevices, mVisibleDevices, mVisibleDevicesDiscovering;
    private ConnectThread        mConnectThread;
    private CommandQueue         mCmdQueue       = new CommandQueue();
    private class CommandQueue 
    {	
	class Command
	{
	    //private String cmd;
	    private String line;
	    private volatile boolean ready;

	    public Command(String cmd, String ref)
	    {
		//this.ref   = ref;
		this.line  = ref + "\t" + cmd + "\n";
		this.ready = false;
		this.createTime = DAO.nowUnixtime();
	    }

	    public String getError()
	    {	        
		return error.toString();
	    }

	    //	    public void reset()
	    //	    {
	    //		success = new StringBuilder();
	    //		error   = new StringBuilder(); 
	    //	    }

	    public void setReady()
	    {
		//System.err.println("COMMAND " + this + ": setReady");
		ready = true;
	    }

	    // wait until the reply for this command has been received via bluetooth
	    public boolean waitReady()
	    {
		for (int timeout=10; !ready && timeout>0; timeout--)
		{
		    U.sleep(1);
		}
		return ready;
	    }

	    //	    public String getRef()
	    //	    {
	    //		return ref;
	    //	    }

	    public void addError(String string)
	    {
		if (ready) return;
		error.append(string);	    
	    }

	    public boolean isError()
	    {
		return error.length()>0;
	    }

	    public void addSuccess(String string)
	    {
		if (ready) return;
		success.append(string);	    
	    }	

	    //	    public String        ref;
	    public StringBuilder success = new StringBuilder();
	    public StringBuilder error   = new StringBuilder();
	    private long createTime;

	    public String getString()
	    {
		return line;
	    }

	    public long age(long now)
	    {
		return now-createTime;
	    }
	}

	private HashMap<String, Command> map = new HashMap<String, Command>();

	public String createRef()
	{
	    for (;;)
	    {
		long l = (long)Math.ceil(0xffffffffL * Math.random());
		String ref = String.format("%x", l);
		if (!map.containsKey(ref)) return ref;
	    }
	}	

	public void cleanupExpired()
	{
	    LinkedList<String> keysToRemove = new LinkedList<String>();
	    long now = DAO.nowUnixtime();
	    for (String key : map.keySet())
	    {
		Command cmd = map.get(key);
		if (cmd.age(now)>20)
		{
		    keysToRemove.add(key);
		}
	    }
	    //System.out.println("cleanupExpired: removing " + keysToRemove.size() + " commands");
	    for (String key : keysToRemove)
	    {
		map.remove(key);
	    }
	}

	//	public final long now()
	//	{
	//	    return Calendar.getInstance().getTimeInMillis();
	//	}
	//	
	public Command send(String name, boolean doWait, String ... args)
	{
	    cleanupExpired();

	    String  ref = createRef();	    

	    Command cmd = new Command(name, ref);
	    try
	    {
		if (!isConnected())
		{
		    cmd.addError("Not connected to bluetooth device");
		    cmd.setReady();
		    return cmd;
		}

		map.put(ref, cmd);
		System.err.println("COMMAND for " + ref + " put in queue");
		String data = cmd.getString() + "\n";
		System.err.println("SENDING COMMAND '" + data + "'");

		mConnectThread.write(data.getBytes());
		if (!doWait)
		{
		    System.err.println("Not waiting for command completion upon user request");
		    return cmd;
		}

		cmd.waitReady();            
		System.err.println("Waiting ends for command in queue. ready: " + cmd.ready + ", command: " + cmd.getString());
	    }
	    catch (Exception e)
	    {
		log(e);
	    }
	    map.remove(ref);
	    return cmd;
	}

	/*
        public boolean wait(Command cmd)
        {
            boolean rc = cmd.waitReady();            
            System.err.println("Waiting ends for command in queue. ready: " + cmd.ready + ", command: " + cmd.getString());

            map.remove(cmd.getRef());
            return rc;            
        }
	 */

	public void addError(String ref, String string)
	{
	    Command cmd = map.get(ref);
	    if (null==cmd)
	    {
		System.err.println("Command reference " + ref + " not found in queue");
		return;
	    }
	    cmd.addError(string);
	}

	public void addSuccess(String ref, String string)
	{
	    Command cmd = map.get(ref);
	    if (null==cmd)
	    {
		System.err.println("Command for " + ref + " not found in queue");
		return;
	    }
	    cmd.addSuccess(string);
	}

	public void setReady(String ref)
	{
	    Command cmd = map.get(ref);
	    if (null!=cmd) cmd.setReady();
	}

	public Object toJson()
	{
	    StringBuilder sb = new StringBuilder();
	    boolean first = true;
	    sb.append("{ \"map\": [\n");
	    for (Command c :  map.values())
	    {
		sb.append(first ? "" : ",\n");
		sb.append("{ \"cmd\": ").append(c.line).append("\", \"time\": ").append(c.createTime).append("\" }");
		first = false; 
	    }
	    sb.append("\n]");
	    return sb.toString();
	}
    }

    public class Binder extends android.os.Binder 
    {
	public GardenoidService getService() 
	{
	    return GardenoidService.this;
	}
    }

    class MyHTTPD extends NanoHTTPD_SSL
    {
	/*
        class Status implements IStatus
        {
            public Status(int rc, String desc) { this.rc=rc; this.desc=desc; }
            @Override
            public int    getRequestStatus() { return rc; }
            @Override
            public String getDescription()   { return desc; }
            private int rc;
            private String desc;
        }
	 */

	private static final String TEMP_UNIT_CELSIUS = "c";
	public final String CT_TEXT_PLAIN   = "text/plain";
	public final String CT_TEXT_HTML    = "text/html;charset=UTF-8";
	public final String CT_TEXT_JSON    = "text/json";
	public final String CT_IMAGE_XICON  = "image/x-icon";
	public final String CT_IMAGE_GIF    = "image/gif";
	public final String CT_IMAGE_PNG    = "image/png";
	public final String CT_IMAGE_JPG    = "image/jpg";
	public final String CT_JAVASCRIPT   = "application/javascript";
	public final String CT_CSS          = "text/css";

	public class CompressedResponse extends Response
	{
	    public CompressedResponse(String contentType, InputStream is, boolean gzipAccepted)
	    {
		super(Status.OK, contentType, is);
		if (gzipAccepted)
		{
		    addHeader("Content-Encoding", "gzip");
		}
	    }
	}

	public class CompressedHtmlResponse extends CompressedResponse
	{
	    public CompressedHtmlResponse(InputStream is, boolean gzipAccepted)
	    {
		super(CT_TEXT_HTML, is, gzipAccepted);
	    }

	    public CompressedHtmlResponse(InputStream is, boolean gzipAccepted, String cookie)
            {
	        this(is, gzipAccepted);
	        addHeader("Set-Cookie", cookie);
            }
	}
	
	public class Redirect extends Response
	{
	    public Redirect(String location, String cookie)
	    {
		this(location, cookie, "");
	    }
	    
	    public Redirect(String location, String cookie, String body)
	    {
		super(Status.REDIRECT, CT_TEXT_PLAIN, body);
		addHeader("Location", location);
		addHeader("Set-Cookie", cookie);		
	    }
	};
	
	public class JsonResponse extends Response
	{
	    public JsonResponse(String body)
	    {
		this(Status.OK, body);
	    }
	    
	    public JsonResponse(Status status, String body)
	    {
		super(status, CT_TEXT_JSON, body);
		addHeader("Pragma", "no-cache");
	    }

	    public JsonResponse(Status ok, InputStream is)
            {
		super(Status.OK, CT_TEXT_JSON, is);
            }	    
	};

	public class GzipResponse extends Response
	{
	    public GzipResponse(Status status, String ct, String cookie, InputStream is, boolean gzipAccepted)
	    {
		super(status, ct, is);
		addHeader("Set-Cookie", cookie);	
		if (gzipAccepted)
		{
		    addHeader("Content-Encoding", "gzip");
		}
	    }
	};

	public MyHTTPD(int port) 
	{
	    super(port);
	}

	private String getCurrentPeer()
	{
	    try
	    {
		BluetoothDevice d = null==mConnectThread ? null : mConnectThread.getDeviceConnected();
		return null==d ? null : d.getName();
	    }
	    catch (Exception e)
	    {
		log(e);
	    }
	    return null;
	}

	private Response createRestResponse(String resource, Map<String, String> params, boolean gzipAccepted) throws ParseException, JSONException, IOException	
	{
	    StringBuilder body = new StringBuilder();
	    Response      resp = null;

	    if (resource.startsWith("/devices/list"))
	    {		 
		Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
		listDevices(pairedDevices, mVisibleDevices, body);
	    }
	    else if (resource.startsWith("/strand/rename"))
	    {
		String tid  = params.get("tid");
		String name = params.get("name");
		String str  = params.get("no");		
		int    no   = Integer.parseInt(str);
		
		mDao.addOrUpdateStrand(no, name, tid);
		
		// send time of last config update so the instance that caused this change
		// may update this information in order not to force a relaod 
		body.append(U.toJson("no", no, "name", name, "reconfig", mDao.getLastReconfigId()));
	    }
	    else if (resource.startsWith("/weather/compact"))
	    {
		if ("yes".equals(params.get("confirm")))
		{ 
		    //System.out.println("Fetching all weather conditions from database");			
		    List<Weather> list = mDao.getAllWeathers(); 
		    //System.out.println("Fetched " + (null==list ? 0 : list.size()) + " weather conditions from database");

		    List<Long> ids = new ArrayList<Long>();

		    Weather last = null;
		    if (null!=list) for (Weather w : list)
		    {
			Calendar prev = (null==last) ? null : last.getDate();
			Calendar curr = w.getDate();

			if (last!=null && DAO.sameDay(prev, curr))
			{				    
			    long delta = (curr.getTimeInMillis()-prev.getTimeInMillis())/1000; // seconds
			    if (delta<6*60*60) // less than 6 hours between two entries?
			    {
				ids.add(w.getId()); // yes? mark id to be deleted
				//System.out.println("Delete: " + w);
			    }
			    else
			    {
				//System.out.println("Keep: " + w);
				last = w; // keep entry 
			    }
			}	
			else
			{
			    last = w;
			}
		    }
		    //System.out.println("Deleting " + ids.size() + " weather conditions from database");
		    int rows = mDao.deleteWeather(ids);

		    body.append(U.toJson("success", true, "rows", rows));
		}
		else
		{
		    body.append(U.toJson("success", false, "reason", "no confirmation"));		    
		}
	    }
	    else if (resource.startsWith("/events/purge"))
	    {		 
		if ("yes".equals(params.get("confirm")))
		{
		    int rows = mDao.purgeEvents();
		    mDao.addEvent(new Event("Purged"));
		    body.append(U.toJson("success", true, "rows", rows));
		}
		else
		{
		    body.append(U.toJson("success", false, "reason", "no confirmation"));        	    
		}
	    }
	    else if (resource.startsWith("/events/list"))
	    {		 
		long maxAgeSeconds = 30*24*60*60; // one month
		List<Event> events = mDao.getAllEvents(maxAgeSeconds);

		body.append("{ \"success\":").append(events!=null).append(",\"events\":[\n");		
		if (null!=events)
		{
		    boolean first = true;
		    for (Event e : events)
		    {			
			String json = e.toJson();
			body.append(first?"":",\n").append(json);
			first = false;
		    }
		}
		body.append("]}"); 
		//resp = new Response(Status.OK, CT_TEXT_JSON, body.toString());
		//resp.addHeader("Refresh", "10"); // auto update every 10s
		//return r;
	    }
	    else if (resource.startsWith("/discover/start"))
	    {		 
		startDiscovery();
		body.append("{ \"success\" : true, \"discovering\" : " + true + " }");
	    }
	    else if (resource.startsWith("/discover/cancel"))
	    {		 
		cancelDiscovery();
		body.append("{ \"success\" : true, \"discovering\" : " + false + " }");
	    }
	    else if (resource.startsWith("/discover/toggle"))
	    {		 
		boolean discovering = toggleDiscovery();
		body.append("{ \"success\" : true, \"discovering\" : " + discovering + " }");
	    }
	    else if (resource.startsWith("/device/select"))
	    {		 
		String address = params.get("addr");
		BluetoothDevice device = findDevice(address);
		if (null==device)
		{
		    body.append("{ \"success\" : false, \"error\" : \"Unknown device\" }");
		}
		else
		{
		    cancelDiscovery();
		    try
		    {
			SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(GardenoidService.this);
			p.edit().putString(PREFKEY_BT_DEVICE_ADDR, address).commit();
		    }
		    catch (Exception e)
		    {
			System.err.println(PREFKEY_BT_DEVICE_ADDR + ":" + e);
		    }

		    boolean success = mConnectThread.connectTo(device);
		    mDao.addEvent(new Event("connectTo", "name", device.getName(), "addr", device.getAddress()));
		    String error = success ? "none" : "Timeout";
		    body.append("{ \"success\" : ").append(success).append(", \"error\": \"").append(error).append("\" }");
		}
	    }
	    else if (resource.startsWith("/connection/stop"))
	    {		 
		boolean success = mConnectThread.disconnect();
		body.append("{ \"success\" : " + success + " }");
	    }
	    else if (resource.startsWith("/onetime/list"))
	    {
		long nowUnixtime = DAO.nowUnixtime();
		long mask = mOneTimeContainer.getMask();
		body
    		.append("{")
    		.append( " \"now\": ").append(nowUnixtime)
    		.append(", \"mask\": ").append(mask)
    		.append(", \"onetime\": ").append(mOneTimeContainer.toJson(nowUnixtime))
    		.append("}");
	    }
	    else if (resource.startsWith("/onetime/add"))
	    {
		int no   = U.toInt(params.get("no"));
		int secs = U.toInt(params.get("secs"));
		body.append(mOneTimeContainer.addTime(no, secs).toJson(no));
	    }
	    else if (resource.startsWith("/onetime/set"))
	    {
		int no   = U.toInt(params.get("no"));
		int secs = U.toInt(params.get("secs"));
		body.append(mOneTimeContainer.setTime(no, secs).toJson(no));
	    }
	    else if (resource.startsWith("/onetime/stop"))
	    {
		int no = U.toInt(params.get("no"));
		body.append(mOneTimeContainer.stop(no).toJson(no));        		
	    }
	    else if (resource.startsWith("/schedules/list") || resource.startsWith("/schedules/active"))
	    {   
		//System.out.println("/schedules/list: params=" + params);
		String newFingerprint;
		String oldFingerprint = params.get("fingerprint");

		for (int trial=4*50; trial>0; trial--)
		{
		    Date now = Calendar.getInstance().getTime();
		    newFingerprint = "" + mDao.getLastChangeOnSchedules() + "_" + (now.getTime()/1000/60); 
		    if (!newFingerprint.equals(oldFingerprint)) break;

		    //System.err.println("Waiting for change: " + oldFingerprint + " " + newFingerprint);
		    U.mSleep(250);
		}

		Date now = Calendar.getInstance().getTime();
		newFingerprint = "" + mDao.getLastChangeOnSchedules() + "_" + (now.getTime()/1000/60); 
		System.err.println("" + resource + ": *********** stopped waiting for a change: " + oldFingerprint + " -> " + newFingerprint + "***********");

		U.mSleep(500);
		System.err.println("" + resource + ": *********** returning new fingerprint: " + newFingerprint + " ***********");

		List<Schedule> all = null;
		if (resource.startsWith("/schedules/list"))
		{
		    all = mDao.getAllSchedules(now);
		}
		else
		{
		    all = mDao.getActiveSchedules(now);
		}

		boolean first = true;
		body.append(
			"{ \"schedules\" :\n" +
			"  [\n");
		for (Schedule s : all)
		{
		    if (!first) body.append(",\n");
		    first = false;        	    
		    body.append(s.toJson());        	    
		}        	
		body.append(
			"\n" +
				"  ],\n" +
				" \"fingerprint\" : \"" + newFingerprint + "\"\n" +
			"}\n");
	    }
	    else if (resource.startsWith("/schedules/get"))
	    {
		int id = U.toInt(params.get("id"));        	
		Schedule s = mDao.getSchedule(id);

		body.append("{ \"schedule\" :\n");
		body.append(s.toJson());    
		body.append("\n}");        	
	    }
	    else if (resource.startsWith("/schedules/modify"))
	    {
		long id = -1;
		try
		{
		    String action  = params.get("action");
		    if (action.equals("del"))
		    {
			id = U.toLong(params.get("id"));
			int rows = mDao.deleteSchedule(id);
			body.append("{ \"success\" : true, \"id\" : ").append(id).append(", \"op\" : \"delete\" }");

			mDao.addEvent(new Event(mActiveStrandsMask, "deleteSchedule", "id", id, "rows",rows));
		    }
		    else
		    {
			int strandMask = U.toInt(params.get("strandMask"));
			int dayMask    = U.toInt(params.get("dayMask"));
			int monthMask  = U.toInt(params.get("monthMask"));

			int startTime  = U.hhmmToInt(params.get("startTime"));
			int endTime    = U.hhmmToInt(params.get("endTime"));
			int duration   = U.hhmmToInt(params.get("duration"));
			int interval   = U.hhmmToInt(params.get("interval"));

			Integer idCondition = toInteger(params.get("idCondition"));
			Integer idException = toInteger(params.get("idException"));

			String conditionArgs = params.get("conditionArgs");
			String exceptionArgs = params.get("exceptionArgs");

			Schedule schedule = new Schedule(strandMask, dayMask, monthMask, 
				startTime, endTime, duration, interval, 
				idCondition, conditionArgs,
				idException, exceptionArgs, 
				false /* not powered on */);

			if (action.equals("add"))
			{
			    id = mDao.addSchedule(schedule);
			    body.append("{ \"success\" : true, \"id\" : ")
			    .append(id)
			    .append(", \"op\" : \"insert\" }");
			    mDao.addEvent(new Event(mActiveStrandsMask, "addSchedule", schedule.toJson()));
			} 
			else if (action.equals("edit"))
			{
			    id = U.toLong(params.get("id"));
			    schedule.setId(id);
			    mDao.updateSchedule(schedule);
			    body.append("{ \"success\" : true, \"id\" : ")
			    .append(id)
			    .append(", \"op\" : \"update\" }");
			    mDao.addEvent(new Event(mActiveStrandsMask, "updateSchedule", schedule.toJson()));
			}
		    }

		    System.err.println("List of schedules was modifies ==> calling checkSchedules()");
		    checkSchedules();
		}
		catch (Exception e)
		{
		    mDao.logException(e);
		    String err  = ("" + e).replaceAll("\"",  "'");
		    String json = "{ \"success\" : false, \"id\" : " + id + ", \"error\" : \"" + err + "\" }";
		    resp = new JsonResponse(Status.INTERNAL_ERROR, json);
		}
	    }
	    else if (resource.startsWith("/db/stats"))
	    {
		body.append(mDao.getStats());		
	    }
	    else if (resource.startsWith("/status"))
	    {
		String oldFingerprint = "" + params.get("fingerprint");    
		String newFingerprint = "";

		boolean changed = false;
		int     onetimeMask = 0;
		// 100 * 100ms = 10secs
		for (int trials=100; trials>0 && !changed; trials--)
		{
		    onetimeMask = mOneTimeContainer.getMask();
		    if (0==trials%10)
		    {
			if (DEBUG_ONETIME_CONTAINER) System.err.println("oneTimeSchedules: onetimeMask=" + onetimeMask + ", mOneTimeContainer=" + mOneTimeContainer);
		    }
		    newFingerprint = "" + mServiceVersion + "_" + mDao.getLastReconfigId() + "_" + isConnected() + "_" + isDiscovering() + "_" + U.urlEncode(getCurrentPeer()) + "_" + (mActiveStrandsMask|onetimeMask) + "_" + mOneTimeContainer.getLastChangeTime();    
		    changed = !newFingerprint.equalsIgnoreCase(oldFingerprint);
		    if (!changed)
		    {
			U.mSleep(100);
			//U.mSleep(4*250);
		    }        	    
		}

		if (DEBUG_ONETIME_CONTAINER) System.err.println("oneTimeSchedules: onetimeMask=" + onetimeMask + ", mOneTimeContainer=" + mOneTimeContainer);
		Calendar now = U.now();
		long nowUnixtime = DAO.datetimeToUnixtime(now);
		body.append("{ \"changed\": ").append(changed);
		// version will let HTML frontend detect when to reload the whole page because of reinstall:
		// adding last strand update time to make page reload when strand names were changed
		body.append(", \"version\":\"").append(mServiceVersion).append("\"");  
		body.append(", \"reconfig\":\"").append(mDao.getLastReconfigId()).append("\"");  
		body.append(", \"discovering\":").append(isDiscovering());
		body.append(", \"connected\":").append(isConnected());
		body.append(", \"peer\":").append(U.nullOrEscapedInDoubleQuotes(getCurrentPeer()));
		body.append(", \"now\":\"").append(U.toYYYYMMDD_hhmmss(now)).append("\"");
		body.append(", \"unixtime\":").append(nowUnixtime);
		body.append(", \"fingerprint\":").append(U.nullOrEscapedInDoubleQuotes(newFingerprint));
		body.append(", \"power\":").append(mActiveStrandsMask|onetimeMask);
		body.append(", \"scheduled\":").append(mActiveStrandsMask);
		body.append(", \"onetime\":").append(onetimeMask);
		body.append(", \"onetimeList\":").append(mOneTimeContainer.toJson(nowUnixtime));
		body.append("}\n");

		if (DEBUG_ONETIME_CONTAINER)
		{
		    System.err.println("********* SENDING STATUS *********: ");
		    System.err.println("old status: " + params);
		    System.err.println("new status: " + body.toString());
		}
	    }
	    else if (resource.startsWith("/queue/list"))
	    {
		body.append(mCmdQueue.toJson());
	    }
	    else if (resource.startsWith("/command"))
	    {
		String name = resource.substring("/command/".length()); 
		CommandQueue.Command cmd = mCmdQueue.send(name, true /* do wait */);
		if (!cmd.ready)
		{
		    body.append("{ error : \"timeout\" }");
		}
		else 
		{
		    body.append(cmd.success);
		    body.append(cmd.error);
		}
	    }
	    else if (resource.startsWith("/weather/list"))
	    {
		String day    = params.get("day");
		//String browse = params.get("browse");

		//		int offset = 0;
		//		if ("next".equals(browse))
		//		{
		//		    day = adjustDay(day, 1);
		//		}
		//		else if ("prev".equals(browse))
		//		{
		//		    day = adjustDay(day,- 1);
		//		}
		//		params.put("day", day);

		List<Weather> list = (null==day) ? mDao.getAllWeathers() : mDao.getAllWeathers(day);
		StringBuilder sb   = body;
		sb.append("{ \"weather\" :\n[\n");
		boolean first = true;
		if (null!=list) for (Weather w : list)
		{
		    sb.append(first ? "" : ",\n");
		    sb.append(w.toJson());
		    first = false;
		}
		sb.append("\n], \n");
		sb.append("\"day\" : ").append(U.escapedOrNull(day));
		sb.append("}");
	    }
	    else if (resource.startsWith("/forecast/list"))
	    {
		List<com.apdlv.gardenoid.db.Forecast> list = mDao.getAllForecasts();
		StringBuilder sb = body;
		sb.append("{ \"forecasts\" :\n[\n");
		Calendar today = DAO.todayNoon();
		boolean first = true;
		for (com.apdlv.gardenoid.db.Forecast w : list)
		{
		    sb.append(first ? "" : ",\n");
		    sb.append(w.toJson(today));
		    first = false;
		}
		sb.append("\n]\n}");
	    }
	    else if (resource.startsWith("/forecast/get"))
	    {
		try
		{
		    String placeCode = params.get("p");
		    String tempUnit  = params.get("u");

		    if (null==placeCode) placeCode = WeatherUpdateThread.PLACECODE_COLOGNE; 
		    if (null==tempUnit)  tempUnit  = TEMP_UNIT_CELSIUS;

		    WeatherConditions cond = mForecastProvider.getForecast(placeCode, tempUnit);	            
		    StringBuilder sb = body;	            
		    sb.append("{\n  ");
		    dump("astronomy",  cond.getAstronomy(),  sb); sb.append(",\n  ");
		    dump("atmosphere", cond.getAtmosphere(), sb); sb.append(",\n  ");
		    dump("condition",  cond.getCondition(),  sb); sb.append(",\n  ");
		    dump("location",   cond.getLocation(),   sb); sb.append(",\n  ");
		    dump("units",      cond.getUnits(),      sb); sb.append(",\n  ");
		    dump("wind",       cond.getWind(),       sb); sb.append(",\n");

		    sb.append("  \"forecasts\" :\n  [\n");
		    Vector<Forecast> fcs = cond.getForecasts();
		    boolean first = true;
		    for (Forecast fc : fcs)
		    {
			String date = fc.getDate();
			String code = fc.getCode();
			String low  = fc.getLow();
			String high = fc.getHigh();
			sb.append(first ? "    " : ",\n    ");
			sb.append("{ \"date\" : \"").append(date).append("\", ");
			sb.append(", \"code\" : \"").append(code).append("\", ");
			sb.append(", \"low\" : \"" ).append(low ).append("\", ");
			sb.append(", \"high\" : \"").append(high).append("\" }");
			first = false;
		    }
		    sb.append("\n  ]");	            
		    sb.append("\n}");	            
		} 
		catch (Exception e)
		{
		    mDao.logException(e);
		    body.append(U.toJson("success", false, "exception", ""+e));
		    resp = new JsonResponse(Status.INTERNAL_ERROR, body.toString());
		}        	
	    }
	    else
	    {
		resp = new JsonResponse(Status.NOT_FOUND, U.toJson("success", false, "reason", "Resource '" + resource + "' does not exist"));
	    }

	    if (null==resp)		
	    {
		resp = new JsonResponse(body.toString());
	    }
	    
	    String content = body.toString();
	    if (gzipAccepted && content.length()>1024)
	    {
		// TODO: add compression here
		InputStream is = TemplateEngine.compress(resource, content); //new GZIPInputStream(new ByteArrayInputStream(content.getBytes()));
		resp = new JsonResponse(Status.OK, is);
		resp.addHeader("Content-Encoding", "gzip");
	    }
	    
	    resp = new Response(Status.OK, CT_TEXT_JSON, content);
	    resp.addHeader("Pragma", "no-cache"); 
	    return resp;
	}

//	private String adjustDay(String day, int i) throws ParseException
//	{
//	    Date date = U.YYYYMMDD.parse(day);
//	    Calendar c = Calendar.getInstance();
//	    c.setTime(date);
//	    c.add(Calendar.DAY_OF_MONTH, i);
//
//	    day = U.YYYYMMDD.format(c.getTime());
//	    return day;
//	}

//	@Override 
//	public Response serve(IHTTPSession session) 
//	{
//	    try
//	    {
//		return serveUnsecurely(session);
//	    }
//	    catch (Exception e)
//	    { 
//		e.printStackTrace();
//		return new Response(Status.INTERNAL_ERROR, CT_TEXT_PLAIN, ""+e);
//	    }
//	}

	final private String FAVICON_B64 = "iVBORw0KGgoAAAANSUhEUgAAABAAAAAQEAYAAABPYyMiAAAABmJLR0T///////8JWPfcAAAACXBIWXMAAABIAAAASABGyWs+AAAAF0lEQVRIx2NgGAWjYBSMglEwCkbBSAcACBAAAeaR9cIAAAAASUVORK5CYII=";
	final private byte[] FAVICON_DATA = Base64.decode(FAVICON_B64,Base64.DEFAULT);

//	public Response serveUnsecurely(IHTTPSession session) throws ParseException, JSONException
//	{
//	    Method method = session.getMethod();
//	    String uri    = session.getUri();
//	    return serveUnsecurely(method, uri);
//	}
	
	private String mTempUnit = "C"; // celsius
	private Cookie getOrCreateCookie(Map<String, String> headers, Cookie session)
	{
	    String name   = headers.get("cookie");
	    Cookie cookie = (null==name) ? null : mCookies.get(name);	    
	    if (null==cookie)
	    {
		cookie = createCookie(
			false, // normal cookie, not a session 
			Cookie.isMobile(session)); // mobile if session is
	    }
	    cookie.prolong(600);
	    return cookie;
	}
	
	public Cookie getSession(Map<String, String> params)
	{
	    synchronized (mCookies)
	    {
		return mCookies.get(params.get("session"));
	    }
	}
	
	public Response serveUnsecurely(Method method, String uri, Map<String, String> params, Map<String, String> headers, String remoteAddr) throws ParseException, JSONException, IOException 
	{
	    if (uri.startsWith("/favicon.ico"))
	    {
		return new Response(Status.OK, CT_IMAGE_XICON, new ByteArrayInputStream(FAVICON_DATA));
	    }
		
	    String ae = headers.get("accept-encoding");
	    boolean gzipAccepted   = (null!=ae && ae.contains("gzip"));
	    boolean isLocalRequest = remoteAddr.startsWith("127.0.0.1"); // || remoteAddr.startsWith("10.2.5.52:");

	    // jquery need to be accessible also when not authorized (since used in login.html"
	    if (uri.startsWith("/js/jquery.min.js"))
	    {		
		InputStream is = mTemplateEngine.getFile("js/jquery.min.js", gzipAccepted);
		return new CompressedResponse(CT_JAVASCRIPT, is, gzipAccepted);
	    }
	    	    
	    Cookie session = getSession(params);	    
	    Cookie cookie  = getOrCreateCookie(headers, session);	    
	    System.out.println("serveUnsecurely: " + method + " '" + uri + "', cookie=" + cookie);
	    
	    if (Cookie.isAuthorized(session) || isLocalRequest)
	    {
		cookie.setAuthorized(true);
	    }	    	    
	    
	    if (!uri.startsWith("/rest") && !uri.startsWith("/js") && !uri.startsWith("/img") && !uri.startsWith("/css"))
	    {
		// just for setting break point here for regular pages 
                uri += "";
	    }

	    if (uri.endsWith("/logout.html") )
	    {
		cookie.setAuthorized(false);
		uri =  "/login.html";
	    }
	    
	    if (isLocalRequest && uri.endsWith("/password.html"))
	    {
		String submit = params.get("submit");
		String pass1  = params.get("pass1");
		String pass2  = params.get("pass2");
		
		if (null==submit)
		{
		    InputStream is = mTemplateEngine.getFile(uri, gzipAccepted);
		    return new CompressedHtmlResponse(is, gzipAccepted);
		}
		else if (null==pass1 || null==pass2 || pass1.length()<1)
		{
		    return new Redirect("/password.html?error=Passwords+missing", cookie.getName(), "Password(s) missing");
		}
		else if (U.matches(pass1, pass2))
		{
		    if (setAdminPassword(pass1))
		    {
			return new Redirect("/login.html", cookie.getName(), "Admin password accepted");
		    }
		    else
		    {
			return new Redirect("/password.html?error=Failed+to+set+password.", cookie.getName(), "Failed to set admin password");
		    }
		}
		
		return new Redirect("/password.html?error=Password+missmatch", cookie.getName(), "Passwords do not match");
	    }
	    
	    if (uri.endsWith("/login.html"))
	    {
		String passwordExpected = getAdminPassword();
		if (null==passwordExpected)		    
		{
		    if (isLocalRequest)
		    {
			return new Redirect("/password.html", cookie.getName(), "Password setup required");
		    }
		    else
		    {
			InputStream is = mTemplateEngine.getFile("reject.html", gzipAccepted);
			Response r = new Response(Status.OK, CT_TEXT_HTML, is);
		        if (gzipAccepted)
		        {
		            r.addHeader("Content-Encoding", "gzip");
		        }
			return r;
		    }
		}

		String passwordSubmitted = params.get("pass");		
		boolean authorized = cookie.isAuthorized() || (U.matches(passwordExpected,passwordSubmitted));						
		if (authorized) 
		{
		    boolean mobile = isMobileDevice(headers);
		    cookie.setMobile(mobile);		    
		    cookie.setAuthorized(authorized);
		    
		    String location = mobile ? "/mobile.html" : "/desktop.html";		    
		    return new Redirect(location, cookie.getName(), "Login sucessful");
		}
		
		if (null!=passwordSubmitted && passwordSubmitted.length()>0)
		{
		    return new Redirect("/login.html?error=Invalid+password", cookie.getName());
		}
		
		InputStream is = mTemplateEngine.getFile(uri, gzipAccepted);
		Response r = new CompressedHtmlResponse(is, gzipAccepted, cookie.getName());
//	        if (gzipAccepted)
//	        {
//	            r.addHeader("Content-Encoding", "gzip");
//	        }
//		r.addHeader("Set-Cookie", cookie.getName());
		return r;
	    }
	    
	    if (uri.startsWith("/mobile.html"))
	    {
		cookie.setMobile(true);
		//System.out.println("mobile.html: cookie=" + cookie);
		uri = "/index.html";
	    }
	    else if (uri.startsWith("/desktop.html"))
	    {
		cookie.setMobile(false);		
	    }
	    
	    if (cookie.isExpired() || !cookie.isAuthorized())
	    {
		if (uri.endsWith(".html") || uri.equals("/") || uri.equals(""))
		{
		    return new Redirect("/login.html", cookie.getName(), "Need to log in first."); 		
		} 
		// no authorization required for CSS (e.g. on login page)
		else if (!uri.endsWith(".css") && !uri.endsWith(".png"))
		{
		    return new Response(Status.NOT_FOUND, "text/plain", "Authorization required");
		}
	    }

	    if (uri.startsWith("/rest"))
	    {
		return createRestResponse(uri.substring(5), params, gzipAccepted);
	    }
	    else if (uri.startsWith("/api"))
	    {		
		return createApiResponse();		
	    }
	    else if (uri.endsWith(".gif") || uri.endsWith(".png") || uri.endsWith(".jpg")) 
	    {
		return createImageResponse(uri);
	    }            
	    else if (uri.equals("/js/global.js"))
	    {		
		return createGlobalJsResponse(params, cookie);
	    }	    
	    else if (uri.endsWith(".js") || uri.endsWith(".css"))
	    {
		return createStaticFileResponse(uri, gzipAccepted);
	    }

	    if ("/".equals(uri) || "".equals(uri)) 
	    { 
		uri="/index.html";
	    }

	    InputStream is = mTemplateEngine.getFile(uri, gzipAccepted);
	    if (null==is)
	    {            
		//page = mTemplateEngine.render("index.html", map);
		is = mTemplateEngine.getFile("index.html", gzipAccepted);
	    }

	    if (null==is)
	    {
		Response r = new Response(Status.NOT_FOUND, CT_TEXT_HTML, "Not found");
		r.addHeader("Connection", "close");
		return r; 
	    }

	    Response r =  new Response(Status.OK, CT_TEXT_HTML, is);
	    r.addHeader("Connection", "close");
	    r.addHeader("Set-Cookie", cookie.getName());
	    if (gzipAccepted)
	    {
		r.addHeader("Content-Encoding", "gzip");
	    }
	    
	    return r;
	}

        private Response createGlobalJsResponse(Map<String, String> params, Cookie cookie)
        {
	    boolean withStrands      = params.containsKey("with_strands");
	    boolean withConditionals = params.containsKey("with_conditionals");
	    
	    String script = "";
	    script += "var global_version = \"" +  mServiceVersion     + "\";\n";
	    script += "var global_cookie  = \"" + cookie.getName()     + "\";\n";
	    script += "var global_desktop = "   + (!cookie.isMobile()) + ";\n";
	    
	    if (withStrands)
	    {
	        script += "var global_strands = " + createStrandsJS() + ";\n";
	    }
	    
	    if (withConditionals)
	    {
	        script += "var global_conditionals = " + Conditional.CONDITIONALS_JSON + ";";
	    }

	    Response r = new Response(Status.OK, CT_JAVASCRIPT, script);
	    // prevent caching
	    r.addHeader("Cache-Control", "no-cache, no-store, must-revalidate");
	    r.addHeader("Pragma", "no-cache");
	    r.addHeader("Expires", "0");
	    return r;
        }

        private Response createStaticFileResponse(String uri, boolean gzipAccepted)
        {
	    InputStream is = mTemplateEngine.getFile(uri, gzipAccepted);
	    if (null!=is)
	    {
	        String expires = createExpirationDate();
	        String ct = getContentType(uri);

	        Response r = new Response(Status.OK, ct, is);
	        r.addHeader("Cache-Control", "Public");
	        r.addHeader("Expires", expires);
	        if (gzipAccepted)
	        {
	    		r.addHeader("Content-Encoding", "gzip");
	        }
	        return r;
	    }
	    return new Response(Status.NOT_FOUND, CT_TEXT_PLAIN, "Not found");
        }

        private Response createImageResponse(String uri)
        {            
	    String expires = createExpirationDate();
 	    InputStream is = mTemplateEngine.getRawFile(uri); 
	    String contentType = getContentType(uri);
	    Response r = new Response(Status.OK, contentType, is);
	    r.addHeader("Cache-Control", "Public");
	    r.addHeader("Expires", expires);
	    return r;
        }

        private Response createApiResponse()
        {
	    StringBuilder msg = new StringBuilder();	    
	    msg.append("<html><body>\n");
	    msg.append("<a href=\"/\">[MAIN]</a>\n");
	    msg.append("<hr/>\n");

	    String resources[] = 
	        { 
	    	"devices/list",  
	    	"events/list",  
	    	"events/purge?confirm=no",  
	    	"discover/start", "discover/cancel", "discover/toggle",
	    	"queue/list",
	    	"onetime/list",
	    	"onetime/add?no=1&secs=60",
	    	"onetime/set?no=2&secs=5",
	    	"onetime/stop?no=1",
	    	"schedules/list",
	    	"schedules/active",
	    	"schedules/get?id=17",
	    	"schedules/modify?action=del&id=1",
	    	"schedules/modify?action=add&strandMask=1&startTime=2300&endTime=2400&duration=0015&interval=0100&idCondition=103&conditionArgs=30&idException=0&exceptionArgs=&dayMask=1&monthMask=1",
	    	"status",
	    	"db/stats",
	    	"weather/list",
	    	"weather/compact?confirm=no",
	    	"forecast/list",
	    	"forecast/get?p=GMXX0018&u=c", // [p]lace code, temperature [u]nit
	    	"command/I", "command/S", "command/H",
	        };		

	    for (String res : resources)
	    {
	        msg.append("<a href=\"/rest/" + res + "\">/rest/" + res + "</a><br/>\n");
	    }

	    msg.append("<hr>\n");
	    msg.append("Connect to:<br/>\n");

	    msg.append("Paired:<br/>\n");
	    for (BluetoothDevice d : mPairedDevices)
	    {
	        String addr = d.getAddress();
	        String name = d.getName();
	        String res  = "device/select?addr=" + addr;
	        msg.append("<a href=\"/rest/" + res + "\">/rest/" + name + "</a><br/>\n");
	    }

	    msg.append("Visible:<br/>\n");
	    for (BluetoothDevice d : mVisibleDevices)
	    {
	        String addr = d.getAddress();
	        String name = d.getName();
	        String res  = "device/select?addr=" + addr;
	        msg.append("<a href=\"/rest/" + res + "\">/rest/" + name + "</a><br/>\n");
	    }

	    msg.append("<hr>\n");
	    String res = "connection/stop"; 
	    msg.append("<a href=\"/rest/" + res + "\">/rest/" + res + "</a><br/>\n");

	    msg.append("</body></html>");
	    return new NanoHTTPD_SSL.Response(Status.OK, CT_TEXT_HTML, msg.toString());
        }


	private String createStrandsJS()
        {
	    String strands[] = mDao.getAllStrands();		
	    StringBuilder sb = new StringBuilder("{");
	    for (int i=1; i<=8; i++)
	    {
	        String name = strands[i-1];
	        sb.append(i).append(":");
	        sb.append("{\"name\":\"").append(name).append("\"},");
	    }
	    sb.append("9:{\"name\":null}}");
	    String strandJS = sb.toString();
	    return strandJS;
        }

        private String getContentType(String uri)
        {
            uri = null==uri ? "" : uri.toLowerCase();
            if (uri.endsWith(".gif"))  return CT_IMAGE_GIF;
            if (uri.endsWith(".png"))  return CT_IMAGE_PNG;
            if (uri.endsWith(".jpg"))  return CT_IMAGE_JPG;
            if (uri.endsWith(".ico"))  return CT_IMAGE_XICON;
            if (uri.endsWith(".css"))  return CT_CSS;
            if (uri.endsWith(".js"))   return CT_JAVASCRIPT;
            if (uri.endsWith(".html")) return CT_TEXT_HTML;
            if (uri.endsWith(".txt"))  return CT_TEXT_PLAIN;
            if (uri.endsWith(".json")) return CT_TEXT_JSON;
	    return CT_TEXT_PLAIN;
        }

	private boolean isMobileDevice(Map<String, String> headers)
        {
	    String ua = headers.get("user-agent");
	    ua = (null==ua) ? "" : ua.toLowerCase();
	    if (ua.contains("ios") || ua.contains("android")) return true;
	    if (ua.contains("nokia") || ua.contains("ericsson") || ua.contains("blackberry")) return true;
	    return false;
        }


	private String createExpirationDate()
	{
	    Calendar calendar = Calendar.getInstance();
	    calendar.add(Calendar.DAY_OF_MONTH, 30);
	    SimpleDateFormat dateFormat = new SimpleDateFormat(
		    "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
	    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	    return dateFormat.format(calendar.getTime());
	}


	private void dump(String category, Map<String, String> map, StringBuilder sb)
	{
	    try
	    {
		sb.append(new JSONObject().put(category, new JSONObject(map)).toString());
	    } 
	    catch (JSONException e)
	    {
		log(e);
	    }

	    /*
            sb.append("\"").append(category).append("\" : ");
            if (null==map)
            {
        	sb.append(" : null");
            }
            else
            {            
        	sb.append("{");
        	boolean first = true;
        	for (String key : map.keySet())
        	{
        	    String val = map.get(key);
        	    sb.append(first ? " " : ", ").append("\"").append(key).append("\" : \"").append(val).append("\"");
        	    first = false;
        	}
        	sb.append("}");
            }            
	     */
	}


	private Map<String, String> toMap()
	{            
	    Map<String, String> m = new HashMap<String, String>();

	    m.put("nonce",       ""+Math.random());
	    m.put("discovering", ""+isDiscovering());
	    m.put("connected",   ""+isConnected());
	    m.put("unit_t", mTempUnit); 
	    String[] nameAddr = getConnectedNameAndAddr();
	    if (null!=nameAddr)
	    {
		m.put("connectedName", nameAddr[0]);
		m.put("connectedAddr", nameAddr[0]);
	    }
	    return m;
	}


	@SuppressWarnings("unused")
        private boolean equals(String a, String b)
	{
	    return null!=a && a.equals(b);
	}


	private Integer toInteger(String string)
	{
	    try { return Integer.parseInt(string); } catch (Exception e) { return null; }
	}


	private BluetoothDevice findDevice(String address)
	{
	    for (BluetoothDevice d : mPairedDevices)
	    {
		if (d.getAddress().equals(address))
		{
		    if (d.getAddress().equals(address)) return d;
		}
	    }
	    for (BluetoothDevice d : mVisibleDevices)
	    {
		if (d.getAddress().equals(address)) return d;
	    }
	    return null;
	}


	private void listDevices(Set<BluetoothDevice> pairedDevices, Set<BluetoothDevice> visibleDevices, StringBuilder msg)
	{
	    msg.append("{ \"devices\" : [");

	    boolean first = true;
	    for (BluetoothDevice d : pairedDevices)
	    {
		String name = d.getName();
		name = (null==name) ? "null" : "\"" + name.replaceAll("\"", "\\\"") + "\"";
		boolean visible = (null!=visibleDevices && visibleDevices.contains(d));
		msg.append(first ? "\n" : ",\n");
		msg.append(" {");        	
		msg.append(" \"addr\" : \"").append(d.getAddress()).append("\",");
		msg.append(" \"paired\" : ").append(true).append(", ");
		msg.append(" \"visible\" : ").append(visible).append(", ");
		msg.append(" \"name\" : ").append(name);
		msg.append(" }");
		first = false;
	    }

	    for (BluetoothDevice d : visibleDevices)
	    {
		if (!isAddressContained(pairedDevices, d.getAddress()))
		{
		    String name = d.getName();
		    name = (null==name) ? "null" : "\"" + name.replaceAll("\"", "\\\"") + "\"";
		    msg.append(first ? "\n" : ",\n");
		    msg.append(" {");
		    msg.append(" \"addr\" : \"").append(d.getAddress()).append("\",");
		    msg.append(" \"paired\" : ").append(false).append(",");
		    msg.append(" \"visible\" : ").append(true).append(",");
		    msg.append(" \"name\" : ").append(name);
		    msg.append(" }");
		    first = false;
		}
	    }
	    msg.append("\n]}");
	}

	private boolean isAddressContained(Set<BluetoothDevice> devices, String address)
	{
	    for (BluetoothDevice d : devices)
	    {
		if (address.equalsIgnoreCase(d.getAddress()))
		{
		    return true;
		}
	    }	    
	    return false;
	}

	@Override
        public Response serve(String uri, Method method, Map<String, String> headers, Map<String, String> parms, Map<String, String> files, String remoteAddr)
        {
	    Response r = null;
	    try
	    {
		long t1 = U.millis();
		r = serveUnsecurely(method, uri, parms, headers, remoteAddr);		
		long t2 = U.millis();
		
		System.out.println("serve: " + method + " " + uri + " in " + (t2-t1) + " ms");
		return r;
	    }
	    catch (Exception e)
	    { 
		System.err.println("Exception serving uri=" + uri + ", header=" + headers + ", parms=" + parms + ":");
		e.printStackTrace();
		r =  new Response(Status.INTERNAL_ERROR, CT_TEXT_PLAIN, ""+e);
	    }
	    
	    return r;
        }

    }
    
    @Override
    public boolean stopService(Intent intent)
    {
	mDao.addEvent(new Event("stopService", "action", intent.getAction()));
        boolean rc = super.stopService(intent);
        this.stopHttp();
        this.stopSelf();
        return rc;
    }

    private DAO mDao; 
    private long lastForecastUpdate = -1;
    private WeatherUpdateThread mUpdateThread = null; 
    private long mLastServiceDiscoveryFailureEventLogged = -1;
}
