package com.curchod.domartin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.curchod.dto.Card;
import com.curchod.dto.DeckCard;
import com.curchod.dto.Game;
import com.curchod.wherewithal.CardPlayerWordsActivity;
import com.curchod.wherewithal.CardPlayersListActivity;
import com.curchod.wherewithal.R;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class IWantTo 
{
	
	private String DEBUG_TAG = "IWantTo";
	private Context context;
	
	public IWantTo(Context _context)
	{
		context = _context;
	}

	/**
	 * Load the IP address from the ip.xml file.
	 * If it doesn't exists, create a </ip> tag in the ip.xml file.
	 * @return
	 */
	public String loadIPFile()
	{
		final String method = "loadIP"; 
    	String file_path = context.getFilesDir().getAbsolutePath();
    	File file = new File(file_path, Constants.IP_XML);
    	boolean exists = file.exists();
    	String ip = null;
    	if (exists == false)
    	{
    		Log.i(DEBUG_TAG, method+": make new file");
    		{
    	    	try 
    	    	{
    	    		FileOutputStream fos = context.openFileOutput(Constants.IP_XML, Context.MODE_PRIVATE);
    		        try
    		        {
    		        	StringBuffer sb = new StringBuffer();
    					sb.append("<ip/>");
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
    	} else
    	{
    		Log.i(DEBUG_TAG, method+": parse file");
    		ip = parseIPFile();
    	}
		return ip;
	}
	
	/**
	 * The formal of the ip.xml file is simple:
	 * <ip><value>*.*.*.*</value></ip>
	 * @return
	 */
	private String parseIPFile()
    {
    	String method = "parseIPFile";
    	String ip = null;
    	FileInputStream fis = null;
		try 
		{
			fis = context.openFileInput(Constants.IP_XML);
		} catch (FileNotFoundException e1) 
		{
			Log.i(DEBUG_TAG, method+": fnfe");
			e1.printStackTrace();
		}
        try 
        {
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
                			if (tag.equals("value"))
                			{
                				ip = value;
                				Log.i(DEBUG_TAG, "parsed: "+tag+" "+value);
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
        return ip;
    }
	
	public void saveIPFile(String ip)
    {
    	String method = "savedIPFile";
    	try 
    	{
    		FileOutputStream fos = context.openFileOutput(Constants.IP_XML, Context.MODE_PRIVATE);
	        try
	        {
	        	StringBuffer sb = new StringBuffer();
				sb.append("<ip>");
				sb.append("<value>"+ip+"</value>");
				sb.append("</ip>");
				Log.i(DEBUG_TAG, method+" writing "+new String(sb));
				fos.write(new String(sb).getBytes());
				fos.close();
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
	
	public void saveTheGameFile(Game game, String class_id)
    {
    	String method = "saveTheGameFile";
        //Log.i(DEBUG_TAG, method+": ");
    	try 
    	{
    		FileOutputStream fos = context.openFileOutput(Constants.GAME_XML, Context.MODE_PRIVATE);
    		//Log.i(DEBUG_TAG, method+": FD "+fos.getFD());
	        try
	        {
	        	StringBuffer sb = new StringBuffer();
				sb.append("<game>");
				sb.append("<test_name>"+game.getTestName()+"</test_name>");
				sb.append("<test_id>"+game.getTestId()+"</test_id>");
				sb.append("<class_id>"+class_id+"</class_id>");
				sb.append("<test_type>"+game.getTestType()+"</test_type>");
				sb.append("<test_status>"+game.getTestStatus()+"</test_status>");
				sb.append("<test_format>"+game.getTestFormat()+"</test_format>");
				Enumeration<String> e = game.getPlayerStatus().keys();
				//Log.i(DEBUG_TAG, method+" students in id_status "+game.getPlayerStatus().size());
                while (e.hasMoreElements())
                {
					String this_player_id = e.nextElement();
					String status = game.getPlayerStatus(this_player_id);
					sb.append("<player_status id=\""+this_player_id+"\">"+status+"</player_status>");
					//Log.i(DEBUG_TAG, method+"<player_status id="+this_player_id+">"+status+"</player_status>");
				}
				sb.append("</game>");
				//Log.i(DEBUG_TAG, "writing "+new String(sb));
				fos.write(new String(sb).getBytes());
				fos.close();
				//Log.i(DEBUG_TAG, method+": done");
	        } catch (FileNotFoundException e)
	        {
	            Log.e(DEBUG_TAG, method+" FileNotFoundException");
			} catch (IOException e1) 
			{
				Log.e(DEBUG_TAG, method+" IOException");
				e1.printStackTrace();
			}
		} catch (IOException e) 
		{
			e.printStackTrace();
		}
    }
	
	public Game loadTheGameFile() 
    {
    	final String method = "loadGameFile"; 
    	Log.i(DEBUG_TAG, method+": Open the local game.xml file and parse it for game info.");
    	Game game = new Game();
    	FileInputStream fis = null;
		try 
		{
			fis = context.openFileInput(Constants.GAME_XML);
		} catch (FileNotFoundException e1) 
		{
			Log.i(DEBUG_TAG, method+": fnfe");
			e1.printStackTrace();
		}
        try 
        {
        	String parse_player_id = "";
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
                			if (tag.equals("test_id"))
                			{
                				game.setTestId(value);
                				//Log.i(DEBUG_TAG, "test_id loaded "+value);
                			} else if (tag.equals("class_id"))
                			{
                				game.setClassId(value);
                				//Log.i(DEBUG_TAG, "class_id loaded "+value);
                			} else if (tag.equals("test_status"))
                			{
                				game.setTestStatus(value);
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
                		tag = null;
                	}
                		
                case XmlPullParser.START_TAG:
                	tag = parser.getName();
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
		return game;
    }
	
	/**
	 * 
	 * @return
	 */
	public Hashtable <String,HouseDeck> loadTheHouseDecks()
	{
		Hashtable <String,HouseDeck> decks = new Hashtable <String,HouseDeck> ();
		final String method = "loadTheHouseDecks"; 
    	//Log.i(DEBUG_TAG, method+": Load or create a new house_decks.xml file.");
    	String file_path = context.getFilesDir().getAbsolutePath();
    	File file = new File(file_path, Constants.HOUSE_DECKS_XML);
    	boolean exists = file.exists();
    	//Log.i(DEBUG_TAG, method+": house_decks.xml exists? "+exists);
    	if (exists == false)
    	{
    		//Log.i(DEBUG_TAG, method+": make new file");
    		createNewHouseDecksFile(Constants.HOUSE_DECKS_XML);
    	} else
    	{
    		//Log.i(DEBUG_TAG, method+": parse file 4");
    		decks = parseHouseDecks();
    	}
		return decks;
	}
	
	/**
 *     <house_deck>
*         <deck_id>
*         <player_id/>
*         <game_id>
*         <name>¡±Player A¡±</name>
*             <deck_card>
*                 <card_id/>
*                 <card_name/>
*                 <index/>
*                 <type/>
*            </deck_card>
*             ...
*    </house_deck>
	 * @return
	 */
	private Hashtable <String,HouseDeck> parseHouseDecks()
	{
		String method = "parseHouseDecks";
		Hashtable <String,HouseDeck> decks = new Hashtable <String,HouseDeck> ();
		Hashtable <String,DeckCard> cards = new Hashtable <String,DeckCard> ();
		HouseDeck deck = new HouseDeck();
		DeckCard card = new DeckCard();
		FileInputStream fis = null;
		try 
		{
			fis = context.openFileInput(Constants.HOUSE_DECKS_XML);
		} catch (FileNotFoundException e1) 
		{
			Log.i(DEBUG_TAG, method+": fnfe");
			e1.printStackTrace();
		}
        try 
        {
        	boolean capture_deck = false;
        	boolean capture_card = false;
        	boolean filling_deck = false;
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
                 				//Log.i(DEBUG_TAG, "card_id "+value);
                 			} else if (tag.equals("card_name"))
                 			{
                 				card.setCardName(value);
                 				//Log.i(DEBUG_TAG, "card_name "+value);
                 			} else if (tag.equals("status"))
                 			{
                 				card.setStatus(value);
                 				//Log.i(DEBUG_TAG, "status "+value);
                 			} else if (tag.equals("index"))
                 			{
                 				card.setIndex(Integer.parseInt(value));
                 			} else if (tag.equals("type"))
                 			{
                 				card.setType(value);
                 				cards.put(card.getCardId(), card);
                 				//Log.i(DEBUG_TAG, method+" added card "+card.getCardId()+" "+card.getCardName()+" type "+value);
                 				capture_card = false;
                 				card = new DeckCard();
                 			} else if (tag.equals("deck_card"))
                 			{
                 				card = new DeckCard();
                 				capture_card = true;
                 				//Log.i(DEBUG_TAG, "capture_card set");
                 			} else if (tag.equals("deck_id"))
                			{
                 				if (filling_deck == true)
                				{
                					// start deck fill
                					deck.setCards(cards);
                					decks.put(deck.getDeckId(), deck);
                                    // Log.i(DEBUG_TAG, "added deck "+deck.getDeckName()+" with "+cards.size()+"  cards");
                                    deck = new HouseDeck();
                                    cards = new Hashtable <String,DeckCard> ();
                                    deck.setDeckId(value); // get the id from the next deck
                                    //capture_deck = false;
                                    //filling_deck = false;
                				} else
                				{
                					filling_deck = true;
                					deck.setDeckId(value);
                					//Log.i(DEBUG_TAG, method+": start fill for deck_id "+value);
                				}
                			} else if (tag.equals("player_id"))
                			{
                				deck.setPlayerId(value);
                				Log.i(DEBUG_TAG, method+": player_id "+value);
                			} else if (tag.equals("game_id"))
                			{
                				deck.setGameId(value);
                				Log.i(DEBUG_TAG, method+": game_id "+value);
                			} else if (tag.equals("deck_name"))
                			{
                				deck.setDeckName(value);
                				//Log.i(DEBUG_TAG, method+": deck_name "+value);
                			} else if (tag.equals("house_deck"))
                			{
                				
                			}
                		}
                		tag = null;
                	}
                		
                case XmlPullParser.START_TAG:
                	tag = parser.getName();
                	if (tag != null)
                	{
                		//Log.i(DEBUG_TAG, "start tag "+tag);
                	}
                }
                parser_event = parser.next();
            }
            if (filling_deck)
            {
            	deck.setCards(cards);
    			decks.put(deck.getDeckId(), deck);
                //Log.i(DEBUG_TAG, "last deck named "+deck.getDeckName()+" added with "+cards.size()+"  cards");
            }
        } catch (Exception e) 
        {
        	Log.i(DEBUG_TAG, method+": exception");
        	e.printStackTrace();
        }
		return decks;
	}
	
	/**
	 *     <house_deck>
	*         <deck_id>
	*         <player_id/>
	*         <game_id>
	*         <name>¡±Player A¡±</name>
	*             <deck_card>
	*                 <card_id/>
	*                 <card_name/>
	*                 <index/>
	*                 <type/>
	*            </deck_card>
	*             ...
	*    </house_deck>
		 * @return
		 */
		private Hashtable <String,HouseDeck> parseHouseDecks2(InputStream is)
		{
			String method = "parseHouseDecks2";
			Hashtable <String,HouseDeck> decks = new Hashtable <String,HouseDeck> ();
			Hashtable <String,DeckCard> cards = new Hashtable <String,DeckCard> ();
			HouseDeck deck = new HouseDeck();
			DeckCard card = new DeckCard();
	        try 
	        {
	        	boolean capture_deck = false;
	        	boolean capture_card = false;
	        	boolean filling_deck = false;
	        	String tag = "";
	            XmlPullParserFactory parserCreator = XmlPullParserFactory.newInstance();
	            XmlPullParser parser = parserCreator.newPullParser();
	            parser.setInput(is, null);
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
	                 				Log.i(DEBUG_TAG, "card_id "+value);
	                 			} else if (tag.equals("card_name"))
	                 			{
	                 				card.setCardName(value);
	                 				Log.i(DEBUG_TAG, "card_name "+value);
	                 			} else if (tag.equals("status"))
	                 			{
	                 				card.setStatus(value);
	                 				Log.i(DEBUG_TAG, "status "+value);
	                 			} else if (tag.equals("index"))
	                 			{
	                 				card.setIndex(Integer.parseInt(value));
	                 			} else if (tag.equals("type"))
	                 			{
	                 				card.setType(value);
	                 				cards.put(card.getCardId(), card);
	                 				Log.i(DEBUG_TAG, method+" added card "+card.getCardId()+" "+card.getCardName()+" type "+value);
	                 				capture_card = false;
	                 				card = new DeckCard();
	                 			} else if (tag.equals("deck_card"))
	                 			{
	                 				card = new DeckCard();
	                 				capture_card = true;
	                 				Log.i(DEBUG_TAG, "capture_card set");
	                 			} else if (tag.equals("deck_id"))
	                			{
	                 				if (filling_deck == true)
	                				{
	                					// start deck fill
	                					deck.setCards(cards);
	                					decks.put(deck.getDeckId(), deck);
	                                    Log.i(DEBUG_TAG, "- added deck_id "+deck.getDeckId()+" name "+deck.getDeckName()+" with "+cards.size()+"  cards");
	                                    deck = new HouseDeck();
	                                    cards = new Hashtable <String,DeckCard> ();
	                                    deck.setDeckId(value); // get the id from the next deck
	                                    //capture_deck = false;
	                                    //filling_deck = false;
	                				} else
	                				{
	                					filling_deck = true;
	                					deck.setDeckId(value);
	                					//Log.i(DEBUG_TAG, method+": start fill for deck_id "+value);
	                				}
	                			} else if (tag.equals("player_id"))
	                			{
	                				deck.setPlayerId(value);
	                				Log.i(DEBUG_TAG, method+": player_id "+value);
	                			} else if (tag.equals("game_id"))
	                			{
	                				deck.setGameId(value);
	                				Log.i(DEBUG_TAG, method+": game_id "+value);
	                			} else if (tag.equals("deck_name"))
	                			{
	                				deck.setDeckName(value);
	                				Log.i(DEBUG_TAG, method+": deck_name "+value);
	                			} else if (tag.equals("house_deck"))
	                			{
	                				Log.i(DEBUG_TAG, method+": tag equals house deck");
	                			}
	                		}
	                		tag = null;
	                	}
	                		
	                case XmlPullParser.START_TAG:
	                	tag = parser.getName();
	                	if (tag != null)
	                	{
	                		//Log.i(DEBUG_TAG, "start tag "+tag);
	                	}
	                }
	                parser_event = parser.next();
	            }
	            if (filling_deck)
	            {
	            	deck.setCards(cards);
	    			decks.put(deck.getDeckId(), deck);
	                //Log.i(DEBUG_TAG, "last deck named "+deck.getDeckName()+" added with "+cards.size()+"  cards");
	            }
	        } catch (Exception e) 
	        {
	        	Log.i(DEBUG_TAG, method+": exception");
	        	e.printStackTrace();
	        }
			return decks;
		}
	
	private void createNewHouseDecksFile(String file_name)
	{
		String method = "createNewHouseDecksFile";
        Log.i(DEBUG_TAG, method+": new file");
    	try 
    	{
    		FileOutputStream fos = context.openFileOutput(file_name, Context.MODE_PRIVATE);
	        try
	        {
	        	StringBuffer sb = new StringBuffer();
				sb.append("<house_decks />");
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
	
	/**
	 *<house_deck>
        <deck_id>
        <player_id>
        <game_id>
        <deck_name>¡±Player A¡±</deck_name>
        <size>24</size>
		<card index=¡±0¡±>91971116904032786</card>
		<card index=¡±1¡±>-3056981135622170599</card>
		...
    </house_deck>
	 */
	public void saveTheHouseDecks(Hashtable <String,HouseDeck> house_decks)
	{
		String method = "saveTheHouseDecks";
		Log.i(DEBUG_TAG, method+" house_decks "+house_decks.size());
		try 
		{
			FileOutputStream fos = context.openFileOutput(Constants.HOUSE_DECKS_XML, Context.MODE_PRIVATE);
			StringBuffer sb = new StringBuffer();
			sb.append("<house_decks>");
			Enumeration<String> e = house_decks.keys();
			while (e.hasMoreElements())
			{
				String key = e.nextElement();
				HouseDeck deck = house_decks.get(key);
				sb.append("<house_deck>");
				sb.append("<deck_id>"+deck.getDeckId()+"</deck_id>");
				sb.append("<player_id>"+deck.getPlayerId()+"</player_id>");
				sb.append("<game_id>"+deck.getGameId()+"</game_id>");
				sb.append("<deck_name>"+deck.getDeckName()+"</deck_name>");
				Hashtable <String,DeckCard>cards = deck.getCards();
				Log.i(DEBUG_TAG, method+" deck name "+deck.getDeckName()+" game_id +"+deck.getGameId()+" player_id+"+deck.getPlayerId()+" with "+cards.size()+" card");
				try
				{
					Enumeration <String> f = cards.keys();
					while (f.hasMoreElements())
					{
						String index = (String)f.nextElement();
						DeckCard deck_card = (DeckCard)cards.get(index);
						sb.append("<deck_card>");
						sb.append("<card_id>"+deck_card.getCardId()+"</card_id>");
						sb.append("<card_name>"+deck_card.getCardName()+"</card_name>");
						sb.append("<status>"+deck_card.getStatus()+"</status>");
						sb.append("<index>"+deck_card.getIndex()+"</index>");
						sb.append("<type>"+deck_card.getType()+"</type>");
						sb.append("</deck_card>");
						//Log.i(DEBUG_TAG, method+" string buffer "+sb.length());
					}
				} catch (java.util.NoSuchElementException nsee)
				{
					Log.i(DEBUG_TAG, method+" nsee");
					nsee.printStackTrace();
				}
				sb.append("</house_deck>");
			}
			sb.append("</house_decks>");
			//Log.i(DEBUG_TAG, method+" total string buffer "+sb.length());
			fos.write(new String(sb).getBytes());
			Log.i(DEBUG_TAG, method+" wrote deck "+house_decks.size()+" cards: "+sb.toString());
			fos.close();
		} catch (FileNotFoundException e1) 
		{
			// TODO Auto-generated catch block
			Log.e(DEBUG_TAG, method+" fnfe");
			e1.printStackTrace();
		} catch (IOException e1) 
		{
			// TODO Auto-generated catch block
			Log.e(DEBUG_TAG, method+" ioe");
			e1.printStackTrace();
		}
	}
	
	public Hashtable <String,HouseDeck> loadRemoteHouseDecks(String teacher_id, String device_id)
	{
	    	String method = "loadRemoteHouseDecks";
	    	Log.i(DEBUG_TAG, method+" get_house_deck.do");
	    	URL text = null;
	    	try 
	        {
	            text = new URL("http://211.220.31.50:8080/indoct/get_house_decks.do?teacher_id="+teacher_id
	            		+"&device_id="+device_id);
	            Log.i(DEBUG_TAG, text.getPath());
	        } catch (MalformedURLException e) 
	   		{
	   			e.printStackTrace();
	   		}
	    	try 
	    	{
				InputStream is = text.openStream();
				Hashtable <String,HouseDeck> house_decks = parseHouseDecks2(is);
				return house_decks;
			} catch (IOException e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	return null;
	}
	
	/**
     * This method sets up what's needed to open the cards.xml file. 
     * If the file doesn't exist yet, create it.
     * Then call the parseCards() method.
     */
    public Vector <Card> loadCardsFile()
    {
    	String method = "loadCards";
    	Vector <Card> file_cards = new Vector<Card>();
    	String file_path = context.getFilesDir().getAbsolutePath();//returns current directory.
    	//Log.i(DEBUG_TAG, method+": file_path - "+file_path);
    	File file = new File(file_path, Constants.CARDS_XML);
    	boolean exists = file.exists();
    	if (exists == false)
    	{
    		createNewCardsFile();
    		//Log.i(DEBUG_TAG, method+" created new cards.xml file");
    	} else
    	{
    		file_cards = parseCards();
    	}
    	return file_cards;
    }
    
    private Vector <Card> parseCards()
    {
    	String method = "parseCards";
    	Vector <Card> file_cards = new Vector<Card>();
    	FileInputStream fis = null;
		try 
		{
			fis = context.openFileInput(Constants.CARDS_XML);
			//Log.i(DEBUG_TAG, method+": fis "+fis.available());
		} catch (FileNotFoundException e1) 
		{
			Log.i(DEBUG_TAG, method+": fnfe");
			e1.printStackTrace();
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
                				Log.i(DEBUG_TAG, method+" added "+value+" "+card.getText()+" id "+card.getCardId());
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
        return file_cards;
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
    		FileOutputStream fos = context.openFileOutput(Constants.CARDS_XML, Context.MODE_PRIVATE);
	        try
	        {
	        	StringBuffer sb = new StringBuffer();
				sb.append("<cards />");
				fos.write(new String(sb).getBytes());
				fos.close();
	        } catch (FileNotFoundException e)
	        {
	            Log.e(DEBUG_TAG, method+"FileNotFoundException");
			} catch (IOException e1) 
			{
				Log.e(DEBUG_TAG, method+"IOException");
				e1.printStackTrace();
			}
		} catch (IOException e) 
		{
			e.printStackTrace();
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
    public void saveCardsFile(Vector cards)
    {
    	String method = "savedCardsFile";
        //Log.i(DEBUG_TAG, method);
    	try 
    	{
    		FileOutputStream fos = context.openFileOutput(Constants.CARDS_XML, Context.MODE_PRIVATE);
    		//Log.i(DEBUG_TAG, method+": FD "+fos.getFD());
	        try
	        {
	        	StringBuffer sb = new StringBuffer();
				sb.append("<cards>");
				for (int i = 0; i < cards.size(); i++)
				{
					Card card = (Card)cards.get(i);
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
	 * Returns a hash with the student id-name as key value pairs.
	 * http://211.220.31.50:8080/indoct/student_names.do?teacher_id=0000000000000000001&pass=teach&class_id=8549097398869562086
	 * Parses 
	 * <students>
	 * 	<student>
	 * 		<student_id>-5519451928541341468</student_id>
	 * 		<student_name>t</student_name>
	 * 	</student>
	 *  ...
	 * </students>
	 * 
	 */
	public Hashtable<String, String> parseRemotePLayerNames(String teacher_id, String class_id)
	{
    	String method = "remoteCall";
    	String message = "OK";
    	Log.i(DEBUG_TAG, method+" student_names.do");
    	URL text = null;
    	try 
        {
            text = new URL("http://211.220.31.50:8080/indoct/student_names.do?teacher_id="+teacher_id
            		+"&class_id="+class_id);
        } catch (MalformedURLException e) 
   		{
   			e.printStackTrace();
   		}
    	Hashtable<String, String>id_player_names = new Hashtable<String, String>();
        String element = null;
        boolean capture_student = false;
    	boolean capture_name = false;
    	boolean capture_id = false;
    	String student_id = null;
    	String student_name = null;
    	int number_of_students = 0;
    	try 
    	{
			XmlPullParserFactory parser_creater = XmlPullParserFactory.newInstance();
			XmlPullParser parser =  parser_creater.newPullParser();
			parser.setInput(text.openStream(), null);
			int parser_event = parser.getEventType();
			while (parser_event != XmlPullParser.END_DOCUMENT)
			{
				switch(parser_event)
				{
					case XmlPullParser.TEXT:
					String value = parser.getText();
					if (capture_student == true && capture_id == true)
					{
						student_id = value;
						capture_id = false;
						Log.i(DEBUG_TAG, method+" student_id "+student_id);
					} else if (capture_student == true && capture_name == true)
					{
						student_name = value;
						id_player_names.put(student_id, student_name);
						capture_student = false;
				    	capture_name = false;
				    	capture_id = false;
						Log.i(DEBUG_TAG, method+" put student_id "+student_id+" student_name "+student_name);
					}
					case XmlPullParser.START_TAG:
					String tag_name = parser.getName();
					try
					{
						if (tag_name.equals("student"))
						{
							capture_student = true;
						} else if (tag_name.equals("student_id"))
						{
							capture_id = true;
						} else if (tag_name.equals("student_name"))
						{
							capture_name = true;
						}
						element = tag_name;
					} catch (java.lang.NullPointerException nope)
					{
					}
					//Log.i(DEBUG_TAG, "case.START_TAG "+tag_name+" capture_the_flag="+capture_the_flag);
				}
				parser_event = parser.next();
			}
			//Log.i(DEBUG_TAG, method+" put number_of_tests = "+(number_of_tests-1)+" 8888888888888");
		} catch (XmlPullParserException e) 
		{
			e.printStackTrace();
			message = "Parser exception.";
			//Toast.makeText(this, message, Toast.LENGTH_LONG ).show();
			Log.i(DEBUG_TAG, method+" XmlPullParserException "+message);
			id_player_names.put("error", message);
			
		} catch (IOException e) 
		{
			e.printStackTrace();
			message = "Network is unreachable.";
			//Toast.makeText(this, message, Toast.LENGTH_LONG ).show();
			Log.i(DEBUG_TAG, method+" IOException "+message);
			id_player_names.put("error", message);
		}
    	return id_player_names;
	}
	
	/**
	 * Parse the remote call to GetSavedClassTestsAction, parse the results, put them in the intent
	 * and start the CardPlayersListAction.
	 * http://211.220.31.50:8080/indoct/get_test_words.do?player_id=-5519451928541341468&test_id=-8834326842304090029   
	 * @param selected_test_id
	 */
	public Hashtable<String, String> getTestWords(final String selected_player_id, String selected_test_id, String number_of_words)
	{
		final String method = "getSavedClassTests";
		URL text = null; 
        try 
        {
            text = new URL("http://211.220.31.50:8080/indoct/get_test_words.do?player_id="+selected_player_id
            		+"&test_id="+selected_test_id+"&number_of_words="+number_of_words);
        } catch (MalformedURLException e) 
   		{
   			e.printStackTrace();
   		}
        Hashtable<String, String> test_words = parseTestWords(text);
        /*
        Enumeration<String> e = test_words.keys();
        int i = 0;
        while (e.hasMoreElements())
        {
        	String key = (String)e.nextElement();
            String val = test_words.get(key).trim();
            //Log.i(DEBUG_TAG, method+" got "+key+" "+val);
                   	//intent.putExtra(key,val);
            i++;
        }
                //intent.putExtra("selected_test_name", selected_test_name);
                //intent.putExtra("number_of_words", number_of_words);
                //intent.putExtra("student_id", selected_player_id);
                //intent.putExtra("selected_test_id", selected_test_id);
                //intent.putExtra("selected_test_format", selected_test_format);
                //Log.i(DEBUG_TAG, method+" start next activity with selected_player_id (student_id)"+selected_player_id);
       */
       return test_words;
	}
	
	/**
	 * This method returns a flat list of name value pairs with an index number
	 * to differentiate between test_word elements sent from the server.
	 * Sit back, and listen King Javaman, here be the stifle:
	 * <test_words>
	 * 		<test_word>
	 * 			<id>-4011783267950267722</id>
	 * 			<text>¸¸¾à</text>
	 *  		<definition>in case</definition>
	 *  		<type>writing</type>
	 *  		<category>2010 Fall Random.xml</category>
	 *  		<reading_deck_card_name>
	 *  		<writing_deck_card_name>
	 *  		<index>0</index>
	 *  	</test_word>
	 *  	...
	 *  </test_words>
	 *  These elements will be flattened into this list:
	 *  1id=-4011783267950267722
	 *  ...
	 *  1index=0
	 *  2id=...
	 * @param text
	 */
	private Hashtable<String, String> parseTestWords(URL text)
    {
    	String method = "parseTestWords";
        Log.i(DEBUG_TAG, method+": Parse a list of saved tests.");
    	Hashtable<String, String> test_words = new Hashtable<String, String>();
    	String element = null;
    	boolean capture_the_flag = false;
    	boolean test_word_flag = false;
    	boolean capture_house_deck_id = false;
    	int number_of_words = 1;
    	try 
    	{
			XmlPullParserFactory parser_creater = XmlPullParserFactory.newInstance();
			XmlPullParser parser =  parser_creater.newPullParser();
			parser.setInput(text.openStream(), null);
			int parser_event = parser.getEventType();
			while (parser_event != XmlPullParser.END_DOCUMENT)
			{
				switch(parser_event)
				{
					case XmlPullParser.TEXT:
					String value = parser.getText();
					if (capture_the_flag == true)
					{
						test_words.put(number_of_words+element, value);
						//Log.i(DEBUG_TAG, method+" put "+number_of_words+" "+element+" = "+value);
					}
					if (element.equals("index"))
					{
						test_word_flag = false;
						number_of_words++;
					}
					if (capture_house_deck_id)
					{
						test_words.put(element, value);
						capture_house_deck_id = false;
					}
					//Log.i(DEBUG_TAG, "case.TEXT "+value+" capture_the_flag="+capture_the_flag);
					case XmlPullParser.START_TAG:
					String tag_name = parser.getName();
					try
					{
						if (test_word_flag == true && tag_name.equals("id"))
						{
							capture_the_flag = true;
						}
						if (tag_name.equals("test_word"))
						{
							test_word_flag = true;
						}
						if (tag_name.equals("house_deck_id"))
						{
							capture_house_deck_id = true;
						}
						element = tag_name;
					} catch (java.lang.NullPointerException nope)
					{
					}
					//Log.i(DEBUG_TAG, "case.START_TAG "+tag_name+" capture_the_flag="+capture_the_flag);
				}
				parser_event = parser.next();
			}
			Log.i(DEBUG_TAG, method+" put number_of_words = "+(number_of_words-1)+"");
			test_words.put("number_of_words", (number_of_words-1)+"");
		} catch (XmlPullParserException e) 
		{
			e.printStackTrace();
		} catch (IOException e) 
		{
			e.printStackTrace();
		}
    	return test_words;
    }
}
