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
 * 
 * 2009-02-25 ducktayp: Modified to parse and format more Vcard fields, including PHOTO.
 * 						No attempt was made to preserve compatibility with previous code.
 *    					
 */

package vcard.io;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.net.Uri.Builder;
import android.provider.BaseColumns;
import android.provider.Contacts;
import android.provider.Contacts.ContactMethodsColumns;
import android.provider.Contacts.OrganizationColumns;
import android.provider.Contacts.PeopleColumns;
import android.provider.Contacts.PhonesColumns;

import com.funambol.util.Log;
import com.funambol.util.StringUtil;

/**
 * A Contact item
 */
public class AndroidParser {
	// Parse birthday in notes
	final static String BIRTHDAY_FIELD = "Birthday:";
	final static Pattern birthdayPattern = Pattern.compile("^"
			+ AndroidParser.BIRTHDAY_FIELD + ":\\s*([^;]+)(;\\s*|\\s*$)",
			Pattern.CASE_INSENSITIVE);

	// Protocol labels
	static final String[] PROTO = { "AIM", // ContactMethods.PROTOCOL_AIM = 0
			"MSN", // ContactMethods.PROTOCOL_MSN = 1
			"YAHOO", // ContactMethods.PROTOCOL_YAHOO = 2
			"SKYPE", // ContactMethods.PROTOCOL_SKYPE = 3
			"QQ", // ContactMethods.PROTOCOL_QQ = 4
			"GTALK", // ContactMethods.PROTOCOL_GOOGLE_TALK = 5
			"ICQ", // ContactMethods.PROTOCOL_ICQ = 6
			"JABBER" // ContactMethods.PROTOCOL_JABBER = 7
	};

	public static class SyncDBStatements {
		public SQLiteStatement querySyncId;
		public SQLiteStatement queryPersonId;
		public SQLiteStatement insertSyncId;
		public SQLiteStatement updateSyncId;
		public SQLiteStatement deleteSyncId;
	}

	SyncDBStatements mSyncDB;

	// Constructors------------------------------------------------
	public AndroidParser(final SyncDBStatements syncDB) {
		this.mSyncDB = syncDB;
	}

	/**
	 * Populate the contact fields from a cursor
	 */
	public void populate(final Contact contact, final Cursor peopleCur,
			final ContentResolver cResolver) {
		contact.reset();
		setPeopleFields(contact, peopleCur);
		final String personID = contact._id;

		if (mSyncDB != null && mSyncDB.querySyncId != null) {
			mSyncDB.querySyncId.bindString(1, personID);
			try {
				contact.syncid = mSyncDB.querySyncId.simpleQueryForString();
			} catch (final SQLiteDoneException e) {
				if (mSyncDB.insertSyncId != null) {
					// Create a new syncid
					contact.syncid = UUID.randomUUID().toString();

					// Write the new syncid
					mSyncDB.insertSyncId.bindString(1, personID);
					mSyncDB.insertSyncId.bindString(2, contact.syncid);
					mSyncDB.insertSyncId.executeInsert();
				}
			}
		}

		final Cursor organization = cResolver.query(
				Contacts.Organizations.CONTENT_URI, null,
				Contacts.OrganizationColumns.PERSON_ID + "=" + personID, null,
				null);

		// Set the organization fields
		if (organization.moveToFirst())
			do
				setOrganizationFields(contact, organization);
			while (organization.moveToNext());
		organization.close();

		final Cursor phones = cResolver.query(Contacts.Phones.CONTENT_URI,
				null, Contacts.Phones.PERSON_ID + "=" + personID, null, null);

		// Set all the phone numbers
		if (phones.moveToFirst())
			do
				setPhoneFields(contact, phones);
			while (phones.moveToNext());
		phones.close();

		final Cursor contactMethods = cResolver.query(
				Contacts.ContactMethods.CONTENT_URI, null,
				Contacts.ContactMethods.PERSON_ID + "=" + personID, null, null);

		// Set all the contact methods (emails, addresses, ims)
		if (contactMethods.moveToFirst())
			do
				setContactMethodsFields(contact, contactMethods);
			while (contactMethods.moveToNext());
		contactMethods.close();

		// Load a photo if one exists.
		final Cursor contactPhoto = cResolver.query(
				Contacts.Photos.CONTENT_URI, null,
				Contacts.PhotosColumns.PERSON_ID + "=" + personID, null, null);
		if (contactPhoto.moveToFirst())
			contact.photo = contactPhoto.getBlob(contactPhoto
					.getColumnIndex(Contacts.PhotosColumns.DATA));
		contactPhoto.close();
	}

