package com.nambar.magicgate.map;

import com.nutiteq.maps.GeoMap;
import com.nutiteq.maps.UnstreamedMap;
import com.nutiteq.maps.projections.EPSG3785;
import com.nutiteq.ui.StringCopyright;

public class YahooMap extends EPSG3785 implements GeoMap, UnstreamedMap {
  public YahooMap() {
    super(new StringCopyright("Yahoo!"), 256, 1, 18);
  }

  public String buildPath(final int mapX, final int mapY, final int zoom) {
    final StringBuffer url = new StringBuffer("http://png.maps.yimg.com/png?t=m&s=256&f=j&v=4.1");
    url.append("&x=");
    url.append(mapX / getTileSize());
    url.append("&y=");
    url.append(((1 << zoom) >> 1) - 1 - mapY / getTileSize());
    url.append("&z=");
    url.append(18 - zoom);
    return url.toString();
  }
}
