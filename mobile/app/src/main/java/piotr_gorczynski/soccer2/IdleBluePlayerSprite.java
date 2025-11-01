package piotr_gorczynski.soccer2;

import android.content.Context;
import android.graphics.Bitmap;

final class IdleBluePlayerSprite {

    private static final int BLUE_START_Y = 257;  // Third row

    private IdleBluePlayerSprite() {
        // no instances
    }

    static Bitmap[] getFrames(Context context) {
        return IdlePlayerSprite.getFrames(context, BLUE_START_Y);
    }
}
