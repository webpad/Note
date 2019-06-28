package ntx.note.image;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.util.Log;

import ntx.note.Global;
import ntx.note.NoteWriterActivity;

public class ThreadSave extends ThreadBase {
	private final static String TAG = "ThreadSave";
	
	private final File input, output;
	private final int rotation;
	private final Rect crop;
	private DialogSave dialogSave;

	protected ThreadSave(File input, File output, int rotation, Rect crop,DialogSave dialogSave) {
		super(output);
		this.input = input;
		this.output = output;
		this.rotation = rotation;
		this.crop = crop;
		this.dialogSave = dialogSave;
		Log.d(TAG, "Saving "+input.getAbsolutePath()+" -> "+output.getAbsolutePath());
	}

	@Override
	protected void worker() {
		if (input.equals(output) && rotation == 0 && crop == null) return;
		Bitmap bitmap = Util.getBitmap(Uri.fromFile(new File(input.getPath())));
		bitmap = convertBitmap2Jpg(bitmap);
		if (isInterrupted()) return;

		bitmap = Util.rotateAndCrop(bitmap, rotation, crop);
		if (isInterrupted()) return;

		OutputStream out = openOutput();
        try {
            bitmap.compress(Bitmap.CompressFormat.JPEG, Global.COMPRESS_QUALITY, out);
        } catch (Exception e) {
			dialogSave.onFinish(null,true);
        }

        closeOutput(out);

        try {
			bitmap.recycle();
		}catch (NullPointerException e){

		}

        Log.d(TAG, "Saved to "+output.getPath());
	}

	private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
		int originalWidth = options.outWidth;
		int originalHeight = options.outHeight;
		int inSampleSize = 1;
		if (originalHeight > reqHeight || originalWidth > reqHeight){
			int halfHeight = originalHeight / 2;
			int halfWidth = originalWidth / 2;
			while ((halfWidth / inSampleSize) >= reqHeight && (halfHeight /inSampleSize)>=reqWidth){
				inSampleSize *= 2;
			}
		}
		return inSampleSize;
	}

	public Bitmap convertBitmap2Jpg(Bitmap bitmap) {

		try {
			Bitmap outB = bitmap.copy(Bitmap.Config.ARGB_8888, true);
			Canvas canvas = new Canvas(outB);
			canvas.drawColor(Color.WHITE);
			canvas.drawBitmap(bitmap, 0, 0, null);
			return outB;
		}catch (OutOfMemoryError error){
			dialogSave.onFinish(null, true);
		}
		return null;
	}

    private OutputStream openOutput() {
        try {
			return new FileOutputStream(output);
		} catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage());
		}
        return null;
    }
    
    private void closeOutput(OutputStream outputStream) {
    	try {
    		outputStream.close();
    	} catch (IOException e) {
    		Log.e(TAG, e.getMessage());
    	}	
    }

}
