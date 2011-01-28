/**
 * @file ContactListWindow.java
 * @brief It contains the ContactListWindow activity class.
 * @author Wyndale Wong
 */

package com.neugent.aethervoice.ui;

import java.io.InputStream;

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
import android.database.CursorJoiner;
import android.database.MatrixCursor;
import android.database.StaleDataException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.neugent.aethervoice.R;

/**
 * @class ContactListWindow
 * @brief An activity class that creates an interactive interface on the second
 *        tab window of AetherVoice for the user to manage phone contacts.
 * @author Wyndale Wong
 */
public class ContactListWindow extends Activity implements OnClickListener {

	/** The application context. **/
	private Context context;

	/** Displays the list of contacts on the screen. */
	private ListView contactList;

	// /** The button for searching contacts. **/
	// private Button btnSearch;

	/** The text box with auto complete function. **/
	private AutoCompleteTextView autoTextView;

	// /** The LinearLayout that contains the search box. **/
	// private LinearLayout searchFrame;

	/** The adapter that dynamically populates the short-listed contactList. **/
	private ShortContactListAdapter shortContactListAdapter;

	/** The adapter that dynamically populates the contactList. */
	private ContactListAdapter contactListAdapter;

	/** The cursor containing the short-listed contact list. **/
	public Cursor constrainedCursor;

	/** The flag that indicates whether the the contactList must be updated. */
	public static boolean mustUpdateContactList = false;

	/** The variable that stores the chosen number to be dialed. */
	private String chosenNumber = "";

	/**
	 * The id assignment of the menu item "View Contact", for long press menu
	 * options.
	 */
	private static final int VIEW_MENU_ITEM = Menu.FIRST + 1;

	/**
	 * The id assignment of the menu item "Call Contact", for long press menu
	 * options.
	 */
	private static final int CALL_MENU_ITEM = Menu.FIRST + 2;

	/**
	 * The id assignment of the menu item "Edit Contact", for long press menu
	 * options.
	 */
	private static final int EDIT_MENU_ITEM = Menu.FIRST + 3;

	/**
	 * The id assignment of the menu item "Add to Speed Dial List", for long
	 * press menu options.
	 */
	private static final int STAR_MENU_ITEM = Menu.FIRST + 4;

	/**
	 * The id assignment of the menu item "Delete Contact", for long press menu
	 * options.
	 */
	private static final int DELETE_MENU_ITEM = Menu.FIRST + 5;

