package com.neugent.aethervoice.ui;

import java.io.InputStream;
import java.util.regex.Pattern;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Chronometer.OnChronometerTickListener;

import com.neugent.aethervoice.R;

/**
 * @author Amando Jose Quinto II Class that handles the callscreen for pstn
 *         calls.
 */
public class PSTNCallScreen {

	/** The application context. **/
	private final Context mContext;

/*	*//** The LinearLayout for the background of the call screen. **//*
	private LinearLayout mCallBackground;

	*//** The TextView for the type of call. **//*
	private TextView mCallStatus;

	*//** The TextView for the duration of the call. **//*
	private TextView mCallDuration;

	*//** The TextView for the contact called/calling. **//*
	private TextView mCallName;

	*//** The ImageView for the photo of the contact. **//*
	private ImageView mCallImage;*/

	/** The flag that indicates that a contact is found. **/
	public boolean isContactFound;

	/** The flag that indicates that a the mode is incoming call. **/
	private boolean fromIncoming = false;
	
	/*******************************************************************************/
	/**************************** Message for UI update ****************************/
	/*******************************************************************************/

	/** The defined index for the outgoing call message. **/
	public static final int MODE_OUTGOING_CALL = 0;

	/** The defined index for the incoming call message. **/
	public static final int MODE_INCOMING_CALL = 1;

	/** The defined index for the ongoing call message. **/
	public static final int MODE_ONGOING_CALL = 2;

	/** The defined index for the onhold call message. **/
	public static final int MODE_HOLD_CALL = 3;

	/** The defined index for the end call message. **/
	public static final int MODE_END_CALL = 4;

	/*******************************************************************************/
	/********************* Message for caller and image update *********************/
	/*******************************************************************************/
	/** The defined index for the image message. **/
	private static final int HANDLE_IMAGE = 0;

	/** The defined index for the name message. **/
	private static final int HANDLE_NAME = 1;

	/**
	 * Instantiate the PSTNCallScreen.
	 * 
	 * @param context the application context
	 */
	public PSTNCallScreen(final Context context) {
		mContext = context;
	}
	
	/** The view that contains the contact image */
	private View csImageFrame;
	
	/** The name of the contact person */
	private TextView csCaller;
//	private TextView csMainStatus;
	private TextView csStatus;
	private ImageView csIcon;
	private ImageView csImage;
	private Chronometer csDuration;
	
	private int mTextColorConnected;
    private int mTextColorEnded;
    
    public int mCallDuration = 0;

	/**
	 * Retrieves the dialer view with all instantiated contents.
	 * 
	 * @return The view containing for the pstn callscreen
	 */
	public View getPSTNCallScreenView() {
		isContactFound = false;
		final View pstnView = LayoutInflater.from(mContext).inflate(
				R.layout.aethervoice_callscreen, null);
		
		mTextColorConnected = mContext.getResources().getColor(R.color.incall_textConnected);
        mTextColorEnded = mContext.getResources().getColor(R.color.incall_textEnded);
		
		csCaller = (TextView) pstnView.findViewById(R.id.cs_caller);
//		csMainStatus = (TextView) pstnView.findViewById(R.id.cs_status1);
		csStatus = (TextView) pstnView.findViewById(R.id.cs_status);
		csDuration = (Chronometer) pstnView.findViewById(R.id.cs_duration);
		csImage = (ImageView) pstnView.findViewById(R.id.cs_image);
		csIcon = (ImageView) pstnView.findViewById(R.id.cs_icon);
		
		csImageFrame = (View) pstnView.findViewById(R.id.cs_image_bg);
		
		csDuration.setOnChronometerTickListener(new OnChronometerTickListener() {
			
			public void onChronometerTick(Chronometer chronometer) {
				mCallDuration++;
			}
		});
		
		((Button) pstnView.findViewById(R.id.send_video)).setVisibility(View.INVISIBLE);
		
//		mCallBackground = (LinearLayout) pstnView.findViewById(R.id.pstn_bg);
//		mCallStatus = (TextView) pstnView.findViewById(R.id.pstn_call_status);
//		mCallDuration = (TextView) pstnView
//				.findViewById(R.id.pstn_call_duration);
//		mCallName = (TextView) pstnView.findViewById(R.id.pstn_call_info);
//		mCallImage = (ImageView) pstnView.findViewById(R.id.pstn_call_image);
		return pstnView;
	}

