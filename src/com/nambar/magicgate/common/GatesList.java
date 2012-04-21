package com.nambar.magicgate.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.Format;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

public class GatesList
{
	private static final String FILENAME = "gates";
	private static GatesList s_instance = new GatesList();
	private File persistencyDir = null;
	private Map<String,Gate> m_gates = new HashMap<String,Gate>();
	private Format format = NumberFormat.getIntegerInstance();
	
	private GatesList()
	{
	}
	
	public static GatesList getInstnace()
	{
		return s_instance;
	}
	
	public void init(File persistencyDir)
	{
		this.persistencyDir = persistencyDir;
		load();
	}
	
	private void load()
	{
		try
		{
			m_gates.clear();
			File file = new File(persistencyDir, FILENAME);
			if(!file.exists())
			{
				return;
			}
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
			int amount = in.readInt();
			for (int i = 0; i < amount; i++)
			{
				Gate gate = (Gate)in.readObject();
				m_gates.put(gate.getId(), gate);
			}
			in.close();
		}
		catch (Exception e)
		{
			Log.e("GatesList", "cannot read gates", e);
		}
	}
	
	public void save()
	{
		try
		{
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(new File(persistencyDir, FILENAME)));
			out.writeInt(m_gates.size());
			for(Gate gate : m_gates.values())
			{
				out.writeObject(gate);
			}
			out.close();
		}
		catch (Exception e)
		{
			Log.e("GatesList", "cannot store gates", e);
		}
	}

	public void add(Gate gate)
	{
		m_gates.put(gate.getId(), gate);
		save();
	}

	public List<Gate> getGates()
	{
		return new ArrayList<Gate>(m_gates.values());
	}

	public Gate lookup(String gateID)
	{
		return m_gates.get(gateID);
	}

	public void remove(Gate gate)
	{
		m_gates.remove(gate.getId());
		save();
	}
	
	public List<HashMap<String, String>> getGateEntries(Location currentLocation)
	{
		ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
		for(Gate gate : m_gates.values())
		{
			HashMap<String, String> map = new HashMap<String, String>();
			map.put("id", gate.getId());
			map.put("name", gate.getName());
			String distance = (currentLocation == null) ? "N/A" : formatDistance(currentLocation, gate); 
			map.put("distance", distance);
			list.add(map);
		}
		return list;
	}

	private String formatDistance(Location currentLocation, Gate gate)
	{
		float distance = gate.distanceTo(currentLocation);
		StringBuilder sb = new StringBuilder();
		sb.append(distance > 10000 ? format.format(distance/1000) + " KM" : format.format(distance) + " Meters");
		sb.append(currentLocation.getProvider().equals(LocationManager.GPS_PROVIDER) ? " Fixed" : " Estimated");
		if(gate.inApproxumateRange(currentLocation))
		{
			sb.append(" (In close range)");
		}
		return sb.toString();
	}

	public boolean hasGates()
	{
		return m_gates.size() > 0;
	}
	
	public boolean inApproxumateRangeOfAnyGate(Location location)
	{
		for(Gate gate : m_gates.values())
		{
			if(gate.inApproxumateRange(location))
			{
				return true;
			}
		}
		return false;
	}
	
}
