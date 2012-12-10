package com.nambar.magicgate.service;

import android.location.Location;
import android.os.Binder;

import com.nambar.magicgate.common.Gate;
import com.nambar.magicgate.common.MagicGateLocationListener;

public class MagicGateServiceBinder extends Binder
{
	private MagicGateService service;
	
	public MagicGateServiceBinder(MagicGateService service)
	{
		this.service = service;
	}
	
	public void registerHandler(MagicGateLocationListener handler)
	{
		service.registerHandler(handler);
	}
	
	public void unregisterHandler()
	{
		service.unregisterHandler();
	}

	public Location getLastLocation()
	{
		return service.getLastLocation();
	}

	public void openGate(Gate gate)
	{
		service.openGate(gate);
	}

	public void notificationAlwaysOn(boolean value)
	{
		service.notificationAlwaysOn(value);
	}

	public void wakeup()
	{
		service.wakeup();
	}

	public long getNextWakeupTime()
	{
		return service.getNextWakeupTime();
	}

	public long getLastWakeupTime()
	{
		return service.getLastWakeupTime();
	}
	
	public void updateAlarmRegistration()
	{
		service.updateAlarmRegistration();
	}
}
