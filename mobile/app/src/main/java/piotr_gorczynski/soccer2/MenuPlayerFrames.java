package piotr_gorczynski.soccer2;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.util.function.BooleanSupplier;

/** Bounded, cancellable bitmap loading for the optional menu decoration. */
final class MenuPlayerFrames {
    private static final int FRAME_SIZE = 64; // Original 128px frames decoded at half resolution.
    private static final int COLUMNS = 22;
    private static final int ROWS = 16;

    interface Cropper { Bitmap crop(Bitmap sheet, int x, int y, int size); }

    static Bitmap[][] load(Resources resources, BooleanSupplier cancelled) {
        if (cancelled.getAsBoolean()) return null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        options.inSampleSize = 2;
        Bitmap sheet = BitmapFactory.decodeResource(resources, R.drawable.spritesheet_run, options);
        if (sheet == null) return null;
        return split(sheet, cancelled, (bitmap, x, y, size) -> Bitmap.createBitmap(bitmap, x, y, size, size));
    }

    // Takes ownership of sheet. Partial frames and sheet are released even if allocation fails.
    static Bitmap[][] split(Bitmap sheet, BooleanSupplier cancelled, Cropper cropper) {
        Bitmap[][] frames = null;
        boolean complete = false;
        try {
            if (sheet.getWidth() != COLUMNS * FRAME_SIZE || sheet.getHeight() != ROWS * FRAME_SIZE)
                throw new IllegalArgumentException("Unexpected menu sprite dimensions");
            frames = new Bitmap[ROWS][COLUMNS];
            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLUMNS; col++) {
                    if (cancelled.getAsBoolean()) return null;
                    frames[row][col] = cropper.crop(sheet, col * FRAME_SIZE, row * FRAME_SIZE, FRAME_SIZE);
                }
            }
            complete = true;
            return frames;
        } finally {
            sheet.recycle();
            if (!complete) recycle(frames);
        }
    }

    static void recycle(Bitmap[][] frames) {
        if (frames == null) return;
        for (Bitmap[] row : frames) {
            if (row == null) continue;
            for (Bitmap frame : row) if (frame != null && !frame.isRecycled()) frame.recycle();
        }
    }
}
