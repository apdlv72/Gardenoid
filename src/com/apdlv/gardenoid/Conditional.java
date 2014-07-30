package com.apdlv.gardenoid;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import android.annotation.SuppressLint;
import android.util.Log;

import com.apdlv.gardenoid.db.DAO;
import com.apdlv.gardenoid.db.Weather;
import com.apdlv.utils.U;
import com.venista.mjoy.weather.model.Forecast;

public class Conditional
{
    public static final String TAG = Conditional.class.getSimpleName();
    
    public static final int COND_NONE = 0;
    public static final int COND_HUMIDITY_ABOVE    = 110;
    public static final int COND_HUMIDITY_BELOW    = 109;
    public static final int COND_RAIN_NEXT_DAYS    = 108;
    public static final int COND_NO_RAIN_NEXT_DAYS = 107;
    public static final int COND_RAIN_PAST_DAYS    = 106;
    public static final int COND_NO_RAIN_PAST_DAYS = 105;
    public static final int COND_CURR_TEMP_BELOW   = 104;
    public static final int COND_CURR_TEMP_ABOVE   = 103;
    public static final int COND_DAILY_MAX_BELOW   = 102;
    public static final int COND_DAILY_MAX_ABOVE   = 101;
    
    public static int POS  = 1;
    public static int NEG  = 2;
    public static int BOTH = 3;
    
    private int    id;
    private int    type;
    private String name;
    

    @SuppressWarnings("serial")
    @SuppressLint("UseSparseArrays")
    public static final Map<Integer,Conditional> CONDITIONALS = new TreeMap<Integer, Conditional>()
    {
	// anonymous constructor
	{
	    put(Integer.valueOf(COND_NONE),              new Conditional(COND_NONE,              BOTH, "none"));
	    put(Integer.valueOf(COND_DAILY_MAX_ABOVE),   new Conditional(COND_DAILY_MAX_ABOVE,   BOTH, "daily max above $X"));
	    put(Integer.valueOf(COND_DAILY_MAX_BELOW),   new Conditional(COND_DAILY_MAX_BELOW,   BOTH, "daily max below $X"));
	    put(Integer.valueOf(COND_CURR_TEMP_ABOVE),   new Conditional(COND_CURR_TEMP_ABOVE,   BOTH, "temperature above $X"));
	    put(Integer.valueOf(COND_CURR_TEMP_BELOW),   new Conditional(COND_CURR_TEMP_BELOW,   BOTH, "temperature below $X"));
	    put(Integer.valueOf(COND_NO_RAIN_PAST_DAYS), new Conditional(COND_NO_RAIN_PAST_DAYS, POS,  "no rain past $X days"));
	    put(Integer.valueOf(COND_RAIN_PAST_DAYS),    new Conditional(COND_RAIN_PAST_DAYS,    NEG,  "rain past $X days"));
	    put(Integer.valueOf(COND_NO_RAIN_NEXT_DAYS), new Conditional(COND_NO_RAIN_NEXT_DAYS, POS,  "no rain next $X days"));
	    put(Integer.valueOf(COND_RAIN_NEXT_DAYS),    new Conditional(COND_RAIN_NEXT_DAYS,    NEG,  "rain next $X days"));
	    put(Integer.valueOf(COND_HUMIDITY_BELOW),    new Conditional(COND_HUMIDITY_BELOW,    POS,  "humidity below $X%"));
	    put(Integer.valueOf(COND_HUMIDITY_ABOVE),    new Conditional(COND_HUMIDITY_ABOVE,    NEG,  "humidity above $X%"));			

	}
    };
    
    
    public static String CONDITIONALS_JSON = getJson();
    private static String getJson() 
    {{
	    StringBuilder sb = new StringBuilder();
	    boolean first = true;
	    sb.append("{");
	    for (Conditional c : CONDITIONALS.values())
	    {
		sb.append(first ? "" : ", ");
		sb.append(c.id).append(": { \"name\" : \"").append(c.name).append("\", \"type\" : ").append(c.type).append("}");
		first = false;
	    }
	    sb.append("}");	
	    return sb.toString();
    }};
    

    private Conditional(int id, int type, String name)
    {
        this.id = id; this.type=type; this.name=name;
    }
    
    
    public boolean matches(DAO database, String args)
    {
        if (0==id)          return true;
        if (null==database) return false;
        boolean rc = false; 
        switch (id)
        {
        case COND_DAILY_MAX_ABOVE:   rc = isAbove(database.getForecast(0).getHigh(), args); break; 
        case COND_DAILY_MAX_BELOW:   rc = isBelow(database.getForecast(0).getHigh(), args); break; 
        case COND_CURR_TEMP_ABOVE:   rc = isAbove(database.getWeather().getTemperature(), args); break;
        case COND_CURR_TEMP_BELOW:   rc = isBelow(database.getWeather().getTemperature(), args);break;
        case COND_RAIN_PAST_DAYS:    rc =  rainPastXDays(database, args); break;
        case COND_NO_RAIN_PAST_DAYS: rc = !rainPastXDays(database, args); break;
        case COND_RAIN_NEXT_DAYS:    rc =  rainNextXDays(database, args); break;
        case COND_NO_RAIN_NEXT_DAYS: rc = !rainNextXDays(database, args); break;
        case COND_HUMIDITY_ABOVE:    rc = isAbove(database.getWeather().getHumidity(), args); break;
        case COND_HUMIDITY_BELOW:    rc = isBelow(database.getWeather().getHumidity(), args); break;
        }
        return rc;
    }

    private static boolean isAbove(float a, String b)
    {
	return null!=b && a>Float.parseFloat(b);
    }


    private static boolean rainNextXDays(DAO database, String args)
    {
        List<com.apdlv.gardenoid.db.Forecast> fcs = database.getAllForecasts();
        if (null==fcs) return false;
        
        float days    = U.toFloat(args);
        int   seconds = (int)Math.round(days*24*60*60);        
        Calendar limit = DAO.todayNoon();        
        limit.add(Calendar.SECOND, seconds);
        
        Log.d(TAG, "rainNextXDays: args=" + args + ", limit=" + U.YYYYMMDD_hhmmss.format(limit.getTime()));
        
        for (com.apdlv.gardenoid.db.Forecast f : fcs)
        {
            Calendar day = f.getDay();
            if (!day.after(limit))
            {
        	Log.d(TAG, "Checking for rain: " + f);
        	if (Forecast.isRainCode(f.getCode()))
        	{
        	    System.err.println("FOUND RAIN (args: " + args + "): " + f);
        	    return true;
        	}
            }
            else
            {
        	Log.d(TAG, "After limit when checking for rain: " + f);
            }
        }
        
        return false;
    }

    private static boolean rainPastXDays(DAO database, String args)
    {
        float days          = U.toFloat(args);
	long  maxAgeSeconds = (long)Math.round(24L*60*60*days);
        
	List<Weather> list = database.getAllWeather(maxAgeSeconds);
        if (null==list)
        {
            Log.e(TAG, "rainPastXDays: no weather iformation available");
        }
        
        for (Weather w : list)
        {
            if (Forecast.isRainCode(w.getCode()))
            {
        	return true;
            }
        }
        
        return false;
    }

//    private static boolean isAbove(String a, String b)
//    {
//        return null!=a && null!=b && isAbove(Integer.parseInt(a), b);
//    }

    private static boolean isBelow(float a, String b)
    {
	return !isAbove(a, b);
    }
    
//    @SuppressWarnings("unused")
//    private static boolean isBelow(String a, String b)
//    {
//        return !isAbove(a, b);
//    }
}