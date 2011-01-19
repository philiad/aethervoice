/*
 * Funambol is a mobile platform developed by Funambol, Inc. 
 * Copyright (C) 2003 - 2007 Funambol, Inc.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License version 3 as published by
 * the Free Software Foundation with the addition of the following permission 
 * added to Section 15 as permitted in Section 7(a): FOR ANY PART OF THE COVERED
 * WORK IN WHICH THE COPYRIGHT IS OWNED BY FUNAMBOL, FUNAMBOL DISCLAIMS THE 
 * WARRANTY OF NON INFRINGEMENT  OF THIRD PARTY RIGHTS.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Affero General Public License 
 * along with this program; if not, see http://www.gnu.org/licenses or write to
 * the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301 USA.
 * 
 * You can contact Funambol, Inc. headquarters at 643 Bair Island Road, Suite 
 * 305, Redwood City, CA 94063, USA, or at email address info@funambol.com.
 * 
 * The interactive user interfaces in modified source and object code versions
 * of this program must display Appropriate Legal Notices, as required under
 * Section 5 of the GNU Affero General Public License version 3.
 * 
 * In accordance with Section 7(b) of the GNU Affero General Public License
 * version 3, these Appropriate Legal Notices must retain the display of the
 * "Powered by Funambol" logo. If the display of the logo is not reasonably 
 * feasible for technical reasons, the Appropriate Legal Notices must display
 * the words "Powered by Funambol".
 */

package com.funambol.util;

import com.funambol.storage.DataAccessException;

/**
 * Generic Log class
 */
public class Log {

	// ----------------------------------------------------------------
	// Constants

	/**
	 * Log level DISABLED: used to speed up applications using logging features
	 */
	public static final int DISABLED = -1;

	/**
	 * Log level ERROR: used to log error messages.
	 */
	public static final int ERROR = 0;

	/**
	 * Log level INFO: used to log information messages.
	 */
	public static final int INFO = 1;

	/**
	 * Log level DEBUG: used to log debug messages.
	 */
	public static final int DEBUG = 2;

	/**
	 * Log level TRACE: used to trace the program execution.
	 */
	public static final int TRACE = 3;

	private static final int PROFILING = -2;

	// ----------------------------------------------------------------
	// Variables
	/**
	 * The default appender is the console
	 */
	private static Appender out;

	/**
	 * The default log level is INFO
	 */
	private static int level = Log.INFO;

	/**
	 * Last time stamp used to dump profiling information
	 */
	private static long initialTimeStamp = -1;

	// -------------------------------------------------------------
	// Constructors
	/**
	 * This class is static and cannot be intantiated
	 */
	private Log() {
	}

	// ----------------------------------------------------------- Public
	// methods
	/**
	 * Initialize log file with a specific log level. With this implementation
	 * of initLog the initialization is skipped. Only a delete of log is
	 * performed.
	 * 
	 * @param object
	 *            the appender object that write log file
	 * @param level
	 *            the log level
	 */
	public static void initLog(final Appender object, final int level) {
		Log.setLogLevel(level);
		Log.out = object;
		// Delete Log invocation removed to speed up startup initialization.
		// However if needed the log store will rotate
		// deleteLog();
		if (level > Log.DISABLED)
			Log.writeLogMessage(level, "INITLOG", "---------");
	}

	/**
	 * Ititialize log file
	 * 
	 * @param object
	 *            the appender object that write log file
	 */
	public static void initLog(final Appender object) {
		Log.out = object;
		Log.out.initLogFile();
	}

	/**
	 * Delete log file
	 * 
	 */
	public static void deleteLog() {
		Log.out.deleteLogFile();
	}

	/**
	 * Accessor method to define log level:
	 * 
	 * @param newlevel
	 *            log level to be set
	 */
	public static void setLogLevel(final int newlevel) {
		Log.level = newlevel;
	}

	/**
	 * Accessor method to retrieve log level:
	 * 
	 * @return actual log level
	 */
	public static int getLogLevel() {
		return Log.level;
	}

	/**
	 * ERROR: Error message
	 * 
	 * @param msg
	 *            the message to be logged
	 */
	public static void error(final String msg) {
		Log.writeLogMessage(Log.ERROR, "ERROR", msg);
	}

	/**
	 * ERROR: Error message
	 * 
	 * @param msg
	 *            the message to be logged
	 * @param obj
	 *            the object that send error message
	 */
	public static void error(final Object obj, final String msg) {
		final String message = "[" + obj.getClass().getName() + "] " + msg;
		Log.writeLogMessage(Log.ERROR, "ERROR", message);
	}

	/**
	 * INFO: Information message
	 * 
	 * @param msg
	 *            the message to be logged
	 */
	public static void info(final String msg) {
		Log.writeLogMessage(Log.INFO, "INFO", msg);
	}

