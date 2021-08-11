package edu.yu.cs.com1320.project.stage5.impl;

import edu.yu.cs.com1320.project.Stack;
import edu.yu.cs.com1320.project.*;
import edu.yu.cs.com1320.project.impl.BTreeImpl;
import edu.yu.cs.com1320.project.impl.MinHeapImpl;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.impl.TrieImpl;
import edu.yu.cs.com1320.project.stage5.Document;
import edu.yu.cs.com1320.project.stage5.DocumentStore;
import edu.yu.cs.com1320.project.stage5.PersistenceManager;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

public class DocumentStoreImpl implements DocumentStore {

    /*Stores Internals*/
    private subBTree<URI, Document> documents;
    private Stack<Undoable> commandStack;
    private subTrie<URI> documentTrie;
    private MinHeap<URI> memoryHeap;

    /*Memory Management*/
    private int size;
    private int byteCount;
    private int maxDocumentCount;
    private int maxByteCount;

    /*BTree Variables*/
    private File baseDir;

    /*Constructor*/
    public DocumentStoreImpl() {
        this.documents = new subBTree<>();
        this.commandStack = new StackImpl<>();
        this.documentTrie = new subTrie<>();
        this.memoryHeap = new MinHeapImpl<>();

        this.maxDocumentCount = 0; //temporary until user inputs one
        this.maxByteCount = 0; //temporary until user inputs one
        this.size = 0;
        this.byteCount = 0;

        this.documents.put(getSmallestURI(),null); //sentinel value
        this.documentTrie.setFunction(trieFunctionForBtree);
        setDocumentPersistenceManager();
    }

    public DocumentStoreImpl(File baseDir) {
        this.documents = new subBTree<>();
        this.commandStack = new StackImpl<>();
        this.documentTrie = new subTrie<>();
        this.memoryHeap = new MinHeapImpl<>();

        this.maxDocumentCount = 0; //temporary until user inputs one
        this.maxByteCount = 0; //temporary until user inputs one
        this.size = 0;
        this.byteCount = 0;

        this.baseDir = baseDir;
        this.documents.put(getSmallestURI(),null); //sentinel value
        this.documentTrie.setFunction(trieFunctionForBtree);
        setDocumentPersistenceManager();
    }


    /*Public API*/


