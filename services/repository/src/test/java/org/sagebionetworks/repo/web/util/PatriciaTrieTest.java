package org.sagebionetworks.repo.web.util;

/**
 * From Kapsi and Berlin, Copyright 2005-2008. Used under Apache Commons License.
 * 
 * rkapsi-patricia-tree-aab36ad
 */

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import junit.framework.TestCase;

import org.ardverk.collection.IntegerKeyAnalyzer;
import org.ardverk.collection.PatriciaTrie;
import org.ardverk.collection.StringKeyAnalyzer;
import org.junit.Test;

public class PatriciaTrieTest {
  
  @Test
  public void testSimple() {
    PatriciaTrie<Integer, String> intTrie = new PatriciaTrie<Integer, String>(IntegerKeyAnalyzer.INSTANCE);
    TestCase.assertTrue(intTrie.isEmpty());
    TestCase.assertEquals(0, intTrie.size());
    
    intTrie.put(1, "One");
    TestCase.assertFalse(intTrie.isEmpty());
    TestCase.assertEquals(1, intTrie.size());
    
    TestCase.assertEquals("One", intTrie.remove(1));
    TestCase.assertNull(intTrie.remove(1));
    TestCase.assertTrue(intTrie.isEmpty());
    TestCase.assertEquals(0, intTrie.size());
    
    intTrie.put(1, "One");
    TestCase.assertEquals("One", intTrie.get(1));
    TestCase.assertEquals("One", intTrie.put(1, "NotOne"));
    TestCase.assertEquals(1, intTrie.size());
    TestCase.assertEquals("NotOne", intTrie.get(1));
    TestCase.assertEquals("NotOne", intTrie.remove(1));
    TestCase.assertNull(intTrie.put(1, "One"));
  }
  
