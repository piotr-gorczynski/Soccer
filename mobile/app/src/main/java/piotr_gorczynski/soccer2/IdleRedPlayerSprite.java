package piotr_gorczynski.soccer2;

import android.content.Context;
import android.graphics.Bitmap;

final class IdleRedPlayerSprite {

    private static final int RED_START_Y = 129;  // Second row

    private IdleRedPlayerSprite() {
        // no instances
    }

    static Bitmap[] getFrames(Context context) {
        return IdlePlayerSprite.getFrames(context, RED_START_Y);
    }
}
