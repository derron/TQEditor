package com.dhu.tqeditor;

import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.dhu.tqeditor.model.ArzFile;
import com.dhu.tqeditor.model.RecordInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class StringAdapter extends BaseAdapter<Pair<Integer, String>>{

    private ArzFile arzFile;
    public StringAdapter(AdapterView.OnItemClickListener itemClickListener) {
        super(itemClickListener);
        this.arzFile = null;
    }

    @Override
    protected List<Pair<Integer, String>> getAllData() {
        List<Pair<Integer, String>> data = new ArrayList<>(arzFile.getStringCount());
        for (int i = 0; i < arzFile.getStringCount(); i++) {
            data.add(Pair.create(i, arzFile.getString(i)));
        }
        return data;
    }

    @Override
    protected List<String> getSearchTexts(Pair<Integer, String> data) {
        return Collections.singletonList(data.second);
    }

    public void setArzFile(ArzFile arzFile) {
        this.arzFile = arzFile;
        setSearchText(searchText);
    }

    @Override
    protected CharSequence getText1(Pair<Integer, String> data) {
        return data.second;
    }

    @Override
    protected CharSequence getText2(Pair<Integer, String> data) {
        return String.valueOf(data.first);
    }

    public int getStringId(int index) {
        return getData(index).first;
    }

}
