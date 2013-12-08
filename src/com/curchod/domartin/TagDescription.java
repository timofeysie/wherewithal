package com.curchod.domartin;

import android.nfc.NdefMessage;

public class TagDescription 
{

    public String title;

    public NdefMessage[] msgs;

    public TagDescription(String title, byte[] bytes) 
    {
        this.title = title;
        try 
        {
            msgs = new NdefMessage[] {new NdefMessage(bytes)};
        } catch (final Exception e) 
        {
            System.err.println("Failed to create tag description: "+title);
            e.printStackTrace();
        }
    }

    @Override
    public String toString() 
    {
        return title;
    }
}
