package com.apdlv.gardenoid.db;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Calendar;

import org.json.JSONException;
import org.json.JSONObject;

import com.apdlv.utils.U;

public class Event
{
    public Event(Throwable t)
    {
	this("exception", (String)null);
	StringWriter sw = new StringWriter();
	PrintWriter pw = new PrintWriter(sw);
	t.printStackTrace(pw);
	
	JSONObject o = new JSONObject();
	try
        {
	    o.put("trace", sw.toString());
        } 
	catch (JSONException e)
        {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
        }
	this.json = o.toString();
    }
    
    public Event(String message)
    {
	this(message, (String)null);
    }

    public Event(String message, String conditions)
    {
	this(-1, message, conditions);
    }

    public Event(String message, Object ... keyVal) 
    {
	this(message);
	
	JSONObject json = new JSONObject();
	for (int i=1; i<keyVal.length; i+=2)	    
	{
	    String key = "" + keyVal[i-1];
	    String val = "" + keyVal[i];
	    try
            {
	        json.put(key, val);
            } 
	    catch (JSONException e)
            {
	        e.printStackTrace();
            }
	}
	
	this.json = json.toString();
    }
    
    public Event(int activeMask, String message, String json)
    {
	this(Calendar.getInstance(), activeMask, message, json);
    }


    
    public Event(int activeMask, String message, Object ... keyVal) 
    {
	this(activeMask, message, (String)null);
	this.datetime = U.now();
	
	JSONObject json = new JSONObject();
	for (int i=1; i<keyVal.length; i+=2)	    
	{
	    String key = "" + keyVal[i-1];
	    String val = "" + keyVal[i];
	    try
            {
	        json.put(key, val);
            } 
	    catch (JSONException e)
            {
	        e.printStackTrace();
            }
	}
	
	this.json = json.toString();
    }

    public Event(Calendar date, int activeMask, String message, String json)
    {
	this(-1,date,activeMask,message, json);
    }

    public Event(int id, Calendar datetime, int activeMask, String message, String conditions)
    {
	this.id = id;
	this.datetime = datetime;
	this.activeMask = activeMask;
	this.message = message;
	this.json = conditions;
    }

    public Calendar getDate()
    {
        return datetime;
    }

    public int getActiveMask()
    {
        return activeMask;
    }

    public String getMessage()
    {
        return message;
    }

    public String getJson()
    {
        return json;
    }

    private int id;
    private Calendar datetime;
    private int activeMask;
    private String message;
    private String json;

    @Override
    public String toString() { return toJson(); }
    
    public String toJson()
    {
	try
	{
	    String str = new JSONObject()
	    .put("id",   id)
	    .put("date", U.YYYYMMDD_hhmmss.format(datetime.getTime()))
	    .put("mask", activeMask)
	    .put("msg",  message)
	    .toString();
	    str = str.replace("}", ",\"json\":" + json + "}");
	    return str;
	}
	catch (Exception e)
	{
	    e.printStackTrace();
	    return ""+e;
	}
    }
}
