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

public class FileAccessor {
    //Hashmap storing line number to byte offset
    HashMap<Integer, Long> linesHashmap = new HashMap<Integer, Long>();
    //Filename being read
    String filename = "linesnumbers.txt";
    //Realistically one is all you can use to read the file due to drive limits, this allows random read and is very fast
    private OptimizedRandomAccessFile raf;
    AsyncCache<Integer, String> cache = null;
    //How many steps between each pointer
    //Disk reads are around 4K so 50 will likely be one read
    int steps = 50;

    int totalLines;

    /**
     * Initialize the file reader
     */
    public FileAccessor() {
        addShutdownHook();
        loadData();
    }

    /**
     * Reads in file and uses an OptimizedRandomAccessFile which buffers the read to get pointers to every {steps} line
     * Every step is recorded in a hashmap
     * Once done a caffeine instance is made for caching with 1/10 the total lines as the size, adjust as needed for space/performance
     */
    public void loadData() {
        try {
            Stopwatch t = Stopwatch.createStarted();
            System.out.println("Starting Read of File " + filename);
            //Open file in
            raf = new OptimizedRandomAccessFile(filename, "r");
            linesHashmap.put(0, (long) 0);
            int i = 0;
            while (raf.readLine() != null) {
                i++;
                if (i % steps == 0) {
                    linesHashmap.put(i, raf.getFilePointer());
                }
            }
            totalLines = i;
            System.out.println("Completed in " + t.toString());
            //Create cache
            cache = Caffeine.newBuilder()
                    .maximumSize(totalLines / 10)
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
                //Check if the line exists in cache
                CompletableFuture<String> lineFuture = cache.getIfPresent(lineNum);
                String line;
                //If it exists return the cached value
                if (lineFuture != null && (line = lineFuture.get()) != null) {
                    System.out.println("Retrieved cached line in " + t.toString());
                    return line;
                } else {
                    //Make a future and use it to return the value from disk
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
                    //Add the future to the cache
                    cache.put(lineNum, newFuture);
                    //This shouldn't be able to be null
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