	/**
	 * Add a new contact to the Content Resolver
	 * 
	 * @param key
	 *            the row number of the existing contact (if known)
	 * @param destGroup
	 *            the contact group to which a new contact will be added
	 * @param useOnlyGroup
	 *            don't add the contact to the "All contacts" group as well
	 * @param replace
	 *            replace existing contacts with the same syncid.
	 * @return The row number of the inserted column
	 */
	public long addContact(final Contact contact,
			final ContentResolver cResolver, long key,
			final List<String> destGroups, final boolean replace) {
		final ContentValues pCV = getPeopleCV(contact);

		boolean addSyncId = false;
		boolean replacing = false;
		boolean foundSyncId = false;

		if (key <= 0 && contact.syncid != null)
			if ((mSyncDB != null) && (mSyncDB.queryPersonId != null))
				try {
					mSyncDB.queryPersonId.bindString(1, contact.syncid);
					contact.setId(mSyncDB.queryPersonId.simpleQueryForString());
					key = contact.getId();
					foundSyncId = true;
				} catch (final SQLiteDoneException e) {
					// Couldn't locate syncid, we'll add it;
					// need to wait until we know what the key is, though.
					addSyncId = true;
				}

		Uri newContactUri = null;

		if (key > 0) {
			newContactUri = ContentUris.withAppendedId(
					Contacts.People.CONTENT_URI, key);
			final Cursor testit = cResolver.query(newContactUri, null, null,
					null, null);
			if (testit == null || testit.getCount() == 0) {
				// We couldn't find a person whose id matches key. It may have
				// been deleted. We'll generate
				// a new one.
				newContactUri = null;
				key = -1;
				if (foundSyncId) {
					// We'll have to delete the old association from the
					// database.
					mSyncDB.deleteSyncId.bindString(1, contact.syncid);
					mSyncDB.deleteSyncId.execute();
					// And re-add the new key.
					addSyncId = true;
				}
			}
			if (testit != null)
				testit.close();
		}
		if (newContactUri == null) {
			newContactUri = insertContentValues(cResolver,
					Contacts.People.CONTENT_URI, pCV);
			if (newContactUri == null) {
				Log.error("Error adding contact." + " (key: " + key + ")");
				return -1;
			}
			// Set the contact person id
			contact.setId(newContactUri.getLastPathSegment());
			key = contact.getId();

			// Add the new contact to the destination groups
			for (final String groupId : destGroups)
				try {
					Contacts.People.addToGroup(cResolver, key, Long
							.parseLong(groupId));
				} catch (final NumberFormatException e) {
					// Ignore
				}
		} else {
			// update existing Uri
			if (!replace)
				return -1;

			replacing = true;

			cResolver.update(newContactUri, pCV, null, null);
		}

		// We need to add the syncid to the database so
		// that we'll detect this contact if we try to import
		// it again.
		if (addSyncId && mSyncDB != null && mSyncDB.querySyncId != null
				&& mSyncDB.insertSyncId != null) {
			mSyncDB.querySyncId.bindLong(1, key);
			try {
				mSyncDB.querySyncId.simpleQueryForString();

				// The personId is already bound to an old syncid.
				// We'll replace it.
				if (mSyncDB.updateSyncId != null) {
					mSyncDB.updateSyncId.bindString(1, contact.syncid);
					mSyncDB.updateSyncId.bindLong(2, key);
					mSyncDB.updateSyncId.execute();
				}
			} catch (final SQLiteDoneException e) {
				// We can insert a new syncid without violating the uniqueness
				// constraint.
				mSyncDB.insertSyncId.bindLong(1, key);
				mSyncDB.insertSyncId.bindString(2, contact.syncid);
				mSyncDB.insertSyncId.executeInsert();
			}
		}

		/*
		 * Insert all the new ContentValues
		 */
		if (replacing) {
			// Remove existing phones
			final Uri phones = Uri.withAppendedPath(ContentUris.withAppendedId(
					Contacts.People.CONTENT_URI, key),
					Contacts.People.Phones.CONTENT_DIRECTORY);
			final String[] phoneID = { BaseColumns._ID };
			final Cursor existingPhones = cResolver.query(phones, phoneID,
					null, null, null);
			if (existingPhones != null && existingPhones.moveToFirst()) {
				final int idColumn = existingPhones
						.getColumnIndex(BaseColumns._ID);
				final List<Long> ids = new ArrayList<Long>(existingPhones
						.getCount());
				do
					ids.add(existingPhones.getLong(idColumn));
				while (existingPhones.moveToNext());
				existingPhones.close();
				for (final Long id : ids) {
					final Uri phone = ContentUris.withAppendedId(
							Contacts.Phones.CONTENT_URI, id);
					cResolver.delete(phone, null, null);
				}
			}

			// Remove existing contact methods (emails, addresses, etc.)
			final Uri methods = Uri.withAppendedPath(ContentUris
					.withAppendedId(Contacts.People.CONTENT_URI, key),
					Contacts.People.ContactMethods.CONTENT_DIRECTORY);
			final String[] methodID = { BaseColumns._ID };
			final Cursor existingMethods = cResolver.query(methods, methodID,
					null, null, null);
			if (existingMethods != null && existingMethods.moveToFirst()) {
				final int idColumn = existingMethods
						.getColumnIndex(BaseColumns._ID);
				final List<Long> ids = new ArrayList<Long>(existingMethods
						.getCount());
				do
					ids.add(existingMethods.getLong(idColumn));
				while (existingMethods.moveToNext());
				existingMethods.close();
				for (final Long id : ids) {
					final Uri method = ContentUris.withAppendedId(
							Contacts.ContactMethods.CONTENT_URI, id);
					cResolver.delete(method, null, null);
				}
			}
		}

		// Phones
		for (final Contact.RowData phone : contact.phones)
			insertContentValues(cResolver, Contacts.Phones.CONTENT_URI,
					getPhoneCV(contact, phone));

		// Organizations
		for (final Contact.OrgData org : contact.orgs)
			insertContentValues(cResolver, Contacts.Organizations.CONTENT_URI,
					getOrganizationCV(contact, org));

		final Builder builder = newContactUri.buildUpon();
		builder
				.appendEncodedPath(Contacts.ContactMethods.CONTENT_URI
						.getPath());

		// Emails
		for (final Contact.RowData email : contact.emails)
			insertContentValues(cResolver, builder.build(), getEmailCV(contact,
					email));

		// Addressess
		for (final Contact.RowData addr : contact.addrs)
			insertContentValues(cResolver, builder.build(), getAddressCV(
					contact, addr));

		// IMs
		for (final Contact.RowData im : contact.ims)
			insertContentValues(cResolver, builder.build(),
					getImCV(contact, im));

		// Photo
		if (contact.photo != null) {
			final Uri person = ContentUris.withAppendedId(
					Contacts.People.CONTENT_URI, key);
			Contacts.People.setPhotoData(cResolver, person, contact.photo);
		}

		return key;
	}

