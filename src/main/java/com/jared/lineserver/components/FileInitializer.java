package com.jared.lineserver.components;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Stopwatch;
import org.bitbucket.kienerj.io.OptimizedRandomAccessFile;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class FileInitializer {
    //Hashmap storing line number to byte offset
    HashMap<Integer, Long> linesHashmap = new HashMap<Integer, Long>();
    //Test Arraylist to see if it would retrieve faster than HashMap with many entries
    //List<Long> lineOffsets = new ArrayList<>();
    //Filename being read
    String filename = "linesnumbers.txt";
    //Realistically one is all you can use to read the file due to drive limits, this allows random read and is very fast
    private OptimizedRandomAccessFile raf;
    AsyncCache<Integer, String> cache = null;
    //How many steps between each pointer
    //Disk reads are around 4K so 50 will likely be one read
    int steps = 50;

    int totalLines;

    /*int sum = 0;
    int count = 0;*/

    /**
     * Initialize the file reader
     */
    public FileInitializer() {
        addShutdownHook();
        loadData();
    }

    //@EventListener(ApplicationReadyEvent.class)
    public void loadData() {
        try {
            Stopwatch t = Stopwatch.createStarted();
            System.out.println("Starting Read of File " + filename);
            raf = new OptimizedRandomAccessFile(filename, "r");
            linesHashmap.put(0, (long) 0);
            //lineOffsets.add((long) 0);
            int i = 0;
            while (raf.readLine() != null) {
                i++;
                if (i % steps == 0) {
                    //lineOffsets.add(raf.getFilePointer());
                    linesHashmap.put(i, raf.getFilePointer());
                }
            }
            totalLines = i;
            System.out.println("Completed in " + t.toString());
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
            Stopwatch t = Stopwatch.createStarted();
            //If not in the set of lines, return null
            if (lineNum <= totalLines && lineNum >= 0) {
                //Use a future to share results if multiple hits
                CompletableFuture<String> lineFuture = cache.getIfPresent(lineNum);
                String line;
                if (lineFuture != null && (line = lineFuture.get()) != null) {
                    System.out.println("Retrieved cached line in " + t.toString());
                    return line;
                } else {
                    CompletableFuture<String> newFuture = CompletableFuture.supplyAsync(() -> {
                        try {
                            raf.seek(linesHashmap.get((lineNum - (lineNum % steps))));
                            for (int i = 0; i < (lineNum % steps); i++) {
                                raf.readLine();
                            }
                            String textLine = raf.readLine();
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
