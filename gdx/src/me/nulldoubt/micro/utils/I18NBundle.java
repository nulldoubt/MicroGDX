package me.nulldoubt.micro.utils;

import me.nulldoubt.micro.exceptions.MicroRuntimeException;
import me.nulldoubt.micro.files.FileHandle;
import me.nulldoubt.micro.utils.collections.ObjectMap;
import me.nulldoubt.micro.utils.strings.StringBuilder;
import me.nulldoubt.micro.utils.strings.TextFormatter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

public class I18NBundle {
	
	private static final String DEFAULT_ENCODING = "UTF-8";
	
	private static final Locale ROOT_LOCALE = Locale.ROOT;
	
	private static boolean simpleFormatter = false;
	private static boolean exceptionOnMissingKey = true;
	
	private I18NBundle parent;
	
	private Locale locale;
	
	private ObjectMap<String, String> properties;
	
	private TextFormatter formatter;
	
	public static boolean getSimpleFormatter() {
		return simpleFormatter;
	}
	
	public static void setSimpleFormatter(boolean enabled) {
		simpleFormatter = enabled;
	}
	
	public static boolean getExceptionOnMissingKey() {
		return exceptionOnMissingKey;
	}
	
	public static void setExceptionOnMissingKey(boolean enabled) {
		exceptionOnMissingKey = enabled;
	}
	
	public static I18NBundle createBundle(FileHandle baseFileHandle) {
		return createBundleImpl(baseFileHandle, Locale.getDefault(), DEFAULT_ENCODING);
	}
	
	public static I18NBundle createBundle(FileHandle baseFileHandle, Locale locale) {
		return createBundleImpl(baseFileHandle, locale, DEFAULT_ENCODING);
	}
	
	public static I18NBundle createBundle(FileHandle baseFileHandle, String encoding) {
		return createBundleImpl(baseFileHandle, Locale.getDefault(), encoding);
	}
	
	public static I18NBundle createBundle(FileHandle baseFileHandle, Locale locale, String encoding) {
		return createBundleImpl(baseFileHandle, locale, encoding);
	}
	
	private static I18NBundle createBundleImpl(FileHandle baseFileHandle, Locale locale, String encoding) {
		if (baseFileHandle == null || locale == null || encoding == null)
			throw new NullPointerException();
		
		I18NBundle bundle;
		I18NBundle baseBundle = null;
		Locale targetLocale = locale;
		do {
			List<Locale> candidateLocales = getCandidateLocales(targetLocale);
			bundle = loadBundleChain(baseFileHandle, encoding, candidateLocales, 0, baseBundle);
			if (bundle != null) {
				Locale bundleLocale = bundle.getLocale();
				if (!bundleLocale.equals(ROOT_LOCALE) || bundleLocale.equals(locale))
					break;
				if (candidateLocales.size() == 1 && bundleLocale.equals(candidateLocales.get(0)))
					break;
				if (baseBundle == null)
					baseBundle = bundle;
			}
			
			targetLocale = getFallbackLocale(targetLocale);
			
		} while (targetLocale != null);
		
		if (bundle == null) {
			if (baseBundle == null)
				throw new MissingResourceException("Can't find bundle for base file handle " + baseFileHandle.path() + ", locale " + locale, baseFileHandle + "_" + locale, "");
			bundle = baseBundle;
		}
		
		return bundle;
	}
	
	private static List<Locale> getCandidateLocales(Locale locale) {
		
		final String language = locale.getLanguage();
		final String country = locale.getCountry();
		final String variant = locale.getVariant();
		final List<Locale> locales = new ArrayList<>(4);
		
		if (!variant.isEmpty())
			locales.add(locale);
		if (!country.isEmpty())
			locales.add(locales.isEmpty() ? locale : Locale.of(language, country));
		if (!language.isEmpty())
			locales.add(locales.isEmpty() ? locale : Locale.of(language));
		
		locales.add(ROOT_LOCALE);
		return locales;
	}
	
