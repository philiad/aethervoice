package com.neugent.aethervoice.ui;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import vcard.io.AndroidParser;
import vcard.io.Contact;
import vcard.io.VCardParser;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.BaseColumns;
import android.provider.Contacts;
import android.provider.Contacts.GroupsColumns;
import android.text.TextUtils;
import android.widget.Toast;

/**
 * @author Amando Jose Quinto II The class for importing and exporting vcards.
 */
public class ImportExportContacts {
	/** The path for the vcard storage. **/
	private static final String FILENAME = Environment
			.getExternalStorageDirectory()
			+ "/backup.vcf";

	/** The database name to for the contacts to be sync. **/
	private static final String DATABASE_NAME = "syncdata.db";

	/** The database table name to for the contacts to be sync. **/
	private static final String SYNCDATA_TABLE_NAME = "sync";

	/** The database name to for the contacts to be sync. **/
	private static final String PERSONID = "person";
	private static final String SYNCID = "syncid";
	private static final String GROUP_ID_QUERY = GroupsColumns.NAME + "=?";

	private static final int DATABASE_VERSION = 1;

	private static final int PROGRESS_EXPORT_SHOW = 1;
	private static final int PROGRESS_IMPORT_SHOW = 2;
	private static final int PROGRESS_EXIT = 3;
	private static ProgressDialog pDialog;

	@SuppressWarnings("deprecation")
	public static void ExportContacts(final Context context) {
		final Message msg = new Message();
		msg.what = ImportExportContacts.PROGRESS_EXPORT_SHOW;
		msg.obj = context;
		ImportExportContacts.mHandler.sendMessage(msg);

		// Log.i("Export Contacts",
		// "STARTING THE EXPORTING CONTACTS TO "+FILENAME);
		final DatabaseHelper mOpenHelper = new DatabaseHelper(context);
		// java.io.File exportFile = new java.io.File(FILENAME);

		// if (exportFile.exists()) {
		// showDialog(DIALOG_CONFIRM_OVERWRITE);
		// return;
		// }

		try {
			final BufferedWriter vcfBuffer = new BufferedWriter(new FileWriter(
					ImportExportContacts.FILENAME));

			final ContentResolver cResolver = context.getContentResolver();
			final Cursor allContacts = cResolver.query(
					Contacts.People.CONTENT_URI, null, null, null, null);
			if (allContacts == null || !allContacts.moveToFirst()) {
				// app.updateStatus("No contacts found");
				allContacts.close();
				return;
			}

			// final long maxlen = allContacts.getCount();

			final TreeSet<String> srcGroupIds;

			// if (srcGroups == null) {
			srcGroupIds = null;
			// } else {
			// srcGroupIds = new TreeSet<String>(srcGroups);
			// }

			// Start lengthy operation in a background thread
			new Thread(new Runnable() {
				public void run() {
					long exportStatus = 0;

					// synchronized (syncMonitor) {
					// mAction = Action.EXPORT;
					// syncFileName = fileName;
					// }
					// showNotification();

					final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
					final AndroidParser aParser = new AndroidParser(
							ImportExportContacts.getStatements(db));
					final VCardParser vParser = new VCardParser();
					final Contact parseContact = new Contact();

					final int personIdCol = allContacts
							.getColumnIndex(BaseColumns._ID);
					try {
						boolean hasNext = true;
						do {
							if (srcGroupIds == null
									|| srcGroupIds.contains(allContacts
											.getString(personIdCol))) {
								// Either we're looking at all contacts
								// (srcGroupId == null) or this contact is in a
								// src Group
								aParser.populate(parseContact, allContacts,
										cResolver);
								vParser.writeVCard(parseContact, vcfBuffer);
							}
							++exportStatus;

							// Update the progress bar
							// app.updateProgress((int) (100 * exportStatus /
							// maxlen));

							hasNext = allContacts.moveToNext();
						} while (hasNext);
						vcfBuffer.close();
						db.close();
						// app.updateProgress(100);
						// synchronized (syncMonitor) {
						// mAction = Action.IDLE;
						// showNotification();
						// }
						// stopSelf();
					} catch (final IOException e) {
						e.printStackTrace();
						ImportExportContacts.mHandler
								.sendEmptyMessage(ImportExportContacts.PROGRESS_EXIT);
						Toast.makeText(context, "Export Failed", Toast.LENGTH_SHORT).show(); // TODO put to
															// strings
						// app.updateStatus("Write error: " + e.getMessage());
					}
					ImportExportContacts.mHandler.sendEmptyMessage(ImportExportContacts.PROGRESS_EXIT);
				}
			}).start();
		} catch (final IOException e) {
			e.printStackTrace();
			ImportExportContacts.mHandler
					.sendEmptyMessage(ImportExportContacts.PROGRESS_EXIT);
			Toast.makeText(context, "Export Failed", Toast.LENGTH_SHORT).show(); 
			// app.updateStatus("Error opening file: " + e.getMessage());
		}

	}

