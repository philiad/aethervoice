/**
 * @file AetherVoice.java
 * @brief It contains the AetherVoice activity class, the main activity class of the AetherVoice application.
 * @author Wyndale Wong
 */

package com.neugent.aethervoice.ui;

import java.io.File;
import java.util.List;

import org.sipdroid.media.RtpStreamReceiver;
import org.sipdroid.sipua.UserAgent;
import org.sipdroid.sipua.phone.Connection;
import org.sipdroid.sipua.ui.Receiver;
import org.sipdroid.sipua.ui.RegisterService;
import org.sipdroid.sipua.ui.Settings;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.TabActivity;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.neugent.aethervoice.R;
import com.neugent.aethervoice.log.ErrorAlert;


/**
 * @class AetherVoice
 * @brief It extends the TabActivity class and implements the Tab-window View
 *        for the AetherVoice application.
 * @author Wyndale Wong
 */
public class AetherVoice extends TabActivity {
	// ************************************************************* //
	// ********************* CLASS AND PACKAGES ******************** //
	// ************************************************************* //
	
	private static final String STRING_COMP_CLS_DIALER = "com.neugent.aethervoice.ui.AetherVoice";
	private static final String STRING_COMP_PKG_SERVICE = "com.neugent.service";
	private static final String STRING_COMP_CLS_SERVICE = "com.neugent.service.DialerService";

	// ************************************************************* //
	// *************************** Flags *************************** //
	// ************************************************************* //
	/** The flag that destroys the threads when lowered. **/
	public static boolean threadFlag = false;

	/** The flag that indicates whether the call listener has to be refreshed. **/
	public static boolean mustRefreshListener = false;

	/** The flag that indicates whether yellow pages are being browsed. **/
	private static boolean webViewing = false;

	/** The flag that indicates whether yellow pages web view is in full screen. **/
	private static boolean fullScreen = false;
	
	/** The flag that indicates whether the settings window is being viewed. **/
	private static boolean viewSettings = false;
	
	/**
	 * The flag that indicates whether the tabFrame is hidden because a contact is being viewed or edited. */
	private static boolean viewOrEdit = false;

	// XXX: removed because changed the ime to done
	// /** The flag that indicates whether the viewing of contacts is triggered
	// via contact search. **/
	// private static boolean fromSearch = false;

	/** The flag that indicates whether the UI is in call mode. **/
	public static boolean isCalling = false;

	/** The flag that indicates whether the call is running on background. **/
	public static boolean isOnBackGround = false;

	/** The flag that indicates whether the application is to be closed. **/
	public static boolean isFinishing = false;

	/** The flag that indicates whether the application is to be closed. **/
	public static boolean isScribbling = false;
	/** The flag that indicates there is an ongoing call. **/
	public static boolean isOngoing = false;

	/** The flag that indicates there is an incoming call. **/
	public static boolean isIncoming = false;

	/** The flag that indicates there is an outgoing call. **/
	public static boolean isOutgoing = false;

	/** The flag that indicates that the current call is on hold. **/
	public static boolean isOnHold = false;
	
	/** Determines if the activity is bound to the service. **/
	private boolean mIsBound = false;
	
	//both are for voip
	public static final boolean release = true;
	public static final boolean market = false;

	// ************************************************************* //
	// *************************** Views *************************** //
	// ************************************************************* //
	/** A TabHost object that contains the tab windows. */
	private static TabHost tabHost;
	
	/** The LinearLayout that contains the dialer interface. **/
	private static LinearLayout dialerFrame;

	/** The memo pad interface. **/
	private static EditText memoFrame;

	/** The LinearLayout that contains the tab windows. **/
	private static LinearLayout tabFrame;

	/** The LinearLayout that contains the view and edit contact info screen. **/
	private static LinearLayout infoFrame;

	/** The LinearLayout that contains the voip call screen. **/
	private static LinearLayout callFrame;

	/** The LinearLayout that contains the yellow page web view browser. **/
	private static LinearLayout webFrame;
	
	/** The LinearLayout that contains the settings display**/
	private static LinearLayout settingsFrame;
 
	/** The class that contains all the necessary views and modules for instantiating the dialer interface. */
	public static Dialer dialer;

	/** The class that contains all the necessary views and modules for instantiating the view contact interface. */
	private static ViewContactInfo viewContactInfo;

	/** The class that contains all the necessary views and modules for instantiating the edit contact interface. */
	private static EditContactInfo editContactInfo;

	/**  The class that contains all the necessary views and modules for instantiating the in-call interface. */
	public static CallScreen mCallScreen;
	
	/**  The class that contains all the necessary views and modules for instantiating the settings interface. */
	private static SettingsWindow settings;
	
	/** The web view that is used to browsed the yellow pages online. **/
	private static WebView webView;

	public static PSTNCallScreen pstnCallScreen;

	/** The global handler that listens to application-wide threads. **/
	public static CallHandler mHandler;
	
	/** The error dialog. **/
	private static ErrorAlert mErrorAlert;
	
	//	
	// /** The menu item for toggling memo pad. **/
	// public static final int MEMO_MENU_ITEM = Menu.FIRST + 1;
	
	// ************************************************************* //
	// *********************** MENU OPTIONS ************************ //
	// ************************************************************* //

	/** The menu item for toggling yellow page browsing. **/
	public static final int WEB_MENU_ITEM = Menu.FIRST + 1;

	/** The menu item for help. **/
	public static final int HELP_MENU_ITEM = Menu.FIRST + 2;

	/** The menu item for toggle full screen. **/
	public static final int FULL_MENU_ITEM = Menu.FIRST + 3;
	
	/** The menu item for the settings screen**/
	public static final int SETTINGS_MENU_ITEM = Menu.FIRST + 5; 
	/** The menu item for about. **/
	public static final int ABOUT_MENU_ITEM = Menu.FIRST + 4;

	// ************************************************************* //
	// ************ Messages received from the service ************* //
	// ************************************************************* //
	/** The message to sent to the service indicating to answer the call. */
	public static final int MSG_CALL_ANSWER = 125;

	/** The message to sent to the service indicating to end the call. */
	public static final int MSG_CALL_END = 126;

	/** The message to sent to the service indicating to put the call on hold. */
	public static final int MSG_CALL_HOLD = 127;

	/** The status message from the service indicating that there is an incoming call. */
	private static final int MSG_INCOMING_CALL = 100;

	/** The status message from the service indicating that there is an outgoing call. */
	public static final int MSG_OUTGOING_CALL = 101;

	/** The status message from the service indicating that there is an ongoing call. */
	private static final int MSG_ONGOING_CALL = 102;

	/** The status message from the service indicating that the call is on hold. */
	private static final int MSG_HOLD_CALL = 103;

	/** The status message from the service indicating that the call just ended. */
	private static final int MSG_END_CALL = 104;

	/** The status message from the service indicating that the manual hookswitch is offhook. */
	private static final int MSG_MANUAL_ON = 105;

	/** The status message from the service indicating that the manual hookswitch is onhook. */
	private static final int MSG_MANUAL_OFF = 106;
	
	/** The status message from the service indicating that the conntection of the service is disconnected from the serial . */
	private static final int MSG_NO_SERIAL = 175;
	
	// ************************************************************** //
	// **************** Messages sent to the service **************** //
	// ************************************************************** //
	/** The status message sent to the service indication that we to enable handsfree mode. **/
	/* private static final int MSG_SPEAKER_ON = 150; */
	
	/** The status message sent to the service indication that we to disable handsfree mode. **/
	/*private static final int MSG_SPEAKER_OFF = 151;*/

	/** The status message sent to the service indication that we to mute the mic. **/
	public static final int MSG_MUTE_ON = 154;
	
	/** The status message sent to the service indication that we to unmute the mic. **/
	public static final int MSG_MUTE_OFF = 155;
	
	/** The status message sent to the service indication that we to enable handsfree mode. **/
	public static final int MSG_SPEAKERCALL_ON = 152;
	
	/** The status message sent to the service indication that we to disable handsfree mode. **/
	public static final int MSG_SPEAKERCALL_OFF = 153;
	
	/** The status message sent to the service indication that we to adjust the volume. **/
	public static final int MSG_VOLUME_CONTROL = 160;
	
	/** Enables the communication with the client and pauses all media. **/
	private static final int MSG_REGISTER_CLIENT = 501;

	/** Disables the communication with the client. **/
	private static final int MSG_UNREGISTER_CLIENT = 502;

	/** Removes the notification **/
	private static final int DIALER_SERVICE_NOTIFICATION = 500;

//	public static int mCallDuration = 0;
	
