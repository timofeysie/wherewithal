package com.curchod.domartin;

import java.util.Hashtable;

/**
 * <game>
<test_name>neu3_writing</test_name>
<test_id>-4580659490352698997</test_id>
<test_type>writing</test_type>
<test_status>setup</test_status>
<test_format>CardGame</test_format>
<player_status id="7655807335881695697">setup</player_status>
<player_status id="4275947837592331318">setup</player_status>
<player_status id="-4532926416625862117">setup</player_status>
</game>
 * @author Administrator
 *
 */
public class Game 
{

	private String test_name;
	private String test_id;
	private String class_id;
	private String test_type;
	private String test_status;
	private String test_format;
	private Hashtable <String,String> player_status;
	
	public Game()
	{
		player_status = new Hashtable<String,String>();
	}
	
	//-- test_name
	public void setTestName(String _test_name)
	{
		test_name = _test_name;
	}
	
	public String getTestName()
	{
		return test_name;
	}
	
	//--test_id
	public void setTestId(String _test_id)
	{
		test_id = _test_id;
	}
		
	public String getTestId()
	{
		return test_id;
	}
	
	//--class_id
		public void setClassId(String _class_id)
		{
			test_id = _class_id;
		}
			
		public String getClassId()
		{
			return class_id;
		}
		
	//-- test_type
	public void setTestType(String _test_type)
	{
		test_type = _test_type;
	}
		
	public String getTestType()
	{
		return test_type ;
	}
		
	//-- test_status
	public void setTestStatus(String _test_status)
	{
		test_status = _test_status;
	}
		
	public String getTestStatus()
	{
		return test_status ;
	}
		
	//--
	public void setTestFormat(String _test_format)
	{
		test_format = _test_format;
	}
		
	public String getTestFormat()
	{
		return test_format;
	}
		
	//-- Player Status
	/**
	 * Set full Hash.
	 * @param _player_status
	 */
	public void setPlayerStatus(Hashtable _player_status)
	{
		 player_status = _player_status;
	}
	
	/**
	 * Get full Hash.
	 * @return
	 */
	public Hashtable getPlayerStatus()
	{
		return  player_status;
	}
	
	/**
	 * Set the status of a player with their id as the key, and status as value.
	 * @param player_id
	 * @param status
	 */
	public void setPlayerStatus(String player_id, String status)
	{
		 player_status.put(player_id, status);
	}
	
	/**
	 * Return the status of a player using their id as the key.
	 * @param player_id
	 * @return
	 */
	public String getPlayerStatus(String player_id)
	{
		return  player_status.get(player_id);
	}
}
