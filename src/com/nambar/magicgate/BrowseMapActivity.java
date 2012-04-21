package com.nambar.magicgate;

import android.os.Bundle;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.nambar.magicgate.R;

public class BrowseMapActivity extends MapActivity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map_view);
		MapView mapView = (MapView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);
		mapView.setSatellite(false);
	}
	
	
	
	@Override
	protected boolean isRouteDisplayed()
	{
		return false;
	}

}