  @Test
  public void testPrefixedBy() {
    PatriciaTrie<String, String> trie 
      = new PatriciaTrie<String, String>(StringKeyAnalyzer.CHAR);
    
    final String[] keys = new String[]{
        "", 
        "Albert", "Xavier", "XyZ", "Anna", "Alien", "Alberto",
        "Alberts", "Allie", "Alliese", "Alabama", "Banane",
        "Blabla", "Amber", "Ammun", "Akka", "Akko", "Albertoo",
        "Amma"
    };

    for (String key : keys) {
      trie.put(key, key);
    }
    
    SortedMap<String, String> map;
    Iterator<String> iterator;
    Iterator<Map.Entry<String, String>> entryIterator;
    Map.Entry<String, String> entry;
    
    map = trie.prefixMap("Al");
    TestCase.assertEquals(8, map.size());
    TestCase.assertEquals("Alabama", map.firstKey());
    TestCase.assertEquals("Alliese", map.lastKey());
    TestCase.assertEquals("Albertoo", map.get("Albertoo"));
    TestCase.assertNotNull(trie.get("Xavier"));
    TestCase.assertNull(map.get("Xavier"));
    TestCase.assertNull(trie.get("Alice"));
    TestCase.assertNull(map.get("Alice"));
    iterator = map.values().iterator();
    TestCase.assertEquals("Alabama", iterator.next());
    TestCase.assertEquals("Albert", iterator.next());
    TestCase.assertEquals("Alberto", iterator.next());
    TestCase.assertEquals("Albertoo", iterator.next());
    TestCase.assertEquals("Alberts", iterator.next());
    TestCase.assertEquals("Alien", iterator.next());
    TestCase.assertEquals("Allie", iterator.next());
    TestCase.assertEquals("Alliese", iterator.next());
    TestCase.assertFalse(iterator.hasNext());
    
    map = trie.prefixMap("Albert");
    iterator = map.keySet().iterator();
    TestCase.assertEquals("Albert", iterator.next());
    TestCase.assertEquals("Alberto", iterator.next());
    TestCase.assertEquals("Albertoo", iterator.next());
    TestCase.assertEquals("Alberts", iterator.next());
    TestCase.assertFalse(iterator.hasNext());
    TestCase.assertEquals(4, map.size());
    TestCase.assertEquals("Albert", map.firstKey());
    TestCase.assertEquals("Alberts", map.lastKey());
    TestCase.assertNull(trie.get("Albertz"));
    map.put("Albertz", "Albertz");
    TestCase.assertEquals("Albertz", trie.get("Albertz"));
    TestCase.assertEquals(5, map.size());
    TestCase.assertEquals("Albertz", map.lastKey());
    iterator = map.keySet().iterator();
    TestCase.assertEquals("Albert", iterator.next());
    TestCase.assertEquals("Alberto", iterator.next());
    TestCase.assertEquals("Albertoo", iterator.next());
    TestCase.assertEquals("Alberts", iterator.next());
    TestCase.assertEquals("Albertz", iterator.next());
    TestCase.assertFalse(iterator.hasNext());
    TestCase.assertEquals("Albertz", map.remove("Albertz"));
    
    map = trie.prefixMap("Alberto");
    TestCase.assertEquals(2, map.size());
    TestCase.assertEquals("Alberto", map.firstKey());
    TestCase.assertEquals("Albertoo", map.lastKey());
    entryIterator = map.entrySet().iterator();
    entry = entryIterator.next();
    TestCase.assertEquals("Alberto", entry.getKey());
    TestCase.assertEquals("Alberto", entry.getValue());
    entry = entryIterator.next();
    TestCase.assertEquals("Albertoo", entry.getKey());
    TestCase.assertEquals("Albertoo", entry.getValue());
    TestCase.assertFalse(entryIterator.hasNext());
    trie.put("Albertoad", "Albertoad");
    TestCase.assertEquals(3, map.size());
    TestCase.assertEquals("Alberto", map.firstKey());
    TestCase.assertEquals("Albertoo", map.lastKey());
    entryIterator = map.entrySet().iterator();
    entry = entryIterator.next();
    TestCase.assertEquals("Alberto", entry.getKey());
    TestCase.assertEquals("Alberto", entry.getValue());
    entry = entryIterator.next();
    TestCase.assertEquals("Albertoad", entry.getKey());
    TestCase.assertEquals("Albertoad", entry.getValue());
    entry = entryIterator.next();
    TestCase.assertEquals("Albertoo", entry.getKey());
    TestCase.assertEquals("Albertoo", entry.getValue());
    TestCase.assertFalse(entryIterator.hasNext());
    TestCase.assertEquals("Albertoo", trie.remove("Albertoo"));
    TestCase.assertEquals("Alberto", map.firstKey());
    TestCase.assertEquals("Albertoad", map.lastKey());
    TestCase.assertEquals(2, map.size());
    entryIterator = map.entrySet().iterator();
    entry = entryIterator.next();
    TestCase.assertEquals("Alberto", entry.getKey());
    TestCase.assertEquals("Alberto", entry.getValue());
    entry = entryIterator.next();
    TestCase.assertEquals("Albertoad", entry.getKey());
    TestCase.assertEquals("Albertoad", entry.getValue());
    TestCase.assertFalse(entryIterator.hasNext());
    TestCase.assertEquals("Albertoad", trie.remove("Albertoad"));
    trie.put("Albertoo", "Albertoo");
    
    map = trie.prefixMap("X");
    TestCase.assertEquals(2, map.size());
    TestCase.assertFalse(map.containsKey("Albert"));
    TestCase.assertTrue(map.containsKey("Xavier"));
    TestCase.assertFalse(map.containsKey("Xalan"));
    iterator = map.values().iterator();
    TestCase.assertEquals("Xavier", iterator.next());
    TestCase.assertEquals("XyZ", iterator.next());
    TestCase.assertFalse(iterator.hasNext());
    
    map = trie.prefixMap("An");
    TestCase.assertEquals(1, map.size());
    TestCase.assertEquals("Anna", map.firstKey());
    TestCase.assertEquals("Anna", map.lastKey());
    iterator = map.keySet().iterator();
    TestCase.assertEquals("Anna", iterator.next());
    TestCase.assertFalse(iterator.hasNext());
    
    map = trie.prefixMap("Ban");
    TestCase.assertEquals(1, map.size());
    TestCase.assertEquals("Banane", map.firstKey());
    TestCase.assertEquals("Banane", map.lastKey());
    iterator = map.keySet().iterator();
    TestCase.assertEquals("Banane", iterator.next());
    TestCase.assertFalse(iterator.hasNext());
    
    map = trie.prefixMap("Am");
    TestCase.assertFalse(map.isEmpty());
    TestCase.assertEquals(3, map.size());
    TestCase.assertEquals("Amber", trie.remove("Amber"));
    iterator = map.keySet().iterator();
    TestCase.assertEquals("Amma", iterator.next());
    TestCase.assertEquals("Ammun", iterator.next());
    TestCase.assertFalse(iterator.hasNext());
    iterator = map.keySet().iterator();
    map.put("Amber", "Amber");
    TestCase.assertEquals(3, map.size());
    try {
      iterator.next();
      TestCase.fail("CME expected");
    } catch(ConcurrentModificationException expected) {}
    TestCase.assertEquals("Amber", map.firstKey());
    TestCase.assertEquals("Ammun", map.lastKey());
    
    map = trie.prefixMap("Ak\0");
    TestCase.assertTrue(map.isEmpty());
    
    map = trie.prefixMap("Ak");
    TestCase.assertEquals(2, map.size());
    TestCase.assertEquals("Akka", map.firstKey());
    TestCase.assertEquals("Akko", map.lastKey());
    map.put("Ak", "Ak");
    TestCase.assertEquals("Ak", map.firstKey());
    TestCase.assertEquals("Akko", map.lastKey());
    TestCase.assertEquals(3, map.size());
    trie.put("Al", "Al");
    TestCase.assertEquals(3, map.size());
    TestCase.assertEquals("Ak", map.remove("Ak"));
    TestCase.assertEquals("Akka", map.firstKey());
    TestCase.assertEquals("Akko", map.lastKey());
    TestCase.assertEquals(2, map.size());
    iterator = map.keySet().iterator();
    TestCase.assertEquals("Akka", iterator.next());
    TestCase.assertEquals("Akko", iterator.next());
    TestCase.assertFalse(iterator.hasNext());
    TestCase.assertEquals("Al", trie.remove("Al"));
    
    map = trie.prefixMap("Akka");
    TestCase.assertEquals(1, map.size());
    TestCase.assertEquals("Akka", map.firstKey());
    TestCase.assertEquals("Akka", map.lastKey());
    iterator = map.keySet().iterator();
    TestCase.assertEquals("Akka", iterator.next());
    TestCase.assertFalse(iterator.hasNext());
    
    map = trie.prefixMap("Ab");
    TestCase.assertTrue(map.isEmpty());
    TestCase.assertEquals(0, map.size());
    try {
      Object o = map.firstKey();
      TestCase.fail("got a first key: " + o);
    } catch(NoSuchElementException nsee) {}
    try {
      Object o = map.lastKey();
      TestCase.fail("got a last key: " + o);
    } catch(NoSuchElementException nsee) {}
    iterator = map.values().iterator();
    TestCase.assertFalse(iterator.hasNext());
    
    map = trie.prefixMap("Albertooo");
    TestCase.assertTrue(map.isEmpty());
    TestCase.assertEquals(0, map.size());
    try {
      Object o = map.firstKey();
      TestCase.fail("got a first key: " + o);
    } catch(NoSuchElementException nsee) {}
    try {
      Object o = map.lastKey();
      TestCase.fail("got a last key: " + o);
    } catch(NoSuchElementException nsee) {}
    iterator = map.values().iterator();
    TestCase.assertFalse(iterator.hasNext());
    
    map = trie.prefixMap("");
    TestCase.assertSame(trie, map); // stricter than necessary, but a good check
    
    map = trie.prefixMap("\0");
    TestCase.assertTrue(map.isEmpty());
    TestCase.assertEquals(0, map.size());
    try {
      Object o = map.firstKey();
      TestCase.fail("got a first key: " + o);
    } catch(NoSuchElementException nsee) {}
    try {
      Object o = map.lastKey();
      TestCase.fail("got a last key: " + o);
    } catch(NoSuchElementException nsee) {}
    iterator = map.values().iterator();
    TestCase.assertFalse(iterator.hasNext());
  }
  
