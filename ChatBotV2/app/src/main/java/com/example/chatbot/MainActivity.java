package com.example.chatbot;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import com.example.chatbot.apibot.ChatterBot;
import com.example.chatbot.apibot.ChatterBotFactory;
import com.example.chatbot.apibot.ChatterBotSession;
import com.example.chatbot.apibot.ChatterBotType;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener{

    private static final int REQUEST_CODE_SPEECH_INPUT = 1000;
    private TextView tvBot, tvHumano;
    private String mensajeHumano = "";
    private String enlace = "https://www.bing.com/ttranslatev3";
    private String respuestaBot ="";
    private static final String TAG = "TextToSpeechDemo";
    private TextToSpeech mTts;
    private String toSpeech = "";
    private ImageView ivMic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tvBot = findViewById(R.id.tvBot);
        tvHumano = findViewById(R.id.tvHumano);

        mTts = new TextToSpeech(this,
                this
        );

        ivMic = findViewById(R.id.ivMic);
        ivMic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speak();
            }
        });
    }

    private void speak() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hola, dí algo");

        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
        }catch (Exception e){
            Toast.makeText(this, e.getMessage().toString(), Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void chat() {
        try {
            ChatterBotFactory factory = new ChatterBotFactory();
            ChatterBot bot1 = factory.create(ChatterBotType.PANDORABOTS, "b0dafd24ee35a477");
            ChatterBotSession bot1session = bot1.createSession();
            String r = mensajeHumano;
            String parametros = formarParametros("es", "en", r);
            r = postHttps(enlace, parametros);
            r = filterTranslation(r);

            r = bot1session.think(r);
            parametros = formarParametros("en", "es", r);
            r = postHttps(enlace, parametros);
            r = filterTranslation(r);
            respuestaBot = r;

        } catch (Exception e) {

        }
    }

    private class Chat extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            chat();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    escribirRespuesta(respuestaBot);
                }
            });
            return null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

    }

    private void escribirRespuesta(String respuesta) {
        tvBot.setText(respuesta);
        toSpeech = respuesta;
        sayText();
    }

    //https://www.bing.com/ttranslatev3 mediante post y con el cuerpo &fromLang=es&text=soy%20programador&to=en

    public String formarParametros(String original, String traducido, String texto) {
        try {
            HashMap<String, String> httpBodyParams;
            httpBodyParams = new HashMap<>();
            httpBodyParams.put("fromLang", original);
            httpBodyParams.put("to", traducido);
            httpBodyParams.put("text", texto);

            StringBuilder result = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, String> entry : httpBodyParams.entrySet()) {
                if (first)
                    first = false;
                else
                    result.append("&");
                result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                result.append("=");
                result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            }
            return result.toString();

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    public String postHttps(String src, String body) {
        StringBuffer buffer = new StringBuffer();
        try {
            URL url = new URL(src);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Accept", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.connect();
            OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
            out.write(body);
            out.flush();
            out.close();
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                buffer.append(line + "\n");
            }
            in.close();
        } catch (IOException e) {
        }
        return buffer.toString();
    }

    public String filterTranslation(String cadena){
        String resultado = "";
        JSONArray jArray = null;
        JSONObject jObject = null;

        try {
            //Obtenemos la única respuesta que tenemos
            jArray = new JSONArray(cadena);
            cadena = jArray.get(0).toString();


            jObject = new JSONObject(cadena);
            cadena = jObject.get("translations").toString();

            jArray = new JSONArray(cadena);
            cadena = jArray.get(0).toString();

            jObject = new JSONObject(cadena);
            cadena = jObject.get("text").toString();
            resultado = cadena;

        } catch (JSONException e) {
            Log.v("---error---", e.toString());
        }

        return resultado;
    }

    @Override
    public void onDestroy() {
        if (mTts != null) {
            mTts.stop();
            mTts.shutdown();
        }
        super.onDestroy();
    }

    public void onInit(int status) {
        Locale spanish = new Locale ("es","ES");
        if (status == TextToSpeech.SUCCESS) {
            int result = mTts.setLanguage(spanish);
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Language is not available.");
            } else {
                sayText();
            }
        } else {
            Log.e(TAG, "Could not initialize TextToSpeech.");
        }
    }

    private static final Random RANDOM = new Random();
    private void sayText() {
        mTts.setPitch(-500);
        mTts.speak(toSpeech,
                TextToSpeech.QUEUE_FLUSH,
                null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode){
            case REQUEST_CODE_SPEECH_INPUT:{
                if(resultCode == RESULT_OK && data != null){
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    tvHumano.setText(result.get(0));
                    mensajeHumano = result.get(0);
                    if (!mensajeHumano.equalsIgnoreCase("")) {
                        new Chat().execute();
                    }
                }
                break;
            }
        }
    }
}
