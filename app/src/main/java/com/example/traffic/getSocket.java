package com.example.traffic;

import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by 이명남 on 2019-06-05.
 */

public class getSocket {
    String route[];
    private Socket clientSocket;
    private BufferedReader socketIn;
    private PrintWriter socketOut;
    private int port = 7777;
    private final String ip = "125.183.218.34";
    private MyHandler myHandler;
    private MyThread myThread;

    String socket_b="";
    String output="";
    static float predict[][] = new float[3][24];
    static float sum[];
    static float avg[];
    static int minIndex;
    static int maxIndex;
    getSocket(){
        route = MainActivity.getStringarr();
        output += route.length-1 +" ";
        avg = new float[route.length+1];
        for(int i=0; i<route.length-1; i++){
           output +=(route[i]+"_"+route[i+1]+" ");
        }

        connect();
        for(int i=0; i<route.length-1; i++){
             String temp[] =socket_b.split(">");
             predict[i]=calculate(temp[i]);
        }
        sumRoute();
    }
    //float 배열로 바꿔줌
    float[] calculate(String s) {
        float value_i[] = new float[24];
        String split[] = s.split("]");
        String value[] = new String[24];
        for (int i = 0; i < 24; i++) {
            String temp[] = split[i].split(",");
            value[i] = temp[1];
            value[i] = value[i].substring(2, value[i].length() - 1);
        }
        for (int i = 0; i < 24; i++) {
            value_i[i] = Float.parseFloat(value[i]);
            Log.v("뀨", value[i] + "");
        }
        avg[0]=0; avg[route.length]=0;
        for(int i=1; i<route.length-1; i++){
            String t;
            String temp[] = split[i].split(",");
            t=temp[2].substring(2, temp[2].length()-1);
            avg[i] = Float.parseFloat(t);
        }
        return value_i;
    }

    //경로별 시간 합
    void sumRoute(){
        sum = new float[24];
        for(int i=0; i<route.length-1; i++){
            for(int j=0; j<24; j++){
                sum[j] += predict[i][j];
            }
        }
        for(int i=0; i<24; i++){
            sum[i]/=60;
        }
        minIndex=0;
        maxIndex=0;
        for(int i=0; i<24; i++){
            if(sum[i]<sum[minIndex]) minIndex = i;
            if(sum[i]>sum[maxIndex]) maxIndex = i;
        }
    }
    public void connect(){
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        try {
            clientSocket = new Socket(ip, port);
            socketOut = new PrintWriter(clientSocket.getOutputStream(), true);
            socketOut.println(output);
            socketIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            socket_b = socketIn.readLine();
            Log.v("소켓", socket_b);

        } catch (Exception e) {
            e.printStackTrace();
        }

        myHandler = new MyHandler();
        myThread = new MyThread();
        myThread.start();
    }
    class MyThread extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    // InputStream의 값을 읽어와서 data에 저장
                    if(socketIn!=null){
                        String data = socketIn.readLine();
                        // Message 객체를 생성, 핸들러에 정보를 보낼 땐 이 메세지 객체를 이용
                        Message msg = myHandler.obtainMessage();
                        msg.obj = data;
                        myHandler.sendMessage(msg);}
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
        }
    }
}
