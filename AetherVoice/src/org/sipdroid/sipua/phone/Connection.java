package org.sipdroid.sipua.phone;

import org.sipdroid.sipua.ui.Receiver;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.provider.Contacts.People;
import android.text.TextUtils;

/*
 * Copyright (C) 2009 The Sipdroid Open Source Project
 * Copyright (C) 2006 The Android Open Source Project
 * 
 * This file is part of Sipdroid (http://www.sipdroid.org)
 * 
 * Sipdroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/**
 * {@hide}
 */
public class Connection
{
    public enum DisconnectCause {
        NOT_DISCONNECTED,   /* has not yet disconnected */
        INCOMING_MISSED,    /* an incoming call that was missed and never answered */
        NORMAL,             /* normal; remote */
        LOCAL,              /* normal; local hangup */
        BUSY,               /* outgoing call to busy line */
        CONGESTION,         /* outgoing call to congested network */
        MMI,                /* not presently used; dial() returns null */
        INVALID_NUMBER,     /* invalid dial string */
        LOST_SIGNAL,
        LIMIT_EXCEEDED,     /* eg GSM ACM limit exceeded */
        INCOMING_REJECTED,  /* an incoming call that was rejected */
        POWER_OFF,          /* radio is turned off explicitly */
        OUT_OF_SERVICE,     /* out of service */
        SIM_ERROR,          /* No SIM, SIM locked, or other SIM error */
        CALL_BARRED,        /* call was blocked by call barrring */
        FDN_BLOCKED         /* call was blocked by fixed dial number */
    }
    
    /** ACTION for publishing information about calls. */
    private static final String ACTION_CM_SIP = // .
    "de.ub0r.android.callmeter.SAVE_SIPCALL";
    /** Extra holding uri of done call. */
    private static final String EXTRA_SIP_URI = "uri";
    /** Extra holding name of provider. */
    private static final String EXTRA_SIP_PROVIDER = "provider";

    Object userData;

    /* Instance Methods */

    /** 
     * Gets address (e.g., phone number) associated with connection
     * TODO: distinguish reasons for unavailablity
     *
     * @return address or null if unavailable
     */
    String address,address2;
    public String getAddress() {
    	return address;
    }
    public String getAddress2() {
    	return address2;
    }
    public void setAddress(String a,String b) {
    	address = a;
    	address2 = b;
    }

    Call c;
    /**
     * @return Call that owns this Connection, or null if none
     */
    public Call getCall() {
    	return c;
    }
    public void setCall(Call a) {
    	c = a;
    }

     /**
     * Returns "NOT_DISCONNECTED" if not yet disconnected
     */
    public DisconnectCause getDisconnectCause() {
    	return DisconnectCause.NORMAL;
    }

   /**
     * If this Connection is connected, then it is associated with
     * a Call.
     * 
     * Returns getCall().getState() or Call.State.IDLE if not
     * connected
     */
    public Call.State getState()
    {
        Call c;

        c = getCall();

        if (c == null) { 
            return Call.State.IDLE;
        } else {
            return c.getState();
        }
    }
    
    /**
     * isAlive()
     * 
     * @return true if the connection isn't disconnected
     * (could be active, holding, ringing, dialing, etc)
     */
    public boolean
    isAlive()
    {
        return getState().isAlive();
    }

    /**
     * Returns true if Connection is connected and is INCOMING or WAITING
     */
    public boolean
    isRinging()
    {
        return getState().isRinging();
    }

    /**
     * 
     * @return the userdata set in setUserData()
     */
    public Object getUserData()
    {
        return userData;
    }

    /**
     * 
     * @param userdata user can store an any userdata in the Connection object.
     */
    public void setUserData(Object userdata)
    {
        this.userData = userdata;
    }
    
    private static Uri result;
    
    public static Uri addCall(final CallerInfo ci, final Context context, final String number,
            final boolean isPrivateNumber, final int callType, final long start, final int duration) {

		/**
		 * We put the thread here to handle the UI delay. Handling of the data
		 * is in the thread.
		 * */
    	
		new Thread(new Runnable() {

			@Override
			public void run() {
				final ContentResolver resolver = context.getContentResolver();
				String newNumber = number;
				if (TextUtils.isEmpty(number))
					if (isPrivateNumber)
						newNumber = CallerInfo.PRIVATE_NUMBER;
					else
						newNumber = CallerInfo.UNKNOWN_NUMBER;

        		final ContentValues values = new ContentValues(5);
				if (newNumber.contains("&"))
					newNumber = number.substring(0, number.indexOf("&"));

				values.put(Calls.NUMBER, newNumber);
				values.put(Calls.TYPE, Integer.valueOf(callType));
				values.put(Calls.DATE, Long.valueOf(start));
				values.put(Calls.DURATION, Long.valueOf(duration));
				values.put(Calls.NEW, Integer.valueOf(1));
				if (ci != null) {
					values.put(Calls.CACHED_NAME, ci.name);
					values.put(Calls.CACHED_NUMBER_TYPE, ci.numberType);
					values.put(Calls.CACHED_NUMBER_LABEL, ci.numberLabel);
				}

				if ((ci != null) && (ci.person_id > 0))
					People.markAsContacted(resolver, ci.person_id);
				result = resolver.insert(Calls.CONTENT_URI, values);
        		if (result != null) { // send info about call to call meter
	        	final Intent intent = new Intent(ACTION_CM_SIP);
	        	intent.putExtra(EXTRA_SIP_URI, result.toString());
	        	// TODO: add provider
	        	// intent.putExtra(EXTRA_SIP_PROVIDER, null);
	        	context.sendBroadcast(intent);
				}
        	}
		}).start();

		return result;
	}

    boolean incoming;
    
    public void setIncoming(boolean a) {
    	incoming = a;
    }
    
    public boolean isIncoming() {
    	return incoming;
    }
    
    public long date;
    
    public void log(long start)
	{
	    String number = getAddress();
        long duration = (start != 0 ? SystemClock.elapsedRealtime()-start : 0);
	    boolean isPrivateNumber = false; // TODO: need API for isPrivate()
	
	    // Set the "type" to be displayed in the call log (see constants in CallLog.Calls)
	    int callLogType;
	    if (isIncoming()) {
	        callLogType = (duration == 0 ?
	                       CallLog.Calls.MISSED_TYPE :
	                       CallLog.Calls.INCOMING_TYPE);
	    } else {
	        callLogType = CallLog.Calls.OUTGOING_TYPE;
	    }
	
	    // get the callerinfo object and then log the call with it.
	    {
	        Object o = getUserData();
	        CallerInfo ci;
	        if ((o == null) || (o instanceof CallerInfo)){
	            ci = (CallerInfo) o;
	        } else {
	            ci = ((PhoneUtils.CallerInfoToken) o).currentInfo;
	        }
	        if (callLogType == CallLog.Calls.MISSED_TYPE)
	        	Receiver.onText(Receiver.MISSED_CALL_NOTIFICATION, ci != null && ci.name != null?ci.name:number, android.R.drawable.stat_notify_missed_call, 0);
	        addCall(ci, Receiver.mContext, number, isPrivateNumber,
	                callLogType, date, (int) duration / 1000);
	    }
	}

}