	/**
	 * Updates the UI depending on the type of call.
	 * 
	 * @param mode The mode of call
	 * <ul>
	 * <li> MODE_OUTGOING_CALL	- Set the UI to outgoing call type (green)
	 * <li> MODE_INCOMING_CALL	- Set the UI to incoming call type (green)
	 * <li> MODE_ONGOING_CALL	- Set the UI to ongoing call type (green)
	 * <li> MODE_HOLD_CALL		- Set the UI to call on hold call type (yellow)
	 * <li> MODE_END_CALL		- Set the UI to end call type (red)
	 * </ul>
	 */
	public void updateStatusUI(final int mode) {
		switch (mode) { 
		case MODE_OUTGOING_CALL:
			csImageFrame.setBackgroundResource(R.drawable.bg_call_dialling);
//			csMainStatus.setTextColor(mTextColorConnected);// added 12-10-10
            csStatus.setTextColor(mTextColorConnected); //
			
			csStatus.setText(mContext.getString(R.string.card_title_dialing));
//            csMainStatus.setText(mContext.getString(R.string.card_title_dialing));
			csDuration.setVisibility(View.INVISIBLE);
			
//			mCallBackground.setBackgroundResource(R.drawable.bg_call);
//			mCallDuration.setVisibility(View.INVISIBLE);
//			mCallStatus.setText("Dialing");
//			mCallStatus.setTextColor(Color.WHITE);
			break;
		case MODE_INCOMING_CALL:
			csImageFrame.setBackgroundResource(R.drawable.bg_call_photo);
//			csMainStatus.setTextColor(mTextColorConnected);// added 12-10-10
            csStatus.setTextColor(mTextColorConnected); //
            
			csStatus.setText(mContext.getString(R.string.card_title_incoming_call));
//            csMainStatus.setText(mContext.getString(R.string.card_title_incoming_call));
			csDuration.setVisibility(View.INVISIBLE);
			
//			mCallBackground.setBackgroundResource(R.drawable.bg_call);
//			mCallDuration.setVisibility(View.INVISIBLE);
//			mCallStatus.setText("Incoming Call");
//			mCallStatus.setTextColor(Color.WHITE);
			break;
		case MODE_ONGOING_CALL:
			csImageFrame.setBackgroundResource(R.drawable.bg_call_photo);
			
			csIcon.setImageResource(R.drawable.ic_incall_ongoing);
//            csMainStatus.setText(mContext.getString(R.string.card_title_in_progress));
            csStatus.setText(mContext.getString(R.string.card_title_in_progress));
//            csMainStatus.setTextColor(mTextColorConnected);
            csStatus.setTextColor(mTextColorConnected);
            
            //start the chronometer
            csDuration.setTextColor(mTextColorConnected);
            if (!AetherVoice.isOngoing) {
                long time = SystemClock.elapsedRealtime();            
                csDuration.setBase(time);
                csDuration.start();
            }
            
            csDuration.setVisibility(View.VISIBLE);
            
//			mCallBackground.setBackgroundResource(R.drawable.bg_call);
//			mCallDuration.setVisibility(View.VISIBLE);
//			mCallStatus.setText("Call in Progress");
//			mCallDuration.setText(formattedTime(AetherVoice.mCallDuration));
//			mCallStatus.setTextColor(Color.GREEN);
			break;
		case MODE_HOLD_CALL:
			csImageFrame.setBackgroundResource(R.drawable.bg_call_photo);
			
			csStatus.setText(mContext.getString(R.string.card_title_on_hold));
//            csMainStatus.setText(mContext.getString(R.string.card_title_on_hold));
			
//			mCallBackground.setBackgroundResource(R.drawable.bg_hold);
//			mCallDuration.setVisibility(View.VISIBLE);
//			mCallStatus.setText("Call on Hold");
//			mCallStatus.setTextColor(Color.YELLOW);
			break;
		case MODE_END_CALL:
			csImageFrame.setBackgroundResource(R.drawable.bg_call_endcall);
			
			csIcon.setImageResource(R.drawable.ic_incall_end);
//            csMainStatus.setText(mContext.getString(R.string.card_title_call_ended));
            csStatus.setText(mContext.getString(R.string.card_title_call_ended));
//            csMainStatus.setTextColor(mTextColorEnded);
            csStatus.setTextColor(mTextColorEnded);
            csDuration.setTextColor(mTextColorEnded);
            
            //hide the call screen and stop the chronometer
        	csDuration.setVisibility(View.INVISIBLE);
        	csDuration.stop();
        	
//			mCallBackground.setBackgroundResource(R.drawable.bg_end);
//			mCallDuration.setVisibility(View.VISIBLE);
//			mCallStatus.setText("Call Ended");
//			mCallStatus.setTextColor(Color.RED);
			fromIncoming = false;
			break;
		}
	}
	
	public int getCallerLength(){
		return csCaller.length();
	}

	/*
	 * Formats the integer to mm:ss.
	 * 
	 * @param number
	 *            integer to be formated
	 * @return the formatted integer string
	 */
	/*private String formattedTime(final int number) {
		final NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumIntegerDigits(2);
		nf.setMinimumIntegerDigits(2);
		final String numberString = String.valueOf((int) Math
				.floor(number / 60))
				+ " : " + nf.format((int) Math.floor(number % 60));
		return numberString;
	}*/

