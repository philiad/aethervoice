package com.neugent.aethervoice.ui;


import android.app.Activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.neugent.aethervoice.R;


public class PldtDirectory extends Activity {
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pldt_directory);
        TextView tv = (TextView)findViewById(R.id.show_message);
        tv.setText("Coming Soon!");
        ((LinearLayout)findViewById(R.id.directory_search_frame)).setVisibility(View.GONE);
        ((Button)findViewById(R.id.directory_btn_clear)).setVisibility(View.GONE);
	}	

	
	
	
}
