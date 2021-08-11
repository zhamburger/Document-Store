package edu.yu.cs.com1320.project.stage5.impl;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import edu.yu.cs.com1320.project.stage5.Document;
import edu.yu.cs.com1320.project.stage5.PersistenceManager;
import java.io.*;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.file.*;
import java.util.Arrays;


/**
 * created by the document store and given to the BTree via a call to BTree.setPersistenceManager
 */
public class DocumentPersistenceManager implements PersistenceManager<URI, Document> {

    private File baseDir;

    public DocumentPersistenceManager(File baseDir){
        if(baseDir == null) return;
        this.baseDir = baseDir;
        if(fileDoesntExist(this.baseDir)) this.baseDir.mkdirs();
    }

    @Override
    public void serialize(URI uri, Document val) throws IOException {
        File file = getFile(uri, true);
        Gson gson = new GsonBuilder().registerTypeAdapter(Document.class, serializer).setPrettyPrinting().create();
        Type documentType = new TypeToken<DocumentImpl>(){
        }.getType();
        FileWriter writer = new FileWriter(new File(file.getAbsoluteFile() + ".json"));
        writer.write(gson.toJson(val, documentType));
        writer.flush();
        writer.close();
    }

    @Override
    public Document deserialize(URI uri) throws IOException {
        if(uri == null) return null;
        File file = getFile(uri, false);
        FileReader reader = new FileReader(new File(file.getPath()+".json"));
        Gson gson = new GsonBuilder().registerTypeAdapter(Document.class, serializer).setPrettyPrinting().create();
        Type documentType = new TypeToken<DocumentImpl>(){}.getType();
        JsonReader jread = new JsonReader(reader);
        Document doc = gson.fromJson(jread, documentType);
        jread.close();
        reader.close();
        String [] directories = uri.toString().split("/");
        this.getParentDirectoriesThenDelete(directories, file, uri.isAbsolute());
        return doc;
    }

    private JsonSerializer<Document> serializer = (document, type, jsonSerializationContext) -> {
        JsonObject json = new JsonObject();
        Gson gson = new Gson();
        json.addProperty("Text", document.getDocumentAsTxt());
        json.addProperty("URI", gson.toJson(document.getKey()));
        json.addProperty("HashCode", document.hashCode());
        json.addProperty("Word Map", gson.toJson(document.getWordMap()));
        return json;
    };

    private File getFile(URI uri, boolean beingMade) throws IOException {
        File file, dirs = null;
        Path uriPath = Paths.get(getUriPath(uri));
        file = new File(getRootDirectoryPath() + File.separator + uriPath + File.separator + new File(uri.toString()).getName());
        
        if(beingMade && this.baseDir == null) {
            dirs = new File(System.getProperty("user.dir")+File.separator+uriPath);
            dirs.mkdirs();
        }else if(beingMade){
            dirs = new File(this.baseDir+File.separator + uriPath);
            dirs.mkdirs();
        }
        return file;
    }

    private String getUriPath(URI uri){
        String [] uriArrayPath = uri.toString().split("/");
        StringBuilder uriPath = new StringBuilder();
        if(uri.isAbsolute()) {
            if (!uriArrayPath[0].contains(uri.getScheme())) {
                uriPath.append(uriArrayPath[0]).append(File.separator);
            }
        }else{
            uriPath.append(uriArrayPath[0]).append(File.separator);
        }
        for(int i = 1; i < uriArrayPath.length - 1; i++){

            uriPath.append(uriArrayPath[i]).append(File.separator);
        }

        return uriPath.toString();
    }

    private void getParentDirectoriesThenDelete(String [] directories, File file, boolean hasScheme) throws IOException {
        for(int i = directories.length - 1; i > 0; i--) {
            File parentDirectory = null;
            if(this.baseDir == null) {
                parentDirectory = new File(System.getProperty("user.dir") +  File.separator + getSubDirectories(directories, i, hasScheme));
            }else{
                parentDirectory = new File(this.baseDir.getPath() + File.separator + getSubDirectories(directories, i, hasScheme));
            }

            if(parentDirectory.getName().equals(file.getName())){
                File deletedFile = new File(parentDirectory+".json");
                boolean deleted = deletedFile.delete();
            }
            if(parentDirectory.isDirectory()) {
                deleteDir(parentDirectory);
            }
        }
    }

    private File getSubDirectories(String [] dirs, int count, boolean hasScheme){
        int starter = 0;
        File subDirs = null;
        if(hasScheme){
            subDirs = new File(dirs[1]);
            starter = 2;
        }else{
            subDirs = new File(dirs[0]);
            starter = 1;
        }
        for (int i = starter; i <= count; i++) {
            String dir = dirs[i];
            File newFile = new File(File.separator+dir);
            subDirs = new File(subDirs.getPath()+ newFile.getPath());
        }
        return subDirs;
    }

    private void deleteDir(File parentDirectory){
        File current = parentDirectory;
        while (current.listFiles().length < 1) {
            current.delete();
            current = current.getParentFile();
        }
    }


    /*Checks*/
    private boolean fileDoesntExist(File file) { return !baseDir.exists(); }
    private boolean thereIsABaseDir(){ return this.baseDir != null; }
    private String getRootDirectoryPath() { return (thereIsABaseDir()) ? this.baseDir.getPath() : System.getProperty("user.dir"); }
}