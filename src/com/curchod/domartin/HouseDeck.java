package com.curchod.domartin;

import java.util.Hashtable;

/**
 * Modelling this xml info:
 *     <house_deck>
 *         <deck_id/>
 *         <player_id/>
 *         <game_id/>
 *         <name>¡±Player A¡±</name>
 *         <size>24</size>
 * 			<card>
 * 				<id/>
 * 				<name/>
 * 				<index/>
 * 				<type/>
 *			</card>
 * 			...
 *    </house_deck>
 * @author user
 *
 */
public class HouseDeck 
{

	private String deck_id;
	private String player_id;
	private String game_id;
	private String deck_name;
	private Hashtable <String,DeckCard> cards;
	private int number_of_reading_cards;
	private int number_of_writing_cards;
	
	
	
	public HouseDeck()
	{
		cards = new Hashtable <String,DeckCard> ();
		number_of_reading_cards = 0;
		number_of_writing_cards = 0;
	}
	
	    //-- name
		public void setDeckName(String _deck_name)
		{
			deck_name = _deck_name;
		}
		
		public String getDeckName()
		{
			return deck_name;
		}
		
		//-- id
		public void setDeckId(String _deck_id)
		{
			deck_id = _deck_id;
		}
			
		public String getDeckId()
		{
			return deck_id;
		}
		
		public void setPlayerId(String _player_id)
		{
			 player_id = _player_id;
		}
		
		/**
		 */
		public String getPlayerId()
		{
			return  player_id;
		}
		
		
		// game id
		public void setGameId(String _game_id)
		{
			game_id = _game_id;
		}
		
		/**
		 */
		public String getGameId()
		{
			return  game_id;
		}
		
		
		
		
		/**
		 */
		public Hashtable <String,DeckCard> getCards()
		{
			return  cards;
		}
		
		/**
		 * id-DeckCard pairs
		 */
		public void setCards(String index, DeckCard _card)
		{
			 cards.put(index, _card);
			 String type = _card.getType();
			 if (type.equals(Constants.READING))
			 {
				 number_of_reading_cards++;
			 } else if (type.equals(Constants.WRITING))
			 {
				 number_of_writing_cards++;
			 }
		}
		
		/**
		 * id-DeckCard pairs
		 */
		public void setCards(Hashtable <String,DeckCard> _cards)
		{
			 cards = _cards;
		}
		
		/**
		 * Return the card_id of a card using the index key.
		 * @param player_id
		 * @return
		 */
		public DeckCard getCard(String index)
		{
			return  cards.get(index);
		}
		
		public int getNumberOfReadingCards()
		{
			return number_of_reading_cards;
		}
		
		public int getNumberOfWritingCards()
		{
			return number_of_writing_cards;
		}
	
}
