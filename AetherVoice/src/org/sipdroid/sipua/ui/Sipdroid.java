/*
 * Copyright (C) 2009 The Sipdroid Open Source Project
 * Copyright (C) 2008 Hughes Systique Corporation, USA (http://www.hsc.com)
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

import java.util.ArrayList;
import java.util.List;

import org.sipdroid.sipua.UserAgent;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnDismissListener;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.CallLog.Calls;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.neugent.aethervoice.R;
import com.neugent.aethervoice.ui.SettingsWindow;

/////////////////////////////////////////////////////////////////////
// this the main activity of Sipdroid
// for modifying it additional terms according to section 7, GPL apply
// see ADDITIONAL_TERMS.txt
/////////////////////////////////////////////////////////////////////
public class Sipdroid extends Activity implements OnDismissListener {

	public static final boolean release = true;
	public static final boolean market = false;

	/* Following the menu item constants which will be used for menu creation */
	public static final int FIRST_MENU_ID = Menu.FIRST;
	public static final int CONFIGURE_MENU_ITEM = FIRST_MENU_ID + 1;
	public static final int ABOUT_MENU_ITEM = FIRST_MENU_ID + 2;
	public static final int EXIT_MENU_ITEM = FIRST_MENU_ID + 3;

	private static AlertDialog m_AlertDlg;
	AutoCompleteTextView sip_uri_box,sip_uri_box2;
	Button createButton;
	
	@Override
	public void onStart() {
		super.onStart();
		Receiver.engine(this).registerMore();
	    ContentResolver content = getContentResolver();
	    Cursor cursor = content.query(Calls.CONTENT_URI,
	            PROJECTION, Calls.NUMBER+" like ?", new String[] { "%@%" }, Calls.DEFAULT_SORT_ORDER);
	    CallsAdapter adapter = new CallsAdapter(this, cursor);
	    sip_uri_box.setAdapter(adapter);
	    sip_uri_box2.setAdapter(adapter);
	}
	
	public static class CallsAdapter extends CursorAdapter implements Filterable {
		List<String> list = null;
		
		public int getCount() {
			if (list == null)
				return 0;
			return list.size();
		}
		
		Cursor uniq(Cursor cursor) {
			list = new ArrayList<String>();
			for (int i = 0; i < cursor.getCount(); i++) {
				cursor.moveToPosition(i);
 		        String phoneNumber = cursor.getString(1);
		        String cachedName = cursor.getString(2);
		        if (cachedName != null && cachedName.trim().length() > 0)
		        	phoneNumber += " <" + cachedName + ">";
		        if (list.contains(phoneNumber)) continue;
				list.add(phoneNumber);
			}
			cursor.moveToFirst();
			return cursor;
		}
		
	    public CallsAdapter(Context context, Cursor c) {
	        super(context, c);
	        mContent = context.getContentResolver();
	    }
	
	    public View newView(Context context, Cursor cursor, ViewGroup parent) {
	        final LayoutInflater inflater = LayoutInflater.from(context);
	        final TextView view = (TextView) inflater.inflate(
	                android.R.layout.simple_dropdown_item_1line, parent, false);
	    	String phoneNumber = list.get(cursor.getPosition());	        
	        view.setText(phoneNumber);
	        return view;
	    }
	
	    @Override
	    public void bindView(View view, Context context, Cursor cursor) {
	    	String phoneNumber = list.get(cursor.getPosition());	        
	        ((TextView) view).setText(phoneNumber);
	    }
	
	    @Override
	    public String convertToString(Cursor cursor) {
	    	String phoneNumber = list.get(cursor.getPosition());
	    	if (phoneNumber.contains(" <"))
	    		phoneNumber = phoneNumber.substring(0,phoneNumber.indexOf(" <"));
	        return phoneNumber;
	    }
	
	    @Override
	    public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
	        if (getFilterQueryProvider() != null) {
	            return uniq(getFilterQueryProvider().runQuery(constraint));
	        }
	
	        StringBuilder buffer;
	        String[] args;
	        buffer = new StringBuilder();
	        buffer.append(Calls.NUMBER);
	        buffer.append(" LIKE ? OR ");
	        buffer.append(Calls.CACHED_NAME);
	        buffer.append(" LIKE ?");
	        String arg = "%" + (constraint != null && constraint.length() > 0?
       				constraint.toString() : "@") + "%";
	        args = new String[] { arg, arg};
	
	        return uniq(mContent.query(Calls.CONTENT_URI, PROJECTION,
	                buffer.toString(), args,
	                Calls.NUMBER + " asc"));
	    }
	
	    private ContentResolver mContent;        
	}
	
	private static final String[] PROJECTION = new String[] {
        Calls._ID,
        Calls.NUMBER,
        Calls.CACHED_NAME
	};

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.sipdroid);
		sip_uri_box = (AutoCompleteTextView) findViewById(R.id.txt_callee);
		sip_uri_box2 = (AutoCompleteTextView) findViewById(R.id.txt_callee2);
		sip_uri_box.setOnKeyListener(new OnKeyListener() {
		    public boolean onKey(View v, int keyCode, KeyEvent event) {
		        if (event.getAction() == KeyEvent.ACTION_DOWN &&
		        		keyCode == KeyEvent.KEYCODE_ENTER) {
		          call_menu(sip_uri_box);
		          return true;
		        }
		        return false;
		    }
		});
		sip_uri_box.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				call_menu(sip_uri_box);
			}
		});
		sip_uri_box2.setOnKeyListener(new OnKeyListener() {
		    public boolean onKey(View v, int keyCode, KeyEvent event) {
		        if (event.getAction() == KeyEvent.ACTION_DOWN &&
		        		keyCode == KeyEvent.KEYCODE_ENTER) {
		          call_menu(sip_uri_box2);
		          return true;
		        }
		        return false;
		    }
		});
		sip_uri_box2.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				call_menu(sip_uri_box2);
			}
		});
		on(this,true);

		Button contactsButton = (Button) findViewById(R.id.contacts_button);
		contactsButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				Intent myIntent = new Intent(Intent.ACTION_DIAL);
				startActivity(myIntent);
			}
		});

		final Context mContext = this;
		final OnDismissListener listener = this;
		
		createButton = (Button) findViewById(R.id.create_button);
		createButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				CreateAccount createDialog = new CreateAccount(mContext);
				createDialog.setOnDismissListener(listener);
		        createDialog.show();
			}
		});
		
		//if (PreferenceManager.getDefaultSharedPreferences(this).getString(Settings.PREF_PREF, Settings.DEFAULT_PREF).equals(Settings.VAL_PREF_PSTN) &&
		if (SettingsWindow.callType.equals(Settings.VAL_PREF_PSTN) &&
				!PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Settings.PREF_NODEFAULT, Settings.DEFAULT_NODEFAULT))
			new AlertDialog.Builder(this)
				.setMessage(R.string.dialog_default)
	            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int whichButton) {
	                		Editor edit = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
	                		edit.putString(Settings.PREF_PREF, Settings.VAL_PREF_SIP);
	                		edit.commit();	
	                    }
	                })
	            .setNeutralButton(R.string.no, new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int whichButton) {
	
	                    }
	                })
	            .setNegativeButton(R.string.dontask, new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int whichButton) {
	                		Editor edit = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
	                		edit.putBoolean(Settings.PREF_NODEFAULT, true);
	                		edit.commit();
	                    }
	                })
				.show();
	}

	public static boolean on(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Settings.PREF_ON, Settings.DEFAULT_ON);
	}

	public static void on(Context context,boolean on) {
		Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
		edit.putBoolean(Settings.PREF_ON, on);
		edit.commit();
        if (on) Receiver.engine(context).isRegistered();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (Receiver.call_state != UserAgent.UA_STATE_IDLE) Receiver.moveTop();
		createButton.setVisibility(Integer.parseInt(Build.VERSION.SDK) >= 5 && CreateAccount.isPossible(this)?
				View.VISIBLE:View.GONE); 
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);

		MenuItem m = menu.add(0, ABOUT_MENU_ITEM, 0, R.string.menu_about);
		m.setIcon(android.R.drawable.ic_menu_info_details);
		m = menu.add(0, EXIT_MENU_ITEM, 0, R.string.menu_exit);
		m.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
		m = menu.add(0, CONFIGURE_MENU_ITEM, 0, R.string.menu_settings);
		m.setIcon(android.R.drawable.ic_menu_preferences);
						
		return result;
	}

	void call_menu(AutoCompleteTextView view)
	{
		String target = view.getText().toString();
		if (m_AlertDlg != null) 
		{
			m_AlertDlg.cancel();
		}
		if (target.length() == 0)
			m_AlertDlg = new AlertDialog.Builder(this)
				.setMessage(R.string.empty)
				.setTitle(R.string.app_name)
				.setIcon(R.drawable.icon22)
				.setCancelable(true)
				.show();
		else if (!Receiver.engine(this).call(target,true))
			m_AlertDlg = new AlertDialog.Builder(this)
				.setMessage(R.string.notfast)
				.setTitle(R.string.app_name)
				.setIcon(R.drawable.icon22)
				.setCancelable(true)
				.show();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean result = super.onOptionsItemSelected(item);
		Intent intent = null;

		switch (item.getItemId()) {
		case ABOUT_MENU_ITEM:
			if (m_AlertDlg != null) 
			{
				m_AlertDlg.cancel();
			}
			m_AlertDlg = new AlertDialog.Builder(this)
			.setMessage(getString(R.string.about).replace("\\n","\n").replace("${VERSION}", getVersion(this)))
			.setTitle(getString(R.string.menu_about))
			.setIcon(R.drawable.icon22)
			.setCancelable(true)
			.show();
			break;
			
		case EXIT_MENU_ITEM: 
			on(this,false);
			Receiver.pos(true);
			Receiver.engine(this).halt();
			Receiver.mSipdroidEngine = null;
			Receiver.reRegister(0);
			stopService(new Intent(this,RegisterService.class));
			finish();
			break;
			
		case CONFIGURE_MENU_ITEM: {
			try {
				intent = new Intent(this, org.sipdroid.sipua.ui.Settings.class);
				startActivity(intent);
			} catch (ActivityNotFoundException e) {
			}
		}
			break;
		}

		return result;
	}
	
	public static String getVersion() {
		return getVersion(Receiver.mContext);
	}
	
	public static String getVersion(Context context) {
		final String unknown = "Unknown";
		
		if (context == null) {
			return unknown;
		}
		
		try {
			return context.getPackageManager()
				   .getPackageInfo(context.getPackageName(), 0)
				   .versionName;
		} catch(NameNotFoundException ex) {}
		
		return unknown;		
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		onResume();
	}
}
