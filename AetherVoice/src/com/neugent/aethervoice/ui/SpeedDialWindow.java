package com.neugent.aethervoice.ui;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.sipdroid.sipua.UserAgent;
import org.sipdroid.sipua.ui.Receiver;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.CallLog;
import android.provider.Contacts;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.neugent.aethervoice.R;

/**
 * @class SpeedDialWindow
 * @brief An activity class that creates an interactive interface on the first
 *        tab window of AetherVoice for the user to make speed dials.
 * @author Wyndale Wong
 */
@SuppressWarnings("deprecation")
public class SpeedDialWindow extends Activity {
	
	/** The application context. **/
	private Context mContext;

	/** The cursor the holds the elements for the speedDialList. **/
	private Cursor speedDialListCursor;

	/** Displays the speed dial list on the screen. **/
	private GridView speedDialList;

	/** The adapter that dynamically populates the speedDialList. **/
	private SpeedDialListAdapter speedDialListAdapter;

	/** The variable that stores the chosen number to be dialed. */
	private String chosenNumber = "";

	/** The flag that indicates whether the the speedDialList must be updated. */
	public static boolean mustUpdateSpeedDialList = false;

	/**
	 * The id assignment of the menu item "Call contact", for long press menu
	 * options.
	 */
	private static final int CALL_MENU_ITEM = Menu.FIRST + 1;

	/**
	 * The id assignment of the menu item "Remove from speed dial", for long
	 * press menu options.
	 */
	private static final int UNSTAR_MENU_ITEM = Menu.FIRST + 2;

	/**
	 * Initializes the SpeedDialWindow and its contents. Called when the
	 * SpeedDialWindow tab is pressed for the first time or when opened as the
	 * default active tab window at the start of the application.
	 * 
	 * @see #initView()
	 */
	@Override
	public void onCreate(final Bundle icicle) {
		super.onCreate(icicle);
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		mContext = this;
		setContentView(R.layout.speed_dial);
		initView();
	}

