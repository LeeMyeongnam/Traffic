package com.example.traffic;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;

public class GraphActivity extends AppCompatActivity {
    LineChart chart;
    int predict_time[];
    ArrayList<Entry> entries;    // 데이터값 리스트
    ArrayList<String> labels;       // 지역 이름
    ArrayList<ItemData> item = new ArrayList<>();
    ListView listview = null;
    int time;
    float value_i[]= new float[24];
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);
        predict_time = new int[24];
        chart = findViewById(R.id.linechart);
        entries = new ArrayList<>();
        labels = new ArrayList<>();
        item = new ArrayList<>();
        time = MainActivity.hour-1;
        value_i = getSocket.sum;
        ItemData mdata1 = new ItemData();
        mdata1.time = "시간";
        mdata1.predict = "예측 시간";
        item.add(mdata1);
        for(int i=0; i<24; i++){
            entries.add(new Entry(value_i[i], i));
            if(time+i<0) labels.add((time+i)%24+24+"");
            else labels.add((time+i)%24+"");

            ItemData mdata = new ItemData();
            if(time+i<0) mdata.time = (time+i)%24+24+"";
            else mdata.time = (time+i)%24+"";
            mdata.predict = ((int)(value_i[i]/60))+"시간 "+Math.round(value_i[i]%60) +"분";
            item.add(mdata);
        }
        LineDataSet dataset = new LineDataSet(entries, "# of Calls");

        LineData data = new LineData(labels, dataset);
        dataset.setColors(new int[]{Color.parseColor("#F9B917")}); //
        dataset.setLineWidth(4f);
        dataset.setCircleColor(Color.parseColor("#F9B917"));
        chart.setData(data);
        chart.getAxisLeft().setDrawGridLines(false);
        chart.getAxisRight().setDrawGridLines(false);
        chart.getAxisRight().setEnabled(false);
        XAxis xAxis = chart.getXAxis();
        xAxis.setDrawGridLines(false);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        chart.getAxisLeft().setStartAtZero(false);
        chart.getAxisRight().setStartAtZero(false);
        chart.animateX(2000);

        listview = findViewById(R.id.listview);
        ListAdapter adapter = new ListAdapter(item);
        listview.setAdapter(adapter);

    }
}
