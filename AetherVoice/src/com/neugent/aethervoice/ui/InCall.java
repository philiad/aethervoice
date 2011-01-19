/**
 * @file InCall.java
 * @brief It contains the InCall class, the class that contains all the necessary views and modules 
 * 		for instantiating the in-call interface.
 * @author Amando Jose Quinto II
 */
package com.neugent.aethervoice.ui;

import java.io.IOException;
import java.net.InetAddress;

import org.sipdroid.net.RtpPacket;
import org.sipdroid.net.RtpSocket;
import org.sipdroid.net.SipdroidSocket;
import org.sipdroid.sipua.UserAgent;
import org.sipdroid.sipua.phone.Call;
import org.sipdroid.sipua.phone.CallCard;
import org.sipdroid.sipua.phone.Phone;
import org.sipdroid.sipua.ui.Receiver;

import android.app.KeyguardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.neugent.aethervoice.R;

/**
 * A class that creates an interactive interface for managing calls.
 * 
 * @author AJ Quinto II
 */
public class InCall {

	/** The application context. **/
	private final Context context;

	/** The inflater object used to inflate views from resource. */
	private LayoutInflater inflater;

	/** The container of views for the in-call screen. **/
	private ViewGroup mInCallPanel;

	/** The view containing the inflated in-call screen layout. **/
	private View inCallView;

	/** The text view that displays the stats. **/
	public TextView mStats;

	/** The text view that displays the codec. **/
	public TextView mCodec;

	/** The CallCard instance. **/
	private CallCard mCallCard;

	/** The Phone instance. **/
	private Phone ccPhone;

	/** The manager class that locks and unlocks the keyboard. **/
	private KeyguardManager mKeyguardManager;

	/** The class that enables/disables the keyguard. **/
	private KeyguardManager.KeyguardLock mKeyguardLock;

	/** The flag that indicates whether the keyboard is enabled. **/
	private boolean enabled;

	/** The flag that indicates whether the toneGeneratorThread is running. **/
	private boolean running;

	/** The thread that implements tone generation. **/
	private Thread toneGeneratorThread;

	/** The variable containing the value for time out. **/
	private int oldtimeout;

	/** The defined screen time out value. **/
	private final int SCREEN_OFF_TIMEOUT = 12000;

	/** The instance of SipdroidSocket. **/
	private SipdroidSocket socket;

	/** The instance of RtpSocket. **/
	private RtpSocket rtp_socket;

	/**
	 * The constructor method of InCall class.
	 * 
	 * @param context
	 *            The application context
	 * 
	 * @see #initInCallView()
	 */
	public InCall(final Context context) {
		this.context = context;
		initInCallView();
	}

	/**
	 * Gets the instantiated in-call view.
	 * 
	 * @return The inCallView view
	 */
	public View getInCallView() {
		return inCallView;
	}

	/**
	 * Instantiates the inCallView and all its contents
	 */
	private void initInCallView() {
		inflater = LayoutInflater.from(context);
		inCallView = inflater.inflate(R.layout.aethervoice_incall, null);

		mInCallPanel = (ViewGroup) inCallView.findViewById(R.id.inCallPanel);

		final View callCardLayout = inflater.inflate(R.layout.call_card_popup,
				mInCallPanel);
		mCallCard = (CallCard) callCardLayout.findViewById(R.id.callCard);
		mCallCard.reset();

		mStats = (TextView) inCallView.findViewById(R.id.stats);
		mCodec = (TextView) inCallView.findViewById(R.id.codec);
		mCallCard.displayOnHoldCallStatus(ccPhone, null);
		mCallCard.displayOngoingCallStatus(ccPhone, null);
	}

