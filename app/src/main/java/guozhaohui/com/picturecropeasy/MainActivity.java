package guozhaohui.com.picturecropeasy;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import guozhaohui.com.picturecropeasy.crop.UCrop;
import guozhaohui.com.picturecropeasy.crop.UCropActivity;




public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private Button bt_photo,bt_album,bt_savePic;
    private  Uri photoUri;
    private ImageView iv;
    // 剪切后图像文件
    private Uri mDestinationUri;
    private Bitmap bitmapLast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDestinationUri = Uri.fromFile(new File(getCacheDir(), "cropImage.jpeg"));

        bt_photo = (Button) this.findViewById(R.id.bt_photo);
        bt_photo.setOnClickListener(this);
        bt_album = (Button) this.findViewById(R.id.bt_album);
        bt_album.setOnClickListener(this);
        bt_savePic = (Button) this.findViewById(R.id.bt_savePic);
        iv = (ImageView) this.findViewById(R.id.iv);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){

            case R.id.bt_photo:  //拍照，打开相机，然后将图片显示在手机上

                takePhoto();

                break;

            case R.id.bt_album:

                if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){

                    ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);

                }else{
                    takeAlbum();
                }

                break;


        }
    }

    /**
     * 打开相机
     */
    public void takePhoto(){

        //将照片文件保存在缓存文件夹中，注意这里使用的是缓存文件夹，地址应该是sdcard/android/包名/cache，这样子拍照完后在图库是看不到拍照照片的
        //如果想在图库看到照片 则这里应该用 Environment.getExternalStorageDirectory()
        File photoFile = new File(getExternalCacheDir(),"hehe.jpg");

        if(photoFile.exists()){
            photoFile.delete();
        }else{
            try {
                photoFile.createNewFile();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * 运行到这，photoFile已经存在，这里需要获得这个文件的uri
         * 分两种情况，android7.0以上和以下
         */
        if(Build.VERSION.SDK_INT>=24){

            /**
             * FileProvider.getUriForFile(),这个方法中需要填写三个参数，
             * 第一个Context，
             * 第二个S
             * tring 任意
             * 第三个File
             */
            photoUri = FileProvider.getUriForFile(this, "guozhaohui.com.picturecropeasy", photoFile);


        }else{
            photoUri = Uri.fromFile(photoFile);
        }


        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        intent.putExtra(MediaStore.EXTRA_OUTPUT,photoUri);
        startActivityForResult(intent,100);

    }

    /**
     * 打开相册
     */
    public void takeAlbum(){
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
//        startActivityForResult(Intent.createChooser(intent,"选择图片aaaa"),200);
        startActivityForResult(intent,200);
    }

    /**
     * 申请权限返回的结果
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){

            case 1:
                if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED){ //同意

                    takeAlbum();

                }else{//拒绝
                    Toast.makeText(MainActivity.this,"拒绝使用相册权限",Toast.LENGTH_SHORT).show();
                }

                break;

        }
    }

    /**
     * 拍照，相册选择图片完成回调这个方法
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==RESULT_OK){

            if(requestCode==100){//拍照

                 try {
                        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(photoUri));
                        iv.setImageBitmap(bitmap);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

            }

            if(requestCode==200){  //相册
                Uri uriBeforeCrop = data.getData();
                if(uriBeforeCrop!=null){
                    startCropActivity(data.getData());
                }else{
                    Toast.makeText(MainActivity.this, "无法剪切选择图片", Toast.LENGTH_SHORT).show();
                }
            }else if(requestCode == UCrop.REQUEST_CROP){
                //处理裁剪后的结果
                handleCropResult(data);
            }


        }
//        switch (requestCode){
//
//            case 100:  //拍照返回结果
//
//                if(resultCode==RESULT_OK){
//
//                    try {
//                        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(photoUri));
//                        iv.setImageBitmap(bitmap);
//                    } catch (FileNotFoundException e) {
//                        e.printStackTrace();
//                    }
//
//                }
//
//                break;
//
//            case 200:   //相册返回结果
//
//                if(resultCode==RESULT_OK){
//
////                    if(Build.VERSION.SDK_INT>=19){  //版本4.4以上选取图片返回的uri处理方式
////
////                        handleImgOver(data);
////
////                    }else{  //版本4.4以下选取图片返回的uri处理方式
////                        handleImgBefore(data);
////                    }
//
//                }
//                break;
//
//        }

    }


    /**
     * 4.4以上对返回的Intent处理，获取图片的path
     * @param data
     */
    @TargetApi(19)
    public void handleImgOver(Uri data){

        String imagePath = null;
        Uri uri = data;
        if(DocumentsContract.isDocumentUri(this,uri)){
            //如果是document类型的uri，则通过documentid处理
            String docId = DocumentsContract.getDocumentId(uri);
            if("com.android.providers.media.documents".equals(uri.getAuthority())){

                String id = docId.split(":")[1];
                String selection = MediaStore.Images.Media._ID+"="+id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,selection);

            }else if("com.android.providers.downloads.documents".equals(uri.getAuthority())){

                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),Long.valueOf(docId));
                imagePath = getImagePath(contentUri,null);

            }
        }else if("content".equalsIgnoreCase(uri.getScheme())){
            //如果是content类型的uri，则使用普通方式处理
            imagePath = getImagePath(uri, null);
        }else if("file".equalsIgnoreCase(uri.getScheme())){
            //如果是file类型的uri，直接获取图片的路径
            imagePath = uri.getPath();
        }

        showImg(imagePath);

    }

    /**
     * 4.4以下对Intent的处理，获取img的path，直接将uri传入getImagePath()就可以获取图片的真实路径了
     * @param data
     */
    public void handleImgBefore(Uri data){

        Uri uri = data;
        String imgPath = getImagePath(uri, null);
        showImg(imgPath);

    }

    /**
     * 此方法用于uri是content类型，通过这个方法可以获取路径path
     * @param uri
     * @param selection
     * @return
     */
    public String getImagePath(Uri uri, String selection){
        String path = null;
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        if(cursor!=null){
            if(cursor.moveToFirst()){
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }


    /**
     * 根据路径获取bitmap，然后显示
     * @param path
     */
    public void showImg(String path){

        if(path!=null){
            bitmapLast = BitmapFactory.decodeFile(path);
            iv.setImageBitmap(bitmapLast);
            handler.sendEmptyMessage(1);
        }
    }


    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what==1){
                bt_savePic.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(bitmapLast!=null){
                            saveImgToSystem(MainActivity.this,bitmapLast);
                            Toast.makeText(MainActivity.this,"保存成功",Toast.LENGTH_SHORT).show();
                        }

                    }
                });
            }


        }
    };


    /**
     * 开始剪切图片
     * @param uri
     */
    private void startCropActivity(Uri uri) {
        UCrop.of(uri, mDestinationUri)
                .withTargetActivity(UCropActivity.class)
                .withAspectRatio(1, 1)
//                .withMaxResultSize(500, 500)
                .start(MainActivity.this);

    }

    /**
     * 处理剪切后的返回值
     * @param result
     */
    private void handleCropResult(Intent result) {
        final Uri resultUri = UCrop.getOutput(result);

        //这里分两种情况试一试
//        if(Build.VERSION.SDK_INT>=19){
//            handleImgOver(resultUri);
//        }else{
//            handleImgBefore(resultUri);
//        }

        if (resultUri != null) {
            Bitmap bmp;
            try {
                bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), resultUri);
                bitmapLast = bmp;
                iv.setImageBitmap(bitmapLast);
                handler.sendEmptyMessage(1);
            } catch (FileNotFoundException e) {
            } catch (IOException e) {
            }
        } else {
            Toast.makeText(MainActivity.this, "无法剪切选择图片", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 仅把图片保存在系统图库中
     * @param context
     * @param bmp
     */
    public void saveImgToSystem(Context context, Bitmap bmp){

        // 直接把图片插入到系统图库，不保存在本地自己建立的文件夹中，避免产生两张一样的图片
        MediaStore.Images.Media.insertImage(context.getContentResolver(), bmp, "title", "description");

        //发送广播，提醒刷新
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File("/"+ Environment.getExternalStorageDirectory()+"/image.jpg"))));

    }


    /**
     * 既保存在系统图库中也保存在自己建立的图库文件夹中
     * @param context
     * @param bmp
     */
    public void saveImgToDouble(Context context, Bitmap bmp){
       // 首先保存图片
        File appDir = new File(Environment.getExternalStorageDirectory(), "hehe");
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        String fileName = System.currentTimeMillis() + ".jpg";
        File file = new File(appDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 其次把文件插入到系统图库
        try {
            MediaStore.Images.Media.insertImage(context.getContentResolver(),
                    file.getAbsolutePath(), fileName, null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        // 最后通知图库更新
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + file.getPath())));
    }

}
