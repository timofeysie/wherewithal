package com.curchod.domartin;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Enumeration;
import java.util.Hashtable;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TableLayout;
import android.widget.TableLayout.*;
import android.widget.TableRow;
import android.widget.TextView;

import com.curchod.dto.Game;
import com.curchod.wherewithal.GameReadingStonesActivity;
import com.curchod.wherewithal.R;

/**
 *  AsyncTask<Params, Progress, Result>{....}
 * @author Administrator
 *
 */
public class AsyncLoadGameFile extends AsyncTask<Context, Void, Game> 
{

	private String DEBUG_TAG = "AsyncLoadGameFile";
	private TableLayout table;
	private GameReadingStonesActivity grsa;
	private Hashtable <String, String> id_player_names;
	
	public void setIdPlayerNamse(Hashtable <String, String> _id_player_names)
	{
		this.id_player_names = _id_player_names;
	}
	
	public void setGameReadingStonesActivity(GameReadingStonesActivity _grsa)
	{
		this.grsa = _grsa;
	}
	
	public void setTable(TableLayout _table)
	{
		this.table = _table;
	}
	
	/**
	 * Open the game.xml file and read the data into a Game object.
	 * This file has the following format:
	 * <game>
     * 	<test_name>
     * 	<test_id>
     *  <class_id>
     * 	<test_type>
     * 	<test_status>
     * 	<test_format>
     *  <player_id>
     *  <player_status id="7655807335881695697">setup</player_status>
     *  ...
     * </game>
	 */
    protected Game doInBackground(Context ... context) 
    {
    	final String method = "loadGameFile"; 
    	Log.i(DEBUG_TAG, method+": Open the local game.xml file and parse it for game info.");
    	Game game = new Game();
    	FileInputStream fis = null;
		try 
		{
			fis = context[0].openFileInput("game.xml");
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
                				Log.i(DEBUG_TAG, "class_id loaded "+value);
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
                				Log.i(DEBUG_TAG, "game.setPlayerStatus("+parse_player_id+","+value+") for test_name "+game.getTestName());
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

    protected void onPostExecute(Game result_game) 
    {
    	table.removeAllViews();
    	Hashtable id_status = result_game.getPlayerStatus();
    	Enumeration<String> e = id_status.keys();
		while (e.hasMoreElements())
		{
			table = (TableLayout) grsa.findViewById(R.id.TableLayout01);
	        TableRow row = new TableRow(grsa);
			String key = (String)e.nextElement();
			String name = id_player_names.get(key);
			int score = Integer.parseInt((String) id_status.get(key));
			TextView t = new TextView(grsa);
	        TextView s = new TextView(grsa);   
	        t.setText(name);
	        s.setText(score+"");
	        row.addView(t);
	        row.addView(s);
	        table.addView(row,new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		}
    }
}
