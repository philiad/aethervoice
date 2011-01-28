/**
 * @file Dialer.java
 * @brief It contains the Dialer class, the class that contains all the necessary views and modules 
 * 		for instantiating the dialer interface.
 * @author Wyndale Wong
 */
package com.neugent.aethervoice.ui;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Random;
import java.util.Vector;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.util.ByteArrayBuffer;
import org.miscwidgets.widget.Panel;
import org.miscwidgets.widget.Panel.OnPanelListener;
import org.sipdroid.media.RtpStreamReceiver;
import org.sipdroid.sipua.UserAgent;
import org.sipdroid.sipua.phone.Connection;
import org.sipdroid.sipua.ui.Receiver;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.CallLog;
import android.provider.Contacts.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView.OnEditorActionListener;

import com.neugent.aethervoice.R;
import com.neugent.aethervoice.xml.parse.BannerHandler;
import com.neugent.aethervoice.xml.parse.BannerDataset;

/**
 * @class Dialer
 * @brief A class that creates an interactive interface for the user to dial and
 *        manage calls.
 * @author Wyndale Wong
 */
public class Dialer implements OnClickListener, OnTouchListener {
	// ************************************************************** //
	// ************************* ACTIVITY *************************** //
	// ************************************************************** //
	
	/** The application context. **/
	private final Context context;

	/** The audio interface manager. **/
	private final AudioManager audioManager;
	
	/** The thread handler. **/
	private final Handler handler;
	
	// ************************************************************** //
	// *************************** VIEWS **************************** //
	// ************************************************************** //

	/** Displays the dialString for the user. */
	public EditText dialBox;

	/** The call/end button. **/
	public Button btnCall;

	/** The button to enable voip calls. **/
	private Button btnVoip;

	/** The button to enable landline calls. **/
	private Button btnLandline;
	
	/** The seek bar that adjusts the device volume. **/
	public SeekBar volumeBar;
	
	/** The Ad image view. **/
	private ImageView adImage;
	
	/** The mute button **/
	private Button btnMute;
	
	/** The memo open/close button **/
	private Button closePanel;
	
	/** The docked reminder. **/
	private TextView tvReminder;
	
	// ************************************************************** //
	// *************************** FLAGS **************************** //
	// ************************************************************** //
	/** The flag that indicates whether the application is in mute mode. **/
	public boolean isMute = false;

	/** The flag that indicates whether the application is in VoIP mode. **/
	public static boolean isVoip = false;

	/** The flag that indicates whether the application is in Speaker mode. **/
	private static boolean isSpeaker = false;

	/** The flag that indicates that the application is not dialing. **/
	private static boolean isNotDialing = true;

	/** The flag that indicates that the application is in MHS off hook. **/
	public boolean isOnMHS = false;

	/** The flag that indicates that the application in on call mode. **/
	public boolean callStatus = false;
	
	/** The flag that **/
	private boolean runThread = false;
	
	/** Determines if the number should be added to the dial box**/
	private boolean onStartCall;
	
	// ************************************************************** //
	// *************************** OTHERS *************************** //
	// ************************************************************** //
	/** The Ad image randomization engine thread. **/
	public static Thread adEngineThread;

	/** The array of ad image weights. **/
	private int[] adWeights;

	/** The list of ad image drawable id's. **/
	private final int[] adDrawableIds = new int[] { R.drawable.ad_1,
			R.drawable.ad_2, R.drawable.ad_3, R.drawable.ad_4 };
	
	/** The value of the saved volume. **/
	/*private int savedVolume;*/

	/** The last number dialed. **/
	private static String mLastNumber;

	/**
	 * The flag that indicates that the application is starting a call. 
	 * This is for shifting of the call/end button image and functionalities
	 **/
	/** The message to be sent to the mhandler to know that we are dialing in PSTN.*/
	private static final int DIAL_PSTN = 0;

	/** for voip thread*/
	private int timeElapsed = 0;
	
	private Vector<BannerDataset> parsedExampleDataSet;
	
	private int myAdCounter;
	
	private Handler bannerHandler;

	/**
	 * The constructor method of the Dialer class.
	 * 
	 * @param context
	 *            The application context
	 * 
	 * @see #getHandler()
	 */
	public Dialer(final Context context) {
		this.context = context;
		handler = getHandler();
		bannerHandler = getBannerHandler();
		audioManager = (AudioManager) context
				.getSystemService(Context.AUDIO_SERVICE);
		
		isVoip = false;
	}

	/**
	 * Retrieves the dialer view with all instantiated contents.
	 * 
	 * @return The dialer view
	 * 
	 * @see #dial(String)
	 */
	public View getDialerView() {
		final View dialerView = LayoutInflater.from(context).inflate(
				R.layout.dialer, null);

		(tvReminder = (TextView) dialerView.findViewById(R.id.reminder)).setSelected(true);

		dialBox = (EditText) dialerView.findViewById(R.id.dial_box);
		
		/*dialBox.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(final View v, final int keyCode, final KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
					String target = ((EditText) v).getText().toString();
					System.out.println("target length is "+target.length() + " "+target);
					if(target.length() > 0)
						if (Dialer.isVoip)
							dial(dialBox.getText().toString(), context);
						else
							dialPSTN(dialBox.getText().toString());
					else
						return true;
				} 
				return false;
			}
		});*/
		
		dialBox.addTextChangedListener(new TextWatcher(){

			@Override
			public void afterTextChanged(Editable s) {
				timeElapsed = 0;
				if(AetherVoice.pstnCallScreen!=null	&& !AetherVoice.isIncoming && (AetherVoice.isOngoing || AetherVoice.isOutgoing)
						 && AetherVoice.pstnCallScreen.getCallerLength() != dialBox.getText().toString().length()){
					if (/*AetherVoice.pstnCallScreen.mCallDuration < 20 
							&&*/ !AetherVoice.pstnCallScreen.isContactFound)
						AetherVoice.pstnCallScreen .updateCaller(AetherVoice.dialer.dialBox
										.getText().toString(), PSTNCallScreen.MODE_ONGOING_CALL);

					AetherVoice.pstnCallScreen.updateStatusUI(PSTNCallScreen.MODE_ONGOING_CALL);
					
				}
			}


			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) { }


			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) { }
			
		}); 
		
		dialBox.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// 
