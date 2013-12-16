package com.curchod.dto;

/**
 * This object is based on the AllWordsTest object from Catechis.
 * The differences are:
 * id is called word_id.
 * We add the grand_test_index, or total number of tests for the user.
 * @author user
 *
 */
public class SingleWord
{
	// data kept on xml file
	private String text;
	private String definition;
	private String category;
	private String test_type;
	private String level;
	private int daily_test_index;
	private int grand_test_index;
	private String word_id;
	
	// users answers
	private String answer;
	
	public void setText(String _text)
	{
		text = _text;
	}
	
	public String getText()
	{
		return text;
	}	
	
	public void setDefinition(String _definition)
	{
		definition = _definition;
	}
	
	public String getDefinition()
	{
		return definition;
	}
	
	public void setCategory(String _category)
	{
		category = _category;
	}
	
	public String getCategory()
	{
		return category;
	}
	
	public void setTestType(String _test_type)
	{
		test_type = _test_type;
	}
	
	public String getTestType()
	{
		return test_type;
	}
	
	public String getLevel()
	{
		return level;
	}
	
	public void setLevel(String _level)
	{
		level = _level;
	}
	
	public void setAnswer(String _answer)
	{
		answer = _answer;
	}
	
	public String getAnswer()
	{
		return answer;
	}
	
	public void setGrandTestIndex(int _grand_test_index)
	{
		grand_test_index = _grand_test_index;
	}
	
	public int getGrandTestIndex()
	{
		return grand_test_index;
	}
	
	public void setDailyTestIndex(int _daily_test_index)
	{
		daily_test_index = _daily_test_index;
	}
	
	public int getDailyTestIndex()
	{
		return daily_test_index;
	}
	
	public void setWordId(String _word_id)
	{
		this.word_id = _word_id;
	}
	
	public String getWordId()
	{
		return this.word_id;
	}

}

