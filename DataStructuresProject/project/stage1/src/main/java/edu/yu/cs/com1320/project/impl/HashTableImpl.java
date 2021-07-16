package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.HashTable;

import java.util.Arrays;
import java.util.Objects;

public class HashTableImpl<Key, Value> implements HashTable<Key, Value> {

    private Node<Key, Value>[] contents;

    public HashTableImpl() {
        contents = new Node[5];
    }

    @Override
    public Value get(Key k) {
        Node node = this.contents[this.hashFunction(k)]; //get the mapped key,value
        if(node == null) return null; //if it doesnt exist
        if(node.key != k) node = this.getCollision(k); //if its a different key, we have a collision
        return (node == null) ? null : (Value) node.value;
    }

    @Override
    public Value put(Key k, Value v) {
        int index = this.hashFunction(k); //get the mapped index
        if(this.get(k) != null) return this.replaceValue(k,v); //if it exists, we have to replace the value and return old one
        if(v == null) deleteContent(k, v);
        if(this.contents[index] != null) this.handleCollision(index, k, v); //if it was a collision
        else this.contents[index] = new Node<>(k, v); // it was an empty bucket just make a new node
        return null;
    }

    //if a key mapped to an occupied bucket, then it puts on the next available chain
    private void handleCollision(int index, Key key, Value value){
        Node current = this.contents[index]; //get the indexed mapped to
        while(current.next != null) current = current.next; //get the next available
        current.next = new Node<>(key, value); //set the new node in the available spot
    }

    //replaces the old value of a key with the given new one
    private Value replaceValue(Key key, Value value){
        Node found = this.getCollision(key);
        Value old = (Value) found.value;
        if(contentIsToBeDeleted(value)) return deleteContent(key, value);
        else found.value = value;
        return old;
    }

    private Node getCollision(Key key){
        Node current = this.contents[this.hashFunction(key)];
        while(current != null) {
            if(current.key == key) return current; //if the key is there return it
            current = current.next; //go to the next one
        }
        return null;
    }

    private Value deleteContent(Key key, Value value){
        Node current = this.contents[this.hashFunction(key)];
        if(current.key == key){
            this.contents[this.hashFunction(key)] = null;
            return null;
        }
        Node previous = null;
        Node found = this.getCollision(key);
        if(found.next != null){
            found.key = found.next.key;
            found.value = found.next.value;
            found.next = found.next.next;
        }
        return null;
    }

    private int hashFunction(Key key){
        int hash = key.hashCode()  & 0xfffffff;
        hash %= this.contents.length;
        return hash;
    }


    private boolean contentIsToBeDeleted(Value v){ return v == null; }

    private class Node<Key, Value>{
        private Key key;
        private Value value;
        private Node<Key, Value> next;

        private Node(Key key, Value value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Node)) return false;
            Node<?, ?> node = (Node<?, ?>) o;
            return Objects.equals(key, node.key) &&
                    Objects.equals(value, node.value) &&
                    Objects.equals(next, node.next);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, value, next);
        }

        @Override
        public String toString() {
            return "Node{" +
                    "key=" + key +
                    ", value=" + value +
                    ", next=" + next +
                    '}';
        }
    }

}
