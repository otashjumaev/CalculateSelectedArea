package com.example.myapp1st;

import java.io.File;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.yalantis.ucrop.UCrop;

public class MainActivity extends Activity implements OnClickListener, OnTouchListener {

    ImageView imageView;
    Button choosePicture;
    Button cameraBt;
    Button findAreaBt;
    Button clearScreen;

    TextView textView;

    String currentPhotoPath;
    Bitmap bmp;
    Bitmap alteredBitmap;
    Canvas canvas;
    Paint paint;
    Matrix matrix;
    float downx = 0;
    float downy = 0;
    private List<Point> pointsList;
    private List<Point> rPointsList;

    static final int REQUEST_TAKE_PHOTO = 7;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.CAMERA)) {
            Toast.makeText(MainActivity.this, "CAMERA permission allows us to Access CAMERA app", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, 1);
        }

        imageView = this.findViewById(R.id.ChoosenImageView);
        choosePicture = this.findViewById(R.id.ChoosePictureButton);
        cameraBt = this.findViewById(R.id.cameraBt);
        findAreaBt = this.findViewById(R.id.findAreaBt);
        clearScreen = this.findViewById(R.id.clearScreen);
        textView = findViewById(R.id.areaText);

        cameraBt.setOnClickListener(this);
        findAreaBt.setOnClickListener(this);
        clearScreen.setOnClickListener(this);
        choosePicture.setOnClickListener(this);
        imageView.setOnTouchListener(this);
    }

    @SuppressLint("SetTextI18n")
    public void onClick(View v) {
        if (v == choosePicture) {
            Intent choosePictureIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(choosePictureIntent, 0);
        } else if (v == cameraBt) {
            dispatchTakePictureIntent();
        } else if (v == clearScreen) {
            setNewImage(alteredBitmap, bmp);
        } else if (v == findAreaBt) {
            textView.setText("Area = " + findArea() + " sq.cm.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] result) {
        switch (requestCode) {
            case 1:
                if (result.length > 0 && result[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "Permission Granted, Now your application can access CAMERA.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, "Permission Canceled, Now your application cannot access CAMERA.", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }


    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(currentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            galleryAddPic();
            cropImage(Uri.fromFile(new File(currentPhotoPath)));

        } else if (resultCode == RESULT_OK && requestCode == 0) {
            Uri imageFileUri = intent.getData();
            cropImage(imageFileUri);

        } else if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            final Uri resultUri = UCrop.getOutput(intent);
            try {
                BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
                bmpFactoryOptions.inJustDecodeBounds = true;
                bmp = BitmapFactory
                        .decodeStream(
                                getContentResolver().openInputStream(
                                        resultUri), null, bmpFactoryOptions);

                bmpFactoryOptions.inJustDecodeBounds = false;
                bmp = BitmapFactory
                        .decodeStream(
                                getContentResolver().openInputStream(
                                        resultUri), null, bmpFactoryOptions);
            } catch (Exception e) {
                Log.v("ERROR", e.toString());
            }
            alteredBitmap = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), bmp.getConfig());
            setNewImage(alteredBitmap, bmp);
        }
    }

    void cropImage(Uri uri) {
        Uri contentUri1 = uri;
//        Uri contentUri1 = FileProvider.getUriForFile(this, "com.example.android.fileprovider", new File(currentPhotoPath));
        Uri photoURI = null;
        try {
            photoURI = Uri.fromFile(createImageFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
        Toast.makeText(this, contentUri1.getPath(), Toast.LENGTH_SHORT).show();
        UCrop.of(contentUri1, photoURI)
                .withAspectRatio(1,1.4142f)
                .start(this);
    }

    public void setNewImage(Bitmap alteredBitmap, Bitmap bmp) {
        canvas = new Canvas(alteredBitmap);
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(15);
        paint.setStyle(Paint.Style.STROKE);
        matrix = new Matrix();
        canvas.drawBitmap(bmp, matrix, paint);
        if (pointsList == null) pointsList = new ArrayList<Point>();
        else pointsList.clear();
        if (rPointsList == null) rPointsList = new ArrayList<Point>();
        else rPointsList.clear();
        imageView.setImageBitmap(bmp);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        float upx1 = event.getX();
        float upy1 = event.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:

                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                downx = getPointerCoords(event)[0];//event.getX();
                downy = getPointerCoords(event)[1];//event.getY();
                canvas.drawBitmap(bmp, matrix, paint);
                imageView.setImageBitmap(alteredBitmap);

                pointsList.add(new Point((int) downx, (int) downy));
                rPointsList.add(new Point((int) event.getX(), (int) event.getY()));
                Point first = pointsList.get(0);
                Point last = pointsList.get(pointsList.size() - 1);
                canvas.drawCircle(first.x, first.y, 5, paint);
                Path path = new Path();

                path.moveTo(first.x, first.y);

                // Iterate on the list
                for (int i = 1; i < pointsList.size(); i++) {
                    canvas.drawCircle(pointsList.get(i).x, pointsList.get(i).y, 5, paint);
                    path.lineTo(pointsList.get(i).x, pointsList.get(i).y);
                }
                path.lineTo(first.x, first.y);
                canvas.drawPath(path, paint);
                break;
            case MotionEvent.ACTION_CANCEL:
                break;
            default:
                break;
        }
        return true;
    }

    final float[] getPointerCoords(MotionEvent e) {
        final int index = e.getActionIndex();
        final float[] coords = new float[]{e.getX(index), e.getY(index)};
        Matrix matrix = new Matrix();
        imageView.getImageMatrix().invert(matrix);
        matrix.postTranslate(imageView.getScrollX(), imageView.getScrollY());
        matrix.mapPoints(coords);
        return coords;
    }

    float findArea() {
        float area = 0;
        for (int i = 0; i < (rPointsList.size() - 1); i++) {
            area += rPointsList.get(i).x * rPointsList.get(i + 1).y - rPointsList.get(i).y * rPointsList.get(i + 1).x;
        }
        area += rPointsList.get(rPointsList.size() - 1).x * rPointsList.get(0).y - rPointsList.get(rPointsList.size() - 1).y * rPointsList.get(0).x;
        float sum = (float) Math.abs(area) / 2.0f;
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE); // the results will be higher than using the activity context object or the getWindowManager() shortcut
        wm.getDefaultDisplay().getMetrics(displayMetrics);

        float w = 25.4f * imageView.getWidth() / displayMetrics.xdpi;
        float h = 25.4f * imageView.getHeight() / displayMetrics.ydpi;
        float ratio = (297 * 210) / (w * h);

        sum = (6.4516f * sum) / (displayMetrics.xdpi * displayMetrics.ydpi);

        return sum * ratio;
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }
}


