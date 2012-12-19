package org.sagebionetworks.logging;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.BitSet;
import java.util.Scanner;

public class LoggingEncoder {

	private static enum Coding {
		Encode,
		Decode
	}

	private static BitSet needsEncoding;
	static {
		needsEncoding = new BitSet(256);
		StringReader sr = new StringReader("%?&=");
		int chr;
		try {
			while ((chr = sr.read()) != -1) {
				needsEncoding.set(chr);
			}
		} catch (IOException e) {
		}
	}

	private static String doCoding(String value, Coding coding) {
		StringBuilder sb = new StringBuilder();
		StringReader sr = new StringReader(value);
		int chr;
		try {
			while ((chr = sr.read()) != -1) {
				if (needsEncoding.get(chr) &&
					coding == Coding.Encode) {
					doEncode(sb, chr);	
				} else if (chr == '%' &&
							coding == Coding.Decode) {
					doDecode(sb, sr);
				} else {
					sb.append(Character.toString((char)chr));
				}
			}
		} catch (IOException e) {
			// This can't happen, right?
		}
		return sb.toString();
	}

	static void doEncode(StringBuilder sb, int chr) {
		int before = sb.length();
		sb.append('%');
		sb.append(String.format("%02X", chr));
		int after = sb.length();
		if ((after - before) != 3) throw new Error(String.format("%c character didn't encode properly.", chr));
	}
	
	static void doDecode(StringBuilder sb, StringReader sr) throws IOException {
		char[] chars = new char[2];
		sr.read(chars);
		Scanner scanner = new Scanner(new String(chars));
		int charValue = scanner.nextInt(16);
		sb.append(Character.toString((char)charValue));
	}

	public static String encode(String in) {
		return doCoding(in, Coding.Encode);
	}

	public static String decode(String out) {
		return doCoding(out, Coding.Decode);
	}
}
