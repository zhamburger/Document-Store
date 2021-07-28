package edu.yu.cs.com1320.project;

import edu.yu.cs.com1320.project.impl.TrieImpl;
import org.junit.Assert;
import org.junit.Test;
import java.util.*;

public class TrieImplTest {

    /**
     * .put(String, Value);
     * add the given value at the given key
     */

    @Test
    public void putInTrieTest() {
        TrieImpl trie = new TrieImpl();
        trie.put("Hello World1", 1);
        trie.put("Hello World2", 2);
        trie.put("Hello World3", 3);
    }

    @Test
    public void putMultipleValuesInTrieTest() {
        TrieImpl trie = new TrieImpl();
        trie.put("Same Document", "Same Document1");
        trie.put("Same Document", "Same Document2");
        trie.put("Different Document", "Different Document");
        List<String> array = new ArrayList<String>();
        array.add("Same Document1");
        array.add("Same Document2");
        Assert.assertEquals(array, trie.getAllSorted("Same Document", getPrefixComparator("Same Document")));
    }

    @Test
    public void putTheSameDocument(){
        TrieImpl trie = new TrieImpl();
        trie.put("Same Document", "Same Document");
        trie.put("Same Document", "Same Document");
        List<String> array = new ArrayList<String>();
        array.add("Same Document");
        Assert.assertEquals(array, trie.getAllSorted("Same Document", getPrefixComparator("Same Document")));
    }

    @Test
    public void testPutNullValue(){
        TrieImpl trie = new TrieImpl();
        trie.put("hi", null);
        Assert.assertEquals(new ArrayList<>(), trie.getAllSorted("hi", getPrefixComparator("hi")));
    }

    @Test
    public void testPutNullKey(){
        TrieImpl trie = new TrieImpl();
        trie.put(null, "null");
        Assert.assertEquals(new ArrayList<>(), trie.getAllSorted("null", getPrefixComparator("null")));
    }



    /**
     * .getAllSorted(String, Comparator);
     * get all exact matches for the given key, sorted in descending order.
     * Search is CASE INSENSITIVE.
     * @return a List of matching Values, in descending order
     */

    @Test
    public void getAllSortedFromTrieTest() {
        TrieImpl trie = new TrieImpl();
        trie.put("Hello World1", "on");
        trie.put("Hello World2", "two");
        trie.put("Hello World3", "three");
        List<String> strings = new ArrayList<String>();
        strings.add("on");
        strings.add("two");
        strings.add("three");
        Assert.assertEquals("on", trie.getAllSorted("World1", getPrefixComparator("World1")).get((0)));
        Assert.assertEquals(strings, trie.getAllSorted("Hello", getPrefixComparator("Hello")));
    }

    @Test
    public void getAllSortedOrder() {
        TrieImpl trie = new TrieImpl();
        trie.put("This document has four four four four", "This document has four four four four");
        trie.put("This document has three four four four", "This document has three four four four");
        trie.put("This document has two four four", "This document has two four four");
        List<String> strings = new ArrayList<String>();
        strings.add("This document has four four four four");
        Assert.assertEquals("This document has four four four four", trie.getAllSorted("four", getPrefixComparator("four")).get((0)));
        strings.add("This document has three four four four");
        Assert.assertEquals("This document has three four four four", trie.getAllSorted("four", getPrefixComparator("four")).get((1)));
        strings.add("This document has two four four");
        Assert.assertEquals("This document has two four four", trie.getAllSorted("four", getPrefixComparator("four")).get((2)));
        Assert.assertEquals(strings, trie.getAllSorted("four", getPrefixComparator("four")));
    }

    @Test
    public void testNullKeyGetAllSorted(){
        TrieImpl trie = new TrieImpl();

        List<String> strings = new ArrayList<String>();
        Assert.assertEquals(strings, trie.getAllSorted(null, getPrefixComparator("World1")));
    }

    @Test
    public void testNullComparatorGetAllSorted(){
        TrieImpl trie = new TrieImpl();
        List<String> strings = new ArrayList<String>();
        Assert.assertEquals(strings, trie.getAllSorted("World1", null));
    }