	/**
	 * Initializes speedDialList, its content and its listeners.
	 * 
	 * @see #loadSpeedDialList()
	 * @see ViewContactInfo#getContactsUri()
	 * @see ViewContactInfo#getSortOrderString()
	 * @see ViewContactInfo#getRawUri()
	 */
	private void initView() {
		speedDialList = (GridView) findViewById(R.id.grid_speed_dial);

		((Button) findViewById(R.id.btn_unstar_all))
				.setOnClickListener(new OnClickListener() {

					public void onClick(final View v) {
						if (speedDialList.getChildCount() > 0)
							getConfirmDialog().show();
					}

				});

		loadSpeedDialList();

		speedDialList.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(final AdapterView<?> arg0, final View v,
					final int position, final long id) {
				callNumbers(position);
			}
		});

		speedDialList
				.setOnCreateContextMenuListener((new OnCreateContextMenuListener() {
					/**
					 * Creates the long press menu options for individual
					 * elements of contactList.
					 */
					public void onCreateContextMenu(final ContextMenu menu,
							final View v,
							final ContextMenu.ContextMenuInfo menuInfo) {
						menu.setHeaderTitle(R.string.header_speed_dial);
						menu.add(0, SpeedDialWindow.CALL_MENU_ITEM, 0,
								R.string.menu_call_contact);
						menu.add(0, SpeedDialWindow.UNSTAR_MENU_ITEM, 0,
								R.string.menu_unstar_contact);
					}
				}));
	}

	public AlertDialog getConfirmDialog(){
		final AlertDialog.Builder confirmDialog = new AlertDialog.Builder(this)
		.setIcon(android.R.drawable.ic_dialog_alert)
		.setTitle("Remove all contacts from Speed Dial List?")
		.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(final DialogInterface dialog,
			 int whichButton) {
				new ClearSpeedDial().execute("-1");
				/*remove();*/
				}
			})
		.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(final DialogInterface dialog,
					int whichButton) {
			}
		});
		return confirmDialog.create();
	}
	
	private void remove(){
		final Cursor speedDialListCursor = getContentResolver()
		.query(ViewContactInfo.getContactsUri(), null,
				"starred = ?", new String[] { "1" },
				ViewContactInfo.getSortOrderString());
			startManagingCursor(speedDialListCursor);
	
			if (speedDialListCursor.moveToFirst()) {
				final ContentValues values = new ContentValues();
				values.put(ViewContactInfo.STARRED, 0);
	
			do {
				final String contactId = speedDialListCursor
						.getString(speedDialListCursor
								.getColumnIndex(ViewContactInfo._ID));
				getContentResolver().update(
						ViewContactInfo.getRawUri(), values,
						ViewContactInfo._ID + " = ?",
						new String[] { contactId });
			} while (speedDialListCursor.moveToNext());
	
		loadSpeedDialList();
	}
	}
	
	/**
	 * Reloads the cursor and the cursor adapter.
	 * 
	 * @see #speedDialListAdapter
	 * @see #speedDialList
	 * @see ViewContactInfo#getContactsUri()
	 * @see ViewContactInfo#getSortOrderString()
	 */
	private void loadSpeedDialList() {

		speedDialListCursor = getContentResolver().query(
				ViewContactInfo.getContactsUri(), null, "starred = ?",
				new String[] { "1" }, ViewContactInfo.getSortOrderString());
		startManagingCursor(speedDialListCursor);

		speedDialListAdapter = new SpeedDialListAdapter(getApplicationContext());

		speedDialList.setAdapter(speedDialListAdapter);

	}

	/**
	 * @class SpeedDialListAdapter
	 * @brief A CursorAdapter class that dynamically populates the content of
	 *        speedDialList with necessary details.
	 * @author Wyndale Wong
	 */
	public class SpeedDialListAdapter extends BaseAdapter {

		/** The inflater object used to inflate views from resource. */
		private final LayoutInflater layoutInflater;

		/** The bitmap containing the contact photo of an entry. **/
		private Bitmap contactPhoto;

		/**
		 * Constructor method of SpeedDialListAdapter, it assumes the same
		 * definition as its super class' constructor method, while obtaining
		 * content using the given context.
		 */
		public SpeedDialListAdapter(final Context context) {
			layoutInflater = LayoutInflater.from(context);
		}

		/**
		 * Gets a View that displays the data at the specified position in the
		 * data set. Contact names and photos are loaded.
		 * 
		 * @see ViewContactInfo#getContactsUri()
		 */
		public View getView(final int position, final View convertView,
				final ViewGroup parent) {
			try{
				final View newView = layoutInflater.inflate(
						R.layout.speed_dial_entry, null);
				final ImageView speedImage = (ImageView) newView
						.findViewById(R.id.speed_image);
				final TextView speedName = (TextView) newView
						.findViewById(R.id.speed_name);

				speedDialListCursor.moveToPosition(position);
				final Long contactId = speedDialListCursor.getLong(speedDialListCursor
								.getColumnIndex(ViewContactInfo._ID));

				final Uri contentUri = ContentUris.withAppendedId(ViewContactInfo
						.getContactsUri(), contactId);

				/** Loads the photo using reflection. **/
				try {
					final Class<?> c = Class
							.forName("android.provider.ContactsContract$Contacts");
					final Method m = c.getMethod("openContactPhotoInputStream",
							ContentResolver.class, Uri.class);
					contactPhoto = BitmapFactory.decodeStream((InputStream) m
							.invoke(c, getContentResolver(), contentUri));

				} catch (final ClassNotFoundException e) {
					e.printStackTrace();
					contactPhoto = BitmapFactory.decodeStream(Contacts.People
							.openContactPhotoInputStream(getContentResolver(),
									contentUri));
				} catch (final SecurityException e) {
					e.printStackTrace();
				} catch (final NoSuchMethodException e) {
					e.printStackTrace();
				} catch (final IllegalArgumentException e) {
					e.printStackTrace();
				} catch (final IllegalAccessException e) {
					e.printStackTrace();
				} catch (final InvocationTargetException e) {
					e.printStackTrace();
				}

				if (contactPhoto != null)
					speedImage.setImageBitmap(contactPhoto);
				else
					speedImage.setImageResource(R.drawable.anonymous_call);

				speedName.setText(speedDialListCursor.getString(speedDialListCursor
						.getColumnIndex(ViewContactInfo.DISPLAY_NAME)));

				return newView;
			}catch(Exception e){
				loadSpeedDialList();
				return getView(position, convertView, parent);
			}
		}

		/**
		 * The number of items that are in the data set represented by this
		 * Adapter.
		 */
		public final int getCount() {
			return speedDialListCursor.getCount();
		}

		/**
		 * Gets the data item associated with the specified position in the data
		 * set.
		 */
		public final Object getItem(final int position) {
			return speedDialListCursor.moveToPosition(position);
		}

		/**
		 * Gets the row id associated with the specified position in the list.
		 */
		public final long getItemId(final int position) {
			return position;
		}
	}

	/**
	 * Calls the numbers of the contact.
	 * 
	 * @param position
	 *            The position of the selected contact along the speed dial list
	 * 
	 * @see Dialer#dial(String, Context)
	 * @see #numberChoiceDialog(ListAdapter)
	 * @see ViewContactInfo#getContactsUri()
	 * @see ViewContactInfo#getSortOrderString()
	 * @see ViewContactInfo#getNumberUri(int)
	 * @see ViewContactInfo#getNumberUri(long)
	 * @see ViewContactInfo#getColumnName(int, int)
	 */
	private void callNumbers(final int position) {
		/** A cursor that contains details of a single contact entry. */
		final Cursor speedDialListCursor = getContentResolver().query(
				ViewContactInfo.getContactsUri(), null, "starred = ?",
				new String[] { "1" }, ViewContactInfo.getSortOrderString());
		startManagingCursor(speedDialListCursor);

		speedDialListCursor.moveToPosition(position);

		/** A cursor that contains the contact numbers of the contact entry. */
		Cursor numberListCursor = null;
		try {
			if (Integer.parseInt(Build.VERSION.SDK) >= 5)
				numberListCursor = getContentResolver()
						.query(
								ViewContactInfo
										.getNumberUri(ViewContactInfo.MODE_LOAD),
								null,
								ViewContactInfo.CONTACT_ID + " = ?",
								new String[] { speedDialListCursor
										.getString(speedDialListCursor
												.getColumnIndex(ViewContactInfo._ID)) },
								null);
			else
				numberListCursor = getContentResolver().query(
						ViewContactInfo.getNumberUri(speedDialListCursor
								.getLong(speedDialListCursor
										.getColumnIndex(ViewContactInfo._ID))),
						null, null, null, null);
			startManagingCursor(numberListCursor);

			if (numberListCursor.getCount() == 1) { // dial immediately if only
													// one number exists
				numberListCursor.moveToFirst();
				if (Dialer.isVoip)
					AetherVoice.dialer.dial(numberListCursor.getString(numberListCursor
							.getColumnIndex(ViewContactInfo.getColumnName(
									ViewContactInfo.KIND_NUMBER,
									ViewContactInfo.COLUMN_DATA))), getApplicationContext());
				else
					// Dialer.btnCall.performClick();
					AetherVoice.dialer
							.dialPSTN(numberListCursor
									.getString(numberListCursor
											.getColumnIndex(ViewContactInfo
													.getColumnName(
															ViewContactInfo.KIND_NUMBER,
															ViewContactInfo.COLUMN_DATA))));
			} else if (numberListCursor.getCount() > 1)
				numberChoiceDialog(
						new SimpleCursorAdapter(getApplicationContext(),
								android.R.layout.select_dialog_singlechoice,
								numberListCursor,
								new String[] { ViewContactInfo.getColumnName(
										ViewContactInfo.KIND_NUMBER,
										ViewContactInfo.COLUMN_DATA) },
								new int[] { android.R.id.text1 })).show();
		} catch (final CursorIndexOutOfBoundsException e) {
			Toast.makeText(getApplicationContext(), "Contact not found",
					Toast.LENGTH_SHORT).show(); // TODO: move to strings
		}
	}

	/**
	 * The alert dialog for dialing a contact with multiple numbers
	 * 
	 * @param numberListAdapter
	 *            The ListAdapter containing the list of contact numbers
	 * @return The alert dialog object
	 * @see Dialer#dial(String, Context)
	 * @see ViewContactInfo#getColumnName(int, int)
	 */
	private AlertDialog numberChoiceDialog(final ListAdapter numberListAdapter) {
		return new AlertDialog.Builder(this).setIcon(
				android.R.drawable.arrow_down_float).setTitle(
				R.string.alert_message_number_list).setSingleChoiceItems(
				numberListAdapter, -1, new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog,
							final int whichButton) {
						final Cursor numberCursor = (Cursor) numberListAdapter
								.getItem(whichButton);
						chosenNumber = numberCursor.getString(numberCursor
								.getColumnIndex(ViewContactInfo.getColumnName(
										ViewContactInfo.KIND_NUMBER,
										ViewContactInfo.COLUMN_DATA)));
					}
				}).setPositiveButton(R.string.alert_button_dial,
				new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog,
							final int whichButton) {
						if (Dialer.isVoip)
							AetherVoice.dialer.dial(chosenNumber, getApplicationContext());
						else
							// Dialer.btnCall.performClick();
							AetherVoice.dialer.dialPSTN(chosenNumber);
					}
				}).setNegativeButton(R.string.alert_button_cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog,
							final int whichButton) {
					}
				}).create();
	}

