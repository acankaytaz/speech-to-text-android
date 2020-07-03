package com.example.hmi_speechtotext;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import android.speech.RecognizerIntent;
//import android.speech.SpeechRecognizer;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;


import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;



public class MainActivity extends AppCompatActivity implements RecognitionListener {



    private EditText editTextName;
    private EditText editTextAddress;
    private EditText editTextEmail;
    private EditText editTextDescription;



    boolean insideloop = true;

    private boolean name = false;
    private boolean address = false;
    private boolean email = false;
    private boolean description = false;
    private boolean next = false;
    private boolean back = false;

    private Button buttonSpeak;

    private static final String KWS_SEARCH = "wakeup";
    private static final String KEYPHRASE = "hey okay";

    /* Used to handle permission request */
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    private SpeechRecognizer recognizer;
//    private HashMap<String, Integer> captions;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextName = (EditText) findViewById(R.id.editText1);
        editTextAddress = (EditText) findViewById(R.id.editText2);
        editTextEmail = (EditText) findViewById(R.id.editText3);
        editTextDescription = (EditText) findViewById(R.id.editText4);

        buttonSpeak = (Button) findViewById(R.id.button5);
        buttonSpeak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recognizer.stop();
                recognizer.shutdown();
                insideloop=true;
                new SetupTask(MainActivity.this).execute();
            }
        });

        // Check if user has given permission to record audio
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            return;
        }
        // Recognizer initialization is a time-consuming and it involves IO,
        // so we execute it in async task
        new SetupTask(this).execute();
    }

    /**
     * Method for selecting the field to be filled
     * */
    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Select: " +
                "\n Name, Address, Email or Description," +
                "Back, Next; Finish to exit");
        try {
            startActivityForResult(intent, 1);
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(
                    this,
                    "You do not have an application to recognize speech!",
                    Toast.LENGTH_LONG
            ).show();
            String playMarketLink = "https://pkay.google.com/store/search?q=speech recognizer&c=apps";
            Intent playMarket = new Intent(Intent.ACTION_VIEW, Uri.parse(playMarketLink));
            startActivity(playMarket);
        }
    }

    /**
     * Method to introduce the data to fill the field
     * */
    public void startFieldInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());


        if (name) {
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say the name to store");
        }
        if (address) {
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say the address to store");
        }
        if (email) {
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say the email to store");
        }
        if (description) {
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say the description to store");
        }
        startActivityForResult(intent, 2);
    }

    /**
     * Requested method for RecognizerListerner from Speech package
     * */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1) {

            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                name = Arrays.asList(result.get(0).split(" ")).contains("name");
                address = Arrays.asList(result.get(0).split(" ")).contains("address");
                email = Arrays.asList(result.get(0).split(" ")).contains("email");
                description = Arrays.asList(result.get(0).split(" ")).contains("description");
                next = Arrays.asList(result.get(0).split(" ")).contains("next");
                back = Arrays.asList(result.get(0).split(" ")).contains("back");
                boolean finish = Arrays.asList(result.get(0).split(" ")).contains("finish");
                if (name || address || email || description) {

                    startFieldInput();
                } else if (next) {
                    setFocusForNextField(getCurrentFocus());

                } else if (back) {
                    setFocusForPreviousField(getCurrentFocus());

                } else if (finish){
                    insideloop = true;
                    new SetupTask(this).execute();
                } else {
                    startVoiceInput();
                }

            }
        }
        if (requestCode == 2) {
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> result2 = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                result2 = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                String value = result2.get(0);

                try {
                    if (name) {
                        editTextName.setText(value);
                        name = false;
                        editTextName.requestFocus();
                        startVoiceInput();
                    } else if (address) {
                        editTextAddress.setText(value);
                        address = false;
                        editTextAddress.requestFocus();
                        startVoiceInput();
                    } else if (email) {
                        String trial = value;
                        String trial2 = "";

                        if(value.contains(" ")){
                            trial = value.toLowerCase().replace(" at ", "@");
                            trial2 = trial.replace(" ",  "");
                            editTextEmail.setText(trial2.toLowerCase());
                        }else{
                            editTextEmail.setText(value);
                        }
                        email = false;
                        editTextEmail.requestFocus();
                        startVoiceInput();
                    } else if (description) {
                        editTextDescription.setText(value);
                        description = false;
                        editTextDescription.requestFocus();
                        startVoiceInput();
                    } else if (next) {
                        setFocusForNextField(getCurrentFocus());
                    }else{
                        startFieldInput();
                    }

                } catch (Exception ex) {

                }
            }
        }
    }


    /**
     * Method to navigate from one field to next one
     * */
    private void setFocusForNextField(View currentView) {
          if (editTextName == currentView) {
            editTextAddress.requestFocus();
            address = true;
              startFieldInput();
        } else if (editTextAddress == currentView) {
            editTextEmail.requestFocus();
            email = true;
              startFieldInput();

        } else if (editTextEmail == currentView) {
            editTextDescription.requestFocus();
            description = true;
              startFieldInput();


        } else if (editTextDescription == currentView) {
            editTextDescription.clearFocus();
        }
    }

    /**
     * Method to navigate from one field to previous one
     * */
    private void setFocusForPreviousField(View currentView) {
        if (editTextName == currentView) {
            editTextDescription.clearFocus();
        } else if (editTextAddress == currentView) {
            editTextName.requestFocus();
            name = true;
            startFieldInput();

        } else if (editTextEmail == currentView) {
            editTextAddress.requestFocus();
            address = true;
            startFieldInput();


        } else if (editTextDescription == currentView) {

            editTextEmail.requestFocus();
            email = true;
            startFieldInput();
        }
    }

    /**
     * Here start the methods requested for the pocketsphinx module
     * */

    /**
     * Inner class within two methods for setting up the recognizer and, in case of exception, show
     * a Toast.
     * */
    private static class SetupTask extends AsyncTask<Void, Void, Exception> {
        WeakReference<MainActivity> activityReference;
        SetupTask(MainActivity activity) {
            this.activityReference = new WeakReference<>(activity);
        }
        @Override
        protected Exception doInBackground(Void... params) {
            try {
                Assets assets = new Assets(activityReference.get());
                File assetDir = assets.syncAssets();
                activityReference.get().setupRecognizer(assetDir);
            } catch (IOException e) {
                return e;
            }
            return null;
        }
        @Override
        protected void onPostExecute(Exception result) {
            if (result != null) {
                ((TextView) activityReference.get().findViewById(R.id.caption_text))
                        .setText("Failed to init recognizer " + result);
            } else {
                activityReference.get().switchSearch(KWS_SEARCH);
            }
        }
    }

    /**
     * As RecordAudio permission is requested, it must be declared and accepted; this method checks
     * if the permission has been granted.
     * */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull  int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Recognizer initialization is a time-consuming and it involves IO,
                // so we execute it in async task
                new SetupTask(this).execute();
            } else {
                finish();
            }
        }
    }

    /**
     * Inner method from pocketsphinx
     * */
    @Override
    public void onBeginningOfSpeech() {
    }

    /**
     * Inner method from pocketsphinx
     * */
    @Override
    public void onEndOfSpeech() {
    }

    /**
     * Method to start functionality when Keyphrase is said
     * */

    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null)
            return;

        String texti = hypothesis.getHypstr();

        if ((texti.equals(KEYPHRASE))&& insideloop ){
            recognizer.stop();
            recognizer.shutdown();
            insideloop = false;
            startVoiceInput();


        }else if(!insideloop){
            // Does nothing
        }

    }

    /**
     * Inner method from pocketsphinx
     * */
    @Override
    public void onResult(Hypothesis hypothesis) {
        if (hypothesis != null) {
        }
    }

    /**
     * Method to switch between cases in case of more of using pocketsphinx for the whole
     * speech recognition process (only used for wake-up function).
     * */
    private void switchSearch(String searchName) {
        recognizer.stop();


        // If we are not spotting, start listening with timeout (10000 ms or 10 seconds).
        if (searchName.equals(KWS_SEARCH)){
            recognizer.startListening(searchName);
        }else{
            recognizer.startListening(searchName, 10000);
        }
    }

    /**
     * Method for setting up the voice recognizer within the assets included
     * */
    private void setupRecognizer(File assetsDir) throws IOException {


        recognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))
                .getRecognizer();
        recognizer.addListener(this);



        // Create keyword-activation search.
        recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);


    }


    /**
     * Method for showing errors
     * */
    @Override
    public void onError(Exception e) {
        ((TextView) findViewById(R.id.caption_text)).setText(e.getMessage());
    }

    /**
     * Inner method from pocketsphinx
     * */
    @Override
    public void onTimeout() {
    }

    /**
     * Method for cancelling and shutdown the recognizer at the moment of destroying the application
     * */
    @Override
    public void onDestroy(){
        super.onDestroy();

        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
        }

    }
}
