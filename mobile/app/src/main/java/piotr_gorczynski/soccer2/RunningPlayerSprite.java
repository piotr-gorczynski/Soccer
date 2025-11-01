package piotr_gorczynski.soccer2;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

/**
 * Lazily loads and caches the running player sprite sheet that is also used on the menu screen.
 * Only the second row of frames is required for the in-game animation, which matches the default
 * menu animation facing forward.
 */
final class RunningPlayerSprite {

    private static final String TAG = "TAG_Soccer";

    static final long FRAME_DURATION_MS = 250L;

    private static Bitmap[] cachedFrames;
    private static SpriteSheetMetadata metadata;

    private RunningPlayerSprite() {
        // no instances
    }

    /**
     * Get metadata for the idle sprite sheet (second row of frames).
     */
    private static SpriteSheetMetadata getMetadata(Context context) {
        if (metadata == null) {
            int spriteSheetResId = context.getResources().getIdentifier(
                    "spritesheet_idle",
                    "drawable",
                    context.getPackageName()
            );
            metadata = new SpriteSheetMetadata.Builder(spriteSheetResId)
                    .frameWidth(128)
                    .frameHeight(128)
                    .frameCount(35)
                    .startX(0)
                    .startY(129)  // Second row
                    .columns(35)
                    .rows(1)
                    .frameDurationMs(FRAME_DURATION_MS)
                    .build();
        }
        return metadata;
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

        SpriteSheetMetadata spriteMetadata = getMetadata(context);
        if (spriteMetadata.getResourceId() == 0) {
            Log.w(TAG, RunningPlayerSprite.class.getSimpleName() + ".getFrames: spritesheet_idle resource missing");
            cachedFrames = new Bitmap[0];
            return cachedFrames;
        }

        cachedFrames = SpriteSheetLoader.loadFrames(context, spriteMetadata);
        return cachedFrames;
    }
}
