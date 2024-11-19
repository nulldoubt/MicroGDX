package com.badlogic.gdx.utils;

import java.text.MessageFormat;
import java.util.Locale;

public class TextFormatter {
	
	private MessageFormat messageFormat;
	private final StringBuilder buffer;
	
	public TextFormatter(final Locale locale, final boolean messageFormat) {
		buffer = new StringBuilder();
		if (messageFormat)
			this.messageFormat = new MessageFormat("", locale);
	}
	
	public String format(final String pattern, final Object... args) {
		if (messageFormat != null) {
			messageFormat.applyPattern(replaceEscapeChars(pattern));
			return messageFormat.format(args);
		}
		return simpleFormat(pattern, args);
	}
	
	private String replaceEscapeChars(final String pattern) {
		buffer.setLength(0);
		boolean changed = false;
		int len = pattern.length();
		for (int i = 0; i < len; i++) {
			char ch = pattern.charAt(i);
			if (ch == '\'') {
				changed = true;
				buffer.append("''");
			} else if (ch == '{') {
				int j = i + 1;
				while (j < len && pattern.charAt(j) == '{')
					j++;
				int escaped = (j - i) / 2;
				if (escaped > 0) {
					changed = true;
					buffer.append('\'');
					do {
						buffer.append('{');
					} while ((--escaped) > 0);
					buffer.append('\'');
				}
				if ((j - i) % 2 != 0)
					buffer.append('{');
				i = j - 1;
			} else {
				buffer.append(ch);
			}
		}
		return changed ? buffer.toString() : pattern;
	}
	
	private String simpleFormat(final String pattern, final Object... args) {
		buffer.setLength(0);
		boolean changed = false;
		int placeholder = -1;
		int patternLength = pattern.length();
		for (int i = 0; i < patternLength; ++i) {
			char ch = pattern.charAt(i);
			if (placeholder < 0) { // processing constant part
				if (ch == '{') {
					changed = true;
					if (i + 1 < patternLength && pattern.charAt(i + 1) == '{') {
						buffer.append(ch); // handle escaped '{'
						++i;
					} else {
						placeholder = 0; // switch to placeholder part
					}
				} else {
					buffer.append(ch);
				}
			} else { // processing placeholder part
				if (ch == '}') {
					if (placeholder >= args.length)
						throw new IllegalArgumentException("Argument index out of bounds: " + placeholder);
					if (pattern.charAt(i - 1) == '{')
						throw new IllegalArgumentException("Missing argument index after a left curly brace");
					if (args[placeholder] == null)
						buffer.append("null"); // append null argument
					else
						buffer.append(args[placeholder].toString()); // append actual argument
					placeholder = -1; // switch to constant part
				} else {
					if (ch < '0' || ch > '9')
						throw new IllegalArgumentException("Unexpected '" + ch + "' while parsing argument index");
					placeholder = placeholder * 10 + (ch - '0');
				}
			}
		}
		if (placeholder >= 0)
			throw new IllegalArgumentException("Unmatched braces in the pattern.");
		
		return changed ? buffer.toString() : pattern;
	}
	
}
