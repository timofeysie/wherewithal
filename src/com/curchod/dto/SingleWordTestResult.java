package com.curchod.dto;

public class SingleWordTestResult 
{

	private int number_of_waiting_reading_tests;
	private int number_of_waiting_writing_tests; 
	private String color;
	
	public int getNumberOfWaitingReadingTests()
	{
		return number_of_waiting_reading_tests;
	}
	
	public void setNumberOfWaitingReadingTests(int _number_of_waiting_reading_tests)
	{
		number_of_waiting_reading_tests = _number_of_waiting_reading_tests;
	}
	
	public int getNumberOfWaitingWritingTests()
	{
		return number_of_waiting_writing_tests;
	}
	
	public void setNumberOfWaitingWritingTests(int _number_of_waiting_writing_tests)
	{
		number_of_waiting_writing_tests = _number_of_waiting_writing_tests;
	}
	
	public String getColor()
	{
		return color;
	}
	
	public void setColor(String _color)
	{
		color = _color;		
	}
	
}
