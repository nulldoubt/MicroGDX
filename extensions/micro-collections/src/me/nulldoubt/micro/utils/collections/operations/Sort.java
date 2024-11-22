package me.nulldoubt.micro.utils.collections.operations;

import me.nulldoubt.micro.utils.collections.Array;

import java.util.Comparator;

public class Sort {
	
	private static Sort instance;
	
	private TimSort timSort;
	private ComparableTimSort comparableTimSort;
	
	public <T extends Comparable> void sort(Array<T> a) {
		if (comparableTimSort == null)
			comparableTimSort = new ComparableTimSort();
		comparableTimSort.doSort(a.items, 0, a.size);
	}
	
	/**
	 * The specified objects must implement {@link Comparable}.
	 */
	public void sort(Object[] a) {
		if (comparableTimSort == null)
			comparableTimSort = new ComparableTimSort();
		comparableTimSort.doSort(a, 0, a.length);
	}
	
	public void sort(Object[] a, int fromIndex, int toIndex) {
		if (comparableTimSort == null)
			comparableTimSort = new ComparableTimSort();
		comparableTimSort.doSort(a, fromIndex, toIndex);
	}
	
	public <T> void sort(Array<T> a, Comparator<? super T> c) {
		if (timSort == null)
			timSort = new TimSort<>();
		timSort.doSort(a.items, c, 0, a.size);
	}
	
	public <T> void sort(T[] a, Comparator<? super T> c) {
		if (timSort == null)
			timSort = new TimSort<>();
		timSort.doSort(a, c, 0, a.length);
	}
	
	public <T> void sort(T[] a, Comparator<? super T> c, int fromIndex, int toIndex) {
		if (timSort == null)
			timSort = new TimSort<>();
		timSort.doSort(a, c, fromIndex, toIndex);
	}
	
	public static Sort instance() {
		if (instance == null)
			instance = new Sort();
		return instance;
	}
	
}