	private static Locale getFallbackLocale(Locale locale) {
		Locale defaultLocale = Locale.getDefault();
		return locale.equals(defaultLocale) ? null : defaultLocale;
	}
	
	private static I18NBundle loadBundleChain(FileHandle baseFileHandle, String encoding, List<Locale> candidateLocales, int candidateIndex, I18NBundle baseBundle) {
		Locale targetLocale = candidateLocales.get(candidateIndex);
		I18NBundle parent = null;
		if (candidateIndex != candidateLocales.size() - 1)
			parent = loadBundleChain(baseFileHandle, encoding, candidateLocales, candidateIndex + 1, baseBundle);
		else if (baseBundle != null && targetLocale.equals(ROOT_LOCALE))
			return baseBundle;
		
		// Load the bundle
		I18NBundle bundle = loadBundle(baseFileHandle, encoding, targetLocale);
		if (bundle != null) {
			bundle.parent = parent;
			return bundle;
		}
		
		return parent;
	}
	
	// Tries to load the bundle for the given locale.
	private static I18NBundle loadBundle(FileHandle baseFileHandle, String encoding, Locale targetLocale) {
		I18NBundle bundle = null;
		Reader reader = null;
		try {
			FileHandle fileHandle = toFileHandle(baseFileHandle, targetLocale);
			if (checkFileExistence(fileHandle)) {
				// Instantiate the bundle
				bundle = new I18NBundle();
				
				// Load bundle properties from the stream with the specified encoding
				reader = fileHandle.reader(encoding);
				bundle.load(reader);
			}
		} catch (IOException e) {
			throw new MicroRuntimeException(e);
		} finally {
			Streams.closeQuietly(reader);
		}
		if (bundle != null)
			bundle.setLocale(targetLocale);
		
		return bundle;
	}
	
