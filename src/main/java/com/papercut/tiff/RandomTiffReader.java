package com.papercut.tiff;

import java.io.IOException;

public class RandomTiffReader {
    /**
     * 内置的randomTiffFile
     */
    private RandomTiffFile randomTiffFile;


    /**
     * 构造函数
     * @param filePath
     */
    public RandomTiffReader(String filePath){
        try {
            randomTiffFile = new RandomTiffFile(filePath, "r");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }







}
