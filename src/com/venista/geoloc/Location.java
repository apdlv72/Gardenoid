package com.venista.geoloc;


public class Location
{
	
	protected Location(String source, String country, String region, String city, String zip, Double longitude, Double latitude, String isp, String organization)
	{
		this.source = source;

		this.country      = nullIfEmpty(country);
		this.region       = nullIfEmpty(region);
		this.city         = nullIfEmpty(city);
		this.isp          = nullIfEmpty(isp);
		this.org = nullIfEmpty(organization);
		
		this.coordsAvailable = false;		
		if (null!=longitude || null!=latitude)
		{    			
			this.longitude       = longitude;
			this.latitude        = latitude;
			this.coordsAvailable = true;
		}
	}
	
	
	protected void setSource(String source)
	{
		this.source = source;
	}

	
	public String getSource()
	{
		return source;
	}


	private static String nullIfEmpty(String string)
	{
		return null==string || string.length()<1 ? null : string;
	}


	public String getCountry()
	{
		return country;
	}

	
	public String getRegion()
	{
		return region;
	}

	
	protected void setCity(String city)
	{
		this.city = city;
	}

	
	public String getCity()
	{
		return city;
	}

	
	public boolean isCoordsAvailable()
	{
		return coordsAvailable;
	}
	
	
	public double getLongitude()
	{ 
		return longitude;
	}

	
	public double getLatitude()
	{ 
		return latitude;
	}
	

	public String getIsp()
	{
		return isp;
	}


	public String getOrg()
	{
		return org;
	}


	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("[src=").append(source);
		
		if (null!=country)
			sb.append(",country=").append(country);
		if (null!=region)
			sb.append(",region=").append(region);
		if (null!=city)
			sb.append(",city=").append(city);
		if (coordsAvailable)
			sb.append(",coords=").append(longitude).append("x").append(latitude);
		if (null!=isp)
			sb.append(",isp=\"").append(isp).append("\"");
		if (null!=org)
			sb.append(",org=\"").append(org).append("\"");
		sb.append("]");
		return sb.toString();
	}
	
	
	private String  source;
	private String  country;
	private String  region;
	private String  city;
	private boolean coordsAvailable;
	private Double  longitude;
	private Double  latitude;
	private String  isp;
	private String  org;
}