	/**
	 * Retrieve the People fields from a Cursor
	 */
	private void setPeopleFields(final Contact contact, final Cursor cur) {

		int selectedColumn;

		// Set the contact id
		selectedColumn = cur.getColumnIndex(BaseColumns._ID);
		final long nid = cur.getLong(selectedColumn);
		contact._id = String.valueOf(nid);

		//
		// Get PeopleColumns fields
		//
		selectedColumn = cur.getColumnIndex(Contacts.PeopleColumns.NAME);
		contact.displayName = cur.getString(selectedColumn);

		contact.parseDisplayName(contact.displayName);

		selectedColumn = cur.getColumnIndex(PeopleColumns.NOTES);
		contact.notes = cur.getString(selectedColumn);
		if (contact.notes != null) {
			final Matcher ppm = AndroidParser.birthdayPattern
					.matcher(contact.notes);

			if (ppm.find()) {
				contact.birthday = ppm.group(1);
				contact.notes = ppm.replaceFirst("");
			}
		}
	}

	/**
	 * Retrieve the organization fields from a Cursor
	 */
	private void setOrganizationFields(final Contact contact, final Cursor cur) {

		int selectedColumn;

		//
		// Get Organizations fields
		//
		selectedColumn = cur
				.getColumnIndex(Contacts.OrganizationColumns.COMPANY);
		final String company = cur.getString(selectedColumn);

		selectedColumn = cur.getColumnIndex(Contacts.OrganizationColumns.TITLE);
		final String title = cur.getString(selectedColumn);

		selectedColumn = cur.getColumnIndex(Contacts.OrganizationColumns.TYPE);
		final int orgType = cur.getInt(selectedColumn);

		String customLabel = null;
		if (orgType == Contacts.ContactMethodsColumns.TYPE_CUSTOM) {
			selectedColumn = cur
					.getColumnIndex(Contacts.ContactMethodsColumns.LABEL);
			customLabel = cur.getString(selectedColumn);
		}

		contact.orgs.add(new Contact.OrgData(orgType, title, company,
				customLabel));
	}

