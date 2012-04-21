package com.nambar.magicgate.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MagicGateAlarmReceiver extends BroadcastReceiver
{
	@Override
	public void onReceive(Context context, Intent intent)
	{
		MagicGateService service = MagicGateService.getInstance();
		if(service != null)
		{
			MagicGateService.getInstance().wakeup();
		}
	}
}
