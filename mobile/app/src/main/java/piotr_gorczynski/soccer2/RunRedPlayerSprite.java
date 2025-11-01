package piotr_gorczynski.soccer2;

import android.content.Context;
import android.graphics.Bitmap;

final class RunRedPlayerSprite {

    private static final int RED_START_Y = 257;  // Third row

    private RunRedPlayerSprite() {
        // no instances
    }

    static Bitmap[] getFrames(Context context) {
        return RunPlayerSprite.getFrames(context, RED_START_Y);
    }
}
