package com.venista.mjoy.weather.service;

//import java.util.Locale;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import com.venista.mjoy.api.exception.ExceptionFilter;
import com.apdlv.utils.URLCodec;
import com.apdlv.utils.WGet;
import com.venista.mjoy.weather.model.WeatherConditions;

public class ForecastProvider
{
	public static final String YAHOO_WEATHER_API_BASE_URL   = "http://weather.yahooapis.com/forecastrss";
	
	public static final String LOCATION_KEY         = "p";
	public static final String UNIT_KEY             = "u";
	public static final String UNIT_VALUE_CELSIUS   = "c";
	public static final String UNIT_VALUE_FARENHEIT = "f";

	private static final int TRIALS_MAX = 3;
		
	public WeatherConditions getForecast(String placeCode, /*Locale locale,*/ String tempUnit) throws Exception
	{		
		WeatherConditions wc = null;
		int trials = 0;

		tempUnit = normalizeTempUnit(tempUnit);
		
		while (trials<TRIALS_MAX && (null==wc || !wc.isAvailable()))
		{				
			try
			{
				trials++;
				
				String url = buildURL(placeCode, tempUnit);
				String xml = wget.getString(url);
				
				/* Simulate an error while getting the forecast data: */
				/*
				System.out.println(xml);
				if (false) return null;
				if (false) xml = "";				
				if (Math.random()>.3) xml = null;
				*/
				
				wc = new WeatherConditions(xml, /*locale,*/ tempUnit);
			}
			catch (Exception ex)
			{
				//ExceptionFilter.logFiltered(logger, ex);
			    System.err.println("getForecast: " + ex);
			}
		}
		
		return wc;
	}
	

	private static String buildURL(String location, String degreesUnit)
	{
		StringBuilder sb = new StringBuilder(YAHOO_WEATHER_API_BASE_URL);		
		sb.append("?").append(LOCATION_KEY).append("=").append(URLCodec.encodeASCII(location));
		sb.append("&").append(UNIT_KEY).append("=").append(URLCodec.encodeASCII(degreesUnit));		
		return sb.toString();
	}

	
	private static String normalizeTempUnit(String tempUnit)
	{
		if (null==tempUnit)
			tempUnit = "c";
		else
			tempUnit = tempUnit.toLowerCase();
			
		return (!tempUnit.matches("c") && !tempUnit.matches("f")) ? "c" : tempUnit; 
	}	

	
	//private static final Log  logger = LogFactory.getLog(ForecastProvider.class);
	//protected final Logger logger = LoggerFactory.getLogger(getClass());

	private static final WGet wget   = new WGet(true,5000);
}
