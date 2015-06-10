package com.example.kim.speech;

import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class Speech extends ActionBarActivity {
    private SpeechRecognizerClient.Builder builder;
    private SpeechRecognizerClient client;

    private TextView mTextView;
    private ProgressDialog mProgressDialog;
    private BluetoothAdapter[] mBTAdapter;
    private BluetoothSocket[] mBTSocket;
    private int hit = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speech);

        // receiver 등록
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        // 연결중 메시지 출력
        mProgressDialog = ProgressDialog.show(this, "", "connecting...");

        // 로그 출력 뷰 설정
        mTextView = (TextView)findViewById(R.id.textView);
        mTextView.setTextSize(40);

        // 내부 변수 선언
        mBTAdapter = new BluetoothAdapter[2];
        mBTSocket = new BluetoothSocket[2];
        mBTSocket[0] = mBTSocket[1] = null;

        // 블루투스 어댑터 설정
        mBTAdapter[0] = BluetoothAdapter.getDefaultAdapter();
        mBTAdapter[1] = BluetoothAdapter.getDefaultAdapter();

        if (mBTAdapter[0].isDiscovering()) mBTAdapter[0].cancelDiscovery();
        if (mBTAdapter[1].isDiscovering()) mBTAdapter[1].cancelDiscovery();

        mBTAdapter[0].startDiscovery();
        mBTAdapter[1].startDiscovery();

        // 음성인식 init
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

        // 음성인식 버튼 등록
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

        // 텍스트 이벤트 등록
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

        Log.d("Embedded", "onCreate Finish");
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

    // 비올 확률 리턴
    private static int isRain = 0;
    public static int getRain()
    {
        int ret = isRain;
        isRain = 0;
        return ret;
    }

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
                if (resEntityGet != null) {
                    JSONObject mainObject = new JSONObject(EntityUtils.toString(resEntityGet));

                    // 최근 9시간의 예상 강수량의 합 구하기
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
                    isRain = 1;
                    Log.i("Rain","비와요!!!");
                }
                else isRain = 0;

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

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        mBTAdapter[0].cancelDiscovery();
        mBTAdapter[1].cancelDiscovery();
        try
        {
            mBTSocket[0].close();
            mBTSocket[1].close();
        }
        catch (Exception e)
        {
            Log.d("Error", e.toString());
        }

        SpeechRecognizerManager.getInstance().finalizeLibrary();
    }

    // 브로드캐스트 리시버
    private final BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();

            Log.d("Embedded", "onReceive receives " + action);
            if (BluetoothDevice.ACTION_FOUND.equals(action))
            {
                int found = -1;
                String UUID_str = "";

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device == null)
                {
                    Log.d("BTchat", "No device!");
                    return ;
                }

                Log.d("BTchat", "device name: " + device.getName());

                // 디바이스 잡기
                if (device.getName().equals("mybt12"))            // 우산
                {
                    found = 0;
                    UUID_str = "00001101-0000-1000-8000-00805f9b34fb";
                }
                else if (device.getName().equals("IM-A850S"))     // 핸드폰
                {
                    found = 1;
                    UUID_str = "00000000-0000-1000-8000-00805F9B34FB";
                }

                if (found == -1) return ;

                Log.d("BTchat", "hit: " + device.getAddress());

                // 둘 다 찾으면 메시지 삭제
                if (mBTSocket[0] != null && mBTSocket[1] != null)
                {
                    hit++;
                    mBTAdapter[0].cancelDiscovery();
                    mBTAdapter[1].cancelDiscovery();
                }

                try
                {
                    if (mBTSocket[found] != null) return ;

                    mBTSocket[found] = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString(UUID_str));
                    mBTSocket[found].connect();
                    if (mBTSocket[found].isConnected())
                    {
                        IOThread mioThread = new IOThread(found, mBTSocket[found].getInputStream(), mBTSocket[found].getOutputStream());
                        mioThread.start();
                    }
                }
                catch (Exception e)
                {
                    Log.d("Error", e.toString());
                }
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
            {
                if (hit < 2) Log.d("BTchat", "Bluetooth device not found. " + hit + " device found.");
                mProgressDialog.dismiss();
            }
        }
    };

    private class IOThread extends Thread
    {
        private InputStream mIStream;
        private OutputStream mOStream;
        private int type;

        public IOThread(int type, InputStream istream, OutputStream ostream)
        {
            super();
            this.type = type;
            mIStream = istream;
            mOStream = ostream;
        }

        public void run()
        {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true)
            {
                try
                {
                    if (type == 0 && Speech.getRain() == 1) // 우산이면서 비오면
                    {
                        Log.d("BTchat", "IOThread: send rain message");
                        mOStream.write("A".getBytes());
                        sleep(1000);
                    }
                    //bytes = mIStream.read(buffer);
                    //Log.d("BTchat", "IOThread: Read " + bytes + " bytes");
                    //mHandler.obtainMessage(17, bytes, -1, buffer).sendToTarget();

                    //buffer = new byte[1024];
                }
                catch (Exception e)
                {
                    Log.e("Error", e.toString());
                    break;
                }
            }

            try
            {
                mIStream.close();
                mOStream.close();
            }
            catch (Exception e)
            {
                Log.e("Error", e.toString());
            }

        }

        Handler mHandler = new Handler()
        {
            private String s = "Message received: ";
            private byte[] buffer = new byte[1024];
            private int i = 0;

            @Override
            public void handleMessage(Message msg)
            {
                if (msg.what == 17)
                {
                    byte[] readBuf = (byte[]) msg.obj;
                    Log.d("BTchat", "Original message: " + new String(readBuf, 0, msg.arg1));

                    for (int j = 0; j <= msg.arg1; j++)
                    {
                        if (readBuf[j] != ';')
                        {
                            buffer[i++] = readBuf[j];
                        }
                        else
                        {
                            mTextView.setText(s + new String(buffer, 0, i));
                            try
                            {
                                mOStream.write(buffer, 0, i);
                            }
                            catch (Exception e)
                            {
                                Log.e("Error", e.toString());
                            }
                            i = 0;
                            buffer = new byte[1024];
                        }
                    }
                }
                else if (msg.what == 9999)
                {
                    Log.d("BTchat", "Will send rain alarm");
                    buffer[0] = 'A';
                    try
                    {
                        mOStream.write(buffer, 0, 2);
                    }
                    catch (Exception e)
                    {
                        Log.e("Error", e.toString());
                    }

                    buffer = new byte[1024];
                }
            }
        };
    }
}
