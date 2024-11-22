package me.nulldoubt.micro.backends.android;

import android.hardware.SensorManager;
import me.nulldoubt.micro.backends.android.surfaceview.FillResolutionStrategy;
import me.nulldoubt.micro.backends.android.surfaceview.ResolutionStrategy;
import me.nulldoubt.micro.utils.natives.MicroNativesLoader;

public class AndroidApplicationConfiguration {
	
	public int r = 8, g = 8, b = 8, a = 0;
	
	public int depth = 16, stencil = 0;
	
	public int numSamples = 0;
	
	public boolean useAccelerometer = true;
	
	public boolean useGyroscope = false;
	
	public boolean useCompass = true;
	
	public boolean useRotationVectorSensor = false;
	
	public int sensorDelay = SensorManager.SENSOR_DELAY_GAME;
	
	public int touchSleepTime = 0;
	
	public boolean useWakelock = false;
	
	public boolean disableAudio = false;
	
	public int maxSimultaneousSounds = 16;
	
	public ResolutionStrategy resolutionStrategy = new FillResolutionStrategy();
	
	public boolean getTouchEventsForLiveWallpaper = false;
	
	public boolean useImmersiveMode = true;
	
	public boolean useGL30 = false;
	
	public boolean renderUnderCutout;
	
	public MicroNativeLoader nativeLoader = MicroNativesLoader::load;
	
}
