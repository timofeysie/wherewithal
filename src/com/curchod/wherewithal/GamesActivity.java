package com.curchod.wherewithal;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.curchod.domartin.Constants;
import com.curchod.domartin.UtilityTo;

/**
 * Display a list of games for the player to choose from.  Currently there are three games:
 * Reading Stones
 * Writing Stones
 * Snazzy Thumbwork
 * @author Administrator
 *
 */
public class GamesActivity extends Activity 
{

	final Context context = this;
	private static final String DEBUG_TAG = "GameActivity"; 
	private Vector <String> player_ids;
	/**player_id-status.  The ids are the same as remote_player_ids.*/
	private Hashtable <String, String> id_status;
	private Hashtable <String, String> id_player_names;
	/** The data that will go into the intend sent to  */
	private Hashtable <String, String> saved_tests;
	private String test_id;
	private String test_name;
	private String test_status;
	private boolean ready;
	private String class_id;
	private String teacher_id = "0000000000000000001";
	
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        String method = "onCreate";
        String build = "build 15";
        Log.i(DEBUG_TAG, method+": "+build);
        ImageButton image_button_reading_game = (ImageButton) findViewById(R.id.image_button_reading_game);
        image_button_reading_game.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
            	loadGameFile("reading button");
            }
        });
        
        Button snazzy_button = (Button)findViewById(R.id.snazzy_button);
        snazzy_button.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
            	Intent intent = new Intent(GamesActivity.this, GameSnazzyThumbworkActivity.class);
        		startActivity(intent);
            }
        });
        
        ImageView iv = (ImageView)findViewById(R.id.image_view_reading_game);
        iv.setOnClickListener(new OnClickListener() 
        {
        	public void onClick(View v) 
        	{
        		loadGameFile("writing button");
        	}
        });
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
	public void loadGameFile(final String activity)
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
                        				//saved_tests.put(number_of_tests+"test_id", value);
                        				//Log.i(DEBUG_TAG, "loaded: test_id "+test_id);
                        			} else if (tag.equals("class_id"))
                        			{
                        				class_id = value;
                        				Log.i(DEBUG_TAG, "class_id loaded "+class_id);
                        			} else if (tag.equals("test_status"))
                        			{
                        				test_status = value;
                        				//saved_tests.put(number_of_tests+"test_status", value);
                        				//Log.i(DEBUG_TAG, "loaded: test_status "+test_status);
                        			} else if (tag.equals("test_name"))
                        			{
                        				test_name = value;
                        				//saved_tests.put(number_of_tests+"test_name", value);
                        				//Log.i(DEBUG_TAG, "loaded: test_name "+test_name);
                        			} else if (tag.equals("player_status"))
                        			{
                        				Log.i(DEBUG_TAG, "player_id = "+attribute+" status="+value);
                        				id_status.put(attribute, value);
                        				//saved_tests.put(number_of_tests+"player_status", value);
                        				//Log.i(DEBUG_TAG, "saved_tests put "+number_of_tests+"player_status-"+value);
                        				attribute = "";
                        			}
                        		}
                        		capture = false;
                        		tag = null;
                        	}
                        		
                        case XmlPullParser.START_TAG:
                        	tag = parser.getName();
                        	capture = true;
                        	attribute = parser.getAttributeValue(null, "id");
                        	Log.i(DEBUG_TAG, "START_TAG: "+tag+" attribute "+attribute);
                        	try
                        	{
                        		if (tag.equals("player_status"))
                        		{
                        			player_ids.add(attribute);
                        			Log.i(DEBUG_TAG, "??? added player_id "+attribute);
                        		}
                        	} catch (java.lang.NullPointerException npe)
                        	{
                        		// no tag
                        	}
                        	attribute = parser.getAttributeValue(null, "id");
                        }
                        parser_event = parser.next();
                    }
                } catch (Exception e) 
                {
                	Log.i(DEBUG_TAG, method+": (not) exception(al)");
                	e.printStackTrace();
                }
                checkForGameReady();
                parseRemotePLayerNames();
                fillIntentAndStartNewActivity(activity);
            }
    	}.start();
    }
	
	/**
	 * http://ip:8080/indoct/student_names.do?teacher_id=0000000000000000001&pass=teach&class_id=8549097398869562086
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
	private void parseRemotePLayerNames()
	{
    	String method = "remoteCall";
    	Log.i(DEBUG_TAG, method+" student_names.do");
    	URL text = null;
    	try 
        {
    		SharedPreferences shared_preferences = context.getSharedPreferences(Constants.PREFERENCES, Activity.MODE_PRIVATE);
    		String ip = shared_preferences.getString(Constants.SERVER_IP, "");
            text = new URL("http://"+ip+":8080/indoct/student_names.do?teacher_id="+teacher_id
            		+"&class_id="+class_id);
        } catch (MalformedURLException e) 
   		{
   			e.printStackTrace();
   		}
        id_player_names = new Hashtable<String, String>();
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
			String message = "Parser exception.";
			Toast.makeText(this, message, Toast.LENGTH_LONG ).show();
		} catch (IOException e) 
		{
			e.printStackTrace();
			final String message = "Network is unreachable.";
			((Activity)context).runOnUiThread(new Runnable() 
    		{
                public void run() 
                {
                    Toast.makeText(context, message, Toast.LENGTH_LONG ).show();
                }
            });
		}
	}
	
	/**
	 * If any student is not ready, then the ready boolean will be false.
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
		}
        if (id_status.size() == 0)
        {
        	ready = false;
        	Log.i(DEBUG_TAG, "test ready");
        }
	}
	
	/**
	 * The setup for the current three games is the same.
	 * We start the activity based on the string passed in:
	 * reading button
	 * writing button
	 * snazzy button (sample code, not implemented.
	 * We also save the player id/name pairs in the shared preferences.
	 * @param activity
	 */
	private void fillIntentAndStartNewActivity(String activity)
	{
		String method = "fillIntentAndStartNewActivity";
		SharedPreferences shared_preferences = context.getSharedPreferences(Constants.PREFERENCES, Activity.MODE_PRIVATE);
        Editor shared_editor = shared_preferences.edit();
		Intent intent = new Intent(GamesActivity.this, GameReadingStonesActivity.class);
		if (activity.equals("snazzy button"))
		{
			intent = new Intent(GamesActivity.this, GameSnazzyThumbworkActivity.class);
		} else if (activity.equals("writing button"))
		{
			intent = new Intent(GamesActivity.this, GameWritingStonesActivity.class);
		}
        intent.putExtra("test_name", test_name);
        intent.putExtra("test_id", test_id);
        intent.putExtra("test_status", test_status);
        intent.putExtra("class_id", class_id);
        Log.i(DEBUG_TAG, method+" put test_name "+test_name);
        Log.i(DEBUG_TAG, method+" put test_id "+test_id);
        Log.i(DEBUG_TAG, method+" put test_status "+test_status);
        int size = player_ids.size();
        for (int i = 0; i < size; i++)
        {
        	String player_id = (String)player_ids.get(i);
        	intent.putExtra(i+"player_id", player_id);
        	String player_name = (String)id_player_names.get(player_id);
        	shared_editor.putString(player_id, player_name);
    		shared_editor.commit();
        	intent.putExtra(i+"player_name", player_name);
        	Log.i(DEBUG_TAG, method+" put player "+player_id);
        }
        intent.putExtra("number_of_players", Integer.toString(size));
        Log.i(DEBUG_TAG, method+" put number_of_players "+size);
        startActivity(intent);
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        getMenuInflater().inflate(R.menu.activity_game, menu);
        return true;
    }
}
