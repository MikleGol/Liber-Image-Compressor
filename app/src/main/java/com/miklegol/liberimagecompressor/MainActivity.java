package com.miklegol.liberimagecompressor;

import static android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.Toolbar;

import com.google.android.material.button.MaterialButtonToggleGroup;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView, imgCompressed, btnDownload;
    private Button btnSelectImage, btnCompress;
    private final int PICK_IMAGE = 100;

    TextView txtOriginal, txtCompressed, txtQuality;
    EditText txtHeight, txtWidth;
    SeekBar seekBar;
    Uri currentImageUri;
    MaterialButtonToggleGroup toggleGroup;
    Bitmap.CompressFormat compressFormat;
    Window w;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imgOriginal);
        imgCompressed = findViewById(R.id.imgCompressed);

        btnSelectImage = findViewById(R.id.btnPick);
        btnCompress = findViewById(R.id.btnCompress);
        btnDownload = findViewById(R.id.btnDownload);

        txtOriginal = findViewById(R.id.txtOriginal);
        txtCompressed = findViewById(R.id.txtCompressed);
        txtQuality = findViewById(R.id.txtQuality);
        txtHeight = findViewById(R.id.txtHeight);
        txtWidth = findViewById(R.id.txtWidth);

        seekBar = findViewById(R.id.seekQuality);

        toggleGroup = findViewById(R.id.toggleButtonGroup);
        toggleGroup.check(R.id.btnJpeg);

        compressFormat = Bitmap.CompressFormat.JPEG;

        w = getWindow();
        w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        btnSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent,PICK_IMAGE);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                txtQuality.setText("Quality: " + i);
                seekBar.setMax(100);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnCompress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (currentImageUri != null) {
                    if (!txtHeight.getText().toString().matches("")) {
                        if (!txtWidth.getText().toString().matches("")) {
                            int quality = seekBar.getProgress();
                            int width = Integer.parseInt(txtWidth.getText().toString());
                            int height = Integer.parseInt(txtHeight.getText().toString());

                            if (height > 0 && width > 0) {
                                compressImage(currentImageUri, height, width, quality, false);
                            } else {Toast.makeText(MainActivity.this, "Height and width must be greater than 0", Toast.LENGTH_SHORT).show();}
                        } else { Toast.makeText(MainActivity.this, "Enter height and width of image", Toast.LENGTH_SHORT).show();}
                    } else { Toast.makeText(MainActivity.this, "Enter height and width of image", Toast.LENGTH_SHORT).show();}
                } else { Toast.makeText(MainActivity.this, "Pick image", Toast.LENGTH_SHORT).show();}
            }
        });

        toggleGroup.addOnButtonCheckedListener(new MaterialButtonToggleGroup.OnButtonCheckedListener() {
            @Override
            public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
                if(isChecked){
                    switch (checkedId){
                        case (R.id.btnPng) : compressFormat = Bitmap.CompressFormat.PNG; return;
                        case (R.id.btnJpeg) : compressFormat = Bitmap.CompressFormat.JPEG; return;
                        case (R.id.btnWebp) : compressFormat = Bitmap.CompressFormat.WEBP; return;
                    }
                }
            }
        });

        btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int quality = seekBar.getProgress();
                int width = Integer.parseInt(txtWidth.getText().toString());
                int height = Integer.parseInt(txtHeight.getText().toString());
                compressImage(currentImageUri, height, width, quality, true);
            }
        });

    }


    @Override
    protected void onResume() {
        super.onResume();
        w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK){
            if (data!=null){
                //compressImage(data.getData());
                currentImageUri = data.getData();
                imageView.setImageURI(currentImageUri);
                imageView.setClipToOutline(true);
                imageView.setBackgroundResource(R.drawable.button_white);

                try (InputStream inputStream = getContentResolver().openInputStream(currentImageUri)) {
                    if(inputStream.available() >= 1000000){
                        txtOriginal.setText("Size: " + String.format("%.2f",inputStream.available()/1000000d) + " Mb");
                    }
                    else{
                        txtOriginal.setText("Size: " + Integer.parseInt(String.valueOf(inputStream.available()/1000)) + " Kb");
                    }
                }catch (FileNotFoundException ex){ ex.printStackTrace();} catch (IOException e){ e.printStackTrace();}



                try {
                    Bitmap imageBitmaps = MediaStore.Images.Media.getBitmap(this.getContentResolver(),currentImageUri);
                    txtHeight.setText("" + imageBitmaps.getHeight());
                    txtWidth.setText("" + imageBitmaps.getWidth());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                btnCompress.setVisibility(View.VISIBLE);

            }
        }
    }

    private void compressImage(Uri imageUri, int height, int width, int quality, boolean isDownload){

        //int quality = seekBar.getProgress();

        try {
            Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),imageUri);
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            String fileName = String.format("%d.jpg",System.currentTimeMillis());
            File finalFile = new File(path,fileName);

            if(txtWidth != null && txtHeight != null) {
                //int width = Integer.parseInt(txtWidth.getText().toString());
                //int height = Integer.parseInt(txtHeight.getText().toString());
                imageBitmap = Bitmap.createScaledBitmap(imageBitmap, width, height, true);
            }


            FileOutputStream fileOutputStream = new FileOutputStream(finalFile);

            //imageBitmap.compress(compressFormat, quality, fileOutputStream);

            imageBitmap.compress(compressFormat, quality, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();

            imgCompressed.setImageURI(Uri.fromFile(finalFile));
            imgCompressed.setClipToOutline(true);
            imgCompressed.setBackgroundResource(R.drawable.button_white);

            if(finalFile.length() >= 1000000){
                txtCompressed.setText("Size: " + String.format("%.2f",finalFile.length()/1000000d) + " Mb");
            }
            else{
                txtCompressed.setText("Size: " + Integer.parseInt(String.valueOf(finalFile.length()/1000)) + " Kb");
            }

            if(!isDownload){
                finalFile.delete();
                btnDownload.setVisibility(View.VISIBLE);
            } else{
                Toast.makeText(this, "Downloaded!", Toast.LENGTH_SHORT).show();
            }

            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(Uri.fromFile(finalFile));
            sendBroadcast(intent);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}