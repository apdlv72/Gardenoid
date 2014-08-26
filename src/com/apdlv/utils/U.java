package com.apdlv.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class U
{

    public static final void sleep(int secs)
    {
	mSleep(1000*secs);
    }

    public static void mSleep(int millies)
    {
	try
	{
	    Thread.sleep(millies);
	}
	catch (InterruptedException e)
	{
	    //e.printStackTrace();
	}
    }


    public static String asString(Throwable t)
    {
	StringWriter sw = new StringWriter();
	PrintWriter pw = new PrintWriter(sw);
	t.printStackTrace(pw);
	return sw.toString(); // stack trace as a string	
    }

    public static String escapedOrNull(String text)
    {
	// TODO Auto-generated method stub
	return null==text ? "null" : "\"" + text.replaceAll("\"", "\\\"") + "\"";
    }

    public static final SimpleDateFormat YYYYMMDD_hhmmss = new  SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static final SimpleDateFormat YYYYMMDD = new  SimpleDateFormat("yyyy-MM-dd");

    public static Integer toInt(String string)
    {
	return null==string ? null : Integer.parseInt(string);
    }

    public static Float toFloat(String string)
    {
	return null==string ? null : Float.parseFloat(string);
    }

    public static long millis()
    {
	return now().getTimeInMillis();
    }
    
    public static Calendar now()
    {
	return Calendar.getInstance();
    }

    public static Integer hh_mmToInt(String string)
    {
	if (null==string) return null;
	return toInt(string.replaceAll("^0+","").replace(":", ""));
    }

    public static int hhmmToInt(String string)
    {
	string = string.replaceAll("^0+","");
	if ("".equals(string)) string = "0";
	return toInt(string);
    }

    public static long toLong(String string)
    {
	return null==string ? null : Long.parseLong(string);
    }

    public static String emptyIfNull(String s)
    {
	return null==s ? "" : s;
    }

    public static String toYYYYMMDD_hhmmss(Calendar c)
    {
	return YYYYMMDD_hhmmss.format(c.getTime());
    }

    public static String urlEncode(String s)
    {
	return null==s ? "" : URLEncoder.encode(s);
    }

}
