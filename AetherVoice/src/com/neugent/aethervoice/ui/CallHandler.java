/**
 * @file AetherVoice.java
 * @brief It contains the Call Handler class, the global handler that listens to threads in the AetherVoice application.
 * @author Amando Jose Quinto II
 */
package com.neugent.aethervoice.ui;

import org.sipdroid.sipua.UserAgent;
import org.sipdroid.sipua.ui.Receiver;

import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.view.inputmethod.InputMethodManager;

/**
 * Defines the global handler that listens to threads in the AetherVoice
 * application.
 * 
 * @author Amando Jose Quinto II
 */
public class CallHandler extends Handler {

	/** The defined index for the display call message. **/
	public static final int MSG_DISPLAY_CALL = 0;

	/** The defined index for the incoming call message. **/
	public static final int MSG_INCOMING_CALL = 1;

	/** The defined index for the answer call message. **/
	public static final int MSG_ANSWER_CALL = 2;

	/** The defined index for the reject call message. **/
	public static final int MSG_REJECT_CALL = 3;

	/** The defined index for the end call message. **/
	public static final int MSG_END_CALL = 4;

	/** The defined index for the answer with speaker message. **/
	public static final int MSG_INCALL_ANSWER_SPEAKER = 5;

	/** The defined index for the back message. **/
	public static final int MSG_INCALL_BACK = 6;

	/** The defined index for the tick message. **/
	public static final int MSG_INCALL_TICK = 7;

	/**
	 * The method called from threads to manipulate UI from call events.
	 * 
	 * @see AetherVoice#showCallFrame(Context)
	 * @see InCall#refreshInCall()
	 * @see InCall#answer()
	 * @see InCall#reject()
	 * @see Receiver#engine(Context)
	 */
	@Override
	public void handleMessage(final Message msg) {
		switch (msg.what) {
		case MSG_DISPLAY_CALL:
				AetherVoice.mCallScreen.refreshInCall();
			break;
		case MSG_INCOMING_CALL:
			//added 12-8-10. closes softkeyboard during incoming call
			System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>INCOMING CALL");
			Context context = (Context) msg.obj;
			final InputMethodManager mgr = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
			mgr.hideSoftInputFromWindow(AetherVoice.dialer.dialBox.getWindowToken(), 0);

			AetherVoice.dialer.switchtoVOIP(true);
			
			AetherVoice.showCallFrame(context);

			break;
		case MSG_ANSWER_CALL:
			AetherVoice.dialer.switchtoCall(true);
			AetherVoice.mCallScreen.answer();
			break;
		case MSG_REJECT_CALL:
			AetherVoice.dialer.switchtoCall(false);
			AetherVoice.mCallScreen.reject();
			break;
		case MSG_END_CALL:
			AetherVoice.dialer.switchtoCall(false);
			AetherVoice.mCallScreen.reject();
			 
			break;
		case MSG_INCALL_ANSWER_SPEAKER:
			if (Receiver.call_state == UserAgent.UA_STATE_INCOMING_CALL) {
				AetherVoice.mCallScreen.answer();
				Receiver.engine((Context) msg.obj).speaker(AudioManager.MODE_NORMAL);
			}
			break;
		case MSG_INCALL_BACK:
			AetherVoice.dialer.switchtoCall(false);
			AetherVoice.mCallScreen.reject();
			
			System.out.println(">>>>>>>>>>>>>>>>>>>>> hiding callframe <<<<<<<<<<<<<<<<<<");
			AetherVoice.hideCallFrame(false);
			break;
		case MSG_INCALL_TICK:
//			AetherVoice.mCallScreen.csCodec.setText(RtpStreamReceiver.getCodec());
//			if (RtpStreamReceiver.good != 0) {
//				if (RtpStreamReceiver.timeout != 0)
//					AetherVoice.mCallScreen.csStats.setText("no data");
//				else if (RtpStreamSender.m == 2)
//					AetherVoice.mCallScreen.csStats
//							.setText(Math.round(RtpStreamReceiver.loss
//									/ RtpStreamReceiver.good * 100)
//									+ "%loss, "
//									+ Math.round(RtpStreamReceiver.lost
//											/ RtpStreamReceiver.good * 100)
//									+ "%lost, "
//									+ Math.round(RtpStreamReceiver.late
//											/ RtpStreamReceiver.good * 100)
//									+ "%late (>"
//									+ (RtpStreamReceiver.jitter - 250 * RtpStreamReceiver.mu)
//									/ 8 / RtpStreamReceiver.mu + "ms)");
//				else
//					AetherVoice.mCallScreen.csStats
//							.setText(Math.round(RtpStreamReceiver.lost
//									/ RtpStreamReceiver.good * 100)
//									+ "%lost, "
//									+ Math.round(RtpStreamReceiver.late
//											/ RtpStreamReceiver.good * 100)
//									+ "%late (>"
//									+ (RtpStreamReceiver.jitter - 250 * RtpStreamReceiver.mu)
//									/ 8 / RtpStreamReceiver.mu + "ms)");
//				AetherVoice.mCallScreen.csStats.setVisibility(View.VISIBLE);
//			} else
//				AetherVoice.mCallScreen.csStats.setVisibility(View.GONE);
			break;
		}
		super.handleMessage(msg);
	}
}
