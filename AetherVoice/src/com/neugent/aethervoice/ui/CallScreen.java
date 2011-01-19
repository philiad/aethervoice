package com.neugent.aethervoice.ui;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.InetAddress;

import org.sipdroid.media.RtpStreamSender;
import org.sipdroid.net.RtpPacket;
import org.sipdroid.net.RtpSocket;
import org.sipdroid.net.SipdroidSocket;
import org.sipdroid.sipua.UserAgent;
import org.sipdroid.sipua.phone.Call;
import org.sipdroid.sipua.phone.CallerInfo;
import org.sipdroid.sipua.phone.CallerInfoAsyncQuery;
import org.sipdroid.sipua.phone.Connection;
import org.sipdroid.sipua.phone.ContactsAsyncHelper;
import org.sipdroid.sipua.phone.Phone;
import org.sipdroid.sipua.phone.PhoneUtils;
import org.sipdroid.sipua.ui.InCallScreen;
import org.sipdroid.sipua.ui.Receiver;
import org.sipdroid.sipua.ui.Sipdroid;
import org.sipdroid.sipua.ui.SipdroidListener;
import org.sipdroid.sipua.ui.VideoCameraNew;
import org.sipdroid.sipua.ui.VideoPreview;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.ToneGenerator;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.provider.Contacts.People;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import com.neugent.aethervoice.R;

//hehehe incomplete

/**
 * @author Amando Jose Quinto II
 *
 */
