/**
 * @file SettingsWindow.java
 * @brief It contains the SettingsWindow activity class.
 * @author Wyndale Wong
 */

package com.neugent.aethervoice.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.sipdroid.sipua.SipdroidEngine;
import org.sipdroid.sipua.ui.Receiver;
import org.sipdroid.sipua.ui.RegisterService;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.neugent.aethervoice.R;

/**
 * @class SettingsWindow
 * @brief An activity class that creates an interactive interface on the fourth tab window of AetherVoice 
 * 		for the user to configure application settings.
 * @author Wyndale Wong
 */
public class SettingsWindow implements OnClickListener {
	
	/** Used to access saved ringtones and important methods for managing ringtones. */
	private RingtoneManager ringtoneManager;
    
	/** The actual ringtone to be played. */
	private Ringtone ringtone;
	
	/** Contains the set of saved ringtones. */
    private Cursor ringtoneCursor;
    
    /** Dynamically populates the contents of the single-choice ringtone Alert Dialog. */
    private ListAdapter ringtoneListAdapter;

	/** The flag that indicates the register toggle status. **/
	public static boolean isRegistered = false;

    /** Position index of the last selected ringtone. */
    private int selectRingtonePos = -1;
    
    /** Stores the name of last selected ringtone. */
    private String tempToneName = "";
    
    /** Temporarily holds the database for access before it is closed. */
	private SQLiteDatabase db = null;
	
	/** The defined key name for the settings database name. */
	public static final String DATABASE_NAME = "settings";

	/** The defined key name for the settings database table name. */
	public static final String DATABASE_TABLE = "settingstable";

    public static String[] username = new String[SipdroidEngine.LINES];
    
    public static String[] password = new String[SipdroidEngine.LINES];
    
    public static String[] server = new String[SipdroidEngine.LINES];
    
    public static String[] domain = new String[SipdroidEngine.LINES];
    
    /** The applied port number being used as a parameter by the application. */
    public static String port;
    
    public static String[] from_user = new String[SipdroidEngine.LINES]; 
    
    /** The index of the applied protocol of choice being used as a parameter by the application. */
    public static int protocol;
    
    /** The index of the applied connection of choice being used as a parameter by the application. */
    public static int connection;
    
    /** The flag the shows whether STUN is enabled. **/
    public static boolean useStun = false;
    
    /** The STUN server name. **/
    public static String stunServer = "";
    
    /** The STUN port number. **/
    public static String stunPort = "";
    
    public static boolean mmtel = false;
    public static String mmtel_qvalue = "1.00";
    
    public static boolean wifi_own = false;
    public static boolean wifi_keepon = false;
    public static boolean wifi_select = false;
    
    public static boolean pbxes_par = false;
    public static boolean pbxes_improve = false;
    public static String pbxes_posurl = "";
    public static boolean pbxes_pos = false;
    public static boolean pbxes_callback = false;
    public static boolean pbxes_callthru = false;
    public static String pbxes_callthru2 = "";
    
    public static boolean notif_vmail = false;
    public static boolean notif_reg = true;
    public static boolean notif_mcall = true; //this is missed call
    public static boolean notif_nodata = false;
    
    public static String video_quality = "low"; 
    
    /** The index of the applied call type of choice being used as a parameter by the application. */
    private int calltypeIndex = 0;
    public static String callType;
    
    public static String adv_search = "";
    public static String adv_exclude = "";
    
    /** The flag that shows whether auto-answer is enabled. **/
    public static boolean autoAnswer = false;
    
    /** The flag that shows whether auto-answer on demand is enabled. **/
    public static boolean autoAnswerOD = false;
    
    /** The flag that shows whether auto-answer with headset is enabled. **/
    public static boolean autoAnswerHS = false;
    
    /** The applied ringtone Uri being used as a parameter by the application. */
    public static String ringtoneUri = "";
    
    /** The applied ringtone name which being used as a parameter by the application. */
    public static String ringtoneName = "";
    
	/** The defined key name for the preset column. */
	private static final String KEY_PRESET = "preset";
	
	/** The defined name string for username of account1 column. */
	private static final String KEY_USERNAME0 = "username0";
	
	/** The defined name string for password of account1 column. */
	private static final String KEY_PASSWORD0 = "password0";
	
	/** The defined name string for server of account1 column. */
	private static final String KEY_SERVER0 = "server0";
	
	/** The defined name string for domain of account1 column. */
	private static final String KEY_DOMAIN0 = "domain0";
	
	/** The defined name string for caller id of account1 column. */
	private static final String KEY_FROMUSER0 = "fromuser0"; 
	
	/** The defined name string for username of account2 column. */
	private static final String KEY_USERNAME1 = "username1";
	
	/** The defined name string for password of account2 column. */
	private static final String KEY_PASSWORD1 = "password1";
	
	/** The defined name string for server of account2 column. */
	private static final String KEY_SERVER1 = "server1";
	
	/** The defined name string for domain of account2 column. */
	private static final String KEY_DOMAIN1 = "domain1";
	
	/** The defined name string for caller id of account2 column. */
	private static final String KEY_FROMUSER1 = "fromuser1"; 
	
	/** The defined name string for port column. */
	private static final String KEY_PORT = "port";
	
	/** The defined name string for protocol column. */
	private static final String KEY_PROTOCOL = "protocol";
	
	/** The defined name string for connection column. */
	private static final String KEY_CONNECTION = "connection";
	
	/** The defined name string for use stun column. */
	private static final String KEY_STUN = "stun";
	
	/** The defined name string for stun server column. */
	private static final String KEY_STUNSERVER = "stunserver";
	
	/** The defined name string for stun port column. */
	private static final String KEY_STUNPORT = "stunport";
	
	/** The defined name string for use mmtel column. */
	private static final String KEY_MMTEL = "mmtel";
	
	/** The defined name string for mmtel qvalue column. */
	private static final String KEY_MMTELQVALUE = "mmtelqvalue";
	
	/** The defined name string for wifi control power column. */
	private static final String KEY_WIFI_OWN = "wifiown";
	
	/** The defined name string for wifi screen on column. */
	private static final String KEY_WIFI_KEEP_ON = "wifikeepon";
	
	/** The defined name string for wifi select ap column. */
	private static final String KEY_WIFI_SELECT = "wifiselect";
	
	/** The defined name string for pbxes simultaneous outbound column. */
	private static final String KEY_PBXES_PAR = "pbxespar";
	
	/** The defined name string for pbxes improve audio column. */
	private static final String KEY_PBXES_IMPROVE = "pbxesimprove";
	
	/** The defined name string for pbxes update location column. */
	private static final String KEY_PBXES_POS = "pbxes_pos";
	
	/** The defined name string for pbxes url callback column. */
	private static final String KEY_PBXES_POSURL = "pbxesposurl";
	
	/** The defined name string for pbxes trigger callback column. */
	private static final String KEY_PBXES_CALLBACK = "pbxescallback";
	
	/** The defined name string for pbxes trigger callthru column. */
	private static final String KEY_PBXES_CALLTHRU = "pbxescallthru";
	
	/** The defined name string for pbxes prefix callthru column. */
	private static final String KEY_PBXES_CALLTHRU2 = "pbxescallthru2";
	
	/** The defined name string for notif voicemail column. */
	private static final String KEY_NOTIF_VMAIL = "notifvmail";
	
	/** The defined name string for notif missed call column. */
	private static final String KEY_NOTIF_MCALL = "notifmcall";
	
	/** The defined name string for notif no data column. */
	private static final String KEY_NOTIF_NODATA = "notifnodata";
	
	/** The defined name string for call type column. */
	private static final String KEY_CALL_TYPE = "calltype";
	
	/** The defined name string for video quality column. */
	private static final String KEY_VIDEO_QUALITY = "videoquality";
	
	/** The defined name string for exclide column. */
	private static final String KEY_EXCLUDE = "exclude";
	
	/** The defined name string for search column. */
	private static final String KEY_SEARCH = "search";
	
	/** The defined name string for auto-answer in use column. */
	private static final String KEY_AUTOANSWER = "autoanswer";
	
	/** The defined name string for auto-answer on demand column. */
	private static final String KEY_AUTOANSWEROD = "autoanswerod";
	
	/** The defined name string for auto-answer on headset column. */
	private static final String KEY_AUTOANSWERHS = "autoanswerhs";
	
	/** The defined name string for rintone uri column. */
	private static final String KEY_RINGTONEURI = "ringtoneuri";
	
	/** The defined name string for ringtone name column. */
	private static final String KEY_RINGTONENAME = "ringtonename";
	
	/** The defined name string for UDP protocol. */
	public static final String PROTOCOL_UDP_NAME = "udp";
	
	/** The defined name string for TCP protocol. */
	public static final String PROTOCOL_TCP_NAME = "tcp";
		
	/** The defined index reference value for UDP. */
	public static final int PROTOCOL_UDP = 0;
	
	/** The defined index reference value for UDP. */
	public static final int PROTOCOL_TCP = 1;
	
	/** The defined index reference value for UDP. */
	public static final int CONNECTION_WLAN = 0;
	
	/** The defined index reference value for UDP. */
	public static final int CONNECTION_GPRS = 1;
	
	/** The defined index reference value for UDP. */
	public static final int CONNECTION_EDGE = 2;
	
	/** The defined index reference value for VPN. */
	public static final int CONNECTION_VPN = 3; //vpn?
	
	/** The defined index reference value for SIP call type. */
	public static final int CALLTYPE_SIP = 0;
	
