package com.monkeybits.edilcloud;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.MimeTypeMap;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.judemanutd.autostarter.AutoStartPermissionHelper;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


class OneSignalSetUser {
    @JavascriptInterface
    public void postMessage(String data) throws IOException {
        final String TAG = "WebviewExample";
        Log.d(TAG, "setExternalUserId: userid is " + data);
        FirebaseMessaging.getInstance().subscribeToTopic("user"+data)
                .addOnCompleteListener(task -> {
                    String msg = "msg subscribed";
                    if (!task.isSuccessful()) {
                        msg = "msg subscribe failed";
                    }
                    Log.d(TAG, msg);
                });

    }
}

class RedirectBrowser {
    private final WebView webview;
    private Context context;

    public RedirectBrowser(Context context, WebView webview) {
        this.context = context;
        this.webview = webview;
    }

    @JavascriptInterface
    public void postMessage(String url) throws IOException {
//        WebView mWebView = webview.findViewById(R.id.root_webview);
//        mWebView.loadUrl(url);
    }
}

class DownloadFiles {
    private Context context;

    public DownloadFiles(Context context) {
        this.context = context;
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

    @JavascriptInterface
    public void postMessage(String data) throws IOException {
        final String TAG = "WebviewExample";
        final String url = data;
        Log.d(TAG, "DownloadFiles: file url is " + data);
        String destination = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/";
        String fileName = url.substring(url.lastIndexOf('/') + 1, url.length());

        Log.d(TAG, "onDownloadStart: filename is " + fileName);

        destination += fileName;

        final Uri uri = Uri.parse("file://" + destination);

        //Delete update file if exists
        final File file = new File(destination);
        if (file.exists()) {    //file.delete() - test this, I think sometimes it doesnt work
            file.delete();
        }

        final Uri uriForFile = FileProvider.getUriForFile(context, "com.monkeybits.edilcloud.download.fileprovider", file);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(data));
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDescription("Downloading file");
        request.setTitle("EdilCloud.io: " + fileName);

        //set destination
        request.setDestinationUri(uri);

        // get download service and enqueue file
        final DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);

        if (manager != null) {
            final long downloadId = manager.enqueue(request);

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
                        context.startActivity(newIntent);
                    } catch (ActivityNotFoundException e) {
                    }

                    context.unregisterReceiver(this);

                }
            };
            //register receiver for when .apk download is compete
            //context.registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        } else {
            Log.d("uneva", "download manager is null");
        }
    }
}

class JavaScriptInterface {
    private Context context;

    public JavaScriptInterface(Context context) {
        this.context = context;
    }

    @JavascriptInterface
    public void getBase64FromBlobData(String base64Data) throws IOException {
        convertBase64StringToPdfAndStoreIt(base64Data);
    }

    public static String getBase64StringFromBlobUrl(String blobUrl) {
        if (blobUrl.startsWith("blob")) {
            return "javascript: var xhr = new XMLHttpRequest();" +
                    "xhr.open('GET', '" + blobUrl + "', true);" +
                    "xhr.setRequestHeader('Content-type','application/pdf');" +
                    "xhr.responseType = 'blob';" +
                    "xhr.onload = function(e) {" +
                    "    if (this.status == 200) {" +
                    "        var blobPdf = this.response;" +
                    "        var reader = new FileReader();" +
                    "        reader.readAsDataURL(blobPdf);" +
                    "        reader.onloadend = function() {" +
                    "            base64data = reader.result;" +
                    "            Android.getBase64FromBlobData(base64data);" +
                    "        }" +
                    "    }" +
                    "};" +
                    "xhr.send();";
        }
        return "javascript: console.log('It is not a Blob URL');";
    }

