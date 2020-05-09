package com.paras.webviewexample;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.DownloadListener;
import android.webkit.MimeTypeMap;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private WebView mWebView;
    private String baseURL = "https://parasasblog.wordpress.com/2020/05/09/how-to-create-webview-in-android/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            // No explanation needed; request the permission
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    }, 0);
        }

        mWebView = findViewById(R.id.root_webview);
        mWebView.loadUrl(baseURL);

        WebSettings webSettings = mWebView.getSettings();

        //used for javascript and dom functioning
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        mWebView.setWebViewClient(new MyWebViewClient());

        mWebView.setWebChromeClient(new MyChromeClient());

        mWebView.setDownloadListener(new DownloadListener() {
            public void onDownloadStart(String url, String userAgent,
                                        String contentDisposition, String mimetype,
                                        long contentLength) {
                download(url);
            }
        });
    }

    private void download(final String url) {

        String destination = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/";
        String fileName = url.substring(url.lastIndexOf("/") + 1);

        Log.d(TAG, "onDownloadStart: filename is " + fileName);

        destination += fileName;

        final Uri uri = Uri.parse("file://" + destination);

        //Delete update file if exists
        final File file = new File(destination);
        if (file.exists()) {    //file.delete() - test this, I think sometimes it doesnt work
            file.delete();
        }

        final Uri uriForFile = FileProvider.getUriForFile(this, "com.paras.webviewexample.download.fileprovider", file);

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDescription("Downloading file");
        request.setTitle("Please wait");


        //set destination
        request.setDestinationUri(uri);

        // get download service and enqueue file
        final DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

        if (manager != null) {
            final long downloadId = manager.enqueue(request);
            Toast.makeText(this, "File will be saved to Downloads", Toast.LENGTH_SHORT).show();

            //set BroadcastReceiver to install app when .apk is downloaded
            BroadcastReceiver onComplete = new BroadcastReceiver() {
                public void onReceive(Context ctxt, Intent intent) {

                    Intent newIntent = new Intent(Intent.ACTION_VIEW);
                    newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    newIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    MimeTypeMap myMime = MimeTypeMap.getSingleton();
                    String extmethod = fileExt(url);
                    Log.d(TAG, "onReceive: fileext Is " + extmethod);
                    String mimeType = myMime.getMimeTypeFromExtension(extmethod);
                    Log.d(TAG, "onReceive: mime type is " + mimeType);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        newIntent.setData(uriForFile);


                    } else {
                        Uri apkUri = Uri.fromFile(file);
                        newIntent.setData(apkUri);

                    }

                    newIntent.setType(mimeType);

                    try {
                        startActivity(newIntent);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(MainActivity.this, "No handler for this type of file.", Toast.LENGTH_LONG).show();
                    }

                    unregisterReceiver(this);

                }
            };
            //register receiver for when .apk download is compete
            registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        } else {
            Log.d("uneva", "download manager is null");
        }
    }

    private String fileExt(String url) {
        if (url.contains("?")) {
            url = url.substring(0, url.indexOf("?"));
        }
        if (url.lastIndexOf(".") == -1) {
            return null;
        } else {
            String ext = url.substring(url.lastIndexOf(".") + 1);
            if (ext.contains("%")) {
                ext = ext.substring(0, ext.indexOf("%"));
            }
            if (ext.contains("/")) {
                ext = ext.substring(0, ext.indexOf("/"));
            }
            return ext.toLowerCase();
        }
    }

    private ValueCallback<Uri[]> mFilePathCallback;
    private String mCameraPhotoPath;
    File photoFile = null;
    private static final String TAG = "WebviewExample";
    private static final int INPUT_FILE_REQUEST_CODE = 1;

    private class MyChromeClient extends WebChromeClient {
        public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePath, WebChromeClient.FileChooserParams fileChooserParams) {
            // Double check that we don't have any existing callbacks
            if (mFilePathCallback != null) {
                mFilePathCallback.onReceiveValue(null);
            }
            mFilePathCallback = filePath;
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                // Create the File where the photo should go

                try {
                    photoFile = createImageFile();
                    takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
                } catch (IOException ex) {
                    // Error occurred while creating the File

                    Log.e(TAG, "Unable to create Image File");
                    ex.printStackTrace();
                }
                // Continue only if the File was successfully created
                if (photoFile != null) {

                    mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                            Uri.fromFile(photoFile));
                } else {
                    takePictureIntent = null;
                }
            }
            Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
            contentSelectionIntent.setType("image/*");
            String[] mimetypes = {"application/pdf", "image/jpg", "image/jpeg", "image/png"};
            contentSelectionIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);

            Intent[] intentArray;
            if (takePictureIntent != null) {
                intentArray = new Intent[]{takePictureIntent};
            } else {
                intentArray = new Intent[0];
            }
            Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);

            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "Document Chooser");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
            startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE);
            return true;
        }
    }


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
    }





    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        Uri[] results = null;
        // Check that the response is a good one
        if (resultCode == Activity.RESULT_OK) {
            if (data == null) {
                // If there is not data, then we may have taken a photo
                if (mCameraPhotoPath != null) {
                    //TODO: can we compress this camera photo?
                    File compressedImageFile = null;

                   //*********Remove this code if you don't want image compression
                    try {
                        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

                        if (storageDir != null && photoFile != null) {
                            compressedImageFile = createImageFile();
                            Log.d(TAG, "onActivityResult: Compressing image");
                            FileOutputStream fileOutputStream = new FileOutputStream(compressedImageFile);
                            Bitmap originalBitmap = BitmapFactory.decodeFile(photoFile.getPath());
                            originalBitmap.compress(Bitmap.CompressFormat.JPEG, 80, fileOutputStream);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        compressedImageFile = null;
                    }
                    //*********till here only remove this code********

                    if (compressedImageFile != null) {
                        results = new Uri[]{Uri.parse("file:" + compressedImageFile.getAbsolutePath())};
                    } else {
                        results = new Uri[]{Uri.parse(mCameraPhotoPath)};
                    }


                }
            } else {
                String dataString = data.getDataString();
                if (dataString != null) {
                    results = new Uri[]{Uri.parse(dataString)};
                }
            }
        }
        mFilePathCallback.onReceiveValue(results);
        mFilePathCallback = null;
    }


    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            String domain = Uri.parse(baseURL).getHost();
            if (domain != null && domain.equals(Uri.parse(url).getHost())) {
                // This is my website, so do not override; let my WebView load the page
                return false;
            }
            // Otherwise, the link is not for a page on my site,
            // so launch another Activity that handles URLs
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            return true;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Check if the key event was the Back button and if there's history
        if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        }
        // If it wasn't the Back key or there's no web page history, bubble up to the default
        // system behavior (probably exit the activity)
        return super.onKeyDown(keyCode, event);
    }

}
