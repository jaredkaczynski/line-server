package com.jared.lineserver.components;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Stopwatch;
import org.bitbucket.kienerj.io.OptimizedRandomAccessFile;

import java.io.*;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public class FileAccessor {
    //Hashmap storing line number to byte offset
    private final HashMap<Integer, Long> linesHashmap = new HashMap<>();
    //Filename being read
    @SuppressWarnings("FieldCanBeLocal")
    private final String filename = "lines.txt";
    //Realistically one is all you can use to read the file due to drive limits, this allows random read and is very fast
    private OptimizedRandomAccessFile raf;
    private AsyncCache<Integer, String> cache = null;
    //How many steps between each pointer
    //Disk reads are around 4K so 50 will likely be one read
    private final int steps = 50;
    //Keep track of total lines in file
    private int totalLines;

    /**
     * Initialize the file reader and add shutdown hooks
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
    private void loadData() {
        try {
            Stopwatch t = Stopwatch.createStarted();
            System.out.println("Starting Read of File " + filename);
            //Open file in
            raf = new OptimizedRandomAccessFile(filename, "r");
            linesHashmap.put(0, (long) 0);
            int i = 0;
            //Read all lines of file
            while (raf.readLine() != null) {
                i++;
                //If the line is an increment of a step store a byte offset
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
                            //Get line offset compared to row in hashmap
                            int offset = (lineNum % steps);
                            //I need to get the closest lowest line number so, subtract the modulo of the line number
                            //and the step count from the requested number to get the hashmap section
                            //Example, 121-(121%100) = 100
                            raf.seek(linesHashmap.get(lineNum - offset));
                            //Skip lines until goal line is next
                            for (int i = 0; i < offset; i++) {
                                raf.readLine();
                            }
                            //System.out.println("Retrieved in " + t.toString());
                            return raf.readLine();
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
    private void addShutdownHook() {
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
