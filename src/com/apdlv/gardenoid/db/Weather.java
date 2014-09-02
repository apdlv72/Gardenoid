package com.apdlv.gardenoid.db;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import com.apdlv.utils.U;


public class Weather
{
    public static final Weather NONE = new Weather(DAO.now(), -1, "UNAVAILABLE", 0, 0, 0, 0, 0);

    public Weather() {}
 
    public Weather(
	    Calendar date,
	    int      code,
	    String   text,
	    int      temperature,
	    int      humidity,
	    float    visibility,
	    float    pressure,
	    float    rising
	    ) 
    {
	this.date        = date;
	this.code        = code;
	this.text        = text;
	this.temperature = temperature;
	this.humidity    = humidity;
	this.visibility  = visibility;
	this.pressure    = pressure;
	this.rising      = rising;
	this.updated     = now();	
    }
 
    public int getHumidity()
    {
        return humidity;
    }

    public void setHumidity(int humidity)
    {
        this.humidity = humidity;
    }

    public float getVisibility()
    {
        return visibility;
    }

    public void setVisibility(float visibility)
    {
        this.visibility = visibility;
    }

    public float getPressure()
    {
        return pressure;
    }

    public void setPressure(float pressure)
    {
        this.pressure = pressure;
    }

    public float getRising()
    {
        return rising;
    }

    public void setRising(float rising)
    {
        this.rising = rising;
    }

    public void setDate(Calendar date)
    {
        this.date = date;
    }

    public void setCode(int code)
    {
        this.code = code;
    }

    public void setText(String text)
    {
        this.text = text;
    }

    public void setTemperature(int temperature)
    {
        this.temperature = temperature;
    }

    public void setUpdated(Calendar at)
    {
	this.updated = at;	
    }    

    public Map<String,String> toMap()
    {
	HashMap<String, String> m = new HashMap<String, String>();
	m.put("date",        "" + date);
	m.put("code",        "" + code);
	m.put("text",        text);
	m.put("temperature", "" + temperature);
	return m;
    }
    
    @Override
    public String toString() 
    {
        return Weather.class.getSimpleName() + toJson(); 
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
 
    public Calendar getDate()
    {
        return date;
    }

    public int getCode()
    {
        return code;
    }

    public String getText()
    {
        return text;
    }

    public int getTemperature()
    {
        return temperature;
    }

    public Calendar getUpdated()
    {
	return updated;
    }
    
    private static Calendar now()
    {
	return Calendar.getInstance();
    }

    public String toJson()
    {
	return U.toJson(
		"id",   id,
		"date", U.YYYYMMDD_hhmmss.format(getDate().getTime()),
		"code", code, 
		"text", text,
		"t",    temperature,
		"hum",  humidity,
		"visi", visibility,
		"p",    pressure,
		"rise", rising,
		"upd",  U.YYYYMMDD_hhmmss.format(getUpdated().getTime())
		);		
//	StringBuilder sb = new StringBuilder();
//	sb.append("  { ");
//	sb.append("\"id\": ").append(getId()).append(", ");
//	sb.append("\"date\": \"").append(U.YYYYMMDD_hhmmss.format(getDate().getTime())).append("\", ");
//	sb.append("\"code\": ").append(getCode()).append(", ");
//	sb.append("\"text\": ").append(U.escapedOrNull(getText())).append(", ");
//	sb.append("\"t\": ").append(getTemperature()).append(", ");
//	sb.append("\"hum\": ").append(getHumidity()).append(", ");
//	sb.append("\"visi\": ").append(getVisibility()).append(", ");
//	sb.append("\"p\": ").append(getPressure()).append(", ");
//	sb.append("\"rise\": ").append(getRising()).append(", ");
//	sb.append("\"upd\": \"").append(U.YYYYMMDD_hhmmss.format(getUpdated().getTime())).append("\"");
//	sb.append(" }");
//	return sb.toString();
    }   

    private long     id;
    private Calendar date, updated;
    private int      code;
    private String   text;
    private int      temperature;
    
    private int      humidity;
    private float    visibility;
    private float    pressure;
    private float    rising;
}
