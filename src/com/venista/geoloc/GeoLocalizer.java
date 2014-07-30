

package com.venista.geoloc;


import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Locale;

//import sun.net.util.IPAddressUtil;

import com.apdlv.utils.WGet;


public class GeoLocalizer
{
	/** Lookup the database for a IP range that matches extract all available information. */ 
    public static final long FLAG_CONSULT_DATABASE        =  1;
    
    /** If there was no match in the database, use the given Locale to find a generic location. */ 
    public static final long FLAG_CONSULT_DNS             =  2;
    
    /** If there was no match in the database, use the given Locale to find a generic location. */ 
    public static final long FLAG_CONSULT_LOCALE          =  4;
    
    /** Query Maxmind's web service. */ 
    public static final long FLAG_CONSULT_WEBSERVICE      =  8;
    
    
    /** Location should contain longitude and latitude. */
    public static final long FLAG_NEED_COORDS             =  256; 
    
    /** Location should contain the city. */
    public static final long FLAG_NEED_CITY               =  512; 
    
    /** Location should contain the country. */
    public static final long FLAG_NEED_COUNTRY            = 1024;    
    
    
    /** If not city could be determined, use the capital city for the detected country. */
    public static final long FLAG_DONT_DEFAULT_TO_CAPITAL = 2048;    

    
    /** Shortcut for cheap lookup including the city (won't use web service) */
    public static final long FLAG_CHEAP                   =	FLAG_CONSULT_DATABASE | FLAG_CONSULT_DNS | FLAG_CONSULT_LOCALE | FLAG_NEED_CITY ;
    
    /** Shortcut for expensive lookup including the city and coordinates (will use also web service) */
    public static final long FLAG_COSTLY                  =	FLAG_CHEAP | FLAG_CONSULT_DATABASE | FLAG_NEED_COORDS;
    
    /** By default, use a chep lookup. */
    public static final long FLAG_DEFAULT                 =	FLAG_CHEAP;
    
    
    /**  Hardcoded to work with MySQL RDBMS but should be merely child's play to port to any other one. */
	public static final String DB_DRIVER_MYSQL = "org.gjt.mm.mysql.Driver";    

	/** Default DB url used if not was set. */
    public static final String DB_URL_DEFAULT  = "jdbc:mysql://yojm_db:3306/GEOLOC";

	protected static final String MAXMIND_BASE_URL          = "http://geoip1.maxmind.com/f";     	
	protected static final String MAXMIND_LICENSE_KEY       = "l";
	
	/** Do not use maxmind license this for production as long as this is a test license only (will allow at most 1000 queries) */
	protected static final String MAXMIND_LICENSE_VAL_TEST  = "ckj2bkHH2Mq5";
	protected static final String MAXMIND_ADDRESS_KEY       = "i";

	
	/** Set whether to dump SQL queries to stdout for debug purposes. */
	public void setShowSQL(boolean b)
	{
		showSQL = b;
	}

	/** Set database URL */
	public void setDbURL(String dbURL)
	{
		this.dbURL = dbURL;
	}


	/** Set database user */
	public void setDbUser(String dbUser)
	{
		this.dbUser = dbUser;
	}


	/** Set database password */
	public void setDbPassword(String dbPassword)
	{
		this.dbPassword = dbPassword;
	}
	
	
	private boolean isDbConfigured()
	{
		return null!=dbURL && null!=dbUser && null!=dbPassword;
	}


	/** Set license string to use for mindmax web service queries. */
	public void setMaxMindLicense(String maxMindLicense)
	{
		this.maxMindLicense = maxMindLicense;
	}

	
	/** Localize an IP using the default lookup flags (FLAGS_CHEAP). */	
	public Location localize(Locale locale, String ip) throws Exception
	{
		return localize(locale, ip, FLAG_CHEAP);
	}
	

	/** Localize an IP using custom lookup flags. */	
	public Location localize(Locale locale, String ip, long flags) throws Exception
    {            
		Location location  = null;
    	long     numericIP = ipToNumeric(ip);
    	
    	if (isFlagSet(flags,FLAG_CONSULT_DATABASE))
    	{
    		if (!isDbConfigured())
    			System.err.println("WARNING: Database initialization incomplete for IP range based localization.");
    		else
    			location = cosultDatabase(numericIP);
    	}
    	if (isComplete(location, flags))
    		return addCapitalIfIndicated(location,flags);
	    	
    	if (isFlagSet(flags,FLAG_CONSULT_DNS))
    		location = consultDNS(ip);
    	if (isComplete(location, flags))
    		return addCapitalIfIndicated(location,flags);    	
    	
    	// country determination based on DNS overrides (e.g. browser) locale, therefore
    	// perform this only if the DNS lookup was not successful
    	if (null==location || null==location.getCountry())
    	{
	    	if (isFlagSet(flags,FLAG_CONSULT_LOCALE) && null!=locale)
	    		location = consultLocale(locale);
	    	if (isComplete(location, flags))
	    		return addCapitalIfIndicated(location,flags);
    	}
    	    	
    	if (isFlagSet(flags,FLAG_CONSULT_WEBSERVICE))
    		location = consultWebService(ip);
    	if (isComplete(location, flags))
    		return addCapitalIfIndicated(location,flags);
    	
    	location =  addCapitalIfIndicated(location,flags);
    	return isComplete(location, flags) ? location : null;
    }


