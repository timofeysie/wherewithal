package com.curchod.wherewithal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.nfc.tech.NfcF;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.curchod.domartin.Card;
import com.curchod.domartin.Constants;
import com.curchod.domartin.DeckCard;
import com.curchod.domartin.Game;
import com.curchod.domartin.HouseDeck;
import com.curchod.domartin.IWantTo;
import com.curchod.domartin.UtilityTo;

/**
 * Provide the user with a list of words to write to NFC cards.
 * Each word is split into text and definition pairs which are set into a flat list.
 * When the user has selected all the words, the status for that player is set to ready in the game file.
 * 
 * The user can choose to use a house deck which will then associate the words and their file cards
 *  with the house deck chosen from a popup list.  The file card ids will be changed to match the 
 *  deck card that they are associated with. 
 *  Then, the next time, this activity will not be started.  Instead we will start the 
 * CardPlayerHouseDeckActivity.  The CardPlayersListActivity makes this decision by the player_id/test_id
 * set in the house deck file which happens after the associateHouseDeckWithWords method here.
 * We also send the associations to the server in the sendDeckCardAssociations method so that these 
 * associations can be used later after other games have been played.
 * Another way not ot get here is if an association has been made before the previous game then the server
 * will return a deck card id and deck card names, in which case this activity is not started.
 * 
 * onCreate
 * setUpPlayerNamesAndExtras();
 * loadCardsFile();  // load the cards.xml file.
 * loadGameFileAndUpdateStatus() set's up the Game object, then calls saveGameFileAndStatus(game).
 * @author Administrator
 *
 */
public class CardPlayerWordsActivity extends ListActivity 
{

