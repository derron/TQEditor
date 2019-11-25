package com.dhu.tqeditor.model;

import android.text.TextUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class Record {

    public static final Pattern RECORD_PATTERN = Pattern.compile("records.*?\\.dbr", Pattern.CASE_INSENSITIVE);

    private String id;
    private String type;
    private List<Variable> variables;

    public Record(String id, String type) {
        this.id = id;
        this.type = type;
        this.variables = new ArrayList<>();
    }

    void addVariable(Variable variable) {
        variables.add(variable);
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public List<Variable> getVariables() {
        return variables;
    }

    static Record decode(byte[] data, String id, String recordType, ArzFile arzFile) throws IOException {
        Record record = new Record(id, recordType);
        try (ByteArrayInputStream bi = new ByteArrayInputStream(data);
             DataInputStream stream = new DataInputStream(bi)) {
            while (stream.available() > 0) {
                short dataType = Short.reverseBytes(stream.readShort());
                short valCount = Short.reverseBytes(stream.readShort());
                int variableID = Integer.reverseBytes(stream.readInt());
                String variableName = arzFile.getString(variableID);
                if (variableName == null || variableName.isEmpty()) {
                    throw new IOException("Failed to load record, invalid variableID: " + variableID);
                }
                if (dataType < 0 || dataType > 3) {
                    throw new IOException("Failed to load record, invalid dataType: " + dataType);
                }
                if (valCount < 1) {
                    throw new IOException("Failed to load record, invalid valCount: " + valCount);
                }
                Variable v = new Variable(variableID, variableName, DataType.values()[dataType], valCount);
                v.decode(stream, arzFile);
                record.addVariable(v);
            }
        }
        return record;
    }

    byte[] encode() throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             DataOutputStream stream = new DataOutputStream(outputStream)) {
            for (Variable v : variables) {
                if (v.isDeleted()) {
                    continue;
                }
                stream.writeShort(Short.reverseBytes((short) v.getDataType().ordinal()));
                stream.writeShort(Short.reverseBytes((short) v.getValueCount()));
                stream.writeInt(Integer.reverseBytes(v.getId()));
                for (Object value : v.getValues()) {
                    switch (v.getDataType()) {
                        case Float:
                            stream.writeInt(Integer.reverseBytes(Float.floatToRawIntBits((Float) value)));
                            break;
                        case StringVar:
                            String[] splits = value.toString().split("\\|");
                            stream.writeInt(Integer.reverseBytes(Integer.parseInt(splits[splits.length-1])));
                            break;
                        default:
                            stream.writeInt(Integer.reverseBytes((Integer) value));
                            break;
                    }
                }
            }
            stream.flush();
            return outputStream.toByteArray();
        }
    }

    private static Set<String> NAME_TAGS = new HashSet<>();
    static {
        NAME_TAGS.add("itemNameTag");
        NAME_TAGS.add("setName");
        NAME_TAGS.add("lootRandomizerName");
        NAME_TAGS.add("description");
    }

    public CharSequence getName(ArcFile arcFile) {
        String textId = null;
        for (Variable variable : variables) {
            if (variable.getValueCount() == 1 && NAME_TAGS.contains(variable.getName())) {
                textId = variable.getValueString().replaceAll("\\|\\d+$", "");
                CharSequence name = arcFile.getString(textId);
                if (!TextUtils.isEmpty(name)) {
                    return name;
                }
            }
        }
        return textId;
    }

}
