package com.apdlv.yahooweather;

import java.util.HashMap;
import java.util.Map;


public class Forecast
{
    public Forecast(Map<String,String> data /*, Locale locale*/)
    {
	this.data     = data;
    }

    public String getDay()
    {
	return data.get("day");
    }

    public String getDate()
    {
	return data.get("date");
    }

    public String getLow()
    {
	return data.get("low");
    }

    public String getHigh()
    {
	return data.get("high");
    }

    public String getCode()
    {
	return data.get("code");
    }

    public String getText()
    {
	return data.get("text");
    }

    public static boolean isRainCode(int i)
    {
        if (i<=CODE_SLEET) return true; // see 
        
        switch (i)
        {
        case CODE_HEAVY_SNOW:
        case CODE_HEAVY_SNOW2:
        case CODE_ISOLATED_THUNDERSHOWERS:
        case CODE_RAIN_HAIL:
        case CODE_SCATTERED_SHOWERS:
        case CODE_SCATTERED_SNOW_SHOWERS:
        case CODE_SNOW_SHOWERS:
        case CODE_THUNDERSHOWERS:
    	return true;
        }
        return false;
    }

    private Map<String, String> data;

    public static final int CODE_SLEET     = 18;
    public static final int CODE_RAIN_HAIL = 35;
    public static final int CODE_SCATTERED_SHOWERS = 40;
    public static final int CODE_HEAVY_SNOW = 41;
    public static final int CODE_SCATTERED_SNOW_SHOWERS = 42;
    public static final int CODE_HEAVY_SNOW2 = 43;	
    public static final int CODE_PARTLY_CLOUDY = 44;
    public static final int CODE_THUNDERSHOWERS = 45;
    public static final int CODE_SNOW_SHOWERS = 46;
    public static final int CODE_ISOLATED_THUNDERSHOWERS = 47;

    // https://developer.yahoo.com/weather/#codes
    @SuppressWarnings("serial")
    public static final Map<String, String> CODES = new HashMap<String, String>()
	    {{
		put("0", "tornado");
		put("1", "tropical storm");
		put("2", "hurricane");
		put("3", "severe thunderstorms");
		put("4", "thunderstorms");
		put("5", "mixed rain and snow");
		put("6", "mixed rain and sleet");
		put("7", "mixed snow and sleet");
		put("8", "freezing drizzle");
		put("9", "drizzle");
		put("10", "freezing rain");
		put("11", "showers");
		put("12", "showers");
		put("13", "snow flurries");
		put("14", "light snow showers");
		put("15", "blowing snow");
		put("16", "snow");
		put("17", "hail");
		put("" + CODE_SLEET, "sleet");		
		put("19", "dust");
		put("20", "foggy");
		put("21", "haze");
		put("22", "smoky");
		put("23", "blustery");
		put("24", "windy");
		put("25", "cold");
		put("26", "cloudy");
		put("27", "mostly cloudy (night)");
		put("28", "mostly cloudy (day)");
		put("29", "partly cloudy (night)");
		put("30", "partly cloudy (day)");
		put("31", "clear (night)");
		put("32", "sunny");
		put("33", "fair (night)");
		put("34", "fair (day)");
		put("" + CODE_RAIN_HAIL, "mixed rain and hail");
		put("36", "hot");
		put("37", "isolated thunderstorms");
		put("38", "scattered thunderstorms");
		put("39", "scattered thunderstorms");
		put("" + CODE_SCATTERED_SHOWERS, "scattered showers");
		put("" + CODE_HEAVY_SNOW, "heavy snow");
		put("42", "scattered snow showers");
		put("" + CODE_HEAVY_SNOW2, "heavy snow");		
		put("44", "partly cloudy");
		put("" + CODE_THUNDERSHOWERS, "thundershowers");
		put("" + CODE_SNOW_SHOWERS, "snow showers");
		put("" + CODE_ISOLATED_THUNDERSHOWERS, "isolated thundershowers");
		put("3200", "not available ");
	    }};

}
