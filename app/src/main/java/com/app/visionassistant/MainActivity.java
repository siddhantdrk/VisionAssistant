package com.app.visionassistant;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.app.visionassistant.databinding.ActivityMainBinding;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;


public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private static final String TAG = "MyTag";
    private static final int CAMERA_PERMISSION_CODE=101;
    private static final int READ_STORAGE_PERMISSION_CODE=102;
    private static final int WRITE_STORAGE_PERMISSION_CODE=103;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private InputImage inputImage;
    private ImageLabeler imageLabeler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());

        imageLabeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);

        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            Intent data = result.getData();
            try{
                Bitmap picture = (Bitmap)data.getExtras().get("data");
                binding.inputImv.setImageBitmap(picture);
                inputImage = InputImage.fromBitmap(picture,0);
                processImage();
            }catch (Exception e){
                Log.d(TAG,"cameraLauncher's onActivityResult : "+e.getMessage());
            }
        });

        galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            Intent data = result.getData();
            try{
                binding.inputImv.setImageURI(data.getData());
                inputImage = InputImage.fromFilePath(MainActivity.this,data.getData());
                processImage();
            }catch (Exception e){
                Log.d(TAG,"cameraLauncher's onActivityResult : "+e.getMessage());
            }
        });

        binding.choosePictureMb.setOnClickListener(view -> {
            String[] options = {"Camera", "Gallery"};
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Select an Option");
            builder.setItems(options, (dialogInterface, i) -> {
                if(i==0){
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    cameraLauncher.launch(cameraIntent);
                }
                else{
                    Intent storageIntent = new Intent();
                    storageIntent.setType("image/*");
                    storageIntent.setAction(Intent.ACTION_GET_CONTENT);
                    galleryLauncher.launch(storageIntent);
                }
            });
            builder.show();
        });
    }

    private void processImage() {
        imageLabeler.process(inputImage).addOnSuccessListener(imageLabels -> {
            StringBuilder result= new StringBuilder();
            for(ImageLabel label:imageLabels){
                result.append(label.getText()).append("\n");
            }
            binding.resultTv.setText(result.toString());
        }).addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Image processing Failed !!", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermission(Manifest.permission.CAMERA,CAMERA_PERMISSION_CODE);
    }

    private void checkPermission(String permission, int requestCode){
        if(ContextCompat.checkSelfPermission(MainActivity.this,permission)== PackageManager.PERMISSION_GRANTED){
            Toast.makeText(this, "Permission Already Granted.", Toast.LENGTH_SHORT).show();
        }
        else{
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{permission},requestCode);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode== CAMERA_PERMISSION_CODE){
            if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
                checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE,READ_STORAGE_PERMISSION_CODE);
            }
            else{
                Toast.makeText(this, "Camera Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
        else if(requestCode==READ_STORAGE_PERMISSION_CODE){
            if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
                checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE,WRITE_STORAGE_PERMISSION_CODE);
            }
            else{
                Toast.makeText(this, "Read Storage Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
        else if(requestCode==WRITE_STORAGE_PERMISSION_CODE){
            if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "All permissions Granted.", Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(this, "Write Storage Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}