	// ************************************************************** //
	// *************************** CALLS **************************** //
	// ************************************************************** //

	/** The time the call has started.  **/
	public static long mstartCallTime;

	/** The number being called.  **/
	public static String mCallNumber = new String();
	
	/** Thread that plays a silent tone. **/
	private Thread playThread;

//	private Thread mCallThreadTimer; // for testing

//	private Call mPSTNCall;
	
	// ************************************************************** //
	// *************************** OTHERS **************************** //
	// ************************************************************** //
	/** The current page number in viewing the help dialog. **/
	private static int helpIndex = 0;
	
	public static boolean bannerFlag = false;
	
	/**
	 * Initializes the AetherVoice Screen and its contents. Called when the
	 * AetherVoice application is launched.
	 * 
	 * @see #initViews()
	 */
	@Override
	public void onCreate(final Bundle icicle) {
		super.onCreate(icicle);
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.aethervoice);
		AetherVoice.mErrorAlert = new ErrorAlert(this);
		
		initViews();
	}


	/**
	 * Initializes the AetherVoice main screen, creating the four layout frames
	 * and for tab windows.
	 * <ul>
	 * <li>Dialer Frame</li>
	 * <li>Tab Frame</li>
	 * <li>Info Frame</li>
	 * <li>Web Frame</li>
	 * <li>Call Frame</li>
	 * <li>Settings Frame</li>
	 * </ul>
	 * Each frame contains the views belonging to certain sections of the user
	 * interface. Sections are separated in order to conveniently manipulate the
	 * visibility and refreshing of each section.
	 * 
	 * <ul>
	 * <li>ContactListWindow</li>
	 * <li>CallHistoryWindow</li>
	 * <li>DialerWindow</li>
	 * <li>DirectoryWindow</li>
	 * </ul>
	 * Each window behaves as an individual activity, assuming the content of
	 * that particular activity class. The activity of a respective tab window
	 * is only launched on the first time its tab is pressed, and after which,
	 * the activity is simply paused or resumed when its tab is set to active or
	 * inactive. The activities inside the tab windows are only destroyed when
	 * the parent activity class holding the TabHost object is destroyed.
	 * 
	 * @see Dialer
	 * @see Dialer#getDialerView()
	 * @see SettingsWindow
	 * @see SettingsWindow#getSettingsView()
	 */
	private void initViews() {

		// Instantiating the frames
		AetherVoice.dialerFrame = (LinearLayout) findViewById(R.id.dialer_frame);
		AetherVoice.memoFrame = (EditText) findViewById(R.id.memo_frame);
		AetherVoice.tabFrame = (LinearLayout) findViewById(R.id.tab_frame);
		AetherVoice.infoFrame = (LinearLayout) findViewById(R.id.info_frame);
		AetherVoice.callFrame = (LinearLayout) findViewById(R.id.call_frame);
		AetherVoice.webFrame = (LinearLayout) findViewById(R.id.web_frame);
		AetherVoice.webView = (WebView) findViewById(R.id.web_view);
		
		AetherVoice.webView.requestFocus(View.FOCUS_DOWN);
		AetherVoice.webView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_UP:
                    if (!v.hasFocus()) {
                        v.requestFocus();
                    }
                    break;
            }
            return false;
			}
        });
		
		AetherVoice.settingsFrame = (LinearLayout)findViewById(R.id.settings_frame);
		
		//AetherVoice.tabFrame.setVisibility(View.GONE);
		
		//setting which view will appear
		AetherVoice.memoFrame.setVisibility(View.GONE);
		AetherVoice.infoFrame.setVisibility(View.GONE);
		AetherVoice.callFrame.setVisibility(View.GONE);
		AetherVoice.webFrame.setVisibility(View.GONE);
		AetherVoice.settingsFrame.setVisibility(View.GONE);
		
		// Instantiating the dialer frame
		AetherVoice.threadFlag = true;
		AetherVoice.bannerFlag = true;
		AetherVoice.dialer = new Dialer(this);
		AetherVoice.dialerFrame.addView(AetherVoice.dialer.getDialerView(),
				new LayoutParams(LayoutParams.FILL_PARENT,
						LayoutParams.FILL_PARENT));
		
		// Instantiating the settings frame
		AetherVoice.settings = new SettingsWindow(this);
		AetherVoice.settingsFrame.addView(AetherVoice.settings.getSettingsView(),
				new LayoutParams(LayoutParams.FILL_PARENT,
						LayoutParams.FILL_PARENT));

		// loads the context to the receiver class
		Receiver.loadReceiverContext(this);
		