  @Test
  public void testPrefixedByRemoval() {
    PatriciaTrie<String, String> trie 
      = new PatriciaTrie<String, String>(StringKeyAnalyzer.CHAR);
    
    final String[] keys = new String[]{
        "Albert", "Xavier", "XyZ", "Anna", "Alien", "Alberto",
        "Alberts", "Allie", "Alliese", "Alabama", "Banane",
        "Blabla", "Amber", "Ammun", "Akka", "Akko", "Albertoo",
        "Amma"
    };

    for (String key : keys) {
      trie.put(key, key);
    }
    
    SortedMap<String, String> map = trie.prefixMap("Al");
    TestCase.assertEquals(8, map.size());
    Iterator<String> iter = map.keySet().iterator();
    TestCase.assertEquals("Alabama", iter.next());
    TestCase.assertEquals("Albert", iter.next());
    TestCase.assertEquals("Alberto", iter.next());
    TestCase.assertEquals("Albertoo", iter.next());
    TestCase.assertEquals("Alberts", iter.next());
    TestCase.assertEquals("Alien", iter.next());
    iter.remove();
    TestCase.assertEquals(7, map.size());
    TestCase.assertEquals("Allie", iter.next());
    TestCase.assertEquals("Alliese", iter.next());
    TestCase.assertFalse(iter.hasNext());
    
    map = trie.prefixMap("Ak");
    TestCase.assertEquals(2, map.size());
    iter = map.keySet().iterator();
    TestCase.assertEquals("Akka", iter.next());
    iter.remove();
    TestCase.assertEquals(1, map.size());
    TestCase.assertEquals("Akko", iter.next());
    if(iter.hasNext())
      TestCase.fail("shouldn't have next (but was: " + iter.next() + ")");
    TestCase.assertFalse(iter.hasNext());
  }
}
