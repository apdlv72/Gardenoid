package com.venista.mjoy.weather.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WeatherConditions 
{
	public WeatherConditions(String xml, /*Locale locale,*/ String tempUnit)
	{
		this.available = false;
		this.tempUnit  = tempUnit;
		
		if (null==xml || xml.length()<10)
		{
			return;
		}

		/*
		System.out.println("FORECAST:" + xml);
		*/		
		/* get complete HTML description? 
		Matcher matcher1 = PATTERN_DESCRIPTION.matcher(xml);		
		if (matcher1.find())
		{
			this.description = matcher1.group(1);
		}
		*/

		Matcher matcher = PATTERN_LINK.matcher(xml);
		if (matcher.find())
		{
			this.link = matcher.group(1);
		}

		matcher = PATTERN_IMAGE.matcher(xml);
		if (matcher.find())
		{
			this.linkImage = matcher.group(1);
		}
		
		matcher = PATTERN_YWEATHER.matcher(xml);
		while (matcher.find())
		{
			String category = matcher.group(1);
			String values   = matcher.group(2);
			/*
			System.out.println("category: " + category);
			System.out.println("  values: " + values);
			*/
			
			Map<String,String> map = parseValues(values);
			
			if (category.matches("forecast"))
			{
				Forecast fc = new Forecast(map /*, locale*/);
				forecasts.add(fc);
			}
			else
			{
				if (category.matches("wind"))
				{
					String direction = discretizeDirection(map.get("direction"));
					map.put("discreteDirection", direction);
				}
				categories.put(category, map);									
			}			

			this.available = true;
		}
		
	}
	
	
	public boolean isAvailable()
	{
		return available;
	}
	
	/*
	public boolean isBaseConditionAvailable() 
	{
		return
			categories.containsKey("units") &&
			categories.containsKey("condition");
	}
	

	public boolean isExtConditionAvailable() 
	{ 
		return 
			categories.containsKey("units") &&
			categories.containsKey("wind") && 
			categories.containsKey("atmosphere");
	}
	*/

	
	public boolean isForecastAvailable() 
	{ 
		return forecasts.size()>0 && categories.containsKey("units");
	}
	
	
	public Map<String,String> getLocation()   { return categories.get("location"); }	
	public Map<String,String> getUnits()      { return categories.get("units"); }	
	public Map<String,String> getCondition()  { return categories.get("condition"); }
	public Map<String,String> getWind()       { return categories.get("wind"); }	
	public Map<String,String> getAtmosphere() { return categories.get("atmosphere"); }	
	public Map<String,String> getAstronomy()  { return categories.get("astronomy"); }
	
	public Forecast getForecast(int n) { return forecasts.get(n); }
	public Forecast getForecastA()     { return forecasts.size()<1 ? null : getForecast(0);   }
	public Forecast getForecastB()     { return forecasts.size()<2 ? null : getForecast(1);   }
	
	
	public String getLink()
	{
		return link;
	}

	
	public String getLinkImage()
	{
		return linkImage;
	}

	
	public Vector<Forecast> getForecasts()
	{
		return forecasts;
	}


	public String getTempUnit()
	{
		return tempUnit;
	}	
	
	
	/* private methods */
	
	
	private Map<String,String> parseValues(String values)
	{
		Map<String,String> map = new HashMap<String,String>();
		
		Matcher matcher = PATTERN_KEY_VALUE.matcher(values);		
		while (matcher.find())
		{
			String key = matcher.group(1);
			String value   = matcher.group(2);
			map.put(key, value);
		}
		
		return map;
	}

	
	private static String discretizeDirection(String string)
	{
		final String[] DIRECTION       = { "n", "ne", "e", "se", "s", "sw", "w", "nw" };
		final int      DEGREES_MAX     = 360;
		final int      DEGREES_DIVISOR = DEGREES_MAX/DIRECTION.length;
		final int      DEGREES_OFFSET  = DEGREES_DIVISOR/2; 
		
		int i = Integer.parseInt(string);
		i = i<0 ? 0 : i;
		i = (i+DEGREES_OFFSET)%DEGREES_MAX;
				
		return DIRECTION[i/DEGREES_DIVISOR];
	}


	/* Uncomment to allow for ready-to-go HTML description provided by Yahoo's API.
	public String getDescription()
	{
		return description;
	}

	protected Pattern PATTERN_DESCRIPTION = Pattern.compile("<description><!\\[CDATA\\[(.*)\\]\\]", Pattern.MULTILINE | Pattern.DOTALL);
	private String description;
	*/
	
	protected Pattern PATTERN_YWEATHER    = Pattern.compile("<yweather:([a-z]+)([^>]*)/>",            Pattern.MULTILINE);
	protected Pattern PATTERN_KEY_VALUE   = Pattern.compile("([a-z]+)=\"([^\"]*)\"",                  Pattern.MULTILINE);
	protected Pattern PATTERN_LINK        = Pattern.compile("<link>([^<]*\\.html[^<]*)</link>",       Pattern.MULTILINE);	
	protected Pattern PATTERN_IMAGE       = Pattern.compile("<image>.*<url>([^<]*)</url>",            Pattern.MULTILINE|Pattern.DOTALL);
	
	private boolean available;
	private String  linkImage;
	private String  link;
	private String  tempUnit;
		
	private Map<String,Map<String,String>> categories = new HashMap<String,Map<String,String>>();
	private Vector<Forecast>               forecasts  = new Vector<Forecast>();
}
