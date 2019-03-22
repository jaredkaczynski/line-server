package com.jared.lineserver.components;

import com.jared.lineserver.configuration.CacheConfig;
import org.bitbucket.kienerj.io.OptimizedRandomAccessFile;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;

import java.io.*;
import java.util.ArrayList;

//@Component
@EnableCaching
public class FileInitializer {
    ArrayList<Long> linePointers = new ArrayList<>();
    String filename = "linessmall.txt";
    private OptimizedRandomAccessFile raf;

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

            raf = new OptimizedRandomAccessFile(filename,"r");
            linePointers.add((long) 0);
            while (raf.readLine() != null) {
                linePointers.add(raf.getFilePointer());
            }
            //raf.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Cacheable(CacheConfig.CACHE_ONE)
    public synchronized String getLine(int lineNum) {
        try {
            raf.seek(linePointers.get(lineNum));
            return raf.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
