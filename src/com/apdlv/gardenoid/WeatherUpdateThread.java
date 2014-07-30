package com.apdlv.gardenoid;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import android.util.Log;

import com.apdlv.gardenoid.db.DAO;
import com.apdlv.gardenoid.db.Weather;
import com.venista.mjoy.weather.model.Forecast;
import com.venista.mjoy.weather.model.WeatherConditions;
import com.venista.mjoy.weather.service.ForecastProvider;

public class WeatherUpdateThread extends Thread
{
    private ForecastProvider mForecastProvider;
    private DAO mDatabase;
    private GardenoidService mService;
    static final String PLACECODE_COLOGNE = "GMXX0018";
    private static final String TAG = WeatherUpdateThread.class.getSimpleName();
    
    public WeatherUpdateThread(GardenoidService service, ForecastProvider forecastProvider, DAO database)
    {
	this.mService = service;
	this.mForecastProvider = forecastProvider;
	this.mDatabase = database;
    }

    @Override
    public void run()
    {				
	WeatherConditions wc;
	try
	{
	    long max = 60*60; // 1 hour
	    Weather w = mDatabase.getWeather(max);

	    // found weather info less than "age" hour old ... skip update
	    if (null==w) 
	    {
		wc = mForecastProvider.getForecast(PLACECODE_COLOGNE, "c");
		if (null!=wc)
		{
		    // http://weather.yahooapis.com/forecastrss?p=GMXX0018&u=c
		    updateForecasts(wc);
		    updateWeather(wc);			
		}
	    }
	    else
	    {
		long age = DAO.nowUnixtime()-DAO.datetimeToUnixtime(w.getDate());			
		Log.d(TAG, "weather info is up to date (" + age + " seconds old");
	    }
	} 
	catch (Exception e)
	{
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	finally
	{
	    mService.onUpdateThreadDone(this); 
	}
    }


    private void updateForecasts(WeatherConditions wc) throws Exception
    {
	Vector<Forecast> all = wc.getForecasts();
	for (Forecast f : all)
	{
	    String date = f.getDate();
	    System.out.println("Updating forecast for " + date);
	    Calendar day = parseDate(f.getDate()); // NOT "day" - this is e.g just "Mon", not the date

	    int code = toInt(f.getCode());
	    String text = f.getText();
	    int high = toInt(f.getHigh());
	    int low  = toInt(f.getLow());

	    com.apdlv.gardenoid.db.Forecast fc = new com.apdlv.gardenoid.db.Forecast(day, code, text, low, high);
	    mDatabase.insertOrUpdateForecast(fc);
	}
    }


    private void updateWeather(WeatherConditions wc) throws Exception
    {
	Map<String, String> cond = wc.getCondition();

	int      code = toInt(cond.get("code"));
	int      temp = toInt(cond.get("temp"));
	String   text = cond.get("text");
	Calendar date = parseDatetime(cond.get("date"));

	Map<String, String> a = wc.getAtmosphere();
	int humid = toInt(a.get("humidity"));
	float visib = toFloat(a.get("visibility"));
	float press = toFloat(a.get("pressure"));
	float rise  = toFloat(a.get("rising"));
	
	Weather w = new Weather(date, code, text, temp, humid, visib, press, rise);
	mDatabase.insertOrUpdateWeather(w);
    }

    private Float toFloat(String string)
    {
	return null==string ? null : Float.parseFloat(string);
    }

    private Calendar parseDatetime(String string) throws ParseException
    {
	// "Mon, 14 Jul 2014 9:49 am CEST"
	Date date = FORMAT_DATETIME.parse(string);
	Calendar cal = Calendar.getInstance();
	cal.setTime(date);
	return cal;
    }

    private Calendar parseDate(String string) throws ParseException
    {
	// "Mon, 14 Jul 2014 9:49 am CEST"
	Date date = FORMAT_DATE.parse(string);
	Calendar cal = Calendar.getInstance();
	cal.setTime(date);
	return cal;
    }

    private static int toInt(String string)
    {
	return Integer.parseInt(string);
    }

    private static final SimpleDateFormat FORMAT_DATETIME = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm a z", Locale.ENGLISH);
    private static final SimpleDateFormat FORMAT_DATE     = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
}
