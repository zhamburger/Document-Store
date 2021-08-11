package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.MinHeap;

import java.util.Arrays;
import java.util.HashMap;

public class MinHeapImpl<E extends Comparable> extends MinHeap<E> {

    public MinHeapImpl() {
        super.elements = (E[]) new Comparable[2];
        super.count = 0;
        super.elementsToArrayIndex = new HashMap<>();
    }

    @Override
    public void reHeapify(E element) {
        int index = this.elementsToArrayIndex.get(element);
        if(!isLeaf(index)) downHeap(index);
        else if(isLeaf(index)) upHeap(index);
    }

    @Override
    public void insert(E x) {
        super.insert(x);
        this.elementsToArrayIndex.put(x,this.getArrayIndex(x));
    }


    @Override
    public E removeMin() {
        E index = super.removeMin();
        this.elementsToArrayIndex.remove(index);
        return index;
    }

    @Override
    protected void downHeap(int k){ super.downHeap(k); }

    @Override
    protected void swap(int i, int j) {
        super.swap(i, j);
        if(isAValidIndex(i)) this.elementsToArrayIndex.put(this.elements[i], i);
        if(isAValidIndex(j)) this.elementsToArrayIndex.put(this.elements[j], j);
    }

    @Override
    protected int getArrayIndex(E element) {
        for (int i = 1; i < this.elements.length; i++) {
            if(this.elements[i] == null) continue;
            if (this.elements[i].equals(element)) return i;
        }
        return 0;
    }

    @Override
    protected void doubleArraySize() {
        super.elements = Arrays.copyOf(super.elements, super.elements.length * 2);
    }

    private E getRightChild(int parent){
        if(rightExceedsTheLimit(parent)) return null;
        return this.elements[getRightChildIndex(parent)];
    }
    private E getLeftChild(int parent){
        if(leftChildCantBeMade(parent)) return null;
        return this.elements[getLeftChildIndex(parent)];
    }
    private int getLeftChildIndex(int parent){return 2*parent;}
    private int getRightChildIndex(int parent){return 2*parent+1;}
    private boolean leftChildCantBeMade(int parent){ return leftExceedsTheLimit(parent) || leftHasNoParent(parent); }
    private boolean leftHasNoParent(int parent){ return this.elements[getLeftChildIndex(parent)] == null; }
    private boolean leftExceedsTheLimit(int parent){ return getLeftChildIndex(parent) > this.count; }
    private boolean rightExceedsTheLimit(int parent){ return getRightChildIndex(parent) > this.count; }
    private boolean isLeaf(int parent){ return getLeftChild(parent) == null || getRightChild(parent) == null; }
    private boolean isAValidIndex(int index){ return this.elements[index] != null && index > 0; }
}
