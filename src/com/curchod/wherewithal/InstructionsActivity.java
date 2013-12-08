package com.curchod.wherewithal;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;

public class InstructionsActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instructions);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_instructions, menu);
        return true;
    }
}
