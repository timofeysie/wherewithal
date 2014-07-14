package com.curchod.wherewithal;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.nfc.tech.MifareClassic;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableLayout.*;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.curchod.domartin.Constants;
import com.curchod.domartin.HouseDeck;
import com.curchod.domartin.IWantTo;
import com.curchod.domartin.UtilityTo;
import com.curchod.dto.Card;
import com.curchod.dto.DeckCard;

public class CardDeckActivity extends Activity 
{

	private String DEBUG_TAG = "CardDeckActivity";
	Hashtable <String,DeckCard> id_deck_cards;
	private TableLayout table;
	/** To identify the rows in the table later. */
	private Hashtable <String,String> card_row_ids;
	private HouseDeck house_deck;
	final Context context = this;
	/** When a user clicks on a card, we get the card name from the view, so this allows us to get that cards id. */
	private Hashtable <String,String> deck_card_name_ids;
	// from card player words activity
	private NfcAdapter nfc_adapter;
	private NfcAdapter mfc_adapter;
	private String[][] tech_lists_array;	  
	private IntentFilter ndef_filter;
	private IntentFilter filters[];
	private IntentFilter writeTagFilters[];
	private IntentFilter[] intentFiltersArray;
	private PendingIntent pending_intent;
	boolean writeMode;
	private Tag card_tag;
	private boolean write_failed;
	boolean testing;
	private Intent new_intent;
	/** This flag lets us know that the user has selected a card to write an id to so that 
	 * the usual behavior to show the contents of the existing card is disabled. */
	private boolean writing_new_id_mode;
	private CardDeckActivity activity;
	String word_player_card;
	String deck_card_name;
	//private boolean wrote_new_id;
	private static final int reuse_id = 1;
    private static final int write_id = 2;
    private boolean write_new_id;
    private boolean reuse_selected;
    private boolean write_selected;
    private static final int id_group = 1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_card_deck); 
		String method = "onCreate";
		String build = "87";
		activity = this;
		nfc_adapter = NfcAdapter.getDefaultAdapter(context);
		Log.i(DEBUG_TAG, method+": build "+build);
		getIntentInfo();
		Button button_add_reading = (Button)findViewById(R.id.button_add_reading);
		button_add_reading.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
            	final String method = "button_add_reading.onClick";
            	createNewCard(Constants.READING);
            	//Log.i(DEBUG_TAG, method+": Hi there reading!");
            }
        });
		Button button_add_writing = (Button)findViewById(R.id.button_add_writing);
		button_add_writing.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
            	final String method = "button_add_writing.onClick";
            	//Log.i(DEBUG_TAG, method+": Hi there writing!");
            	createNewCard(Constants.WRITING);
            }
        });
		createCardsList();
		setupPendingIntent();
	}
	
	/*
	 * We create a card with the name *type**index*, like R1 or W1.
	 * Then we add it to the deck and save the deck.
	 * Then we re create the cards list.
	 * Then we save the house deck with the new card.
	 * 
	 */
	private void createNewCard(String type)
	{
		String method = "createNewCard";
		DeckCard new_card = new DeckCard();
		String card_name = "R";
		int card_index = 0;
		if (type.equals(Constants.READING))
		{
			 card_name = "R";
			 card_index = house_deck.getNumberOfReadingCards();
			 new_card.setType(Constants.READING);
		} else if (type.equals(Constants.WRITING))
		{
			 card_name = "W";
			 card_index = house_deck.getNumberOfWritingCards();
			 new_card.setType(Constants.WRITING);
		}
		card_index++;
		String new_card_name = card_name+card_index;
		new_card.setIndex(card_index);
		new_card.setCardName(new_card_name);
		new_card.setStatus(Constants.BLANK);
		long new_id = UtilityTo.getNewID();
		new_card.setCardId(new_id+"");
		id_deck_cards.put(new_id+"", new_card);
		house_deck.setCards(new_id+"", new_card);
		deck_card_name_ids.put(new_card_name, Long.toString(new_id));
		Log.i(DEBUG_TAG, method+": created new card and put "+new_card_name+" id "+new_id+" into deck_card_name_ids.");
		if (table != null)
		{
			//Log.i(DEBUG_TAG, method+": table is null");
			table.removeAllViews();
		}
    	createCardsList();
    	loadAndSaveModifiedDecks();
	}
	
	private void loadAndSaveModifiedDecks()
	{
		String method = "loadAndSaveModifiedDecks";
		IWantTo i_want_to = new IWantTo(context);
    	Hashtable <String,HouseDeck> house_decks = i_want_to.loadTheHouseDecks();
    	Log.i(DEBUG_TAG, method+": got "+house_decks.size()+" decks from file");
    	Hashtable <String,HouseDeck> copy_house_decks = i_want_to.loadTheHouseDecks();
    	Enumeration <String> e = house_decks.keys();
    	while (e.hasMoreElements())
    	{
    		String key = (String)e.nextElement();
    		HouseDeck this_house_deck = house_decks.get(key);
    		String this_house_deck_id = this_house_deck.getDeckId();
    		if (this_house_deck_id.equals(house_deck.getDeckId()))
    		{
    			copy_house_decks.put(this_house_deck_id, house_deck);
    			Log.i(DEBUG_TAG, method+": game id "+house_deck.getGameId()+" player_id "+house_deck.getPlayerId());
    			Log.i(DEBUG_TAG, method+": putting this deck "+house_deck.getDeckName());
    		} else
    		{
    			copy_house_decks.put(this_house_deck_id, this_house_deck);
    			//Log.i(DEBUG_TAG, method+": copied game id "+this_house_deck.getGameId()+" player_id "+this_house_deck.getPlayerId());
    			//Log.i(DEBUG_TAG, method+": copy deck "+this_house_deck_id);
    		}
    	}
    	i_want_to.saveTheHouseDecks(copy_house_decks);
	}
	
	/**
	 * To create a sorted list of reading and a separate sorted list of writing cards
	 * we have to call the sortCards row separately for each list type.  Then  we call
	 * setupRow for each member of the hashtable returned by that method.
	 */
	private void createCardsList()
	{
		String method = "createCardsList";
		card_row_ids = new Hashtable <String,String> ();
		Hashtable <String,DeckCard> reading_rows = sortCards(Constants.READING);
		Enumeration<String> reading = reading_rows.keys();
		while (reading.hasMoreElements())
		{
			String key = reading.nextElement();
			DeckCard card = reading_rows.get(key);
			try
			{
				setupRow(card);
			} catch (java.lang.NullPointerException npe)
			{
				Log.i(DEBUG_TAG, method+": setupRow reading npe");
				npe.printStackTrace();
			}
		}
		Hashtable <String,DeckCard> writing_rows = sortCards(Constants.WRITING);
		Enumeration<String> writing = writing_rows.keys();
		while (writing.hasMoreElements())
		{
			String key = writing.nextElement();
			DeckCard card = writing_rows.get(key);
			try
			{
				setupRow(card);
			} catch (java.lang.NullPointerException npe)
			{
				Log.i(DEBUG_TAG, method+": setupRow reading npe");
				npe.printStackTrace();
			}
		}
	}
	
	/**
	 * This method is called for reading and again for writing words to create separate lists.
	 * The cards names and type are put into a table row, and an on click listener is attached.
	 * The listener gets the selected card, finds the DeckCard object then calls selectedCard.
	 */
	private void setupRow(DeckCard deck_card)
	{
		final String method = "setupRow";
		table = (TableLayout) findViewById(R.id.deck_card_table_layout);
		table.setVerticalScrollBarEnabled(true);
		table.setColumnStretchable(2, true);
		TableRow row = new TableRow(this);
		String card_name = deck_card.getCardName();
		String card_id = deck_card.getCardName();
		String card_status = deck_card.getStatus();
		card_row_ids.put(row.toString(), card_id);
		TextView name = new TextView(this);
        TextView type = new TextView(this);   
        name.setText(card_name);
        type.setText(card_status);
        Log.i(DEBUG_TAG, method+": table row "+card_name+" status "+card_status);
        row.addView(name);
        row.addView(type);
        table.addView(row,new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        row.setClickable(true);
        row.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
            	String inner_method = "onClick";
            	final String method = "card_list.onClick";
            	String selected_card_row_id = v.toString();
            	Log.i(DEBUG_TAG, method+inner_method+" selected_card_row_id "+selected_card_row_id);
            	String selected_card_name = card_row_ids.get(selected_card_row_id);
            	Log.i(DEBUG_TAG, method+inner_method+" selected_card_name "+selected_card_name);
            	String selected_card_id = deck_card_name_ids.get(selected_card_name);
            	Log.i(DEBUG_TAG, method+inner_method+" selected_card_id "+selected_card_id);
            	Log.i(DEBUG_TAG, method+inner_method+" selected "+selected_card_name+" selected_card_row_id "+selected_card_row_id+" selected_card_name "+selected_card_name+" selected_card_id "+selected_card_id);
            	DeckCard selected_card = house_deck.getCard(selected_card_id);
            	Log.i(DEBUG_TAG, method+inner_method+" selected "+selected_card_name+" (from card-"+selected_card.getCardName()+") "+selected_card_id);
            	selectedCard(selected_card);
            }
        });
	}
	
	private void setupPendingIntent()
	{
		mfc_adapter = NfcAdapter.getDefaultAdapter(this);
		pending_intent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
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
   	 	// tech_lists_array = new String [][] { new String[] {NfcF.class.getName()}}; // from CardPlayerWordsActivity
   	 	tech_lists_array = new String[][] { new String[] { MifareClassic.class.getName() } }; // from GameReadingStonesActibity
   	 	write_failed = false;
	}
	
	/**
	 * Show a popup asking the user to set up the card.  When the user clicks the OK button
	 * we check if the tag is OK to write, and get the id written to the tag like this:
	 * String card_id = readWriteFormatReject(selected_card);
	 * If it's being tested on a virtual device, then we emulate that behaviour.
	 * @param selected_card
	 */
	private void selectedCard(final DeckCard selected_card)
	{
		String method = "selectedCard";
		writing_new_id_mode = true; //set writing mode
		LayoutInflater layout_inflater = LayoutInflater.from(context);
		View popup_view = layout_inflater.inflate(R.layout.card_player_words_popup, null);
		final AlertDialog.Builder alert_dialog_builder = new AlertDialog.Builder(context);
		alert_dialog_builder.setView(popup_view);
		final TextView card_player_words_popup_text = (TextView) popup_view.findViewById(R.id.card_player_words_popup_text);
		//String message = R.string.scan_+selected_card.getCardName()+R.string.now;
		String message = (String)selected_card.getCardName();
		card_player_words_popup_text.setText(message);
		Log.i(DEBUG_TAG, method+" popup message "+message);
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
							Log.i(DEBUG_TAG, "DialogInterface.onClick: writing");
							try
							{
								String card_id = readWriteFormatReject(selected_card.getCardId()); // this returns a new id now
								if (card_id == null || write_failed)
								{
									Toast.makeText(context, R.string.cannot_use_card, Toast.LENGTH_LONG ).show();
								} else
								{
									Toast.makeText(context, context.getString(R.string.dont_move_writing), Toast.LENGTH_LONG ).show();
									Toast.makeText(context, context.getString(R.string.ok_writing), Toast.LENGTH_LONG ).show();
									updateId(card_id, selected_card);
									// change card id 
									// copy decks and replace with the new deck
									// load and save the deck
									// set the list as set up.
									testing = false;
									writing_new_id_mode = false;
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
						//Card card = itemSelected(selected_word);
						//Log.i(DEBUG_TAG, "DialogInterface.onClick: test writing");
						try
						{
							Log.i(DEBUG_TAG, "testing: no npe");
							//String card_id = readWriteFormatReject(selected_card);
							String new_id = UtilityTo.getNewID()+"";
							updateId(new_id, selected_card);
							writing_new_id_mode = false;
							//file_cards.add(card);
							//savedCardsFile();
							//checkIfSetupComplete();
						}
			    		 catch (java.lang.NullPointerException npe)
						{
							// no tag to read
							Log.i(DEBUG_TAG, "DialogInterface.onClick: no tag to read");
							Toast.makeText(context, "Test mode: Put the tag under the phone and try again.", Toast.LENGTH_LONG ).show();
							Log.i(DEBUG_TAG, "testing: npeee!!!");
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
					  writing_new_id_mode = false;
				  }
			  });
    	AlertDialog alert_dialog = alert_dialog_builder.create();
    	alert_dialog.show();
	}
	
	/**
	 * Put the new id into the card decks, and call updateCards to do the same thing with the cards file.
	 * @param new_card_id
	 * @param selected_card
	 */
	private void updateId(String new_card_id, DeckCard selected_card)
	{
		String method = "updateId";
		selected_card.setCardId(new_card_id);
		selected_card.setStatus(Constants.READY);
		boolean found = false;
		Log.i(DEBUG_TAG, method+" new card id "+new_card_id);
		Hashtable<String, DeckCard> deck_cards = house_deck.getCards();
		Hashtable<String, DeckCard> deck_cards_copy = new Hashtable<String, DeckCard> ();
		Enumeration <String> e = deck_cards.keys();
		while (e.hasMoreElements())
		{
			String key = (String)e.nextElement();
			DeckCard this_card = deck_cards.get(key);
			 if (this_card.getCardId().equals(selected_card.getCardId()))
			{
				this_card.setCardId(new_card_id);
				deck_cards_copy.put(new_card_id, this_card);
				Log.i(DEBUG_TAG, method+" new card name match. set "+this_card.getCardName()+" id to "+new_card_id);
				Log.i(DEBUG_TAG, method+" Check game association ");
				found = true;
			} else
			{
				deck_cards_copy.put(this_card.getCardId(), this_card);
				//Log.i(DEBUG_TAG, method+" no match for "+this_card.getCardName()+" id to "+new_card_id);
			}
		}
		if (!found)
		{
			Log.i(DEBUG_TAG, method+" WARNING! Could not update card id!");
		}
		house_deck.setCards(deck_cards_copy);
		loadAndSaveModifiedDecks();
		updateCards(new_card_id, selected_card);
		table.removeAllViews();
		createCardsList();
	}
	
	/**
	 * Copy the cards file with the new card id set in the appropriate card if it exists.
	 * @param new_card_id
	 * @param selected_card
	 */
	private void updateCards(final String new_card_id, DeckCard selected_card)
	{
		final String method = "updateCards";
    	new Thread()
        {
            public void run()
            {   
            	// we have the house_deck which has the deck cards and their ids-deck card.
            	Hashtable <String,DeckCard> deck_cards = house_deck.getCards();
            	boolean file_change = false;
            	IWantTo i_want_to = new IWantTo(context);
            	Vector <Card> file_cards = i_want_to.loadCardsFile();
            	Log.i(DEBUG_TAG, method+" Update Cards File.  file_cards size "+file_cards.size());
            	Vector <Card> file_cards_copy = new Vector <Card> ();
            	for (int i = 0; i < file_cards.size(); i++)
            	{
            		Card this_card = file_cards.get(i);
            		if (this_card.getCardId().equals(new_card_id))
            		{
            			Log.i(DEBUG_TAG, method+" new card name match. set "+this_card.getCardId()+" id to "+new_card_id);
            			this_card.setCardId(new_card_id);
            			file_cards_copy.add(this_card);
            			file_change = true;
            		} else
            		{
            			//Log.i(DEBUG_TAG, method+" no match for "+this_card.getCardId()+" id to "+new_card_id);
            			file_cards_copy.add(this_card);
            		}
            	}
            	if (file_change)
            	{
            		Log.i(DEBUG_TAG, method+" save cards copy");
            		i_want_to.saveCardsFile(file_cards_copy);
            	} else
            	{
            		Log.i(DEBUG_TAG, method+" WARNING! Could not update card id!");
            	}
            }
        }.start();
	}
	
	/**
     * From http://www.tapwise.com/svn/nfcwritetag/trunk/src/com/tapwise/nfcwritetag/MainActivity.java
     * @param selected_card
     * @return
     */
    private String readWriteFormatReject(String existing_deck_card_id)
    {
    	String method = "readWriteFormatReject";
        Tag detectedTag = new_intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        Log.i(DEBUG_TAG, method+" existing_deck_card_id "+existing_deck_card_id);
        if(supportedTechs(card_tag.getTechList())) 
        {
        	//Log.i(DEBUG_TAG, method+" supported techs");
            // check if tag is writable (to the extent that we can
            if(writableTag(card_tag)) 
            {
            	//String existing_message = writeTag(getTagAsNdef(selected_card.getCardId()), detectedTag);
            	String existing_message = null;
            	if (write_new_id)
            	{
            		long new_id = UtilityTo.getNewID();
            		existing_message = writeTag(getTagAsNdef(new_id+""), detectedTag);
            		existing_message = new_id+"";
            	} else
            	{
            		existing_message = writeTag(getTagAsNdef(existing_deck_card_id), detectedTag);
            	}
            	String message = "This tag can be written, new id is : "+existing_message;
            	Log.i(DEBUG_TAG, method+" "+message);
            	existing_deck_card_id = existing_message;
            	//Toast.makeText(context,message,Toast.LENGTH_SHORT).show();
            } else 
            {
            	String existing_message = getExistingTagID();
            	String message = "This tag is not writable. existing_message "+existing_message;
            	Log.i(DEBUG_TAG, method+" "+message);
            	Log.i(DEBUG_TAG, method+" existing message "+existing_message);
            	//Toast.makeText(context,message,Toast.LENGTH_SHORT).show();
            	existing_deck_card_id = existing_message;
            }	            
        } else 
        {
        	String existing_message = getExistingTagID();
        	Log.i(DEBUG_TAG, method+"un-supported techs "+existing_message);
        	//Toast.makeText(context,"This tag type is not supported",Toast.LENGTH_SHORT).show();
        	existing_deck_card_id = existing_message;
        }
        try
        {
        	if (existing_deck_card_id.equals(""))
        	{
        		existing_deck_card_id = null;
        		Log.i(DEBUG_TAG, method+" card_id set to null");
        	}
        } catch (java.lang.NullPointerException npe)
        {
        	Log.i(DEBUG_TAG, method+" card_id is null");
        }
        return existing_deck_card_id;
    }
    
    /**
     * Try and read a tag to get the first sector to use as an id if we can't write the tag.
     * @return
     */
    private String getExistingTagID()
	{
		String method = "getExistingTagID";
		Log.i(DEBUG_TAG, method+": called.");
		Parcelable[] raw_messages = new_intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
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
			Log.i(DEBUG_TAG, method+" raw_messages == null");
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
					return message;
				}
			}
		} catch (java.lang.NullPointerException npe)
		{
			Log.i(DEBUG_TAG, method+" failed to write tag.");
			write_failed = true;
			npe.printStackTrace();
		}
		return null;
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
	 * First create a list of just the type of cards needed.
	 * Then create an ordered hash of index card pairs.
	 * @param type
	 * @return
	 */
	private Hashtable <String,DeckCard> sortCards(String type)
	{
		String method = "sortCards";
		Vector <DeckCard> type_results = new Vector <DeckCard> ();
		Enumeration<String> e = id_deck_cards.keys();
		while (e.hasMoreElements())
		{
			String key = e.nextElement();
			DeckCard card = id_deck_cards.get(key);
			String this_type = card.getType();
			if (this_type.equals(type))
			{
				type_results.add(card);
			}
		}
		//Log.i(DEBUG_TAG, method+" type "+type+" cards "+type_results.size());
		Hashtable <String,DeckCard> results = new Hashtable <String,DeckCard> ();
		for (int i = 0; i < type_results.size(); i++)
		{
			DeckCard deck_card = (DeckCard)type_results.get(i);
			int index = deck_card.getIndex();
			results.put(Integer.toString(index), deck_card);
			//Log.i(DEBUG_TAG, method+" i "+i+" index "+index+" deck_card "+deck_card.getCardName());
		}
		return results;
	}
	
	/**
	 * Starting at 1 for cards, we load the amount of cards we have set in the number_of_cards extra.
	 * 
	 */
	private void getIntentInfo()
	{
		String method = "getIntentInfo";
		Intent sender = getIntent();
		String deck_name = sender.getExtras().getString("deck_name");
		String deck_id = sender.getExtras().getString("deck_id");
		String player_id = sender.getExtras().getString("player_id");
		String game_id = sender.getExtras().getString("game_id");
		id_deck_cards = new Hashtable <String,DeckCard> ();
		deck_card_name_ids = new Hashtable <String,String> ();
		house_deck = new HouseDeck();
		house_deck.setDeckId(deck_id);
		house_deck.setDeckName(deck_name);
		house_deck.setPlayerId(player_id);
		house_deck.setGameId(game_id);
		int number_of_cards = Integer.parseInt(sender.getExtras().getString("number_of_cards"));
		for (int i = 1; i < number_of_cards; i++)
		{
			try
			{
				DeckCard card = new DeckCard();
				String card_id = sender.getExtras().getString(i+"card_id");
				String card_name = sender.getExtras().getString(i+"card_name");
				String status = sender.getExtras().getString(i+"status");
				card.setCardId(card_id);
				card.setCardName(card_name);
				card.setStatus(status);
				card.setIndex(Integer.parseInt(sender.getExtras().getString(i+"index")));
				card.setType(sender.getExtras().getString(i+"type"));
				id_deck_cards.put(card.getCardId(), card);
				house_deck.setCards(card.getCardId(), card);
				deck_card_name_ids.put(card_name, card_id);
				//Log.i(DEBUG_TAG, method+" got "+card.getCardName()+" id "+card.getCardId()+" indes "+card.getIndex()+" type "+card.getType());
			} catch (java.lang.NullPointerException npe)
			{
				Log.i(DEBUG_TAG, method+" npe for "+i);
			}
		}
		setTitle(deck_name);
		Log.i(DEBUG_TAG, method+" number of cards got from intent: "+house_deck.getCards().size());
	}
	
	/**
	  * This is called each time a NFC tag is detected.  This is the GameReadingStonesActivity
	  */
   //@Override
   public void onNewIntent2(Intent intent) 
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
			//getTestCard(test_card_type);
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
					Log.i(DEBUG_TAG, method+" "+i+" message "+message);
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
    * Check to see if the id scanned from the NFC tag is associated with a deck card.
    * Load the cards file and see if the id matches any cards in it.  
    * Then display found deck card name if any and the card word/player name if any.
    * @param maybe_card_id
    */
   private void associateWithCardsFile(final String raw_card_id)
   {
	   final String maybe_card_id = UtilityTo.removeHiddenCharacters(raw_card_id);
	   final String method = "associateWithCardsFile";
	   new Thread()
       {
           public void run()
           {   
        	   deck_card_name = "No deck card, "; // needs a space cause it comes first
          		Hashtable<String,DeckCard> deck_cards = house_deck.getCards();
          		Log.i(DEBUG_TAG, method+" deck_cards "+deck_cards.size());
          		Enumeration <String> e = deck_cards.keys();
      			while (e.hasMoreElements())
      			{
      				String key = (String) e.nextElement();
      				DeckCard this_deck_card = (DeckCard) deck_cards.get(key);
      				if (this_deck_card.getCardId().equals(maybe_card_id))
      				{
      					Log.i(DEBUG_TAG, method+" association made for "+this_deck_card.getCardName());
      					deck_card_name = this_deck_card.getCardName()+", ";
      				} else
      				{
      					Log.i(DEBUG_TAG, method+" key "+key+" "+this_deck_card.getCardName()+" compared to "+maybe_card_id+" and failed.");
      				}
      			}
           		IWantTo i_want_to = new IWantTo(context);
           		Vector <Card> file_cards = i_want_to.loadCardsFile();
           		word_player_card = "No file card";
           		Card found_card = getIdFileCard(file_cards, maybe_card_id);
           		if (found_card != null)
           		{
           			word_player_card = UtilityTo.getWord(found_card);
           			Log.i(DEBUG_TAG, method+" found card for palyer "+found_card.getPlayerName()+" id "+found_card.getPlayerName());
           		}
           		Log.i(DEBUG_TAG, method+" looking at file cards "+file_cards.size());
           		activity.runOnUiThread(new Runnable()
                {
                    public void run()
                    {
                        Toast.makeText(context, deck_card_name+" "+word_player_card, Toast.LENGTH_LONG ).show();
                    }
                });
           }
       }.start();
   }
   
   /**
    * Debugging.
    */
   private void printDeckCards()
   {
	   String method = "printDeckCards";
	   Hashtable <String,DeckCard> deck_cards = house_deck.getCards();
	   Enumeration <String> e = deck_cards.keys();
	   while (e.hasMoreElements())
	   {
		   String key = (String)e.nextElement();
		   DeckCard this_deck_card = house_deck.getCard(key);
		   Log.i(DEBUG_TAG, method+" "+this_deck_card.getCardName()+" "+this_deck_card.getCardId());
				   
	   }
   }
   
   /**
    * Search through all the file cards for a card with the search_id equal to the card id.
    * If nonr id found return a null card. 
    * @param file_cards
    * @param search_id
    * @return
    */
   private Card getIdFileCard(Vector <Card> file_cards, String search_id)
   {
	   String method = "getIdFileCard";
	   Card my_card = null;
	   for (int i=0; i < file_cards.size(); i++)
	   {
		   Card this_card = file_cards.get(i);
		   if (this_card.getCardId().equals(search_id))
		   {
			   Log.i(DEBUG_TAG, method+" found "+this_card.getCardId()+" "+UtilityTo.getWord(this_card));
			   my_card = this_card;
		   } else
		   {
			   Log.i(DEBUG_TAG, method+" search id "+search_id+" doesn't match "+this_card.getCardId()+" "+UtilityTo.getWord(this_card));
		   }
	   }
	   return my_card;
   }
   
   /**
    * From CardPlayerWordsActivity
    */
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
       	if (!writing_new_id_mode)
       	{
       		onNewIntent2(intent);   
       	}
       } else
       {
       	Log.i(DEBUG_TAG, "onNewIntent: ACTION (not) DISCOVERED");
       }
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
	  * 	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		getMenuInflater().inflate(R.menu.card_deck, menu);
		return true;
	}
	  */
	 
	 public boolean onCreateOptionsMenu(Menu menu) 
	    {
	    	super.onCreateOptionsMenu(menu);
	    	writing_new_id_mode = false;
			write_new_id = false;
			reuse_selected = true;
		    write_selected = false;
	    	//menu.add(0, reuse_id, 0, "Reuse Card ID").setCheckable(true);
		    getMenuInflater().inflate(R.menu.card_decks, menu);
	    	SubMenu id_choice = menu.addSubMenu("Reuse or Write ID").setIcon(android.R.drawable.ic_menu_gallery);
	    	id_choice.add(id_group, reuse_id, 1, "Reuse Old Card ID").setChecked(reuse_selected);
	    	id_choice.add(id_group, write_id, 1, "Write New Card ID").setChecked(write_selected);
	    	return true;
	    }
	 
	 public boolean onOptionsItemSelected(MenuItem item) 
	    {
	    	String method = "onOptionsItemSelected(MenuItem)";
	    	if (item.getItemId() == reuse_id)
	    	{
	    		Log.i(DEBUG_TAG, method+" set reuse true");
	    		reuse_selected = true;
			    write_selected = false;
			    write_new_id = false;
	    	    return true;
	    	} if (item.getItemId() == write_id)
	    	{
	    		Log.i(DEBUG_TAG, method+" set wrtie true");
	    		reuse_selected = false;
			    write_selected = true;
			    write_new_id = true;
	    	    return true;
	    	} 
	    	return super.onOptionsItemSelected(item);
	    }

}
