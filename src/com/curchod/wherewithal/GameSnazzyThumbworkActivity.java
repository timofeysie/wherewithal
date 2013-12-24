package com.curchod.wherewithal;

import java.util.Date;

import com.curchod.domartin.Constants;
import com.curchod.domartin.RemoteCall;
import com.curchod.domartin.Scoring;
import com.curchod.domartin.UtilityTo;
import com.curchod.dto.SingleWord;
import com.curchod.dto.SingleWordTestResult;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This activity is under construction.
 * Currently it must get the next word to be tested from the server.
 * It creates the appropriate question and creates a text field for the answer.
 * The user must login so that we can get their words.
 * 
 * @author user
 *
 */
public class GameSnazzyThumbworkActivity extends Activity implements OnKeyListener
{
	
	private static final String DEBUG_TAG = "GameSnazzyThumbworkActivity";
	private String current_player_id;
	final Context context = this;
	private TextView text_question;
	private TextView text_player_name;
	private EditText text_answer;
	private SingleWord word;
	long timer;
	boolean answered;

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		String method = "onCreate";
		String build = "build 15c";
		Log.i(DEBUG_TAG, method+": "+build);
		SharedPreferences shared_preferences = context.getSharedPreferences(Constants.PREFERENCES, Activity.MODE_PRIVATE);
        current_player_id = shared_preferences.getString(Constants.CURRENT_PLAYER_ID, "");
        String current_player_name = shared_preferences.getString(current_player_id, "");
        setContentView(R.layout.activity_game_snazzy_thumbwork);
        if (current_player_id == null || current_player_id.equals(""))
        {
        	text_question = (TextView)findViewById(R.id.question);
    		text_answer = (EditText)findViewById(R.id.answer);
    		text_question.setVisibility(View.GONE);
    		text_answer.setVisibility(View.GONE);
        	Toast.makeText(this, "Please log in to play this game.", Toast.LENGTH_LONG ).show();
        } else
        {
        	text_player_name = (TextView)findViewById(R.id.text_view_player_name);
        	text_player_name.setText(current_player_name);
        	getFirstWord();
        }
	}
	
	private void getFirstWord()
	{
		setup();
    	getNextWord();
    	text_answer.addTextChangedListener(new TextWatcher()
    	{
    		public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
    		/**
    		 * 13 || c == 10
    		 */	
    		public void onTextChanged(CharSequence s, int start, int before, int count) 
    		{
    			if (!answered)
    			{
    				for (int i = 0; i < s.length(); i++)
    				{
    					char c = s.charAt(i);
    					Log.i(DEBUG_TAG, i+" "+Character.getNumericValue(c));
    					if (Character.getNumericValue(c) == -1)
    					{
    						answered = true;
    						scoreResult();
    					}
    				}
    			}
    		}
    		@Override
    		public void afterTextChanged(Editable arg0) 
    		{

    		}

    	});
	}
	
	private void setup()
	{
		answered = false;
		text_question = (TextView)findViewById(R.id.question);
		text_answer = (EditText)findViewById(R.id.answer);
	}
	
	/**
	 * 
	 */
	private void scoreResult()
	{
		String method = "scoreResult";
		String player_answer = text_answer.getText().toString();
		String correct_answer = UtilityTo.getAnswer(word);
		String grade = "fail";
		if (Scoring.scoreAnswer(correct_answer, player_answer))
		{
			Log.i(DEBUG_TAG, method+" incorrect");
			startSnazzyFail();
			text_answer.setText(correct_answer);
		} else
		{	
			Log.i(DEBUG_TAG, method+" correct");
			grade = "pass";
			startSnazzyPass();
		}
		sendResultToServer(grade);
        getNextWord();
	}
	
	private void sendResultToServer(final String grade)
	{
		new Thread()
        {
            public void run()
            {   
            	RemoteCall remote = new RemoteCall(context);
            	SingleWordTestResult swtr = remote.scoreSingleWordTest(current_player_id, grade, timer);
            	// what to do with the swtr?
            }
        }.start();
	}
	
	private void getNextWord()
	{
		timer = new Date().getTime();
		new Thread()
        {
            public void run()
            {   
            	RemoteCall remote = new RemoteCall(context);
            	word = remote.loadSingleWord(current_player_id);
            	((Activity) context).runOnUiThread(new Runnable() 
        		{
                    public void run() 
                    {
                    	text_question.setText(UtilityTo.getQuestion(word));
                    	text_answer.setText("");
                    	answered = false;
                    }
                });
            }
        }.start();
	}
	
	private void startSnazzyPass()
	{
		RelativeLayout layoutToAnimate = (RelativeLayout)findViewById(R.id.anwer_layout);
        Animation an =  AnimationUtils.loadAnimation(this, R.anim.snazzypass);
        layoutToAnimate.startAnimation(an);
	}
	
	private void startSnazzyFail()
	{
		RelativeLayout layoutToAnimate = (RelativeLayout)findViewById(R.id.anwer_layout);
        Animation an =  AnimationUtils.loadAnimation(this, R.anim.snazzyfail);
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
