/**
 * @file ViewContactInfo.java
 * @brief It contains the ViewContactInfo class, the class that contains all the necessary views and modules 
 * 		for instantiating the view contact interface.
 * @author Wyndale Wong
 */

package com.neugent.aethervoice.ui;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.sipdroid.sipua.ui.Receiver;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.Contacts;
import android.provider.Contacts.ContactMethodsColumns;
import android.provider.Contacts.PhonesColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.neugent.aethervoice.R;

/**
 * @class ViewContactInfo
 * @brief A class that creates an interactive interface for the user to view the
 *        info and details of a contact.
 * @author Wyndale Wong
 */
@SuppressWarnings("deprecation")
public class ViewContactInfo {

	/** The contact's id displayed on the screen. */
	private String contactId = "";

	/** The contact's name displayed on the screen. */
	private String contactName = "";

	private int mStarred = 0;

	/** The Bitmap object holding the photo of the contact. */
	private Bitmap contactPhoto = null;

	/** The current context. */
	private final Context context;

	/** The inflater object used to inflate views from resource. */
	private final LayoutInflater layoutInflater;

	/** The LinearLayout holding the contact profile. */
	private LinearLayout contactFrame;

	/** The LinearLayout holding the contact numbers. */
	private LinearLayout numberFrame;

	/** The LinearLayout holding the email addresses. */
	private LinearLayout emailFrame;

	/** The defined index for the number kind of column name. */
	public static final int KIND_NUMBER = 1;

	/** The defined index for the email kind of column name. */
	public static final int KIND_EMAIL = 2;

	/** The defined index for the column name for data. */
	public static final int COLUMN_DATA = 1;

	/** The defined index for the column name for type. */
	public static final int COLUMN_TYPE = 2;

	/** The defined index for the column name for label. */
	public static final int COLUMN_LABEL = 3;

	/** The defined index for the load mode of getting the number URI. */
	public static final int MODE_LOAD = 1;

	/** The defined index for the save mode of getting the number URI. */
	public static final int MODE_SAVE = 2;

	/** The defined key name for the id column. **/
	public static final String _ID = "_id";

	/** The defined key name for the display name column. **/
	public static final String DISPLAY_NAME = "display_name";

	/** The defined key name for the display number column. **/
	public static final String NUMBER = "number";

	/** The defined key name for the contact id column. **/
	public static final String CONTACT_ID = "contact_id";

	/** The defined key name for the raw contact id column. **/
	public static final String RAW_CONTACT_ID = "raw_contact_id";

	/** The defined key name for the starred column. **/
	public static final String STARRED = "starred";

	/** The defined key name for the data1 column. **/
	public static final String DATA1 = "data1";

	/** The defined key name for the data2 column. **/
	public static final String DATA2 = "data2";

	/** The defined key name for the data3 column. **/
	public static final String DATA3 = "data3";

	/** The defined key name for the data15 column. **/
	public static final String DATA15 = "data15";

	/** The defined key name for the account name column. **/
	public static final String ACCOUNT_NAME = "account_name";

	/** The defined key name for the account type column. **/
	public static final String ACCOUNT_TYPE = "account_type";

	/** The defined key name for the mime type column. **/
	public static final String MIME_TYPE = "mimetype";

	/** The defined key name for the photo item type. **/
	public static final String ITEM_TYPE_PHOTO = "vnd.android.cursor.item/photo";

	/** The defined key name for the name item type. **/
	public static final String ITEM_TYPE_NAME = "vnd.android.cursor.item/name";

	/** The defined key name for the membership item type. **/
	public static final String ITEM_TYPE_MEMBERSHIP = "vnd.android.cursor.item/group_membership";

	/** The defined key name for the phone item type. **/
	public static final String ITEM_TYPE_NUMBER = "vnd.android.cursor.item/phone_v2";

	/** The defined key name for the email item type. **/
	public static final String ITEM_TYPE_EMAIL = "vnd.android.cursor.item/email_v2";

	/** The defined key name for the contact authority. **/
	public static final String CONTACT_AUTHORITY = "com.android.contacts";

	/** The defined key name for the is primary column. **/
	public static final String IS_PRIMARY = "is_super_primary";

	/** The defined key name for the My Contacts system group title. **/
	public static final String SYSTEM_GROUP_TITLE = "System Group: My Contacts";

