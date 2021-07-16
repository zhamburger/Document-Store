package edu.yu.cs.com1320.project.stage1.impl;

import edu.yu.cs.com1320.project.HashTable;
import edu.yu.cs.com1320.project.impl.HashTableImpl;
import edu.yu.cs.com1320.project.stage1.Document;
import edu.yu.cs.com1320.project.stage1.DocumentStore;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class DocumentStoreImpl implements DocumentStore {

    private HashTable<URI, Document> documents;

    public DocumentStoreImpl() {
        this.documents = new HashTableImpl<>();
    }

    @Override
    public int putDocument(InputStream input, URI uri, DocumentFormat format) {
        if (inputIsInvalid(input, format, uri)) throw new IllegalArgumentException();
        if(documentShouldBeDeleted(uri)) deleteDocument(uri);
        try {
            Document document = initializeDocument(input, uri, format);
            this.documents.put(uri, document);
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
        this.documents.put(uri, null);
        return true;
    }

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

    /*Input Checks*/
    private boolean documentExistsInStore(URI uri) { return this.documents.get(uri) != null; }
    private boolean documentShouldBeDeleted(URI uri){ return uri == null; }
    private boolean inputIsInvalid(InputStream input, DocumentFormat format, URI uri){ return !isAValidFormat(format) || !URIExists(uri) || !inputStreamExists(input); }
    private boolean isAValidFormat(DocumentFormat format){ return isATxtFile(format) || isAPdfFile(format);}
    private boolean isATxtFile(DocumentFormat format){ return format == DocumentFormat.TXT; }
    private boolean isAPdfFile(DocumentFormat format){ return format == DocumentFormat.PDF; }
    private boolean URIExists(URI uri){ return uri != null; }
    private boolean inputStreamExists(InputStream input){ return input != null; }
}