	/**
	 * Retrieve the Phone fields from a Cursor
	 */
	private void setPhoneFields(final Contact contact, final Cursor cur) {

		int selectedColumn;
		int selectedColumnType;
		int preferredColumn;
		int phoneType;
		String customLabel = null;

		//
		// Get PhonesColums fields
		//
		selectedColumn = cur.getColumnIndex(Contacts.PhonesColumns.NUMBER);
		selectedColumnType = cur.getColumnIndex(Contacts.PhonesColumns.TYPE);
		preferredColumn = cur.getColumnIndex(Contacts.PhonesColumns.ISPRIMARY);
		phoneType = cur.getInt(selectedColumnType);
		final String phone = cur.getString(selectedColumn);
		final boolean preferred = cur.getInt(preferredColumn) != 0;
		if (phoneType == Contacts.PhonesColumns.TYPE_CUSTOM)
			customLabel = cur.getString(cur
					.getColumnIndex(Contacts.PhonesColumns.LABEL));

		contact.phones.add(new Contact.RowData(phoneType, phone, preferred,
				customLabel));
	}

	/**
	 * Retrieve the email fields from a Cursor
	 */
	private void setContactMethodsFields(final Contact contact, final Cursor cur) {

		int selectedColumn;
		int selectedColumnType;
		int selectedColumnKind;
		int selectedColumnPrimary;
		int selectedColumnLabel;

		int methodType;
		int kind;
		String customLabel = null;
		String auxData = null;

		//
		// Get ContactsMethodsColums fields
		//
		selectedColumn = cur
				.getColumnIndex(Contacts.ContactMethodsColumns.DATA);
		selectedColumnType = cur
				.getColumnIndex(Contacts.ContactMethodsColumns.TYPE);
		selectedColumnKind = cur
				.getColumnIndex(Contacts.ContactMethodsColumns.KIND);
		selectedColumnPrimary = cur
				.getColumnIndex(Contacts.ContactMethodsColumns.ISPRIMARY);

		kind = cur.getInt(selectedColumnKind);

		methodType = cur.getInt(selectedColumnType);
		final String methodData = cur.getString(selectedColumn);
		final boolean preferred = cur.getInt(selectedColumnPrimary) != 0;
		if (methodType == Contacts.ContactMethodsColumns.TYPE_CUSTOM) {
			selectedColumnLabel = cur
					.getColumnIndex(Contacts.ContactMethodsColumns.LABEL);
			customLabel = cur.getString(selectedColumnLabel);
		}

		switch (kind) {
		case Contacts.KIND_EMAIL:
			contact.emails.add(new Contact.RowData(methodType, methodData,
					preferred, customLabel));
			break;
		case Contacts.KIND_POSTAL:
			contact.addrs.add(new Contact.RowData(methodType, methodData,
					preferred, customLabel));
			break;
		case Contacts.KIND_IM:
			final Contact.RowData newRow = new Contact.RowData(methodType,
					methodData, preferred, customLabel);

			selectedColumn = cur
					.getColumnIndex(Contacts.ContactMethodsColumns.AUX_DATA);
			auxData = cur.getString(selectedColumn);

			if (auxData != null) {
				final String[] auxFields = StringUtil.split(auxData, ":");
				if (auxFields.length > 1) {
					if (auxFields[0].equalsIgnoreCase("pre")) {
						int protval = 0;
						try {
							protval = Integer.decode(auxFields[1]);
						} catch (final NumberFormatException e) {
							// Do nothing; protval = 0
						}
						if (protval < 0
								|| protval >= AndroidParser.PROTO.length)
							protval = 0;
						newRow.auxData = AndroidParser.PROTO[protval];
					} else if (auxFields[0].equalsIgnoreCase("custom"))
						newRow.auxData = auxFields[1];
				} else
					newRow.auxData = auxData;
			}

			contact.ims.add(newRow);
			break;
		}
	}

