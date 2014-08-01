package com.apdlv.gardenoid.db;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DAO extends SQLiteOpenHelper
{
    private static final String TAG = DAO.class.getSimpleName(); 
    
    // Database Version
    private static final int   DATABASE_VERSION = 26;
    // Database Name
    private static final String DATABASE_NAME = "ScheduleDB";
    // Table names
    private static final String TABLE_SCHEDULES = "schedules";
    private static final String TABLE_WEATHER   = "weather";
    private static final String TABLE_FORECAST  = "forecast";
    private static final String TABLE_EVENTS    = "events";
    private static final String TABLE_CODES    = "codes";


    private static final String[] COLUMNS_SCHEDULES = 
	{ "id","strand_mask","day_mask","month_mask","start_time","end_time","duration","interval","id_condition","condition_args","id_exception","exception_args","power" };
    private static final String[] COLUMNS_WEATHER  = 
	{ "id", "date", "code", "text", "temp", "humidity", "visibility", "pressure", "rising", "updated" }; 
    private static final String[] COLUMNS_FORECAST = 
	{ "id", "day",  "code", "text", "low", "high", "updated" }; 
    private static final String[] COLUMNS_EVENTS  = 
	{ "id", "active_mask", "date", "message", "json", };     
    private static final String[] COLUMNS_CODES =
	{ "id", "code", "imglink", };

    private static final String COLUMN_LIST = concatColumns(COLUMNS_SCHEDULES); 

    private static String concatColumns(final String array[])
    {
	StringBuilder sb = new StringBuilder();
	for (String s : array)
	{
	    sb.append(sb.length()>0 ? "," : "").append(s);
	}
	return sb.toString();
    }

    private long mLastChange;
    
    public DAO(Context context) 
    {
	super(context, DATABASE_NAME, null, DATABASE_VERSION);  
    }

    public DAO(Context context, String name, CursorFactory factory, int version)
    {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) 
    {	
	try
	{
	    String CREATE_TABLE = 
		    "CREATE TABLE IF NOT EXISTS codes " +
			    "( " +
			    "id INTEGER PRIMARY KEY AUTOINCREMENT " 
			    + ", code         INTEGER " 
			    + ", imglink      STRING "
			    + ")";
	    db.execSQL(CREATE_TABLE);
	    for (String column : new String[] { "code" })
	    {
		String SQL_IDX = "CREATE INDEX idx_codes_" + column + " ON codes (" + column + ")"; 
		db.execSQL(SQL_IDX);
	    }
	}
	catch (Exception e)
	{
	    StringWriter sw = new StringWriter();
	    e.printStackTrace(new PrintWriter(sw));
	    String exceptionAsString = sw.toString();
	    Log.e(TAG, "onCreate: " + exceptionAsString);
	}
	
	try
	{
	    //db.execSQL("DROP TABLE IF EXISTS " + TABLE_EVENTS);	    
	    // SQL statement to create schedule table
	    String CREATE_EVENTS_TABLE = 
		    "CREATE TABLE IF NOT EXISTS events " +
			    "( " +
			    "id INTEGER PRIMARY KEY AUTOINCREMENT " 
			    + ", date         INTEGER " 
			    + ", active_mask  INTEGER "
			    + ", message      STRING "
			    + ", json         STRING "
			    + ")";
	    // create schedules table
	    db.execSQL(CREATE_EVENTS_TABLE);
	    for (String column : new String[] { "date", "message" })
	    {
		String SQL_IDX = "CREATE INDEX idx_events_" + column + " ON events (" + column + ")"; 
		db.execSQL(SQL_IDX);
	    }
	}
	catch (Exception e)
	{
	    StringWriter sw = new StringWriter();
	    e.printStackTrace(new PrintWriter(sw));
	    String exceptionAsString = sw.toString();
	    Log.e(TAG, "onCreate: " + exceptionAsString);
	}
	
	try
	{
	    // SQL statement to create schedule table
	    String CREATE_SCHEDULE_TABLE = 
		    "CREATE TABLE IF NOT EXISTS schedules " +
			    "( " +
			    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
			    "strand_mask    INTEGER, " +
			    "day_mask       INTEGER, " +
			    "month_mask     INTEGER, " +
			    "start_time     INTEGER, " +
			    "end_time       INTEGER, " +
			    "duration       INTEGER, " +
			    "interval       INTEGER, " +
			    "id_condition   INTEGER, " +
			    "condition_args STRING,  " +
			    "id_exception   INTEGER, " + 
			    "exception_args STRING,  " +
			    "power          INTEGER  " + 
			    ")";
	    // create schedules table
	    db.execSQL(CREATE_SCHEDULE_TABLE);
	    for (String column : new String[] { "interval", "duration", "start_time", "end_time", "day_mask", "month_mask"})
	    {
		String SQL_IDX = "CREATE INDEX idx_schedule_" + column + " ON schedules (" + column + ")"; 
		db.execSQL(SQL_IDX);
	    }

	    String CREATE_WEATHER_TABLE =
		    "CREATE TABLE IF NOT EXISTS weather (" +
			    "id         INTEGER PRIMARY KEY AUTOINCREMENT, " +
			    "date       INTEGER, " +
			    "code       INTEGER, " +
			    "text       TEXT, "    +
			    "temp       INTEGER, " +
			    "humidity   INTEGER, " +
			    "visibility REAL," +
			    "pressure   REAL," + 
			    "rising     REAL," + 
			    "updated    INTEGER"   +			    
			    ")";
	    db.execSQL(CREATE_WEATHER_TABLE);
	    for (String column : new String[] { "date", "updated", "code", "temp"})
	    {
		String SQL_IDX = "CREATE INDEX idx_weather_" + column + " ON weather (" + column + ")"; 
		db.execSQL(SQL_IDX);
	    }

	    String CREATE_FORECAST_TABLE =
		    "CREATE TABLE IF NOT EXISTS forecast (" +
			    "id      INTEGER PRIMARY KEY AUTOINCREMENT, " +
			    "day     INTEGER, " +
			    "code    INTEGER, " +
			    "text    TEXT, "    +
			    "low     INTEGER, " +
			    "high    INTEGER, " +
			    "updated INTEGER"   +
			    ")";
	    db.execSQL(CREATE_FORECAST_TABLE);
	    for (String column : new String[] { "day", "updated", "code", "low", "high" })
	    {
		String SQL_IDX = "CREATE INDEX idx_forecast_" + column + " ON forecast (" + column + ")"; 
		db.execSQL(SQL_IDX);
	    }
	    Log.d(TAG, "onCreate: complete");
	}
	catch (Exception e)
	{
	    StringWriter sw = new StringWriter();
	    e.printStackTrace(new PrintWriter(sw));
	    String exceptionAsString = sw.toString();
	    Log.e(TAG, "onCreate: " + exceptionAsString);
	}
    }

    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
	// Drop older schedules table if existed
//	db.execSQL("DROP TABLE IF EXISTS " + TABLE_SCHEDULES);
//	db.execSQL("DROP TABLE IF EXISTS " + TABLE_WEATHER);
//	db.execSQL("DROP TABLE IF EXISTS " + TABLE_FORECAST);
//	db.execSQL("DROP TABLE IF EXISTS " + TABLE_EVENTS);

	// create fresh schedules table
	this.onCreate(db);

	// create test data
	Schedule testSchedules[] = 
	    {	
//		new Schedule(1, 127, 4095,  800, 2300,  100,  100, Conditional.COND_CURR_TEMP_ABOVE, "30", 0, null, false),
//		new Schedule(2, 127, 4095, 1215, 1815,   15,  400, 0, null, 0, null, false), 
//		new Schedule(2,   1, 4095, 1215, 1815,   15,  200, 0, null, 0, null, false), 
//		new Schedule(2,   2, 4095, 1215, 1815,  100,  200, 0, null, 0, null, false), 
//		new Schedule(2,   4, 4095, 1215, 1815,   15,  200, 0, null, 0, null, false), 
//		new Schedule(2,   8, 4095, 1215, 1815,   15,  200, 0, null, 0, null, false), 
//		new Schedule(2,  16, 4095, 1215, 1815,   15,  200, 0, null, 0, null, false), 
//		new Schedule(2,  32, 4095, 1215, 1815,   15,  200, 0, null, 0, null, false), 
//		new Schedule(2,  64, 4095, 1215, 1815,   15,  200, 0, null, 0, null, false),
	    };
	
	Weather testWeathers[] = 
	    {
		new Weather(now(), 28, "Test weather", 17, 90, 9.99f, 1015.92f, 0.0f)
	    };

	Forecast testForecasts[] = 
	    {
		new Forecast(now(), 28, "Test forecast", 17, 24)
	    };

	for (Schedule s : testSchedules)
	{
	    addSchedule(db, s);
	}
	for (Weather w : testWeathers)
	{
	    addWeather(db, w);
	}
	for (Forecast f : testForecasts)
	{
	    addForecast(db, f);
	}
    }

    public long addOrUpdateCode(long code, String url)
    {
        // 1. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();
        long id = -1;
        try
        {
            id = updateCode(db, code, url);
        }
        catch (Exception e)
        {
            Log.e(TAG, ""+e);
        }
        
        if (id<1)
        {
            try
            {
                id = addCode(db, code, url);
            }
            catch (Exception e)
            {
                Log.e(TAG, ""+e);
            }            
        }
        
        // 4. close            
        db.close();
        return id;
    }
    
    
    private long updateCode(SQLiteDatabase db, long code, String url)
    {
        ContentValues values = codeToContentValues(code, url);
        int i = db.update(TABLE_CODES, // table
        	  values,  // column/value
        	  "code=?",
        	  new String[] { String.valueOf(code) }); //selection args
        // do NOT close here
        //db.close();
        return i;
    }





    public long addSchedule(Schedule schedule)
    {
        // 1. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();
        long id = addSchedule(db, schedule);
        // 4. close
        db.close();
        return schedule.setId(id);
    }

    public void purgeEvents()
    {
        SQLiteDatabase db = this.getWritableDatabase();
	db.execSQL("DELETE FROM " + TABLE_EVENTS);
    }

    public long addEvent(Event event)
    {
	long id = -1;
	SQLiteDatabase db = null;
	
	try
	{
	    // 1. get reference to writable DB
	    db = this.getWritableDatabase();
	    id = addEvent(db, event);
	    // 4. close
	}	
	catch (Exception e)
	{
	    Log.e(TAG, "addEvent: " + e);
	}
	finally
	{
	    if (null!=db) db.close();
	}
	
	return id;
    }


    public long insertOrUpdateWeather(Weather weather)
    {
        // 1. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();
        int  rc = updateWeather(db, weather);
        if (rc<1) // no rows affected?
        {
            long id = addWeather(db, weather);
            // 4. close
            db.close();
            return id;
        }
        return -1;
    }

    
    private int updateWeather(SQLiteDatabase db, Weather weather)
    {
	//for logging
	Log.d("updateWeather", weather.toString());
	long unixtime = datetimeToUnixtime(weather.getDate());

	System.out.println("updateWeather: date=" + unixtime);

	ContentValues values = weatherToContentValues(weather);
	int i = db.update(TABLE_WEATHER, // table
		values,  // column/value
		"date=?",
		new String[] { String.valueOf(unixtime) }); //selection args
	// do NOT close here
	//db.close();
	return i;
    }

    
    public long addWeather(Weather weather)
    {
        // 1. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();
        long id = addWeather(db, weather);
        // 4. close
        db.close();  
        return id;
    }

    
    public Weather getWeather(long maxAgeSeconds)
    {
        SQLiteDatabase db = this.getReadableDatabase();
	long threshold = nowUnixtime()-maxAgeSeconds;
	
	// "SELECT * FROM weather WHERE date>" + threshold + " ODER BY date DESC"; 	
	Cursor cursor = 
		db.query(TABLE_WEATHER, // a. table
			 COLUMNS_WEATHER, // b. column names
			 " date>?", // c. selections 
			 new String[] { String.valueOf(threshold) }, // d. selections args
			 null, // e. group by
			 null, // f. having
			 "date DESC", // g. order by
			 null); // h. limit
	
	if (cursor==null || !cursor.moveToFirst()) 
	    return null;
	
	// 4. build schedule object
	Weather w = cursorToWeather(cursor);

	//log 
	Log.d(TAG, "getWeather("+maxAgeSeconds+"): " + w);

	// 5. return schedule
	return w;
    }

    
    public List<Weather> getAllWeather(long maxAgeSeconds)
    {
        SQLiteDatabase db = this.getReadableDatabase();
	long threshold = nowUnixtime()-maxAgeSeconds;
	List<Weather> list = new LinkedList<Weather>();
	
	// "SELECT * FROM weather WHERE date>" + threshold + " ODER BY date DESC"; 	
	Cursor cursor = 
		db.query(TABLE_WEATHER, // a. table
			 COLUMNS_WEATHER, // b. column names
			 " date>?", // c. selections 
			 new String[] { String.valueOf(threshold) }, // d. selections args
			 null, // e. group by
			 null, // f. having
			 "date DESC", // g. order by
			 null); // h. limit
	
	if (cursor==null || !cursor.moveToFirst()) 
	    return null;
	
	do 
	{
	    Weather w = cursorToWeather(cursor);
	    // Add schedule to schedules
	    list.add(w);
	} 
	while (cursor.moveToNext());
	
	// 5. return schedule
	return list;
    }

    public List<Event> getAllEvents(long maxAgeSeconds)
    {
	try
	{
	    SQLiteDatabase db = this.getReadableDatabase();
	    long threshold = nowUnixtime()-maxAgeSeconds;
	    List<Event> list = new LinkedList<Event>();

	    // "SELECT * FROM weather WHERE date>" + threshold + " ODER BY date DESC"; 	
	    Cursor cursor = 
		    db.query(TABLE_EVENTS, // a. table
			    COLUMNS_EVENTS, // b. column names
			    "date>?", // c. selections 
			    new String[] { String.valueOf(threshold) }, // d. selections args
			    null, // e. group by
			    null, // f. having
			    "date DESC", // g. order by newest first
			    null); // h. limit

	    if (cursor==null || !cursor.moveToFirst()) 
	    {
		Log.e(TAG,  "No events in database");
		return null;
	    }

	    do 
	    {
		Event e = cursorToEvent(cursor);
		// Add schedule to schedules
		list.add(e);
	    } 
	    while (cursor.moveToNext());

	    // 5. return schedule
	    return list;
	}
	catch (Exception e)
	{
	     e.printStackTrace();
	    return null;
	}
    }


    public Forecast getForecast(Calendar now, int daysInAdvance) // 0: today, 1: tomorrow
    {
	Calendar clone = (Calendar) now.clone();
	clone.add(Calendar.DAY_OF_YEAR, daysInAdvance);	
	long unixtime = dateToUnixtime(clone);
	
        SQLiteDatabase db = this.getReadableDatabase();
	
	// "SELECT * FROM weather WHERE date>" + threshold + " ODER BY date DESC"; 	
	Cursor cursor = 
		db.query(TABLE_FORECAST,  // a. table
			COLUMNS_FORECAST, // b. column names
			" day=?", // c. selections 
			new String[] { String.valueOf(unixtime) }, // d. selections args
			null,  // e. group by
			null,  // f. having
			null,  // g. order by
			null); // h. limit
	
	if (cursor == null)
	    return null;
	
	if (!cursor.moveToFirst())
	    return null;

	// 4. build schedule object
	Forecast w = cursorToForecast(cursor);

	//log 
	Log.d(TAG, "getForecast(futureDay="+daysInAdvance+"): " + w);

	// 5. return schedule
	return w;
    }

    
    public long addForecast(Forecast forecast)
    {
        // 1. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();
        long id = addForecast(db, forecast);
        // 4. close
        db.close();  
        return id;
    }

    
    private void updateLastChangeOnSchedules()
    {
	this.mLastChange = Calendar.getInstance().getTimeInMillis();
    }

    
    public long getLastChangeOnSchedules()
    {
	return mLastChange;
    }

    
    private long addEvent(SQLiteDatabase db, Event event)
    {
        //for logging
        Log.d("addEvent", event.toString()); 
    
        ContentValues values = eventToContentValues(event);
    
        // 3. insert
        long id = db.insert(TABLE_EVENTS, // table
        	null, //nullColumnHack
        	values); // key/value -> keys = column names/ values = column values
        // do NOT close here
        updateLastChangeOnSchedules(); 
        return id;
    }

    private long addSchedule(SQLiteDatabase db, Schedule schedule)
    {
        //for logging
        Log.d("addSchedule", schedule.toString()); 
    
        ContentValues values = scheduleToContentValues(schedule);
    
        // 3. insert
        long id = db.insert(TABLE_SCHEDULES, // table
        	null, //nullColumnHack
        	values); // key/value -> keys = column names/ values = column values
        // do NOT close here
        updateLastChangeOnSchedules(); 
        return id;
    }

    private long addCode(SQLiteDatabase db, long code, String url)
    {
        //for logging
        Log.d("addCode ", "" + code + ", " + url); 
    
        ContentValues values = codeToContentValues(code, url);
    
        // 3. insert
        long id = db.insert(TABLE_CODES, // table
        	null, //nullColumnHack
        	values); // key/value -> keys = column names/ values = column values
        // do NOT close here
        return id;
    }

    private long addWeather(SQLiteDatabase db, Weather weather)
    {
        //for logging
        Log.d("addWeather", weather.toString()); 
    
        ContentValues values = weatherToContentValues(weather);
    
        // 3. insert
        long id = db.insert(TABLE_WEATHER, // table
        	null, //nullColumnHack
        	values); // key/value -> keys = column names/ values = column values
        // do NOT close here
        //updateLastChange();
        return id;
    }

    private long addForecast(SQLiteDatabase db, Forecast forecast)
    {
        //for logging
        Log.d("addWeather", forecast.toString()); 
    
        ContentValues values = forecastToContentValues(forecast);
    
        // 3. insert
        long id = db.insert(TABLE_FORECAST, // table
        	null, //nullColumnHack
        	values); // key/value -> keys = column names/ values = column values
        // do NOT close here
        //updateLastChange();
        return id;
    }

    /*
    private int updateForecast(Forecast forecast)
    {
	SQLiteDatabase db = getWritableDatabase();
	int rc = updateForecast(db, forecast);
	db.close();
	return rc;
    }
    */
    
    public Schedule getSchedule(int id)
    {
	// 1. get reference to readable DB
	SQLiteDatabase db = this.getReadableDatabase();

	// 2. build query
	Cursor cursor = 
		db.query(TABLE_SCHEDULES, // a. table
			 COLUMNS_SCHEDULES, // b. column names
			 " id = ?", // c. selections 
			 new String[] { String.valueOf(id) }, // d. selections args
			 null, // e. group by
			 null, // f. having
			 null, // g. order by
			 null); // h. limit

	// 3. if we got results get the first one
	if (cursor != null)
	    cursor.moveToFirst();

	// 4. build schedule object
	Schedule schedule = cursorToSchedule(cursor, false);

	//log 
	Log.d("getSchedule("+id+")", schedule.toString());

	// 5. return schedule
	return schedule;
    }


    public String getUrlForCode(int code)
    {
	// 1. get reference to readable DB
	SQLiteDatabase db = this.getReadableDatabase();

	// 2. build query
	Cursor cursor = 
		db.query(TABLE_CODES, // a. table
			 COLUMNS_CODES, // b. column names
			 " code = ?", // c. selections 
			 new String[] { String.valueOf(code) }, // d. selections args
			 null, // e. group by
			 null, // f. having
			 null, // g. order by
			 null); // h. limit

	// 3. if we got results get the first one
	if (cursor != null)
	    cursor.moveToFirst();

	int n=0;
        @SuppressWarnings("unused")
        int id     = cursor.getInt(n++);
        @SuppressWarnings("unused")
        int code2  = cursor.getInt(n++);
	String url = cursor.getString(n++);

	// 5. return schedule
	return url;
    }


    public List<Schedule> getAllSchedules(int day, int month, int timeHHMM) 
    {
	final String ACTIVE_CONDITION = createActiveCondition(day, month, timeHHMM);	
//	System.err.println("ACTIVE_CONDITION: " + ACTIVE_CONDITION);
	
	// 1. build the actual query
	String query = 
		"SELECT " + COLUMN_LIST + ",\n" + ACTIVE_CONDITION + "\n" +
		"FROM " + TABLE_SCHEDULES;	
	//System.err.println("query: " + query);
	
	// 2. get reference to writable DB
	SQLiteDatabase db = getReadableDatabase();
	Cursor cursor = db.rawQuery(query, null);

	// 3. go over each row, build schedule and add it to list
	List<Schedule> schedules = new LinkedList<Schedule>();
	if (cursor.moveToFirst()) 
	{
	    do 
	    {
		Schedule schedule = cursorToSchedule(cursor, true);
		// Add schedule to schedules
		schedules.add(schedule);
	    } 
	    while (cursor.moveToNext());
	}

	Log.d("getAllSchedules()", schedules.toString());

	// return schedules
	return schedules;
    }
    
    

    public List<Weather> getAllWeathers() 
    {
	String query = "SELECT * FROM " + TABLE_WEATHER;	
	//System.err.println("query: " + query);
	
	// 2. get reference to writable DB
	SQLiteDatabase db = this.getWritableDatabase();
	Cursor cursor = db.rawQuery(query, null);

	// 3. go over each row, build schedule and add it to list
	List<Weather> weather = new LinkedList<Weather>();
	if (cursor.moveToFirst()) 
	{
	    do 
	    {
		Weather w = cursorToWeather(cursor);
		// Add schedule to schedules
		weather.add(w);
	    } 
	    while (cursor.moveToNext());
	}

	Log.d("getAllWeathers()", weather.toString());

	// return schedules
	return weather;
    }
    
    
    public List<Forecast> getAllForecasts() 
    {
	String query = "SELECT * FROM " + TABLE_FORECAST;	
	//System.err.println("query: " + query);
	
	// 2. get reference to writable DB
	SQLiteDatabase db = this.getWritableDatabase();
	Cursor cursor = db.rawQuery(query, null);

	// 3. go over each row, build schedule and add it to list
	List<Forecast> weather = new LinkedList<Forecast>();
	if (cursor.moveToFirst()) 
	{
	    do 
	    {
		Forecast f = cursorToForecast(cursor);
		// Add schedule to schedules
		weather.add(f);
	    } 
	    while (cursor.moveToNext());
	}

	Log.d("getAllForecasts()", weather.toString());

	// return schedules
	return weather;
    }
    
    
    public List<Schedule> getActiveSchedules(int day, int month, int timeHHMM)
    {
	final String ACTIVE_CONDITION = createActiveCondition(day, month, timeHHMM);
//	System.err.println("ACTIVE_CONDITION: " + ACTIVE_CONDITION);
	
	// 1. build the actual query
	String query = 
		"SELECT  " + COLUMN_LIST + ", 1\n" + // 1=active
		"FROM " + TABLE_SCHEDULES + "\n"   + 	
		"WHERE\n" + ACTIVE_CONDITION;	
	//System.err.println("query: " + query);
	
	// 2. get reference to writable DB
	SQLiteDatabase db = this.getWritableDatabase();
	Cursor cursor = db.rawQuery(query, null);

	// 3. go over each row, build schedule and add it to list
	List<Schedule> schedules = new LinkedList<Schedule>();
	if (cursor.moveToFirst()) {
	    do {
		Schedule schedule = cursorToSchedule(cursor, true);
		// Add schedule to schedules
		schedules.add(schedule);
	    } 
	    while (cursor.moveToNext());
	}

	Log.d("getAllSchedules()", schedules.toString());

	// return schedules
	return schedules;	
    }


    private static String createActiveCondition(int day, int month, int timeHHMM)
    {
	int dayBit   = 1<<day;       // days start at 0
	int monthBit = 1<<(month-1); // months start at 1
	int minutes  = hhmmToMinutes(timeHHMM);
	
	// make sure, that an interval==0 will not cause a division by zero:
	// if interval is 0,  nullif will return null and ifnull will return first non-null argument: end_time-start_time as
	// if interval is >0, nullif will return its first argument: interval
	final String INTERVAL =
		"ifnull(nullif(interval,0),end_time-start_time) ";
	
	final String ACTIVE_CONDITION =
		"  (day_mask & " + dayBit + ")>0 AND (month_mask & " + monthBit + ")>0\n" +
		"AND\n" +
		"  ( ((" + minutes + "-start_time) % " + INTERVAL + ") BETWEEN 0 AND duration )\n" + 
		"AND\n" + 
		"  ( " + minutes + " BETWEEN start_time AND end_time) \n"; 	
	return ACTIVE_CONDITION;
    }

    public void insertOrUpdateForecast(Forecast forecast)
    {
	SQLiteDatabase db = getWritableDatabase();
	try
	{
	    int i = updateForecast(db, forecast);
	    if (i<1) addForecast(db, forecast);
	}
	catch (Exception e)
	{
	    System.err.println("insertOrUpdateForecast: " + e);
	}	
	finally
	{
	    db.close();
	}
    }    
    

    public int updateSchedule(Schedule schedule) 
    {
	// 1. get reference to writable DB
	SQLiteDatabase db = this.getWritableDatabase();

	// 2. create ContentValues to add key "column"/value
	ContentValues values = scheduleToContentValues(schedule);

	// 3. updating row
	int i = db.update(TABLE_SCHEDULES, //table
		values, // column/value
		"id=?", // selections
		new String[] { String.valueOf(schedule.getId()) }); //selection args

	// 4. close
	db.close();

        updateLastChangeOnSchedules();
	return i;
    }
    
    
    private int updateForecast(SQLiteDatabase db, Forecast forecast)
    {
        //for logging
        Log.d("updateForecast", forecast.toString());
        long day = dateToUnixtime(forecast.getDay());
        
        System.out.println("updateForecast: day=" + day);

        ContentValues values = forecastToContentValues(forecast);
        int i = db.update(TABLE_FORECAST, // table
        	  values,  // column/value
        	  "day=?",
        	  new String[] { String.valueOf(day) }); //selection args
        // do NOT close here
        //db.close();
        return i;
    }


    public int deleteSchedule(long id /*Schedule schedule*/) 
    {
        // 1. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();
    
        // 2. delete
        int rows = db.delete(
        	TABLE_SCHEDULES, //table name
        	"id=?",  // selections
        	new String[] { String.valueOf(id /* schedule.getId()*/) }); //selections args
    
        // 3. close
        db.close();
    
        //log
        Log.d("deleteSchedule", "id " + id /*schedule.toString()*/);
        updateLastChangeOnSchedules();
        return rows;
    }
    

    private static Schedule cursorToSchedule(Cursor cursor, boolean withActiveColumn)
    {
        Schedule s = new Schedule();
        int n=0;
        s.setId(toInt(cursor.getString(n++)));
        s.setStrandMask(toInt(cursor.getString(n++)));
        s.setDayMask(toInt(cursor.getString(n++)));
        s.setMonthMask(toInt(cursor.getString(n++)));
        // all times need to be converted from the representation in the db (minutes since midnight)
        // to the more human readable representation as a decimal number HHMM 
        s.setStartTime(minutesToHHMM(cursor.getString(n++)));
        s.setEndTime(  minutesToHHMM(cursor.getString(n++)));
        s.setDuration( minutesToHHMM(cursor.getString(n++)));
        s.setInterval( minutesToHHMM(cursor.getString(n++)));
        // 
        s.setIdCondition(toInt(cursor.getString(n++)));
        s.setConditionArgs(cursor.getString(n++));
        s.setIdException(toInt(cursor.getString(n++)));
        s.setExceptionArgs(cursor.getString(n++));
        s.setPower(cursor.getInt(n++)>0);
        int active = withActiveColumn ? cursor.getInt(n++) : 0;
        s.setActive(active>0);
        return s;
    }

    
    private static Weather cursorToWeather(Cursor cursor)
    {
	Weather w = new Weather();
        int n=0;
        w.setId(toInt(cursor.getString(n++)));
        w.setDate(fromUnixtime(toInt(cursor.getString(n++))));
        w.setCode(toInt(cursor.getString(n++)));
        w.setText(cursor.getString(n++));
        w.setTemperature(toInt(cursor.getString(n++)));
        w.setHumidity(toInt(cursor.getString(n++)));
        w.setVisibility(toFloat(cursor.getString(n++)));
        w.setPressure(toFloat(cursor.getString(n++)));
        w.setRising(toFloat(cursor.getString(n++)));
        w.setUpdated(fromUnixtime(toInt(cursor.getString(n++))));
        return w;
    }

    
    private static Event cursorToEvent(Cursor cursor)
    {
	// { "id", "active_mask", "date", "message", "conditions", };
        int n=0;
        
        int id   = cursor.getInt(n++);
        int mask = cursor.getInt(n++);
	Calendar date = fromUnixtime(cursor.getLong(n++));
	String message = cursor.getString(n++);
	String json    = cursor.getString(n++);
	
	return new Event(id, date, mask, message, json);
    }

    
    private static float toFloat(String string)
    {
	return Float.parseFloat(string);
    }

    
    private static Forecast cursorToForecast(Cursor cursor)
    {
	Forecast f = new Forecast();
	
//	String names[] = cursor.getColumnNames(); // "id", "day",  "code", "text", "low", "high", "updated" };
//	int    count   = cursor.getColumnCount();
//	String id   = cursor.getString(0);
//	String day  = cursor.getString(1);
//	String code = cursor.getString(2);
//	String text = cursor.getString(3);
//	String low  = cursor.getString(4);
//	String high = cursor.getString(5);
//	String upd  = cursor.getString(6);
	
	int n=0;
        f.setId(toInt(cursor.getString(n++)));
        f.setDay(fromUnixtime(toInt(cursor.getString(n++))));
        f.setCode(toInt(cursor.getString(n++)));
        f.setText(cursor.getString(n++));
        f.setLow(toInt(cursor.getString(n++)));
        f.setHigh(toInt(cursor.getString(n++)));
        f.setUpdated(fromUnixtime(toInt(cursor.getString(n++))));
        return f;
    }

    private static ContentValues codeToContentValues(long id, String url)
    {
	ContentValues values = new ContentValues();
	values.put("code",      id);
	values.put("imgurl",    url);
	return values;
    }
    
    private static ContentValues scheduleToContentValues(Schedule schedule)
    {
	ContentValues values = new ContentValues();
	values.put("strand_mask",      schedule.getStrandMask());
	values.put("day_mask",       schedule.getDayMask());
	values.put("month_mask",     schedule.getMonthMask());
        // all times need to be converted from the human readable HHMM representation to
	// the representation in the db (as minutes since midnight)	
	int startMins = hhmmToMinutes(schedule.getStartTime());
	int endMins   = hhmmToMinutes(schedule.getEndTime());
	
	int interval  = schedule.getInterval();	
	int duration  = schedule.getDuration();	
	int intvMins  = hhmmToMinutes(interval);
	int duraMins  = hhmmToMinutes(duration);
	
	values.put("start_time",     startMins);
	values.put("end_time",       endMins);
	
	values.put("interval",       intvMins);
	values.put("duration",       duraMins);
	
	values.put("id_condition",   schedule.getIdCondition());
	values.put("condition_args", schedule.getConditionArgs());
	values.put("id_exception",   schedule.getIdException());
	values.put("exception_args", schedule.getExceptionArgs());
	// do not update power state. power state is toggled only via setSchedulePower
	//values.put("power",          schedule.isPower() ? "1" : "0");
	return values;
    }

    private static ContentValues eventToContentValues(Event event)
    {
   	ContentValues values = new ContentValues();
	values.put("active_mask", event.getActiveMask());
	values.put("date"    ,    datetimeToUnixtime(event.getDate()));
	values.put("message",     event.getMessage());
	values.put("json",        event.getJson());
	return values;
    }

    private static ContentValues weatherToContentValues(Weather weather)
    {
	ContentValues values = new ContentValues();
	values.put("date",       datetimeToUnixtime(weather.getDate()));
	values.put("updated",    datetimeToUnixtime(now()));
	values.put("code",       weather.getCode());
	values.put("text",       weather.getText());
	values.put("temp",       weather.getTemperature());
	values.put("humidity",   weather.getHumidity());
	values.put("visibility", weather.getVisibility());
	values.put("pressure",   weather.getPressure());
	values.put("rising",     weather.getRising());
	return values;
    }

    private static ContentValues forecastToContentValues(Forecast forecast)
    {
	ContentValues values = new ContentValues();
	values.put("day",     dateToUnixtime(forecast.getDay()));
	values.put("updated", datetimeToUnixtime(now()));
	values.put("code",    forecast.getCode());
	values.put("text",    forecast.getText());
	values.put("high",    forecast.getHigh());
	values.put("low",     forecast.getLow());
	return values;
    }

    public List<Schedule> getActiveSchedules(Date now)
    {
	int day     = now.getDay();
	int month   = now.getMonth()+1;
	int hours   = now.getHours();
	int minutes = now.getMinutes();
	int hhmm    = 100*hours+minutes;
	List<Schedule> active = getActiveSchedules(day, month, hhmm);
	return active;
	
    }

    public List<Schedule> getAllSchedules(Date now)
    {
	int day     = now.getDay();
	int month   = now.getMonth()+1;
	int hours   = now.getHours();
	int minutes = now.getMinutes();
	int hhmm    = 100*hours+minutes;
	return getAllSchedules(day, month, hhmm);
    }
    
    public static Calendar fromUnixtime(long unixtime)
    {
	Calendar c = Calendar.getInstance();
	c.setTimeInMillis(1000L*unixtime);
	return c;
    }


    // converts 1000 -> 10*60+00 = 600 minutes, 2000 -> 20*60+00 = 1200 minutes 
    private static int hhmmToMinutes(int hhmm)
    {
	return 60*(hhmm/100)+(hhmm%100); 	
    }

    // converts back "10:00" minutes -> 1000 
    private static int minutesToHHMM(String string)
    {
	return minutesToHHMM(toInt(string));
    }

    // converts back 600 minutes -> 10:00 
    private static int minutesToHHMM(int minutes)
    {
	return 100*(minutes/60)+(minutes%60);
    }

    public static long todayNoonAsUnixtime()
    {
	return dateToUnixtime(now());
    }

    public static Calendar todayNoon()
    {
	return toNoon(now());
    }

    public static Calendar now()
    {
	return Calendar.getInstance();
    }

    public static long nowUnixtime()
    {
	return datetimeToUnixtime(now());
    }

    
    public static Calendar toNoon(Calendar date)
    {
	Calendar clone = (Calendar) date.clone();
	clone.set(Calendar.HOUR_OF_DAY, 12);
	clone.set(Calendar.MINUTE,      00);
	clone.set(Calendar.SECOND,      00);
	clone.set(Calendar.MILLISECOND, 00);
	return clone;	
    }

    
    public static long dateToUnixtime(Calendar date)
    {
	return toNoon(date).getTimeInMillis()/1000;
    }
    

    public static long datetimeToUnixtime(Calendar datetime)
    {
	return datetime.getTimeInMillis()/1000;
    }

    
    private static int toInt(String s)
    {
	return Integer.parseInt(s);
    }
    

    public Forecast getForecast(int daysInAdvance)
    {
	Forecast f = getForecast(now(), daysInAdvance);
	f = null==f ? Forecast.NONE : f;
	return f;
    }

    
    public Weather getWeather()
    {
	Weather w = getWeather(3*3600); // max. age: three hours
	return null==w ? Weather.NONE : w;
    }

    public int testSetSchedulePower(List<Schedule> poweredOn)
    {
	int n = 0;
	try
	{
	    StringBuilder sb = new StringBuilder("-1"); // make sure brackets will contain a valid list of IDs
	    for (Schedule s : poweredOn)
	    {
		sb.append(sb.length()>0 ? "," : "");
		sb.append(s.getId());
	    }
	    
	    String listOfIds = sb.toString(); 
	    System.out.println("####### Setting power=1 on iDs " + listOfIds);
	    //String query     = "UPDATE schedules SET power=1 WHERE id in (" + listOfIds + ")";
	    SQLiteDatabase db = getWritableDatabase();

	    db.rawQuery("BEGIN TRANSACTION", null);
	    //db.rawQuery("UPDATE schedules SET power=0", null);
	    //db.rawQuery(query, null); 

	    ContentValues values0 = new ContentValues();
	    values0.put("power", "1");
	    n += db.update(TABLE_SCHEDULES, //table
		    values0, // column/value
		    "id IN (" + listOfIds + ")", // selection
		    null); //selection args

	    ContentValues values1 = new ContentValues();
	    values1.put("power", "0");
	    n += db.update(TABLE_SCHEDULES, //table
		    values1, // column/value
		    "id NOT IN (" + listOfIds + ")", // selection
		    null); //selection args

	    db.rawQuery("COMMIT", null);
	}
	catch (Exception e)
	{
	    System.err.println("testSetSchedulePower: " + e);	    
	}
	
	if (n>0)
	{
	    updateLastChangeOnSchedules();
	}
	return n;
    }

    
    public boolean logException(Throwable t)
    {
	try
	{
	    Event e = new Event(t);
	    addEvent(e);
	    return true;
	}
	catch (Exception e)
	{
	    e.printStackTrace();
	    return false;
	}
    }
    
    
    public int setSchedulePower(List<Schedule> poweredOn)
    {
	int rc = testSetSchedulePower(poweredOn);
	return rc;
	/*
	// 1. get reference to writable DB
	SQLiteDatabase db = this.getWritableDatabase();

	// reset all schedules to "powered off"
	{
	    ContentValues values = new ContentValues();
	    values.put("power", "0");
	    db.update(TABLE_SCHEDULES, //table
		    values, // column/value
		    null, // selections: all
		    null); //selection args
	}
	
	int total = 0;
	for (Schedule s : poweredOn)
	{
	    // 2. create ContentValues to add key "column"/value
	    ContentValues values = new ContentValues();
	    values.put("power",  "1");

	    // 3. updating row
	    int i = db.update(TABLE_SCHEDULES, //table
		    values, // column/value
		    "id=?", // selections
		    new String[] { String.valueOf(s.getId()) }); //selection args
	    total += i;
	}
	
	// 4. close
	db.close();

	if (total>0)
	{
	    updateLastChangeOnSchedules();
	}
	return total;
	*/
    }
}