	/** Does a DNS looup of the passed hostname. */
    public static String hostToIP(String hostname) throws UnknownHostException
    {
    	return InetAddress.getByName(hostname).getHostAddress();
    }
    
    
	/** Add the capital cito to the givenlocation if none is was set and the flags allow to do so. */
	private Location addCapitalIfIndicated(Location location, long flags)
	{
		if (!isFlagSet(flags,FLAG_DONT_DEFAULT_TO_CAPITAL) && null!=location && null==location.getCity())
    		location.setCity(Capitals.getCapital(location.getCountry()));
		return location;
	}
	

	/** Perform a localization based on the result of a reverse DNS looup of the IP address. 
	 * @throws SQLException 
	 */
	private Location consultDNS(String ip) throws IOException, SQLException
	{
		String countryCode = dnsLocalizer.getCountryCodeByIP(ip);		
		Location l = consultLocale(countryCode);
		if (null!=l)
			l.setSource("dns");
		return l;
	}

	
	/** Perform a localization based on the locale's country solely. */
	private Location consultLocale(Locale locale) throws SQLException
	{
		return consultLocale(null==locale ? null : locale.getCountry());			
	}
	
	
	/** Perform a localization based on the locale's country solely. */
	private Location consultLocale(String countryCode) throws SQLException
	{
		Location location = null;
		try
		{
			if (null!=countryCode && countryCode.length()>0)
			{   
				countryCode = countryCode.toUpperCase().replaceAll("'", "\\'");
				location = new Location("locale", countryCode, null, null, null, null, null, null, null);
				
				if (isDbConfigured())
				{
					String query = "select ID, LATITUDE, LONGITUDE from LOCATION where REGION='' and CITY='' and COUNTRY='" + countryCode + "'";
					
					if (showSQL) System.out.println("executing query: " + query);
					Statement st = getConnection().createStatement();	            	            
				    ResultSet rs = st.executeQuery(query);	            
				    
					if (rs.next())
					{
						location = new Location("locale", countryCode, null, null, null, rs.getDouble(2), rs.getDouble(3), null, null);
					}
				}
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return location;
	}


	/** Perform a localization by looking the IP up in the local database . */
	private Location cosultDatabase(long numericIP)	throws SQLException
	{
		Location location = null;
		try
		{
			long locationID = findLocationID(numericIP);
			if (locationID>-1)
			{	    		
				String query = "select COUNTRY, REGION, CITY, LATITUDE, LONGITUDE from LOCATION where ID=" + locationID + " limit 1";

				if (showSQL) System.out.println("executing query: " + query);        	
				ResultSet rs = getConnection().createStatement().executeQuery(query);

				if (rs.next())
				{
					location = new Location("db", rs.getString(1), rs.getString(2), rs.getString(3), null, rs.getDouble(4), rs.getDouble(5), null, null);
				}
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return location;
	}

	
	/** Lookup an IP range in the local database and return the location ID if found and -1 otherwise. */
	private long findLocationID(long numericIP) throws SQLException
    {
    	String query = "select ID_LOCATION from IP_BLOCK where " + numericIP + " between START and END limit 1";
    	if (showSQL) System.out.println("executing query: " + query);        	
        ResultSet rs = getConnection().createStatement().executeQuery(query);
    	return rs.next() ? rs.getLong(1) : -1;
    }
    
    
	/** Perform a localization by querying maxmind's web service and parisng the response. */
	private Location consultWebService(String ip) throws Exception
	{
		Location location = null;
		try
		{
			String source = "cache";
	    	String result = knownIPsCash.get(ip);
	    	if (null==result)
	    	{	    	
	        	String url = MAXMIND_BASE_URL + "?" + MAXMIND_LICENSE_KEY + "=" + maxMindLicense + "&" + MAXMIND_ADDRESS_KEY + "=" + ip;
		    	WGet wget = new WGet(true, 5000);        	
		    	result = wget.getString(url);
				source = "maxmind";
	    	}
	    	
	    	if (null!=result)
	    	{
	    		String parts[] = result.split(",");
	    		/* 
	    		 * http://www.maxmind.com/app/web_services#country
				 * Our City service is also easy to set up and use, and it returns 
				 * region code, city, metropolitan code, area code, latitude, longitude, ISP, and organization as well as country. 
	    		 */
	    		// "US,FL,Miami,33134,25.754101,-80.271004,528,305,\"Terra Networks Operations\",\"Terra Networks Operations\"";

	    		String country      = parts[0];	    		
	    		String region       = parts[1];
	    		// workaround for maxmind bug sending "(null)" if there is no region in some cases
	    		if (null!=region && region.equals("(null)"))
	    				region = "";	    		
	    		String city         = parts[2];
	    		String zip          = parts[3];
	    		double longitude    = Double.parseDouble(parts[4]);
	    		double latitude     = Double.parseDouble(parts[5]);
	    		/* unused for now
	    		String metro        = parts[6];
	    		String area         = parts[7];
	    		*/
	    		String isp          = stripDoubleQuotes(parts[8]);
	    		String organization = stripDoubleQuotes(parts[9]);
	
	    		location = new Location(source, country, region, city, zip, longitude, latitude, isp, organization);
	    	}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return location;
	}

    
	/** Remove leading and trailing double quote from the input string.  */
	private static String stripDoubleQuotes(String string)
	{
		if (null==string || string.length()<1)
			return string;
		if (string.startsWith("\""))
			string = string.substring(1);
		if (string.endsWith("\""))
			string = string.substring(0,string.length()-1);
		return string;
	}


	/** Checks if the location contains all information requested by the flags.  */
	private static boolean isComplete(Location location, long flags)
	{
		if (null==location) return false;		
		if (isFlagSet(flags,FLAG_NEED_COORDS)  && !location.isCoordsAvailable()) return false;
		if (isFlagSet(flags,FLAG_NEED_CITY)    && null==location.getCity())      return false;
		if (isFlagSet(flags,FLAG_NEED_COUNTRY) && null==location.getCountry())   return false;
		return true;
	}


	/** Convert a string representation (e.g. "127.0.0.1") of an IP to a long value. */
    private static long ipToNumeric(String ip)
    {
	String split[] = ip.split("\\.");
	byte n[] = new byte[4];
	for (int i=0; i<4; i++)
	{
	    n[i]=(byte)Integer.parseInt(split[i]);
	}	
    	//byte n[] = IPAddressUtil.textToNumericFormatV4(ip);
    	
    	return ((long)n[0] & 0xff)<<24 | ((long)n[1] & 0xff)<<16 | ((long)n[2] & 0xff)<<8 | ((long)n[3] & 0xff);
    }
    
    
	/** Returns true if the given flags do have all those bits set as defined in the mask.*/
    private static final boolean isFlagSet(long flag, long mask)
    {
    	return (flag & mask)>0;
    }
    
    
    /** Return an existing DB dbConnection or establish a new one. */
	private synchronized Connection getConnection() throws SQLException
    {
		if (null!=dbConnection)
		{
			if (dbConnection.isValid(1000))
				return dbConnection;
		}
		
		try
		{
			Class.forName(DB_DRIVER_MYSQL).newInstance();
		}
		catch (Exception ex)
		{
			throw new SQLException("Failed to load database driver", ex);
		}
		
        return (dbConnection = DriverManager.getConnection(dbURL, dbUser, dbPassword));
    }

    
	/** This cache is for testing purposed (i.e. to have test runs without causing costs to query the web service). */
	private static final HashMap<String,String> knownIPsCash = new HashMap<String,String>();
	static
	{
		knownIPsCash.put("72.21.206.5","US,WA,Seattle,98104,47.602600,-122.328400,819,206,\"AMAZON.COM\",\"AMAZON.COM\"");
		knownIPsCash.put("72.21.210.11","US,WA,Seattle,98104,47.602600,-122.328400,819,206,\"AMAZON.COM\",\"AMAZON.COM\"");
		knownIPsCash.put("84.53.182.56","EU,(null),,,47.000000,8.000000,0,0,\"AKAMAI TECHNOLOGIES\",\"AKAMAI TECHNOLOGIES\"");
		knownIPsCash.put("84.53.182.32","EU,(null),,,47.000000,8.000000,0,0,\"AKAMAI TECHNOLOGIES\",\"AKAMAI TECHNOLOGIES\"");
		knownIPsCash.put("82.165.103.153","DE,01,Karlsruhe,,49.004700,8.385800,0,0,\"Germany\",\"Schlund + Partner AG\"");
		knownIPsCash.put("62.27.58.36","DE,05,Dreieich,,50.000000,8.700000,0,0,\"nacamar GmbH\",\"Venista Holding GmbH & Co. KG\"");
		knownIPsCash.put("78.35.6.130","DE,07,Kˆln,,50.933300,6.950000,0,0,\"NetCologne GmbH\",\"NetCologne GmbH\"");
		knownIPsCash.put("84.36.233.152","EG,11,Cairo,,30.049999,31.250000,0,0,\"EgyNet\",\"xDSL Service - Users\"");
		knownIPsCash.put("64.152.34.204","US,TX,Houston,77008,29.798700,-95.419197,618,713,\"Level 3 Communications\",\"Iland Internet Solutions Corporation\"");
		knownIPsCash.put("213.203.217.108","CH,05,Bern,,46.916698,7.466700,0,0,\"INET-People - Providerservices\",\"Akamai International B.V.\"");
		knownIPsCash.put("64.17.131.28","US,KY,Hopkinsville,42240,36.907398,-87.459702,659,270,\"Ecommerce Corporation\",\"Ecommerce Corporation\"");
		knownIPsCash.put("64.34.230.217","IL,(null),,,31.500000,34.750000,0,0,\"Peer 1 Network\",\"Spiral Solutions\"");
		knownIPsCash.put("60.28.207.52","CN,28,Tianjin,,39.142200,117.176697,0,0,\"CNCGROUP Tianjin province network\",\"Yiouwangluo Limited company\"");
		knownIPsCash.put("72.21.203.1","US,WA,Seattle,98104,47.602600,-122.328400,819,206,\"AMAZON.COM\",\"AMAZON.COM\"");
		knownIPsCash.put("66.135.221.11","US,CA,Campbell,95008,37.280300,-121.956703,807,408,\"eBay\",\"eBay\"");
		knownIPsCash.put("72.5.124.61","US,CA,Santa Clara,95054,37.396099,-121.961700,807,408,\"Internap Network Services\",\"SUN MICROSYSTEMS\"");
		knownIPsCash.put("62.27.58.36","DE,05,Dreieich,,50.000000,8.700000,0,0,\"nacamar GmbH\",\"Venista Holding GmbH & Co. KG\"");
		knownIPsCash.put("213.203.217.107","CH,05,Bern,,46.916698,7.466700,0,0,\"INET-People - Providerservices\",\"Akamai International B.V.\"");
		knownIPsCash.put("81.173.245.10","DE,(null),,,51.000000,9.000000,0,0,\"NetCologne GmbH\",\"RTL interactive GmbH\"");
		knownIPsCash.put("81.200.195.135","DE,16,Berlin,,52.516701,13.400000,0,0,\"Deutsche Bahn AG / DB Systems - German Railway\",\"Deutsche Bahn AG / DB Systems GmbH (German Railway\"");
		knownIPsCash.put("82.165.103.153","DE,01,Karlsruhe,,49.004700,8.385800,0,0,\"Germany\",\"Schlund + Partner AG\"");
		knownIPsCash.put("62.27.58.36","DE,05,Dreieich,,50.000000,8.700000,0,0,\"nacamar GmbH\",\"Venista Holding GmbH & Co. KG\"");
		knownIPsCash.put("78.35.6.130","DE,07,Kˆln,,50.933300,6.950000,0,0,\"NetCologne GmbH\",\"NetCologne GmbH\"");
		knownIPsCash.put("208.70.190.119","US,FL,Miami,33134,25.754101,-80.271004,528,305,\"Terra Networks Operations\",\"Terra Networks Operations\"");
		knownIPsCash.put("217.146.186.51","GB,H9,London,,51.500000,-0.116700,0,0,\"London\",\"Yahoo Europe Operations\"");
		knownIPsCash.put("217.72.195.42","DE,01,Karlsruhe,,49.004700,8.385800,0,0,\"Germany\",\"WEB.DE-AG\"");
		knownIPsCash.put("212.58.3.26","TR,34,Istanbul,,41.018600,28.964701,0,0,\"DORUK-NET\",\"DorukNet hosting block\"");
		knownIPsCash.put("216.239.59.104","US,CA,Mountain View,94043,37.419201,-122.057404,807,650,\"Google\",\"Google\"");
		knownIPsCash.put("194.158.58.68","MT,00,Sliema,,35.912498,14.501900,0,0,\"DATASTREAM\",\"GTXMedia\"");
		knownIPsCash.put("202.4.234.57","AU,02,Sydney,,-33.883301,151.216705,0,0,\"Anchor Systems Pty Ltd\",\"Anchor Systems Pty Ltd\"");
		knownIPsCash.put("208.70.190.119","US,FL,Miami,33134,25.754101,-80.271004,528,305,\"Terra Networks Operations\",\"Terra Networks Operations\"");
	}

	
    private boolean      showSQL = false;
        
	private DNSLocalizer dnsLocalizer = new DNSLocalizer(4000);
	
	private String       maxMindLicense = MAXMIND_LICENSE_VAL_TEST; 
	
	private String       dbURL = DB_URL_DEFAULT;		
	private String       dbUser;
	private String       dbPassword;
	private Connection   dbConnection;
}

