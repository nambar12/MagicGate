package com.nambar.magicgate.common;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

import android.location.Location;
import android.net.Uri;

public class Gate implements Serializable
{
	public static String DEFAULT_GATE_NAME = "New gate";
	public static float DEFAULT_OPEN_DISTANCE = 200F;
	public static float DEFAULT_GPS_ACTIVATION_DISTANCE = 2000F;
	private static long FINE_REGISTRATION_TIMEOUT_MILLI = 5 * 60 * 1000;
	private static final long serialVersionUID = 1L;

	private UUID id = UUID.randomUUID();
	private String name;
	private double latitude;
	private double longitude;
	private float openDistance;
	private float gpsActivationDistance;
	private long gpsActivationTimeoutMilli;
	private String phoneNumber;
	private boolean timeLimit = false;
	private int fromHour = 20;
	private int fromMinute = 0;
	private int untilHour = 6;
	private int untilMinute = 0;
	private boolean lastLocationWasOutOfApproximateRange = false;
	private boolean wasOpened = false;

	public Gate()
	{
		name = DEFAULT_GATE_NAME;
		openDistance = DEFAULT_OPEN_DISTANCE;
		gpsActivationDistance = DEFAULT_GPS_ACTIVATION_DISTANCE;
		gpsActivationTimeoutMilli = FINE_REGISTRATION_TIMEOUT_MILLI;
	}
	