	private ContentValues getPeopleCV(final Contact contact) {
		final ContentValues cv = new ContentValues();

		final StringBuffer fullname = new StringBuffer();
		if (contact.displayName != null)
			fullname.append(contact.displayName);
		else {
			if (contact.preName != null)
				fullname.append(contact.preName);
			if (contact.firstName != null) {
				if (fullname.length() > 0)
					fullname.append(" ");
				fullname.append(contact.firstName);
			}
			if (contact.midNames != null) {
				if (fullname.length() > 0)
					fullname.append(" ");
				fullname.append(contact.midNames);
			}
			if (contact.lastName != null) {
				if (fullname.length() > 0)
					fullname.append(" ");
				fullname.append(contact.lastName);
			}
			if (contact.sufName != null) {
				if (fullname.length() > 0)
					fullname.append(" ");
				fullname.append(contact.sufName);
			}
		}

		// Use company name if only the company is given.
		if (fullname.length() == 0 && contact.orgs.size() > 0
				&& contact.orgs.get(0).company != null)
			fullname.append(contact.orgs.get(0).company);

		cv.put(PeopleColumns.NAME, fullname.toString());

		if (!StringUtil.isNullOrEmpty(contact._id))
			cv.put(BaseColumns._ID, contact._id);

		final StringBuffer allnotes = new StringBuffer();
		if (contact.birthday != null)
			allnotes.append(AndroidParser.BIRTHDAY_FIELD).append(" ").append(
					contact.birthday);
		if (contact.notes != null) {
			if (contact.birthday != null)
				allnotes.append(";\n");
			allnotes.append(contact.notes);
		}

		if (allnotes.length() > 0)
			cv.put(PeopleColumns.NOTES, allnotes.toString());

		return cv;
	}

