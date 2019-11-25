package com.dhu.tqeditor;

import android.widget.AdapterView;
import com.dhu.tqeditor.model.ArzFile;
import com.dhu.tqeditor.model.RecordInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecordAdapter extends BaseAdapter<RecordInfo>{

    private ArzFile arzFile;
    public RecordAdapter(AdapterView.OnItemClickListener itemClickListener) {
        super(itemClickListener);
        this.arzFile = null;
    }

    @Override
    protected List<RecordInfo> getAllData() {
        return arzFile.getRecordInfoList();
    }

    public void setArzFile(ArzFile arzFile) {
        this.arzFile = arzFile;
        setSearchText(searchText);
    }

    @Override
    protected List<String> getSearchTexts(RecordInfo data) {
        List<String> texts = new ArrayList<>(2);
        CharSequence name = data.getName();
        if (name != null) {
            texts.add(name.toString());
        }
        texts.addAll(data.getRelativeNames());
        texts.add(data.getNormalizedId());
        return texts;
    }

    @Override
    protected CharSequence getText1(RecordInfo data) {
        return data.getNormalizedId();
    }

    @Override
    protected CharSequence getText2(RecordInfo data) {
        return data.getName();
    }

}