    @Override
    public int putDocument(InputStream input, URI uri, DocumentFormat format) {
        if (inputIsInvalid(input, format, uri)) throw new IllegalArgumentException();
        try {
            Document document = initializeDocument(input, uri, format);
            if(document == null) return 0;
            //add "replace" or "add" action to stack
            if ((documentExistsInStore(uri))) controlStack("replace", this.documents.get(uri));
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
        List<Document> documents = addURIsToListOfDocuments(this.documentTrie.getAllSorted(keyword, getWordComparator(keyword)));
        reheapifyDocuments(documents);
        return addDocumentStringToList(documents);
    }

    @Override
    public List<byte[]> searchPDFs(String keyword) {
        if(isInvalidString(keyword)) return new ArrayList<>();
        List<Document> documents = addURIsToListOfDocuments(this.documentTrie.getAllSorted(keyword, getWordComparator(keyword)));
        reheapifyDocuments(documents);
        return addDocumentBytesToList(documents);
    }

    @Override
    public List<String> searchByPrefix(String keywordPrefix) {
        if(isInvalidString(keywordPrefix)) return new ArrayList<>();
        List<Document> documents = addURIsToListOfDocuments(this.documentTrie.getAllWithPrefixSorted(keywordPrefix, getWordComparator(keywordPrefix)));
        reheapifyDocuments(documents);
        return addDocumentStringToList(documents);
    }

    @Override
    public List<byte[]> searchPDFsByPrefix(String keywordPrefix) {
        if(isInvalidString(keywordPrefix)) return new ArrayList<>();
        List<Document> documents = addURIsToListOfDocuments(this.documentTrie.getAllWithPrefixSorted(keywordPrefix, getWordComparator(keywordPrefix)));
        reheapifyDocuments(documents);
        return addDocumentBytesToList(documents);
    }

    @Override
    public Set<URI> deleteAll(String keyword) {
        if(isInvalidString(keyword)) return new HashSet<>();
        Set<Document> documents = addSetOfURIsToSetOfDocuments(this.documentTrie.deleteAll(keyword));
        deleteDocumentThenAddCommandSetToStack(documents);
        return addDocumentsUrisToSet(documents);
    }

    @Override
    public Set<URI> deleteAllWithPrefix(String keywordPrefix) {
        if(isInvalidString(keywordPrefix)) return new HashSet<>();
        Set<Document> documents = addSetOfURIsToSetOfDocuments(this.documentTrie.deleteAllWithPrefix(keywordPrefix));
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
        return new DocumentImpl(uri, documentContents, documentContents.hashCode());
    }
    private Document createPdfDocument(InputStream input, URI uri) throws IOException {
        byte[] byteContents = turnInputStreamToByteArray(input);
        String documentContents = null;
        try(PDDocument document = PDDocument.load(byteContents)) {
            documentContents = getTextFromPdfDocument(document);
        }catch (IOException e){
            e.printStackTrace();
        }
        return new DocumentImpl(uri, documentContents, documentContents.hashCode(), byteContents);
    }
    private String getTextFromPdfDocument(PDDocument document) throws IOException {
        PDFTextStripper pdfTextStripper = new PDFTextStripper();
        return pdfTextStripper.getText(document).trim();
    }
    private byte[] turnInputStreamToByteArray(InputStream inputStream) throws IOException {
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        byte[] bytes = new byte[bufferedInputStream.available()];
        bufferedInputStream.read(bytes);
        bufferedInputStream.close();
        return bytes;
    }


    /*META*/


    private void removeAllTracesOfDocument(Document document){
        try {
            this.documents.moveToDisk(document.getKey());
        }catch(Exception e){
            e.printStackTrace();
        }
        //remove from hashmap
        this.documents.put(document.getKey(), null);
        //remove all searching from trie
        this.documentTrie.delete(document.getDocumentAsTxt(),document.getKey());
        //remove all previously stored operations on the document
        this.getSpecifiedCommandToUndo(document.getKey());
    }


    /*BTree*/


    private Document addDocumentToStore(URI uri, Document document){
        updateDocumentTime(document);
        incrementCounters(document);
        if(thereIsADocumentLimit() && exceedsDocumentLimit()) removeFromHeap();
        //since we are worried about the size of the document
        //if its very large we may have to remove a few documents to fit it into the store
        else if(thereIsAByteLimit() && exceedsByteLimit()){
            while(exceedsByteLimit()){
                removeFromHeap();
            }
        }
        //add the new document to memory
        addToHeap(uri);
        return this.documents.put(uri, document);
    }
    private URI getSmallestURI(){
        URI uri = null;
        try{
            uri = new URI("a");
        }catch(URISyntaxException ignored){ }
        return uri;
    }
    private Document getFromBTree(URI uri){
        Document document = this.documents.get(uri);
        if(document == null) document = this.documents.getDeserializedObject(uri);
        return document;
    }
    private Document getDocFromMemoryThenAddToStore(URI uri){
        Document document = this.documents.getDeserializedObject(uri);
        if(document == null) return null;
        addDocumentToStore(uri, document);
        return document;
    }
    private void setDocumentPersistenceManager(){
        PersistenceManager pm = new DocumentPersistenceManager(baseDir);
        this.documents.setPersistenceManager(pm);
    }


    /*Command Stack*/


    private void controlStack(String action, Document document){
        getSpecifiedCommandToUndo(document.getKey()); //gets rid of the previous commands of that document
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


    private void mapIntoTrie(String content, Document document){ this.documentTrie.put(content, document.getKey()); }



    /*MinHeap*/


    private boolean invalidLimit(int limit){ return limit <= 0; }
    private void removeFromHeap(){
        Document removed = getFromBTree(this.memoryHeap.removeMin());
        removeAllTracesOfDocument(removed);
        decrementCounters(removed);
    }
    private void reheapifyDocuments(List<Document> documents){
        for(Document doc : documents) this.memoryHeap.reHeapify(doc.getKey());
    }
    private void addToHeap(URI uri){
        this.memoryHeap.insert(uri);
        this.memoryHeap.reHeapify(uri);
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
    private List<Document> addURIsToListOfDocuments(List<URI> uris){
        List<Document> documents = new ArrayList<>();
        for(URI uri : uris){
            documents.add(getFromBTree(uri));
        }
        return documents;
    }
    private Set<Document> addSetOfURIsToSetOfDocuments(Set<URI> uris){
        Set<Document> documents = new HashSet<>();
        for(URI uri : uris){
            documents.add(getFromBTree(uri));
        }
        return documents;
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
            addDocumentToStore(uri, documentToBeDeleted);
            mapIntoTrie(documentToBeDeleted.getDocumentAsTxt(), documentToBeDeleted);
            return true;
        };
    }
    private Function<URI, Boolean> getReplacedValue(Document documentToBeReplaced){
        return (URI uri) -> {
            this.documents.put(uri, documentToBeReplaced);
            return true;
        };
    }
    private Function<URI, Document> trieFunctionForBtree = (URI uri) ->{
        if(this.documents.get(uri) == null){
            return this.getDocFromMemoryThenAddToStore(uri);
        }
        return this.documents.get(uri);
    };



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


    private Comparator<URI> getWordComparator(String keyword){
        return (o1, o2) -> {
            Document doc1  = null;
            Document doc2 = null;
            if(this.documents.get(o1) == null){
                doc1 = this.getDocFromMemoryThenAddToStore(o1);
            }else{
                doc1 = this.documents.get(o1);
            }
            if(this.documents.get(o2) == null){
                doc2 = this.getDocFromMemoryThenAddToStore(o2);
            }else{
                doc2 = this.documents.get(o2);
            }
            return Integer.compare(doc2.wordCount(keyword), doc1.wordCount(keyword));
        };
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



    /*Internal Classes*/


    protected class subTrie<Value> extends TrieImpl<Value>{
        @Override
        protected void setFunction(Function<URI, Document> function) {
            super.setFunction(function);
        }
    }
    protected class subBTree<Key extends Comparable<Key>, Value> extends BTreeImpl<Key, Value>{
        @Override
        protected PersistenceManager<Key, Value> getPM() {
            return super.getPM();
        }
        @Override
        protected Value getDeserializedObject(Key uri) {
            PersistenceManager pm = this.getPM();
            Object value = null;
            try {
                value = pm.deserialize(uri);
            }catch(IOException e){

            }
            return (Value) value;
        }
    }


    /*For Testing Purposes*/


    public Document getDocument(URI uri){ return this.documents.get(uri); }
}
