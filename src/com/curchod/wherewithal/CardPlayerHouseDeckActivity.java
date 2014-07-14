package com.curchod.wherewithal;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.widget.TableLayout;
import android.widget.TableLayout.LayoutParams;
import android.widget.TableRow;
import android.widget.TextView;

public class CardPlayerHouseDeckActivity extends Activity 
{

	private String DEBUG_TAG = "CardPlayerHouseDeckActivity";
	private TableLayout table;
	private Hashtable <String,String> deck_card_name_words;
	private Hashtable <String,String> word_deck_card_names;
	/** To identify the rows in the table later. */
	private Hashtable <String,String> card_row_names;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_card_player_house_deck);
		String method = "onCreate";
		String build = "26";
		Log.i(DEBUG_TAG, method+": build "+build+" (rock it baby rock it baby, tonight~)");
		getIntentInfo();
		createCardsList();
	}
	
	private void createCardsList()
	{
		String method = "createCardsList";
		card_row_names = new Hashtable <String,String> ();
		//table.setVerticalScrollBarEnabled(true);
		Hashtable <String,String> reading_rows = sortWords("R");
		Log.i(DEBUG_TAG, method+": after sortWords, reading_rows size "+reading_rows.size());
		for (int i = 0; i < reading_rows.size(); i++)
		{
			String word = reading_rows.get(i+"");
			try
			{
				Log.i(DEBUG_TAG, method+" word "+i+" "+word);
				setupRow(word);
			} catch (java.lang.NullPointerException npe)
			{
				Log.i(DEBUG_TAG, method+": setupRow reading npe for "+i);
				npe.printStackTrace();
			}
		}
		Hashtable <String,String> writing_rows = sortWords("W");
		Log.i(DEBUG_TAG, method+": after sortWords, writing_rows size "+writing_rows.size());
		for (int i = 0; i < reading_rows.size(); i++)
		{
			String word = writing_rows.get(i+"");
			try
			{
				Log.i(DEBUG_TAG, method+" word "+i+" "+word);
				setupRow(word);
			} catch (java.lang.NullPointerException npe)
			{
				Log.i(DEBUG_TAG, method+": setupRow reading npe for "+i);
				npe.printStackTrace();
			}
		}
	}
	
	private void setupRow(String word)
	{
		String method = "setupRow";
		table = (TableLayout) findViewById(R.id.card_player_house_deck_table_layout);
		table.setVerticalScrollBarEnabled(true);
		table.setColumnStretchable(2, true);
		TableRow row = new TableRow(this);
		String card_name_string = word_deck_card_names.get(word);
		card_row_names.put(row.toString(), card_name_string);
		TextView card_name_text = new TextView(this);
        TextView word_text = new TextView(this);   
        card_name_text.setWidth(50);
        card_name_text.setText(card_name_string);
        word_text.setText(word);
        word_text.setGravity(Gravity.LEFT);
        //Log.i(DEBUG_TAG, method+": table row "+card_name_string+" word "+word);
        row.addView(card_name_text);
        row.addView(word_text);
        table.addView(row,new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
	}
	
	/**
	 * First create a list of just the type of cards needed.
	 * Then create an ordered hash of index card pairs.
	 * @param type
	 * @return
	 */
	private Hashtable <String,String> sortWords(String type)
	{
		String method = "sortCards";
		//Log.i(DEBUG_TAG, method+" start ------");
		Vector <String> type_results = new Vector <String> ();
		Hashtable <String,String> word_index = new Hashtable <String,String> ();
		Enumeration<String> e = deck_card_name_words.keys();
		while (e.hasMoreElements())
		{
			String key = e.nextElement();
			String word = deck_card_name_words.get(key);
			String this_type_string = key.substring(0,1);
			String this_word_index = key.substring(1,2);
			//Log.i(DEBUG_TAG, word+" this_type_string "+this_type_string+" this_word_index "+this_word_index);
			word_index.put(word, this_word_index);
			if (this_type_string.equals(type))
			{
				type_results.add(word);
				//Log.i(DEBUG_TAG, "put "+word+" this_type_string "+this_type_string+" this_word_index "+this_word_index);
			}
		}
		//Log.i(DEBUG_TAG, method+" type "+type+" cards "+type_results.size());
		Hashtable <String,String> results = new Hashtable <String,String> ();
		for (int i = 0; i < type_results.size(); i++)
		{
			String word = (String)type_results.get(i);
			int index = Integer.parseInt(word_index.get(word));
			results.put(i+"", word);
			Log.i(DEBUG_TAG, method+" i "+i+" index "+index+" deck_card "+word_deck_card_names.get(word)+" - "+word);
		}
		//Log.i(DEBUG_TAG, method+" type "+type+" results "+results.size());
		return results;
	}
	
	/**
	 * Starting at 1 for cards, we loade the amount of cards we have set in the number_of_cards extra.
	 * intent.putExtra(i+"deck_card_name", deck_card_name);
     * intent.putExtra(i+"word", word);
	 * intent.putExtra("number_of_words", i+"");
	 */
	private void getIntentInfo()
	{
		String method = "getIntentInfo";
		deck_card_name_words = new Hashtable <String,String> ();
		word_deck_card_names = new Hashtable <String,String> ();
		Intent sender = getIntent();
		int number_of_words = Integer.parseInt(sender.getExtras().getString("number_of_words"));
		String player_name = sender.getExtras().getString("player_name");
		for (int i = 1; i < number_of_words; i++)
		{
			try
			{
				String card_name = sender.getExtras().getString(i+"deck_card_name");
				String word = sender.getExtras().getString(i+"word");
				deck_card_name_words.put(card_name, word);
				word_deck_card_names.put(word, card_name);
				Log.i(DEBUG_TAG, method+" got "+card_name+" - "+word);
			} catch (java.lang.NullPointerException npe)
			{
				Log.i(DEBUG_TAG, method+" npe for "+i);
			}
		}
		setTitle(player_name);
		Log.i(DEBUG_TAG, method+" number of words got from intent: "+deck_card_name_words.size());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.card_player_house_deck, menu);
		return true;
	}

}
