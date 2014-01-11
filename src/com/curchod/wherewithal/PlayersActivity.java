package com.curchod.wherewithal;

import java.io.File;
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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;

import com.curchod.domartin.Constants;
import com.curchod.domartin.UtilityTo;
import com.curchod.dto.PlayerInfo;

/**
 * Show a list of players who have logged in and an add player button.
 * @author timothy
 *
 */
public class PlayersActivity extends Activity 
{
	Vector<PlayerInfo> players = new Vector<PlayerInfo>();
	final Context context = this;
	final String default_icon = "seahorse_228"; // default
	PlayersActivity pa = this;
	/** This is used to store the info for a newly added player.*/
	Hashtable <String,String>player_info = new Hashtable<String,String>();
	URL text = null;
	
	/**
	 * This holds a list of ListView indexes associated with their PlayerInfo objects.
	 */
	Hashtable<String, PlayerInfo> player_info_list = new Hashtable<String, PlayerInfo>();
	Handler mHandler = new Handler();
	int player_count = 0;
	
	private static final String DEBUG_TAG = "PlayersActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_players);
        final String method = "onCreate";
        String build = "1";
        Log.i(DEBUG_TAG, method+": "+build);
        loadPlayers(this); // load players from the players.xml file.
        final Button add_player_button = (Button)findViewById(R.id.button_add_player);
        add_player_button.setOnClickListener(new Button.OnClickListener()
        {
        	@Override
        	public void onClick(View arg0) 
        	{
        		Log.i(DEBUG_TAG, "onClick");
        		LayoutInflater layout_inflater = LayoutInflater.from(context);
				View popup_view = layout_inflater.inflate(R.layout.activity_add_player, null);
				final AlertDialog.Builder alert_dialog_builder = new AlertDialog.Builder(context);
				alert_dialog_builder.setView(popup_view);
				final EditText player_name_text = (EditText) popup_view.findViewById(R.id.name_text);
				final EditText player_pass_text = (EditText) popup_view.findViewById(R.id.password_text);
				alert_dialog_builder.setCancelable(false).setPositiveButton("OK",
                		new DialogInterface.OnClickListener() 
                    	{
    					    public void onClick(DialogInterface dialog,int id) 
    					    {
    					    	String name = player_name_text.getText().toString();
    					    	String pass = player_pass_text.getText().toString();
    					    	Log.i(DEBUG_TAG, "onClick: name "+player_name_text.getText().toString());
    					    	Log.i(DEBUG_TAG, "onClick: pass "+player_pass_text.getText());
    					    	dialog.cancel();
    					    	Spinner service_spinner = (Spinner)findViewById(R.id.service_spinner); 
    					    	try
    					    	{
    					    		String service = (String)service_spinner.getSelectedItem();
    					    	} catch (java.lang.NullPointerException npe)
    					    	{
    					    		Log.i(DEBUG_TAG, "spinnr is null");
    			            		Log.i(DEBUG_TAG, "EnglishGlue onClick: name "+name);
        					    	Log.i(DEBUG_TAG, "EnglishGlue onClick: pass "+pass);
        					    	loginEnglishGluePlayer(name, pass);
        					    	updateList(pa);
    			            	}
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
    
    /*
     * After the web call is complete, start the next activity.
     */
    private void startPlayerActivity(String name)
    {
    	String method = "startPlayerActivity";
    	Log.i(DEBUG_TAG, method+": After the web call is complete, start the next activity.");
    	Intent intent = new Intent(PlayersActivity.this, PlayerActivity.class);
        intent.putExtra("player_name", name);
        intent.putExtra("icon", "seahorse_228");
        Enumeration <String> keys = player_info.keys();
	    while (keys.hasMoreElements())
	    {
		    String key = (String)keys.nextElement();
		    String val = (String)player_info.get(key);
		    intent.putExtra(key,val);
	    }
	    String player_id = (String)player_info.get("player_id");
	    String user_id = (String)player_info.get("user_id");
	    String id = (String)player_info.get("id");
	    Log.i(DEBUG_TAG, method+" player_id "+player_id);
	    Log.i(DEBUG_TAG, method+" user_id "+user_id);
	    Log.i(DEBUG_TAG, method+" id "+id);
	    PlayerInfo info = new PlayerInfo(name, 0, user_id, default_icon);
	    intent.putExtra("player_id", user_id);
    	players.add(info);
    	savePlayersFile();
    	Log.i(DEBUG_TAG, method+": players after add "+players.size());
    	updateList(pa);
		startActivity(intent);
    }
    
    /**
     * Set up the url for the EnglishGlue service, sent the name and password,
     * receive the raw xml and call remotePlayerLogin to parse the elements
     * to fill the player_info hash table.
     * @param name
     * @param pass
     * @return
     */
    private void loginEnglishGluePlayer(final String name, final String pass)
    {
    	String method = "Hashtable loginEnglishGluePlayer(name,pass)";
        Log.i(DEBUG_TAG, method+": Set up the url for the EnglishGlue service, sent the name and password, receive the raw xml and call remoteplayerLogin to parse the elements to fill the player_info hash table.");
        SharedPreferences shared_preferences = context.getSharedPreferences(Constants.PREFERENCES, Activity.MODE_PRIVATE);
        String ip = shared_preferences.getString(Constants.SERVER_IP, "");
        try 
        {
            text = new URL("http://"+ip+":8080/indoct/remote_login.do?name="+name+"&pass="+pass);
        } catch (MalformedURLException e) 
   		{
   			e.printStackTrace();
   		}
    	new Thread()
        {
            public void run()
            {   
            	remotePlayerLogin(text); // login and retireve id and stats
                Enumeration<String> e = player_info.keys();
                while (e.hasMoreElements())
                {
                   	String key = (String)e.nextElement();
                   	String val = player_info.get(key);
                   	//Log.i(DEBUG_TAG, "appending "+key+" - "+val);
                }
                player_info.put("player_icon", default_icon); 
                startPlayerActivity(name);
           }
       }.start();
    }
    
    /**
     * Write all the PlayerInfo objects as xml elements
     * @param path_to_players_files
     * @param name
     * @param id
     */
    private void savePlayersFile()
    {
    	String method = "savePlayersFile(players)";
        Log.i(DEBUG_TAG, method+": Using a string buffer, we create the initial players.xml file with a new entry for the first player with a default icon name.");
    	try 
    	{
    		FileOutputStream fos = openFileOutput("players.xml", Context.MODE_PRIVATE);
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
					sb.append("<id>"+info.getId()+"</id>");
					sb.append("<icon>"+info.getIcon()+"</icon>");
					sb.append("</player>");
				}
				sb.append("</players>");
				Log.i(DEBUG_TAG, "writing "+new String(sb));
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
     * Parse the text from the EnglisGlueService RemoteLoginAction which 
     * takes the name and password of the player and
     * sends back an xml file with the current status of that player.
     * @param URL text
     * @return Hashtable with name-value pairs.
     */
    private void remotePlayerLogin(URL text)
    {
    	String method = "remotePlayerLogin(URL text)";
        Log.i(DEBUG_TAG, method+": Parse the text from the EnglisGlueService RemoteLoginAction which takes the name and password of the player and sends back an xml file with the current status of that player.");
    	player_info = new Hashtable<String, String>();
    	String element = null;
    	boolean capture_the_flag = false;
    	//EditText edit_text = (EditText)findViewById(R.id.editText2);
    	String status = "parsing .";
       	//edit_text.setText(status);
    	try 
    	{
			XmlPullParserFactory parser_creater = XmlPullParserFactory.newInstance();
			XmlPullParser parser =  parser_creater.newPullParser();
			parser.setInput(text.openStream(), null);
			// set text 'Parsing...
			boolean player_flag = false;
			int parser_event = parser.getEventType();
			while (parser_event != XmlPullParser.END_DOCUMENT)
			{
				switch(parser_event)
				{
					case XmlPullParser.TEXT:
					String value = parser.getText();
					if (capture_the_flag == true && player_flag == true && element !=null && value !=null)
					{
						player_info.put(element, value);
						capture_the_flag = false;
						if (element.equals("user_id"))
						{
							element = "player_id";
						}
						Log.i(DEBUG_TAG, "put "+element+" - "+value);
						element = null;
						value = null;
					}
					case XmlPullParser.START_TAG:
					String tag1 = parser.getName();
					if (player_flag == true)
					{
						element = tag1;
						capture_the_flag = true;
					}
					try
					{
						if (tag1.equals("user"))
						{
							player_flag = true; // start capture next round.
						}
					} catch (java.lang.NullPointerException nope)
					{
						Log.i(DEBUG_TAG, "NOPE for if (tag1.equals player");
					}
					status = status+".";
			       	//edit_text.setText(status);
				}
				//Log.i(DEBUG_TAG, "--------------------------");
				parser_event = parser.next();
			}
			// set status "Done."
		} catch (XmlPullParserException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    private void updateList(final PlayersActivity pa)
    {
    	runOnUiThread(new Runnable() 
    	{
    	     public void run() 
    	     {
    	    	 final String method = "updateList";
    	    	 String[] values = new String[players.size()];
    	    	 Log.i(DEBUG_TAG, method+": players "+players.size());
    	    	 ListView players_list_view = (ListView)findViewById(R.id.players_list_view);
    	    	 for (int i = 0 ; i  <players.size() ; i++)
    	    	 {
    	    		 PlayerInfo player = (PlayerInfo)players.get(i);
    	    		 
    	    		 values [i] = player.getName();    		
    	    		 player_info_list.put(i+"", player);
    	    	 } 
    	    	 ArrayAdapter<String> adapter = new ArrayAdapter<String>(pa, android.R.layout.simple_list_item_1, android.R.id.text1, values);
    	    	 try
    	    	 {
    	    		 players_list_view.setAdapter(adapter); 
    	    	 } catch (Exception e) 
    	    	 {
    	    		 Log.i(DEBUG_TAG, method+": What exception?");
    	    		 e.printStackTrace();
    	    	 }
    	    	 players_list_view.setOnItemClickListener(new OnItemClickListener() 
    	    	 {
    	    		  @Override
    	    		  public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
    	    		  {
    	    			  Log.i(DEBUG_TAG, method+": click on position "+position);
    	    			  PlayerInfo player = player_info_list.get(position+""); 
    	    			  Intent intent = new Intent(pa, PlayerActivity.class);
    	    		      intent.putExtra("player_name", player.getName());
    	    		      intent.putExtra("player_id", player.getId());
    	    		      intent.putExtra("icon", player.getIcon());
    	    		      startActivity(intent);
    	    		  }
    	    		}); 
    	     }
    	});
    }
    
    private void loadPlayers(PlayersActivity pa)
    {
    	String method = "loadPlayers";
    	Context context = getApplicationContext();
    	String file_path = context.getFilesDir().getAbsolutePath();//returns current directory.
    	Log.i(DEBUG_TAG, method+": file_path - "+file_path);
    	File players_file = new File(file_path, Constants.PLAYERS_XML);
    	Log.i(DEBUG_TAG, method+": exists? "+players_file.exists());
    	parsePlayers(Constants.PLAYERS_XML, pa);
    	//return players;
    }
    
    private void parsePlayers(final String filename, final PlayersActivity pa)
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
                	boolean capture = false;
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
                        	if (capture = true)
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
                        				Log.i(DEBUG_TAG, method+": score "+score);
                        			} else if (tag.equals("id"))
                        			{
                        				id = value;
                        				Log.i(DEBUG_TAG, method+": id "+id);
                        			} else if (tag.equals("icon"))
                        			{
                        				PlayerInfo player_info = new PlayerInfo(name, score, id, value);
                        				players.add(player_info);
                        				Log.i(DEBUG_TAG, method+": name "+name+" id "+id+" icon "+value);
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
                updateList(pa); // put the players in the list now.
            }
    	}.start();
    	Log.i(DEBUG_TAG, method+": finished");
    }
    
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_players, menu);
        return true;
    }
}
