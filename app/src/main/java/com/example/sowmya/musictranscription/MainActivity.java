package com.example.sowmya.musictranscription;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity{
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final String LOG_TAG = "MainActivity";

    private Button uploadButton, transcribeButton, recordButton, playButton;
    private TextView pathField;

    private static final int PICK_FILE_REQUEST = 1;

    private  ServerHandler serverHandler;
    private SoundRecorder soundRecorder;
    private FileManager fileManager;
    private ProgressDialog progress;

    private String  directoryPath, audioPath, browsedPath, mRecordFilePath, musicSheetPath;

    private boolean mStartRecording = true;
    private boolean mStartPlaying = true;

    private enum ButtonSelected {BROWSE, RECORD};
    ButtonSelected buttonSelected = null;

    private boolean permissionToRecordAccepted = false;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted ) finish();

    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        directoryPath = Environment.getExternalStorageDirectory()+File.separator+"musicTranscription/";

        soundRecorder = new SoundRecorder(directoryPath);
        serverHandler = new ServerHandler();
        fileManager = new FileManager();

        uploadButton = (Button) findViewById(R.id.uploadButton);

        transcribeButton = (Button) findViewById(R.id.transcribeButton);
        transcribeButton.setEnabled(false);

        recordButton = (Button) findViewById(R.id.recordButton);

        playButton = (Button) findViewById(R.id.playButton);
        playButton.setEnabled(false);

        pathField =  (TextView) findViewById(R.id.pathField);

        if(!(fileManager.isExists(directoryPath)))
              if(!fileManager.create(directoryPath))
                  finish();

        uploadButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                if(showFileChooser()){
                    buttonSelected = ButtonSelected.BROWSE;
                    transcribeButton.setEnabled(true);
                }else{
                    buttonSelected = null;
                    transcribeButton.setEnabled(false);
                }
            }
        });
        transcribeButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                transcribeMusic();
            }
        });

        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonSelected = ButtonSelected.RECORD;

                if (mStartRecording) {
                    recordButton.setText("Stop recording");

                    SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
                    Date now = new Date();
                    String strDate = sdfDate.format(now);
                    mRecordFilePath = directoryPath + strDate + ".3gp";

                    if(!soundRecorder.startRecording(mRecordFilePath)){
                        mRecordFilePath = "";
                        recordButton.setText("Start recording");
                        buttonSelected = null;

                        Toast.makeText(getApplicationContext(),"something went wrong.please try again",Toast.LENGTH_LONG).show();

                    }else{
                        Toast.makeText(getApplicationContext(),"Recording",Toast.LENGTH_LONG).show();
                    }

                } else {
                    recordButton.setText("Start recording");
                    soundRecorder.stopRecording();
                    Toast.makeText(getApplicationContext(),"Recording stopped",Toast.LENGTH_SHORT).show();
                    playButton.setEnabled(true);
                }
                mStartRecording = !mStartRecording;
            }
        });

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mStartPlaying) {
                    playButton.setText("Stop playing");
                    if(fileManager.isExists(mRecordFilePath))
                        soundRecorder.startPlaying(mRecordFilePath);
                    else{
                        Toast.makeText(getApplicationContext(),"something went wrong. please try again",Toast.LENGTH_SHORT).show();
                    }
                } else {
                    soundRecorder.stopPlaying();
                    playButton.setText("Start playing");
                }
                mStartPlaying = !mStartPlaying;
            }
        });

    }

    private boolean showFileChooser() {
        try {
            Intent intent = new Intent();
            //sets the select file to audio types of files
            intent.setType("audio/*");
            //allows to select data and return it
            intent.setAction(Intent.ACTION_GET_CONTENT);
            //starts new activity to select file and return data
            startActivityForResult(Intent.createChooser(intent, "Choose File to Upload.."), PICK_FILE_REQUEST);
            return true;
        }catch (Exception e){
            Log.e(LOG_TAG, "exception in show file chooser function");
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data){

        if(requestCode == 1){

            if(resultCode == RESULT_OK){
                //the selected audio.
                Uri uri = data.getData();
                //if(getMimeType(getApplicationContext(), uri).equals("wav")){
                String mimeType = getMimeType(getApplicationContext(), uri);
                if(!mimeType.isEmpty()){
                    if(mimeType.startsWith("audio")){
                        browsedPath = getRealPathFromURI(getApplicationContext(),  uri);
                    }
                }else{
                    Log.e(LOG_TAG, "mime type empty");
                    Toast.makeText(getApplicationContext()," please select an audio file ",Toast.LENGTH_LONG).show();
                }
            }else{
                Log.e(LOG_TAG, "result code not equals to ok");
                Toast.makeText(getApplicationContext()," Something went wrong, please try again ",Toast.LENGTH_LONG).show();
            }
        }else{

        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void transcribeMusic(){
        // Assume thisActivity is the current activity
        //int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        Toast.makeText(getApplicationContext(),audioPath,Toast.LENGTH_LONG).show();
        if(buttonSelected != null){
            if(buttonSelected.equals(ButtonSelected.BROWSE))
                audioPath = browsedPath;
            else if(buttonSelected.equals(ButtonSelected.RECORD))
                audioPath = mRecordFilePath;
        }
        pathField.setText(audioPath);

        //transcribeButton.setEnabled(false);
        // connect to php server to send recorded file and get notes
        progress = ProgressDialog.show(MainActivity.this, "Connect to server", "uploading file", true);

        if(serverHandler.uploadFile(audioPath)) {
            serverHandler.downloadFile("http://192.168.1.3/musicTranscription/sheets/test.pdf", directoryPath+"test.pdf");
           // serverHandler.transcribeFile();
            progress.dismiss();
            //get notes
        }
        else{
            progress.dismiss();
            Toast.makeText(getApplicationContext(),"couldn't upload wav file",Toast.LENGTH_SHORT).show();

        }

    }

    public String getExtension(Context context, Uri uri) {
        String extension;

        //Check uri format to avoid null
        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            //If scheme is a content
            final MimeTypeMap mime = MimeTypeMap.getSingleton();
            extension = mime.getExtensionFromMimeType(context.getContentResolver().getType(uri));
        } else {
            //If scheme is a File
            //This will replace white spaces with %20 and also other special characters. This will avoid returning null values on file name with spaces and special characters.
            extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(new File(uri.getPath())).toString());

        }

        return extension;
    }

    public String getMimeType(Context context, Uri uri){
        //Check uri format to avoid null
        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            //If scheme is a content
            return context.getContentResolver().getType(uri);
        } else {
            //If scheme is a File
            //This will replace white spaces with %20 and also other special characters. This will avoid returning null values on file name with spaces and special characters.
            return MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(new File(uri.getPath())).toString());

        }
    }

    public String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

}
