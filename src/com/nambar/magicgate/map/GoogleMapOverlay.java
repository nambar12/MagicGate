package com.nambar.magicgate.map;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

public class GoogleMapOverlay extends ItemizedOverlay<OverlayItem> {

	OverlayItem item = null;
	Context context;
	
	public GoogleMapOverlay(Drawable defaultMarker, Context context)
	{
		super(boundCenter(defaultMarker));
		this.context = context;
	}

	@Override
	protected OverlayItem createItem(int i) {
		return item;
	}

	@Override
	public int size() {
		return item == null ? 0 : 1;
	}
	
	public void setOverlay(OverlayItem item)
	{
		this.item = item;
		populate();
	}

}