	/**
	 * Updates the number calling/called.
	 * 
	 * @param number The contact calling/called
	 * @param mode The mode of call
	 * 
	 * @see AetherVoice
	 * @see #getContactInfo(String)
	 */
	public void updateCaller(final String number, final int mode) {
		if (mode == PSTNCallScreen.MODE_INCOMING_CALL)
			fromIncoming = true;
		if (!isContactFound)
			if (number.length() < 1 && !fromIncoming) {
				sendMessage(PSTNCallScreen.HANDLE_NAME, mContext.getString(R.string.pstn_dialing));
				sendMessage(PSTNCallScreen.HANDLE_IMAGE, BitmapFactory.decodeResource(mContext.getResources(),
								R.drawable.anonymous_call));
			} else
				getContactInfo(number);
	}

	/**
	 * Handler for the call UI
	 * <ul>
	 * <li> HANDLE_IMAGE	- change the image of the caller
	 * <li> HANDLE_NAME		- update the name of the caller
	 * </ul>
	 */
	private final Handler mPSTNHandler = new Handler() {
		@Override
		public void handleMessage(final Message msg) {
			switch (msg.what) {
			case HANDLE_IMAGE:
				csImage.setImageBitmap((Bitmap) msg.obj);
				break;
			case HANDLE_NAME:
				csCaller.setText((CharSequence) msg.obj);
				break;
			}
		};
	};

	/**
	 * Constructs and sends a message to the PSTN handler.
	 * 
	 * @param what the message identification
	 * @param obj the object to be passed
	 */
	private void sendMessage(final int what, final Object obj) {
		final Message msg = new Message();
		msg.what = what;
		msg.obj = obj;
		mPSTNHandler.sendMessage(msg);
	}

	/**
	 * Retrieves the contact information from the database.
	 * 
	 * @param number The number that is calling/called
	 */
	private void getContactInfo(final String number) {
		new Thread(new Runnable() {
			public void run() {
				try {
					final Pattern pattern = Pattern.compile("[:punct:]");
					final String phoneNumber = number.replaceAll(pattern.toString(), ""); // replace all string characters
					if (number.length() < 1	|| Long.parseLong(phoneNumber) == -1) {
						sendMessage(PSTNCallScreen.HANDLE_NAME, mContext.getString(R.string.pstn_unknown_caller));
						sendMessage(PSTNCallScreen.HANDLE_IMAGE, BitmapFactory.decodeResource(mContext.getResources(), R.drawable.anonymous_call));
					} else {
						final ContentResolver cr = mContext
								.getContentResolver();

						// encode the phone number and build the filter URI
						final Cursor contactListCursor = cr
								.query(
										Uri
												.withAppendedPath(
														ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
														Uri.encode(phoneNumber)),
										null, null, null, ViewContactInfo
												.getSortOrderString());

						if (contactListCursor.moveToFirst()) {
							isContactFound = true;
							final String name = contactListCursor
									.getString(contactListCursor
											.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
							sendMessage(PSTNCallScreen.HANDLE_NAME, name);
							final Cursor nameCursor = cr
									.query(
											Uri
													.withAppendedPath(
															ContactsContract.Contacts.CONTENT_FILTER_URI,
															name),
											new String[] { "_id" }, null, null,
											null);
							if (nameCursor.moveToFirst()) {
								final long id = nameCursor.getLong(nameCursor
										.getColumnIndex(BaseColumns._ID));
								final InputStream ins = ContactsContract.Contacts
										.openContactPhotoInputStream(
												cr,
												ContentUris
														.withAppendedId(
																ContactsContract.Contacts.CONTENT_URI,
																id));

								if (ins == null)
									sendMessage(PSTNCallScreen.HANDLE_IMAGE, BitmapFactory.decodeResource( mContext.getResources(), R.drawable.anonymous_call));
								else
									sendMessage(PSTNCallScreen.HANDLE_IMAGE, BitmapFactory.decodeStream(ins));

							} else
								sendMessage(PSTNCallScreen.HANDLE_IMAGE, BitmapFactory.decodeResource( mContext.getResources(), R.drawable.anonymous_call));
						} else { // the cursor did not find any matching value
							sendMessage(PSTNCallScreen.HANDLE_NAME, number);
							sendMessage(PSTNCallScreen.HANDLE_IMAGE, BitmapFactory.decodeResource( mContext.getResources(), R.drawable.anonymous_call));
						}
					}
				} catch (final NumberFormatException e) {
					sendMessage(PSTNCallScreen.HANDLE_IMAGE, BitmapFactory
							.decodeResource(mContext.getResources(),
									R.drawable.anonymous_call));
					sendMessage(PSTNCallScreen.HANDLE_NAME, mContext
							.getString(R.string.pstn_invalid_number));
				} catch (final NullPointerException e) {
					sendMessage(PSTNCallScreen.HANDLE_IMAGE, BitmapFactory
							.decodeResource(mContext.getResources(),
									R.drawable.anonymous_call));
					sendMessage(PSTNCallScreen.HANDLE_NAME, mContext
							.getString(R.string.pstn_invalid_number));
				}
			}
		}).start();
	}
}
