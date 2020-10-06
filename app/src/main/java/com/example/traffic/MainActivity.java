package com.example.traffic;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    private String departure, arrive;
    Button btn;
    Spinner departure_s, arrive_s;
    int d_index, a_index;
    private String city[];
    static String[] route;
    String output;
    static int hour;
    getSocket socket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        output="";
        departure_s = findViewById(R.id.departure_spinner);
        arrive_s = findViewById(R.id.arrive_spinner);
        btn = findViewById(R.id.go);
        city=getResources().getStringArray(R.array.departure_all);
        Calendar cal = Calendar.getInstance();
        hour = cal.get(Calendar.HOUR_OF_DAY);

        setSpinner();

            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.v("확인", departure+"::"+arrive);
                    if(d_index!=0 && a_index!=0){
                    go_intent();
                    socket = new getSocket();
                    }}
            });

    }
    public void setSpinner(){
        ArrayAdapter departureAdapter = ArrayAdapter.createFromResource(this, R.array.departure_all, android.R.layout.simple_spinner_item);
        departure_s.setAdapter(departureAdapter);
        departure_s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            int arrive_array[] ={R.array.arrive_seoul, R.array.arrive_gwangju, R.array.arrive_other, R.array.departure_incheon};

            int temp;
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                departure =(String)departure_s.getSelectedItem();
                d_index = position;
                if(departure.equals("서울")) temp=0;
                else if(departure.equals("광주")) temp=1;
                else if(departure.equals("인천") || departure.equals("동해")) temp=3;
                else temp=2;

                ArrayAdapter arriveAdapter = ArrayAdapter.createFromResource(MainActivity.this, arrive_array[temp], android.R.layout.simple_spinner_item);
                arrive_s.setAdapter(arriveAdapter);
                arrive_s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){

                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        arrive =(String)arrive_s.getSelectedItem();
                        for(int i=0; i<city.length; i++){
                            if(city[i].equals(arrive)) a_index=i;
                        }

                    }
                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

    }
    public void go_intent(){
        Intent intent = new Intent(MainActivity.this, MapActivity.class);

        getRoute();

        intent.putExtra("departure", departure);
        intent.putExtra("arrive", arrive);
        intent.putExtra("route", route);
        Log.v("route", output);
        startActivity(intent);
    }
    public void getRoute(){
        String city_ENG[]=getResources().getStringArray(R.array.city);
        String name=city_ENG[d_index]+"_"+city_ENG[a_index];
        int resID = getResources().getIdentifier(name, "array", this.getPackageName());
        if(resID!=0x0)
            route = getResources().getStringArray(resID);
    }

    static String[] getStringarr(){
        return route;
    }
}
