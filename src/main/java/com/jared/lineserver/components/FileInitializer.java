package com.jared.lineserver.components;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.bitbucket.kienerj.io.OptimizedRandomAccessFile;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import static java.lang.Thread.sleep;

//@CacheConfig(cacheNames = {"lines"})
public class FileInitializer {
    HashMap<Integer, Long> linesHashmap = new HashMap<Integer, Long>();
    ArrayList<Long> linePointers = new ArrayList<>();
    String filename = "linesnumbers.txt";
    //This can be scaled out since it's only file reading, one should be fine though
    private OptimizedRandomAccessFile raf;
    AsyncCache<Integer, String> cache = null;
    int steps = 200;

    public FileInitializer() {
        loadData();
    }

    //@EventListener(ApplicationReadyEvent.class)
    public void loadData() {
        try {
            long startTime = System.currentTimeMillis();
            System.out.println("Starting Read of File " + filename);
            raf = new OptimizedRandomAccessFile(filename, "r");
            linesHashmap.put(0,(long) 0);
            int i = 0;
            while (raf.readLine() != null) {
                i++;
                if (i % steps == 0) {
                    linesHashmap.put(i,raf.getFilePointer());
                }
            }
            System.out.println("Completed in " + (System.currentTimeMillis() - startTime) + " Milliseconds");
            //Create cache
            cache = Caffeine.newBuilder()
                    .maximumSize(linesHashmap.size() / 10)
                    .buildAsync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getLine(int lineNum) {
        try {
            //If not in the set of lines, return null
            if (lineNum <= linesHashmap.size()*steps+steps) {
                //Use a future to share results if multiple hits
                CompletableFuture<String> lineFuture = cache.getIfPresent(lineNum);
                String line;
                if (lineFuture != null && (line = lineFuture.get()) != null) {
                    //System.out.println("Returning Cached Line " + lineNum);
                    return line;
                } else {
                    CompletableFuture<String> newFuture = CompletableFuture.supplyAsync(() -> {
                        try {
                            long startTime = System.currentTimeMillis();
                            //System.out.println("Getting File Line " + lineNum);
                            raf.seek(linesHashmap.get(lineNum-(lineNum%steps)));
                            String textLine = null;
                            for(int i = 0; i< (lineNum%steps); i++){
                                raf.readLine();
                            }
                            textLine = raf.readLine();
                            //System.out.println("Retrieved in " + (System.currentTimeMillis() - startTime) + " Milliseconds");
                            return textLine;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return null;
                    });
                    cache.put(lineNum, newFuture);
                    return cache.getIfPresent(lineNum).get();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
