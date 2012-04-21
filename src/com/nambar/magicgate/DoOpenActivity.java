package com.nambar.magicgate;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.Window;
import android.view.WindowManager;

import com.nambar.magicgate.common.Gate;
import com.nambar.magicgate.common.GatesList;

public class DoOpenActivity extends Activity
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
	    super.onCreate(savedInstanceState);
	    requestWindowFeature(Window.FEATURE_NO_TITLE);
	    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
	    String gateID = getIntent().getData().getSchemeSpecificPart();
	    Gate gate = GatesList.getInstnace().lookup(gateID);
	    if(canDial())
	    {
	    	dial(gate.getPhoneNumber());
	    }
	}

	private void dial(String number)
	{
		try
		{
			Intent intent = new Intent(Intent.ACTION_CALL);
			intent.setData(Uri.parse("tel:" + number));
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	        startActivity(intent);
	    }
		catch (Exception e)
		{
	        e.printStackTrace();
	    }

	}
	
	private boolean canDial()
	{
		TelephonyManager tm = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
		return tm.getCallState() == TelephonyManager.CALL_STATE_IDLE;
	}



}
