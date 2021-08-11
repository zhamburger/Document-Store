package edu.yu.cs.com1320.project.impl;


import edu.yu.cs.com1320.project.Trie;
import edu.yu.cs.com1320.project.stage5.Document;
import edu.yu.cs.com1320.project.stage5.impl.DocumentImpl;

import java.net.URI;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TrieImpl<Value> implements Trie<Value> {

    private final int alphabetSize = 256; // extended ASCII
    private Node root;
    protected Function<URI, Document> function;

    protected void setFunction(Function<URI, Document> function){
        this.function = function;
    }

    public TrieImpl() {
    }

    @Override
    public void put(String key, Value val) {
        if(inputIsInvalid(key, val)) return;
        for(String word : getStringArrayOfWords(key)) this.root = recursivelyPut(this.root, word, val, 0);
    }

    @Override
    public List<Value> getAllSorted(String key, Comparator<Value> comparator) {
        if(getInputIsInvalid(key, comparator)) return new ArrayList<>();
        Node documents = this.recursivelyGet(this.root, key, 0);
        if (wordDoesntExist(documents)) return new ArrayList<>();
        documents.documents.sort(comparator);
        //update document times when searched for
        for(Object val : documents.documents) if(val instanceof URI) this.function.apply((URI) val).setLastUseTime(System.nanoTime());
        return documents.documents;
    }

    @Override
    public List<Value> getAllWithPrefixSorted(String prefix, Comparator<Value> comparator) {
        if(getInputIsInvalid(prefix, comparator)) return new ArrayList<>();
        List<Value> documents = new ArrayList<>();
        collectDocuments(this.root, prefix, 0, documents);
        documents.sort(comparator);
        //update document times when searched for
        for(Object val : documents) if(val instanceof URI) this.function.apply((URI) val).setLastUseTime(System.nanoTime());
        return documents;
    }

    @Override
    public Set<Value> deleteAllWithPrefix(String prefix) {
        if(!isValidKey(prefix)) return new HashSet<>();

        List<Value> prefixList = new ArrayList<>();
        collectDocuments(this.root, prefix, 0, prefixList);
        for(Value val : prefixList){

            if(val instanceof String) {
                for(String word : getStringArrayOfWords((String) val)){
                    deleteAll(word);
                }
            }
            else if(val instanceof URI){
                for(String word : getStringArrayOfWords(this.function.apply((URI) val).getDocumentAsTxt())){
                    deleteAll(word);
                }
            }
        }
        return new HashSet<>(prefixList);
    }

    @Override
    public Set<Value> deleteAll(String key) {
        if(!isValidKey(key)) return new HashSet<>();
        Set<Value> documents = new HashSet<>();
        Node value = recursivelyGet(this.root, key, 0);
        if(wordDoesntExist(value)) return new HashSet<>();
        Iterator iterator = value.documents.iterator();
        while(iterator.hasNext()){
            documents.add((Value) iterator.next());
            iterator.remove();
        }
        return documents;
    }

    @Override
    public Value delete(String key, Value val) {
        if (inputIsInvalid(key, val)) return null;
        for(String n : getStringArrayOfWords(key)){
            List documents = Objects.requireNonNull(recursivelyGet(this.root, n, 0)).documents;
            for(Object value : documents) return deleteValue(documents, value, val);
        }
        return null;
    }


    /*Internal Node*/
    private class Node<Value>{
        private List<Value> documents = new ArrayList<>();
        private Node[] proceedingCharacters = new Node[alphabetSize];
        private boolean isWord = false;
    }

    /*Tree Operations*/
    private Node recursivelyPut(Node currentWord, String key, Value val, int depth){
        if (wordDoesntExist(currentWord)) currentWord = new Node();
        if (reachedTheDepthOfTheWord(key, depth)){
            if(!currentWord.documents.contains(val)) currentWord.documents.add(val);
            currentWord.isWord = true;
            return currentWord;
        }
        char c = key.charAt(depth);
        currentWord.proceedingCharacters[indexConversion(c)] = this.recursivelyPut(currentWord.proceedingCharacters[indexConversion(c)], key, val, depth + 1);
        return currentWord;
    }
    private Node recursivelyGet(Node currentWord, String key, int depth) {
        if (wordDoesntExist(currentWord)) return null;
        if (reachedTheDepthOfTheWord(key, depth)) return currentWord;
        char c = key.charAt(depth);
        int index = indexConversion(c);
        if(characterIsASpace(c)) return currentWord;
        return this.recursivelyGet(currentWord.proceedingCharacters[index], key, depth + 1);
    }
    private void collectDocuments(Node currentWord, String key, int depth, List<Value> list){
        if (wordDoesntExist(currentWord)) return;
        if (reachedTheDepthOfTheWord(key, depth)) {
            list.addAll((List<Value>) currentWord.documents.stream()
                                        .filter(i -> !list.contains(i))
                                        .collect(Collectors.toList()));
            for (int i = 0; i < currentWord.proceedingCharacters.length; i++) {
                this.collectDocuments(currentWord.proceedingCharacters[i], key, depth + 1, list);
            }
        }
        else {
            char c = key.charAt(depth);
            int index = indexConversion(c);
            this.collectDocuments(currentWord.proceedingCharacters[index], key, depth + 1, list);
        }
    }

    /*Helper Functions*/
    private String[] getStringArrayOfWords(String documentText){
        documentText = documentText.replaceAll("[@#$%^&()_<>+*{}|';!?,:.-]", "");
        documentText = documentText.toLowerCase();
        return documentText.split(" ");
    }
    private int indexConversion(char c){
        int index = 0;
        if(Character.isDigit(c)){
            index = (int) c + Character.getNumericValue('z');
        }else{
            index = Character.getNumericValue(c) - Character.getNumericValue('a');
        }
        return index;
    }
    private Value removeValue(List list, Value value){
        list.remove(value);
        return value;
    }
    private Value deleteValue(List documents, Object value, Value val){
        if(value instanceof String && val instanceof String){
            if(((String) value).contains((String)val)) return removeValue(documents, (Value) value);

        }else if(value instanceof URI || val instanceof URI) {
            if(value == val) return removeValue(documents, (Value) value);
        }
        return null;
    }

    /*Argument Checks*/
    private boolean inputIsInvalid(String key, Value val){ return !isValidKey(key) || !isValidValue(val); }
    private boolean isValidKey(String key){ return key != null; }
    private boolean isValidValue(Value val){ return val != null; }
    private boolean reachedTheDepthOfTheWord(String key, int depth){ return depth >= key.length(); }
    private boolean wordDoesntExist(Node currentWord){ return currentWord == null; }
    private boolean characterIsASpace(char c){ return c == ' '; }
    private boolean getInputIsInvalid(String key, Comparator<Value> comparator){ return key == null || comparator == null; }
}
