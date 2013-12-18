package com.curchod.wherewithal;

import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.curchod.domartin.Constants;
import com.curchod.domartin.IWantTo;
import com.curchod.domartin.RemoteCall;
import com.curchod.dto.SavedTest;

public class MainActivity extends Activity 
{
	
	MainActivity main = this;
	private static final String DEBUG_TAG = "MainActivity";
	private Button game_button;
	private Button players_button;
	private Button cards_button;
	final Context context = this;
	
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String method = "onCreate";
        Log.i(DEBUG_TAG, method+": build 12c");
        SharedPreferences shared_preferences = getSharedPreferences(Constants.SERVER_IP, MODE_PRIVATE);
        String ip = (shared_preferences.getString("ip", ""));
        Log.i(DEBUG_TAG, method+": current ip: "+ip);
        if (ip == null || ip.equals(""))
        {
        	promptForIPAndSave();
        }
        // Players Activity
        players_button = (Button) findViewById(R.id.players_button);
        players_button.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
            	disableButtons();
                startActivity(new Intent(MainActivity.this,
                    PlayersActivity.class));
            }
        });
        
        // GameActivity
        game_button = (Button) findViewById(R.id.game_button);
        game_button.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
            	disableButtons();
                startActivity(new Intent(MainActivity.this,
                    GamesActivity.class));
            }
        });
        
        // CardsActivity
        cards_button = (Button) findViewById(R.id.cards_button);
        cards_button.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
            	disableButtons();
            	RemoteCall remote_call = new RemoteCall();
        		remote_call.savedTestsListAction("teach", "teach", main);
        		Vector <SavedTest> saved_tests = remote_call.getSavedTests();
        		Log.i(DEBUG_TAG, "Saved_tests "+saved_tests.size());
            }
        });
    }
    
    private void loadIP()
    {
    	final String method = "loadIP";
    	new Thread() 
    	{
            public void run() 
            {
            	IWantTo i_want_to = new IWantTo(context);
            	String ip = i_want_to.loadIPFile();
            	if (ip == null || ip.equals(""))
            	{
            		Log.i(DEBUG_TAG, method+"ip is null.  Promt from server ip.");
            		//promptForIP();
            	} else
            	{
            		Log.i(DEBUG_TAG, method+" using IP "+ip);
            	}
            }
    	}.start();
    }
    
    private void promptForIPAndSave()
    {
    	final String method = "promptForIPAndSave";
    	 AlertDialog.Builder alert = new AlertDialog.Builder(context);
         alert.setTitle("Server Configuration");
         alert.setMessage("Enter Server IP"); 
         final EditText input = new EditText(context);
         alert.setView(input);
         alert.setPositiveButton("OK", new DialogInterface.OnClickListener() 
         {
        	 public void onClick(DialogInterface dialog, int whichButton) 
        	 {
        		String new_ip = input.getEditableText().toString();
        		SharedPreferences shared_preferences = getPreferences(Context.MODE_PRIVATE);
        	    SharedPreferences.Editor preferences_editor = shared_preferences.edit();
        	    preferences_editor.putString("ip", new_ip);
        	    preferences_editor.commit();
        	    Log.i(DEBUG_TAG, method+" saved new ip "+new_ip);
        	 }
         }); 
         alert.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() 
         {
           public void onClick(DialogInterface dialog, int whichButton) 
           {
               dialog.cancel();
           }
         });
         AlertDialog alertDialog = alert.create();
         alertDialog.show();
    }
    
    private void disableButtons()
    {
    	players_button.setEnabled(false);
    	game_button.setEnabled(false);
    	cards_button.setEnabled(false);
    }
    
    public void forceError()
    {
    	Log.i(DEBUG_TAG, "Something here thisway comes");
    	if (true)
    	{
    		throw new Error("Whoa~");
    	}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    @Override
    protected void onResume() 
    {
        super.onResume();
        if (players_button !=null)
        {
        	enableButtons();
        }
    }

    
    private void enableButtons()
    {
    	players_button.setEnabled(true);
    	game_button.setEnabled(true);
    	cards_button.setEnabled(true);
    }
}
