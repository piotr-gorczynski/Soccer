package piotr_gorczynski.soccer2;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Lazily loads and caches frames from the idle player sprite sheet.
 *
 * <p>The metadata-based system makes it easy to switch between different rows of the same
 * sprite sheet. Each row can be used for a differently coloured player.</p>
 */
final class IdlePlayerSprite {

    private static final String TAG = "TAG_Soccer";

    static final long FRAME_DURATION_MS = 250L;

    private static final int FRAME_WIDTH = 128;
    private static final int FRAME_HEIGHT = 128;
    private static final int FRAME_COUNT = 35;
    private static final int START_X = 0;
    private static final int COLUMNS = 35;
    private static final int ROWS = 1;

    private static final Map<Integer, Bitmap[]> framesCache = new HashMap<>();
    private static final Map<Integer, SpriteSheetMetadata> metadataCache = new HashMap<>();

    private IdlePlayerSprite() {
        // no instances
    }

    static synchronized Bitmap[] getFrames(Context context, int startY) {
        Bitmap[] cached = framesCache.get(startY);
        if (cached != null) {
            return cached;
        }

        if (context == null) {
            Log.w(TAG, IdlePlayerSprite.class.getSimpleName() + ".getFrames: context is null");
            Bitmap[] emptyFrames = new Bitmap[0];
            framesCache.put(startY, emptyFrames);
            return emptyFrames;
        }

        SpriteSheetMetadata spriteMetadata = getMetadata(context, startY);
        if (spriteMetadata.getResourceId() == 0) {
            Log.w(TAG, IdlePlayerSprite.class.getSimpleName() + ".getFrames: spritesheet_idle resource missing");
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
                "spritesheet_idle",
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
