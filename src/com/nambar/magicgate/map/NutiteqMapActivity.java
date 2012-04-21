package com.nambar.magicgate.map;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.ZoomControls;

import com.nambar.magicgate.R;
import com.nutiteq.BasicMapComponent;
import com.nutiteq.android.MapView;
import com.nutiteq.cache.AndroidFileSystemCache;
import com.nutiteq.cache.Cache;
import com.nutiteq.cache.CachingChain;
import com.nutiteq.cache.MemoryCache;
import com.nutiteq.components.MapPos;
import com.nutiteq.components.Place;
import com.nutiteq.components.PlaceIcon;
import com.nutiteq.components.WgsPoint;
import com.nutiteq.controls.AndroidKeysHandler;
import com.nutiteq.listeners.OnLongClickListener;
import com.nutiteq.location.LocationSource;
import com.nutiteq.location.NutiteqLocationMarker;
import com.nutiteq.location.providers.AndroidGPSProvider;
import com.nutiteq.maps.CloudMade;
import com.nutiteq.maps.GeoMap;
import com.nutiteq.maps.GoogleStaticMap;
import com.nutiteq.maps.JaredOpenStreetMap;
import com.nutiteq.maps.MapTileOverlay;
import com.nutiteq.maps.MicrosoftMap;
import com.nutiteq.maps.NutiteqStreamedMap;
import com.nutiteq.maps.OpenStreetMap;
import com.nutiteq.maps.SimpleWMSMap;
import com.nutiteq.ui.StringCopyright;
import com.nutiteq.ui.ThreadDrivenPanning;
import com.nutiteq.utils.Utils;

public class NutiteqMapActivity extends Activity
{
	public static final String NEW_GATE = "New gate";
	private static final int GPS_NOT_ENABLED_DIALOG = 0;
	private static final int CONFIRM_DIALOG_ID = 1;
	private static final int INITIAL_DIALOG_ID = 2;
	private MapView mapView;
	private BasicMapComponent mapComponent;
	private ZoomControls zoomControls;
	private boolean onRetainCalled;
	private Place gatePlace = null;
	private WgsPoint selectedPoint = null;
	private String gateName;
	private LocationSource locationSource;
	private NutiteqLocationMarker gpsMarker = new NutiteqLocationMarker(new PlaceIcon(Utils
	        .createImage("/res/drawable/gps_marker.png"), 5, 17), 3000, true);
	private CloudMade cmMap;


	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		String[] geos = getIntent().getData().getSchemeSpecificPart().split(",");
		selectedPoint = new WgsPoint(Double.valueOf(geos[1]), Double.valueOf(geos[0]));
		gateName = geos[2];
        
		initMap(selectedPoint);
		initListeners();
		initZoomControls();
		
		mapView = new MapView(this, mapComponent);
		onRetainCalled = false;
		
		final RelativeLayout relativeLayout = new RelativeLayout(this);
		setContentView(relativeLayout);
		final RelativeLayout.LayoutParams mapViewLayoutParams = new RelativeLayout.LayoutParams( RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.FILL_PARENT);
		relativeLayout.addView(mapView, mapViewLayoutParams);
		
		final RelativeLayout.LayoutParams zoomControlsLayoutParams = new RelativeLayout.LayoutParams( RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		zoomControlsLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		zoomControlsLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
		relativeLayout.addView(zoomControls, zoomControlsLayoutParams);
		
		if(!gateName.equals(NEW_GATE))
		{
			showGate();
		}
		else
		{
			showInitalMessage();
		}
	}

	private void initListeners()
	{
		mapComponent.setOnLongClickListener(new OnLongClickListener()
		{
	    	@Override
	    	public void onLongClick(int x, int y)
	    	{
			    MapPos placePoint = mapComponent.getInternalMiddlePoint().copy();
			    placePoint.setX(placePoint.getX()-mapComponent.getWidth()/2+x);
			    placePoint.setY(placePoint.getY()-mapComponent.getHeight()/2+y);
			    selectedPoint = mapComponent.getMap().mapPosToWgs(placePoint).toWgsPoint();
			    showGate();
	    	}
	    });
	}

