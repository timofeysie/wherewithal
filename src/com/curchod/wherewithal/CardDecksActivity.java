package com.curchod.wherewithal;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableLayout.LayoutParams;
import android.widget.TableRow;
import android.widget.TextView;

import com.curchod.domartin.Constants;
import com.curchod.domartin.HouseDeck;
import com.curchod.domartin.IWantTo;
import com.curchod.domartin.UtilityTo;
import com.curchod.dto.DeckCard;

/**
 * This method gets deck names and ids of the house decks from the intent.  The iteration starts at 1.
 * The user can create a new house deck with the button and enter it's name.
 * Then they are sent to that blank deck to add cards and setup cards there.
 * If the user clicks on a previously created deck, they are taken to the next activity which
 * displays the deck and lets them set up cards there.
 * @author user
 *
 */
public class CardDecksActivity extends Activity 
{
	
	private String DEBUG_TAG = "CardDecksActivity";
	private String teacher_id = "0000000000000000001";
	private String[] DECK_NAMES; // not used?
	private String[] DECK_IDS; // not used?
	Hashtable <String,HouseDeck> decks;
	final Context context = this;
	String deck_name;
	private TableLayout table;
	private Hashtable <String,String> deck_names_ids; // not used so far?
	/** When we create the table, we save the row id in the deck_row_ids so that we can associate 
	 * that row id with a particular deck when a user clicks on it.*/
	private Hashtable <String,String> deck_row_ids;
	private Hashtable<String, String> player_id_names;

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_card_decks);
		String build = "57";
		String method = "onCreate";
		Log.i(DEBUG_TAG, method+": build "+build);
		getIntentInfo();
		createDecksList();
		Button button_add_deck = (Button)findViewById(R.id.button_add_deck);
		if (button_add_deck == null)
		{
			Log.i(DEBUG_TAG, method+": button is null");
		}
		button_add_deck.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
            	final String method = "onClick";
            	deck_name = null;
            	LayoutInflater layout_inflater = LayoutInflater.from(context);
        		View popup_view = layout_inflater.inflate(R.layout.card_decks_add_popup, null);
        		final AlertDialog.Builder alert_dialog_builder = new AlertDialog.Builder(context);
        		alert_dialog_builder.setView(popup_view);
        		final EditText edit_text_deck_name = (EditText) popup_view.findViewById(R.id.edit_text_deck_name);
        		alert_dialog_builder.setCancelable(false).setPositiveButton("OK",
            		new DialogInterface.OnClickListener() 
                	{
					    public void onClick(DialogInterface dialog,int id) 
					    {
					    	deck_name = edit_text_deck_name.getText().toString();
					    	Log.i(DEBUG_TAG, method+" selected deck_name "+deck_name);
					    	dialog.cancel();
					    	createNewDeckAndStartNextActivity();
					    }
					  }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() 
					  {
						  public void onClick(DialogInterface dialog,int id) 
						  {
							  dialog.cancel();
						  }
					  });
            	AlertDialog alert_dialog = alert_dialog_builder.create();
            	alert_dialog.show();
            }
        });
	}
	
	/**
	 * When we create the table, we save the row id in the deck_row_ids so that we can associate 
	 * that row id with a particular deck when a user clicks on it.
	 */
	private void createDecksList()
	{
		String method = "createDecksList";
		deck_row_ids = new Hashtable <String,String> ();
		Enumeration<String> e = decks.keys();
		while (e.hasMoreElements())
		{
			table = (TableLayout) findViewById(R.id.card_decks_table_layout);
	        TableRow row = new TableRow(this);
			String key = e.nextElement();
			HouseDeck deck = decks.get(key);
			String deck_name = deck.getDeckName();
			String deck_id = deck.getDeckId();
			deck_row_ids.put(row.toString(), deck_id);
			Log.i(DEBUG_TAG, method+" deck_dow_ids.put "+deck_id);
			int number_of_cards = deck.getCards().size();
			TextView t = new TextView(this);
	        TextView s = new TextView(this);   
	        t.setText(deck_name);
	        //s.setText(number_of_cards+"");
	        String player_name = deck.getPlayerId();
	        try
	        {
	        	player_name = player_id_names.get(deck.getPlayerId());
	        } catch (java.lang.NullPointerException npe)
	        {
	        	Log.i(DEBUG_TAG, method+" npe for player name get");
	        }
	        s.setText(player_name);
	        row.addView(t);
	        row.addView(s);
	        table.addView(row,new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
	        row.setClickable(true);
	        row.setOnClickListener(new OnClickListener()
	        {
	            @Override
	            public void onClick(View v)
	            {
	            	final String method = "onClick";
	            	String selected_deck_row_id = v.toString();
	            	String selected_deck_id = deck_row_ids.get(selected_deck_row_id);
	            	String selected_deck_name = decks.get(selected_deck_id).getDeckName();
	            	//Log.i(DEBUG_TAG, method+" selected "+selected_deck_name+" id "+selected_deck_id);
	            	loadDeckAndStartNextActivity(selected_deck_id);
	            }
	        });
		}
	}
	
	/**
	 * Load the house decks, find the current deck selected, then fill the intent with the deck
	 * information and the deck cards, starting from 1+deck_card_propterty, then start the next
	 * activity.
	 * @param selected_deck_id
	 */
	private void loadDeckAndStartNextActivity(final String selected_deck_id)
	{
		final String method = "loadDecksAndStartNextActivity";
		new Thread()
        {
            public void run()
            {   
            	IWantTo i_want_to = new IWantTo(context);
                Hashtable <String,HouseDeck> decks = i_want_to.loadTheHouseDecks();
                HouseDeck selected_deck = decks.get(selected_deck_id);
                Hashtable <String,DeckCard> cards = selected_deck.getCards();
                Intent intent = new Intent(CardDecksActivity.this, CardDeckActivity.class);
                intent.putExtra("deck_name", selected_deck.getDeckName());
                intent.putExtra("deck_id", selected_deck.getDeckId());
                intent.putExtra("player_id", selected_deck.getPlayerId());
                intent.putExtra("game_id", selected_deck.getGameId());
                Enumeration<String> e = cards.keys();
                int i = 1;
                while (e.hasMoreElements())
                {
                	String key = (String)e.nextElement();
                	DeckCard card = cards.get(key);
                	intent.putExtra(i+"card_id", card.getCardId());
                	intent.putExtra(i+"card_name", card.getCardName());
                	intent.putExtra(i+"status", card.getStatus());
                	intent.putExtra(i+"index", Integer.toString(card.getIndex()));
                	intent.putExtra(i+"type", card.getType());
                	//Log.i(DEBUG_TAG, method+" put "+card.getCardName()+" id "+card.getCardId()+" indes "+card.getIndex()+" type "+card.getType());
                	i++;
                }
                intent.putExtra("number_of_cards", i+"");
                //Log.i(DEBUG_TAG, method+" selected deck "+selected_deck.getDeckName()+" used by "+selected_deck.getPlayerId()+" for game "+selected_deck.getGameId());
                startActivity(intent);
            }
        }.start();
	}
	
	/**
	 * Load the house decks file, add the new blank deck with a new id,
	 * put the new deck in the intent, then start the next activity.
	 */
	private void createNewDeckAndStartNextActivity()
	{
		final String method = "createNewDeckAndStartNextActivity";
		final HouseDeck house_deck = new HouseDeck();
    	house_deck.setDeckName(deck_name);
    	final long new_id = UtilityTo.getNewID();
    	house_deck.setDeckId(new_id+"");
    	house_deck.setPlayerId("");
    	house_deck.setGameId("");
    	decks.put(new_id+"", house_deck);
    	//Log.i(DEBUG_TAG, method+" global decks now "+decks.size());
    	try
    	{
    		table.removeAllViews();
    	} catch (java.lang.NullPointerException npe)
    	{
    		table = (TableLayout) findViewById(R.id.card_decks_table_layout);
    	}
    	createDecksList();
    	//Hashtable <String,DeckCard> no_cards = new Hashtable <String,DeckCard> (); 
    	//Log.i(DEBUG_TAG, method+" new deck "+deck_name+" "+new_id);
		new Thread() 
    	{
            public void run() 
            {
            	IWantTo i_want_to = new IWantTo(context);
            	Hashtable <String,HouseDeck> house_decks = i_want_to.loadTheHouseDecks();
            	//Log.i(DEBUG_TAG, method+" existing number of decks "+house_decks.size());
            	house_decks.put(new_id+"", house_deck); 
            	//Log.i(DEBUG_TAG, method+" after add "+house_decks.size());
            	i_want_to.saveTheHouseDecks(house_decks);
            	Intent intent = new Intent(CardDecksActivity.this, CardDeckActivity.class);
                intent.putExtra("deck_name", deck_name);
                intent.putExtra("deck_id", new_id+"");
                intent.putExtra("number_of_cards", "1");
                intent.putExtra("player_id", "");
                intent.putExtra("game_id", "");
                startActivity(intent);
            }
    	}.start();
    }
	
	/**
	 * Decks are listed starting at 1 in the intent.  The number_of_decks extra holds the number of
	 * iterations needed to get all the decks.
	 */
	private void getIntentInfo()
	{
		decks = new Hashtable <String,HouseDeck> ();
		deck_names_ids = new Hashtable <String,String> ();
		deck_row_ids = new Hashtable <String,String> ();
		Hashtable<String, String> player_id_names = new Hashtable<String, String> ();
		String method = "getIntentInfo";
		Intent sender = getIntent();
		int size = Integer.parseInt(sender.getExtras().getString("number_of_decks")); // starts at 1.
		DECK_NAMES = new String[size];
		DECK_IDS = new String[size];
		for (int i = 1; i < size; i++)
		{
			String deck_name = sender.getExtras().getString((i+"deck_name"));
			String deck_id = sender.getExtras().getString((i+"deck_id"));
			String player_id = sender.getExtras().getString((i+"player_id"));
			String player_name = sender.getExtras().getString((i+"player_name"));
			String game_id = sender.getExtras().getString((i+"game_id"));
			Log.i(DEBUG_TAG, method+" deck_name "+i+" "+deck_name+" player_name "+player_name+" id "+player_id);
			DECK_NAMES[i] = deck_name;
			DECK_IDS[i] = deck_id;
			HouseDeck deck = new HouseDeck();
			deck.setDeckId(deck_id);
			deck.setDeckName(deck_name);
			deck.setPlayerId(player_id);
			deck.setGameId(game_id);
			try
			{
				player_id_names.put(player_id, player_name);
			} catch (java.lang.NullPointerException npe1)
			{
				try
				{
					player_id_names.put(player_id, "n/a");
					Log.i(DEBUG_TAG, method+" no player name");
				} catch (java.lang.NullPointerException npe2)
				{
					Log.i(DEBUG_TAG, method+" no player id");
				}
			}
			decks.put(deck_id, deck);
			deck_names_ids.put(deck_name, deck_id);
		}
	}

	/**
	 * Set up a List of name value pairs with the following info:
	 * number_of_decks
     * teacher_id
     * i is from 1 to number of decks.
     * i+deck_name
     * i+deck_id
     * i+player_id
     * i+game_id
     * i+number_of_cards
     * j is from 1 to number_of_cards
     * i+"card"+j+"card_id"
     * i+"card"+j+"card_name"
     * i+"card"+j+"status"
     * i+"card"+j+"type"
     * i+"card"+j+"index
	 * @param loaded_decks
	 * @return
	 */
	private List<NameValuePair> setupHouseDeckPairs(Hashtable <String,HouseDeck> loaded_decks)
    {
    	String method = "setupDeckCardAssociationsPairs()";
    	List<NameValuePair> name_value_pairs = new ArrayList<NameValuePair>(); 
    	String device_id = getDeviceId();
        name_value_pairs.add(new BasicNameValuePair("device_id", device_id));
    	name_value_pairs.add(new BasicNameValuePair("number_of_decks", decks.size()+""));
    	name_value_pairs.add(new BasicNameValuePair("teacher_id", teacher_id));
    	Log.i(DEBUG_TAG, method+" device_id "+device_id+" teacher_id "+teacher_id+" number_of_decks "+decks.size());
    	Enumeration <String> e = loaded_decks.keys();
    	int i = 1;
    	while (e.hasMoreElements())
    	{
    		String deck_id = (String)e.nextElement();
    		HouseDeck deck = (HouseDeck)loaded_decks.get(deck_id);
    		name_value_pairs.add(new BasicNameValuePair(i+"deck_name", deck.getDeckName()));
    		name_value_pairs.add(new BasicNameValuePair(i+"deck_id", deck.getDeckId()));
    		name_value_pairs.add(new BasicNameValuePair(i+"player_id", deck.getPlayerId()));
    		name_value_pairs.add(new BasicNameValuePair(i+"game_id", deck.getGameId()));
    		Hashtable <String,DeckCard> cards = deck.getCards();
    		name_value_pairs.add(new BasicNameValuePair(i+"number_of_cards", cards.size()+""));
    		Log.i(DEBUG_TAG, method+" deck "+i+" - "+deck.getDeckName()+" cards "+cards.size());
    		Enumeration <String> f = cards.keys();
    		int j = 1;
    		while (f.hasMoreElements())
    		{
    			String card_id = (String)f.nextElement();
    			DeckCard card = cards.get(card_id);
    			name_value_pairs.add(new BasicNameValuePair(i+"card"+j+"card_id", card.getCardId()));
    			name_value_pairs.add(new BasicNameValuePair(i+"card"+j+"card_name", card.getCardName()));
    			name_value_pairs.add(new BasicNameValuePair(i+"card"+j+"status", card.getStatus()));
    			name_value_pairs.add(new BasicNameValuePair(i+"card"+j+"type", card.getType()));
    			name_value_pairs.add(new BasicNameValuePair(i+"card"+j+"index", card.getIndex()+""));
    			Log.i(DEBUG_TAG, "i "+i+" j "+j+" card_id "+card_id);
    			j++;
    		}
    		i++;
    	}
    	return name_value_pairs;
    }
	
	private String getDeviceId()
	{
		TelephonyManager telephony_manager = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
        String device_id = telephony_manager.getDeviceId();
		if (device_id.equals("000000000000000") || device_id == null);
		{
			Date date = new Date();
			long now  = date.getTime();
			//device_id = "villa_decks";
			device_id = now+"";
		}
		return device_id;
	}
    
	/**
	 * Remote send to save_house_decks.do with the name value pairs described in 
	 * setupHouseDeckPairs to save a complete copy of the house decks on the phone on the server.
	 * @param loaded_decks
	 */
    private void saveHouseDecksOnServer(Hashtable <String,HouseDeck> loaded_decks)
    {
    	 DefaultHttpClient httpclient = new DefaultHttpClient();
    	 SharedPreferences shared_preferences = context.getSharedPreferences(Constants.PREFERENCES, Activity.MODE_PRIVATE);
    	 String ip = shared_preferences.getString(Constants.SERVER_IP, "");
         HttpPost httppost = new HttpPost("http://"+ip+"/indoct/save_house_decks.do");     
         httppost.addHeader("Accept", "text");
         httppost.addHeader("Content-Type", "application/x-www-form-urlencoded");
         List<NameValuePair> name_value_pairs = setupHouseDeckPairs(loaded_decks);
         try 
         {
			httppost.setEntity(new UrlEncodedFormEntity(name_value_pairs));
			HttpResponse response = httpclient.execute(httppost);
		 } catch (UnsupportedEncodingException e) 
		 {
			e.printStackTrace();
		 } catch (ClientProtocolException e) 
		 {
			e.printStackTrace();
		 } catch (IOException e) 
		 {
			e.printStackTrace();
		 }
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		getMenuInflater().inflate(R.menu.card_decks, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		final String method = "onOptionsItemSelected";
		if (item.getItemId() == R.id.save_house_decks)
		{
			Log.i(DEBUG_TAG, method+" save decks");
			new Thread()
	        {
	            public void run()
	            {   
	            	IWantTo i_want_to = new IWantTo(context);
	                Hashtable <String,HouseDeck> loaded_decks = i_want_to.loadTheHouseDecks();
	                saveHouseDecksOnServer(loaded_decks);
	            }
	        }.start();
		} else if (item.getItemId() == R.id.load_house_decks)
		{
			Log.i(DEBUG_TAG, method+" load decks");
			new Thread()
	        {
	            public void run()
	            {   
	            	IWantTo i_want_to = new IWantTo(context);
	            	String device_id = getDeviceId();
	            	Hashtable <String,HouseDeck> remote_house_decks = i_want_to.loadRemoteHouseDecks(teacher_id, device_id);
	            	try
	            	{
	            		Log.i(DEBUG_TAG, method+" received "+remote_house_decks.size()+" house decks");
	            	} catch (java.lang.NullPointerException npe)
	            	{
	            		Log.i(DEBUG_TAG, method+" npe");
	            	}
	            	Enumeration <String> e = remote_house_decks.keys();
	            	int i = 1;
	            	while (e.hasMoreElements())
	            	{
	            		String deck_id = (String)e.nextElement();
	            		HouseDeck deck = (HouseDeck)remote_house_decks.get(deck_id);
	            		Hashtable <String,DeckCard> cards = deck.getCards();
	            		Log.i(DEBUG_TAG, method+i+"deck_name "+deck.getDeckName()+"number_of_cards "+cards.size());
	            		Enumeration <String> f = cards.keys();
	            		int j = 1;
	            		while (f.hasMoreElements())
	            		{
	            			String card_id = (String)f.nextElement();
	            			DeckCard card = cards.get(card_id);
	            			Log.i(DEBUG_TAG, method+i+"card"+j+"card_id "+card.getCardId()+j+" card_name "+card.getCardName());
	            			j++;
	            		}
	            		i++;
	            	}
	            	
	            }
	        }.start();
		}	
		return super.onOptionsItemSelected(item);
	}

}
