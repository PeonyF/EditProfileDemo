package com.practice.edituserprofiledemo;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.loader.content.CursorLoader;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;
import com.practice.edituserprofiledemo.model.PImageData;
import com.practice.edituserprofiledemo.model.entity.ApiResultDto;
import com.practice.edituserprofiledemo.utils.Const;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


/**
 * A simple {@link Fragment} subclass.
 */
public class ProfileFragment extends Fragment implements Button.OnClickListener {

    private static final String TAG = "ProfileFragment";
    private static final int MAX_WIDTH = 640;
    private static final int MAX_HEIGHT = 640;
    private ImageView profileView;
    private ImageButton imageButton;
    // private Bitmap cameraBitmap;

    //String originalUrl = "";
    private String IMG_BASE_URL = "http://www.ppizil.kro.kr/review/file/";
    String thumbnailImagePath = "http://www.ppizil.kro.kr/review/files/1.jpeg";
    String originImagePath = "http://www.ppizil.kro.kr/review/files/0.jpeg";





    private static final int REQUEST_CAMERA = 101;
    private static final int PICK_IMAGE_REQUEST = 102;
    private static final int REQUEST_CODE_CROP_IMAGE = 103;


    String cameraPermissions[];
    String storagePermissions[];

    Uri image_uri;
    File photoFile;
    String imageUriRetrofit;

    private Bitmap bitmap;

    public ProfileFragment() {
        // Required empty public constructor
        super();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        bindView(view);
        imageButton.setOnClickListener(this);
        profileView.setOnClickListener(this::onClick);
        //setData(KEY_IMAGE);
        // setData(imageUriRetrofit);

        return view;
    }

    private void bindView(View view) {
        profileView = view.findViewById(R.id.profile_image);
        imageButton = view.findViewById(R.id.btn_choose_picture);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_choose_picture:
                selectImage();
                break;
            case R.id.profile_image:
                setFullImageActivity();
                break;
        }
    }

    private void setFullImageActivity(){
        Intent sendOriginalImageIntent = new Intent(getActivity(), FullImageActivity.class);
        String path = SharedPreferenceBase.getInstance().getString("PROFILEPATH",null);
        sendOriginalImageIntent.putExtra("originalImage",path);
       startActivity(sendOriginalImageIntent);
    }

//    private void setOriginalImage(String originalImageUrl) {
//        Intent sendOriginalImagePath = new Intent(getActivity(), FullImageActivity.class);
//        sendOriginalImagePath.putExtra("originalImage",originalImageUrl);
//        startActivity(sendOriginalImagePath);
//    }

//    private void setData(String internetUrl) {
//        Glide.with(getActivity())
//                .load(internetUrl)
//                .transform(new CircleCrop())
//                .thumbnail(0.25f)
//                .into(profileView);
//
//    }






    private void selectImage() {
        checkPermission();
        final CharSequence[] items = {"Take Photo", "Choose from Library", "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Add Photo!");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (items[item].equals("Take Photo")) {
                    cameraIntent();
                } else if (items[item].equals("Choose from Library")) {
                    galleryIntent();
                } else if (items[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }


    private void cameraIntent() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        getActivity().startActivityForResult(cameraIntent, REQUEST_CAMERA);

    }

    private void galleryIntent() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        getActivity().startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST);
    }


    private void checkPermission() {
        PermissionListener permissionListener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                Toast.makeText(getActivity(), "Permission Granted", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPermissionDenied(ArrayList<String> deniedPermissions) {
                Toast.makeText(getActivity(), "Permission Denied\n" + deniedPermissions.toString(), Toast.LENGTH_SHORT).show();
            }
        };

        TedPermission.with(getActivity())
                .setPermissionListener(permissionListener)
                .setDeniedMessage("If you reject permission,you can not use this service\n\nPlease turn on permissions at [Setting] > [Permission]")
                .setPermissions(Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                .check();
    }


    //    //Bitmap 인자로 받도록
    public Bitmap fixOrientation(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return bitmap;
    }


    public void onResult(int requestCode, int resultCode, @Nullable Intent data) throws IOException {
        Log.e("aaa", requestCode + "," + resultCode);
        if (resultCode != getActivity().RESULT_CANCELED) {
            Bitmap bitmap = null;

            switch (requestCode) {
                case PICK_IMAGE_REQUEST:
                    image_uri = data.getData();

                    Log.e("bbb", "onActivityResult: " + image_uri);
                    String uriPath = getRealPathFromURI(image_uri);
                    bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), image_uri);
                    setImageResource(image_uri.toString(), profileView);
                    updateData(bitmap);


                    break;

                case REQUEST_CAMERA:
                    bitmap = (Bitmap) data.getExtras().get("data");


                    String uriPathPicture = MediaStore.Images.Media.insertImage(getActivity().getContentResolver(), bitmap, "title", null);
                    bitmap = fixOrientation(bitmap);
                    image_uri = Uri.parse(uriPathPicture);

                    compressImage(uriPathPicture);
                    String uriPathReal = getRealPathFromURI(image_uri);
                    setImageResource(image_uri.toString(), profileView);
                    updateData(bitmap);
                    break;
            }
        }
    }

    private void compressImage(String uriPath) {
        File file = new File(uriPath);
        try {
            bitmap = BitmapFactory.decodeFile(file.getPath());
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
        } catch (Throwable t) {
            Log.e("ERROR", "Error compressing file." + t.toString());
            t.printStackTrace();
        }
    }

    private String getRealPathFromURI(Uri contentUri) {
        //String[] filePathColumn = {MediaStore.Images.Media.DATA};
        String[] filePathColumn = {MediaStore.Images.Thumbnails.DATA};
        CursorLoader loader = new CursorLoader(getContext(), contentUri, filePathColumn, null, null, null);

        Cursor cursor = loader.loadInBackground();

        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String result = cursor.getString(column_index);

        cursor.close();
        return result;
    }


