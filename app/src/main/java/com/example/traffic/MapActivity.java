package com.example.traffic;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapMarkerItem;
import com.skt.Tmap.TMapPOIItem;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapPolyLine;
import com.skt.Tmap.TMapView;

public class MapActivity extends AppCompatActivity {
    TMapView tmapview;
    FrameLayout frameLayout;
    TMapPoint tMapPoint[];
    TMapPOIItem point1, point2;
    TMapData tmapdata;
    TextView set1, set2, predict_t, recommand_t;
    Button graph;
    String departure, arrive;
    String route[];
    int d_p=1, a_p=1;
    getLatLon getlatlon;
    Context context;
    float result[]=new float[24];
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        tmapdata = new TMapData();
        point1 = new TMapPOIItem();
        point2 = new TMapPOIItem();
        getlatlon = new getLatLon();
        tMapPoint = new TMapPoint[12];
        context  = getApplicationContext();
        set1 = findViewById(R.id.set1);
        set2 = findViewById(R.id.set2);
        predict_t = findViewById(R.id.predict_time);
        recommand_t = findViewById(R.id.recommand_time);
        graph = findViewById(R.id.go_graph);
        graph.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MapActivity.this, GraphActivity.class);
                startActivity(intent);
            }
        });
        frameLayout = (FrameLayout)findViewById(R.id.map_container);
        tmapview = new TMapView(this);
        tmapview.setSKTMapApiKey("fa5704ee-50f7-4bab-805c-ff16ff302e72");
        frameLayout.addView(tmapview);
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.flags);

        getInformation();
        getLatLon();
        TMapMarkerItem markerItem1 = new TMapMarkerItem();
        markerItem1.setTMapPoint(tMapPoint[d_p]);
        markerItem1.setIcon(bitmap);
        TMapMarkerItem markerItem2 = new TMapMarkerItem();
        markerItem2.setTMapPoint(tMapPoint[a_p]);
        markerItem2.setIcon(bitmap);
        tmapview.addMarkerItem("markerItem1", markerItem1);
        tmapview.addMarkerItem("markerItem2", markerItem2);

        tmapview.setZoomLevel(7);
        tmapview.setCenterPoint(128.2139691,36.3883917);
        Log.v("아제발", "들어왔당1");
        new DrawLineTask().execute();
    }
    public void getInformation(){
        Intent intent = getIntent();

        departure = intent.getExtras().getString("departure");
        arrive = intent.getExtras().getString("arrive");
        route = intent.getExtras().getStringArray("route");
        set1.setText(departure);
        set2.setText(arrive);
        float temp = getSocket.sum[0];
        predict_t.setText(((int)(temp/60))+"시간 "+Math.round(temp%60) +"분");
        if(getSocket.minIndex+MainActivity.hour==0) recommand_t.setText((getSocket.minIndex+MainActivity.hour-1)%24+24+"시");
        else recommand_t.setText((getSocket.minIndex+MainActivity.hour-1)%24+"시");
    }
    public void getLatLon(){
        String[] temp = getResources().getStringArray(R.array.departure_all);
        for(int i=0 ; i<12; i++){
            if(temp[i].equals(departure)) d_p = i;
            if(temp[i].equals(arrive)) a_p = i;
        }
        tMapPoint[1] = new TMapPoint(37.5657374,126.9769096); //서울
        tMapPoint[2]= new TMapPoint(35.1600695,126.8514351); //광주
        tMapPoint[3] = new TMapPoint(35.1797913,129.074987); //부산
        tMapPoint[4] = new TMapPoint(35.8684263,128.59753); //대구
        tMapPoint[5]= new TMapPoint(36.349349,127.3828368); //대전
        tMapPoint[6] = new TMapPoint(35.5106718,129.301614); //울산
        tMapPoint[7] = new TMapPoint(36.4814782,127.2878852); //세종
        tMapPoint[8]= new TMapPoint(37.5233869,129.1110912); //동해
        tMapPoint[9] = new TMapPoint(37.4560897,126.7048214); //인천
        tMapPoint[10] = new TMapPoint(37.7519967,128.8737637); //강릉
        tMapPoint[11]= new TMapPoint(35.8241932,127.1480005); //전주
    }
    private class DrawLineTask extends AsyncTask {
        TMapPolyLine tMapPolyLine[];
        @Override
        protected Object doInBackground(Object[] objects) {
            try {

                int i=0;
                tMapPolyLine = new TMapPolyLine[route.length+1];
                tMapPolyLine[0] = new TMapData().findPathData(tMapPoint[d_p], getlatlon.map.get(route[0]));

                for(i=0; i<route.length-1; i++){
                    tMapPolyLine[i+1] = new TMapData().findPathData(getlatlon.map.get(route[i]), getlatlon.map.get(route[i+1]));
                }
                tMapPolyLine[route.length] = new TMapData().findPathData(getlatlon.map.get(route[i]), tMapPoint[a_p]);

                for(i=0; i<route.length+1; i++){
                    //int color[] = {Color.RED, Color.GREEN, Color.YELLOW};//빨, 초, 노
                    int temp=1;
                    tMapPolyLine[i].setLineColor( Color.GREEN);
                    if(i>0 && i<route.length){
                        if(getSocket.avg[i]<=-0.01)
                            tMapPolyLine[i].setLineColor(Color.GREEN);
                        else if(getSocket.avg[i]<=0.01)
                            tMapPolyLine[i].setLineColor(Color.YELLOW);
                        else
                            tMapPolyLine[i].setLineColor(Color.RED);
                    }
                    tmapview.addTMapPolyLine("Line"+(i+1), tMapPolyLine[i]);
                    Log.v("그리기"+i, temp+"성공");
                }
            }catch(Exception e) {
                e.printStackTrace();
            }
            return tMapPolyLine;
        }
    }
}