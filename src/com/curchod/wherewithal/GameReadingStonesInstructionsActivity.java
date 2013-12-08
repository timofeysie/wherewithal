package com.curchod.wherewithal;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;

public class GameReadingStonesInstructionsActivity extends Activity 
{

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_game_reading_stones_instructions);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater()
				.inflate(R.menu.game_reading_stones_instructions, menu);
		return true;
	}

}
