

package com.venista.mjoy.weather.model;

import com.venista.geoloc.YahooCountry;


public class Place
{
	public void setCode(String code)
	{
		this.code = code;
	}
	
	
	public String getCode()
	{
		return code;
	}
	
	
	public void setYahooCountryCode(String country)
	{
		this.yahooCountryCode = country.toUpperCase();
	}
	
	
	public String getIsoCountryCode()
	{
		String isoCountryCode = YahooCountry.getISOCountryCode(yahooCountryCode);
		return isoCountryCode;
	}
	
	
	/*
	public String getYahooCountryCode()
	{
		return yahooCountryCode;
	}
	*/
	
	public void setCity(String city)
	{
		this.city = city;
	}

	
	public String getCity()
	{
		return city;
	}
	

	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("[ycountry=").append(yahooCountryCode).append(",pcode=").append(code).append(",city=").append(city).append("]");
		return sb.toString();
	}

	
	private String code;
	private String yahooCountryCode;
	private String city;
}
