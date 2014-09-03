package com.apdlv.gardenoid;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import android.content.res.AssetManager;

public class TemplateEngine
{
    public static final String DIR_PREFIX_WEBPAGES  = "webpages";
    //public static final String DIR_PREFIX_CHECKSUMS = "checksums";    

    public TemplateEngine(AssetManager assetManager, File externalDir)
    {
	mAssetManager = assetManager;
	//mExternalDir  = externalDir;
    }
    
    /*
    public String render(String templateName, Map<String,String> map)
    {
	try
	{
	    String page = getPage(templateName);
	    if (null!=page)
	    {
		for (String key : map.keySet())
		{
		    String needle = "\\{\\{" + key + "\\}\\}";
		    String val = emptyIfNull(map.get(key));
		    val = val.replaceAll("\\$", "\\\\\\$");
		    page = page.replaceAll(needle, val);
		}
	    }
	    return page;
	}
	catch (Exception e)
	{
	    StringWriter sw = new StringWriter();
	    e.printStackTrace(new PrintWriter(sw));
	    String exceptionAsString = sw.toString();
	    return exceptionAsString;
	}
    }

    public String render(String templateName)
    {
	return getPage(templateName);
    }
    */
    
    public InputStream getRawFile(String fileName)
    {
	try
	{
	    return findInputStream(fileName);
	}
	catch (IOException e) 
	{
	    System.err.println("getFile: " + e);
	} 
	return null;
    }
    
    public InputStream getFile(String fileName, boolean gzipAccepted)
    {
	// TODO: always read compressed version and unzip on the fly if not supported since GZIP support is the default rather than the exception  
	InputStream is = null;
	try 
	{
	    if (fileName.startsWith("/")) fileName = fileName.substring(1);
	    fileName += ".gzip";
	    is = findInputStream(fileName);
	    
	    if (null!=is && !gzipAccepted)
	    {
		// Uncompress on the fly of the client does not support compression
		// Note: cannot just return a GZIPInputStream here bacause NanoHTTPD will fail
		// since it cannot determine the ContentLength because GZIPInputStream.available
		// always returns 1.
		is = gunzip(is);
		System.err.println("UNZIPPING ON THE FLY: " + fileName);
	    }
	    
	    if (null!=is) System.out.println("getFile: is.available=" + is.available());
	} 
	catch (IOException e) 
	{
	    System.err.println("getFile: " + e);
	} 
	return is;
    }

    private ByteArrayInputStream gunzip(InputStream is) throws IOException
    {
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	GZIPInputStream       gzip = null;	
	try
	{
	    gzip = new GZIPInputStream(is);
	    
	    byte b[] = new byte[1024];

	    do 
	    {
		int n = gzip.read(b, 0, b.length);
		baos.write(b, 0, n);
	    }
	    while (gzip.available()>0);
	}
	catch (Exception e)
	{
	    System.err.println("uncompress: " + e);
	}
	if (null!=gzip) gzip.close();
	if (null!=is)   is.close();
	
	ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
	baos.close();
	return bais;
    }

    /*
    public String getPage(String templateName)
    {
	BufferedReader reader = null;
	try {
	    if (templateName.startsWith("/")) templateName = templateName.substring(1);
	    InputStream is = findInputStream(templateName);
	    String str = readAll(is);
	    return str;
	} 
	catch (IOException e) 
	{
	    System.err.println("getPage: " + e);
	} 
	finally 
	{
	    if (reader != null) 
	    {
		try
		{
		    reader.close();
		} 
		catch (IOException e)
		{
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		}
	    }
	}
	return null;
    }
    */
    
    //@SuppressWarnings("resource")
    private InputStream findInputStream(String name) throws IOException
    {
	InputStream is = null; 
	try
	{
//	    File file = new File(mExternalDir, name);
//	    if (file.exists())
//	    {
//		System.out.println("findInputStream: found on external storage: " + file);
//		is = new FileInputStream(file);
//	    }
//	    else
//	    {	
//		System.out.println("findInputStream: NOT found on external storage: " + file); 
	    	String path =  DIR_PREFIX_WEBPAGES + (name.startsWith("/") ? "" : "/") + name;
		is = mAssetManager.open(path);
		if (null==is)
		{
		    System.err.println("findInputStream: NOT FOUND: " + path);
		}
//	    }
	}
	catch (Exception e)
	{
	    System.err.println("findInputStream: " + e);
	}
	return is;
    }

    /*
    private static String emptyIfNull(String s)
    {
	return null==s ? "" : s;
    }
    */
    
    /*
    public String getAsset(String path) throws IOException
    {
	InputStream is = mAssetManager.open(path);
	return readAll(is);
    }
    */

    /*
    public String readExternalFile(String path) throws IOException
    {
	File file = new File(mExternalDir, path);
	if (file.exists())
	{
	    System.out.println("findInputStream: found on external storage: " + file);
	    InputStream is = new FileInputStream(file);
	    return readAll(is);
	}
	return null;
    }
    */

    /*
    private String readAll(InputStream is) throws IOException
    {
	if (null==is) return null;
	BufferedReader reader = new BufferedReader(new InputStreamReader(is));

	// do reading, usually loop until end of file reading  
	String mLine = reader.readLine();
	StringBuilder sb = new StringBuilder();
	while (mLine!=null) 
	{
	    sb.append(mLine).append("\r\n");
	    mLine = reader.readLine();
	}

	return sb.toString();
    }
    */

    private AssetManager mAssetManager;
    //private File         mExternalDir;

    public static InputStream compress(String uri, String data) throws IOException
    {
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	GZIPOutputStream gzip = new GZIPOutputStream(baos, 16384){{def.setLevel(Deflater.BEST_COMPRESSION);}};	
	//GZIPOutputStream      gzip = new GZIPOutputStream(baos); 
	byte[] bytes = data.getBytes();
	gzip.write(bytes);	
	gzip.close();
	baos.close();
	
	byte[] compr = baos.toByteArray();

	int in  = data.length();
	int raw = bytes.length;
	int out = compr.length;	
	System.out.println("compress " + uri + ": IN=" + in + " bytes, RAW=" + raw + ", OUT=" + out + " bytes");

	return new ByteArrayInputStream(compr);
    }
}