	private void showGate()
	{
		if (gatePlace != null) mapComponent.removePlace(gatePlace);
	    gatePlace = new Place(0, new BalloonLabel("Gate", selectedPoint.toString()), new GatePlacemark(gateName), selectedPoint);
	    mapComponent.addPlace(gatePlace);
	}
	
	private void showInitalMessage()
	{
		showDialog(INITIAL_DIALOG_ID);
	}

	private void initZoomControls()
	{
		zoomControls = new ZoomControls(this);
		zoomControls.setOnZoomInClickListener(new View.OnClickListener()
		{
			public void onClick(final View v)
			{
				mapComponent.zoomIn();
			}
		});
		zoomControls.setOnZoomOutClickListener(new View.OnClickListener()
		{
			public void onClick(final View v)
			{
				mapComponent.zoomOut();
			}
		});
	}

	private void initMap(WgsPoint point) {
		mapComponent = new BasicMapComponent("7e7757b1e12abcb736ab9a754ffb617a4bd7f63dbd64d2.86913678", "Nutiteq", "CMRouteTestAndroid", 1, 1, point, 17);
		cmMap = new CloudMade("222c0ceb31794934a888ed9403a005d8", "test", 256, 1);
		mapComponent.setMap(cmMap);

		MemoryCache memoryCache = new MemoryCache(1024 * 1024);
		File cacheDir = new File("/sdcard/mapcache");
		if (!cacheDir.exists()) cacheDir.mkdir();
		AndroidFileSystemCache fileSystemCache = new AndroidFileSystemCache(this, "network_cache", cacheDir, 1024 * 1024 * 128);
		mapComponent.setNetworkCache(new CachingChain(new Cache[] { memoryCache, fileSystemCache }));

		mapComponent.setPanningStrategy(new ThreadDrivenPanning());
		mapComponent.setControlKeysHandler(new AndroidKeysHandler());
		mapComponent.setSmoothZoom(true);
		mapComponent.startMapping();
	}
	
	@Override public Object onRetainNonConfigurationInstance()
	{
		onRetainCalled = true;
		return mapComponent;
	}
	
	@Override
	protected void onDestroy()
	{
		showDialog(CONFIRM_DIALOG_ID);
		super.onDestroy();
		if (mapView != null)
		{
			mapView.clean();
			mapView = null;
		}
		if (!onRetainCalled)
		{
			mapComponent.stopMapping();
			mapComponent = null;
		}
	}
	
