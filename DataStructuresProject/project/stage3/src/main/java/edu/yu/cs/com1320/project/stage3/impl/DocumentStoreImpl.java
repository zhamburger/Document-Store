package edu.yu.cs.com1320.project.stage3.impl;

import edu.yu.cs.com1320.project.*;
import edu.yu.cs.com1320.project.Stack;
import edu.yu.cs.com1320.project.impl.HashTableImpl;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.impl.TrieImpl;
import edu.yu.cs.com1320.project.stage3.Document;
import edu.yu.cs.com1320.project.stage3.DocumentStore;
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
    private int size;

    /*Constructor*/
    public DocumentStoreImpl() {
        this.documents = new HashTableImpl<>();
        this.commandStack = new StackImpl<>();
        this.documentTrie = new TrieImpl<>();
        this.size = 0;
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
            if(documentExistsInStore(uri)) controlStack("replace", this.documents.get(uri));
            else controlStack("add", document);
            this.documents.put(uri, document);
            this.documentTrie.put(document.getDocumentAsTxt(), document);
            incrementSizeOfStore();
            if(document != null) return document.getDocumentAsTxt().hashCode();
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
        if(this.commandStack.peek() instanceof  CommandSet) ((CommandSet)this.commandStack.pop()).undoAll();
        else this.commandStack.pop().undo();
    }

    @Override
    public void undo(URI uri) throws IllegalStateException {
        if(this.commandStack.size() <= 0) throw new IllegalStateException("no command to undo");
        Undoable commandToUndo = getSpecifiedCommandToUndo(uri);
        if(commandToUndo instanceof  CommandSet) ((CommandSet)commandToUndo).undo(uri);
        else ((GenericCommand)commandToUndo).undo();
    }

    @Override
    public List<String> search(String keyword) {
        if(isInvalidString(keyword)) return new ArrayList<>();
        List<String> list = new ArrayList<>();
        List<Document> documents = this.documentTrie.getAllSorted(keyword, getWordComparator(keyword));
        documents.forEach((document)->list.add(document.getDocumentAsTxt()));
        return list;
    }

    @Override
    public List<byte[]> searchPDFs(String keyword) {
        if(isInvalidString(keyword)) return new ArrayList<>();
        List<byte[]> list = new ArrayList<>();
        List<Document> documents = this.documentTrie.getAllSorted(keyword, getWordComparator(keyword));
        documents.forEach((document)->list.add(document.getDocumentAsPdf()));
        return list;
    }

    @Override
    public List<String> searchByPrefix(String keywordPrefix) {
        if(isInvalidString(keywordPrefix)) return new ArrayList<>();
        List<String> list = new ArrayList<>();
        List<Document> documents = this.documentTrie.getAllWithPrefixSorted(keywordPrefix, getWordComparator(keywordPrefix));
        documents.forEach((document)->list.add(document.getDocumentAsTxt()));
        return list;
    }

    @Override
    public List<byte[]> searchPDFsByPrefix(String keywordPrefix) {
        if(isInvalidString(keywordPrefix)) return new ArrayList<>();
        List<byte[]> list = new ArrayList<>();
        List<Document> documents = this.documentTrie.getAllWithPrefixSorted(keywordPrefix, getWordComparator(keywordPrefix));
        documents.forEach((document)->list.add(document.getDocumentAsPdf()));
        return list;
    }

    @Override
    public Set<URI> deleteAll(String keyword) {
        if(isInvalidString(keyword)) return new HashSet<>();
        Set<URI> set = new HashSet<>();
        Set<Document> documents = this.documentTrie.deleteAll(keyword);
        deleteDocumentThenAddCommandSetToStack(documents);
        documents.forEach((document)->set.add(document.getKey()));
        return set;
    }

    @Override
    public Set<URI> deleteAllWithPrefix(String keywordPrefix) {
        if(isInvalidString(keywordPrefix)) return new HashSet<>();
        Set<URI> set = new HashSet<>();
        Set<Document> documents = this.documentTrie.deleteAllWithPrefix(keywordPrefix);
        deleteDocumentThenAddCommandSetToStack(documents);
        documents.forEach((document)->set.add(document.getKey()));
        return set;
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

    /*Trie Operations*/
    private void mapIntoTrie(String content, Document document){ this.documentTrie.put(content, document); }

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
    private void incrementSizeOfStore(){this.size++;}
    private void decrementSizeOfStore(){this.size--;}

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