	/**
	 * Initializes the ContactListWindow and its contents. Called when the
	 * ContactListWindow tab is pressed for the first time or when opened as the
	 * default active tab window at the start of the application.
	 * 
	 * @see #initView()
	 */
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.contact_list);
		initView();
	}

	/**
	 * Initializes contactList, its content and its listeners.
	 * 
	 * @see #loadContactList()
	 * @see AetherVoice#showViewContactInfo(Context, String, String)
	 * @see AetherVoice#showEditContactInfo(Context, String, String, String)
	 */
	private void initView() {

		context = this;

		contactList = (ListView) findViewById(R.id.list_contacts);
		// btnSearch = (Button) findViewById(R.id.btn_search);
		// searchFrame = (LinearLayout) findViewById(R.id.search_frame);
		autoTextView = (AutoCompleteTextView) findViewById(R.id.autocomplete_search);

		// searchFrame. setVisibility(View.GONE);
		// btnSearch. setOnClickListener(this);

		((Button) findViewById(R.id.btn_add_contact)).setOnClickListener(this);
		((Button) findViewById(R.id.btn_delete_all)).setOnClickListener(this);
		((Button) findViewById(R.id.btn_export)).setOnClickListener(this);
		((Button) findViewById(R.id.btn_import)).setOnClickListener(this);

		loadContactList();

		// Sets the listeners for contactList
		contactList.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(final AdapterView<?> arg0, final View v,
					final int position, final long id) {
				/** A cursor that contains details of a single contact entry. */
				final Cursor contactListEntryCursor = (Cursor) contactListAdapter.getItem(position);
				startManagingCursor(contactListEntryCursor);

				AetherVoice
						.showViewContactInfo(
								context,
								contactListEntryCursor
										.getString(contactListEntryCursor
												.getColumnIndex(ViewContactInfo._ID)),
								contactListEntryCursor
										.getString(contactListEntryCursor
												.getColumnIndex(ViewContactInfo.DISPLAY_NAME)),
								contactListEntryCursor
										.getInt(contactListEntryCursor
												.getColumnIndex(ContactsContract.Contacts.STARRED)));
			}

		});

		contactList
				.setOnCreateContextMenuListener((new OnCreateContextMenuListener() {
					/**
					 * Creates the long press menu options for individual
					 * elements of contactList.
					 */
					public void onCreateContextMenu(final ContextMenu menu,
							final View v,
							final ContextMenu.ContextMenuInfo menuInfo) {
						menu.setHeaderTitle("Contact Entry Menu");
						menu.add(0, ContactListWindow.VIEW_MENU_ITEM, 0,
								R.string.menu_view_contact);
						menu.add(0, ContactListWindow.CALL_MENU_ITEM, 0,
								R.string.menu_call_contact);
						menu.add(0, ContactListWindow.EDIT_MENU_ITEM, 0,
								R.string.menu_edit_contact);
						menu.add(0, ContactListWindow.STAR_MENU_ITEM, 0,
								R.string.menu_star_contact);
						menu.add(0, ContactListWindow.DELETE_MENU_ITEM, 0,
								R.string.menu_delete_contact);
					}
				}));

		autoTextView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(final AdapterView<?> arg0, final View v,
					final int position, final long id) {
				// AetherVoice.dialer.dialBox.setFocusable(false);
				// btnSearch.performClick();

				final Cursor contactListEntryCursor = (Cursor) shortContactListAdapter
						.getItem(position);
				startManagingCursor(contactListEntryCursor);

				// XXX: removed because changed the ime to done
				// AetherVoice.setFromSearch();

				if (contactListEntryCursor.moveToFirst()) {
					contactListEntryCursor.moveToPosition(position);
					final String name = contactListEntryCursor
							.getString(contactListEntryCursor
									.getColumnIndex(ViewContactInfo.DISPLAY_NAME));
					AetherVoice
							.showViewContactInfo(
									context,
									contactListEntryCursor
											.getString(contactListEntryCursor
													.getColumnIndex(ViewContactInfo._ID)),
									name,
									contactListEntryCursor
											.getInt(contactListEntryCursor
													.getColumnIndex(ContactsContract.Contacts.STARRED)));

					StringBuilder buffer = null;
					String[] args = null;
					buffer = new StringBuilder();
					buffer.append("UPPER(");
					buffer.append(ViewContactInfo.DISPLAY_NAME);
					buffer.append(") GLOB ?");
					args = new String[] { name.toUpperCase() };

					constrainedCursor = context.getContentResolver().query(
							ViewContactInfo.getContactsUri(), null,
							buffer == null ? null : buffer.toString(), args,
							ViewContactInfo.getSortOrderString());

				}

				loadContactList();

				// for hiding the edittext
				final InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				mgr.hideSoftInputFromWindow(autoTextView.getWindowToken(), 0);
			}
		});

		/*autoTextView.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(final Editable s) {
				// if(s.length() == 0){
				// constrainedCursor = null;
				// }
				// loadContactList();
			}

			public void beforeTextChanged(final CharSequence s,
					final int start, final int count, final int after) {
			}

			public void onTextChanged(final CharSequence s, final int start,
					final int before, final int count) {
			}

		});*/

		autoTextView.setHint(R.string.hint_search);
	}

	/**
	 * Raises and lowers the mustUpdateContactList flag using a boolean input.
	 * 
	 * @param mustUpdate
	 *            true raises the flag and false lowers it
	 */
	public static void setMustUpdateContactList() {
		ContactListWindow.mustUpdateContactList = true;
	}

	/**
	 * Reloads the cursor and the cursor adapter.
	 * 
	 * @see #contactListAdapter
	 * @see #shortContactListAdapter
	 * @see #contactList
	 * @see ViewContactInfo#getContactsUri()
	 */
	private void loadContactList() {

		/**
		 * A cursor holding a whole table, where each row corresponds to a
		 * single contact list entry.
		 */

		Cursor contactListCursor = null;

		if (constrainedCursor == null)
			// Uri uri = ContactsContract.Contacts.CONTENT_URI;
			// String selection = ContactsContract.Contacts.IN_VISIBLE_GROUP +
			// " = '1'";
			// String sortOrder = ContactsContract.Contacts.DISPLAY_NAME +
			// " COLLATE LOCALIZED ASC";
			contactListCursor = context.getContentResolver().query(
					ViewContactInfo.getContactsUri(), null, null, null,
					ViewContactInfo.getSortOrderString());
		// contactListCursor = context.getContentResolver().query(uri, null,
		// selection, null, sortOrder);
		else
			contactListCursor = constrainedCursor;

		startManagingCursor(contactListCursor);

		// if(contactListAdapter == null){
		contactListAdapter = new ContactListAdapter(context, contactListCursor);
		// }else{
		// contactListAdapter .changeCursor(contactListCursor);
		// }

		shortContactListAdapter = new ShortContactListAdapter(context,
				contactListCursor);

		contactList.setAdapter(contactListAdapter);
		autoTextView.setAdapter(shortContactListAdapter);
	}

	/**
	 * @class ContactListAdapter
	 * @brief A CursorAdapter class that dynamically populates the content of
	 *        contactList with necessary details.
	 * @author Wyndale Wong
	 */
	private class ContactListAdapter extends CursorAdapter {

		/** The inflater object used to inflate views from resource. */
		private final LayoutInflater layoutInflater;

		/**
		 * The constructor method of ContactListAdapter, it merely assumes the
		 * same definition as its super class' constructor method.
		 */
		public ContactListAdapter(final Context context,
				final Cursor contactListCursor) {
			super(context, contactListCursor);
			layoutInflater = LayoutInflater.from(context);
		}

		/**
		 * Makes a new view to hold the data pointed to by contactListCursor.
		 */
		@Override
		public View newView(final Context context,
				final Cursor contactListEntryCursor, final ViewGroup parent) {
			return layoutInflater.inflate(R.layout.contact_list_entry, null);
		}

		@Override
		public void changeCursor(final Cursor cursor) {
			super.changeCursor(cursor);
		}

		/**
		 * Binds an existing view to the data pointed to by contactListCursor.
		 */
		@Override
		public void bindView(final View view, final Context context,
				Cursor contactListEntryCursor) {
			try {
				((TextView) view.findViewById(R.id.contact_name))
						.setText(contactListEntryCursor
								.getString(contactListEntryCursor
										.getColumnIndex(ViewContactInfo.DISPLAY_NAME)));
				
				final ContentResolver cr = context.getContentResolver();
				final long id = contactListEntryCursor.getLong(contactListEntryCursor.getColumnIndex(ViewContactInfo._ID));
				InputStream ins = null;
				ins = ContactsContract.Contacts.openContactPhotoInputStream(cr,
						ContentUris.withAppendedId(ViewContactInfo
								.getContactsUri(), id));
				final Cursor c = cr
						.query(
								ContactsContract.Data.CONTENT_URI,
								new String[] {
										ContactsContract.CommonDataKinds.Phone.NUMBER,
										ContactsContract.CommonDataKinds.Email.DISPLAY_NAME },
								ContactsContract.Data.CONTACT_ID
										+ "=?"
										+ " AND "
										+ ContactsContract.Data.MIMETYPE
										+ "='"
										+ ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
										+ "'", new String[] { String
										.valueOf(id)}, null);
				if (c.moveToFirst()) {
					final String phone = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
					final String email = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Email.DISPLAY_NAME));
					
					if (phone.length() > 0)
//						((TextView) view.findViewById(R.id.contact_detail)).setText(phone);
						setText(view, phone);
					else if (email.length() > 0)
//						((TextView) view.findViewById(R.id.contact_detail)).setText(email);
						setText(view, email);
				}else{
					setText(view, new String()); //word around for now
				}

				if (ins == null)
					// image.setBackgroundResource(R.drawable.unknown_contact);
					/*((ImageView) view.findViewById(R.id.contact_image))
							.setImageBitmap(BitmapFactory.decodeResource(
									context.getResources(),
									R.drawable.unknown_contact));*/
					setImage(BitmapFactory.decodeResource(context.getResources(), R.drawable.anonymous_call), view);
				else
					setImage(BitmapFactory.decodeStream(ins), view);

				// if(contactListEntryCursor.getPosition()%2 == 0) {
				// view.setBackgroundResource(R.drawable.panel_entry_1);
				// } else {
				// view.setBackgroundResource(R.drawable.panel_entry_2);
				// }
			} catch (final StaleDataException e) {
				// err open the cursor again?
				// Log.e("ContactListWindow", "STALE DATA EXCEPTION");
			}

		}
		
		private void setImage(Bitmap bm, View view){
			((ImageView) view.findViewById(R.id.contact_image)).setImageBitmap(bm);
		}
		
		private void setText(View view, String text){
			((TextView) view.findViewById(R.id.contact_detail)).setText(text);
		}
	}

	/**
	 * @class ShortContactListAdapter
	 * @brief An CursorAdapter class that dynamically populates the content of
	 *        autoCompleteTextView, and supports the short-listing feature.
	 * @author Wyndale Wong
	 */
	private class ShortContactListAdapter extends CursorAdapter implements
			Filterable {

		/** The obtained ContentResolver. */
		private final ContentResolver contentResolver;

		/** The inflater object used to inflate views from resource. */
		private final LayoutInflater layoutInflater;

		/**
		 * Constructor method of ShortContactListAdapter, it assumes the same
		 * definition as its super class' constructor method, while obtaining
		 * content using the given context.
		 */
		public ShortContactListAdapter(final Context context,
				final Cursor contactListCursor) {
			super(context, contactListCursor);
			contentResolver = context.getContentResolver();
			layoutInflater = LayoutInflater.from(context);
		}

		/**
		 * Makes a new view to hold the data pointed to by contactListCursor.
		 */
		@Override
		public View newView(final Context context,
				final Cursor contactListEntryCursor, final ViewGroup parent) {
			final TextView view = (TextView) layoutInflater.inflate(
					android.R.layout.simple_dropdown_item_1line, parent, false);
			return view;
		}

		/**
		 * Binds an existing view to the data pointed to by contactListCursor.
		 */
		@Override
		public void bindView(final View view, final Context context,
				final Cursor contactListEntryCursor) {
			final TextView tv = (TextView) view;
			tv.setText(contactListEntryCursor.getString(contactListEntryCursor
					.getColumnIndex(ViewContactInfo.DISPLAY_NAME)));
		}

		/**
		 * Converts the cursor into a CharSequence.
		 */
		@Override
		public String convertToString(final Cursor contactListEntryCursor) {
			return contactListEntryCursor.getString(contactListEntryCursor
					.getColumnIndex(ViewContactInfo.DISPLAY_NAME));
		}

		@Override
		public void changeCursor(final Cursor cursor) {
			contactListAdapter = new ContactListAdapter(context,
					constrainedCursor);
			contactList.setAdapter(contactListAdapter);
			super.changeCursor(cursor);
		}

		/**
		 * Runs a query on background with a specified constraint and returns a
		 * new cursor used by the adapter that is already short-listed
		 * accordingly.
		 */
		@Override
		public Cursor runQueryOnBackgroundThread(final CharSequence constraint) {
			Cursor cursorPeople;

			if (getFilterQueryProvider() != null) {
				cursorPeople = getFilterQueryProvider().runQuery(constraint);
				if (cursorPeople != null && cursorPeople.getCount() > 0)
					constrainedCursor = cursorPeople;
				else
					constrainedCursor = null;
				return cursorPeople;
			}

			StringBuilder bufferPeople = null;
			String[] args = null;
			if (constraint != null) {
				bufferPeople = new StringBuilder();
				bufferPeople.append("UPPER(");
				bufferPeople.append(ViewContactInfo.DISPLAY_NAME);
				bufferPeople.append(") GLOB ?");
				args = new String[] { "*"+constraint.toString().toUpperCase() + "*" };
			}
			
			String[] projection = new String[]{ 
						ContactsContract.Contacts._ID,
						ContactsContract.Contacts.DISPLAY_NAME,
						ContactsContract.Contacts.LOOKUP_KEY,
						ContactsContract.Contacts.STARRED};

			cursorPeople = contentResolver.query(ViewContactInfo.getContactsUri(),
					projection, bufferPeople == null ? null : bufferPeople.toString(), args,
					ViewContactInfo.getSortOrderString());
			
			if (cursorPeople != null && cursorPeople.getCount() > 0)
				constrainedCursor = cursorPeople;
			else
				constrainedCursor = null;
			
			return cursorPeople;
		}
	}

	/**
	 * Dials the number and uploads the dialed number on the dialer screen
	 * 
	 * @param target
	 *            the number to be dialed
	 *//*
	private void dial(final String target) {
		AetherVoice.setInput(target);
		if (target.length() != 0)
			if (SettingsWindow.isRegistered)
				Receiver.engine(getApplicationContext()).call(target, true);
			else
				Toast.makeText(getApplicationContext(),
						getString(R.string.toast_register), Toast.LENGTH_SHORT)
						.show();
	}*/

	/**
	 * The alert dialog for dialing a contact with multiple numbers
	 * 
	 * @param numberListAdapter
	 *            The ListAdapter containing the list of contact numbers
	 * @return The alert dialog object
	 * @see Dialer#dial(String,Context)
	 * @see ViewContactInfo#getColumnName(int, int)
	 */
	private AlertDialog numberChoiceDialog(final ListAdapter numberListAdapter) {
		return new AlertDialog.Builder(this)
				.setTitle(R.string.alert_message_number_list)
				.setSingleChoiceItems(numberListAdapter, -1, 
						new DialogInterface.OnClickListener() {
							public void onClick(final DialogInterface dialog,final int whichButton) {
								final Cursor numberCursor = (Cursor) numberListAdapter.getItem(whichButton);
								chosenNumber = numberCursor.getString(numberCursor
										.getColumnIndex(ViewContactInfo.getColumnName(
												ViewContactInfo.KIND_NUMBER,
												ViewContactInfo.COLUMN_DATA)));
					}})
				.setPositiveButton(R.string.alert_button_dial,
						new DialogInterface.OnClickListener() {
							public void onClick(final DialogInterface dialog, final int whichButton) {
								if (Dialer.isVoip)
									AetherVoice.dialer.dial(chosenNumber, context);
								else
									// Dialer.btnCall.performClick();
									AetherVoice.dialer.dialPSTN(chosenNumber);
					}})
				.setNegativeButton(R.string.alert_button_cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(final DialogInterface dialog, final int whichButton) {
						}
				}).create();
	}

	/**
	 * The alert dialog for deleting a single contact
	 * 
	 * @param id
	 *            The id of the contact to be deleted
	 * @param name
	 *            The name of the contact to be deleted
	 * @return The alert dialog object
	 * @see #loadContactList()
	 * @see ViewContactInfo#deleteContact(android.content.ContentResolver, int)
	 */
	private AlertDialog deleteAlertDialog(final int id, final String name) {
		return new AlertDialog.Builder(this).setIcon(
				android.R.drawable.ic_dialog_alert).setTitle(
				getString(R.string.alert_message_delete_1) + " " + name + " "
						+ getString(R.string.alert_message_delete_2))
				.setPositiveButton(R.string.alert_button_delete,
						new DialogInterface.OnClickListener() {
							public void onClick(final DialogInterface dialog,
									final int whichButton) {
								/*ViewContactInfo.deleteContact(
										getContentResolver(), id);
								loadContactList();
								CallHistoryWindow.setMustUpdateCallHistory();*/
								new DeleteContact().execute(id);
							}
						}).setNegativeButton(R.string.alert_button_cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(final DialogInterface dialog,
									final int whichButton) {
							}
						}).create();
	}

	/**
	 * The alert dialog for deleting all contacts
	 * 
	 * @return The alert dialog object
	 * @see #loadContactList()
	 * @see ViewContactInfo#deleteContact(android.content.ContentResolver, int)
	 */
	private AlertDialog deleteAllAlertDialog() {
		return new AlertDialog.Builder(this).setIcon(
				android.R.drawable.ic_dialog_alert).setTitle(
				R.string.alert_message_delete_all).setPositiveButton(
				R.string.alert_button_delete,
				new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog,
							final int whichButton) {
						/*ViewContactInfo.deleteContact(getContentResolver(), -1);
						loadContactList();
						SpeedDialWindow.setMustUpdateContactList(true);
						CallHistoryWindow.setMustUpdateCallHistory();*/
						new DeleteContact().execute(-1);
					}
				}).setNegativeButton(R.string.alert_button_cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog,
							final int whichButton) {
					}
				}).create();
	}

	/**
	 * Defines the lines of code to be executed when a long press menu option is
	 * selected.
	 * <ul>
	 * <li>View Contact - Exports the id and the name of the selected contact as
	 * bundle extras to ViewContactInfo activity before being launched</li>
	 * <li>Call Contact - Calls the selected contact</li>
	 * <li>Edit Contact - Exports the id and the name of the selected contact as
	 * bundle extras to EditContactInfo activity before being launched</li>
	 * <li>Star Contact - Adds the contact to the speed dial list</li>
	 * <li>Delete Contact - Deletes the contact from the database</li>
	 * </ul>
	 * 
	 * @see Dialer#dial(String, Context)
	 * @see #loadContactList()
	 * @see #deleteAlertDialog(String, String)
	 * @see ViewContactInfo#getNumberUri(long)
	 * @see ViewContactInfo#getColumnName(int, int)
	 * @see AetherVoice#showViewContactInfo(Context, String, String)
	 * @see AetherVoice#showEditContactInfo(Context, String, String, String)
	 * @see SpeedDialWindow#setMustUpdateContactList(boolean)
	 */
	@Override
	public boolean onContextItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case VIEW_MENU_ITEM: {
			final AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item
					.getMenuInfo();

			/** A cursor that contains details of a single contact entry. */
			final Cursor contactListEntryCursor = (Cursor) contactListAdapter
					.getItem(menuInfo.position);
			startManagingCursor(contactListEntryCursor);

			AetherVoice
					.showViewContactInfo(
							this,
							contactListEntryCursor
									.getString(contactListEntryCursor
											.getColumnIndex(ViewContactInfo._ID)),
							contactListEntryCursor
									.getString(contactListEntryCursor
											.getColumnIndex(ViewContactInfo.DISPLAY_NAME)),
							contactListEntryCursor
									.getInt(contactListEntryCursor
											.getColumnIndex(ContactsContract.Contacts.STARRED)));

			break;
		}
		case CALL_MENU_ITEM: {
			final AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item
					.getMenuInfo();

			/** A cursor that contains details of a single contact entry. */
			final Cursor contactListEntryCursor = (Cursor) contactListAdapter
					.getItem(menuInfo.position);
			startManagingCursor(contactListEntryCursor);

			/** A cursor that contains the contact numbers of the contact entry. */
			Cursor numberListCursor;

			if (Integer.parseInt(Build.VERSION.SDK) >= 5)
				numberListCursor = getContentResolver()
						.query(
								ViewContactInfo
										.getNumberUri(ViewContactInfo.MODE_LOAD),
								null,
								ViewContactInfo.MIME_TYPE + " = '"
										+ ViewContactInfo.ITEM_TYPE_NUMBER
										+ "' AND " + ViewContactInfo.CONTACT_ID
										+ " = ?",
								new String[] { contactListEntryCursor
										.getString(contactListEntryCursor
												.getColumnIndex(ViewContactInfo._ID)) },
								null);
			else
				numberListCursor = getContentResolver().query(
						ViewContactInfo.getNumberUri(contactListEntryCursor
								.getLong(contactListEntryCursor
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
									ViewContactInfo.COLUMN_DATA))), context);
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
			break;
		}
		case EDIT_MENU_ITEM: {
			final AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item
					.getMenuInfo();

			/** A cursor that contains details of a single contact entry. */
			final Cursor contactListEntryCursor = (Cursor) contactListAdapter
					.getItem(menuInfo.position);
			startManagingCursor(contactListEntryCursor);

			AetherVoice.showEditContactInfo(this,
							contactListEntryCursor
									.getString(contactListEntryCursor
											.getColumnIndex(ViewContactInfo._ID)),
							contactListEntryCursor
									.getString(contactListEntryCursor
											.getColumnIndex(ViewContactInfo.DISPLAY_NAME)),
							"",
							contactListEntryCursor
									.getInt(contactListEntryCursor
											.getColumnIndex(ContactsContract.Contacts.STARRED)));
			SpeedDialWindow.setMustUpdateContactList(true);

			break;
		}
		case STAR_MENU_ITEM: {
			final AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item
					.getMenuInfo();

			/** A cursor that contains details of a single contact entry. */
			final Cursor contactListEntryCursor = (Cursor) contactListAdapter
					.getItem(menuInfo.position);
			startManagingCursor(contactListEntryCursor);

			final String contactId = contactListEntryCursor
					.getString(contactListEntryCursor
							.getColumnIndex(ViewContactInfo._ID));

			final ContentValues values = new ContentValues();
			values.put(ViewContactInfo.STARRED, 1);
			getContentResolver().update(ViewContactInfo.getRawUri(), values,
					ViewContactInfo._ID + " = ?", new String[] { contactId });
			SpeedDialWindow.setMustUpdateContactList(true);
			break;
		}
		case DELETE_MENU_ITEM: {
			final AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item
					.getMenuInfo();

			/** A cursor that contains details of a single contact entry. */
			final Cursor contactListEntryCursor = (Cursor) contactListAdapter
					.getItem(menuInfo.position);
			startManagingCursor(contactListEntryCursor);
			deleteAlertDialog(contactListEntryCursor.getInt(contactListEntryCursor
							.getColumnIndex(ViewContactInfo._ID)),
					contactListEntryCursor.getString(contactListEntryCursor
							.getColumnIndex(ViewContactInfo.DISPLAY_NAME)))
					.show();

			loadContactList();
			SpeedDialWindow.setMustUpdateContactList(true);
			CallHistoryWindow.setMustUpdateCallHistory();
			break;
		}
		}
		return super.onContextItemSelected(item);
	}

	/**
	 * Defines the line of code to be executed when a button is clicked.
	 * <ul>
	 * <li>Add New - Launched edit contact screen with empty fields</li>
	 * <li>Search - Toggles the search bar on or off</li>
	 * <li>Delete All - Deletes all contacts</li>
	 * </ul>
	 * 
	 * @see #loadContactList()
	 * @see #deleteAllAlertDialog()
	 * @see AetherVoice#showEditContactInfo(Context, String, String, String)
	 */
	public void onClick(final View v) {
		final int id = v.getId();
		if (id == R.id.btn_add_contact)
			AetherVoice.showEditContactInfo(this, "", "", "", 0);
		else if (id == R.id.btn_delete_all)
			deleteAllAlertDialog().show();
		else if (id == R.id.btn_export) {
			if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
				if(context.getContentResolver().query(
						ViewContactInfo.getContactsUri(), null, null, null,
						ViewContactInfo.getSortOrderString()).getCount() > 0)
					ImportExportContacts.ExportContacts(context);
				else
					Toast.makeText(context,	"No Contacts to Export.", Toast.LENGTH_SHORT).show();
			else
				Toast.makeText(context,	context.getString(R.string.toast_no_storage), Toast.LENGTH_SHORT).show();

		} else if (id == R.id.btn_import)
			if (Environment.getExternalStorageState().equals(
					Environment.MEDIA_MOUNTED)){
				ImportExportContacts.ImportContacts(context);
				SpeedDialWindow.setMustUpdateContactList(true);
			} else
				Toast.makeText(context,
						context.getString(R.string.toast_no_storage),
						Toast.LENGTH_SHORT).show();
	}

	/**
	 * Called when the tab is reselected, and upon doing so,
	 * refreshContactList() is then called if the mustUpdateContactList is
	 * raised.
	 * 
	 * @see #loadContactList()
	 * @see AetherVoice#setIsFinishing(boolean)
	 */
	@Override
	protected void onResume() {
		super.onResume();
		if (Receiver.call_state != UserAgent.UA_STATE_IDLE)
			Receiver.moveTop();
		if (ContactListWindow.mustUpdateContactList == true) {
			loadContactList();
			ContactListWindow.mustUpdateContactList = false;
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
	
	private class DeleteContact extends AsyncTask<Integer, Void, Void>{
		private ProgressDialog pDialog;
		
		@Override
		protected void onPreExecute() {
			pDialog = new ProgressDialog(context);
			pDialog.setCancelable(false);
			pDialog.setMessage("Deleting Contact");
			pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			pDialog.show();
			super.onPreExecute();
		}

		@Override
		protected Void doInBackground(Integer... params) {
			ViewContactInfo.deleteContact(getContentResolver(), params[0]);
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			pDialog.dismiss();
			
			loadContactList();
			SpeedDialWindow.setMustUpdateContactList(true);
			CallHistoryWindow.setMustUpdateCallHistory();
			
			super.onPostExecute(result);
		}
		
	}
}
