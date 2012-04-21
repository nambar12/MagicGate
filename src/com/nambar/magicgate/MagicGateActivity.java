package com.nambar.magicgate;

import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.nambar.magicgate.common.Gate;
import com.nambar.magicgate.common.GatesList;
import com.nambar.magicgate.common.MagicGateLocationListener;
import com.nambar.magicgate.service.MagicGateService;
import com.nambar.magicgate.service.MagicGateServiceBinder;

public class MagicGateActivity extends Activity
{
	private static final int ADD_GATE_REQUEST = 1;
	private static final int CHANGE_GATE_REQUEST = 2;
	private static final int GATE_POPUP_MENU_DIALOG = 1;
	private static final int CONFIRM_STOP_SERVICE_DIALOG = 2;
	private static final int GPS_DISABLED_DIALOG = 3;
	
	private ListView gates = null;
	private TextView emptyMessageTextView = null;
	private MagicGateServiceBinder binder;
	private Gate lastSelectedGate = null;
	private Handler locationHandler = null;
	private MagicGateLocationListener locationListener = null;
	
	private ServiceConnection serviceConnection = new ServiceConnection()
	{
		@Override
		public void onServiceConnected(ComponentName name, IBinder service)
		{
			binder = (MagicGateServiceBinder)service;
	        fillGatesList(binder.getLastLocation());
			binder.registerHandler(locationListener);
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
        setContentView(R.layout.main);
        init();
		testGPSEnabled();
    }

	private void init()
	{
		locationHandler = new Handler();
		locationListener = new MagicGateLocationListener()
		{
			@Override
			public void update(final Location location)
			{
				locationHandler.post(new Runnable()
				{
					@Override
					public void run()
					{
						fillGatesList(location);
					}
				});
			}
		};
		
		final Intent serviceIntent = new Intent(this, MagicGateService.class);
        startService(serviceIntent);
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
        
        GatesList.getInstnace().init(getFilesDir());
        gates = (ListView)findViewById(R.id.ListViewGates);
        gates.setOnItemClickListener(new OnItemClickListener()
        {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				@SuppressWarnings("unchecked")
				Map<String,String> map = (Map<String,String>)parent.getItemAtPosition(position);
				lastSelectedGate = GatesList.getInstnace().lookup(map.get("id"));
				editGate();
			}
		});
        gates.setOnItemLongClickListener(new OnItemLongClickListener()
        {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
			{
				@SuppressWarnings("unchecked")
				Map<String,String> map = (Map<String,String>)parent.getItemAtPosition(position);
				lastSelectedGate = GatesList.getInstnace().lookup(map.get("id"));
				showDialog(GATE_POPUP_MENU_DIALOG);
				return true;
			}
		});
        
        emptyMessageTextView = (TextView)findViewById(R.id.GatesEmptyMessage);
	}
	

	@Override
	protected Dialog onCreateDialog(int id)
	{
		switch(id)
		{
			case GATE_POPUP_MENU_DIALOG:
	            return new AlertDialog.Builder(this)
                .setTitle(lastSelectedGate.getName())
                .setItems(new String[] {"Delete", "Edit", "Open now!"}, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int position) {
        				switch(position)
        				{
        					case 0 : deleteGate(); fillGatesList(binder.getLastLocation()); break;
        					case 1 : editGate(); break;
        					case 2 : binder.openGate(lastSelectedGate); break;
        				}
                    }
                })
                .create();

			case CONFIRM_STOP_SERVICE_DIALOG:
	            return new AlertDialog.Builder(this)
                .setTitle("Stop running?")
                .setMessage("Gates won't be opened until the next time that you'll open the application. Really quit?")
                .setNegativeButton(android.R.string.cancel, null)
	            .setPositiveButton(android.R.string.ok, new OnClickListener()
	            {
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						stopRunning();
						
					}
				})
	            .create();

			case GPS_DISABLED_DIALOG:
	            return new AlertDialog.Builder(this)
                .setTitle("GPS/Network is disabled")
                .setMessage("Your GPS and/or Wireless Network locations are currently disabled. Magic Gate requires that both GPS and Wirless Network locations to be enabled in order to open gates. Your GPS will be activated only when getting close to the gate. Open settings now?")
                .setNegativeButton("No", null)
	            .setPositiveButton("Yes", new OnClickListener()
	            {
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
						startActivityForResult(intent, 0); 
						
					}
				})
	            .create();

		}
		return super.onCreateDialog(id);
	}
	
	private void fillGatesList(Location location)
	{
		if(GatesList.getInstnace().hasGates())
		{
			gates.setVisibility(View.VISIBLE);
			emptyMessageTextView.setVisibility(View.GONE);
			gates.setAdapter(new SimpleAdapter(this, GatesList.getInstnace().getGateEntries(location),
				                               android.R.layout.simple_list_item_2, 
					                           new String[] { "name", "distance" }, 
					                           new int[] { android.R.id.text1, android.R.id.text2 }));
		}
		else
		{
			gates.setVisibility(View.GONE);
			emptyMessageTextView.setVisibility(View.VISIBLE);
		}
		
	}

	private void addGate()
	{
		GatePropertiesActivity.setGate(new Gate());
		startActivityForResult(new Intent(this, GatePropertiesActivity.class), ADD_GATE_REQUEST);
	}
	
	private void deleteGate()
	{
		GatesList.getInstnace().remove(lastSelectedGate);
	}

	private void editGate()
	{
		GatePropertiesActivity.setGate(lastSelectedGate);
		Intent intent = new Intent(this, GatePropertiesActivity.class);
		startActivityForResult(intent, CHANGE_GATE_REQUEST);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if(binder == null || resultCode != RESULT_OK)
		{
			return;
		}
		if(requestCode == ADD_GATE_REQUEST)
		{
			GatesList.getInstnace().add(GatePropertiesActivity.getGate());
			fillGatesList(binder.getLastLocation());
		}
		else if(requestCode == CHANGE_GATE_REQUEST)
		{
			GatesList.getInstnace().save();
			fillGatesList(binder.getLastLocation());
		}
	}

	@Override
	protected void onDestroy()
	{
		if(binder != null)
		{
			binder.unregisterHandler();
		}
		unbindService(serviceConnection);
		super.onDestroy();
	}
	
    @Override
	public boolean onCreateOptionsMenu(final Menu menu)
	{
    	getMenuInflater().inflate(R.menu.main_menu, menu);
    	return true;
	}
    
    @Override
    public boolean onMenuItemSelected(final int featureId, final MenuItem item)
    {
		switch (item.getItemId())
		{
		  case R.id.main_menu_new_gate:
			  addGate();
			  break;
		
		  case R.id.main_menu_stop_running:
			  showDialog(CONFIRM_STOP_SERVICE_DIALOG);
			  break;
		
		  case R.id.main_menu_exit:
			  finish();
			  break;
		    	
		  case R.id.main_menu_settings:
			  startActivity(new Intent(getBaseContext(), MagicGateSettingsActivity.class));
			  break;
		 }
		 return super.onMenuItemSelected(featureId, item);
    }
    

	private void stopRunning()
	{
        stopService(new Intent(this, MagicGateService.class));
		finish();
	}
    
	private void testGPSEnabled()
	{
	    String provider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
	    if(provider == null || !provider.contains(LocationManager.GPS_PROVIDER) || !provider.contains(LocationManager.NETWORK_PROVIDER))
	    {         
	    	showDialog(GPS_DISABLED_DIALOG);
	    }
		
	}

}