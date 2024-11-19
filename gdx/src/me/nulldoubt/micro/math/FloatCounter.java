package me.nulldoubt.micro.math;

import me.nulldoubt.micro.utils.pools.Pool.Poolable;

public class FloatCounter implements Poolable {
	
	public int count;
	public float total;
	public float min;
	public float max;
	public float average;
	public float latest;
	public float value;
	
	public final WindowedMean mean;
	
	public FloatCounter(int windowSize) {
		mean = (windowSize > 1) ? new WindowedMean(windowSize) : null;
		reset();
	}
	
	public void put(float value) {
		latest = value;
		total += value;
		count++;
		average = total / count;
		
		if (mean != null) {
			mean.addValue(value);
			this.value = mean.getMean();
		} else
			this.value = latest;
		
		if (mean == null || mean.hasEnoughData()) {
			if (this.value < min)
				min = this.value;
			if (this.value > max)
				max = this.value;
		}
	}
	
	@Override
	public void reset() {
		count = 0;
		total = 0f;
		min = Float.MAX_VALUE;
		max = -Float.MAX_VALUE;
		average = 0f;
		latest = 0f;
		value = 0f;
		if (mean != null)
			mean.clear();
	}
	
	@Override
	public String toString() {
		return "FloatCounter{" + "count=" + count + ", total=" + total + ", min=" + min + ", max=" + max + ", average=" + average + ", latest=" + latest + ", value=" + value + '}';
	}
	
}
