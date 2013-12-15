package com.curchod.wherewithal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;

import com.curchod.domartin.Constants;
import com.curchod.domartin.Filer;
import com.curchod.domartin.PlayerInfo;
import com.curchod.domartin.UtilityTo;

/**
 * This activity accepts a name and a password, as well as remember me on this phone and service input
 * from the user to login to a service remotely and retrieve the players current status
 * including their id and the number of words they have waiting for play.
 * We also look in the players.xml file on the local hand set for the user's icon image,
 * then forward this information to the PlayerActivity.
 * @author user
 *
 */
public class AddPlayerActivity extends Activity 
{

	private static final String DEBUG_TAG = "AddPlayerActivity";
	
	Vector<PlayerInfo> players = new Vector<PlayerInfo>();
	
    /**
     * Create the form interface and call on the service requested to login the user
     * and get their status info when the button is pressed.  
     * Right now we only use the English Glue service
     * in the englishGLueService(name, password) method.
     */
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_player);
        String method = "onCreate";
        String build = "1";
        Log.i(DEBUG_TAG, method+": "+build);
        Log.i(DEBUG_TAG, method+": Create the form interface and call on the service requested to login the user and get their status info when the button is pressed.  Right now we only use the English Glue service in the englishGLueService(name, password) method.");
        /*
        Button button_login_player = (Button) findViewById(R.id.button_login_player);
        button_login_player.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
            	EditText name_text = (EditText)findViewById(R.id.name_text); // player name
            	Editable editable_name = name_text.getText();
            	String name = editable_name.toString();
            	EditText pass_text = (EditText)findViewById(R.id.password_text); // password
            	Editable editable_pass = pass_text.getText();
            	String password = editable_pass.toString();
            	Spinner service_spinner = (Spinner)findViewById(R.id.service_spinner); // Englishglue
            	String service = (String)service_spinner.getSelectedItem();
            	CheckBox remember_me_box = (CheckBox)findViewById(R.id.check_box_remember_me); // remember pw
            	boolean remember_me = remember_me_box.isChecked();
            	//Log.i(DEBUG_TAG, name+"\n"+password+"\n"+service+"\n"+remember_me);
            	EditText edit_text = (EditText)findViewById(R.id.editText2);
            	edit_text.setText("contacting service ...");
            	if (service.equals("EnglishGlue"))
            	{
            		englishGLueService(name, password);
            	}
            }
        });
        */
    }
    
    /**
     * Get a hash table from the loginEnglishGluePlayer(name, password) method
     * and put all it's items in the intent object to start the PlayerActivity
     * after saving the player status info sent from the service.
     * @param name
     * @param password
     */
    private void englishGLueService(String name, String password)
    {
    	String method = "englishGLueService(String name, String password)";
        Log.i(DEBUG_TAG, method+": Get a hash table from the loginEnglishGluePlayer(name, password) method and put all it's items in the intent object to start the PlayerActivity after saving the player status info sent from the service.");
    	Hashtable <String,String> user_info = loginEnglishGluePlayer(name, password);
		Intent intent = new Intent(AddPlayerActivity.this, PlayerActivity.class);
        intent.putExtra("player_name", name);
        intent.putExtra("icon", "seahorse_228");
        Enumeration <String> keys = user_info.keys();
	    while (keys.hasMoreElements())
	    {
		    String key = (String)keys.nextElement();
		    String val = (String)user_info.get(key);
		    intent.putExtra(key,val);
	    }
	    //updatePlayerInfo(user_info);  // saves updated status info
		startActivity(intent);
    }
    
    /**
     * Set up the url for the EnglishGlue service, sent the name and password,
     * receive the raw xml and call remoteUserLogin to parse the elements
     * to fill the user_info hash table.
     * @param name
     * @param pass
     * @return
     */
    private Hashtable <String,String> loginEnglishGluePlayer(final String name, final String pass)
    {
    	String method = "Hashtable <String,String> loginEnglishGluePlayer(final String name, final String pass)";
        Log.i(DEBUG_TAG, method+": Set up the url for the EnglishGlue service, sent the name and password, receive the raw xml and call remoteUserLogin to parse the elements to fill the user_info hash table.");
        final String icon = "seahorse_228"; // default
        URL text = null;
        try 
        {
            text = new URL("http://211.220.31.50:8080/indoct/remote_login.do?name="+name+"&pass="+pass);
        } catch (MalformedURLException e) 
   		{
   			e.printStackTrace();
   		}
    	final Hashtable <String,String>user_info = remoteUserLogin(text); // login and retireve id and stats
    	new Thread()
        {
            public void run()
            {   
                //Log.i(DEBUG_TAG, "items found "+user_info.size());
                Enumeration<String> e = user_info.keys();
                while (e.hasMoreElements())
                {
                   	String key = (String)e.nextElement();
                   	String val = user_info.get(key);
                   	//Log.i(DEBUG_TAG, "appending "+key+" - "+val);
                }
                user_info.put("user_icon", icon); 
           }
       }.start();
       addPlayerFileEntry(name, (String)user_info.get("user_id"), icon);
       return user_info;
    }
    
    /**
     * Load the players.xml file if it exists then call parsePlayersForIcon(). 
     * If it is the players first time on this phone, create a new icon.
     * If this is the first time the app is being run on the phone, 
     * also create the players.xml file.
     * <player>
     * 	<name>
     * 	<password>
     *  <icon>
     *  <id>
     * </player>
     * @param name
     * @param pass
     * @return
     */
    private void addPlayerFileEntry(String name, String id, String icon)
    { 
    	String method = "addPlayerFileEntry(String name, String id, String icon)";
        Log.i(DEBUG_TAG, method+": Load the players.xml file if it exists then call parsePlayersForIcon(). If it is the players first time on this phone, create a new icon.  If this is the first time the app is being run on the phone, also create the players.xml file.");
    	Context context = getApplicationContext();
    	String path_to_players_file = UtilityTo.pathToPlayersFile(context);
    	String file_path = context.getFilesDir().getAbsolutePath();
    	Log.i(DEBUG_TAG, "getPlayerIcon: file_path: "+file_path);
    	File players_file = new File(file_path, Constants.PLAYERS_XML);
    	Log.i(DEBUG_TAG, "getNewPlayerIcon: exists? "+players_file.exists());
    	if (!players_file.exists())
    	{
    		Log.i(DEBUG_TAG, "getNewPlayerIcon: no players.xml file yet, create it now");
    		createNewPlayersFile(path_to_players_file, name, id);
    	}
    	createPlayerEntry(name, id, icon);
    }
    
    /**
     * Using a string buffer, we create the initial players.xml file with a new entry
     * for the first player with a default icon name.
     * @param path_to_players_files
     * @param name
     * @param id
     */
    private void createNewPlayersFile(String path_to_players_files, String name, String id)
    {
    	String method = "createNewPlayersFile(String path_to_players_files, String name, String id)";
        Log.i(DEBUG_TAG, method+": Using a string buffer, we create the initial players.xml file with a new entry for the first player with a default icon name.");
    	try 
    	{
    		FileOutputStream fos = openFileOutput("players.xml", Context.MODE_PRIVATE);
	        try
	        {
	        	StringBuffer sb = new StringBuffer();
				sb.append("<players />");
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
     * Create an entry in the players.xml file for this new player.
     * @param path_to_players_file
     * @param name
     * @param id
     * @param icon
     */
    private void createPlayerEntry(String name, String id, String icon)
    {
    	String method = "createPlayerEntry(name, id, icon)";
    	Log.i(DEBUG_TAG, method+": Create an entry in the players.xml file for this new player.");
    	Context context = getApplicationContext();
    	Filer filer = new Filer();
    	parsePlayers();
    	Log.i(DEBUG_TAG, method+": players "+players.size());
    	PlayerInfo info = new PlayerInfo(name, 0, id, icon);
    	players.add(info);
    	Log.i(DEBUG_TAG, method+": players after add "+players.size());
    	savePlayersFile(players);
    }
    
    private void parsePlayers()
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
					fis = openFileInput(Constants.PLAYERS_XML);
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
                	String tag = "";String name = "";int score = 0; String id = "";
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
                        			}  else if (tag.equals("score"))
                        			{
                        				score = Integer.parseInt(value);
                        				Log.i(DEBUG_TAG, method+": score "+score);
                        			}else if (tag.equals("id"))
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
    private void savePlayersFile(Vector <PlayerInfo> players)
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
     * sends back an xml file with the current status of that user.
     * @param URL text
     * @return Hashtable with name-value pairs.
     */
    private Hashtable<String, String> remoteUserLogin(URL text)
    {
    	String method = "Hashtable<String, String> remoteUserLogin(URL text)";
        Log.i(DEBUG_TAG, method+": Parse the text from the EnglisGlueService RemoteLoginAction which takes the name and password of the player and sends back an xml file with the current status of that user.");
    	Hashtable <String,String>user_info = new Hashtable<String, String>();
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
			boolean user_flag = false;
			int parser_event = parser.getEventType();
			while (parser_event != XmlPullParser.END_DOCUMENT)
			{
				switch(parser_event)
				{
					case XmlPullParser.TEXT:
					String value = parser.getText();
					//Log.i(DEBUG_TAG, "case XmlPullParser.TEXT: value = "+value+" tag = "+tag);
					if (capture_the_flag == true && user_flag == true && element !=null && value !=null)
					{
						user_info.put(element, value);
						capture_the_flag = false;
						//Log.i(DEBUG_TAG, "put "+element+" - "+value);
						//Log.i(DEBUG_TAG, "--------------------------");
						element = null;
						value = null;
					}
					case XmlPullParser.START_TAG:
					String tag1 = parser.getName();
					//Log.i(DEBUG_TAG, "case XmlPullParser.START_TAG: name = "+tag1);
					if (user_flag == true)
					{
						element = tag1;
						capture_the_flag = true;
					}
					try
					{
						if (tag1.equals("user"))
						{
							user_flag = true; // start catpure next round.
							//Log.i(DEBUG_TAG, "user_flag = true ");
						}
					} catch (java.lang.NullPointerException nope)
					{
						//Log.i(DEBUG_TAG, "NOPE for if (tag1.equals user");
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
    	return user_info;
    }
    
    /**
     * Not used.
     */
    private void loadPractice()
    {
    	//EditText edit_text = (EditText)findViewById(R.id.editText2);
    	try
        {
        	URL text = new URL("http://211.220.31.50:8080/indoct/remote_login.do?name=t&pass=t");
        	HttpURLConnection http = (HttpURLConnection)text.openConnection();
        	Log.i(DEBUG_TAG, "length           = "+http.getContentLength());
        	Log.i(DEBUG_TAG, "reasponse code   = "+http.getResponseCode());
        	Log.i(DEBUG_TAG, "content type     = "+http.getContentType());
        	Log.i(DEBUG_TAG, "content encoding = "+http.getContentEncoding());
        	Log.i(DEBUG_TAG, "content          = "+http.getContent());
        	InputStream is_text = text .openStream();
        	byte[] b_text =new byte[250];
        	int read_size = is_text.read(b_text);
        	//edit_text.setText(read_size+" - "+ new String(b_text));
        	Log.i(DEBUG_TAG, "read_size = "+read_size);
        	Log.i(DEBUG_TAG, "b_text = "+new String (b_text));
        	
        	is_text.close();
        } catch (Exception e)
        {
        	//edit_text.setText("Error in network call"+e.toString());
        	Log.e(DEBUG_TAG, "Error in network call", e);
        }
    }
    
    /**
     * Not used.
     * @param text
     */
    private void parseXMLforImages(URL text)
    {
    	//EditText edit_text = (EditText)findViewById(R.id.editText2);
    	try 
    	{
			XmlPullParserFactory parser_creater = XmlPullParserFactory.newInstance();
			XmlPullParser parser =  parser_creater.newPullParser();
			parser.setInput(text.openStream(), null);
			// set text 'Parsing...
			int parser_event = parser.getEventType();
			while (parser_event != XmlPullParser.END_DOCUMENT)
			{
				switch(parser_event)
				{
				case XmlPullParser.START_TAG:
					String tag = parser.getName();
					if (tag.compareTo("link") == 0)
					{
						String rel_type = parser.getAttributeValue(null, "rel");
						if (rel_type.compareTo("enclosing") == 0)
						{
							String enc_type = parser.getAttributeValue(null, "tyoe");
							if (enc_type.startsWith("image/"))
							{
								String image_src = parser.getAttributeValue(null, "href");
								Log.i(DEBUG_TAG, "image source = "+image_src);
								//edit_text.setText(image_src);
							}
						}
					}
					break;
				}
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        getMenuInflater().inflate(R.menu.activity_add_player, menu);
        return true;
    }
}
