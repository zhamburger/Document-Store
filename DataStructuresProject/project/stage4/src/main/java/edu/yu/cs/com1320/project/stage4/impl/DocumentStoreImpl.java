package edu.yu.cs.com1320.project.stage4.impl;

import edu.yu.cs.com1320.project.Stack;
import edu.yu.cs.com1320.project.*;
import edu.yu.cs.com1320.project.impl.HashTableImpl;
import edu.yu.cs.com1320.project.impl.MinHeapImpl;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.impl.TrieImpl;
import edu.yu.cs.com1320.project.stage4.Document;
import edu.yu.cs.com1320.project.stage4.DocumentStore;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

public class DocumentStoreImpl implements DocumentStore {

    /*Stores Internals*/
    private HashTable<URI, Document> documents;
    private Stack<Undoable> commandStack;
    private Trie<Document> documentTrie;
    private MinHeap<Document> memoryHeap;

    /*Memory Management*/
    private int size;
    private int byteCount;
    private int maxDocumentCount;
    private int maxByteCount;

    /*Constructor*/
    public DocumentStoreImpl() {
        this.documents = new HashTableImpl<>();
        this.commandStack = new StackImpl<>();
        this.documentTrie = new TrieImpl<>();
        this.memoryHeap = new MinHeapImpl<>();

        this.maxDocumentCount = 0; //temporary until user inputs one
        this.maxByteCount = 0; //temporary until user inputs one
        this.size = 0;
        this.byteCount = 0;
    }

