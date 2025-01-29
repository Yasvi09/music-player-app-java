package com.example.music_java.ui.theme;

import static com.example.music_java.ui.theme.AlbumDetailsAdapter.albumFiles;
import static com.example.music_java.ui.theme.ApplicationClass.ACTION_NEXT;
import static com.example.music_java.ui.theme.ApplicationClass.ACTION_PLAY;
import static com.example.music_java.ui.theme.ApplicationClass.ACTION_PREVIOUS;
import static com.example.music_java.ui.theme.ApplicationClass.CHANNEL_ID_2;
import static com.example.music_java.ui.theme.MainActivity.musicFiles;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.music_java.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class PlayerActivity extends AppCompatActivity implements ActionPlaying, ServiceConnection {

    TextView song_name, artist_name, duration_played, duration_total;
    ImageView cover_art, nextBtn, prevBtn, backBtn, shuffleBtn, repeatBtn;
    FloatingActionButton playPauseBtn;
    SeekBar seekBar;
    int position = -1;
    static ArrayList<MusicFiles> listSongs = new ArrayList<>();
    static Uri uri;
    // static MediaPlayer mediaPlayer;
    private Handler handler = new Handler();
    private Thread playThread,prevThread,nextThread;
    MusicService musicService;
    MediaSessionCompat mediaSessionCompat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        mediaSessionCompat=new MediaSessionCompat(getBaseContext(),"My Audio");
        mediaSessionCompat.setActive(true);
        initViews();
        getIntentMethod();
       /* song_name.setText(listSongs.get(position).getTitle());
        artist_name.setText(listSongs.get(position).getArtist());
        musicService.OnCompleted();*/

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (musicService != null && fromUser) {
                    musicService.seekTo(progress * 1000);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        PlayerActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (musicService != null) {
                    int mCurrentPosition = musicService.getCurrentPosition() / 1000;
                    seekBar.setProgress(mCurrentPosition);
                    duration_played.setText(formattedTime(mCurrentPosition));
                }
                handler.postDelayed(this, 1000);
            }
        });
    }

    @Override
    protected void onResume() {
        Intent intent=new Intent(this,MusicService.class);
        bindService(intent,this,BIND_AUTO_CREATE);
        playThreadBtn();
        nextThreadBtn();
        prevThreadBtn();
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(this);
    }

    private void prevThreadBtn() {
        prevThread=new Thread(){
            @Override
            public void run() {
                super.run();
                prevBtn.setOnClickListener(new View.OnClickListener(){

                    @Override
                    public void onClick(View v) {
                        prevBtnClicked();
                    }
                });
            }
        };
        prevThread.start();
    }

    public void prevBtnClicked(){
        if(musicService.isPlaying()){
            musicService.stop();
            musicService.release();
            position=((position - 1) < 0 ? (listSongs.size() - 1): (position -1 )) ;
            uri=Uri.parse(listSongs.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);
            song_name.setText(listSongs.get(position).getTitle());
            artist_name.setText(listSongs.get(position).getArtist());
            seekBar.setMax(musicService.getDuration()/1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService != null) {
                        int mCurrentPosition = musicService.getCurrentPosition() / 1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this, 1000);
                }
            });
            showNotification(R.drawable.ic_pause);
            musicService.OnCompleted();
            playPauseBtn.setImageResource(R.drawable.ic_pause);
            musicService.start();
        }
        else{
            musicService.stop();
            musicService.release();
            position=((position - 1) < 0 ? (listSongs.size() - 1): (position -1 )) ;
            uri=Uri.parse(listSongs.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);
            song_name.setText(listSongs.get(position).getTitle());
            artist_name.setText(listSongs.get(position).getArtist());
            seekBar.setMax(musicService.getDuration()/1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService != null) {
                        int mCurrentPosition = musicService.getCurrentPosition() / 1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this, 1000);
                }
            });
            showNotification(R.drawable.ic_play);
            musicService.OnCompleted();
            playPauseBtn.setImageResource(R.drawable.ic_play);
        }
    }

    private void nextThreadBtn() {
        nextThread=new Thread(){
            @Override
            public void run() {
                super.run();
                nextBtn.setOnClickListener(new View.OnClickListener(){

                    @Override
                    public void onClick(View v) {
                        nextBtnClicked();
                    }
                });
            }
        };
        nextThread.start();
    }

    public void nextBtnClicked(){
        if(musicService.isPlaying()){
            musicService.stop();
            musicService.release();
            position=(position + 1) % listSongs.size();
            uri=Uri.parse(listSongs.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);
            song_name.setText(listSongs.get(position).getTitle());
            artist_name.setText(listSongs.get(position).getArtist());
            seekBar.setMax(musicService.getDuration()/1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService != null) {
                        int mCurrentPosition = musicService.getCurrentPosition() / 1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this, 1000);
                }
            });
            showNotification(R.drawable.ic_pause);
            musicService.OnCompleted();
            playPauseBtn.setImageResource(R.drawable.ic_pause);
            musicService.start();
        }
        else{
            musicService.stop();
            musicService.release();
            position=(position + 1) % listSongs.size();
            uri=Uri.parse(listSongs.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);
            song_name.setText(listSongs.get(position).getTitle());
            artist_name.setText(listSongs.get(position).getArtist());
            seekBar.setMax(musicService.getDuration()/1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService != null) {
                        int mCurrentPosition = musicService.getCurrentPosition() / 1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this, 1000);
                }
            });
            showNotification(R.drawable.ic_play);
            musicService.OnCompleted();
            playPauseBtn.setImageResource(R.drawable.ic_play);
        }
    }

    private void playThreadBtn() {
        playThread=new Thread(){
            @Override
            public void run() {
                super.run();
                playPauseBtn.setOnClickListener(new View.OnClickListener(){

                    @Override
                    public void onClick(View v) {
                        playPauseBtnClicked();
                    }
                });
            }
        };
        playThread.start();
    }

    public void playPauseBtnClicked(){
        if(musicService.isPlaying()){
            playPauseBtn.setImageResource(R.drawable.ic_play);
            showNotification(R.drawable.ic_play);
            musicService.pause();
            seekBar.setMax(musicService.getDuration()/1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService != null) {
                        int mCurrentPosition = musicService.getCurrentPosition() / 1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this, 1000);
                }
            });
        }
        else{
            showNotification(R.drawable.ic_pause);
            playPauseBtn.setImageResource(R.drawable.ic_pause);
            musicService.start();
            seekBar.setMax(musicService.getDuration()/1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (musicService != null) {
                        int mCurrentPosition = musicService.getCurrentPosition() / 1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this, 1000);
                }
            });
        }
    }

    private String formattedTime(int mCurrentPosition) {
        String totalout = "";
        String totalNew = "";
        String seconds = String.valueOf(mCurrentPosition % 60);
        String minutes = String.valueOf(mCurrentPosition / 60);
        totalout = minutes + ":" + seconds;
        totalNew = minutes + ":" + "0" + seconds;
        if (seconds.length() == 1) {
            return totalNew;
        } else {
            return totalout;
        }
    }

    private void getIntentMethod() {
        position = getIntent().getIntExtra("position", -1);
        String sender=getIntent().getStringExtra("sender");
        if (sender!=null && sender.equals("albumDetails")){
            listSongs=albumFiles;
        }
        else {
            listSongs = musicFiles;
        }

        if (listSongs != null && position != -1) {

            playPauseBtn.setImageResource(R.drawable.ic_pause);
            String fullPath = listSongs.get(position).getPath();
            Log.d("PlayerActivity", "Full Path: " + fullPath);
            File file = new File(fullPath);

            if (!file.exists()) {
                Toast.makeText(this, "File does not exist: " + fullPath, Toast.LENGTH_SHORT).show();
                return;
            }

            uri = Uri.fromFile(file);

            /*try {
                if (musicService != null) {
                    musicService.stop();
                    musicService.release();
                }
                musicService.createMediaPlayer(position);
                if (musicService != null) {
                    musicService.start();
                } else {
                    Log.e("PlayerActivity", "Failed to initialize MediaPlayer");
                }
            } catch (Exception e) {
                Log.e("PlayerActivity", "Error initializing MediaPlayer: " + e.getMessage());
                Toast.makeText(this, "Error initializing player", Toast.LENGTH_SHORT).show();
            }*/

            showNotification(R.drawable.ic_pause);
            Intent intent=new Intent(this,MusicService.class);
            intent.putExtra("servicePosition",position);
            startService(intent);
            /*if (musicService != null) {
                seekBar.setMax(musicService.getDuration() / 1000);
            }
            metaData(uri);*/
        }
    }

    private void initViews() {
        song_name = findViewById(R.id.song_name);
        artist_name = findViewById(R.id.song_artist);
        duration_played = findViewById(R.id.durationplayed);
        duration_total = findViewById(R.id.durationTotal);
        cover_art = findViewById(R.id.cover_art);
        nextBtn = findViewById(R.id.id_next);
        prevBtn = findViewById(R.id.id_prev);
        backBtn = findViewById(R.id.back_btn);
        shuffleBtn = findViewById(R.id.id_shuffle);
        repeatBtn = findViewById(R.id.id_repeat);
        playPauseBtn = findViewById(R.id.play_pause);
        seekBar = findViewById(R.id.seekBar);
    }

    private void metaData(Uri uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(getApplicationContext(), uri);
            int durationTotal = Integer.parseInt(listSongs.get(position).getDuration()) / 1000;
            duration_total.setText(formattedTime(durationTotal));

            byte[] art = retriever.getEmbeddedPicture();
            if (art != null) {
                Glide.with(this)
                        .asBitmap()
                        .load(art)
                        .into(cover_art);
            } else {
                Glide.with(this)
                        .asBitmap()
                        .load(R.drawable.bewedoc)
                        .into(cover_art);
            }
        } catch (Exception e) {
            Log.e("PlayerActivity", "Error retrieving metadata: " + e.getMessage());
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {

        MusicService.MyBinder myBinder=(MusicService.MyBinder) service;
        musicService=myBinder.getService();
        Toast.makeText(this,"Connected" + musicService,Toast.LENGTH_SHORT).show();
        if (musicService != null) {
            seekBar.setMax(musicService.getDuration() / 1000);
        }
        metaData(uri);
        song_name.setText(listSongs.get(position).getTitle());
        artist_name.setText(listSongs.get(position).getArtist());
        musicService.OnCompleted();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

        musicService=null;
    }

    void showNotification(int playPauseBtn){
        Intent intent=new Intent(this, PlayerActivity.class);
        PendingIntent contentIntent=PendingIntent.getActivity(this,0,intent, PendingIntent.FLAG_IMMUTABLE);

//        Intent prevIntent=new Intent(this, NotificationReceiver.class)
//                .setAction(ACTION_PREVIOUS);
//        PendingIntent prevPending=PendingIntent.getBroadcast(this,0,prevIntent,PendingIntent.FLAG_UPDATE_CURRENT);
//
//        Intent pauseIntent=new Intent(this, NotificationReceiver.class)
//                .setAction(ACTION_PLAY);
//        PendingIntent pausePending=PendingIntent.getBroadcast(this,0,pauseIntent,PendingIntent.FLAG_UPDATE_CURRENT);
//
//        Intent nextIntent=new Intent(this, NotificationReceiver.class)
//                .setAction(ACTION_NEXT);
//        PendingIntent nextPending=PendingIntent.getBroadcast(this,0,nextIntent,PendingIntent.FLAG_UPDATE_CURRENT);

        Intent prevIntent=new Intent(this, NotificationReceiver.class)
                .setAction(ACTION_PREVIOUS);
        PendingIntent prevPending=PendingIntent.getBroadcast(this,0,prevIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent pauseIntent=new Intent(this, NotificationReceiver.class)
                .setAction(ACTION_PLAY);
        PendingIntent pausePending=PendingIntent.getBroadcast(this,0,pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent nextIntent=new Intent(this, NotificationReceiver.class)
                .setAction(ACTION_NEXT);
        PendingIntent nextPending=PendingIntent.getBroadcast(this,0,nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        byte[] picture=null;
        picture=getAlbumArt(musicFiles.get(position).getPath());
        Bitmap thumb=null;
        if(picture!=null){
            thumb= BitmapFactory.decodeByteArray(picture,0,picture.length);
        }
        else{
            thumb=BitmapFactory.decodeResource(getResources(),R.drawable.bewedoc);
        }
        Notification notification=new NotificationCompat.Builder(this,CHANNEL_ID_2)
                .setSmallIcon(playPauseBtn)
                .setLargeIcon(thumb)
                .setContentTitle(musicFiles.get(position).getTitle())
                .setContentText(musicFiles.get(position).getArtist())
                .addAction(R.drawable.ic_skip_previous,"Previous",prevPending)
                .addAction(playPauseBtn,"Pause",pausePending)
                .addAction(R.drawable.ic_skip_next,"Next",nextPending)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSessionCompat.getSessionToken()))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOnlyAlertOnce(true)
                .build();

        NotificationManager notificationManager=(NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(0,notification);
        Log.d("NotificationDebug", "Attempting to show notification");
        if (notificationManager != null) {
            try {
                notificationManager.notify(0, notification);
                Log.d("NotificationDebug", "Notification sent to system");
            } catch (Exception e) {
                Log.e("NotificationDebug", "Error showing notification: " + e.getMessage());
            }
        }
    }

    private byte[] getAlbumArt(String uri){
        MediaMetadataRetriever retriever=new MediaMetadataRetriever();
        retriever.setDataSource(uri);
        byte[] art=retriever.getEmbeddedPicture();
        return art;
    }
}

