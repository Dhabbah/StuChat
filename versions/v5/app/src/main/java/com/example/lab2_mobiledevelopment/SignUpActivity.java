package com.example.lab2_mobiledevelopment;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

public class SignUpActivity extends AppCompatActivity {

    int REQUEST_CAMERA = 0;
    int SELECT_FILE = 1;
    ImageView userImage;
    Bitmap userphoto = null;

    private FirebaseAuth auth;
    DatabaseReference reference;


    private EditText userEmail, userPassword, FirstName, LastName;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        userImage = (ImageView) findViewById(R.id.UserImage);
        userEmail = (EditText) findViewById(R.id.signup_email);
        userPassword = (EditText) findViewById(R.id.signup_password);
        FirstName = (EditText) findViewById(R.id.signup_firstname);
        LastName = (EditText) findViewById(R.id.signup_lastname);
        auth = FirebaseAuth.getInstance();

    }

    public void onClickPhotoButton(View v){
        selectImage();

    }

    private void selectImage(){
        final CharSequence[] items = {"Take Photo", "Choose from Gallery", "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(SignUpActivity.this);
        builder.setTitle("Add Photo!");

        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {

                if(items[item].equals("Take Photo")){
                    cameraIntent();
                }
                else if(items[item].equals("Choose from Gallery")){
                    galleryItent();
                }else if(items[item].equals("Cancel")){
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    private void cameraIntent(){
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    private void galleryItent(){
        Intent intent = new Intent();

        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select File"), SELECT_FILE);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == Activity.RESULT_OK){
            if(requestCode == SELECT_FILE){
                onSelecFromGalleryResult(data);
            }else if(requestCode == REQUEST_CAMERA){
                onCaptureImageResult(data);
            }
        }
    }

    private void onSelecFromGalleryResult(Intent data){

        Bitmap bm = null;

        if(data != null){
            try{
                bm = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), data.getData());
            }catch (IOException e){
                e.printStackTrace();
            }
        }
        this.userphoto = bm;

        userImage.setImageBitmap(bm);
    }

    private void onCaptureImageResult(Intent data){
        Bitmap thumbnail = (Bitmap) data.getExtras().get("data");

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
        File destination = new File(Environment.getExternalStorageDirectory(), System.currentTimeMillis() + ".jpg");

        try{
            destination.createNewFile();
            FileOutputStream fileOutputStream = new FileOutputStream(destination);
            fileOutputStream.write(byteArrayOutputStream.toByteArray());
            fileOutputStream.close();

        }catch (FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
        this.userphoto = thumbnail;
        userImage.setImageBitmap(thumbnail);
    }


    public void onSignUp(View v){

        // add the sign up details to firebase
        String email = userEmail.getText().toString().trim();
        String password = userPassword.getText().toString().trim();
        final String Firstname = FirstName.getText().toString().trim();
        final String Lastname = LastName.getText().toString().trim();

        if(TextUtils.isEmpty(email)){
            Toast.makeText(getApplicationContext(), "please enter your email address!", Toast.LENGTH_SHORT).show();
            return;
        }

        if(TextUtils.isEmpty(password)){
            Toast.makeText(getApplicationContext(), "Please enter password", Toast.LENGTH_SHORT).show();
            return;
        }
        if(password.length() < 6 ){
            Toast.makeText(getApplicationContext(), "Password is too short, enter minimum 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }
        if(true){
            Toast.makeText(getApplicationContext(), "Account is beeing create...", Toast.LENGTH_SHORT).show();
        }

        //create user

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(SignUpActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.


                        if(task.isSuccessful()){
                            FirebaseUser firebaseUser = auth.getCurrentUser();
                            String userid = firebaseUser.getUid();

                            reference = FirebaseDatabase.getInstance().getReference("Users").child(userid);

                            HashMap<String, String> hashMap = new HashMap<>();
                            hashMap.put("id", userid);
                            hashMap.put("Firstname", Firstname);
                            hashMap.put("Lastname", Lastname);
                            hashMap.put("imageURL", "default");

                            reference.setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()){
                                        Toast.makeText(SignUpActivity.this, "Create user complete" + task.isSuccessful(),Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                                        finish();
                                    }
                                }
                            });
                        }
                        else{
                            Toast.makeText(SignUpActivity.this, "Failed to create account" + task.getException(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });

    }
}
