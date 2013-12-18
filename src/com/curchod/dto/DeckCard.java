package com.curchod.dto;


/**
 * This holds info about a reusable deck of NFC tag cards which can be setup previously
 * and then reused with new words for multiple games after registering the deck for a new game.
 * An individual card is associated with a word from a game.  The same id will then correspond
 * to the id in the cards.xml file.
 * The name is a combination of the type and the index.
 * The status is blank after a cards is first created, ready when it's id is written to a tag,
 * and used when it has been associated with a word.
 * index 1, type reading, name R1.
 * @author user
 *
 */
public class DeckCard 
{

	private String card_id;
	private String card_name;
	private int index;
	private String type;
	private String status;
	
	public DeckCard()
	{
		
	}
	
	//-- id
	public void setCardId(String _card_id)
	{
		card_id = _card_id;
	}
		
	public String getCardId()
	{
		return card_id;
	}
	
	    //-- name
		public void setCardName(String _card_name)
		{
			card_name = _card_name;
		}
		
		public String getCardName()
		{
			return card_name;
		}
		
		//-- status
		public void setStatus(String _status)
		{
			this.status = _status;
		}
				
		public String getStatus()
		{
			return status;
		}
		
		//-- index
		public void setIndex(int _index)
		{
			index = _index;
		}
			
		public int getIndex()
		{
			return index;
		}
		
	
		public void setType(String _type)
		{
			this.type = _type;
		}
		
		public String getType()
		{
			return type;
		}
		
}
