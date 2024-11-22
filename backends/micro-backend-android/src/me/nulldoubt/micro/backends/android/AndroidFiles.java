
package me.nulldoubt.micro.backends.android;

import me.nulldoubt.micro.Files;

public interface AndroidFiles extends Files {
	
	boolean setAPKExpansion(int mainVersion, int patchVersion);
	
	ZipResourceFile getExpansionFile();
	
}
