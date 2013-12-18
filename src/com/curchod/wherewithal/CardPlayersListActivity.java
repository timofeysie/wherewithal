package com.curchod.wherewithal;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.curchod.domartin.Constants;
import com.curchod.domartin.HouseDeck;
import com.curchod.domartin.IWantTo;
import com.curchod.domartin.UtilityTo;
import com.curchod.dto.Card;
import com.curchod.dto.DeckCard;

/**
 * Display a list of players signed up for a game, as well as info about the game.
 * Let the user choose a player to move on to the next activity
 * where we show the words for their cards and register each card allow them to register each card
 * in a time consuming fashion.
 * For that reason, in the next activity a user can choose to use a house deck from a menu item.
 * Once that association is made it is sent to the server, which when we get the test words again,
 * will have those deck card associations.
 * @author user
 *
 */
public class CardPlayersListActivity extends ListActivity 
{

	private static final String DEBUG_TAG = "CardPlayersListActivity"; 
	private Hashtable <String, String> player_name_ids;
	private CardPlayersListActivity activity = this;
	private String selected_test_id;
	private String selected_test_name;
	private String selected_test_type;
	private String selected_test_status;
	private String selected_test_format;
	private String number_of_words;
	final Context context = this;	
	URL text = null;
	private Hashtable <String, String> test_words;
	/**This is used to highlight the players who have finished setting up their cards.*/
	private Hashtable <String, String> player_name_status;
	private ArrayAdapter array_adapter;
	//private static final int use_house_deck_id = 1;
	private HouseDeck house_deck;
	private Hashtable <String, HouseDeck> house_deck_names_decks;
	String selected_house_deck_name;
	/* This is used so that when a user selects a test, and we find out that the test has been associated with a house deck in the past
	 * which means they have cards with those house deck card names on them, we need to have the house decks on hand to get the right one. */
	private Hashtable <String,HouseDeck> house_decks;
	
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_card_players_list);
        String method = "onCreate";
        String build = "build 51";
        Log.i(DEBUG_TAG, method+" "+build);
        bundleIterator();
		final String[] PLAYERS = getPlayerNamesAndExtras(); 
		//ListView list_view = (ListView) findViewById(R.id.card_players_list_layout);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, PLAYERS);
   	 	//list_view.setAdapter(adapter); 
		this.setListAdapter(
				array_adapter = new ArrayAdapter<String>(this, com.curchod.wherewithal.R.layout.activity_card_players_list, 
						com.curchod.wherewithal.R.id.card_players_list_layout, PLAYERS)
				{
		    @Override
		    public View getView(int position, View convertView, ViewGroup parent)
		    {
		        final View renderer = super.getView(position, convertView, parent);
		        int this_position = 0;
		        ListView list_view = getListView();
		        ArrayAdapter<?> adapter = (ArrayAdapter<?>) list_view.getAdapter();
		        for (int pos = 0; pos < adapter.getCount(); pos++)
				{		
					String item = (String)adapter.getItem(pos);
					try
					{
						if (player_name_status.get(item).equals(Constants.READY))
						{
							this_position = pos;
							Log.i(DEBUG_TAG, pos+" Ready "+player_name_status.get(item));
						} else
						{
							Log.i(DEBUG_TAG,  pos+" not ready "+player_name_status.get(item));
						}
					} catch (java.lang.NullPointerException npe)
					{
						Log.i(DEBUG_TAG,  "Line 82 error: It would be a better idea to fix the way the player status is put into the intext instead of using a try/catch block.");
					}
				}
		        if (position == this_position)
		        {
		            renderer.setBackgroundResource(android.R.color.darker_gray);
		        } else
                {
                	renderer.setBackgroundResource(android.R.color.background_light);
                }
		        return renderer;
		    }
		});
		final ListView list_view = getListView();
		list_view.setTextFilterEnabled(true); 
   	 	list_view.setOnItemClickListener(new OnItemClickListener() 
		{
	          public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
	          {
	              String player_name = PLAYERS[position];
	              String player_id = (String)player_name_ids.get(player_name);
	              try
	                {
	                	list_view.post(new Runnable() 
	                	{
	                		public void run() 
	                		{
	                			((ArrayAdapter<String>) array_adapter).notifyDataSetChanged();
	                		}	
	                	});
	                } catch (java.lang.NullPointerException npe)
	                {
	                	Log.i(DEBUG_TAG, "OnItemClickListener: list_view is null");
	                }
	              Log.i(DEBUG_TAG, "onItemClick: selected player "+player_name+" id "+player_id);
	              setupHouseDeckOrRandomCards(player_id, player_name);
	          }
	    });
   	 	//Log.i(DEBUG_TAG, method+" selected_test_name "+selected_test_name);
   	    setTitle(selected_test_name);
   	 	//TextView test_name = (TextView)findViewById(R.id.test_name);
   	 	//test_name.setText(selected_test_type);
    }
    
    private boolean setupHouseDeckOrRandomCards(final String player_id, final String player_name)
    {
    	final String method = "setupHouseDeckOrRandomCards";
    	new Thread() 
    	{
            public void run() 
            {
            	boolean house_deck_already_setup = loadHouseDecks(player_id);
            	if (house_deck_already_setup)
            	{
            		// association already made, fill intent with deck card names-words and start next activity
            		// start the CardPlayerHouseDeckActivity
            		associateCardNamesWithWords(player_id);
            	} else
            	{
            		// get words for test from the server, fill the intent and start CardPlayerWordsActivity.
            		// if a previous association was made, then a different game played, the saved test will
            		// have the house deck associations, and then we will send the user to a similar method to 
            		// associateCardNamesWithWords above.
            		getTestWordsOrMakePreviousAsscociation(player_id, player_name);
            	}
            }
    	}.start();
    	return false;
    }
    
    /**
     * When we fill the intent i+deck_card_name and i+word, we start with 1. 
     * @param selected_player_id
     */
    private void associateCardNamesWithWords(final String selected_player_id)
    {
    	final String method = "associateCardNamesWithWords";
    	new Thread()
        {
            public void run()
            {   
            	// we have the house_deck which has the deck cards and their ids-deck card.
            	Hashtable <String,DeckCard> deck_cards = house_deck.getCards();
            	//Log.i(DEBUG_TAG, method+" deck_cards size "+deck_cards.size());
            	IWantTo i_want_to = new IWantTo(context);
            	Vector <Card> file_cards = i_want_to.loadCardsFile();
            	Hashtable <String,Card> id_file_cards = getIdFileCards(file_cards, selected_player_id);
            	//Log.i(DEBUG_TAG, method+" id_file_cards size "+id_file_cards.size());
            	// now we have the cards.xml Vector
            	Intent intent = new Intent(CardPlayersListActivity.this, CardPlayerHouseDeckActivity.class);
                Enumeration<String> e = id_file_cards.keys();
                int i = 1;
                while (e.hasMoreElements())
                {
                	//try
                	//{
                		String deck_card_id = (String)e.nextElement(); 
                		Card card = id_file_cards.get(deck_card_id);
                		Log.i(DEBUG_TAG, method+" deck_card_id "+deck_card_id);
                		try
                		{
                			DeckCard deck_card = deck_cards.get(deck_card_id);
                			String deck_card_name = deck_card.getCardName();
                			String word = UtilityTo.getWord(card);
                			Log.i(DEBUG_TAG, method+" set "+i+" "+deck_card_name+" "+word+" id "+deck_card_id);
                			intent.putExtra(i+"deck_card_name", deck_card_name);
                			intent.putExtra(i+"word", word);
                			i++;
                	} catch (java.lang.NullPointerException npe)
                	{
                		Log.i(DEBUG_TAG, method+" woah! didn't set "+deck_card_id);
                		//intent.putExtra(i+"deck_card_name", "R1null");
                		//intent.putExtra(i+"word", "null"+i);
                	}
                }
                intent.putExtra("selected_test_name", selected_test_name);
                intent.putExtra("number_of_words", i+"");
                intent.putExtra("student_id", selected_player_id);
                intent.putExtra("selected_test_id", selected_test_id);
                Log.i(DEBUG_TAG, method+" set "+id_file_cards.size()+" cards into the intent. with number_of_words "+number_of_words);
                startActivity(intent);
           }
       }.start();
    }
    
    /**
     * This method creates a hash with the file_card ids as keys, and the Card objects in the _file_cards
     * passed in as an argument and returns the hashtable.
     * @param _file_cards
     * @param selected_player_id
     * @return
     */
    private Hashtable <String,Card> getIdFileCards(Vector <Card> _file_cards, String selected_player_id)
    {
    	String method = "getIdFileCards";
    	Hashtable <String,Card> id_file_cards = new Hashtable <String,Card> ();
    	for (int i = 0; i < _file_cards.size(); i++)
    	{
    		Card file_card = _file_cards.get(i);
    		if (selected_player_id.equals(file_card.getPlayerId()))
    		{
    			id_file_cards.put(file_card.getCardId(), file_card);
    			Log.i(DEBUG_TAG, method+" added "+UtilityTo.getWord(file_card)+" for user "+file_card.getPlayerId());
    		} else
    		{
    			Log.i(DEBUG_TAG, method+" didn't add "+UtilityTo.getWord(file_card)+" for user "+file_card.getPlayerId());
    		}
    	}
		return id_file_cards;
    }
    
    private boolean loadHouseDecks(final String player_id)
    {
    	String method = "loadHouseDecks";
    	boolean house_deck_already_setup = false;
    	IWantTo i_want_to = new IWantTo(context);
    	house_deck_names_decks =  new Hashtable <String, HouseDeck> ();
    	house_decks = i_want_to.loadTheHouseDecks();
    	Log.i(DEBUG_TAG, method+" house_decks after load "+house_decks.size());
    	Enumeration <String> e = house_decks.keys();
    	Log.i(DEBUG_TAG, method+" player_id "+player_id+" selected_test_id "+selected_test_id);
    	while (e.hasMoreElements())
    	{
    		String key = (String)e.nextElement();
    		HouseDeck this_house_deck = house_decks.get(key);
    		String house_deck_game_id = this_house_deck.getGameId();
    		String house_deck_player_id = this_house_deck.getPlayerId();
    		if (player_id.equals(house_deck_player_id)&&selected_test_id.equals(house_deck_game_id))
    		{
    			house_deck_already_setup = true;
    			house_deck = this_house_deck;
    			Log.i(DEBUG_TAG, method+" if player_id "+player_id+" equals house_deck_player_id "+house_deck_player_id+" && selected_test_id "+selected_test_id+" equals house_deck_game_i "+house_deck_game_id+" house deck already chosen: "+this_house_deck.getDeckName());
    		} else
    		{
    			Log.i(DEBUG_TAG, method+" looked at "+this_house_deck.getDeckName()+" id "+this_house_deck.getDeckId()+" but no chosen");
    		}
    	}
    	return house_deck_already_setup;
    }
    
    /**
	 * Parse the remote call to GetSavedClassTestsAction, parse the results, put them in the intent
	 * and start the CardPlayersListAction.
	 * http://211.220.31.50:8080/indoct/get_test_words.do?player_id=-5519451928541341468&test_id=-8834326842304090029   
	 * If the test words returned contain no house deck id, then we
	 * send the test_words to the CardPlayerWordsActivity.
	 * If the test words have house deck id then that deck has been 
	 * associated with this test previously.  In this case we associate the file cards with 
     * the deck card names sent from the server, and then send the list of
     * card names and words to the CardPlayerHouseDeckActivity.
     * IF the selected_test_format is Writing Stones, then we want to re-use the previously assigned
     * definition cards.
	 * 
	 * @param selected_test_id
	 */
	private void getTestWordsOrMakePreviousAsscociation(final String selected_player_id, final String player_name)
	{
		final String method = "getTestWordsOrMakePreviousAsscociation";
    	new Thread()
        {
            public void run()
            {   
            	//remoteCall(text);
            	IWantTo i_want_to = new IWantTo(context);
            	test_words = i_want_to.getTestWords(selected_player_id, selected_test_id, number_of_words);
            	Log.i(DEBUG_TAG, method+" test_words "+test_words.size());
            	String house_deck_id = (String)test_words.get("house_deck_id");
            	if (house_deck_id == null)
            	{
            		Log.i(DEBUG_TAG, method+" house_deck_id "+house_deck_id+" not found in decks, setting to null");
            		Log.i(DEBUG_TAG, method+" go to startCardPlayerWordsActivity");
            		startCardPlayerWordsActivity(selected_player_id);
            	} else
            	{
            		HouseDeck _house_deck = house_decks.get(house_deck_id);
            		if (_house_deck == null)
                	{
                		Log.i(DEBUG_TAG, method+" house_deck_id "+house_deck_id+" not found in decks, setting to nul.  go to startCardPlayerWordsActivity");
                		startCardPlayerWordsActivity(selected_player_id);
                	} else
                	{
            		Hashtable <String,DeckCard> deck_cards = _house_deck.getCards();
            		Log.i(DEBUG_TAG, method+" house_deck_id "+house_deck_id+" name "+_house_deck.getDeckName()+" cards "+_house_deck.getCards().size());
            		// an association was made between these cards and a house deck (just not
            		// the game previously) and so we need to associate the file cards with 
            		// the deck card names sent from the server, and then send the list of
            		// card names and words to the CardPlayerHouseDeckActivity.
            		associatePreviousCardNamesWithWords(selected_player_id, deck_cards, player_name);
                	}
            	}
           }
       }.start();
	}

	
	/**
	 * We need to reset the file_card ids to match the associations made previously
	 * which are now contained in the test words hash.
	 * @param selected_player_id
	 * @param test_words
	 */
	private void associatePreviousCardNamesWithWords(String selected_player_id, Hashtable deck_cards, String player_name)
	{
		String method = "associatePreviousCardNamesWithWords";
		Log.i(DEBUG_TAG, method+" number_of_words from test_words: "+number_of_words);
		Hashtable <String,String> deck_card_name_words = reAssociateFileCards(selected_player_id, deck_cards, player_name);
    	Intent intent = new Intent(CardPlayersListActivity.this, CardPlayerHouseDeckActivity.class);
        Enumeration<String> e = deck_card_name_words.keys();
        int i = 1;
        while (e.hasMoreElements())
        {
        		String deck_card_name = (String)e.nextElement(); 
        		String deck_card_word = deck_card_name_words.get(deck_card_name);
        		try
        		{
        			Log.i(DEBUG_TAG, method+" set "+i+" "+deck_card_name+" "+deck_card_word);
        			intent.putExtra(i+"deck_card_name", deck_card_name);
        			intent.putExtra(i+"word", deck_card_word);
        			i++;
        	} catch (java.lang.NullPointerException npe)
        	{
        		Log.i(DEBUG_TAG, method+" woah! didn't set "+deck_card_name);
        	}
        }
        intent.putExtra("selected_test_name", selected_test_name);
        intent.putExtra("number_of_words", i+"");
        intent.putExtra("student_id", selected_player_id);
        intent.putExtra("selected_test_id", selected_test_id);
        startActivity(intent);
	}
	
	/**
	 * First we call getDeckCardNameIds to get the deck_card_name_ids.  We fill the word_deck_card_ids
	 * with the file card word and the deck card id which is used in the second loop to set the file card
	 * ids to that of the associated deck card.
	 * The test_words passed in contain key value pairs for ALL the data passed in from the remote call.
	 * The first for loops through the test_words starting at 1 and ending at the number of words set into the hash as a key.
	 * It then sets up the card objects, leaving the card id blank.  It will be filled in the next loop.
	 * It also sets up the return hash deck_card_name_words which will be put into the intent.
	 * The second for loop goes thru each new card and adds it to the file cards just loaded.
	 * Then we save all the file cards and we're done.
	 * Other info available but no used:
	 * category
	 * grade
	 * index
	 * @param selected_player_id
	 * @param file_cards
	 * @param test_words
	 */
	private Hashtable <String,String> reAssociateFileCards(String selected_player_id, Hashtable deck_cards, String player_name)
	{
		String method = "reAssociateFileCards";
		Hashtable <String,String> deck_card_name_words = new Hashtable <String,String> ();
		Hashtable <String, String> deck_card_name_ids = getDeckCardNameIds(deck_cards);
		Vector <Card> new_file_cards = new Vector <Card> ();
		Log.i(DEBUG_TAG, method+" number_of_words "+number_of_words+" selected_test_format "+selected_test_format);
		for (int i = 1; i <= Integer.parseInt(number_of_words); i++)
		{
			// fill the deck_card_name_ids
			String word_text = (String)test_words.get(i+"text").trim();
			String word_definition = (String)test_words.get(i+"definition").trim();
			String word_id = (String)test_words.get(i+"id");
			String category = (String)test_words.get(i+"category");
			String writing_deck_card_name = (String)test_words.get(i+"writing_deck_card_name");
			if (!selected_test_format.equals(Constants.WRITING_STONES))
			{
				String reading_deck_card_name = (String)test_words.get(i+"reading_deck_card_name");
				String reading_deck_card_id = deck_card_name_ids.get(reading_deck_card_name);
				Card reading_new_card = setupCardFromIntent(reading_deck_card_id, Constants.READING, selected_player_id, 
					player_name, word_id, word_text, word_definition, i+"", category);
				new_file_cards.add(reading_new_card);
				deck_card_name_words.put(reading_deck_card_name, word_text);
				Log.i(DEBUG_TAG, method+" i "+i+" writing stones qoes "+word_definition+" "+word_text+" "+reading_deck_card_name+" "+writing_deck_card_name);
			}
			String writing_deck_card_id = deck_card_name_ids.get(writing_deck_card_name);
			deck_card_name_words.put(writing_deck_card_name, word_definition);
			Card writing_new_card = setupCardFromIntent(writing_deck_card_id, Constants.WRITING, selected_player_id, 
					player_name, word_id, word_text, word_definition, i+"", category);
			new_file_cards.add(writing_new_card);
		}
		IWantTo i_want_to = new IWantTo(context);
    	Vector <Card> file_cards = i_want_to.loadCardsFile();
    	Log.i(DEBUG_TAG, method+" file_cards after load "+file_cards.size()+" adding new file cards "+new_file_cards.size());
    	for (int i = 0; i < new_file_cards.size(); i++)
		{
    		Card this_card = new_file_cards.get(i);
    		file_cards.add(this_card);
    		Log.i(DEBUG_TAG, method+" set card "+UtilityTo.getWord(this_card)+" to card_id "+this_card.getCardId());
		}
    	i_want_to.saveCardsFile(file_cards);
		return deck_card_name_words;
	}
	
	private Card setupCardFromIntent(String card_id, String type, String player_id, String player_name,
			String word_id, String word_text, String word_definition, String index, String category)
    {
    	Card card = new Card();
    	card.setCardId(card_id);
    	card.setCardStatus(Constants.YET_TO_BE_PLAYED);
    	card.setPlayerName(player_name);
    	card.setPlayerId(player_id);
    	card.setWordId(word_id); 
		card.setText(word_text); 
		card.setDefinition(word_definition);
		card.setIndex(index);
		card.setWordCategory(category);
		card.setWordType(type);
		return card;
    }
	
	private Hashtable <String,String> getDeckCardNameIds(Hashtable <String,DeckCard> deck_cards)
	{
		Hashtable <String,String> deck_cards_name_ids = new Hashtable <String,String> ();
		Enumeration <String> e =  deck_cards.keys();
    	while (e.hasMoreElements())
    	{
    		String key = (String)e.nextElement();
    		DeckCard this_deck_card = (DeckCard)deck_cards.get(key);
    		deck_cards_name_ids.put(this_deck_card.getCardName(), this_deck_card.getCardId());
    	}
		return deck_cards_name_ids;
	}
	
	/**
     * This method takes the 
     */
    private Hashtable <String,String> getCardNameWords(Vector <Card> _file_cards, Hashtable<String, String> test_words)
    {
    	String method = "getCardNameWords";
    	Hashtable <String,String> card_name_words = new Hashtable <String,String> ();
    	Enumeration <String> e = test_words.keys();
    	while (e.hasMoreElements())
    	{
    		String key = (String)e.nextElement();
    		String val = test_words.get(key);
    		Log.i(DEBUG_TAG, method+" key "+key+" val "+val);
    	}
    	return card_name_words;
    }
	
	/**
	 * This was our first method of setting up the cards.  A user simply sees a flat list of
	 * text or definition words and chooses them to write to NFC tag cards.
	 * This is time consuming and a buzz kill for something that is supposed to be fun.
	 * @param selected_player_id
	 */
	private void startCardPlayerWordsActivity(String selected_player_id)
	{
		String method = "startCardPlayerWordsActivity";
		Intent intent = new Intent(CardPlayersListActivity.this, CardPlayerWordsActivity.class);
        Enumeration<String> e = test_words.keys();
        int i = 0;
        while (e.hasMoreElements())
        {
           	String key = (String)e.nextElement();
           	String val = test_words.get(key).trim();
            Log.i(DEBUG_TAG, method+" got "+key+" "+val);
           	intent.putExtra(key,val);
           	i++;
        }
        intent.putExtra("selected_test_name", selected_test_name);
        intent.putExtra("number_of_words", number_of_words);
        intent.putExtra("student_id", selected_player_id);
        intent.putExtra("selected_test_id", selected_test_id);
        intent.putExtra("selected_test_format", selected_test_format);
        //Log.i(DEBUG_TAG, method+" start next activity with selected_player_id (student_id)"+selected_player_id);
        if (i==0)
        {
        	// remote call failed. 
        	activity.runOnUiThread(new Runnable()
        	{
        		public void run()
        		{
        			Toast.makeText(context, R.string.remote_call_failed, Toast.LENGTH_LONG ).show();
        		}
        	});
        } else
        {
        	startActivity(intent);
        }
	}
    
    /**
     * Get the player names from the intent, as well as all other info, including the test name.
     * @return
     */
    private String [] getPlayerNamesAndExtras()
    {
    	String method = "getPlayerNamesAndExtras &&&&&&&&&&&&";
    	player_name_ids = new Hashtable<String,String>();
    	player_name_status = new Hashtable<String,String>();
    	Intent sender = getIntent();
    	int size = Integer.parseInt(sender.getExtras().getString("number_of_tests"));
    	final String[] PLAYERS = new String[size];
		for(int i=0;i<size;i++)
		{
			String player_name = sender.getExtras().getString((i+1)+"student_name"); // starts at 1.
			String player_id = sender.getExtras().getString((i+1)+"student_id"); // starts at 1.
			String player_status = sender.getExtras().getString((i+1)+"player_status"); // starts at 1.
		    Log.i(DEBUG_TAG, method+"player "+i+" player_name "+player_name+" player_status "+player_status);
		    PLAYERS[i] = player_name;
		    player_name_ids.put(player_name, player_id);
		    try
		    {
		    	player_name_status.put(player_name, player_status);
		    } catch (java.lang.NullPointerException npe)
		    {
		    	Log.i(DEBUG_TAG, "player_name_status.put NPE");
		    }
		    Log.i(DEBUG_TAG, "player_name_status.put(player_name, player_status) ("+player_name+", "+player_status+")");
		}
		Bundle extras = sender.getExtras();
		Set<String> keys = extras.keySet();  
		Iterator<String> iterate = keys.iterator();
		while (iterate.hasNext()) 
		{  
		    String key = iterate.next();  
		    char c = key.charAt(0);
		    if (Character.isDigit(c))
		    {
		    	String property = key.substring(1, key.length());
		    	String value = (String)extras.get(key);
		    	if (property.equals("test_type"))
		    	{
		    		selected_test_type = value;
		    	} else if (property.equals("test_status"))
		    	{
		    		selected_test_status = value;
		    	} else if (property.equals("test_format"))
		    	{
		    		selected_test_format = value;
		    	}
		    }
		    //Log.i(DEBUG_TAG, key + "-" + extras.get(key));  
		} 
		selected_test_name = sender.getExtras().getString("selected_test_name");
		selected_test_id = sender.getExtras().getString("selected_test_id");
		number_of_words = sender.getExtras().getString("1number_of_words");
		//Log.i(DEBUG_TAG, "extras: 1number_of_words - " + number_of_words);  
		return PLAYERS;
    }
    
    private void bundleIterator()
    {
    	Intent sender = getIntent();
    	Bundle extras = sender.getExtras();
		Set<String> keys = extras.keySet();  
		Iterator<String> iterate = keys.iterator();
		while (iterate.hasNext()) 
		{  
		    String key = iterate.next();  
		    char c = key.charAt(0);
		    if (Character.isDigit(c))
		    {
		    	String property = key.substring(1, key.length());
		    	String value = (String)extras.get(key);
		    	Log.i(DEBUG_TAG, "bundleIterator: "+property+"-"+value);
		    }
		    //Log.i(DEBUG_TAG, key + "-" + extras.get(key));  
		} 
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        getMenuInflater().inflate(R.menu.activity_card_players_list, menu);
        //menu.add(0 , use_house_deck_id, 0, com.curchod.wherewithal.R.string.use_house_deck); 
        return true;
    }
    
    @Override
    public void onResume()
    {
         super.onResume();
         array_adapter.notifyDataSetChanged();
    }
    
}