	/**
	 * INFO: Information message
	 * 
	 * @param msg
	 *            the message to be logged
	 * @param obj
	 *            the object that send log message
	 */
	public static void info(final Object obj, final String msg) {
		Log.writeLogMessage(Log.INFO, "INFO", msg);
	}

	/**
	 * DEBUG: Debug message
	 * 
	 * @param msg
	 *            the message to be logged
	 */
	public static void debug(final String msg) {
		Log.writeLogMessage(Log.DEBUG, "DEBUG", msg);
	}

	/**
	 * DEBUG: Information message
	 * 
	 * @param msg
	 *            the message to be logged
	 * @param obj
	 *            the object that send log message
	 */
	public static void debug(final Object obj, final String msg) {
		final String message = "[" + obj.getClass().getName() + "] " + msg;
		Log.writeLogMessage(Log.DEBUG, "DEBUG", message);
	}

	/**
	 * TRACE: Debugger mode
	 */
	public static void trace(final String msg) {
		Log.writeLogMessage(Log.TRACE, "TRACE", msg);
	}

	/**
	 * TRACE: Information message
	 * 
	 * @param msg
	 *            the message to be logged
	 * @param obj
	 *            the object that send log message
	 */
	public static void trace(final Object obj, final String msg) {
		final String message = "[" + obj.getClass().getName() + "] " + msg;
		Log.writeLogMessage(Log.TRACE, "TRACE", message);
	}

	/**
	 * Dump memory statistics at this point. Dump if level >= DEBUG.
	 * 
	 * @param msg
	 *            message to be logged
	 */
	public static void memoryStats(final String msg) {
		// Try to force a garbage collection, so we get the real amount of
		// available memory
		final long available = Runtime.getRuntime().freeMemory();
		Runtime.getRuntime().gc();
		Log.writeLogMessage(Log.PROFILING, "PROFILING-MEMORY", msg + ":"
				+ available + " [bytes]");
	}

	/**
	 * Dump memory statistics at this point.
	 * 
	 * @param obj
	 *            caller object
	 * @param msg
	 *            message to be logged
	 */
	public static void memoryStats(final Object obj, final String msg) {
		// Try to force a garbage collection, so we get the real amount of
		// available memory
		Runtime.getRuntime().gc();
		final long available = Runtime.getRuntime().freeMemory();
		Log.writeLogMessage(Log.PROFILING, "PROFILING-MEMORY", obj.getClass()
				.getName()
				+ "::" + msg + ":" + available + " [bytes]");
	}

	/**
	 * Dump time statistics at this point.
	 * 
	 * @param msg
	 *            message to be logged
	 */
	public static void timeStats(final String msg) {
		final long time = System.currentTimeMillis();
		if (Log.initialTimeStamp == -1) {
			Log.writeLogMessage(Log.PROFILING, "PROFILING-TIME", msg
					+ ": 0 [msec]");
			Log.initialTimeStamp = time;
		} else {
			final long currentTime = time - Log.initialTimeStamp;
			Log.writeLogMessage(Log.PROFILING, "PROFILING-TIME", msg + ": "
					+ currentTime + "[msec]");
		}
	}

	/**
	 * Dump time statistics at this point.
	 * 
	 * @param obj
	 *            caller object
	 * @param msg
	 *            message to be logged
	 */
	public static void timeStats(final Object obj, final String msg) {
		// Try to force a garbage collection, so we get the real amount of
		// available memory
		final long time = System.currentTimeMillis();
		if (Log.initialTimeStamp == -1) {
			Log.writeLogMessage(Log.PROFILING, "PROFILING-TIME", obj.getClass()
					.getName()
					+ "::" + msg + ": 0 [msec]");
			Log.initialTimeStamp = time;
		} else {
			final long currentTime = time - Log.initialTimeStamp;
			Log.writeLogMessage(Log.PROFILING, "PROFILING-TIME", obj.getClass()
					.getName()
					+ "::" + msg + ":" + currentTime + " [msec]");
		}
	}

	/**
	 * Dump time statistics at this point.
	 * 
	 * @param msg
	 *            message to be logged
	 */
	public static void stats(final String msg) {
		Log.memoryStats(msg);
		Log.timeStats(msg);
	}

	/**
	 * Dump time statistics at this point.
	 * 
	 * @param obj
	 *            caller object
	 * @param msg
	 *            message to be logged
	 */
	public static void stats(final Object obj, final String msg) {
		Log.memoryStats(obj, msg);
		Log.timeStats(obj, msg);
	}

	private static void writeLogMessage(final int msgLevel,
			final String levelMsg, final String msg) {

		if (Log.level >= msgLevel)
			try {
				if (Log.out != null)
					Log.out.writeLogMessage(levelMsg, msg);
				else {
					// System.out.print(MailDateFormatter.dateToUTC(new
					// Date()));
					// System.out.print(" [" + levelMsg + "] " );
					// System.out.println(msg);
				}
			} catch (final DataAccessException ex) {
				ex.printStackTrace();
			}
	}
}
