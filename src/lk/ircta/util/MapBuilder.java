package lk.ircta.util;

import java.util.HashMap;
import java.util.Map;

public class MapBuilder<K, V> {
	private final Map<K, V> map;
	
	public MapBuilder() {
		this.map = new HashMap<K, V>();
	}
	
	public MapBuilder(int capacity) {
		this.map = new HashMap<K, V>(capacity);
	}
	
	public MapBuilder(Class<Map<K, V>> clazz) {
		Map<K, V> map = null;
		try {
			map = clazz.newInstance();
		} catch (InstantiationException e) {
			throw new InstantiationError();
		} catch (IllegalAccessException e) {
			throw new InstantiationError();
		}
		
		this.map = map;
	}
	
	public MapBuilder<K, V> put(K key, V value) {
		map.put(key, value);
		
		return this;
	}
	
	public MapBuilder<K, V> putAll(Map<? extends K, ? extends V> map) {
		this.map.putAll(map);
		
		return this;
	}
	
	public Map<K, V> build() {
		return map;
	}
}
