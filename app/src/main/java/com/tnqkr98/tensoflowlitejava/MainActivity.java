package com.tnqkr98.tensoflowlitejava;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.HandlerCompat;

import android.media.AudioRecord;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import org.tensorflow.lite.support.audio.TensorAudio;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.task.audio.classifier.AudioClassifier;
import org.tensorflow.lite.task.audio.classifier.Classifications;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String MODEL_FILE = "yamnet.tflite";
    private static final float MINIMUM_DISPLAY_THRESHOLD = 0.3f;

    private AudioClassifier mAudioClassifier;
    private AudioRecord mAudioRecord;
    private long classficationInterval = 500;       // 0.5 sec
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        HandlerThread handlerThread = new HandlerThread("backgroundThread");
        handlerThread.start();
        mHandler = HandlerCompat.createAsync(handlerThread.getLooper());

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, 4);

        startAudioClassification();
    }

    private void startAudioClassification(){
        if(mAudioClassifier != null) return;

        try {
            AudioClassifier classifier = AudioClassifier.createFromFile(this, MODEL_FILE);
            TensorAudio audioTensor = classifier.createInputTensorAudio();

            AudioRecord record = classifier.createAudioRecord();
            record.startRecording();

            Runnable run = new Runnable() {
                @Override
                public void run() {
                    //long startTime = System.currentTimeMillis();
                    audioTensor.load(record);
                    List<Classifications> output = classifier.classify(audioTensor);
                    List<Category> filterModelOutput = output.get(0).getCategories();
                    for(Category c : filterModelOutput) {
                        if (c.getScore() > MINIMUM_DISPLAY_THRESHOLD)
                            Log.d("tensorAudio_java", " label : " + c.getLabel() + " score : " + c.getScore());
                    }
                    //long finishTime = System.currentTimeMillis();

                    mHandler.postDelayed(this,classficationInterval);
                }
            };

            mHandler.post(run);
            mAudioClassifier = classifier;
            mAudioRecord = record;
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private void stopAudioClassfication(){
        mHandler.removeCallbacksAndMessages(null);
        mAudioRecord.stop();
        mAudioRecord = null;
        mAudioClassifier = null;
    }

    @Override
    public void onTopResumedActivityChanged(boolean isTopResumedActivity) {
        //if(isTopResumedActivity)
        //    startAudioClassification();
        //else
        //    stopAudioClassfication();
    }

    @Override
    protected void onDestroy() {
        stopAudioClassfication();
        super.onDestroy();
    }
}