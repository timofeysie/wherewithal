package com.curchod.wherewithal;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Looper;
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

import com.curchod.domartin.Constants;
import com.curchod.domartin.HouseDeck;
import com.curchod.domartin.IWantTo;
import com.curchod.domartin.UtilityTo;

/**
 * Setup the list of tests for the user to select.
 * Load the game.xml file in id_status.put(attribute, value) so that
 * 1. we can highlight the game/test previously selected.
 * 2. we know if the user is selecting a different test, in which case the old game file
 * will be over written.
 * 3. we will know if all the players have set up their cards and the game is ready to play.
 * test_name and test_id contains the name saved in the game.xml file.
 * Starting with onCreate 
 * from the intent, test names and ids.
 * test_name_ids = new Hashtable<String,String>();
 * String[] TESTS set into the list adapter.
 * loadGameFile();
 * saved_tests is set up with info from the file, like status.
 * checkForGameReady();
 * Then we wait for the user to select a test.  For previously selected tests we call:
 * getSavedClassTestsAndSaveGame(selected_test_id, selected_test_name, already_saved);
 * or for newly selected tests. 	          
 * confirmDeletion(selected_test_name, selected_test_id);  
 * 
 * In a menu item, the user can create a house deck which can be re-used.
 * 
 * @author user
 *
 */
public class CardsActivity extends ListActivity 
{

