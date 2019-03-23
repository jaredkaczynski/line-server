package com.jared.lineserver.components;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Stopwatch;
import org.bitbucket.kienerj.io.OptimizedRandomAccessFile;

import java.io.*;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class FileInitializer {
    //Hashmap storing line number to byte offset
    HashMap<Integer, Long> linesHashmap = new HashMap<Integer, Long>();
    //Filename being read
    String filename = "linesnumbers.txt";
    //Realistically one is all you can use to read the file, this allows random read and is very fast
    private OptimizedRandomAccessFile raf;
    AsyncCache<Integer, String> cache = null;
    //How many steps between each pointer line, 0,200,400
    //Since the goal was to be efficient in storage while also being fast, this is a good balance since a database is
    //not allowed and there seems to be a floor of performance of around 100microseconds per line
    int steps = 100;

    int sum = 0;
    int count = 0;
    private OptimizedRandomAccessFile raf2;

    public FileInitializer() {
        addShutdownHook();
        loadData();
    }

    //@EventListener(ApplicationReadyEvent.class)
    public void loadData() {
        try {
            long startTime = System.currentTimeMillis();
            System.out.println("Starting Read of File " + filename);
            raf = new OptimizedRandomAccessFile(filename, "r");
            linesHashmap.put(0, (long) 0);
            int i = 0;
            while (raf.readLine() != null) {
                i++;
                if (i % steps == 0) {
                    linesHashmap.put(i, raf.getFilePointer());
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

    /**
     *
     * @param lineNum Specific line of file requested
     * @return The line content
     *
     * This uses an async cache loader from Caffeine to allow multiple threads to get the same file line read if both request
     * while one is still reading the requested value
     *
     */
    public String getLine(int lineNum) {
        try {
            //If not in the set of lines, return null
            if (lineNum <= linesHashmap.size() * steps + steps) {
                //Use a future to share results if multiple hits
                CompletableFuture<String> lineFuture = cache.getIfPresent(lineNum);
                String line;
                if (lineFuture != null && (line = lineFuture.get()) != null) {
                    System.out.println("Returning Cached Line " + lineNum);
                    return line;
                } else {
                    CompletableFuture<String> newFuture = CompletableFuture.supplyAsync(() -> {
                        try {
                            //System.out.println("Getting File Line " + lineNum);
                            Stopwatch t = Stopwatch.createStarted();
                            count++;
                            raf.seek(linesHashmap.get(lineNum - (lineNum % steps)));
                            String textLine = null;
                            for (int i = 0; i < (lineNum % steps); i++) {
                                raf.readLine();
                            }
                            textLine = raf.readLine();
                            sum+=t.elapsed(TimeUnit.MICROSECONDS);
                            System.out.println("Retrieved in " + t.toString());
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

    /**
     * Close file on shutdown
     */
    public void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Closing files");
            try {
                raf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));

    }

}
