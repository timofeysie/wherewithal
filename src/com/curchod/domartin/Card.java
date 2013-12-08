package com.curchod.domartin;

/**
 * Bean to hold card info like this:
 	 *     <card>
     *         <card_id>
     *         <card_status>
     *         <player_id>
     *         <player_name>
     *         <player_icon>
     *         <word_id>
     *         <word_type>
     *         <word_category>
     *         <text>
     *         <definition>
     *     </card> 
 * @author user
 *
 */
public class Card 
{

	private String card_id;
	private String card_status;
	private String player_id;
	private String player_name;
	private String player_icon;
	private String word_id;
	private String word_type;
	private String word_category;
	private String text;
	private String definition;
	private String index;
	
	public Card()
	{
		// no arguments here!
	}
	
	public Card(String _card_id, String _card_status, String _player_id, String _player_name, String  _player_icon,
			String _word_id, String _word_type, String _word_category, String _text, String _definition)
	{
		card_id = _card_id;
		card_status = _card_status;
		player_id = _player_id;
		player_name = _player_name;
		player_icon = _player_icon;
		word_id = _word_id;
		word_type = _word_type;
		word_category = _word_category;
		text = _text;
		definition = _definition;
	}
	
	//--- card id
	public void setCardId(String _card_id)
	{
		card_id = _card_id;
	}
	
	public String getCardId()
	{
		return card_id;
	}
	
	//--- card status
		public void setCardStatus(String _card_status)
		{
			card_status = _card_status;
		}
		
		public String getCardStatus()
		{
			return card_status;
		}
	
	//--- player id
	public void setPlayerId(String _player_id)
	{
		player_id = _player_id;
	}
	
	public String getPlayerId()
	{
		return player_id;
	}
	
	//--- player icon
	public void setPlayerIcon(String _player_icon)
	{
		player_icon = _player_icon;
	}
	
	public String getPlayerIcon()
	{
		return player_icon;
	}
	
	//----player name
	public void setPlayerName(String _player_name)
	{
		player_name = _player_name;
	}
	
	public String getPlayerName()
	{
		return player_name;
	}
	
	//--- word id
	public void setWordId(String _word_id)
	{
		word_id = _word_id;
	}
	
	public String getWordId()
	{
		return word_id;
	}
	
	
	//--- word_type
	public void setWordType(String _word_type)
	{
		word_type = _word_type;
	}
		
	public String getWordType()
	{
		return word_type;
	}	
	
	//--- word_category
	public void setWordCategory(String _word_category)
	{
		word_category = _word_category;
	}
		
	public String getWordCategory()
	{
		return word_category;
	}		
	
	//--- text
	public void setText(String _text)
	{
		text = _text;
	}
		
	public String getText()
	{
		return text;
	}
	
	//--- definition
	public void setDefinition(String _definition)
	{
		definition = _definition;
	}
		
	public String getDefinition()
	{
		return definition;
	}

	//--- index
	public void setIndex(String _index)
	{
		index = _index;
	}
		
	public String getIndex()
	{
		return index;
	}	
	
}
