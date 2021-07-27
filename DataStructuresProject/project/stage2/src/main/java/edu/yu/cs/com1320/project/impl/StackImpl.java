package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.Stack;

public class StackImpl<T> implements Stack<T> {

    Node root;
    private int size;

    public StackImpl(){ this.root = null; }

    @Override
    public void push(T element) {
        if(inputIsInvalid(element)) return;
        if(isFirstElement()) this.root = new Node(element);
        else addNode(element);
        this.size++;
    }

    @Override
    public T pop() {
        Node popped = this.root; //pop the top off
        if(hasNextNode(this.root)) this.root = this.root.next;
        else this.root = null;
        this.size--;
        return popped.data;
    }

    @Override
    public T peek() {
        if(isFirstElement()) return null;
        return this.root.data;
    }

    @Override
    public int size() {
        return this.size;
    }

    private class Node{
        private T data;
        private Node next;
        private Node(T data){ this.data = data; }
    }
    private void addNode(T element){
        Node temp = this.root;
        this.root = new Node(element);
        this.root.next = temp;
    }
    private boolean hasNextNode(Node node){ return node.next != null; }
    private boolean isFirstElement(){ return this.root == null; }
    private boolean inputIsInvalid(T element){ return element == null; }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        Stack<T> tmp = new StackImpl<>();
        int index = this.size;

        while (this.size > 0){
            T content = pop();
            tmp.push(content);
            stringBuilder.append(index--).append(": ").append(content).append("\n");
        }
        while (tmp.size() > 0){ push(tmp.pop()); }
        return stringBuilder.toString();
    }
}
