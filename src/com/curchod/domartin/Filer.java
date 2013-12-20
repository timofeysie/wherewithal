package com.curchod.domartin;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import com.curchod.dto.PlayerInfo;

import android.content.Context;
import android.util.Log;

public class Filer 
{

	private static final String DEBUG_TAG = "Filer";
	
	/**
	 * Open the local playrs.xml file and add PlayerInfo object for each player entry.
	 * @param filename
	 * @param context_wrapper
	 * @return
	 */
	public Vector <PlayerInfo> getPlayers(final Context context)
    { 
    	final Vector <PlayerInfo> players = new Vector <PlayerInfo> ();
    	final String method = "getPlayers(filename)"; 
    	Log.i(DEBUG_TAG, method+": start parse");
    	new Thread() 
    	{
            public void run() 
            {
            	FileInputStream fis = null;
				try 
				{
					fis = context.openFileInput("players.xml");
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
                        	 Log.i(DEBUG_TAG, method+"text event: "+parser.getText());
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
                        			Log.i(DEBUG_TAG, method+": should be here");
                        		}
                        		capture = false;
                        		tag = null;
                        		Log.i(DEBUG_TAG, method+": reset capture");
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
            }
    	}.start();
    	Log.i(DEBUG_TAG, method+": finished");
    	return players;
    }
	
	/**
     * Write all the PlayerInfo objects as xml elements
     * @param path_to_players_files
     * @param name
     * @param id
     */
    public void savePlayersFile(Vector <PlayerInfo> players, Context context)
    {
    	String method = "savePlayersFile(players,context)";
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
					sb.append("<id>"+info.getId()+"</id>");
					sb.append("<icon>"+info.getIcon()+"</icon>");
					sb.append("</player>");
				}
				sb.append("</players>");
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
    
    public void changePlayerIcon(String id, String new_icon, Context context)
    {
    	String method = "changePlayerIcon(id,new_icon,context)";
    	Vector <PlayerInfo> new_players = new Vector <PlayerInfo> ();
    	Vector <PlayerInfo> players = getPlayers(context);
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
    	savePlayersFile(new_players, context);
    }
    
	
}
