package com.curchod.wherewithal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.graphics.drawable.ColorDrawable;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.tech.MifareClassic;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.view.ViewPager.LayoutParams;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.curchod.domartin.AsyncLoadGameFile;
import com.curchod.domartin.Constants;
import com.curchod.domartin.IWantTo;
import com.curchod.domartin.PlayerInfo;
import com.curchod.domartin.Scoring;
import com.curchod.domartin.TagDescription;
import com.curchod.domartin.UtilityTo;
import com.curchod.dto.Card;
import com.curchod.dto.Game;

/**
 * This class is based on the GameReadingStonesActivity.
 * The big difference is there is a toggle button in this
 * game that sets the mode from 'guess' to 'hint"
 * Guess mode brings up a three table popup when a tag is scanned.
 * text - definition - score checkbox.  The scoreboard is updated by correct
 * answers which a user judges based on what the player has written down.
 * 
 * When the user clicks on the Writing Stones button, the onClick calls loadGameFile() 
 * which in turn calls checkForGameReady() and then fillIntentAndStartNewActivity() before the thread ends.  
 * At the end the next GameReadingStonesActivity is started.
 * 
 * We call loadCardsFile()followed by loadPlayers().  If there is no players.xml file yet,
 * we create that and populate it with the player id's retrieved from the intent.
 * We use the normal method of number_of_players indicating how many times to iterate 
 * through the player_ids in the intent.  So we load their id's like this:
 * 0player_id= *id*
 * 1player_id= *id*
 * The loadPlayers() will also create PlayerInfo objects for players that are listed in the game
 * file and hence passed in in the intent, but not in the players.xml file because they
 * haven't logged in.  
 * 
 * Each time a NFC tag is detected, onNewIntent(Intent intent) is called.  This
 * method then calls resolveIntent3 which gets the NDEF messages, and extracts the record
 * with the card id.  This we call getId to associate that with a card from the cards file.
 * 
 * When a tag/card is scanned, onNewIntent(Intent intent) gets called.
 * This method simply calls resolveIntent.  This method gets an array of NDEF Messages, 
 * which is used to get an array of NDEF Records, of which the one in the 0 position contains the tag id.  
 * When found, the associateWithCardsFile method is called.
 * This method tried to associate the tag id with an id in the cards.xml file.
 * Due to hidden characters in the tag message, we use the removeHiddenCharacters 
 * so that we can match the tag id with the card id.
 * If the card is a registered game card, foundGameCard(Card) is called with that card as an arg.
 * This method decides what to do with the card, 
 * If it's a played card,  we show toast.
 * If it's a new card, call fistCard().
 * If it's not a new card, then we check to see what part of the turn the player is at and act accordingly.
 * 
 * When a match is found, updateTurnCards() is called.
 * if it is the second card scanned, dismiss the dialog and call showCards() followed by checkForMatch().
 * If the player tried to match two cards that don't, both words from those cards are shown in a contemplation window.
 * 
 * The score is incremented by the number of matches in each turn.  First pair is 1, second pair so the score goes up 2,
 * then the third match in a turn goes up three.  The score is kept in the global game_file object.
 * When the onCreate method is called, which will also happen if the device is rotated, the game file is
 * loaded again using an inner asynchronous task that then updates the score board when it completes.
 * @author Administrator
 *
 */
public class GameWritingStonesActivity extends Activity implements View.OnClickListener
{
	
	private static final String DEBUG_TAG = "GameWritingStonesActivity";
	private String test_id;
	private String test_name;
	private String test_status;
	private String class_id;
	NfcAdapter mfc_adapter;
	PendingIntent pending_intent;
	String[][] tech_lists_array;	  
	IntentFilter ndef_filer;
	IntentFilter filters[];
	final Context context = this;
	//private String card_message;
	ArrayAdapter<TagDescription> a_adapter;
	Hashtable <String,Card> cards;
    private static final int icon_group = 1;
    private static final int instructions_menu_item_id = 3;
    private static final int reset_game_menu_item_id = 4;
    Hashtable <String,Card> played_cards;
    /** During each players turn, scanned cards are kept in this list.*/
    Vector <Card> turn_cards;
    ArrayAdapter<String> array_dapter;
    private Dialog list_dialog;
    private ListAdapter list_adapter;
    /** This stores two consecutive scanned cards to see if they match.*/
    private Vector <Card> card_pairs;
    /** This holds the player info objects from the players.xml file. */
    private Hashtable <String,PlayerInfo> players;
    /** This holds the player ids from the intent.*/
    private Vector <String>player_ids;
    private Hashtable <String, String> id_player_names;
    /** To hold the currently tag-scanning player id */
    private String current_player_id;		  // ditto
    private String previously_played_card_id; // the gentlemanly way to do this
    private String previously_played_card_player_id;
    private String previously_played_word_id;		 //
    private String previously_played_word_type;		 //
    private Card previous_card;
    /** This holds the player names and scores */
    private TableLayout table;
    private int number_of_matches;
    private long last_turn_time;
    private Game game_file;
    
    // added for the writing game
    private TableLayout guess_table;
    ToggleButton toggle_button;
    private boolean guess_mode;
    private Dialog guess_1_popup;
    /** This holds the table row id as a key and the id of the card on that row as the value. */
    private Hashtable <String,String> card_row_ids;
    private static final int checkbox_id = 1;
    private static final int input_id = 2;
    private boolean input_mode;
    
