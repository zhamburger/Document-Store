package edu.yu.cs.com1320.project.stage4.impl;

import edu.yu.cs.com1320.project.impl.MinHeapImpl;
import edu.yu.cs.com1320.project.stage4.impl.DocumentImpl;
import org.junit.Assert;
import org.junit.Test;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;


public class MinHeapImplTest {

    ArrayList<DocumentImpl> docs = new ArrayList<>();

    public void makeDocuments(int amount, MinHeapImpl<Comparable> heap) throws URISyntaxException {
        String name = "document";
        for(int i = 0; i < amount; i++){
            String number = String.valueOf(i+1);
            String documentName = name+number;
            String uriName = documentName + ".uri";
            DocumentImpl document = new DocumentImpl(new URI(uriName), documentName, documentName.hashCode(), documentName.getBytes());
            docs.add(document);
            document.setLastUseTime(System.nanoTime());
            heap.insert(document);
        }
    }

    public DocumentImpl updateDocument(int index, MinHeapImpl<Comparable> heap){
        docs.get(index).setLastUseTime(System.nanoTime());
        heap.reHeapify(docs.get(index));
        return docs.get(index);
    }

    @Test
    public void testOneDocumentInHeap() throws URISyntaxException {
        MinHeapImpl<Comparable> heap = new MinHeapImpl<Comparable>();
        makeDocuments(1, heap);
        Assert.assertEquals(this.docs.get(0), heap.removeMin());
    }

    @Test
    public void testMultipleDocumentInHeap() throws URISyntaxException {
        MinHeapImpl<Comparable> heap = new MinHeapImpl<Comparable>();
        makeDocuments(90, heap);
        Assert.assertEquals(this.docs.get(0), heap.removeMin());
    }

    @Test
    public void testMultipleDocumentInHeapWithUpdates() throws URISyntaxException {
        MinHeapImpl<Comparable> heap = new MinHeapImpl<Comparable>();
        makeDocuments(4, heap);
        updateDocument(0, heap);
        Assert.assertEquals(this.docs.get(1), heap.removeMin());
    }

    @Test
    public void testOneUpdatedDocumentInHeap() throws URISyntaxException {
        MinHeapImpl<Comparable> heap = new MinHeapImpl<Comparable>();
        makeDocuments(50, heap);
        updateDocument(0, heap);
        updateDocument(1, heap);
        Assert.assertEquals(this.docs.get(2), heap.removeMin());
    }

    @Test
    public void testOneInMultipleUpdatedDocumentInHeap() throws URISyntaxException {
        MinHeapImpl<Comparable> heap = new MinHeapImpl<Comparable>();
        makeDocuments(4, heap);
        updateDocument(0, heap);
        updateDocument(1, heap);
        updateDocument(3, heap);
        Assert.assertEquals(this.docs.get(2), heap.removeMin());
    }

    @Test
    public void testMultipleInMultipleUpdatedDocumentInHeap() throws URISyntaxException {
        MinHeapImpl<Comparable> heap = new <Comparable>MinHeapImpl();
        makeDocuments(4, heap);
        updateDocument(0, heap);
        updateDocument(1, heap);
        updateDocument(3, heap);
        Assert.assertEquals(this.docs.get(2), heap.removeMin());
        Assert.assertEquals(this.docs.get(0), heap.removeMin());
    }
}