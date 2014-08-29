package com.apdlv.gardenoid.db;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import com.apdlv.utils.U;


public class Forecast
{
    public Forecast() {}
 
    public Forecast(
	    Calendar day,
	    int      code,
	    String   text,
	    int      low,
	    int      high
	    ) 
    {
	super();
	this.day  = day;
	this.code = code;
	this.text = text;
	this.low  = low;
	this.high = high;
    }
 
    public Map<String,String> toMap()
    {
	HashMap<String, String> m = new HashMap<String, String>();
	m.put("day",  "" + day);
	m.put("code", "" + code);
	m.put("text", text);
	m.put("low",  "" + low);
	m.put("high", "" + high);
	return m;
    }
    
    @Override
    public String toString() 
    {
	Calendar now = DAO.todayNoon();
        return Weather.class.getSimpleName() + toJson(now); 
    }
        
    //getters & setters

    public long getId()
    {
	return id;
    }

    public void setId(long id)
    {
	this.id = id;	
    }
 
    public Calendar getDay()
    {
        return day;
    }

    public int getCode()
    {
        return code;
    }

    public String getText()
    {
        return text;
    }

    public int getLow()
    {
        return low;
    }

    public int getHigh()
    {
        return high;
    }

    public Calendar getUpdated()
    {
        return updated;
    }

    public void setUpdated(Calendar updated)
    {
        this.updated = updated;
    }

    public void setDay(Calendar day)
    {
        this.day = day;
    }
    public void setCode(int code)
    {
        this.code = code;
    }

    public void setText(String text)
    {
        this.text = text;
    }

    public void setLow(int low)
    {
        this.low = low;
    }

    public void setHigh(int high)
    {
        this.high = high;
    }

    public String toJson(Calendar todayNoon)
    {
	StringBuilder sb = new StringBuilder();
	sb.append("  { ");
	sb.append("\"id\": ").append(getId()).append(", ");
	sb.append("\"day\": \"").append(U.YYYYMMDD.format(getDay().getTime())).append("\", ");
	sb.append("\"code\": ").append(getCode()).append(", ");
	sb.append("\"text\": ").append(U.escapedOrNull(getText())).append(", ");
	sb.append("\"low\": ").append(getLow()).append(", ");
	sb.append("\"high\": ").append(getHigh()).append(", ");
	
	Calendar dayNoon = getDay();
	boolean future = dayNoon.after(todayNoon); 
	sb.append("\"future\": ").append(future).append(", ");
	
	Calendar u = getUpdated();
	String updated = null==u ? "null" : "\"" + U.YYYYMMDD_hhmmss.format(getUpdated().getTime()) + "\""; 
	
	sb.append("\"upd\": ").append(updated);
	sb.append(" }");
	return sb.toString();
    }

    public static final Forecast NONE = new Forecast(Calendar.getInstance(), -1, "UNAVAILABLE", 0, 0);

    private long     id;
    private Calendar day, updated;
    private int      code;
    private String   text;
    private int      low;
    private int      high;
}
