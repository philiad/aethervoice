/**

 * @file EditContactInfo.java
 * @brief It contains the EditContactInfo class, the class that contains all the necessary views and modules 
 * 		for instantiating the edit contact interface.
 * @author Wyndale Wong
 */

package com.neugent.aethervoice.ui;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.provider.BaseColumns;
import android.provider.Contacts;
import android.provider.Contacts.ContactMethodsColumns;
import android.provider.Contacts.PeopleColumns;
import android.provider.Contacts.PhonesColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.neugent.aethervoice.R;

/**
 * @class EditContactInfo
 * @brief A class that creates an interactive interface for the user to edit the
 *        info and details of a contact.
 * @author Wyndale Wong
 */
@SuppressWarnings("deprecation")
public class EditContactInfo {

	/** The contact's id displayed on the screen. */
	private String contactId = "";

	/** The contact's name displayed on the screen. */
	private String contactName = "";

	/** The contact number to be added. */
	private String contactNumber = "";

	private int mStarred = 0;

	/** The Bitmap object holding the photo of the contact. */
	private static Bitmap contactPhoto = null;

	/** The application context. */
	private final Context context;

	/** The inflater object used to inflate views from resource. */
	private final LayoutInflater layoutInflater;

	/** The LinearLayout holding the contact profile. */
	private LinearLayout editDetailsFrame;

	/** The LinearLayout holding the contact numbers. */
	private LinearLayout editNumberFrame;

	/** The LinearLayout holding the email addresses. */
	private LinearLayout editEmailFrame;

	/** The text box for the contact name. */
	private EditText editName;

	/** Displays the profile photo of the contact. */
	private static ImageButton editPhoto;

	/**
	 * The ArrayList of phone number types corresponding to the type values of
	 * the phone numbers displayed on the screen.
	 */
	private List<Integer> numberTypeList;

	/**
	 * The ArrayList of phone number labels corresponding to the labels strings
	 * of the phone numbers displayed on the screen.
	 */
	private List<String> numberLabelList;

	/**
	 * The ArrayList of phone numbers corresponding to the phone numbers
	 * displayed on the screen.
	 */
	private List<String> numberDataList;

	/**
	 * The ArrayList of email types corresponding to the type values of the
	 * email addresses displayed on the screen.
	 */
	private List<Integer> emailTypeList;

	/**
	 * The ArrayList of email labels corresponding to the labels strings of the
	 * email addresses displayed on the screen.
	 */
	private List<String> emailLabelList;

	/**
	 * The ArrayList of email addresses corresponding to the email addresses
	 * displayed on the screen.
	 */
	private List<String> emailDataList;

	/** The defined index of custom label for the phone number labels array. */
	private static final int LABEL_CUSTOM_NUMBER = 7;

	/** The defined index of custom label for the email labels array. */
	private static final int LABEL_CUSTOM_EMAIL = 3;

	/** The defined key of custom label. */
	private static final int LABEL_KEY_CUSTOM = 0;

	/** The defined key of home label. */
	private static final int LABEL_KEY_HOME = 1;

	/** The defined key of mobile label. */
	private static final int LABEL_KEY_MOBILE = 2;

	/** The defined index of phone number as a type of contact detail. */
	private static final int KIND_NUMBER = 1;

	/** The defined index of E-mail as a type of contact detail. */
	private static final int KIND_EMAIL = 2;
	
	private Handler mHandler = new Handler(){
		private ProgressDialog mProgress;
		
		public void handleMessage(android.os.Message msg) {
			switch(msg.what){
			case 0:
				mProgress = new ProgressDialog(context);
				mProgress.setCancelable(false);
				mProgress.setMessage("Saving Contact");
				mProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				mProgress.show();
				break;
			case 1:
				mProgress.dismiss();
				AetherVoice.hideInfoFrame(false);
				break;
			}
		};
	};

	/**
	 * The constructor method of the EditContactInfo class.
	 * 
	 * @param context
	 *            The application context
	 * @param id
	 *            The id of the contact to be edited
	 * @param name
	 *            The name of the contact to be edited
	 * @param number
	 *            A newly added number of the contact to be edited
	 */
	public EditContactInfo(final Context context, final String id,
			final String name, final String number, final int starred) {
		this.context = context;
		EditContactInfo.contactPhoto = null;
		contactId = id;
		contactName = name;
		contactNumber = number;
		mStarred = starred;
		
		layoutInflater = LayoutInflater.from(context);
	}

