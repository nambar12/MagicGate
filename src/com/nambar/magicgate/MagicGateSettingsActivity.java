package com.nambar.magicgate;

import com.nambar.magicgate.service.MagicGateService;
import com.nambar.magicgate.service.MagicGateServiceBinder;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;

public class MagicGateSettingsActivity extends PreferenceActivity
{
	private MagicGateServiceBinder binder;

	private ServiceConnection serviceConnection = new ServiceConnection()
	{
		@Override
		public void onServiceConnected(ComponentName name, IBinder service)
		{
			binder = (MagicGateServiceBinder)service;
		}
		@Override
		public void onServiceDisconnected(ComponentName name)
		{
			binder = null;
		}
	};

	@Override
    public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		final Intent serviceIntent = new Intent(this, MagicGateService.class);
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);

		addPreferencesFromResource(R.xml.settings);
		
		Preference showIconPref = findPreference(getString(R.string.PREF_NOTIFICATION_ALWAYS_ON));
		showIconPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
		{
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue)
			{
				if(binder == null) return false;
				binder.notificationAlwaysOn(((Boolean)newValue).booleanValue());
				return true;
			}
		});

		Preference sampleRatePref = findPreference(getString(R.string.PREF_SAMPLE_RATE));
		sampleRatePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
		{
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue)
			{
				if(binder == null) return false;
				binder.updateAlarmRegistration();
				return true;
			}
		});
	}
}
