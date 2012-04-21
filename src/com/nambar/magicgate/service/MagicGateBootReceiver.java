package com.nambar.magicgate.service;

import com.nambar.magicgate.R;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class MagicGateBootReceiver extends BroadcastReceiver
{
	@Override
	public void onReceive(Context context, Intent intent)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		if(prefs.getBoolean(context.getText(R.string.PREF_START_AT_BOOT).toString(), true))
		{
			Intent serviceIntent = new Intent(context, MagicGateService.class);
			context.startService(serviceIntent);
		}
		
	}
}