//	/**
//	 * Dials the number and uploads the dialed number on the dialer screen
//	 * 
//	 * @param target
//	 *            the number to be dialed
//	 */
//	private void dial(final String target) {
//		AetherVoice.setInput(target);
//		AetherVoice.setTab(0);
//		if (target.length() != 0)
//			if (SettingsWindow.isRegistered)
//				Receiver.engine(this).call(target, true);
//			else
//				Toast.makeText(getApplicationContext(),
//						getString(R.string.toast_register), Toast.LENGTH_SHORT)
//						.show();
//	}

	/**
	 * Raises and lowers the mustUpdateContactList flag using a boolean input.
	 * 
	 * @param mustUpdate
	 *            true raises the flag and false lowers it
	 */
	public static void setMustUpdateContactList(final boolean mustUpdate) {
		SpeedDialWindow.mustUpdateSpeedDialList = mustUpdate;
	}

	/**
	 * Defines the lines of code to be executed when a long press menu option is
	 * selected.
	 * <ul>
	 * <li>Call Contact - Calls the selected contact</li>
	 * <li>Unstar Contact - Removes the contact from the speed dial list</li>
	 * </ul>
	 * 
	 * @see #callNumbers(int)
	 * @see #loadSpeedDialList()
	 * @see ViewContactInfo#getContactsUri()
	 * @see ViewContactInfo#getSortOrderString()
	 * @see ViewContactInfo#getRawUri()
	 */
	@Override
	public boolean onContextItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case CALL_MENU_ITEM: {
			final AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item
					.getMenuInfo();
			callNumbers(menuInfo.position);
			break;
		}
		case UNSTAR_MENU_ITEM: {
			getConfirmRemove(item).show();
			break;
		}
		}
		return super.onContextItemSelected(item);
	}
	
	private AlertDialog getConfirmRemove(final MenuItem item){
		final AlertDialog.Builder confirmDialog = new AlertDialog.Builder(this)
		.setIcon(android.R.drawable.ic_dialog_alert)
		.setTitle("Remove contact from Speed Dial List?")
		.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(final DialogInterface dialog,
			 int whichButton) {
					removeItem(item);
				}
			})
		.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(final DialogInterface dialog,
					int whichButton) {
			}
		});
		return confirmDialog.create();
	}
	
	private void removeItem(MenuItem item){
		final AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item
		.getMenuInfo();

		final Cursor speedDialListCursor = getContentResolver().query(
				ViewContactInfo.getContactsUri(), null, "starred = ?",
				new String[] { "1" }, ViewContactInfo.getSortOrderString());
		startManagingCursor(speedDialListCursor);
		
		speedDialListCursor.moveToPosition(menuInfo.position);
		
		final String contactId = speedDialListCursor
				.getString(speedDialListCursor
						.getColumnIndex(ViewContactInfo._ID));
		
		new ClearSpeedDial().execute(contactId);
		
		/*final ContentValues values = new ContentValues();
		values.put(ViewContactInfo.STARRED, 0);
		getContentResolver().update(ViewContactInfo.getRawUri(), values,
				ViewContactInfo._ID + " = ?", new String[] { contactId });
		loadSpeedDialList();*/
	}
	
	/**
	 * Called when the tab is reselected, and upon doing so,
	 * refreshContactList() is then called if the mustUpdateContactList is
	 * raised.
	 * 
	 * @see #loadContactList()
	 * @see #mustUpdateContactList
	 * @see AetherVoice#setIsFinishing(boolean)
	 */
	@Override
	protected void onResume() {
		super.onResume();
		if (Receiver.call_state != UserAgent.UA_STATE_IDLE)
			Receiver.moveTop();

		if (SpeedDialWindow.mustUpdateSpeedDialList == true) {
			loadSpeedDialList();
			SpeedDialWindow.mustUpdateSpeedDialList = false;
		}

		AetherVoice.setIsFinishing(false);
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
			if(AetherVoice.isScribbling){
				
			}else if (AetherVoice.isFinishing){
				finish();
				return true;
			}else {
				Toast.makeText(getApplicationContext(), R.string.toast_finish,
						Toast.LENGTH_SHORT).show();
				AetherVoice.setIsFinishing(true);
			}
			return false;

		} else if (keyCode == KeyEvent.KEYCODE_CALL)
			return true;
		else if (keyCode == KeyEvent.KEYCODE_ENDCALL)
			return true;
		return super.onKeyDown(keyCode, event);*/
		return false;
	}
	
