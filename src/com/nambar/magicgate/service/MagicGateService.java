package com.nambar.magicgate.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.nambar.magicgate.MagicGateActivity;
import com.nambar.magicgate.R;
import com.nambar.magicgate.common.Gate;
import com.nambar.magicgate.common.GatesList;
import com.nambar.magicgate.common.MagicGateLocationListener;

public class MagicGateService extends Service implements LocationListener
{
	private final IBinder binder = new MagicGateServiceBinder(this);
	private static float FAR_RANGE = 10000F;
	AlarmManager alarmManager = null;
	LocationManager locationManager = null;
	private Location lastLocation = null;
	private static final String LOG_TAG = "MagicGateService";
	private static final long MIN_NETWORK_UPDATE_TIME_MILLI = 10 * 1000;
	private static final long IMMEDIATE_ALARM_MILLI = 500L;
	private static final int WAKEUP_REQUEST = 1;
	private static final long IN_CLOSE_RANGE_UPDATE_TIME_MILLI = 7 * 60 * 3600;
	private static final float MAX_REASONABLE_SPEED_METER_PER_MILLI = 150000F / 3600F / 1000F;
	private long fineLocationRegistrationTime = 0;
	private MagicGateLocationListener listener = null;
	private float lastShortestDistance = Float.MAX_VALUE;
	private static MagicGateService s_instance = null;
	private SampleMode sampleMode = SampleMode.ALARM;
	private long fineLocationTimeout = 0;
	private PendingIntent alarmPendingIntent = null;
	
	public MagicGateService()
	{
		Log.d(LOG_TAG, "MagicGateService() - [in]");
		s_instance = this;
	}
	
	private enum SampleMode {ALARM, CONTINUOUS; };
	
	@Override
	public IBinder onBind(Intent intent)
	{
		return binder;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		Log.d(LOG_TAG, "onStartCommand() - [in]");
		return START_STICKY;
	}
	
	@Override
	public void onCreate()
	{
		Log.d(LOG_TAG, "onCreate() - [in]");
		super.onCreate();
		init();
	}
	
	private void init()
	{
		Log.d(LOG_TAG, "init() - [in]");
		alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
		locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
		//requestNetworkUpdates();
		showAlwaysOnNotificationIfNeeded();
		GatesList.getInstnace().init(getFilesDir());
		
		Intent intent = new Intent(this, MagicGateAlarmReceiver.class);
		alarmPendingIntent = PendingIntent.getBroadcast(this, WAKEUP_REQUEST, intent, 0);
		
		registerForAlarm(IMMEDIATE_ALARM_MILLI);
	}

