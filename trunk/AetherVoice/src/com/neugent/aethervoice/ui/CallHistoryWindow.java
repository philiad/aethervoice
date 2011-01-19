/**
 * @file CallHistoryWindow.java
 * @brief It contains the CallHistoryWindow activity class.
 * @author Wyndale Wong
 */

package com.neugent.aethervoice.ui;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.sipdroid.sipua.UserAgent;
import org.sipdroid.sipua.ui.Receiver;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.CallLog;
import android.provider.Contacts;
import android.provider.Settings;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.neugent.aethervoice.R;

/**
 * @class CallHistoryWindow
 * @brief An activity class that creates an interactive interface on the third
 *        tab window of AetherVoice for the user to manage call history.
 * @author Wyndale Wong
 */
@SuppressWarnings("deprecation")
public class CallHistoryWindow extends Activity {

	/** Displays the call history on the screen. */
	private ListView callHistory;

	/** Dynamically populates the list elements of callHistory. */
	private static CallHistoryAdapter callHistoryAdapter;

	/** The index indicating the mode of the call history list. **/
	private int mode = 0;

	/** The tab button that shows all call. **/
	private Button btnShowAll;

	/** The tab button that shows all outgoing calls. **/
	private Button btnShowDialed;

	/** The tab button that shows all received calls. **/
	private Button btnShowReceived;

	/** The tab button that shows all missed call. **/
	private Button btnShowMissed;

	/** The flag that indicates whether the callHistory must be updated. */
	public static boolean mustUpdateCallHistory = false;

	/** The defined index for show all logs mode. **/
	private static final int MODE_SHOW_ALL = 0;

	/** The defined index for show dialed logs mode. **/
	private static final int MODE_SHOW_DIALED = 1;

	/** The defined index for show received call logs mode. **/
	private static final int MODE_SHOW_RECEIVED = 2;

	/** The defined index for show missed call logs mode. **/
	private static final int MODE_SHOW_MISSED = 3;

	/**
	 * The id assignment of the menu item "Call Number", for long press menu
	 * options.
	 */
	private static final int CALL_MENU_ITEM = Menu.FIRST + 1;

	/**
	 * The id assignment of the menu item "Edit Number Before Call", for long
	 * press menu options.
	 */
	private static final int EDIT_MENU_ITEM = Menu.FIRST + 2;

	/**
	 * The id assignment of the menu item "Add to Contacts", for long press menu
	 * options.
	 */
	private static final int ADD_MENU_ITEM = Menu.FIRST + 3;

	/**
	 * The id assignment of the menu item "Remove from log", for long press menu
	 * options.
	 */
	private static final int REMOVE_MENU_ITEM = Menu.FIRST + 4;