private class ClearSpeedDial extends AsyncTask<String, Void, Void>{
		
		private ProgressDialog pDialog;
		
		@Override
		protected void onPreExecute() {
			pDialog = new ProgressDialog(mContext);
			pDialog.setCancelable(false);
			pDialog.setMessage("Removing from speed dial");
			pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			pDialog.show();
			super.onPreExecute();
		}

		@Override
		protected Void doInBackground(String... params) {
			if(params[0].equals("-1")){
				final Cursor speedDialListCursor = getContentResolver().query(ViewContactInfo.getContactsUri(), null,
								"starred = ?", new String[] { "1" }, ViewContactInfo.getSortOrderString());
					startManagingCursor(speedDialListCursor);
			
					if (speedDialListCursor.moveToFirst()) {
						final ContentValues values = new ContentValues();
						values.put(ViewContactInfo.STARRED, 0);
						do {
							final String contactId = speedDialListCursor.getString(
									speedDialListCursor.getColumnIndex(ViewContactInfo._ID));
							getContentResolver().update(ViewContactInfo.getRawUri(), values, 
									ViewContactInfo._ID + " = ?", new String[] { contactId });
					} while (speedDialListCursor.moveToNext());
				}
			}else{
				final ContentValues values = new ContentValues();
				values.put(ViewContactInfo.STARRED, 0);
				getContentResolver().update(ViewContactInfo.getRawUri(), values,
						ViewContactInfo._ID + " = ?", new String[] { params[0] });
			}
			return null;
		}
		
		@Override
		protected void onProgressUpdate(Void... values) {
			super.onProgressUpdate(values);
		}
		
		@Override
		protected void onPostExecute(Void result) {
			pDialog.dismiss();
			loadSpeedDialList();
			super.onPostExecute(result);
		}
		
	}
	
}
