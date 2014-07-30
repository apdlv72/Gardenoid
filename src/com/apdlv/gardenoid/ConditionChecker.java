package com.apdlv.gardenoid;

import android.util.Log;

import com.apdlv.gardenoid.db.DAO;
import com.apdlv.gardenoid.db.Schedule;

public class ConditionChecker
{
    public ConditionChecker(DAO database)
    {
	this.mDatabase = database;
    }
    
    
    public boolean isFullFilled(Schedule schedule)
    {
	boolean conditionFullfilled = matches(schedule.getIdCondition(), schedule.getConditionArgs());
	int idEx = schedule.getIdException();
	boolean exceptionFullfilled = idEx>0 && matches(idEx, schedule.getExceptionArgs());
	boolean rc = conditionFullfilled && !exceptionFullfilled;
	
	Log.d(TAG, "isFullFilled: " + schedule + ": cond=" + conditionFullfilled + ", e=" + exceptionFullfilled + ", rc=" + rc);
	return rc;
    }

    
    private boolean matches(int id, String args)
    {
	Conditional c = Conditional.CONDITIONALS.get(id);	
	return null!=c && c.matches(mDatabase, args);
    }

    
    private static final String TAG = ConditionChecker.class.getSimpleName();
    
    private DAO mDatabase;
}
