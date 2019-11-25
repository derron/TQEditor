package com.dhu.tqeditor.model;

import android.text.TextUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class RecordInfo {
    private String normalizedId;
    private int idStringIndex;
    private String recordType;
    private int offset;
    private int length;
    private String id;
    private CharSequence name;
    private List<String> relativeNames = new ArrayList<>();

    public String getNormalizedId() {
        return normalizedId;
    }

    public String getId() {
        return id;
    }

    public int getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

    public String getRecordType() {
        return recordType;
    }

    public int getIdStringIndex() {
        return idStringIndex;
    }

    void decode(RandomAccessFile randomAccessFile, int baseOffset, ArzFile arzFile) throws IOException {
        // Record Entry Format
        // 0x0000 int32 stringEntryID (dbr filename)
        // 0x0004 int32 string length
        // 0x0008 string (record type)
        // 0x00?? int32 offset
        // 0x00?? int32 length in bytes
        // 0x00?? int32 timestamp?
        // 0x00?? int32 timestamp?
        this.idStringIndex = ArzFile.readInt32(randomAccessFile);
        this.recordType = ArzFile.readCString(randomAccessFile);
        this.offset = ArzFile.readInt32(randomAccessFile) + baseOffset;

        // Compressed size
        // We throw it away and just advance the offset in the file.
        this.length = ArzFile.readInt32(randomAccessFile);

        // Crap1 - timestamp?
        // We throw it away and just advance the offset in the file.
        ArzFile.readInt32(randomAccessFile);

        // Crap2 - timestamp?
        // We throw it away and just advance the offset in the file.
        ArzFile.readInt32(randomAccessFile);

        // Get the ID string
        this.id = arzFile.getString(this.idStringIndex);
        this.normalizedId = id.toLowerCase().replace('/', '\\');
    }

    public Record loadRecord(ArzFile arzFile, ArcFile arcFile) throws IOException {
        // record variables have this format:
        // 0x00 int16 specifies data type:
        //      0x0000 = int - data will be an int32
        //      0x0001 = float - data will be a Single
        //      0x0002 = string - data will be an int32 that is index into string table
        //      0x0003 = bool - data will be an int32
        // 0x02 int16 specifies number of values (usually 1, but sometimes more (for arrays)
        // 0x04 int32 key string ID (the id into the string table for this variable name
        // 0x08 data value
        byte[] data = this.decompressBytes(arzFile);
        if (data.length % 4 != 0) {
            throw new IOException("Invalid data length: " + data.length);
        }
        Record record = Record.decode(data, id, recordType, arzFile);
        if (arcFile != null) {
            name = record.getName(arcFile);
            relativeNames.clear();
            for (Variable variable : record.getVariables()) {
                List<RecordInfo> recordInfos = variable.getRecordInfos(arzFile);
                if (recordInfos != null) {
                    for (RecordInfo recordInfo : recordInfos) {
                        relativeNames.add(recordInfo.getNormalizedId());
                        CharSequence recordName = recordInfo.getName();
                        if (!TextUtils.isEmpty(recordName)) {
                            relativeNames.add(recordName.toString());
                        }
                    }
                }
            }
        }
        return record;
    }

    public CharSequence getName() {
        return name;
    }

    public List<String> getRelativeNames() {
        return relativeNames;
    }

    public void saveRecord(Record record, ArzFile arzFile) throws IOException {
        byte[] data = record.encode();
        byte[] compressedData = compressData(data);
        if (compressedData.length > this.length) {
            throw new IOException("Record too large");
        }
        RandomAccessFile randomAccessFile = new RandomAccessFile(new File(arzFile.getPath()), "rws");
        randomAccessFile.seek(this.offset);
        randomAccessFile.write(compressedData);
        randomAccessFile.close();
    }

    private byte[] decompressBytes(ArzFile arzFile) throws IOException {
        RandomAccessFile randomAccessFile = new RandomAccessFile(new File(arzFile.getPath()), "r");
        randomAccessFile.seek(this.offset);
        byte[] data = new byte[this.length];
        randomAccessFile.readFully(data);
        byte[] bytes = decompressData(data);
        randomAccessFile.close();
        return bytes;
    }

    static byte[] decompressData(byte[] bytes) throws IOException {
        Inflater decompressor = new Inflater();
        decompressor.setInput(bytes);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            byte[] result = new byte[4096];
            int len = 0;
            while ((len = decompressor.inflate(result)) > 0) {
                outputStream.write(result, 0, len);
            }
        } catch (DataFormatException e) {
            throw new IOException(e);
        } finally {
            decompressor.end();
        }
        return outputStream.toByteArray();
    }

    static byte[] compressData(byte[] bytes) throws IOException {
        Deflater compressor = new Deflater(Deflater.BEST_COMPRESSION);
        compressor.setInput(bytes);
        compressor.finish();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            byte[] result = new byte[4096];
            int len = 0;
            while ((len = compressor.deflate(result)) > 0) {
                outputStream.write(result, 0, len);
            }
        } finally {
            compressor.end();
        }
        return outputStream.toByteArray();
    }

}
