package com.curchod.domartin;

import java.util.Hashtable;

import android.util.Log;

public class Scoring 
{
	
	private static final String DEBUG_TAG = "Scoring";
	
	public static String applyOptions(Hashtable <String,String>user_options, String text)
	{
		if (user_options == null)
		{
			user_options = defaultOptions();
		}
		String grade_whitespace = (String)user_options.get("grade_whitespace");
		String exclude_chars = (String)user_options.get("exclude_chars");
		String exclude_area = (String)user_options.get("exclude_area");
		String exclude_area_begin_char = (String)user_options.get("exclude_area_begin_char");
		String exclude_area_end_char = (String)user_options.get("exclude_area_end_char");
		//String alternate_answer_separator = (String)user_options.get("alternate_answer_separator");
		if (grade_whitespace.equals("false"))
		{
			text = text.replace(" ", "");
		}
		if (exclude_area.equals("true"))
		{
			text = excludeArea(exclude_area_begin_char, exclude_area_end_char, text);
		}
		if (exclude_chars.length()>0)
		{
			text = removeExcludeChars(text, exclude_chars);
		}
		return text;
	}
	
	public static String removeExcludeChars(String text, String exclude_chars)
	{
		int i = 0;
		int size = exclude_chars.length();
		while (i<size)
		{
			String c = exclude_chars.substring(i, i+1);
			if (text.contains(c))
			{
				text = text.replace(c, "");
			}
			i++;
		}
		return text;
	}
	
	private static String excludeArea(String exclude_area_begin, String exclude_area_end, String text)
	{
		String first_part = new String();
		int first = text.indexOf(exclude_area_begin);
		int last = text.lastIndexOf(exclude_area_end);
		int length = text.length();
		try
		{
			first_part = text.substring(0, first);
			if (length != last)
			{
				String last_part = text.substring(last+1, length);
				first_part = first_part.concat(last_part);
			}
		} catch (java.lang.StringIndexOutOfBoundsException sioob)
		{
			first_part = text;
		}
		return first_part;
	}
	
	public static boolean scoreAnswer(String question, String answer)
	{
		String method = "scoreAnswer";
		answer = Scoring.applyOptions(null, answer);
		answer = Scoring.applyOptions(null, answer);
		Log.i(DEBUG_TAG, method+" answer  after apply options "+answer);
		Log.i(DEBUG_TAG, method+" correct after apply options "+answer);
		if (answer.equals(answer))
		{ 
			Log.i(DEBUG_TAG, method+" correct");
			return true;
		}
		Log.i(DEBUG_TAG, method+" incorrect!");
		return false;
	}
	
	private static Hashtable<String,String> defaultOptions()
	{
		Hashtable <String,String> user_options = new Hashtable <String,String>();
		user_options.put("grade_whitespace","false");
		user_options.put("max_level","3");
		user_options.put("exclude_chars",".~!@#$%^*()_+-=?'\";/:-//");
		user_options.put("exclude_area","true");
		user_options.put("exclude_area_begin_char","[");
		user_options.put("exclude_area_end_char","]");
		user_options.put("alternate_answer_separator","");
		return user_options;
	}

}