public class CallScreen implements CallerInfoAsyncQuery.OnQueryCompleteListener,
ContactsAsyncHelper.OnImageLoadCompleteListener, /*OnLongClickListener,*/
SurfaceHolder.Callback, MediaRecorder.OnErrorListener, MediaPlayer.OnErrorListener,
SipdroidListener, SensorEventListener{
	
	/** The defined screen time out value. **/
	private final int SCREEN_OFF_TIMEOUT = 12000;
	
	private static final String LOG_TAG = "CallScreen";
	
	private Context mContext;
	private View mViewCallScreen;
	
	/** The thread that implements tone generation. **/
	private Thread toneGeneratorThread;

	/** The variable containing the value for time out. **/
	private int oldtimeout;

	/** The instance of SipdroidSocket. **/
	private SipdroidSocket socket;

	/** The instance of RtpSocket. **/
	private RtpSocket rtp_socket;
	
	/** The manager class that locks and unlocks the keyboard. **/
	private KeyguardManager mKeyguardManager;

	/** The class that enables/disables the keyguard. **/
	private KeyguardManager.KeyguardLock mKeyguardLock;
	
	private PowerManager mPowerManager;
	private PowerManager.WakeLock mWakeLock;
	
	// Track the state for the photo.
    private ContactsAsyncHelper.ImageTracker mPhotoTracker;
	
	/** The flag that indicates whether the keyboard is enabled. **/
	private boolean enabled;

	/** The flag that indicates whether the toneGeneratorThread is running. **/
	private boolean running;
	
//	public TextView csStats;
//	public TextView csCodec;
	public Button csBtnSendVideo;
	
	private View csImageFrame;
	private TextView csCaller;
	private TextView csVCaller;
	private TextView csMainStatus;
	private TextView csStatus;
	private ImageView csIcon;
	private ImageView csImage;
	private Chronometer csDuration;
		
	private VideoView mVideoFrame;
	private VideoPreview mVideoPreview;
    
	private RelativeLayout mVideoLayout;
	private LinearLayout mVoiceLayout;
	
	
	// Text colors, used with the lower title and "other call" info areas
    private int mTextColorConnected;
    private int mTextColorEnded;
    private int mTextColorOnHold; //XXX: ?
    
    /** The Phone instance. **/
	public Phone ccPhone; //err para san to?
	
	
	
	//TODO video call variables
	Thread t;
	private static final String TAG = "videocamera";
	private final static int UPDATE_RECORD_TIME = 1;
    private final static int START_STREAM = 2;
    private final int VIDEO_PACKET_RECIEVED = 3;
    
    private final int STREAM_DELAY = 60000;
	
    private boolean isStreaming = false;
    
    private static final float VIDEO_ASPECT_RATIO = 176.0f / 144.0f;
    SurfaceHolder mSurfaceHolder = null;
    MediaController mMediaController;

    private MediaRecorder mMediaRecorder;
    private boolean mMediaRecorderRecording = false;
    
    private Handler mHandler = new MainHandler();
	LocalSocket receiver,sender;
	LocalServerSocket lss;
	int obuffering;
	
	boolean videoQualityHigh;
	Camera mCamera;
	
	private Intent intent;
	boolean justplay;
	boolean isAvailableSprintFFC;
	
	int fps;
	boolean change;
	
	StreamDelay streamDelay = null;
	
	protected String SEND = "sent";
	protected String QUIT = "quit";
	protected String DONT_SEND = "notSent";
	private String isSent = DONT_SEND;

	//private LinearLayout mVideoPreviewParent;

	private Sensor proximitySensor;

	private SensorManager sensorManager;

	private boolean first;
	
	public CallScreen(final Context context){
		mContext = context;
		
		intent = ((AetherVoice)(context)).getIntent();
		initCallScreen();
		initVideoCallComponents();
	}
	
	public View getCallScreenView(){
		return mViewCallScreen;
	}
	
	private void initCallScreen(){
		
		mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
		mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, TAG);
		
		sensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
		
		mPhotoTracker = new ContactsAsyncHelper.ImageTracker();

		// Text colors
        mTextColorConnected = mContext.getResources().getColor(R.color.incall_textConnected);
        mTextColorEnded = mContext.getResources().getColor(R.color.incall_textEnded);
        mTextColorOnHold = mContext.getResources().getColor(R.color.incall_textOnHold);
		
		mViewCallScreen = LayoutInflater.from(mContext).inflate(R.layout.aethervoice_callscreen, null);
		
		csVCaller = (TextView) mViewCallScreen.findViewById(R.id.cs_vcaller);
		csCaller = (TextView) mViewCallScreen.findViewById(R.id.cs_caller);
		
		csMainStatus = (TextView) mViewCallScreen.findViewById(R.id.cs_status1);
		csStatus = (TextView) mViewCallScreen.findViewById(R.id.cs_status);
		csDuration = (Chronometer) mViewCallScreen.findViewById(R.id.cs_duration);
		csImage = (ImageView) mViewCallScreen.findViewById(R.id.cs_image);
		csIcon = (ImageView) mViewCallScreen.findViewById(R.id.cs_icon);
		
		mVideoPreview = (VideoPreview) mViewCallScreen.findViewById(R.id.camera_preview);
        mVideoFrame = (VideoView) mViewCallScreen.findViewById(R.id.video_frame);
        
        // Layouts 
        mVoiceLayout = ((LinearLayout) mViewCallScreen.findViewById(R.id.voice_call));
        mVideoLayout = ((RelativeLayout) mViewCallScreen.findViewById(R.id.video_call));
        //mVideoLayout.setOnLongClickListener(this);
		
//		csStats = (TextView) mViewCallScreen.findViewById(R.id.cs_stats);
//		csCodec = (TextView) mViewCallScreen.findViewById(R.id.cs_codec);
		csBtnSendVideo = (Button) mViewCallScreen.findViewById(R.id.send_video);
		
		csImageFrame = (View) mViewCallScreen.findViewById(R.id.cs_image_bg);
		
		csBtnSendVideo.setSelected(false);
		
		csBtnSendVideo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	
            	if (isSent.equals(DONT_SEND)){
            		//intent.putExtra("justplay",true);
            		intent.removeExtra("justplay");
            		isSent = SEND;   
            		csBtnSendVideo.setSelected(true);
            	}else if (isSent.equals(SEND)){
            		intent.putExtra("justplay",true);
            		isSent = DONT_SEND;
            		csBtnSendVideo.setSelected(false);
            	}
            	sendHandlerMSG(isSent, MSG_SETSENDVIDEO);	//setSendVideo(isSent);
            	
            }   
        }); 
	}
	
	private void sendHandlerMSG(Object message, int what){
		Message msg = new Message();
		msg.what  = what;
		msg.obj = message;
		callScreenHandler.sendMessage(msg);
	}
	
	private final int MSG_SETSENDVIDEO = 1;
	private final int MSG_UPDATETITLEWIDGETS = 2;
	private final int MSG_DISPLAYMAINCALLSTATUS = 3;
	
	private Handler callScreenHandler = new Handler(){
		
		public void handleMessage(Message msg) {
			switch(msg.what){
			case MSG_SETSENDVIDEO:
				setSendVideo((String) msg.obj);
				break;
			case MSG_UPDATETITLEWIDGETS:
				setState((Call)msg.obj);
				break;
			case MSG_DISPLAYMAINCALLSTATUS:
				displayMainCallStatus(ccPhone, Receiver.ccCall);
				break;
			}
		};
		
		//Set send video button
		private void setSendVideo(String p) {
	    	isSent = p;
	    	    	
	    	if ( isSent.equals(SEND)){
	    		Log.i("VideoCamera", "VideoCamera -->  SENT VIDEO---------");
	    		justplay = intent.hasExtra("justplay");
	    		csBtnSendVideo.setBackgroundResource(R.drawable.btn_send_video_f2);
	    		csBtnSendVideo.postInvalidate();
	    		mVoiceLayout.setVisibility(View.GONE);
	    		mVideoLayout.setVisibility(View.VISIBLE);
	    		csBtnSendVideo.setVisibility(View.VISIBLE);
	    		csVCaller.setVisibility(View.VISIBLE);
	    		//mVideoLayout.setOnLongClickListener(this);
	    		
				if (socket != null) {
		    			socket.close();
		    			socket = null;
	    		}
				videoQualityHigh = SettingsWindow.video_quality.equals("high");
				videoCameraOnResume();
	    	} else if (isSent.equals(QUIT)){
	    		if (streamDelay != null)
	    			streamDelay.kill();
	    		
	    		mVideoFrame.stopPlayback();
	    		isStreaming = false;
	    		videoCameraOnPause();
	    		isSent = DONT_SEND;
	    		csBtnSendVideo.setBackgroundResource(R.drawable.btn_send_video_f1);
	    		csBtnSendVideo.postInvalidate();
	    		csBtnSendVideo.setVisibility(View.INVISIBLE);
	    		csBtnSendVideo.setSelected(false);
	    		csVCaller.setVisibility(View.INVISIBLE);
	    		mVideoLayout.setVisibility(View.GONE);
	    		mVoiceLayout.setVisibility(View.VISIBLE);
	    		mHandler.removeMessages(UPDATE_RECORD_TIME);
	    		
	    	}else if (isSent.equals(DONT_SEND)){
	    		csBtnSendVideo.setBackgroundResource(R.drawable.btn_send_video_f1);
	    		csBtnSendVideo.postInvalidate();
	    		csVCaller.setVisibility(View.INVISIBLE);
	    		//csBtnSendVideo.setVisibility(View.VISIBLE); ADDED 12-15-10 by winnie
	    		
	    		mVoiceLayout.setVisibility(View.VISIBLE); 	//mVoiceLayout.setVisibility(View.GONE)
	    		mVideoLayout.setVisibility(View.GONE);		//mVideoLayout.setVisibility(View.VISIBLE);
	    		
	    		mVideoPreview.setVisibility(View.INVISIBLE);
	    		
	    		videoCameraOnPause();
	    		
	    		justplay = intent.hasExtra("justplay");
	    		videoCameraOnResume();
	    	}
	    }
		
		private void setState(Call call){
			Call.State state = call.getState();

	        // TODO: Still need clearer spec on exactly how title *and* status get
	        // set in all states.  (Then, given that info, refactor the code
	        // here to be more clear about exactly which widgets on the card
	        // need to be set.)

	        // Normal "foreground" call card:
	        String cardTitle = getTitleForCallCard(call);
			
			if (state == Call.State.ACTIVE) {
	            // Use the "lower title" (in green).
//	            mLowerTitleViewGroup.setVisibility(View.VISIBLE);
	        	csIcon.setVisibility(View.VISIBLE);
	            csIcon.setImageResource(R.drawable.ic_incall_ongoing);
	            csMainStatus.setText("");
	            csStatus.setText(cardTitle);
	            csMainStatus.setTextColor(mTextColorConnected);
	            csStatus.setTextColor(mTextColorConnected);
	            csDuration.setTextColor(mTextColorConnected);
	            csDuration.setBase(call.base);
	            csDuration.start();
	            csDuration.setVisibility(View.VISIBLE);
	        } else if (state == Call.State.DISCONNECTED) {
	            // Use the "lower title" (in red).
	            // TODO: We may not *always* want to use the lower title for
	            // the DISCONNECTED state.  "Error" states like BUSY or
	            // CONGESTION (see getCallFailedString()) should probably go
	            // in the upper title, for example.  In fact, the lower title
	            // should probably be used *only* for the normal "Call ended"
	            // case.
//	            mLowerTitleViewGroup.setVisibility(View.VISIBLE);
	        	csIcon.setVisibility(View.VISIBLE);
	            csIcon.setImageResource(R.drawable.ic_incall_end);
	            csMainStatus.setText("");
	            csStatus.setText(cardTitle);
	            csMainStatus.setTextColor(mTextColorEnded);
	            csStatus.setTextColor(mTextColorEnded);
	            csDuration.setTextColor(mTextColorEnded);
	            if (call.base != 0) {
		            csDuration.setBase(call.base);
		            csDuration.start();
		            csDuration.stop();
	            } else
	            	csDuration.setVisibility(View.INVISIBLE);
	            
	            csBtnSendVideo.setVisibility(View.INVISIBLE);
	        } else if (state == Call.State.DIALING || state == Call.State.ALERTING){
	        	csMainStatus.setText(cardTitle);
	        	csStatus.setText("");
	        	csDuration.setText("");
	        	csMainStatus.setTextColor(mTextColorConnected);
	            csStatus.setTextColor(mTextColorConnected);
	            csDuration.setTextColor(mTextColorConnected);
	        	csIcon.setVisibility(View.INVISIBLE);
	        }else if (state == Call.State.INCOMING || state == Call.State.WAITING){
	        	csMainStatus.setText(cardTitle);
	        	csStatus.setText("");
	        	csDuration.setText("");
	        	csMainStatus.setTextColor(mTextColorConnected);
	            csStatus.setTextColor(mTextColorConnected);
	            csDuration.setTextColor(mTextColorConnected);
	        	csIcon.setVisibility(View.INVISIBLE);
	        }else {
	            // All other states use the "upper title":
//	            csStatus.setText(cardTitle);
	            csMainStatus.setText(cardTitle);
	            csIcon.setVisibility(View.INVISIBLE);
//	            mLowerTitleViewGroup.setVisibility(View.INVISIBLE);
	            if (state != Call.State.HOLDING)
	            	csDuration.setVisibility(View.INVISIBLE);
	        }
		}
		
	};
	
	/**
	 * Refreshes the InCall, and executes a response depending on the new state.
	 * 
	 * @see #disableKeyguard()
	 * @see #callEnd()
	 */
	public void refreshInCall() {
		//on start of inCallScreen
		if (Receiver.call_state == UserAgent.UA_STATE_IDLE)
			AetherVoice.mHandler.sendEmptyMessageDelayed(CallHandler.MSG_INCALL_BACK, Receiver.call_end_reason == -1 ? 2000 : 5000);
		
		first = true;
	    sensorManager.registerListener(this,proximitySensor,SensorManager.SENSOR_DELAY_NORMAL);
	    //end of on start of inCallScreen
	    
	    
		if (Integer.parseInt(Build.VERSION.SDK) < 5 || Integer.parseInt(Build.VERSION.SDK) > 7)
			disableKeyguard();
		
		//onResume of InCallScreeen
		// if (!AetherVoice.release) Log.i("SipUA:","on resume");
		switch (Receiver.call_state) {
		case UserAgent.UA_STATE_INCOMING_CALL:
			if(!mPowerManager.isScreenOn() || !mWakeLock.isHeld()){
				mWakeLock.acquire();
			}
			
			if (Receiver.pstn_state == null
					|| Receiver.pstn_state.equals("IDLE"))
				// if
				// (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(org.sipdroid.sipua.ui.Settings.PREF_AUTO_ON,
				// org.sipdroid.sipua.ui.Settings.DEFAULT_AUTO_ON) &&
				if (SettingsWindow.autoAnswer
						&& !mKeyguardManager.inKeyguardRestrictedInputMode())
					AetherVoice.mHandler.sendEmptyMessageDelayed(
							CallHandler.MSG_ANSWER_CALL, 1000);
				//else if ((PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(org.sipdroid.sipua.ui.Settings.PREF_AUTO_ONDEMAND, org.sipdroid.sipua.ui.Settings.DEFAULT_AUTO_ONDEMAND) &&
				else if ((SettingsWindow.autoAnswerOD &&		
						PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(org.sipdroid.sipua.ui.Settings.PREF_AUTO_DEMAND, org.sipdroid.sipua.ui.Settings.DEFAULT_AUTO_DEMAND)) ||
						//(PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(org.sipdroid.sipua.ui.Settings.PREF_AUTO_HEADSET, org.sipdroid.sipua.ui.Settings.DEFAULT_AUTO_HEADSET) &&
						(SettingsWindow.autoAnswerHS &&
								Receiver.headset > 0))
					AetherVoice.mHandler.sendEmptyMessageDelayed(
							CallHandler.MSG_INCALL_ANSWER_SPEAKER, 10000);
			break;
		case UserAgent.UA_STATE_INCALL:
			if(!mPowerManager.isScreenOn() || !mWakeLock.isHeld()){
				mWakeLock.acquire();
			}
			
			if (Receiver.docked <= 0)
				screenOff(true);
			
			//san galing to?
			csBtnSendVideo.setVisibility(View.VISIBLE);
			Log.i("InCall", "InCall --> "+(socket == null)+":"+(Receiver.engine(mContext).getLocalVideo() != 0)+
					":"+(Receiver.engine(mContext).getRemoteVideo() != 0));
			
			if (/*Receiver.call_state == UserAgent.UA_STATE_INCALL &&*/ socket == null
					&& Receiver.engine(mContext).getLocalVideo() != 0
					&& Receiver.engine(mContext).getRemoteVideo() != 0
//					&& PreferenceManager.getDefaultSharedPreferences(context).getString(org.sipdroid.sipua.ui.Settings.PREF_SERVER, org.sipdroid.sipua.ui.Settings.DEFAULT_SERVER).equals(org.sipdroid.sipua.ui.Settings.DEFAULT_SERVER)
					&& SettingsWindow.server.equals(org.sipdroid.sipua.ui.Settings.DEFAULT_SERVER))
				(new Thread() {
					@Override
					public void run() {
						final RtpPacket keepalive = new RtpPacket(new byte[12], 0);
						final RtpPacket videopacket = new RtpPacket(new byte[1000], 0);
						try {
							if (intent == null || rtp_socket == null) {
								
								Log.i("CallScreen", "InCall --> rtp_socket == null");

								rtp_socket = new RtpSocket(socket = new SipdroidSocket(Receiver.engine(mContext).getLocalVideo()),
										InetAddress.getByName(Receiver.engine(mContext).getRemoteAddr()), 
										Receiver.engine(mContext).getRemoteVideo());
								Thread.sleep(3000);
							} else{
								Log.i("InCall", "InCall --> rtp_socket != null");
//								rtp_socket.setDatagramSocket(socket = new SipdroidSocket(Receiver.engine(mContext).getLocalVideo()));
								socket = rtp_socket.getDatagramSocket();
							}
							rtp_socket.getDatagramSocket().setSoTimeout(15000);
						} catch (final Exception e) {
							Log.i("InCall", "InCall --> exception1: "+e.getLocalizedMessage());
							if (!AetherVoice.release) e.printStackTrace();
							return;
						}

						keepalive.setPayloadType(126);

						try {
							rtp_socket.send(keepalive);
						} catch (final Exception e1) {
							Log.i("InCall", "InCall --> exception2: "+e1.getMessage()+" "+e1.getCause());
							e1.printStackTrace();
							return;
						}

						while(mVideoLayout.getVisibility() != View.VISIBLE){
							try {
								Log.i("InCall", "InCall --> receiving packet");
								rtp_socket.receive(videopacket);
							} catch (final IOException e) {
								rtp_socket.getDatagramSocket().disconnect();
								try {
									Log.i("InCall", "InCall --> sending keep alive");
									rtp_socket.send(keepalive);
								} catch (final IOException e1) {
									break;
								}
							}
							
							Log.i("InCall", "InCall --> videopacket.getPayloadLength:"+videopacket.getPayloadLength());
							
							if (videopacket.getPayloadLength() > 200){
								intent.putExtra("justplay",true);
								mHandler.sendEmptyMessage(VIDEO_PACKET_RECIEVED);
								Log.i("InCall", "InCall --> PayloadLength > 200");
								break;
							}
						}
						
						Log.i("InCall", "InCall --> receiving packet ended");
					}
				}).start();
			
			
			break;
		case UserAgent.UA_STATE_IDLE:
			if(mWakeLock.isHeld())
				mWakeLock.release();
			
			callEnd();
			break;
		}
		
		
		if (Receiver.ccCall != null)
//			mCallCard.displayMainCallStatus(ccPhone, Receiver.ccCall);
			displayMainCallStatus(ccPhone, Receiver.ccCall);
		AetherVoice.mHandler.sendEmptyMessage(CallHandler.MSG_INCALL_TICK);

		if (toneGeneratorThread == null
				&& Receiver.call_state != UserAgent.UA_STATE_IDLE) {
			running = true;
			(toneGeneratorThread = new Thread() {
				@Override
				public void run() {
					ToneGenerator tg = null;

					if (Settings.System.getInt(mContext.getContentResolver(),
							Settings.System.DTMF_TONE_WHEN_DIALING, 1) == 1)
						tg = new ToneGenerator(
								AudioManager.STREAM_VOICE_CALL,
								(int) (ToneGenerator.MAX_VOLUME * 2 * org.sipdroid.sipua.ui.Settings
										.getEarGain()));
					for (;;) {
						if (!running) {
							toneGeneratorThread = null;
							break;
						}
						AetherVoice.mHandler.sendEmptyMessage(CallHandler.MSG_INCALL_TICK);
						try {
							Thread.sleep(1000);
						} catch (final InterruptedException e) {
						}
					}
					if (tg != null)
						tg.release();
				}
			}).start();
		}
	}
	
	/**
	 * Disables the keyguard.
	 */
	public void disableKeyguard() {
		if (mKeyguardManager == null) {
			mKeyguardManager = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
			mKeyguardLock = mKeyguardManager.newKeyguardLock("AetherVoice");
			enabled = true;
		}
		if (enabled) {
			mKeyguardLock.disableKeyguard();
			enabled = false;
		}
	}

	/**
	 * Re-enables the keyguard.
	 */
	public void reenableKeyguard() {
		if (!enabled) {
			try {
				if (Integer.parseInt(Build.VERSION.SDK) < 5)
					Thread.sleep(1000);
			} catch (final InterruptedException e) {
			}

			/*if (mKeyguardManager == null) {
				mKeyguardManager = (KeyguardManager) mContext
						.getSystemService(Context.KEYGUARD_SERVICE);
				mKeyguardLock = mKeyguardManager.newKeyguardLock("AetherVoice");
				enabled = true;
			} else*/
			mKeyguardLock.reenableKeyguard();
			enabled = true;
		}
	}

	/**
	 * Answers the incoming call
	 */
	public void answer() {

		(new Thread() {
			@Override
			public void run() {
				Receiver.stopRingtone();
				Receiver.engine(mContext).answercall();
			}
		}).start();

		if (Receiver.ccCall != null) {
			Receiver.ccCall.setState(Call.State.ACTIVE);
			Receiver.ccCall.base = SystemClock.elapsedRealtime();
//			mCallCard.displayMainCallStatus(ccPhone, Receiver.ccCall);
			displayMainCallStatus(ccPhone, Receiver.ccCall);
			csBtnSendVideo.setVisibility(View.VISIBLE);
		}
	}

	/**
	 * Drops a call.
	 * 
	 * @see Receiver#stopRingtone()
	 */
	public void reject() {
		if (Receiver.ccCall != null) {
			Receiver.stopRingtone();
			Receiver.ccCall.setState(Call.State.DISCONNECTED);
//			mCallCard.displayMainCallStatus(ccPhone, Receiver.ccCall);
			displayMainCallStatus(ccPhone, Receiver.ccCall);
			isSent = QUIT; //isSent = "quit";
			sendHandlerMSG(QUIT, MSG_SETSENDVIDEO); //setSendVideo(QUIT);
		}
		(new Thread() {
			@Override
			public void run() {
				Receiver.engine(mContext).rejectcall();
			}
		}).start();
	}

	/**
	 * Ends a call.
	 * 
	 * @see #reenableKeyguard()
	 * @see #screenOff(boolean)
	 * @see AetherVoice#hideCallFrame(boolean)
	 */
	public void callEnd() {
		if (Integer.parseInt(Build.VERSION.SDK) >= 5
				&& Integer.parseInt(Build.VERSION.SDK) <= 7)
			reenableKeyguard();

		AetherVoice.mHandler.removeMessages(CallHandler.MSG_INCALL_BACK);

		sendHandlerMSG(DONT_SEND, MSG_SETSENDVIDEO); //setSendVideo(DONT_SEND);
    	
		if (socket != null) {
			socket.close();
			socket = null;
		}

		// if (!AetherVoice.release) Log.i("SipUA:","on pause");

		switch (Receiver.call_state) {
		case UserAgent.UA_STATE_INCOMING_CALL:
			Receiver.moveTop();
			csBtnSendVideo.setVisibility(View.INVISIBLE);
			break;
		case UserAgent.UA_STATE_IDLE:
			if (Receiver.ccCall != null)
//				mCallCard.displayMainCallStatus(ccPhone, Receiver.ccCall);
				callScreenHandler.sendEmptyMessage(MSG_DISPLAYMAINCALLSTATUS); //displayMainCallStatus(ccPhone, Receiver.ccCall);
			
			AetherVoice.mHandler.sendEmptyMessageDelayed(
					CallHandler.MSG_INCALL_BACK,
					Receiver.call_end_reason == -1 ? 2000 : 5000);
			break;
		}

		if (toneGeneratorThread != null) {
			running = false;
			toneGeneratorThread.interrupt();
		}

		screenOff(false);
		/*if (mCallCard.mElapsedTime != null)
			mCallCard.mElapsedTime.stop();*/
		if (csDuration != null)
			csDuration.stop();

//		AetherVoice.hideCallFrame(false);
	}
	
	/**
	 * Turns the screen on/off.
	 * 
	 * @param off
	 *            True if the screen is to be turned off
	 */
	private void screenOff(final boolean off) {
		final ContentResolver cr = mContext.getContentResolver();

		if (off) {
			if (oldtimeout == 0) {
				oldtimeout = Settings.System.getInt(cr,
						Settings.System.SCREEN_OFF_TIMEOUT, 60000);
				Settings.System.putInt(cr, Settings.System.SCREEN_OFF_TIMEOUT,
						SCREEN_OFF_TIMEOUT);
			}
		} else {
			if (oldtimeout == 0
					&& Settings.System.getInt(cr,
							Settings.System.SCREEN_OFF_TIMEOUT, 60000) == SCREEN_OFF_TIMEOUT)
				oldtimeout = 60000;
			if (oldtimeout != 0) {
				Settings.System.putInt(cr, Settings.System.SCREEN_OFF_TIMEOUT,
						oldtimeout);
				oldtimeout = 0;
			}
		}
	}
	
	public void reset() {
        // default to show ACTIVE call style, with empty title and status text
//        showCallConnected(); // notused
        csStatus.setText("");
        csMainStatus.setText("");
    }
	
	 /**
     * Updates the main block of caller info on the CallCard
     * (ie. the stuff in the mainCallCard block) based on the specified Call.
     */
    public void displayMainCallStatus(Phone phone, Call call) {

//        Call.State state = call.getState();
//        int callCardBackgroundResid = 0;

        // Background frame resources are different between portrait/landscape:
//        boolean landscapeMode = mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

//        switch (state) {
//            case ACTIVE:
//                showCallConnected(); //not used
//
//                callCardBackgroundResid =
//                        landscapeMode ? R.drawable.incall_frame_connected_tall_land
//                        : R.drawable.incall_frame_connected_tall_port;
//
//                // update timer field
//                break;
//
//            case HOLDING:
//                showCallOnhold(); //not used
//
//                callCardBackgroundResid =
//                        landscapeMode ? R.drawable.incall_frame_hold_tall_land
//                        : R.drawable.incall_frame_hold_tall_port;
//                break;
//
//            case DISCONNECTED:
//                reset();
//                showCallEnded(); //not used
//
//                callCardBackgroundResid =
//                        landscapeMode ? R.drawable.incall_frame_ended_tall_land
//                        : R.drawable.incall_frame_ended_tall_port;
//
//                break;
//
//            case DIALING:
//            case ALERTING:
//                showCallConnecting(); //not used
//
//                callCardBackgroundResid =
//                        landscapeMode ? R.drawable.incall_frame_normal_tall_land
//                        : R.drawable.incall_frame_normal_tall_port;
//
//                break;
//
//            case INCOMING:
//            case WAITING:
//                showCallIncoming(); //not used
//
//                callCardBackgroundResid =
//                        landscapeMode ? R.drawable.incall_frame_normal_tall_land
//                        : R.drawable.incall_frame_normal_tall_port;
//               break;
//
//            case IDLE:
//                // The "main CallCard" should never display an idle call!
//                Log.w(LOG_TAG, "displayMainCallStatus: IDLE call in the main call card!");
//                break;
//
//            default:
//                Log.w(LOG_TAG, "displayMainCallStatus: unexpected call state: " + state);
//                break;
//        }
    	
        updateCardTitleWidgets(phone, call);

        {
            // Update onscreen info for a regular call (which presumably
            // has only one connection.)
            Connection conn = call.getEarliestConnection();

            boolean isPrivateNumber = false; // TODO: need isPrivate() API
           
            if (conn == null) {
                // if the connection is null, we run through the behaviour
                // we had in the past, which breaks down into trivial steps
                // with the current implementation of getCallerInfo and
                // updateDisplayForPerson.
            	
                updateDisplayForPerson(null, isPrivateNumber, false, call);
            } else {

                // make sure that we only make a new query when the current
                // callerinfo differs from what we've been requested to display.
                boolean runQuery = true;
                Object o = conn.getUserData();
             
                if (o instanceof PhoneUtils.CallerInfoToken) {
                    runQuery = mPhotoTracker.isDifferentImageRequest(((PhoneUtils.CallerInfoToken) o).currentInfo);
                    
                } else {
                	
                    runQuery = mPhotoTracker.isDifferentImageRequest(conn);
                }
                
                if (runQuery) {
                    PhoneUtils.CallerInfoToken info =
                            PhoneUtils.startGetCallerInfo(mContext, conn, this, call);
                   
                    updateDisplayForPerson(info.currentInfo, isPrivateNumber, !info.isFinal, call);
                } else {
                    // No need to fire off a new query.  We do still need
                    // to update the display, though (since we might have
                    // previously been in the "conference call" state.)
                	
                    if (o instanceof CallerInfo) {
                        CallerInfo ci = (CallerInfo) o;
                       
                        updateDisplayForPerson(ci, false, false, call);
                    } else if (o instanceof PhoneUtils.CallerInfoToken){
                        CallerInfo ci = ((PhoneUtils.CallerInfoToken) o).currentInfo;
                        updateDisplayForPerson(ci, false, true, call);
                    } else {
                        Log.w(LOG_TAG, "displayMainCallStatus: runQuery was false, "
                              + "but we didn't have a cached CallerInfo object!  o = " + o);
                        // TODO: any easy way to recover here (given that
                        // the CallCard is probably displaying stale info
                        // right now?)  Maybe force the CallCard into the
                        // "Unknown" state?
                    }
                }
            }
        }

        // In some states we override the "photo" ImageView to be an
        // indication of the current state, rather than displaying the
        // regular photo as set above.
        updatePhotoForCallState(call);

        // Set the background frame color based on the state of the call.
//        setMainCallCardBackgroundResource(callCardBackgroundResid); //not needed
        // (Text colors are set in updateCardTitleWidgets().)
    }
	
	 /**
     * Returns the "card title" displayed at the top of a foreground
     * ("active") CallCard to indicate the current state of this call, like
     * "Dialing" or "In call" or "On hold".  A null return value means that
     * there's no title string for this state.
     */
    private String getTitleForCallCard(Call call) {
        String retVal = null;
        Call.State state = call.getState();
        Context context = mContext;

        switch (state) {
            case IDLE:
                break;

            case ACTIVE:
                // Title is "Call in progress".  (Note this appears in the
                // "lower title" area of the CallCard.)
                retVal = context.getString(R.string.card_title_in_progress);
                break;

            case HOLDING:
                retVal = context.getString(R.string.card_title_on_hold);
                // TODO: if this is a conference call on hold,
                // maybe have a special title here too?
                break;

            case DIALING:
            case ALERTING:
                retVal = context.getString(R.string.card_title_dialing);
                break;

            case INCOMING:
            case WAITING:
                retVal = context.getString(R.string.card_title_incoming_call);
                break;

            case DISCONNECTED:
                retVal = getCallFailedString(call);
                break;
        }

        return retVal;
    }
    
    /**
     * Updates the CallCard "photo" IFF the specified Call is in a state
     * that needs a special photo (like "busy" or "dialing".)
     *
     * If the current call does not require a special image in the "photo"
     * slot onscreen, don't do anything, since presumably the photo image
     * has already been set (to the photo of the person we're talking, or
     * the generic "picture_unknown" image, or the "conference call"
     * image.)
     */
    private void updatePhotoForCallState(Call call) { //XXX: for photos :D
        int photoImageResource = 0;
        int photoBGResource = 0;

        // Check for the (relatively few) telephony states that need a
        // special image in the "photo" slot.
        Call.State state = call.getState();
        switch (state) {
            case DISCONNECTED:
                // Display the special "busy" photo for BUSY or CONGESTION.
                // Otherwise (presumably the normal "call ended" state)
                // leave the photo alone.
                Connection c = call.getEarliestConnection();
                // if the connection is null, we assume the default case,
                // otherwise update the image resource normally.
                if (c != null) {
                    Connection.DisconnectCause cause = c.getDisconnectCause();
                    if ((cause == Connection.DisconnectCause.BUSY)
                        || (cause == Connection.DisconnectCause.CONGESTION)) {
                        photoImageResource = R.drawable.picture_busy;
                    }
                }
                
                photoBGResource = R.drawable.bg_call_photo;

                // TODO: add special images for any other DisconnectCauses?
                break;
            case DIALING:
            case ALERTING:
//                photoImageResource = R.drawable.picture_dialing;
            	photoBGResource = R.drawable.bg_call_dialling;
                break;

            default:
            	photoBGResource = R.drawable.bg_call_photo;
            	
                // Leave the photo alone in all other states.
                // If this call is an individual call, and the image is currently
                // displaying a state, (rather than a photo), we'll need to update
                // the image.
                // This is for the case where we've been displaying the state and
                // now we need to restore the photo.  This can happen because we
                // only query the CallerInfo once, and limit the number of times
                // the image is loaded. (So a state image may overwrite the photo
                // and we would otherwise have no way of displaying the photo when
                // the state goes away.)

                // if the photoResource field is filled-in in the Connection's
                // caller info, then we can just use that instead of requesting
                // for a photo load.

                // look for the photoResource if it is available.
                CallerInfo ci = null;
                {
                    Connection conn = call.getEarliestConnection();
                    if (conn != null) {
                        Object o = conn.getUserData();
                        if (o instanceof CallerInfo) {
                            ci = (CallerInfo) o;
                        } else if (o instanceof PhoneUtils.CallerInfoToken) {
                            ci = ((PhoneUtils.CallerInfoToken) o).currentInfo;
                        }
                    }
                }

                if (ci != null) {
                    photoImageResource = ci.photoResource;
                }

                // If no photoResource found, check to see if this is a conference call. If
                // it is not a conference call:
                //   1. Try to show the cached image
                //   2. If the image is not cached, check to see if a load request has been
                //      made already.
                //   3. If the load request has not been made [DISPLAY_DEFAULT], start the
                //      request and note that it has started by updating photo state with
                //      [DISPLAY_IMAGE].
                // Load requests started in (3) use a placeholder image of -1 to hide the
                // image by default.  Please refer to CallerInfoAsyncQuery.java for cases
                // where CallerInfo.photoResource may be set.
                if (photoImageResource == 0) {
            		if (!showCachedImage(mContext, csImage, ci) && (mPhotoTracker.getPhotoState() ==               			ContactsAsyncHelper.ImageTracker.DISPLAY_DEFAULT)) {
                        ContactsAsyncHelper.updateImageViewWithContactPhotoAsync(ci,
                               mContext, csImage, mPhotoTracker.getPhotoUri(), -1);
                        mPhotoTracker.setPhotoState(
                                ContactsAsyncHelper.ImageTracker.DISPLAY_IMAGE);
                    }
                } else {
                    showImage(csImage, photoImageResource);
                    mPhotoTracker.setPhotoState(ContactsAsyncHelper.ImageTracker.DISPLAY_IMAGE);
                    return;
                }
                break;
        }

        if (photoImageResource != 0) {
            showImage(csImage, photoImageResource);
            // Track the image state.
            mPhotoTracker.setPhotoState(ContactsAsyncHelper.ImageTracker.DISPLAY_DEFAULT);
        }
        
        if(photoBGResource !=0){
        	csImageFrame.setBackgroundResource(photoBGResource);
        	mPhotoTracker.setPhotoState(ContactsAsyncHelper.ImageTracker.DISPLAY_DEFAULT);
        }
    }

    /**
     * Updates the name / photo / number / label fields on the CallCard
     * based on the specified CallerInfo.
     *
     * If the current call is a conference call, use
     * updateDisplayForConference() instead.
     */
    private void updateDisplayForPerson(CallerInfo info,
                                        boolean isPrivateNumber,
                                        boolean isTemporary,
                                        Call call) {
    	
        // inform the state machine that we are displaying a photo.
        mPhotoTracker.setPhotoRequest(info);
        mPhotoTracker.setPhotoState(ContactsAsyncHelper.ImageTracker.DISPLAY_IMAGE);

        String name;
        String displayNumber = null;
//        String label = null;
        Uri personUri = null;
     
        if (info != null) {
            // It appears that there is a small change in behaviour with the
            // PhoneUtils' startGetCallerInfo whereby if we query with an
            // empty number, we will get a valid CallerInfo object, but with
            // fields that are all null, and the isTemporary boolean input
            // parameter as true.

            // In the past, we would see a NULL callerinfo object, but this
            // ends up causing null pointer exceptions elsewhere down the
            // line in other cases, so we need to make this fix instead. It
            // appears that this was the ONLY call to PhoneUtils
            // .getCallerInfo() that relied on a NULL CallerInfo to indicate
            // an unknown contact.
        	
            if (TextUtils.isEmpty(info.name)) {
            	if (TextUtils.isEmpty(info.phoneNumber)) {
                	{
                        name = mContext.getString(R.string.unknown_contact_name);
                    }
                } else {
                	name = info.name = getContactName(info.phoneNumber);
                	if(name.equals(""))
                		name = info.phoneNumber;
                }
            } else {
                name = info.name;
                displayNumber = info.phoneNumber;
//                label = info.phoneLabel;
            }
            personUri = ContentUris.withAppendedId(People.CONTENT_URI, info.person_id);
        } else {
        	{
                name = mContext.getString(R.string.unknown_contact_name);
            }
        }
        csCaller.setText(name);
        csVCaller.setText(name);
        csCaller.setVisibility(View.VISIBLE);
//        

        // Update mPhoto
        // if the temporary flag is set, we know we'll be getting another call after
        // the CallerInfo has been correctly updated.  So, we can skip the image
        // loading until then.

        // If the photoResource is filled in for the CallerInfo, (like with the
        // Emergency Number case), then we can just set the photo image without
        // requesting for an image load. Please refer to CallerInfoAsyncQuery.java
        // for cases where CallerInfo.photoResource may be set.  We  can also avoid
        // the image load step if the image data is cached.
        if (isTemporary && (info == null || !info.isCachedPhotoCurrent)) {
            csImage.setVisibility(View.INVISIBLE);
        } else if (info != null && info.photoResource != 0){
            showImage(csImage, info.photoResource);
        } else if (!showCachedImage(mContext, csImage, info)) {
            // Load the image with a callback to update the image state.
            // Use a placeholder image value of -1 to indicate no image.
            ContactsAsyncHelper.updateImageViewWithContactPhotoAsync(info, 0, this, call,
                   mContext, csImage, personUri, -1);
        }
        if (name==null && displayNumber != null) {
            csCaller.setText(displayNumber);
            csCaller.setVisibility(View.VISIBLE);
        }
       
        	/*else { //we do not need this
        
//            mPhoneNumber.setVisibility(View.GONE);
        	csCaller.setText("");
        }*/

        /*if (label != null) {
            mLabel.setText(label);
            mLabel.setVisibility(View.VISIBLE);
        } else {
            mLabel.setVisibility(View.GONE);
        	mLabel.setText("");
        }*/
    }
    
    //XXX: work around if contacts are not found...
    private String getContactName(String number){
    	String name = "";
    	final Cursor nameCursor = mContext.getContentResolver().query(Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number)), new String[]{ ContactsContract.PhoneLookup.DISPLAY_NAME }, null, null, ViewContactInfo.getSortOrderString());
    	if(nameCursor.moveToFirst()){
    		name = nameCursor.getString(nameCursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
    	}
    	return name;
    }
    
    /**
     * Updates the "upper" and "lower" titles based on the current state of this call.
     */
    private void updateCardTitleWidgets(Phone phone, Call call) {
    	sendHandlerMSG(call, MSG_UPDATETITLEWIDGETS);
        /*Call.State state = call.getState();

        // TODO: Still need clearer spec on exactly how title *and* status get
        // set in all states.  (Then, given that info, refactor the code
        // here to be more clear about exactly which widgets on the card
        // need to be set.)

        // Normal "foreground" call card:
        String cardTitle = getTitleForCallCard(call);

        // We display *either* the "upper title" or the "lower title", but
        // never both.
        if (state == Call.State.ACTIVE) {
            // Use the "lower title" (in green).
//            mLowerTitleViewGroup.setVisibility(View.VISIBLE);
        	csIcon.setVisibility(View.VISIBLE);
            csIcon.setImageResource(R.drawable.ic_incall_ongoing);
            csMainStatus.setText("");
            csStatus.setText(cardTitle);
            csMainStatus.setTextColor(mTextColorConnected);
            csStatus.setTextColor(mTextColorConnected);
            csDuration.setTextColor(mTextColorConnected);
            csDuration.setBase(call.base);
            csDuration.start();
            csDuration.setVisibility(View.VISIBLE);
        } else if (state == Call.State.DISCONNECTED) {
            // Use the "lower title" (in red).
            // TODO: We may not *always* want to use the lower title for
            // the DISCONNECTED state.  "Error" states like BUSY or
            // CONGESTION (see getCallFailedString()) should probably go
            // in the upper title, for example.  In fact, the lower title
            // should probably be used *only* for the normal "Call ended"
            // case.
//            mLowerTitleViewGroup.setVisibility(View.VISIBLE);
        	csIcon.setVisibility(View.VISIBLE);
            csIcon.setImageResource(R.drawable.ic_incall_end);
            csMainStatus.setText("");
            csStatus.setText(cardTitle);
            csMainStatus.setTextColor(mTextColorEnded);
            csStatus.setTextColor(mTextColorEnded);
            csDuration.setTextColor(mTextColorEnded);
            if (call.base != 0) {
	            csDuration.setBase(call.base);
	            csDuration.start();
	            csDuration.stop();
            } else
            	csDuration.setVisibility(View.INVISIBLE);
        } else if (state == Call.State.DIALING || state == Call.State.ALERTING){
        	csMainStatus.setText(cardTitle);
        	csStatus.setText("");
        	csDuration.setText("");
        	csMainStatus.setTextColor(mTextColorConnected);
            csStatus.setTextColor(mTextColorConnected);
            csDuration.setTextColor(mTextColorConnected);
        	csIcon.setVisibility(View.INVISIBLE);
        }else if (state == Call.State.INCOMING || state == Call.State.WAITING){
        	csMainStatus.setText(cardTitle);
        	csStatus.setText("");
        	csDuration.setText("");
        	csMainStatus.setTextColor(mTextColorConnected);
            csStatus.setTextColor(mTextColorConnected);
            csDuration.setTextColor(mTextColorConnected);
        	csIcon.setVisibility(View.INVISIBLE);
        }else {
            // All other states use the "upper title":
//            csStatus.setText(cardTitle);
            csMainStatus.setText(cardTitle);
            csIcon.setVisibility(View.INVISIBLE);
//            mLowerTitleViewGroup.setVisibility(View.INVISIBLE);
            if (state != Call.State.HOLDING)
            	csDuration.setVisibility(View.INVISIBLE);
        }*/
    }
	
    private String getCallFailedString(Call call) {
    	int resID = R.string.card_title_call_ended;

    	if (Receiver.call_end_reason != -1)
    	    resID = Receiver.call_end_reason;

    	return mContext.getString(resID);
    }
    
    /**
     * Try to display the cached image from the callerinfo object.
     *
     *  @return true if we were able to find the image in the cache, false otherwise.
     */
    private static final boolean showCachedImage (Context context, ImageView view, CallerInfo ci) {
        if ((ci != null) && ci.isCachedPhotoCurrent) {
            if (ci.cachedPhoto != null) {
                showImage(view, ci.cachedPhoto);
            } else {
            	//showImage(view, R.drawable.picture_unknown);
            	if(!getContactBitmap(context, view, ci))
            		showImage(view, R.drawable.anonymous_call);
            	
            	
            }
            return true;
        }
        return false;
    }
    
    //work around for photo
    private static final boolean getContactBitmap(Context context, ImageView view, CallerInfo ci){
    	if(ci.name.equals(""))
    		return false;
    	
    	System.out.println(ci.name+"ooooooooooooooooooooooooooooooooooooooooooooooooooo");
    	final ContentResolver cr = context.getContentResolver();
    	final Cursor idCursor = cr.query(Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_FILTER_URI, ci.name), new String[]{ "_id" }, null, null, null);
    	if(idCursor.moveToFirst()){
    		final long id = idCursor.getLong(idCursor.getColumnIndex(ContactsContract.Contacts._ID));
        	final InputStream is = ContactsContract.Contacts.openContactPhotoInputStream(cr, ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id));
        	if(is == null ) return false;
        	else{
        		view.setImageBitmap(BitmapFactory.decodeStream(is));
        		view.setVisibility(View.VISIBLE);
        		return true;
        	}
    	}
    	return false;
    }
    
    /** Helper function to display the resource in the imageview AND ensure its visibility.*/
    private static final void showImage(ImageView view, int resource) {
        view.setImageResource(resource);
        view.setVisibility(View.VISIBLE);
    }
    
    /**
     * Sets the background drawable of the main call card.
     *//* not needed
    private void setMainCallCardBackgroundResource(int resid) {
        mMainCallCard.setBackgroundResource(resid);
    }*/
    
    /** Helper function to display the drawable in the imageview AND ensure its visibility.*/
    private static final void showImage(ImageView view, Drawable drawable) {
        view.setImageDrawable(drawable);
        view.setVisibility(View.VISIBLE);
    }
    
    /**
     * Implemented for CallerInfoAsyncQuery.OnQueryCompleteListener interface.
     * refreshes the CallCard data when it called.
     */
    public void onQueryComplete(int token, Object cookie, CallerInfo ci) {
        if (cookie instanceof Call) {
            // grab the call object and update the display for an individual call,
            // as well as the successive call to update image via call state.
            // If the object is a textview instead, we update it as we need to.
            Call call = (Call) cookie;
            updateDisplayForPerson(ci, false, false, call);
            updatePhotoForCallState(call);

        } else if (cookie instanceof TextView){
            ((TextView) cookie).setText(PhoneUtils.getCompactNameFromCallerInfo(ci,mContext));
        }
    }

    /**
     * Implemented for ContactsAsyncHelper.OnImageLoadCompleteListener interface.
     * make sure that the call state is reflected after the image is loaded.
     */
    public void onImageLoadComplete(int token, Object cookie, ImageView iView,
            boolean imagePresent){
        if (cookie != null) {
            updatePhotoForCallState((Call) cookie);
        }
    }
    
    
	/*public boolean onLongClick(View v) {
		if (mVideoPreviewParent.getVisibility() == View.VISIBLE){
			mVideoPreviewParent.setVisibility(View.INVISIBLE);
			mVideoPreview.setVisibility(View.INVISIBLE);
		}
			
		else{
			mVideoPreviewParent.setVisibility(View.VISIBLE);
			mVideoPreview.setVisibility(View.VISIBLE);
		}
		
		return true;
	}*/
    
	
	
	//video call engine
	/** This Handler is used to post message back onto the main thread of the application */
    private class MainHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
        	
        	switch (msg.what) {
				case START_STREAM:
						isStreaming = true;
					
						Log.i("VideoCamera", "VideoCamera --> START_STREAM");
						//rtsp://192.168.1.161/sample_300kbit.mp4
						mVideoFrame.setVideoURI(Uri.parse("rtsp://"+Receiver.engine(mContext).getRemoteAddr()+"/"+
				        		Receiver.engine(mContext).getRemoteVideo()+"/sipdroid"));
						//mVideoFrame.setVideoURI(Uri.parse("rtsp://192.168.1.161/sample_300kbit.mp4"));
						mVideoFrame.start();
					break;

					case VIDEO_PACKET_RECIEVED:
						sendHandlerMSG(DONT_SEND, MSG_SETSENDVIDEO); //setSendVideo(DONT_SEND);
						break;
				default:
				
					long now = SystemClock.elapsedRealtime();
	                long delta = now - Receiver.ccCall.base;
	
	                long seconds = (delta + 500) / 1000;  // round to nearest
	                long minutes = seconds / 60;
	                long hours = minutes / 60;
	                long remainderMinutes = minutes - (hours * 60);
	                long remainderSeconds = seconds - (minutes * 60);
	
	                String secondsString = Long.toString(remainderSeconds);
	                if (secondsString.length() < 2) {
	                    secondsString = "0" + secondsString;
	                }
	                String minutesString = Long.toString(remainderMinutes);
	                if (minutesString.length() < 2) {
	                    minutesString = "0" + minutesString;
	                }
	                String text = minutesString + ":" + secondsString;
	                if (hours > 0) {
	                    String hoursString = Long.toString(hours);
	                    if (hoursString.length() < 2) {
	                        hoursString = "0" + hoursString;
	                    }
	                    text = hoursString + ":" + text;
	                }
	               	//mRecordingTimeView.setText(text);
	                //if (fps != 0) mFPS.setText(fps+(videoQualityHigh?"h":"l")+"fps");
	               	if (mVideoFrame != null) {
	               		int buffering = mVideoFrame.getBufferPercentage();
//	                    if (buffering != 100 && buffering != 0) {
//	                    	mMediaController.show();
//	                    }
	               		
	               		mMediaController.hide();
	               			               		
	                    //Log.i("VideoCamera", "VideoCamera --> UPDATE_RECORD_TIME --> "+(buffering != 0)+":"+!mMediaRecorderRecording+":"+(mVideoPreview.getVisibility() == View.VISIBLE));
	                    if (buffering != 0 && !mMediaRecorderRecording) mVideoPreview.setVisibility(View.INVISIBLE);
	                    /*if (buffering != 0 && !mMediaRecorderRecording){
	                    	mVideoPreviewParent.setVisibility(View.INVISIBLE);
	                    	mVideoPreview.setVisibility(View.INVISIBLE);
	                    }*/
	                    if (obuffering != buffering && buffering == 100 && rtp_socket != null) {
							RtpPacket keepalive = new RtpPacket(new byte[12],0);
							keepalive.setPayloadType(125);
							try {
								rtp_socket.send(keepalive);
							} catch (IOException e) {
							}
	                    }
	                    obuffering = buffering;
	              	}
	                
	                // Work around a limitation of the T-Mobile G1: The T-Mobile
	                // hardware blitter can't pixel-accurately scale and clip at the same time,
	                // and the SurfaceFlinger doesn't attempt to work around this limitation.
	                // In order to avoid visual corruption we must manually refresh the entire
	                // surface view when changing any overlapping view's contents.
	                mVideoPreview.invalidate();
	                
	                
	                
	                //if (Receiver.call_state == UserAgent.UA_STATE_INCALL)
	                	mHandler.sendEmptyMessageDelayed(UPDATE_RECORD_TIME, 1000);
				
	                break;
			}
         }
    };
    
    
    protected class StreamDelay implements Runnable{
		private boolean isAlive= true;
		public StreamDelay(){
			
			Log.i("Thread", "Thread started");
			new Thread(this).start();
		}

		@Override
		public void run() {
			
			try {
				Thread.sleep(STREAM_DELAY);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			if (isAlive)
				mHandler.sendEmptyMessage(START_STREAM);
				
			// TODO Auto-generated method stub
		}
		
		protected void kill(){
			isAlive = false;
		}
	}
    
    private void initVideoCallComponents() {
		mVideoPreview.setAspectRatio(VIDEO_ASPECT_RATIO);

		SurfaceHolder holder = mVideoPreview.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
		mVideoFrame.setMediaController(mMediaController = new MediaController(mContext));
    	mVideoFrame.setOnErrorListener(this);
	}
	
    @Override
	public void onError(MediaRecorder mr, int what, int extra) {
		 if (what == MediaRecorder.MEDIA_RECORDER_ERROR_UNKNOWN) {
	            //finish();
	        }
		
	}


	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		//isStreaming = false;
		
		//if (!isStreaming) {
		mVideoFrame.stopPlayback(); 
		Log.i("VideoCamera", "VideoCamera --> error --> STREAM_DELAY: "+STREAM_DELAY);
		//mHandler.sendEmptyMessageDelayed(START_STREAM, STREAM_DELAY);
		createStreamDelay();
		
		return true;
	}

	
	private void createStreamDelay() {
		// TODO Auto-generated method stub
		if (streamDelay != null)
			streamDelay.kill();
		
		streamDelay = new StreamDelay();
	}
	
	private void videoCameraOnResume(){
		//video_quality
		
		Log.i("VideoCamera", "VideoCamera --> on resume");
	        if (!justplay) {
	        	
	        	if (!isStreaming)
	        		mHandler.sendEmptyMessage(START_STREAM);
	        	
				receiver = new LocalSocket();
				try {
					lss = new LocalServerSocket("Sipdroid");
					receiver.connect(new LocalSocketAddress("Sipdroid"));
					receiver.setReceiveBufferSize(500000);
					receiver.setSendBufferSize(500000);
					sender = lss.accept();
					sender.setReceiveBufferSize(500000);
					sender.setSendBufferSize(500000);
				} catch (IOException e1) {
					e1.printStackTrace();
					/*if (!Sipdroid.release) e1.printStackTrace();
					finish();
					return;*/
				}
		        checkForCamera();
	            mVideoPreview.setVisibility(View.VISIBLE);
		        //mVideoPreviewParent.setVisibility(View.VISIBLE);
		        if (!mMediaRecorderRecording) initializeVideo();
		        startVideoRecording();
		        	        
	        } else if (Receiver.engine(mContext).getRemoteVideo() != 0) {
	        	
	        	if (!isStreaming)
	        		mHandler.sendEmptyMessage(START_STREAM);
	        }
	        
	        mHandler.sendEmptyMessage(UPDATE_RECORD_TIME);
		
		//videoQualityHigh = SettingsWindow.video_quality.equals("high");
		
		
	}
	
	private void videoCameraOnPause(){
		if (mMediaRecorderRecording) {
            stopVideoRecording();

            try {
    			lss.close();
    	        receiver.close();
    	        sender.close();
    		} catch (IOException e) {
    			if (!Sipdroid.release) e.printStackTrace();
    		}
        }
	}
	
	private void stopVideoRecording() {
        Log.i("VideoCamera", "stopVideoRecording --> "+mMediaRecorderRecording+":"+(mMediaRecorder != null));
        if (mMediaRecorderRecording || mMediaRecorder != null) {
    		Receiver.listener_video = null;
    		t.interrupt();
            RtpStreamSender.delay = 0;

            if (mMediaRecorderRecording && mMediaRecorder != null) {
                try {
                    mMediaRecorder.setOnErrorListener(null);
                    mMediaRecorder.setOnInfoListener(null);
                    mMediaRecorder.stop();
                } catch (RuntimeException e) {
                    Log.e(TAG, "stop fail: " + e.getMessage());
                }

                mMediaRecorderRecording = false;
            }
            releaseMediaRecorder();
        }
        
        Log.i("VideoCamera", "stopVideoRecording --> "+mMediaRecorderRecording+":"+(mMediaRecorder != null));
    }
	
	private void startVideoRecording() {
        Log.v(TAG, "startVideoRecording");
        
        Log.i("VideoCamera", "VideoCamera --> startVideoRecording");

            if (Receiver.listener_video == null) {
    			Receiver.listener_video = this;   	
                RtpStreamSender.delay = 1;
    	        (t = new Thread() {
    				public void run() {
    					int frame_size = 1400;
    					byte[] buffer = new byte[frame_size + 14];
    					buffer[12] = 4;
    					RtpPacket rtp_packet = new RtpPacket(buffer, 0);
    					int seqn = 0;
    					int num,number = 0,src,dest,len = 0,head = 0,lasthead = 0,cnt = 0,stable = 0;
    					long now,lasttime = 0;
    					double avgrate = videoQualityHigh?45000:24000;
    					double avglen = avgrate/20;
    					
    					try {
    						if (rtp_socket == null){
    							Log.i("VideoCamera", "VideoCamera --> rtp_socket == null");
    							rtp_socket = new RtpSocket(new SipdroidSocket(Receiver.engine(mContext).getLocalVideo()),
        								InetAddress.getByName(Receiver.engine(mContext).getRemoteAddr()),
        								Receiver.engine(mContext).getRemoteVideo());
    							rtp_socket.getDatagramSocket().setSoTimeout(15000);
//    						}else{
//    							Log.i("VideoCamera", "VideoCamera --> rtp_socket != null");
//    							rtp_socket.setDatagramSocket(socket = new SipdroidSocket(Receiver.engine(mContext).getLocalVideo()));
//								socket = rtp_socket.getDatagramSocket();
    						}
    							
    					} catch (Exception e) {
    						if (!Sipdroid.release) e.printStackTrace();
    						return;
    					}
    				
    					
    					//getListenerThread().start();
    					
    					InputStream fis = null;
						try {
		   					fis = receiver.getInputStream();
						} catch (IOException e1) {
							if (!Sipdroid.release) e1.printStackTrace();
							rtp_socket.getDatagramSocket().close();
							return;
						}
						
     					rtp_packet.setPayloadType(34);
     					
     					//Log.i("VideoCamera", "VideoCamera --> set payload type");
     					
    					while (Receiver.listener_video != null && videoValid()) {
    						num = -1;
    						try {
    							num = fis.read(buffer,14+number,frame_size-number);
    						} catch (IOException e) {
    							if (!Sipdroid.release) e.printStackTrace();
    							break;
    						}
    						if (num < 0) {
    							try {
    								sleep(20);
    							} catch (InterruptedException e) {
    								break;
    							}
    							continue;							
    						}
    						number += num;
    						head += num;
    						try {
								if (lasthead != head+fis.available() && ++stable >= 5) {
									now = SystemClock.elapsedRealtime();
									if (lasttime != 0) {
										fps = (int)((double)cnt*1000/(now-lasttime));
										avgrate = (double)fis.available()*1000/(now-lasttime);
									}
									if (cnt != 0 && len != 0)
										avglen = len/cnt;
									lasttime = now;
									lasthead = head+fis.available();
									len = cnt = stable = 0;
								}
							} catch (IOException e1) {
    							if (!Sipdroid.release) e1.printStackTrace();
    							break;
							}
    						
        					for (num = 14; num <= 14+number-2; num++)
    							if (buffer[num] == 0 && buffer[num+1] == 0) break;
    						if (num > 14+number-2) {
    							num = 0;
    							rtp_packet.setMarker(false);
    						} else {	
    							num = 14+number - num;
    							rtp_packet.setMarker(true);
    						}
    						
    			 			rtp_packet.setSequenceNumber(seqn++);
    			 			rtp_packet.setPayloadLength(number-num+2);
    			 			if (seqn > 10) try {
    			 				
    			 				//Log.i("VideoCamera", "VideoCamera --> sending packet");
    			 				
    			 				rtp_socket.send(rtp_packet);
        			 			len += number-num;
    			 			} catch (IOException e) {
    			 				if (!Sipdroid.release) e.printStackTrace();
    			 				break;
    			 			}
							
    			 			if (num > 0) {
    				 			num -= 2;
    				 			dest = 14;
    				 			src = 14+number - num;
    				 			if (num > 0 && buffer[src] == 0) {
    				 				src++;
    				 				num--;
    				 			}
    				 			number = num;
    				 			while (num-- > 0)
    				 				buffer[dest++] = buffer[src++];
    							buffer[12] = 4;
    							
    							cnt++;
    							try {
    								if (avgrate != 0)
    									Thread.sleep((int)(avglen/avgrate*1000));
								} catch (Exception e) {
    								break;
								}
        			 			rtp_packet.setTimestamp(SystemClock.elapsedRealtime()*90);
    			 			} else {
    			 				number = 0;
    							buffer[12] = 0;
    			 			}
    			 			if (change) {
    			 				change = false;
    			 				long time = SystemClock.elapsedRealtime();
    			 				
    	    					try {
    								while (fis.read(buffer,14,frame_size) > 0 &&
    										SystemClock.elapsedRealtime()-time < 3000);
    							} catch (Exception e) {
    							}
    			 				number = 0;
    							buffer[12] = 0;
    			 			}
    					}
    					rtp_socket.getDatagramSocket().close();
    					try {
							while (fis.read(buffer,0,frame_size) > 0);
						} catch (IOException e) {
						}
    				}
    			}).start();   
            }
    }
	
	static TelephonyManager tm;
	
	static boolean videoValid() {
		if (Receiver.on_wlan)
			return true;
		if (tm == null) tm = (TelephonyManager) Receiver.mContext.getSystemService(Context.TELEPHONY_SERVICE);
		if (tm.getNetworkType() < TelephonyManager.NETWORK_TYPE_UMTS)
			return false;
		return true;	
	}
	
	private boolean initializeVideo() {
        Log.v(TAG, "");
        
        Log.i("VideoCamera", "VideoCamera --> initializeVideo");
        
        if (mSurfaceHolder == null) {
        	Log.i("VideoCamera", "VideoCamera --> initializeVideo --> SurfaceHolder is null");
            Log.v(TAG, "SurfaceHolder is null");
            return false;
        }

        mMediaRecorderRecording = true;

        if (mMediaRecorder == null)
        	mMediaRecorder = new MediaRecorder();
        else
        	mMediaRecorder.reset();
        if (mCamera != null) {
        	mCamera.release();
        	mCamera = null;
        }

		if (isAvailableSprintFFC)
		{
			try
			{
				Method method = Class.forName("android.hardware.HtcFrontFacingCamera").getDeclaredMethod("getCamera", null);
				mCamera = (Camera) method.invoke(null, null);
			}
			catch (Exception ex)
			{
				Log.d(TAG, ex.toString());
			}
			VideoCameraNew.unlock(mCamera);
			mMediaRecorder.setCamera(mCamera);
	        //mVideoPreview.setOnClickListener(this);
		}
        //mVideoPreview.setOnLgClickListener(this);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mMediaRecorder.setOutputFile(sender.getFileDescriptor());

        // Use the same frame rate for both, since internally
        // if the frame rate is too large, it can cause camera to become
        // unstable. We need to fix the MediaRecorder to disable the support
        // of setting frame rate for now.
        mMediaRecorder.setVideoFrameRate(20);
        if (videoQualityHigh) {
            mMediaRecorder.setVideoSize(352,288);
        } else {
            mMediaRecorder.setVideoSize(176,144);
        }
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H263);
        mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());

        try {
            mMediaRecorder.prepare();
            mMediaRecorder.setOnErrorListener(this);
            mMediaRecorder.start();
        } catch (IOException exception) {
        	exception.printStackTrace();
            releaseMediaRecorder();
            //finish();
            return false;
        }
        return true;
    }
	
	private void releaseMediaRecorder() {
        Log.v(TAG, "Releasing media recorder.");
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }
	
	private void checkForCamera()
	{
		try
		{
			Class.forName("android.hardware.HtcFrontFacingCamera");
			isAvailableSprintFFC = true;
		}
		catch (Exception ex)
		{
			isAvailableSprintFFC = false;
		}
	}
	
	void setScreenBacklight(float a) {
		try{
        WindowManager.LayoutParams lp = ((Activity) mContext).getWindow().getAttributes(); 
        lp.screenBrightness = a; 
        ((Activity) mContext).getWindow().setAttributes(lp);
		}catch(ClassCastException e){ /*passing results as an error?*/ }
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		Log.i("VideoCamera", "VideoCamera --> surfaceChanged: "+!justplay+" "+!mMediaRecorderRecording);
		if (!justplay && !mMediaRecorderRecording) initializeVideo();
		
	}


	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.i("VideoCamera", "VideoCamera --> created");
		mSurfaceHolder = holder;
	}


	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		mSurfaceHolder = null;		
	}

	@Override
	public void onHangup() {
		callEnd();
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) { }

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (first) {
			first = false;
			return;
		}
		float distance = event.values[0];
        boolean active = (distance >= 0.0 && distance < InCallScreen.PROXIMITY_THRESHOLD && distance < event.sensor.getMaximumRange());
        setScreenBacklight((float) (active?0.1:-1));
		
	}
}
