package com.neugent.aethervoice.call;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

import com.neugent.aethervoice.R;

public class PSTNCallListener{
	private final String ACTION_PHONE_STATE_CHANGED = "android.intent.action.PHONE_STATE";
	private final String PAUSE_ACTION = "com.android.music.musicservicecommand.pause";
	private final String PLAY_ACTION = "com.android.music.musicservicecommand.play";
	final static String TOGGLEPAUSE_ACTION = "com.android.music.musicservicecommand.togglepause";
	
	private Context mContext;
	private boolean was_playing;
	private String mLaststate;
	private String mLastNumber;
	
//	public PSTNCallListener(Context context) {
//		mContext = context;
//	}
	
	public void broadcastCallStateChanged(String state, String number){
		if (state == null) {
            state = mLaststate;
            number = mLastNumber;
		}
		Intent intent = new Intent(ACTION_PHONE_STATE_CHANGED);
		intent.putExtra("state",state);
        if (number != null) intent.putExtra("incoming_number", number);
        intent.putExtra(mContext.getString(R.string.app_name), true);
        mContext.sendBroadcast(intent, android.Manifest.permission.READ_PHONE_STATE);
        if (state.equals("IDLE")) {
        	if (was_playing) {
//                 	if (pstn_state == null || pstn_state.equals("IDLE"))
                   	mContext.sendBroadcast(new Intent(TOGGLEPAUSE_ACTION));
                   was_playing = false;
            }
        } else {
            AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            if ((mLaststate == null || mLaststate.equals("IDLE")) && (was_playing = am.isMusicActive()))
                    mContext.sendBroadcast(new Intent(PAUSE_ACTION));
        }  
        
        mLaststate = state;
        mLastNumber = number;
	}

}
