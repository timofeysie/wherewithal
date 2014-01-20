package com.curchod.json;

import org.apache.http.message.BasicNameValuePair;

/**
 * Create a definition JSON node based on the following structure:
 * "definition":
            "name": "ko-KR": "고양이"
            "description":  Reading ,Writing, Speaking or Listening
            "type": "http://ko.wiktionary.org/wiki/고양이"           
 * @author user
 *
 */
public class VocabularyDefinition 
{
	
	private BasicNameValuePair name;
	private String description;
	private String type;
	 
	public void setName(String locale, String word)
	{
		this.name = new BasicNameValuePair(locale, word);
	}
	
	public BasicNameValuePair getName()
	{
		return this.name;
	}
	
	public String getNameLocale()
	{
		return this.name.getName();
	}
	
	public String getNameValue()
	{
		return this.name.getValue();
	}

	public void setDescription(String _description)
	{
		this.description = _description;
	}
	
	public String getDescription()
	{
		return this.description;
	}
	
	public void setType(String _type)
	{
		this.type = _type;
	}
	
	public String getType()
	{
		return this.type;
	}

}
