/*
 * Copyright (C) 2010 The Sipdroid Open Source Project
 * 
 * This file is part of Sipdroid (http://www.sipdroid.org)
 * 
 * Sipdroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.sipdroid.sipua.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

import org.sipdroid.sipua.SipdroidEngine;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.neugent.aethervoice.R;
import com.neugent.aethervoice.ui.SettingsWindow;

public class CreateAccount extends Dialog {

	Context mContext;
	
	public CreateAccount(Context context) {
		super(context);
		mContext = context;
	}
	
	static String email;
	
	public static Boolean isPossible(Context context) {
		Boolean found = false;
	   	for (int i = 0; i < SipdroidEngine.LINES; i++) {
	   		String j = (i!=0?""+i:"");
	   		//String username = PreferenceManager.getDefaultSharedPreferences(context).getString(Settings.PREF_USERNAME+j, Settings.DEFAULT_USERNAME),
	   		String username = SettingsWindow.username[1],
	   			//server = PreferenceManager.getDefaultSharedPreferences(context).getString(Settings.PREF_SERVER+j, Settings.DEFAULT_SERVER);
	   			server = SettingsWindow.server[i];
	   		if (username.equals("") || server.equals(""))
	   			continue;
	   		if (server.equals(Settings.DEFAULT_SERVER))
	   			found = true;
	   	}
	   	if (found) return false;
		Intent intent = new Intent(Intent.ACTION_SENDTO);
		intent.setPackage("com.google.android.apps.googlevoice");
		intent.setData(Uri.fromParts("smsto", "", null));
		List<ResolveInfo> a = context.getPackageManager().queryIntentActivities(intent,PackageManager.GET_INTENT_FILTERS);
		if (a == null || a.size() == 0)
			return false;
        Account[] accounts = AccountManager.get(context).getAccountsByType("com.google");
        for (Account account : accounts)
          if (account.name.contains("@gmail.com") || account.name.contains("@googlemail.com")) {
        	  email = account.name;
        	  return true;
          }
        return false;
	}
	
	String line;
	
    Handler mHandler = new Handler() {
    	public void handleMessage(Message msg) {
			Toast.makeText(mContext, line, Toast.LENGTH_LONG).show();
			buttonCancel.setEnabled(true);
			buttonOK.setEnabled(true);
			setCancelable(true);
    	}
    };

	void CreateAccountNow() {
		buttonCancel.setEnabled(false);
		buttonOK.setEnabled(false);
		setCancelable(false);
		Toast.makeText(mContext, "Please stand by while your account is being created", Toast.LENGTH_LONG).show();
        (new Thread() {
			public void run() {
				try {
			        URL url = new URL("https://www1.pbxes.com/config.php?m=register&a=update&f=action&username="+Uri.encode(etName.getText().toString())+"&password="
			        		+Uri.encode(etPass.getText().toString())+"&password_confirm="+Uri.encode(etConfirm.getText().toString())+"&language=en&email="+Uri.encode(email)+"&land="+Uri.encode(Time.getCurrentTimezone())+
			        		"&sipdroid="+Uri.encode(etSipdroid.getText().toString()));
			        BufferedReader in;
					in = new BufferedReader(new InputStreamReader(url.openStream()));
					line = in.readLine();
					if (line == null) {
						in = new BufferedReader(new InputStreamReader(url.openStream()));
						line = in.readLine();
					}
					if (line != null) {
						if (line.equals("OK")) {
							Editor edit = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
							edit.putString(Settings.PREF_SERVER, Settings.DEFAULT_SERVER);
							edit.putString(Settings.PREF_USERNAME, etName.getText()+"-200");
							edit.putString(Settings.PREF_DOMAIN, Settings.DEFAULT_DOMAIN);
							edit.putString(Settings.PREF_FROMUSER, Settings.DEFAULT_FROMUSER);
							edit.putString(Settings.PREF_PORT, Settings.DEFAULT_PORT);
							edit.putString(Settings.PREF_PROTOCOL, "tcp");
							edit.putString(Settings.PREF_PASSWORD, etSipdroid.getText().toString());
							edit.commit();
				        	Receiver.engine(mContext).updateDNS();
				       		Receiver.engine(mContext).halt();
				   			Receiver.engine(mContext).StartEngine();
							dismiss();
						}
					}
			        in.close();
				} catch (IOException e) {
					if (!Sipdroid.release) e.printStackTrace();
				}
				mHandler.sendEmptyMessage(0);
			}
		}).start();   
	}

	EditText etName,etPass,etConfirm,etSipdroid;
	TextView tAdd;
	Button buttonCancel,buttonOK;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_dialog);
        setTitle("Create Free Account");
        buttonOK = (Button) findViewById(R.id.Button01);
		buttonOK.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				CreateAccountNow();
			}
		});
        buttonCancel = (Button) findViewById(R.id.Button02);
		buttonCancel.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				dismiss();
			}
		});
        etName = (EditText) findViewById(R.id.EditText01);
        etName.setText(email.substring(0,email.indexOf("@")));
        tAdd = (TextView) findViewById(R.id.Text01);
        tAdd.setText(email);
        etPass = (EditText) findViewById(R.id.EditText02);
        etConfirm = (EditText) findViewById(R.id.EditText03);
        etSipdroid = (EditText) findViewById(R.id.EditText04);
    }

}
