package com.jared.lineserver.components;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.bitbucket.kienerj.io.OptimizedRandomAccessFile;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import static java.lang.Thread.sleep;

//@CacheConfig(cacheNames = {"lines"})
public class FileInitializer {
    ArrayList<Long> linePointers = new ArrayList<>();
    String filename = "linesbig.txt";
    private OptimizedRandomAccessFile raf;
    AsyncCache<Integer, String> cache = null;

    public FileInitializer() {
        loadData();
    }

    //@EventListener(ApplicationReadyEvent.class)
    public void loadData() {
        try {
            /*FileInputStream fis = new FileInputStream(filename);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));

            while (br.readLine() != null) {
                linePointers.add(fis.getChannel().position());
                //System.out.println(thisLine);
            }*/
            long startTime = System.currentTimeMillis();
            System.out.println("Starting Read of File " + filename);
            raf = new OptimizedRandomAccessFile(filename, "r");
            linePointers.add((long) 0);
            while (raf.readLine() != null) {
                linePointers.add(raf.getFilePointer());
            }
            System.out.println("Completed in " + (System.currentTimeMillis() - startTime) + " Milliseconds");
            //Create cache
            cache = Caffeine.newBuilder()
                    .maximumSize(linePointers.size() / 10)
                    .buildAsync();
            //raf.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //@Cacheable("lines")
    public synchronized String getLine(int lineNum) {
        try {
            //If not in the set of lines, return null
            if (lineNum <= linePointers.size()) {
                CompletableFuture<String> lineFuture = cache.getIfPresent(lineNum);
                String line;
                if (lineFuture != null && (line = lineFuture.get())!= null) {
                    System.out.println("Returning Cached Line");
                    return line;
                } else {
                    CompletableFuture newFuture = CompletableFuture.supplyAsync(()->{
                        try {
                        System.out.println("Sleeping 5s to show cache");
                            try {
                                sleep(5000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            raf.seek(linePointers.get(lineNum));
                            return raf.readLine();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return null;
                    });
                    cache.put(lineNum,newFuture);
                    return cache.getIfPresent(lineNum).get();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
