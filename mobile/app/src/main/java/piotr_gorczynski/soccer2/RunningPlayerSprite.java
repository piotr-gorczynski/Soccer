package piotr_gorczynski.soccer2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import androidx.annotation.Nullable;

/**
 * Lazily loads and caches the running player sprite sheet that is also used on the menu screen.
 * Only the first row of frames is required for the in-game animation, which matches the default
 * menu animation.
 */
final class RunningPlayerSprite {

    private static final String TAG = "TAG_Soccer";

    private static final int FRAME_COUNT = 22;
    private static final int FRAME_WIDTH = 128;
    private static final int FRAME_HEIGHT = 128;
    static final long FRAME_DURATION_MS = 250L;

    private static Bitmap[] cachedFrames;

    private RunningPlayerSprite() {
        // no instances
    }

    static synchronized Bitmap[] getFrames(Context context) {
        if (cachedFrames != null) {
            return cachedFrames;
        }

        if (context == null) {
            Log.w(TAG, RunningPlayerSprite.class.getSimpleName() + ".getFrames: context is null");
            cachedFrames = new Bitmap[0];
            return cachedFrames;
        }

        int spriteSheetResId = context.getResources().getIdentifier(
                "spritesheet",
                "drawable",
                context.getPackageName()
        );
        if (spriteSheetResId == 0) {
            Log.w(TAG, RunningPlayerSprite.class.getSimpleName() + ".getFrames: spritesheet resource missing");
            cachedFrames = new Bitmap[0];
            return cachedFrames;
        }

        Bitmap spriteSheet;
        try {
            BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
            decodeOptions.inScaled = false;
            spriteSheet = BitmapFactory.decodeResource(context.getResources(), spriteSheetResId, decodeOptions);
        } catch (Exception e) {
            Log.e(TAG, RunningPlayerSprite.class.getSimpleName() + ".getFrames: Failed to decode sprite sheet", e);
            cachedFrames = new Bitmap[0];
            return cachedFrames;
        }

        if (spriteSheet == null) {
            Log.w(TAG, RunningPlayerSprite.class.getSimpleName() + ".getFrames: sprite sheet decoding returned null");
            cachedFrames = new Bitmap[0];
            return cachedFrames;
        }

        int sheetWidth = spriteSheet.getWidth();
        int sheetHeight = spriteSheet.getHeight();
        if (sheetWidth <= 0 || sheetHeight <= 0) {
            Log.w(TAG, RunningPlayerSprite.class.getSimpleName() + ".getFrames: invalid sheet dimensions");
            spriteSheet.recycle();
            cachedFrames = new Bitmap[0];
            return cachedFrames;
        }

        int framesAvailable = Math.min(FRAME_COUNT, sheetWidth / FRAME_WIDTH);
        if (framesAvailable <= 0) {
            Log.w(TAG, RunningPlayerSprite.class.getSimpleName() + ".getFrames: no frames available in sheet");
            spriteSheet.recycle();
            cachedFrames = new Bitmap[0];
            return cachedFrames;
        }

        int usableHeight = Math.min(FRAME_HEIGHT, sheetHeight);
        Bitmap[] frames = new Bitmap[framesAvailable];
        for (int i = 0; i < framesAvailable; i++) {
            int sourceX = i * FRAME_WIDTH;
            int frameWidth = Math.min(FRAME_WIDTH, sheetWidth - sourceX);
            if (frameWidth <= 0) {
                break;
            }
            try {
                frames[i] = Bitmap.createBitmap(
                        spriteSheet,
                        sourceX,
                        0,
                        frameWidth,
                        usableHeight
                );
            } catch (IllegalArgumentException e) {
                Log.e(TAG, RunningPlayerSprite.class.getSimpleName() + ".getFrames: failed to create frame " + i, e);
                recycleFrames(frames);
                frames = new Bitmap[0];
                break;
            }
        }

        spriteSheet.recycle();
        cachedFrames = frames;
        return cachedFrames;
    }

    private static void recycleFrames(@Nullable Bitmap[] frames) {
        if (frames == null) {
            return;
        }
        for (Bitmap frame : frames) {
            if (frame != null && !frame.isRecycled()) {
                frame.recycle();
            }
        }
    }
}