    @Test
    public void putTheSameValueInGetAllSorted(){
        TrieImpl trie = new TrieImpl();
        trie.put("Same Document", "Same Document1");
        trie.put("Same Document", "Same Document2");
        List<String> array = new ArrayList<String>();
        array.add("Same Document1");
        array.add("Same Document2");
        Assert.assertEquals(array, trie.getAllSorted("Same", getPrefixComparator("Same")));
    }

    @Test
    public void putTheSameValueUpperCaseInGetAllSorted(){
        TrieImpl trie = new TrieImpl();
        trie.put("SAME DOCUMENT", "Same Document");
        trie.put("Same Document", "Same Document");
        List<String> array = new ArrayList<String>();
        array.add("Same Document");
        Assert.assertEquals(array, trie.getAllSorted("Same", getPrefixComparator("Same")));
    }

    /**
     * .getAllWithPrefixSorted(String, Comparator);
     * get all matches which contain a String with the given prefix, sorted in descending order.
     * For example, if the key is "Too", you would return any value that contains "Tool", "Too", "Tooth", "Toodle", etc.
     * Search is CASE INSENSITIVE.
     * @return a List of all matching Values containing the given prefix, in descending order
     */

    @Test
    public void getAllPrefixTest() {
        TrieImpl trie = new TrieImpl();
        trie.put("Pickle Rick pickle pick", "Pickle Rick pickle pick");
        trie.put("Mr Meeseeks", "Mr Meeseeks");
        trie.put("Pick", "Pick");
        trie.put("picked apart pickles", "picked apart pickles");
        List strings = trie.getAllWithPrefixSorted("pick", getPrefixComparator("pick"));
        Assert.assertEquals("Pickle Rick pickle pick", strings.get(0));
        Assert.assertEquals("picked apart pickles", strings.get(1));
        Assert.assertEquals("Pick", strings.get(2));
    }

    @Test
    public void testNullKeyGetAllWithPrefixSorted(){
        TrieImpl trie = new TrieImpl();
        List<String> strings = new ArrayList<String>();
        Assert.assertEquals(strings, trie.getAllWithPrefixSorted(null, getPrefixComparator("World1")));
    }

    @Test
    public void testNullComparatorGetAllWithPrefixSorted(){
        TrieImpl trie = new TrieImpl();
        List<String> strings = new ArrayList<String>();
        Assert.assertEquals(strings, trie.getAllWithPrefixSorted("World1", null));
    }

    @Test
    public void putTheSameDocumentInGetAllWithPrefixSorted(){
        TrieImpl trie = new TrieImpl();
        trie.put("Same Document", "Same Document");
        trie.put("Same Document", "Same Document");
        List<String> array = new ArrayList<String>();
        array.add("Same Document");
        Assert.assertEquals(array, trie.getAllWithPrefixSorted("Sam", getPrefixComparator("Sam")));
    }

    @Test
    public void putTheSameValueInGetAllWithPrefixSorted(){
        TrieImpl trie = new TrieImpl();
        trie.put("Same Document", "Same Document1");
        trie.put("Same Document", "Same Document2");
        List<String> array = new ArrayList<String>();
        array.add("Same Document1");
        array.add("Same Document2");
        Assert.assertEquals(array, trie.getAllWithPrefixSorted("Sam", getPrefixComparator("Sam")));
    }

    @Test
    public void putTheSameValueUpperCaseInGetAllWithPrefixSorted(){
        TrieImpl trie = new TrieImpl();
        trie.put("SAME DOCUMENT", "Same Document");
        trie.put("Same Document", "Same Document");
        List<String> array = new ArrayList<String>();
        array.add("Same Document");
        Assert.assertEquals(array, trie.getAllWithPrefixSorted("Sam", getPrefixComparator("Sam")));
    }



    /**
     * .deleteAllWithPrefix(String);
     * Delete the subtree rooted at the last character of the prefix.
     * Search is CASE INSENSITIVE.
     * @return a Set of all Values that were deleted.
     */