	private void showAlwaysOnNotificationIfNeeded()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if(prefs.getBoolean(getString(R.string.PREF_NOTIFICATION_ALWAYS_ON), false)) notificationAlwaysOn(true);
	}

	private void hideAlwaysOnNotification()
	{
		notificationAlwaysOn(false);
	}
	
	@Override
	public void onDestroy()
	{
		s_instance = null;
		Log.d(LOG_TAG, "onDestroy() - [in]");
		locationManager.removeUpdates(this);
		NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(R.string.ALWAYS_ON_NOTIFICATION);
	}
	

	private void setForeground(Gate gate)
	{
		hideAlwaysOnNotification();
		String message = "Getting close to " + gate.getName();
		Notification notification = new Notification(R.drawable.icon, message, System.currentTimeMillis());
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MagicGateActivity.class), 0);
		notification.setLatestEventInfo(this, this.getText(R.string.app_name), message, contentIntent);
		startForeground(R.string.FORGROUND_NOTIFICATION, notification);
	}
	
	private void unsetForeground()
	{
		stopForeground(true);
		showAlwaysOnNotificationIfNeeded();
	}

	@Override
	public void onLocationChanged(Location location)
	{
		lastLocation = location;
		if(listener != null)
		{
			listener.update(location);
		}

		switch(sampleMode)
		{
			case ALARM :      onLocationChangedAlarmMode(location); break;
			case CONTINUOUS : onLocationChangedContinuousMode(location); break;
		}
	}
	
	public void onLocationChangedAlarmMode(Location location)
	{
		try
		{
			if(location != null)
			{
				checkCourseLocation(location);
			}
		}
		catch(Exception e)
		{
			Log.e(LOG_TAG,"caught exception", e);
		}
		finally
		{
			if(sampleMode == SampleMode.ALARM)
			{
				locationManager.removeUpdates(this);
				registerForAlarm(getNextWakeupTime());
			}
		}
	}

	public void onLocationChangedContinuousMode(Location location)
	{
		if(location != null)
		{
			if(location.getProvider().equals(LocationManager.GPS_PROVIDER))
			{
				checkFineLocation(location);
			}
		}
	}

	public Location getLastLocation()
	{
		return lastLocation;
	}

	private void checkCourseLocation(Location location)
	{
		float shortestDistance = Float.MAX_VALUE;
		for(Gate gate : GatesList.getInstnace().getGates())
		{
			float distance = gate.distanceTo(location);
			if(distance < shortestDistance)
			{
				shortestDistance = distance;
			}
			if(distance < gate.getOpenDistance() + gate.getGPSActivationDistance())
			{
				if(gate.isLastLocationWasOutOfApproximateRange())
				{
					if(gate.meetsTimeConstraints())
					{
						Log.i(LOG_TAG, "Switching to fine location");
						gate.setLastLocationWasOutOfApproximateRange(false);
						registerForFineLocation(gate);
						return;
					}
					else
					{
						Log.i(LOG_TAG, "Gate " + gate.getName() + " does not within time constraints. Will not open");
					}
				}
			}
			else
			{
				gate.setLastLocationWasOutOfApproximateRange(true);
				gate.setWasOpened(false);
			}
		}
		if(shortestDistance < Float.MAX_VALUE)
		{
			lastShortestDistance  = shortestDistance;
		}
	}
	
	private void registerForFineLocation(Gate gate)
	{
		locationManager.removeUpdates(this);
		fineLocationRegistrationTime = System.currentTimeMillis();
		sampleMode = SampleMode.CONTINUOUS;
		fineLocationTimeout = gate.getGpsActivationTimeoutMilli();
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
		setForeground(gate);
	}

	private void unregisterForFineLocation()
	{
		fineLocationRegistrationTime = 0;
		locationManager.removeUpdates(this);
		registerForAlarm(getNextWakeupTime());
		sampleMode = SampleMode.ALARM;
		unsetForeground();
	}
	
	private long getNextWakeupTime()
	{
		if(GatesList.getInstnace().inApproxumateRangeOfAnyGate(lastLocation))
		{
			return IN_CLOSE_RANGE_UPDATE_TIME_MILLI;
		}
		if(lastShortestDistance < FAR_RANGE || lastShortestDistance == Float.MAX_VALUE)
		{
			return MIN_NETWORK_UPDATE_TIME_MILLI;
		}
		return new Float((lastShortestDistance / MAX_REASONABLE_SPEED_METER_PER_MILLI) / 2).longValue();
	}

	private void requestNetworkUpdates()
	{
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
	}
	private void checkFineLocation(Location location)
	{
		if(System.currentTimeMillis() > fineLocationRegistrationTime + fineLocationTimeout)
		{
			unregisterForFineLocation();
			return;
		}
		
		boolean keepFineLocation = false;
		for(Gate gate : GatesList.getInstnace().getGates())
		{
			float distance = gate.distanceTo(location);
			if(distance < gate.getOpenDistance() && !gate.wasOpened())
			{
				openGate(gate);
				unregisterForFineLocation();
				return;
			}
			else if(distance < gate.getOpenDistance() + gate.getGPSActivationDistance())
			{
				keepFineLocation = true;
			}
			
		}
		
		if(!keepFineLocation)
		{
			unregisterForFineLocation();
		}
	}

	public void openGate(Gate gate)
	{
		gate.setWasOpened(true);
		Intent intent = new Intent(Intent.ACTION_CALL);
		intent.setData(Uri.parse("tel:" + gate.getPhoneNumber()));
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
//		Intent intent = new Intent(this, DoOpenActivity.class);
//		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//		intent.setData(gate.getUri());
//		startActivity(intent);
	}

	@Override
	public void onProviderDisabled(String provider)
	{
	}

	@Override
	public void onProviderEnabled(String provider)
	{
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras)
	{
	}

	public void registerHandler(MagicGateLocationListener listener)
	{
		this.listener = listener;
	}
	
	public void unregisterHandler()
	{
		listener = null;
	}

	public void notificationAlwaysOn(boolean value)
	{
		NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		if(value)
		{
			String message = "Magic Gate is running";
			Notification notification = new Notification(R.drawable.icon, message, System.currentTimeMillis());
			notification.flags |= Notification.FLAG_ONGOING_EVENT;
			PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()), 0);
			notification.setLatestEventInfo(this, getText(R.string.app_name), message, contentIntent);
			notificationManager.notify(R.string.ALWAYS_ON_NOTIFICATION, notification);
		}
		else
		{
			notificationManager.cancel(R.string.ALWAYS_ON_NOTIFICATION);
		}
	}

	private void registerForAlarm(long delay)
	{
		alarmManager.cancel(alarmPendingIntent);
		long wakeupTime = System.currentTimeMillis() + delay;
		alarmManager.set(AlarmManager.RTC_WAKEUP, wakeupTime, alarmPendingIntent);
	}

	public void wakeup()
	{
		requestNetworkUpdates();
	}

	public static MagicGateService getInstance()
	{
		return s_instance;
	}
}
