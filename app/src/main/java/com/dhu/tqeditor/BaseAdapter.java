package com.dhu.tqeditor;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.dhu.tqeditor.model.Deletable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public abstract class BaseAdapter<T> extends RecyclerView.Adapter<BaseAdapter.ViewHolder> {

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView textView1;
        public TextView textView2;
        public ViewHolder(View v) {
            super(v);
            textView1 = v.findViewById(android.R.id.text1);
            textView2 = v.findViewById(android.R.id.text2);
        }
    }

    protected String searchText;
    protected Pattern pattern;
    protected List<T> dataList;
    protected AdapterView.OnItemClickListener itemClickListener;
    protected BaseAdapter(AdapterView.OnItemClickListener itemClickListener) {
        this.dataList = new ArrayList<>();
        this.itemClickListener = itemClickListener;
    }

    protected abstract List<T> getAllData();

    public boolean canDelete() {
        return false;
    }

    public void removeItem(int position) {
        if (!canDelete()) {
            return;
        }
        T data = dataList.remove(position);
        if (data instanceof Deletable) {
            ((Deletable) data).setDeleted(true);
        }
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, dataList.size());
    }

    public void restoreItem(T data, int position) {
        if (!canDelete()) {
            return;
        }
        if (data instanceof Deletable) {
            ((Deletable) data).setDeleted(false);
        }
        dataList.add(position, data);
        // notify item added by position
        notifyItemInserted(position);
    }

    protected boolean acceptData(T data) {
        if (data instanceof Deletable && ((Deletable) data).isDeleted()) {
            return false;
        }
        if (pattern == null) {
            if (!searchText.isEmpty()) {
                List<String> texts = getSearchTexts(data);
                if (texts == null || texts.isEmpty()) {
                    return false;
                }
                for (String text : texts) {
                    if (text.toLowerCase().contains(searchText.toLowerCase())) {
                        return true;
                    }
                }
                return false;
            }
            return true;
        }
        List<String> texts = getSearchTexts(data);
        if (texts == null || texts.isEmpty()) {
            return false;
        }
        for (String text : texts) {
            if (pattern.matcher(text).find()) {
                return true;
            }
        }
        return false;
    }

    protected abstract List<String> getSearchTexts(T data);

    public void setSearchText(String text) {
        this.searchText = text != null ? text.trim() : "";
        if (this.searchText.isEmpty()) {
            pattern = null;
        } else {
            try {
                pattern = Pattern.compile(text, Pattern.CASE_INSENSITIVE);
            } catch (Exception e) {
                pattern = null;
            }
        }
        List<T> allData = getAllData();
        if (allData == null) {
            return;
        }
        ArrayList<T> newList = new ArrayList<>();
        for (T data : allData) {
            if (acceptData(data)) {
                newList.add(data);
            }
        }
        this.dataList = newList;
        this.notifyDataSetChanged();
    }

    public String getSearchText() {
        return searchText;
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public T getData(int index) {
        return dataList.get(index);
    }

    protected abstract CharSequence getText1(T data);
    protected abstract CharSequence getText2(T data);

    // Create new views (invoked by the layout manager)
    @Override
    public BaseAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        return new BaseAdapter.ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(BaseAdapter.ViewHolder holder, final int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        T data = getData(position);
        holder.textView1.setText(getText1(data));
        holder.textView2.setText(getText2(data));
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                itemClickListener.onItemClick(null, v, position, -1);
            }
        });
    }

}