//		mPSTNCall = new Call();

		// instantiates the global handler
		AetherVoice.mHandler = new CallHandler();

		// starts the refresh listener thread
		getRefreshListenerThread().start();

		// Instantiating the tab windows
		AetherVoice.tabHost = getTabHost();

		//Adding tabs
		AetherVoice.tabHost.addTab(AetherVoice.tabHost.newTabSpec(
				getString(R.string.tabname_speed_dial)).setIndicator(
				getString(R.string.tablabel_speed_dial),
				getResources().getDrawable(R.drawable.tab_speed_dial_bg))
				.setContent(new Intent(this, SpeedDialWindow.class)));

		AetherVoice.tabHost.addTab(AetherVoice.tabHost.newTabSpec(
				getString(R.string.tabname_contact_list)).setIndicator(
				getString(R.string.tablabel_contact_list),
				getResources().getDrawable(R.drawable.tab_contacts_bg))
				.setContent(new Intent(this, ContactListWindow.class)));

		AetherVoice.tabHost.addTab(AetherVoice.tabHost.newTabSpec(
				getString(R.string.tabname_call_history)).setIndicator(
				getString(R.string.tablabel_call_history),
				getResources().getDrawable(R.drawable.tab_call_history_bg))
				.setContent(new Intent(this, CallHistoryWindow.class)));

		AetherVoice.tabHost.addTab(AetherVoice.tabHost.newTabSpec(
				getString(R.string.tabname_directory)).setIndicator(
				getString(R.string.tablabel_directory),
				getResources().getDrawable(R.drawable.tab_settings_bg))
				.setContent(new Intent(this, PldtDirectory.class)));
		
		
		/*AetherVoice.tabHost.addTab(tabHost.newTabSpec(
				getString(R.string.tabname_settings)).setIndicator(
				getString(R.string.tablabel_settings),
				getResources().getDrawable(R.drawable.tab_settings_bg))
				.setContent(new Intent(this, SettingsWindow.class)));*/
		
		/*AetherVoice.tabHost.addTab(tabHost.newTabSpec(
				getString(R.string.tabname_settings)).setIndicator(
				getString(R.string.tablabel_settings),
				getResources().getDrawable(R.drawable.tab_settings_bg))
				.setContent(new Intent(this, CallScreen.class)));*/
		
		// set the text size...
		//TODO: find a better way of setting the text size
		final int count = AetherVoice.tabHost.getTabWidget().getChildCount();
		for (int i = 0; i < count; i++)
			((TextView) ((RelativeLayout) AetherVoice.tabHost.getTabWidget()
					.getChildAt(i)).getChildAt(1)).setTextAppearance(this,
					R.style.tabText);
	}

	/**
	 * Launches the view contact info screen.
	 * 
	 * @param context The application context
	 * @param id The id of the contact to be displayed
	 * @param name The name of the contact to be displayed
	 * 
	 * @see ViewContactInfo#getContactInfoView()
	 */
	public static void showViewContactInfo(final Context context,
			final String id, final String name, final int starred) {
		AetherVoice.viewOrEdit = true;
		AetherVoice.isFinishing = false;

		AetherVoice.viewContactInfo = new ViewContactInfo(context, id, name,
				starred);
		AetherVoice.infoFrame.addView(AetherVoice.viewContactInfo
				.getContactInfoView(), new LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

		if(!isCalling){
			AetherVoice.tabFrame.setVisibility(View.GONE); //added by PJ 
			AetherVoice.infoFrame.setVisibility(View.VISIBLE);
		}
		

		// removed because changed the ime to done - aj
		//reloads the dialer frame view contact info is launched via search
		// if(fromSearch) {
		// dialerFrame. removeAllViews();
		// dialerFrame. addView(dialer.getDialerView(), new
		// LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		// fromSearch = false;
		// }
	}

	/**
	 * Launches the edit contact info screen.
	 * 
	 * @param context The application context
	 * @param id The id of the contact to be edited
	 * @param name The name of the contact to be edited
	 * @param number A newly added number of the contact to be edited
	 * @param starred Identifies if the contact is in speeddial list
	 * 
	 * @see EditContactInfo#getEditContactInfoView()
	 */
	public static void showEditContactInfo(final Context context, final String id, final String name, final String number, final int starred) {
		AetherVoice.viewOrEdit = true;
		AetherVoice.isFinishing = false;

		AetherVoice.editContactInfo = new EditContactInfo(context, id, name,
				number, starred);
		AetherVoice.infoFrame.addView(AetherVoice.editContactInfo
				.getEditContactInfoView(), new LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

		if(!isCalling){
			AetherVoice.tabFrame.setVisibility(View.GONE);
			AetherVoice.infoFrame.setVisibility(View.VISIBLE);
		}
	}

	/**
	 * Hides the view and edit contact info screen.
	 * 
	 * @param mustRunOnBackGround Whether the view or edit contact screen is to be retained on background.
	 */
	public static void hideInfoFrame(final boolean mustRunOnBackGround) {
		if (!mustRunOnBackGround) {
			AetherVoice.viewOrEdit = false;
			AetherVoice.infoFrame.removeAllViews();
		}
		
		AetherVoice.infoFrame.setVisibility(View.GONE);
		
		if(!isCalling && !viewSettings)
			AetherVoice.tabFrame.setVisibility(View.VISIBLE); //added by pj to prevent tabframe and callscreen appearing at the same time
	}


	/**
	 * Displays the call screen
	 * 
	 * @param context The application context
	 * 
	 * @see #hideContactInfo()
	 * @see CallScreen#getCallScreenView()
	 * @see PSTNCallScreen#getPSTNCallScreenView()
	 */
	public static void showCallFrame(final Context context) {
//		System.out.println(">>>>>>>>>>>>>>> hiding all screens");
		
		
		if (AetherVoice.viewOrEdit)
			AetherVoice.infoFrame.setVisibility(View.GONE);

		if (AetherVoice.webViewing) {
			AetherVoice.fullScreen = false;
			AetherVoice.webViewing = false;

			if (AetherVoice.isScribbling)
				AetherVoice.memoFrame.setVisibility(View.VISIBLE);
			else
				AetherVoice.dialerFrame.setVisibility(View.VISIBLE);
			
			AetherVoice.webFrame.setVisibility(View.GONE);
		}

		if (AetherVoice.viewSettings){
			AetherVoice.viewSettings = false;
			AetherVoice.settingsFrame.setVisibility(View.GONE);
		}
		
		if (Dialer.isVoip) {
//			System.out.println(">>>>>>>>>>>>>>> showing call screen + "+mCallScreen);
			if (AetherVoice.mCallScreen == null)
				AetherVoice.mCallScreen = new CallScreen(context);
			
			if (!AetherVoice.isCalling && !(AetherVoice.callFrame.getChildCount() > 0)) {
				
				AetherVoice.isCalling = true;
				AetherVoice.tabFrame.setVisibility(View.GONE);
				
				AetherVoice.callFrame.addView(AetherVoice.mCallScreen
						.getCallScreenView(), new LayoutParams(
						LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
				
				AetherVoice.callFrame.setVisibility(View.VISIBLE);
			}else{
				if(AetherVoice.isOnBackGround){
//					System.out.println("TODO SOMETHING");
				}else{
					
					AetherVoice.tabFrame.setVisibility(View.GONE);
					
//					System.out.println(callFrame.getChildCount());
					AetherVoice.callFrame.setVisibility(View.VISIBLE);
				}
				
					
			}

			(new Thread(new Runnable() {
				public void run() {
					for (;;)
						if (AetherVoice.callFrame.getHeight() > 0) {
							AetherVoice.mHandler.sendEmptyMessage(CallHandler.MSG_DISPLAY_CALL);
							break;
						}
				}
			})).start();
		} else {
			
			if (AetherVoice.pstnCallScreen == null)
				AetherVoice.pstnCallScreen = new PSTNCallScreen(context);
			
			if (!AetherVoice.isCalling
					&& !(AetherVoice.callFrame.getChildCount() > 0)) {
				AetherVoice.isCalling = true;
				AetherVoice.tabFrame.setVisibility(View.GONE);
				AetherVoice.callFrame.setVisibility(View.VISIBLE);
				AetherVoice.callFrame.addView(AetherVoice.pstnCallScreen
						.getPSTNCallScreenView(), new LayoutParams(
						LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
			}
		}
		
	}

	/**
	 * Hides the call screen.
	 * 
	 * @param callOnBackGround Whether the call is retained on background
	 * 
	 * @see CallHistoryWindow#setMustUpdateCallHistory()
	 */
	public static void hideCallFrame(final boolean mustRunOnBackGround) {
		CallHistoryWindow.setMustUpdateCallHistory();
		
		if (!AetherVoice.isOnBackGround && AetherVoice.isCalling) {

			AetherVoice.isOnBackGround = mustRunOnBackGround;
			if (Dialer.isVoip)
				AetherVoice.mCallScreen.getCallScreenView().clearFocus();
			AetherVoice.callFrame.setVisibility(View.GONE);

			if (AetherVoice.viewOrEdit)
				AetherVoice.infoFrame.setVisibility(View.VISIBLE);

			if (AetherVoice.webViewing)
				AetherVoice.webFrame.setVisibility(View.VISIBLE);
			
			if (AetherVoice.viewSettings)
				AetherVoice.settingsFrame.setVisibility(View.VISIBLE);

			if (!AetherVoice.viewOrEdit && !AetherVoice.webViewing)
				AetherVoice.tabFrame.setVisibility(View.VISIBLE);
		}


		if (!mustRunOnBackGround && AetherVoice.isCalling) {
			
			
			AetherVoice.callFrame.setVisibility(View.GONE);
			if (AetherVoice.viewOrEdit)
				AetherVoice.infoFrame.setVisibility(View.VISIBLE);

			if (AetherVoice.webViewing)
				AetherVoice.webFrame.setVisibility(View.VISIBLE);

			if (AetherVoice.viewSettings)
				AetherVoice.settingsFrame.setVisibility(View.VISIBLE);
			
			if (!AetherVoice.viewOrEdit && !AetherVoice.webViewing && !AetherVoice.viewSettings)
				AetherVoice.tabFrame.setVisibility(View.VISIBLE);
			

			if(Dialer.isVoip){
//				mCallScreen = null;
			}else{
				pstnCallScreen = null;
			}
			
//			System.out.println(">>>>>>>>>>>>>>>>>>>>>> removing all views hehehe ");
			
			
			AetherVoice.callFrame.removeAllViews();
			AetherVoice.isOnBackGround = false;
			AetherVoice.isCalling = false;
			
		}

		
	}

	/**
	 * Creates the thread that refreshes the call listener.
	 * 
	 * @return The Thread object
	 * 
	 * @see Receiver#engine(Context)
	 */
	private Thread getRefreshListenerThread() {
		return new Thread(new Runnable() {
			public void run() {
				while (AetherVoice.threadFlag) {
					if (AetherVoice.mustRefreshListener)
						Receiver.engine(getApplicationContext()).listen();
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
	 * Selects the active tab window in the main AetherVoice screen.
	 * 
	 * @param index the integer value of the tab window to be selected
	 */
	public static void setTab(final int index) {
		AetherVoice.tabHost.setCurrentTab(index);
	}

	// XXX: removed because changed the ime to done
	// /**
	// * Raises the fromSearch flag.
	// */
	// public static void setFromSearch() {
	// fromSearch = true;
	// }

	/** Sets the isFinishing flag. */
	public static void setIsFinishing(final boolean mustFinish) {
		AetherVoice.isFinishing = mustFinish;
	}

	/** Sets the mustRefreshListener flag. */
	public static void setMustRefreshListener(final boolean mustRefresh) {
		AetherVoice.mustRefreshListener = mustRefresh;
	}

	/**
	 * Sets the string to be displayed on the dialBox.
	 * 
	 * @param inputString the string to be displayed
	 */
	public static void setInput(final String inputString) {
		AetherVoice.dialer.dialBox.setText(inputString);
	}
	
	/** Sets the isScribbling flag. */
	public static void setScribbling(boolean flag){
		AetherVoice.isScribbling = flag;
	}

	/**
	 * Defines the AlertDialog for help.
	 * 
	 * @param helpIndex The help message index
	 * @return The AlertDialog object
	 */
	public static AlertDialog getHelpDialog(final Context context) { //XXX
		helpIndex = 0;
		final Integer[] helpMessage = { R.string.help_message_dialer,R.string.help_message_speed_dial,R.string.help_message_contacts, 
				R.string.help_message_call_history, R.string.help_message_settings};
		final AlertDialog.Builder helpDialog = new AlertDialog.Builder(context).setIcon(android.R.drawable.ic_dialog_info).setTitle(R.string.help_title);
		helpDialog.setMessage(R.string.help_message_dialer);
		helpDialog.setPositiveButton("Previous", new DialogInterface.OnClickListener() {
				public void onClick(final DialogInterface dialog,
				 int whichButton) {
					if (helpIndex > 0) {helpIndex --;
						helpDialog.setMessage(helpMessage[helpIndex]).show();}
					}
				});
			helpDialog.setNegativeButton("Next", new DialogInterface.OnClickListener() {
							public void onClick(final DialogInterface dialog,
									int whichButton) {
								if(helpIndex < 4){
									helpIndex++;
								helpDialog.setMessage(helpMessage[helpIndex]).show();
								}
							}
						});
		return helpDialog.create();
		
//		System.out.println(AetherVoice.tabHost.getCurrentTab());
//		String help_string = "";
//		if (AetherVoice.tabHost.getCurrentTab() == 0)
//			help_string = context.getString(R.string.help_message_dialer)+"\n " +context.getString(R.string.help_message_speed_dial);
//		else if (AetherVoice.tabHost.getCurrentTab() == 1)
//			help_string = context.getString(R.string.help_message_contacts);
//		else if (AetherVoice.tabHost.getCurrentTab() == 2)
//			help_string = context.getString(R.string.help_message_call_history);
//		else if (AetherVoice.tabHost.getCurrentTab() == 3)
//			help_string = context.getString(R.string.help_message_settings);
//		else help_string = context.getString(R.string.help_message_dialer);
//		
//		return new AlertDialog.Builder(context).setIcon(
//				android.R.drawable.ic_dialog_info)
//				.setTitle(R.string.help_title).setMessage(
//					help_string)
//				.setPositiveButton(context.getString(R.string.alert_button_ok),
//						new DialogInterface.OnClickListener() {
//							public void onClick(final DialogInterface dialog,
//									final int whichButton) {
//							}
//						}).create();
		
		
	}

	/**
	 * Initialize the contents of the AetherVoics's standard options menu.
	 * Creates the screen menu options, including their labels and drawable
	 * icons.
	 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		super.onCreateOptionsMenu(menu);
		// menu.add(0, MEMO_MENU_ITEM, 0, R.string.menu_memo);
		//menu.add(0, AetherVoice.WEB_MENU_ITEM, 0, R.string.menu_web);
		//menu.add(0, AetherVoice.FULL_MENU_ITEM, 0, R.string.menu_full);
		menu.add(0, AetherVoice.SETTINGS_MENU_ITEM,0 , R.string.menu_settings)			
			.setIcon(MyUtils.resizeImage(getApplicationContext(), R.drawable.settings_voip, 60, 60));
		menu.add(0, AetherVoice.HELP_MENU_ITEM, 0, R.string.menu_help)
			.setIcon(MyUtils.resizeImage(getApplicationContext(), R.drawable.settings_help, 60, 60));
		menu.add(0, AetherVoice.ABOUT_MENU_ITEM, 0, R.string.menu_about)
			.setIcon(MyUtils.resizeImage(getApplicationContext(), R.drawable.settings_about, 60, 60));		
		// menu.add(0, MENU_ITEM_TEST, 0, "Run Call Test");
		// menu.add(0, MENU_ITEM_RESULT, 0, "Export Result");
		return true;
	}

	/**
	 * Prepare the Screen's standard options menu to be displayed. This is
	 * called right before the menu is shown, every time it is shown. The full
	 * screen options is made visible or invisible depending if yellow page
	 * viewing is toggled on.
	 */
	@Override
	public boolean onPrepareOptionsMenu(final Menu menu) {
//		if (AetherVoice.webViewing) {
//			menu.findItem(AetherVoice.FULL_MENU_ITEM).setVisible(true);
//			if (AetherVoice.fullScreen) {
//				menu.findItem(AetherVoice.FULL_MENU_ITEM).setTitle(R.string.menu_minimize);				
//			} else {
//				menu.findItem(AetherVoice.FULL_MENU_ITEM).setTitle(R.string.menu_full);					
//			}						
//		} else {
//			menu.findItem(AetherVoice.FULL_MENU_ITEM).setVisible(false);			
//		}
			

		// if(isRecording){
		// menu.findItem(MENU_ITEM_TEST).setVisible(false);
		// menu.findItem(MENU_ITEM_RESULT).setVisible(true);
		// }else{
		// menu.findItem(MENU_ITEM_TEST).setVisible(true);
		// menu.findItem(MENU_ITEM_RESULT).setVisible(false);
		// }

		// if(fullScreen) {
		// menu.findItem(MEMO_MENU_ITEM).setVisible(false);
		//			
		// } else {
		// menu.findItem(MEMO_MENU_ITEM).setVisible(true);
		// }
		return true;
	}

	/**
	 * Defines the lines of code to be executed when a screen menu option is
	 * selected.
	 * <ul>
	 * <li> Toggle Yellow Page Browsing - Opens the web frame for yellow page
	 * online browsing</li>
	 * <li> Help Screen - Opens the help screen</li>
	 * <li> Toggle Full Screen - Toggles full screen during browsing</li>
	 * <li> About - Displays details about the application
	 * <li> VoIP Settings - Displays the Voip Settings
	 * </ul>
	 * 
	 * @see #hideInfoFrame(boolean)
	 * @see #getHelpDialog(Context)
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		// case MEMO_MENU_ITEM:{
		// if(isScribbling) {
		// isScribbling = false;
		// dialerFrame. setVisibility(View.VISIBLE);
		// memoFrame. setVisibility(View.GONE);
		//            		
		// } else {
		// isScribbling = true;
		// dialerFrame. setVisibility(View.GONE);
		// memoFrame. setVisibility(View.VISIBLE);
		// }
		// return true;
		// }
		case WEB_MENU_ITEM: {
			if (AetherVoice.webViewing) {
				AetherVoice.webViewing = false;
				AetherVoice.webView.stopLoading();
				AetherVoice.fullScreen = false;

				AetherVoice.webFrame.setVisibility(View.GONE);

				if (AetherVoice.isScribbling)
					AetherVoice.memoFrame.setVisibility(View.VISIBLE);
				else
					AetherVoice.dialerFrame.setVisibility(View.VISIBLE);

				if (AetherVoice.isCalling)
					AetherVoice.callFrame.setVisibility(View.VISIBLE);
				else if (AetherVoice.viewOrEdit)
					AetherVoice.infoFrame.setVisibility(View.VISIBLE);
				else if (AetherVoice.viewSettings)
					AetherVoice.settingsFrame.setVisibility(View.VISIBLE);
				else
					AetherVoice.tabFrame.setVisibility(View.VISIBLE);

			} else {
				AetherVoice.webViewing = true;
				AetherVoice.isFinishing = false;

				if (AetherVoice.isCalling)
//					hideCallFrame(true);
					AetherVoice.callFrame.setVisibility(View.GONE);

				if (AetherVoice.viewOrEdit)
					AetherVoice.hideInfoFrame(true);
				if (AetherVoice.viewSettings){
					AetherVoice.viewSettings = false;
					AetherVoice.settingsFrame.setVisibility(View.GONE);
				}

				AetherVoice.webView.getSettings().setJavaScriptEnabled(true);
				AetherVoice.webView.getSettings().setBuiltInZoomControls(true);
				AetherVoice.webView.getSettings().setLoadsImagesAutomatically(
						true);
				AetherVoice.webView
						.loadUrl(getString(R.string.yellow_page_url));

				AetherVoice.tabFrame.setVisibility(View.GONE);
				AetherVoice.webFrame.setVisibility(View.VISIBLE);
			}
			return true;
		}
		case HELP_MENU_ITEM: {
			AetherVoice.getHelpDialog(this).show();
			return true;
		}
		case FULL_MENU_ITEM: {
			if(AetherVoice.isCalling){
			}
			else{
				if (AetherVoice.fullScreen) { 
					AetherVoice.fullScreen = false;
					
					if (AetherVoice.isScribbling)
						AetherVoice.memoFrame.setVisibility(View.VISIBLE);
					else
						AetherVoice.dialerFrame.setVisibility(View.VISIBLE);
				} else {
					AetherVoice.fullScreen = true;
					if (AetherVoice.isScribbling)
						AetherVoice.memoFrame.setVisibility(View.GONE);
					else
						AetherVoice.dialerFrame.setVisibility(View.GONE);
				}
			}
			return true;
		}
		case ABOUT_MENU_ITEM: {
			//TODO: expand this
			AetherVoice.mErrorAlert.showErrorDialog("About", getVersionName(this, STRING_COMP_CLS_DIALER));
			return true;
		}
		case SETTINGS_MENU_ITEM:{
//			System.out.println("Settings chosen");
			if (!AetherVoice.isCalling){
				if (AetherVoice.viewSettings){
			
				AetherVoice.viewSettings = false;
//				if (AetherVoice.isCalling){
//					AetherVoice.showCallFrame(this);
//				}else
					if (AetherVoice.webViewing){
//					if (AetherVoice.fullScreen)
//						AetherVoice.fullScreen = false;
					AetherVoice.webFrame.setVisibility(View.VISIBLE);
				}
				else{ 
					AetherVoice.tabFrame.setVisibility(View.VISIBLE);
					
					
				}
//			if (AetherVoice.isCalling){
//				AetherVoice.callFrame.setVisibility(View.VISIBLE);
//			}
			AetherVoice.settingsFrame.setVisibility(View.GONE);
			}else {
				AetherVoice.viewSettings = true;
				AetherVoice.tabFrame.setVisibility(View.GONE);
				AetherVoice.settingsFrame.setVisibility(View.VISIBLE);
//				if (AetherVoice.isCalling)
////					AetherVoice.hideCallFrame(false);
//					AetherVoice.callFrame.setVisibility(View.GONE);
				if (AetherVoice.viewOrEdit){
					AetherVoice.hideInfoFrame(false);
				}
				if (AetherVoice.webViewing){
					if (AetherVoice.fullScreen){
						AetherVoice.fullScreen = false;
						AetherVoice.dialerFrame.setVisibility(View.VISIBLE);}
					AetherVoice.webViewing = false;
					AetherVoice.webFrame.setVisibility(View.GONE);
				}
				
			}
			}
			return true;
		}
		}
		return super.onOptionsItemSelected(item);
	}
	
	/** Called when user clicks the about menu */
	public static String getVersionName(Context context, String cls) {
		try {
			ComponentName comp = new ComponentName(context, cls);
			PackageInfo pinfo = context.getPackageManager().getPackageInfo(comp.getPackageName(), 0);
						
			return pinfo.versionName;
		} catch (android.content.pm.PackageManager.NameNotFoundException e) {
			return null;
		}
	}

	/**
	 * Called when a hardware button is pressed down
	 * <ul>
	 * <li> There is an onGoing call
	 * <ul>
	 * <li> KEYCODE_CALL answers the incoming call or toggles the call to be on hold
	 * <li> KEYCODE_BACK Ends the call
	 * <li> KEYCODE_CAMERA Disable the camera
	 * <li> KEYCODE_VOLUME_DOWN / KEYCODE_VOLUME_UP Stops the ringtone and adjust the volume accordingly
	 * </ul>
	 * <li> There is no onGoing call
	 * <ul>
	 * <li> KEYCODE_CALL Disables the button
	 * <li> KEYCODE_ENDCALL Disables the button
	 * <li> KEYCODE_BACK Reverts to the previous view or cancels incoming call otherwise exit the application
	 * </ul>
	 * </ul>
	 * 
	 * @see #hideInfoFrame(boolean)
	 * @see #hideCallFrame(boolean)
	 */
	@Override
	public boolean onKeyDown(final int keyCode, final KeyEvent event) {
		if (AetherVoice.isCalling)
			switch (keyCode) {
/*			case KeyEvent.KEYCODE_MENU:
				if (Receiver.call_state == UserAgent.UA_STATE_INCOMING_CALL) {
					AetherVoice.mCallScreen.answer();
					return true;
				}
				break;*/

			case KeyEvent.KEYCODE_CALL:
				//TODO: add pstn on hold module here
				switch (Receiver.call_state) {
				case UserAgent.UA_STATE_INCOMING_CALL:
					AetherVoice.mCallScreen.answer();
					break;
				case UserAgent.UA_STATE_INCALL:
				case UserAgent.UA_STATE_HOLD:
					Receiver.engine(getApplicationContext()).togglehold();
					break;
				}
				// consume KEYCODE_CALL so PhoneWindow doesn't do anything with
				// it
				return true;

			case KeyEvent.KEYCODE_BACK:
				if (Dialer.isVoip) {
					if (Receiver.call_state == UserAgent.UA_STATE_INCOMING_CALL)
						AetherVoice.mCallScreen.reject();
//					else
//						if(isOnBackGround){
//							mCallScreen.reject();
//							AetherVoice.hideCallFrame(false);
//							}
//						else
//							AetherVoice.hideCallFrame(true);
//					return true;
				} else
					if(isIncoming)
						AetherVoice.dialer.endCall(true, false);
					else
						AetherVoice.dialer.endCall(false, true);

			case KeyEvent.KEYCODE_CAMERA:
				// Disable the CAMERA button while in-call since it's too
				// easy to press accidentally.
				return true;
			case KeyEvent.KEYCODE_VOLUME_DOWN:
			case KeyEvent.KEYCODE_VOLUME_UP:
				if (Receiver.call_state == UserAgent.UA_STATE_INCOMING_CALL) {
					Receiver.stopRingtone();
					return true;
				}
				RtpStreamReceiver.adjust(keyCode, true);
				return true;
			}
		else
			switch (keyCode) {
			case KeyEvent.KEYCODE_CALL:
				return true;
			case KeyEvent.KEYCODE_ENDCALL:
				return true;
			case KeyEvent.KEYCODE_BACK: {

				
				if (AetherVoice.webViewing) {
					if (AetherVoice.isScribbling)
						AetherVoice.memoFrame.setVisibility(View.VISIBLE);
					else
						AetherVoice.dialerFrame.setVisibility(View.VISIBLE);
					if (AetherVoice.fullScreen)
						AetherVoice.fullScreen = false;
					else {
						AetherVoice.webViewing = false;
						AetherVoice.webView.stopLoading();
						if (AetherVoice.viewOrEdit)
							AetherVoice.infoFrame.setVisibility(View.VISIBLE);
						else if (AetherVoice.viewSettings)
							AetherVoice.settingsFrame.setVisibility(View.VISIBLE);
						else
							AetherVoice.tabFrame.setVisibility(View.VISIBLE);
						AetherVoice.webFrame.setVisibility(View.GONE);
					}
				} else if (AetherVoice.viewOrEdit)
					AetherVoice.hideInfoFrame(false);
				else if (AetherVoice.viewSettings){
					AetherVoice.viewSettings = false;
					AetherVoice.settingsFrame.setVisibility(View.GONE);
					AetherVoice.tabFrame.setVisibility(View.VISIBLE);
				}
				else if (AetherVoice.isScribbling) {
					AetherVoice.isScribbling = false;
					AetherVoice.dialerFrame.setVisibility(View.VISIBLE);
					AetherVoice.memoFrame.setVisibility(View.GONE);
					AetherVoice.dialer.closePanel();
				} else {
					if (AetherVoice.isIncoming && !AetherVoice.isOngoing)
						AetherVoice.dialer.endCall(true, true);
					else
						AetherVoice.sendServiceMessage(AetherVoice.MSG_CALL_END);

					if (AetherVoice.isFinishing) {
						AetherVoice.bannerFlag = false;						
						finish();
					} else {
						Toast.makeText(getApplicationContext(),
								R.string.toast_finish, Toast.LENGTH_SHORT)
								.show();
						AetherVoice.isFinishing = true;
					}
				}
				return true;
			}
			}
		return super.onKeyDown(keyCode, event);
	}

	/** Called when a hardware button is lifted up from a pressed state.
	 * 	Handles the events when there is an ongoing call.
	 * <ul>
	 * <li> KEYCODE_VOLUME_DOWN / KEYCODE_VOLUME_UP Adjust the volume accordingly
	 * <li> KEYCODE_ENDCALL		The call will be ended
	 * </ul>
	 * @see Activity#onKeyUp(int, KeyEvent)
	 * 
	 * Actually these are never called because our device does not have these buttons.
	 * For future use .... probably
	 **/
	@Override
	public boolean onKeyUp(final int keyCode, final KeyEvent event) {
		if (AetherVoice.isCalling) {
			switch (keyCode) {
			case KeyEvent.KEYCODE_VOLUME_DOWN:
			case KeyEvent.KEYCODE_VOLUME_UP:
				RtpStreamReceiver.adjust(keyCode, false);
				return true;
			case KeyEvent.KEYCODE_ENDCALL:
				if (Dialer.isVoip) {
					if (Receiver.pstn_state == null
							|| (Receiver.pstn_state.equals("IDLE") && (SystemClock
									.elapsedRealtime() - Receiver.pstn_time) > 3000)) {
						AetherVoice.mCallScreen.reject();
						return true;
					}
				} else if (AetherVoice.isIncoming && !AetherVoice.isOngoing)
					AetherVoice.dialer.endCall(true, false);
				else {
//					AetherVoice.dialer.btnCall.performClick();
					AetherVoice.sendServiceMessage(AetherVoice.MSG_CALL_END);
				}

				break;
			}
			Receiver.pstn_time = 0;
		}
		return super.onKeyUp(keyCode, event);
	}

	/**
	 * Called when the activity is resumed.
	 * Bind the Activity to the service and set the tab accordingly.
	 * @see Activity#onResume
	 * @see #doBindService()
	 */
	@Override
	public void onResume() {
		super.onResume();
		AetherVoice.bannerFlag = true;
		
//		System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> onResume");
		
//		if(isOnBackGround){
//			hideCallFrame(false);
//		}else{
//			if (Receiver.call_state != UserAgent.UA_STATE_IDLE){
//				Receiver.moveTop();
//				System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> MOVING TO TOP");
//			}
//		}
//		if (AetherVoice.isCalling) hideCallFrame(false);
//		if(AetherVoice.isCalling) {
//			hideCallFrame(false);
//			
//		}
//		if (AetherVoice.isCalling && !callFrame.isShown()){
//			AetherVoice.isCalling = false;
//			AetherVoice.mHandler.sendEmptyMessage(CallHandler.MSG_INCOMING_CALL);
//		}
		
		if (Receiver.call_state != UserAgent.UA_STATE_IDLE){
			
			Receiver.moveTop();
//			System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> MOVING TO TOP");
		}
//		
//		if(dialer == null){
//			System.out.println(">>>>>>>>>>>>>>>>> the dialer is null");
//		}
//
//		
//		
		
//		System.out.println("++++++++++++++++++++++++++++ isCalling "+isCalling);
//		System.out.println("++++++++++++++++++++++++++++ webViewing "+webViewing);
//		System.out.println("++++++++++++++++++++++++++++ fullScreen "+fullScreen);
//		System.out.println("++++++++++++++++++++++++++++ viewOrEdit "+viewOrEdit);
//		System.out.println("++++++++++++++++++++++++++++ isOnBackGround "+isOnBackGround);
//		System.out.println("++++++++++++++++++++++++++++ isScribbling "+isScribbling);
		
		doBindService(); // bind the service
		
		/*if(getIntent()!=null){
			System.out.println("AetherVoice >>>>>>>> the intent is type "+getIntent().getAction());
			System.out.println("AetherVoice >>>>>>>> the intent is type "+getIntent().getType());
			System.out.println("AetherVoice >>>>>>>> the intent is scheme "+getIntent().getScheme());
			System.out.println("AetherVoice >>>>>>>> the intent is data "+getIntent().getDataString());		}else{
		}*/
		
		//Filters the current action and gives the appropriate result.
		String action = getIntent().getAction();
		if(action!=null){
			if(action.equals(Intent.ACTION_INSERT)){ //insert a new contact
				showEditContactInfo(this, "", "", "", 0);
			}else if(action.equals(Intent.ACTION_EDIT)){
				Cursor contact = getContentResolver().query(getIntent().getData(), null, null, null, null);
				startManagingCursor(contact);
				if(contact.moveToFirst()){
					showEditContactInfo(this,
							contact.getString(contact.getColumnIndex(ContactsContract.Contacts._ID)),
							contact.getString(contact.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)),
							"",
							contact.getInt(contact.getColumnIndex(ContactsContract.Contacts.STARRED)));
				}else{
					AetherVoice.tabHost.setCurrentTab(1);
				}
			}else if(action.equals(Intent.ACTION_VIEW)){
				if(getIntent().getType().equals("vnd.android.cursor.dir/calls"))
					AetherVoice.tabHost.setCurrentTab(2);
				else{
					Uri data = getIntent().getData();
					String dataString = data.toString();
					if(dataString.contains("contact")){
						Cursor contact = getContentResolver().query(data, null, null, null, null);
						startManagingCursor(contact);
						if(contact.moveToFirst()){
							showViewContactInfo(this,
									contact.getString(contact.getColumnIndex(ContactsContract.Contacts._ID)),
									contact.getString(contact.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)),
									contact.getInt(contact.getColumnIndex(ContactsContract.Contacts.STARRED)));
						}else{
							AetherVoice.tabHost.setCurrentTab(1);
						}
					}else{
						//other view uris...set tab here I think
					}
				}
			}else if(action.equals(Intent.ACTION_DIAL)){
				Uri data = getIntent().getData();
				String data3 = PhoneNumberUtils.extractNetworkPortion(data.getSchemeSpecificPart());
				data3 = PhoneNumberUtils.formatNumber(data3);
				if(data3.equals("")){
					data3 = data.toString().substring(data.getScheme().length()+1);
				}
				dialer.dialBox.setText(data3);
			}else if(action.equals("android.intent.action.CALL_PRIVILEGED")){
				//call what?
				Uri data = getIntent().getData();
				dialer.dialBox.setText(data.toString().substring(data.getScheme().length()+1));
			}else{
//				AetherVoice.tabHost.setCurrentTab(0); // do we need this?
			}
		}

	}
	
	/** Assigns the new intent to be handled by onResume
	 * @see Activity#onNewIntent(android.content.Intent)
	 */
	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
		super.onNewIntent(intent);
	}

	/** Called when the activity is stopped.
	 * 	Finishes the activity when there is no current calls.
	 * @see Activity#onStop()
	 **/
	@Override
	protected void onStop() {
		if (Dialer.adEngineThread != null) {
			Dialer.adEngineThread.interrupt();
			Dialer.adEngineThread = null;
		}
		
		
		AetherVoice.bannerFlag = false;
		
		if (AetherVoice.isCalling){
			//TODO handle home while there is an ongoing call
		}else{
			finish(); // test
			try{
				onDestroy();
			}catch(UnsupportedOperationException e){
				//this is a work around until a proper work around is found
			}
			
		}
		super.onStop();
	}

	/**
	 * Called when the activity is destroyed.
	 * Unbinds the activity to the service and stops the toggleDialThread
	 * @see Dialer#toggleDialThread(boolean)
	 * @see Activity#onDestroy
	 * @see #doUnbindService()
	 */
	@Override
	public void onDestroy() {
		System.out.println(">>>>>>>>>>>>>>>>>>>>> ONDESTROY");
		
		AetherVoice.threadFlag = false;	
		AetherVoice.bannerFlag = false;
		if (Dialer.adEngineThread != null) {
			Dialer.adEngineThread.stop();
			Dialer.adEngineThread = null;
		}
		
		
		if (AetherVoice.dialer != null)
			AetherVoice.dialer.toggleDialThread(false);

		doUnbindService();
		
		deleteDir(new File("/data/data/com.neugent.aethervoice/banners"));
		
		super.onDestroy();
	}

	/*****************************************************************************/
	/****************************** This is for VIOP *****************************/
	/*****************************************************************************/
	
	public static boolean on(final Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Settings.PREF_ON, Settings.DEFAULT_ON);
	}

	public static void on(final Context context, final boolean on) {
		final Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
		edit.putBoolean(Settings.PREF_ON, on);
		edit.commit();
        if (on) Receiver.engine(context).isRegistered();
	}

	public static String getVersion() {
		return AetherVoice.getVersion(Receiver.mContext);
	}

	public static String getVersion(final Context context) {
		final String unknown = "Unknown";

		if (context == null)
			return unknown;

		try {
			return context.getPackageManager().getPackageInfo(
					context.getPackageName(), 0).versionName;
		} catch (final NameNotFoundException ex) {
		}

		return unknown;
	}
	
	/** The convenience method to register. */
	private void register() {
		Receiver.engine(getApplicationContext()).StartEngine();
		if (Receiver.engine(this).isRegistered()) {
			Receiver.engine(getApplicationContext()).updateDNS();
			updateSleep();
			AetherVoice.setMustRefreshListener(true);
		} else
			Toast.makeText(getApplicationContext(),
					getString(R.string.toast_internet), Toast.LENGTH_SHORT).show();
	}

	/** The convenience method to unregister. */
	private void unregister() {
		AetherVoice.setMustRefreshListener(false);
		Receiver.pos(true);
		Receiver.engine(this).halt();
		Receiver.mSipdroidEngine = null;
		Receiver.reRegister(0);
		stopService(new Intent(this,RegisterService.class));
	}

	/** Updates the wifi sleep policy. */
	private void updateSleep() {
		final ContentResolver cr = getContentResolver();
		final int get = android.provider.Settings.System.getInt(cr,
				android.provider.Settings.System.WIFI_SLEEP_POLICY, -1);
		int set = get;

		if (SettingsWindow.connection == SettingsWindow.CONNECTION_EDGE
				|| SettingsWindow.connection == SettingsWindow.CONNECTION_GPRS) {
			set = android.provider.Settings.System.WIFI_SLEEP_POLICY_DEFAULT;
			if (set != get)
				Toast.makeText(this, R.string.settings_policy_default,
						Toast.LENGTH_LONG).show();
		} else if (SettingsWindow.connection == SettingsWindow.CONNECTION_WLAN) {
			set = android.provider.Settings.System.WIFI_SLEEP_POLICY_NEVER;
			if (set != get)
				Toast.makeText(this, R.string.settings_policy_never,
						Toast.LENGTH_LONG).show();
		}
		if (set != get)
			android.provider.Settings.System.putInt(cr,
					android.provider.Settings.System.WIFI_SLEEP_POLICY, set);
	}
	
	/*****************************************************************************/
	/****************************** This is for PSTN *****************************/
	/*****************************************************************************/

	/** Messenger that communicates with the dialer service. */
	private final Messenger mMessenger = new Messenger(new IncomingHandler(this));

	/** Handler that handles the messages from the dialer service.
	 * <ul>
	 * <li> MSG_INCOMING_CALL	- Message that there is an incoming call
	 * <li> MSG_OUTGOING_CALL	- Message that there is an outgoing call
	 * <li> MSG_ONGOING_CALL	- Message that there is an ongoing call
	 * <li> MSG_HOLD_CALL		- Message that there is an call on hold
	 * <li> MSG_END_CALL		- Message that there is an end call
	 * <li> MSG_MANUAL_ON		- Message that the manual hook switch is offhook
	 * <li> MSG_MANUAL_OFF		- Message that the manual hook switch is onhook
	 * <li> MSG_NO_SERIAL		- Message that the connection to the service has been lost
	 * <li> 10000				- Message that the tablet is undocked
	 * <li> 10001				- Message that the tablet is docked
	 * </ul>
	 * 
	 */
	private class IncomingHandler extends Handler {
		/** The context of the application */
		Context mContext;

		public IncomingHandler(final Context context) {
			mContext = context;
		}

		@Override
		public void handleMessage(final Message msg) {
			switch (msg.what) {
			case MSG_INCOMING_CALL:
//				mPSTNCall.setState(Call.State.INCOMING);
				
				//we need to switch the mode to pstn so tha tthe proper callscreen will show.
				AetherVoice.dialer.switchtoVOIP(false);
				
				AetherVoice.isIncoming = true;
				
				//retrieve the caller from the dialer service and set it to the views.
				AetherVoice.mCallNumber = String.valueOf(msg.arg1 == -1 ? "" : ((Intent) msg.obj).getAction());
				AetherVoice.dialer.dialBox.setText(mCallNumber);
				
				//show the call screen
				AetherVoice.showCallFrame(mContext);
				
				//unregister the voip so that no voip will get through
				if (SettingsWindow.isRegistered)
					unregister();

				//set the the caller
				if (/*pstnCallScreen.mCallDuration < 20
						&&*/ !AetherVoice.pstnCallScreen.isContactFound)
					AetherVoice.pstnCallScreen.updateCaller(
							AetherVoice.dialer.dialBox.getText().toString(),
							PSTNCallScreen.MODE_INCOMING_CALL); 

				AetherVoice.pstnCallScreen.updateStatusUI(PSTNCallScreen.MODE_INCOMING_CALL);
				
				if(playThread == null || playThread.getState() == Thread.State.TERMINATED){
					playThread = getPlayThread();
					playThread.start();
				}
				
				break;
			case MSG_OUTGOING_CALL:
//				mPSTNCall.setState(Call.State.DIALING);
				
				AetherVoice.isOutgoing = true;
				
				if(!isCalling)
					AetherVoice.showCallFrame(mContext);

				if (SettingsWindow.isRegistered)
					unregister();

				//we need to update the callee because we do not have the means to determine if
				//we are already dialing the number
				if (/*pstnCallScreen.mCallDuration < 20
						&&*/ !AetherVoice.pstnCallScreen.isContactFound 
						&& !AetherVoice.isIncoming) {
					AetherVoice.pstnCallScreen.updateCaller(
							AetherVoice.dialer.dialBox.getText().toString(),
							PSTNCallScreen.MODE_ONGOING_CALL);
					AetherVoice.pstnCallScreen.updateStatusUI(PSTNCallScreen.MODE_ONGOING_CALL);
				}
				
				if(playThread == null || playThread.getState() == Thread.State.TERMINATED){
					playThread = getPlayThread();
					playThread.start();
				}

				// pstnCallScreen.updateStatusUI(PSTNCallScreen.MODE_OUTGOING_CALL);

				// dialer.mHandler.sendEmptyMessage(Dialer.CALL_BTN_END);
				break;
			case MSG_ONGOING_CALL:
//				mPSTNCall.setState(Call.State.ACTIVE);
				if(!isOngoing && AetherVoice.callFrame.getVisibility() != View.VISIBLE)
					AetherVoice.showCallFrame(mContext);
				
				if (!AetherVoice.isOngoing) {
					AetherVoice.mstartCallTime = System.currentTimeMillis();
//					System.out.println(">>>>>>>>>>>>>>>>>> ONGOING CALL");
//					mCallDuration = 0;
					
					/*if (mCallThreadTimer == null
							|| mCallThreadTimer.getState() == Thread.State.TERMINATED) {
						mCallThreadTimer = getCallTimerThread();
						mCallThreadTimer.start();
					}*/
					// showCallFrame(mContext);
				}
				
				//we need to update the callee because we do not have the means to determine if
				//we are already dialing the number
				if (AetherVoice.callFrame.getVisibility() == View.VISIBLE) {
					if (/*pstnCallScreen.mCallDuration < 20 
							&&*/ !AetherVoice.pstnCallScreen.isContactFound
							&& !AetherVoice.isIncoming)
						AetherVoice.pstnCallScreen .updateCaller(AetherVoice.dialer.dialBox
										.getText().toString(), PSTNCallScreen.MODE_ONGOING_CALL);

					AetherVoice.pstnCallScreen.updateStatusUI(PSTNCallScreen.MODE_ONGOING_CALL);
				}
				
				AetherVoice.isOngoing = true;
				
				if(playThread == null || playThread.getState() == Thread.State.TERMINATED){
					playThread = getPlayThread();
					playThread.start();
				}
				
				// dialer.mHandler.sendEmptyMessage(Dialer.CALL_BTN_END);
				break;
			case MSG_HOLD_CALL:
//				mPSTNCall.setState(Call.State.HOLDING);
				
				if (!AetherVoice.isOnHold) {
					AetherVoice.isOnHold = true;
					AetherVoice.showCallFrame(mContext);
					AetherVoice.pstnCallScreen
							.updateStatusUI(PSTNCallScreen.MODE_HOLD_CALL);
				} else {
					AetherVoice.isOnHold = false;
					AetherVoice.showCallFrame(mContext);
					AetherVoice.pstnCallScreen
							.updateStatusUI(PSTNCallScreen.MODE_ONGOING_CALL);
				}
				break;
			case MSG_END_CALL: // is the msg_end call same as listening for incoming calls?
							   // who wrote this? btw the ans is when you end a call it will automatically listen for incoming call -aj
//				mPSTNCall.setState(Call.State.DISCONNECTED);
				
				if (!Dialer.isVoip) {

					if (AetherVoice.isIncoming && !isOngoing)
						Connection.addCall(null, getApplicationContext(),
								AetherVoice.mCallNumber, false,
								CallLog.Calls.MISSED_TYPE, System
										.currentTimeMillis(), 0);

					if (AetherVoice.isOngoing || AetherVoice.isIncoming || AetherVoice.isOutgoing || AetherVoice.isOnHold) {
						this.removeMessages(AetherVoice.MSG_ONGOING_CALL);
						if (AetherVoice.callFrame.getVisibility() == View.VISIBLE)
							AetherVoice.pstnCallScreen.updateStatusUI(PSTNCallScreen.MODE_END_CALL);
					} else {
						if (SettingsWindow.isRegistered)
							register();
						AetherVoice.hideCallFrame(false);
					}
					
//					if (mCallThreadTimer != null)
//						mCallThreadTimer.interrupt();
					// dialer.mHandler.sendEmptyMessage(Dialer.CALL_BTN_CALL);
					
					int duration = 0;
					try { duration = pstnCallScreen.mCallDuration; } catch (Exception e) { }
					
					if (AetherVoice.dialer.isOnMHS)
						if (AetherVoice.dialer.dialBox.length() > 1)
							Connection.addCall(null, mContext,
									AetherVoice.dialer.dialBox.getText()
											.toString(), false,
									CallLog.Calls.OUTGOING_TYPE,
									AetherVoice.mstartCallTime,
									duration);
					
					// dialer.updateMHSStatus(false);

					AetherVoice.mstartCallTime = 0;
					AetherVoice.isOngoing = false;
					AetherVoice.isOnHold = false;
					AetherVoice.isOutgoing = false;
					AetherVoice.isIncoming = false;
					
					dialer.dialBox.setText("");
					dialer.dialBox.setSelection(dialer.dialBox.length());

					CallHistoryWindow.setMustUpdateCallHistory();
					
					if(playThread != null){
						playThread.interrupt();
						playThread = null;
					}
					
					
				}
				break;
			case MSG_MANUAL_ON:
				if (SettingsWindow.isRegistered)
					unregister();
				AetherVoice.dialer.updateMHSStatus(true);
				break;
			case MSG_MANUAL_OFF:
				if (!AetherVoice.dialer.callStatus) {
					if (SettingsWindow.isRegistered)
						register();
					/*AetherVoice.dialer.dialBox.setText("");*/ //This should be handled by end call
					
					if (AetherVoice.pstnCallScreen != null) {
						if(isIncoming)
							Connection.addCall(null, getApplicationContext(),
									AetherVoice.mCallNumber, false,
									CallLog.Calls.INCOMING_TYPE,
									AetherVoice.mstartCallTime,
									AetherVoice.pstnCallScreen.mCallDuration);
						else if(isOngoing)
							Connection.addCall(null, getApplicationContext(),
									dialer.dialBox.getText().toString(), false,
									CallLog.Calls.OUTGOING_TYPE,
									AetherVoice.mstartCallTime,
									AetherVoice.pstnCallScreen.mCallDuration);
						CallHistoryWindow.setMustUpdateCallHistory();
					}
				}
				AetherVoice.dialer.updateMHSStatus(false);
				break;
			case MSG_NO_SERIAL:
				AetherVoice.mErrorAlert.showErrorDialog("Closing Application", "Connection to serial is terminated"); //XXX: uncomment unless not tablet
				doUnbindService();
				break;
			case 10000:
				AetherVoice.dialer.undockedReminder(true);
				break;
			case 10001:
				AetherVoice.dialer.undockedReminder(false);
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}
	
	/**
	 * Plays a silent tone. This is a fix for the audio in speaker mode.
	 * *****************************DO NOT REMOVE***********************
	 * @return Thread object
	 */
	private Thread getPlayThread(){
		return new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					while(isCalling){
						dialer.mToneGenerator.startTone(ToneGenerator.TONE_CDMA_SIGNAL_OFF);
						Thread.sleep(1000);
						
						/*if (pstnCallScreen.mCallDuration < 20
								&& !AetherVoice.pstnCallScreen.isContactFound
								&& !(AetherVoice.dialer.dialBox.length() == pstnCallScreen.getCallerLength()))
							AetherVoice.pstnCallScreen.updateCaller(
									AetherVoice.dialer.dialBox.getText().toString(),
									PSTNCallScreen.MODE_ONGOING_CALL);*/
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
//				System.out.println("ending thread");
			}
			
		});	
	}
	
/*	private Thread getCallTimerThread() {
		return new Thread(new Runnable() {
			public void run() {
				try {
					while (AetherVoice.isOngoing  && dialer.callStatus ) {
						if (!AetherVoice.isOnHold) {
							AetherVoice.mCallDuration++;
							if (AetherVoice.isOngoing)
								try {
									mMessenger.send(Message.obtain(null,
											AetherVoice.MSG_ONGOING_CALL));
								} catch (final RemoteException e) {
									e.printStackTrace();
								}
								
//								dialer.mToneGenerator.startTone(ToneGenerator.TONE_CDMA_SIGNAL_OFF);
								
						}
						Thread.sleep(1000);
					}
				} catch (final InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
	}*/

	/** Sends a message to the service **/
	private static Messenger mService = null;

	/**
	 * Sends a message to the service.
	 * @param number the data to be sent.
	 */
	public static void sendServiceMessage(final int number) {
		if (AetherVoice.mService != null)
			try {
				AetherVoice.mService.send(Message.obtain(null, number));
			} catch (final RemoteException e) {
				e.printStackTrace();
			}
		else //if the service is null there is no connection to the service
			AetherVoice.mErrorAlert.showErrorDialog("Closing Application",
					"Connection to the service has been lost.");
	}
	
	/**
	 * Sends a message to the service.
	 * @param msg the data to be sent.
	 */
	public static void sendServiceMessage(final Message msg) {
		if (AetherVoice.mService != null)
			try {
				AetherVoice.mService.send(msg);
			} catch (final RemoteException e) {
				e.printStackTrace();
			}
		else
			AetherVoice.mErrorAlert.showErrorDialog("Closing Application",
					"Connection to the service has been lost.");
	}

	/** The connection to the to the service. */
	private final ServiceConnection mConnection = new ServiceConnection() {

		public void onServiceConnected(final ComponentName name,
				final IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service. We are communicating with our
			// service through an IDL interface, so get a client-side
			// representation of that from the raw service object.
			AetherVoice.mService = new Messenger(service);

			// We want to monitor the service for as long as we are
			// connected to it.
			try { // this is a test message to see if we are really connected
				Message msg = Message.obtain(null,
						AetherVoice.MSG_REGISTER_CLIENT);
				msg.replyTo = mMessenger;
				AetherVoice.mService.send(msg);

				// remove the notification
				msg = Message.obtain(null,
						AetherVoice.DIALER_SERVICE_NOTIFICATION, 0, 0);
				AetherVoice.mService.send(msg);
				
				// we will send the current volume to the client
				Message msg2 = Message.obtain(null, AetherVoice.MSG_VOLUME_CONTROL);
				msg2.arg1 = dialer.volumeBar.getProgress();
				AetherVoice.sendServiceMessage(msg2); 
			} catch (final RemoteException e) {
				// In this case the service has crashed before we could even
				// do anything with it; we can count on soon being
				// disconnected (and then reconnected if it can be restarted)
				// so there is no need to do anything here.
				e.printStackTrace();
			}
		}

		public void onServiceDisconnected(final ComponentName name) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			AetherVoice.mService = null;
		}

	};

	/** Binds the activity to the service. **/
	private void doBindService() {
		// Establish a connection with the service. We use an explicit
		// class name because there is no reason to be able to let other
		// applications replace our component.
		final Intent i = new Intent();
		i.setClassName(AetherVoice.STRING_COMP_PKG_SERVICE,
				AetherVoice.STRING_COMP_CLS_SERVICE);

		// trial
		final ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		final List<RunningServiceInfo> services = manager
				.getRunningServices(Integer.MAX_VALUE);
		final int size = services.size();
		for (int x = 0; x < size; x++)
			if (AetherVoice.STRING_COMP_PKG_SERVICE.equals(services.get(x).service.getPackageName())
					&& AetherVoice.STRING_COMP_CLS_SERVICE.equals(services.get(x).service.getClassName())) {
				Log.i("AetherVoice", "Dialer Service is found. Attempting connection.");
				mIsBound = bindService(i, mConnection, 0);
				break;
			}

		if (!mIsBound){
			Log.i("AetherVoice", "Dialer Service is not found. Creating new service.");
			mIsBound = bindService(i, mConnection, Context.BIND_AUTO_CREATE);
		}
			

		if (!mIsBound) {
			Log.i("AetherVoice", "Connection to Dialer Service resulted in failure.");
			unbindService(mConnection);
			AetherVoice.mErrorAlert.showErrorDialog("Closing Application", "Failed to connect to the service.");
		}
		
		Log.i("AetherVoice", "AetherVoice bound to the service properly.");
	}

	/** Unbinds the activity to the service. **/
	private void doUnbindService() {
		if (mIsBound) {
			// If we have received the service, and hence registered with
			// it, then now is the time to unregister.
			if (AetherVoice.mService != null)
				try {
					final Message msg = Message.obtain(null,
							AetherVoice.MSG_UNREGISTER_CLIENT);
					msg.replyTo = mMessenger;
					AetherVoice.mService.send(msg);
				} catch (final RemoteException e) {
					// There is nothing special we need to do if the service
					// has crashed.
				}

			// Detach our existing connection.
			unbindService(mConnection);
			mIsBound = false;
		}
	}
	
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }
	
}