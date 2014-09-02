package com.apdlv.utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/*
if ( link.startsWith("https:") && System.getProperty( "java.protocol.handler.pkgs" ) == null )
{
	java.security.Provider p = (java.security.Provider) Class.forName( "com.sun.net.ssl.internal.ssl.Provider" ).newInstance();
	java.security.Security.addProvider( p );

	// -Djava.protocol.handler.pkgs=com.sun.net.ssl.internal.www.protocol
	System.getProperties().setProperty( "java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol" );
}
*/


public class WGet 
{
	public static final int DEFAULT_CONNECTION_TIMEOUT_MILLIES = 5000;
	
	
	public WGet()
	{
		this(DEFAULT_CONNECTION_TIMEOUT_MILLIES);
	}
	
	
	public WGet(int connectTimeoutMillies) 
	{
		this(false, connectTimeoutMillies);
	}
	

	public WGet(int connectTimeoutMillies, int readTimeoutMillies) 
	{
		this(false, connectTimeoutMillies, readTimeoutMillies);
	}

	
	public WGet(boolean followRedirects)
	{
		this(followRedirects, DEFAULT_CONNECTION_TIMEOUT_MILLIES);
	}

	
	public WGet(boolean followRedirects, int connectTimeoutMillies, int readTimeoutMillies) 
	{
		this(followRedirects, connectTimeoutMillies);
		this.readTimeoutMillies = readTimeoutMillies; 
	}
	
	public WGet(boolean followRedirects, int connectTimeoutMillies) 
	{
		this.followRedirects = followRedirects;
		this.connectTimeoutMillies = connectTimeoutMillies;
	}
		
	public byte[] get(String url) throws Exception
	{
		return this.get(url,null);
	}
	
	
	public byte[] get(String url, Map<String, String> requestHeaders) throws Exception
	{
		InputStream           is   = this.getInputStream(url, requestHeaders);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();		
		copyStream(is, baos);

		return baos.toByteArray();
	}


	public String getString(String url) throws Exception
	{
		return this.getString(url, null);
	}
	

	public String getString(String url, Map<String, String> requestHeaders) throws Exception
	{
		byte b[] = get(url, requestHeaders);
		String s = new String(b);
		return s;
	}
	
	
	public InputStream getInputStream(String url) throws Exception
	{
		return getInputStream(url,null);
	}
	
	
	public InputStream getInputStream(String url, Map<String,String> requestHeaders) throws Exception
	{
		if (url.toLowerCase().startsWith("https") )
		{
//			X509TrustManagerTrustAny.activate();
		    throw new RuntimeException("SSL not supported");
		}

		URL u = new URL(url);
		HttpURLConnection uc = (HttpURLConnection)u.openConnection();
		
		uc.setInstanceFollowRedirects(this.followRedirects);	
		uc.setConnectTimeout(this.connectTimeoutMillies);
		uc.setReadTimeout(this.readTimeoutMillies);
		
		if (null!=requestHeaders && requestHeaders.size()>0)
		{
			for (String key : requestHeaders.keySet())
			{
				uc.addRequestProperty(key, requestHeaders.get(key));
			}
		}

		uc.connect();

		this.responseCode = uc.getResponseCode();
		return uc.getInputStream();
	}
	
	
	public List<String> getLines(String url) throws Exception, IOException
	{
		InputStream       is  = getInputStream(url);
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader    br  = new BufferedReader(isr);
		
		List<String> list = new ArrayList<String>();
		String line = null;
		while (null != (line = br.readLine()))
		{
			list.add(line);
		}
		
		return list;
	}
	

	private static void copyStream(InputStream src, OutputStream dest) throws Exception 
	{
	    byte[] buffer = new byte[4096];	
	    int    len;
	    
	    while ((len = src.read(buffer)) > 0) 
	      dest.write(buffer, 0, len);

	    src.close();
	    dest.close();	
	}

	
	public int getResponseCode() 
	{
		return responseCode;
	}
	

	public void setFollowRedirects(boolean followRedirects)
	{
		this.followRedirects = followRedirects;
	}


	public boolean getFollowRedirects()
	{
		return followRedirects;
	}
	

	public void setConnectTimeoutMillies(int connectTimeoutMillies)
	{
		this.connectTimeoutMillies = connectTimeoutMillies;
	}


	public int getConnectTimeoutMillies()
	{
		return connectTimeoutMillies;
	}

	public int getConnectTimeoutSeconds()
	{
		return connectTimeoutMillies/1000;
	}

	public void setReadTimeoutMillies(int readTimeoutMillies)
	{
		this.readTimeoutMillies = readTimeoutMillies;
	}


	public int getReadTimeoutSeconds()
	{
		return readTimeoutMillies/1000;
	}

	public int getReadTimeoutMillies()
	{
		return readTimeoutMillies;
	}

	private int     responseCode;
	private boolean followRedirects = false;
	private int     connectTimeoutMillies = 1000;
	private int     readTimeoutMillies    = 5000;


	/*
	public static void main(String[] args) 
	{
		WGet wget = new WGet();
		
		try 
		{			
			TreeMap<String, String> headers = new TreeMap<String,String>();
			headers.put("Test", "bla bla");			
			headers.put("MyHeader", "as;klkfjaklsfasfkljaklsjfalskj");			
			headers.put("User-Agent", "NokiaN95");			
			
			String response = wget.getString("http://mjoy.com", headers);
			System.out.println("Response:\n" + response);
			
			//String response = wget.getString("http://kernel.org/pub/linux/kernel/v2.6/ChangeLog-2.6.23.9");
			
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		} 
	}
	*/

}