	@SuppressWarnings("deprecation")
	public static void ImportContacts(final Context context) {
		final Message msg = new Message();
		msg.what = ImportExportContacts.PROGRESS_IMPORT_SHOW;
		msg.obj = context;
		ImportExportContacts.mHandler.sendMessage(msg);

		final DatabaseHelper mOpenHelper = new DatabaseHelper(context);
		final String contactGroupStr = Long.toString(ImportExportContacts
				.getGroupId(context.getContentResolver(),
						Contacts.Groups.GROUP_MY_CONTACTS));
		final List<String> destGroups = Arrays.asList(TextUtils.split(
				contactGroupStr, ","));

		try {
			// File vcfFile = new File(FILENAME);

			final BufferedReader vcfBuffer = new BufferedReader(new FileReader(
					ImportExportContacts.FILENAME));

			// final long maxlen = vcfFile.length(); // for progress bar...

			new Thread(new Runnable() {
				public void run() {
					long importStatus = 0;

					// for service
					// synchronized (syncMonitor) {
					// mAction = Action.IMPORT;
					// syncFileName = fileName;
					// }
					// showNotification();

					final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
					final AndroidParser aParser = new AndroidParser(
							ImportExportContacts.getStatements(db));
					final VCardParser vParser = new VCardParser();
					final Contact parseContact = new Contact();
					final ContentResolver cResolver = context
							.getContentResolver();

					try {
						long ret = 0;
						do {
							ret = vParser.parseVCard(parseContact, vcfBuffer);
							if (ret >= 0) {
								aParser.addContact(parseContact, cResolver, 0,
										destGroups, false);
								importStatus += vParser.getParseLen(); // for
																		// progress
																		// bar

								// // Update the progress bar
								// app.updateProgress((int) (100 * importStatus
								// / maxlen));
							}
						} while (ret > 0);

						db.close();
						// app.updateProgress(100);
						// synchronized (syncMonitor) {
						// mAction = Action.IDLE;
						// showNotification();
						// }
						// stopSelf();
					} catch (final IOException e) {
						e.printStackTrace();
						ImportExportContacts.mHandler
								.sendEmptyMessage(ImportExportContacts.PROGRESS_EXIT);
						Toast.makeText(context, "import failed",
								Toast.LENGTH_SHORT).show(); // TODO put to
															// strings
						// app.updateStatus("IO error: " + e.getMessage());
					}
					ImportExportContacts.mHandler
							.sendEmptyMessage(ImportExportContacts.PROGRESS_EXIT);
				}
			}).start();
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
			ImportExportContacts.mHandler
					.sendEmptyMessage(ImportExportContacts.PROGRESS_EXIT);
			Toast.makeText(context, "Import Failed", Toast.LENGTH_SHORT).show(); // TODO
																					// put
																					// to
																					// strings
		}

	}

	public static Handler mHandler = new Handler() {
		@Override
		public void handleMessage(final android.os.Message msg) {
			switch (msg.what) {
			case PROGRESS_EXPORT_SHOW:
				ImportExportContacts.pDialog = new ProgressDialog(
						(Context) msg.obj);
				ImportExportContacts.pDialog
						.setMessage("Exporting contacts... ");
				ImportExportContacts.pDialog.show();
				break;
			case PROGRESS_IMPORT_SHOW:
				ImportExportContacts.pDialog = new ProgressDialog(
						(Context) msg.obj);
				ImportExportContacts.pDialog
						.setMessage("Importing contacts... ");
				ImportExportContacts.pDialog.show();
				break;
			case PROGRESS_EXIT:
				ImportExportContacts.pDialog.dismiss();
				break;
			}
		};
	};

	private static long getGroupId(final ContentResolver cResolver,
			final String groupName) {
		final Cursor cur = cResolver.query(Contacts.Groups.CONTENT_URI, null,
				ImportExportContacts.GROUP_ID_QUERY,
				new String[] { groupName }, null);
		long groupId = -1;
		if (cur != null) {
			if (cur.moveToFirst())
				groupId = cur.getLong(cur.getColumnIndex(BaseColumns._ID));
			cur.close();
		}
		return groupId;
	}

	private static AndroidParser.SyncDBStatements getStatements(
			final SQLiteDatabase db) {
		final AndroidParser.SyncDBStatements syncDB = new AndroidParser.SyncDBStatements();
		syncDB.querySyncId = db.compileStatement("SELECT "
				+ ImportExportContacts.SYNCID + " FROM "
				+ ImportExportContacts.SYNCDATA_TABLE_NAME + " WHERE "
				+ ImportExportContacts.PERSONID + "=?");
		syncDB.queryPersonId = db.compileStatement("SELECT "
				+ ImportExportContacts.PERSONID + " FROM "
				+ ImportExportContacts.SYNCDATA_TABLE_NAME + " WHERE "
				+ ImportExportContacts.SYNCID + "=?");
		syncDB.insertSyncId = db.compileStatement("INSERT INTO  "
				+ ImportExportContacts.SYNCDATA_TABLE_NAME + " ("
				+ ImportExportContacts.PERSONID + ","
				+ ImportExportContacts.SYNCID + ") VALUES (?,?)");
		syncDB.updateSyncId = db.compileStatement("UPDATE "
				+ ImportExportContacts.SYNCDATA_TABLE_NAME + " SET "
				+ ImportExportContacts.SYNCID + "=? WHERE "
				+ ImportExportContacts.PERSONID + "=?");
		syncDB.deleteSyncId = db.compileStatement("DELETE FROM "
				+ ImportExportContacts.SYNCDATA_TABLE_NAME + " WHERE "
				+ ImportExportContacts.SYNCID + "=?");
		return syncDB;
	}

	/**
	 * This class helps open, create, and upgrade the database file.
	 */
	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(final Context context) {
			super(context, ImportExportContacts.DATABASE_NAME, null,
					ImportExportContacts.DATABASE_VERSION);
		}

		@Override
		public void onCreate(final SQLiteDatabase db) {
			db.execSQL("CREATE TABLE "
					+ ImportExportContacts.SYNCDATA_TABLE_NAME + " ("
					+ ImportExportContacts.PERSONID + " INTEGER PRIMARY KEY,"
					+ ImportExportContacts.SYNCID + " TEXT UNIQUE" + ");");
		}

		@Override
		public void onUpgrade(final SQLiteDatabase db, final int oldVersion,
				final int newVersion) {
			// No need to do anything --- this is version 1

		}
	}

}
