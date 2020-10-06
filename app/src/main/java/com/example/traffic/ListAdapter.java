package com.example.traffic;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class ListAdapter extends BaseAdapter {
    LayoutInflater inflater = null;
    private ArrayList<ItemData> m_data = null;
    private int m_count = 0;

    public ListAdapter(ArrayList<ItemData> m_data){
        this.m_data = m_data;
        m_count = m_data.size();
    }

    @Override
    public int getCount() {
        return m_count;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null){
            final Context context = parent.getContext();
            if(inflater==null){
                inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            }
        }
        convertView = inflater.inflate(R.layout.listview_item, parent, false);
        TextView time = convertView.findViewById(R.id.time);
        TextView predict = convertView.findViewById(R.id.predict_time);

        time.setText(m_data.get(position).time);
        predict.setText(m_data.get(position).predict);
        return convertView;
    }
}

class ItemData {
    public String time;
    public String predict;
}