	/**
	 * Retrieves the edit contact info view with all instantiated contents.
	 * 
	 * @return The EditContactInfo view
	 * 
	 * @see #getEditNameFrame()
	 * @see #loadNumberArrayContents()
	 * @see #loadEmailArrayContents()
	 * @see #loadEditNumberFrame()
	 * @see #loadEditEmailFrame()
	 * @see #initButtons(View)
	 */
	public View getEditContactInfoView() {
		final View editContactInfoView = layoutInflater.inflate(
				R.layout.contact_profile, null);

		editDetailsFrame = (LinearLayout) editContactInfoView
				.findViewById(R.id.contact_frame);
		editNumberFrame = (LinearLayout) layoutInflater.inflate(
				R.layout.contact_details_frame, null);
		editEmailFrame = (LinearLayout) layoutInflater.inflate(
				R.layout.contact_details_frame, null);

		editDetailsFrame.addView(getEditNameFrame());
		editDetailsFrame.addView(editNumberFrame);
		editDetailsFrame.addView(editEmailFrame);

		numberTypeList = new ArrayList<Integer>();
		numberLabelList = new ArrayList<String>();
		numberDataList = new ArrayList<String>();

		emailTypeList = new ArrayList<Integer>();
		emailLabelList = new ArrayList<String>();
		emailDataList = new ArrayList<String>();

		if (!contactId.equals("")) {
			loadNumberArrayContents();
			loadEmailArrayContents();
		}

		if (!contactNumber.equals("")) {
			numberTypeList.add(7);
			numberLabelList.add("");
			numberDataList.add(contactNumber);
		}

		loadEditNumberFrame();
		loadEditEmailFrame();

		initButtons(editContactInfoView);

		return editContactInfoView;
	}
	
	/**
	 * Initializes all buttons and their listeners.
	 * 
	 * @param editContactInfoView
	 *            The inflated editContactInfoView
	 * 
	 * @see #saveAlertDialog(String)
	 * @see #save(String, String, Bitmap, List, List, List, List, List, List)
	 * @see ContactListWindow#setMustUpdateContactList()
	 * @see CallHistoryWindow#setMustUpdateCallHistory()
	 * @see AetherVoice#hideInfoFrame(boolean)
	 */
	private void initButtons(final View editContactInfoView) {

		final Button btnLeft = (Button) editContactInfoView
				.findViewById(R.id.btn_left);
		final Button btnRight = (Button) editContactInfoView
				.findViewById(R.id.btn_right);

		btnLeft.setBackgroundResource(R.drawable.btn_save_bg);
		btnRight.setBackgroundResource(R.drawable.btn_discard_bg);

		btnLeft.setOnClickListener(new OnClickListener() {
			public void onClick(final View v) {
				contactName = editName.getText().toString();
				
				new SaveContact().execute();
				
				/*new Thread(new Runnable(){

					@Override
					public void run() {
						mHandler.sendEmptyMessage(0);
						
						
						if (getId(contactName).equals("") || getId(contactName).equals(contactId))
							save(contactId, contactName, EditContactInfo.contactPhoto,
									numberTypeList, numberLabelList, numberDataList,
									emailTypeList, emailLabelList, emailDataList);
						else
							saveAlertDialog(contactName).show();
						
						if (!getId(contactName).equals("")
								&& !getId(contactName).equals(contactId))
							saveAlertDialog(contactName).show();
						else
							save(contactId, contactName, EditContactInfo.contactPhoto,
									numberTypeList, numberLabelList, numberDataList,
									emailTypeList, emailLabelList, emailDataList);
						

						ContactListWindow.setMustUpdateContactList();
						CallHistoryWindow.setMustUpdateCallHistory();
						SpeedDialWindow.setMustUpdateContactList(true);	
						
//						AetherVoice.hideInfoFrame(false);
						
						mHandler.sendEmptyMessage(1);
					}
					
				}).start();*/
			}
		});

		btnRight.setOnClickListener(new OnClickListener() {
			public void onClick(final View v) {
				AetherVoice.hideInfoFrame(false); 
			}
		});
	}
	

