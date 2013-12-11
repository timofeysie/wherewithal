package com.curchod.domartin;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

public class UtilityTo 
{
	
	private static final String DEBUG_TAG = "Utilities";
	
	public static final String PLAYERS = "players";
	public static final String PLAYERS_XML = "players.xml";
	public static final String GAME_XML = "game.xml";
	public static final String CARDS_XML = "cards.xml";
	public static final String PLAYERS_TAG = "<players/>";
	public static final String READING = "reading";
	public static final String WRITING = "writing";
	public static final String READY = "ready";
	public static final String SETUP = "setup";
	public static final String SET = "set";
	public static final String YET_TO_BE_PLAYED = "yet_to_be_played";
	public static final String PLAYED = "played";
	public static final String GAME_READY = "game_ready";
	public static final String PLAYING = "playing";
	public static final String FINAL_ROUND = "final_round";
	public static final String GAME_OVER = "game_over";

	/**
	 * Create a path which would work out to:
	 * path_to_files/users/
	 * @param context
	 * @param file
	 * @return
	 */
	public static String pathToPlayersFile(Context context)
	{
    	File file_dir = context.getFilesDir();
    	String path_to_players_file = file_dir.getAbsolutePath()+File.separator+UtilityTo.PLAYERS_XML;
    	return path_to_players_file;
	}
	
	public static int getStringIdentifier(Context context, String name) 
	{
		String method = "getStringIdentifier";
		int identifier = 0;
		try
		{
			identifier = context.getResources().getIdentifier(name, "string", context.getPackageName());
		} catch (java.lang.NullPointerException npe)
		{
			Log.i(DEBUG_TAG, method+": npe!");
		}
		return identifier;
	}
	
	/*
	 * Send the permission type you want to check for availability, read or write.
	 */
	public boolean checkMediaVailablilty(String permission_type)
	{
		boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    // We can read and write the media
		    mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
		    // We can only read the media
		    mExternalStorageAvailable = true;
		    mExternalStorageWriteable = false;
		} else {
		    // Something else is wrong. It may be one of many other states, but all we need
		    //  to know is we can neither read nor write
		    mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
		if (permission_type.equals("write"))
		{
			return mExternalStorageWriteable;
		}
		return mExternalStorageAvailable;
	}
	
	public static synchronized long getNewID()
	{
		long current = System.currentTimeMillis();
		Random rnd = new Random();
		long uniqueId = ((System.currentTimeMillis()>>>16)<<16)+rnd.nextLong();
		return current+uniqueId;
	}
	
	public static String getWord(Card card)
	{
		String word = null;
		String type = card.getWordType();
		if (type.equals(UtilityTo.READING))
		{
			word = card.getText();
		} else if (type.equals(UtilityTo.WRITING))
		{
			word = card.getDefinition();
		}
		return word;
	}
	
	public static void printBytes(byte[] data, String message)
    {
    	Log.i(DEBUG_TAG, "printBytes: "+message+"-----");
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < data.length; i++)
        {
            sb.append(" "+data[i]+" ");
        }
        Log.i(DEBUG_TAG, sb.toString());
        Log.i(DEBUG_TAG, "printBytes: "+message+"-----");
    }
	
	public static void printArray(String array [], String message)
	{
		Log.i(DEBUG_TAG, "printArray: "+message+"*****");
		for (int i = 0; i < array.length; i += 2) 
	    {
			Log.i(DEBUG_TAG, array[i]);
	    }
		Log.i(DEBUG_TAG, "printArray: "+message+"*****");
	}
	
	/**
	<p>This method will encode any string using a preset encoding.
	*/
	public static String encodeThis(String original_value)
	{
		try
		{
			byte[] utf8Bytes = original_value.getBytes("UTF8");
			String new_value = new String(utf8Bytes, "UTF8");
			return new_value;
		} catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
			return null;
		} catch (java.lang.NullPointerException n)
		{
			n.printStackTrace();
			return null;
		}
	}
	
	/**
	<p>This method will encode any string using a preset encoding.
	*/
	public static String encodeThisString(String original_value, String encoding)
	{
		try
		{
			byte[] utf8Bytes = original_value.getBytes(encoding);
			String new_value = new String(utf8Bytes, encoding);
			return new_value;
		} catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
			return null;
		} catch (java.lang.NullPointerException n)
		{
			n.printStackTrace();
			return null;
		}
	}
	
	private void bundleIterator(Intent intent)
    {
    	Bundle extras = intent.getExtras();
    	if (extras != null) 
    	{
    		Set<String> keys = extras.keySet();  
    		Iterator<String> iterate = keys.iterator();
    		while (iterate.hasNext()) 
    		{  
    			String key = iterate.next();  
    			char c = key.charAt(0);
    			if (Character.isDigit(c))
    			{
    				String property = key.substring(1, key.length());
    				String value = (String)extras.get(key);
    				Log.i(DEBUG_TAG, "property "+property+" value "+value);
		    	}
			} 
    	} else 
    	{
    		Log.i(DEBUG_TAG, "bundleIterator: intent is null");  
    	}
    }

	/**
	 * When reading an NFC tag, the encoding and language info set into the tags.
	 * This allows only numbers and a '-' character representing the long value
	 * stored in the tag.
	 * @param message
	 * @return
	 */
	public static String removeHiddenCharacters(String message)
	{
		String dash_string = "-";
		char dash = dash_string.charAt(0);
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < message.length(); i++)
		{
			char c = message.charAt(i);
			if (Character.isDigit(c))
			{
				sb.append(c);
			} else if (c == dash)
			{
				sb.append(dash);
			}
		}
		String result = new String(sb);
		return result;
	}
	
}