	private ContentValues getOrganizationCV(final Contact contact,
			final Contact.OrgData org) {

		if (StringUtil.isNullOrEmpty(org.company)
				&& StringUtil.isNullOrEmpty(org.title))
			return null;
		final ContentValues cv = new ContentValues();

		cv.put(OrganizationColumns.COMPANY, org.company);
		cv.put(OrganizationColumns.TITLE, org.title);
		cv.put(OrganizationColumns.TYPE, org.type);
		cv.put(OrganizationColumns.PERSON_ID, contact._id);
		if (org.customLabel != null)
			cv.put(OrganizationColumns.LABEL, org.customLabel);

		return cv;
	}

	private ContentValues getPhoneCV(final Contact contact,
			final Contact.RowData data) {
		final ContentValues cv = new ContentValues();

		cv.put(PhonesColumns.NUMBER, data.data);
		cv.put(PhonesColumns.TYPE, data.type);
		cv.put(PhonesColumns.ISPRIMARY, data.preferred ? 1 : 0);
		cv.put(Contacts.Phones.PERSON_ID, contact._id);
		if (data.customLabel != null)
			cv.put(PhonesColumns.LABEL, data.customLabel);

		return cv;
	}

	private ContentValues getEmailCV(final Contact contact,
			final Contact.RowData data) {
		final ContentValues cv = new ContentValues();

		cv.put(ContactMethodsColumns.DATA, data.data);
		cv.put(ContactMethodsColumns.TYPE, data.type);
		cv.put(ContactMethodsColumns.KIND, Contacts.KIND_EMAIL);
		cv.put(ContactMethodsColumns.ISPRIMARY, data.preferred ? 1 : 0);
		cv.put(Contacts.ContactMethods.PERSON_ID, contact._id);
		if (data.customLabel != null)
			cv.put(ContactMethodsColumns.LABEL, data.customLabel);

		return cv;
	}

	private ContentValues getAddressCV(final Contact contact,
			final Contact.RowData data) {
		final ContentValues cv = new ContentValues();

		cv.put(ContactMethodsColumns.DATA, data.data);
		cv.put(ContactMethodsColumns.TYPE, data.type);
		cv.put(ContactMethodsColumns.KIND, Contacts.KIND_POSTAL);
		cv.put(ContactMethodsColumns.ISPRIMARY, data.preferred ? 1 : 0);
		cv.put(Contacts.ContactMethods.PERSON_ID, contact._id);
		if (data.customLabel != null)
			cv.put(ContactMethodsColumns.LABEL, data.customLabel);

		return cv;
	}

	private ContentValues getImCV(final Contact contact,
			final Contact.RowData data) {
		final ContentValues cv = new ContentValues();

		cv.put(ContactMethodsColumns.DATA, data.data);
		cv.put(ContactMethodsColumns.TYPE, data.type);
		cv.put(ContactMethodsColumns.KIND, Contacts.KIND_IM);
		cv.put(ContactMethodsColumns.ISPRIMARY, data.preferred ? 1 : 0);
		cv.put(Contacts.ContactMethods.PERSON_ID, contact._id);
		if (data.customLabel != null)
			cv.put(ContactMethodsColumns.LABEL, data.customLabel);

		if (data.auxData != null) {
			int protoNum = -1;
			for (int i = 0; i < AndroidParser.PROTO.length; ++i)
				if (data.auxData.equalsIgnoreCase(AndroidParser.PROTO[i])) {
					protoNum = i;
					break;
				}
			if (protoNum >= 0)
				cv.put(ContactMethodsColumns.AUX_DATA, "pre:" + protoNum);
			else
				cv
						.put(ContactMethodsColumns.AUX_DATA, "custom:"
								+ data.auxData);
		}

		return cv;
	}

	/**
	 * Insert a new ContentValues raw into the Android ContentProvider
	 */
	private Uri insertContentValues(final ContentResolver cResolver,
			final Uri uri, final ContentValues cv) {
		if (cv != null)
			return cResolver.insert(uri, cv);
		return null;
	}
}
