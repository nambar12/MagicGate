package com.nambar.magicgate;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.StringTokenizer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import com.nambar.magicgate.common.Gate;
import com.nambar.magicgate.common.GatesList;
import com.nambar.magicgate.map.GoogleMapActivity;
import com.nambar.magicgate.service.MagicGateService;
import com.nambar.magicgate.service.MagicGateServiceBinder;

public class GatePropertiesActivity extends PreferenceActivity implements LocationListener
{
	private static final String PREF_PREFIX = "gate.";
	private static final String EMPTY_PHONE_MESSAGE = "Click to enter phone number";
	private static final String EMPTY_LOCATION_MESSAGE = "Click to enter location";
	private static final int PICK_CONTACT_REQUEST = 1;
	private static final int PLACE_IN_MAP_REQUEST = 2;
	private static final int PICK_SCHEDULE_REQUEST = 3;

	private static final int NO_LOCATION_DIALOG = 1;
	private static final int DISCARD_CHANGES_DIALOG = 2;
	private static final int FIND_LOCATION_DIALOG = 3;
	private static final int GPS_DISABLED_DIALOG = 4;
	private static final int GPS_ACTIVATION_TOO_SHORT_DIALOG = 5;
	private static final String SCHEDULE_ALWAYS = "Always on";
	private static final float GPS_ACTIVATION_REASONABLE_DISTANCE = 2000F;
	private static Gate gate = null;

	private MagicGateServiceBinder binder;
	private Preference prefGateName = null;
	private Preference prefPhoneNumber = null;
	private Preference prefOpenDistance = null;
	private Preference prefGPSActivationDistance = null;
	private Preference prefLocation = null;
	private Preference prefSchedule = null;
	private ProgressDialog findLocationDialog = null;
	private LocationManager locationManager = null;
	private float lastSelectGPSActivationDistance = 0F;
	private SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm aaa", Locale.US);
	