	private void updateLocation()
	{
		Intent intent = new Intent("", Uri.parse("geo:" + selectedPoint.getLat() + "," + selectedPoint.getLon()));
		setResult(RESULT_OK, intent);
		//finish();
	}
	
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
    	if(keyCode == KeyEvent.KEYCODE_BACK)
    	{
    		updateLocation();
    		finish();
    		return true;
    	}
    	else
    	{
    		return super.onKeyDown(keyCode, event);
    	}
    }

    @Override
	protected Dialog onCreateDialog(int id)
	{
		switch(id)
		{
			case CONFIRM_DIALOG_ID:
		         return new AlertDialog.Builder(this)
	                .setTitle("Save location?")
	                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int whichButton) {
	                    	updateLocation();
	                    }
	                })
	                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int whichButton) {
	                    }
	                })
	                .create();
			case INITIAL_DIALOG_ID:
		         return new AlertDialog.Builder(this)
	                .setTitle("Magic Gate").setMessage("Nagivate to the gate's location and long click on the map to place the gate")
	                .setPositiveButton(android.R.string.ok, null)
	                .create();
		}
		return super.onCreateDialog(id);
	}
    
    @Override
	public boolean onCreateOptionsMenu(final Menu menu)
	{
    	getMenuInflater().inflate(R.menu.map_menu, menu);
    	return true;
	}

    @Override
    public boolean onMenuItemSelected(final int featureId, final MenuItem item)
    {

		  // remember and restore tile overlay in case if map is changed
	      MapTileOverlay ovl = mapComponent.getMap().getTileOverlay();

		  switch (item.getItemId()) {

		    	

		    // GPS location    
		    case R.id.menu_item_location:
		      LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );
		      if ( manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {

		    	 if (locationSource == null) {
		    		 // if you o not specify provider, then it tries to select automatically best and fastest Network+GPS positioning
		          locationSource = new AndroidGPSProvider(
		              (LocationManager) getSystemService(Context.LOCATION_SERVICE),
		              10000L);
		          locationSource.setLocationMarker(gpsMarker);
		          mapComponent.setLocationSource(locationSource);
		        }
		        gpsMarker.setTrackingEnabled(true);

		      }
		      else {
		        showDialog(GPS_NOT_ENABLED_DIALOG);
		      }
		      break;

		    // map types
		    case R.id.menu_openstreetmap:
		    	mapComponent.setMap(OpenStreetMap.MAPNIK);
		    	item.setChecked(!item.isChecked());
		        break;
		        
		    case R.id.menu_cloudmade:
		    	mapComponent.setMap(cmMap);
		    	item.setChecked(!item.isChecked());
		    	break;

		    case R.id.menu_nutiteq:
		    	mapComponent.setMap(new NutiteqStreamedMap("http://aws.nutiteq.ee/mapstream.php?ts=128&", "© OpenStreetMap - Nutiteq", 128, 0, 18));
		    	item.setChecked(!item.isChecked());
		        break;

		    case R.id.menu_jared:
		    	mapComponent.setMap( new JaredOpenStreetMap(new StringCopyright("© OpenStreetMap - Bundled"), "/res/raw/",
		    	        256, 0, 4));
		    	item.setChecked(!item.isChecked());
		        break;
		        
		    case R.id.menu_hybrid:
		        final GeoMap jaredMap = new JaredOpenStreetMap(new StringCopyright("© OpenStreetMap"), "/res/raw/",
		    	        256, 0, 18); // set max zoom to total max, not jared max, otherwise you can't zoom in
		        final GeoMap openStreetMap = OpenStreetMap.MAPNIK;
		        final GeoMap[] searched = new GeoMap[] { jaredMap, openStreetMap };

		        mapComponent.setTileSearchStrategy(searched);
		        mapComponent.setZoom(4);
		        
		    	item.setChecked(!item.isChecked());
		        break;

		    case R.id.menu_yahoo:
		    	mapComponent.setMap(new YahooMap());
		    	item.setChecked(!item.isChecked());
		        break;

		    case R.id.menu_google_static:
		    	mapComponent.setMap(new GoogleStaticMap("dummy", 256, 0, 19, GoogleStaticMap.IMAGE_FORMAT_PNG_32, GoogleStaticMap.MAP_TYPE_TERRAIN, true));
		    	item.setChecked(!item.isChecked());
		        break;
		        
		    case R.id.menu_microsoft:
		    	mapComponent.setMap(MicrosoftMap.LIVE_MAP);
		    	item.setChecked(!item.isChecked());
		        break;

		    case R.id.menu_wms:
		    	SimpleWMSMap wms = new SimpleWMSMap(
		    			   "http://vmap0.tiles.osgeo.org/wms/vmap0?",
		    			   256, 0, 18,"basic", "image/png",
		    			   "default", "GetMap", "WMS VMAP0");
		    			wms.setWidthHeightRatio(1.0); // use "real" EPSG:4326 Lat-Long, not square one
		    			mapComponent.setMap(wms);
		    	item.setChecked(!item.isChecked());
		        break;
		        
		    case R.id.map_menu_save_and_close:
			    updateLocation();
			    finish();
		    	break;
		  
		    case R.id.map_menu_cancel:
			    setResult(RESULT_CANCELED);
			    finish();
		    	break;
		  
		  }
		  // restore tile overlay
	      mapComponent.getMap().addTileOverlay(ovl);

		 return true;
		    
	  }
	  
	
}
