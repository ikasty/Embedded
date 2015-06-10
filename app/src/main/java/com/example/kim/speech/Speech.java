package com.example.kim.speech;

import android.app.DownloadManager;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognizerIntent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import net.daum.mf.speech.api.SpeechRecognizeListener;
import net.daum.mf.speech.api.SpeechRecognizerClient;
import net.daum.mf.speech.api.SpeechRecognizerManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;


public class Speech extends ActionBarActivity {
    public SpeechRecognizerClient.Builder builder;
    public SpeechRecognizerClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speech);

        SpeechRecognizerManager.getInstance().initializeLibrary(this);

        builder = new SpeechRecognizerClient.Builder().
                setApiKey("076710d74a3fa3a441bfee2175219421").
                setServiceType(SpeechRecognizerClient.SERVICE_TYPE_WEB);
        client = builder.build();
        client.setSpeechRecognizeListener(new SpeechRecognizeListener() {
            @Override
            public void onReady() {
                //To change body of implemented methods use File | Settings | File Templates.
                Log.i("Speech", "Start!");
            }

            @Override
            public void onBeginningOfSpeech() {
            }

            @Override
            public void onEndOfSpeech() {
            }

            @Override
            public void onError(int i, String s) {
                Log.e("SPEECH", s);
            }

            @Override
            public void onPartialResult(String s) {
                Log.e("Speech123", s);
            }

            @Override
            public void onResults(Bundle bundle) {
                list = bundle.getStringArrayList(SpeechRecognizerClient.KEY_RECOGNITION_RESULTS);
                bhandler.sendMessage(bhandler.obtainMessage());
                changeStartHandler.sendMessage(changeStartHandler.obtainMessage());
            }

            @Override
            public void onAudioLevel(float v) {
            }

            @Override
            public void onFinished() {
            }
        });

        Button button = (Button) findViewById(R.id.speech_toggle);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button b = (Button)v;
                String text = b.getText().toString();
                if(text.equals("Start!")){
                    b.setText("Stop!");
                    client.startRecording(true);
                }
                else{
                    b.setText("Start!");
                    client.stopRecording();
                }
            }
        });

        View.OnClickListener textOnclick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("speech_test", "test!");
                TextView tv = (TextView) v;
                String text = tv.getText().toString();
                Bundle data = new Bundle();
                data.putString("data", text);
                Message msg = viewHandler.obtainMessage();
                msg.setData(data);
                viewHandler.sendMessage(msg);
            }
        };

        TextView text = (TextView) findViewById(R.id.text0);
        text.setOnClickListener(textOnclick);
        text = (TextView) findViewById(R.id.text1);
        text.setOnClickListener(textOnclick);
        text = (TextView) findViewById(R.id.text2);
        text.setOnClickListener(textOnclick);
    }


    private ArrayList list;
    final Handler bhandler = new Handler()
    {
        public void handleMessage(Message msg){
            int sizeOfList = 3;
            if(list.size() < 3) sizeOfList = list.size();
            int i;
            for(i=0; i<sizeOfList; i++){
                int resID = getResources().getIdentifier("text"+i,
                        "id", getPackageName());
                TextView text = (TextView) findViewById(resID);
                Log.i("list_size", String.valueOf(list.size()));
                text.setText(list.get(i).toString());
            }
            for(i = sizeOfList; i<3; i++){
                int resID = getResources().getIdentifier("text"+i,
                        "id", getPackageName());
                TextView text = (TextView) findViewById(resID);
                Log.i("list_size", String.valueOf(list.size()));
                text.setText("검색 결과가 없습니다.");
            }
        }
    };

    final Handler viewHandler = new Handler()
    {
        public void handleMessage(Message msg){
            String text = msg.getData().getString("data");
            Log.i("handleMsg", text);
            if(text.equals("날씨")){
                Log.i("handleMsg", "profit!");
                new WeatherRequest().execute(null, null, null);
            }
        }
    };

    final Handler toastHandler = new Handler()
    {
        public void handleMessage(Message msg){
            double rain = msg.getData().getDouble("rain");

            Toast.makeText(getApplicationContext(),
                    "앞으로 9시간동안 강수량: " + rain*100 + "mm", Toast.LENGTH_SHORT).show();
        }
    };

    private class WeatherRequest extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try
            {
                HttpClient client = new DefaultHttpClient();
                String getURL = "http://api.openweathermap.org/data/2.5/forecast?lat=35&lon=125";
                HttpGet get = new HttpGet(getURL);
                HttpResponse responseGet = client.execute(get);
                HttpEntity resEntityGet = responseGet.getEntity();
                double rain = 0;
                if (resEntityGet != null) {                    JSONObject mainObject = new JSONObject(EntityUtils.toString(resEntityGet));

                    for(int i=0; i<3; i++) {
                        JSONObject threeh = mainObject.getJSONArray("list").getJSONObject(i);
                        if (threeh.has("rain")) {
                            rain += threeh.getDouble("3h");
                        }
                    }
                }
                else {
                    rain = -1;
                }
                Bundle data = new Bundle();
                data.putDouble("rain", rain);
                Message msg = toastHandler.obtainMessage();
                msg.setData(data);
                toastHandler.sendMessage(msg);

                CheckBox checkbox = (CheckBox) findViewById(R.id.checkbox);
                if(rain>0 || checkbox.isChecked()){
                    // Send signal to ardoino
                    Log.i("Rain","비와요!!!");
                }

            } catch (Exception e){
                Log.i("RESPONSE", "error!");
                e.printStackTrace();
            }
            return null;
        }
    }
    final Handler changeStartHandler = new Handler()
    {
        public void handleMessage(Message msg){
            Button b = (Button) findViewById(R.id.speech_toggle);
            b.setText("Start!");
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_speech, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
