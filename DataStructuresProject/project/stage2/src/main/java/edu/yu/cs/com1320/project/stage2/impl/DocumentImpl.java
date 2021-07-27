package edu.yu.cs.com1320.project.stage2.impl;

import edu.yu.cs.com1320.project.stage2.Document;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.*;
import java.net.URI;

public class DocumentImpl implements Document {
    private String documentAsText;
    private byte[] documentAsByteArray;
    private URI uri;
    private int txtHash;

    public DocumentImpl(URI uri, String text, int txtHash) {
        this.documentAsText = text;
        this.uri = uri;
        this.txtHash = txtHash;
    }

    public DocumentImpl(URI uri, String text, int txtHash, byte[] byteArray) {
        this.documentAsByteArray = byteArray;
        this.uri = uri;
        this.documentAsText = text;
        this.txtHash = txtHash;
    }



    public byte[] getDocumentAsPdf() { //need to use pdfbox to turn this into a pdf
        if(this.documentAsByteArray == null){
            return getDocAsPDF();
        }
        return this.documentAsByteArray; //return byte array
    }

    public String getDocumentAsTxt() {
        return this.documentAsText;
    }

    public int getDocumentTextHashCode() {
        return this.txtHash;
    }

    public URI getKey() {
        return this.uri;
    }

    @Override
    public int hashCode() {
        char[] stringToArray = documentAsText.toCharArray();
        int hash = 1;
        for (char c : stringToArray) {
            hash += c * 3;
        }
        hash *= 31;
        int num = 5;
        num %= txtHash;
        hash+=num;
        return hash;
    }

    /*
     * Private methods
     */

    private byte[] getByteArray(InputStream input) throws IOException {
        BufferedInputStream bf = new BufferedInputStream(input);
        byte[] newByteArray = new byte[bf.available()];
        bf.read(newByteArray);
        return newByteArray;
    }

    private byte[] getDocAsPDF() {
        byte[] byteArrayPDF = null;
        try {
            PDDocument document = new PDDocument(); //new PDDocument
            PDPage page = new PDPage(); //new page
            document.addPage(page); //add page
            PDFont font = PDType1Font.HELVETICA_BOLD; //font
            PDPageContentStream contentStream = new PDPageContentStream(document, page); //content
            contentStream.beginText(); //begin text input
            contentStream.setFont(font, 12); //font
            contentStream.newLineAtOffset(100, 700); // page location
            contentStream.showText(this.documentAsText); //print the contents of text
            contentStream.endText(); //end the content input
            contentStream.close(); //close content input
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(); //byte array stream output
            document.save(byteArrayOutputStream); //save byte array
            document.close(); //close document input
            InputStream ip = new ByteArrayInputStream(byteArrayOutputStream.toByteArray()); //turn bytes to input stream to make it a byte array
            byteArrayPDF = getByteArray(ip); //turn to byte array
        }catch(IOException e){
            e.printStackTrace();
        }
        return byteArrayPDF;
    }
}
