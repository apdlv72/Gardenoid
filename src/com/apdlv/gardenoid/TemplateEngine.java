package com.apdlv.gardenoid;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

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
		// uncompress on the fly of the client does not support compression
		is = new GZIPInputStream(is);
	    }
	} 
	catch (IOException e) 
	{
	    System.err.println("getFile: " + e);
	} 
	return is;
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
		is = mAssetManager.open(DIR_PREFIX_WEBPAGES + "/" + name);
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
}