	private static final String DEBUG_TAG = "CardsActivity";
	private String teacher_id = "0000000000000000001";
	private String password = "teach";
	URL text = null;
	private Hashtable <String, String> test_name_ids;
	private Hashtable <String, String> saved_tests;
	private String test_id;
	private String test_name;
	private String test_status;
	private String class_id;
	private Vector <String> player_ids;
	/**These are used to show the status of each student is at while setting up cards.*/
	/**player_id-status.  The ids are the same as remote_player_ids.*/
	private Hashtable <String, String> id_status;
	final Context context = this;
	private boolean ready;
	private String[] TESTS;
	private ArrayAdapter array_adapter;
	private boolean ok_to_start_next_activity;
	private static final int house_decks_id = 1;
	/** This hold the player id-name pairs after loadGameFileAndParsePlayerNames finished. */
	private Hashtable<String, String> player_id_names;
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		String method = "onCreate";
		String build = "build 54";
		Log.i(DEBUG_TAG, method+" "+build);
		test_name_ids = new Hashtable<String,String>();
		ready = false;
		Intent sender = getIntent();
		int size = Integer.parseInt(sender.getExtras().getString("saved_tests"));
		TESTS = new String[size];
		for (int i=0; i < size; i++)
		{
			String saved_test_name_id = sender.getExtras().getString(i+"");
			int index_of_separator = saved_test_name_id.indexOf("@");
			String saved_test_name = saved_test_name_id.substring(0, index_of_separator);
			String saved_test_id = saved_test_name_id.substring(index_of_separator+1, saved_test_name_id.length());
			//Log.i(DEBUG_TAG, method+" "+i+" saved_test_name_id "+saved_test_name_id);
			test_name_ids.put(saved_test_name, saved_test_id);
			TESTS[i] = saved_test_name;
		}
		this.setListAdapter(
				array_adapter = new ArrayAdapter<String>(this, com.curchod.wherewithal.R.layout.activity_cards, 
						com.curchod.wherewithal.R.id.card_layout, TESTS)
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
					if (item.equals(test_name))
					{
						this_position = pos;
					} else
					{
					}
				}
		        if (position == this_position)
		        {
		        	if (ready)
		        	{
		        		renderer.setBackgroundResource(com.curchod.wherewithal.R.color.aqua);
		        	} else
		        	{
		        		renderer.setBackgroundResource(android.R.color.darker_gray);
		        	}
		        } else
                {
                	renderer.setBackgroundResource(android.R.color.background_light);
                }
		        return renderer;
		    }
		});
		final ListView list_view = getListView();
		list_view.setTextFilterEnabled(true); 
		loadGameFileAndParsePlayerNames();
		list_view.setOnItemClickListener(new OnItemClickListener() 
		{
	          public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
	          {
	        	  String method = "onItemClick";
	              String selected_test_name = TESTS[position];
	              String selected_test_id = test_name_ids.get(selected_test_name);
	              //Log.i(DEBUG_TAG, "selected_test_name "+selected_test_name);
	              //Log.i(DEBUG_TAG, "selected_test_id "+selected_test_id);
	              //Log.i(DEBUG_TAG, "test_id "+test_id);
	              //Log.i(DEBUG_TAG, "test_name "+test_name);
	              if (selected_test_id.equals(test_id))
	              {
	            	  //Log.i(DEBUG_TAG, method+" game file already saved.");
	            	  boolean already_saved = true;
	            	  getSavedClassTestsAndSaveGame(selected_test_id, selected_test_name, already_saved);
	              } else
	              {
	            	  //Log.i(DEBUG_TAG, method+" confirm deletion and make new game file.");
	            	  confirmDeletion(selected_test_name, selected_test_id);
	              }
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
	              //Log.i(DEBUG_TAG, "onItemClick: selected test "+selected_test_id+" "+selected_test_name);	              
			}
	    });
	}
	
	/**
	 * If the user selects a different game to setup, we need to alert the user before we re-write the game.xml file with the new game info.
	 * @param selected_test_name
	 * @param selected_test_id
	 */
	private void confirmDeletion(final String selected_test_name, final String selected_test_id)
	{
		LayoutInflater layout_inflater = LayoutInflater.from(context);
		View popup_view = layout_inflater.inflate(com.curchod.wherewithal.R.layout.card_player_words_popup, null);
		final AlertDialog.Builder alert_dialog_builder = new AlertDialog.Builder(context);
		alert_dialog_builder.setView(popup_view);
		final TextView ard_player_words_popup_text = (TextView) popup_view.findViewById(com.curchod.wherewithal.R.id.card_player_words_popup_text);
		ard_player_words_popup_text.setText(com.curchod.wherewithal.R.string.confirm_deletion);
		alert_dialog_builder.setCancelable(false).setPositiveButton("OK",
        		new DialogInterface.OnClickListener() 
            	{
				    public void onClick(DialogInterface dialog,int id) 
				    {
				    	String msg = context.getString(com.curchod.wherewithal.R.string.set_up_game)+" "+selected_test_name;
				    	Toast.makeText(context, msg, Toast.LENGTH_LONG ).show();
				    	createNewCardsFile();
				    	boolean already_saved = false;
				    	saved_tests = new Hashtable<String, String>();
				    	getSavedClassTestsAndSaveGame(selected_test_id, selected_test_name, already_saved);
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
	
	/**
	 * Using a string buffer, we create the initial players.xml file with a new entry for the first player with a default icon name.
	 */
	private void createNewCardsFile()
    {
    	String method = "createNewPlayersFile(String path_to_players_files, String name, String id)";
        //Log.i(DEBUG_TAG, method+": Using a string buffer, we create the initial players.xml file with a new entry for the first player with a default icon name.");
    	try 
    	{
    		FileOutputStream fos = openFileOutput(Constants.CARDS_XML, Context.MODE_PRIVATE);
	        try
	        {
	        	StringBuffer sb = new StringBuffer();
				sb.append("<cards />");
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
	 * Highlight the game that has been selected previously and is currently being setup.
	 */
	private void higlightSavedGame()
	{
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View row_view = inflater.inflate(android.R.layout.simple_list_item_1, this.getListView(), false);
		ListView list_view = getListView();
		list_view.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		ArrayAdapter<?> adapter = (ArrayAdapter<?>) list_view.getAdapter();
		//Log.i(DEBUG_TAG, "higlightSavedGame: looking for test name "+test_name);
		for (int position = 0; position < adapter.getCount(); position++)
		{		
			String item = (String)adapter.getItem(position);
			if (item.equals(test_name))
			{
				View view = adapter.getView(position, list_view, null);
				view.setBackgroundColor(Color.RED);
				//Log.i(DEBUG_TAG, "found item");
			} else
			{
				//Log.i(DEBUG_TAG, "item "+item);
			}
		}
	}
	
	/**
	 * Parse the remote call to GetSavedClassTestsAction, parse the results, put them in the intent
	 * and start the CardPlayersListAction.
	 * http://ip/indoct/get_saved_class_tests.do?teacher_id=0000000000000000001&pass=teach&test_id=-8834326842304090029
	 * @param selected_test_id
	 */
	private void getSavedClassTestsAndSaveGame(final String selected_test_id, final String selected_test_name, final boolean already_saved)
	{
		final String method = "getSavedClassTests";
        //Log.i(DEBUG_TAG, method+": ");
		SharedPreferences shared_preferences = context.getSharedPreferences(Constants.PREFERENCES, Activity.MODE_PRIVATE);
		String ip = shared_preferences.getString(Constants.SERVER_IP, "");
        try 
        {
            text = new URL("http://"+ip+"/indoct/get_saved_class_tests.do?teacher_id="+teacher_id
            		+"&pass="+password
            		+"&test_id="+selected_test_id);
        } catch (MalformedURLException e) 
   		{
   			e.printStackTrace();
   		}
    	new Thread()
        {
            public void run()
            {   
            	if (!already_saved)
            	{
            		resetDeckPlayerIdGameNames();
            	}
            	remoteSavedClassTestsCall(text);
            	//Log.i(DEBUG_TAG, method+" saved_tests after remote call "+saved_tests.size());
            	Intent intent = new Intent(CardsActivity.this, CardPlayersListActivity.class);
                Enumeration<String> e = saved_tests.keys();
                while (e.hasMoreElements())
                {
                   	String key = (String)e.nextElement();
                   	String val = saved_tests.get(key);
                    //Log.i(DEBUG_TAG, method+" --- put "+key+" "+val+" into the intent");
                   	intent.putExtra(key,val);
                }
                intent.putExtra("selected_test_name", selected_test_name);
                intent.putExtra("selected_test_id", selected_test_id);
                //Log.i(DEBUG_TAG, method+" start next activity");
                if (!already_saved)
                {
                	saveGameFile(selected_test_name, selected_test_id);
                }
                if (ok_to_start_next_activity)
                {
                	startActivity(intent);
                } else
                {
                	Looper.prepare();
                	Toast.makeText(context, com.curchod.wherewithal.R.string.remote_call_failed, Toast.LENGTH_LONG ).show();
                }
           }
       }.start();
	}
	
	private void resetDeckPlayerIdGameNames()
	{
		IWantTo i_want_to = new IWantTo(context);
    	Hashtable <String,HouseDeck> house_decks_copy = new Hashtable <String,HouseDeck> ();
    	Hashtable <String,HouseDeck> house_decks = i_want_to.loadTheHouseDecks();
    	Enumeration <String> e = house_decks.keys();
    	while (e.hasMoreElements())
    	{
    		String key = (String)e.nextElement();
    		HouseDeck this_house_deck = house_decks.get(key);
    		this_house_deck.setGameId("null");
    		this_house_deck.setPlayerId("null");
    		house_decks_copy.put(key, this_house_deck);
    	}
    	i_want_to.saveTheHouseDecks(house_decks_copy);
	}
	
	/**
     * Write all the game.xml file with the following format:
     * <game>
     * 	<test_name>
     * 	<test_id>
     *  <class_id>
     * 	<test_type>
     * 	<test_status>
     * 	<test_format>
     * </game>
     * @param selected_test_name
     * @param selected_test_id
     */
    private void saveGameFile(String selected_test_name, String selected_test_id)
    {
    	String method = "saveGameFile";
        //Log.i(DEBUG_TAG, method+": Using a string buffer, we create the initial players.xml file with a new entry for the first player with a default icon name.");
    	try 
    	{
    		FileOutputStream fos = openFileOutput(Constants.GAME_XML, Context.MODE_PRIVATE);
    		//Log.i(DEBUG_TAG, method+": FD "+fos.getFD());
	        try
	        {
	        	StringBuffer sb = new StringBuffer();
				sb.append("<game>");
				sb.append("<test_name>"+selected_test_name+"</test_name>");
				sb.append("<test_id>"+selected_test_id+"</test_id>");
				sb.append("<class_id>"+class_id+"</class_id>");
				sb.append("<test_type>"+saved_tests.get("1test_type")+"</test_type>");
				//sb.append("<test_status>"+saved_tests.get("1test_status")+"</test_status>");
				sb.append("<test_status>setup</test_status>");
				sb.append("<test_format>"+saved_tests.get("1test_format")+"</test_format>");
				Log.i(DEBUG_TAG, method+" test_format "+saved_tests.get("1test_format"));
				Enumeration<String> e = id_status.keys();
				//Log.i(DEBUG_TAG, " students in id_status "+id_status.size());
                while (e.hasMoreElements())
                {
					String player_id = e.nextElement();
					String status = id_status.get(player_id);
					sb.append("<player_status id=\""+player_id+"\">setup</player_status>");
					//Log.i(DEBUG_TAG,  method+"wrote <player_status id=\""+player_id+"\">setup</player_status>");
				}
				sb.append("</game>");
				//Log.i(DEBUG_TAG, "writing "+new String(sb));
				fos.write(new String(sb).getBytes());
				fos.close();
				//Log.i(DEBUG_TAG, method+": done");
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
	 * Remote call with this url http://ip/indoct/get_saved_class_tests.do?teacher_id=0000000000000000001&pass=teach&test_id=-8834326842304090029
	 * retrieves a list of saved tests for players who are part of that test.
	 * This method depends on knowing the last sub-element
	 * <saved_tests>
	 * 	<saved_test>
	 * 		<student_id>91971116904032786</student_id>
	 * 		<student_name>two</student_name>
	 * 		<test_date>1353122725066</test_date>
	 * 		<test_type>reading_and_writing</test_type>
	 * 		<test_status>pending</test_status>
	 * 		<test_format>IntegratedTest</test_format>
	 * 		and number_of_words
	 * 	</saved_test>
	 * 	...
	 * </saved_tests>
	 * 
	 * shared info:
	 * test_date=
	 * test_format=
	 * player_id=
	 * player_name=
	 * player_test_type=
	 * player_test_status=
	 * player_id=
	 * player_name=
	 * player_test_type=
	 * player_test_status=
	 * number_of_players=
	 * @param text
	 */
	private void remoteSavedClassTestsCall(URL text)
    {
    	String method = "remoteCall(URL text)";
        //Log.i(DEBUG_TAG, method+": Parse a list of saved tests. 888888888888");
        //saved_tests = new Hashtable<String, String>();
        id_status = new Hashtable<String, String>();
        String element = null;
    	boolean capture_the_flag = false;
    	boolean saved_test_flag = false;
    	int number_of_tests = 1;
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
						saved_tests.put(number_of_tests+element, value);
						//Log.i(DEBUG_TAG, method+" saved_tests.put(number_of_tests + "+number_of_tests+" element "+element+" = "+number_of_tests+element+", "+value+")");
					} if (element.equals("number_of_words"))
					{
						saved_test_flag = false;
						number_of_tests++;
						//Log.i(DEBUG_TAG, method+" number_of_tests incremented to "+number_of_tests);
					} else if (element.equals("student_id"))
					{
						//saved_tests.put(number_of_tests+"player_status", id_status.get(value));
						id_status.put(value, Constants.SETUP);
						//Log.i(DEBUG_TAG,  method+" put "+number_of_tests+"player_status, id_status.get(value) "+id_status.get(value));
					} else if (element.equals("class_id"))
					{
						class_id = value;
						//Log.i(DEBUG_TAG,  method+" class_id "+class_id);
					}
					case XmlPullParser.START_TAG:
					String tag_name = parser.getName();
					try
					{
						if (saved_test_flag == true && tag_name.equals("student_id"))
						{
							capture_the_flag = true;
						}
						if (tag_name.equals("saved_test"))
						{
							saved_test_flag = true;
						}
						element = tag_name;
					} catch (java.lang.NullPointerException nope)
					{
					}
					//Log.i(DEBUG_TAG, "case.START_TAG "+tag_name+" capture_the_flag="+capture_the_flag);
				}
				parser_event = parser.next();
			}
			saved_tests.put("number_of_tests", (number_of_tests-1)+"");
			resetPlayerStatus(number_of_tests);
			ok_to_start_next_activity = true;
			//Log.i(DEBUG_TAG, method+" put number_of_tests = "+(number_of_tests-1)+" 8888888888888");
		} catch (XmlPullParserException e) 
		{
			Log.i(DEBUG_TAG, method+" XmlPullParserException");
			ok_to_start_next_activity = false;
			e.printStackTrace();
		} catch (java.net.SocketException se)
		{
			Log.i(DEBUG_TAG, method+" SocketException");
			ok_to_start_next_activity = false;
			se.printStackTrace();
		} catch (IOException e) 
		{
			Log.i(DEBUG_TAG, method+" IOException");
			ok_to_start_next_activity = false;
			e.printStackTrace();
		}
    }

	private void resetPlayerStatus(int number_of_tests)
	{
		for (int i = 0; i < number_of_tests; i++)
		{
			saved_tests.put(number_of_tests+"player_status", Constants.SETUP);
		}
	}
	
	/**
	 * Open the game.xml file. Read the data into class member variables.
	 * This file has the following format:
	 * <game>
     * 	<test_name>
     * 	<test_id>
     * 	<test_type>
     * 	<test_status>
     * 	<test_format>
     *  <player_id>
     *  <player_status id="7655807335881695697">setup</player_status>
     *  ...
     * </game>
	 */
	public void loadGameFileAndParsePlayerNames()
    { 
		final String method = "loadGameFile"; 
    	//Log.i(DEBUG_TAG, method+": ===============");
    	player_ids = new Vector();
    	id_status = new Hashtable<String,String>();
    	saved_tests = new Hashtable<String, String>();
    	new Thread() 
    	{
            public void run() 
            {
            	int number_of_tests = 1;
            	FileInputStream fis = null;
				try 
				{
					fis = openFileInput("game.xml");
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
                	String attribute = "";
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
                        				test_id = value;
                        				saved_tests.put(number_of_tests+"test_id", value);
                        				//Log.i(DEBUG_TAG, "test_id loaded "+test_id);
                        			} else if (tag.equals("class_id"))
                        			{
                        				class_id = value;
                        				saved_tests.put(number_of_tests+"class_id", value);
                        				//Log.i(DEBUG_TAG, "class_id loaded "+class_id);
                        			}  else if (tag.equals("test_status"))
                        			{
                        				test_status = value;
                        				saved_tests.put(number_of_tests+"test_status", value);
                        				//Log.i(DEBUG_TAG, "loaded "+test_status);
                        			} else if (tag.equals("test_name"))
                        			{
                        				test_name = value;
                        				saved_tests.put(number_of_tests+"test_name", value);
                        				//Log.i(DEBUG_TAG, "loaded "+test_name);
                        			} else if (tag.equals("player_id"))
                        			{
                        				player_ids.add(value);
                        				saved_tests.put(number_of_tests+"player_id", value);
                        				//Log.i(DEBUG_TAG, "??? added player_id "+value);
                        			} else if (tag.equals("player_status"))
                        			{
                        				//Log.i(DEBUG_TAG, "player_id="+attribute+" status="+value);
                        				id_status.put(attribute, value);
                        				saved_tests.put(number_of_tests+"player_status", value);
                        				//Log.i(DEBUG_TAG, "saved_tests put "+number_of_tests+"player_status-"+value);
                        				number_of_tests++;
                        				attribute = "";
                        			}
                        		}
                        		capture = false;
                        		tag = null;
                        	}
                        		
                        case XmlPullParser.START_TAG:
                        	tag = parser.getName();
                        	capture = true;
                        	//Log.i(DEBUG_TAG, tag+" attribute "+parser.getAttributeValue(null, "id"));
                        	attribute = parser.getAttributeValue(null, "id");
                        }
                        parser_event = parser.next();
                    }
                    if (test_status.equals(Constants.READY))
            		{
                    	ready = true;
            		} else
            		{
            			checkForGameReady();
            		}
                } catch (Exception e) 
                {
                	Log.i(DEBUG_TAG, method+": (not) exception(al)");
                	e.printStackTrace();
                }
                IWantTo i_want_to = new IWantTo(context);
                player_id_names = i_want_to.parseRemotePLayerNames("0000000000000000001", class_id);
                //Log.i(DEBUG_TAG, method+": ===============");
            }
    	}.start();
    }
	
	/**
	 * If any student is not ready, then the ready boolean will be fals.
	 */
	private void checkForGameReady()
	{
		ready = true;
		Enumeration<String> e = id_status.keys();
		//Log.i(DEBUG_TAG, "checkForGameReady"+id_status.size());
        while (e.hasMoreElements())
        {
			String player_id = e.nextElement();
			String status = id_status.get(player_id);
			if (status.equals(Constants.SETUP))
			{
				ready = false;
			}
			//Log.i(DEBUG_TAG, "wrote <player_status id=\""+player_id+"\">setup</player_status>");
		}
        if (id_status.size() == 0)
        {
        	ready = false;
        }
	}
	
	/**
	 * Load the house_decks.xml file, fill the intent with the names and sizes of each and
	 * start the next activity.  The number_of_decks starts at 1!
	 */
	private void loadDecksAndStartNextActivity()
	{
		final String method = "loadDecksAndStartNextActivity";
		new Thread()
        {
            public void run()
            {   
            	IWantTo i_want_to = new IWantTo(context);
                Hashtable <String,HouseDeck> decks = i_want_to.loadTheHouseDecks();
                Log.i(DEBUG_TAG, method+" decks "+decks.size());
                Intent intent = new Intent(CardsActivity.this, CardDecksActivity.class);
                Enumeration<String> e = decks.keys();
                int i = 0;
                while (e.hasMoreElements())
                {
                	String key = (String)e.nextElement();
                	HouseDeck deck = decks.get(key);
                	intent.putExtra((i+1)+"deck_name", deck.getDeckName());
                	intent.putExtra((i+1)+"deck_id", deck.getDeckId());
                	intent.putExtra((i+1)+"player_id", deck.getPlayerId());
                	try
                	{
                		intent.putExtra((i+1)+"player_name", player_id_names.get(deck.getPlayerId()));
                		Log.i(DEBUG_TAG, method+" put "+deck.getDeckName()+" for player "+player_id_names.get(deck.getPlayerId()));
                	} catch (java.lang.NullPointerException npe)
                	{
                		intent.putExtra((i+1)+"player_name", "n/a");
                		Log.i(DEBUG_TAG, method+" put "+deck.getDeckName()+" player name n/s");
                	}
                	intent.putExtra((i+1)+"game_id", deck.getGameId());
                	i++;
                }
                intent.putExtra("number_of_decks", (i+1)+"");
                Log.i(DEBUG_TAG, method+" number_of_decks: "+(i+1));
                startActivity(intent);
            }
        }.start();
	}
	
	@Override
    public void onResume()
    {
         super.onResume();
         Log.i(DEBUG_TAG, "onResume");
         array_adapter.notifyDataSetChanged();
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		super.onCreateOptionsMenu(menu);
		menu.add(0 , house_decks_id, 0, com.curchod.wherewithal.R.string.house_decks);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
    {
    	String method = "onOptionsItemSelected(MenuItem)";
    	getIntent();
    	if (item.getItemId() == house_decks_id)
    	{
    		Log.i(DEBUG_TAG, method+": selected house decks");
    		loadDecksAndStartNextActivity();
    	    return true;
    	}
    	return super.onOptionsItemSelected(item);
    }

}