	private static final String DEBUG_TAG = "CardPlayerWordsActivity";
	private String[] WORDS;
	/** For cards that have already been written to tags and should be highlighted.  Matches the position of WORDS array.*/
	private String[] FINISHED_WORDS;
	/** This is a mirror of FINISHED_WORDS but with player_ids as values to make sure we only highlight words from the current player.**/
	//private String[] FINISHED_WORDS_PLAYER_ID;
	private Hashtable <String,String> word_ids;
	private Hashtable <String,Integer> word_position;
	private Hashtable <Integer,Card> position_cards;
	private Vector <String> selected_words;
	/** A list of words from the cards.xml file that have already been written to card tags.*/
	private Vector <String> previously_selected_words;
	private Hashtable <String,String> previously_written_words_ids;
	private NfcAdapter nfc_adapter;
	final Context context = this;	
	String[][] techListsArray;	  
	PendingIntent pendingIntent;
	IntentFilter writeTagFilters[];
	boolean writeMode;
	Tag card_tag;
	int selected;
	CardPlayerWordsActivity activity = this;
	private Vector <Card> cards; 
	private Vector <Card> file_cards; 
	private String encoding = "euc-kr";
	boolean testing;
	private String player_id;
	private String player_name;
	private ArrayAdapter array_adapter;
	private ListView list_view;
	private int number_of_words;
	private String game_status;
	private IntentFilter[] intentFiltersArray;
	private Intent new_intent;
	private boolean write_failed;
	private String class_id;
	private static final int use_house_deck_id = 1;
	/** This holds the names of the house decks and the decks themselves and is used to get the selected house deck after the dialog.*/ 
	private Hashtable <String, HouseDeck> house_deck_names_decks;
	private Hashtable <String, String> house_deck_names_ids;
	private String selected_test_id;
	private String selected_test_format;
	/** Used to associate the deck card names with words when starting the CardPlayerHouseDeckActivity */
	private Hashtable <String,String> deck_cards_id_names;
	private Hashtable <String,String> word_deck_card_associations;
	/** In the setupDeckCardAssociationsPairs method, we need to know the type of the deck card.  So we use this hash/*/
	private Hashtable <String, String> deck_card_name_types;
	private Dialog dialog;
	/** We need this to get the word ids from the deck card ids when trying to send the associations to the server.*/
	private Hashtable <String,String> deck_card_id_word_ids;
	/** THis is used if the test is the writing test is part of s triptych series, and we need to reassociate the cards by re-using the associations from the previous test in the series.  Used in the method getPreviousWritingCardNamesWordIds. */
	private Vector <String> previous_writing_word_ids;
	/** When coming back to this activity, the super call to getView cause a null pointer, so we put it here. */
	private View renderer;
	private Vector <String> previously_used_deck_names;
	/** Set up in the getRemainingWritingCardNamesWordIdsAddGameTwoCard method and used in the addGameTwoCards method.*/
	private Vector <Card> game_two_writing_cards;
	private Hashtable <String,DeckCard> deck_card_name_deck_cards;
	/** Used to associate the test two writing word ids with the deck card names in E.*/
	private Hashtable <String,String> writing_word_definition_card_names;
	
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        String method = "onCreate";
        String build = "build 172";
        nfc_adapter = NfcAdapter.getDefaultAdapter(context);
        Log.i(DEBUG_TAG, method+": "+build);
        setUpPlayerNamesAndExtras();
        file_cards = new Vector <Card> ();  // this will be filled in the next method.
        loadCardsFile();  // load the cards.xml file.
        Log.i(DEBUG_TAG, method+" loaded "+file_cards.size()+" words.");
   	 	this.setListAdapter(
   	 			array_adapter = new ArrayAdapter<String>(this, com.curchod.wherewithal.R.layout.activity_card_player_words, 
   	 					com.curchod.wherewithal.R.id.card_player_words_layout, WORDS)
				{
		    @Override
		    public View getView(int position, View convertView, ViewGroup parent)
		    {
		        try
		        {
		        	renderer = super.getView(position, convertView, parent); // used to be outside the try block but got an npe when coming back to this activity.
		        	if (FINISHED_WORDS[position].equals(UtilityTo.SET))
		        	{
		        		//Log.i(DEBUG_TAG, "FINISHED_WORDS["+position+"] = set");
		        		//Log.i(DEBUG_TAG, "FINISHED_WORDS_PLAYER_ID["+position+"] = "+player_id);
		        		renderer.setBackgroundResource(android.R.color.darker_gray);
		        	} else
		        	{
		        		renderer.setBackgroundResource(android.R.color.background_light);
		        	}
		        } catch (java.lang.NullPointerException npe)
		        {
		        		//Log.i(DEBUG_TAG, "onCreate.setListAdapter.getView: npe at "+position);
		        }
		        return renderer;
		    }
		});
   	 	list_view = getListView();
		list_view.setTextFilterEnabled(true); 
		list_view.setOnItemClickListener(new OnItemClickListener() 
		{
	          public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
	          {
	        	    selected = position;
	        	    testing = true;
	                final String selected_word = WORDS[position];
	                Card new_card = position_cards.get(position);
	            	if (checkIfWordIsAlreadyRead(new_card))
	            	{
	            		//Log.i(DEBUG_TAG, "Card already selected.");
		    			Toast.makeText(context, context.getString(R.string.already_written) , Toast.LENGTH_SHORT ).show();
	            	}
	            	{	                
	            		LayoutInflater layout_inflater = LayoutInflater.from(context);
	            		View popup_view = layout_inflater.inflate(R.layout.card_player_words_popup, null);
	            		final AlertDialog.Builder alert_dialog_builder = new AlertDialog.Builder(context);
	            		alert_dialog_builder.setView(popup_view);
	            		final TextView card_player_words_popup_text = (TextView) popup_view.findViewById(R.id.card_player_words_popup_text);
	            		card_player_words_popup_text.setText(selected_word);
	            		//card_player_words_popup_text.setText(R.string.scan_+selected_word+R.string.now);
	            		alert_dialog_builder.setCancelable(false).setPositiveButton(R.string.ok,
	                		new DialogInterface.OnClickListener() 
	                    	{
	    					    public void onClick(DialogInterface dialog,int id) 
	    					    {
	    					    	if(card_tag==null)
									{
									      Toast.makeText(context, context.getString(R.string.error_detected), Toast.LENGTH_LONG ).show();
									      Log.i(DEBUG_TAG, "onClick: error detected: card tag is null");
									} else
									{
										//try
										//{
											Log.i(DEBUG_TAG, "DialogInterface.onClick: writing");
											Card card = itemSelected(selected_word);
											//write(card);
											try
											{
												String card_id = readWriteFormatReject(card);
												if (card_id == null || write_failed)
												{
													// make toast, can't use card
													Toast.makeText(context, R.string.cannot_use_card, Toast.LENGTH_LONG ).show();
												} else
												{
													selectedItemForHighlighting(selected_word);
													card.setCardId(card_id);
													file_cards.add(card);
													Toast.makeText(context, context.getString(R.string.dont_move_writing), Toast.LENGTH_LONG ).show();
													Toast.makeText(context, context.getString(R.string.ok_writing), Toast.LENGTH_LONG ).show();
													savedCardsFile();
													checkIfSetupComplete();
													testing = false;
												}
											} catch (java.lang.NullPointerException npe)
											{
												// no tag to read
												Log.i(DEBUG_TAG, "DialogInterface.onClick: no tag to read");
												Toast.makeText(context, R.string.put_the_tag_under_the_phone, Toast.LENGTH_LONG ).show();
											}
									}
	    					    	if (testing)
	    					    	{
	    					    		Log.i(DEBUG_TAG, "DialogInterface.onClick: Emulation mode, no card written during testing");
										Card card = itemSelected(selected_word);
										Log.i(DEBUG_TAG, "DialogInterface.onClick: test writing");
										try
										{
											Log.i(DEBUG_TAG, "testing: no npe");
											readWriteFormatReject(card);
											file_cards.add(card);
											savedCardsFile();
											checkIfSetupComplete();
										}
	    					    		 catch (java.lang.NullPointerException npe)
										{
											// no tag to read
											Log.i(DEBUG_TAG, "DialogInterface.onClick: no tag to read");
											Toast.makeText(context, "Test mode: Put the tag under the phone and try again.", Toast.LENGTH_LONG ).show();
											Log.i(DEBUG_TAG, "testing: npe");
											npe.printStackTrace();
										}
	    					    	}
	    					    	dialog.cancel();
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
			}
	    });
   	 	pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
   	 	IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
   	 	IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
   	 	try 
   	 	{
   	 		ndef.addDataType("*/*");
   	 	} catch (MalformedMimeTypeException e) 
   	 	{
   	 		throw new RuntimeException("fail", e);
   	 	}

   	 	IntentFilter tech = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
   	 	try 
   	 	{
   	 		tech.addDataType("*/*");
   	 	} catch (MalformedMimeTypeException e) 
   	 	{
   	 		throw new RuntimeException("fail", e);
   	 	}
   	 	intentFiltersArray = new IntentFilter[] { tagDetected, ndef, tech };
   	 	tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
   	 	writeTagFilters = new IntentFilter[] { tagDetected };
   	 	techListsArray = new String [][] { new String[] {NfcF.class.getName()}};
   	 	write_failed = false;
    }
    
    /**
     * Check to see if all the words have been selected.
     */
    private void checkIfSetupComplete()
    {
    	String method = "checkIfSetupComplete";
    	int count = 0;
    	int size = FINISHED_WORDS.length;
    	//Log.i(DEBUG_TAG, method+" size "+size);
    	for (int i = 0; i < size; i++)
    	{
    		String status = FINISHED_WORDS[i];
    		//Log.i(DEBUG_TAG, method+"status equals "+Utilities.SET+"?");
    		if (status.equals(UtilityTo.SET))
    		{
    			count++;
    			//Log.i(DEBUG_TAG, method+" count++ to "+count);
    		} else
    		{
    			//Log.i(DEBUG_TAG, method+" FINISHED_WORDS[i]="+status+" "+count);
    		}
    	} 
    	if (count == size)
    	{
    		Log.i(DEBUG_TAG, "checkIfSetupComplete: file_cards.size() "+file_cards.size()+" "+number_of_words*2+" setup complete!");
    		Toast.makeText(context, context.getString(R.string.player_setup_complete) , Toast.LENGTH_LONG ).show();
    		game_status = UtilityTo.READY;
    		loadGameFileAndUpdateStatus();
    		file_cards = new Vector <Card> ();
    		//Log.i(DEBUG_TAG, method+" complete: game_status set to "+game_status);
    	} else
    	{
    		//Log.i(DEBUG_TAG, method+" count "+count+" vs size "+size);
    	}
    }
    
    /**
	 * Open the game.xml file. Read the data into class member variables.
	 * This file has the following format:
	 * <game>
     * 	<test_name>
     * 	<test_id>
     * <class_id>
     * 	<test_type>
     * 	<test_status>
     * 	<test_format>
     *  <player_id>
     *  <player_status id="7655807335881695697">setup</player_status>
     *  ...
     * </game>
	 */
	public void loadGameFileAndUpdateStatus()
    { 
		final String method = "loadGameFile"; 
    	Log.i(DEBUG_TAG, method+": Open the local game.xml file and parse it for game info.");
    	final Game game = new Game();
    	new Thread() 
    	{
            public void run() 
            {
            	FileInputStream fis = null;
				try 
				{
					fis = openFileInput("game.xml");
					//Log.i(DEBUG_TAG, method+": fis "+fis.available());
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
                	String parse_player_id = "";
                	boolean capture = false;
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
                        	if (capture = true)
                        	{
                        		if (tag!=null)
                        		{
                        			String value = parser.getText();
                        			if (tag.equals("test_id"))
                        			{
                        				game.setTestId(value);
                        				//Log.i(DEBUG_TAG, "test_id loaded "+value);
                        			} else if (tag.equals("class_id"))
                        			{
                        				class_id = value;
                        				//Log.i(DEBUG_TAG, "class_id loaded "+class_id);
                        			} else if (tag.equals("test_status"))
                        			{
                        				game.setTestStatus(game_status);
                        				//game.setTestStatus(value);
                        				//game_status = value;
                        				//Log.i(DEBUG_TAG, "loaded "+test_status);
                        			} else if (tag.equals("test_name"))
                        			{
                        				game.setTestName(value);
                        				//Log.i(DEBUG_TAG, "loaded "+test_name);
                        			} else if (tag.equals("player_id"))
                        			{
                        				//student_id = value;
                        				//Log.i(DEBUG_TAG, "added player_id "+value);
                        			} else if (tag.equals("player_status"))
                        			{
                        				//Log.i(DEBUG_TAG, "game.setPlayerStatus("+parse_player_id+","+value+") for test_name "+game.getTestName());
                        				game.setPlayerStatus(parse_player_id, value);
                        				parse_player_id = "";
                        			}
                        		}
                        		capture = false;
                        		tag = null;
                        	}
                        		
                        case XmlPullParser.START_TAG:
                        	tag = parser.getName();
                        	capture = true;
                        	//Log.i(DEBUG_TAG, tag+" attribute "+parser.getAttributeValue(null, "id"));
                        	parse_player_id = parser.getAttributeValue(null, "id");
                        }
                        parser_event = parser.next();
                    }
                    
                } catch (Exception e) 
                {
                	Log.i(DEBUG_TAG, method+": (not) exception(al)");
                	e.printStackTrace();
                }
                saveGameFileAndStatus(game);
            }
    	}.start();
    	//Log.i(DEBUG_TAG, method+": finished");
    }
	
	/**
	 * If set the current player status to ready and save the game file.
     * Write all the game.xml file with the following format:
     * <game>
     * 	<test_name>
     * 	<test_id>
     * 	<test_type>
     * 	<test_status>
     * 	<test_format>
     *  <player_id>    
     *  <player_status id="7655807335881695697">setup</player_status>
     *   ...
     * </game>
     * @param selected_test_name
     * @param selected_test_id
     */
    private void saveGameFileAndStatus(Game game)
    {
    	String method = "saveGameFile";
        Log.i(DEBUG_TAG, method+": Using a string buffer, we create the initial players.xml file with a new entry for the first player with a default icon name.");
    	try 
    	{
    		FileOutputStream fos = openFileOutput(UtilityTo.GAME_XML, Context.MODE_PRIVATE);
    		//Log.i(DEBUG_TAG, method+": FD "+fos.getFD());
	        try
	        {
	        	StringBuffer sb = new StringBuffer();
				sb.append("<game>");
				sb.append("<test_name>"+game.getTestName()+"</test_name>");
				sb.append("<test_id>"+game.getTestId()+"</test_id>");
				sb.append("<class_id>"+class_id+"</class_id>");
				sb.append("<test_type>"+game.getTestType()+"</test_type>");
				//sb.append("<test_status>"+game.getTestStatus()+"</test_status>");
				sb.append("<test_status>"+game_status+"</test_status>");
				sb.append("<test_format>"+game.getTestFormat()+"</test_format>");
				Enumeration<String> e =game.getPlayerStatus().keys();
				//Log.i(DEBUG_TAG, " students in id_status "+game.getPlayerStatus().size());
                while (e.hasMoreElements())
                {
					String this_player_id = e.nextElement();
					String status = game.getPlayerStatus(this_player_id);
					if (this_player_id.equals(player_id))
					{
						//Log.i(DEBUG_TAG, "Changed "+this_player_id+" player_status from "+status+" to ready");
						status = UtilityTo.READY;
					}
					sb.append("<player_status id=\""+this_player_id+"\">"+status+"</player_status>");
					//Log.i(DEBUG_TAG, "<player_status id="+this_player_id+">"+status+"</player_status>");
				}
				sb.append("</game>");
				//Log.i(DEBUG_TAG, "writing "+new String(sb));
				fos.write(new String(sb).getBytes());
				fos.close();
				//Log.i(DEBUG_TAG, method+": done");
	        } catch (FileNotFoundException e)
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
     * Get the info about the card from word_position hash which has the word as the key.
     * Then update the list to highlight that item.  Create an id for the card.
     * Put all the info about that word in a Card object and return that.
     * @param selected_word
     * @return
     */
    private Card itemSelected(String selected_word)
    {
    	String method = "itemSelected()";
    	Integer word_pos = word_position.get(selected_word);
    	Log.i(DEBUG_TAG, "word pos "+word_pos.toString()+" added "+selected_word+" to selected_words");
    	//FINISHED_WORDS[selected] = UtilityTo.SET;
    	//list_view = getListView();
    	//ArrayAdapter<?> adapter = (ArrayAdapter<?>) list_view.getAdapter();
    	//adapter.notifyDataSetChanged();
    	Card new_card = position_cards.get(word_pos);
    	//Log.i(DEBUG_TAG, method+"word pos "+word_pos.toString()+" added "+selected_word+" got card "+UtilityTo.getWord(new_card));
    	long new_card_id = UtilityTo.getNewID();
    	new_card.setCardId(new_card_id+"");
    	dumpCard(new_card);
    	return new_card;
    }
    
    private void selectedItemForHighlighting(String selected_word)
    {
    	String method = "selectedItemForHighlighting";
    	Integer word_pos = word_position.get(selected_word);
    	//Log.i(DEBUG_TAG, "word pos "+word_pos.toString()+" added "+selected_word+" to selected_words");
    	FINISHED_WORDS[selected] = UtilityTo.SET;
    	list_view = getListView();
    	ArrayAdapter<?> adapter = (ArrayAdapter<?>) list_view.getAdapter();
    	adapter.notifyDataSetChanged();
    }
    
    /**
     * Only add a card to the file if it meets three conditions"
     * the word id/type/player_id ARE ALL different from any other words in the cards.xml file,
     * @param new_card
     * @return
     */
    private boolean checkIfWordIsAlreadyRead(Card new_card)
    {
    	String method = "checkIfWordIsAlreadyRead(new_card)";
    	Log.i(DEBUG_TAG, method+" file_cards.size() "+file_cards.size());
    	boolean written = false;
    	for (int i=0;i<file_cards.size();i++)
    	{
    		Card previously_written_card = (Card)file_cards.get(i);
    		String previously_written_card_word_id = previously_written_card.getWordId();
    		String previously_written_card_word_type = previously_written_card.getWordType();
    		String previously_written_card_player_id = previously_written_card.getPlayerId();
    		String this_card_word_id = new_card.getWordId();
    		String this_card_word_type = new_card.getWordType();
    		String this_card_player_id = new_card.getPlayerId();
    		//Log.i(DEBUG_TAG, i+" previously_written_card_word_id "+previously_written_card_word_id+" this_card_word_id "+this_card_word_id);
    		//Log.i(DEBUG_TAG, i+" previously_written_card_word_type "+previously_written_card_word_type+" this_card_word_type "+this_card_word_type);
    		//Log.i(DEBUG_TAG, i+" previously_written_card_player_id "+previously_written_card_player_id+" this_card_player_id "+this_card_player_id);
    		if (previously_written_card_word_id.equals(this_card_word_id)&&previously_written_card_word_type.equals(this_card_word_type)&&previously_written_card_player_id.equals(this_card_player_id))
    		{
    			//Log.i(DEBUG_TAG, "return false: word already written");
    			//Log.i(DEBUG_TAG, i+" previously_written_card_word_id "+previously_written_card_word_id+" this_card_word_id "+this_card_word_id);
        		//Log.i(DEBUG_TAG, i+" previously_written_card_word_type "+previously_written_card_word_type+" this_card_word_type "+this_card_word_type);
        		//Log.i(DEBUG_TAG, i+" previously_written_card_player_id "+previously_written_card_player_id+" this_card_player_id "+this_card_player_id);
    			written = true;
    			break;
    		}

    	}
    	Log.i(DEBUG_TAG, "card already written "+written);
    	return written;
    }
    
    /**
     * Saved all the card info in the card.xml file, which has the following format:
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
    private void savedCardsFile()
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
				for (int i=0;i<file_cards.size();i++)
				{
					Card card = file_cards.get(i);
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
					//Log.i(DEBUG_TAG, " card.getCardId(): "+card.getCardId()+" word "+UtilityTo.getWord(card));
				}
				sb.append("</cards>");
				//Log.i(DEBUG_TAG, "writing "+new String(sb));
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
     * This method sets up whats needed to open the cards.xml file. 
     * If the file doesn't exist yet, create it.
     * Then call the parseCards() method.
     */
    private void loadCardsFile()
    {
    	String method = "loadCards";
    	previously_selected_words = new Vector<String>();
    	previously_written_words_ids = new Hashtable <String,String>();
    	Context context = getApplicationContext();
    	String file_path = context.getFilesDir().getAbsolutePath();//returns current directory.
    	//Log.i(DEBUG_TAG, method+": file_path - "+file_path);
    	File file = new File(file_path, UtilityTo.CARDS_XML);
    	boolean exists = file.exists();
    	if (exists == false)
    	{
    		createNewCardsFile();
    		//Log.i(DEBUG_TAG,"created new cards.xml file");
    	} else
    	{
    		parseCards();
    	}
    }

    
    /**
     * Load the cards.xml file and then call parseCards to do it.
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
    	final String method = "parsePlayers"; 
    	//Log.i(DEBUG_TAG, method+": start parse");
    	new Thread() 
    	{
            public void run() 
            {
            	FileInputStream fis = null;
				try 
				{
					fis = openFileInput(UtilityTo.CARDS_XML);
					//Log.i(DEBUG_TAG, method+": fis "+fis.available());
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
                	boolean capture = false; boolean card_start = false;
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
                        	if (capture = true)
                        	{
                        		if (tag!=null)
                        		{
                        			String value = parser.getText();
                        			if (tag.equals("card_id"))
                        			{
                        				card.setCardId(value);
                        				//Log.i(DEBUG_TAG, "parsed: "+tag+" "+value);
                        			} else if (tag.equals("card_status"))
                        			{
                        				card.setCardStatus(value);
                        				//Log.i(DEBUG_TAG, "parsed: "+tag+" "+value);
                        			} else if (tag.equals("player_id"))
                        			{
                        				card.setPlayerId(value);
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
                        				file_cards.add(card);
                        				if (card.getPlayerId().equals(player_id))
                        				{
                        					if (card.getWordType().equals(UtilityTo.READING))
                        					{
                        						previously_selected_words.add(card.getText());
                        						previously_written_words_ids.put(card.getText(), card.getPlayerId());
                        						//Log.i(DEBUG_TAG,card.getText()+" text set in reading");
                        					} else
                        					{
                        						previously_selected_words.add(card.getDefinition());
                        						previously_written_words_ids.put(card.getDefinition(), card.getPlayerId());
                        						//Log.i(DEBUG_TAG, card.getDefinition()+" def set in writing");
                        					}
                        					//Log.i(DEBUG_TAG, "card.getPlayerId("+card.getPlayerId()+").equals("+player_id+")");
                        				}
                        				capture = false;
                        				card_start = false;
                        				card = new Card();
                        			}
                        		}
                        		capture = false;
                        		tag = null;
                        	}
                        		
                        case XmlPullParser.START_TAG:
                        	tag = parser.getName();
                        	//Log.i(DEBUG_TAG, "catpture set");
                        }
                        parser_event = parser.next();
                    }
                } catch (Exception e) 
                {
                	Log.i(DEBUG_TAG, method+": (not) exception(al)");
                	e.printStackTrace();
                }
               // Log.i(DEBUG_TAG, method+" file_cards size after load "+file_cards.size());
                setUpPlayerNamesAndExtras();  // set up words array
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
                	Log.i(DEBUG_TAG, method+" list_view is null");
                }
            }
    	}.start();
    	Log.i(DEBUG_TAG, method+": finished");
    }
    
    @Override
    protected void onNewIntent(Intent intent)
    {
    	String action = intent.getAction();
    	new_intent = intent;
    	String nfc_action = NfcAdapter.ACTION_TAG_DISCOVERED;
    	Log.i(DEBUG_TAG, "onNewIntent(intent) action "+intent.getAction());
    	Log.i(DEBUG_TAG, "onNewIntent(intent) nfc_action "+intent.getAction());
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) 
        {
            // reag TagTechnology object...
        	card_tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        	Log.i(DEBUG_TAG, "onNewIntent(intent) ACTION_TAG_DISCOVERED");
        } else if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) 
        {
            // read NDEF message...
        	card_tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        	Log.i(DEBUG_TAG, "onNewIntent(intent) ACTION_NDEF_DISCOVERED");
        } else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) 
        {
        	card_tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        	Log.i(DEBUG_TAG, "onNewIntent(intent) ACTION_TECH_DISCOVERED");
        } else
        {
        	Log.i(DEBUG_TAG, "onNewIntent: ACTION (not) DISCOVERED");
        }

    }

    /**
     * From http://www.tapwise.com/svn/nfcwritetag/trunk/src/com/tapwise/nfcwritetag/MainActivity.java
     * @param new_card
     * @return
     */
    private String readWriteFormatReject(Card new_card)
    {
    	String method = "readWriteFormatReject";
    	String card_id = new_card.getCardId();
        Tag detectedTag = new_intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if(supportedTechs(card_tag.getTechList())) 
        {
        	Log.i(DEBUG_TAG, method+" supported techs");
            // check if tag is writable (to the extent that we can
            if(writableTag(card_tag)) 
            {
            	String existing_message = writeTag(getTagAsNdef(new_card.getCardId()), detectedTag);
            	String message = "This tag can be written, id from the new card is: "+new_card.getCardId();
            	Log.i(DEBUG_TAG, method+" "+message);
            	//Toast.makeText(context,message,Toast.LENGTH_SHORT).show();
            } else 
            {
            	String existing_message = getExistingTagID();
            	String message = "This tag is not writable. existing_message "+existing_message;
            	Log.i(DEBUG_TAG, method+" "+message);
            	Log.i(DEBUG_TAG, method+" existing message "+existing_message);
            	//Toast.makeText(context,message,Toast.LENGTH_SHORT).show();
            	card_id = existing_message;
            }	            
        } else 
        {
        	String existing_message = getExistingTagID();
        	Log.i(DEBUG_TAG, method+"un-supported techs "+existing_message);
        	//Toast.makeText(context,"This tag type is not supported",Toast.LENGTH_SHORT).show();
        	card_id = existing_message;
        }
        try
        {
        	if (card_id.equals(""))
        	{
        		card_id = null;
        		Log.i(DEBUG_TAG, method+" card_id set to null");
        	}
        } catch (java.lang.NullPointerException npe)
        {
        	Log.i(DEBUG_TAG, method+" card_id is null");
        }
        return card_id;
    }
    
    /**
     * From http://www.tapwise.com/svn/nfcwritetag/trunk/src/com/tapwise/nfcwritetag/MainActivity.java
     * @param techs
     * @return
     */
    public static boolean supportedTechs(String[] techs) 
    {
    	UtilityTo.printArray(techs, "tech list");
	    boolean mifare_classic=false;
	    boolean nfcA=false;
	    boolean ndef=false;
	    for(String tech:techs) 
	    {
	    	if(tech.equals("android.nfc.tech.MifareClassic")) 
	    	{
	    		mifare_classic=true;
	    	}else if(tech.equals("android.nfc.tech.NfcA")) 
	    	{
	    		nfcA=true;
	    	} else if(tech.equals("android.nfc.tech.Ndef") || tech.equals("android.nfc.tech.NdefFormatable")) {
	    		ndef=true;
	    	}
	    }
        if(mifare_classic && nfcA && ndef) 
        {
        	return true;
        } else 
        {
        	return false;
        }
	}
    
    /**
     * From http://www.tapwise.com/svn/nfcwritetag/trunk/src/com/tapwise/nfcwritetag/MainActivity.java
     * @param tag
     * @return
     */
    private boolean writableTag(Tag tag) 
    {

        try
        {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) 
            {
                ndef.connect();
                if (!ndef.isWritable()) 
                {
                    //Toast.makeText(context,"Tag is read-only.",Toast.LENGTH_SHORT).show();
                    ndef.close(); 
                    return false;
                }
                ndef.close();
                return true;
            } 
        } catch (Exception e) 
        {
            //Toast.makeText(context,"Failed to read tag",Toast.LENGTH_SHORT).show();
        }

        return false;
    }

    /**
     * From http://www.tapwise.com/svn/nfcwritetag/trunk/src/com/tapwise/nfcwritetag/MainActivity.java
     * @param message
     * @param tag
     * @return
     */
    public String writeTag(NdefMessage message, Tag tag) 
    {
    	String method = "writeTag";
    	int size = message.toByteArray().length;
        String mess = "";
        try 
        {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) 
            {
                ndef.connect();
                if (!ndef.isWritable()) 
                {
                	String existing_message = getExistingTagID();
                	String debug_message = "Tag is read-only. Exising message "+existing_message;
                	Log.i(DEBUG_TAG, method+" "+debug_message);
                	//Toast.makeText(context,debug_message,Toast.LENGTH_SHORT).show();
                	return existing_message;
                }
                if (ndef.getMaxSize() < size) 
                {
                	String existing_message = getExistingTagID();                    
                    String debug_message = "Tag capacity is " + ndef.getMaxSize() + " bytes, message is " + size
                            + " bytes.  Existing message "+existing_message;
                	Log.i(DEBUG_TAG, method+" "+debug_message);
                    //Toast.makeText(context, mess, Toast.LENGTH_SHORT).show();
                    return existing_message;
                }
                ndef.writeNdefMessage(message);
                //if(writeProtect)  ndef.makeReadOnly();
               // mess = "Wrote message to pre-formatted tag. Status 1";
                Log.i(DEBUG_TAG, method+" "+mess);
                //Toast.makeText(context, mess, Toast.LENGTH_SHORT).show();
                return "wrote card";
            } else 
            {
                NdefFormatable format = NdefFormatable.get(tag);
                if (format != null) 
                {
                    try 
                    {
                        format.connect();
                        format.format(message);
                        mess = "Formatted tag and wrote message.  status 1";
                        Log.i(DEBUG_TAG, method+" "+mess);
                        //Toast.makeText(context, mess, Toast.LENGTH_SHORT).show();
                        return "formatted and wrote";
                    } catch (IOException e) 
                    {
                    	String existing_message = getExistingTagID();
                        mess = "Failed to format tag. status 0.  existing_message "+existing_message;
                       // Toast.makeText(context, mess, Toast.LENGTH_SHORT).show();
                        Log.i(DEBUG_TAG, method+" "+mess);
                        getExistingTagID();
                        return existing_message;
                    }
                } else 
                {
                	String existing_message = getExistingTagID();
                    mess = "Tag doesn't support NDEF. status 0. existing_message "+existing_message;
                    Log.i(DEBUG_TAG, method+" "+mess);
                    //Toast.makeText(context, mess, Toast.LENGTH_SHORT).show();
                    getExistingTagID(); // ???
                    return existing_message;
                }
            }
        } catch (Exception e) 
        {
        	String existing_message = getExistingTagID();
            mess = "Failed to write tag.  existing_message "+existing_message;
            Log.i(DEBUG_TAG, method+" "+mess);
            return existing_message;
        }
    }
    
    /**
     * From http://www.tapwise.com/svn/nfcwritetag/trunk/src/com/tapwise/nfcwritetag/MainActivity.java
     * @return
     */
    private NdefMessage getTagAsNdef(String new_card_id) 
    {  
        byte[] bytes = new_card_id.getBytes(Charset.forName("UTF-8"));
        byte[] payload = new byte[bytes.length + 1]; 
        System.arraycopy(bytes, 0, payload, 1, bytes.length);  //appends URI to payload
        NdefRecord record = new NdefRecord(
        NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);
        //NdefRecord new_record = createRecord(new_card_id);
        NdefRecord new_record = createTextRecord(new_card_id);
        return new NdefMessage(new NdefRecord[] {record, new_record}); 
    }
    
    /**
     * Create the record with the message passed in in the payload.
     * The card id in sector 1.  THere will be an en and byte info there also.
     * @param payload
     * @return
     */
    public NdefRecord createTextRecord(String payload) 
    {
    	String language = "en";
    	boolean encodeInUtf8 = true;
        byte[] langBytes = language.getBytes(Charset.forName("US-ASCII"));
        Charset utfEncoding = encodeInUtf8 ? Charset.forName("UTF-8") : Charset.forName("UTF-16");
        byte[] textBytes = payload.getBytes(utfEncoding);
        int utfBit = encodeInUtf8 ? 0 : (1 << 7);
        char status = (char) (utfBit + langBytes.length);
        byte[] data = new byte[1 + langBytes.length + textBytes.length];
        data[0] = (byte) status;
        System.arraycopy(langBytes, 0, data, 1, langBytes.length);
        System.arraycopy(textBytes, 0, data, 1 + langBytes.length, textBytes.length);
        NdefRecord record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
        NdefRecord.RTD_TEXT, new byte[0], data);
        return record;
    }
    
