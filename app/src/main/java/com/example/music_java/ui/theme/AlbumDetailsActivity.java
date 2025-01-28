package com.example.music_java.ui.theme;

import static com.example.music_java.ui.theme.MainActivity.musicFiles;

import android.media.Image;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.music_java.R;

import java.io.IOException;
import java.util.ArrayList;

public class AlbumDetailsActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    ImageView albumPhoto;
    String albumName;
    ArrayList<MusicFiles> albumSongs=new ArrayList<>();
    AlbumDetailsAdapter albumDetailsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_details);

        recyclerView=findViewById(R.id.recyclerView);
        albumPhoto=findViewById(R.id.albumPhoto);
        albumName=getIntent().getStringExtra("albumName");
        int j=0;

        for(int i=0;i<musicFiles.size();i++){
            if(albumName.equals(musicFiles.get(i).getAlbum())){
                albumSongs.add(j,musicFiles.get(i));
                j++;
            }
        }
        byte[] image=getAlbumArt(albumSongs.get(0).getPath());
        if(image !=null){
            Glide.with(this)
                    .load(image)
                    .into(albumPhoto);
        }
        else{
            Glide.with(this)
                    .load(R.drawable.bewedoc)
                    .into(albumPhoto);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!(albumSongs.size() < 1)){
            albumDetailsAdapter = new AlbumDetailsAdapter(this,albumSongs);
            recyclerView.setAdapter(albumDetailsAdapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(this,RecyclerView.VERTICAL,false));
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
}