    @Test
    public void deleteAllWithPrefixTrieTest() {
        TrieImpl trie = new TrieImpl();
        trie.put("Pickle Rick pickle pick", "Pickle Rick pickle pick");
        trie.put("Mr Meeseeks", "Mr Meeseeks");
        trie.put("Pickles", "Pickles");
        trie.put("picked apart pickles", "picked apart pickles");
        List<String> strings = new ArrayList<String>();
        strings.add("Pickle Rick pickle pick");
        strings.add("picked apart pickles");
        strings.add("Pickles");
        Assert.assertEquals(strings, trie.getAllWithPrefixSorted("pic", getPrefixComparator("pic")));

        Set<String> deleted = new HashSet<String>();
        deleted.add("Pickle Rick pickle pick");
        deleted.add("picked apart pickles");
        deleted.add("Pickles");

        Assert.assertEquals(deleted ,trie.deleteAllWithPrefix("pic"));

        strings.clear();
        Assert.assertEquals(strings, trie.getAllWithPrefixSorted("pic", getPrefixComparator("pic")));
    }

    @Test
    public void testNullKeyDeleteAllWithPrefixSorted(){
        TrieImpl trie = new TrieImpl();
        Assert.assertEquals(new HashSet(), trie.deleteAllWithPrefix(null));
    }

    /**
     * .deleteAll(String);
     * Delete all values from the node of the given key (do not remove the values from other nodes in the Trie)
     * @return a Set of all Values that were deleted.
     */

    @Test
    public void deleteAllInTrieTest() {
        TrieImpl trie = new TrieImpl();
        trie.put("Pickle Rick pickle pick", "Pickle Rick pickle pick");
        trie.put("Mr Meeseeks", "Mr Meeseeks");
        trie.put("Pickle", "Pickles");
        trie.put("picked apart pickles", "picked apart pickle");
        List<String> strings = new ArrayList<String>();
        strings.add("Pickle Rick pickle pick");
        strings.add("Pickles");
        Assert.assertEquals(strings, trie.getAllSorted("pickle", getPrefixComparator("pickle")));

        Set<String> deleted = new HashSet<String>();
        deleted.add("Pickle Rick pickle pick");
        deleted.add("Pickles");

        Assert.assertEquals(deleted, trie.deleteAll("pickle"));

        strings.clear();
        Assert.assertEquals(strings, trie.getAllSorted("pickle", getPrefixComparator("pickle")));
    }

    @Test
    public void testNullKeyDeleteAll(){
        TrieImpl trie = new TrieImpl();
        Assert.assertEquals(new HashSet(), trie.deleteAll(null));
    }

    /**
     * .delete(String, Value);
     * Remove the given value from the node of the given key (do not remove the value from other nodes in the Trie)
     * @return the value which was deleted. If the key did not contain the given value, return null.
     */

    @Test
    public void deleteInTrieTest() {
        TrieImpl trie = new TrieImpl();
        trie.put("This document one", "This document one");
        trie.put("This is document two", "This is document two");
        trie.put("This is document three", "This is document three");
        Assert.assertEquals("This is document two", trie.delete("two", "two"));
        List<String> strings = new ArrayList<String>();
        strings.add("This document one");
        strings.add("This is document two");
        strings.add("This is document three");
        Assert.assertEquals(strings, trie.getAllSorted("This", getPrefixComparator("This")));
        Assert.assertEquals(new ArrayList<>(), trie.getAllSorted("two", getPrefixComparator("two")));

    }

    @Test
    public void testDeleteNullValue(){
        TrieImpl trie = new TrieImpl();
        Assert.assertNull(trie.delete("null", null));
    }

    @Test
    public void testDeleteNullKey(){
        TrieImpl trie = new TrieImpl();
        Assert.assertNull(trie.delete(null, "null"));
    }

    //comparators
    private Comparator getPrefixComparator(String prefix) {
        Comparator<String> comparator = (o1, o2) -> {
            int document1LetterCount = 0;
            String[] prefixes1 = o1.split(" ");
            for (String s : prefixes1) {
                if (s.contains(prefix)) {
                    document1LetterCount++;
                }
            }

            int document2LetterCount = 0;
            String[] prefixes2 = o2.split(" ");
            for (String s : prefixes2) {
                if (s.contains(prefix)) {
                    document2LetterCount++;
                }
            }
            if (document1LetterCount == document2LetterCount) {
                return 0;
            } else if (document1LetterCount < document2LetterCount) {
                return 1;
            } else
                return -1;
        };
        return comparator;
    }
}