//				System.out.println("dialbox CLICKED+++++++++++++++");
				timeElapsed = -2;
			}
			
		});
		
		dialBox.setOnEditorActionListener(new OnEditorActionListener() {
			
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				
				if(actionId == EditorInfo.IME_ACTION_DONE){
					String target = v.getText().toString();
//					if(target.length() > 0)
						if(isVoip){
							if (SettingsWindow.isRegistered){
								dial(target, context);
								timeElapsed = 0;
								toggleVoipThread(false);
							} else 
								Toast.makeText(context, R.string.toast_register, Toast.LENGTH_SHORT).show();
						}else{
							dialPSTN(dialBox.getText().toString());
						}
					
				} 
				return false;
			}
		});
		
		final FrameLayout scribbleFrame = (FrameLayout) dialerView
				.findViewById(R.id.panelContent);

		final Scribble scribble = new Scribble(context);
		final ImageButton clearButton = new ImageButton(context);
		clearButton.setBackgroundResource(R.drawable.btn_image_clear_bg);
		clearButton.setOnClickListener(new OnClickListener() {
			public void onClick(final View v) {
				scribble.eraseAll();
			}
		});

		scribbleFrame.addView(scribble, new LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		scribbleFrame.addView(clearButton, new LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

		final Panel panel = (Panel) dialerView.findViewById(R.id.topPanel);
		panel.setOnPanelListener(new OnPanelListener() {
			public void onPanelOpened(final Panel panel) {
				AetherVoice.setScribbling(true);
			}

			public void onPanelClosed(final Panel panel) {
				AetherVoice.setScribbling(false);
			}
		});
		
		closePanel = (Button) panel.getHandle();

		dialerView.findViewById(R.id.one).setOnTouchListener(this);
		dialerView.findViewById(R.id.two).setOnTouchListener(this);
		dialerView.findViewById(R.id.three).setOnTouchListener(this);
		dialerView.findViewById(R.id.four).setOnTouchListener(this);
		dialerView.findViewById(R.id.five).setOnTouchListener(this);
		dialerView.findViewById(R.id.six).setOnTouchListener(this);
		dialerView.findViewById(R.id.seven).setOnTouchListener(this);
		dialerView.findViewById(R.id.eight).setOnTouchListener(this);
		dialerView.findViewById(R.id.nine).setOnTouchListener(this);
		dialerView.findViewById(R.id.zero).setOnTouchListener(this);
		dialerView.findViewById(R.id.star).setOnTouchListener(this);
		dialerView.findViewById(R.id.pound).setOnTouchListener(this);

		dialerView.findViewById(R.id.btn_left).setOnClickListener(this);
		
		 dialerView.findViewById(R.id.btn_dual).setOnClickListener(this);
		dialerView.findViewById(R.id.btn_left).setOnLongClickListener(new OnLongClickListener() {
					public boolean onLongClick(final View v) {
						dialBox.setText("");
						return false;
					}
				});

		btnVoip = (Button) dialerView.findViewById(R.id.btn_voip);
		btnLandline = (Button) dialerView.findViewById(R.id.btn_landline);
		
		btnMute = (Button) dialerView.findViewById(R.id.btn_mute);
		volumeBar = (SeekBar) dialerView.findViewById(R.id.volume_bar);
		adImage = (ImageView) dialerView.findViewById(R.id.ad_image);
		btnCall = (Button) dialerView.findViewById(R.id.btn_dual);

		btnVoip.setOnClickListener(this);
		btnLandline.setOnClickListener(this);
		
		btnMute.setOnClickListener(this);
		dialerView.findViewById(R.id.btn_speaker).setOnClickListener(this);
		btnCall.setOnClickListener(this);

		final float maxVolume = 100;
		final int amaxVol = 100; //95
		final int aminVol = 0; //7

		final float increment = (float) (audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / maxVolume);
		

		final int progress = (int) Math.round(audioManager
				.getStreamVolume(AudioManager.STREAM_MUSIC)
				/ increment);
		
		volumeBar.setProgress(progress < aminVol || progress > amaxVol ? 
				(progress < aminVol ? aminVol : amaxVol) : progress);
		volumeBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			public void onProgressChanged(final SeekBar seekBar,
					final int progress, final boolean fromUser) {

				if (progress > amaxVol)
					seekBar.setProgress(amaxVol);
				else if (progress < aminVol)
					seekBar.setProgress(aminVol);

				/*if (fromUser)
					audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
							(int) Math.round(progress * increment), 0);*/

				/*if (progress > aminVol) {
					isMute = false;
					btnMute.setBackgroundResource(R.drawable.btn_mute_off_bg);
				}*/
			}

			public void onStartTrackingTouch(final SeekBar seekBar) { }

			public void onStopTrackingTouch(final SeekBar seekBar) { 
				Message msg = Message.obtain(null, AetherVoice.MSG_VOLUME_CONTROL);
				msg.arg1 = seekBar.getProgress();
				AetherVoice.sendServiceMessage(msg);
			}
		});
		
		//remove to fix switching problem - Dennis 20110112
