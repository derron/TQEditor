package com.dhu.tqeditor.model;

import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

public class ArzFile {
    private final String path;
    private String[] strings;
    private Map<String, Integer> stringMap;
    private Map<String, RecordInfo> recordInfoMap;
    private List<RecordInfo> recordInfoList;

    private ArzFile(String path) {
        this.path = path;
        recordInfoMap = new HashMap<>();
        recordInfoList = new ArrayList<>();
        stringMap = new HashMap<>();
    }

    public String getPath() {
        return path;
    }

    public int getRecordCount() {
        return recordInfoList.size();
    }

    public List<RecordInfo> getRecordInfoList() {
        return recordInfoList;
    }

    public RecordInfo getRecord(int index) {
        return recordInfoList.get(index);
    }

    public RecordInfo getRecord(String id) {
        return recordInfoMap.get(id.toLowerCase().replace('/', '\\'));
    }

    public static ArzFile load(String path) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            throw new FileNotFoundException(path);
        }
        ArzFile arzFile = new ArzFile(path);
        arzFile.read();
        return arzFile;
    }

    private void read() throws IOException {
        RandomAccessFile randomAccessFile = new RandomAccessFile(new File(path), "r");
        int[] header = new int[6];
        for (int i = 0; i < header.length; i++) {
            header[i] = readInt32(randomAccessFile);
        }
        int firstTableStart = header[1];
        int firstTableCount = header[3];
        int secondTableStart = header[4];
        this.readStringTable(secondTableStart, randomAccessFile);
        this.readRecordTable(firstTableStart, firstTableCount, randomAccessFile);
        randomAccessFile.close();
    }

    private void readRecordTable(int offset, int count, RandomAccessFile randomAccessFile) throws IOException {
        randomAccessFile.seek(offset);
        recordInfoMap = new HashMap<>((int) (count * 1.2));
        recordInfoList = new ArrayList<>((int) (count * 1.2));
        for (int i = 0; i < count; i++) {
            RecordInfo recordInfo = new RecordInfo();
            // 24 is the offset of where all record data begins
            recordInfo.decode(randomAccessFile, 24, this);
            recordInfoList.add(recordInfo);
            recordInfoMap.put(recordInfo.getNormalizedId(), recordInfo);
        }
        Collections.sort(recordInfoList, new Comparator<RecordInfo>() {
            @Override
            public int compare(RecordInfo o1, RecordInfo o2) {
                return Integer.compare(o1.getOffset(), o2.getOffset());
            }
        });
        Log.i("ArzFile", String.format("Loaded %d records", count));
    }

    private void readStringTable(int secondTableStart, RandomAccessFile randomAccessFile) throws IOException {
        randomAccessFile.seek(secondTableStart);
        int numStrings = readInt32(randomAccessFile);
        this.strings = new String[numStrings];
        this.stringMap = new HashMap<>(numStrings);
        for (int i = 0; i < numStrings; ++i) {
            this.strings[i] = readCString(randomAccessFile);
            this.stringMap.put(this.strings[i], i);
        }
    }

    static String readCString(RandomAccessFile randomAccessFile) throws IOException {
        // first 4 bytes is the string length, followed by the string.
        int len = readInt32(randomAccessFile);
        if (len == 0) {
            return "";
        }
        byte[] rawData = new byte[len];
        randomAccessFile.read(rawData);
        return new String(rawData, "windows-1252");
    }

    static int readInt32(RandomAccessFile randomAccessFile) throws IOException {
        return Integer.reverseBytes(randomAccessFile.readInt());
    }

    static short readInt16(RandomAccessFile randomAccessFile) throws IOException {
        return Short.reverseBytes(randomAccessFile.readShort());
    }

    public String getString(int index) {
        if (index < 0 || index >= strings.length) {
            return "";
        }
        return strings[index];
    }

    public int getStringCount() {
        return strings.length;
    }

    public int getStringId(String string) {
        return stringMap.containsKey(string) ? stringMap.get(string) : -1;
    }
}
