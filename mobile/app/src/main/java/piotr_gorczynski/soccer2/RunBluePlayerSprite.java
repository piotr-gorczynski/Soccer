package piotr_gorczynski.soccer2;

import android.content.Context;
import android.graphics.Bitmap;

final class RunBluePlayerSprite {

    private static final int ROW_HEIGHT = 128;
    // The blue sprites occupy the lower half of the shared run sheet. The first
    // blue row starts immediately after the red rows (8 * ROW_HEIGHT pixels). We
    // intentionally avoid adding an extra offset here because doing so would
    // push the final south-west row one pixel beyond the sheet's height (2048px),
    // causing BitmapFactory to return an empty frame array. By using exactly
    // ROW_HEIGHT * 8 we keep every blue frame—especially the south-west sequence
    // used when moving (-1, -1)—within bounds so the animation never disappears.
    private static final int FIRST_ROW_START_Y = ROW_HEIGHT * 8;

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