//		switchtoVOIP(isVoip);

		adWeights = context.getResources().getIntArray(R.array.ad_weights);
		
		myAdCounter = -1;
		
		adImage.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if (parsedExampleDataSet != null && parsedExampleDataSet.size() > 0) {
					if (myAdCounter >= 0) {
						System.out.println(">>>>>>>>>>>> "+myAdCounter);
						if (parsedExampleDataSet.get(myAdCounter).getPage_url() == null) {
							Toast.makeText(context, "URL not found!", Toast.LENGTH_SHORT).show();
						} else {							
							Intent browserIntent = new Intent(Intent.ACTION_VIEW, 
									Uri.parse(parsedExampleDataSet.get(myAdCounter).getPage_url())); 
									context.startActivity(browserIntent); 
						}
					}
				} 
			}
			
		});
		
		_FakeX509TrustManager.allowAllSSL();

		adEngineThread = getAdEngineThread();
		adEngineThread.start();
		
		

		return dialerView;
	}
	
	public void closePanel(){
		closePanel.performClick();
	}
	
	
	
	public void undockedReminder(boolean flag){
		/*AetherVoice.appIsDocked = flag;*/
		if(flag){
			if(!isVoip && AetherVoice.isCalling){
				btnCall.setBackgroundResource(R.drawable.call);
				try{
					dialHandler.removeMessages(0);
					toggleDialThread(false);
				}catch (NullPointerException e) { }
				
				AetherVoice.sendServiceMessage(AetherVoice.MSG_SPEAKERCALL_OFF);
			}
			btnVoip.performClick();
			btnLandline.setClickable(false);
			tvReminder.setVisibility(View.INVISIBLE);
			tvReminder.setText(R.string.reminderundocked);
		} else{			
			tvReminder.setVisibility(View.VISIBLE);
			btnLandline.setClickable(true);
			tvReminder.setText(R.string.reminderdocked);
			if(!AetherVoice.isCalling){
				btnLandline.performClick();				
			}					
		}
			
	}
	
	public void switchtoVOIP(boolean isVoip){
		if(isVoip) 
			btnVoip.performClick();
		else 
			btnLandline.performClick();					
	}
	
	public void switchtoCall(boolean isInCall){
		callStatus = isInCall;
//		btnCall.setBackgroundResource(isInCall? R.drawable.end : R.drawable.call);
		Message msg = new Message();
		msg.arg1 = isInCall ? 1 : 0;
		msg.what = 1;
		mHandler.sendMessage(msg);
		
		if(isInCall){
			Receiver.engine(context).speaker(AudioManager.MODE_NORMAL);
		}else{
			Receiver.engine(context).speaker(AudioManager.MODE_IN_CALL);
			
			Dialer.isSpeaker = false;
			toggleDialThread(false);
			
		}
	}

	/**
	 * Updates the MHS status and enables/disables the dialer thread.
	 * 
	 * @param enable
	 *            The flag that determines if MHS is on or off.
	 */
	public void updateMHSStatus(final boolean enable) {
		if (isOnMHS == enable)
			return;
		
		isOnMHS = enable;
		
		new Thread(new Runnable(){

			@Override
			public void run() {
				while(dialHandler == null){
					try {
						Thread.sleep(300);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				if(enable){
					dialHandler.sendEmptyMessage(3);
				}else{
					dialHandler.sendEmptyMessage(2);
					
				}
			}
		
		}).start();
		
//		System.out.println("Enable is "+enable);
		
		if (isOnMHS || Dialer.isSpeaker){
			toggleDialThread(true);
//			dialHandler.sendEmptyMessage(3);
		}
		else {
			toggleDialThread(false);
//			dialHandler.sendEmptyMessage(2);
			if (!Dialer.isSpeaker)
				AetherVoice.sendServiceMessage(AetherVoice.MSG_CALL_END);
		}
		
//		if(enable){
//			dialHandler.sendEmptyMessage(3);
//		}else{
//			dialHandler.sendEmptyMessage(2);
//		}

	}

	/**
	 * Defines the lines of code to be executed when a particular button is
	 * pressed.
	 * <ul>
	 * <li>Delete Button - Concatenates the last character of the string in the
	 * dialBox and moves the cursor on the farthest right</li>
	 * <li>Mute Button - Toggles mute</li>
	 * <li>Redial Button - Redial the previous number</li>
	 * <li>End/Call Button - Ends or rejects a call / Dials the displayed string
	 * in the dialBox, answer an incoming call, or toggle hold</li>
	 * </ul>
	 * 
	 * @see #dial(String)
	 */
	public void onClick(final View v) {
		AetherVoice.setIsFinishing(false);
		String dialString = dialBox.getText().toString();
		switch (v.getId()) {
		case R.id.btn_left:
			if(callStatus && !isVoip) return; //do not erase any number during pstn call
			if (dialString.length() > 0)
				dialString = dialString.substring(0, (dialString.length() - 1));
			
			dialBox.setText(dialString);
			dialBox.setSelection(dialBox.length());
			
			if (onStartCall) {
				onStartCall = false;
				Dialer.mLastNumber = "";
			} else if ((Dialer.isSpeaker || isOnMHS))
				Dialer.mLastNumber = dialString;
			
			
			break;
		case R.id.btn_voip:
			if(callStatus){
//				Toast.makeText(context, "Please end call first.", Toast.LENGTH_SHORT).show();
			}else{
				btnVoip.setBackgroundResource(R.drawable.btn_voip_on_bg);
				btnLandline.setBackgroundResource(R.drawable.btn_pstn_off_bg);
				Dialer.isVoip = true;
			}
			break;
		case R.id.btn_landline:
			if(callStatus){
//				Toast.makeText(context, "Please end call first.", Toast.LENGTH_SHORT).show();
			}else{
				btnVoip.setBackgroundResource(R.drawable.btn_voip_off_bg);
				btnLandline.setBackgroundResource(R.drawable.btn_pstn_on_bg);
				Dialer.isVoip = false;
			}
			break;
		case R.id.btn_mute:
			/*final float incrementation = (float) (audioManager
			.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 100.0f);*/
			
			/*if (Receiver.call_state == UserAgent.UA_STATE_INCALL
					|| Receiver.call_state == UserAgent.UA_STATE_HOLD)
				Receiver.engine(context).togglemute();*/
			
			if(RtpStreamReceiver.speakermode == AudioManager.MODE_NORMAL || (!isVoip&&isSpeaker)){//XXX:edited. will mute if voip is on call or pstn is ongoing
				if (isMute) {
					isMute = false;
					
					((Button) v).setBackgroundResource(R.drawable.btn_mute_off_bg);
					
					/*audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
							Math.round(savedVolume * incrementation), 0);
					volumeBar.setProgress(savedVolume);*/
					
					audioManager.setMicrophoneMute(false);
					
					if(isVoip)
						Receiver.engine(context).togglemute();
					else
						AetherVoice.sendServiceMessage(AetherVoice.MSG_MUTE_OFF);
	
				} else {
					isMute = true;
					
					((Button) v).setBackgroundResource(R.drawable.btn_mute_on_bg);
					
					/*savedVolume = volumeBar.getProgress();
					
					audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
					volumeBar.setProgress(0);*/
					
					audioManager.setMicrophoneMute(true);
	
					if(isVoip)
						Receiver.engine(context).togglemute();
					else
						AetherVoice.sendServiceMessage(AetherVoice.MSG_MUTE_ON);	
				}
			}
			break;
		case R.id.btn_speaker: // redial
//			System.out.println(Dialer.mLastNumber+"=========================== REDIAL");
			if (Dialer.mLastNumber != null && Dialer.mLastNumber.length() > 0){
				dialBox.setText(dialString);
				dialBox.setSelection(dialBox.length());
				if (Dialer.isVoip){
					if (SettingsWindow.isRegistered) 
						dial(Dialer.mLastNumber, context);
					else 
						Toast.makeText(context, R.string.toast_register, Toast.LENGTH_SHORT).show();
				}
				else
					dialPSTN(Dialer.mLastNumber);
				runThread = false;
			}
			break;
		case R.id.btn_dual:
			v.setClickable(false);
			if (callStatus) {
				unMute(); //added by PJ
				callStatus = false;
				toggleVoipThread(false);
				btnCall.setBackgroundResource(R.drawable.call);

				if (Dialer.isVoip) {
					Dialer.isSpeaker = false;
//					AetherVoice.hideCallFrame(false);
					dialString = "";
					dialBox.setText("");
					dialBox.setSelection(dialBox.length());
					
					// for voip call
					if (Receiver.call_state == UserAgent.UA_STATE_INCOMING_CALL)
						AetherVoice.mHandler.sendEmptyMessage(CallHandler.MSG_REJECT_CALL);
					else if (Receiver.call_state == UserAgent.UA_STATE_INCALL
							|| Receiver.call_state == UserAgent.UA_STATE_HOLD
							|| Receiver.call_state == UserAgent.UA_STATE_OUTGOING_CALL)
						AetherVoice.mHandler.sendEmptyMessage(CallHandler.MSG_END_CALL);
					
					// for voip speaker
					if (RtpStreamReceiver.speakermode == AudioManager.MODE_IN_CALL)
						Receiver.engine(context).speaker(
								AudioManager.MODE_NORMAL);
					
				} else {
					if (AetherVoice.isOngoing || AetherVoice.isOnHold) {
						endCall(false, true);

						if (!isOnMHS)
							if (AetherVoice.isIncoming)
								Connection.addCall(null, context,
										AetherVoice.mCallNumber, false,
										CallLog.Calls.INCOMING_TYPE,
										AetherVoice.mstartCallTime,
										AetherVoice.pstnCallScreen.mCallDuration);
							else if (dialString.length() > 1)
								Connection.addCall(null, context, dialString,
										false, CallLog.Calls.OUTGOING_TYPE,
										AetherVoice.mstartCallTime,
										AetherVoice.pstnCallScreen.mCallDuration);
					} else if (AetherVoice.isIncoming)
						endCall(true, false);
					else if (AetherVoice.isOutgoing) {
						/** CURRENTLY NOT BEING USED **/
						endCall(false, true);

						Connection.addCall(null, context, dialString, false,
								CallLog.Calls.OUTGOING_TYPE,
								AetherVoice.mstartCallTime, 0);
					}

					if (!isOnMHS) {
						dialString = "";
						
						//this should be handled by end call
						/*dialBox.setText("");
						dialBox.setSelection(dialBox.length());*/
					}
					Dialer.isNotDialing = true;

				}

				mToneGenerator.stopTone();
				
				System.out.println("DialerService Sending Empty Message of Handsfree off? "+isOnMHS); //XXX
				
				if(isOnMHS){
					System.out.println("DialerService Sending of Handsfree off"); //XXX
					dialHandler.sendEmptyMessageDelayed(3, 500); //msg_handsfreeoff
				}else{
					System.out.println("DialerService Stoping DialThread"); //XXX
					toggleDialThread(false);
				}

			} else {
				callStatus = true;

				btnCall.setBackgroundResource(R.drawable.end);
				
				if (Dialer.isVoip) {
					if(SettingsWindow.isRegistered){
						Dialer.isSpeaker = true;

						
//						dialString = ""; 
//						dialBox.setText("");
//						dialBox.setSelection(dialBox.length());
						
						// for voip call
						if (Receiver.call_state == UserAgent.UA_STATE_INCOMING_CALL) {
							final Message m = new Message();
							m.what = CallHandler.MSG_INCALL_ANSWER_SPEAKER;
							m.obj = context;
							AetherVoice.mHandler.sendMessage(m);

							// for VOIP speaker
							if (RtpStreamReceiver.speakermode == AudioManager.MODE_NORMAL)
								Receiver.engine(context).speaker(AudioManager.MODE_IN_CALL);
						} else if (Receiver.call_state == UserAgent.UA_STATE_IDLE) {
							
							/** 
							 * call thread here
							 * w8 for input
							 * autodial after 10idle seconds*/
							// XXX: changed
							if (!AetherVoice.isCalling){
								if (dialString.length() > 0)
									dial(dialString, context);
								else{
									toggleVoipThread(true);
									voipDialHandler = new Handler(){
										@Override
										public void handleMessage(Message msg){
											if (msg.what == 0){ 
													((InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(dialBox.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
		//											System.out.println("Dialing+-=-=-=-=-=-=-=-=-=-+ " + dialBox.getText());
													dial(dialBox.getText().toString(), context);
													
													toggleVoipThread(false);
											}
										}
									};
								}
							}
							
							// for VOIP speaker
							if (RtpStreamReceiver.speakermode == AudioManager.MODE_NORMAL)
								Receiver.engine(context).speaker(AudioManager.MODE_IN_CALL);
							
						} else if (Receiver.call_state == UserAgent.UA_STATE_INCALL || Receiver.call_state == UserAgent.UA_STATE_HOLD)
							Receiver.engine(context).togglehold();
					}else{
						Toast.makeText(context,	context.getString(R.string.toast_register), 3000).show();
						dialBox.setText("");
						switchtoCall(false);
					}
//					} else {
//						Toast.makeText(context, R.string.toast_register, Toast.LENGTH_LONG).show();
//						callStatus = false;
//						btnCall.setBackgroundResource(R.drawable.call);
//						mToneGenerator.stopTone();
//					}
				} else if (AetherVoice.isIncoming && !AetherVoice.isOngoing) { // is on call session

					try {
						Thread.sleep(300);
					} catch (final InterruptedException e) {
						e.printStackTrace();
					}

					// AetherVoice.sendServiceMessage(AetherVoice.MSG_CALL_ANSWER);
					// try {
					// Thread.sleep(300);
					// } catch (InterruptedException e) {
					// e.printStackTrace();
					// }
					//						
					// AetherVoice.sendServiceMessage(AetherVoice.MSG_SPEAKER_ON);

					AetherVoice.sendServiceMessage(AetherVoice.MSG_SPEAKERCALL_ON);
					
					Message msg = Message.obtain(null, AetherVoice.MSG_VOLUME_CONTROL);
					msg.arg1 = volumeBar.getProgress();
					AetherVoice.sendServiceMessage(msg);

					Dialer.isSpeaker = true;

					toggleDialThread(true);
					
//					ampSpeaker(volumeBar.getProgress());

				} else if (AetherVoice.isOngoing) {
					if (!Dialer.isSpeaker) {
						// AetherVoice.sendServiceMessage(AetherVoice.MSG_CALL_ANSWER);
						//							
						// try {
						// Thread.sleep(300);
						// } catch (InterruptedException e) {
						// e.printStackTrace();
						// }
						//							
						// AetherVoice.sendServiceMessage(AetherVoice.MSG_SPEAKER_ON);

						AetherVoice.sendServiceMessage(AetherVoice.MSG_SPEAKERCALL_ON);
						
						Message msg = Message.obtain(null, AetherVoice.MSG_VOLUME_CONTROL);
						msg.arg1 = volumeBar.getProgress();
						AetherVoice.sendServiceMessage(msg);

						Dialer.isSpeaker = true;
						
//						ampSpeaker(volumeBar.getProgress());
					} else
						AetherVoice.sendServiceMessage(AetherVoice.MSG_CALL_HOLD);
				} else if (isOnMHS || !AetherVoice.isCalling
						&& !AetherVoice.isOutgoing && !AetherVoice.isIncoming) {
					/*if (dialString.length() > 0){
						// mLastNumber = dialString;
						// dialPSTN(dialString);
						dialBox.setText("");
						dialBox.setSelection(dialBox.length());
					}*/
					
					
//					try {
//						Thread.sleep(300);
//					} catch (final InterruptedException e) {
//						e.printStackTrace();
//					}

					// AetherVoice.sendServiceMessage(AetherVoice.MSG_CALL_ANSWER);
					//						
					// try {
					// Thread.sleep(300);
					// } catch (InterruptedException e) {
					// e.printStackTrace();
					// }
					//													
					// AetherVoice.sendServiceMessage(AetherVoice.MSG_SPEAKER_ON);

					AetherVoice.sendServiceMessage(AetherVoice.MSG_SPEAKERCALL_ON);
					
					Message msg = Message.obtain(null, AetherVoice.MSG_VOLUME_CONTROL);
					msg.arg1 = volumeBar.getProgress();
					AetherVoice.sendServiceMessage(msg);

					Dialer.isSpeaker = true;

					try {
						Thread.sleep(300);
					} catch (final InterruptedException e) {
						e.printStackTrace();
					}
					
//					ampSpeaker(volumeBar.getProgress());
				}
				
				if ((isVoip && SettingsWindow.isRegistered) || !isVoip) {
					
						toggleDialThread(true);
						
						//send a message upon initialization of the handler
						new Thread(new Runnable() {
							
							@Override
							public void run() {
								while(dialHandler == null){
									try{
										Thread.sleep(100);
									}catch(InterruptedException e){}
								}
								dialHandler.sendEmptyMessageDelayed(2, 500); //msg_handsfreeon
							}
						}).start();
					
				}
//
//				if (isVoip){
//					if (SettingsWindow.isRegistered){
//					toggleDialThread(true);
//					
//					//send a message upon initialization of the handler
//					new Thread(new Runnable() {
//						
//						@Override
//						public void run() {
//							while(dialHandler == null){
//								try{
//									Thread.sleep(100);
//								}catch(InterruptedException e){}
//							}
//							dialHandler.sendEmptyMessageDelayed(2, 500); //msg_handsfreeon
//						}
//					}).start();
//					}
//					
//				} else {
//					toggleDialThread(true);
//					
//					//send a message upon initialization of the handler
//					new Thread(new Runnable() {
//						
//						@Override
//						public void run() {
//							while(dialHandler == null){
//								try{
//									Thread.sleep(100);
//								}catch(InterruptedException e){}
//							}
//							dialHandler.sendEmptyMessageDelayed(2, 500); //msg_handsfreeon
//						}
//					}).start();
//					}
				}
			break;
		}

//		dialBox.setText(dialString);
//		dialBox.setSelection(dialBox.length());
		
		v.setClickable(true);
	}
	
	/*private boolean isRampingDone = false;
	
	public void disableRamping(){
		isRampingDone = false;
	}
	
	private void ampSpeaker(final int volumemax){
		final int incrementation = volumemax / 6;
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				for(int i = 0; i<=volumemax;){
					Message msg = Message.obtain(null, AetherVoice.MSG_VOLUME_CONTROL);
					msg.arg1 = i;
					AetherVoice.sendServiceMessage(msg);
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) { 
						break;
					}
					i+=incrementation;
				}
				isRampingDone = true;
			}
		}).start();
	}*/

	/**
	 * @author Amando Jose Quinto II and Byron Cortez
	 * 
	 * @param incoming flag if there is an incoming call
	 * @param speaker_off flag if you want to turn off the speaker
	 */
	public void endCall(final boolean incoming, final boolean speaker_off) {
		// btnCall.setClickable(false);
		new Thread(new Runnable() {
			public void run() {
				if (incoming) {
					AetherVoice.sendServiceMessage(AetherVoice.MSG_CALL_ANSWER);
					try {
						Thread.sleep(800);
					} catch (final InterruptedException e) {
						e.printStackTrace();
					}
				}

				// Log.i("AetherVoice", "Turning Speaker off");
				// if (speaker_off) { //TURN OFF SPEAKER
				// AetherVoice.sendServiceMessage(AetherVoice.MSG_SPEAKER_OFF);

				// }
				// try {
				// Thread.sleep(100);
				// } catch (InterruptedException e) {
				// e.printStackTrace();
				// }
				//				
				// AetherVoice.sendServiceMessage(AetherVoice.MSG_CALL_END);
				if (speaker_off) {
					Dialer.isSpeaker = false;
					AetherVoice.sendServiceMessage(AetherVoice.MSG_SPEAKERCALL_OFF);
				} else
					AetherVoice.sendServiceMessage(AetherVoice.MSG_CALL_END);

				if(!isOnMHS){
					toggleDialThread(false);
					 mHandler.sendMessage(Message.obtain(null, 1, 0, 0));
				}
			}
		}).start();
	}

	
	
	//added by PJ
	public void unMute(){
		Log.d("AetherVoice", "++++ENDING CALL+++");
		if(isMute){
			isMute = false;
			btnMute.setBackgroundResource(R.drawable.btn_mute_off_bg);
			audioManager.setMicrophoneMute(false); //12-16-2010
			/*final float incrementation = (float) (audioManager
					.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 100.0f);
			audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
					Math.round(savedVolume * incrementation), 0);
			volumeBar.setProgress(savedVolume);*/
//			audioManager.setMicrophoneMute(false);
			if(isVoip)
				Receiver.engine(context).togglemute();
			else
				AetherVoice.sendServiceMessage(AetherVoice.MSG_MUTE_OFF);	
		}
	}
	
	
	/**
	 * Handles the view for the call ui
	 */
	public Handler mHandler = new Handler() {
		@Override
		public void handleMessage(final Message msg) {
			
			switch (msg.what) {
			case DIAL_PSTN:
				btnCall.setClickable(true);

				dialBox.setText((CharSequence) msg.obj);
				if (msg.arg1 == 0)
					btnCall.setBackgroundResource(R.drawable.end);
					callStatus = false;
				break;
			
			case 1:
				btnCall.setBackgroundResource(msg.arg1 == 1 ? R.drawable.end : R.drawable.call);
				if (msg.arg1==0){
					dialBox.setText("");
					callStatus = false;
				}
					
//				}else {//XXX: added
//					if(isMute){
//						isMute = false;
//						btnMute.setBackgroundResource(R.drawable.btn_mute_off_bg);
//						final float incrementation = (float) (audioManager
//								.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 100.0f);
//						audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
//								Math.round(savedVolume * incrementation), 0);
//						volumeBar.setProgress(savedVolume);
//						if(isVoip)
//							Receiver.engine(context).togglemute();
//						else
//							AetherVoice.sendServiceMessage(AetherVoice.MSG_MUTE_OFF);	
//						}
////					AetherVoice.hideCallFrame(false);
//					}
				break;
			case 2:
				unMute();
				break;
			}
			super.handleMessage(msg);
		}

		
	};

	/**
	 * Dials the number
	 * 
	 * @param the target number to be dialed
	 */
	public void dialPSTN(final String target) {
		dialBox.setText(target);
		Dialer.mLastNumber = target;
		toggleDialThread(true);
		new Thread(new Runnable() {
			public void run() {
				onStartCall = true;
				

				if (!isOnMHS) {
					// //enable speaker
					// AetherVoice.sendServiceMessage(AetherVoice.MSG_SPEAKER_ON);

					//
					// try {
					// Thread.sleep(300);
					// } catch (InterruptedException e) { }
					//					
					// //enable ehs
					// AetherVoice.sendServiceMessage(AetherVoice.MSG_CALL_ANSWER);

					Dialer.isSpeaker = true;
					if(!callStatus)
						AetherVoice.sendServiceMessage(AetherVoice.MSG_SPEAKERCALL_ON);
					
					AetherVoice.sendServiceMessage(Message.obtain(null, AetherVoice.MSG_VOLUME_CONTROL, volumeBar.getProgress(), -1));
					
					new Thread(new Runnable() {
						
						@Override
						public void run() {
							while(dialHandler == null){
								try{
									Thread.sleep(100);
								}catch(InterruptedException e){}
							}
							dialHandler.sendEmptyMessageDelayed(2, 500); //msg_handsfreeon
						}
					}).start();

					try {
						Thread.sleep(2000);
					} catch (final InterruptedException e) {
					}
					
//					ampSpeaker(volumeBar.getProgress());
					
					callStatus = true;
					
					final Message msg = new Message();
					msg.obj = target;
					msg.what = Dialer.DIAL_PSTN;

					if (isOnMHS)
						msg.arg1 = 1;
					else
						msg.arg1 = 0;

					mHandler.sendMessage(msg);
				}

				Dialer.isNotDialing = false;
				boolean first = true;
				final char[] digits = target.toCharArray();
				for (final char a : digits) {
					int digit = Character.getNumericValue(a);
					if (digit == -1)
						if (a == '*')
							digit = 10;
						else if (a == '#')
							digit = 11;
						else
							continue; // skip unwanted numbers
					if (Dialer.isNotDialing)
						break;
					sendDigit(digit);

					if (first) {
						first = false;
						try {
							Thread.sleep(700);
						} catch (final InterruptedException e) {
						}
					}
				}
				Dialer.isNotDialing = true;
			}
		}).start();
	}

	/**
	 * Dials the number
	 * 
	 * @param target the number to be dialed
	 */
	public void dial(final String target, Context context) {
		Dialer.mLastNumber = target; //added by PJ 12-10-10
		if (target.length() == 0){ 
			switchtoCall(false);
			Toast.makeText(context, "No number to dial.", Toast.LENGTH_SHORT).show();
		}
		else
			if (SettingsWindow.isRegistered  && Receiver.engine(context).isRegistered()){
				Receiver.engine(context).call(target, true);
				switchtoCall(true);
			} else{
				Toast.makeText(context,	context.getString(R.string.toast_register), Toast.LENGTH_LONG).show();
				dialBox.setText("");
				switchtoCall(false);
			}

	}
	
	
    private void parseBanners() {
        try {
            /* Create a URL we want to load some xml-data from. */
            URL url = new URL("https://www.phoenixph.com/TelpadDialerService/TelpadService/AdBanner/Get");
            
            /* Get a SAXParser from the SAXPArserFactory. */
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();

            /* Get the XMLReader of the SAXParser we created. */
            XMLReader xr = sp.getXMLReader();
            /* Create a new ContentHandler and apply it to the XML-Reader*/
            
            BannerHandler myExampleHandler = new BannerHandler();
            xr.setContentHandler(myExampleHandler);
           
            /* Parse the xml-data from our URL. */
            xr.parse(new InputSource(url.openStream()));
            /* Parsing has finished. */
            
            parsedExampleDataSet = myExampleHandler.getParsedData();
            
            for (int x=0; x<parsedExampleDataSet.size(); x++) {
                System.out.println("Banner ID: "+parsedExampleDataSet.get(x).getBannerid());
                System.out.println("Description: "+parsedExampleDataSet.get(x).getDescription());
                System.out.println("Filename: "+parsedExampleDataSet.get(x).getFilename());
                System.out.println("Image Url: "+parsedExampleDataSet.get(x).getImage_url());
                System.out.println("Page URL: "+parsedExampleDataSet.get(x).getPage_url());
            }
           
        } catch (Exception e) {
            /* Display any Error to the GUI. */
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
	public void createTempFolder (){
		try{
			File f =  new File("/data/data/com.neugent.aethervoice/banners");
			if(f.mkdir())
				System.out.println("Directory Created>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<");
			else
				System.out.println("Directory is not created<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<FAIL");
		}catch(Exception e){
				e.fillInStackTrace();
		}
	}
	
	
	 public boolean DownloadFromUrl(String imageURL, String fileName) {  //this is the downloader method
         try {
                 URL url = new URL(imageURL); //you can write here any link
                 File file = new File(fileName);

                 long startTime = System.currentTimeMillis();
                 Log.d("ImageManager", "download begining");
                 Log.d("ImageManager", "download url:" + url);
                 Log.d("ImageManager", "downloaded file name:" + fileName);
                 /* Open a connection to that URL. */              
                 URLConnection ucon = url.openConnection();

                 /*
                  * Define InputStreams to read from the URLConnection.
                  */                 
                 InputStream is = ucon.getInputStream();
                 BufferedInputStream bis = new BufferedInputStream(is);

                 /*
                  * Read bytes to the Buffer until there is nothing more to read(-1).
                  */
                 ByteArrayBuffer baf = new ByteArrayBuffer(50);
                 int current = 0;
                 while ((current = bis.read()) != -1) {
                         baf.append((byte) current);
                 }

                 /* Convert the Bytes read to a String. */
                 FileOutputStream fos = new FileOutputStream(file);
                 fos.write(baf.toByteArray());
                 fos.close();
                 Log.d("ImageManager", "download ready in"
                                 + ((System.currentTimeMillis() - startTime) / 1000)
                                 + " sec");
                 
                 return true;

         } catch (IOException e) {
                 Log.d("ImageManager", "Error: " + e);
                 return false;
         } catch (Exception e) {
        	 	Log.d("ImageManager", "Error: " + e);
        	 	return false;
         }

 }
	 
	public boolean checkInternetConnection(){			
		try {
			HttpURLConnection connection;
			connection = (HttpURLConnection) new URL("http://www.phoenixph.com/").openConnection();
			connection.setRequestMethod("HEAD");
			int responseCode = connection.getResponseCode();
			if (responseCode != 200) {
				return false;
			} 				
				return true;
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}

	}
		
	/**
	 * Creates the Ad image randomization engine.
	 * 
	 * @return The Thread object
	 * 
	 * @see #refreshAdImage()
	 */
	private Thread getAdEngineThread() {
		return new Thread(new Runnable() {
			public void run() {			
				handler.sendEmptyMessage(refreshAdImage());
				
				//check wifi
		    	final ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		    	final android.net.NetworkInfo wifi = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		    	
		    	createTempFolder();
		    	
		    	// if On		    	
		    	if(wifi.isConnected() && checkInternetConnection()) {		    		
		    		parseBanners();
		    	}
				
				while (AetherVoice.bannerFlag) {
					if (parsedExampleDataSet != null && parsedExampleDataSet.size() > 0) {
						if (wifi.isConnected()) {
							bannerHandler.sendEmptyMessage(randomImage());
						}
						
					} else {
						handler.sendEmptyMessage(refreshAdImage());
					}					
					try {
						Thread.sleep(10000);
					} catch (final InterruptedException e) {
						e.printStackTrace();
					}

				}				
				
			}
		});
	}

	/**
	 * The randomization method.
	 * 
	 * @return The array position of the randomly selected Ad image
	 */
	private int refreshAdImage() {
		int weightSum = 0;
		int i = 0;
		for (i = 0; i < adDrawableIds.length; i++)
			weightSum = weightSum + adWeights[i];

		final int randNum = (new Random().nextInt(weightSum));
		int n = 0;
		for (i = 0; i < adDrawableIds.length; i++) {
			n = n + adWeights[i];
			if (n >= randNum)
				return i;
		}
		return 0;
	}
	
	
	Bitmap bm;
	
	private int randomImage() {
		int start = 0;
	    int end = parsedExampleDataSet.size() - 1;
		
		Random number = new Random();
		
		int randomNumber = showRandomInteger(start, end, number);
		
		System.out.println("RAndom>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> "+randomNumber);
		bm = null;
		if (new File("/data/data/com.neugent.aethervoice/banners/"+parsedExampleDataSet.get(randomNumber).getFilename()).canRead()) {
			myAdCounter = randomNumber;
			bm = BitmapFactory.decodeFile("/data/data/com.neugent.aethervoice/banners/"+parsedExampleDataSet.get(randomNumber).getFilename());	
		} else {
			if (DownloadFromUrl("https://www.pldtathome.com/images/telpadbanners/"+parsedExampleDataSet.get(randomNumber).getFilename(), "/data/data/com.neugent.aethervoice/banners/"+parsedExampleDataSet.get(randomNumber).getFilename())) {
				myAdCounter = randomNumber;
				bm = BitmapFactory.decodeFile("/data/data/com.neugent.aethervoice/banners/"+parsedExampleDataSet.get(randomNumber).getFilename());
			} else {
				handler.sendEmptyMessage(refreshAdImage());
			}
		}
		
		return randomNumber;			
	}
	
	
	private int showRandomInteger(int aStart, int aEnd, Random aRandom){
	    if ( aStart > aEnd ) {
	      throw new IllegalArgumentException("Start cannot exceed End.");
	    }
	    //get the range, casting to long to avoid overflow problems
	    long range = (long)aEnd - (long)aStart + 1;
	    // compute a fraction of the range, 0 <= frac < range
	    long fraction = (long)(range * aRandom.nextDouble());
	    int randomNumber =  (int)(fraction + aStart);    
	    return randomNumber;
	}
	

	/**
	 * Creates the handler for the thread, which refreshes the image being displayed.
	 * 
	 * @return The Handler object
	 */
	private Handler getHandler() {
		return new Handler() {
			@Override
			public void handleMessage(final Message msg) {
				adImage.setBackgroundResource(adDrawableIds[msg.what]);
			}
		};
	}
	
	private Handler getBannerHandler() {
		return new Handler() {
			@Override
			public void handleMessage(final Message msg) {				
				adImage.setImageBitmap(bm);
				bm = null;
			}
		};
	}
	
	
	public ToneGenerator mToneGenerator = new ToneGenerator(
			AudioManager.STREAM_DTMF, ToneGenerator.MAX_VOLUME);

	/**
	 * Touch events for the number pad
	 * 
	 * @see android.view.View.OnTouchListener#onTouch(android.view.View, android.view.MotionEvent)
	 */
	public boolean onTouch(final View v, final MotionEvent event) {
		AetherVoice.setIsFinishing(false);
		if (!isOnMHS && !isSpeaker /*&& !isRampingDone*/)
			return false;
		if(RtpStreamReceiver.speakermode == AudioManager.MODE_NORMAL) return false; //added by PJ
 
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
//			if (!isMute)
//				switch (v.getId()) {
//				case R.id.one:
//					mToneGenerator.startTone(ToneGenerator.TONE_DTMF_1);
//					break;
//				case R.id.two:
//					mToneGenerator.startTone(ToneGenerator.TONE_DTMF_2);
//					break;
//				case R.id.three:
//					mToneGenerator.startTone(ToneGenerator.TONE_DTMF_3);
//					break;
//				case R.id.four:
//					mToneGenerator.startTone(ToneGenerator.TONE_DTMF_4);
//					break;
//				case R.id.five:
//					mToneGenerator.startTone(ToneGenerator.TONE_DTMF_5);
//					break;
//				case R.id.six:
//					mToneGenerator.startTone(ToneGenerator.TONE_DTMF_6);
//					break;
//				case R.id.seven:
//					mToneGenerator.startTone(ToneGenerator.TONE_DTMF_7);
//					break;
//				case R.id.eight:
//					mToneGenerator.startTone(ToneGenerator.TONE_DTMF_8);
//					break;
//				case R.id.nine:
//					mToneGenerator.startTone(ToneGenerator.TONE_DTMF_9);
//					break;
//				case R.id.zero:
//					mToneGenerator.startTone(ToneGenerator.TONE_DTMF_0);
//					break;
//				case R.id.star:
//					mToneGenerator.startTone(ToneGenerator.TONE_DTMF_S);
//					break;
//				case R.id.pound:
//					mToneGenerator.startTone(ToneGenerator.TONE_DTMF_P);
//					break;
//				}
		} else if (event.getAction() == MotionEvent.ACTION_UP) {

//			if (!isMute)
//				mToneGenerator.stopTone();// STOP THE TONE FROM PLAYING

			String dialString = dialBox.getText().toString();
			final Message msg = new Message();
			msg.what = 1;
			switch (v.getId()) {
			case R.id.one:
				dialString = dialString + "1";
				if(isVoip){
					timeElapsed = 0;

				}
				else {
				if (dialThread != null)
					msg.arg1 = 1;
				else
					sendDigit(1);}
				break;
			case R.id.two:
				dialString = dialString + "2";
				if(isVoip){
					timeElapsed = 0;

					}
					else {
				if (dialThread != null)
					msg.arg1 = 2;
				else
					sendDigit(2);}
				break;
			case R.id.three:
				dialString = dialString + "3";
				if(isVoip){
					timeElapsed = 0;

					}
					else {
				if (dialThread != null)
					msg.arg1 = 3;
				else
					sendDigit(3);}
				break;
			case R.id.four:
				dialString = dialString + "4";
				if(isVoip){
					timeElapsed = 0;

					}
					else {
				if (dialThread != null)
					msg.arg1 = 4;
				else
					sendDigit(4);}
				break;
			case R.id.five:
				dialString = dialString + "5";
				if(isVoip){
					timeElapsed = 0;

					}
					else {
				if (dialThread != null)
					msg.arg1 = 5;
				else
					sendDigit(5);}
				break;
			case R.id.six:
				dialString = dialString + "6";
				if(isVoip){
					timeElapsed = 0;

					}
					else {
				if (dialThread != null)
					msg.arg1 = 6;
				else
					sendDigit(6);}
				break;
			case R.id.seven:
				dialString = dialString + "7";
				if(isVoip){
					timeElapsed = 0;

					}
					else {
						if (dialThread != null)
							msg.arg1 = 7;
						else
							sendDigit(7);}
				break;
			case R.id.eight:
				dialString = dialString + "8";
				if(isVoip){
					timeElapsed = 0;

					}
					else {
						if (dialThread != null)
							msg.arg1 = 8;
						else
							sendDigit(8);
						}
				break;
			case R.id.nine:
				dialString = dialString + "9";
				if(isVoip){
					timeElapsed = 0;
					}
					else {
						if (dialThread != null)
							msg.arg1 = 9;
						else
							sendDigit(9);}
				break;
			case R.id.zero:
				dialString = dialString + "0";
				if(isVoip){
					timeElapsed = 0;

					}
					else {
				if (dialThread != null)
					msg.arg1 = 0;
				else
					sendDigit(0);}
				break;
			case R.id.star:
				dialString = dialString + "*";
				if(isVoip){
					timeElapsed = 0;
				} else {
					if (dialThread != null)
						msg.arg1 = 10;
					else
						sendDigit(10);
				}
				break;
			case R.id.pound:
				dialString = dialString + "#";
				if(isVoip){
					timeElapsed = 0;
					}
					else {
				if (dialThread != null)
					msg.arg1 = 11;
				else
					sendDigit(11);}
				break;
			}

			if (dialThread != null) {
				dialHandler.removeMessages(0);
				dialHandler.sendMessage(msg);
			}

			if (onStartCall) {
				onStartCall = false;
				Dialer.mLastNumber = "";
			} else if ((Dialer.isSpeaker || isOnMHS))
				Dialer.mLastNumber = dialString;

			dialBox.setText(dialString);
			dialBox.setSelection(dialString.length());
		}
		return false;
	}

	/**
	 * Sends data to the serial 
	 * 
	 * @param number The data to be sent to the serial
	 */
	private void sendDigit(final int number) {
		if ((isOnMHS || Dialer.isSpeaker) && !Dialer.isVoip) {

			if(!AetherVoice.isIncoming)
				AetherVoice.sendServiceMessage(AetherVoice.MSG_OUTGOING_CALL); // this is a work around ... the context of the dialer can't show the views

			AetherVoice.sendServiceMessage(number);
			try {
				Thread.sleep(200);
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
			// if (isNotDialing)
			// break;
			AetherVoice.sendServiceMessage(12);
			try {
				Thread.sleep(200);
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}

		}

		// System.out.println("****************************************************************************");
		// System.out.println("isVoip= "+isVoip+", isOnMHS= "+isOnMHS+", isSpeaker= "+isSpeaker+", number= "+number);
		// System.out.println("****************************************************************************");
	}

	public void toggleDialThread(final boolean enable) {
		if (enable) {
//			System.out.println("Will Create Dial Thread");
			if (dialThread == null) {
				dialThread = new DialThread();
				dialThread.start();
			}
			
		} else if (dialThread != null) {
			dialHandler.removeMessages(0);
			dialHandler.getLooper().quit();
			dialThread = null;
		}
	}

	private Handler dialHandler;
	private DialThread dialThread;

	private class DialThread extends Thread {
		private boolean first = true;

		@Override
		public void run() {
//			System.out.println("Created Dial Thread");
			try {
				Looper.prepare();
				dialHandler = new Handler() {
					@Override
					public void handleMessage(final Message msg) {
						switch (msg.what) {
						case 0:
							try {
								Thread.sleep(1000);
							} catch (final InterruptedException ie) {
							}
							AetherVoice.sendServiceMessage(0x31);
							dialHandler.sendEmptyMessage(0);
							break;
						case 1:
							try {
								Thread.sleep(100);
							} catch (final InterruptedException ie) {
							}
							sendDigit(msg.arg1);
							dialHandler.sendEmptyMessage(0);
							break;
						case 2:
							removeMessages(0);
							try {
								Thread.sleep(100);
							} catch (final InterruptedException ie) {
							}
//							System.out.println("++++++++++++++++ shifting to speaker phone");
							AetherVoice.sendServiceMessage(0x35);
							dialHandler.sendEmptyMessage(0);
							break;
						case 3:
							removeMessages(0);
							try {
								Thread.sleep(100);
							} catch (final InterruptedException ie) {
							}
//							System.out.println("++++++++++++++++ shifting to headphone");
							AetherVoice.sendServiceMessage(0x33);
							dialHandler.sendEmptyMessage(0);
							break;
						}
					}
				};
				
				if (first) {
					first = false;
					/*if (isOnMHS) {
						AetherVoice.sendServiceMessage(AetherVoice.MSG_CALL_ANSWER);

						try {
							Thread.sleep(300);
						} catch (final InterruptedException e) {
							e.printStackTrace();
						}
					}*/

					dialHandler.sendEmptyMessage(0);
				}
				Looper.loop();
			} catch (final Exception e) {

			} catch (final Throwable t) {

			}
		}
	}
	private VoipDialThread myDialThread;
	public void toggleVoipThread(final boolean active){
		if (active) {
			if (myDialThread == null) {
				runThread = true;
				myDialThread = new VoipDialThread();
				myDialThread.start();
			}
		} else if (myDialThread != null) {
			runThread = false;
			myDialThread.interrupt();
			myDialThread = null;
			timeElapsed = 0;
		}
		
	}
	private Handler voipDialHandler;
	
	public class VoipDialThread extends Thread{
		
		@Override
		public void run(){
			
			while(runThread){
				if(isVoip){
					try {
						sleep(1000);
					}
					catch (Exception e){}
					timeElapsed++;
//					System.out.println(timeElapsed);
					if(timeElapsed == 7) {
						runThread = false;
						voipDialHandler.sendEmptyMessage(0);
						
					}
				}
			}
			
		}
	}
}
