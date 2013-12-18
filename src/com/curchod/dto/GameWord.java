package com.curchod.dto;

/**
 * This class holds information similar to the catechis Word object, but with a test index, word type,
 * and the player id and card_id.  The word type will tell which word an item comes from.
 * Reading indicates that the text is the primary word.
 * Writing indicates that the definition is.
 * @author user
 *
 */
public class GameWord 
{

	public class Word
	{
		
		private String text;
		private String definition;
		private int writing_level;
		private int reading_level;
		private long date_of_entry;
		private long id;
		private String category;
		private boolean retired;
		
		private int index;
		private String word_type;
		private long player_id;
		private long card_id;
		
		public Word()
		{
		}
		
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
		
		public void setWritingLevel(int _writing_level)
		{
			writing_level = _writing_level;
		}
		
		public int getWritingLevel()
		{
			return writing_level;
		}	
		
		public void setReadingLevel(int _reading_level)
		{
			reading_level = _reading_level;
		}
		
		public int getReadingLevel()
		{
			return reading_level;
		}
		
		public void setDateOfEntry(long time)
		{
			this.date_of_entry = time;
		}
		
		public long getDateOfEntry()
		{
			return this.date_of_entry;
		}
		
		public void setId(long _id)
		{
			this.id = _id;
		}
		
		public long getId()
		{
			return this.id;
		}
		
		
		public void setCategory(String _category)
		{
			category = _category;
		}
		
		public String getCategory()
		{
			return category;
		}
		
		public void setRetired(boolean _retired)
		{
			this.retired = _retired;
		}
		
		public boolean getRetired()
		{
			return this.retired;
		}
		
		// --index
		public void setIndex(int _index)
		{
			this.index = _index;
		}
		
		public int getIndex()
		{
			return this.index;
		}
		
		
		//--word_type;
		public void setWordType(String _word_type)
		{
			this.word_type = _word_type;
		}
		
		public String getWordType()
		{
			return this.word_type;
		}
		
		//--player_id
		public void setPlayerId(long _player_id)
		{
			this.player_id = _player_id;
		}
		
		public long getPlayerId()
		{
			return this.player_id;
		}
		
		//--card_id
		public void setCardId(long _card_id)
		{
			this.card_id = _card_id;
		}
		
		public long getCardId()
		{
			return this.card_id;
		}		

	}

	
}
