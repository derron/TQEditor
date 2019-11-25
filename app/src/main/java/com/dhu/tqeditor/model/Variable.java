package com.dhu.tqeditor.model;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

public class Variable implements Deletable {
    private int id;
    private String name;
    private Object[] values;
    private DataType dataType;
    private boolean deleted;

    public Variable(int id, String name, DataType dataType, int valCount) {
        this.id = id;
        this.name = name;
        this.dataType = dataType;
        this.values = new Object[valCount];
    }

    public void setId(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Object[] getValues() {
        return values;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isDeleted() {
        return deleted;
    }

    void decode(DataInputStream stream, ArzFile arzFile) throws IOException {
        for (int i = 0; i < values.length; ++i) {
            switch (dataType) {
                case Float: {
                    float val = Float.intBitsToFloat(Integer.reverseBytes(stream.readInt()));
                    values[i] = val;
                    break;
                }
                case StringVar: {
                    int id = Integer.reverseBytes(stream.readInt());
                    String val = arzFile.getString(id);
                    val = val.trim();
                    values[i] = val + "|" + id;
                    break;
                }
                default: {
                    int val = Integer.reverseBytes(stream.readInt());
                    values[i] = val;
                    break;
                }
            }
        }
    }

    public String getValueString() {
        if (values.length == 1) {
            return values[0].toString();
        }
        StringBuilder builder = new StringBuilder();
        for (Object value : values) {
            if (builder.length() > 0) {
                builder.append(",");
            }
            builder.append(value);
        }
        return builder.toString();
    }

    public List<RecordInfo> getRecordInfos(ArzFile arzFile) {
        List<RecordInfo> recordInfos = new ArrayList<>();
        if (dataType != DataType.StringVar) {
            return recordInfos;
        }
        Matcher matcher = Record.RECORD_PATTERN.matcher(getValueString());
        while (matcher.find()) {
            final RecordInfo recordInfo = arzFile.getRecord(matcher.group());
            if (recordInfo != null) {
                recordInfos.add(recordInfo);
            }
        }
        return recordInfos;
    }

    public void setValues(String text) throws NumberFormatException {
        String[] splits = text.split(",");
        Object[] newValues = new Object[splits.length];
        for (int i = 0; i < newValues.length; i++) {
            switch (dataType) {
                case Float:
                    newValues[i] = Float.parseFloat(splits[i]);
                    break;
                case StringVar:
                    newValues[i] = splits[i];
                    break;
                default:
                    newValues[i] = Integer.parseInt(splits[i]);
                    break;
            }
        }
        this.values = newValues;
    }

    @Override
    public String toString() {
        return this.name + " = " + (values.length == 1 ? values[0] : Arrays.toString(values));
    }

    public int getValueCount() {
        return values.length;
    }

}
