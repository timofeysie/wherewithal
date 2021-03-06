package com.curchod.wherewithal;

import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.curchod.domartin.Constants;
import com.curchod.domartin.IWantTo;
import com.curchod.domartin.RemoteCall;
import com.curchod.dto.SavedTest;

/**
 * The Main Activity for the Stepping Stones Android Application.
 * This Activity shows the main page stepping stones image and displays three buttons that lead to
 * cards, players and games.
 * If this is a new installation, a dialog will be shown to allow the user to enter the server IP address.
 * The user can change this with the single menu item after that.
 * @author timothy
 *
 */
public class MainActivity extends Activity 
{
	
	MainActivity main = this;
	private static final String DEBUG_TAG = "MainActivity";
	private Button game_button;
	private Button players_button;
	private Button cards_button;
	final Context context = this;
	private SharedPreferences shared_preferences;
    private Editor shared_editor;
	
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String method = "onCreate";
        Log.i(DEBUG_TAG, method+": build 23d");
        this.shared_preferences = context.getSharedPreferences(Constants.PREFERENCES, Activity.MODE_PRIVATE);
        this.shared_editor = shared_preferences.edit();
        String ip = shared_preferences.getString(Constants.SERVER_IP, "");
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
            	RemoteCall remote_call = new RemoteCall(context);
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
        		shared_editor.putString(Constants.SERVER_IP, new_ip);
        		shared_editor.commit();
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
        menu.add(0 , 1, 0, R.string.change_server_ip);
        return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	String method = "onOptionsItemSelected";
    	String selected = item.toString();
    	Log.i(DEBUG_TAG, method+": selected "+selected);
    	getIntent();
    	if (item.getItemId() == 1)
    	{
    		promptForIPAndSave();
    	    return true;
    	} 
    	return super.onOptionsItemSelected(item);
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
