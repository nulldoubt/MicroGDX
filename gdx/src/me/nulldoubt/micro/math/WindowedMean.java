package me.nulldoubt.micro.math;

import java.util.Arrays;

public final class WindowedMean {
	
	float[] values;
	int added_values = 0;
	int last_value;
	float mean = 0;
	boolean dirty = true;
	
	public WindowedMean(int window_size) {
		values = new float[window_size];
	}
	
	public boolean hasEnoughData() {
		return added_values >= values.length;
	}
	
	public void clear() {
		added_values = 0;
		last_value = 0;
		Arrays.fill(values, 0);
		dirty = true;
	}
	
	public void addValue(float value) {
		if (added_values < values.length)
			added_values++;
		values[last_value++] = value;
		if (last_value > values.length - 1)
			last_value = 0;
		dirty = true;
	}
	
	public float getMean() {
		if (hasEnoughData()) {
			if (dirty) {
				float mean = 0;
				for (float value : values)
					mean += value;
				
				this.mean = mean / values.length;
				dirty = false;
			}
			return this.mean;
		} else
			return 0;
	}
	
	public float getOldest() {
		return added_values < values.length ? values[0] : values[last_value];
	}
	
	public float getLatest() {
		return values[last_value - 1 == -1 ? values.length - 1 : last_value - 1];
	}
	
	public float standardDeviation() {
		if (!hasEnoughData())
			return 0;
		
		float mean = getMean();
		float sum = 0;
		for (float value : values)
			sum += (value - mean) * (value - mean);
		
		return (float) Math.sqrt(sum / values.length);
	}
	
	public float getLowest() {
		float lowest = Float.MAX_VALUE;
		for (float value : values)
			lowest = Math.min(lowest, value);
		return lowest;
	}
	
	public float getHighest() {
		float lowest = Float.MIN_NORMAL;
		for (float value : values)
			lowest = Math.max(lowest, value);
		return lowest;
	}
	
	public int getValueCount() {
		return added_values;
	}
	
	public int getWindowSize() {
		return values.length;
	}
	
	public float[] getWindowValues() {
		final float[] windowValues = new float[added_values];
		if (hasEnoughData()) {
			for (int i = 0; i < windowValues.length; i++)
				windowValues[i] = values[(i + last_value) % values.length];
		} else
			System.arraycopy(values, 0, windowValues, 0, added_values);
		return windowValues;
	}
	
}
