package jp.ac.shibaura_it.ma15082;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ListMap<K, V> implements Map<K, V> {
	private List<K> keys;
	private List<V> values;

	public ListMap() {
		keys = new ArrayList<K>();
		values = new ArrayList<V>();
	}

	public ListMap(int size) {
		keys = new ArrayList<K>(size);
		values = new ArrayList<V>(size);
	}

	public ListMap(Map<K, V> map) {
		keys = new ArrayList<K>(map.size());
		values = new ArrayList<V>(map.size());
		for (Entry<K, V> ery : map.entrySet()) {
			keys.add(ery.getKey());
			values.add(ery.getValue());
		}
	}

	public ListMap(ListMap<K, V> map) {
		keys = new ArrayList<K>(map.size());
		values = new ArrayList<V>(map.size());
		for (int i = 0; i < map.size(); ++i) {
			keys.add(map.getKey(i));
			values.add(map.getValue(i));
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder("");
		for (int i = 0; i < keys.size(); ++i) {
			sb.append("[" + keys.get(i) + "," + values.get(i) + "]");
		}

		return sb.toString();
	}

	final public K getKey(int i) {

		if (keys.size() <= i || i < 0) {
			return null;
		}
		return keys.get(i);

	}

	final public V getValue(int i) {

		if (values.size() <= i || i < 0) {
			return null;
		}
		return values.get(i);

	}

	final public List<K> keyList() {
		return new ArrayList<K>(keys);
	}

	final public List<V> valueList() {
		return new ArrayList<V>(values);
	}

	@Override
	public void clear() {
		keys.clear();
		values.clear();
	}

	@Override
	final public V put(K key, V value) {
		V prev = null;
		int index = keys.indexOf(key);
		if (index < 0) {
			keys.add(key);
			values.add(value);
		} else {
			prev = values.get(index);
			values.set(index, value);
		}
		return prev;
	}

	final public void add(K key, V value) {
		keys.add(key);
		values.add(value);
	}

	@Override
	final public V get(Object key) {
		int index = keys.indexOf(key);
		if (index < 0) {
			return null;
		} else {
			return values.get(index);
		}
	}

	@Override
	final public int size() {
		return keys.size();
	}

	@Override
	final public boolean containsKey(Object arg0) {
		return keys.contains(arg0);
	}

	@Override
	final public boolean containsValue(Object arg0) {
		return values.contains(arg0);
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		Set<Entry<K, V>> ret = new LinkedHashSet<Entry<K, V>>();

		for (int i = 0; i < keys.size(); ++i) {
			ret.add(new Pair<K, V>(keys.get(i), values.get(i)));
		}
		return ret;
	}

	public List<Entry<K, V>> entryList() {
		List<Entry<K, V>> ret = new ArrayList<Entry<K, V>>();
		for (int i = 0; i < keys.size(); ++i) {
			ret.add(new Pair<K, V>(keys.get(i), values.get(i)));
		}
		return ret;
	}

	@Override
	final public boolean isEmpty() {
		return (keys.size() <= 0);
	}

	@Override
	public Set<K> keySet() {
		return new LinkedHashSet<K>(keys);
	}

	@Override
	final public void putAll(Map<? extends K, ? extends V> arg0) {
		for (K key : arg0.keySet()) {
			put(key, arg0.get(key));
		}

	}

	@Override
	final public V remove(Object key) {
		int index = keys.indexOf(key);
		if (index < 0) {
			return null;
		} else {
			V ret = values.remove(index);
			keys.remove(index);
			return ret;
		}
	}

	public boolean delete(int i) {
		if (keys.size() <= i) {
			return false;
		}
		values.remove(i);
		keys.remove(i);
		return true;
	}

	public void setValue(int i, V val) {
		values.set(i, val);
	}

	public void setKey(int i, K key) {
		keys.set(i, key);
	}

	@Override
	public Collection<V> values() {
		return new ArrayList<V>(values);
	}

	final public Pair<K, V> first() {
		return (size() <= 0) ? null : new Pair<K, V>(keys.remove(0), values.remove(0));
	}

	final public boolean exchange(int i, int j) {
		K key1, key2;
		V val1, val2;
		key1 = keys.get(i);
		key2 = keys.get(j);
		val1 = values.get(i);
		val2 = values.get(j);
		if (key1 == null || key2 == null || val1 == null || val2 == null) {
			return false;
		}
		keys.set(j, key1);
		keys.set(i, key2);
		values.set(j, val1);
		values.set(i, val2);
		return true;
	}

	final public void swap(int i, int j) {
		K key = keys.set(j, keys.get(i));
		keys.set(i, key);
		V val = values.set(j, values.get(i));
		values.set(i, val);
		return;
	}

}
