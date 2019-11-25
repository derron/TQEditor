package com.dhu.tqeditor;

import android.widget.AdapterView;
import com.dhu.tqeditor.model.DataType;
import com.dhu.tqeditor.model.Record;
import com.dhu.tqeditor.model.Variable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VariableAdapter extends BaseAdapter<Variable>{

    private Record record;
    private boolean hideEmptyVariable;
    public VariableAdapter(AdapterView.OnItemClickListener itemClickListener) {
        super(itemClickListener);
        this.record = null;
    }

    public Record getRecord() {
        return record;
    }

    @Override
    protected List<Variable> getAllData() {
        if (record == null) {
            return null;
        }
        return record.getVariables();
    }

    @Override
    protected List<String> getSearchTexts(Variable data) {
        return Collections.singletonList(data.getName());
    }

    @Override
    protected boolean acceptData(Variable data) {
        if (hideEmptyVariable
                && data.getDataType() != DataType.StringVar
                && data.getValueCount() == 1
                && Math.abs(Float.parseFloat(data.getValueString())) < 0.001) {
            return false;
        }
        return super.acceptData(data);
    }

    @Override
    public boolean canDelete() {
        return true;
    }

    public void setRecord(Record record) {
        this.record = record;
        setSearchText(searchText);
    }

    public void setHideEmptyVariable(boolean hideEmptyVariable) {
        this.hideEmptyVariable = hideEmptyVariable;
        setSearchText(searchText);
    }

    @Override
    protected CharSequence getText1(Variable data) {
        return data.getName();
    }

    @Override
    protected CharSequence getText2(Variable data) {
        return data.getValueString();
    }

}
