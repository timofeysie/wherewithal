package com.curchod.dto;

public class PlayerInfo 
{

	private String name;
	private String id;
	private String icon;
	private int score;
	
	public PlayerInfo()
	{
	}
	
	public PlayerInfo(String _name, int _score, String _id, String _icon)
	{
		name = _name;
		score = _score;
		id = _id;
		icon = _icon;
	}
	
	public void setName(String _name)
	{
		name = _name;
	}
	
	public String getName()
	{
		return name;
	}
	
	public void setId(String _id)
	{
		id = _id;
	}
	
	public String getId()
	{
		return id;
	}
	
	public void setIcon(String _icon)
	{
		icon = _icon;
	}
	
	public String getIcon()
	{
		return icon;
	}
	
	
	public void setScore(int _score)
	{
		score = _score;
	}
	
	public int getScore()
	{
		return score;
	}
	
	
	
	
}