	/** The defined index reference value for PSTN call type. */
	public static final int CALLTYPE_PSTN = 1;
	
    private int active_account = -1;
    
    private LinearLayout frameAccountInfo;
    
    private ScrollView frameNotification;
    private ScrollView frameAdvanced;
    private ScrollView frameAccount;
    private ScrollView framePbxes;
    
    private LinearLayout subframeCallExceptions;
    private LinearLayout subframeVideoOptions;
    private LinearLayout subframeStunOptions;
    private LinearLayout subframeMmtel;
    private LinearLayout subframeWireless;
//    private LinearLayout subframePbx;
    private LinearLayout subframeSubCallthru;
	private LinearLayout subframeSubStun;
	private LinearLayout subframeSubMmtel;
	private LinearLayout subframeLocationCallBack;
    
    private TextView account1;
    private TextView account2;
    
    private EditText fieldUsername;
    private EditText fieldPassword;
    private EditText fieldServer;
    private EditText fieldDomain;
    private EditText fieldCallerID;
    
    private Button btnRingtone;
    
    private String[] callTypeArray;
    
    private boolean isFirstLoad = true;
    
    /** 
     * Initializes the SettingsWindow and its contents.
     * Called when the SettingsWindow tab is pressed for the first time.
     * @see #initView()
     * @see #initDB()
     * @see #initTone()
     */
	
    private final Context context;
    
    LinearLayout accountTitle;
	LinearLayout notifications;
	LinearLayout advanceSettings;
	LinearLayout pbxesOptions;
	
    
	ArrayAdapter<CharSequence> protocolAdapter;
    ArrayAdapter<CharSequence> connectionAdapter;
	ArrayAdapter<CharSequence> callTypeAdapter;
	ArrayAdapter<CharSequence> videoAdapter; 
	Button saveButton;
	Button revertButton;
	Button registerButton;	
    