	private static boolean checkFileExistence(FileHandle fh) {
		try {
			fh.read().close();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	private void load(Reader reader) throws IOException {
		if (reader == null)
			throw new NullPointerException("Reader cannot be null");
		properties = new ObjectMap<>();
		int mode = 0, unicode = 0, count = 0;
		char nextChar;
		char[] buf = new char[40];
		int offset = 0, keyLength = -1, intVal;
		boolean firstChar = true;
		
		BufferedReader br = new BufferedReader(reader);
		
		while (true) {
			intVal = br.read();
			if (intVal == -1) {
				break;
			}
			nextChar = (char) intVal;
			
			if (offset == buf.length) {
				char[] newBuf = new char[buf.length * 2];
				System.arraycopy(buf, 0, newBuf, 0, offset);
				buf = newBuf;
			}
			if (mode == 2) {
				int digit = Character.digit(nextChar, 16);
				if (digit >= 0) {
					unicode = (unicode << 4) + digit;
					if (++count < 4) {
						continue;
					}
				} else if (count <= 4) {
					throw new IllegalArgumentException("Invalid Unicode sequence: illegal character");
				}
				mode = 0;
				buf[offset++] = (char) unicode;
				if (nextChar != '\n') {
					continue;
				}
			}
			if (mode == 1) {
				mode = 0;
				switch (nextChar) {
					case '\r':
						mode = 3; // Look for a following \n
						continue;
					case '\n':
						mode = 5; // Ignore whitespace on the next line
						continue;
					case 'b':
						nextChar = '\b';
						break;
					case 'f':
						nextChar = '\f';
						break;
					case 'n':
						nextChar = '\n';
						break;
					case 'r':
						nextChar = '\r';
						break;
					case 't':
						nextChar = '\t';
						break;
					case 'u':
						mode = 2;
						unicode = count = 0;
						continue;
				}
			} else {
				switch (nextChar) {
					case '#':
					case '!':
						if (firstChar) {
							while (true) {
								intVal = br.read();
								if (intVal == -1) {
									break;
								}
								nextChar = (char) intVal;
								if (nextChar == '\r' || nextChar == '\n') {
									break;
								}
							}
							continue;
						}
						break;
					case '\n':
						if (mode == 3) { // Part of a \r\n sequence
							mode = 5; // Ignore whitespace on the next line
							continue;
						}
						// fall into the next case
					case '\r':
						mode = 0;
						firstChar = true;
						if (offset > 0 || (offset == 0 && keyLength == 0)) {
							if (keyLength == -1) {
								keyLength = offset;
							}
							String temp = new String(buf, 0, offset);
							properties.put(temp.substring(0, keyLength), temp.substring(keyLength));
						}
						keyLength = -1;
						offset = 0;
						continue;
					case '\\':
						if (mode == 4) {
							keyLength = offset;
						}
						mode = 1;
						continue;
					case ':':
					case '=':
						if (keyLength == -1) { // if parsing the key
							mode = 0;
							keyLength = offset;
							continue;
						}
						break;
				}
				// if (Character.isWhitespace(nextChar)) { <-- not supported by GWT; replaced with isSpace.
				if (Character.isWhitespace(nextChar)) {
					if (mode == 3) {
						mode = 5;
					}
					// if key length == 0 or value length == 0
					if (offset == 0 || offset == keyLength || mode == 5) {
						continue;
					}
					if (keyLength == -1) { // if parsing the key
						mode = 4;
						continue;
					}
				}
				if (mode == 5 || mode == 3) {
					mode = 0;
				}
			}
			firstChar = false;
			if (mode == 4) {
				keyLength = offset;
				mode = 0;
			}
			buf[offset++] = nextChar;
		}
		
		if (mode == 2 && count <= 4)
			throw new IllegalArgumentException("Invalid Unicode sequence: expected format \\uxxxx");
		
		if (keyLength == -1 && offset > 0)
			keyLength = offset;
		
		if (keyLength >= 0) {
			String temp = new String(buf, 0, offset);
			String key = temp.substring(0, keyLength);
			String value = temp.substring(keyLength);
			if (mode == 1)
				value += "\u0000";
			properties.put(key, value);
		}
	}
	
	private static FileHandle toFileHandle(FileHandle baseFileHandle, Locale locale) {
		StringBuilder sb = new StringBuilder(baseFileHandle.name());
		if (!locale.equals(ROOT_LOCALE)) {
			String language = locale.getLanguage();
			String country = locale.getCountry();
			String variant = locale.getVariant();
			boolean emptyLanguage = language.isEmpty();
			boolean emptyCountry = country.isEmpty();
			boolean emptyVariant = variant.isEmpty();
			
			if (!(emptyLanguage && emptyCountry && emptyVariant)) {
				sb.append('_');
				if (!emptyVariant)
					sb.append(language).append('_').append(country).append('_').append(variant);
				else if (!emptyCountry)
					sb.append(language).append('_').append(country);
				else
					sb.append(language);
			}
		}
		return baseFileHandle.sibling(sb.append(".properties").toString());
	}
	
	public Locale getLocale() {
		return locale;
	}
	
	private void setLocale(Locale locale) {
		this.locale = locale;
		this.formatter = new TextFormatter(locale, !simpleFormatter);
	}
	
	public String get(String key) {
		String result = properties.get(key);
		if (result == null) {
			if (parent != null)
				result = parent.get(key);
			if (result == null) {
				if (exceptionOnMissingKey)
					throw new MissingResourceException("Can't find bundle key " + key, this.getClass().getName(), key);
				else
					return "???" + key + "???";
			}
		}
		return result;
	}
	
	public Set<String> keys() {
		Set<String> result = new LinkedHashSet<>();
		ObjectMap.Keys<String> keys = properties.keys();
		if (keys != null) {
			for (String key : keys) {
				result.add(key);
			}
		}
		return result;
	}
	
	public String format(String key, Object... args) {
		return formatter.format(get(key), args);
	}
	
	public void debug(String placeholder) {
		ObjectMap.Keys<String> keys = properties.keys();
		if (keys == null)
			return;
		
		for (String s : keys) {
			properties.put(s, placeholder);
		}
	}
	
}
