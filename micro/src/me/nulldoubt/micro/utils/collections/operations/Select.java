package me.nulldoubt.micro.utils.collections.operations;

import me.nulldoubt.micro.exceptions.MicroRuntimeException;

import java.util.Comparator;

public class Select {
	
	private static Select instance;
	private QuickSelect quickSelect;
	
	/**
	 * Provided for convenience
	 */
	public static Select instance() {
		if (instance == null)
			instance = new Select();
		return instance;
	}
	
	public <T> T select(T[] items, Comparator<T> comp, int kthLowest, int size) {
		int idx = selectIndex(items, comp, kthLowest, size);
		return items[idx];
	}
	
	public <T> int selectIndex(T[] items, Comparator<T> comp, int kthLowest, int size) {
		if (size < 1) {
			throw new MicroRuntimeException("cannot select from empty array (size < 1)");
		} else if (kthLowest > size) {
			throw new MicroRuntimeException("Kth rank is larger than size. k: " + kthLowest + ", size: " + size);
		}
		int idx;
		// naive partial selection sort almost certain to outperform quickselect where n is min or max
		if (kthLowest == 1) {
			// find min
			idx = fastMin(items, comp, size);
		} else if (kthLowest == size) {
			// find max
			idx = fastMax(items, comp, size);
		} else {
			// quickselect a better choice for cases of k between min and max
			if (quickSelect == null)
				quickSelect = new QuickSelect();
			idx = quickSelect.select(items, comp, kthLowest, size);
		}
		return idx;
	}
	
	/**
	 * Faster than quickselect for n = min
	 */
	private <T> int fastMin(T[] items, Comparator<T> comp, int size) {
		int lowestIdx = 0;
		for (int i = 1; i < size; i++) {
			int comparison = comp.compare(items[i], items[lowestIdx]);
			if (comparison < 0) {
				lowestIdx = i;
			}
		}
		return lowestIdx;
	}
	
	/**
	 * Faster than quickselect for n = max
	 */
	private <T> int fastMax(T[] items, Comparator<T> comp, int size) {
		int highestIdx = 0;
		for (int i = 1; i < size; i++) {
			int comparison = comp.compare(items[i], items[highestIdx]);
			if (comparison > 0) {
				highestIdx = i;
			}
		}
		return highestIdx;
	}
	
}
