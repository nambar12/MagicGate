package com.nambar.magicgate.map;

import java.util.List;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.nambar.magicgate.R;
import com.nambar.magicgate.map.CustomMapView.OnLongpressListener;

public class GoogleMapActivity extends MapActivity 
{
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.google_map);
		initMapView();
	    
		String[] geos = getIntent().getData().getSchemeSpecificPart().split(",");
		double lat = Double.valueOf(geos[0]) * 1E6;
		double lng = Double.valueOf(geos[1]) * 1E6;
	    GeoPoint point = new GeoPoint(Double.valueOf(lat).intValue(), Double.valueOf(lng).intValue());

	    setGateLocation(point);
	    
	    Toast.makeText(this, R.string.MAP_INITAL_MESSAGE, Toast.LENGTH_LONG).show();
	}

	private CustomMapView initMapView() {
	    CustomMapView mapView = (CustomMapView) findViewById(R.id.mapview);
	    mapView.setBuiltInZoomControls(true);
	    mapView.setOnLongpressListener(new OnLongpressListener() {
			
			@Override
			public void onLongpress(MapView view, GeoPoint longpressLocation) {
			    setGateLocation(longpressLocation);
			    double lat = longpressLocation.getLatitudeE6() / 1E6;
			    double lng = longpressLocation.getLongitudeE6() / 1E6;
				Intent intent = new Intent("", Uri.parse("geo:" + lat + "," + lng));
				setResult(RESULT_OK, intent);
			}
		});
		return mapView;
	}
	
	private void setGateLocation(GeoPoint point) {
		CustomMapView mapView = (CustomMapView) findViewById(R.id.mapview);
	    List<Overlay> mapOverlays = mapView.getOverlays();
	    mapOverlays.clear();
	    Drawable drawable = this.getResources().getDrawable(R.drawable.gate_black);
	    GoogleMapOverlay itemizedoverlay = new GoogleMapOverlay(drawable, this);
	    OverlayItem overlayitem = new OverlayItem(point, "Gate", "New Gate");
	    itemizedoverlay.setOverlay(overlayitem);
	    mapOverlays.add(itemizedoverlay);
	}

}