	boolean modified = false;

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
		addPreferencesFromResource(R.xml.edit_gate);
		bindService(new Intent(this, MagicGateService.class), serviceConnection, BIND_AUTO_CREATE);
        init();
	}
	
	private OnPreferenceChangeListener prefChangeListener = new OnPreferenceChangeListener()
    {
		@Override
		public boolean onPreferenceChange(Preference p, Object value)
		{
			p.setSummary(value.toString());
			modified = true;
			return true;
		}
	};

	private OnPreferenceChangeListener gpsActivationPrefChangeListener = new OnPreferenceChangeListener()
    {
		@Override
		public boolean onPreferenceChange(Preference p, Object value)
		{
			lastSelectGPSActivationDistance = Float.parseFloat(value.toString().split(" ")[0]);
			if(lastSelectGPSActivationDistance < GPS_ACTIVATION_REASONABLE_DISTANCE)
			{
				showDialog(GPS_ACTIVATION_TOO_SHORT_DIALOG);
				return false;
			}
			else
			{
				p.setSummary(value.toString());
				modified = true;
				return true;
			}
		}
	};
	
	
	private OnPreferenceChangeListener locationPrefChangeListener = new OnPreferenceChangeListener()
    {
		@Override
		public boolean onPreferenceChange(Preference p, Object value)
		{
			if(value.equals("0"))
			{
				findMyLocation();
			}
			else
			{
				showMap();
			}
			return true;
		}
	};
	
	private void resetPref(Preference pref)
	{
		pref.getEditor().remove(pref.getKey());
		pref.getEditor().commit();
	}

	private void init()
	{
		locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
		
		getPreferenceManager().setSharedPreferencesName(PREF_PREFIX + gate.getId());
		getPreferenceManager().setSharedPreferencesMode(MODE_PRIVATE);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = prefs.edit();
		
        prefGateName = findPreference(getString(R.string.GATE_PREF_NAME));
        prefGateName.setOnPreferenceChangeListener(prefChangeListener);
        prefGateName.setSummary(gate.getName());
        editor.putString(getString(R.string.GATE_PREF_NAME), gate.getName());

        prefPhoneNumber = findPreference(getString(R.string.GATE_PREF_PHONE));
        prefPhoneNumber.setOnPreferenceChangeListener(prefChangeListener);
        prefPhoneNumber.setSummary(getPhoneNumberSummary());
        editor.putString(getString(R.string.GATE_PREF_PHONE), getPhoneNumberSummary());
        prefPhoneNumber.setOnPreferenceClickListener(new OnPreferenceClickListener()
        {
			@Override public boolean onPreferenceClick(Preference preference) { selectPhoneNumber(); return true; }
		});
        
        prefLocation = findPreference(getString(R.string.GATE_PREF_LOCATION));
        prefLocation.setOnPreferenceChangeListener(locationPrefChangeListener);
        String formattedLocation = formattedLocation(gate.getLatitude(), gate.getLongitude());
        prefLocation.setSummary(formattedLocation);
        editor.putString(getString(R.string.GATE_PREF_LOCATION), formattedLocation);
        //editor.remove(getString(R.string.GATE_PREF_LOCATION));
        

        prefOpenDistance = findPreference(getString(R.string.GATE_PREF_OPEN_DISTANCE));
        prefOpenDistance.setOnPreferenceChangeListener(prefChangeListener);
        String openDistanceStr = formatDistanceInMeters(gate.getOpenDistance());
        prefOpenDistance.setSummary(openDistanceStr);
        editor.putString(getString(R.string.GATE_PREF_OPEN_DISTANCE), openDistanceStr);
        prefOpenDistance.getEditor().putString(getString(R.string.GATE_PREF_OPEN_DISTANCE), openDistanceStr);
        prefOpenDistance.getEditor().commit();

        prefGPSActivationDistance = findPreference(getString(R.string.GATE_PREF_GPS_ACTIVATION_DISTANCE));
        prefGPSActivationDistance.setOnPreferenceChangeListener(gpsActivationPrefChangeListener);
        String gpsActivationDistanceStr = formatDistanceInMeters(gate.getGPSActivationDistance());
        prefGPSActivationDistance.setSummary(gpsActivationDistanceStr);
        editor.putString(getString(R.string.GATE_PREF_GPS_ACTIVATION_DISTANCE), gpsActivationDistanceStr);

        prefSchedule = findPreference(getString(R.string.GATE_PREF_SCHEDULE));
        String sSchedule = formatSchedule();
        prefSchedule.setSummary(sSchedule);
        editor.putString(getString(R.string.GATE_PREF_GPS_ACTIVATION_DISTANCE), sSchedule);
        prefSchedule.setOnPreferenceClickListener(new OnPreferenceClickListener()
        {
			@Override public boolean onPreferenceClick(Preference preference) { selectSchedule(); return true; }
		});


		editor.commit();
	}
	
	private String formatDistanceInMeters(float distance)
	{
		return String.valueOf(Float.valueOf(distance).intValue() + " meters");
	}

	private void selectSchedule()
    {
		Intent intent = new Intent(GatePropertiesActivity.this, ScheduleActivity.class);
		startActivityForResult(intent, PICK_SCHEDULE_REQUEST);
	}


	private String formatSchedule()
	{
		if(!gate.hasTimeLimit())
		{
			return SCHEDULE_ALWAYS;
		}
		return "" + formatTime(gate.getFromHour(),gate.getFromMinute()) + " - "
		          + formatTime(gate.getUntilHour(),gate.getUntilMinute());
		      
	}
	
	private String formatTime(int hour, int minute)
	{
		Date date = new Date();
		date.setHours(hour);
		date.setMinutes(minute);
		return timeFormat.format(date);
	}

	private String getPhoneNumberSummary()
	{
		return gate.getPhoneNumber() != null ? gate.getPhoneNumber() : EMPTY_PHONE_MESSAGE;
	}

	private void selectPhoneNumber()
	{
		 Intent intent = new Intent(Intent.ACTION_PICK);
		 intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
		 startActivityForResult(intent, PICK_CONTACT_REQUEST);
		
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = prefs.edit();
		switch(requestCode)
		{
			case PICK_CONTACT_REQUEST :
				if (resultCode == Activity.RESULT_OK)
				{
					Uri contactData = data.getData();
					Cursor c = managedQuery(contactData, null, null, null, null);
					if (c.moveToFirst())
					{
						String phoneNumber = c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
				        prefPhoneNumber.setSummary(phoneNumber);
				        editor.putString(getString(R.string.GATE_PREF_PHONE), phoneNumber);
						modified = true;
					}
				}
				break;

			case PLACE_IN_MAP_REQUEST :
				if (resultCode == Activity.RESULT_OK)
				{
					String[] geos = data.getData().getSchemeSpecificPart().split(",");
					String formattedLocation = formattedLocation(Double.valueOf(geos[0]), Double.valueOf(geos[1]));
					prefLocation.setSummary(formattedLocation);
					editor.putString(getString(R.string.GATE_PREF_LOCATION), formattedLocation);
					modified = true;
				}
				break;

			case PICK_SCHEDULE_REQUEST :
				if (resultCode == Activity.RESULT_OK)
				{
					GatesList.getInstnace().save();
					prefSchedule.setSummary(formatSchedule());
					modified = true;
				}
				break;
		}
		editor.commit();
	}

	private String formattedLocation(double latitude, double longitude)
	{
		return latitude > 0 ?  "Latitude:   " + latitude + "\nLongitude: " + longitude : EMPTY_LOCATION_MESSAGE;
	}

	private void showMap()
	{
		Uri uri = null;
		String sLocation = prefLocation.getSummary().toString();
		if(!sLocation.equals(EMPTY_LOCATION_MESSAGE))
		{
			uri = Uri.parse("geo:" + parseLatitude(sLocation) + "," + parseLongitude(sLocation) + "," + gate.getName());
		}
		else
		{
			uri = getNewGateUri();
		}
		if(uri != null)
		{
			Intent intent = new Intent(this, GoogleMapActivity.class);
			intent.setData(uri);
			startActivityForResult(intent, PLACE_IN_MAP_REQUEST);
		}
	}

	private Uri getNewGateUri()
	{
		Location location = binder.getLastLocation();
		if(location == null)
		{
			showDialog(NO_LOCATION_DIALOG);
			return null;
		}
		return Uri.parse("geo:" + location.getLatitude() + "," + location.getLongitude());
	}
	
	private void findMyLocation()
	{
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
		showDialog(FIND_LOCATION_DIALOG);
	}

    @Override
	protected Dialog onCreateDialog(int id)
	{
		switch(id)
		{
			case NO_LOCATION_DIALOG:
	            return new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage("Still didn't figure out your currnet location, please try again later")
	            .setPositiveButton(android.R.string.ok, null)
	            .create();

			case DISCARD_CHANGES_DIALOG:
	            return new AlertDialog.Builder(this)
                .setTitle("Discard changes?")
                .setMessage("Configuration is incomplete. Phone number/Location is missing. Discard changes?")
                .setNegativeButton("Discard", new OnClickListener()
	            {
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						setResult(RESULT_CANCELED);
						finish();
					}
				})
	            .setPositiveButton("Cancel", null)
	            .create();

			case FIND_LOCATION_DIALOG:
				findLocationDialog = new ProgressDialog(this);
	            findLocationDialog.setTitle("Checking location...");
	            findLocationDialog.setMessage("Waiting for GPS location. Please make sure that you are in an open place");
	            findLocationDialog.setIndeterminate(true);
	            findLocationDialog.setCancelable(true);
	            findLocationDialog.setOnCancelListener(new OnCancelListener()
	            {
					@Override public void onCancel(DialogInterface dialog)
					{
						removeLocationUpdates();
						resetPref(prefLocation);
					}
				});
	            return findLocationDialog;

			case GPS_DISABLED_DIALOG:
	            return new AlertDialog.Builder(this)
                .setTitle("Your GPS is Disabled")
                .setMessage("GPS receiver needs to be enabled for Magic Gate to work properly")
	            .setPositiveButton(android.R.string.ok, null)
	            .create();

			case GPS_ACTIVATION_TOO_SHORT_DIALOG:
	            return new AlertDialog.Builder(this)
                .setTitle("Shot distance notice")
                .setMessage("Cellular network accuracy is limited and distance lower than 2KM can be problematic. Keep changes?")
                .setNegativeButton("No", null)
	            .setPositiveButton("Yes", new OnClickListener()
	            {
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						prefGPSActivationDistance.setSummary(formatDistanceInMeters(lastSelectGPSActivationDistance));
						modified = true;
						setResult(RESULT_OK);
					}
	            })
	            .create();

		}
		return super.onCreateDialog(id);
	}
    
    private void saveChanges()
    {
    	gate.setName(prefGateName.getSummary().toString());
    	gate.setPhoneNumber(prefPhoneNumber.getSummary().toString());
    	gate.setLatitude(parseLatitude(prefLocation.getSummary().toString()));
    	gate.setLongitude(parseLongitude(prefLocation.getSummary().toString()));
    	gate.setOpenDistance(parseDistance(prefOpenDistance.getSummary().toString()));
    	gate.setGPSActivationDistance(parseDistance(prefGPSActivationDistance.getSummary().toString()));
    }

    private float parseDistance(String s)
    {
    	String[] tokens = s.split(" ");
    	return tokens.length == 2 ? Float.valueOf(tokens[0]) : 0F;
    }
    
	private double parseLatitude(String s)
	{
		return parseLocationToken(s, true);
	}

	private double parseLongitude(String s)
	{
		return parseLocationToken(s, false);
	}

	private double parseLocationToken(String s, boolean isLangitude)
	{
		String latitude;
		String longitude;
		StringTokenizer st = new StringTokenizer(s, " \t\n");
		if(st.countTokens() != 4 || s.equals(EMPTY_LOCATION_MESSAGE))
		{
			return 0D;
		}
		st.nextToken();
		latitude = st.nextToken();
		st.nextToken();
		longitude = st.nextToken();
		return Double.valueOf(isLangitude ? latitude : longitude);
	}

	@Override
	public void onLocationChanged(Location location)
	{
		if(location != null)
		{
			removeLocationUpdates();
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			SharedPreferences.Editor editor = prefs.edit();
	        String formattedLocation = formattedLocation(location.getLatitude(), location.getLongitude());
	        prefLocation.setSummary(formattedLocation);
	        editor.putString(getString(R.string.GATE_PREF_LOCATION), formattedLocation);
	        editor.commit();
			modified = true;
			findLocationDialog.dismiss();
		}
	}

	private void removeLocationUpdates()
	{
		locationManager.removeUpdates(this);
	}

	@Override
	public void onProviderDisabled(String provider)
	{
		removeLocationUpdates();
		dismissDialog(FIND_LOCATION_DIALOG);
		showDialog(GPS_DISABLED_DIALOG);
	}

	@Override
	public void onProviderEnabled(String provider)
	{
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras)
	{
	}

	@Override
	protected void onDestroy()
	{
		removeLocationUpdates();
		unbindService(serviceConnection);
		super.onDestroy();
	}

	public static Gate getGate()
	{
		return gate;
	}

	public static void setGate(Gate gate)
	{
		GatePropertiesActivity.gate = gate;
	}

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
    	if(keyCode == KeyEvent.KEYCODE_BACK)
    	{
    		return close(true);
    	}
    	else
    	{
    		return super.onKeyDown(keyCode, event);
    	}
    }

	private boolean close(boolean save)
	{
		if(modified)
		{
			if(save) saveChanges();
			setResult(RESULT_OK);
		}
		if(isIncompleteConfiguration())
		{
			showDialog(DISCARD_CHANGES_DIALOG);
		}
		else
		{
			finish();
		}
		return true;
	}

    @Override
	public boolean onCreateOptionsMenu(final Menu menu)
	{
    	getMenuInflater().inflate(R.menu.gate_properties_menu, menu);
    	return true;
	}
    
    @Override
    public boolean onMenuItemSelected(final int featureId, final MenuItem item)
    {
		switch (item.getItemId())
		{
		  case R.id.gate_properties_save_and_close:
			  close(true);
			  break;
		
		  case R.id.gate_properties_menu_cancel:
			  close(false);
			  break;
		
		 }
		 return super.onMenuItemSelected(featureId, item);
    }

    private boolean isIncompleteConfiguration()
	{
		return gate.getPhoneNumber() == null || gate.getPhoneNumber().equals(EMPTY_PHONE_MESSAGE) || gate.getLatitude() == 0D;
	}
}
