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
	Intent service = new Intent(context, GardenoidService.class);
	context.startService(service);

	//Start App On Boot Start Up
	Intent App = new Intent(context, GardenoidActivity.class);
	App.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	context.startActivity(App);
    }
}