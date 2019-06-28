package ntx.note.image;

import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import junit.framework.Assert;

import java.io.File;
import java.util.UUID;

import name.vbraun.view.write.GraphicsImage;
import ntx.note.ActivityBase;
import ntx.note.Global;
import ntx.note.NoteWriterActivity;
import ntx.note.data.Book;
import ntx.note.data.Bookshelf;
import ntx.note.data.Storage;
import ntx.note2.R;
import utility.HomeWatcher;

public class ImageActivity
        extends
        ActivityBase
        implements
        OnClickListener,
        OnCheckedChangeListener {

    private static final String TAG = "ImageActivity";
    private static final boolean useNtxPicker = true;//use NTX or 3rd party

    private View layout;
    private CropImageView preview;
    private Menu menu;
    private MenuItem menuAspect;
    private Button buttonSave, buttonErase, buttonCancel;
    private ImageButton buttonRotateLeft, buttonRotateRight, checkBoxCrop, buttonImagePick, btn_ok, btn_back, btn_crop_back;
    private CheckBox checkBoxAspect;
    private TextView noImageText, title_textView;

    private LinearLayout toolbar_layout, okbar_layout, bottom_layout;

    private boolean crop_isOpen = false;

    private Book book = null;
    private UUID uuid = null;

    private Bitmap bitmap;
    private File photoFile = null;

    // persistent data
    protected Uri uri = null;
    protected int rotation;
    protected boolean constrainAspect;

    public final static String EXTRA_UUID = "extra_uuid";
    public final static String EXTRA_ROTATION = "extra_rotation";
    public final static String EXTRA_CONSTRAIN_ASPECT = "extra_constrain_aspect";
    public final static String EXTRA_FILE_URI = "extra_file_uri";
    public final static String EXTRA_PHOTO_TMP_FILE = "extra_photo_tmp_file";
    public final static String BLANK = "blank";

    private Boolean newPhoto = false;
    HomeWatcher mHomeWatcher = new HomeWatcher(this);

    private Handler mHandler = new Handler();
    private Context mContext;
    
    private boolean bBlankObject = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        book = Bookshelf.getInstance().getCurrentBook();

        layout = getLayoutInflater().inflate(R.layout.image_editor, null);
        setContentView(layout);

        preview = (CropImageView) findViewById(R.id.image_editor_preview);
        buttonSave = (Button) findViewById(R.id.image_editor_save);
        buttonErase = (Button) findViewById(R.id.image_editor_erase);
        buttonCancel = (Button) findViewById(R.id.image_editor_cancel);
        buttonRotateLeft = (ImageButton) findViewById(R.id.image_editor_rotate_left);
        buttonRotateRight = (ImageButton) findViewById(R.id.image_editor_rotate_right);
        checkBoxCrop = (ImageButton) findViewById(R.id.image_editor_check_crop);
        checkBoxAspect = (CheckBox) findViewById(R.id.image_editor_aspect);
        noImageText = (TextView) findViewById(R.id.image_editor_no_image_text);
        buttonImagePick = (ImageButton) findViewById(R.id.image_editor_image_pick);
        toolbar_layout = findViewById(R.id.toolbar_layout);
        okbar_layout = findViewById(R.id.okbar_layout);
        bottom_layout = findViewById(R.id.bottom_layout);
        btn_ok = findViewById(R.id.btn_ok);
        btn_back = findViewById(R.id.btn_back);
        btn_crop_back = findViewById(R.id.btn_crop_back);
        title_textView = findViewById(R.id.title_textView);

        buttonSave.setOnClickListener(this);
        buttonErase.setOnClickListener(this);
        buttonCancel.setOnClickListener(this);
        buttonRotateLeft.setOnClickListener(this);
        buttonRotateRight.setOnClickListener(this);
        buttonImagePick.setOnClickListener(this);
        checkBoxCrop.setOnClickListener(this);
        btn_ok.setOnClickListener(this);
        btn_back.setOnClickListener(this);
        btn_crop_back.setOnClickListener(this);

        Intent intent = getIntent();
        if (savedInstanceState != null) {
            restoreFrom(savedInstanceState);
        } else {
            restoreFrom(intent.getExtras());
        }

        //////////////////////////////////////////////////
        mHomeWatcher.setOnHomePressedListener(new HomeWatcher.OnHomePressedListener() {
            @Override
            public void onHomePressed() {

                mHomeWatcher.stopWatch();

                if (uri == null) {
                    for (GraphicsImage image : book.currentPage().images) {
                        if (image.getUuid().equals(uuid)) {
                            book.currentPage().images.remove(image);
                            break;
                        }
                    }

                } else {
                    DialogFragment newFragment =
                            DialogSave.newInstance(uri, getBookImageFile(),
                                    rotation, preview.getCropRect(), false);
                    newFragment.show(getFragmentManager(), "saveImage");

                    for (GraphicsImage image : book.currentPage().images) {
                        if (image.getUuid().equals(uuid)) {
                            String name = uri.getPath();
                            image.setFile(name, constrainAspect);
                            break;
                        }
                    }
                }
                //set page modified
                book.currentPage().setModified(true);
                book.save();
                finish();

            }

            @Override
            public void onHomeLongPressed() {
            }
        });
        mHomeWatcher.startWatch();
        //////////////////////////////////////////////////
    }

    private void restoreFrom(Bundle bundle) {
        if (bundle == null) return;
        String uuidStr = bundle.getString(EXTRA_UUID);
        if (uuidStr != null)
            uuid = UUID.fromString(uuidStr);
        else {
            Log.e(TAG, "no image uuid set");
            return;
        }
        constrainAspect = bundle.getBoolean(EXTRA_CONSTRAIN_ASPECT, true);
        rotation = bundle.getInt(EXTRA_ROTATION, 0);
        String uriStr = bundle.getString(EXTRA_FILE_URI);
        if (uriStr != null) {
            uri = Uri.parse(uriStr);
            loadBitmap();
            newPhoto = false;
        } else {
            newPhoto = true;
        }

        if (bundle.containsKey(EXTRA_PHOTO_TMP_FILE))
            photoFile = new File(bundle.getString(EXTRA_PHOTO_TMP_FILE));
    }

    private Bundle saveTo(Bundle bundle) {
        Log.d(TAG, "saveTo");
        if (uri != null)
            bundle.putString(EXTRA_FILE_URI, uri.toString());
        bundle.putString(EXTRA_UUID, uuid.toString());
        bundle.putInt(EXTRA_ROTATION, rotation);
        bundle.putBoolean(EXTRA_CONSTRAIN_ASPECT, constrainAspect);
        if (photoFile != null)
            bundle.putString(EXTRA_PHOTO_TMP_FILE, photoFile.getAbsolutePath());
        return bundle;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveTo(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
/*
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.image_editor, menu);
        this.menu = menu;
        menuAspect = menu.findItem(R.id.image_editor_aspect);
        menuAspect.setChecked(constrainAspect);
*/
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        //  bBlankObject=false;
        Intent intent = getIntent();
        int control = intent.getIntExtra(BLANK, 99);
        if (bBlankObject) {
            if (control == 1) {
                Global.openWaitDialog(this);
                Intent intentA = new Intent(ImageActivity.this, ImagePickerActivity.class);
                intentA.putExtra(BLANK, 1);
                startActivityForResult(intentA, REQUEST_CODE_PICK_IMAGE);
            }
            bBlankObject = false;
            noImageText.setVisibility(View.GONE);

            return;
        }

        if (control == 2 || bBlankObject == false) {
            checkBoxAspect.setChecked(constrainAspect);
            checkBoxAspect.setOnCheckedChangeListener(this);
            if (menuAspect != null)
                menuAspect.setChecked(constrainAspect);
            showImageTools(uri != null);

            //show "Erase" or "Cancel"
            if ((uri != null) && (newPhoto == false)) {
                buttonErase.setEnabled(false);
                buttonErase.setVisibility(View.GONE);
                buttonCancel.setEnabled(true);
                buttonCancel.setVisibility(View.VISIBLE);
            } else {
                buttonErase.setEnabled(true);
                buttonErase.setVisibility(View.VISIBLE);
                buttonCancel.setEnabled(false);
                buttonCancel.setVisibility(View.GONE);

            }
        }
    }

    private void showImageTools(boolean show) {
        okbar_layout.setVisibility(show ? View.GONE : View.VISIBLE);
        btn_crop_back.setVisibility(show ? View.GONE : View.VISIBLE);
        toolbar_layout.setVisibility(show ? View.VISIBLE : View.GONE);
        btn_back.setVisibility(show ? View.VISIBLE : View.GONE);
        bottom_layout.setVisibility(show ? View.VISIBLE : View.GONE);
//        checkBoxAspect.setVisibility(show ? View.VISIBLE : View.GONE);
        title_textView.setText(show ? getResources().getString(R.string.image_editor_title) : getResources().getString(R.string.menu_image_cropped_condensed));
    }

    @Override
    protected void onPause() {
        checkBoxAspect.setOnCheckedChangeListener(null);
        Global.closeWaitDialog(this);
        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.image_editor_pick:

                //////////////////////////////////
                if (useNtxPicker) {
                    Global.openWaitDialog(this);
                    intent = new Intent(ImageActivity.this, ImagePickerActivity.class);
                    startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
                    ///////////////////////////////////
                } else {
                    intent = new Intent();
                    intent.setAction(Intent.ACTION_PICK);
                    intent.setData(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
                }

                return true;
                /*
                case R.id.image_editor_photo:
                intent = new Intent();
                intent.setAction("android.media.action.IMAGE_CAPTURE");
                photoFile = new File(Environment.getExternalStorageDirectory(),
                        uuid.toString() + ".jpg");
                photoFile.deleteOnExit();
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                startActivityForResult(intent, REQUEST_CODE_TAKE_PHOTO);
                return true;*/
            case R.id.image_editor_aspect:
                constrainAspect = !constrainAspect;
                menuAspect.setChecked(constrainAspect);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private final static int REQUEST_CODE_PICK_IMAGE = 1;
    private final static int REQUEST_CODE_TAKE_PHOTO = 2;

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        switch (requestCode) {
            case REQUEST_CODE_TAKE_PHOTO:
                if (resultCode != RESULT_OK)
                    break;
                if (photoFile == null || !photoFile.exists()) {
                    photoFile = null;
                    Toast.makeText(this, R.string.image_editor_err_no_photo,
                            Toast.LENGTH_LONG).show();
                    Log.e(TAG, "no photo");
                    return;
                }
                loadBitmap(Uri.fromFile(photoFile), 0);
                photoFile = null;
                break;
            case REQUEST_CODE_PICK_IMAGE:
                if (resultCode != RESULT_OK) {
                    mHomeWatcher.stopWatch();
                    intent = new Intent();
                    intent.putExtra(EXTRA_UUID, uuid.toString());
                    setResult(RESULT_OK, intent);
                    finish();
                    break;
                }
                Log.d(TAG, "Selected image is NULL!");
                Uri selectedImage = intent.getData();
                if (selectedImage == null) {
                    Log.e(TAG, "Selected image is NULL!");
                    return;
                } else {
                    Log.d(TAG, "Selected image: " + selectedImage);
                }

                //////////////////////////////////
                if (useNtxPicker) {
                    loadBitmap(selectedImage, 0);
                }
                ////////////////////////////////////
                else {
                    if (selectedImage.toString().startsWith(
                            "content://com.android.gallery3d.provider")) {
                        // some devices/OS versions return an URI of com.android instead
                        // of com.google.android
                        String str = selectedImage.toString()
                                .replace("com.android.gallery3d", "com.google.android.gallery3d");
                        selectedImage = Uri.parse(str);
                        Log.d(TAG, "Rewrote to: " + selectedImage);
                    }
                    boolean picasaImage = selectedImage.toString().startsWith(
                            "content://com.google.android.gallery3d");

                    final String[] filePathColumn = {MediaColumns.DATA,
                            MediaColumns.DISPLAY_NAME};
                    Cursor cursor = getContentResolver().query(selectedImage,
                            filePathColumn, null, null, null);
                    if (cursor == null) {
                        Toast.makeText(this, R.string.image_editor_err_no_such_image, Toast.LENGTH_LONG)
                                .show();
                        Log.e(TAG, "cursor is null");
                        return;
                    }

                    cursor.moveToFirst();
                    if (picasaImage) {
                        int columnIndex = cursor.getColumnIndex(MediaColumns.DISPLAY_NAME);
                        if (columnIndex == -1) {
                            Log.e(TAG, "no DISPLAY_NAME column");
                            return;
                        }
                        downloadImage(selectedImage);
                    } else {
                        int columnIndex = cursor.getColumnIndex(MediaColumns.DATA);
                        if (columnIndex == -1) {
                            Log.e(TAG, "no DATA column");
                            return;
                        }
                        String name = cursor.getString(columnIndex);
                        File file = new File(name);
                        if (!file.exists() || !file.canRead()) {
                            Toast.makeText(this, R.string.image_editor_err_permissions,
                                    Toast.LENGTH_LONG).show();
                            Log.e(TAG, "image file not readable");
                            return;
                        }
                        loadBitmap(Uri.fromFile(file), 0);
                    }
                    cursor.close();
                }
                break;
            default:
                Log.d(TAG, "Selected image is NULL!");
                Assert.fail("Unknown request code");
        }
    }


    protected void loadBitmap(Uri sourceUri, int rotation) {
        this.uri = sourceUri;
        this.rotation = rotation;
        loadBitmap();
    }

    private void loadBitmap() {
        if (uri == null) return;
        Log.i(TAG, "showing " + uri);
        try {
            bitmap = Util.getBitmap(uri);
            bitmap = Util.rotate(bitmap, rotation);
            preview.setImageBitmapResetBase(bitmap, true);
        } catch (OutOfMemoryError e) {
            onSaveFinished(null, false, true);
        }
    }

    private File getBookImageFile() {
        Storage storage = Storage.getInstance();
        File dir = storage.getBookDirectory(book);
        return new File(dir, getImageFileName());
    }

    private void downloadImage(final Uri uri) {
        DialogFragment newFragment = DialogPicasaDownload.newInstance(uri, getCacheFile());
        newFragment.show(getFragmentManager(), "downloadImage");
    }

    private String getImageFileName() {
        return Util.getImageFileName(uuid);
    }

    private File getCacheFile() {
        String randomFileName = getImageFileName();
        File file = new File(getCacheDir(), randomFileName);
        file.deleteOnExit();
        return file;
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.image_editor_erase:
                mHomeWatcher.stopWatch();
                intent = new Intent();
                intent.putExtra(EXTRA_UUID, uuid.toString());
                setResult(RESULT_OK, intent);
                finish();
                break;
            case R.id.image_editor_cancel:
                mHomeWatcher.stopWatch();
                intent = new Intent();
                setResult(RESULT_CANCELED, intent);
                finish();
                break;
            case R.id.image_editor_save:
                stopWatchPic(false);
                break;
            case R.id.image_editor_rotate_right:
                addToRotation(90);
                break;
            case R.id.image_editor_rotate_left:
                addToRotation(270);
                break;
            case R.id.image_editor_image_pick:
                //////////////////////////////////
                if (useNtxPicker) {
                    Global.openWaitDialog(this);
                    intent = new Intent(ImageActivity.this, ImagePickerActivity.class);
                    startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
                    ///////////////////////////////////
                } else {
                    intent = new Intent();
                    intent.setAction(Intent.ACTION_PICK);
                    intent.setData(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
                }
                break;

            case R.id.image_editor_check_crop:
                makeHighlight();
                showImageTools(false);
                break;

            case R.id.btn_ok:
                stopWatchPic(true);
                break;

            case R.id.btn_back:
                setBtn_back();
                break;

            case R.id.btn_crop_back:
                preview.setHighlight(null);
                showImageTools(true);
                break;
        }
    }

    private void addToRotation(int degrees) {
        rotation = (rotation + degrees) % 360;
        bitmap = Util.rotate(bitmap, degrees);
        preview.setImageBitmapResetBase(bitmap, true);
        preview.setHighlight(null);
    }

    @Override
    public void onCheckedChanged(CompoundButton button, boolean isChecked) {

        if (button == checkBoxAspect) {
            constrainAspect = !constrainAspect;
            checkBoxAspect.setChecked(constrainAspect);
        }
    }

    private void makeHighlight() {
        if (bitmap == null) return;
        HighlightView hv = new HighlightView(preview);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Rect imageRect = new Rect(0, 0, width, height);

        // make the default size about 4/5 of the width or height
        int cropWidth = Math.min(width, height) * 4 / 5;
        int cropHeight = cropWidth;
        int x = (width - cropWidth) / 2;
        int y = (height - cropHeight) / 2;
        RectF cropRect = new RectF(x, y, x + cropWidth, y + cropHeight);
        hv.setup(preview.getImageMatrix(), imageRect, cropRect, false, false);
        hv.setFocus(true);
        preview.setHighlight(hv);
    }

    @Override
    public void onBackPressed() {
        setBtn_back();
    }

    private void setBtn_back() {
        mHomeWatcher.stopWatch();
        Intent intent = new Intent();
        if (uri != null && getIntent().getIntExtra(BLANK, 99) != 1) {
            setResult(RESULT_CANCELED, intent);
            finish();
        } else {
            intent.putExtra(EXTRA_UUID, uuid.toString());
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    /**
     * Callback from the save progress dialog
     */
    protected void onSaveFinished(File file, Boolean isCrop, Boolean oom) {

        if (isCrop) {
            rotation = 0;
            resetPhoto(Uri.fromFile(file));
            uri = Uri.fromFile(file);
            preview.setHighlight(null);
            showImageTools(true);
        } else if (oom) {
            Intent intent = new Intent();
            intent.putExtra(EXTRA_UUID, uuid.toString());
            setResult(NoteWriterActivity.REQUEST_PICK_IMAGE_OOM, intent);
            finish();
        } else {
            Intent intent = new Intent();
            intent.putExtra(EXTRA_FILE_URI, Uri.fromFile(file).toString());
            intent.putExtra(EXTRA_UUID, uuid.toString());
            intent.putExtra(EXTRA_CONSTRAIN_ASPECT, constrainAspect);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    private void stopWatchPic(boolean isCrop) {
        try{
            mHomeWatcher.stopWatch();
        } catch (IllegalArgumentException i) {

        }
        DialogFragment newFragment =
                DialogSave.newInstance(uri, getBookImageFile(),
                        rotation, preview.getCropRect(), isCrop);
        newFragment.show(getFragmentManager(), "saveImage");
    }

    private void resetPhoto(Uri uri) {
        bitmap = Util.getBitmap(uri);
        bitmap = Util.rotate(bitmap, rotation);
        preview.setImageBitmapResetBase(bitmap, true);
    }

}
