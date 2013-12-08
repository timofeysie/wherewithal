package com.curchod.wherewithal;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.LinearLayout;

public class SnazzyFootworkActivity extends Activity implements OnKeyListener
{
	
	private static final String DEBUG_TAG = "SnazzyFootworkActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		String method = "onCreate";
		String build = "build 2";
		Log.i(DEBUG_TAG, method+": "+build);
		setContentView(R.layout.activity_snazzy_footwork);
		EditText answer = (EditText)findViewById(R.id.answer);
		//String value = answer.getText().toString();
		answer.addTextChangedListener(new TextWatcher()
		{
	        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
	        
	        public void onTextChanged(CharSequence s, int start, int before, int count) 
	        {
	        	for (int i = 0; i < s.length(); i++)
	        	{
	        		char c = s.charAt(i);
	        		Log.i(DEBUG_TAG, i+" "+Character.getNumericValue(c));
	        		if (c == 13 || c == 10)
	        		{
	        			startSnaz();
	        		}
	        	}
	        }
			@Override
			public void afterTextChanged(Editable arg0) 
			{
	
			}

	    });
	}
	
	private void startSnaz()
	{
		LinearLayout layoutToAnimate = (LinearLayout)findViewById(R.id.LayoutRow);
        Animation an =  AnimationUtils.loadAnimation(this, R.anim.snazzyintro);
        layoutToAnimate.startAnimation(an);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.snazzy_footwork, menu);
		return true;
	}

	@Override
	public boolean onKey(DialogInterface arg0, int arg1, KeyEvent arg2) 
	{
		// TODO Auto-generated method stub
		return false;
	}

}