    /**
     * Get words from the intent.  If it is a reading test, then we create arrays with
     * text and definition words with multiple redundancies to allow highlighting
     * of words that have been written to cards.  If the test words are for a Writing Stones
     * game, then we only use the writing/definition words for each Word object.
     * @return
     */
    private void setUpPlayerNamesAndExtras()
    {
    	String method = "getPlayerNamesAndExtras()";
    	initializeVariables();
    	Card card = new Card();
    	int text_def = 0;
		for(int i=0;i<number_of_words;i++)
		{
			if (!selected_test_format.equals(Constants.WRITING_STONES))
			{
				// reading
				card = setupCardFromIntent(i, UtilityTo.READING);
				WORDS[text_def] = card.getText();
				FINISHED_WORDS[text_def] = "";
				try
				{
					word_ids.put(card.getText(), card.getWordId());
				} catch (java.lang.NullPointerException nope)
				{
					Log.i(DEBUG_TAG, "NoPE "+card.getCardId());
				}	
				word_position.put(card.getText(), text_def); // not used
				position_cards.put(Integer.valueOf(text_def), card);
				//Log.i(DEBUG_TAG, "Reading word "+card.getText());
				if (previously_selected_words != null)
				{
					if (previously_selected_words.contains(card.getText()))
					{
						// if a card has be written to file already.
						if (previously_written_words_ids.get(card.getText()).equals(player_id))
						{
							FINISHED_WORDS[text_def]= UtilityTo.SET;
							//FINISHED_WORDS_PLAYER_ID[text_def]=card.getPlayerId();
						}
						//Log.i(DEBUG_TAG, "set "+card.getText()+" Utilities.getWord "+Utilities.getWord(card));
					} else
					{
						//Log.i(DEBUG_TAG, "didn't set "+card.getText()+" Utilities.getWord "+Utilities.getWord(card));
					}
				}
				text_def++;
			}
			// writing
			card = setupCardFromIntent(i, UtilityTo.WRITING);
			WORDS[text_def] = card.getDefinition();
			FINISHED_WORDS[text_def] = "";
			word_ids.put(card.getDefinition(), card.getWordId());
			word_position.put(card.getDefinition(), text_def); // not used.
			position_cards.put(Integer.valueOf(text_def), card);
			//Log.i(DEBUG_TAG, "Writing word "+card.getDefinition());
			if (previously_selected_words != null)
			{
				if (previously_selected_words.contains(card.getDefinition()))
				{
					try
					{
						if (previously_written_words_ids.get(card.getText()).equals(player_id))
						{
							FINISHED_WORDS[text_def]= UtilityTo.SET;
							//FINISHED_WORDS_PLAYER_ID[text_def]=card.getPlayerId();
							//Log.i(DEBUG_TAG, "set card player_id"+card.getPlayerId()+" Utilities.getWord "+Utilities.getWord(card));
						}
					} catch (java.lang.ArrayIndexOutOfBoundsException aioobe)
					{
						Log.i(DEBUG_TAG, "aioobe: tried to add at position "+text_def+" Utilities.getWord "+UtilityTo.getWord(card));
					} catch (java.lang.NullPointerException npe)
					{
						Log.i(DEBUG_TAG, "npe: tried to add at position "+text_def+" Utilities.getWord "+UtilityTo.getWord(card));
					}
				}
			}
			text_def++;
			//Log.i(DEBUG_TAG, " adding word "+card.getText()+" "+card.getDefinition()+" id "+card.getDefinition());
		}
		//Log.i(DEBUG_TAG, method+" word_ids size "+word_ids.size());
    }
    