    public SettingsWindow(final Context context){
        this.context = context;
//        initDB();
//        initView();
//        initTone(); 
        
	}	
    public View getSettingsView()	{
    	final View settingsView = LayoutInflater.from(context).inflate(
				R.layout.settings2, null);
    	callTypeArray = context.getResources().getStringArray(R.array.calltypevalues);
    	accountTitle = (LinearLayout) settingsView.findViewById(R.id.account_title);
    	notifications = (LinearLayout) settingsView.findViewById(R.id.notifications_title);
    	advanceSettings = (LinearLayout) settingsView.findViewById(R.id.advance_settings_title);
    	pbxesOptions = (LinearLayout) settingsView.findViewById(R.id.pbxes_option_title);
    	
		EditText fieldPort = (EditText)settingsView.findViewById(R.id.port);
		Spinner fieldProtocol = (Spinner)settingsView.findViewById(R.id.protocol);
		Spinner fieldConnection = (Spinner)settingsView.findViewById(R.id.connection);
		Spinner fieldCallType = (Spinner)settingsView.findViewById(R.id.calltype);
		Spinner videoOptions = (Spinner)settingsView.findViewById(R.id.adv_video_option);
		
		CheckBox auto_in_use = (CheckBox)settingsView.findViewById(R.id.answer_in_use);
		CheckBox auto_demand = (CheckBox)settingsView.findViewById(R.id.answer_on_demand);
		CheckBox auto_headset = (CheckBox)settingsView.findViewById(R.id.answer_headset);
		
		CheckBox missed_call = (CheckBox)settingsView.findViewById(R.id.missed_call);
		CheckBox voice_mail = (CheckBox)settingsView.findViewById(R.id.voice_mail);
		CheckBox no_data = (CheckBox)settingsView.findViewById(R.id.no_data);
		
		EditText advCallSearch = (EditText) settingsView.findViewById(R.id.adv_search_replace_option);
		EditText advCallExclude = (EditText) settingsView.findViewById(R.id.adv_exclude_option);
		
		CheckBox advStunUse = (CheckBox)settingsView.findViewById(R.id.adv_use_stun);
		EditText advStunServer = (EditText)settingsView.findViewById(R.id.adv_stun_server);
		EditText advStunPort = (EditText) settingsView.findViewById(R.id.adv_stun_port);
		
		CheckBox advMmtelUse = (CheckBox)settingsView.findViewById(R.id.adv_use_mmtel);
		EditText advMmtelQvalue = (EditText) settingsView.findViewById(R.id.adv_mmtel_q_value);
		
		CheckBox wireless_control_wifi = (CheckBox)settingsView.findViewById(R.id.adv_wifi_power);
		CheckBox wireless_select_wifi_ap = (CheckBox)settingsView.findViewById(R.id.adv_wifi_ap);
		CheckBox wireless_screen_on = (CheckBox)settingsView.findViewById(R.id.adv_wireless_screen_on);
		
		CheckBox pbx_simultaneous_outbound = (CheckBox)settingsView.findViewById(R.id.adv_pbx_simul_outbound);
		CheckBox pbx_improve_audio = (CheckBox)settingsView.findViewById(R.id.adv_pbx_improve_audio);
		EditText pbx_url_for_location_callback = (EditText)settingsView.findViewById(R.id.adv_pbx_url_loc_callback);
		CheckBox pbx_update_location = (CheckBox)settingsView.findViewById(R.id.adv_pbx_upd_loc);
		CheckBox pbx_trigger_callback = (CheckBox)settingsView.findViewById(R.id.adv_pbx_trigger_callback);
		CheckBox pbx_trigger_callthru = (CheckBox)settingsView.findViewById(R.id.adv_pbx_trigger_callthru);
		EditText pbx_pref_callthru2 = (EditText)settingsView.findViewById(R.id.adv_pbx_pref_callthru);;
		
		if(isFirstLoad){
			frameAccountInfo = (LinearLayout) settingsView.findViewById(R.id.account_info_group);
			frameAccount = (ScrollView) settingsView.findViewById(R.id.accounts_group);
			
			frameNotification = (ScrollView) settingsView.findViewById(R.id.notifications_group);
			frameAdvanced = (ScrollView) settingsView.findViewById(R.id.advance_settings_group);
			framePbxes = (ScrollView) settingsView.findViewById(R.id.pbxes_option_group);			
			
			subframeCallExceptions = (LinearLayout)settingsView.findViewById(R.id.adv_call_exceptions_frame);
			subframeVideoOptions = (LinearLayout)settingsView.findViewById(R.id.adv_video_option_frame);
			subframeStunOptions = (LinearLayout)settingsView.findViewById(R.id.adv_stun_option_frame);
			subframeMmtel = (LinearLayout)settingsView.findViewById(R.id.adv_mmtel_option_frame);
			subframeWireless = (LinearLayout)settingsView.findViewById(R.id.adv_wireless_option_frame);
//			subframePbx = (LinearLayout)settingsView.findViewById(R.id.adv_pbxes_option_frame);
			subframeSubCallthru = (LinearLayout)settingsView.findViewById(R.id.adv_pbx_callthru_field_group);
			subframeSubStun = (LinearLayout)settingsView.findViewById(R.id.adv_stun_field_group);
			subframeSubMmtel = (LinearLayout)settingsView.findViewById(R.id.adv_mmtel_field_group);
			subframeLocationCallBack = (LinearLayout)settingsView.findViewById(R.id.adv_pbx_url_location_callback_subgroup);
			
			account1 = (TextView) settingsView.findViewById(R.id.account1);
			account2 = (TextView) settingsView.findViewById(R.id.account2);
			
			fieldUsername = (EditText)settingsView.findViewById(R.id.username);
			fieldPassword = (EditText)settingsView.findViewById(R.id.password);
			fieldServer = (EditText)settingsView.findViewById(R.id.server);
			fieldDomain = (EditText)settingsView.findViewById(R.id.domain);
			fieldCallerID = (EditText)settingsView.findViewById(R.id.callerid);
			
			btnRingtone = (Button)settingsView.findViewById(R.id.ringtone);
			
			resetFramesAndIcons();
			frameAccountInfo.setVisibility(View.GONE);
    		frameAccount.setVisibility(View.VISIBLE);
			
    		accountTitle.		setOnClickListener(this);
    		notifications.		setOnClickListener(this);
			advanceSettings.	setOnClickListener(this);
			pbxesOptions.		setOnClickListener(this);
			
			((TextView) settingsView.findViewById(R.id.account_submenu)).			setOnClickListener(this);
			((TextView) settingsView.findViewById(R.id.call_options_title)).			setOnClickListener(this);
			((TextView)settingsView.findViewById(R.id.adv_call_exceptions)).			setOnClickListener(this);
			((TextView)settingsView.findViewById(R.id.adv_mmtel_option)).			setOnClickListener(this);
//			((TextView)settingsView.findViewById(R.id.adv_pbxes_option)).			setOnClickListener(this);
			((TextView)settingsView.findViewById(R.id.adv_stun_option)).				setOnClickListener(this);
			((TextView)settingsView.findViewById(R.id.adv_video_quality)).			setOnClickListener(this);
			((TextView)settingsView.findViewById(R.id.adv_wireless_option)).			setOnClickListener(this);
			
			saveButton = ((Button) settingsView.findViewById(R.id.btn_save));
			revertButton = ((Button) settingsView.findViewById(R.id.btn_revert));
			registerButton = ((Button) settingsView.findViewById(R.id.btn_register));
			
			saveButton.			setOnClickListener(this);
			revertButton.		setOnClickListener(this);
			registerButton.		setOnClickListener(this);
			
			
			account1.			setOnClickListener(this);
			account2.			setOnClickListener(this);
			btnRingtone.		setOnClickListener(this);
			
			fieldUsername.					addTextChangedListener(getTextWatcher(fieldUsername, KEY_USERNAME0));
			fieldPassword.					addTextChangedListener(getTextWatcher(fieldPassword, KEY_PASSWORD0));
			fieldDomain.					addTextChangedListener(getTextWatcher(fieldDomain, KEY_DOMAIN0));
			fieldServer.					addTextChangedListener(getTextWatcher(fieldServer, KEY_SERVER0));
			fieldCallerID.					addTextChangedListener(getTextWatcher(fieldCallerID, KEY_FROMUSER0));
			fieldPort.						addTextChangedListener(getTextWatcher(fieldPort, KEY_PORT));
			pbx_url_for_location_callback.	addTextChangedListener(getTextWatcher(pbx_url_for_location_callback, KEY_PBXES_POSURL));
			pbx_pref_callthru2.				addTextChangedListener(getTextWatcher(pbx_pref_callthru2, KEY_PBXES_CALLTHRU2));
			advCallSearch.					addTextChangedListener(getTextWatcher(advCallSearch, KEY_SEARCH));
			advCallExclude.					addTextChangedListener(getTextWatcher(advCallExclude, KEY_EXCLUDE));
			advStunServer.					addTextChangedListener(getTextWatcher(advStunServer, KEY_STUNSERVER));
			advStunPort.					addTextChangedListener(getTextWatcher(advStunPort, KEY_STUNPORT));
			advMmtelQvalue.					addTextChangedListener(getTextWatcher(advMmtelQvalue, KEY_MMTELQVALUE));
			
			protocolAdapter = 	ArrayAdapter.createFromResource(context, R.array.protocol, android.R.layout.simple_spinner_item);
	        connectionAdapter = 	ArrayAdapter.createFromResource(context, R.array.connection, android.R.layout.simple_spinner_item);
			callTypeAdapter = 	ArrayAdapter.createFromResource(context, R.array.calltype, android.R.layout.simple_spinner_item);
			videoAdapter = 		ArrayAdapter.createFromResource(context, R.array.vquality_display_values, R.layout.spinner_item);
				
			protocolAdapter.	setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			connectionAdapter.	setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			callTypeAdapter.	setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			videoAdapter.		setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			
			fieldProtocol.	setAdapter(protocolAdapter);
			fieldConnection.setAdapter(connectionAdapter);
			fieldCallType.	setAdapter(callTypeAdapter);
			videoOptions.	setAdapter(videoAdapter);
			
			fieldProtocol.	setOnItemSelectedListener(getOnItemSelectedListener(KEY_PROTOCOL));
			fieldConnection.setOnItemSelectedListener(getOnItemSelectedListener(KEY_CONNECTION));
			fieldCallType.	setOnItemSelectedListener(getOnItemSelectedListener(KEY_CALL_TYPE));
			videoOptions.	setOnItemSelectedListener(getOnItemSelectedListener(KEY_VIDEO_QUALITY));
			
			auto_in_use.				setOnCheckedChangeListener(getOnCheckedChangeListener(auto_in_use, KEY_AUTOANSWER));
			auto_demand.				setOnCheckedChangeListener(getOnCheckedChangeListener(auto_demand, KEY_AUTOANSWEROD));
			auto_headset.				setOnCheckedChangeListener(getOnCheckedChangeListener(auto_headset, KEY_AUTOANSWERHS));
			missed_call.				setOnCheckedChangeListener(getOnCheckedChangeListener(missed_call, KEY_NOTIF_MCALL));
			voice_mail.					setOnCheckedChangeListener(getOnCheckedChangeListener(voice_mail, KEY_NOTIF_VMAIL));
			no_data.					setOnCheckedChangeListener(getOnCheckedChangeListener(no_data, KEY_NOTIF_NODATA));
			
			advStunUse.			setOnCheckedChangeListener(getOnCheckedChangeListener(advStunUse, KEY_STUN));
			advMmtelUse.			setOnCheckedChangeListener(getOnCheckedChangeListener(advMmtelUse, KEY_MMTEL));
			
			wireless_control_wifi.		setOnCheckedChangeListener(getOnCheckedChangeListener(wireless_control_wifi, KEY_WIFI_OWN));
			wireless_select_wifi_ap.	setOnCheckedChangeListener(getOnCheckedChangeListener(wireless_select_wifi_ap, KEY_WIFI_SELECT));
			wireless_screen_on.			setOnCheckedChangeListener(getOnCheckedChangeListener(wireless_screen_on, KEY_WIFI_KEEP_ON));

			pbx_simultaneous_outbound.	setOnCheckedChangeListener(getOnCheckedChangeListener(pbx_simultaneous_outbound, KEY_PBXES_PAR));
			pbx_improve_audio.			setOnCheckedChangeListener(getOnCheckedChangeListener(pbx_improve_audio, KEY_PBXES_IMPROVE));
			pbx_update_location.		setOnCheckedChangeListener(getOnCheckedChangeListener(pbx_update_location, KEY_PBXES_POS));
			pbx_trigger_callback.		setOnCheckedChangeListener(getOnCheckedChangeListener(pbx_trigger_callback, KEY_PBXES_CALLBACK));
			pbx_trigger_callthru.		setOnCheckedChangeListener(getOnCheckedChangeListener(pbx_trigger_callthru, KEY_PBXES_CALLTHRU));
			
		}
		
		auto_in_use.					setChecked(autoAnswer);
		auto_demand.					setChecked(autoAnswerOD);
		auto_headset.					setChecked(autoAnswerHS);
		missed_call.					setChecked(notif_mcall);
		voice_mail.						setChecked(notif_vmail);
		no_data.						setChecked(notif_nodata);
		
		advStunUse.						setChecked(useStun);
		advMmtelUse.					setChecked(mmtel);
		
		wireless_control_wifi.			setChecked(wifi_own);
		wireless_select_wifi_ap.		setChecked(wifi_select);
		wireless_screen_on.				setChecked(wifi_keepon);
		
		pbx_simultaneous_outbound.		setChecked(pbxes_par);
		pbx_improve_audio.				setChecked(pbxes_improve);
		pbx_trigger_callthru.			setChecked(pbxes_callthru);
		pbx_url_for_location_callback.	setText(pbxes_posurl);
		
		fieldPort.		setText(port);
		
		
		initDB();
		
		
		if(username[0].length() > 0 && server[0].length() > 0) account1.		setText(username[0]+"@"+server[0]);
		if(username[1].length() > 0 && server[1].length() > 0) account2.		setText(username[1]+"@"+server[1]);
		
		btnRingtone.	setText(ringtoneName);
		
		advCallSearch.	setText(adv_search);
		advCallExclude.	setText(adv_exclude);
		
		fieldProtocol.	setSelection(protocol);
		fieldConnection.setSelection(connection);
		fieldCallType.	setSelection(calltypeIndex);
		videoOptions.	setSelection(video_quality.equals("high")?0:1);
		
		if(!isFirstLoad){
			fieldProtocol.	invalidate();
			fieldConnection.invalidate();
			fieldCallType.	invalidate();
			videoOptions.	invalidate();
		}
		
		callType = callTypeArray[calltypeIndex];
		
		if(useStun) {
			subframeSubStun.setVisibility(View.VISIBLE);
			
			advStunServer.	setText(stunServer);
			advStunPort.	setText(stunPort);
		} else subframeSubStun.setVisibility(View.GONE);
		
		if(mmtel){
			subframeSubMmtel.setVisibility(View.VISIBLE);
			
			advMmtelQvalue.	setText(mmtel_qvalue);
		} else subframeSubMmtel.setVisibility(View.GONE);
		
		if(pbxes_callthru){
			subframeSubCallthru.setVisibility(View.VISIBLE);
			
			pbx_pref_callthru2.setText(pbxes_callthru2);
		} else subframeSubCallthru.setVisibility(View.GONE);
		
		if(pbxes_posurl.length()>10){
			subframeLocationCallBack.setVisibility(View.VISIBLE);
			
			pbx_update_location.	setChecked(pbxes_pos);
			pbx_trigger_callback.	setChecked(pbxes_callback);
		} else subframeLocationCallBack.setVisibility(View.GONE);
		
		isFirstLoad = false;
		
		initTone();
		return settingsView;
	}