    // carried over and re-used in a different way
    /** A list of ids currently in the guess popup.*/
    Vector <String> turn_card_ids;
    /** When the cards.xml file is loaded, cards with the status set to played are added here.
     * Also, if the player answers the definition with the correct text, the card id is added here.*/
    private Vector <String> played_card_ids;
    private Hashtable <CheckBox,String> checkbox_ids;
    private Hashtable <EditText,String> edit_text_ids;
    
    
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_game_writing_stones);
		String method = "onCreate";
		String build = "build 54";
		Log.i(DEBUG_TAG, method+": "+build);
		setup();
		getIntentInfo();
		setTitle(test_name);
		mfc_adapter = NfcAdapter.getDefaultAdapter(this);
		pending_intent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
		ndef_filer = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        try
        { 
        	ndef_filer.addDataType("*/*");
        } catch (MalformedMimeTypeException e) 
        {
        	Log.i(DEBUG_TAG, method+": ndef_filer.addDataType fail");
            throw new RuntimeException("fail", e);
        }
        filters = new IntentFilter[] {ndef_filer,};
        tech_lists_array = new String[][] { new String[] { MifareClassic.class.getName() } };
        setupGameObject();
        printGame(game_file, ": game file after setup.");
        loadCardsFile();
        loadPlayers();
        try
        {
        	printGame(game_file, ": game file before setup.");
        } catch (java.lang.NullPointerException npe)
        {
        	Log.i(DEBUG_TAG, method+": game file npe");
        }
        createScoreboard();
        toggle_button = (ToggleButton)findViewById(R.id.mode_toggle_button);
        toggle_button.setOnClickListener(this);
        toggle_button.setPressed(false);
        toggle_button.setPressed(guess_mode);
        new AsyncLoadGameFileTask().execute("");
	}
	
	@Override
	public void onClick(View v) 
	{
		String method = "onClick";
		if((toggle_button.isChecked()))
		{
			Log.i(DEBUG_TAG, method+" checked");
			guess_mode = true;
		}
		else
		{
			Log.i(DEBUG_TAG, method+" unchecked");
			guess_mode = false;
		}
    }
	
	/**
	 * Load the game file in the background, then call updateScoreFromGame in the 
	 * onPostExecute method after the file is loaded.
	 * @author user
	 *
	 */
	private class AsyncLoadGameFileTask extends AsyncTask <String, String, Game> 
	{  
		private String debug = "YourCustomAsyncTask";
        @Override  
        protected void onPreExecute() 
        {  
            
        }  

        @Override  
        protected Game doInBackground(String ... String) 
        {
        	String method = "doInBackground";
        	Log.i(DEBUG_TAG, method+" start");
        	IWantTo i_want_to = new IWantTo(context);
            Game result_game = i_want_to.loadTheGameFile();
            Log.i(DEBUG_TAG, method+" we got game "+result_game.getTestName());
			return result_game;  
        } 

        @Override  
        protected void onPostExecute(Game result_game) 
        {  
        	String method = "onPostExecute";
        	Log.i(DEBUG_TAG, method+" finished");
        	updateScoreFromGame(result_game);
        }  
	}
	
	/**
	 * Take the scores from the status element of the result_game passed in as an argument
	 * and replace the PlayerInfo object score.
	 * @param result_game
	 */
	private void updateScoreFromGame(Game result_game)
    {
    	String method = "updateScoreFromGame";
    	printGame(result_game, method+": result_game");
    	Log.i(DEBUG_TAG, method+": game file before overwite");
    	printGame(game_file, method+": game_file");
    	game_file = result_game;
    	Hashtable <String,PlayerInfo> temp_players = new Hashtable<String,PlayerInfo>();
    	Enumeration<String> e = players.keys();
    	Log.i(DEBUG_TAG, method+" players size "+players.size());
		while (e.hasMoreElements())
		{
			String key = e.nextElement();
			PlayerInfo this_player_info = players.get(key);
			String this_player_id = this_player_info.getId();
			PlayerInfo new_player_info = new PlayerInfo();
			String player_status = "0";
			try
			{
				player_status = game_file.getPlayerStatus(this_player_id); 
				new_player_info = players.get(key);
				printPlayerInfo(new_player_info, "new player info");
				//player_status = Integer.toString(new_player_info.getScore());
				Log.i(DEBUG_TAG, method+" player_status "+player_status);
			} catch  (java.lang.NullPointerException npe)
			{
				Log.i(DEBUG_TAG, method+" no score yet.  start at 0");
			}
			try
			{
				this_player_info.setScore(Integer.parseInt(player_status));
			} catch (java.lang.NumberFormatException nfe)
			{
				this_player_info.setScore(0);
			}
			printPlayerInfo(this_player_info, "this player info");
			//temp_players.put(this_player_id, this_player_info);
			temp_players.put(this_player_id, new_player_info);
			Log.i(DEBUG_TAG, method+" player_id "+this_player_id+" set score "+this_player_info.getScore());
		}
		players = temp_players;
    	table.removeAllViews();
    	createScoreboard();
    }
	
	/**
	 * We create this table each time there is an update to a players score.
	 * Using the list of ids in id_player_names we get the PlayerInfo and 
	 * retrieve the score and the name and put them in a two column table.
	 */
	private void createScoreboard()
	{
		Enumeration<String> e = id_player_names.keys();
		while (e.hasMoreElements())
		{
			table = (TableLayout) findViewById(R.id.TableLayout01);
	        TableRow row = new TableRow(this);
			String key = e.nextElement();
			String name = id_player_names.get(key);
			PlayerInfo info = players.get(key);
			int score = info.getScore();
			TextView t = new TextView(this);
	        TextView s = new TextView(this);   
	        t.setText(name);
	        s.setText(score+"");
	        row.addView(t);
	        row.addView(s);
	        table.addView(row,new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		}
	}
	
	/**
	 * Initialize all the global variables before they are used.
	 */
	private void setup()
	{
		turn_cards = new Vector<Card>();
		turn_card_ids = new Vector <String>();
        card_pairs = new Vector<Card>();
        played_card_ids = new Vector<String>();
        players = new Hashtable <String,PlayerInfo>();
        player_ids = new Vector<String>();
        id_player_names = new Hashtable<String, String>();
        current_player_id = "";
        previously_played_card_player_id = "";
        previously_played_word_id = "";	 //
        previously_played_word_type = "";	
        previously_played_card_id = "";
        mock_word_counter = 0;
        // added for writing game.
        guess_mode = false;
        card_row_ids = new Hashtable <String,String> ();
        checkbox_ids = new Hashtable <CheckBox,String> ();
        edit_text_ids = new Hashtable <EditText,String> ();
        input_mode = false;
	}
	
	/**
	 * We call intent.getParcelableArrayExtra on the intent to return a Parcelable array
	 * which holds NdefMessage objects which are the raw_messages.  We loop through all these
	 * and cast them to NdefMessage objects and create an array of ndef_messages.
	 * From this array we get an array of NdefRecords, which we loop thru to get each individual
	 * record.  Since all we are looking for is a string of numbers representing a long card id
	 * we simply call record.getPayload() on each record.  We only write to sector 1,
	 * so we get the string id from the first record object.
	 * This is sent to getIds to associate a card from the card file with that id.
	 * @param intent
	 */
	private void resolveIntent(Intent intent)
	{
		String method = "resolveIntent3";
		Log.i(DEBUG_TAG, method+": called.");
		Parcelable[] raw_messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
		NdefMessage[] ndef_messages = null;
		if (raw_messages != null) 
		{
			Log.i(DEBUG_TAG, method+" rawMsgs != null");
			ndef_messages = new NdefMessage[raw_messages.length];
		    for (int i = 0; i < raw_messages.length; i++) 
		    {
		    	NdefMessage ndef_message = (NdefMessage)raw_messages[i];
		        Log.i(DEBUG_TAG, "i "+i+" ndef_message = "+ndef_message.toString());
		        ndef_messages[i] = (NdefMessage) raw_messages[i];
		    }
		} else
		{
			Log.i(DEBUG_TAG, method+" raw_messages == null, or it's a virtual device");
			String test_card_type = (String)intent.getExtras().getString("test_card");
			getTestCard(test_card_type);
		}
		try
		{
			NdefRecord[] records = ndef_messages[0].getRecords();
			final int size = records.length;
			for (int i = 0; i < size; i++) 
			{
				NdefRecord record = records[i];
				String message = new String(record.getPayload());
				Log.i(DEBUG_TAG, method+" "+i+" message "+message);
				if (i==0)
				{
					associateWithCardsFile(message);
				}
			}
		} catch (java.lang.NullPointerException npe)
		{
			Log.i(DEBUG_TAG, method+" npe");
			npe.printStackTrace();
		} catch (java.lang.ArrayIndexOutOfBoundsException aioobe)
		{
			Log.i(DEBUG_TAG, method+" aioobe!");
			aioobe.printStackTrace();
		}
	}
	
	/**
	 * We had a problem with hidden characters, probably from the encoding and language info set into the tags.
	 * This allows only numbers and a '-' character representing the long stored in the cards.xml file.
	 * @param message
	 * @return
	 */
	private String removeHiddenCharacters(String message)
	{
		String dash = "-";
		char d = dash.charAt(0);
		StringBuffer sb = new StringBuffer();
		//Log.i(DEBUG_TAG, "printCharacters");
		for (int i = 0; i < message.length(); i++)
		{
			char c = message.charAt(i);
			//Log.i(DEBUG_TAG, i+" char "+c);
			if (Character.isDigit(c))
			{
				sb.append(c);
			} else if (c == d)
			{
				sb.append(d);
			}
		}
		String result = new String(sb);
		//Log.i(DEBUG_TAG, " result "+result);
		return result;
	}
	
	/**
	 * Go through the cards.xml file and add game cards to the turn_cards Vector.
	 * 
	 * @param tag_id
	 */
	private void associateWithCardsFile(String tag_id)
	{
		String method = "associateWithCardsFile";
		Log.i(DEBUG_TAG, method+" tag_id to match "+tag_id+" from "+cards.size()+" cards.");
		tag_id.trim();
		Enumeration<String> e = cards.keys();
		while (e.hasMoreElements())
		{
			String this_card_id = e.nextElement();
			this_card_id.trim();
			String card_id_str = UtilityTo.encodeThisString(this_card_id, "UTF-8");
			String tag_id_str = removeHiddenCharacters(tag_id);
            Log.i(DEBUG_TAG, method+" to card_id_str "+card_id_str);
			if (card_id_str.equals(tag_id_str))
			{
				Card matching_card = cards.get(this_card_id);
				foundGameCard(matching_card);
				break;
				
			}
		}
	}
	
	/**
	 * All the branching needed when a card is scanned is done in one convenient method.
	 * If guess_mode is set, and input_mode is false, then we call addGuessRow.
	 * If guess_mode is set, and input_mode is true, then we call addInputRow.
	 * If guess_mode is false, then we call addHintRow.
	 * @param game_card
	 */
	private void foundGameCard(Card game_card)
	{
		String method = "foundGameCard";
		Log.i(DEBUG_TAG, method+": found");
		printCard(game_card);
		current_player_id = game_card.getPlayerId();
		Log.i(DEBUG_TAG, method+" previously_played_card_player_id "+previously_played_card_player_id);
		Log.i(DEBUG_TAG, method+" current_player_id "+current_player_id);
		Log.i(DEBUG_TAG, method+" number_of_matches "+number_of_matches);
		if (played_card_ids.contains(game_card.getCardId())||turn_card_ids.contains(game_card.getCardId()))
		{
			Toast.makeText(this, com.curchod.wherewithal.R.string.card_already_played, Toast.LENGTH_LONG ).show();
		} else
		{
			if (previously_played_card_player_id.equals(""))
			{
				Log.i(DEBUG_TAG, method+" first card in turn");
				firstCard(game_card);
			} else
			{
				Log.i(DEBUG_TAG, method+" next card in same player's turn");
				if (samePlayer(game_card.getPlayerId()))
				{
					Log.i(DEBUG_TAG, method+" same player, add row");
					if (guess_mode)
			        {
						if (input_mode)
						{
							Log.i(DEBUG_TAG, method+" input_mode");
							addInputRow(game_card);
						} else
						{
							Log.i(DEBUG_TAG, method+" guess_mode");
							addGuessRow(game_card);
						}
			        } else
			        {
			            Log.i(DEBUG_TAG, method+" hint_mode");
			            addHintRow(game_card);
			        }
				} else
				{
					Log.i(DEBUG_TAG, method+" different player.  first word");
					resetTurn();
					firstCard(game_card);
				}
			}
		}
		//checkForEndOfTurn();
	}
	
	/**
	 * We add definition-text-checkbox rows to the table.
	 * We add each checkbox reference-card id to the checkbox_ids as well as the row id-card id to the card_row_ids
	 * and the card id to the turn_card_ids vector.
	 * @param game_card
	 */
	private void addInputRow(Card game_card)
	{
		String method = "addInputRow";
		guess_table = (TableLayout)guess_1_popup.findViewById(R.id.guess_1_popup_table_layout);
        guess_table.setVerticalScrollBarEnabled(true);
        guess_table.setColumnStretchable(2, true);
		TableRow row = new TableRow(this);
		row.setBackgroundColor(getResources().getColor(R.color.white));
		String text_string = game_card.getText();
		String card_id = game_card.getCardId();
		String defi_string = game_card.getDefinition();
		card_row_ids.put(row.toString(), card_id);
		turn_card_ids.add(card_id);
		Log.i(DEBUG_TAG, method+": text_string "+text_string+" defi_string "+defi_string+" card_id "+card_id);
		TextView defi = new TextView(this);   
		//TextView text = new TextView(this);
		EditText edit_text = new EditText(this);    // Create a new EditText
		LinearLayout.LayoutParams edit_text_params = new LinearLayout.LayoutParams(24, 1);  // doesn't work
		edit_text_params.leftMargin=20;
		//edit_text.setInputType(InputType.TYPE_CLASS_TEXT);   // Setting the type of input that you want   
		int max_length = 20;
		row.setMinimumWidth(max_length);
		edit_text_params.setMargins(edit_text_params.FILL_PARENT, edit_text_params.FILL_PARENT, edit_text_params.FILL_PARENT, edit_text_params.FILL_PARENT);
        InputFilter[] fArray = new InputFilter[1];
        fArray[0] = new InputFilter.LengthFilter(max_length);
        edit_text.setFilters(fArray);
        edit_text.setMinimumWidth(max_length);
		//edit_text.setLayoutParams(new LayoutParams()); // setting height/width for your editText
        edit_text_ids.put(edit_text, card_id);
        defi.setText(defi_string);
        row.addView(defi);
        //row.addView(edit_text, edit_text_params);
        row.addView(edit_text);
        guess_table.addView(row,new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
	}
	
	/**
	 * We add definition-text-checkbox rows to the table.
	 * We add each checkbox reference-card id to the checkbox_ids as well as the row id-card id to the card_row_ids
	 * and the card id to the turn_card_ids vector.
	 * @param game_card
	 */
	private void addGuessRow(Card game_card)
	{
		String method = "addGuessRow";
		guess_table = (TableLayout)guess_1_popup.findViewById(R.id.guess_1_popup_table_layout);
        guess_table.setVerticalScrollBarEnabled(true);
        guess_table.setColumnStretchable(2, true);
		TableRow row = new TableRow(this);
		row.setBackgroundColor(getResources().getColor(R.color.white));
		String text_string = game_card.getText();
		String card_id = game_card.getCardId();
		String defi_string = game_card.getDefinition();
		card_row_ids.put(row.toString(), card_id);
		turn_card_ids.add(card_id);
		Log.i(DEBUG_TAG, method+": text_string "+text_string+" defi_string "+defi_string+" card_id "+card_id);
		TextView defi = new TextView(this);   
		TextView text = new TextView(this);
        CheckBox cbox = new CheckBox(this);
        checkbox_ids.put(cbox, card_id);
        defi.setText(defi_string);
        text.setText(text_string);
        row.addView(defi);
        row.addView(text);
        row.addView(cbox);
        guess_table.addView(row,new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
	}
	
	/**
	 * We create a Dialog with a table and a button.  The table rows will get created in
	 * addRow, which puts the definition-text-checkbox columns in each row, and adds them as each 
	 * consecutive card is read.  When the user clicks on the button, we go through the check boxes and
	 * called the checked ones.  The checked boxes themselves are collected in the checkbox_ids hash
	 * as they are created to keep the reference to the checkbox and the id of the card it represents.
	 * The score is compounded by the number of correct or checked boxes to arrive at the total score.
	 * If the score has changed then we call updateScore.
	 * @param game_card
	 */
	private void createGuessPopupTable(final Card game_card)
	{
		final String method = "createGuessPopupTable";
		if (guess_table != null)
		{
			Log.i(DEBUG_TAG, method+": guess_table is null");
			guess_table.removeAllViews();
		}
		guess_1_popup = new Dialog(this);
    	String player_name = id_player_names.get(current_player_id);
    	guess_1_popup.setTitle("Player "+player_name);
        LayoutInflater li = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = li.inflate(R.layout.game_writing_stones_guess_1_popup, null, false);
        guess_1_popup.setContentView(view);
        guess_1_popup.setCancelable(true);
        guess_1_popup.setOnDismissListener(new OnDismissListener()
        {
        	@Override
            public void onDismiss(final DialogInterface arg0) 
        	{
        		//resetTurn();
            }
        });
        Button button_finish = (Button)guess_1_popup.findViewById(R.id.button_finish);
        button_finish.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
            	if (!input_mode)
            	{
            		Log.i(DEBUG_TAG, method+": input mode not set, calling scoreGuesses");
            		scoreGuesses(game_card);
            	} else
            	{
            		Log.i(DEBUG_TAG, method+": input mode set, calling scoreInputs");
            		scoreInputs(game_card);
            	}
            }
        });
        Log.i(DEBUG_TAG, method+" turn_cards "+turn_cards.size());
        if (input_mode)
		{
			Log.i(DEBUG_TAG, method+" input_mode");
			addInputRow(game_card);
		} else
		{
			Log.i(DEBUG_TAG, method+" guess_mode");
			addGuessRow(game_card);
		}
        guess_1_popup.show();
	}
	
	private void scoreInputs(Card game_card)
	{
		final String method = "button_add_reading.onClick.scoreInputs";
    	number_of_matches = 0;
    	int new_score = 0;
    	Log.i(DEBUG_TAG, method+": Hi there button_finish");
    	Enumeration e = edit_text_ids.keys();
    	while (e.hasMoreElements())
    	{
    		EditText edit_text = (EditText)e.nextElement();
    		String this_card_id = edit_text_ids.get(edit_text);
    		String answer = edit_text.getText().toString();
    		Log.i(DEBUG_TAG, method+" answer "+answer);
    		if (scoreAnswer(answer, game_card))
    		{
    			Log.i(DEBUG_TAG, method+" correct");
    			played_card_ids.add(this_card_id);
    			number_of_matches++;
    	    	new_score = new_score + number_of_matches;
    	    	Log.i(DEBUG_TAG, method+" new score "+new_score+" for id "+this_card_id);
    		} else
    		{
    			Log.i(DEBUG_TAG, method+" answer not correct for id "+this_card_id);
    		}
    	}
    	guess_1_popup.dismiss();
    	checkbox_ids = new Hashtable <CheckBox,String> ();
    	if (number_of_matches>0)
    	{
    		Log.i(DEBUG_TAG, method+" total new score "+new_score);
    		final int updated_score = updateScore(game_card.getPlayerId(), new_score);
    		((Activity) context).runOnUiThread(new Runnable() 
    		{
                public void run() 
                {
                    Toast my_toast = Toast.makeText(context, "New score "+updated_score, Toast.LENGTH_LONG);
                    my_toast.setGravity(Gravity.CENTER, 0, 0);
                    my_toast.show();
                }
            });
    		addScoreToPlayerInfo(new_score, game_card.getPlayerId());
    		updateAndSavePlayerAndGameInfo(game_card, updated_score);
    		resetAfterTurn();
    	} else
    	{
    		Log.i(DEBUG_TAG, method+" score remains the same");
    		resetAfterTurn();
    	}
	}
	
	private boolean scoreAnswer(String answer, Card game_card)
	{
		String method = "scoreAnswer";
		String correct_answer = game_card.getText();
		correct_answer = Scoring.applyOptions(null, correct_answer);
		answer = Scoring.applyOptions(null, answer);
		Log.i(DEBUG_TAG, method+" answer  after apply options "+answer);
		Log.i(DEBUG_TAG, method+" correct after apply options "+correct_answer);
		if (answer.equals(correct_answer))
		{ 
			Log.i(DEBUG_TAG, method+" correct "+game_card.getDefinition()+" "+game_card.getText());
			return true;
		}
		Log.i(DEBUG_TAG, method+" wrong! "+game_card.getDefinition()+" is "+game_card.getText()+" not "+answer);
		return false;
	}
	
	/**
	 * When the score button is pressed in a guess popup, the check boxes are retrieved
	 * and the score is adjusted for checked boxes which indicate correct answers the user
	 * has written down on a piece of paper.  Then we call:
	 * addScoreToPlayerInfo(new_score, game_card.getPlayerId())
     * updateAndSavePlayerAndGameInfo(game_card, updated_score)
     * resetAfterTurn();
     * If now boxes are checked, then we just call resetAfterTurn()
	 * @param game_card
	 */
	private void scoreGuesses(Card game_card)
	{
		final String inner_method = "button_add_reading.onClick";
    	number_of_matches = 0;
    	int new_score = 0;
    	Log.i(DEBUG_TAG, inner_method+": Hi there button_finish");
    	Enumeration e = checkbox_ids.keys();
    	while (e.hasMoreElements())
    	{
    		CheckBox cbox = (CheckBox)e.nextElement();
    		String this_check_box_card_id = checkbox_ids.get(cbox);
    		if (cbox.isChecked())
    		{
    			Log.i(DEBUG_TAG, inner_method+" id "+this_check_box_card_id+" is checked");
    			played_card_ids.add(this_check_box_card_id);
    			number_of_matches++;
    	    	new_score = new_score + number_of_matches;
    	    	Log.i(DEBUG_TAG, inner_method+" new score "+new_score+" for id "+this_check_box_card_id);
    		} else
    		{
    			Log.i(DEBUG_TAG, inner_method+" answer not correct for id "+this_check_box_card_id);
    		}
    	}
    	guess_1_popup.dismiss();
    	checkbox_ids = new Hashtable <CheckBox,String> ();
    	if (number_of_matches>0)
    	{
    		Log.i(DEBUG_TAG, inner_method+" total new score "+new_score);
    		final int updated_score = updateScore(game_card.getPlayerId(), new_score);
    		((Activity) context).runOnUiThread(new Runnable() 
    		{
                public void run() 
                {
                    Toast my_toast = Toast.makeText(context, "New score "+updated_score, Toast.LENGTH_LONG);
                    my_toast.setGravity(Gravity.CENTER, 0, 0);
                    my_toast.show();
                }
            });
    		addScoreToPlayerInfo(new_score, game_card.getPlayerId());
    		updateAndSavePlayerAndGameInfo(game_card, updated_score);
    		resetAfterTurn();
    	} else
    	{
    		Log.i(DEBUG_TAG, inner_method+" score remains the same");
    		resetAfterTurn();
    	}
	}
	
	/**
	 * The hint dialog only shows players the definition-text rows and the button simply dismisses the dialog.
	 * @param game_card
	 */
	private void createHintPopupTable(final Card game_card)
	{
		String method = "createGuessPopupTable";
		if (guess_table != null)
		{
			Log.i(DEBUG_TAG, method+": guess_table is null");
			guess_table.removeAllViews();
		}
		guess_1_popup = new Dialog(this);
    	String player_name = id_player_names.get(current_player_id);
    	guess_1_popup.setTitle("Player "+player_name);
        LayoutInflater li = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = li.inflate(R.layout.game_writing_stones_guess_1_popup, null, false);
        guess_1_popup.setContentView(view);
        guess_1_popup.setCancelable(true);
        guess_1_popup.setOnDismissListener(new OnDismissListener()
        {
        	@Override
            public void onDismiss(final DialogInterface arg0) 
        	{
        		//resetTurn();
            }
        });
        Button button_finish = (Button)guess_1_popup.findViewById(R.id.button_finish);
        button_finish.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
            	final String inner_method = "hint button_add_reading.onClick";
            	guess_1_popup.dismiss();
            	resetAfterTurn();
            }
        });
        Log.i(DEBUG_TAG, method+" turn_cards "+turn_cards.size());
        addHintRow(game_card);
        guess_1_popup.show();
	}
	
	/**
	 * For a hint dialog, we only show the definition-text rows for the player to contemplate.
	 * @param game_card
	 */
	private void addHintRow(Card game_card)
	{
		String method = "addRow";
		guess_table = (TableLayout)guess_1_popup.findViewById(R.id.guess_1_popup_table_layout);
        guess_table.setVerticalScrollBarEnabled(true);
        guess_table.setColumnStretchable(2, true);
		TableRow row = new TableRow(this);
		row.setBackgroundColor(getResources().getColor(R.color.white));
		String text_string = game_card.getText();
		String card_id = game_card.getCardId();
		String defi_string = game_card.getDefinition();
		card_row_ids.put(row.toString(), card_id);
		turn_card_ids.add(card_id);
		Log.i(DEBUG_TAG, method+": text_string "+text_string+" defi_string "+defi_string+" card_id "+card_id);
		TextView defi = new TextView(this);   
		TextView text = new TextView(this);
        defi.setText(defi_string);
        text.setText(text_string);
        row.addView(defi);
        row.addView(text);
        guess_table.addView(row,new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
	}
	
	private void checkForEndOfTurn()
	{
		long current_time = new Date().getTime();
		long elapsed_time = current_time - last_turn_time;
		if (elapsed_time> 15000)
		{
			previously_played_card_player_id = "";
			resetTurn();
		} else
		{
			last_turn_time = current_time;
		}
	}
	
	private void resetTurn()
	{
	    previously_played_word_id = "";
	    previously_played_word_type = "";
	    previously_played_card_id = "";
	    //previously_played_card_player_id = "";
	    previous_card = new Card();
	    number_of_matches = 0;
	    turn_cards = new Vector<Card>();
	    Toast.makeText(this, "Your turn is over.", Toast.LENGTH_LONG ).show();
	}
	
	private void firstCard(Card game_card)
	{
		String method = "firstCard";
		previously_played_word_type = game_card.getWordType(); 		// don't need
		previously_played_word_id = game_card.getWordId();     		// ?
		previously_played_card_player_id = game_card.getPlayerId();	// need
		previously_played_card_id = game_card.getCardId();			// ?
		current_player_id = game_card.getPlayerId();				// need
		previous_card = game_card;									// ?
		Log.i(DEBUG_TAG, method+" set up to show cards");
		turn_cards.add(game_card);									// ok
		turn_card_ids.add(game_card.getCardId());					// ok
		card_pairs.add(game_card);									// dont need											
		last_turn_time = new Date().getTime();
		if (guess_mode)
		{
			Log.i(DEBUG_TAG, method+" guess_mode");
			createGuessPopupTable(game_card);
		} else
		{
			Log.i(DEBUG_TAG, method+" hint_mode");
			createHintPopupTable(game_card);
		}
	}
	
	private void resetAfterTurn()
	{
	    previously_played_word_id = "";
	    previously_played_word_type = "";
	    previously_played_card_id = "";
	    previously_played_card_player_id = "";
	    turn_card_ids = new Vector<String>(); // reset the turn cards.
	    toggle_button.setPressed(false);
	    turn_card_ids = new Vector <String>();
	    
	}
	
	private boolean samePlayer(String game_card_player_id)
	{
		if (game_card_player_id.equals(previously_played_card_player_id))
		{
			return true;
		} else
		{
			return false;
		}
	}
	
	/**
	 * Retuns false even if the card is the same as before.
	 * Only returns true if it is the opposite type
	 * @param game_card_id
	 * @param game_card_type
	 * @return
	 */
	private boolean sameWord(String game_card_id, String game_card_type)
	{
		if (game_card_id.equals(previously_played_word_id))
		{
			if (previously_played_word_type.equals(game_card_type))
			{
				// same card again, right?
				return false;
			} else 
			{
				return true;
			}
		} else
		{
			return false;
		}
			
	}
	
	// update card status
	// reset set previous_player_id and previous_card_id
	// increment matches.
	/**
	 * Save the the game.xml file using the status element as the score.
	 * @param game_card
	 */
	private void updateAndSavePlayerAndGameInfo(Card game_card, int new_score)
	{
		String method = "updateAndSavePlayerAndGameInfo";
		addScoreToPlayerInfo(new_score, game_card.getPlayerId());
		turn_cards.add(game_card);
		turn_card_ids.add(game_card.getCardId());
		card_pairs.add(game_card);  // do we use this?  think not..
    	if (list_adapter != null)
    	{
        	played_card_ids.add(game_card.getCardId());
        	played_card_ids.add(previously_played_card_id);
        	Hashtable <String,Card> new_cards = new Hashtable <String,Card> ();
        	Enumeration<String> e = cards.keys();
    		while (e.hasMoreElements())
    		{
    			String key = e.nextElement();
    			Card card = cards.get(key);
    			String this_id = card.getCardId();
    			if (this_id.equals(game_card.getCardId()) || this_id.equals(previously_played_card_id))
    			{
    				card.setCardStatus(Constants.PLAYED);
    				Log.i(DEBUG_TAG, method+" card "+this_id+" new status "+card.getCardStatus());
    			} else
    			{
    				Log.i(DEBUG_TAG, method+" card "+this_id+" status "+card.getCardStatus());
    			}
    			new_cards.put(this_id, card);
    		}
    		cards = new_cards;
    		showCards();
    		saveCardsFile();
    		IWantTo i_want_to = new IWantTo(context);
            setupGameObject();
            i_want_to.saveTheGameFile(game_file, class_id);
    	} else
    	{
    		Log.i(DEBUG_TAG, method+" list_adapter is null");
    	}
	}
	
	private void addScoreToPlayerInfo(int new_score, String scoring_player_id)
	{
		String method = "addScoreToPlayerInfo";
		Hashtable<String,String> player_statuses = game_file.getPlayerStatus();
		Log.i(DEBUG_TAG, method+" player_statuses "+player_statuses);
		Enumeration<String> e = player_statuses.keys();
		while (e.hasMoreElements())
		{
			String key = e.nextElement();
			String score = player_statuses.get(key);
			Log.i(DEBUG_TAG, method+" key "+key+" score "+score);
			PlayerInfo player_info = players.get(scoring_player_id);
			if (player_info.getId().equals(scoring_player_id))
			{
				player_info.setScore(new_score);
				Log.i(DEBUG_TAG, method+" update player "+player_info.getId()+" score to "+new_score);
				game_file.setPlayerStatus(key, score);
				replacePlayerInfoForPlayer(player_info);
				break;
			}
		}
		updateScoreFromGame(game_file);
	}
	
	private void replacePlayerInfoForPlayer(PlayerInfo player_info)
	{
		String method = "replacePlayerInfoForPlayer";
		Hashtable <String,PlayerInfo> temp_players = new Hashtable<String,PlayerInfo>();
    	Enumeration<String> e = players.keys();
    	Log.i(DEBUG_TAG, method+" players size "+players.size());
		while (e.hasMoreElements())
		{
			String key = e.nextElement();
			PlayerInfo this_player_info = players.get(key);
			String this_player_id = this_player_info.getId();
			if (this_player_id.equals(player_info.getId()))
			{
				Log.i(DEBUG_TAG, method+" update player "+this_player_id+" with new score "+player_info.getScore());
				temp_players.put(this_player_id, player_info);
			} else
			{
				Log.i(DEBUG_TAG, method+" copying player "+this_player_id+" with old score "+this_player_info.getScore());
				temp_players.put(this_player_id, this_player_info);
			}		}
		players = temp_players;
	}
	
	/**
     * Helper method for onOptionsItemSelected.
     * @param right_or_wrong
     * @return
     */
    private NdefRecord getCardRecord(String right_or_wrong)
    {
    	Enumeration<String> e = cards.keys();
        String key = e.nextElement();
        Card card = cards.get(key);
		NdefRecord  record = null;
		try 
		{
			if (right_or_wrong.equals("right"))
			{
				record = createMockRecord2(card.getCardId(), card.getText());
			} else
			{
				record = createMockRecord(card.getCardId(), card.getDefinition());
			}
		} catch (UnsupportedEncodingException uee) 
		{
			// TODO Auto-generated catch block
			uee.printStackTrace();
		}
		return record;
    }
    
    /**
     * Show a popup with the text and deff for both card pairs that do not match 
     * and let the user 'contemplate' thier mistake for 20 seconds, then dismiss it.
     */
    private void showContempationPopup(Card card2)
    {
    	String method = "showContempationPopup";
    	if (list_adapter != null)
    	{
			list_dialog.dismiss();
		}
    	String text1 = previous_card.getText();
    	String def1 = previous_card.getDefinition();
    	String text2 = card2.getText();
    	String def2 = card2.getDefinition();
    	Log.i(DEBUG_TAG, method+" display "+def1+" and "+def1);
    	LayoutInflater layout_inflater = LayoutInflater.from(context);
    	ViewGroup group = (ViewGroup)findViewById(R.id.contemplation_linear_layoutB);
    	final View popup_view = layout_inflater.inflate(R.layout.game_reading_contemplation_window, group);
		final AlertDialog.Builder alert_dialog_builder = new AlertDialog.Builder(context);
		alert_dialog_builder.setView(popup_view);
		final TextView text_view_1 = (TextView) popup_view.findViewById(R.id.textA);
		final TextView def_view_1 = (TextView) popup_view.findViewById(R.id.defA);
		final TextView text_view_2 = (TextView) popup_view.findViewById(R.id.textB);
		final TextView def_view_2 = (TextView) popup_view.findViewById(R.id.defB);
		text_view_1.setText(text1);
		def_view_1.setText(def1);
		text_view_2.setText(text2);
		def_view_2.setText(def2);
		final AlertDialog alert_dialog = alert_dialog_builder.create();
		alert_dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        alert_dialog.show();
        Handler handler = new Handler(); 
        startSlideA(popup_view);
        handler.postDelayed(new Runnable() 
        { 
             public void run() 
             { 
            	 startSlideB(popup_view);
             } 
        }, 5000); 
        handler.postDelayed(new Runnable() 
        { 
             public void run() 
             { 
            	 alert_dialog.dismiss();
             } 
        }, 15000); 
    }
    
    private void startSlideA(View popup_view)
	{
		TextView def_view_1 = (TextView) popup_view.findViewById(R.id.defA);
        Animation def_1_an =  AnimationUtils.loadAnimation(this, R.anim.diagonal_slide);
        def_view_1.startAnimation(def_1_an);
	}
    
    private void startSlideB(View popup_view)
	{
		TextView def_view_2 = (TextView) popup_view.findViewById(R.id.defB);
        Animation def_2_an =  AnimationUtils.loadAnimation(this, R.anim.diagonal_slide);
        def_view_2.startAnimation(def_2_an);
	}
    
    /**
     * Increment player score, copy new hash to old hash, then save the players file.
     * Return the new score;
     * @param player_id
     */
    private int updateScore(String player_id, int new_score)
    {
    	String method = "updateScore";
    	PlayerInfo player_info = players.get(player_id);
    	int score = player_info.getScore();
    	Log.i(DEBUG_TAG, method+" score before update "+score);
    	//previously_played_word_id = "";
    	score = score + new_score;
    	player_info.setScore(score);
    	Hashtable <String,PlayerInfo> temp_players = new Hashtable<String,PlayerInfo>();
    	Enumeration<String> e = players.keys();
    	Log.i(DEBUG_TAG, method+" players size "+players.size());
		while (e.hasMoreElements())
		{
			String key = e.nextElement();
			PlayerInfo this_player_info = players.get(key);
			String this_player_id = this_player_info.getId();
			if (player_id.equals(this_player_id))
			{
				player_info.setScore(score);
				game_file.setPlayerStatus(player_id, Integer.toString(score));
				temp_players.put(player_id, player_info);
				Log.i(DEBUG_TAG, method+" player_id.equals(this_player_id) update score to "+score);
			} else
			{
				temp_players.put(this_player_id, this_player_info);
				Log.i(DEBUG_TAG, method+" player_id "+this_player_id+" score "+this_player_info.getScore());
			}
		}
		players = temp_players;
    	table.removeAllViews();
    	createScoreboard();
    	return score;
    }
    
    /**
     * Set everything back to what it should be before the game started.
     */
    private void resetGame()
    {
    	String method = "resetGame";
    	Log.i(DEBUG_TAG, method+" starting new game");
    	resetAndSavePlayers();
    	table.removeAllViews();
    	createScoreboard();
    	resetAndSaveCards();
		IWantTo i_want_to = new IWantTo(context);
		printGame(game_file, "");
        setupGameObject();
        game_file.setTestStatus(Constants.READY);
        i_want_to.saveTheGameFile(game_file, class_id);
        played_cards = new Hashtable <String,Card> ();
        turn_cards = new Vector <Card> ();
        turn_card_ids = new Vector <String> ();
        card_pairs =  new Vector <Card> ();
        played_card_ids =  new Vector <String> ();
        previously_played_word_id = "";
	    previously_played_word_type = "";
	    previously_played_card_id = "";
	    previously_played_card_player_id = "";
	    previous_card = new Card();
	    number_of_matches = 0;
	    // added for writing game.
	    toggle_button.setPressed(guess_mode);
        card_row_ids = new Hashtable <String,String> ();
        checkbox_ids = new Hashtable <CheckBox,String> ();
        played_card_ids = new Vector <String> ();
        turn_card_ids = new Vector <String> ();
    }
    
    private void resetAndSavePlayers()
    {
    	String method = "resetPlayers";
    	Hashtable <String,PlayerInfo> temp_players = new Hashtable<String,PlayerInfo>();
    	Enumeration<String> e = players.keys();
    	Log.i(DEBUG_TAG, method+" players size "+players.size());
		while (e.hasMoreElements())
		{
			String key = e.nextElement();
			PlayerInfo this_player_info = players.get(key);
			this_player_info.setScore(0);
			temp_players.put(this_player_info.getId(), this_player_info);
		}
		players = temp_players;
		savePlayersFile();
    }
    
    /**
     * Set all cards to 'yet_to_be_played'.
     */
    private void resetAndSaveCards()
    {
    	String method = "resetCards";
    	Hashtable <String,Card> cards_copy = new Hashtable <String,Card> ();
    	Enumeration<String> e = cards.keys();
		while (e.hasMoreElements())
		{
			String this_card_id = e.nextElement();
			Card this_card = cards.get(this_card_id);
			Log.i(DEBUG_TAG, method+" set card "+this_card.getDefinition()+" from "+this_card.getCardStatus()+" to yet to be played");
			this_card.setCardStatus(Constants.YET_TO_BE_PLAYED);
			cards_copy.put(this_card_id, this_card);
		}
		cards = cards_copy;
		saveCardsFile();
    }
    
    private void setupGameObject()
    {
    	String method = "setupGameObject";
    	Game game = new Game();
    	game.setTestFormat("reading_stones");
    	game.setTestId(test_id);
    	game.setTestName(test_name);
    	game.setTestStatus("playing");
    	game.setTestType(Constants.READING);
    	Hashtable <String,String> player_id_status = new Hashtable<String,String>();
    	Enumeration<String> e = players.keys();
    	Log.i(DEBUG_TAG, method+" players size "+players.size());
		while (e.hasMoreElements())
		{
			String key = e.nextElement();
			PlayerInfo player_info = players.get(key);
			player_id_status.put(player_info.getId(), player_info.getScore()+"");
			Log.i(DEBUG_TAG, method+" setting up "+player_info.getId()+" player status "+player_info.getScore());
		}
		game.setPlayerStatus(player_id_status);
		printGame(game_file, method+" game_file before setup");
		game_file = game;
		printGame(game_file, method+" game_file after setup");
    }
    
    /**
     * Create the dialog to show a list of scanned cards.
     * This will provide a list of the current turn cards for a single player
     * until a card from a new player is scanned and a new list starts.
     */
    private void showCards()
    {
    	String method = "showCards";
    	if (list_adapter != null)
    	{
			list_dialog.dismiss();
		}
    	list_dialog = new Dialog(this);
    	String player_name = id_player_names.get(current_player_id);
    	list_dialog.setTitle("Player "+player_name);
        LayoutInflater li = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = li.inflate(R.layout.game_reading_stones_popup, null, false);
        list_dialog.setContentView(v);
        list_dialog.setCancelable(true);
        list_dialog.setOnDismissListener(new OnDismissListener()
        {
        	@Override
            public void onDismiss(final DialogInterface arg0) 
        	{
        		//resetTurn();
            }
        });
        Log.i(DEBUG_TAG, method+" turn_cards "+turn_cards.size());
        String[] val = new String [turn_cards.size()];
        for (int i = 0; i < turn_cards.size(); i++)
        {
        	Card card = turn_cards.get(i);
        	val [i] = UtilityTo.getWord(card);
        }
        ListView list1 = (ListView) list_dialog.findViewById(R.id.popup_listview);
        list_adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, val);
        list1.setAdapter(list_adapter);
        list_dialog.show();
    }
    
    /**
     * For testing.  Currently this doesn't work?
     * @param card_id
     * @param word
     * @return
     * @throws UnsupportedEncodingException
     */
    private NdefRecord createMockRecord(String card_id, String word) throws UnsupportedEncodingException 
    {
    	String method = "createMockRecord";
    	Log.i(DEBUG_TAG, method);
        String lang = "en";
        String card_info = card_id+"@"+word;
        Log.i(DEBUG_TAG, method+" card_info "+card_info);
        byte [] card_bytes = card_info.getBytes();
        byte [] lang_bytes = lang.getBytes("UTF-8");
        int lang_length = lang_bytes.length;
        int card_info_length = card_bytes.length;
        Log.i(DEBUG_TAG, method+" lang_leng "+lang_length+" "+card_info_length);
        byte [] payload = new byte[1 + lang_length + card_info_length];
        payload[0] = (byte) lang_length;
        System.arraycopy(lang_bytes, 0, payload, 1, lang_length);
        System.arraycopy(card_bytes, 0, payload, 1 + lang_length, card_info_length);
        NdefRecord recordNFC = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);
        byte [] payload1 = recordNFC.getPayload();
        Log.i(DEBUG_TAG, method+": payload check "+payload1.length);
        return recordNFC;
  }
    
    /**
     * For testing in a virtual device.
     * @param card_id
     * @param word
     * @return
     * @throws UnsupportedEncodingException
     */
    private NdefRecord createMockRecord2(String card_id, String word) throws UnsupportedEncodingException 
    {
    	String method = "createMockRecord";
        String card_info = "en"+card_id+"@"+word;
        Log.i(DEBUG_TAG, method+" card_info "+card_info);
        byte [] card_bytes = card_info.getBytes();
        int card_info_length = card_bytes.length;
        Log.i(DEBUG_TAG, method+" info length "+card_info_length);
        byte [] payload = new byte[1 + card_info_length];
        UtilityTo.printBytes(payload, "payload");
        System.arraycopy(card_bytes, 0, payload, 1, card_info_length);
        NdefRecord recordNFC = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);
        byte [] payload1 = recordNFC.getPayload();
        Log.i(DEBUG_TAG, method+": payload length "+payload1.length);
        UtilityTo.printBytes(payload1, "payload1");
        return recordNFC;
  }
    
    /**
     * Lload the cards.xml file and then call parseCards to do it.
     * We need to know what the last element in the card will be to finish each card object.
     * Right now, this is definition.
     * The full format is:
     *  <cards>
     *     <card>
     *         <card_id>
     *         <player_id>
     *         <player_name>
     *         <player_icon>
     *         <word_id>
     *         <word_type>
     *         <word_category>
     *         <text>
     *         <definition>
     *     </card> 
     *     ...
     * </cards>
     */
    private void parseCards()
    {
    	final String method = "parseCards"; 
    	//Log.i(DEBUG_TAG, method+": start parse");
    	new Thread() 
    	{
            public void run() 
            {
            	FileInputStream fis = null;
				try 
				{
					fis = openFileInput(Constants.CARDS_XML);
					Log.i(DEBUG_TAG, method+": fis "+fis.available());
				} catch (FileNotFoundException e1) 
				{
					Log.i(DEBUG_TAG, method+": fnfe");
					e1.printStackTrace();
				} catch (java.io.IOException ioe)
				{
					Log.i(DEBUG_TAG, method+": ioe");
					ioe.printStackTrace();
				}
                try 
                {
                	Card card = new Card();
                	String tag = "";
                    XmlPullParserFactory parserCreator = XmlPullParserFactory.newInstance();
                    XmlPullParser parser = parserCreator.newPullParser();
                    parser.setInput(fis, null);
                    int parser_event = parser.getEventType();
                    while (parser_event != XmlPullParser.END_DOCUMENT) 
                    {
                        switch (parser_event) 
                        {
                        case XmlPullParser.TEXT:
                        	if (true)
                        	{
                        		if (tag!=null)
                        		{
                        			String value = parser.getText();
                        			if (tag.equals("card_id"))
                        			{
                        				card.setCardId(value);
                        				Log.i(DEBUG_TAG, "parsed card id: "+tag+" "+value);
                        			} else if (tag.equals("player_id"))
                        			{
                        				card.setPlayerId(value);
                        				player_ids.add(value);
                        				Log.i(DEBUG_TAG, "parsed: "+tag+" "+value);
                        			} else if (tag.equals("card_status"))
                        			{
                        				card.setCardStatus(value);
                        				if (value.equals(Constants.PLAYED))
                        				{
                        					played_card_ids.add(card.getCardId());
                        					Log.i(DEBUG_TAG, "card played");
                        				}
                        				Log.i(DEBUG_TAG, "parsed: "+tag+" "+value);
                        			} else if (tag.equals("player_name"))
                        			{
                        				card.setPlayerName(value);
                        				//Log.i(DEBUG_TAG, "parsed: "+tag+" "+value);
                        			} else if (tag.equals("word_id"))
                        			{
                        				card.setWordId(value);
                        				//Log.i(DEBUG_TAG, "parsed: "+tag+" "+value);
                        			} else if (tag.equals("word_type"))
                        			{
                        				card.setWordType(value);
                        				//Log.i(DEBUG_TAG, "parsed: word_type "+tag+" "+value);
                        			} else if (tag.equals("word_category"))
                        			{
                        				card.setWordCategory(value);
                        				Log.i(DEBUG_TAG, "parsed: "+tag+" "+value);
                        			} else if (tag.equals("text"))
                        			{
                        				card.setText(value);
                        				Log.i(DEBUG_TAG, "parsed: "+tag+" "+value);
                        			} else if (tag.equals("definition"))
                        			{
                        				card.setDefinition(value);
                        				cards.put(card.getCardId(),card);
                        				Log.i(DEBUG_TAG, "parse cards file: put "+card.getCardId()+" "+UtilityTo.getWord(card));
                        				card = new Card();
                        			}
                        		}
                        		tag = null;
                        	}
                        	//Log.i(DEBUG_TAG, method+": parser.getText() "+parser.getText());
                        		
                        case XmlPullParser.START_TAG:
                        	tag = parser.getName();
                        	//Log.i(DEBUG_TAG, "catpture set "+tag);
                        }
                        parser_event = parser.next();
                    }
                } catch (Exception e) 
                {
                	Log.i(DEBUG_TAG, method+": (not) exception(al)");
                	e.printStackTrace();
                }
            }
    	}.start();
    	//Log.i(DEBUG_TAG, method+": finished");
    }
    
    /**
	 * Set the test name id and status form intent.
	 */
	private void getIntentInfo()
	{
		player_ids = new Vector<String>();
		id_player_names = new Hashtable<String, String>();
		String method = "getIntentInfo";
		Intent sender = getIntent();
		test_id = sender.getExtras().getString("test_id");
		test_name = sender.getExtras().getString("test_name"); // test_name
		test_status = sender.getExtras().getString("test_status");
		try
		{
			int number_of_players = Integer.parseInt(sender.getExtras().getString("number_of_players"));
			class_id = sender.getExtras().getString("class_id");
			for (int i = 0; i < number_of_players; i++)
			{
				String player_id =  sender.getExtras().getString(i+"player_id");
				String player_name =  sender.getExtras().getString(i+"player_name");
				Log.i(DEBUG_TAG, method+" retrieved player id "+player_id+", player name "+player_name);
				id_player_names.put(player_id, player_name);
				player_ids.add(player_id);
			}
			Log.i(DEBUG_TAG, method+" got test_name "+test_name+" test_status "+test_status+" test_id "+test_id);
		} catch (java.lang.NumberFormatException nufie)
		{
			Log.i(DEBUG_TAG, method+" NuFiE!");
			nufie.printStackTrace();
		}
	}
	
    /**
     * This method sets up whats needed to open the cards.xml file. 
     * If the file doesn't exist yet, create it.
     * Then call the parseCards() method.
     */
    private void loadCardsFile()
    {
    	String method = "loadCardsFile";
    	cards = new Hashtable <String,Card>();
    	Context context = getApplicationContext();
    	String file_path = context.getFilesDir().getAbsolutePath();//returns current directory.
    	Log.i(DEBUG_TAG, method+": file_path - "+file_path);
    	File file = new File(file_path, Constants.CARDS_XML);
    	boolean exists = file.exists();
    	if (exists == false)
    	{
    		// what do we do if there is no file yet?
    		Toast.makeText(this, "Please set up cards brefore game play", Toast.LENGTH_LONG ).show();
    		Log.i(DEBUG_TAG,"create new cards.xml file?");
    	} else
    	{
    		parseCards();
    	}
    }
    
    /**
     * Save all the card info in the card.xml file, which has the following format:
     * We use the file_cards vector which represents cards from the cards we read and write.
     * <cards>
     *     <card>
     *         <card_id>
     *         <player_id>
     *         <player_name>
     *         <player_icon>
     *         <word_id>
     *         <word_type>
     *         <word_category>
     *         <text>
     *         <definition>
     *     </card> 
     *     ...
     * </cards>
     */
    private void saveCardsFile()
    {
    	String method = "savedCardsFile";
        //Log.i(DEBUG_TAG, method);
    	try 
    	{
    		FileOutputStream fos = openFileOutput(Constants.CARDS_XML, Context.MODE_PRIVATE);
    		//Log.i(DEBUG_TAG, method+": FD "+fos.getFD());
	        try
	        {
	        	StringBuffer sb = new StringBuffer();
				sb.append("<cards>");
				Enumeration<String> e = cards.keys();
				while (e.hasMoreElements())
				{
					String key = e.nextElement();
					Card card = cards.get(key);
					sb.append("<card>");
						sb.append("<card_id>"+card.getCardId()+"</card_id>");
						sb.append("<card_status>"+card.getCardStatus()+"</card_status>");
						sb.append("<player_id>"+card.getPlayerId()+"</player_id>");
						sb.append("<player_name>"+card.getPlayerName()+"</player_name>");
						sb.append("<player_icon>"+card.getPlayerIcon()+"</player_icon>");
						sb.append("<word_id>"+card.getWordId()+"</word_id>");
						sb.append("<word_type>"+card.getWordType()+"</word_type>");
						sb.append("<word_category>"+card.getWordCategory()+"</word_category>");
						sb.append("<text>"+card.getText()+"</text>");
						sb.append("<definition>"+card.getDefinition()+"</definition>");
					sb.append("</card>");
					Log.i(DEBUG_TAG, " card.getCardId(): "+card.getCardId()+" word "+UtilityTo.getWord(card));
				}
				sb.append("</cards>");
				Log.i(DEBUG_TAG, method+" writing "+new String(sb));
				fos.write(new String(sb).getBytes());
				fos.close();
				//Log.i(DEBUG_TAG, method+": done");
	         }catch (FileNotFoundException e)
	        {
	            Log.e(DEBUG_TAG, "FileNotFoundException");
			} catch (IOException e1) 
			{
				Log.e(DEBUG_TAG, "IOException");
				e1.printStackTrace();
			}
		} catch (IOException e) 
		{
			e.printStackTrace();
		}
    }
    
    /**
     * Setup to parse the players.xml file.  If there is no file, create one and populate it
     * with the info in the game file and asign random icons to players.
     */
    private void loadPlayers()
    {
    	String method = "loadPlayers";
    	Context context = getApplicationContext();
    	String file_path = context.getFilesDir().getAbsolutePath();//returns current directory.
    	Log.i(DEBUG_TAG, method+": file_path - "+file_path);
    	File players_file = new File(file_path, Constants.PLAYERS_XML);
    	boolean exists = players_file.exists();
    	Log.i(DEBUG_TAG, method+": exists? "+exists);
    	if (exists)
    	{
    		Log.i(DEBUG_TAG, method+": parse players.xml and merge with game players");
    		parsePlayers(Constants.PLAYERS_XML);
    		Log.i(DEBUG_TAG, method+": before merge");
    		printPlayers();
    		mergeGamePlayersWithFilePlayers();
    		Log.i(DEBUG_TAG, method+": after merge");
    		printPlayers();
    		savePlayersFile();
    	} else
    	{
    		Log.i(DEBUG_TAG, method+": create new players.xml and populate it from the inten.");
    		createNewPlayersFile();
    		populatePlayersFromIntent();
    		savePlayersFile();
    	}
    }
    
    /**
     * After parsing the cards and the players file, 
     * we create PlayerInfo objects for each player that has an id in player_ids
     * but doesn't already have an object in the players hash.
     * This vould happen if the players haven't logged in, and someone else sets up the game
     * and they want to go ahead and play without logging in.
     */
    private void mergeGamePlayersWithFilePlayers()
    {
    	String method = "mergeGamePlayersWithFilePlayers";
    	for (int i = 0; i < player_ids.size(); i++)
    	{
    		String this_player_id = player_ids.get(i);
    		Log.i(DEBUG_TAG, method+": this_player_id "+this_player_id);
    		if (!players.containsKey(this_player_id))
    		{
    			String player_name = id_player_names.get(this_player_id);
    			PlayerInfo player_info = new PlayerInfo(player_name, 0, this_player_id, "crab_228");
    			players.put(this_player_id, player_info);
    			Log.i(DEBUG_TAG, method+": put "+this_player_id+" into players hash named "+player_name);
    		} else
    		{
    			Log.i(DEBUG_TAG, method+": player "+this_player_id+" not in game");
    		}
    	}
    }
    
    /**
     * If there are no players logged in, we setup the player info from the ids sent in 
     * in the intent.
     */
    private void populatePlayersFromIntent()
    {
    	for (int i = 0; i < player_ids.size(); i++)
    	{
    		String player_id = player_ids.get(i);
    		PlayerInfo player_info = new PlayerInfo();
    		player_info.setId(player_id);
    		player_info.setName(player_id);
    		player_info.setScore(0);
    		player_info.setIcon("crab_228");
    		players.put(player_id, player_info);
    	}
    }
    
	/**
	 * Using a string buffer, we create the initial players.xml file with a new entry for the first player with a default icon name.
	 */
	private void createNewPlayersFile()
    {
    	//Log.i(DEBUG_TAG, method+": Using a string buffer, we create the initial players.xml file with a new entry for the first player with a default icon name.");
    	try 
    	{
    		FileOutputStream fos = openFileOutput(Constants.CARDS_XML, Context.MODE_PRIVATE);
	        try
	        {
	        	StringBuffer sb = new StringBuffer();
				sb.append("<players/>");
				fos.write(new String(sb).getBytes());
				fos.close();
	        }catch(FileNotFoundException e)
	        {
	            Log.e(DEBUG_TAG, "FileNotFoundException");
			} catch (IOException e1) 
			{
				Log.e(DEBUG_TAG, "IOException");
				e1.printStackTrace();
			}
		} catch (IOException e) 
		{
			e.printStackTrace();
		}
    }
    
    /**
     * Parse the players.xml file.
     * As with other parsings, the final element indicates when to finish each player and 
     * add a PlayerInfo object to the players hash.
     * @param filename
     * @param pa
     */
    private void parsePlayers(final String filename)
    {
    	final String method = "parsePlayers"; 
    	Log.i(DEBUG_TAG, method+": start parse");
    	new Thread() 
    	{
            public void run() 
            {
            	FileInputStream fis = null;
				try 
				{
					fis = openFileInput(filename);
					Log.i(DEBUG_TAG, method+": fis "+fis.available());
				} catch (FileNotFoundException e1) 
				{
					Log.i(DEBUG_TAG, method+": fnfe");
					e1.printStackTrace();
				} catch (java.io.IOException ioe)
				{
					Log.i(DEBUG_TAG, method+": ioe");
					ioe.printStackTrace();
				}
                try 
                {
                	String tag = "";String name = "";int score = 0;String id = "";
                    XmlPullParserFactory parserCreator = XmlPullParserFactory.newInstance();
                    XmlPullParser parser = parserCreator.newPullParser();
                    parser.setInput(fis, null);
                    int parser_event = parser.getEventType();
                    while (parser_event != XmlPullParser.END_DOCUMENT) 
                    {
                        switch (parser_event) 
                        {
                        case XmlPullParser.TEXT:
                        	if (true)
                        	{
                        		if (tag!=null)
                        		{
                        			String value = parser.getText();
                        			if (tag.equals("name"))
                        			{
                        				name = value;
                        				Log.i(DEBUG_TAG, method+": name "+name);
                        			} else if (tag.equals("score"))
                        			{
                        				score = Integer.parseInt(value);
                        				//Log.i(DEBUG_TAG, method+": score "+score);
                        			} else if (tag.equals("id"))
                        			{
                        				id = value;
                        				//Log.i(DEBUG_TAG, method+": id "+id);
                        			} else if (tag.equals("icon"))
                        			{
                        				PlayerInfo player_info = new PlayerInfo(name, score, id, value);
                        				players.put(id, player_info);
                        				if (!player_ids.contains(id))
                        				{
                        					player_ids.add(id);
                        					Log.i(DEBUG_TAG, method+": added to players_ids");
                        				}
                        				Log.i(DEBUG_TAG, method+": name "+name+" id "+id+" icon "+value+" score "+score);
                        			}
                        		}
                        		tag = null;
                        	}
                        		
                        case XmlPullParser.START_TAG:
                        	tag = parser.getName();
                        }
                        parser_event = parser.next();
                    }
                } catch (Exception e) 
                {
                	Log.i(DEBUG_TAG, method+": (not) exception(al)");
                	e.printStackTrace();
                }
            }
    	}.start();
    	Log.i(DEBUG_TAG, method+": finished");
    }
    
    /**
     * Write all the PlayerInfo objects as xml elements
     * @param path_to_players_files
     * @param name
     * @param id
     */
    public void savePlayersFile()
    {
    	String method = "savePlayersFile";
        Log.i(DEBUG_TAG, method+": players.size() "+players.size());
    	try 
    	{
    		FileOutputStream fos = context.openFileOutput("players.xml", Context.MODE_PRIVATE);
    		Log.i(DEBUG_TAG, method+": FD "+fos.getFD());
	        try
	        {
	        	StringBuffer sb = new StringBuffer();
				sb.append("<players>");
				Enumeration<String> e = players.keys();
				while (e.hasMoreElements())
				{
					String key = e.nextElement();
					PlayerInfo info = players.get(key);
					sb.append("<player>");
					sb.append("<name>"+info.getName()+"</name>");
					sb.append("<score>"+info.getScore()+"</score>");
					sb.append("<id>"+info.getId()+"</id>");
					sb.append("<icon>"+info.getIcon()+"</icon>");
					sb.append("</player>");
					Log.i(DEBUG_TAG, method+" added " + info.getName());
				}
				sb.append("</players>");
				Log.i(DEBUG_TAG, "writing "+sb.toString());
				fos.write(new String(sb).getBytes());
				fos.close();
				Log.i(DEBUG_TAG, method+": done");
	        }catch(FileNotFoundException e)
	        {
	            Log.e(DEBUG_TAG, "FileNotFoundException");
			} catch (IOException e1) 
			{
				Log.e(DEBUG_TAG, "IOException");
				e1.printStackTrace();
			}
		} catch (IOException e) 
		{
			e.printStackTrace();
		}
    }
    
    /**
	  * This is called each time a NFC tag is detected.
	  */
    @Override
    public void onNewIntent(Intent intent) 
    {
        //Log.i(DEBUG_TAG, "onNewIntent - Foreground dispatch:  action: " + intent.getAction());   
        resolveIntent(intent);   
    }

    @Override
    public void onPause() 
    {
        super.onPause();
        try
        {
        	mfc_adapter.disableForegroundDispatch(this);
        } catch (java.lang.NullPointerException npe)
        {
        	Log.i(DEBUG_TAG, "unable to disable Foreground Dispatch");
        	npe.printStackTrace();
        }
    }
	
    int next_card_menu_item_id = 100;
    /**
     * Testing card options, and a selection to get instructions on this game.
     * A player can also choose to reset the game.
     */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		super.onCreateOptionsMenu(menu);
		//menu.add("Next Card").setIcon(android.R.drawable.star_big_off);
		menu.add(0, next_card_menu_item_id, 0, "Next Card").setIcon(android.R.drawable.star_big_off);
		SubMenu icon_choice = menu.addSubMenu(R.string.checkbox_or_input_answers).setIcon(android.R.drawable.ic_menu_gallery);
		icon_choice.add(icon_group, checkbox_id, 1, R.string.checkbox);
		icon_choice.add(icon_group, input_id, 2, R.string.input);
		menu.add(0 , instructions_menu_item_id, 0, R.string.instructions_menu_item);
		menu.add(0 , reset_game_menu_item_id, 0, R.string.reset_game_menu_item);
		return true;
	}
	
	/**
	 * This method captures the NFC adapter which will be used to scan tags.
	 * This is part of the "Foreground Dispatch" system.
	 */
	 @Override
     public void onResume() 
	 {
         super.onResume();
         try
         {
        	 mfc_adapter.enableForegroundDispatch(this, pending_intent, filters, tech_lists_array);
         } catch (java.lang.NullPointerException npe)
         {
       	  	Log.i(DEBUG_TAG, "onResume:  npeee!");
       	  	mfc_adapter = NfcAdapter.getDefaultAdapter(context);
       	  	//nfc_adapter.enableForegroundDispatch(this,pendingIntent,writeTagFilters,techListsArray);
         }
     }
	
	/**
     * When testing on a virtual device we want a way to choose the next correct or incorrect card.
     */
    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	String method = "onOptionsItemSelected(MenuItem)";
    	String selected = item.toString();
    	Log.i(DEBUG_TAG, method+": selected "+selected);
    	getIntent();
    	if (item.getItemId() == checkbox_id)
    	{
    		input_mode = false;
    		toggle_button.setText("Guess");
    	    return true;
    	} else if (item.getItemId() == input_id)
    	{
    		input_mode = true;
    		toggle_button.setText("Input");
    	    return true;
    	} else if (item.getItemId() == instructions_menu_item_id)
    	{
    		Intent intent = new Intent(GameWritingStonesActivity.this, GameReadingStonesInstructionsActivity.class);
    		startActivity(intent);
    	} else if (item.getItemId() == reset_game_menu_item_id)
    	{
    		resetGame();
    	} else if (selected.equals("Next Card")||item.getItemId() == next_card_menu_item_id)
    	{
    		Log.i(DEBUG_TAG, method+": calling testCards"); 
    		testCard();
    	}
    	return super.onOptionsItemSelected(item);
    }
    
    /**
	 * Pcik the first card from the enumeration and send that id to the foundGameCard method. 
	 * @param tag_id
	 */
	private void testCard()
	{
		String method = "testCards";
		Log.i(DEBUG_TAG, method+" cards "+cards.size());
		Enumeration<String> e = cards.keys();
		while (e.hasMoreElements())
		{
			String this_card_id = e.nextElement();
			Card matching_card = cards.get(this_card_id);
			if (!played_card_ids.contains(this_card_id))
			{
				Log.i(DEBUG_TAG, method+" using "+matching_card.getDefinition());
				foundGameCard(matching_card);
				break;
			}
		}
	}
    
    private int mock_word_counter;
    
    private void getTestCard(String test_card_type)
    {
    	int i = 0;
    	Enumeration<String> e = cards.keys();
    	while (e.hasMoreElements())
    	{
    		String key = e.nextElement();
    		Card card = cards.get(key);
    		i++;
    		if (i>mock_word_counter)
    		{
    			mock_word_counter++;
    			foundGameCard(card);
    		}
    	}
    }
    
    private void printPlayers()
    {
    	String method = "printPlayers";
    	Enumeration<String> e = players.keys();
    	Log.i(DEBUG_TAG, method+" players size "+players.size());
		while (e.hasMoreElements())
		{
			String key = e.nextElement();
			PlayerInfo this_player_info = players.get(key);
			Log.i(DEBUG_TAG, method+": name "+this_player_info.getName());
			Log.i(DEBUG_TAG, method+": id "+this_player_info.getId());
			Log.i(DEBUG_TAG, method+": score "+this_player_info.getScore());
			Log.i(DEBUG_TAG, method+": icon "+this_player_info.getIcon());
		}
    }
    
    private void printCard(Card card)
    {
    	Log.i(DEBUG_TAG, "printCard --------");
    	Log.i(DEBUG_TAG, "card "+card.getCardId());
    	Log.i(DEBUG_TAG, "card "+card.getCardStatus());
    	Log.i(DEBUG_TAG, "card "+card.getDefinition());
    	Log.i(DEBUG_TAG, "card "+card.getIndex());
    	Log.i(DEBUG_TAG, "card "+card.getPlayerIcon());
    	Log.i(DEBUG_TAG, "card "+card.getPlayerId());
    	Log.i(DEBUG_TAG, "card "+card.getPlayerName());
    	Log.i(DEBUG_TAG, "card "+card.getText());
    	Log.i(DEBUG_TAG, "card "+card.getWordCategory());
    	Log.i(DEBUG_TAG, "card "+card.getWordId());
    	Log.i(DEBUG_TAG, "card "+card.getWordType());
    	Log.i(DEBUG_TAG, "printCard --------");
    }
    
    private void printGame(Game game, String message)
    {
    	try
    	{
    		Log.i(DEBUG_TAG, "Game -------- "+message);
    		Log.i(DEBUG_TAG, "class id "+game.getClassId());
    		Log.i(DEBUG_TAG, "test format "+game.getTestFormat());
    		Log.i(DEBUG_TAG, "test id "+game.getTestId());
    		Log.i(DEBUG_TAG, "test status "+game.getTestStatus());
    		Log.i(DEBUG_TAG, "test name "+game.getTestName());
    		Log.i(DEBUG_TAG, "test type "+game.getTestType());
    		Enumeration<String> e = players.keys();
    		while (e.hasMoreElements())
    		{
    			String key = (String) e.nextElement();
    			PlayerInfo this_player_info = players.get(key);
    			String status = game.getPlayerStatus(key);
    			Log.i(DEBUG_TAG, "Player status "+status);
    			Log.i(DEBUG_TAG, "Player )(--");
    			Log.i(DEBUG_TAG, "name "+this_player_info.getName());
				Log.i(DEBUG_TAG, "id "+this_player_info.getId());
				Log.i(DEBUG_TAG, "score "+this_player_info.getScore());
				Log.i(DEBUG_TAG, "icon "+this_player_info.getIcon());
				Log.i(DEBUG_TAG, "Player )(--");
    		}
    		Log.i(DEBUG_TAG, "Game --------");
    	} catch (java.lang.NullPointerException npe)
    	{
    		Log.i(DEBUG_TAG, "No game file to print");
    	}
    }
    
    private void printPlayerInfo(PlayerInfo this_player_info, String message)
    {
    	Log.i(DEBUG_TAG, "Player )))) - "+message+" - (((((");
		Log.i(DEBUG_TAG, "name "+this_player_info.getName());
		Log.i(DEBUG_TAG, "id "+this_player_info.getId());
		Log.i(DEBUG_TAG, "score "+this_player_info.getScore());
		Log.i(DEBUG_TAG, "icon "+this_player_info.getIcon());
		Log.i(DEBUG_TAG, "Player )    ----------------    (");
    }
    
}
