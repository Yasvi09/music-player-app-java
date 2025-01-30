package com.example.music_java.ui.theme;

import static android.content.Context.BIND_AUTO_CREATE;
import static android.content.Context.MODE_PRIVATE;
import static com.example.music_java.ui.theme.MainActivity.ARTIST_TO_FRAG;
import static com.example.music_java.ui.theme.MainActivity.PATH_TO_FRAG;
import static com.example.music_java.ui.theme.MainActivity.SHOW_MINI_PLAYER;
import static com.example.music_java.ui.theme.MainActivity.SONG_NAME_TO_FRAG;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.music_java.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;

public class NowPlayingFragmentBottom extends Fragment implements ServiceConnection {

    ImageView nextBtn, albumArt;
    TextView artist, songName;
    FloatingActionButton playPauseBtn;
    View view;
    MusicService musicService;
    public static final String MUSIC_LAST_PLAYED = "LAST_PLAYED";
    public static final String MUSIC_FILE = "STORED_MUSIC";
    public static final String ARTIST_NAME = "ARTIST NAME";
    public static final String SONG_NAME = "SONG NAME";
    private boolean isServiceBound = false;

    public NowPlayingFragmentBottom() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_now_playing_bottom, container, false);
        artist = view.findViewById(R.id.song_artist_miniPlayer);
        songName = view.findViewById(R.id.song_name_miniPlayer);
        albumArt = view.findViewById(R.id.bottom_album_art);
        nextBtn = view.findViewById(R.id.skip_next_bottom);
        playPauseBtn = view.findViewById(R.id.play_pause_miniPlayer);

        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (musicService != null && isServiceBound) {
                    musicService.nextBtnCicked();
                    if (getActivity() != null) {
                        updateUI();
                    }
                } else {
                    Toast.makeText(getContext(), "Music service not ready", Toast.LENGTH_SHORT).show();
                }
            }
        });

        playPauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (musicService != null && isServiceBound) {
                    musicService.playPauseBtnClicked();
                    if (musicService.isPlaying()) {
                        playPauseBtn.setImageResource(R.drawable.ic_pause);
                    } else {
                        playPauseBtn.setImageResource(R.drawable.ic_play);
                    }
                } else {
                    Toast.makeText(getContext(), "Music service not ready", Toast.LENGTH_SHORT).show();
                }
            }
        });

        View cardBottomPlayer = view.findViewById(R.id.card_bottom_player);
        if (cardBottomPlayer != null) {
            cardBottomPlayer.setOnClickListener(v -> {
                // Check if music service is ready and we have valid data
                if (musicService != null && musicService.mediaPlayer != null &&
                        MainActivity.PATH_TO_FRAG != null && musicService.position >= 0) {

                    Intent intent = new Intent(getContext(), PlayerActivity.class);
                    intent.putExtra("position", musicService.position);
                    // Pass the current playback position
                    intent.putExtra("current_position", musicService.getCurrentPosition());
                    // If the song is from album details, pass that information
                    if (AlbumDetailsAdapter.albumFiles != null && !AlbumDetailsAdapter.albumFiles.isEmpty()) {
                        intent.putExtra("sender", "albumDetails");
                    }
                    startActivity(intent);
                }
            });
        }
        return view;
    }

    private void updateUI() {
        if (getActivity() == null) return;

        SharedPreferences.Editor editor = getActivity().getSharedPreferences(MUSIC_LAST_PLAYED, MODE_PRIVATE).edit();
        if (musicService != null && musicService.musicFiles != null && musicService.position >= 0
                && musicService.position < musicService.musicFiles.size()) {

            editor.putString(MUSIC_FILE, musicService.musicFiles.get(musicService.position).getPath());
            editor.putString(ARTIST_NAME, musicService.musicFiles.get(musicService.position).getArtist());
            editor.putString(SONG_NAME, musicService.musicFiles.get(musicService.position).getTitle());
            editor.apply();

            SharedPreferences preferences = getActivity().getSharedPreferences(MUSIC_LAST_PLAYED, MODE_PRIVATE);
            String path = preferences.getString(MUSIC_FILE, null);
            String artistName = preferences.getString(ARTIST_NAME, null);
            String song_name = preferences.getString(SONG_NAME, null);

            if (path != null) {
                SHOW_MINI_PLAYER = true;
                PATH_TO_FRAG = path;
                ARTIST_TO_FRAG = artistName;
                SONG_NAME_TO_FRAG = song_name;
            } else {
                SHOW_MINI_PLAYER = false;
                PATH_TO_FRAG = null;
                ARTIST_TO_FRAG = null;
                SONG_NAME_TO_FRAG = null;
            }

            if (SHOW_MINI_PLAYER && PATH_TO_FRAG != null) {
                updateBottomPlayerUI();
            }
        }
    }

    private void updateBottomPlayerUI() {
        if (getContext() == null) return;

        byte[] art = getAlbumArt(PATH_TO_FRAG);
        if (art != null) {
            Glide.with(getContext()).load(art)
                    .into(albumArt);
        } else {
            Glide.with(getContext()).load(R.drawable.bewedoc)
                    .into(albumArt);
        }
        songName.setText(SONG_NAME_TO_FRAG);
        artist.setText(ARTIST_TO_FRAG);

        // Update play/pause button state
        updatePlayPauseButton();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (SHOW_MINI_PLAYER) {
            if (PATH_TO_FRAG != null) {
                if (getContext() != null) {
                    Intent intent = new Intent(getContext(), MusicService.class);
                    getContext().bindService(intent, this, Context.BIND_AUTO_CREATE);
                    updateBottomPlayerUI();
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isServiceBound) {
            if (getContext() != null) {
                getContext().unbindService(this);
                isServiceBound = false;
            }
        }
    }

    private byte[] getAlbumArt(String uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(uri);
            return retriever.getEmbeddedPicture();
        } catch (Exception e) {
            return null;
        } finally {
            try {
                retriever.release();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        MusicService.MyBinder binder = (MusicService.MyBinder) service;
        musicService = binder.getService();
        isServiceBound = true;

        // Update play/pause button state when service connects
        if (musicService != null) {
            updatePlayPauseButton();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        musicService = null;
        isServiceBound = false;
    }

    private void updatePlayPauseButton() {
        if (musicService != null && musicService.mediaPlayer != null) {
            playPauseBtn.setImageResource(
                    musicService.isPlaying() ?
                            R.drawable.ic_pause :
                            R.drawable.ic_play
            );
        }
    }
}