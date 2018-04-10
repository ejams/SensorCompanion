package com.jamierajewski.sensorcompanion;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public class HelpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        TextView helpTextview = findViewById(R.id.helpTextview);
        helpTextview.setMovementMethod(LinkMovementMethod.getInstance());
        helpTextview.setText(Html.fromHtml(getString(R.string.help_text)));
    }
}