	/** The defined content URI string for contacts. **/
	public static final String URI_STRING_CONTACTS = "content://com.android.contacts/contacts";

	/** The defined content URI string for raw contacts. **/
	public static final String URI_STRING_RAW = "content://com.android.contacts/raw_contacts";

	/** The defined content URI string for data. **/
	public static final String URI_STRING_DATA = "content://com.android.contacts/data";

	/** The defined content URI string for phones. **/
	public static final String URI_STRING_PHONES = "content://com.android.contacts/data/phones";

	/** The defined content URI string for emails. **/
	public static final String URI_STRING_EMAILS = "content://com.android.contacts/data/emails";

	/**
	 * The constructor method of the ContactInfo class.
	 * 
	 * @param context
	 *            The application context
	 * @param id
	 *            The id of the contact to be edited
	 * @param name
	 *            The name of the contact to be edited
	 */
	public ViewContactInfo(final Context context, final String id,
			final String name, final int starred) {
		this.context = context;
		contactId = id;
		contactName = name;
		mStarred = starred;
		layoutInflater = LayoutInflater.from(context);
	}

	/**
	 * Retrieves the contact info view with all instantiated contents.
	 * 
	 * @return The ViewContactInfo view
	 * 
	 * @see #getNameFrame()
	 * @see #loadNumberFrame()
	 * @see #loadEmailFrame()
	 * @see #initButtons(View)
	 */
	public View getContactInfoView() {
		final View contactInfoView = layoutInflater.inflate(
				R.layout.contact_profile, null);

		contactFrame = (LinearLayout) contactInfoView
				.findViewById(R.id.contact_frame);
		numberFrame = (LinearLayout) layoutInflater.inflate(
				R.layout.contact_details_frame, null);
		emailFrame = (LinearLayout) layoutInflater.inflate(
				R.layout.contact_details_frame, null);

		contactFrame.addView(getNameFrame());
		contactFrame.addView(numberFrame);
		contactFrame.addView(emailFrame);

		loadNumberFrame();
		loadEmailFrame();

		initButtons(contactInfoView);

		return contactInfoView;
	}

	/**
	 * Initializes all buttons and their listeners.
	 * 
	 * @param contactInfoView
	 *            The inflated contactInfoView
	 * 
	 * @see #deleteAlertDialog(String, String)
	 * @see AetherVoice#hideInfoFrame(boolean)
	 * @see AetherVoice#showEditContactInfo(Context, String, String, String)
	 * @see ContactListWindow#setMustUpdateContactList()
	 */
	private void initButtons(final View contactInfoView) {
		final Button btnLeft = (Button) contactInfoView
				.findViewById(R.id.btn_left);
		final Button btnRight = (Button) contactInfoView
				.findViewById(R.id.btn_right);

		btnLeft.setBackgroundResource(R.drawable.btn_edit_bg);
		btnRight.setBackgroundResource(R.drawable.btn_delete_bg);

		btnLeft.setOnClickListener(new OnClickListener() {
			public void onClick(final View v) {
				AetherVoice.hideInfoFrame(false);
				AetherVoice.showEditContactInfo(context, contactId,
						contactName, "", mStarred);
			}
		});

		btnRight.setOnClickListener(new OnClickListener() {
			public void onClick(final View v) {
				ContactListWindow.setMustUpdateContactList();
				deleteAlertDialog(contactId, contactName).show();
			}
		});
	}