	/**
	 * Initializes TextViews, EditTexts, Spinners, Buttons, and their listeners.
	 * 
	 * @see #refreshCallHistory()
	 */
//	private void initView()	{
//		
//		EditText fieldPort = (EditText)findViewById(R.id.port);
//		Spinner fieldProtocol = (Spinner)findViewById(R.id.protocol);
//		Spinner fieldConnection = (Spinner)findViewById(R.id.connection);
//		Spinner fieldCallType = (Spinner)findViewById(R.id.calltype);
//		Spinner videoOptions = (Spinner)findViewById(R.id.adv_video_option);
//		
//		CheckBox auto_in_use = (CheckBox)findViewById(R.id.answer_in_use);
//		CheckBox auto_demand = (CheckBox)findViewById(R.id.answer_on_demand);
//		CheckBox auto_headset = (CheckBox)findViewById(R.id.answer_headset);
//		
//		CheckBox missed_call = (CheckBox)findViewById(R.id.missed_call);
//		CheckBox voice_mail = (CheckBox)findViewById(R.id.voice_mail);
//		CheckBox no_data = (CheckBox)findViewById(R.id.no_data);
//		
//		EditText advCallSearch = (EditText) findViewById(R.id.adv_search_replace_option);
//		EditText advCallExclude = (EditText) findViewById(R.id.adv_exclude_option);
//		
//		CheckBox advStunUse = (CheckBox)findViewById(R.id.adv_use_stun);
//		EditText advStunServer = (EditText) findViewById(R.id.adv_stun_server);
//		EditText advStunPort = (EditText) findViewById(R.id.adv_stun_port);
//		
//		CheckBox advMmtelUse = (CheckBox)findViewById(R.id.adv_use_mmtel);
//		EditText advMmtelQvalue = (EditText) findViewById(R.id.adv_mmtel_q_value);
//		
//		CheckBox wireless_control_wifi = (CheckBox)findViewById(R.id.adv_wifi_power);
//		CheckBox wireless_select_wifi_ap = (CheckBox)findViewById(R.id.adv_wifi_ap);
//		CheckBox wireless_screen_on = (CheckBox)findViewById(R.id.adv_wireless_screen_on);
//		
//		CheckBox pbx_simultaneous_outbound = (CheckBox)findViewById(R.id.adv_pbx_simul_outbound);
//		CheckBox pbx_improve_audio = (CheckBox)findViewById(R.id.adv_pbx_improve_audio);
//		EditText pbx_url_for_location_callback = (EditText)findViewById(R.id.adv_pbx_url_loc_callback);
//		CheckBox pbx_update_location = (CheckBox)findViewById(R.id.adv_pbx_upd_loc);
//		CheckBox pbx_trigger_callback = (CheckBox)findViewById(R.id.adv_pbx_trigger_callback);
//		CheckBox pbx_trigger_callthru = (CheckBox)findViewById(R.id.adv_pbx_trigger_callthru);
//		EditText pbx_pref_callthru2 = (EditText)findViewById(R.id.adv_pbx_pref_callthru);;
//		
//		if(isFirstLoad){
//			frameAccountInfo = (LinearLayout) findViewById(R.id.account_info_group);
//			frameAccount = (ScrollView) findViewById(R.id.accounts_group);
//			
//			frameNotification = (ScrollView) findViewById(R.id.notifications_group);
//			frameAdvanced = (ScrollView) findViewById(R.id.advance_settings_group);
//			
//			subframeCallExceptions = (LinearLayout)findViewById(R.id.adv_call_exceptions_frame);
//			subframeVideoOptions = (LinearLayout)findViewById(R.id.adv_video_option_frame);
//			subframeStunOptions = (LinearLayout)findViewById(R.id.adv_stun_option_frame);
//			subframeMmtel = (LinearLayout)findViewById(R.id.adv_mmtel_option_frame);
//			subframeWireless = (LinearLayout)findViewById(R.id.adv_wireless_option_frame);
//			subframePbx = (LinearLayout)findViewById(R.id.adv_pbxes_option_frame);
//			subframeSubCallthru = (LinearLayout)findViewById(R.id.adv_pbx_callthru_field_group);
//			subframeSubStun = (LinearLayout)findViewById(R.id.adv_stun_field_group);
//			subframeSubMmtel = (LinearLayout)findViewById(R.id.adv_mmtel_field_group);
//			subframeLocationCallBack = (LinearLayout)findViewById(R.id.adv_pbx_url_location_callback_subgroup);
//			
//			account1 = (TextView) findViewById(R.id.account1);
//			account2 = (TextView) findViewById(R.id.account2);
//			
//			fieldUsername = (EditText)findViewById(R.id.username);
//			fieldPassword = (EditText)findViewById(R.id.password);
//			fieldServer = (EditText)findViewById(R.id.server);
//			fieldDomain = (EditText)findViewById(R.id.domain);
//			fieldCallerID = (EditText)findViewById(R.id.callerid);
//			
//			btnRingtone = (Button)findViewById(R.id.ringtone);
//			
//			resetFramesAndIcons();
//			frameAccountInfo.setVisibility(View.GONE);
//    		frameAccount.setVisibility(View.VISIBLE);
//			
//			((LinearLayout) findViewById(R.id.account_title)).			setOnClickListener(this);
//			((LinearLayout) findViewById(R.id.notifications_title)).	setOnClickListener(this);
//			((LinearLayout) findViewById(R.id.advance_settings_title)).	setOnClickListener(this);
//			
//			((TextView) findViewById(R.id.account_submenu)).			setOnClickListener(this);
//			((TextView) findViewById(R.id.call_options_title)).			setOnClickListener(this);
//			((TextView)findViewById(R.id.adv_call_exceptions)).			setOnClickListener(this);
//			((TextView)findViewById(R.id.adv_mmtel_option)).			setOnClickListener(this);
//			((TextView)findViewById(R.id.adv_pbxes_option)).			setOnClickListener(this);
//			((TextView)findViewById(R.id.adv_stun_option)).				setOnClickListener(this);
//			((TextView)findViewById(R.id.adv_video_quality)).			setOnClickListener(this);
//			((TextView)findViewById(R.id.adv_wireless_option)).			setOnClickListener(this);
//			
//			((Button) findViewById(R.id.btn_save)).						setOnClickListener(this);
//			((Button) findViewById(R.id.btn_revert)).					setOnClickListener(this);
//			((Button) findViewById(R.id.btn_register)).					setOnClickListener(this);
//			
//			account1.			setOnClickListener(this);
//			account2.			setOnClickListener(this);
//			btnRingtone.		setOnClickListener(this);
//			
//			fieldUsername.					addTextChangedListener(getTextWatcher(fieldUsername, KEY_USERNAME0));
//			fieldPassword.					addTextChangedListener(getTextWatcher(fieldPassword, KEY_PASSWORD0));
//			fieldDomain.					addTextChangedListener(getTextWatcher(fieldDomain, KEY_DOMAIN0));
//			fieldServer.					addTextChangedListener(getTextWatcher(fieldServer, KEY_SERVER0));
//			fieldCallerID.					addTextChangedListener(getTextWatcher(fieldCallerID, KEY_FROMUSER0));
//			fieldPort.						addTextChangedListener(getTextWatcher(fieldPort, KEY_PORT));
//			pbx_url_for_location_callback.	addTextChangedListener(getTextWatcher(pbx_url_for_location_callback, KEY_PBXES_POSURL));
//			pbx_pref_callthru2.				addTextChangedListener(getTextWatcher(pbx_pref_callthru2, KEY_PBXES_CALLTHRU2));
//			advCallSearch.					addTextChangedListener(getTextWatcher(advCallSearch, KEY_SEARCH));
//			advCallExclude.					addTextChangedListener(getTextWatcher(advCallExclude, KEY_EXCLUDE));
//			advStunServer.					addTextChangedListener(getTextWatcher(advStunServer, KEY_STUNSERVER));
//			advStunPort.					addTextChangedListener(getTextWatcher(advStunPort, KEY_STUNPORT));
//			advMmtelQvalue.					addTextChangedListener(getTextWatcher(advMmtelQvalue, KEY_MMTELQVALUE));
//			
//			ArrayAdapter<CharSequence> protocolAdapter = 	ArrayAdapter.createFromResource(this, R.array.protocol, android.R.layout.simple_spinner_item);
//	        ArrayAdapter<CharSequence> connectionAdapter = 	ArrayAdapter.createFromResource(this, R.array.connection, android.R.layout.simple_spinner_item);
//			ArrayAdapter<CharSequence> callTypeAdapter = 	ArrayAdapter.createFromResource(this, R.array.calltype, android.R.layout.simple_spinner_item);
//			ArrayAdapter<CharSequence> videoAdapter = 		ArrayAdapter.createFromResource(this, R.array.vquality_display_values, R.layout.spinner_item);
//				
//			protocolAdapter.	setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//			connectionAdapter.	setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//			callTypeAdapter.	setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//			videoAdapter.		setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//			
//			fieldProtocol.	setAdapter(protocolAdapter);
//			fieldConnection.setAdapter(connectionAdapter);
//			fieldCallType.	setAdapter(callTypeAdapter);
//			videoOptions.	setAdapter(videoAdapter);
//			
//			fieldProtocol.	setOnItemSelectedListener(getOnItemSelectedListener(KEY_PROTOCOL));
//			fieldConnection.setOnItemSelectedListener(getOnItemSelectedListener(KEY_CONNECTION));
//			fieldCallType.	setOnItemSelectedListener(getOnItemSelectedListener(KEY_CALL_TYPE));
//			videoOptions.	setOnItemSelectedListener(getOnItemSelectedListener(KEY_VIDEO_QUALITY));
//			
//			auto_in_use.				setOnCheckedChangeListener(getOnCheckedChangeListener(auto_in_use, KEY_AUTOANSWER));
//			auto_demand.				setOnCheckedChangeListener(getOnCheckedChangeListener(auto_demand, KEY_AUTOANSWEROD));
//			auto_headset.				setOnCheckedChangeListener(getOnCheckedChangeListener(auto_headset, KEY_AUTOANSWERHS));
//			missed_call.				setOnCheckedChangeListener(getOnCheckedChangeListener(missed_call, KEY_NOTIF_MCALL));
//			voice_mail.					setOnCheckedChangeListener(getOnCheckedChangeListener(voice_mail, KEY_NOTIF_VMAIL));
//			no_data.					setOnCheckedChangeListener(getOnCheckedChangeListener(no_data, KEY_NOTIF_NODATA));
//			
//			advStunUse.			setOnCheckedChangeListener(getOnCheckedChangeListener(advStunUse, KEY_STUN));
//			advMmtelUse.			setOnCheckedChangeListener(getOnCheckedChangeListener(advMmtelUse, KEY_MMTEL));
//			
//			wireless_control_wifi.		setOnCheckedChangeListener(getOnCheckedChangeListener(wireless_control_wifi, KEY_WIFI_OWN));
//			wireless_select_wifi_ap.	setOnCheckedChangeListener(getOnCheckedChangeListener(wireless_select_wifi_ap, KEY_WIFI_SELECT));
//			wireless_screen_on.			setOnCheckedChangeListener(getOnCheckedChangeListener(wireless_screen_on, KEY_WIFI_KEEP_ON));
//
//			pbx_simultaneous_outbound.	setOnCheckedChangeListener(getOnCheckedChangeListener(pbx_simultaneous_outbound, KEY_PBXES_PAR));
//			pbx_improve_audio.			setOnCheckedChangeListener(getOnCheckedChangeListener(pbx_improve_audio, KEY_PBXES_IMPROVE));
//			pbx_update_location.		setOnCheckedChangeListener(getOnCheckedChangeListener(pbx_update_location, KEY_PBXES_POS));
//			pbx_trigger_callback.		setOnCheckedChangeListener(getOnCheckedChangeListener(pbx_trigger_callback, KEY_PBXES_CALLBACK));
//			pbx_trigger_callthru.		setOnCheckedChangeListener(getOnCheckedChangeListener(pbx_trigger_callthru, KEY_PBXES_CALLTHRU));
//			
//		}
//		
//		auto_in_use.					setChecked(autoAnswer);
//		auto_demand.					setChecked(autoAnswerOD);
//		auto_headset.					setChecked(autoAnswerHS);
//		missed_call.					setChecked(notif_mcall);
//		voice_mail.						setChecked(notif_vmail);
//		no_data.						setChecked(notif_nodata);
//		
//		advStunUse.						setChecked(useStun);
//		advMmtelUse.					setChecked(mmtel);
//		
//		wireless_control_wifi.			setChecked(wifi_own);
//		wireless_select_wifi_ap.		setChecked(wifi_select);
//		wireless_screen_on.				setChecked(wifi_keepon);
//		
//		pbx_simultaneous_outbound.		setChecked(pbxes_par);
//		pbx_improve_audio.				setChecked(pbxes_improve);
//		pbx_trigger_callthru.			setChecked(pbxes_callthru);
//		pbx_url_for_location_callback.	setText(pbxes_posurl);
//		
//		fieldPort.		setText(port);
//		
//		if(username[0].length() > 0 && server[0].length() > 0) account1.		setText(username[0]+"@"+server[0]);
//		if(username[1].length() > 0 && server[1].length() > 0) account2.		setText(username[1]+"@"+server[1]);
//		
//		btnRingtone.	setText(ringtoneName);
//		
//		advCallSearch.	setText(adv_search);
//		advCallExclude.	setText(adv_exclude);
//		
//		fieldProtocol.	setSelection(protocol);
//		fieldConnection.setSelection(connection);
//		fieldCallType.	setSelection(calltypeIndex);
//		videoOptions.	setSelection(video_quality.equals("high")?0:1);
//		
//		if(!isFirstLoad){
//			fieldProtocol.	invalidate();
//			fieldConnection.invalidate();
//			fieldCallType.	invalidate();
//			videoOptions.	invalidate();
//		}
//		
//		callType = callTypeArray[calltypeIndex];
//		
//		if(useStun) {
//			subframeSubStun.setVisibility(View.VISIBLE);
//			
//			advStunServer.	setText(stunServer);
//			advStunPort.	setText(stunPort);
//		} else subframeSubStun.setVisibility(View.GONE);
//		
//		if(mmtel){
//			subframeSubMmtel.setVisibility(View.VISIBLE);
//			
//			advMmtelQvalue.	setText(mmtel_qvalue);
//		} else subframeSubMmtel.setVisibility(View.GONE);
//		
//		if(pbxes_callthru){
//			subframeSubCallthru.setVisibility(View.VISIBLE);
//			
//			pbx_pref_callthru2.setText(pbxes_callthru2);
//		} else subframeSubCallthru.setVisibility(View.GONE);
//		
//		if(pbxes_posurl.length()>10){
//			subframeLocationCallBack.setVisibility(View.VISIBLE);
//			
//			pbx_update_location.	setChecked(pbxes_pos);
//			pbx_trigger_callback.	setChecked(pbxes_callback);
//		} else subframeLocationCallBack.setVisibility(View.GONE);
//		
//		isFirstLoad = false;
//	}
	
