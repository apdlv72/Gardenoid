package com.apdlv.gardenoid;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import android.content.res.AssetManager;

public class TemplateEngine
{
    public static final String DIR_PREFIX_WEBPAGES = "webpages";
    

    public TemplateEngine(AssetManager assetManager, File externalDir)
    {
	mAssetManager = assetManager;
	mExternalDir  = externalDir;
    }
    
    
    public String render(String templateName, Map<String,String> map)
    {
	try
	{
	    String page = getPage(templateName);	
	    for (String key : map.keySet())
	    {
		String needle = "\\{\\{" + key + "\\}\\}";
		String val = emptyIfNull(map.get(key));
		val = val.replaceAll("\\$", "\\\\\\$");
		page = page.replaceAll(needle, val);
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

    
    public InputStream getFile(String fileName)
    {
	try 
	{
	    if (fileName.startsWith("/")) fileName = fileName.substring(1);
	    InputStream is = findInputStream(fileName);	    
	    return is;
	} 
	catch (IOException e) 
	{
	    System.err.println("getFile: " + e);
	} 
	return null;
    }


    public String getPage(String templateName)
    {
	BufferedReader reader = null;
	try {
	    if (templateName.startsWith("/")) templateName = templateName.substring(1);
	    InputStream ims = findInputStream(templateName);
	    if (null==ims) return null;
	    reader = new BufferedReader(new InputStreamReader(ims));

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

    
    @SuppressWarnings("resource")
    private InputStream findInputStream(String name) throws IOException
    {
	InputStream is = null; 
	try
	{
	    File file = new File(mExternalDir, name);
	    if (file.exists())
	    {
		System.out.println("findInputStream: found on external storage: " + file);
		is = new FileInputStream(file);
	    }
	    else
	    {	
		System.out.println("findInputStream: NOT found on external storage: " + file);
		is = mAssetManager.open(DIR_PREFIX_WEBPAGES + "/" + name);
	    }
	}
	catch (Exception e)
	{
	    System.err.println("findInputStream: " + e);
	}
	return is;
    }

    
    private static String emptyIfNull(String s)
    {
	return null==s ? "" : s;
    }

    private AssetManager mAssetManager;
    private File mExternalDir;

}