//    private Bitmap getThumbnailPathForLocalFile(Activity context, Uri fileUri){
//        Long fileId = getRealPathFromURI(fileUri);
//        return MediaStore.Images.Thumbnails.getThumbnail(context.getContentResolver(),fileId,MediaStore.Images.Thumbnails.MICRO_KIND,null);
//    }

//
//    private String convertToString() {
//        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
//        byte[] imgByte = byteArrayOutputStream.toByteArray();
//        return Base64.encodeToString(imgByte, Base64.DEFAULT);
//    }


    private void updateData(Bitmap bitmap) {
        // imageUriRetrofit = uri;
        // Log.e("connect0000", uri);

        MultipartBody.Part originalFile = Const.bitmapConvertToFile(getContext(), bitmap, 0);
        Bitmap thumbNail = Const.resizeThumnail(bitmap, bitmap.getWidth(), bitmap.getHeight());
        MultipartBody.Part thumbnailFile = Const.bitmapConvertToFile(getContext(), thumbNail, 1);

        //비트맵 다 썼으니까 리사이클링으로 메모리 회수
        bitmap.recycle();
        thumbNail.recycle();

        String getToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJlbWFpbCI6InRlc3QxMUB0ZXN0dGVzdHRlc3QuY29tIi" +
                "widWlkIjoxNTQsIm5pY2tuYW1lIjoiaGgiLCJpYXQiOjE1OTQ4MDMzMzAsImV4cCI6MTU5NDg4OTczMH0.Gj-9LMIZAL7" +
                "-Q0ps-s_nnWR_jb-GnrzHZykORlBSzLA";

        RetrofitInterface api = RetrofitClient.getRestMethods();

        // Log.e("connect00", "lll");
        Call<ApiResultDto> call = api.pimage(getToken, originalFile, thumbnailFile);
        // Log.e("connect0", "aaaaa");
        call.enqueue(new Callback<ApiResultDto>() {
            @Override
            public void onResponse(Call<ApiResultDto> call, Response<ApiResultDto> response) {
                //     Log.e("connect1", "aaaa" + response);
                if (response.isSuccessful()) {
                    //       Log.e("connect1", "vvvv" + response);
                    ApiResultDto apiResultDto = response.body();
                    JsonObject resultData = apiResultDto.getResultData();

                    PImageData pImageData = new Gson().fromJson(resultData, PImageData.class);


                    String originalImage = IMG_BASE_URL + pImageData.getStoredPath();
                    //setData(thumbnailImage);
                    Log.i("originalImage", originalImage);

                    //originalUrl = originalImage;


                    //  String imagePath = baseStoragePath+resultData.get("uri").getAsString();
                    // String img = response.body().getResultData().get("storedPath").getAsString();

                    SharedPreferenceBase.getInstance().setString("PROFILEPATH",pImageData.getStoredPath());
                    String path =SharedPreferenceBase.getInstance().getString("PROFILEPATH",null);

                    Log.d("-----path ----",IMG_BASE_URL+path);
                    //split에 대한 예외 처리 ,
                    if(path!=null &&path.isEmpty()){
                        String[] names=path.split("\\.");
                        String thumbNailPath =names[0]+"Thumbnail";
                        setImageResource(IMG_BASE_URL+thumbNailPath, profileView);
                        //setOriginalImage(IMG_BASE_URL+path);

                        Toast.makeText(getActivity(), path, Toast.LENGTH_LONG).show();
                    }

                } else {
                    //Toast.makeText(getContext(),)
                }
            }

            @Override
            public void onFailure(Call<ApiResultDto> call, Throwable t) {
                Log.d(TAG, "onFailure() called with: call = [" + call + "], t = [" + t + "]");
                //Logm 으로 하면 자동 Log가 입력이 된다. 이후 TAG 설정을 위해 Logt를 입력하여서 자동 완성 시킨다.
            }
        });
    }


    public void setImageResource(String url, ImageView imageView) {
        Glide.with(this)
                .load(url)
                .circleCrop()
                .into(imageView);
    }


}
