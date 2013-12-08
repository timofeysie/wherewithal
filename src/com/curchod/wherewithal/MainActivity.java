package com.curchod.wherewithal;

import java.util.Vector;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.curchod.domartin.RemoteCall;
import com.curchod.domartin.SavedTest;

public class MainActivity extends Activity 
{
	
	MainActivity main = this;
	private static final String DEBUG_TAG = "MainActivity";
	private Button game_button;
	private Button players_button;
	private Button cards_button;
	
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(DEBUG_TAG, "onCreate: build 7a");
        
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
