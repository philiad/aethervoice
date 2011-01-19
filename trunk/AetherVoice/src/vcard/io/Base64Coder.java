package vcard.io;

import java.io.IOException;

/**
 * A Base64 Encoder/Decoder.
 * 
 * <p>
 * This class is used to encode and decode data in Base64 format as described in
 * RFC 1521.
 * 
 * <p>
 * This is "Open Source" software and released under the <a
 * href="http://www.gnu.org/licenses/lgpl.html">GNU/LGPL</a> license.<br>
 * It is provided "as is" without warranty of any kind.<br>
 * Copyright 2003: Christian d'Heureuse, Inventec Informatik AG, Switzerland.<br>
 * Home page: <a href="http://www.source-code.biz">www.source-code.biz</a><br>
 * 
 * <p>
 * Version history:<br>
 * 2003-07-22 Christian d'Heureuse (chdh): Module created.<br>
 * 2005-08-11 chdh: Lincense changed from GPL to LGPL.<br>
 * 2006-11-21 chdh:<br>
 * &nbsp; Method encode(String) renamed to encodeString(String).<br>
 * &nbsp; Method decode(String) renamed to decodeString(String).<br>
 * &nbsp; New method encode(byte[],int) added.<br>
 * &nbsp; New method decode(String) added.<br>
 * 2009-02-25 ducktayp:<br>
 * &nbsp; New method mimeEncode(Appendable, byte[], int, String) for creating
 * mime-compatible output.<br>
 * &nbsp; New method decodeInPlace(StringBuffer) for whitespace-tolerant
 * decoding without allocating memory.<br>
 */

public class Base64Coder {

	// Mapping table from 6-bit nibbles to Base64 characters.
	private static char[] map1 = new char[64];
	static {
		int i = 0;
		for (char c = 'A'; c <= 'Z'; c++)
			Base64Coder.map1[i++] = c;
		for (char c = 'a'; c <= 'z'; c++)
			Base64Coder.map1[i++] = c;
		for (char c = '0'; c <= '9'; c++)
			Base64Coder.map1[i++] = c;
		Base64Coder.map1[i++] = '+';
		Base64Coder.map1[i++] = '/';
	}

	// Mapping table from Base64 characters to 6-bit nibbles.
	private static byte[] map2 = new byte[128];
	static {
		for (int i = 0; i < Base64Coder.map2.length; i++)
			Base64Coder.map2[i] = -1;
		for (int i = 0; i < 64; i++)
			Base64Coder.map2[Base64Coder.map1[i]] = (byte) i;
	}

	/**
	 * Encodes a byte array into Base64 format. A separator is inserted after
	 * every linelength chars
	 * 
	 * @param out
	 *            an StringBuffer into which output is appended.
	 * @param in
	 *            an array containing the data bytes to be encoded.
	 * @param linelength
	 *            number of characters per line
	 * @param sep
	 *            Separator placed every linelength characters.
	 * 
	 */

	public static void mimeEncode(final Appendable out, final byte[] in,
			final int linelength, final String sep) throws IOException {
		int pos = 0;
		int bits = 0;
		int val = 0;
		for (final byte b : in) {
			val <<= 8;
			val |= (b) & 0xff;
			bits += 8;

			if (bits == 24) {
				for (int i = 0; i < 4; ++i) {
					out.append(Base64Coder.map1[(val >>> 18) & 0x3f]);
					val <<= 6;
					pos++;
					if (pos % linelength == 0)
						out.append(sep);
				}
				bits = 0;
				val = 0;
			}
		}

		int pad = (3 - (bits / 8)) % 3;

		if (bits > 0) {
			val <<= 24 - bits;
			while (bits > 0) {
				out.append(Base64Coder.map1[(val >>> 18) & 0x3f]);
				val <<= 6;
				bits -= 6;
				pos++;
				if (pos % linelength == 0)
					out.append(sep);
			}
		}
		while (pad-- > 0)
			out.append('=');
	}

	/**
	 * Decodes data in Base64 format in a StringBuffer. Whitespace and invalid
	 * characters are ignored in the Base64 encoded data;
	 * 
	 * @param inout
	 *            a StringBuffer containing the Base64 encoded data; Buffer is
	 *            modified to contain decoded data (one byte per char)
	 * @return new length of the StringBuffer
	 */
	public static int decodeInPlace(final StringBuffer inout) {
		final int n = inout.length();

		int pos = 0; // Writer position

		int val = 0; // Current byte contents
		int pad = 0; // Number of padding chars
		int bits = 0; // Number of bits decoded in val
		for (int i = 0; i < n; ++i) {
			char ch = inout.charAt(i);
			switch (ch) {
			case ' ':
			case '\t':
			case '\n':
			case '\r':
				continue; // whitespace
			case '=':
				// Padding
				++pad;
				ch = 'A';
			default:
				if (ch > 127)
					continue; // invalid char
				final int ch2 = Base64Coder.map2[ch];
				if (ch2 < 0)
					continue; // invalid char

				// Shift val and put decoded bits into val's MSB
				val <<= 6;
				val |= ch2;
				bits += 6;

				// If we have 24 bits write them out.
				if (bits == 24) {
					bits -= 8 * pad;
					pad = 0;
					while (bits > 0) {
						inout.setCharAt(pos++, (char) ((val >>> 16) & 0xff));
						val <<= 8;
						bits -= 8;
					}
					val = 0;
				}
			}
		}
		inout.setLength(pos);
		return pos;
	}

	// Dummy constructor.
	private Base64Coder() {
	}

} // end class Base64Coder
