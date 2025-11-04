package piotr_gorczynski.soccer2;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads running animation frames from the shared run sprite sheet.
 */
final class RunPlayerSprite {

    private static final String TAG = "TAG_Soccer";

    static final long FRAME_DURATION_MS = 350L;
    static final int FRAME_COUNT = 22;

    private static final int FRAME_WIDTH = 128;
    private static final int FRAME_HEIGHT = 128;
    private static final int START_X = 0;
    private static final int COLUMNS = FRAME_COUNT;
    private static final int ROWS = 1;

    private static final Map<Integer, Bitmap[]> framesCache = new HashMap<>();
    private static final Map<Integer, SpriteSheetMetadata> metadataCache = new HashMap<>();

    private RunPlayerSprite() {
        // no instances
    }

    static synchronized Bitmap[] getFrames(Context context, int startY) {
        Bitmap[] cached = framesCache.get(startY);
        if (cached != null) {
            return cached;
        }

        if (context == null) {
            Log.w(TAG, RunPlayerSprite.class.getSimpleName() + ".getFrames: context is null");
            Bitmap[] emptyFrames = new Bitmap[0];
            framesCache.put(startY, emptyFrames);
            return emptyFrames;
        }

        SpriteSheetMetadata spriteMetadata = getMetadata(context, startY);
        if (spriteMetadata.getResourceId() == 0) {
            Log.w(TAG, RunPlayerSprite.class.getSimpleName() + ".getFrames: spritesheet_run resource missing");
            Bitmap[] emptyFrames = new Bitmap[0];
            framesCache.put(startY, emptyFrames);
            return emptyFrames;
        }

        Bitmap[] frames = SpriteSheetLoader.loadFrames(context, spriteMetadata);
        if (frames == null) {
            frames = new Bitmap[0];
        }
        framesCache.put(startY, frames);
        return frames;
    }

    private static SpriteSheetMetadata getMetadata(Context context, int startY) {
        SpriteSheetMetadata cached = metadataCache.get(startY);
        if (cached != null) {
            return cached;
        }

        int spriteSheetResId = context.getResources().getIdentifier(
                "spritesheet_run",
                "drawable",
                context.getPackageName()
        );

        SpriteSheetMetadata metadata = new SpriteSheetMetadata.Builder(spriteSheetResId)
                .frameWidth(FRAME_WIDTH)
                .frameHeight(FRAME_HEIGHT)
                .frameCount(FRAME_COUNT)
                .startX(START_X)
                .startY(startY)
                .columns(COLUMNS)
                .rows(ROWS)
                .frameDurationMs(FRAME_DURATION_MS)
                .build();

        metadataCache.put(startY, metadata);
        return metadata;
    }
}
