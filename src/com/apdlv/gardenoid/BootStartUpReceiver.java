package com.apdlv.gardenoid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootStartUpReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) 
    {
	// TODO: This method is called when the BroadcastReceiver is receiving

	// Start Service On Boot Start Up
	Intent serviceIntent = new Intent(context, GardenoidService.class);
	serviceIntent.setAction("BootStartUpReceiver.start");
	context.startService(serviceIntent);

	//Start App On Boot Start Up
	Intent activityIntent = new Intent(context, GardenoidActivity.class);
	activityIntent.setAction("BootStartUpReceiver.start");
	activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	context.startActivity(activityIntent);
    }
}