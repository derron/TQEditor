package com.dhu.tqeditor.model;

import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArcFile {

    public static ArcFile load(String path) throws IOException {
        ArcFile arcFile = new ArcFile(path);
        arcFile.load();
        return arcFile;
    }

    public enum TQColor {
        /**
        * Titan Quest Aqua color
        */
        Aqua("a"),

        /**
        * Titan Quest Blue color
        */
        Blue("b"),

        /**
        * Titan Quest Light Cyan color
        */
        LightCyan("c"),

        /**
        * Titan Quest Dark Gray color
        */
        DarkGray("d"),

        /**
        * Titan Quest Fuschia color
        */
        Fuschia("f"),

        /**
        * Titan Quest Green color
        */
        Green("g"),

        /**
        * Titan Quest Indigo color
        */
        Indigo("i"),

        /**
        * Titan Quest Khaki color
        */
        Khaki("k"),

        /**
        * Titan Quest Yellow Green color
        */
        YellowGreen("l"),

        /**
        * Titan Quest Maroon color
        */
        Maroon("m"),

        /**
        * Titan Quest Orange color
        */
        Orange("o"),

        /**
        * Titan Quest Purple color
        */
        Purple("p"),

        /**
        * Titan Quest Red color
        */
        Red("r"),

        /**
        * Titan Quest Silver color
        */
        Silver("s"),

        /**
        * Titan Quest Turquoise color
        */
        Turquoise("t"),

        /**
        * Titan Quest White color
        */
        White("w"),

        /**
        * Titan Quest Yellow color
        */
        Yellow("y");

        private final String code;
        private TQColor(String code) {
            this.code = code;
        }

        public static TQColor getColor(String code) {
            for (TQColor color : values()) {
                if (color.code.equalsIgnoreCase(code)) {
                    return color;
                }
            }
            return TQColor.White;
        }
    }

    private static class ARCPartEntry {
        /**
         * Gets or sets the offset of this part entry within the file.
         */
        public int fileOffset;

        /**
         * Gets or sets the compressed size of this part entry.
         */
        public int compressedSize;

        /**
         * Gets or sets the real size of this part entry.
         */
        public int realSize;
    }

    private static class ARCDirEntry {
        /**
         * Gets or sets the filename.
         */
        public String fileName;

        /**
         * Gets or sets the storage type.
         * Data is either compressed (3) or stored (1)
         */
        public int storageType;

        /**
         * Gets or sets the offset within the file.
         */
        public int fileOffset;

        /**
         * Gets or sets the compressed size of this entry.
         */
        public int compressedSize;

        /**
         * Gets or sets the real size of this entry.
         */
        public int realSize;

        /**
         * Gets or sets the part data
         */
        public ARCPartEntry[] parts;

        /**
         * Gets a value indicating whether this part is active.
         */
        public boolean isActive() {
            if (this.storageType == 1) {
                return true;
            } else {
                return this.parts != null;
            }
        }
    }

    private String fileName;
    private Map<String, ARCDirEntry> directoryEntries;
    private Map<String, CharSequence> stringMap;
    public ArcFile(String fileName) {
        this.fileName = fileName;
    }

    public void load() throws IOException {
        RandomAccessFile arcFile = new RandomAccessFile(this.fileName, "r");
        try {
            readARCToC(arcFile);
            readAllString(arcFile);
        } finally {
            arcFile.close();
        }
    }

    public Map<String, CharSequence> getStringMap() {
        return stringMap;
    }

    public CharSequence getString(String key) {
        return stringMap.get(key);
    }

    private static int fromRgb(int r, int g, int b) {
        return (255 << 24) | (r << 16) | (g <<  8) | b;
    }

    public static int getColor(TQColor color) {
        switch (color) {
            case Aqua:
                return fromRgb(0, 255, 255);

            case Blue:
                return fromRgb(0, 163, 255);

            case DarkGray:
                return fromRgb(153, 153, 153);

            case Fuschia:
                return fromRgb(255, 0, 255);

            case Green:
                return fromRgb(64, 255, 64);

            case Indigo:
                return fromRgb(75, 0, 130);

            case Khaki:
                return fromRgb(195, 176, 145);

            case LightCyan:
                return fromRgb(224, 255, 255);

            case Maroon:
                return fromRgb(128, 0, 0);

            case Orange:
                return fromRgb(255, 173, 0);

            case Purple:
                return fromRgb(217, 5, 255);

            case Red:
                return fromRgb(255, 0, 0);

            case Silver:
                return fromRgb(224, 224, 224);

            case Turquoise:
                return fromRgb(0, 255, 209);

            case White:
                return fromRgb(255, 255, 255);

            case Yellow:
                return fromRgb(255, 245, 43);

            case YellowGreen:
                return fromRgb(145, 203, 0);

            default:
                return Color.WHITE;
        }
    }

    private void readAllString(RandomAccessFile arcFile) throws IOException {
        stringMap = new HashMap<>();
        for (ARCDirEntry arcDirEntry : directoryEntries.values()) {
            byte[] data = readData(arcFile, arcDirEntry.fileName);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(data), StandardCharsets.UTF_16LE))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] splits = line.split("=");
                    if (splits.length != 2) {
                        continue;
                    }
                    stringMap.put(splits[0].trim(), parseText(splits[1].trim()));
                }
            }
        }
    }

    static final Pattern COLOR_PATTERN = Pattern.compile("\\{\\^([a-z])\\}");
    static final Pattern SPACE_PATTERN = Pattern.compile(" ?([\\u4e00-\\u9fa5]) ");
    public static CharSequence parseText(String text) {
        // Remove spaces between chinese characters
        Matcher spaceMatcher = SPACE_PATTERN.matcher(text.trim());
        text = spaceMatcher.replaceAll("$1");
        int color = Color.WHITE;
        int index = 0;
        SpannableStringBuilder builder = new SpannableStringBuilder();
        Matcher matcher = COLOR_PATTERN.matcher(text);
        while (matcher.find()) {
            int start = matcher.start();
            if (start > index) {
                TQColor tqColor = TQColor.getColor(matcher.group(1));
                builder.append(text.substring(index, start));
                builder.setSpan(new ForegroundColorSpan(color), builder.length() - (start - index), builder.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                color = getColor(tqColor);
                index = matcher.end();
            } else {
                TQColor tqColor = TQColor.getColor(matcher.group(1));
                color = getColor(tqColor);
                index = matcher.end();
            }
        }
        if (index < text.length()) {
            builder.append(text.substring(index));
            builder.setSpan(new ForegroundColorSpan(color), builder.length() - (text.length() - index), builder.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        builder.setSpan(new BackgroundColorSpan(Color.BLACK), 0, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return builder;
    }

    /**
    * Read the table of contents of the ARC file
    */
    private void readARCToC(RandomAccessFile arcFile) throws IOException {
        // Format of an ARC file
        // 0x08 - 4 bytes = # of files
        // 0x0C - 4 bytes = # of parts
        // 0x18 - 4 bytes = offset to directory structure
        //
        // Format of directory structure
        // 4-byte int = offset in file where this part begins
        // 4-byte int = size of compressed part
        // 4-byte int = size of uncompressed part
        // these triplets repeat for each part in the arc file
        // After these triplets are a bunch of null-terminated strings
        // which are the sub filenames.
        // After the subfilenames comes the subfile data:
        // 4-byte int = 3 == indicates start of subfile item  (maybe compressed flag??)
        //          1 == maybe uncompressed flag??
        // 4-byte int = offset in file where first part of this subfile begins
        // 4-byte int = compressed size of this file
        // 4-byte int = uncompressed size of this file
        // 4-byte crap
        // 4-byte crap
        // 4-byte crap
        // 4-byte int = numParts this file uses
        // 4-byte int = part# of first part for this file (starting at 0).
        // 4-byte int = length of filename string
        // 4-byte int = offset in directory structure for filename

        // check the file header
        if (arcFile.readByte() != 0x41) {
            throw new IOException("Invalid arc file");
        }

        if (arcFile.readByte() != 0x52) {
            throw new IOException("Invalid arc file");
        }

        if (arcFile.readByte() != 0x43) {
            throw new IOException("Invalid arc file");
        }

        if (arcFile.length() < 0x21) {
            throw new IOException("Invalid arc file");
        }

        arcFile.seek(0x08);
        int numEntries = ArzFile.readInt32(arcFile);
        int numParts = ArzFile.readInt32(arcFile);

        ARCPartEntry[] parts = new ARCPartEntry[numParts];
        ARCDirEntry[] records = new ARCDirEntry[numEntries];

        arcFile.seek(0x18);
        int tocOffset = ArzFile.readInt32(arcFile);

        // Make sure all 3 entries exist for the toc entry.
        if (arcFile.length() < (tocOffset + 12)) {
            throw new IOException("Invalid arc file");
        }

        // Read in all of the part data
        arcFile.seek(tocOffset);
        for (int i = 0; i < numParts; i++) {
            parts[i] = new ARCPartEntry();
            parts[i].fileOffset = ArzFile.readInt32(arcFile);
            parts[i].compressedSize = ArzFile.readInt32(arcFile);
            parts[i].realSize = ArzFile.readInt32(arcFile);
        }

        // Now record this offset so we can come back and read in the filenames
        // after we have read in the file records
        int fileNamesOffset = (int)arcFile.getFilePointer();

        // Now seek to the location where the file record data is
        // This offset is from the end of the file.
        int fileRecordOffset = 44 * numEntries;

        arcFile.seek(arcFile.length() - fileRecordOffset);
        for (int i = 0; i < numEntries; i++) {
            records[i] = new ARCDirEntry();

            // storageType = 3 - compressed / 1- non compressed
            int storageType = ArzFile.readInt32(arcFile);

            // Added by VillageIdiot to support stored types
            records[i].storageType = storageType;
            records[i].fileOffset = ArzFile.readInt32(arcFile);
            records[i].compressedSize = ArzFile.readInt32(arcFile);
            records[i].realSize = ArzFile.readInt32(arcFile);
            int crap = ArzFile.readInt32(arcFile); // crap
            int crap3 = ArzFile.readInt32(arcFile); // crap
            int crap4 = ArzFile.readInt32(arcFile); // crap

            int numberOfParts = ArzFile.readInt32(arcFile);
            if (numberOfParts < 1) {
                records[i].parts = null;
            } else {
                records[i].parts = new ARCPartEntry[numberOfParts];
            }

            int firstPart = ArzFile.readInt32(arcFile);
            int fileLength = ArzFile.readInt32(arcFile); // filename length
            int fileOffset = ArzFile.readInt32(arcFile); // filename offset
            if (storageType != 1 && records[i].isActive()) {
                System.arraycopy(parts, firstPart, records[i].parts, 0, records[i].parts.length);
            }
        }

        // Now read in the record names
        arcFile.seek(fileNamesOffset);
        byte[] buffer = new byte[2048];
        for (int i = 0; i < numEntries; i++) {
            // only Active files have a filename entry
            if (records[i].isActive()) {
                // For each string, read bytes until I hit a 0x00 byte.
                int bufferSize = 0;
                while ((buffer[bufferSize++] = arcFile.readByte()) != 0x00) {
                    if (buffer[bufferSize - 1] == 0x03) {
                        // File is null?
                        arcFile.seek(arcFile.getFilePointer() - 1); // backup
                        bufferSize--;
                        buffer[bufferSize] = 0x00;
                        break;
                    }

                    if (bufferSize >= buffer.length) {
                        throw new IOException("ARCFile.readARCToC() Error - Buffer size of 2048 has been exceeded.");
                    }
                }

                String newFile;
                if (bufferSize >= 1) {
                    // Now convert the buffer to a string
                    newFile = new String(buffer, 0, bufferSize - 1);
                } else {
                    throw new IOException("Null File " + i);
                }

                records[i].fileName = newFile.toLowerCase().replace('/', '\\');
            }
        }

        // Now convert the array of records into a Dictionary.
        Map<String, ARCDirEntry> dictionary = new HashMap<>(numEntries);
        for (int i = 0; i < numEntries; i++) {
            if (records[i].isActive()) {
                dictionary.put(records[i].fileName, records[i]);
            }
        }
        this.directoryEntries = dictionary;
    }

    public byte[] readData(RandomAccessFile arcFile, String dataId) throws IOException {
        if (this.directoryEntries == null) {
            return null;
        }

        // First normalize the filename
        dataId = dataId.toLowerCase().replace('/', '\\');

        // Find our file in the toc.
        // First strip off the leading folder since it is just the ARC name
        int firstPathDelim = dataId.indexOf('\\');
        if (firstPathDelim != -1) {
            dataId = dataId.substring(firstPathDelim + 1);
        }

        // Now see if this file is in the toc.
        ARCDirEntry directoryEntry;
        if (directoryEntries.containsKey(dataId)) {
            directoryEntry = this.directoryEntries.get(dataId);
        } else {
            // record not found
            return null;
        }
        // Allocate memory for the uncompressed data
        byte[] data = new byte[directoryEntry.realSize];

        // First see if the data was just stored without compression.
        if ((directoryEntry.storageType == 1) && (directoryEntry.compressedSize == directoryEntry.realSize)) {
            arcFile.seek(directoryEntry.fileOffset);
            arcFile.readFully(data, 0, directoryEntry.realSize);
        } else {
            // Now process each part of this record
            int startPosition = 0;
            // The data was compressed so we attempt to decompress it.
            for (ARCPartEntry partEntry : directoryEntry.parts) {
                // seek to the part we want
                arcFile.seek(partEntry.fileOffset);
                byte[] compressedData = new byte[partEntry.compressedSize];
                arcFile.readFully(compressedData);
                System.arraycopy(RecordInfo.decompressData(compressedData), 0, data, startPosition, partEntry.realSize);
                startPosition += partEntry.realSize;
            }
        }
        return data;
    }

}