	/**
	 * Initializes the CallHistoryWindow and its contents. Called when the
	 * CallHistoryWindow tab is pressed for the first time.
	 * 
	 * @see #initView()
	 */
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.call_history);
		initViews();
	}

	/**
	 * Initializes callHistory, its content and its listeners.
	 * 
	 * @see #loadCallHistory()
	 * @see #selectTab(int)
	 * @see Dialer#dial(String, Context)
	 * @see AetherVoice#setInput(String)
	 */
	private void initViews() {
		callHistory = (ListView) findViewById(R.id.list_history);

		btnShowAll = (Button) findViewById(R.id.btn_show_all);
		btnShowDialed = (Button) findViewById(R.id.btn_show_dialed);
		btnShowReceived = (Button) findViewById(R.id.btn_show_received);
		btnShowMissed = (Button) findViewById(R.id.btn_show_missed);
		
		System.out.println("+++++++++++++"+CallLog.Calls.CONTENT_URI);

		((Button) findViewById(R.id.btn_clear_all))
				.setOnClickListener(new OnClickListener() {
					public void onClick(final View v) {

						if (mode == CallHistoryWindow.MODE_SHOW_ALL)
							getContentResolver().delete(
									CallLog.Calls.CONTENT_URI, null, null);
						else {
							final Cursor callhistoryCursor = getContentResolver()
									.query(
											CallLog.Calls.CONTENT_URI,
											new String[] { BaseColumns._ID,
													CallLog.Calls.NUMBER,
													CallLog.Calls.TYPE,
													CallLog.Calls.DATE,
													CallLog.Calls.DURATION },
											getSelection(mode), null,
											CallLog.Calls.DEFAULT_SORT_ORDER);

							startManagingCursor(callhistoryCursor);

							if (callhistoryCursor != null
									&& callhistoryCursor.moveToFirst())
								do
									getContentResolver()
											.delete(
													CallLog.Calls.CONTENT_URI,
													BaseColumns._ID
															+ "="
															+ callhistoryCursor
																	.getString(callhistoryCursor
																			.getColumnIndex(BaseColumns._ID)),
													null);
								while (callhistoryCursor.moveToNext());
						}

						loadCallHistory(mode);
					}
				});

		btnShowAll.setOnClickListener(new OnClickListener() {
			public void onClick(final View v) {
				if (mode != CallHistoryWindow.MODE_SHOW_ALL)
					selectTab(CallHistoryWindow.MODE_SHOW_ALL);
			}
		});

		btnShowDialed.setOnClickListener(new OnClickListener() {
			public void onClick(final View v) {
				if (mode != CallHistoryWindow.MODE_SHOW_DIALED)
					selectTab(CallHistoryWindow.MODE_SHOW_DIALED);
			}
		});

		btnShowReceived.setOnClickListener(new OnClickListener() {
			public void onClick(final View v) {
				if (mode != CallHistoryWindow.MODE_SHOW_RECEIVED)
					selectTab(CallHistoryWindow.MODE_SHOW_RECEIVED);
			}
		});

		btnShowMissed.setOnClickListener(new OnClickListener() {
			public void onClick(final View v) {
				if (mode != CallHistoryWindow.MODE_SHOW_MISSED)
					selectTab(CallHistoryWindow.MODE_SHOW_MISSED);
			}
		});

		selectTab(mode);

		// Sets the listeners for the call history
		callHistory.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(final AdapterView<?> arg0, final View v,
					final int position, final long id) {
				/**
				 * A cursor that contains details of a single call history
				 * entry.
				 */
				final Cursor callhistoryEntryCursor = (Cursor) callHistoryAdapter
						.getItem(position);
				startManagingCursor(callhistoryEntryCursor);

				AetherVoice.setInput(callhistoryEntryCursor
						.getString(callhistoryEntryCursor
								.getColumnIndex(CallLog.Calls.NUMBER)));
				if (Dialer.isVoip)
					AetherVoice.dialer.dial(callhistoryEntryCursor.getString(callhistoryEntryCursor.getColumnIndex(CallLog.Calls.NUMBER)), getApplicationContext());
				else
					// Dialer.btnCall.performClick();
					AetherVoice.dialer.dialPSTN(callhistoryEntryCursor
							.getString(callhistoryEntryCursor
									.getColumnIndex(CallLog.Calls.NUMBER)));
			}
		});

		callHistory
				.setOnCreateContextMenuListener((new OnCreateContextMenuListener() {
					/**
					 * Creates the long press menu options for individual
					 * elements of contactList.
					 */
					public void onCreateContextMenu(final ContextMenu menu,
							final View v,
							final ContextMenu.ContextMenuInfo menuInfo) {
						menu.setHeaderTitle(R.string.header_call_history);
						menu.add(0, CallHistoryWindow.CALL_MENU_ITEM, 0,
								R.string.menu_call_number);
						menu.add(0, CallHistoryWindow.EDIT_MENU_ITEM, 0,
								R.string.menu_edit_number);
						menu.add(0, CallHistoryWindow.ADD_MENU_ITEM, 0,
								R.string.menu_add_number);
						menu.add(0, CallHistoryWindow.REMOVE_MENU_ITEM, 0,
								R.string.menu_remove_log);
					}
				}));
	}

	/**
	 * Raises and lowers the mustUpdateContactList flag using a boolean input.
	 * 
	 * @param mustUpdate
	 *            true raises the flag and false lowers it
	 */
	public static void setMustUpdateCallHistory() {
		CallHistoryWindow.mustUpdateCallHistory = true;
		if (callHistoryAdapter!=null)
			callHistoryAdapter.notifyDataSetChanged(); //TODO: clear data later --AJ added if statement to avoid null pointer
	}

	/**
	 * Reloads the cursor and the cursor adapter.
	 * 
	 * @param newMode
	 *            The mode of the call history list
	 * 
	 * @see #callHistoryAdapter
	 * @see #callHistory
	 */
	private void loadCallHistory(final int newMode) {

		mode = newMode;

		/**
		 * A cursor holding a whole table, where each row corresponds a single
		 * call history entry.
		 */
		final Cursor callhistoryCursor = getContentResolver().query(
				CallLog.Calls.CONTENT_URI,
				new String[] { BaseColumns._ID, CallLog.Calls.NUMBER,
						CallLog.Calls.TYPE, CallLog.Calls.DATE,
						CallLog.Calls.DURATION }, getSelection(mode), null,
				CallLog.Calls.DEFAULT_SORT_ORDER);

		startManagingCursor(callhistoryCursor);

		if (callhistoryCursor != null) {
			if (callhistoryCursor.getCount() > 0) {
				callHistoryAdapter = new CallHistoryAdapter(
						getApplicationContext(), callhistoryCursor);
				callHistory.setAdapter(callHistoryAdapter);
				callHistory.setVisibility(View.VISIBLE);
			} else
				callHistory.setVisibility(View.INVISIBLE);
		} else {
			callHistory.setVisibility(View.INVISIBLE);
			Toast.makeText(getApplicationContext(),
					R.string.toast_logs_not_found, Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Updates the button images accordingly.
	 * 
	 * @param newMode
	 *            The mode of the call history list
	 * 
	 * @see #loadCallHistory(int)
	 */
	private void selectTab(final int newMode) {

		btnShowAll
				.setBackgroundResource((newMode == CallHistoryWindow.MODE_SHOW_ALL) ? (R.drawable.btn_show_all_f2)
						: (R.drawable.btn_show_all_bg));

		btnShowDialed
				.setBackgroundResource((newMode == CallHistoryWindow.MODE_SHOW_DIALED) ? (R.drawable.btn_show_dialed_f2)
						: (R.drawable.btn_show_dialed_bg));

		btnShowReceived
				.setBackgroundResource((newMode == CallHistoryWindow.MODE_SHOW_RECEIVED) ? (R.drawable.btn_show_received_f2)
						: (R.drawable.btn_show_received_bg));

		btnShowMissed
				.setBackgroundResource((newMode == CallHistoryWindow.MODE_SHOW_MISSED) ? (R.drawable.btn_show_missed_f2)
						: (R.drawable.btn_show_missed_bg));

		loadCallHistory(newMode);
	}

	/**
	 * Gets the appropritate selection string for different modes.
	 * 
	 * @param callType
	 *            The callType of the call history list
	 * @return The selection string
	 */
	private String getSelection(final int callType) {

		if (callType == CallHistoryWindow.MODE_SHOW_DIALED)
			return CallLog.Calls.TYPE + "=" + CallLog.Calls.OUTGOING_TYPE;
		else if (callType == CallHistoryWindow.MODE_SHOW_RECEIVED)
			return CallLog.Calls.TYPE + "=" + CallLog.Calls.INCOMING_TYPE;
		else if (callType == CallHistoryWindow.MODE_SHOW_MISSED)
			return CallLog.Calls.TYPE + "=" + CallLog.Calls.MISSED_TYPE;
		else
			return null;
	}

	/**
	 * @class CallHistoryAdapter
	 * @brief A CursorAdapter class that dynamically populates the content of
	 *        callHistory with necessary details.
	 * @author Wyndale Wong
	 */
	private class CallHistoryAdapter extends CursorAdapter {

		/** The inflater object used to inflate views from resource. */
		private final LayoutInflater layoutInflater;

		/**
		 * Constructor method of CallHistoryAdapter, it merely assumes the same
		 * definition as its super class' constructor method.
		 */
		public CallHistoryAdapter(final Context context,
				final Cursor callhistoryCursor) {
			super(context, callhistoryCursor);
			layoutInflater = LayoutInflater.from(context);
		}

		/**
		 * Makes a new view to hold the data pointed to by callhistoryCursor.
		 */
		@Override
		public View newView(final Context context,
				final Cursor callhistoryEntryCursor, final ViewGroup parent) {
			final View newView = layoutInflater.inflate(
					R.layout.call_history_entry, null);
			return newView;
		}

		/**
		 * Binds an existing view to the data pointed to by callhistoryCursor.
		 * 
		 * @see CallHistoryWindow#getNameString(Context, Cursor)
		 * @see CallHistoryWindow#getDateString(long)
		 * @see CallHistoryWindow#getCallTypeResource(Context, int)
		 */
		@Override
		public void bindView(final View view, final Context context,
				final Cursor callhistoryEntryCursor) {

			((TextView) view.findViewById(R.id.call_date))
					.setText(CallHistoryWindow
							.getDateString(context, callhistoryEntryCursor
									.getLong(callhistoryEntryCursor
											.getColumnIndex(CallLog.Calls.DATE))));

			((ImageView) view.findViewById(R.id.call_type))
					.setBackgroundResource(getCallTypeResource(callhistoryEntryCursor
							.getInt(callhistoryEntryCursor
									.getColumnIndex(CallLog.Calls.TYPE))));

			final TextView nameText = (TextView) view
					.findViewById(R.id.call_name);

			final String name = CallHistoryWindow.getNameString(context,
					callhistoryEntryCursor.getString(callhistoryEntryCursor
							.getColumnIndex(CallLog.Calls.NUMBER)));

			if (name.equals("")) {
				String number = callhistoryEntryCursor
						.getString(callhistoryEntryCursor
								.getColumnIndex(CallLog.Calls.NUMBER));
				if (number.equals("-1"))
					number = "UNKNOWN";
				nameText.setText(number);
			} else
				nameText.setText(name);

			// if(callhistoryEntryCursor.getPosition()%2 == 0) {
			// view.setBackgroundResource(R.drawable.panel_entry_1);
			// } else {
			// view.setBackgroundResource(R.drawable.panel_entry_2);
			// }
		}
	}

	/**
	 * The name of the contact who owns the given number if any
	 * 
	 * @param context
	 *            Interface to the parent class' global information
	 * @param number
	 *            The number contact used to query for an existing contact
	 * @return The name of the contact who owns the number, and an empty string
	 *         if contact doesn't exists
	 */
	public static String getNameString(final Context context,
			final String number) {

		String name = "";

		/** A cursor holding single contact entry's name. */
		Cursor nameCursor = null;

		if (Integer.parseInt(Build.VERSION.SDK) >= 5) {
			final Cursor numberCursor = context.getContentResolver().query(
					ViewContactInfo.getNumberUri(ViewContactInfo.MODE_LOAD),
					null,
					ViewContactInfo.MIME_TYPE + " = '"
							+ ViewContactInfo.ITEM_TYPE_NUMBER + "' AND "
							+ ViewContactInfo.DATA1 + "= ?",
					new String[] { number }, null);
			if (numberCursor != null) {
				if (numberCursor.moveToFirst())
					nameCursor = context
							.getContentResolver()
							.query(
									ViewContactInfo.getContactsUri(),
									null,
									ViewContactInfo._ID + " = ? ",
									new String[] { numberCursor
											.getString(numberCursor
													.getColumnIndex(ViewContactInfo.CONTACT_ID)) },
									ViewContactInfo.getSortOrderString());
				numberCursor.close();
			}
		} else {
			/**
			 * A filtered Uri containing contact details of a contacts using a
			 * given contact number.
			 */
			final Uri contactUri = Uri.withAppendedPath(
					Contacts.Phones.CONTENT_FILTER_URL, Uri.encode(number));

			nameCursor = context.getContentResolver().query(contactUri,
					new String[] { ViewContactInfo.DISPLAY_NAME }, null, null,
					null);
		}

		if (nameCursor != null) {
			if (nameCursor.moveToFirst())
				name = nameCursor.getString(nameCursor
						.getColumnIndex(ViewContactInfo.DISPLAY_NAME));
			nameCursor.close();
		}

		return name;
	}

	/**
	 * Converts the calltype index to its corresponding call type string
	 * 
	 * @param context
	 *            Interface to the parent class' global information
	 * @param calltype
	 *            The type of call of a call history entry
	 * @return The String format of the calltype
	 */
	private int getCallTypeResource(final int callType) {
		if (callType == CallLog.Calls.INCOMING_TYPE)
			return R.drawable.calltype_received;
		else if (callType == CallLog.Calls.OUTGOING_TYPE)
			return R.drawable.calltype_dialed;
		else if (callType == CallLog.Calls.MISSED_TYPE)
			return R.drawable.calltype_missed;
		return 0;
	}
	
	/**
	 * Converts date in milliseconds to date string
	 * 
	 * @param timeInMilliS
	 *            date in milliseconds since January 01, 1970 00:00:00am
	 * @return the date in the format of MMM DD HH:MM:SS of type String
	 */
	private static String getDateString(final Context context, final Long timeInMilliS) {
		final String time = Settings.System.getString(context.getContentResolver(),Settings.System.TIME_12_24);
		DateFormat formatter;
		final Calendar calendar = Calendar.getInstance();
		
		calendar.setTimeInMillis(timeInMilliS);
		if (time.equals("24")){
			formatter = new SimpleDateFormat("MMM-dd-yyyy HH:mm:ss");
		}else {
			formatter = new SimpleDateFormat("MMM-dd-yyyy hh:mm:ss");
		}
		
		
		return formatter.format(calendar.getTime());	
	}

//	/**
//	 * Dials the number
//	 * 
//	 * @param target
//	 *            the number to be dialed
//	 */
//	private void dial(final String target) {
//		if (target.length() != 0)
//			if (SettingsWindow.isRegistered)
//				Receiver.engine(getApplicationContext()).call(target, true);
//			else
//				Toast.makeText(getApplicationContext(),
//						getString(R.string.toast_register), Toast.LENGTH_SHORT)
//						.show();
//			
//	}
	
	/**
	 * Defines the lines of code to be executed when a long press menu option is
	 * selected.
	 * <ul>
	 * <li>Call Number - Dials the number of the selected call log entry</li>
	 * <li>Edit Number Before Call - Exports the number of the selected call log
	 * entry to the DialerWindow for editing</li>
	 * <li>Add to Contacts - Exports number of the selected call log entry as a
	 * bundle extra to EditContactInfo activity before being launched</li>
	 * <li>Remove from log - Deletes the call history entry from the database</li>
	 * </ul>
	 * 
	 * @see Dialer#dial(String, Context)
	 * @see #loadCallHistory()
	 * @see AetherVoice#setInput(String)
	 * @see AetherVoice#showEditContactInfo(Context, String, String, String)
	 */
	@Override
	public boolean onContextItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case CALL_MENU_ITEM: {
			final AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item
					.getMenuInfo();

			/**
			 * A cursor holding a row of a table that corresponds to a single
			 * call history entry.
			 */
			final Cursor callhistoryEntryCursor = (Cursor) callHistoryAdapter
					.getItem(menuInfo.position);
			startManagingCursor(callhistoryEntryCursor);

			AetherVoice.setInput(callhistoryEntryCursor
					.getString(callhistoryEntryCursor
							.getColumnIndex(CallLog.Calls.NUMBER)));
			if (Dialer.isVoip)
				AetherVoice.dialer.dial(callhistoryEntryCursor.getString(callhistoryEntryCursor.getColumnIndex(CallLog.Calls.NUMBER)), getApplicationContext());
			else
				// Dialer.btnCall.performClick();
				AetherVoice.dialer.dialPSTN(callhistoryEntryCursor.getString(callhistoryEntryCursor.getColumnIndex(CallLog.Calls.NUMBER)));
			break;
		}
		case EDIT_MENU_ITEM: {
			final AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item
					.getMenuInfo();

			/**
			 * A cursor holding a row of a table that corresponds to a single
			 * call history entry.
			 */
			final Cursor callhistoryEntryCursor = (Cursor) callHistoryAdapter
					.getItem(menuInfo.position);
			startManagingCursor(callhistoryEntryCursor);

			AetherVoice.setInput(callhistoryEntryCursor
					.getString(callhistoryEntryCursor
							.getColumnIndex(CallLog.Calls.NUMBER)));
			break;
		}
		case ADD_MENU_ITEM: {
			final AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item
					.getMenuInfo();

			/**
			 * A cursor holding a row of a table that corresponds to a single
			 * call history entry.
			 */
			final Cursor callhistoryEntryCursor = (Cursor) callHistoryAdapter
					.getItem(menuInfo.position);
			startManagingCursor(callhistoryEntryCursor);
			
//			final String name = CallHistoryWindow.getNameString(getApplicationContext(),
//					callhistoryEntryCursor.getString(callhistoryEntryCursor
//							.getColumnIndex(CallLog.Calls.NUMBER)));
			final String number = callhistoryEntryCursor.getString(callhistoryEntryCursor
					.getColumnIndex(CallLog.Calls.NUMBER));
			final Cursor numberCursor = getContentResolver().query(
					ViewContactInfo.getNumberUri(ViewContactInfo.MODE_LOAD),
					null,
					ViewContactInfo.MIME_TYPE + " = '"
							+ ViewContactInfo.ITEM_TYPE_NUMBER + "' AND "
							+ ViewContactInfo.DATA1 + "= ?",
					new String[] { number }, null);
			
			String id = "";
			String name = "";
			String phoneNumber = "";
			int starred = 0;
			if (numberCursor != null) {
				startManagingCursor(numberCursor);
				if (numberCursor.moveToFirst()){
					id = numberCursor.getString(numberCursor
							.getColumnIndex(ViewContactInfo.CONTACT_ID));
					name = numberCursor.getString(numberCursor
							.getColumnIndex(ViewContactInfo.DISPLAY_NAME));
					
					starred = numberCursor.getInt(numberCursor
							.getColumnIndex(ViewContactInfo.STARRED));
				}else{
					phoneNumber = callhistoryEntryCursor.getString(callhistoryEntryCursor
							.getColumnIndex(CallLog.Calls.NUMBER));//added by AJ 10-8-10
				}
			}else{
				phoneNumber = callhistoryEntryCursor.getString(callhistoryEntryCursor
						.getColumnIndex(CallLog.Calls.NUMBER));
			}
			
			
			AetherVoice.showEditContactInfo(this, id, name, phoneNumber, starred);
			break;
		}
		case REMOVE_MENU_ITEM: {
			final AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item
					.getMenuInfo();

			/**
			 * A cursor holding a row of a table that corresponds to a single
			 * call history entry.
			 */
			final Cursor callhistoryEntryCursor = (Cursor) callHistoryAdapter
					.getItem(menuInfo.position);
			startManagingCursor(callhistoryEntryCursor);

			getContentResolver().delete(
					CallLog.Calls.CONTENT_URI,
					BaseColumns._ID
							+ "="
							+ callhistoryEntryCursor
									.getString(callhistoryEntryCursor
											.getColumnIndex(BaseColumns._ID)),
					null);
			loadCallHistory(mode);
			break;
		}
		}
		return super.onContextItemSelected(item);
	}

	/**
	 * Called when the tab is reselected, and upon doing so,
	 * refreshCallHistory() is then called if the mustUpdateCallHistory is
	 * raised.
	 * 
	 * @see #loadCallHistory()
	 * @see AetherVoice#setIsFinishing(boolean)
	 */
	@Override
	protected void onResume() {
		super.onResume();
		if (Receiver.call_state != UserAgent.UA_STATE_IDLE)
			Receiver.moveTop();

		if (CallHistoryWindow.mustUpdateCallHistory == true) {
			loadCallHistory(mode);
			CallHistoryWindow.mustUpdateCallHistory = false;
		}

		AetherVoice.setIsFinishing(false);
	}
	
	@Override
	protected void onDestroy() {
		callHistoryAdapter = null; //added by AJ
		super.onDestroy();
	}

	/**
	 * Called when a hardware button is pressed, and in this case the
	 * functionality of the call, end_call and back button is overridden to do
	 * nothing in this particular activity.
	 * 
	 * @see AetherVoice#setIsFinishing(boolean)
	 */
	@Override
	public boolean onKeyDown(final int keyCode, final KeyEvent event) {
		/*if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (AetherVoice.isFinishing)
				finish();
			else {
				Toast.makeText(getApplicationContext(), R.string.toast_finish,
						Toast.LENGTH_SHORT).show();
				AetherVoice.setIsFinishing(true);
			}
			return true;

		} else if (keyCode == KeyEvent.KEYCODE_CALL)
			return true;
		else if (keyCode == KeyEvent.KEYCODE_ENDCALL)
			return true;
		return super.onKeyDown(keyCode, event);*/
		return false;
	}
}
