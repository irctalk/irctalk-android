package lk.ircta.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

public class SortedList<E> extends ArrayList<E> {
	private static final long serialVersionUID = 3943385112546367984L;
	
	private final Comparator<E> comparator;
	private final boolean isSet;
	
	public SortedList(Comparator<E> comparator, boolean isSet) {
		this.comparator = comparator;
		this.isSet = isSet;
	}
	
	public SortedList(Collection<E> collection, Comparator<E> comparator, boolean isSet) {
		super(collection);
		
		this.comparator = comparator;
		this.isSet = isSet;
		Collections.sort(this, comparator);
	}
	
	@Override
	public void add(int index, E object) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean addAll(int index, Collection<? extends E> collection) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean add(E object) {
		int idx = Collections.binarySearch(this, object, comparator);
		if (isSet && idx >= 0)
			return false;
		if (idx < 0) 
			idx = -idx - 1;
		super.add(idx, object);
		return true;
	}

	public boolean addIfAbsent(E object) {
		int idx = Collections.binarySearch(this, object, comparator);
		if (idx >= 0)
			return false;
		super.add(-idx - 1, object);
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends E> collection) {
		for (E element : collection) 
			add(element);
		return true;
	}
}
