package com.curchod.domartin;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Vector;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Intent;
import android.util.Log;

import com.curchod.wherewithal.CardsActivity;
import com.curchod.wherewithal.MainActivity;

public class RemoteCall 
{
	
	/**
	 * This is filled after savedTestsListAction is called.
	 */
	Vector <SavedTest> saved_tests = null;

	private static final String DEBUG_TAG = "RemoteCall";
	
	/**
	 * Call the SavedTestsListAction to parse a list of saved tests.
	 * @param name
	 * @param pass
	 * @return
	 */
	public void savedTestsListAction(final String name, final String pass, final MainActivity main)
    {
    	String method = "Vector<SavedTest> savedTestsListAction(name,pass)";
        Log.i(DEBUG_TAG, method+": Call the SavedTestsListAction to parse a list of saved tests.");
        saved_tests = new Vector <SavedTest> ();
        new Thread()
        {
            public void run()
            {   
     
            	URL text = null;
                try 
                {
                    text = new URL("http://211.220.31.50:8080/indoct/saved_tests_list.do?name="+name+"&pass="+pass);
                } catch (MalformedURLException e) 
           		{
           			e.printStackTrace();
           		}
                parseSavedTestsList(text);
                Intent intent = new Intent(main, CardsActivity.class);
        		intent.putExtra("saved_tests", saved_tests.size()+"");
        		for (int i=0; i < saved_tests.size(); i++)
        		{
        			SavedTest saved_test = (SavedTest)saved_tests.get(i);
        			String test_name = saved_test.getTestName();
        			String test_id = saved_test.getTestId();
        			intent.putExtra(i+"", test_name+"@"+test_id);
        		}
                main.startActivity(intent);
                /*
                Intent intent = new Intent();
                intent.setAction("com.curchod.wherewithal.RemoteBroadcast");
                main.sendBroadcast(intent); 
                */
           }
       }.start();
    }
	
	/*
	 * Parse a list of xml with this format:
	 * <saved_tests>
	 * 	<saved_test>
	 * 		<test_id>-4522254587927892754</test_id>
	 * 		<test_name>bullshit</test_name>
	 * 		<creation_timg>1302422050054</creation_timg>
	 * 	</saved_test>
	 *  ...
	 * <saved_tests>
	 * 
	 */
	private void parseSavedTestsList(URL text)
    {
    	String method = "parse(URL text)Parse a list of saved test elements.";
        Log.i(DEBUG_TAG, method+": ");
    	Hashtable <String,String>user_info = new Hashtable<String, String>();
    	String element = null;
    	boolean capture_the_flag = false;
    	String status = "parsing .";
    	try 
    	{
			XmlPullParserFactory parser_creater = XmlPullParserFactory.newInstance();
			XmlPullParser parser =  parser_creater.newPullParser();
			parser.setInput(text.openStream(), null);
			boolean start_flag = false;
			boolean test_id_flag = false; boolean test_name_flag = false;boolean creation_time_flag = false;
			String tag = null;String test_id = "";String test_name = "";String creation_time = "";
			int parser_event = parser.getEventType();
			while (parser_event != XmlPullParser.END_DOCUMENT)
			{
				switch (parser_event) 
                {

                case XmlPullParser.TEXT:
                	String value = parser.getText();
                	if (tag!=null)
                	{
                		if (start_flag = true)
                		{
                			if (tag.equals("test_id"))
                			{
                				test_id = value;
                				test_id_flag = true;
                				start_flag = false;
                				//Log.i(DEBUG_TAG, "test_id "+test_id);
                			} else if (tag.equals("test_name"))
                			{
                				test_name = value;
                				test_name_flag = true;
                				start_flag = false;
                				//Log.i(DEBUG_TAG, "test_name "+test_name);
                			} else if (tag.equals("creation_time"))
                			{
                				creation_time = value;
                				creation_time_flag = true;
                				start_flag = false;
                				//Log.i(DEBUG_TAG, "creation_time "+creation_time);
                			}
                		}
                	}
                		
                case XmlPullParser.START_TAG:
                	tag = parser.getName();
                	try
                	{
                		if (tag.equals("saved_test"))
                			start_flag = true;
                		//Log.i(DEBUG_TAG, "start tag "+tag);
                	} catch (java.lang.NullPointerException npe)
                	{
                		//Log.i(DEBUG_TAG, "NPEEEEE");
                	}
                	
                case XmlPullParser.END_TAG:
                	if (creation_time_flag&&test_name_flag&&test_id_flag)
                	{
                		SavedTest saved_test = new SavedTest();
                		saved_test.setTestId(test_id);
                		saved_test.setTestName(test_name);
                		saved_test.setCreationTime(creation_time);
                		saved_tests.add(saved_test);
                		Log.i(DEBUG_TAG, "added: test_id "+test_id+" test_name "+test_name+" creation "+creation_time);
                		creation_time_flag = false;test_name_flag = false;test_id_flag = false; start_flag = false;
                	}
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
	
	public Vector <SavedTest> getSavedTests()
	{
		return saved_tests;
	}
}
