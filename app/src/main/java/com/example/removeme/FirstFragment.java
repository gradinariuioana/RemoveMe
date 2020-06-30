package com.example.removeme;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import static android.app.Activity.RESULT_OK;

public class FirstFragment extends Fragment {

    private final Intent intent;
    private static Uri selectedImage;
    private static Bitmap bitmap;
    private CropImageView image;
    private Button crop_btn;
    private String maskName;
    private ProgressDialog progressDialog;
    public static final String FILE_UPLOAD_URL = "http://10.0.2.2:5000/";
    private ConstraintLayout layout;


    FirstFragment(Intent intent)
    {
        this.intent = intent;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_first, container, false);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        layout = view.findViewById(R.id.fragment_first);

        image = view.findViewById(R.id.cropImageView);
        image.setAutoZoomEnabled(false);
        crop_btn = view.findViewById(R.id.crop_btn);

        try {
            selectedImage = Uri.parse(intent.getStringExtra("URI"));
            bitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), selectedImage);
            bitmap = ImageUtils.rotateImage(getContext(), bitmap, selectedImage);
        } catch (Exception e) {
            e.printStackTrace();
        }

        image.setImageUriAsync(selectedImage);


        crop_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                progressDialog = new ProgressDialog(getContext());
                progressDialog.show();
                progressDialog.setContentView(R.layout.load);
                progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

                // obținem zona selectată în cadrul obiectului image de tip CropImageView
                // sub forma unui obiect de tip Rect
                Rect cropped = image.getCropRect();
                // definim un nou obiect de tip Bitmap
                Bitmap mask = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
                // definim un obiect Canvas pe baza bitmap-ului
                Canvas canvas = new Canvas(mask);
                // setăm background-ul canvas-ului negru
                canvas.drawColor(Color.BLACK);
                // definim un nou obiect de tip Paint
                Paint p = new Paint();
                // setăm culoarea cu care vom marca zona
                p.setColor(Color.WHITE);
                // marcăm zona delimitată de Rect cu alb
                canvas.drawRect(cropped, p);

                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                mask.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
                maskName = "mask" + System.currentTimeMillis();
                String maskPath = MediaStore.Images.Media.insertImage(getContext().getContentResolver(), mask, maskName, null);
                Uri maskUri = Uri.parse(maskPath);

                //create mask for image
                try {
                    uploadFiles(selectedImage, maskUri);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                layout.removeView(crop_btn);
            }
        });
    }

    private MultipartBody.Part prepareFilePart(String partName, Uri fileUri) {
        File dir = new File(Objects.requireNonNull(ImageUtils.getPath(getContext(), fileUri)));

        RequestBody requestFile = RequestBody.create(MediaType.parse(Objects.requireNonNull(requireContext().getContentResolver().getType(fileUri))), dir);

        return MultipartBody.Part.createFormData(partName, dir.getName(), requestFile);

    }


    private void uploadFiles(Uri imageUri, Uri maskUri) {

        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl(FILE_UPLOAD_URL)
                .callbackExecutor(Executors.newSingleThreadExecutor());

        Retrofit retrofit = builder.build();
        ClientServer client = retrofit.create(ClientServer.class);

        Call<ResponseBody> call = client.predict(
                prepareFilePart("image", imageUri),
                prepareFilePart("mask", maskUri)
        );

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        final Bitmap res = BitmapFactory.decodeStream(response.body().byteStream());
                        progressDialog.dismiss();
                        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                        SecondFragment secondFragment = new SecondFragment(res);
                        fragmentTransaction.replace(R.id.container, secondFragment, "first_fragment");
                        fragmentTransaction.commit();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                System.out.println("failure");
                System.out.println(t.toString());
                try {
                    call.clone().execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                assert result != null;
                image.setImageUriAsync(result.getUri());
            }
        }
    }

}
