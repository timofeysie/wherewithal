package com.curchod.json;

import java.net.URI;

/**
 * Create a JSON object based on the TinCan API with the following structure:
 * 
 * "object":
        "id": "http://en.wiktionary.org/wiki/cat",
        "objectType": "Activity",
        "definition":
            "name": "ko-KR": "고양이"
            "description": “Reading”
            "type": "http://ko.wiktionary.org/wiki/고양이"
 * @author user
 *
 */
public class VocabularyLearningObject 
{
	private URI id;
	private String objectType;
	private VocabularyDefinition definition;
	
	public void setId(URI _id)
	{
		this.id = _id;
	}
	
	public URI getId()
	{
		return this.id;
	}
	
	public void setObjectType(String _objectType)
	{
		this.objectType = _objectType;
	}
	
	public String getObjectType()
	{
		return this.objectType;
	}
	
	public void setDefintion(VocabularyDefinition _definition)
	{
		this.definition = _definition;
	}
	
	public VocabularyDefinition getDefinition()
	{
		return this.definition;
	}
	
	public String toJSON()
	{
		StringBuffer buffer = new StringBuffer();
		buffer.append("\"object\":");
        buffer.append("\"id\": \""+id.toString()+"\",");
        buffer.append("\"objectType\": \""+objectType+"\",");
        buffer.append("\"definition\":");
        buffer.append("\"name\": \""
        		+definition.getNameLocale()+"\": \""
        		+definition.getNameValue()+"\",");
        buffer.append("\"description\": \""+definition.getDescription()+"\",");
        buffer.append("\"type\": \""+definition.getType()+"");
        return buffer.toString();
	}
	
}