    private void convertBase64StringToPdfAndStoreIt(String base64PDf) throws IOException {
        final int notificationId = 1;
        String currentDateTime = DateFormat.getDateTimeInstance().format(new Date());
        final File dwldsPath = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS) + "/YourFileName_" + currentDateTime + "_.jpg");
        byte[] pdfAsBytes = Base64.decode(base64PDf.replaceFirst("^data:image/jpeg;base64,", ""), 0);
        FileOutputStream os;
        os = new FileOutputStream(dwldsPath, false);
        os.write(pdfAsBytes);
        os.flush();

        if (dwldsPath.exists()) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            Uri apkURI = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", dwldsPath);
            intent.setDataAndType(apkURI, MimeTypeMap.getSingleton().getMimeTypeFromExtension("jpg"));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            String CHANNEL_ID = "MYCHANNEL";
            final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, "name", NotificationManager.IMPORTANCE_LOW);
                Notification notification = new Notification.Builder(context, CHANNEL_ID)
                        .setContentText("You have got something new!")
                        .setContentTitle("File downloaded")
                        .setContentIntent(pendingIntent)
                        .setChannelId(CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.sym_action_chat)
                        .build();
                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(notificationChannel);
                    notificationManager.notify(notificationId, notification);
                }

            } else {
                NotificationCompat.Builder b = new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .setWhen(System.currentTimeMillis())
                        .setSmallIcon(android.R.drawable.sym_action_chat)
                        //.setContentIntent(pendingIntent)
                        .setContentTitle("MY TITLE")
                        .setContentText("MY TEXT CONTENT");

                if (notificationManager != null) {
                    notificationManager.notify(notificationId, b.build());
                    Handler h = new Handler();
                    long delayInMilliseconds = 1000;
                    h.postDelayed(() -> notificationManager.cancel(notificationId), delayInMilliseconds);
                }
            }
        }
        Toast.makeText(context, "PDF FILE DOWNLOADED!", Toast.LENGTH_SHORT).show();
    }
}

public class MainActivity extends AppCompatActivity {
    private WebView mWebviewPop;
    private FrameLayout mContainer;
    private AlertDialog builder;
    private Context globalContext;
    public static WebView mWebView;
    private String baseURL = "https://app.edilcloud.io";
    private static final String ONESIGNAL_APP_ID = "0fbdf0cf-d9f5-4363-809f-4735b1bba268";
    public static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) ' + 'AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.94 Mobile Safari/537.36";
    private static final String target_url_prefix = "test.edilcloud.io";
    public static boolean activityStarted;


    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseMessaging.getInstance().setAutoInitEnabled(true);

        // Intent intent = new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS);
        //  intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // startActivity(intent);

        /*Intent  intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
*/
        activityStarted = true;
        // Enable verbose OneSignal logging to debug issues if needed.
//        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE);
//
//        // OneSignal Initialization
//        OneSignal.startInit(this).inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
//                .unsubscribeWhenNotificationsAreDisabled(true).setNotificationOpenedHandler(new MyNotificationOpenedHandler())
//                .init();


        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CAMERA,
                }, 101);

        mWebView = findViewById(R.id.root_webview);

        if (getIntent().getStringExtra("url")!=null){
            mWebView.loadUrl(getIntent().getStringExtra("url"));
        }else {
            mWebView.loadUrl(baseURL);
        }


        mContainer = findViewById(R.id.webview_frame);

        WebSettings webSettings = mWebView.getSettings();
        //webSettings.setForceDark(webSettings.FORCE_DARK_OFF);
        //used for javascript and dom functioning
        webSettings.setUserAgentString(USER_AGENT);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportMultipleWindows(true);
        webSettings.setMediaPlaybackRequiresUserGesture(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        mWebView.setBackgroundColor(Color.parseColor("#919191"));
        mWebView.setWebViewClient(new MyWebViewClient());
        mWebView.setBackgroundColor(0);
        mWebView.getBackground().setAlpha(0);
        mWebView.setWebChromeClient(new MyChromeClient());
        globalContext = this.getApplicationContext();
        webSettings.setSaveFormData(true);
        mWebView.addJavascriptInterface(new JavaScriptInterface(getBaseContext()), "Android");
        mWebView.addJavascriptInterface(new OneSignalSetUser(), "OneSignalSetUser");
        mWebView.addJavascriptInterface(new DownloadFiles(getBaseContext()), "DownloadFiles");
        mWebView.addJavascriptInterface(new RedirectBrowser(getBaseContext(), mWebView), "RedirectBrowser");
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cookieManager.setAcceptThirdPartyCookies(mWebView, true);
        }
