package com.curchod.wherewithal;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.curchod.domartin.Constants;
import com.curchod.domartin.UtilityTo;
import com.curchod.dto.PlayerInfo;

/**
 * This activity gets the player information from the intent and displays everything.
 * The player can change their id from the menu and delete themselves if needed.
 * The player id is set into the shared preferences as the CURRENT_PLAYER_ID which needs
 * to be set to use the game Snazzy Thumbwork.
 * @author user
 *
 */
public class PlayerActivity extends Activity 
{

	private static final String DEBUG_TAG = "PlayerActivity";
	private static final int octopus_id = 1;
    private static final int seahorse_id = 2;
    private static final int crab_id = 3;
    private static final int icon_group = 1;
    private boolean octopus_selected = false;
    private boolean seahorse_selected = false;
    private boolean crab_selected = false;
    Vector <PlayerInfo> players = new Vector<PlayerInfo>();
    final Context context = this;
	
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        String method = "onCreate";
        String build = "2";
        Log.i(DEBUG_TAG, method+": "+build);
        Intent sender = getIntent();
        String player_name = sender.getExtras().getString("player_name");
        String player_id = sender.getExtras().getString("user_id");
        String old_id = sender.getExtras().getString("player_id");
        String id = sender.getExtras().getString("id");
        setCurrentPlayerId(id);
        String icon = sender.getExtras().getString("icon");
        if (player_id == null)
        {
        	player_id = old_id;
        }
        Log.i(DEBUG_TAG, method+": player_name "+player_name);
        Log.i(DEBUG_TAG, method+": player_id (user_id) "+player_id);
        Log.i(DEBUG_TAG, method+": old_id (player_id)"+old_id);
        Log.i(DEBUG_TAG, method+": icon "+icon);
        String waiting_reading_words = sender.getExtras().getString("waiting_reading_tests");
        String waiting_writing_words = sender.getExtras().getString("waiting_writing_tests");
        EditText edit_name = (EditText)findViewById(R.id.edit_player_name);
    	edit_name.setText(player_name);
    	EditText edit_id = (EditText)findViewById(R.id.edit_player_id);
    	edit_id.setText(player_id);
    	TextView value_for_waiting_reading = (TextView)findViewById(R.id.value_for_waiting_reading);
    	value_for_waiting_reading.setText(waiting_reading_words);
    	TextView value_for_waiting_writing = (TextView)findViewById(R.id.value_for_waiting_writing);
    	value_for_waiting_writing.setText(waiting_writing_words);
    	Set<String> keys = sender.getExtras().keySet();
    	createImageView(icon, player_id);
    	Iterator<String> i = keys.iterator();
    	while (i.hasNext())
    	{
    		String key_string = (String)i.next();
    		Log.i(DEBUG_TAG, "key: "+key_string);
    	}
    }
    
    private void setCurrentPlayerId(String player_id)
    {
    	String method = "setPlayerId";
    	SharedPreferences shared_preferences = context.getSharedPreferences(Constants.PREFERENCES, Activity.MODE_PRIVATE);
    	Editor shared_editor = shared_preferences.edit();
    	shared_editor.putString(Constants.CURRENT_PLAYER_ID, player_id);
		shared_editor.commit();
	    Log.i(DEBUG_TAG, method+" set current player id "+player_id);
    }
    
    /**
     * Set the image based on the text in the icon.
     * @param icon
     */
    private void createImageView(String icon, String player_id)
    {
    	String method = "createImageView";
    	ImageView image = (ImageView)findViewById(R.id.image_view_reading_game);
    	Log.i(DEBUG_TAG, method+": utility found "+UtilityTo.getStringIdentifier(context, icon)); //icon = "crab_228";
    	if (icon.equals("crab_228"))
    	{
    		image.setImageResource(R.drawable.crab_228);
    	} else if (icon.equals("octopus_228"))
    	{
    		image.setImageResource(R.drawable.octopus_228);
    	} else if (icon.equals("seahorse_228"))
    	{
    		image.setImageResource(R.drawable.seahorse_228);
    	}
    	//ImageView image = (ImageView)findViewById(R.id.imageView1);
    	//ImageView img = new ImageView(this);
    	//img.setImageResource(Utilities.getStringIdentifier(context, icon)); 
    	//image.setImageResource(Utilities.getStringIdentifier(context, icon)); // don't work
    	//Resources res = getResources();
    	//Drawable drawable = res.getDrawable(Utilities.getStringIdentifier(context, icon));
    	//image.setImageDrawable(drawable);
    }

    /**
     * Create a menu and icon submenu.
     */
    public boolean onCreateOptionsMenu(Menu menu) 
    {
    	super.onCreateOptionsMenu(menu);
    	menu.add("Icons").setIcon(android.R.drawable.toast_frame);
    	menu.add("Delete Player");
    	SubMenu icon_choice = menu.addSubMenu("Choose icon").setIcon(android.R.drawable.ic_menu_gallery);
    	icon_choice.add(icon_group, octopus_id, 1, "Octopus").setChecked(octopus_selected);
    	icon_choice.add(icon_group, seahorse_id, 2, "Seahorse").setChecked(seahorse_selected);
    	icon_choice.add(icon_group, crab_id, 2, "Crab").setChecked(crab_selected);
    	icon_choice.setGroupCheckable(icon_group, true, true);
    	return true;
    }
    
    /**
     * Change the icon when the item is selected.
     */
    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	String method = "onOptionsItemSelected(MenuItem)";
    	Log.i(DEBUG_TAG, method+": Change the icon when the item is selected.");
    	Log.i(DEBUG_TAG, method+": selected "+item.toString());
    	Intent sender = getIntent();
    	String player_id = sender.getExtras().getString("user_id");
    	if (item.getItemId() == crab_id)
    	{
    	    crab_selected = true;
        	octopus_selected = false;
        	seahorse_selected = false;
        	loadPlayersAndChangeIcon(player_id, "crab_228", sender);
        	Log.i(DEBUG_TAG, method+" - crab_id");
        	createImageView("crab_228", player_id);
    	    return true;
    	} if (item.getItemId() == octopus_id)
    	{
    	    crab_selected = false;
        	octopus_selected = true;
        	seahorse_selected = false;
        	loadPlayersAndChangeIcon(player_id, "octopus_228", sender);
        	Log.i(DEBUG_TAG, method+" - octopus_id");
        	createImageView("octopus_228", player_id);
    	    return true;
    	} else if (item.getItemId() == seahorse_id)
    	{
    	    crab_selected = false;
        	octopus_selected = false;
        	seahorse_selected = true;
        	loadPlayersAndChangeIcon(player_id, "seahorse_228", sender);
        	Log.i(DEBUG_TAG, method+" - seahorse_id");
        	createImageView("seahorse_228", player_id);
    	    return true;
    	} else if (item.getTitle() == "Delete Player")
    	{
    		Log.i(DEBUG_TAG, method+": delete this player");
    		deletePlayer(player_id);
    		// return to players activity
    		startActivity(new Intent(PlayerActivity.this, PlayersActivity.class));
    	}
    	return super.onOptionsItemSelected(item);
    }
    
    /**
     * Delete this player.
     * @param player_id
     */
    private void deletePlayer(final String player_id)
    {
		final String method = "deletePlayer"; 
    	Log.i(DEBUG_TAG, method+": Delete this player.");
    	players = new Vector<PlayerInfo>();
    	new Thread() 
    	{
            public void run() 
            {
            	FileInputStream fis = null;
				try 
				{
					fis = openFileInput("players.xml");
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
                	boolean capture = false;
                	String tag = "";String name = "";int score = 0;String this_id = "";
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
                        			if (tag.equals("name"))
                        			{
                        				name = value;
                        				Log.i(DEBUG_TAG, method+": name");
                        			} else if (tag.equals("score"))
                        			{
                        				score = Integer.parseInt(value);
                        				Log.i(DEBUG_TAG, method+": score "+score);
                        			}  else if (tag.equals("id"))
                        			{
                        				this_id = value;
                        				Log.i(DEBUG_TAG, method+": id");
                        			} else if (tag.equals("icon"))
                        			{
                        				PlayerInfo player_info = new PlayerInfo();
                        				if (this_id.equals(player_id))
                        				{
                        					Log.i(DEBUG_TAG, method+": not adding "+name+" id "+this_id+" from "+value);
                        				}
                        				else
                        				{
                        					player_info = new PlayerInfo(name, score, this_id, value);
                        					Log.i(DEBUG_TAG, method+": loaded "+name+" id "+this_id+" icon "+value);
                        					//players.add(player_info);
                        				}
                        			}
                        		}
                        		capture = false;
                        		tag = null;
                        	}
                        		
                        case XmlPullParser.START_TAG:
                        	tag = parser.getName();
                        	capture = true;
                        	Log.i(DEBUG_TAG, method+": start_tag. set capture");
                        }
                        parser_event = parser.next();
                    }
                } catch (Exception e) 
                {
                	Log.i(DEBUG_TAG, method+": (not) exception(al)");
                	e.printStackTrace();
                }

            	savePlayersFile();
            }
    	}.start();
    }
    
    /*
    public void changePlayerIcon(String id, String new_icon)
    {
    	String method = "changePlayerIcon(id,new_icon,context)";
    	Vector <PlayerInfo> new_players = new Vector <PlayerInfo> ();
    	players = new Vector();
    	players = getPlayers();
    	Log.i(DEBUG_TAG, method+": players "+players.size());
    	for (int i = 0; i < players.size(); i++)
    	{
    		PlayerInfo player = (PlayerInfo)players.get(i);
    		String this_id = player.getId();
    		if (this_id.equals(id))
    		{
    			player.setIcon(new_icon);
    		}
    		new_players.add(player);
    	}
    	Log.i(DEBUG_TAG, method+": new_players "+new_players.size());
    	savePlayersFile();
    }
    */
    
    /**
	 * Open the local playrs.xml file and add PlayerInfo object for each player entry.
	 * @param filename
	 * @param context_wrapper
	 * @return
	 */
	public void loadPlayersAndChangeIcon(final String id, final String new_icon, final Intent sender)
    { 
		final String method = "getPlayers"; 
    	Log.i(DEBUG_TAG, method+": Open the local playrs.xml file and add PlayerInfo object for each player entry.");
    	new Thread() 
    	{
            public void run() 
            {
            	FileInputStream fis = null;
				try 
				{
					fis = openFileInput("players.xml");
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
                	boolean capture = false;
                	String tag = "";String name = "";int score = 0;String this_id = "";
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
                        			if (tag.equals("name"))
                        			{
                        				name = value;
                        			} else if (tag.equals("score"))
                        			{
                        				score = Integer.parseInt(value);
                        				Log.i(DEBUG_TAG, method+": score "+score);
                        			} else if (tag.equals("id"))
                        			{
                        				this_id = value;
                        			} else if (tag.equals("icon"))
                        			{
                        				PlayerInfo player_info = new PlayerInfo();
                        				if (this_id.equals(id))
                        				{
                        					player_info = new PlayerInfo(name, score, this_id, new_icon);
                        					Log.i(DEBUG_TAG, method+": changing icon for "+name+" id "+this_id+" from "+value+" to "+new_icon);
                        				}
                        				else
                        				{
                        					player_info = new PlayerInfo(name, score, this_id, value);
                        					Log.i(DEBUG_TAG, method+": loaded "+name+" id "+this_id+" icon "+value);
                        				}
                        				players.add(player_info);
                        			}
                        		}
                        		capture = false;
                        		tag = null;
                        	}
                        		
                        case XmlPullParser.START_TAG:
                        	tag = parser.getName();
                        	capture = true;
                        }
                        parser_event = parser.next();
                    }
                } catch (Exception e) 
                {
                	Log.i(DEBUG_TAG, method+": (not) exception(al)");
                	e.printStackTrace();
                }
            	sender.putExtra("icon", new_icon);
            	savePlayersFile();
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
        Log.i(DEBUG_TAG, method+": Using a string buffer, we create the initial players.xml file with a new entry for the first player with a default icon name.");
    	try 
    	{
    		FileOutputStream fos = context.openFileOutput("players.xml", Context.MODE_PRIVATE);
    		Log.i(DEBUG_TAG, method+": FD "+fos.getFD());
	        try
	        {
	        	StringBuffer sb = new StringBuffer();
				sb.append("<players>");
				for (int i=0;i<players.size();i++)
				{
					PlayerInfo info = (PlayerInfo)players.get(i);
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
 
}
