
package com.apdlv.ilibaba.bt;

import java.util.HashSet;

import com.apdlv.ilibaba.bt.SPPDataHandler.Device;


import android.app.Dialog;
import android.content.Context;
import android.view.View;

public abstract class SPPStatusAwareDialog extends Dialog implements SPPStatusListener, android.view.View.OnClickListener
{
    public abstract void onClick(int id);    

    public SPPStatusAwareDialog(Context context)
    {
	super(context);
    }

    public void registerViews(int ... ids)
    {
	registerViews(true, true, ids);
    }

    public void registerViews(boolean hide, int ... ids)
    {
	registerViews(hide, true, ids);
    }

    public void registerViews(boolean hide, boolean onClick, int ... ids)
    {
	for (int id: ids)
	{
	    View v = findViewById(id);
	    if (null!=v) registerView(hide, onClick, v);
	}
    }

    public void registerView(View v)
    {
	registerView(true, true, v);
    }

    public void registerView(boolean hide, View v)
    {
	registerView(hide, true, v);
    }

    public void registerView(boolean hide, boolean onClick, View v)
    {
	if (null==v) return;
	(hide ? invisibleWhenDisconnected : disabledWhenDisconnected).add(v); 
	if (onClick) v.setOnClickListener(this);
    }

    public void setStatus(boolean connected)
    {
	for (View v : invisibleWhenDisconnected) 
	{
	    v.setVisibility(connected ? View.VISIBLE : View.INVISIBLE);
	}
	for (View v : disabledWhenDisconnected)
	{
	    v.setEnabled(connected);
	}	
    }

    public void onConnect(Device device)
    {
	setStatus(true);
    }

    public void onDisconnect(Device device)
    {
	setStatus(false);
    }

    public void onClick(View v)
    {
	int id = v.getId();
	this.onClick(id);
    }

    private HashSet<View> invisibleWhenDisconnected = new HashSet<View>(); 
    private HashSet<View> disabledWhenDisconnected  = new HashSet<View>();
}
