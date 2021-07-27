package edu.yu.cs.com1320.project.stage2.impl;

import edu.yu.cs.com1320.project.Command;
import edu.yu.cs.com1320.project.HashTable;
import edu.yu.cs.com1320.project.Stack;
import edu.yu.cs.com1320.project.impl.HashTableImpl;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.stage2.Document;
import edu.yu.cs.com1320.project.stage2.DocumentStore;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

public class DocumentStoreImpl implements DocumentStore {

    /*Stores Internals*/
    private HashTable<URI, Document> documents;
    private Stack<Command> commandStack;
    private int size;

    /*Constructor*/
    public DocumentStoreImpl() {
        this.documents = new HashTableImpl<>();
        this.commandStack = new StackImpl<>();
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
        this.commandStack.pop().undo();
    }

    @Override
    public void undo(URI uri) throws IllegalStateException {
        if(this.commandStack.size() <= 0) throw new IllegalStateException("no command to undo");
        Command commandToUndo = getSpecifiedCommandToUndo(uri);
        commandToUndo.undo();
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
        PDDocument document = PDDocument.load(byteContents);
        String documentContents = getTextFromPdfDocument(document);
        return new DocumentImpl(uri, documentContents, documentContents.hashCode(), byteContents);
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
        Command prev = getSpecifiedCommandToUndo(document.getKey()); //gets rid of the previous commands of that document
        switch (action){
            case "add":
                this.commandStack.push(new Command(document.getKey(), getUndoAdd()));
                break;
            case "delete":
                this.commandStack.push(new Command(document.getKey(), getUndoDelete(document)));
                break;
            case "replace":
                this.commandStack.push(new Command(document.getKey(), getReplacedValue(document)));
                break;
        }
    }
    private Command getSpecifiedCommandToUndo(URI uri){
        Stack<Command> temporaryStack = new StackImpl<>();
        Command currentCommand;
        Command found = null;
        while(this.commandStack.size() > 0){
            currentCommand = this.commandStack.pop();
            if(currentCommand.getUri() == uri) {
                found = currentCommand;
                break;
            }
            temporaryStack.push(currentCommand);
        }
        pushTemporaryStackBack(temporaryStack);
        return found;
    }
    private void pushTemporaryStackBack(Stack<Command> temporaryStack){
        while (temporaryStack.size() > 0){
            this.commandStack.push(temporaryStack.pop());
        }
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


    /*Input Checks*/
    private boolean documentExistsInStore(URI uri) { return this.documents.get(uri) != null; }
    private boolean documentShouldBeDeleted(URI uri){ return uri == null; }
    private boolean inputIsInvalid(InputStream input, DocumentFormat format, URI uri){ return !isAValidFormat(format) || !URIExists(uri) || !inputStreamExists(input); }
    private boolean isAValidFormat(DocumentFormat format){ return isATxtFile(format) || isAPdfFile(format);}
    private boolean isATxtFile(DocumentFormat format){ return format == DocumentFormat.TXT; }
    private boolean isAPdfFile(DocumentFormat format){ return format == DocumentFormat.PDF; }
    private boolean URIExists(URI uri){ return uri != null; }
    private boolean inputStreamExists(InputStream input){ return input != null; }


    /*For Testing Purposes*/
    public Document getDocument(URI uri){ return this.documents.get(uri); }
}
