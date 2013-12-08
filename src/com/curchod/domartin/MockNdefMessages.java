package com.curchod.domartin;

import java.math.BigInteger;

/**
 * This class provides a list of fake NFC Ndef format Tags.
 * It comes from the API Level 15 Android NFCDemo source code.
 */
public class MockNdefMessages 
{
	
	public static byte[] getPromulgation()
	{
		String plain_text = "en2467473067840752146@promulgation";
	    String hex = toHex(plain_text);
	    byte[] bytes = hexStringToByteArray(hex); 
	    return bytes;
	}
	
	/**
	 * by Dave L. with 14.6k (that's 14600 reputation points.  Go Dave!
	 * @param s
	 * @return
	 */
	public static byte[] hexStringToByteArray(String s) 
	{
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) 
	    {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}
	
	/**
	 * by Kaleb Pederson 15700 reputation points
	 * @param arg
	 * @return
	 */
	public static String toHex(String arg) 
	{
	    return String.format("%040x", new BigInteger(arg.getBytes(/*YOUR_CHARSET?*/)));
	}

    /**
     * A Smart Poster containing a URL and no text.
     */
    public static final byte[] SMART_POSTER_URL_NO_TEXT =
        new byte[] {(byte) 0xd1, (byte) 0x02, (byte) 0x0f, (byte) 0x53, (byte) 0x70, (byte) 0xd1,
            (byte) 0x01, (byte) 0x0b, (byte) 0x55, (byte) 0x01, (byte) 0x67, (byte) 0x6f,
            (byte) 0x6f, (byte) 0x67, (byte) 0x6c, (byte) 0x65, (byte) 0x2e, (byte) 0x63,
            (byte) 0x6f, (byte) 0x6d};

    /**
     * A plain text tag in english.
     */
    public static final byte[] ENGLISH_PLAIN_TEXT =
        new byte[] {(byte) 0xd1, (byte) 0x01, (byte) 0x1c, (byte) 0x54, (byte) 0x02, (byte) 0x65,
            (byte) 0x6e, (byte) 0x53, (byte) 0x6f, (byte) 0x6d, (byte) 0x65, (byte) 0x20,
            (byte) 0x72, (byte) 0x61, (byte) 0x6e, (byte) 0x64, (byte) 0x6f, (byte) 0x6d,
            (byte) 0x20, (byte) 0x65, (byte) 0x6e, (byte) 0x67, (byte) 0x6c, (byte) 0x69,
            (byte) 0x73, (byte) 0x68, (byte) 0x20, (byte) 0x74, (byte) 0x65, (byte) 0x78,
            (byte) 0x74, (byte) 0x2e};

    /**
     * Smart Poster containing a URL and Text.
     */
    public static final byte[] SMART_POSTER_URL_AND_TEXT =
        new byte[] {(byte) 0xd1, (byte) 0x02, (byte) 0x1c, (byte) 0x53, (byte) 0x70, (byte) 0x91,
            (byte) 0x01, (byte) 0x09, (byte) 0x54, (byte) 0x02, (byte) 0x65, (byte) 0x6e,
            (byte) 0x47, (byte) 0x6f, (byte) 0x6f, (byte) 0x67, (byte) 0x6c, (byte) 0x65,
            (byte) 0x51, (byte) 0x01, (byte) 0x0b, (byte) 0x55, (byte) 0x01, (byte) 0x67,
            (byte) 0x6f, (byte) 0x6f, (byte) 0x67, (byte) 0x6c, (byte) 0x65, (byte) 0x2e,
            (byte) 0x63, (byte) 0x6f, (byte) 0x6d};

    /**
     * All the mock Ndef tags.
     */
    public static final byte[][] ALL_MOCK_MESSAGES =
        new byte[][] {SMART_POSTER_URL_NO_TEXT, ENGLISH_PLAIN_TEXT, SMART_POSTER_URL_AND_TEXT};
}