	private static final ObjectStreamField 	serialPersistentFields[] =
	{
		new ObjectStreamField("id", String.class),
		new ObjectStreamField("name", String.class),
		new ObjectStreamField("latitude", Double.TYPE),
		new ObjectStreamField("longitude", Double.TYPE),
		new ObjectStreamField("openDistance", Float.TYPE),
		new ObjectStreamField("gpsActivationDistance", Float.TYPE),
		new ObjectStreamField("gpsActivationTimeoutMilli", Long.TYPE),
		new ObjectStreamField("phoneNumber", String.class),
		new ObjectStreamField("timeLimit", Boolean.TYPE),
		new ObjectStreamField("fromHour", Integer.TYPE),
		new ObjectStreamField("fromMinute", Integer.TYPE),
		new ObjectStreamField("untilHour", Integer.TYPE),
		new ObjectStreamField("untilMinute", Integer.TYPE),
	};

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		ObjectInputStream.GetField fields= in.readFields();
		id  = UUID.fromString((String)fields.get("id",null));
		name = (String)fields.get("name",null);
		latitude = fields.get("latitude", 0D);
		longitude = fields.get("longitude", 0D);
		openDistance = fields.get("openDistance", 0F);
		gpsActivationDistance = fields.get("gpsActivationDistance", DEFAULT_GPS_ACTIVATION_DISTANCE);
		gpsActivationTimeoutMilli = fields.get("gpsActivationTimeoutMilli", FINE_REGISTRATION_TIMEOUT_MILLI);
		phoneNumber = (String)fields.get("phoneNumber", null);
		timeLimit = fields.get("timeLimit", false);
		fromHour = fields.get("fromHour", 0);
		fromMinute = fields.get("fromMinute", 0);
		untilHour = fields.get("untilHour", 0);
		untilMinute = fields.get("untilMinute", 0);
	}
	
	private void writeObject(ObjectOutputStream out) throws IOException
	{
		ObjectOutputStream.PutField fields= out.putFields();
		fields.put("id",  id.toString());
		fields.put("name", name);
		fields.put("latitude", latitude);
		fields.put("longitude", longitude);
		fields.put("openDistance", openDistance);
		fields.put("gpsActivationDistance", gpsActivationDistance);
		fields.put("gpsActivationTimeoutMilli", gpsActivationTimeoutMilli);
		fields.put("phoneNumber", phoneNumber);
		fields.put("timeLimit", timeLimit);
		fields.put("fromHour", fromHour);
		fields.put("fromMinute", fromMinute);
		fields.put("untilHour", untilHour);
		fields.put("untilMinute", untilMinute);
		out.writeFields();	
	}

	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public float getOpenDistance()
	{
		return openDistance;
	}
	public void setOpenDistance(float openDistance)
	{
		this.openDistance = openDistance;
	}
	public float getGPSActivationDistance()
	{
		return gpsActivationDistance;
	}
	public void setGPSActivationDistance(float distance)
	{
		this.gpsActivationDistance = distance;
	}
	public String getPhoneNumber()
	{
		return phoneNumber;
	}
	public void setPhoneNumber(String phoneNumber)
	{
		this.phoneNumber = phoneNumber;
	}

	public double getLatitude()
	{
		return latitude;
	}
	public void setLatitude(double latitude)
	{
		this.latitude = latitude;
	}
	public double getLongitude()
	{
		return longitude;
	}
	public void setLongitude(double longitude)
	{
		this.longitude = longitude;
	}

	public String getId()
	{
		return id.toString();
	}

	public int getFromHour()
	{
		return fromHour;
	}

	public void setFromHour(int fromHour)
	{
		this.fromHour = fromHour;
	}

	public int getFromMinute()
	{
		return fromMinute;
	}

	public void setFromMinute(int fromMinute)
	{
		this.fromMinute = fromMinute;
	}

	public int getUntilHour()
	{
		return untilHour;
	}

	public void setUntilHour(int untilHour)
	{
		this.untilHour = untilHour;
	}

	public int getUntilMinute()
	{
		return untilMinute;
	}

	public void setUntilMinute(int untilMinute)
	{
		this.untilMinute = untilMinute;
	}
	
	public boolean meetsTimeConstraints()
	{
		if(!timeLimit)
		{
			return true;
		}
		Date now = new Date();
		if(fromHour < untilHour || (fromHour == untilHour && fromMinute < untilMinute))
		{
			return (now.getHours() >= fromHour || (now.getHours() == fromHour && now.getMinutes() > fromMinute)) &&
			       (now.getHours() < untilHour || (now.getHours() == untilHour && now.getMinutes() < untilMinute));
		}
		else
		{
			return (now.getHours() >= fromHour || (now.getHours() == fromHour && now.getMinutes() > fromMinute)) ||
		       (now.getHours() < untilHour || (now.getHours() == untilHour && now.getMinutes() < untilMinute));
		}
	}
	
	public float distanceTo(Location location)
	{
		Location myLocation = new Location(location);
		myLocation.setLatitude(latitude);
		myLocation.setLongitude(longitude);
		return myLocation.distanceTo(location); 
	}

	public boolean inApproxumateRange(Location location)
	{
		return distanceTo(location) < openDistance + gpsActivationDistance;
	}
	
	public Uri getUri()
	{
		return Uri.parse("gate:" + getId());
	}
	
	@Override
	public String toString()
	{
		return getName();
	}

	public boolean isLastLocationWasOutOfApproximateRange()
	{
		return lastLocationWasOutOfApproximateRange;
	}

	public void setLastLocationWasOutOfApproximateRange(boolean lastLocationWasOutOfApproximateRange)
	{
		this.lastLocationWasOutOfApproximateRange = lastLocationWasOutOfApproximateRange;
	}

	public boolean wasOpened()
	{
		return wasOpened;
	}

	public void setWasOpened(boolean wasOpened)
	{
		this.wasOpened = wasOpened;
	}

	public boolean hasTimeLimit()
	{
		return timeLimit;
	}

	public void setTimeLimit(boolean timeLimit)
	{
		this.timeLimit = timeLimit;
	}

	public long getGpsActivationTimeoutMilli()
	{
		return gpsActivationTimeoutMilli;
	}

	public void setGpsActivationTimeoutMilli(long gpsActivationTimeoutMilli)
	{
		this.gpsActivationTimeoutMilli = gpsActivationTimeoutMilli;
	}

}
