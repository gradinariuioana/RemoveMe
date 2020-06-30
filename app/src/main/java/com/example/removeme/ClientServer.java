package com.example.removeme;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ClientServer {

    @Multipart
    @POST("predict")
    Call<ResponseBody> predict(
            @Part() MultipartBody.Part image,
            @Part() MultipartBody.Part mask
    );
}