	/**
	 * The listener for the Spinners.
	 * @param key The key indicating the specific Spinner it is used for
	 * @return The OnItemSelectedListener object
	 */
	private OnItemSelectedListener getOnItemSelectedListener(final String key) {
		return new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				if(key.equals(KEY_PROTOCOL)) {
					protocol = position;
				} else if(key.equals(KEY_CONNECTION)) {
					connection = position;
				} else if(key.equals(KEY_CALL_TYPE)) {
					calltypeIndex = position;
					callType = callTypeArray[calltypeIndex];
				}else if(key.equals(KEY_VIDEO_QUALITY)){
					video_quality = position == 0 ? "high":"low";
				}
			}
			public void onNothingSelected(AdapterView<?> parent) {
				if(key.equals(KEY_PROTOCOL)) {
					protocol = Integer.parseInt(context.getString(R.string.settings_default_protocol));
				} else if(key.equals(KEY_CONNECTION)) {
					connection = Integer.parseInt(context.getString(R.string.settings_default_connection));
				} else if(key.equals(KEY_CALL_TYPE)) {
					calltypeIndex = Integer.parseInt(context.getString(R.string.settings_default_calltype));
					callType = callTypeArray[calltypeIndex];
				} else if(key.equals(KEY_VIDEO_QUALITY)){
					video_quality = "low";
				}
			}
		};
	}
	
	/**
	 * The text change listener for the EditTexts.
	 * @param editText The EditText object to be listened
	 * @param key The key indicating the specific EditText it is used for
	 * @return The TextWatcher object
	 */
	private TextWatcher getTextWatcher(final EditText editText, final String key) {
		return new TextWatcher() {
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
	        public void onTextChanged(CharSequence s, int start, int before, int count) {}
			public void afterTextChanged(Editable s) {
				if(key.equals(KEY_PORT)) {
					port = editText.getText().toString();
				} else if(key.equals(KEY_USERNAME0)){
					username[active_account] = editText.getText().toString();
				} else if(key.equals(KEY_PASSWORD0)){
					password[active_account] = editText.getText().toString();
				} else if(key.equals(KEY_SERVER0)){
					server[active_account] = editText.getText().toString();
				} else if(key.equals(KEY_DOMAIN0)){
					domain[active_account] = editText.getText().toString();
				} else if(key.equals(KEY_FROMUSER0)){
					from_user[active_account] = editText.getText().toString();
				} else if(key.equals(KEY_PBXES_POSURL)){
					pbxes_posurl = editText.getText().toString();
					if(s.length()>10) {
						subframeLocationCallBack.setVisibility(View.VISIBLE);
					}else {
						subframeLocationCallBack.setVisibility(View.GONE);
					}
				} else if(key.equals(KEY_PBXES_CALLTHRU2)){
					pbxes_callthru2 = editText.getText().toString();
				} else if(key.equals(KEY_SEARCH)){
					adv_search = editText.getText().toString();
				} else if(key.equals(KEY_EXCLUDE)){
					adv_exclude = editText.getText().toString();
				} else if(key.equals(KEY_STUNSERVER)){
					stunServer = editText.getText().toString();
				} else if(key.equals(KEY_STUNPORT)){
					stunPort = editText.getText().toString();
				} else if(key.equals(KEY_MMTELQVALUE)){
					mmtel_qvalue = editText.getText().toString();
				}
			}
		};
	}
	
	/**
	 * The listener for the check boxes.
	 * @param checkbox The check box object to be listened
	 * @param key The key indicating the specific CheckBox it is used for
	 * @return The OnCheckedChangeListener object
	 */
	private OnCheckedChangeListener getOnCheckedChangeListener(final CheckBox checkbox, final String key) {
		return new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView,	boolean isChecked) {
				if(key.equals(KEY_STUN)) {
					useStun = checkbox.isChecked();
					if(useStun) {
						subframeSubStun.setVisibility(View.VISIBLE);
					} else {
						subframeSubStun.setVisibility(View.GONE);
					}
				} else if(key.equals(KEY_MMTEL)){
					mmtel = checkbox.isChecked();
					if(mmtel) {
						subframeSubMmtel.setVisibility(View.VISIBLE);
					} else {
						subframeSubMmtel.setVisibility(View.GONE);
					}
				}else if(key.equals(KEY_PBXES_CALLTHRU)) {
					pbxes_callthru = checkbox.isChecked();
					if(pbxes_callthru) {
						subframeSubCallthru.setVisibility(View.VISIBLE);
					} else {
						subframeSubCallthru.setVisibility(View.GONE);
					}
				} else if(key.equals(KEY_AUTOANSWER)) {
					autoAnswer = checkbox.isChecked();
				} else if(key.equals(KEY_AUTOANSWEROD)) {
					autoAnswerOD = checkbox.isChecked();
					
				} else if(key.equals(KEY_AUTOANSWERHS)) {
					autoAnswerHS = checkbox.isChecked();
					
				} else if(key.equals(KEY_NOTIF_MCALL)) {
					notif_mcall = checkbox.isChecked();
					
				} else if(key.equals(KEY_NOTIF_VMAIL)) {
					notif_vmail = checkbox.isChecked();
				} 
//				else if(key.equals(KEY_NOTIF_REG)) {
//					notif_reg = checkbox.isChecked();
//				} 
				else if(key.equals(KEY_NOTIF_NODATA)){
					notif_nodata = checkbox.isChecked();
				}
			}
		};
	}
	
	/** 
	 * Initializes the database for the settings preset. 
	 * If the database already exists, the contents of an existing table is loaded to the screen.
	 * Otherwise, create a new one, load default preset values to the screen and save it the database.
	 * @see #createDB(File)
	 * @see #loadDefaultPreset()
	 * @see #savePreset(File)
	 * @see #getPresetFromDB(File)
	 * @see #loadPreset(Cursor)
	 * @see #applyPreset()
	 */
	private void initDB() {
		/** A cursor holding all the saved settings preset. */
		Cursor presetCursor = getPresetCursor();
		if(presetCursor == null) {
			System.out.println("Creating New Database");
			createDB();
	    	loadDefaultPreset();
			savePreset();
			
		}
		else {
//			startManagingCursor(presetCursor);
			loadPreset(presetCursor);
			presetCursor.close();
		}
		
    }
	
	/** 
	 * Initializes the RingtoneManager, and the ringtoneListAdapter and its contents. 
	 */ 
	private void initTone()	{
		/** Used to temporarily store the list of ringtones while it is being built by the program. */
		List<String> ringtoneList = new ArrayList<String>();
        ringtoneList.add(context.getString(R.string.tone_label_default));
        ringtoneList.add(context.getString(R.string.tone_label_silent));
        
        ringtoneManager = new RingtoneManager(context); 
        ringtoneCursor = ringtoneManager.getCursor();
       
//        startManagingCursor(ringtoneCursor);
        if(ringtoneCursor.moveToFirst()) {
	    	do{
	        	ringtoneList.add(ringtoneCursor.getString(ringtoneCursor.getColumnIndex(MediaStore.MediaColumns.TITLE)));
	        }while(ringtoneCursor.moveToNext());
        }
        
    	ringtoneListAdapter = new ArrayAdapter<String>(context, 
    			android.R.layout.select_dialog_singlechoice, ringtoneList);
//        setVolumeControlStream(ringtoneManager.inferStreamType()); //XXX 
    	 ringtoneCursor.close();
	}
	
	/** 
	 * Creates the settings database. 
	 */
	private void createDB(){
		try {
			db = context.openOrCreateDatabase(DATABASE_NAME, Context.MODE_PRIVATE, null);
            db.execSQL("CREATE TABLE IF NOT EXISTS " + DATABASE_TABLE + " (" + 
            		KEY_PRESET + " text primary key not null, " +
            		KEY_USERNAME0 + " text, " +
            		KEY_PASSWORD0 + " text, " +
            		KEY_SERVER0 + " text, " +
            		KEY_DOMAIN0 + " text, " +
            		KEY_FROMUSER0 + " text, " +
            		KEY_USERNAME1 + " text, " + 
            		KEY_PASSWORD1 + " text, " +
            		KEY_SERVER1 + " text, " + 
            		KEY_DOMAIN1 + " text, " +
            		KEY_FROMUSER1 + " text, " +
            		KEY_PORT + " text, " + 
            		KEY_PROTOCOL + " integer not null, " + 
            		KEY_CONNECTION + " integer not null, " + 
            		
            		KEY_STUN + " text not null, " + 
            		KEY_STUNSERVER + " text, " +
            		KEY_STUNPORT + " text, " +
            		
            		KEY_MMTEL + " text not null, " +
            		KEY_MMTELQVALUE + " text not null, " + 
            		KEY_WIFI_KEEP_ON + " text not null, " + 
            		KEY_WIFI_OWN + " text not null, " + 
            		KEY_WIFI_SELECT + " text not null, " + 
            		KEY_PBXES_CALLBACK + " text not null, " + 
            		KEY_PBXES_CALLTHRU + " text not null, " + 
            		KEY_PBXES_CALLTHRU2 + " text, " + 
            		KEY_PBXES_IMPROVE + " text not null, " + 
            		KEY_PBXES_PAR + " text not null, " + 
            		KEY_PBXES_POS + " text not null, " +
            		KEY_PBXES_POSURL + " text, " +
            		KEY_NOTIF_MCALL + " text not null, " +
            		KEY_NOTIF_NODATA + " text not null, " +
            		KEY_NOTIF_VMAIL + " text not null, " +
//            		KEY_CALL_INUSE + " text not null, " + 
//            		KEY_CALL_ONDEMAND + " text not null, " +
//            		KEY_CALL_ONHEADSET + " text not null, " +
            		KEY_CALL_TYPE + " integer not null, " +
            		KEY_VIDEO_QUALITY + " text not null, " +
            		KEY_EXCLUDE + " text, " +
            		KEY_SEARCH + " text, " + 
            		KEY_AUTOANSWER + " text not null, " +
            		KEY_AUTOANSWEROD + " text not null, " + 
            		KEY_AUTOANSWERHS + " text not null, " +
            		KEY_RINGTONEURI + " text not null, " + 
            		KEY_RINGTONENAME + " text not null);");
            db.close();
    	} catch (Exception e) {
    		if (db != null)
				db.close();
    	}
	}
	
	/**
	 * Loads the default settings preset values to the screen. 
	 */
	private void loadDefaultPreset() {
		for(int i = 0;i <SipdroidEngine.LINES;i++){
			username[i] = new String();
			password[i] = new String();
			server[i] = new String();
			domain[i] = new String();
			from_user[i] = new String();
		}
		
		port = context.getString(R.string.settings_default_port);
		protocol = Integer.parseInt(context.getString(R.string.settings_default_protocol));
		connection = Integer.parseInt(context.getString(R.string.settings_default_connection));
		stunPort = context.getString(R.string.settings_default_stun_port);
		stunServer = context.getString(R.string.settings_default_stun_server);
		calltypeIndex = Integer.parseInt(context.getString(R.string.settings_default_calltype));
		ringtoneUri = context.getString(R.string.settings_default_ringtoneuri);
		ringtoneName = context.getString(R.string.settings_default_ringtonename);
		video_quality = "low";
	}
	
	/**
	 * Gets the cursor containing the saved preset from the database.
	 * @param dbFile the database File
	 * @return the cursor holding the table of the saved preset values 
	 */
	private Cursor getPresetCursor() {
		try{
			db = context.openOrCreateDatabase(DATABASE_NAME, Context.MODE_PRIVATE, null);
			Cursor presetCursor = db.query(DATABASE_TABLE, null, null, null, null, null, null); //load all
//			startManagingCursor(presetCursor);
			presetCursor.moveToFirst();
			db.close();
			return presetCursor;
		} catch (Exception e){
			if (db != null)
				db.close();
			return null;
		}
	}
	
	/**
	 * Loads the settings preset values stored in a cursor into the screen.
	 * @param cursor the cursor holding the table of the saved preset values
	 */
	private void loadPreset(Cursor cursor) {	
		String test; //tester if the domain and caller id is null
		username[0] = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USERNAME0));
		password[0] = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PASSWORD0));
		server[0] = cursor.getString(cursor.getColumnIndexOrThrow(KEY_SERVER0));
		
		test = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DOMAIN0));
		domain[0] = test.length() == 0 ? new String(): test;
		test = cursor.getString(cursor.getColumnIndexOrThrow(KEY_FROMUSER0));
		from_user[0] = test.length() == 0 ? new String(): test;
		username[1] = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USERNAME1));
		password[1] = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PASSWORD1));
		server[1] = cursor.getString(cursor.getColumnIndexOrThrow(KEY_SERVER1));
		
		test = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DOMAIN1));
		domain[1] = test.length() == 0 ? new String(): test;
		test = cursor.getString(cursor.getColumnIndexOrThrow(KEY_FROMUSER1));
		from_user[1] = test.length() == 0 ? new String(): test;
		
		port =cursor.getString(cursor.getColumnIndexOrThrow(KEY_PORT));
		protocol = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_PROTOCOL));
		connection = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_CONNECTION));
		useStun = Boolean.parseBoolean(cursor.getString(cursor.getColumnIndexOrThrow(KEY_STUN)));
		stunServer = cursor.getString(cursor.getColumnIndexOrThrow(KEY_STUNSERVER));
		stunPort = cursor.getString(cursor.getColumnIndexOrThrow(KEY_STUNPORT));
		mmtel = Boolean.parseBoolean(cursor.getString(cursor.getColumnIndexOrThrow(KEY_MMTEL)));
		mmtel_qvalue = cursor.getString(cursor.getColumnIndexOrThrow(KEY_MMTELQVALUE));
		wifi_keepon = Boolean.parseBoolean(cursor.getString(cursor.getColumnIndexOrThrow(KEY_WIFI_KEEP_ON)));
		wifi_own = Boolean.parseBoolean(cursor.getString(cursor.getColumnIndexOrThrow(KEY_WIFI_OWN)));
		wifi_select = Boolean.parseBoolean(cursor.getString(cursor.getColumnIndexOrThrow(KEY_WIFI_SELECT)));
		pbxes_callback = Boolean.parseBoolean(cursor.getString(cursor.getColumnIndexOrThrow(KEY_PBXES_CALLBACK)));
		pbxes_callthru = Boolean.parseBoolean(cursor.getString(cursor.getColumnIndexOrThrow(KEY_PBXES_CALLTHRU)));
		pbxes_callthru2 = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PBXES_CALLTHRU2));
		pbxes_improve = Boolean.parseBoolean(cursor.getString(cursor.getColumnIndexOrThrow(KEY_PBXES_IMPROVE)));
		pbxes_par = Boolean.parseBoolean(cursor.getString(cursor.getColumnIndexOrThrow(KEY_PBXES_PAR)));
		pbxes_pos = Boolean.parseBoolean(cursor.getString(cursor.getColumnIndexOrThrow(KEY_PBXES_POS)));
		pbxes_posurl = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PBXES_POSURL));
		notif_mcall = Boolean.parseBoolean(cursor.getString(cursor.getColumnIndexOrThrow(KEY_NOTIF_MCALL)));
		notif_nodata = Boolean.parseBoolean(cursor.getString(cursor.getColumnIndexOrThrow(KEY_NOTIF_NODATA)));
		notif_vmail = Boolean.parseBoolean(cursor.getString(cursor.getColumnIndexOrThrow(KEY_NOTIF_VMAIL)));
		calltypeIndex = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_CALL_TYPE));
		video_quality = cursor.getString(cursor.getColumnIndexOrThrow(KEY_VIDEO_QUALITY));
		adv_search = cursor.getString(cursor.getColumnIndexOrThrow(KEY_SEARCH));
		adv_exclude = cursor.getString(cursor.getColumnIndexOrThrow(KEY_EXCLUDE));
		autoAnswer = Boolean.parseBoolean(cursor.getString(cursor.getColumnIndexOrThrow(KEY_AUTOANSWER)));
		autoAnswerHS = Boolean.parseBoolean(cursor.getString(cursor.getColumnIndexOrThrow(KEY_AUTOANSWERHS)));
		autoAnswerOD = Boolean.parseBoolean(cursor.getString(cursor.getColumnIndexOrThrow(KEY_AUTOANSWEROD)));
		ringtoneUri = cursor.getString(cursor.getColumnIndexOrThrow(KEY_RINGTONEURI));
		ringtoneName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_RINGTONENAME));
		
	}
	
	/**
	 * Saves the settings preset values currently displayed on the screen to the database. 
	 * @param dbFile the database File
	 */ 
	private void savePreset() {
		new Thread(new Runnable() {
			public void run() {
				try {
					db = context.openOrCreateDatabase(DATABASE_NAME, Context.MODE_PRIVATE, null);
					db.delete(DATABASE_TABLE, null, null);
					
					ContentValues values = new ContentValues();
					values.put(KEY_PRESET, 1);
		    		values.put(KEY_USERNAME0, username[0]);
		    		values.put(KEY_PASSWORD0, password[0]);
		    		values.put(KEY_SERVER0, server[0]);
		    		values.put(KEY_DOMAIN0, domain[0]);
		    		values.put(KEY_FROMUSER0, from_user[0]);
		    		values.put(KEY_USERNAME1, username[1]); 
		    		values.put(KEY_PASSWORD1, password[1]);
		    		values.put(KEY_SERVER1, server[1]); 
		    		values.put(KEY_DOMAIN1, domain[1]);
		    		values.put(KEY_FROMUSER1, from_user[1]);
		    		values.put(KEY_PORT, port); 
		    		values.put(KEY_PROTOCOL, protocol);
		    		values.put(KEY_CONNECTION, connection);
		    		values.put(KEY_STUN, String.valueOf(useStun)); 
		    		values.put(KEY_STUNSERVER, stunServer);
		    		values.put(KEY_STUNPORT, stunPort);
		    		values.put(KEY_MMTEL, String.valueOf(mmtel));
		    		values.put(KEY_MMTELQVALUE, mmtel_qvalue); 
		    		values.put(KEY_WIFI_KEEP_ON, String.valueOf(wifi_keepon)); 
		    		values.put(KEY_WIFI_OWN, String.valueOf(wifi_own)); 
		    		values.put(KEY_WIFI_SELECT, String.valueOf(wifi_select)); 
		    		values.put(KEY_PBXES_CALLBACK, String.valueOf(pbxes_callback)); 
		    		values.put(KEY_PBXES_CALLTHRU, String.valueOf(pbxes_callthru)); 
		    		values.put(KEY_PBXES_CALLTHRU2, pbxes_callthru2); 
		    		values.put(KEY_PBXES_IMPROVE, String.valueOf(pbxes_improve)); 
		    		values.put(KEY_PBXES_PAR, String.valueOf(pbxes_par)); 
		    		values.put(KEY_PBXES_POS, String.valueOf(pbxes_pos));
		    		values.put(KEY_PBXES_POSURL, pbxes_posurl);
		    		values.put(KEY_NOTIF_MCALL, String.valueOf(notif_mcall));
		    		values.put(KEY_NOTIF_NODATA, String.valueOf(notif_nodata));
		    		values.put(KEY_NOTIF_VMAIL, String.valueOf(notif_vmail));
		    		values.put(KEY_CALL_TYPE, calltypeIndex);
		    		values.put(KEY_VIDEO_QUALITY, video_quality);
		    		values.put(KEY_EXCLUDE, adv_exclude);
		    		values.put(KEY_SEARCH, adv_search); 
		    		values.put(KEY_AUTOANSWER, String.valueOf(autoAnswer));
		    		values.put(KEY_AUTOANSWEROD, String.valueOf(autoAnswerHS)); 
		    		values.put(KEY_AUTOANSWERHS, String.valueOf(autoAnswerOD));
		    		values.put(KEY_RINGTONEURI, ringtoneUri); 
		    		values.put(KEY_RINGTONENAME, ringtoneName);
		    		db.insertOrThrow(DATABASE_TABLE, null, values);
		    		
			        db.close();
				} catch (Exception e){
					e.printStackTrace();
					if (db != null)
						db.close();
				}
			}
		}).start();
	}
	
	/** 
	 * Gets the string counterpart corresponding to the given protocol index.
	 * @param index the index of the applied protocol of choice
	 * @return the string name of the referred protocol of choice
	 */
	public static String getProtocol(int index) {
		if(index == 0)
			return PROTOCOL_UDP_NAME;
		else if(index == 1)
			return PROTOCOL_TCP_NAME;
		else
			return PROTOCOL_UDP_NAME;
	}
	
	/**
	 * Sets up the playback for the selected ringtone and updates the value of selectRingtonePos.
	 * @param position the position of the selected ringtone to be played
	 * @param delayMs the delay value for playing a ringtone in milliseconds
	 */
	private void playRingtone(int position) {
        if(position > -1){
	    	if(position == 0)
	        	ringtone = RingtoneManager.getRingtone(context, Settings.System.DEFAULT_RINGTONE_URI);
	        else if (position == 1)
	        	ringtone = null;
	        else
	    		ringtone = ringtoneManager.getRingtone(position-2);
	        
	    	if (ringtone != null)	
	            ringtone.play();
    	}
    }
    
	/** Stops any currently playing ringtone. */
    private void stopRingtone() {
        if (ringtone != null)
            ringtone.stop();
    }
	
	/**
	 * Updates the wifi sleep policy.
	 */
	private void updateSleep() {
		final ContentResolver cr = context.getContentResolver();
		final int get = android.provider.Settings.System.getInt(cr,
				android.provider.Settings.System.WIFI_SLEEP_POLICY, -1);
		int set = get;

		if (SettingsWindow.connection == SettingsWindow.CONNECTION_EDGE
				|| SettingsWindow.connection == SettingsWindow.CONNECTION_GPRS) {
			set = android.provider.Settings.System.WIFI_SLEEP_POLICY_DEFAULT;
			if (set != get)
				Toast.makeText(context, R.string.settings_policy_default,
						Toast.LENGTH_LONG).show();
		} else if (SettingsWindow.connection == SettingsWindow.CONNECTION_WLAN) {
			set = android.provider.Settings.System.WIFI_SLEEP_POLICY_NEVER;
			if (set != get)
				Toast.makeText(context, R.string.settings_policy_never,
						Toast.LENGTH_LONG).show();
		}
		if (set != get)
			android.provider.Settings.System.putInt(cr,
					android.provider.Settings.System.WIFI_SLEEP_POLICY, set);
	}

	/**
	 * The convenience method to register.
	 */
	private void register() {
		Receiver.engine(context.getApplicationContext()).StartEngine();
		if (Receiver.engine(context).isRegistered()) {
			SettingsWindow.isRegistered = true;
			Receiver.engine(context.getApplicationContext()).updateDNS();
			updateSleep();
			AetherVoice.setMustRefreshListener(true);
			registerButton.setBackgroundResource(R.drawable.btn_unregister_bg);
		} else
			Toast.makeText(context.getApplicationContext(), context.getString(R.string.toast_internet), Toast.LENGTH_SHORT).show();
	}

	/**
	 * The convenience method to unregister.
	 */
	private void unregister() {
		AetherVoice.setMustRefreshListener(false);
		SettingsWindow.isRegistered = false;
		Receiver.pos(true);
		Receiver.engine(context).halt();
		Receiver.mSipdroidEngine = null;
		Receiver.reRegister(0);
		context.stopService(new Intent(context,RegisterService.class));
		registerButton.setBackgroundResource(R.drawable.btn_register_bg);
	}

	/**
	 * Defines the lines of code to be executed a button is pressed.
	 * <ul>
	 * <li>Register - Starts/Restarts the application engine</li>
	 * <li>Unregister - Stops the application engine</li>
	 * <li>Ringtone - Selection for ringtones</li>
	 * </ul>
	 * 
	 * @see #ringtoneAlertDialog()
	 * @see #savePreset()
	 * @see #getPresetCursor()
	 * @see #loadPreset(Cursor)
	 * @see #applyPreset()
	 * @see Receiver#isFast()
	 * @see Receiver#engine(Context)
	 * @see AetherVoice#setMustRefreshListener(boolean)
     */
    public void onClick(View v) {
    	int id = v.getId();
    	switch(id){
    	case R.id.btn_save:
			savePreset();
			Toast.makeText(context, "Settings have been saved", Toast.LENGTH_SHORT).show();
    		break;
    	case R.id.btn_revert:
    		Cursor presetCursor = getPresetCursor();
//			startManagingCursor(presetCursor);
			if(presetCursor == null){
				//The database cannot be accessed
			}else
				if(presetCursor.moveToFirst()) {
					loadPreset(presetCursor);
//					initView();
					presetCursor.close();
				}
			
    		break;
    	case R.id.btn_register:
    		if (AetherVoice.isCalling){
    			Toast.makeText(context, "Call in progress", Toast.LENGTH_LONG).show();
    		}
    		else{	
    			if(isRegistered) {
    				unregister();
	    		} else {
	    			register();
				}
    		}
    		break;

    	case R.id.account_submenu:	
    	case R.id.account_title:
    		resetFramesAndIcons();
    		accountTitle.			setBackgroundResource(R.drawable.header_collapse_bg);
    		
    		frameAccountInfo.setVisibility(View.GONE);
    		frameAccount.setVisibility(View.VISIBLE);
    		
    		account1.setVisibility(View.VISIBLE);
    		account2.setVisibility(View.VISIBLE);
    		
    		if(username[0].length() > 0 && server[0].length() > 0) account1.setText(username[0]+"@"+server[0]);
    		if(username[1].length() > 0 && server[1].length() > 0) account2.setText(username[1]+"@"+server[1]);
    		break;
    	case R.id.notifications_title:
    		resetFramesAndIcons();
    		
    		((LinearLayout) v).setBackgroundResource(R.drawable.header_collapse_bg);
    		frameNotification.setVisibility(View.VISIBLE);
    		
    		break;
    	case R.id.advance_settings_title:
    		resetFramesAndIcons();
    		
    		((LinearLayout) v).setBackgroundResource(R.drawable.header_collapse_bg);
    		hideOtherAdvanceOptions();
    		frameAdvanced.setVisibility(View.VISIBLE);
    		break;
    	case R.id.pbxes_option_title:
    		resetFramesAndIcons();
    		
    		((LinearLayout) v).setBackgroundResource(R.drawable.header_collapse_bg);
    		framePbxes.setVisibility(View.VISIBLE);
//    		if(!(active_account == id)){
////    			subframePbx.setVisibility(View.VISIBLE);
////    			framePbxes.setVisibility(View.VISIBLE);
//    			active_account = id;
//    		}else active_account = -1;
    		break;
    	case R.id.account1:
    		active_account = 0;
    		activateAccountList();
    		break;
    	case R.id.account2:
    		active_account = 1;
    		activateAccountList();
    		break;
    	case R.id.ringtone:
    		ringtoneAlertDialog().show();
    		break;
    	case R.id.adv_call_exceptions:
    		hideOtherAdvanceOptions();
    		if(!(active_account == id)){
    			subframeCallExceptions.setVisibility(View.VISIBLE);
    			active_account = id;
    		}else active_account = -1;
    		break;
    	case R.id.adv_mmtel_option:
    		hideOtherAdvanceOptions();
    		if(!(active_account == id)){
    			subframeMmtel.setVisibility(View.VISIBLE);
    			active_account = id;
    		}else active_account = -1;
    		break;
    	
    	case R.id.adv_stun_option:
    		hideOtherAdvanceOptions();
    		if(!(active_account == id)){
    			subframeStunOptions.setVisibility(View.VISIBLE);
    			active_account = id;
    		}else active_account = -1;
    		break;
    	case R.id.adv_video_quality:
    		hideOtherAdvanceOptions();
    		if(!(active_account == id)){
    			subframeVideoOptions.setVisibility(View.VISIBLE);
    			active_account = id;
    		}else active_account = -1;
    		break;
    	case R.id.adv_wireless_option:
    		hideOtherAdvanceOptions();
    		if(!(active_account == id)){
    			subframeWireless.setVisibility(View.VISIBLE);
    			active_account = id;
    		}else active_account = -1;
    		break;
		default:
			System.out.println("ERROR REACHED THE DEFAULT SWITCH CASE WITH VALUE OF "+v.getId());
			break;
    	}
	}
    
	private void resetFramesAndIcons() {
		accountTitle.		setBackgroundResource(R.drawable.header_expand_bg);
		notifications.		setBackgroundResource(R.drawable.header_expand_bg);
		advanceSettings.	setBackgroundResource(R.drawable.header_expand_bg);
		pbxesOptions.		setBackgroundResource(R.drawable.header_expand_bg);
		
		frameAdvanced.setVisibility(View.GONE);
		frameNotification.setVisibility(View.GONE);
		frameAccount.setVisibility(View.GONE);
		framePbxes.setVisibility(View.GONE);
		frameAccount.clearFocus();
	}
	
	private void hideOtherAdvanceOptions() {
		subframeCallExceptions.setVisibility(View.GONE);
		subframeVideoOptions.setVisibility(View.GONE);
		subframeStunOptions.setVisibility(View.GONE);
		subframeMmtel.setVisibility(View.GONE);
		subframeWireless.setVisibility(View.GONE);
//		subframePbx.setVisibility(View.GONE);
	}
	private void activateAccountList() {
		account1.setVisibility(View.GONE);
		account2.setVisibility(View.GONE);
		frameAccountInfo.setVisibility(View.VISIBLE);
		frameAccountInfo.requestFocus();
		
		fieldCallerID.setText(from_user[active_account]);
		fieldDomain.setText(domain[active_account]);
		fieldPassword.setText(password[active_account]);
		fieldServer.setText(server[active_account]);
		fieldUsername.setText(username[active_account]);
	}

	/**
	 * Defines the AlertDialog to be launched when showDialog(id) is called, and
	 * in this case, the select ringtone Alert Dialog is created with single
	 * choice list, assuming the contents of ringtoneListAdapter, while having a
	 * listener when an item is selected, and when one of the two buttons is
	 * pressed.
	 * <ul>
	 * <li>When an item is selected - Play the selected ringtone for preview,
	 * save its position in the list and its name</li>
	 * <li>When "Use Selected Ringtone" is pressed - Save the Uri of the
	 * selected ringtone as a string, and display its name on the screen</li>
	 * <li>When "Cancel" is pressed - exit the AlertDialog without any changes,
	 * and reset the variables storing the selected ringtone's name and position
	 * in the list</li>
	 * </ul>
	 * @see #playRingtone(int)
	 * @see #stopRingtone()
     */
    private AlertDialog ringtoneAlertDialog(){
	    return new AlertDialog.Builder(context)
	        .setIcon(android.R.drawable.ic_media_play)
	        .setTitle(R.string.alert_message_ringtone)
	        .setSingleChoiceItems(ringtoneListAdapter, -1, new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int whichButton) {
	            	System.out.println("Position = " + String.valueOf(whichButton));
	            	stopRingtone();
	            	selectRingtonePos = whichButton;
	            	playRingtone(whichButton);
	            	tempToneName = (String) ringtoneListAdapter.getItem(whichButton);
	            }
	        })
	        .setPositiveButton(R.string.alert_button_ringtone, new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int whichButton) {
	            	stopRingtone();
	            	if(selectRingtonePos > -1){
	            		if(selectRingtonePos == 0)
	            			ringtoneUri = Settings.System.DEFAULT_RINGTONE_URI.toString();
	            		else if (selectRingtonePos == 1)
	            			ringtoneUri = "";
	            		else
	            			ringtoneUri = ringtoneManager.getRingtoneUri(selectRingtonePos-2).toString();
	            			btnRingtone.setText((ringtoneName = tempToneName));
//	            		editRingtoneName.setText(tempToneName); TODO: fix
	            	}	
	            }
	        })
	        .setNegativeButton(R.string.alert_button_cancel, new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int whichButton) {
	            	stopRingtone();
	            	selectRingtonePos = -1;
	            	tempToneName = "";
	            }
	        })
	        .create();
     }
    
    
	
	/**
	 * Called when the tab is reselected
	 */
//	@Override
//	public void onResume() {
//		super.onResume();
//		if(isRegistered) (findViewById(R.id.btn_register))	.setBackgroundResource(R.drawable.btn_unregister_bg);
//		
//		AetherVoice.setIsFinishing(false);
//	}
//	
//	@Override
//	protected void onPause() {
//		System.out.println("onPause");
//		super.onPause();
//	}
	
//	/**
//	 * Called when the SettingsWindow activity is destroyed, and when that
//	 * happens, the database is closed.
//	 */
//	@Override
//	protected void onDestroy() {
//		super.onDestroy();
////		Receiver.pos(true);
////		Receiver.engine(this).halt();
////		Receiver.mSipdroidEngine = null;
////		Receiver.reRegister(0);
////		stopService(new Intent(this,RegisterService.class));
//		
//		if (db != null)
//			db.close();
//	}

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
			return true;*/
		//return super.onKeyDown(keyCode, event);
		return false;
	}
}