	/**
	 * Refreshes the InCall, and executes a response depending on the new state.
	 * 
	 * @see #disableKeyguard()
	 * @see #callEnd()
	 */
	public void refreshInCall() {
		if (Receiver.call_state == UserAgent.UA_STATE_IDLE)
			AetherVoice.mHandler.sendEmptyMessageDelayed(
					CallHandler.MSG_INCALL_BACK,
					Receiver.call_end_reason == -1 ? 2000 : 5000);

		if (Integer.parseInt(Build.VERSION.SDK) < 5
				|| Integer.parseInt(Build.VERSION.SDK) > 7)
			disableKeyguard();

		if (Receiver.call_state == UserAgent.UA_STATE_INCALL
				&& socket == null
				&& Receiver.engine(context).getLocalVideo() != 0
				&& Receiver.engine(context).getRemoteVideo() != 0
//				&& PreferenceManager.getDefaultSharedPreferences(context).getString(org.sipdroid.sipua.ui.Settings.PREF_SERVER,org.sipdroid.sipua.ui.Settings.DEFAULT_SERVER).equals(org.sipdroid.sipua.ui.Settings.DEFAULT_SERVER))
				&& SettingsWindow.server.equals(org.sipdroid.sipua.ui.Settings.DEFAULT_SERVER))
			(new Thread() {
				@Override
				public void run() {
					final RtpPacket keepalive = new RtpPacket(new byte[12], 0);
					final RtpPacket videopacket = new RtpPacket(new byte[1000],
							0);

					try {
						if (rtp_socket == null) {
							rtp_socket = new RtpSocket(
									socket = new SipdroidSocket(Receiver
											.engine(context).getLocalVideo()),
									InetAddress.getByName(Receiver.engine(
											context).getRemoteAddr()), Receiver
											.engine(context).getRemoteVideo());
							Thread.sleep(3000);
						} else
							socket = rtp_socket.getDatagramSocket();
						rtp_socket.getDatagramSocket().setSoTimeout(15000);
					} catch (final Exception e) {
						if (!AetherVoice.release)
							e.printStackTrace();
						return;
					}

					keepalive.setPayloadType(126);

					try {
						rtp_socket.send(keepalive);
					} catch (final Exception e1) {
						return;
					}

					for (;;)
						try {
							rtp_socket.receive(videopacket);
						} catch (final IOException e) {
							rtp_socket.getDatagramSocket().disconnect();
							try {
								rtp_socket.send(keepalive);
							} catch (final IOException e1) {
								return;
							}
						}
				}
			}).start();

		// if (!AetherVoice.release) Log.i("SipUA:","on resume");
		switch (Receiver.call_state) {
		case UserAgent.UA_STATE_INCOMING_CALL:
			if (Receiver.pstn_state == null
					|| Receiver.pstn_state.equals("IDLE"))
				// if
				// (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(org.sipdroid.sipua.ui.Settings.PREF_AUTO_ON,
				// org.sipdroid.sipua.ui.Settings.DEFAULT_AUTO_ON) &&
				if (SettingsWindow.autoAnswer
						&& !mKeyguardManager.inKeyguardRestrictedInputMode())
					AetherVoice.mHandler.sendEmptyMessageDelayed(
							CallHandler.MSG_ANSWER_CALL, 1000);
				// else if
				// ((PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(org.sipdroid.sipua.ui.Settings.PREF_AUTO_ONDEMAND,
				// org.sipdroid.sipua.ui.Settings.DEFAULT_AUTO_ONDEMAND) &&
				else if (SettingsWindow.autoAnswerOD
						|| (SettingsWindow.autoAnswerHS && Receiver.headset > 0))
					// PreferenceManager.getDefaultSharedPreferences(context).getBoolean(org.sipdroid.sipua.ui.Settings.PREF_AUTO_DEMAND,
					// org.sipdroid.sipua.ui.Settings.DEFAULT_AUTO_DEMAND)) ||
					// (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(org.sipdroid.sipua.ui.Settings.PREF_AUTO_HEADSET,
					// org.sipdroid.sipua.ui.Settings.DEFAULT_AUTO_HEADSET) &&
					AetherVoice.mHandler.sendEmptyMessageDelayed(
							CallHandler.MSG_INCALL_ANSWER_SPEAKER, 10000);
			break;
		case UserAgent.UA_STATE_INCALL:
			if (Receiver.docked <= 0)
				screenOff(true);
			break;
		case UserAgent.UA_STATE_IDLE:
			callEnd();
			break;
		}
		if (Receiver.ccCall != null)
			mCallCard.displayMainCallStatus(ccPhone, Receiver.ccCall);
		// if (mSlidingCardManager != null) mSlidingCardManager.showPopup();
		AetherVoice.mHandler.sendEmptyMessage(CallHandler.MSG_INCALL_TICK);

		if (toneGeneratorThread == null
				&& Receiver.call_state != UserAgent.UA_STATE_IDLE) {
			running = true;
			(toneGeneratorThread = new Thread() {
				@Override
				public void run() {
					ToneGenerator tg = null;

					if (Settings.System.getInt(context.getContentResolver(),
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
						AetherVoice.mHandler
								.sendEmptyMessage(CallHandler.MSG_INCALL_TICK);
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
	 * Turns the screen on/off.
	 * 
	 * @param off
	 *            True if the screen is to be turned off
	 */
	private void screenOff(final boolean off) {
		final ContentResolver cr = context.getContentResolver();

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

	/**
	 * Disables the keyguard.
	 */
	public void disableKeyguard() {
		if (mKeyguardManager == null) {
			mKeyguardManager = (KeyguardManager) context
					.getSystemService(Context.KEYGUARD_SERVICE);
			mKeyguardLock = mKeyguardManager.newKeyguardLock("Sipdroid");
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
				Thread.sleep(1000);
			} catch (final InterruptedException e) {
			}

			if (mKeyguardManager == null) {
				mKeyguardManager = (KeyguardManager) context
						.getSystemService(Context.KEYGUARD_SERVICE);
				mKeyguardLock = mKeyguardManager.newKeyguardLock("Sipdroid");
				enabled = true;
			} else
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
				Receiver.engine(context).answercall();
			}
		}).start();

		if (Receiver.ccCall != null) {
			Receiver.ccCall.setState(Call.State.ACTIVE);
			Receiver.ccCall.base = SystemClock.elapsedRealtime();
			mCallCard.displayMainCallStatus(ccPhone, Receiver.ccCall);
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
			mCallCard.displayMainCallStatus(ccPhone, Receiver.ccCall);
		}
		(new Thread() {
			@Override
			public void run() {
				Receiver.engine(context).rejectcall();
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

		if (socket != null) {
			socket.close();
			socket = null;
		}

		// if (!AetherVoice.release) Log.i("SipUA:","on pause");

		switch (Receiver.call_state) {
		case UserAgent.UA_STATE_INCOMING_CALL:
			Receiver.moveTop();
			break;
		case UserAgent.UA_STATE_IDLE:
			if (Receiver.ccCall != null)
				mCallCard.displayMainCallStatus(ccPhone, Receiver.ccCall);
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
		if (mCallCard.mElapsedTime != null)
			mCallCard.mElapsedTime.stop();

		AetherVoice.hideCallFrame(false);
	}
}
