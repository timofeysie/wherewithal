package com.curchod.wherewithal;

import com.curchod.domartin.RemoteCall;
import com.curchod.domartin.UtilityTo;
import com.curchod.dto.SingleWord;

import android.app.Activity;
import android.content.Context;
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
import android.widget.TextView;

public class GameSnazzyThumbworkActivity extends Activity implements OnKeyListener
{
	
	private static final String DEBUG_TAG = "GameSnazzyThumbworkActivity";
	private String player_id;
	final Context context = this;
	private TextView question;
	private EditText answer;

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		String method = "onCreate";
		String build = "build 7d";
		Log.i(DEBUG_TAG, method+": "+build);
		player_id = "-5519451928541341468";
		setContentView(R.layout.activity_snazzy_thumbwork);
		question = (TextView)findViewById(R.id.question);
		answer = (EditText)findViewById(R.id.answer);
		getNextWord();
		answer.addTextChangedListener(new TextWatcher()
		{
	        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
	        
	        /**
	         * 13 || c == 10
	         */
	        public void onTextChanged(CharSequence s, int start, int before, int count) 
	        {
	        	for (int i = 0; i < s.length(); i++)
	        	{
	        		char c = s.charAt(i);
	        		Log.i(DEBUG_TAG, i+" "+Character.getNumericValue(c));
	        		if (Character.getNumericValue(c) == -1)
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
	
	private void getNextWord()
	{
		new Thread()
        {
            public void run()
            {   
            	RemoteCall remote = new RemoteCall(context);
            	final SingleWord word = remote.loadSingleWord(player_id);
            	((Activity) context).runOnUiThread(new Runnable() 
        		{
                    public void run() 
                    {
                    	question.setText(UtilityTo.getWord(word));
                    }
                });
            }
        }.start();
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