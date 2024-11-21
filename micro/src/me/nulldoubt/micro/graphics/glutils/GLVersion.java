package me.nulldoubt.micro.graphics.glutils;

import me.nulldoubt.micro.Application;
import me.nulldoubt.micro.Micro;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GLVersion {
	
	public static final String TAG = "GLVersion";
	
	private int majorVersion;
	private int minorVersion;
	private int releaseVersion;
	
	private final String versionString;
	private final String vendorString;
	private final String rendererString;
	
	private final Type type;
	
	public GLVersion(Application.ApplicationType appType, String versionString, String vendorString, String rendererString) {
		if (appType == Application.ApplicationType.Android)
			this.type = Type.GLES;
		else if (appType == Application.ApplicationType.Desktop)
			this.type = Type.OpenGL;
		else if (appType == Application.ApplicationType.Applet)
			this.type = Type.OpenGL;
		else
			this.type = Type.NONE;
		
		if (type == Type.GLES)
			extractVersion("OpenGL ES (\\d(\\.\\d){0,2})", versionString);
		else if (type == Type.OpenGL)
			extractVersion("(\\d(\\.\\d){0,2})", versionString);
		else {
			majorVersion = -1;
			minorVersion = -1;
			releaseVersion = -1;
			vendorString = "";
			rendererString = "";
		}
		this.versionString = versionString;
		this.vendorString = vendorString;
		this.rendererString = rendererString;
	}
	
	private void extractVersion(String patternString, String versionString) {
		Pattern pattern = Pattern.compile(patternString);
		Matcher matcher = pattern.matcher(versionString);
		boolean found = matcher.find();
		if (found) {
			String result = matcher.group(1);
			String[] resultSplit = result.split("\\.");
			majorVersion = parseInt(resultSplit[0], 2);
			minorVersion = resultSplit.length < 2 ? 0 : parseInt(resultSplit[1], 0);
			releaseVersion = resultSplit.length < 3 ? 0 : parseInt(resultSplit[2], 0);
		} else {
			Micro.app.log(TAG, "Invalid version string: " + versionString);
			majorVersion = 2;
			minorVersion = 0;
			releaseVersion = 0;
		}
	}
	
	private int parseInt(String v, int defaultValue) {
		try {
			return Integer.parseInt(v);
		} catch (NumberFormatException nfe) {
			Micro.app.error("libGDX GL", "Error parsing number: " + v + ", assuming: " + defaultValue);
			return defaultValue;
		}
	}
	
	public Type getType() {
		return type;
	}
	
	public int getMajorVersion() {
		return majorVersion;
	}
	
	public int getMinorVersion() {
		return minorVersion;
	}
	
	public int getReleaseVersion() {
		return releaseVersion;
	}
	
	public String getVersionString() {
		return versionString;
	}
	
	public String getVendorString() {
		return vendorString;
	}
	
	public String getRendererString() {
		return rendererString;
	}
	
	public boolean isVersionEqualToOrHigher(int testMajorVersion, int testMinorVersion) {
		return majorVersion > testMajorVersion || (majorVersion == testMajorVersion && minorVersion >= testMinorVersion);
	}
	
	public String getDebugVersionString() {
		return "Type: " + type + "\n" + "Version: " + majorVersion + ":" + minorVersion + ":" + releaseVersion + "\n" + "Vendor: " + vendorString + "\n" + "Renderer: " + rendererString;
	}
	
	public enum Type {
		OpenGL, GLES, NONE
	}
	
}
