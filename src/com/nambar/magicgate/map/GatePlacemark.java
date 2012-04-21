package com.nambar.magicgate.map;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import com.nutiteq.components.Placemark;

public class GatePlacemark implements Placemark
{
  private Image gate;
  private final Font nameFont;
  final int nameWidth;
  private final String name;
  private final int boxHeight;

  public GatePlacemark(final String name)
  {
    this.name = name;
    try {
      gate = Image.createImage("/res/drawable-ldpi/gate_black.png");
    } catch (final Exception ignore) {
    }
    nameFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
    nameWidth = nameFont.stringWidth(name);
    boxHeight = Math.max(gate.getHeight(), nameFont.getHeight()) + 4;
  }

  public int getAnchorX(final int zoom) {
    //anchor is on image center
    return gate.getWidth() / 2 + (zoom < 10 ? 0 : 2);
  }

  public int getAnchorY(final int zoom) {
    return gate.getHeight() / 2 + (zoom < 10 ? 0 : 2);
  }

  public int getHeight(final int zoom) {
    return zoom < 10 ? gate.getHeight() : boxHeight;
  }

  public int getWidth(final int zoom) {
    return zoom < 10 ? gate.getWidth() : nameWidth + 4 + gate.getWidth();
  }

  public void paint(final Graphics g, final int screenX, final int screenY, final int zoom) {
	  
	  // here is different placemark depending on zoom
    if (zoom < 10) {
      g.drawImage(gate, screenX, screenY, Graphics.TOP | Graphics.LEFT);
    } else {
      g.setColor(0xFF4A4A4A);
      g.fillRoundRect(screenX, screenY, nameWidth + 4 + gate.getWidth(), boxHeight, 10, 10);
      g.setColor(0xFFFFFFFF);
      g.fillRoundRect(screenX + 1, screenY + 1, nameWidth + 2 + gate.getWidth(), boxHeight - 2,
          10, 10);
      g.drawImage(gate, screenX + 2, screenY + 2, Graphics.TOP | Graphics.LEFT);
      g.setColor(0xFF000000);
      g.setFont(nameFont);
      g.drawString(name, screenX + 2 + gate.getWidth(), screenY
          + (boxHeight - nameFont.getHeight()) / 2, Graphics.TOP | Graphics.LEFT);
    }
  }
}
