package com.dhu.tqeditor;

import android.widget.AdapterView;
import com.dhu.tqeditor.model.ArcFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TextAdapter extends BaseAdapter<Map.Entry<String, CharSequence>>{

    private ArcFile arcFile;
    public TextAdapter(AdapterView.OnItemClickListener itemClickListener) {
        super(itemClickListener);
        this.arcFile = null;
    }

    @Override
    protected List<Map.Entry<String, CharSequence>> getAllData() {
        return new ArrayList<>(arcFile.getStringMap().entrySet());
    }

    @Override
    protected List<String> getSearchTexts(Map.Entry<String, CharSequence> data) {
        return Arrays.asList(data.getKey(), data.getValue().toString());
    }

    public void setArcFile(ArcFile arcFile) {
        this.arcFile = arcFile;
        setSearchText(searchText);
    }

    @Override
    protected CharSequence getText1(Map.Entry<String, CharSequence> data) {
        return data.getValue();
    }

    @Override
    protected CharSequence getText2(Map.Entry<String, CharSequence> data) {
        return data.getKey();
    }

    public String getTextId(int index) {
        return getData(index).getKey();
    }

}