    /*Public API*/
    @Override
    public int putDocument(InputStream input, URI uri, DocumentFormat format) {
        if (inputIsInvalid(input, format, uri)) throw new IllegalArgumentException();
        if(documentShouldBeDeleted(uri)) {
            deleteDocument(uri);
            return 0;
        }
        try {
            Document document = initializeDocument(input, uri, format);
            if(document == null) return 0;
            //add the action to the stack
            if(documentExistsInStore(uri)) controlStack("replace", this.documents.get(uri));
            else controlStack("add", document);
            addDocumentToStore(uri, document);
            mapIntoTrie(document.getDocumentAsTxt(), document);
            return document.getDocumentAsTxt().hashCode();
        }catch (IOException e){
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public byte[] getDocumentAsPdf(URI uri) {
        if(!documentExistsInStore(uri)) return null;
        return this.documents.get(uri).getDocumentAsPdf();
    }

    @Override
    public String getDocumentAsTxt(URI uri) {
        if(!documentExistsInStore(uri)) return null;
        return this.documents.get(uri).getDocumentAsTxt();
    }

    @Override
    public boolean deleteDocument(URI uri) {
        if(!documentExistsInStore(uri)) return false;
        controlStack("delete", this.documents.get(uri));
        this.documents.put(uri, null);
        decrementSizeOfStore();
        return true;
    }

    @Override
    public void undo() throws IllegalStateException {
        if(this.commandStack.size() <= 0) throw new IllegalStateException("no command to undo");
        if(this.commandStack.peek() instanceof CommandSet) ((CommandSet)this.commandStack.pop()).undoAll();
        else this.commandStack.pop().undo();
    }

    @Override
    public void undo(URI uri) throws IllegalStateException {
        if(this.commandStack.size() <= 0) throw new IllegalStateException("no command to undo");
        Undoable commandToUndo = getSpecifiedCommandToUndo(uri);
        if(commandToUndo instanceof CommandSet) ((CommandSet)commandToUndo).undo(uri);
        else ((GenericCommand)commandToUndo).undo();
    }

    @Override
    public List<String> search(String keyword) {
        if(isInvalidString(keyword)) return new ArrayList<>();
        List<Document> documents = this.documentTrie.getAllSorted(keyword, getWordComparator(keyword));
        reheapifyDocuments(documents);
        return addDocumentStringToList(documents);
    }

    @Override
    public List<byte[]> searchPDFs(String keyword) {
        if(isInvalidString(keyword)) return new ArrayList<>();
        List<Document> documents = this.documentTrie.getAllSorted(keyword, getWordComparator(keyword));
        reheapifyDocuments(documents);
        return addDocumentBytesToList(documents);
    }

    @Override
    public List<String> searchByPrefix(String keywordPrefix) {
        if(isInvalidString(keywordPrefix)) return new ArrayList<>();
        List<Document> documents = this.documentTrie.getAllWithPrefixSorted(keywordPrefix, getWordComparator(keywordPrefix));
        reheapifyDocuments(documents);
        return addDocumentStringToList(documents);
    }

    @Override
    public List<byte[]> searchPDFsByPrefix(String keywordPrefix) {
        if(isInvalidString(keywordPrefix)) return new ArrayList<>();
        List<Document> documents = this.documentTrie.getAllWithPrefixSorted(keywordPrefix, getWordComparator(keywordPrefix));
        reheapifyDocuments(documents);
        return addDocumentBytesToList(documents);
    }

    @Override
    public Set<URI> deleteAll(String keyword) {
        if(isInvalidString(keyword)) return new HashSet<>();
        Set<Document> documents = this.documentTrie.deleteAll(keyword);
        deleteDocumentThenAddCommandSetToStack(documents);
        return addDocumentsUrisToSet(documents);
    }

    @Override
    public Set<URI> deleteAllWithPrefix(String keywordPrefix) {
        if(isInvalidString(keywordPrefix)) return new HashSet<>();
        Set<Document> documents = this.documentTrie.deleteAllWithPrefix(keywordPrefix);
        deleteDocumentThenAddCommandSetToStack(documents);
        return addDocumentsUrisToSet(documents);
    }

    @Override
    public void setMaxDocumentCount(int limit) {
        if(invalidLimit(limit)) return;
        this.maxDocumentCount = limit;
    }

    @Override
    public void setMaxDocumentBytes(int limit) {
        if(invalidLimit(limit)) return;
        this.maxByteCount = limit;
    }


    /*Creating Documents*/
    private Document initializeDocument(InputStream input, URI uri, DocumentFormat format) throws IOException {
        if(isAPdfFile(format)) return createPdfDocument(input, uri);
        if(isATxtFile(format)) return createTxtDocument(input, uri);
        return null;
    }
    private Document createTxtDocument(InputStream input, URI uri) throws IOException {
        String documentContents = new String(turnInputStreamToByteArray(input), StandardCharsets.UTF_8);
        Document doc =  new DocumentImpl(uri, documentContents, documentContents.hashCode());
        mapIntoTrie(documentContents, doc);
        return doc;
    }
    private Document createPdfDocument(InputStream input, URI uri) throws IOException {
        byte[] byteContents = turnInputStreamToByteArray(input);
        PDDocument document = PDDocument.load(byteContents);
        String documentContents = getTextFromPdfDocument(document);
        Document doc = new DocumentImpl(uri, documentContents, documentContents.hashCode(), byteContents);
        mapIntoTrie(documentContents, doc);
        return doc;
    }
    private String getTextFromPdfDocument(PDDocument document) throws IOException {
        PDFTextStripper pdfTextStripper = new PDFTextStripper();
        return  pdfTextStripper.getText(document).trim();
    }
    private byte[] turnInputStreamToByteArray(InputStream inputStream) throws IOException {
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        byte[] bytes = new byte[bufferedInputStream.available()];
        bufferedInputStream.read(bytes);
        return bytes;
    }

    /*META*/
    private void removeAllTracesOfDocument(Document document){
        //remove from hashmap
        this.documents.put(document.getKey(), null);
        //remove all searching from trie
        this.documentTrie.delete(document.getDocumentAsTxt(), document);
        //remove all previously stored operations on the document
        this.getSpecifiedCommandToUndo(document.getKey());
    }

    /*HashMap*/
    private Document addDocumentToStore(URI uri, Document document){
        updateDocumentTime(document);
        this.memoryHeap.insert(document);
        incrementCounters(document);

        if(thereIsADocumentLimit() && exceedsDocumentLimit()) removeFromHeap();
        else if(thereIsAByteLimit() && exceedsByteLimit()){
            DocumentImpl removed = (DocumentImpl) this.memoryHeap.removeMin();
            decrementCounters(document);
            while(this.byteCount > this.maxByteCount){
                removeFromHeap();
            }
            this.removeAllTracesOfDocument(removed);
        }
        this.memoryHeap.reHeapify(document);
        return this.documents.put(uri, document);
    }


    /*Command Stack*/
    private void controlStack(String action, Document document){
        Undoable prev = getSpecifiedCommandToUndo(document.getKey()); //gets rid of the previous commands of that document
        switch (action){
            case "add":
                this.commandStack.push(new GenericCommand<URI>(document.getKey(), getUndoAdd()));
                break;
            case "delete":
                this.commandStack.push(new GenericCommand<URI>(document.getKey(), getUndoDelete(document)));
                break;
            case "replace":
                this.commandStack.push(new GenericCommand<URI>(document.getKey(), getReplacedValue(document)));
                break;
        }
    }
    private Undoable getSpecifiedCommandToUndo(URI uri){
        Stack<Undoable> temporaryStack = new StackImpl<>();
        Undoable currentCommand;
        Undoable found = null;
        while(this.commandStack.size() > 0){
            currentCommand = this.commandStack.pop();

            if(currentCommand instanceof GenericCommand){
                if(((GenericCommand) currentCommand).getTarget() == uri){
                    found = currentCommand;
                    break;
                }
            } else if(currentCommand instanceof CommandSet){
                if(((CommandSet) currentCommand).containsTarget(uri)){
                    found = currentCommand;
                    break;
                }
            }
            temporaryStack.push(currentCommand);
        }
        pushTemporaryStackBack(temporaryStack);
        return found;
    }
    private void pushTemporaryStackBack(Stack<Undoable> temporaryStack){
        while (temporaryStack.size() > 0){
            this.commandStack.push(temporaryStack.pop());
        }
    }
    private void deleteDocumentThenAddCommandSetToStack(Set<Document> documents){
        CommandSet<URI> commandSet = new CommandSet<>();
        documents.forEach((document) -> commandSet.addCommand(new GenericCommand<>(document.getKey(), getUndoDelete(document))));
        this.commandStack.push(commandSet);

        for(Document document : documents){
            this.documents.put(document.getKey(), null);
            decrementSizeOfStore();
        }
    }


    /*Trie*/
    private void mapIntoTrie(String content, Document document){ this.documentTrie.put(content, document); }


    /*MinHeap*/
    private boolean invalidLimit(int limit){ return limit <= 0; }
    private void removeFromHeap(){
        Document removed = this.memoryHeap.removeMin();
        removeAllTracesOfDocument(removed);
        decrementCounters(removed);
    }
    private void reheapifyDocuments(List<Document> documents){
        for(Document doc : documents) this.memoryHeap.reHeapify(doc);
    }


    /*Helper Methods*/
    private Set<URI> addDocumentsUrisToSet(Set<Document> documents){
        Set<URI> setOfUri = new HashSet<>();
        for(Document doc : documents) setOfUri.add(doc.getKey());
        return setOfUri;
    }
    private List<String> addDocumentStringToList(List<Document> documents){
        List<String> listOfTxts = new ArrayList<>();
        for(Document docs : documents) listOfTxts.add(docs.getDocumentAsTxt());
        return listOfTxts;
    }
    private List<byte[]> addDocumentBytesToList(List<Document> documents){
        List<byte[]> listOfBytes = new ArrayList<>();
        for(Document docs : documents) listOfBytes.add(docs.getDocumentAsPdf());
        return listOfBytes;
    }


    /*Functions*/
    private Function<URI, Boolean> getUndoAdd() {
        return (URI uri) -> {
            this.documents.put(uri, null);
            decrementSizeOfStore();
            return true;
        };
    }
    private Function<URI, Boolean> getUndoDelete(Document documentToBeDeleted) {
        return (URI uri) -> {
            this.documents.put(uri, documentToBeDeleted);
            mapIntoTrie(documentToBeDeleted.getDocumentAsTxt(), documentToBeDeleted);
            incrementSizeOfStore();
            return true;
        };
    }
    private Function<URI, Boolean> getReplacedValue(Document documentToBeReplaced){
        return (URI uri) -> {
            this.documents.put(uri, documentToBeReplaced);
            return true;
        };
    }


    /*Store Values Manipulation*/
    private void incrementSizeOfStore(){ this.size++; }
    private void decrementSizeOfStore(){ this.size--; }
    private void incrementByteCount(Document document){ this.byteCount += document.getDocumentAsPdf().length + document.getDocumentAsTxt().getBytes().length; }
    private void decrementByteCount(Document document){ this.byteCount -= document.getDocumentAsPdf().length + document.getDocumentAsTxt().getBytes().length; }
    private void incrementCounters(Document document){
        incrementByteCount(document);
        incrementSizeOfStore();
    }
    private void decrementCounters(Document document){
        decrementByteCount(document);
        decrementSizeOfStore();
    }
    private void updateDocumentTime(Document document){ document.setLastUseTime(System.nanoTime()); }
    private boolean thereIsADocumentLimit(){ return this.maxDocumentCount > 0; }
    private boolean thereIsAByteLimit(){ return this.maxByteCount > 0; }
    private boolean exceedsDocumentLimit(){ return this.size > this.maxDocumentCount; }
    private boolean exceedsByteLimit(){ return this.byteCount > this.maxByteCount; }


    /*Comparators*/
    private Comparator getWordComparator(String keyword){
        Comparator<DocumentImpl> comparator = (o1, o2) -> {
            if (o1.wordCount(keyword) == o2.wordCount(keyword)){
                return 0;
            }
            else if (o1.wordCount(keyword) < o2.wordCount(keyword)){
                return 1;
            }
            else
                return -1;
        };
        return comparator;
    }


    /*Input Checks*/
    private boolean documentExistsInStore(URI uri) { return this.documents.get(uri) != null; }
    private boolean documentShouldBeDeleted(URI uri){ return uri == null; }
    private boolean inputIsInvalid(InputStream input, DocumentFormat format, URI uri){ return !isAValidFormat(format) || !URIExists(uri) || !inputStreamExists(input); }
    private boolean isAValidFormat(DocumentFormat format){ return isATxtFile(format) || isAPdfFile(format);}
    private boolean isATxtFile(DocumentFormat format){ return format == DocumentFormat.TXT; }
    private boolean isAPdfFile(DocumentFormat format){ return format == DocumentFormat.PDF; }
    private boolean URIExists(URI uri){ return uri != null; }
    private boolean inputStreamExists(InputStream input){ return input != null; }
    private boolean isInvalidString(String s){ return s == null; }


    /*For Testing Purposes*/
    public Document getDocument(URI uri){ return this.documents.get(uri); }
}
