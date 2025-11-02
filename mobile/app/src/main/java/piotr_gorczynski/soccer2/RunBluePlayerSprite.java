package piotr_gorczynski.soccer2;

import android.content.Context;
import android.graphics.Bitmap;

final class RunBluePlayerSprite {

    private static final int ROW_HEIGHT = 128;
    private static final int FIRST_ROW_START_Y = 1 + ROW_HEIGHT * 8;

    private RunBluePlayerSprite() {
        // no instances
    }

    static Bitmap[] getWestFrames(Context context) {
        return getFramesForRow(context, 1);
    }

    static Bitmap[] getWestNorthFrames(Context context) {
        return getFramesForRow(context, 2);
    }

    static Bitmap[] getNorthFrames(Context context) {
        return getFramesForRow(context, 3);
    }

    static Bitmap[] getEastNorthFrames(Context context) {
        return getFramesForRow(context, 4);
    }

    static Bitmap[] getEastFrames(Context context) {
        return getFramesForRow(context, 5);
    }

    static Bitmap[] getEastSouthFrames(Context context) {
        return getFramesForRow(context, 6);
    }

    static Bitmap[] getSouthFrames(Context context) {
        return getFramesForRow(context, 7);
    }

    static Bitmap[] getSouthWestFrames(Context context) {
        return getFramesForRow(context, 8);
    }

    private static Bitmap[] getFramesForRow(Context context, int rowIndex) {
        int startY = FIRST_ROW_START_Y + ROW_HEIGHT * (rowIndex - 1);
        return RunPlayerSprite.getFrames(context, startY);
    }
}
