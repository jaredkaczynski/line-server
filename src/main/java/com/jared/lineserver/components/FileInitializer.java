package com.jared.lineserver.components;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

//@Component
public class FileInitializer {
    RandomAccessFile raf = null;
    ArrayList<Long> linePointers = new ArrayList<>();

    public FileInitializer() {
        loadData();
    }

    //@EventListener(ApplicationReadyEvent.class)
    public void loadData() {
        try {
            raf = new RandomAccessFile("lines.txt", "r");
            linePointers.add((long) 0);
            while (raf.readLine() != null) {
                linePointers.add(raf.getFilePointer());
            }
            //raf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getLine(int lineNum){
        try {
            raf.seek(linePointers.get(lineNum));
            return raf.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
