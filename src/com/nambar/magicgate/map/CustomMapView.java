package com.nambar.magicgate.map;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;

public class CustomMapView extends MapView {

	private static final int LONGPRESS_THRESHOLD = 400;

	public interface OnLongpressListener {
		public void onLongpress(MapView view, GeoPoint longpressLocation);
	}

	private GeoPoint lastMapCenter;


	private Timer longpressTimer = new Timer();
	private CustomMapView.OnLongpressListener longpressListener;

	public CustomMapView(Context context, String apiKey) {
		super(context, apiKey);
	}

	public CustomMapView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public CustomMapView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public void setOnLongpressListener(CustomMapView.OnLongpressListener listener) {
		longpressListener = listener;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		handleLongpress(event);
		return super.onTouchEvent(event);
	}

	private void handleLongpress(final MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			longpressTimer = new Timer();
			longpressTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					GeoPoint longpressLocation = getProjection().fromPixels((int)event.getX(), 
							(int)event.getY());
					longpressListener.onLongpress(CustomMapView.this, longpressLocation);
				}
			}, LONGPRESS_THRESHOLD);
			lastMapCenter = getMapCenter();
		}

		if (event.getAction() == MotionEvent.ACTION_MOVE) {
			if (!getMapCenter().equals(lastMapCenter)) {
				// User is panning the map, this is no longpress
				longpressTimer.cancel();
			}
			lastMapCenter = getMapCenter();
		}
		if (event.getAction() == MotionEvent.ACTION_UP) {
			// User has removed finger from map.
			longpressTimer.cancel();
		}
		if (event.getPointerCount() > 1)
		{
			longpressTimer.cancel();
		}
	}
}