    private void initializeVariables()
    {
    	Intent sender = getIntent();
    	number_of_words = Integer.parseInt(sender.getExtras().getString("number_of_words"));
    	player_id = sender.getExtras().getString("student_id");
    	selected_test_id = sender.getExtras().getString("selected_test_id");
    	selected_test_format = sender.getExtras().getString("selected_test_format");
    	Log.i(DEBUG_TAG, "read student_id/player_id"+player_id+" number_of_words "+number_of_words);
    	WORDS = new String[number_of_words*2];
    	FINISHED_WORDS = new String[number_of_words*2];
    	//FINISHED_WORDS_PLAYER_ID = new String[number_of_words*2];
    	word_ids = new Hashtable<String,String>();
    	word_position = new Hashtable<String,Integer>();
    	position_cards = new Hashtable<Integer,Card>();
    	selected_words = new Vector<String>();
    }
    
    /**
     * We don't have a card_id, player_name or icon yet, but all the other items are set.
     * All words in the intent should have the same player_id.
     * Words come from the intent like this:
     * 1index-0
     * number_of_words-3
     * selected_test_name-nue5-next_reading
     * 1text-À¯¾Æ
     * 1type-reading
     * 1definition-infant
     * 1id-3428634204343159672
     * 1category-word list.xml
     * Since vectors and lists start at 0, we add one to get the keys.
     * @param i
     * @return
     */
    private Card setupCardFromIntent(int i, String type)
    {
    	Card card = new Card();
    	Intent sender = getIntent();
    	card.setCardStatus(UtilityTo.YET_TO_BE_PLAYED);
    	card.setPlayerName(player_name);
    	card.setPlayerId(player_id);
    	card.setWordId(sender.getExtras().getString((i+1)+"id")); // starts at 1.
		card.setText(sender.getExtras().getString((i+1)+"text")); // starts at 1.
		card.setDefinition(sender.getExtras().getString((i+1)+"definition"));
		card.setIndex(sender.getExtras().getString((i+1)+"index"));
		card.setWordCategory(sender.getExtras().getString((i+1)+"category"));
		card.setWordType(type);
		return card;
    }
    
