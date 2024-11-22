package me.nulldoubt.micro.backends.android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import me.nulldoubt.micro.Input;
import me.nulldoubt.micro.math.MathUtils;

public class AndroidHaptics {
	
	private final Vibrator vibrator;
	private AudioAttributes audioAttributes;
	private boolean vibratorSupport;
	private boolean hapticsSupport;
	
	public AndroidHaptics(Context context) {
		vibratorSupport = false;
		hapticsSupport = false;
		this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
		if (vibrator != null && vibrator.hasVibrator()) {
			vibratorSupport = true;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
				if (vibrator.hasAmplitudeControl()) {
					hapticsSupport = true;
				}
				this.audioAttributes = new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).setUsage(AudioAttributes.USAGE_GAME).build();
			}
		}
	}
	
	@SuppressLint("MissingPermission")
	public void vibrate(int milliseconds) {
		if (vibratorSupport) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
				vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE));
			else
				vibrator.vibrate(milliseconds);
		}
	}
	
	@SuppressLint("MissingPermission")
	public void vibrate(Input.VibrationType vibrationType) {
		if (hapticsSupport) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
				int vibrationEffect = switch (vibrationType) {
					case LIGHT -> VibrationEffect.EFFECT_TICK;
					case MEDIUM -> VibrationEffect.EFFECT_CLICK;
					case HEAVY -> VibrationEffect.EFFECT_HEAVY_CLICK;
					default -> throw new IllegalArgumentException("Unknown VibrationType " + vibrationType);
				};
				vibrator.vibrate(VibrationEffect.createPredefined(vibrationEffect), audioAttributes);
			}
		}
	}
	
	@SuppressLint("MissingPermission")
	public void vibrate(int milliseconds, int intensity, boolean fallback) {
		if (hapticsSupport) {
			intensity = MathUtils.clamp(intensity, 0, 255);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
				vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, intensity));
		} else if (fallback)
			vibrate(milliseconds);
	}
	
	public boolean hasVibratorAvailable() {
		return vibratorSupport;
	}
	
	public boolean hasHapticsSupport() {
		return hapticsSupport;
	}
	
}
