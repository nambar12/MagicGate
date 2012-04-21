package com.nambar.magicgate;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.TimePicker.OnTimeChangedListener;

import com.nambar.magicgate.common.Gate;

public class ScheduleActivity extends Activity
{
	private CheckBox enabled;
	private TimePicker from;
	private TimePicker to;
	private TextView fromText;
	private TextView toText;
	private boolean modified = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.schedule);
	    init();
	}

	private void init()
	{
        enabled = (CheckBox) findViewById(R.id.CheckBoxEnableSchedule);
        from = (TimePicker) findViewById(R.id.TimePickerFrom);
        to = (TimePicker) findViewById(R.id.TimePickerTo);
        fromText = (TextView) findViewById(R.id.TextViewFrom);
        toText = (TextView) findViewById(R.id.TextViewTo);
        
        Gate gate = GatePropertiesActivity.getGate();
        enabled.setChecked(gate.hasTimeLimit());
        from.setCurrentHour(gate.getFromHour());
        from.setCurrentMinute(gate.getFromMinute());
        to.setCurrentHour(gate.getUntilHour());
        to.setCurrentMinute(gate.getUntilMinute());
        setVisibility(gate.hasTimeLimit());
        
        enabled.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				setVisibility(isChecked);
				modified = true;
			}
		});
        
        from.setOnTimeChangedListener(timeChangeListener);
        to.setOnTimeChangedListener(timeChangeListener);
	}

	private OnTimeChangedListener timeChangeListener = new OnTimeChangedListener()
	{
		@Override
		public void onTimeChanged(TimePicker view, int hourOfDay, int minute)
		{
			modified = true;
		}
	};
	
	private void setVisibility(boolean visible)
	{
		int visibility = visible ? View.VISIBLE : View.GONE;
		from.setVisibility(visibility);
		to.setVisibility(visibility);
		fromText.setVisibility(visibility);
		toText.setVisibility(visibility);
	}

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
    	if(keyCode == KeyEvent.KEYCODE_BACK)
    	{
    		if(modified)
    		{
    			saveChanges();
    			setResult(RESULT_OK);
    		}
   			finish();
    		return true;
    	}
    	else
    	{
    		return super.onKeyDown(keyCode, event);
    	}
    }

	private void saveChanges()
	{
		Gate gate = GatePropertiesActivity.getGate();
		gate.setTimeLimit(enabled.isChecked());
		gate.setFromHour(from.getCurrentHour());
		gate.setFromMinute(from.getCurrentMinute());
		gate.setUntilHour(to.getCurrentHour());
		gate.setUntilMinute(to.getCurrentMinute());
	}
}
