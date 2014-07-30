package com.apdlv.gardenoid.db;

import com.apdlv.gardenoid.MyJson;


public class Schedule
{
    public static final int DAY_SUN = 0;
    public static final int DAY_MON = 1;
    public static final int DAY_TUE = 2;
    public static final int DAY_WED = 3;
    public static final int DAY_THU = 4;
    public static final int DAY_FRI = 5;
    public static final int DAY_SAT = 6;

    public static final String dayName(int day)
    {
	switch (day%7)
	{
	case DAY_SUN : return "Sun"; 
	case DAY_MON : return "Mon"; 
	case DAY_TUE : return "Tue"; 
	case DAY_WED : return "Wed"; 
	case DAY_THU : return "Thu"; 
	case DAY_FRI : return "Fri"; 
	case DAY_SAT : return "Sat"; 
	}
	return null;
    }

    public static final int MONTH_JAN =  1;
    public static final int MONTH_FEB =  2;
    public static final int MONTH_MAR =  3;
    public static final int MONTH_APR =  4;
    public static final int MONTH_MAY =  5;
    public static final int MONTH_JUN =  6;
    public static final int MONTH_JUL =  7;
    public static final int MONTH_AUG =  8;
    public static final int MONTH_SEP =  9;
    public static final int MONTH_OCT = 10;
    public static final int MONTH_NOV = 11;
    public static final int MONTH_DEC = 12;
    
    public static final Schedule TEMPLATE = new Schedule(
	    0, 0x37, 0x01ff8, // no strand, Mo-Fr, Apr-Sep  
	    1000, 1800, /*00*/10, /*0*/400,  // from 10:00 till 18:00 ten minutes every 4 hours
	    0, null, 0, null // unconditional, no exception
	    , false
	    );
    
    public static final String monthName(int month)
    {
	switch (((month-1)%12)+1)
	{
	case 0       : return null;
	case MONTH_JAN : return "Jan"; 
	case MONTH_FEB : return "Feb"; 
	case MONTH_MAR : return "Mar"; 
	case MONTH_APR : return "Apr"; 
	case MONTH_MAY : return "May"; 
	case MONTH_JUN : return "Jun"; 
	case MONTH_JUL : return "Jul"; 
	case MONTH_AUG : return "Aug"; 
	case MONTH_SEP : return "Sep"; 
	case MONTH_OCT : return "Oct"; 
	case MONTH_NOV : return "Nov"; 
	case MONTH_DEC : return "Dec"; 
	}
	return null;
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
	StringBuilder sb = new StringBuilder();
	sb.append("    { ");        	    
	sb.append(  "\"id\" : ").append(getId());
	sb.append(", \"active\" : ").append(getActive());
	sb.append(", \"strandMask\" : ").append(getStrandMask());
	sb.append(", \"dayMask\" : ").append(getDayMask());
	sb.append(", \"monthMask\" : ").append(getMonthMask());

	sb.append(", \"startTime\" : \"").append(getStartTime()).append("\"");
	sb.append(", \"endTime\" : \"").append(getEndTime()).append("\"");
	sb.append(", \"duration\" : \"").append(getDuration()).append("\"");
	sb.append(", \"interval\" : \"").append(getInterval()).append("\"");
	sb.append(", \"idCondition\" : ").append(getIdCondition());
	sb.append(", \"conditionArgs\" : ").append(MyJson.nullOrInDoubleQuotes(getConditionArgs())).append("");
	sb.append(", \"idException\" : ").append(getIdException());
	sb.append(", \"exceptionArgs\" : ").append(MyJson.nullOrInDoubleQuotes(getExceptionArgs())).append("");    
	sb.append(", \"power\" : ").append(isPower());    
	sb.append(" }");
	return sb.toString();	
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
