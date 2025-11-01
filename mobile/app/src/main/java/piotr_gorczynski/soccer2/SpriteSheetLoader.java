package piotr_gorczynski.soccer2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import androidx.annotation.Nullable;

/**
 * Loads sprite sheet frames based on metadata configuration.
 * This class handles the extraction of individual frames from a sprite sheet
 * using the provided metadata.
 */
public final class SpriteSheetLoader {

    private static final String TAG = "TAG_Soccer";

    private SpriteSheetLoader() {
        // Utility class, no instances
    }

    /**
     * Loads frames from a sprite sheet based on the provided metadata.
     * 
     * @param context Android context for resource access
     * @param metadata Sprite sheet metadata describing frame layout
     * @return Array of bitmap frames, or empty array on error
     */
    public static Bitmap[] loadFrames(Context context, SpriteSheetMetadata metadata) {
        if (context == null) {
            Log.w(TAG, "SpriteSheetLoader.loadFrames: context is null");
            return new Bitmap[0];
        }
        if (metadata == null) {
            Log.w(TAG, "SpriteSheetLoader.loadFrames: metadata is null");
            return new Bitmap[0];
        }

        int resourceId = metadata.getResourceId();
        if (resourceId == 0) {
            Log.w(TAG, "SpriteSheetLoader.loadFrames: invalid resource ID");
            return new Bitmap[0];
        }

        Bitmap spriteSheet;
        try {
            BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
            decodeOptions.inScaled = false;
            spriteSheet = BitmapFactory.decodeResource(context.getResources(), resourceId, decodeOptions);
        } catch (Exception e) {
            Log.e(TAG, "SpriteSheetLoader.loadFrames: Failed to decode sprite sheet", e);
            return new Bitmap[0];
        }

        if (spriteSheet == null) {
            Log.w(TAG, "SpriteSheetLoader.loadFrames: sprite sheet decoding returned null");
            return new Bitmap[0];
        }

        int sheetWidth = spriteSheet.getWidth();
        int sheetHeight = spriteSheet.getHeight();
        if (sheetWidth <= 0 || sheetHeight <= 0) {
            Log.w(TAG, "SpriteSheetLoader.loadFrames: invalid sheet dimensions");
            spriteSheet.recycle();
            return new Bitmap[0];
        }

        int frameWidth = metadata.getFrameWidth();
        int frameHeight = metadata.getFrameHeight();
        int frameCount = metadata.getFrameCount();
        int startX = metadata.getStartX();
        int startY = metadata.getStartY();
        int columns = metadata.getColumns();

        // Calculate how many frames we can actually extract
        int maxFramesInRow = (sheetWidth - startX) / frameWidth;
        int maxFramesInCol = (sheetHeight - startY) / frameHeight;
        // Respect the metadata's column layout and frame count
        int effectiveColumns = Math.min(columns, maxFramesInRow);
        int maxFrames = Math.min(frameCount, effectiveColumns * maxFramesInCol);

        if (maxFrames <= 0) {
            Log.w(TAG, "SpriteSheetLoader.loadFrames: no frames available in sheet");
            spriteSheet.recycle();
            return new Bitmap[0];
        }

        Bitmap[] frames = new Bitmap[maxFrames];
        int frameIndex = 0;

        // Extract frames row by row, left to right
        for (int row = 0; row < maxFramesInCol && frameIndex < maxFrames; row++) {
            for (int col = 0; col < effectiveColumns && frameIndex < maxFrames; col++) {
                int sourceX = startX + col * frameWidth;
                int sourceY = startY + row * frameHeight;

                // Check bounds
                if (sourceX + frameWidth > sheetWidth || sourceY + frameHeight > sheetHeight) {
                    Log.w(TAG, "SpriteSheetLoader.loadFrames: frame " + frameIndex + " out of bounds");
                    break;
                }

                try {
                    frames[frameIndex] = Bitmap.createBitmap(
                            spriteSheet,
                            sourceX,
                            sourceY,
                            frameWidth,
                            frameHeight
                    );
                    frameIndex++;
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "SpriteSheetLoader.loadFrames: failed to create frame " + frameIndex, e);
                    recycleFrames(frames);
                    spriteSheet.recycle();
                    return new Bitmap[0];
                }
            }
        }

        spriteSheet.recycle();
        return frames;
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