	/**
	 * Loads the nameFrame and its contents.
	 * 
	 * @return The new nameFrame view
	 */
	private View getNameFrame() {

		/**
		 * The contact's photo displayed on the screen and passed as parameters
		 * for edit and delete methods.
		 */
		ImageView photoView;

		/** Displays the contact name. */
		TextView nameText;

		final View nameFrame = layoutInflater.inflate(
				R.layout.contact_name_frame, null);

		photoView = (ImageView) nameFrame.findViewById(R.id.contact_photo);
		nameText = (TextView) nameFrame.findViewById(R.id.contact_name);

		final Uri contentUri = ContentUris.withAppendedId(ViewContactInfo
				.getContactsUri(), Long.parseLong(contactId));

		/** Loads the photo using reflection. **/
		try {
			final Class<?> c = Class
					.forName("android.provider.ContactsContract$Contacts");
			final Method m = c.getMethod("openContactPhotoInputStream",
					ContentResolver.class, Uri.class);
			contactPhoto = BitmapFactory.decodeStream((InputStream) m.invoke(c,
					context.getContentResolver(), contentUri));

		} catch (final ClassNotFoundException e) {
			e.printStackTrace();
			contactPhoto = BitmapFactory.decodeStream(Contacts.People
					.openContactPhotoInputStream(context.getContentResolver(),
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
			photoView.setImageBitmap(contactPhoto);
		else
			photoView.setImageResource(R.drawable.anonymous_call);

		photoView.setScaleType(ImageView.ScaleType.FIT_XY);

		nameText.setText(contactName);

		return nameFrame;
	}

	/**
	 * Loads the header of the numberFrame.
	 * 
	 * @return The header view
	 */
	private View getNumberHeaderView() {

		/** Displays the label for the list of contact details. */
		TextView detailLabel;

		final View headerView = layoutInflater.inflate(
				R.layout.contact_details_header, null);
		detailLabel = (TextView) headerView.findViewById(R.id.kind_label);
		detailLabel.setText(R.string.frame_label_number);

		return headerView;
	}

	/**
	 * Loads the header of the emailFrame.
	 * 
	 * @return The header view
	 */
	private View getEmailHeaderView() {

		/** Displays the label for the list of contact details. */
		TextView detailLabel;

		final View headerView = layoutInflater.inflate(
				R.layout.contact_details_header, null);
		detailLabel = (TextView) headerView.findViewById(R.id.kind_label);
		detailLabel.setText(R.string.frame_label_email);

		return headerView;
	}

	/**
	 * Loads the numberFrame and its contents.
	 * 
	 * @see #getNumberUri(int)
	 * @see #getNumberUri(long)
	 * @see #newNumberHeaderView()
	 * @see #getColumnName(int, int)
	 * @see #getNumberTypeString(Context, int)
	 */
	private void loadNumberFrame() {

		/** Displays the data of a contact detail. */
		TextView dataText;

		/** Displays the label for the contact detail. */
		TextView labelText;

		/** The dial button. **/
		Button btnDial;

		/** A cursor that contains the phone numbers of the contact entry. */
		Cursor numberListCursor;

		if (Integer.parseInt(Build.VERSION.SDK) >= 5)
			numberListCursor = context.getContentResolver().query(
					ViewContactInfo.getNumberUri(ViewContactInfo.MODE_LOAD),
					null, ViewContactInfo.CONTACT_ID + " = ?",
					new String[] { contactId }, null);
		else
			numberListCursor = context.getContentResolver().query(
					ViewContactInfo.getNumberUri(Long.parseLong(contactId)),
					null, null, null, null);
		if (numberListCursor.moveToFirst()) {
			numberFrame.addView(getNumberHeaderView());
			do {
				final View detailView = layoutInflater.inflate(
						R.layout.contact_details_entry, null);
				dataText = (TextView) detailView.findViewById(R.id.detail_data);
				labelText = (TextView) detailView
						.findViewById(R.id.detail_label);
				btnDial = (Button) detailView.findViewById(R.id.btn_dial);

				dataText.setText(numberListCursor.getString(numberListCursor
						.getColumnIndex(ViewContactInfo.getColumnName(
								ViewContactInfo.KIND_NUMBER,
								ViewContactInfo.COLUMN_DATA))));
				if (numberListCursor.getInt(numberListCursor
						.getColumnIndex(ViewContactInfo.getColumnName(
								ViewContactInfo.KIND_NUMBER,
								ViewContactInfo.COLUMN_TYPE))) == PhonesColumns.TYPE_CUSTOM)
					labelText
							.setText(numberListCursor
									.getString(numberListCursor
											.getColumnIndex(ViewContactInfo
													.getColumnName(
															ViewContactInfo.KIND_NUMBER,
															ViewContactInfo.COLUMN_LABEL))));
				else
					labelText
							.setText(ViewContactInfo
									.getNumberTypeString(
											context,
											numberListCursor
													.getInt(numberListCursor
															.getColumnIndex(ViewContactInfo
																	.getColumnName(
																			ViewContactInfo.KIND_EMAIL,
																			ViewContactInfo.COLUMN_TYPE)))));

				// if(numberListCursor.getPosition()%2 == 0) {
				// detailView.setBackgroundResource(R.drawable.panel_entry_1);
				// } else {
				// detailView.setBackgroundResource(R.drawable.panel_entry_2);
				// }

				final String dialString = dataText.getText().toString();
				btnDial.setOnClickListener(new OnClickListener() {
					public void onClick(final View v) {
						AetherVoice.setInput(dialString);
						if (Dialer.isVoip)
							AetherVoice.dialer.dial(dialString, context);
						else
							// Dialer.btnCall.performClick();
							AetherVoice.dialer.dialPSTN(dialString);
					}
				});

				numberFrame.addView(detailView);
			} while (numberListCursor.moveToNext());
		}
	}

	/**
	 * Loads the emailFrame and its contents.
	 * 
	 * @see #getEmailUri(int)
	 * @see #getEmailUri(long)
	 * @see #newEmailHeaderView()
	 * @see #getColumnName(int, int)
	 * @see #getEmailTypeString(Context, int)
	 */
	private void loadEmailFrame() {

		/** Displays the data of a contact detail. */
		TextView dataText;

		/** Displays the label for the contact detail. */
		TextView labelText;

		Button btnCall;

		/** A cursor that contains the email addresses of the contact entry. */
		Cursor emailListCursor;
		if (Integer.parseInt(Build.VERSION.SDK) >= 5)
			emailListCursor = context.getContentResolver().query(
					ViewContactInfo.getEmailUri(ViewContactInfo.MODE_LOAD),
					null, ViewContactInfo.CONTACT_ID + " = ?",
					new String[] { contactId }, null);
		else
			emailListCursor = context.getContentResolver().query(
					ViewContactInfo.getEmailUri(Long.parseLong(contactId)),
					null,
					ContactMethodsColumns.KIND + " = \"" + Contacts.KIND_EMAIL
							+ "\"", null, null);
		if (emailListCursor.moveToFirst()) {
			emailFrame.addView(getEmailHeaderView());
			do {
				final View detailView = layoutInflater.inflate(
						R.layout.contact_details_entry, null);
				dataText = (TextView) detailView.findViewById(R.id.detail_data);
				labelText = (TextView) detailView
						.findViewById(R.id.detail_label);
				btnCall = (Button) detailView.findViewById(R.id.btn_dial);

				// btnCall = (Button) detailView.findViewById(R.id.btn_call);

				dataText.setText(emailListCursor.getString(emailListCursor
						.getColumnIndex(ViewContactInfo.getColumnName(
								ViewContactInfo.KIND_EMAIL,
								ViewContactInfo.COLUMN_DATA))));
				if (emailListCursor.getInt(emailListCursor
						.getColumnIndex(ViewContactInfo.getColumnName(
								ViewContactInfo.KIND_EMAIL,
								ViewContactInfo.COLUMN_TYPE))) == ContactMethodsColumns.TYPE_CUSTOM)
					labelText.setText(emailListCursor.getString(emailListCursor
							.getColumnIndex(ViewContactInfo.getColumnName(
									ViewContactInfo.KIND_EMAIL,
									ViewContactInfo.COLUMN_LABEL))));
				else
					labelText
							.setText(ViewContactInfo
									.getEmailTypeString(
											context,
											emailListCursor
													.getInt(emailListCursor
															.getColumnIndex(ViewContactInfo
																	.getColumnName(
																			ViewContactInfo.KIND_EMAIL,
																			ViewContactInfo.COLUMN_TYPE)))));

				btnCall.setVisibility(View.GONE);

				emailFrame.addView(detailView);
			} while (emailListCursor.moveToNext());
		}
	}

	/**
	 * Assembles the appropriate sort order string for contacts query.
	 * 
	 * @return The sort order string
	 */
	public static String getSortOrderString() {
		String extra = "";

		if (Integer.parseInt(Build.VERSION.SDK) >= 5)
			extra = " COLLATE LOCALIZED";

		return ViewContactInfo.DISPLAY_NAME + extra + " ASC";
	}

	/**
	 * Gets the phone content directory Uri of the contact using the contact's
	 * id.
	 * 
	 * @param id
	 *            The id of the contact
	 * @return The appended Uri object
	 */
	public static Uri getNumberUri(final long id) {
		/**
		 * A contentUri under Contacts.People.CONTENT_URI, containing the
		 * contact details of specific contact.
		 */
		final Uri personUri = ContentUris.withAppendedId(
				Contacts.People.CONTENT_URI, id);

		/**
		 * A contentUri under personUri, containing the phone numbers of
		 * specific contact.
		 */
		final Uri numberUri = Uri.withAppendedPath(personUri,
				Contacts.People.Phones.CONTENT_DIRECTORY);

		if (numberUri != null)
			return numberUri;

		return null;
	}

	/**
	 * Gets the email content directory Uri of the contact using the contact's id.
	 * 
	 * @param id The id of the contact
	 * @return The appended Uri object
	 */
	public static Uri getEmailUri(final long id) {
		/**
		 * A contentUri under Contacts.People.CONTENT_URI, containing the
		 * contact details of specific contact.
		 */
		final Uri personUri = ContentUris.withAppendedId(
				Contacts.People.CONTENT_URI, id);

		/**
		 * A contentUri under personUri, containing the phone numbers of specific contact.
		 */
		final Uri emailUri = Uri.withAppendedPath(personUri,
				Contacts.People.ContactMethods.CONTENT_DIRECTORY);

		if (emailUri != null)
			return emailUri;

		return null;
	}

	/**
	 * Constructs the appropriate contacts content URI.
	 * 
	 * @return The Uri object
	 */
	public static Uri getContactsUri() {
		if (Integer.parseInt(Build.VERSION.SDK) >= 5)
			return Uri.parse(ViewContactInfo.URI_STRING_CONTACTS);
		else
			return Contacts.People.CONTENT_URI;
	}

	/**
	 * Constructs the appropriate number content URI.
	 * 
	 * @return The Uri object
	 */
	public static Uri getNumberUri(final int mode) {
		if (Integer.parseInt(Build.VERSION.SDK) >= 5) {
			if (mode == 1)
				return Uri.parse(ViewContactInfo.URI_STRING_PHONES);
			else if (mode == 2)
				return Uri.parse(ViewContactInfo.URI_STRING_DATA);
			return null;
		} else
			return Contacts.Phones.CONTENT_URI;
	}

	/**
	 * Constructs the appropriate email content URI.
	 * 
	 * @return The Uri object
	 */
	public static Uri getEmailUri(final int mode) {
		if (Integer.parseInt(Build.VERSION.SDK) >= 5) {
			if (mode == 1)
				return Uri.parse(ViewContactInfo.URI_STRING_EMAILS);
			else if (mode == 2)
				return Uri.parse(ViewContactInfo.URI_STRING_DATA);
			return null;
		} else
			return Contacts.ContactMethods.CONTENT_URI;
	}

	/**
	 * Constructs the appropriate raw contacts content URI.
	 * 
	 * @return The Uri object
	 */
	public static Uri getRawUri() {
		if (Integer.parseInt(Build.VERSION.SDK) >= 5)
			return Uri.parse(ViewContactInfo.URI_STRING_RAW);
		else
			return Contacts.CONTENT_URI;
	}

	/**
	 * Gets the appropriate column name.
	 * 
	 * @return The Uri object
	 */
	public static String getColumnName(final int kind, final int column) {
		if (Integer.parseInt(Build.VERSION.SDK) >= 5) {
			if (column == ViewContactInfo.COLUMN_DATA)
				return ViewContactInfo.DATA1;
			else if (column == ViewContactInfo.COLUMN_TYPE)
				return ViewContactInfo.DATA2;
			else if (column == ViewContactInfo.COLUMN_LABEL)
				return ViewContactInfo.DATA3;
		} else if (column == ViewContactInfo.COLUMN_DATA) {
			if (kind == ViewContactInfo.KIND_NUMBER)
				return PhonesColumns.NUMBER;
			else if (kind == ViewContactInfo.KIND_EMAIL)
				return ContactMethodsColumns.DATA;
		} else if (column == ViewContactInfo.COLUMN_TYPE)
			return PhonesColumns.TYPE;
		else if (column == ViewContactInfo.COLUMN_LABEL)
			return PhonesColumns.LABEL;
		return null;
	}

	/**
	 * The convenience method for deleting contacts.
	 * 
	 * @param cr
	 *            The ContentResolver object
	 * @param id
	 *            the id of the contact to be deleted, -1 for delete all
	 */
	public static void deleteContact(final ContentResolver cr, final int id) {
		try {

			final Class<?> c = Class
					.forName("android.content.ContentProviderOperation");
			final Class<?> c2 = Class
					.forName("android.content.ContentProviderOperation$Builder");

			final ArrayList<Object> ops = new ArrayList<Object>();

			final Method m = c.getMethod("newDelete", Uri.class);
			final Object o1 = m.invoke(c, ViewContactInfo.getRawUri());

			if (id == -1) {
				final Method m3 = c2.getMethod("build", (Class[]) null);
				ops.add(m3.invoke(o1, (Object[]) null));

			} else {
				final Method m2 = c2.getMethod("withSelection", String.class,
						String[].class);
				final Object o2 = m2.invoke(o1, ViewContactInfo.CONTACT_ID
						+ "=?", new String[] { String.valueOf(id) });

				final Method m3 = c2.getMethod("build", (Class[]) null);
				ops.add(m3.invoke(o2, (Object[]) null));
			}

			final Method m4 = cr.getClass().getMethod("applyBatch",
					String.class, ArrayList.class);
			m4.invoke(cr, ViewContactInfo.CONTACT_AUTHORITY, ops);

		} catch (final ClassNotFoundException e) {
			e.printStackTrace();
			if (id == -1)
				cr.delete(Contacts.People.CONTENT_URI, null, null);
			else
				cr.delete(Contacts.People.CONTENT_URI, ViewContactInfo._ID
						+ "=" + id, null);
		} catch (final NoSuchMethodException e) {
			e.printStackTrace();
		} catch (final IllegalArgumentException e) {
			e.printStackTrace();
		} catch (final IllegalAccessException e) {
			e.printStackTrace();
		} catch (final InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Gets the corresponding string name of a phone number type.
	 * 
	 * @param context
	 *            The interface to the parent class' global information
	 * @param type
	 *            The integer value of the number type
	 * @return The number type string
	 */
	public static String getNumberTypeString(final Context context,
			final int type) {
		if (type == 1)
			return context.getString(R.string.type_home);
		else if (type == 2)
			return context.getString(R.string.type_mobile);
		else if (type == 3)
			return context.getString(R.string.type_work);
		else if (type == 4)
			return context.getString(R.string.type_fax_home);
		else if (type == 5)
			return context.getString(R.string.type_fax_work);
		else if (type == 6)
			return context.getString(R.string.type_pager);
		else if (type == 7)
			return context.getString(R.string.type_other);
		return context.getString(R.string.type_other);
	}

	/**
	 * Gets the corresponding string name of an email type.
	 * 
	 * @param context
	 *            The interface to the parent class' global information
	 * @param type
	 *            The integer value of the email type
	 * @return The email type string
	 */
	public static String getEmailTypeString(final Context context,
			final int type) {
		if (type == 1)
			return context.getString(R.string.type_home);
		else if (type == 2)
			return context.getString(R.string.type_work);
		else if (type == 3)
			return context.getString(R.string.type_other);
		return context.getString(R.string.type_other);
	}

//	/**
//	 * Dials the number and uploads the dialed number on the dialer screen
//	 * 
//	 * @param target
//	 *            the number to be dialed
//	 */
//	private void dial(final String target) {
//		AetherVoice.setInput(target);
//		if (target.length() != 0)
//			if (SettingsWindow.isRegistered)
//				Receiver.engine(context).call(target, true);
//			else
//				Toast.makeText(context,
//						context.getString(R.string.toast_register),
//						Toast.LENGTH_SHORT).show();
//	}

	/**
	 * The alert dialog for deleting a single contact
	 * 
	 * @param id
	 *            The id of the contact to be deleted
	 * @param name
	 *            The name of the contact to be deleted
	 * @return The alert dialog object
	 * @see #deleteContact(ContentResolver, int)
	 */
	private AlertDialog deleteAlertDialog(final String id, final String name) {
		return new AlertDialog.Builder(context).setIcon(
				android.R.drawable.ic_dialog_alert).setTitle(
				context.getString(R.string.alert_message_delete_1) + " " + name
						+ " "
						+ context.getString(R.string.alert_message_delete_2))
				.setPositiveButton(R.string.alert_button_delete,
						new DialogInterface.OnClickListener() {
							public void onClick(final DialogInterface dialog,
									final int whichButton) {
								ViewContactInfo.deleteContact(context
										.getContentResolver(), Integer
										.parseInt(id));
								AetherVoice.hideInfoFrame(false);
								CallHistoryWindow.setMustUpdateCallHistory();
							}
						}).setNegativeButton(R.string.alert_button_cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(final DialogInterface dialog,
									final int whichButton) {

							}
						}).create();
	}
}