//        mWebView.setDownloadListener(new DownloadListener() {
//            public void onDownloadStart(String url, String userAgent,
//                                        String contentDisposition, String mimetype,
//                                        long contentLength) {
//                mWebView.loadUrl(JavaScriptInterface.getBase64StringFromBlobUrl(url));
//            }
//        });
        AutoStartPermissionHelper.getInstance().getAutoStartPermission(getBaseContext());
        AutoStartPermissionHelper.getInstance().isAutoStartPermissionAvailable(getBaseContext());

        //remove after testing
        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.e("--->Error", consoleMessage.message() + "sourceId " + consoleMessage.sourceId());
                return super.onConsoleMessage(consoleMessage);
            }
        });
    }

    private void download(final String url) {

        String destination = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/";
        String fileName = "ciao.jpg";

        Log.d(TAG, "onDownloadStart: filename is " + fileName);

        destination += fileName;

        final Uri uri = Uri.parse("file://" + destination);

        //Delete update file if exists
        final File file = new File(destination);
        if (file.exists()) {    //file.delete() - test this, I think sometimes it doesnt work
            file.delete();
        }

        final Uri uriForFile = FileProvider.getUriForFile(this, "com.monkeybits.edilcloud.download.fileprovider", file);
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
    File photoFile = null;
    private static final String TAG = "WebviewExample";

    private class MyChromeClient extends WebChromeClient {
        @Override
        public void onPermissionRequest(final PermissionRequest request) {
            request.grant(request.getResources());
        }

        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
            mWebviewPop = new WebView(globalContext);
            mWebviewPop.setVerticalScrollBarEnabled(false);
            mWebviewPop.setHorizontalScrollBarEnabled(false);
            mWebviewPop.setWebChromeClient(new MyChromeClient());
            mWebviewPop.getSettings().setJavaScriptEnabled(true);
            mWebviewPop.getSettings().setSaveFormData(true);
            mWebviewPop.getSettings().setEnableSmoothTransition(true);
            mWebviewPop.getSettings().setUserAgentString(USER_AGENT + "yourAppName");


            // pop the  webview with alert dialog
            builder = new AlertDialog.Builder(MainActivity.this).create();
            builder.setTitle("");
            builder.setView(mWebviewPop);

            builder.setButton("Close", (dialog, id) -> {
                mWebviewPop.destroy();
                dialog.dismiss();
            });

            builder.show();
            builder.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);
            if (Build.VERSION.SDK_INT >= 21) {
                cookieManager.setAcceptThirdPartyCookies(mWebviewPop, true);
                cookieManager.setAcceptThirdPartyCookies(mWebView, true);
            }

            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(mWebviewPop);
            resultMsg.sendToTarget();

            return true;
        }

        @Override
        public void onCloseWindow(WebView window) {
            //Toast.makeText(contextPop,"onCloseWindow called",Toast.LENGTH_SHORT).show();
            try {
                mWebviewPop.destroy();
            } catch (Exception e) {
                Log.d("Destroyed with Error ", e.getStackTrace().toString());
            }

            try {
                builder.dismiss();
            } catch (Exception e) {
                Log.d("Dismissed with Error: ", e.getStackTrace().toString());
            }

        }

        public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePath, FileChooserParams fileChooserParams) {
            if (mFilePathCallback != null) {
                mFilePathCallback.onReceiveValue(null);
            }
            mFilePathCallback = filePath;
            showCameraAndFileChooser(fileChooserParams);
            return true;
        }
    }

    public void takeCameraPhoto() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            Toast.makeText(this, "Device without camera", Toast.LENGTH_SHORT).show();
            return;
        }
        String filePath = getExternalFilesDir("/").getAbsolutePath();

        //String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath();

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File mFileFromCamera = new File(filePath);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mTakePhotoFile));
            startActivityForResult(intent, REQUEST_FILE_CAMERA_CODE);
        } else {
            try {
                mTakePhotoFile = File.createTempFile("JPEG_" + System.nanoTime(), ".jpg", mFileFromCamera);
                Uri contentUri = FileProvider.getUriForFile(MainActivity.this, "com.monkeybits.edilcloud.download.fileprovider", mTakePhotoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, contentUri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                startActivityForResult(intent, REQUEST_FILE_CAMERA_CODE);
            } catch (IOException e) {
                Toast.makeText(this, "IOException" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void showCameraAndFileChooser(final WebChromeClient.FileChooserParams fileChooserParams) {

        CharSequence[] itemlist = {"Take a Photo", "Pick from Gallery", "Open from File"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.mipmap.ic_launcher);
        builder.setTitle("Scegli o scatta");
        builder.setItems(itemlist, (dialog, which) -> {
            switch (which) {
                case 0:
                    takeCameraPhoto();
                    break;
                case 1:
                    pickFromGallery();
                    break;
                case 2:
                    openFromFile(fileChooserParams);
                    break;
                default:
                    break;
            }
        });
        AlertDialog alert = builder.create();
        alert.setCancelable(true);
        alert.show();
    }

    private void pickFromGallery() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        startActivityForResult(Intent.createChooser(i, "File Browser"), REQUEST_FILE_CHOOSER_CODE);
    }

    private void openFromFile(final WebChromeClient.FileChooserParams fileChooserParams) {
        try {
            Intent intent = fileChooserParams.createIntent();
            startActivityForResult(intent, REQUEST_SELECT_FILE_CODE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(globalContext, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    int REQUEST_FILE_CAMERA_CODE = 1001;
    int REQUEST_SELECT_FILE_CODE = 1002;
    int REQUEST_FILE_CHOOSER_CODE = 1003;
    private File mTakePhotoFile;

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        String filePath = getExternalFilesDir("/").getAbsolutePath();

        // String destination = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/";
        return File.createTempFile(imageFileName, ".jpg", new File(filePath));
    }

    private static Bitmap rotateBitmap(Bitmap bitmap, int orientation) {
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                return bitmap;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default:
                return bitmap;
        }
        try {
            Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();

            return bmRotated;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mFilePathCallback == null) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        Uri[] results = null;


        if (requestCode == REQUEST_FILE_CAMERA_CODE && resultCode == Activity.RESULT_OK) {
            if (mTakePhotoFile != null) {

                File compressedImageFile = null;
                try {
                    File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

                    if (storageDir != null && photoFile != null) {
                        compressedImageFile = createImageFile();
                        FileOutputStream fileOutputStream = new FileOutputStream(compressedImageFile);
                        Bitmap originalBitmap = BitmapFactory.decodeFile(photoFile.getPath());
                        ExifInterface exif = new ExifInterface(photoFile.getPath());
                        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
                        Bitmap rotatedBitmap = rotateBitmap(originalBitmap, orientation);
                        assert rotatedBitmap != null;
                        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, fileOutputStream);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(globalContext, "Exception" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    compressedImageFile = null;
                }

                if (compressedImageFile != null) {
                    results = new Uri[]{Uri.parse("file:" + compressedImageFile.getAbsolutePath())};
                } else {
                    results = new Uri[]{Uri.parse("file:" + mTakePhotoFile.getAbsolutePath())};
                }


            }
        } else if (requestCode == REQUEST_SELECT_FILE_CODE || requestCode == REQUEST_FILE_CHOOSER_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                results = WebChromeClient.FileChooserParams.parseResult(resultCode, data);

            }
        }

        mFilePathCallback.onReceiveValue(results);
        mFilePathCallback = null;

    }


    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            String host = Uri.parse(url).getHost();
            if (url.startsWith("mailto:")) {
                startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse(url)));
                return true;
            } else if (url.startsWith("tel:")) {
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse(url)));
                return true;
            } else {
            }
            if (url.contains("customer-portal") || url.contains("billing")) {
                return false;
            }
            //Log.d("shouldOverrideUrlLoading", url);
            if (host.equals(target_url_prefix)) {
                // This is my web site, so do not override; let my WebView load
                // the page
                if (mWebviewPop != null) {
                    mWebviewPop.setVisibility(View.GONE);
                    mContainer.removeView(mWebviewPop);
                    mWebviewPop = null;
                }
                return false;
            }

            if (host.equals("m.facebook.com")) {
                return false;
            }
//            // Otherwise, the link is not for a page on my site, so launch
//            // another Activity that handles URLs
            return true;
        }
    }

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


 @Override
 protected void onDestroy() {
     super.onDestroy();
     activityStarted=false;
 }

    @Override
    protected void onPause() {
        super.onPause();
        activityStarted=true;

    }

        @Override
    protected void onResume() {
        super.onResume();
        activityStarted=false;
    }
}

class BootUpReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent(context, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }
}



