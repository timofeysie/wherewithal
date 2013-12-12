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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.curchod.domartin.Card;
import com.curchod.domartin.Game;
import com.curchod.domartin.IWantTo;
import com.curchod.domartin.PlayerInfo;
import com.curchod.domartin.TagDescription;
import com.curchod.domartin.UtilityTo;

/**
 * When the user clicks on the Reading Stones button, the onClick calls loadGameFile() 
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
 * 
 * After a player has matched all their cards, the game enters the final round.  Remaining players can
 * match their left over cards for 1 point each per match.  
 * @author Administrator
 *
 */
public class GameReadingStonesActivity extends Activity 
{
	
	private static final String DEBUG_TAG = "GameReadingStonesActivity";
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
	private static final int right_card_id = 1;
    private static final int wrong_card_id = 2;
    private static final int icon_group = 1;
    private static final int instructions_menu_item_id = 3;
    private static final int reset_game_menu_item_id = 4;
    Hashtable <String,Card> played_cards;
    /** During each players turn, scanned cards are kept in this list.*/
    Vector <Card> turn_cards;
    /** This will hold all the card ids a players scans during their turn.*/
    Vector <String> turn_card_ids;
    ArrayAdapter<String> array_dapter;
    private Dialog list_dialog;
    private ListAdapter list_adapter;
    /** This stores two consecutive scanned cards to see if they match.*/
    private Vector <Card> card_pairs;
    /** When the cards.xml file is loaded, cards with the status set to played are added here.
     * Also, if the player matches two cards, both card ids are added to played_card_ids.*/
    private Vector <String> played_card_ids;
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
    /** After a player has one, the remaining players have a chance to match their remaining cards and get 1 point for each match when this has been set.*/
    private boolean final_round;
    /** Used to change the game status from ready to underway to final_round.*/
    private TextView game_status;
    boolean end_game;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_game_reading_stones);
		String method = "onCreate";
		String build = "build 157b";
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
        new AsyncLoadGameFileTask().execute("");
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
		updateGameStatus();
	}
	
	/**
	 *     <string name="game_ready">Game Ready</string>
     *     <string name="playing">Playing</string>
     *     <string name="final_round">Final Round</string>
     *     <string name="game_over">Game Over</string>
	 */
	private void updateGameStatus()
	{
		String status_string = game_file.getTestStatus();
		if (status_string.equals("setup"))
		{
			game_status.setText(R.string.game_ready);
		} else if (status_string.equals(UtilityTo.GAME_OVER))
		{
			game_status.setText(R.string.game_over);
		} else if (status_string.equals(UtilityTo.FINAL_ROUND))
		{
			game_status.setText(R.string.final_round);
		} else if (status_string.equals(UtilityTo.PLAYING))
		{
			game_status.setText(R.string.playing);
		}
	}
	
	/**
	 * Initialize all the global variables before they are used.
	 */
	private void setup()
	{
		game_status = (TextView)findViewById(R.id.text_view_game_status);
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
        final_round = false;
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
			Log.i(DEBUG_TAG, method+" Problemo!");
			npe.printStackTrace();
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
	 * @param game_card
	 */
	private void foundGameCard(Card game_card)
	{
		String method = "foundGameCard";
		Log.i(DEBUG_TAG, method+": found");
		try
		{
		printCard(game_card);
		} catch (java.lang.NullPointerException npe)
		{
			Log.i(DEBUG_TAG, method+" can't print card");
		}
		current_player_id = game_card.getPlayerId();
		Log.i(DEBUG_TAG, method+" previously_played_card_player_id "+previously_played_card_player_id);
		Log.i(DEBUG_TAG, method+" current_player_id "+current_player_id);
		Log.i(DEBUG_TAG, method+" number_of_matches "+number_of_matches);
		if (played_card_ids.contains(game_card.getCardId()))
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
				// second card
				if (samePlayer(game_card.getPlayerId()))
				{
					if (sameWord(game_card.getWordId(), game_card.getWordType()))
					{
						Log.i(DEBUG_TAG, method+" same player, same word, match");
						int new_score = updateScore(game_card.getPlayerId());
	    				Toast.makeText(this, "Match! Score "+new_score, Toast.LENGTH_LONG ).show();
	    				updateAndSavePlayerAndGameInfo(game_card, new_score);
						resetAfterMatch();
					} else if (previously_played_word_id.equals("")&&number_of_matches>0)
					{
						Log.i(DEBUG_TAG, method+" first card after match");
						firstCard(game_card);
					} else
					{
						Log.i(DEBUG_TAG, method+" same player wrong word: contemplation");
						showContempationPopup(game_card);
						resetTurn();
					}
				} else
				{
					Log.i(DEBUG_TAG, method+" different player.  first word");
					resetTurn();
					firstCard(game_card);
				}
			}
		}
		checkForEndOfTurn();
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
	    previously_played_card_player_id = "";
	    previous_card = new Card();
	    number_of_matches = 0;
	    turn_cards = new Vector<Card>();
	    Toast.makeText(this, "Your turn is over.", Toast.LENGTH_LONG ).show();
	}
	
	private void firstCard(Card game_card)
	{
		String method = "firstCard";
		previously_played_word_type = game_card.getWordType();
		previously_played_word_id = game_card.getWordId();
		previously_played_card_player_id = game_card.getPlayerId();
		previously_played_card_id = game_card.getCardId();
		current_player_id = game_card.getPlayerId();
		previous_card = game_card;
		Log.i(DEBUG_TAG, method+" set up to show cards");
		turn_cards.add(game_card);
		turn_card_ids.add(game_card.getCardId());
		card_pairs.add(game_card);
		showCards();
		last_turn_time = new Date().getTime();
	}
	
	private void resetAfterMatch()
	{
	    previously_played_word_id = "";
	    previously_played_word_type = "";
	    previously_played_card_id = "";
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
	
	/**
	 * update card status
	 * reset set previous_player_id and previous_card_id
	 * increment matches.
	 * Save the the game.xml file using the status element as the score.
	 * If any card has status yet-to-be-played then end_game is set to false.
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
        	end_game = true;  // if any cards are unplayed, we will set this to false again.
        	Enumeration<String> e = cards.keys();
    		while (e.hasMoreElements())
    		{
    			String key = e.nextElement();
    			Card card = cards.get(key);
    			String this_id = card.getCardId();
    			if (card.getCardStatus().equals(UtilityTo.YET_TO_BE_PLAYED))
    			{
    				end_game = false;
    			}
    			if (this_id.equals(game_card.getCardId()) || this_id.equals(previously_played_card_id))
    			{
    				card.setCardStatus(UtilityTo.PLAYED);
    				Log.i(DEBUG_TAG, method+" card "+this_id+" new status "+card.getCardStatus()+" end_game "+end_game);
    			} else
    			{
    				Log.i(DEBUG_TAG, method+" card "+this_id+" status "+card.getCardStatus()+" end_game "+end_game);
    			}
    			new_cards.put(this_id, card);
    		}
    		cards = new_cards;
    		showCards();
    		saveCardsFile();
    		IWantTo i_want_to = new IWantTo(context);
            setupGameObject();
            i_want_to.saveTheGameFile(game_file, class_id);
            if (end_game)
            {
            	winner();
            }
    	} else
    	{
    		Log.i(DEBUG_TAG, method+" list_adapter is null");
    	}
	}
	
	/**
	 * To do: this is the place to send info about the win to the learning record store.
	 */
	private void winner()
	{
		String player_name = id_player_names.get(current_player_id);
    	Toast.makeText(this, player_name+" wins!  Final round.", Toast.LENGTH_LONG ).show();
    	final_round = true;
    	game_status.setText(R.string.final_round);
    	game_file.setTestStatus(UtilityTo.FINAL_ROUND);
    	//saveGameFile();
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
    private int updateScore(String player_id)
    {
    	String method = "updateScore";
    	PlayerInfo player_info = players.get(player_id);
    	int score = player_info.getScore();
    	Log.i(DEBUG_TAG, method+" score before update "+score);
    	previously_played_word_id = "";
    	score = incrementScore(score);
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
     * During the final round, each matched pair only increments the score by 1.
     * Otherwise, new_score = score + number_of_matches.
     * @param score
     * @return
     */
    private int incrementScore(int score)
    {
    	int new_score = 0;
    	if (final_round)
    	{
    		number_of_matches++;
        	new_score = score + 1;
    		
    	} else
    	{
    		number_of_matches++;
        	new_score = score + number_of_matches;
    	}
    	return new_score;
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
        game_file.setTestStatus(UtilityTo.READY);
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
	    final_round = false;
	    game_status.setText(R.string.game_ready);
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
			this_card.setCardStatus(UtilityTo.YET_TO_BE_PLAYED);
			cards_copy.put(this_card_id, this_card);
		}
		cards = cards_copy;
		saveCardsFile();
    }
    
    /**
     * This happens every time a match is made. 
     */
    private void setupGameObject()
    {
    	String method = "setupGameObject";
    	printGame(game_file, method+" game_file before setup");
    	game_file = new Game();
    	game_file.setTestFormat("reading_stones");
    	game_file.setTestId(test_id);
    	game_file.setTestName(test_name);
    	game_file.setTestType(UtilityTo.READING);
    	setGameStatus();
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
		game_file.setPlayerStatus(player_id_status);
		printGame(game_file, method+" game_file after setup");
    }
    
    /**
     * As soon as a match is made the status is changed to playing, 
     * unless the final_round or end_game flag is set.
     */
    private void setGameStatus()
    {
    	if (final_round)
    	{
    		game_file.setTestStatus(UtilityTo.FINAL_ROUND);
    	} else if (end_game)
    	{
    		game_file.setTestStatus(UtilityTo.GAME_OVER);
    	}
    	{
    		game_file.setTestStatus(UtilityTo.READING);
    	}
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
					fis = openFileInput(UtilityTo.CARDS_XML);
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
                        				//Log.i(DEBUG_TAG, "parsed card id: "+tag+" "+value);
                        			} else if (tag.equals("player_id"))
                        			{
                        				card.setPlayerId(value);
                        				player_ids.add(value);
                        				//Log.i(DEBUG_TAG, "parsed: "+tag+" "+value);
                        			} else if (tag.equals("card_status"))
                        			{
                        				card.setCardStatus(value);
                        				if (value.equals(UtilityTo.PLAYED))
                        				{
                        					played_card_ids.add(card.getCardId());
                        					//Log.i(DEBUG_TAG, "card played");
                        				}
                        				//Log.i(DEBUG_TAG, "parsed: "+tag+" "+value);
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
                        				//Log.i(DEBUG_TAG, "parsed: "+tag+" "+value);
                        			} else if (tag.equals("text"))
                        			{
                        				card.setText(value);
                        				//Log.i(DEBUG_TAG, "parsed: "+tag+" "+value);
                        			} else if (tag.equals("definition"))
                        			{
                        				card.setDefinition(value);
                        				cards.put(card.getCardId(),card);
                        				Log.i(DEBUG_TAG, method+" put "+card.getCardId()+" "+UtilityTo.getWord(card));
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
    	//Log.i(DEBUG_TAG, method+": file_path - "+file_path);
    	File file = new File(file_path, UtilityTo.CARDS_XML);
    	boolean exists = file.exists();
    	if (exists == false)
    	{
    		// what do we do if there is no file yet?
    		Toast.makeText(this, "Please set up cards brefore game play", Toast.LENGTH_LONG ).show();
    		Log.i(DEBUG_TAG,"create new cards.xml file?");
    	} else
    	{
    		 //parseCards();
            IWantTo i_want_to = new IWantTo(context);
            cardsVectorToHash(i_want_to.loadCardsFile());
    		if (cards.size() == 0)
        	{
        		// what do we do if there is no file yet?
        		Toast.makeText(this, "No cards.  Please set up cards brefore game play", Toast.LENGTH_LONG ).show();
        		Log.i(DEBUG_TAG,"no cards!");
        	}
    	}
    }
    
    private void cardsVectorToHash(Vector <Card> cards_vector)
    {
    	boolean game_over = true;
    	for (int i = 0; i < cards_vector.size(); i++)
    	{
    		Card card = cards_vector.get(i);
    		cards.put(card.getCardId(), card);
    		if (card.getCardStatus().equals(UtilityTo.PLAYED))
    		{
    			game_file.setTestStatus(UtilityTo.PLAYING);
    		} else if (card.getCardStatus().equals(UtilityTo.YET_TO_BE_PLAYED))
    		{
    			game_over = false;
    		}
    	}
    	if (game_over)
    	{
    		game_status.setText(R.string.game_over);
        	game_file.setTestStatus(UtilityTo.GAME_OVER);
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
    		FileOutputStream fos = openFileOutput(UtilityTo.CARDS_XML, Context.MODE_PRIVATE);
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
    	File players_file = new File(file_path, UtilityTo.PLAYERS_XML);
    	boolean exists = players_file.exists();
    	Log.i(DEBUG_TAG, method+": exists? "+exists);
    	if (exists)
    	{
    		Log.i(DEBUG_TAG, method+": parse players.xml and merge with game players");
    		parsePlayers(UtilityTo.PLAYERS_XML);
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
    		FileOutputStream fos = openFileOutput(UtilityTo.CARDS_XML, Context.MODE_PRIVATE);
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
	
    /**
     * Testing card options, and a selection to get instructions on this game.
     * A player can also choose to reset the game.
     */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		super.onCreateOptionsMenu(menu);
		menu.add("Next Card").setIcon(android.R.drawable.star_big_off);
		SubMenu icon_choice = menu.addSubMenu("Choose").setIcon(android.R.drawable.ic_menu_gallery);
		icon_choice.add(icon_group, right_card_id, 1, "Right card");
		icon_choice.add(icon_group, wrong_card_id, 2, "Wrong card");
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
    	Log.i(DEBUG_TAG, method+": Change the icon when the item is selected.");
    	Log.i(DEBUG_TAG, method+": selected "+item.toString());
    	getIntent();
    	if (item.getItemId() == right_card_id)
    	{
    		testCard();
    	    return true;
    	} else if (item.getItemId() == wrong_card_id)
    	{
    		NdefRecord record = getCardRecord("wrong");
    		final TagDescription description = new TagDescription("Wrong Word", record.getPayload());
    		final Intent intent = new Intent(NfcAdapter.ACTION_TAG_DISCOVERED);
            intent.putExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, description.msgs);
            intent.putExtra("test_card", "wrong");
            onNewIntent(intent);
    	    return true;
    	} else if (item.getItemId() == instructions_menu_item_id)
    	{
    		Intent intent = new Intent(GameReadingStonesActivity.this, GameReadingStonesInstructionsActivity.class);
    		startActivity(intent);
    	} else if (item.getItemId() == reset_game_menu_item_id)
    	{
    		resetGame();
    	} else if (item.toString().equals("Next Card"))
    	{
    		testCard();
    	} 
    	
    	return super.onOptionsItemSelected(item);
    }
    
    /**
	 * Pick the first card from the enumeration and send that id to the foundGameCard method. 
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
			Card card = cards.get(this_card_id);
			String card_status = card.getCardStatus();
			Log.i(DEBUG_TAG, method+" card "+card.getDefinition()+" status "+card_status);
			if (!played_card_ids.contains(this_card_id)&&!card_status.equals(UtilityTo.PLAYED))
			{				
				Log.i(DEBUG_TAG, method+" using "+card.getDefinition());
				card.setCardStatus(UtilityTo.PLAYED);
				foundGameCard(card);
				Card matching_card = findMatchingCard(card);
				foundGameCard(matching_card);
				break;
			}
		}
	}
	
	/**
	 * Find the matching card by checking the word id.  If it's the same, return the card.
	 * If the card is not found, something is really wrong, and we return null.
	 * @param card_to_match
	 * @return
	 */
	private Card findMatchingCard(Card card_to_match)
	{
		String method = "findMatchingCard";
		Enumeration<String> e = cards.keys();
		while (e.hasMoreElements())
		{
			String this_card_id = e.nextElement();
			Card this_card = cards.get(this_card_id);
			Log.i(DEBUG_TAG, method+" card "+UtilityTo.getWord(this_card));
			if (this_card.getWordId() == card_to_match.getWordId())
			{
				// make sure it's not the exact same card.
				if (this_card.getWordType()!=card_to_match.getWordType())
				{
					return this_card;
				}
			}
					
		}
		return null;
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
