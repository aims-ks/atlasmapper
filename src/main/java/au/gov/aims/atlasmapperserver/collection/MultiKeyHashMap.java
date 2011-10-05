/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.gov.aims.atlasmapperserver.collection;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author glafond
 */
public class MultiKeyHashMap<K1, K2, O> {
	// Dictionnary - each keys can return the other one
	private Map<K1, K2> keys1;
	private Map<K2, K1> keys2;

	// The values are store in one map, accessible with the primary key
	private Map<K1, O> values;

	public MultiKeyHashMap() {
		this.keys1 = new HashMap<K1, K2>();
		this.keys2 = new HashMap<K2, K1>();
		
		this.values = new HashMap<K1, O>();
	}

	public int size() {
		return this.values.size();
	}

	public boolean isEmpty() {
		return this.values.isEmpty();
	}

	public boolean containsKey1(K1 key) {
		return this.keys1.containsKey(key);
	}

	public boolean containsKey2(K2 key) {
		return this.keys2.containsKey(key);
	}

	public boolean containsValue(O value) {
		return this.values.containsValue(value);
	}

	public O get1(K1 key) {
		return this.values.get(key);
	}

	public O get2(K2 key) {
		if (!this.keys2.containsKey(key)) {
			return null;
		}
		return this.values.get(this.keys2.get(key));
	}

	public O put(K1 key1, K2 key2, O value) {
		// Avoid possible de-sync:
		// If K1 get updated, K2 MUST be updated too, even if its key has changed.
		// Keeping both keys in sync with their value may request a lot of
		// processing. It's easier and faster to delete the old values before
		// inserting a new one.
		//
		// Example:
		//     m.put(1, "a", "val");
		// Expected:
		//     [1, "a"] => "val"
		// Insert with same key2
		//     m.put(4, "a", "new val");
		// Expected:
		//     [1, "a"] => NOT PRESENT, key2 "a" has overriden this one.
		//     [4, "a"] => "new val"
		// If both were present, get2("a") would not be able to choose which one
		// to return.
		//
		// NOTE: Calling remove on a key that is not present is faster than
		// checking the presence of the key first.
		this.remove1(key1);
		this.remove2(key2);

		this.keys1.put(key1, key2);
		this.keys2.put(key2, key1);
		return this.values.put(key1, value);
	}

	public O remove1(K1 key) {
		if (this.keys1.containsKey(key)) {
			this.keys2.remove(this.keys1.get(key));
		}
		this.keys1.remove(key);
		return this.values.remove(key);
	}

	public O remove2(K2 key) {
		O obj = null;
		if (this.keys2.containsKey(key)) {
			K1 key1 = this.keys2.get(key);
			this.keys1.remove(key1);
			obj = this.values.remove(key1);
		}
		this.keys2.remove(key);
		return obj;
	}

	public void putAll(MultiKeyHashMap<? extends K1, ? extends K2, ? extends O> m) {
		// Avoid possible de-sync: see put
		for (K1 k1 : m.key1Set()) {
			this.remove1(k1);
		}
		for (K2 k2 : m.key2Set()) {
			this.remove2(k2);
		}

		this.keys1.putAll(m.keys1);
		this.keys2.putAll(m.keys2);
		this.values.putAll(m.values);
	}

	public void clear() {
		this.keys1.clear();
		this.keys2.clear();

		this.values.clear();
	}

	public Set<K1> key1Set() {
		return this.keys1.keySet();
	}

	public Set<K2> key2Set() {
		return this.keys2.keySet();
	}

	public Collection<O> values() {
		return this.values.values();
	}

	public Set<Map.Entry<K1, O>> entrySet() {
		return this.values.entrySet();
	}
}
