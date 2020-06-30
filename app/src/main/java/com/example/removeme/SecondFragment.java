package com.example.removeme;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.io.ByteArrayOutputStream;

public class SecondFragment extends Fragment {

    private Bitmap bitmap;

    SecondFragment (Bitmap bitmap)
    {
        this.bitmap = bitmap;
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_second, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ImageView myimgv = view.findViewById(R.id.img_sf);
        myimgv.setImageBitmap(bitmap);
        view.findViewById(R.id.savebtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
                String fileName = "edited_image" + System.currentTimeMillis();
                MediaStore.Images.Media.insertImage(requireContext().getContentResolver(), bitmap, fileName, null);
                Toast.makeText(getContext(), "Image saved", Toast.LENGTH_LONG).show();
                requireActivity().finish();
            }
        });
    }
}