    /**
     * Try and read a tag to get the first sector to use as an id if we can't write the tag.
     * @return
     */
    private String getExistingTagID()
	{
		String method = "getExistingTagID";
		//Log.i(DEBUG_TAG, method+": called.");
		Parcelable[] raw_messages = new_intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
		NdefMessage[] ndef_messages = null;
		if (raw_messages != null) 
		{
			//Log.i(DEBUG_TAG, method+" rawMsgs != null");
			ndef_messages = new NdefMessage[raw_messages.length];
		    for (int i = 0; i < raw_messages.length; i++) 
		    {
		    	NdefMessage ndef_message = (NdefMessage)raw_messages[i];
		        //Log.i(DEBUG_TAG, "i "+i+" ndef_message = "+ndef_message.toString());
		        ndef_messages[i] = (NdefMessage) raw_messages[i];
		    }
		} else
		{
			//Log.i(DEBUG_TAG, method+" raw_messages == null");
		}
		try
		{
			NdefRecord[] records = ndef_messages[0].getRecords();
			final int size = records.length;
			for (int i = 0; i < size; i++) 
			{
				NdefRecord record = records[i];
				String message = new String(record.getPayload());
				//Log.i(DEBUG_TAG, method+" "+i+" message "+message);
				if (i==0)
				{
					return message;
				}
			}
		} catch (java.lang.NullPointerException npe)
		{
			//Log.i(DEBUG_TAG, method+" failed to write tag.");
			write_failed = true;
			npe.printStackTrace();
		}
		return null;
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.activity_card_player_words, menu);
        menu.add(0 , use_house_deck_id, 0, com.curchod.wherewithal.R.string.use_house_deck);
        return true;
    }
    
    /**
     * When the user selects "Use house deck" from the menu, we need to create a list of house decks for
     * them to select from.
     */
    @Override
	public boolean onOptionsItemSelected(MenuItem item) 
    {
    	final String method = "onOptionsItemSelected(MenuItem)";
    	getIntent();
    	if (item.getItemId() == use_house_deck_id)
    	{
    		new Thread() 
        	{
                public void run() 
                {
                	final Vector <String> house_deck_names = loadHouseDecks();
                	Log.i(DEBUG_TAG, method+": loaded "+house_deck_names.size()+" decks");
                	activity.runOnUiThread(new Runnable() 
                	{
                		  public void run() 
                		  {
                			  createDeckSelector(house_deck_names);
                		  }
                		});
                }
        	}.start();	
    	    return true;
    	}
    	return super.onOptionsItemSelected(item);
    }
    
    /**
     * Create a dialog popup to let the user select a house deck to associate with the test words.
     * With this we call associateHouseDeckWithWords.
     * @param house_deck_names
     */
    private void createDeckSelector(Vector <String> house_deck_names)
    {
    	final String method = "createDeckSelector";
    	Log.i(DEBUG_TAG, method+" creating dialog to choose deck.");
    	dialog = new Dialog(this);
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle("Select house deck");
    	ListView modeList = new ListView(this);
    	modeList.setBackgroundColor(getResources().getColor(com.curchod.wherewithal.R.color.white));
    	int size = house_deck_names.size();
    	final String[] names = new String [size];
    	for (int i = 0;i<size;i++)
    	{
    		names[i] = house_deck_names.get(i);
    	}
    	ArrayAdapter<String> modeAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, names);
    	modeList.setAdapter(modeAdapter);
    	builder.setView(modeList);
    	
    	dialog = builder.create();
    	dialog.show();
    	modeList.setOnItemClickListener(new OnItemClickListener() 
		{
	          public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
	          {
	              String selected_house_deck_name = names[position];
	              Log.i(DEBUG_TAG, method+" selected "+selected_house_deck_name+" "+selected_test_format);
	              if (selected_test_format.equals(Constants.WRITING_STONES))
	              {
	            	  associateWritingStonesWithHouseDeck(selected_house_deck_name);
	              } else
	              {		  
	            	  associateHouseDeckWithWords(selected_house_deck_name);
	              }
	              dialog.dismiss();
	          }
	    });
    }
    
    /**
     * IF the test_format is WritingStones, we set up only writing/definition words.
     * Make a list of all the deck cards in card_ids, and deck_cards_id_names.
     * Then, go thru all the word_ids and do what? (notes truncated mysteriously)
     * If the test id has a previous id that returns words, then we need to re associate the writing 
     * words with their previously associated deck cards so that the players can leave those cards
     * in their card holders.  So in this case reAssociateWritingStonesWithHouseDeck is called.
     * @param selected_house_deck_name
     */
    private void associateWritingStonesWithHouseDeck(String selected_house_deck_name)
    {
    	String method = "associateWritingStonesWithHouseDeck";
    	IWantTo i_want_to = new IWantTo(context);
    	long previously_selected_test_id = Long.parseLong(selected_test_id) - 1;
    	Log.i(DEBUG_TAG, method+"            selected_test_id "+selected_test_id);
    	Log.i(DEBUG_TAG, method+" previously_selected_test_id "+previously_selected_test_id);
        Hashtable <String, String> previous_test_words = i_want_to.getTestWords(player_id, previously_selected_test_id+"", number_of_words+"");
    	if (previous_test_words.size()>0)
    	{
    		Log.i(DEBUG_TAG, method+" Triptych writing test, re-associate using previous test associations.");
    		reAssociateWritingStonesWithHouseDeck(selected_house_deck_name, previous_test_words);
    	} else
    	{
    		Log.i(DEBUG_TAG, method+" non-triptych writing test.");
    		associateNonTriptychWritingGame(selected_house_deck_name);
    	}
    }
    
    private void associateNonTriptychWritingGame(String selected_house_deck_name)
    {
    	String method = "associateNonTriptychWritingGame";
    	deck_cards_id_names = new Hashtable <String,String>();
    	HouseDeck selected_house_deck = house_deck_names_decks.get(selected_house_deck_name);
    	Vector <String> card_ids = new Vector <String> ();
    	Hashtable<String, DeckCard> deck_cards = selected_house_deck.getCards();
    	word_deck_card_associations = new Hashtable <String,String> ();
    	Enumeration <String> d = deck_cards.keys();
    	while (d.hasMoreElements())
    	{
    		String key = (String)d.nextElement();
    		DeckCard deck_card = deck_cards.get(key);
    		card_ids.add(deck_card.getCardId());
    		deck_cards_id_names.put(deck_card.getCardId(), deck_card.getCardName());
    		Log.i(DEBUG_TAG, method+" deck card "+deck_card.getCardName()+" "+deck_card.getCardId());
    	}
    	Enumeration <String> e = word_ids.keys();
    	int number_of_cards = 0;
    	while (e.hasMoreElements())
    	{
    		String key = (String)e.nextElement();
    		//String val = word_ids.get(key);
    		int position = word_position.get(key);
    		Card card = position_cards.get(position);
    		try
    		{
    			String associated_id = card_ids.get(number_of_cards);
    			number_of_cards++;
    			card.setCardId(associated_id);
    			card.setWordType(UtilityTo.WRITING);
    			word_deck_card_associations.put(card.getCardId(), deck_cards_id_names.get(associated_id)); 
    			Log.i(DEBUG_TAG, method+" associated "+UtilityTo.getWord(card)+" with deck card "+card.getCardId()+" id "+associated_id+" name "+deck_cards_id_names.get(associated_id)+" id "+card.getPlayerId());
    			//dumpCard(card);
    			file_cards.add(card);
    		} catch (java.lang.ArrayIndexOutOfBoundsException aioob)
    		{
    			Log.i(DEBUG_TAG, method+" aioobe: number_of_cards "+number_of_cards);
    		}
    	}
    	Log.i(DEBUG_TAG, method+" file_cards size "+file_cards.size());
    	sendDeckCardAssociations(selected_house_deck_name);
    	saveHouseDeckAndCardsThenStartWordsDeckCards(selected_house_deck_name);
    }
    
    /**
     * We get the following:
     * A the previous writing words/deck card names done in getPreviousWritingCardNamesWordIds()
     * B  the current test words excluding A.done in getRemainingWritingCardNamesWordIds()
     * C deck cards minus A done in removePreviouslyAssociatedDeckCards
     * Then we associate C with B in associatePerviouslyUnusedDeckCardsWithRemainingWritingCards()
     * Then following need to be set up
     *     	word_deck_card_associations
     *     	house_deck_names_ids.get(house_deck_name)
     *     	deck_card_id_word_ids
     * For the remaining calls:
     * sendDeckCardAssociations()
     * saveHouseDeckAndCardsThenStartWordsDeckCards()
     * @param selected_house_deck_name
     * @param previous_test_words
     */
    private void reAssociateWritingStonesWithHouseDeck(String selected_house_deck_name, 
    		Hashtable <String, String> previous_test_words)
    {
    	String method = "reAssociateWritingStonesWithHouseDeck";
    	// A the previous writing words/deck card names
    	Hashtable <String,String> writing_card_names_word_ids = getPreviousWritingCardNamesWordIds(previous_test_words);
    	// B  the current test words excluding A.
    	Vector <String> remaining_writing_word_ids = getRemainingWritingCardNamesWordIds(writing_card_names_word_ids);
    	// C deck cards minus A
    	Hashtable<String, DeckCard> remaining_deck_cards = removePreviouslyAssociatedDeckCards(selected_house_deck_name);
    	// D associate C with B.
    	associatePerviouslyUnusedDeckCardsWithRemainingWritingCards(selected_house_deck_name, remaining_deck_cards, remaining_writing_word_ids);
    	// what needs to be setup to make the following calls?
    	//word_deck_card_associations
    	// house_deck_names_decks and house_deck_names_ids are set up in loadHouseDecks().
    	// deck_card_id_word_ids.get(deck_card_id)
    	//"number_of_words", file_cards.size()
    	//E add the cards that were not added in getRemainingWritingCardNamesWordIds.
    	addGameTwoCards();
    	// this is the same in all the association methods.
    	sendDeckCardAssociations(selected_house_deck_name);
    	saveHouseDeckAndCardsThenStartWordsDeckCards(selected_house_deck_name);
    }
    
    /**
     * E.
     * Add the cards that were not added in getRemainingWritingCardNamesWordIds.
     */
    private void addGameTwoCards()
    {
    	String method = "addGameTwoCards";
    	Log.i(DEBUG_TAG, method+" "+game_two_writing_cards.size());
    	for (int i = 0; i < game_two_writing_cards.size(); i++)
    	{
    		try
    		{
    			Card game_two_writing_card = game_two_writing_cards.get(i);
    			String definition =  encodeThis(game_two_writing_card.getDefinition().trim());
    			//Log.i(DEBUG_TAG, i+" E: game_two_writing_card: array "+Arrays.toString(definition.toCharArray()));
    			String deck_card_name = writing_word_definition_card_names.get(definition);
    			//Log.i(DEBUG_TAG, i+" E: trying deck_card_name "+deck_card_name);
    			DeckCard deck_card = deck_card_name_deck_cards.get(deck_card_name);
    			game_two_writing_card.setCardId(deck_card.getCardId());
    			//dumpCard(game_two_writing_card);
    			word_deck_card_associations.put(deck_card.getCardId(), deck_card.getCardName()); 
    			Log.i(DEBUG_TAG, i+" E: "+deck_card.getCardId()+" "+deck_card.getCardName()+" "+game_two_writing_card.getDefinition());
    			deck_card_id_word_ids.put(deck_card.getCardId(), game_two_writing_card.getWordId());
    			deck_cards_id_names.put(deck_card.getCardId(), deck_card.getCardName());
    			file_cards.add(game_two_writing_card);
    		} catch (java.lang.NullPointerException npe)
    		{
    			Log.i(DEBUG_TAG, "E: NPE for "+i);
    			dumpCard(game_two_writing_cards.get(i));
    			npe.printStackTrace();
    		}
    	}
    }
    
    /**
     * D.
     * D-1: remaining deck cards loop
     * D-2: remaining_writing_card_ids
     *  Associate C, the deck cards minus the previous writing words/deck card names with
     * B, the current test words also excluding the previous writing words/deck card names.
     * 
     */
    private void associatePerviouslyUnusedDeckCardsWithRemainingWritingCards(String selected_house_deck_name,
    		Hashtable<String, DeckCard> remaining_deck_cards, Vector <String> remaining_writing_word_ids)
    {
    	String method = "associatePerviouslyUnusedDeckCardsWithRemainingWritingCards";
    	Log.i(DEBUG_TAG, method+" file_cards "+file_cards.size()+" before remaining cards association");
    	deck_cards_id_names = new Hashtable <String,String>();
    	HouseDeck selected_house_deck = house_deck_names_decks.get(selected_house_deck_name);
    	Vector <String> card_ids = new Vector <String> ();
    	//Hashtable<String, DeckCard> deck_cards = selected_house_deck.getCards();
    	word_deck_card_associations = new Hashtable <String,String> ();
    	deck_card_id_word_ids = new Hashtable <String,String> ();
    	deck_card_name_types = new Hashtable <String,String> ();
    	Vector<String> remaining_deck_card_ids = new Vector<String> ();
    	Enumeration <String> d = remaining_deck_cards.keys();
    	while (d.hasMoreElements())
    	{
    		String key = (String)d.nextElement();
    		DeckCard deck_card = remaining_deck_cards.get(key);
    		deck_card.setType(UtilityTo.WRITING);
    		card_ids.add(deck_card.getCardId());
    		deck_cards_id_names.put(deck_card.getCardId(), deck_card.getCardName());
    		deck_card_name_types.put(deck_card.getCardName(), deck_card.getType());
    		remaining_deck_card_ids.add(deck_card.getCardId());
    		Log.i(DEBUG_TAG, "D-1: deck card "+deck_card.getCardName()+" "+deck_card.getCardId()+" type "+deck_card.getType());
    	}
    	// go thru C
    	Enumeration <Integer> e = position_cards.keys();
    	int i = 0;
    	while (e.hasMoreElements())
    	{
    		Integer key = (Integer)e.nextElement();
    		Card card = position_cards.get(key);
    		if (remaining_writing_word_ids.contains(card.getWordId()))
    		{
    			String associated_id = remaining_deck_card_ids.get(i);
    			i++;
    			card.setCardId(associated_id);
    			card.setWordType(UtilityTo.WRITING);
    			word_deck_card_associations.put(card.getCardId(), deck_cards_id_names.get(associated_id)); 
    			Log.i(DEBUG_TAG, "D-2: "+card.getCardId()+" "+deck_cards_id_names.get(associated_id));
    			deck_card_id_word_ids.put(card.getCardId(), card.getWordId());
    			//dumpCard(card);
    			file_cards.add(card);
    		}
    	}
    }
    
    /**
     * C.
     * Go through the house deck and remove the cards from the first array.
     * C-1: Deck cards that are not previously used.
     * C-2: Deck cards that are previously associated with game cards.
     */
	private Hashtable<String, DeckCard> removePreviouslyAssociatedDeckCards(String selected_house_deck_name)
    {
		String method = "removePreviouslyAssociatedDeckCards";
		HouseDeck selected_house_deck = house_deck_names_decks.get(selected_house_deck_name);
    	Vector <String> card_ids = new Vector <String> ();
    	deck_card_name_deck_cards = new Hashtable <String,DeckCard> ();
    	Hashtable<String, DeckCard> deck_cards = selected_house_deck.getCards();
    	Hashtable<String, DeckCard> deck_cards_wo_previous_writing_cards = new Hashtable<String, DeckCard> ();
    	Enumeration <String> d = deck_cards.keys();
    	while (d.hasMoreElements())
    	{
    		String key = (String)d.nextElement();
    		DeckCard deck_card = deck_cards.get(key);
    		String deck_card_name = deck_card.getCardName();
    		if (!previously_used_deck_names.contains(deck_card_name)||deck_card_name.substring(0, 1).equals("R"))
			{
    			deck_cards_wo_previous_writing_cards.put(deck_card.getCardId(), deck_card);
    			//Log.i(DEBUG_TAG, "C-1: "+deck_card.getCardId()+" "+deck_card.getCardName());
			} else
			{
				deck_card_name_deck_cards.put(deck_card.getCardName(), deck_card); // used in E to provide a ref from the card/deck_card id to the deck card name.
				Log.i(DEBUG_TAG, "C-2: "+deck_card.getCardId()+" "+deck_card.getCardName());
			}
    	}
		return deck_cards_wo_previous_writing_cards;
    }
	
	/**
     * B.
     * Return the current test words excluding the previous writing words/deck card names
     * B-1 list is the returned vector of remaining writing card ids.
     * B-2 sets up the already associated cards from the 2nd test words by adding them to 
     * word_deck_card_associations, deck_card_id_word_ids and file_cards.
     * @return
     */
    private Vector <String> getRemainingWritingCardNamesWordIds(Hashtable <String,String> writing_card_names_word_ids)
    {
    	String method = "getRemainingWritingCardNamesWordIds";
    	Vector <String> remaining_writing_card_ids = new Vector<String>();
    	game_two_writing_cards = new Vector <Card> ();
    	//Log.i(DEBUG_TAG, method+" file_cards "+file_cards.size()); no file cards at this point
    	Log.i(DEBUG_TAG, method+" position_cards "+position_cards.size());
    	Enumeration <Integer> e = position_cards.keys();
    	while (e.hasMoreElements())
    	{
    		Integer key = (Integer)e.nextElement();
    		Card card = position_cards.get(key);
			if (!previous_writing_word_ids.contains(card.getWordId()))
			{
				remaining_writing_card_ids.add(card.getWordId()); // this will be used to associate remaining deck cards in C the cards with B.
				//Log.i(DEBUG_TAG, "B-1 "+card.getWordId()+" "+card.getDefinition());
			} else
			{
				game_two_writing_cards.add(card); // This is set up for E.
				Log.i(DEBUG_TAG, "B-2 "+card.getWordId()+" "+card.getDefinition());
			}
		}
    	return remaining_writing_card_ids;
    }
    
    /**
     * A.
     * Make an array of only the writing card names/word ids.
     * The hash writing_word_definition_card_names will be used in E to retrieve the previously used deck card name.
     * @param previous_test_words
     * @return
     */
    private Hashtable <String,String> getPreviousWritingCardNamesWordIds(Hashtable <String, String> previous_test_words)
    {
    	String method = "getPreviousWritingCardNamesWordIds";
    	previous_writing_word_ids = new Vector<String>();
    	previously_used_deck_names = new Vector <String> ();
    	writing_word_definition_card_names = new Hashtable <String,String>();
    	Hashtable <String,String> writing_card_names_word_ids = new Hashtable <String,String>();
    	Log.i(DEBUG_TAG, method+"number_of_words "+number_of_words+" to iterate over.");
    	for (int i = 1; i <= number_of_words/2; i++)
		{
			String word_id = (String)previous_test_words.get(i+"id");
			String writing_deck_card_name = (String)previous_test_words.get(i+"writing_deck_card_name");
			String definition = encodeThis((String)previous_test_words.get(i+"definition")).trim();
			writing_card_names_word_ids.put(writing_deck_card_name, word_id);
			writing_word_definition_card_names.put(definition, writing_deck_card_name);
			previously_used_deck_names.add(writing_deck_card_name);
			previous_writing_word_ids.add(word_id);
			Log.i(DEBUG_TAG, "A: "+writing_deck_card_name+" "+definition+" "+word_id);
		}
    	return writing_card_names_word_ids;
    }
    
    private String encodeThis(String message)
    {
    	String temp = null;
    	byte [] utf8bytes = message.getBytes();
    	try {
			temp = new String(utf8bytes, "UTF8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return temp;		
    }
    
    private void printSavedTest(Hashtable <String, String> previous_test_words)
    {
    	String method = "printSavedTest";
    	Enumeration <String> d = previous_test_words.keys();
    	int number_of_previous_words = 1; // words always start at 1.
    	while (d.hasMoreElements())
    	{
    		String key = (String)d.nextElement();
    		String val = previous_test_words.get(key);
    		Log.i(DEBUG_TAG, method+" key "+key+" val "+val);
    		if (key.contains("definition"))
    		{
    			number_of_previous_words++;
    		}
    	}
    	Log.i(DEBUG_TAG, method+" number_of_previous_words "+number_of_previous_words);
    }
    
    /**
     * Separate the deck cards into reading and writing types.  Then associate each deck card
     * with a word in the word_ds hash.
     * @param selected_house_deck_name
     */
    private void associateHouseDeckWithWords(String selected_house_deck_name)
    {
    	String method = "associateHouseDeckWithWords";
    	deck_cards_id_names = new Hashtable <String,String>();
    	HouseDeck selected_house_deck = house_deck_names_decks.get(selected_house_deck_name);
    	Vector <String> reading_card_ids = new Vector <String> ();
    	Vector <String> writing_card_ids = new Vector <String> ();
    	word_deck_card_associations = new Hashtable <String,String> ();
    	deck_card_id_word_ids = new Hashtable <String,String> ();
    	deck_card_name_types = new Hashtable <String,String> ();
    	Hashtable<String, DeckCard> deck_cards = selected_house_deck.getCards();
    	Enumeration <String> d = deck_cards.keys();
    	while (d.hasMoreElements())
    	{
    		String key = (String)d.nextElement();
    		DeckCard deck_card = deck_cards.get(key);
    		if (deck_card.getType().equals(UtilityTo.READING))
    		{
    			reading_card_ids.add(deck_card.getCardId());
    		} else if (deck_card.getType().equals(UtilityTo.WRITING))
    		{
    			writing_card_ids.add(deck_card.getCardId());
    		}
    		deck_cards_id_names.put(deck_card.getCardId(), deck_card.getCardName());
    		deck_card_name_types.put(deck_card.getCardName(), deck_card.getType());
    		Log.i(DEBUG_TAG, method+" deck card "+deck_card.getCardName()+" id "+deck_card.getCardId()+" type "+deck_card.getType());
    	}
    	Enumeration <String> e = word_ids.keys();
    	int number_of_reading_card = 0;
    	int number_of_writing_card = 0;
    	while (e.hasMoreElements())
    	{
    		String key = (String)e.nextElement();
    		String val = word_ids.get(key);
    		int position = word_position.get(key);
    		Card card = position_cards.get(position);
    		String type = card.getWordType();
    		Log.i(DEBUG_TAG, method+" type "+type+" - "+card.getDefinition());
    		String associated_id = null;
    		if (type.equals(UtilityTo.READING))
    		{
    			if (number_of_reading_card<reading_card_ids.size())
    			{
    				associated_id = reading_card_ids.get(number_of_reading_card);
    				number_of_reading_card++;
    			}
    		} else if (type.equals(UtilityTo.WRITING))
    		{
    			if (number_of_writing_card<writing_card_ids.size())
    			{
    				associated_id = writing_card_ids.get(number_of_writing_card);
    				number_of_writing_card++;
    			}
    		}
    		card.setCardId(associated_id);
    		word_deck_card_associations.put(card.getCardId(), deck_cards_id_names.get(associated_id)); 
    		deck_card_id_word_ids.put(card.getCardId(), card.getWordId());
			Log.i(DEBUG_TAG, method+" associated "+UtilityTo.getWord(card)+" with deck card "+card.getCardId()+" word_id "+card.getWordId()+" name "+deck_cards_id_names.get(associated_id));
			file_cards.add(card);
    	}
    	//Log.i(DEBUG_TAG, method+" file_cards size "+file_cards.size());
    	sendDeckCardAssociations(selected_house_deck_name);
    	saveHouseDeckAndCardsThenStartWordsDeckCards(selected_house_deck_name);
    }

    /**
     * This method sends the name value pairs created in setupDeckCardAssociationsPairs to the
     * action word_deck_card_associations.do.  This action will add the deck card associations
     * to the saved test word file for the user.
     */
    private void sendDeckCardAssociations(String selected_house_deck_name)
    {
    	 DefaultHttpClient httpclient = new DefaultHttpClient();
         HttpPost httppost = new HttpPost("http://211.220.31.50:8080/indoct/word_deck_card_associations.do");     
         httppost.addHeader("Accept", "text");
         httppost.addHeader("Content-Type", "application/x-www-form-urlencoded");
         List<NameValuePair> name_value_pairs = setupDeckCardAssociationsPairs(selected_house_deck_name);
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
    
    /**
     * Set up a list of name value pairs to send to the server.  The format is similar to the
     * way we send info in between activities in the intent.
     * First there is this info:
     * number_of_words
     * player_id
     * test_id
     * Then, we iterate through the file_cards and add:
     * i+"deck_card_name"
     * i+"word_id"
     * @return
     */
    private List<NameValuePair> setupDeckCardAssociationsPairs(String house_deck_name)
    {
        String method = "setupDeckCardAssociationsPairs()";
        List<NameValuePair> name_value_pairs = new ArrayList<NameValuePair>();
        String number_of_words = word_deck_card_associations.size()+""; 
        Log.i(DEBUG_TAG, method+" number_of_words "+number_of_words);
        Log.i(DEBUG_TAG, method+" house_deck_name "+house_deck_name);
        Log.i(DEBUG_TAG, method+" selected_test_format "+selected_test_format);
        name_value_pairs.add(new BasicNameValuePair("number_of_words", number_of_words));
        name_value_pairs.add(new BasicNameValuePair("player_id", player_id));
        name_value_pairs.add(new BasicNameValuePair("test_id",selected_test_id)); 
        name_value_pairs.add(new BasicNameValuePair("test_format",selected_test_format));
        name_value_pairs.add(new BasicNameValuePair("house_deck_name", house_deck_name));
        name_value_pairs.add(new BasicNameValuePair("house_deck_id", house_deck_names_ids.get(house_deck_name)));
        int i = 1;
        Enumeration <String> e = word_deck_card_associations.keys();
        while (e.hasMoreElements())
        {
        	String deck_card_id = (String) e.nextElement();
        	String deck_card_name = word_deck_card_associations.get(deck_card_id);
        	String type = deck_card_name_types.get(deck_card_name);
        	if (type == null)
        	{
        		type = UtilityTo.WRITING; // this happens for the third test in a triptych.
        	}
        	name_value_pairs.add(new BasicNameValuePair(i+"word_type", type));
        	if (type.equals(UtilityTo.READING))
        	{
        		name_value_pairs.add(new BasicNameValuePair(i+"reading_deck_card_name", deck_card_name));
        		Log.i(DEBUG_TAG, method+" put "+i+"reading_deck_card_name - "+deck_card_name);
        	} else if (type.equals(UtilityTo.WRITING))
        	{
        		name_value_pairs.add(new BasicNameValuePair(i+"writing_deck_card_name", deck_card_name));
        		Log.i(DEBUG_TAG, method+" put "+i+"writing_deck_card_name - "+deck_card_name);
        	}
        	String word_id = (String)deck_card_id_word_ids.get(deck_card_id);
            name_value_pairs.add(new BasicNameValuePair(i+"word_id", word_id));
            Log.i(DEBUG_TAG, method+" "+i+"deck_card_name "+deck_card_name+" - "+i+" word_id "+word_id+" type "+type);
            i++;
        }
        Log.i(DEBUG_TAG, method+" i from 1 to "+i);
        Log.i(DEBUG_TAG, method+" j from 0 to "+file_cards.size());
        name_value_pairs.add(new BasicNameValuePair("number_of_words", file_cards.size()+""));
        return name_value_pairs;
    }
    
    /**
     * Load the house decks and replace the player_id and test_id for the current deck 
     * then save the file again.  Then call startCardPlayerHouseDeckActivity.
     * @param selected_house_deck_name
     */
    private void saveHouseDeckAndCardsThenStartWordsDeckCards(String selected_house_deck_name)
    {
    	String method = "saveHouseDeckAndCardsThenStartWordsDeckCards";
    	savedCardsFile();
    	Log.i(DEBUG_TAG, method+" file_cards  after load "+file_cards.size());
    	IWantTo i_want_to = new IWantTo(context);
    	Hashtable <String,HouseDeck> house_decks_copy = new Hashtable <String,HouseDeck> ();
    	Hashtable <String,HouseDeck> house_decks = i_want_to.loadTheHouseDecks();
    	Enumeration <String> e = house_decks.keys();
    	while (e.hasMoreElements())
    	{
    		String key = (String)e.nextElement();
    		HouseDeck this_house_deck = house_decks.get(key);
    		String house_deck_game_id = this_house_deck.getGameId();
    		String house_deck_player_id = this_house_deck.getPlayerId();
    		String this_house_deck_name = this_house_deck.getDeckName();
    		if (this_house_deck_name.equals(selected_house_deck_name))
    		{
    			this_house_deck.setPlayerId(player_id);
    			this_house_deck.setGameId(selected_test_id);
    			Log.i(DEBUG_TAG, "house deck "+this_house_deck_name+" now used by "+player_id+" for test "+selected_test_id);
    		} else
    		{
    			Log.i(DEBUG_TAG, "copying deck "+this_house_deck_name+" used by "+house_deck_player_id+" for game "+house_deck_game_id);
    		}
    		house_decks_copy.put(key, this_house_deck);
    	}
    	i_want_to.saveTheHouseDecks(house_decks_copy);
    	startCardPlayerHouseDeckActivity(selected_house_deck_name);
    }
    
    private void startCardPlayerHouseDeckActivity(String selected_house_deck_name)
    {
    	String method = "startCardPlayerHouseDeckActivity";
    	Vector <String> check_for_repeat_words = new Vector <String>();
    	Vector <String> check_for_repeat_cards = new Vector <String>();
    	Intent intent = new Intent(CardPlayerWordsActivity.this, CardPlayerHouseDeckActivity.class);
    	int i = 1;
        for (int j = 0; j < file_cards.size(); j++)
        {
           	Card card = file_cards.get(j);
           	//if (selected_test_format.equals(Constants.WRITING_STONES))
            //{
           		//card.setWordType(UtilityTo.WRITING);
            //}
           	String deck_card_name = deck_cards_id_names.get(card.getCardId());
           	String word = UtilityTo.getWord(card);
           	Log.i(DEBUG_TAG, method+" put "+deck_card_name+" "+word);
           	intent.putExtra(i+"deck_card_name", deck_card_name);
           	intent.putExtra(i+"word", word);
           	check_for_repeat_words.add(word);		   // debug
           	check_for_repeat_cards.add(deck_card_name); // debug
           	i++;
        }
        intent.putExtra("selected_test_name", selected_house_deck_name);
        intent.putExtra("selected_test_format", selected_test_format);
        intent.putExtra("number_of_words", i+"");
        intent.putExtra("player_name", player_name);
        intent.putExtra("selected_test_id", selected_test_id);
        startActivity(intent);
    }
    
    private Vector loadHouseDecks()
    {
    	String method = "loadHouseDecks";
    	boolean house_deck_already_setup = false;
    	IWantTo i_want_to = new IWantTo(context);
    	house_deck_names_decks =  new Hashtable <String, HouseDeck> ();
    	house_deck_names_ids =  new Hashtable <String, String> ();
    	Vector <String> house_deck_names = new Vector <String> ();
    	Hashtable <String,HouseDeck> house_decks = i_want_to.loadTheHouseDecks();
    	Enumeration <String> e = house_decks.keys();
    	while (e.hasMoreElements())
    	{
    		String key = (String)e.nextElement();
    		HouseDeck this_house_deck = house_decks.get(key);
    		String house_deck_game_id = this_house_deck.getGameId();
    		String house_deck_player_id = this_house_deck.getPlayerId();
    		String this_house_deck_name = this_house_deck.getDeckName();
    		house_deck_names_decks.put(this_house_deck_name, this_house_deck);
    		house_deck_names_ids.put(this_house_deck_name, this_house_deck.getDeckId());
    		house_deck_names.add(this_house_deck_name);
    		Log.i(DEBUG_TAG, method+" collect names of house decks "+this_house_deck_name);
    	}
    	return house_deck_names;
    }
    
    /**
     * Listing 16-29: Using the foreground dispatch system
     */
    public void onPause()
    {
      super.onPause();
      try
      {
    	  nfc_adapter.disableForegroundDispatch(this);
      } catch (java.lang.NullPointerException npe)
      {
    	  Log.i(DEBUG_TAG, "back button we assume?");
      }
      
      Log.i(DEBUG_TAG, "onPause");
    }

    /**
     * Using a string buffer, we create the initial players.xml file with a new entry 
     * for the first player with a default icon name.
     */
    private void createNewCardsFile()
    {
    	String method = "createNewPlayersFile(String path_to_players_files, String name, String id)";
        //Log.i(DEBUG_TAG, method+": ");
    	try 
    	{
    		FileOutputStream fos = openFileOutput(UtilityTo.CARDS_XML, Context.MODE_PRIVATE);
	        try
	        {
	        	StringBuffer sb = new StringBuffer();
				sb.append("<cards />");
				fos.write(new String(sb).getBytes());
				fos.close();
	        } catch (FileNotFoundException e)
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

    @Override
    public void onResume() 
    {
      super.onResume();
      NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
      Log.i(DEBUG_TAG, "onResume called");
      try
      {
    	  nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray);
    	  pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, this.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
    	  nfc_adapter.enableForegroundDispatch(this,pendingIntent,writeTagFilters,techListsArray);
    	  Log.i(DEBUG_TAG, "onResume:  enableForegroundDispatch.");
      } catch (java.lang.NullPointerException npe)
      {
    	  Log.i(DEBUG_TAG, "onResume:  npeee! enableForegroundDispatch failed.");
    	  nfc_adapter = NfcAdapter.getDefaultAdapter(context);
      }
    }
    
    private void dumpCard(Card card)
    {
    	Log.i(DEBUG_TAG, "Card --- "+card.getCardId());
    	Log.i(DEBUG_TAG, "status - "+card.getCardStatus());
    	Log.i(DEBUG_TAG, "text: "+card.getText());
    	Log.i(DEBUG_TAG, "def: "+card.getDefinition());
    	Log.i(DEBUG_TAG, "indes: "+card.getIndex());
    	Log.i(DEBUG_TAG, "name: "+card.getPlayerName());
    	Log.i(DEBUG_TAG, "cat: "+card.getWordCategory());
    	Log.i(DEBUG_TAG, "word id: "+card.getWordId());
    	Log.i(DEBUG_TAG, "type: "+card.getWordType());
    	Log.i(DEBUG_TAG, "Card --- "+card.getCardId());
    }
    
    private void dumpDeckCard(DeckCard deck_card)
    {
    	Log.i(DEBUG_TAG, "Deck card id     "+deck_card.getCardId());
    	Log.i(DEBUG_TAG, "Deck card name   "+deck_card.getCardName());
    	Log.i(DEBUG_TAG, "Deck card index  "+deck_card.getIndex());
    	Log.i(DEBUG_TAG, "Deck card status "+deck_card.getStatus());
    	Log.i(DEBUG_TAG, "Deck card type   "+deck_card.getType());
    	Log.i(DEBUG_TAG, "Deck card class  "+deck_card.getClass());
    }
    
    private void dumpFinishedWords()
    {
    	Log.i(DEBUG_TAG, "SELECTED_WORDS");
    	for(int i=0;i<FINISHED_WORDS.length;i++)
    	{
    		try
    		{
    		Log.i(DEBUG_TAG, i+" "+FINISHED_WORDS[i]);
    		} catch (java.lang.NullPointerException npe)
    		{
    			Log.i(DEBUG_TAG, i+" is null");
    		}
    	}
    }
    
    /*
    private void dumpFinishedWordPlayerIds()
    {
    	Log.i(DEBUG_TAG, "FINISHED_WORDS_PLAYER_IDs");
    	for(int i=0;i<FINISHED_WORDS_PLAYER_ID.length;i++)
    	{
    		try
    		{
    		Log.i(DEBUG_TAG, i+" "+FINISHED_WORDS_PLAYER_ID[i]);
    		} catch (java.lang.NullPointerException npe)
    		{
    			Log.i(DEBUG_TAG, i+" is null");
    		}
    	}
    }
    */
    
    private void dumpPreviouslySelectedWords()
    {
    	Log.i(DEBUG_TAG, "previous_WORDS");
    	for(int i=0;i<previously_selected_words.size();i++)
    	{
    		Log.i(DEBUG_TAG, i+" "+previously_selected_words.get(i));
    	}
    }

}