	/**
	 * Loads the editNameFrame and its contents.
	 * 
	 * @see #photoSourceDialog()
	 * @return The new editNameFrame view
	 */
	private View getEditNameFrame() {

		/** The view holding the contact name and photo. */
		final View nameFrame = layoutInflater.inflate(
				R.layout.edit_contact_name_frame, null);

		EditContactInfo.editPhoto = (ImageButton) nameFrame
				.findViewById(R.id.edit_contact_photo);
		editName = (EditText) nameFrame.findViewById(R.id.edit_contact_name);

		if (!contactId.equals("")) {

			final Uri contentUri = ContentUris.withAppendedId(ViewContactInfo
					.getContactsUri(), Long.parseLong(contactId));

			/** Loads the photo using reflection. **/
			try {
				final Class<?> c = Class
						.forName("android.provider.ContactsContract$Contacts");
				final Method m = c.getMethod("openContactPhotoInputStream",
						ContentResolver.class, Uri.class);
				EditContactInfo.contactPhoto = BitmapFactory
						.decodeStream((InputStream) m.invoke(c, context
								.getContentResolver(), contentUri));

			} catch (final ClassNotFoundException e) {
				e.printStackTrace();
				EditContactInfo.contactPhoto = BitmapFactory
						.decodeStream(Contacts.People
								.openContactPhotoInputStream(context
										.getContentResolver(), contentUri));
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

			if (EditContactInfo.contactPhoto != null)
				EditContactInfo.editPhoto
						.setImageBitmap(EditContactInfo.contactPhoto);
			else
				EditContactInfo.editPhoto
						.setImageResource(R.drawable.anonymous_call);
		} else
			EditContactInfo.editPhoto
					.setImageResource(R.drawable.incall_photo_border);

		EditContactInfo.editPhoto.setScaleType(ImageButton.ScaleType.FIT_XY);
		EditContactInfo.editPhoto.setOnClickListener(new OnClickListener() {
			public void onClick(final View v) {
//				final Intent intent = new Intent(context, FileBrowser.class);
//				final Intent intent = new Intent(context, ImageBrowser.class);
				final Intent intent = new Intent(context, ImageGallery.class);
				context.startActivity(intent);
			}
		});

		editName.setText(contactName);

		return nameFrame;
	}

	/**
	 * Initializes the numberTypeList, the numberLabelList, the numberDataList,
	 * and loads the editNumberFrame.
	 * 
	 * @see #loadEditNumberFrame()
	 * @see ViewContactInfo#getNumberUri(int)
	 * @see ViewContactInfo#getNumberUri(long)
	 * @see ViewContactInfo#getColumnName(int, int)
	 */
	private void loadNumberArrayContents() {

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

		if (numberListCursor.moveToFirst())
			do {
				numberTypeList.add(numberListCursor.getInt(numberListCursor
						.getColumnIndex(ViewContactInfo.getColumnName(
								ViewContactInfo.KIND_NUMBER,
								ViewContactInfo.COLUMN_TYPE))));
				numberLabelList.add(numberListCursor.getString(numberListCursor
						.getColumnIndex(ViewContactInfo.getColumnName(
								ViewContactInfo.KIND_NUMBER,
								ViewContactInfo.COLUMN_LABEL))));
				numberDataList.add(numberListCursor.getString(numberListCursor
						.getColumnIndex(ViewContactInfo.getColumnName(
								ViewContactInfo.KIND_NUMBER,
								ViewContactInfo.COLUMN_DATA))));
			} while (numberListCursor.moveToNext());
	}

	/**
	 * Initializes the emailTypeList, the emailLabelList, the emailDataList, and
	 * loads the editEmailFrame.
	 * 
	 * @see #loadEditNumberFrame()
	 * @see ViewContactInfo#getEmailUri(int)
	 * @see ViewContactInfo#getEmailUri(long)
	 * @see ViewContactInfo#getColumnName(int, int)
	 */
	private void loadEmailArrayContents() {

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

		if (emailListCursor.moveToFirst())
			do {
				emailTypeList.add(emailListCursor.getInt(emailListCursor
						.getColumnIndex(ViewContactInfo.getColumnName(
								ViewContactInfo.KIND_EMAIL,
								ViewContactInfo.COLUMN_TYPE))));
				emailLabelList.add(emailListCursor.getString(emailListCursor
						.getColumnIndex(ViewContactInfo.getColumnName(
								ViewContactInfo.KIND_EMAIL,
								ViewContactInfo.COLUMN_LABEL))));
				emailDataList.add(emailListCursor.getString(emailListCursor
						.getColumnIndex(ViewContactInfo.getColumnName(
								ViewContactInfo.KIND_EMAIL,
								ViewContactInfo.COLUMN_DATA))));
			} while (emailListCursor.moveToNext());

	}

	/**
	 * Loads the editNumberFrame and its contents.
	 * 
	 * @see #getEditNumberHeaderView()
	 * @see #addEditNumberEntries()
	 */
	private void loadEditNumberFrame() {
		editNumberFrame.addView(getEditNumberHeaderView());
		addEditNumberEntries();
	}

	/**
	 * Loads the editEmailFrame and its contents.
	 * 
	 * @see #getEditEmailHeaderView()
	 * @see #addEditEmailEntries()
	 */
	private void loadEditEmailFrame() {
		editEmailFrame.addView(getEditEmailHeaderView());
		addEditEmailEntries();
	}

	/**
	 * Loads the header of the editNumberFrame.
	 * 
	 * @return The header view
	 * @see #loadEditNumberFrame()
	 */
	private View getEditNumberHeaderView() {

		/** Displays the label for the list of contact details. */
		TextView editDetailLabel;

		/** The Button to add a new contact detail. */
		ImageButton addButton;

		final View headerView = layoutInflater.inflate(
				R.layout.edit_contact_details_header, null);

		editDetailLabel = (TextView) headerView
				.findViewById(R.id.edit_kind_label);
		addButton = (ImageButton) headerView.findViewById(R.id.btn_add);

		editDetailLabel.setText(R.string.frame_label_number);

		addButton.setOnClickListener(new OnClickListener() {
			public void onClick(final View v) {
				numberTypeList.add(EditContactInfo.LABEL_KEY_MOBILE);
				numberLabelList.add("");
				numberDataList.add("");
				editNumberFrame.removeAllViews();
				loadEditNumberFrame();
			}
		});

		return headerView;
	}

	/**
	 * Loads the header of the editEmailFrame.
	 * 
	 * @return The header view
	 * @see #loadEditEmailFrame()
	 */
	private View getEditEmailHeaderView() {

		/** Displays the label for the list of contact details. */
		TextView editDetailLabel;

		/** The Button to add a new contact detail. */
		ImageButton addButton;

		final View headerView = layoutInflater.inflate(
				R.layout.edit_contact_details_header, null);

		editDetailLabel = (TextView) headerView
				.findViewById(R.id.edit_kind_label);
		addButton = (ImageButton) headerView.findViewById(R.id.btn_add);

		editDetailLabel.setText(R.string.frame_label_email);

		addButton.setOnClickListener(new OnClickListener() {
			public void onClick(final View v) {
				emailTypeList.add(EditContactInfo.LABEL_KEY_HOME);
				emailLabelList.add("");
				emailDataList.add("");
				editEmailFrame.removeAllViews();
				loadEditEmailFrame();
			}
		});

		return headerView;
	}

	/**
	 * Loads the phone number entries in the editNumberFrame.
	 * 
	 * @see ViewContactInfo#getNumberTypeString(Context, int)
	 * @see #getLabelClickListener(Context, Button, int, int)
	 * @see #getDataClickListener(Context, Button, int, int)
	 * @see #loadEditNumberFrame()
	 */
	private void addEditNumberEntries() {

		/** Displays and changes the data of a contact detail. */
		EditText editData;

		/** Displays and changes the label for the contact detail. */
		Button labelButton;

		/** Removes the number from the list. */
		ImageButton removeButton;

		for (int i = 0; i < numberDataList.size(); i++) {
			final int position = i;
			final View detailView = layoutInflater.inflate(
					R.layout.edit_contact_details_entry, null);

			editData = (EditText) detailView
					.findViewById(R.id.edit_detail_data);
			labelButton = (Button) detailView
					.findViewById(R.id.edit_detail_label);
			removeButton = (ImageButton) detailView
					.findViewById(R.id.edit_detail_remove);

			if (numberTypeList.get(position) == PhonesColumns.TYPE_CUSTOM)
				labelButton.setText(numberLabelList.get(position));
			else
				labelButton.setText(ViewContactInfo.getNumberTypeString(
						context, numberTypeList.get(position)));
			editData.setText(numberDataList.get(position));

			labelButton.setOnClickListener(getLabelClickListener(labelButton,
					EditContactInfo.KIND_NUMBER, position));

			editData.addTextChangedListener(getTextWatcher(
					EditContactInfo.KIND_NUMBER, position));

			removeButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(final View v) {
					numberTypeList.remove(position);
					numberLabelList.remove(position);
					numberDataList.remove(position);
					editNumberFrame.removeAllViews();
					loadEditNumberFrame();
				}
			});

			// if(position%2 == 0) {
			// detailView.setBackgroundResource(R.drawable.panel_entry_1);
			// } else {
			// detailView.setBackgroundResource(R.drawable.panel_entry_2);
			// }

			editNumberFrame.addView(detailView);
		}
	}

	/**
	 * Loads the email address entries in the editEmailFrame.
	 * 
	 * @see ViewContactInfo#getEmailTypeString(Context, int)
	 * @see #getLabelClickListener(Context, Button, int, int)
	 * @see #getDataClickListener(Context, Button, int, int)
	 * @see #loadEditEmailFrame()
	 */
	private void addEditEmailEntries() {
		/** Displays and changes the data of a contact detail. */
		EditText editData;

		/** Displays and changes the label for the contact detail. */
		Button labelButton;

		/** Removes the number from the list. */
		ImageButton removeButton;

		for (int i = 0; i < emailDataList.size(); i++) {
			final int position = i;
			final View detailView = layoutInflater.inflate(
					R.layout.edit_contact_details_entry, null);

			editData = (EditText) detailView
					.findViewById(R.id.edit_detail_data);
			labelButton = (Button) detailView
					.findViewById(R.id.edit_detail_label);
			removeButton = (ImageButton) detailView
					.findViewById(R.id.edit_detail_remove);

			if (emailTypeList.get(position) == ContactMethodsColumns.TYPE_CUSTOM)
				labelButton.setText(emailLabelList.get(position));
			else
				labelButton.setText(ViewContactInfo.getEmailTypeString(context,
						emailTypeList.get(position)));
			editData.setText(emailDataList.get(position));

			labelButton.setOnClickListener(getLabelClickListener(labelButton,
					EditContactInfo.KIND_EMAIL, position));

			editData.addTextChangedListener(getTextWatcher(
					EditContactInfo.KIND_EMAIL, position));

			removeButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(final View v) {
					emailTypeList.remove(position);
					emailLabelList.remove(position);
					emailDataList.remove(position);
					editEmailFrame.removeAllViews();
					loadEditEmailFrame();
				}
			});

			// if(position%2 == 0) {
			// detailView.setBackgroundResource(R.drawable.panel_entry_1);
			// } else {
			// detailView.setBackgroundResource(R.drawable.panel_entry_2);
			// }

			editEmailFrame.addView(detailView);
		}
	}

	/**
	 * Sets the contact photo from the file browser.
	 * 
	 * @param context
	 *            The application context
	 * @param photoFile
	 *            The photo file
	 * @see #invalidPhotoDialog(Context)
	 */
	public static void setProfilePhoto(final Context context,
			final File photoFile) {
		try {
			final int idealWidth = 186;
			final int idealHeight = 199;
			final BitmapFactory.Options bfo = new BitmapFactory.Options();
			bfo.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(photoFile.getAbsolutePath(), bfo);
			final int imageWidth = bfo.outWidth;
			final int imageHeight = bfo.outHeight;
			if (imageWidth > idealWidth || imageHeight > idealHeight) {
				final int imageWidthRatio = (int) Math.floor(imageWidth
						/ idealWidth);
				final int imageHeightRatio = (int) Math.floor(imageHeight
						/ idealHeight);
				bfo.inSampleSize = imageWidthRatio > imageHeightRatio ? imageHeightRatio
						: imageWidthRatio;
			}

			bfo.inJustDecodeBounds = false;
			EditContactInfo.contactPhoto = BitmapFactory.decodeFile(photoFile
					.getAbsolutePath(), bfo);
			if (EditContactInfo.contactPhoto != null)
				EditContactInfo.editPhoto
						.setImageBitmap(EditContactInfo.contactPhoto);
			else
				EditContactInfo.invalidPhotoDialog(context).show();
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(context, "Unreadable SDcard!", Toast.LENGTH_LONG).show();
		}

	}

	/**
	 * The id of the contact who had the given name if any
	 * 
	 * @param context
	 *            Interface to the parent class' global information
	 * @param name
	 *            The contact name used to query for an existing contact
	 * @return The id of the contact who had the given name, and an empty string
	 *         if contact doesn't exists
	 */
	private String getId(final String name) {
		/** A cursor holding single contact entry's name. */
		final Cursor idCursor = context.getContentResolver().query(
				Contacts.People.CONTENT_URI, new String[] { BaseColumns._ID },
				PeopleColumns.NAME + "= \"" + name + "\"", null, null);

		String id = "";
		if (idCursor.moveToFirst())
			id = idCursor.getString(idCursor.getColumnIndex(BaseColumns._ID));
		return id;
	}

	/**
	 * Gets the dialog title string for label selection given the king of
	 * contact detail being saved.
	 * 
	 * @param kind
	 *            The kind of contact detail being saved (1) phone number, (2)
	 *            E-mail address
	 * @return The dialog title string
	 */
	private String getLabelDialogTitle(final int kind) {
		if (kind == EditContactInfo.KIND_NUMBER)
			return context.getString(R.string.alert_message_number_label);
		else if (kind == EditContactInfo.KIND_EMAIL)
			return context.getString(R.string.alert_message_email_label);
		else
			return "";
	}

	/**
	 * Gets the id of the array from an xml file used in label selection.
	 * 
	 * @param kind
	 *            The kind of contact detail being saved (1) phone number, (2)
	 *            E-mail address
	 * @return The id of the array
	 */
	private int getLabelDialogArrayId(final int kind) {
		if (kind == EditContactInfo.KIND_NUMBER)
			return R.array.number_labels;
		else if (kind == EditContactInfo.KIND_EMAIL)
			return R.array.email_labels;
		else
			return 0;
	}

	/**
	 * The general saving method that adds a new contact or updates an old one
	 * with the given contact details.
	 * 
	 * @param id
	 *            The id of the contact to be saved, assumes the value of an
	 *            empty string if the contact is new
	 * @param name
	 *            The name of the contact to be saved
	 * @param sipNumber
	 *            The SIP number of the contact to be saved
	 * @param mobileNumber
	 *            The mobile number of the contact to be saved
	 * @see #insertRecord(String, List, List, List, List, List, List)
	 */
	private void save(final String id, String name, final Bitmap photo,
			final List<Integer> numberTypeList,
			final List<String> numberLabelList,
			final List<String> numberDataList,
			final List<Integer> emailTypeList,
			final List<String> emailLabelList, final List<String> emailDataList) {
		if (name.equals(""))
			name = context.getString(R.string.unknown_contact_name);
		if (id == null || id.equals(""))
			insertRecord(name, photo, numberTypeList, numberLabelList,
					numberDataList, emailTypeList, emailLabelList,
					emailDataList);
		else {
			// Update is not optimal for now, delete-insert is the alternative
			// implementation
			System.out.println("inserting on else statement");
			ViewContactInfo.deleteContact(context.getContentResolver(), Integer.parseInt(id));
			insertRecord(name, photo, numberTypeList, numberLabelList,
					numberDataList, emailTypeList, emailLabelList,
					emailDataList);
		}
		if (EditContactInfo.contactPhoto != null)
			EditContactInfo.contactPhoto.recycle();
	}

	/**
	 * Inserts a new record into the contacts database using a given name and
	 * ArrayLists of details.
	 * 
	 * @param name
	 *            The name of the new contact
	 * @param number
	 *            the number of the new contact
	 * @see ViewContactInfo#getRawUri()
	 * @see ViewContactInfo#getNumberUri(int)
	 * @see ViewContactInfo#getEmailUri(int)
	 * @see ViewContactInfo#getColumnName(int, int)
	 */
	private void insertRecord(final String name, final Bitmap photo,
			final List<Integer> numberTypeList,
			final List<String> numberLabelList,
			final List<String> numberDataList,
			final List<Integer> emailTypeList,
			final List<String> emailLabelList, final List<String> emailDataList) {
		final ContentValues values = new ContentValues();
		long rawContactId = 0;
		Uri newPersonUri;
		
		System.out.println("inserting contact");
		
		if (Integer.parseInt(Build.VERSION.SDK) >= 5) {
			// values.put(ViewContactInfo.ACCOUNT_TYPE,
			// ViewContactInfo.SYSTEM_GROUP_TITLE);
			// values.put(ViewContactInfo.ACCOUNT_NAME, name);
			newPersonUri = context.getContentResolver().insert(
					ViewContactInfo.getRawUri(), values);
			rawContactId = ContentUris.parseId(newPersonUri);

			values.clear();
			values.put(ViewContactInfo.RAW_CONTACT_ID, rawContactId);
			values.put(ViewContactInfo.MIME_TYPE,
					ViewContactInfo.ITEM_TYPE_NAME);
			values.put(ViewContactInfo.DATA1, name);
			context.getContentResolver().insert(
					Uri.parse(ViewContactInfo.URI_STRING_DATA), values);

			values.clear();
			values.put(ViewContactInfo.RAW_CONTACT_ID, rawContactId);
			values.put(ViewContactInfo.DATA1, 1);
			values.put(ViewContactInfo.MIME_TYPE,
					ViewContactInfo.ITEM_TYPE_MEMBERSHIP);
			context.getContentResolver().insert(
					Uri.parse(ViewContactInfo.URI_STRING_DATA), values);

		} else {
			values.put(PeopleColumns.NAME, name);
			newPersonUri = Contacts.People.createPersonInMyContactsGroup(
					context.getContentResolver(), values);
		}
		
		System.out.println("inserting photo");

		if (photo != null) {
			values.clear();

			final ByteArrayOutputStream stream = new ByteArrayOutputStream();
			photo.compress(Bitmap.CompressFormat.JPEG, 75, stream);

			if (Integer.parseInt(Build.VERSION.SDK) >= 5) {
				int photoRow = -1;

				final String where = ViewContactInfo.RAW_CONTACT_ID + " = "
						+ rawContactId + " AND " + ViewContactInfo.MIME_TYPE
						+ "=='" + ViewContactInfo.ITEM_TYPE_PHOTO + "'";

				final Cursor cursor = context.getContentResolver().query(
						Uri.parse(ViewContactInfo.URI_STRING_DATA), null,
						where, null, null);

				final int idIdx = cursor
						.getColumnIndexOrThrow(ViewContactInfo._ID);

				if (cursor.moveToFirst())
					photoRow = cursor.getInt(idIdx);

				cursor.close();

				values.put(ViewContactInfo.RAW_CONTACT_ID, rawContactId);
				values.put(ViewContactInfo.IS_PRIMARY, 1);
				values.put(ViewContactInfo.DATA15, stream.toByteArray());
				values.put(ViewContactInfo.MIME_TYPE,
						ViewContactInfo.ITEM_TYPE_PHOTO);

				if (photoRow >= 0)
					context.getContentResolver().update(
							Uri.parse(ViewContactInfo.URI_STRING_DATA), values,
							ViewContactInfo._ID + " = " + photoRow, null);
				else
					context.getContentResolver().insert(
							Uri.parse(ViewContactInfo.URI_STRING_DATA), values);
			} else
				Contacts.People.setPhotoData(context.getContentResolver(),
						newPersonUri, stream.toByteArray());
		}
		
		System.out.println("inserting phone numbers");

		for (int i = 0; i < numberDataList.size(); i++) {
			values.clear();
			if (!numberDataList.get(i).equals("")) {
				if (Integer.parseInt(Build.VERSION.SDK) >= 5) {
					values.put(ViewContactInfo.RAW_CONTACT_ID, rawContactId);
					values.put(ViewContactInfo.MIME_TYPE,
							ViewContactInfo.ITEM_TYPE_NUMBER);
				} else
					values.put(Contacts.Phones.PERSON_ID, newPersonUri
							.getLastPathSegment());

				values.put(ViewContactInfo.getColumnName(
						ViewContactInfo.KIND_NUMBER,
						ViewContactInfo.COLUMN_DATA), numberDataList.get(i));
				values.put(ViewContactInfo.getColumnName(
						ViewContactInfo.KIND_NUMBER,
						ViewContactInfo.COLUMN_TYPE), numberTypeList.get(i));
				if (numberTypeList.get(i) == EditContactInfo.LABEL_KEY_CUSTOM)
					values.put(ViewContactInfo.getColumnName(
							ViewContactInfo.KIND_NUMBER,
							ViewContactInfo.COLUMN_LABEL), numberLabelList
							.get(i));
				context
						.getContentResolver()
						.insert(
								ViewContactInfo
										.getNumberUri(ViewContactInfo.MODE_SAVE),
								values);
			}
		}
		System.out.println("inserting emails");

		for (int i = 0; i < emailDataList.size(); i++) {
			values.clear();
			if (!emailDataList.get(i).equals("")) {
				if (Integer.parseInt(Build.VERSION.SDK) >= 5) {
					values.put(ViewContactInfo.RAW_CONTACT_ID, rawContactId);
					values.put(ViewContactInfo.MIME_TYPE,
							ViewContactInfo.ITEM_TYPE_EMAIL);
				} else
					values.put(Contacts.ContactMethods.PERSON_ID, newPersonUri
							.getLastPathSegment());
				values.put(ViewContactInfo
						.getColumnName(ViewContactInfo.KIND_EMAIL,
								ViewContactInfo.COLUMN_DATA), emailDataList
						.get(i));
				values.put(ViewContactInfo
						.getColumnName(ViewContactInfo.KIND_EMAIL,
								ViewContactInfo.COLUMN_TYPE), emailTypeList
						.get(i));
				if (emailTypeList.get(i) == EditContactInfo.LABEL_KEY_CUSTOM)
					values.put(ViewContactInfo.getColumnName(
							ViewContactInfo.KIND_EMAIL,
							ViewContactInfo.COLUMN_LABEL), emailLabelList
							.get(i));
				context.getContentResolver().insert(
						ViewContactInfo.getEmailUri(ViewContactInfo.MODE_SAVE),
						values);
			}
		}
		
		System.out.println("finish inserting contact");

		values.clear();
		values.put(ViewContactInfo.STARRED, mStarred);
		context.getContentResolver().update(ViewContactInfo.getRawUri(),
				values, ViewContactInfo._ID + " = ?",
				new String[] { String.valueOf(rawContactId) });
	}

	/**
	 * The OnClickListener that waits the for labelButton to be pressed and
	 * launches an alert dialog for label choices.
	 * 
	 * @see #labelChoiceDialog(Context, Button, int, int)
	 */
	private OnClickListener getLabelClickListener(final Button labelButton,
			final int kind, final int index) {
		return new View.OnClickListener() {
			public void onClick(final View v) {
				labelChoiceDialog(labelButton, kind, index).show();
			}
		};
	}

	/**
	 * The TextWatcher that listens to changes in text contents on the text
	 * boxes.
	 * 
	 * @param kind
	 *            The kind of data being changed
	 * @param index
	 *            The index of the entry being changed
	 * @return The TextWatcher object
	 */
	private TextWatcher getTextWatcher(final int kind, final int index) {
		return new TextWatcher() {
			public void beforeTextChanged(final CharSequence s,
					final int start, final int count, final int after) {
			}

			public void onTextChanged(final CharSequence s, final int start,
					final int before, final int count) {
			}

			public void afterTextChanged(final Editable s) {
				System.gc();
				if (kind == EditContactInfo.KIND_NUMBER) {
					numberDataList.remove(index);
					numberDataList.add(index, s.toString());
				} else if (kind == EditContactInfo.KIND_EMAIL) {
					emailDataList.remove(index);
					emailDataList.add(index, s.toString());
				}
			}
		};
	}

	/**
	 * The alert dialog shown to display the set of labels to be chosen.
	 * 
	 * @param context
	 *            The interface to the parent class' global information
	 * @param button
	 *            The button object that was pressed
	 * @param type
	 *            The type of detail, (1) Phone number or (2) E-mail address
	 * @param index
	 *            The position of the view along the ListView on which the
	 *            button was pressed
	 * @return The alert dialog object
	 * @see #getLabelDialogTitle(int)
	 * @see #getLabelDialogArrayId(int)
	 * @see #textEntryDialog(Context, Button, int, int, int)
	 */
	private AlertDialog labelChoiceDialog(final Button button, final int kind,
			final int index) {
		return new AlertDialog.Builder(context).setIcon(
				android.R.drawable.arrow_down_float).setTitle(
				getLabelDialogTitle(kind)).setItems(
				getLabelDialogArrayId(kind),
				new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog,
							final int which) {
						if (kind == EditContactInfo.KIND_NUMBER) {
							if (which == EditContactInfo.LABEL_CUSTOM_NUMBER) {
								numberTypeList.remove(index);
								numberTypeList.add(index,
										EditContactInfo.LABEL_KEY_CUSTOM);
								textEntryDialog(button, kind, index).show();
							} else {
								button
										.setText(ViewContactInfo
												.getNumberTypeString(context,
														which + 1));
								numberTypeList.remove(index);
								numberTypeList.add(index, which + 1);
							}
						} else if (kind == EditContactInfo.KIND_EMAIL)
							if (which == EditContactInfo.LABEL_CUSTOM_EMAIL) {
								emailTypeList.remove(index);
								emailTypeList.add(index,
										EditContactInfo.LABEL_KEY_CUSTOM);
								textEntryDialog(button, kind, index).show();
							} else {
								button
										.setText(ViewContactInfo
												.getEmailTypeString(context,
														which + 1));
								emailTypeList.remove(index);
								emailTypeList.add(index, which + 1);
							}

					}
				}).create();
	}

	/**
	 * The alert dialog shown for text input from the user
	 * 
	 * @param context
	 *            The interface to the parent class' global information
	 * @param button
	 *            The button object that was pressed
	 * @param type
	 *            The type of detail, (1) Phone number or (2) E-mail address
	 * @param index
	 *            The position of the view along the ListView on which the
	 *            button was pressed
	 * @param field
	 *            The kind of button that was pressed, (1) labelButton or (2)
	 *            dataButton
	 * @return The alert dialog object
	 * @see #getTextEntryDialogTitle(int, int)
	 */
	private AlertDialog textEntryDialog(final Button button, final int kind,
			final int index) {
		final View textEntryView = layoutInflater.inflate(
				R.layout.dialog_text_entry, null);
		final EditText textEntry = (EditText) textEntryView
				.findViewById(R.id.text_entry);
		textEntry.setText(button.getText().toString());
		return new AlertDialog.Builder(context).setIcon(
				android.R.drawable.arrow_down_float).setTitle(
				R.string.alert_message_custom_label).setView(textEntryView)
				.setPositiveButton(R.string.alert_button_ok,
						new DialogInterface.OnClickListener() {
							public void onClick(final DialogInterface dialog,
									final int whichButton) {
								button.setText(textEntry.getText().toString());
								if (kind == EditContactInfo.KIND_NUMBER) {
									numberLabelList.remove(index);
									numberLabelList.add(index, button.getText()
											.toString());
								} else if (kind == EditContactInfo.KIND_EMAIL) {
									emailLabelList.remove(index);
									emailLabelList.add(index, button.getText()
											.toString());
								}
							}
						}).setNegativeButton(R.string.alert_button_cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(final DialogInterface dialog,
									final int whichButton) {
							}
						}).create();
	}

	/**
	 * The alert dialog shown for invalid image files
	 * 
	 * @return The alert dialog object
	 */
	private static AlertDialog invalidPhotoDialog(final Context context) {
		return new AlertDialog.Builder(context).setIcon(
				android.R.drawable.ic_dialog_alert).setTitle(
				R.string.alert_message_invalid_image).setPositiveButton(
				R.string.alert_button_ok,
				new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog,
							final int whichButton) {
					}
				}).create();
	}

	/**
	 * The alert dialog shown when the name of the contact being saved is the
	 * same as the name of an existing contact.
	 * 
	 * @param id
	 *            The id specifying the form of the alert dialog
	 * @param text
	 *            The string argument used in the alert dialogs
	 * @return The appropriate alert dialog object
	 * @see #save(String, String, List, List, List, List, List, List)
	 */
	private AlertDialog saveAlertDialog(final String name) {
		return new AlertDialog.Builder(context)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle(
						context.getString(R.string.alert_message_name_exists_1)
								+ " "
								+ name
								+ " "
								+ context
										.getString(R.string.alert_message_name_exists_2))
				.setPositiveButton(R.string.alert_button_replace,
						new DialogInterface.OnClickListener() {
							public void onClick(final DialogInterface dialog,
									final int whichButton) {
								context.getContentResolver().delete(
										Contacts.People.CONTENT_URI,
										"_id=" + getId(contactName), null);
								save(contactId, contactName,
										EditContactInfo.contactPhoto,
										numberTypeList, numberLabelList,
										numberDataList, emailTypeList,
										emailLabelList, emailDataList);
							}
						})

				.setNegativeButton(R.string.alert_button_cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(final DialogInterface dialog,
									final int whichButton) {
							}
						}).create();
	}
	
	private class SaveContact extends AsyncTask<Void, Void, Void>{

		@Override
		protected Void doInBackground(Void... params) {
			mHandler.sendEmptyMessage(0);
			
			
			if (getId(contactName).equals("") || getId(contactName).equals(contactId))
				save(contactId, contactName, EditContactInfo.contactPhoto,
						numberTypeList, numberLabelList, numberDataList,
						emailTypeList, emailLabelList, emailDataList);
			else
				saveAlertDialog(contactName).show();

			ContactListWindow.setMustUpdateContactList();
			CallHistoryWindow.setMustUpdateCallHistory();
			SpeedDialWindow.setMustUpdateContactList(true);	
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			 mHandler.sendEmptyMessage(1);
			super.onPostExecute(result);
		}
		
	}
	
}
