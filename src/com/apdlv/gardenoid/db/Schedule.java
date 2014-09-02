package com.apdlv.gardenoid.db;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.apdlv.utils.U;

public class Schedule
{
    public static final String[] DAY_NAMES = new String[]
    {
	"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat",
    };
    
    public static final String[] MONTH_NAMES = new String[]
    {
	null, // 0
	"Jan", "Feb", "Mar", "Apr", "Mar", "May",
	"Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };
    
    public static final Schedule TEMPLATE = new Schedule(
	    0, 0x37, 0x01ff8, // no strand, Mo-Fr, Apr-Sep  
	    1000, 1800, /*00*/10, /*0*/400,  // from 10:00 till 18:00 ten minutes every 4 hours
	    0, null, 0, null // unconditional, no exception
	    , false
	    );
    
    public static final String dayName(int day)
    {
	return DAY_NAMES[(day%7)];
    }
    
    public static final String monthName(int month)
    {
	return MONTH_NAMES[(((month-1)%12)+1)];
    }
    
    public Schedule() {}
 
    public Schedule(
	    int strandMask, 
	    int dayMask, 
	    int monthMask, 
	    int startTime, 
	    int endTime, 
	    int duration, 
	    int interval, 
	    int idCondition, 
	    String conditionArgs, 
	    int idException, 
	    String exceptionArgs,
	    boolean on)
    {
	this(strandMask, dayMask, monthMask, startTime, endTime, duration, interval, idCondition, conditionArgs, idException, exceptionArgs, on, false);
    }
    
    
    public Schedule(
	    int strandMask, 
	    int dayMask, 
	    int monthMask, 
	    int startTime, 
	    int endTime, 
	    int duration, 
	    int interval, 
	    int idCondition, 
	    String conditionArgs, 
	    int idException, 
	    String exceptionArgs,
	    boolean on, 
	    boolean active) 
    {
	super();
	this.strandMask    = strandMask;
	this.dayMask       = dayMask;
	this.monthMask     = monthMask;
	this.startTime     = startTime;
	this.endTime       = endTime;
	this.duration      = duration;
	this.interval      = interval;
	this.idCondition   = idCondition;
	this.conditionArgs = conditionArgs;
	this.idException   = idException;
	this.exceptionArgs = exceptionArgs;
	this.power = on;
    }
 
    public boolean isPower()
    {
        return power;
    }

    public void setPower(boolean on)
    {
        this.power = on;
    }

    //getters & setters

    public long getId()
    {
	return id;
    }

    public long setId(long id)
    {
	return (this.id = id);	
    }

 
    public int getStrandMask()
    {
        return strandMask;
    }

    public void setStrandMask(int strandMask)
    {
        this.strandMask = strandMask;
    }

    public int getDayMask()
    {
        return dayMask;
    }

    public void setDayMask(int dayMask)
    {
        this.dayMask = dayMask;
    }

    public int getMonthMask()
    {
        return monthMask;
    }

    public void setMonthMask(int monthMask)
    {
        this.monthMask = monthMask;
    }

    public int getStartTime()
    {
        return startTime;
    }

    public void setStartTime(int startTime)
    {
        this.startTime = startTime;
    }

    public int getEndTime()
    {
        return endTime;
    }

    public void setEndTime(int endTime)
    {
        this.endTime = endTime;
    }

    public int getDuration()
    {
        return duration;
    }

    public void setDuration(int duration)
    {
        this.duration = duration;
    }

    public int getInterval()
    {
        return interval;
    }

    public void setInterval(int interval)
    {
        this.interval = interval;
    }

    public int getIdCondition()
    {
        return idCondition;
    }

    public void setIdCondition(int idCondition)
    {
        this.idCondition = idCondition;
    }

    public int getIdException()
    {
        return idException;
    }

    public void setIdException(int idException)
    {
        this.idException = idException;
    }

    public String getConditionArgs()
    {
        return conditionArgs;
    }

    public void setConditionArgs(String conditionArgs)
    {
        this.conditionArgs = conditionArgs;
    }

    public String getExceptionArgs()
    {
        return exceptionArgs;
    }

    public void setExceptionArgs(String exceptionArgs)
    {
        this.exceptionArgs = exceptionArgs;
    }
    
    public void setActive(boolean b)
    {
	this.active = b;
    }

    public boolean getActive()
    {
	return active;
    }

    public String toJson()
    {
//	HashMap<String, Object> map = 
//		new HashMap<String, Object>()
//		{{
//		   put("id",getId()); 
//		   put("active",getActive()); 
//		   put("strandMask",getStrandMask()); 
//		   put("dayMask",getDayMask()); 
//		   put("monthMask",getMonthMask()); 
//		   put("startTime",getStartTime()); 
//		   put("endTime",getEndTime()); 
//		   put("duration",getDuration()); 
//		   put("interval",getInterval()); 
//		   put("idCondition",getIdCondition()); 
//		   put("conditionArgs",getConditionArgs()); 
//		   put("idException",getIdException()); 
//		   put("exceptionArgs",getExceptionArgs()); 
//		   put("power",isPower()); 
//		}};
//	return (new JSONObject(map)).toString();
	
	return U.toJson(
		"id",            id, 
                "active",        active, 
                "strandMask",    strandMask, 
                "dayMask",       dayMask,
                "monthMask",     monthMask, 
                "startTime",     startTime, 
                "endTime",       endTime,
                "duration",      duration, 
                "interval",      interval, 
                "idCondition",   getIdCondition(), 
                "conditionArgs", getConditionArgs(), 
                "idException",   getIdException(), 
                "exceptionArgs", getExceptionArgs(), 
                "power",         isPower()); 
	
//	StringBuilder sb = new StringBuilder();
//	sb.append("{");        	    
//	sb.append(  "\"id\":").append(getId());
//	sb.append(",\"active\":").append(getActive());
//	sb.append(",\"strandMask\":").append(getStrandMask());
//	sb.append(",\"dayMask\":").append(getDayMask());
//	sb.append(",\"monthMask\":").append(getMonthMask());
//	sb.append(",\"startTime\":\"").append(getStartTime()).append("\"");
//	sb.append(",\"endTime\":\"").append(getEndTime()).append("\"");
//	sb.append(",\"duration\":\"").append(getDuration()).append("\"");
//	sb.append(",\"interval\":\"").append(getInterval()).append("\"");
//	sb.append(",\"idCondition\":").append(getIdCondition());
//	sb.append(",\"conditionArgs\":").append(U.nullOrEscapedInDoubleQuotes(getConditionArgs())).append("");
//	sb.append(",\"idException\":").append(getIdException());
//	sb.append(",\"exceptionArgs\":").append(U.nullOrEscapedInDoubleQuotes(getExceptionArgs())).append("");    
//	sb.append(",\"power\":").append(isPower());    
//	sb.append("}");
//	return sb.toString();	
    }
    
    @Override
    public String toString() 
    {
        return Schedule.class.getSimpleName() + toJson();
    }
        
    private long id;
    private int strandMask;
    private int dayMask;
    private int monthMask;
    private int startTime;
    private int endTime;
    private int duration;
    private int interval;
    private int idCondition;
    private int idException;
    
    private String conditionArgs;
    private String exceptionArgs;
    private boolean active; // active (according to time, not weather conditions)
    private boolean power; // powered 

}
