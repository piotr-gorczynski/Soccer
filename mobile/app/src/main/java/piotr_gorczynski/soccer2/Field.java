package piotr_gorczynski.soccer2;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.os.SystemClock;
import androidx.core.content.ContextCompat;

import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Log;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

public class Field {

    private final int intFieldWidth;
    private final int intFieldHeight;//,intBallX, intBallY;
    private final float flFieldMarginX;
    private final float flFieldMarginY;
    private final float flDots;
    private final float flText;
    private final float flLinesWidth;
    private final float flSpriteSize;
    private final Paint pField;
    private final Paint pFieldBorder;
    private final Paint pDots;
    private final Paint pPlayer0;
    private final Paint pPlayer1;
    private final Paint pHintText;
    private final Rect rField;
    private final Rect rText;
    private final Bitmap ballBitmap;
    private static final Bitmap[] EMPTY_BITMAP_ARRAY = new Bitmap[0];

    private final Bitmap[] idleRedPlayerFrames;
    private final Bitmap[] runRedPlayerWestFrames;
    private final Bitmap[] runRedPlayerWestNorthFrames;
    private final Bitmap[] runRedPlayerNorthFrames;
    private final Bitmap[] runRedPlayerEastNorthFrames;
    private final Bitmap[] runRedPlayerEastFrames;
    private final Bitmap[] runRedPlayerEastSouthFrames;
    private final Bitmap[] runRedPlayerSouthFrames;
    private final Bitmap[] runRedPlayerSouthWestFrames;
    private final Bitmap[] runBluePlayerWestFrames;
    private final Bitmap[] runBluePlayerWestNorthFrames;
    private final Bitmap[] runBluePlayerNorthFrames;
    private final Bitmap[] runBluePlayerEastNorthFrames;
    private final Bitmap[] runBluePlayerEastFrames;
    private final Bitmap[] runBluePlayerEastSouthFrames;
    private final Bitmap[] runBluePlayerSouthFrames;
    private final Bitmap[] runBluePlayerSouthWestFrames;
    private final Bitmap[] idleBluePlayerFrames;
    private Bitmap[] activeRunRedPlayerFrames;
    private Bitmap[] activeRunBluePlayerFrames;
    private final String sPlayer0;
    private final String sPlayer1;
    private final int gameType;
    private final Context context;
    final ArrayList<MoveTo> possibleMoves;//= new ArrayList<MoveTo>();
    final ArrayList<MoveTo> Moves;//= new ArrayList<MoveTo>();
    private boolean isFlipped=false;

    private long remainingTime0, remainingTime1;

    private Long turnStartTime;
    private final boolean showIdlePlayerSprite;
    private int idlePlayerFrameIndex = 0;
    private long idlePlayerLastFrameTime = 0L;
    private boolean runAnimationActive = false;
    private int runPlayerFrameIndex = 0;
    private long runPlayerLastFrameTime = 0L;
    private float runStartGridX = 0f;
    private float runStartGridY = 0f;
    private float runDirectionX = 0f;
    private float runDirectionY = 0f;
    private float runTargetGridX = 0f;
    private float runTargetGridY = 0f;
    private float runTotalDistance = 0f;
    private int runFrameLimit = RunPlayerSprite.FRAME_COUNT;
    private boolean runStartRedCloser = false;
    private boolean runTargetRedCloser = false;
    private boolean runStartBlueCloser = false;
    private boolean runTargetBlueCloser = false;
    private int runMovingPlayer = -1;
    private int runRedDelayFrames = 0;
    private int runBlueDelayFrames = 0;

    private static final int RUN_FRAME_COUNT = RunPlayerSprite.FRAME_COUNT;
    private static final float RUN_FRAME_STEP_DISTANCE = RUN_FRAME_COUNT > 0
            ? (float) (Math.sqrt(2.0) / RUN_FRAME_COUNT)
            : 0f;
    private static final float ACTIVE_SPRITE_PROXIMITY_RATIO = 0.6f;
    private static final int RUN_DELAY_CYCLES = 2;

    public Field(Context current, ArrayList<MoveTo> argMoves, ArrayList<MoveTo> argPossibleMoves, int argGameType, String player0Name, String player1Name, int localPlayerIndex, boolean animationsEnabled) {

        // simpler log—no reflection, no nulls
        Log.d("TAG_Soccer", getClass().getSimpleName()
                + ".<init>: Started, received argMoves.size=" + argMoves.size()
                + ", argPossibleMoves.size=" + argPossibleMoves.size()
                + ", argGameType=" + argGameType
                + ", player0Name=" + player0Name
                + ", player1Name=" + player1Name
                + ", localPlayerIndex=" + localPlayerIndex
                + ", animationsEnabled=" + animationsEnabled);

        this.gameType = argGameType;  // ✅ Save GameType for later use
        this.context = current;

        switch (argGameType) {
            case 1 -> {
                sPlayer0 = "Player 1";
                sPlayer1 = "Player 2";
            }
            case 2 -> {
                sPlayer0 = "Player";
                sPlayer1 = "Android";
            }
            case 3 -> {
                sPlayer0 = player0Name != null ? player0Name : "Player 0";
                sPlayer1 = player1Name != null ? player1Name : "Player 1";
                isFlipped = localPlayerIndex == 1;
            }
            default -> {
                sPlayer0 = "Player 0";
                sPlayer1 = "Player 1";
            }
        }

        pField = new Paint();
        pFieldBorder= new Paint();
        pDots= new Paint();

        pField.setColor(ContextCompat.getColor(current, R.color.colorGreen));

        pFieldBorder.setStyle(Paint.Style.STROKE);
        pFieldBorder.setColor(Color.WHITE);

        pDots.setStyle(Paint.Style.FILL);
        pDots.setColor(Color.WHITE);

        rField = new Rect();
        rText = new Rect();

        Resources res = current.getResources();
        try {
            flFieldMarginX = res.getFraction(R.fraction.flFieldMarginX,1,1);
            flFieldMarginY = res.getFraction(R.fraction.flFieldMarginY,1,1);
            flLinesWidth = res.getFraction(R.fraction.flLinesWidth,1,1);
            flDots= res.getFraction(R.fraction.flDots,1,1);
            flText = res.getFraction(R.fraction.flText,1,1);
            flSpriteSize = res.getFraction(R.fraction.flSpriteSize,1,1);
            intFieldWidth = res.getInteger(R.integer.intFieldHalfWidth)*2;
            intFieldHeight = res.getInteger(R.integer.intFieldHalfHeight)*2;
        } catch (Exception e) {
            Log.e("TAG_Soccer", getClass().getSimpleName() + ".<init>: Failed to load field resources", e);
            throw new RuntimeException("Failed to load field configuration resources", e);
        }

        showIdlePlayerSprite = animationsEnabled && (gameType == 1 || gameType == 2);
        if (showIdlePlayerSprite) {
            idleRedPlayerFrames = IdleRedPlayerSprite.getFrames(current);
            idleBluePlayerFrames = IdleBluePlayerSprite.getFrames(current);
            runRedPlayerWestFrames = RunRedPlayerSprite.getWestFrames(current);
            runRedPlayerWestNorthFrames = RunRedPlayerSprite.getWestNorthFrames(current);
            runRedPlayerNorthFrames = RunRedPlayerSprite.getNorthFrames(current);
            runRedPlayerEastNorthFrames = RunRedPlayerSprite.getEastNorthFrames(current);
            runRedPlayerEastFrames = RunRedPlayerSprite.getEastFrames(current);
            runRedPlayerEastSouthFrames = RunRedPlayerSprite.getEastSouthFrames(current);
            runRedPlayerSouthFrames = RunRedPlayerSprite.getSouthFrames(current);
            runRedPlayerSouthWestFrames = RunRedPlayerSprite.getSouthWestFrames(current);
            runBluePlayerWestFrames = RunBluePlayerSprite.getWestFrames(current);
            runBluePlayerWestNorthFrames = RunBluePlayerSprite.getWestNorthFrames(current);
            runBluePlayerNorthFrames = RunBluePlayerSprite.getNorthFrames(current);
            runBluePlayerEastNorthFrames = RunBluePlayerSprite.getEastNorthFrames(current);
            runBluePlayerEastFrames = RunBluePlayerSprite.getEastFrames(current);
            runBluePlayerEastSouthFrames = RunBluePlayerSprite.getEastSouthFrames(current);
            runBluePlayerSouthFrames = RunBluePlayerSprite.getSouthFrames(current);
            runBluePlayerSouthWestFrames = RunBluePlayerSprite.getSouthWestFrames(current);
            activeRunRedPlayerFrames = runRedPlayerNorthFrames;
            activeRunBluePlayerFrames = runBluePlayerNorthFrames;
            idlePlayerLastFrameTime = SystemClock.uptimeMillis();
        } else {
            idleRedPlayerFrames = EMPTY_BITMAP_ARRAY;
            idleBluePlayerFrames = EMPTY_BITMAP_ARRAY;
            runRedPlayerWestFrames = EMPTY_BITMAP_ARRAY;
            runRedPlayerWestNorthFrames = EMPTY_BITMAP_ARRAY;
            runRedPlayerNorthFrames = EMPTY_BITMAP_ARRAY;
            runRedPlayerEastNorthFrames = EMPTY_BITMAP_ARRAY;
            runRedPlayerEastFrames = EMPTY_BITMAP_ARRAY;
            runRedPlayerEastSouthFrames = EMPTY_BITMAP_ARRAY;
            runRedPlayerSouthFrames = EMPTY_BITMAP_ARRAY;
            runRedPlayerSouthWestFrames = EMPTY_BITMAP_ARRAY;
            activeRunRedPlayerFrames = EMPTY_BITMAP_ARRAY;
            runBluePlayerWestFrames = EMPTY_BITMAP_ARRAY;
            runBluePlayerWestNorthFrames = EMPTY_BITMAP_ARRAY;
            runBluePlayerNorthFrames = EMPTY_BITMAP_ARRAY;
            runBluePlayerEastNorthFrames = EMPTY_BITMAP_ARRAY;
            runBluePlayerEastFrames = EMPTY_BITMAP_ARRAY;
            runBluePlayerEastSouthFrames = EMPTY_BITMAP_ARRAY;
            runBluePlayerSouthFrames = EMPTY_BITMAP_ARRAY;
            runBluePlayerSouthWestFrames = EMPTY_BITMAP_ARRAY;
            activeRunBluePlayerFrames = EMPTY_BITMAP_ARRAY;
        }

        pPlayer0=new Paint();
        pPlayer0.setStyle(Paint.Style.FILL);
        pPlayer0.setColor(ContextCompat.getColor(current, R.color.colorPlayer0));
        pPlayer0.setTextAlign(Paint.Align.CENTER);


        pPlayer1=new Paint();
        pPlayer1.setStyle(Paint.Style.FILL);
        pPlayer1.setColor(ContextCompat.getColor(current, R.color.colorPlayer1));
        pPlayer1.setTextAlign(Paint.Align.CENTER);

        pHintText=new Paint();
        pHintText.setStyle(Paint.Style.FILL);
        pHintText.setColor(Color.WHITE);
        pHintText.setTextAlign(Paint.Align.CENTER);

        // paint style and color
        Paint pHintBalloon = new Paint();
        pHintBalloon.setStyle(Paint.Style.FILL);
        pHintBalloon.setColor(Color.YELLOW);
        try {
            ballBitmap = BitmapFactory.decodeResource(current.getResources(), R.drawable.ball);
            if (ballBitmap == null) {
                Log.w("TAG_Soccer", getClass().getSimpleName() + ".<init>: Ball bitmap could not be loaded from resources");
                throw new RuntimeException("Failed to load ball bitmap resource");
            }
        } catch (Exception e) {
            Log.e("TAG_Soccer", getClass().getSimpleName() + ".<init>: Failed to load ball bitmap", e);
            throw new RuntimeException("Failed to load ball bitmap resource", e);
        }




        Moves=argMoves;
        possibleMoves=argPossibleMoves;
    }

    // called from GameView
    public void setRemainingTimes(long t0, long t1, Long ts) {
        remainingTime0 = t0;
        remainingTime1 = t1;
        turnStartTime = ts;
    }

    public int getFieldWidth() {
        return intFieldWidth;
    }

    public int getFieldHeight() {
        return intFieldHeight;
    }

    public boolean isFlipped() {
        return isFlipped;
    }

    public int x2w(float x) {


        if(rField.height()>rField.width() ) {
            //portrait
            return Math.round((x-rField.left)*intFieldWidth/rField.width());
        } else {
            //landscape
            return Math.round((x-rField.left)*intFieldHeight/rField.width());
        }
    }

    public int y2h(float y) {
        if(rField.height()>rField.width() ) {
            //portrait
            return Math.round((y-rField.top)*intFieldHeight/rField.height());
        } else {
            //landscape
            return Math.round((rField.bottom-y)*intFieldWidth/rField.height());
        }
    }

    private float w2x(int w) {

        if(rField.height()>rField.width() ) {
            //portrait
            return rField.left + (float) (w * rField.width()) / intFieldWidth;
        } else {
            //landscape
            return rField.left + (float) (w * rField.width()) / intFieldHeight;
        }

    }

    private float w2x(float w) {
        if (rField.height() > rField.width()) {
            return rField.left + (w * rField.width()) / intFieldWidth;
        } else {
            return rField.left + (w * rField.width()) / intFieldHeight;
        }
    }

    private float h2y(int h) {
        if(rField.height()>rField.width() ) {
            //portrait
            return rField.top+ (float) (h * rField.height()) /intFieldHeight;
        } else {
            //landscape
            return rField.bottom- (float) (h * rField.height()) /intFieldWidth;
        }

    }

    private float h2y(float h) {
        if (rField.height() > rField.width()) {
            return rField.top + (h * rField.height()) / intFieldHeight;
        } else {
            return rField.bottom - (h * rField.height()) / intFieldWidth;
        }
    }



    public void set(int x, int y, int width, int height) {
        int xMin, xMax, yMin, yMax;
        xMin = x+(int)(width*flFieldMarginX);
        xMax = x + width -(int)(width*flFieldMarginX)- 1;
        yMin = y+(int)(height*flFieldMarginY);
        yMax = y + height - (int)(height*flFieldMarginY)- 1;
        // The box's rField do not change unless the view's size changes
        rField.set(xMin, yMin, xMax, yMax);
    }

    public void startRunAnimation(MoveTo previous, MoveTo next) {
        if (!showIdlePlayerSprite) {
            return;
        }
        if (previous == null || next == null) {
            return;
        }
        if (RUN_FRAME_COUNT <= 0) {
            return;
        }
        if ((previous.X == next.X && previous.Y == next.Y)
                || (previous.X == -1 && previous.Y == -1)
                || (next.X == -1 && next.Y == -1)) {
            return;
        }

        float flippedStartX = flipX(previous.X);
        float flippedStartY = flipY(previous.Y);
        float flippedTargetX = flipX(next.X);
        float flippedTargetY = flipY(next.Y);

        float totalDeltaX = flippedTargetX - flippedStartX;
        float totalDeltaY = flippedTargetY - flippedStartY;

        RunAnimationFrameSet frameSet = selectRunAnimationFrames(totalDeltaX, totalDeltaY);
        if (frameSet.isEmpty()) {
            return;
        }

        activeRunRedPlayerFrames = frameSet.redFrames;
        activeRunBluePlayerFrames = frameSet.blueFrames;
        int availableRedFrames = Math.min(RUN_FRAME_COUNT, frameSet.redFrames.length);
        int availableBlueFrames = Math.min(RUN_FRAME_COUNT, frameSet.blueFrames.length);
        int availableFrames = Math.max(availableRedFrames, availableBlueFrames);
        if (availableFrames <= 0) {
            return;
        }

        float totalDistance = (float) Math.hypot(totalDeltaX, totalDeltaY);
        int frameLimit = availableFrames;
        if (totalDistance > 0f && RUN_FRAME_STEP_DISTANCE > 0f) {
            float framesForDistance = totalDistance / RUN_FRAME_STEP_DISTANCE;
            frameLimit = Math.max(1, Math.min(availableFrames, (int) Math.ceil(framesForDistance)));
        }

        runMovingPlayer = (previous.P == 0 || previous.P == 1) ? previous.P : -1;
        runRedDelayFrames = frameSet.redFrames.length > 0 && runMovingPlayer == 1
                ? RUN_DELAY_CYCLES
                : 0;
        runBlueDelayFrames = frameSet.blueFrames.length > 0 && runMovingPlayer == 0
                ? RUN_DELAY_CYCLES
                : 0;

        runFrameLimit = frameLimit + Math.max(runRedDelayFrames, runBlueDelayFrames);
        runStartRedCloser = previous.P == 0;
        runTargetRedCloser = next.P == 0;
        runStartBlueCloser = previous.P == 1;
        runTargetBlueCloser = next.P == 1;
        runStartGridX = flippedStartX;
        runStartGridY = flippedStartY;
        runTargetGridX = flippedTargetX;
        runTargetGridY = flippedTargetY;
        runTotalDistance = totalDistance;
        if (totalDistance > 0f) {
            runDirectionX = totalDeltaX / totalDistance;
            runDirectionY = totalDeltaY / totalDistance;
        } else {
            runDirectionX = 0f;
            runDirectionY = 0f;
        }
        runPlayerFrameIndex = 0;
        runPlayerLastFrameTime = SystemClock.uptimeMillis();
        runAnimationActive = true;
    }

    public boolean isRunAnimationActive() {
        return runAnimationActive;
    }

    private void stopRunAnimation(long referenceTime) {
        runAnimationActive = false;
        runPlayerFrameIndex = 0;
        runPlayerLastFrameTime = 0L;
        idlePlayerLastFrameTime = referenceTime;
        runFrameLimit = RUN_FRAME_COUNT;
        runDirectionX = 0f;
        runDirectionY = 0f;
        runTotalDistance = 0f;
        runStartRedCloser = false;
        runTargetRedCloser = false;
        runStartBlueCloser = false;
        runTargetBlueCloser = false;
        activeRunRedPlayerFrames = EMPTY_BITMAP_ARRAY;
        activeRunBluePlayerFrames = EMPTY_BITMAP_ARRAY;
        runMovingPlayer = -1;
        runRedDelayFrames = 0;
        runBlueDelayFrames = 0;
    }

    private void drawRunAnimation(Canvas canvas, float ballRadius) {
        if (!runAnimationActive) {
            return;
        }
        Bitmap[] redFrames = activeRunRedPlayerFrames != null ? activeRunRedPlayerFrames : EMPTY_BITMAP_ARRAY;
        Bitmap[] blueFrames = activeRunBluePlayerFrames != null ? activeRunBluePlayerFrames : EMPTY_BITMAP_ARRAY;
        int frameCount = Math.max(redFrames.length, blueFrames.length);
        int maxDelay = Math.max(runRedDelayFrames, runBlueDelayFrames);
        int frameLimit = Math.min(runFrameLimit, frameCount + maxDelay);
        if (frameLimit <= 0) {
            stopRunAnimation(SystemClock.uptimeMillis());
            return;
        }

        long now = SystemClock.uptimeMillis();
        if (runPlayerLastFrameTime == 0L) {
            runPlayerLastFrameTime = now;
        }

        long elapsed = now - runPlayerLastFrameTime;
        if (RunPlayerSprite.FRAME_DURATION_MS > 0 && elapsed >= RunPlayerSprite.FRAME_DURATION_MS) {
            long framesToAdvance = elapsed / RunPlayerSprite.FRAME_DURATION_MS;
            runPlayerFrameIndex += (int) framesToAdvance;
            if (runPlayerFrameIndex >= frameLimit) {
                stopRunAnimation(now);
                return;
            }
            long remainder = elapsed % RunPlayerSprite.FRAME_DURATION_MS;
            runPlayerLastFrameTime = now - remainder;
        }

        if (!runAnimationActive) {
            return;
        }

        int redFrameIndex = runPlayerFrameIndex - runRedDelayFrames;
        int blueFrameIndex = runPlayerFrameIndex - runBlueDelayFrames;

        Bitmap redFrame = redFrameIndex >= 0
                ? getRunFrame(redFrames, redFrameIndex, frameCount)
                : null;
        Bitmap blueFrame = blueFrameIndex >= 0
                ? getRunFrame(blueFrames, blueFrameIndex, frameCount)
                : null;

        if ((redFrame == null || redFrame.isRecycled())
                && (blueFrame == null || blueFrame.isRecycled())) {
            stopRunAnimation(now);
            return;
        }

        float spriteHeight = canvas.getHeight() * flSpriteSize;
        if (spriteHeight <= 0f) {
            stopRunAnimation(now);
            return;
        }

        float stepIndex = Math.min(runPlayerFrameIndex + 1, frameLimit);
        float distanceTraveled = RUN_FRAME_STEP_DISTANCE * stepIndex;
        if (runTotalDistance > 0f && distanceTraveled > runTotalDistance) {
            distanceTraveled = runTotalDistance;
        }

        float currentGridX = runStartGridX + runDirectionX * distanceTraveled;
        float currentGridY = runStartGridY + runDirectionY * distanceTraveled;

        if (runDirectionX > 0f) {
            currentGridX = Math.min(currentGridX, runTargetGridX);
        } else if (runDirectionX < 0f) {
            currentGridX = Math.max(currentGridX, runTargetGridX);
        }

        if (runDirectionY > 0f) {
            currentGridY = Math.min(currentGridY, runTargetGridY);
        } else if (runDirectionY < 0f) {
            currentGridY = Math.max(currentGridY, runTargetGridY);
        }

        float ballCenterX = w2x(currentGridX);
        float ballCenterY = h2y(currentGridY);
        float ballTop = ballCenterY - ballRadius;
        boolean drewFrame = false;

        float animationProgress;
        if (runTotalDistance > 0f) {
            animationProgress = distanceTraveled / runTotalDistance;
        } else if (runFrameLimit > 1) {
            animationProgress = (float) runPlayerFrameIndex / (float) (runFrameLimit - 1);
        } else {
            animationProgress = 1f;
        }
        animationProgress = clamp(animationProgress, 0f, 1f);

        float blueStartProximity = runStartBlueCloser ? 1f : 0f;
        float blueEndProximity = runTargetBlueCloser ? 1f : 0f;
        float blueProximity = clamp(lerp(blueStartProximity, blueEndProximity, animationProgress), 0f, 1f);

        float redStartProximity = runStartRedCloser ? 1f : 0f;
        float redEndProximity = runTargetRedCloser ? 1f : 0f;
        float redProximity = clamp(lerp(redStartProximity, redEndProximity, animationProgress), 0f, 1f);

        if (blueFrame != null && !blueFrame.isRecycled()) {
            float blueFarBottom = ballCenterY - ballRadius;
            float blueCloseBottom = ballCenterY + spriteHeight * (1f - ACTIVE_SPRITE_PROXIMITY_RATIO);
            float blueBottom = lerp(blueFarBottom, blueCloseBottom, blueProximity);
            float blueTop = blueBottom - spriteHeight;
            if (blueTop < 0f) {
                blueTop = 0f;
            }
            if (blueBottom > canvas.getHeight()) {
                blueBottom = canvas.getHeight();
            }

            float actualBlueHeight = blueBottom - blueTop;
            if (actualBlueHeight > 0f) {
                float blueWidth = actualBlueHeight * blueFrame.getWidth() / (float) blueFrame.getHeight();
                float blueLeft = ballCenterX - blueWidth / 2f;
                float blueRight = ballCenterX + blueWidth / 2f;
                RectF blueDst = new RectF(blueLeft, blueTop, blueRight, blueBottom);
                canvas.drawBitmap(blueFrame, null, blueDst, null);
                drewFrame = true;
            }
        }

        if (redFrame != null && !redFrame.isRecycled()) {
            float redFarTop = ballCenterY + ballRadius;
            float redCloseTop = ballCenterY - spriteHeight * ACTIVE_SPRITE_PROXIMITY_RATIO;
            float redTop = lerp(redFarTop, redCloseTop, redProximity);
            float redBottom = redTop + spriteHeight;
            if (redBottom > canvas.getHeight()) {
                redBottom = canvas.getHeight();
            }
            if (redTop < 0f) {
                redTop = 0f;
            }

            float actualRedHeight = redBottom - redTop;
            if (actualRedHeight > 0f) {
                float redWidth = actualRedHeight * redFrame.getWidth() / (float) redFrame.getHeight();
                float redLeft = ballCenterX - redWidth / 2f;
                float redRight = ballCenterX + redWidth / 2f;
                RectF redDst = new RectF(redLeft, redTop, redRight, redBottom);
                canvas.drawBitmap(redFrame, null, redDst, null);
                drewFrame = true;
            }
        }

        if (!drewFrame) {
            stopRunAnimation(now);
            return;
        }

        if (runPlayerFrameIndex >= frameLimit - 1) {
            stopRunAnimation(now);
        }
    }

    private static float lerp(float start, float end, float t) {
        return start + (end - start) * t;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private RunAnimationFrameSet selectRunAnimationFrames(float deltaX, float deltaY) {
        if (deltaX == 0f && deltaY == 0f) {
            return RunAnimationFrameSet.EMPTY;
        }

        double angle = Math.atan2(-deltaY, deltaX);
        double degrees = Math.toDegrees(angle);
        if (degrees < 0) {
            degrees += 360.0;
        }

        Bitmap[] redFrames;
        Bitmap[] blueFrames;
        if (degrees >= 157.5 && degrees < 202.5) {
            redFrames = runRedPlayerWestFrames;
            blueFrames = runBluePlayerWestFrames;
        } else if (degrees >= 112.5 && degrees < 157.5) {
            redFrames = runRedPlayerWestNorthFrames;
            blueFrames = runBluePlayerWestNorthFrames;
        } else if (degrees >= 67.5 && degrees < 112.5) {
            redFrames = runRedPlayerNorthFrames;
            blueFrames = runBluePlayerNorthFrames;
        } else if (degrees >= 22.5 && degrees < 67.5) {
            redFrames = runRedPlayerEastNorthFrames;
            blueFrames = runBluePlayerEastNorthFrames;
        } else if (degrees >= 337.5 || degrees < 22.5) {
            redFrames = runRedPlayerEastFrames;
            blueFrames = runBluePlayerEastFrames;
        } else if (degrees >= 292.5 && degrees < 337.5) {
            redFrames = runRedPlayerEastSouthFrames;
            blueFrames = runBluePlayerEastSouthFrames;
        } else if (degrees >= 247.5 && degrees < 292.5) {
            redFrames = runRedPlayerSouthFrames;
            blueFrames = runBluePlayerSouthFrames;
        } else {
            redFrames = runRedPlayerSouthWestFrames;
            blueFrames = runBluePlayerSouthWestFrames;
        }

        if ((redFrames == null || redFrames.length == 0)
                && (blueFrames == null || blueFrames.length == 0)) {
            return RunAnimationFrameSet.EMPTY;
        }

        return new RunAnimationFrameSet(redFrames, blueFrames);
    }

    private Bitmap getRunFrame(Bitmap[] frames, int frameIndex, int frameCount) {
        if (frames == null || frames.length == 0 || frameCount <= 0) {
            return null;
        }

        int maxIndex = Math.min(frameCount - 1, frames.length - 1);
        int safeIndex = Math.min(frameIndex, maxIndex);
        if (safeIndex < 0 || safeIndex >= frames.length) {
            return null;
        }

        return frames[safeIndex];
    }

    private static final class RunAnimationFrameSet {
        static final RunAnimationFrameSet EMPTY = new RunAnimationFrameSet(EMPTY_BITMAP_ARRAY, EMPTY_BITMAP_ARRAY);

        final Bitmap[] redFrames;
        final Bitmap[] blueFrames;

        RunAnimationFrameSet(Bitmap[] redFrames, Bitmap[] blueFrames) {
            this.redFrames = redFrames != null ? redFrames : EMPTY_BITMAP_ARRAY;
            this.blueFrames = blueFrames != null ? blueFrames : EMPTY_BITMAP_ARRAY;
        }

        boolean isEmpty() {
            return redFrames.length == 0 && blueFrames.length == 0;
        }
    }

    private int flipX(int x) {
        return isFlipped ? intFieldWidth - x : x;
    }

    private int flipY(int y) {
        return isFlipped ? intFieldHeight - y : y;
    }

    public void draw(Canvas canvas) {
        //Log.d("TAG_Soccer", "Field.draw: Started");

        int oldx, oldy;

        boolean isPortrait = rField.height() > rField.width();
        float textSize = isPortrait ? rField.height() * flText : rField.width() * flText;
        float strokeWidth = isPortrait ? rField.height() * flLinesWidth : rField.width() * flLinesWidth;
        float dotSize = isPortrait ? rField.height() * flDots : rField.width() * flDots;

        pPlayer0.setTextSize(textSize);
        pPlayer1.setTextSize(textSize);
        pPlayer0.setStrokeWidth(strokeWidth);
        pPlayer1.setStrokeWidth(strokeWidth);
        pFieldBorder.setStrokeWidth(strokeWidth);
        pHintText.setTextSize(textSize);


        // Shared banner width (95 % of the visible field)
        //PG: former: float bannerWidthPx = rField.width() * 0.95f;
        //PG: now:
        float bannerWidthPx = canvas.getWidth() * 0.95f;

        // Draw field
        canvas.drawRect(rField, pField);
        canvas.drawRect(rField, pFieldBorder);

        // Gates and labels
        // Top gate (above Y = 0)
        canvas.drawRect(
                w2x(flipX((intFieldWidth / 2) - 1)),
                h2y(flipY(-1)),
                w2x(flipX((intFieldWidth / 2) + 1)),
                h2y(flipY(0)),
                pField
        );
        canvas.drawRect(
                w2x(flipX((intFieldWidth / 2) - 1)),
                h2y(flipY(-1)),
                w2x(flipX((intFieldWidth / 2) + 1)),
                h2y(flipY(0)),
                pFieldBorder
        );

        // Bottom gate (below Y = intFieldHeight)
        canvas.drawRect(
                w2x(flipX((intFieldWidth / 2) - 1)),
                h2y(flipY(intFieldHeight)),
                w2x(flipX((intFieldWidth / 2) + 1)),
                h2y(flipY(intFieldHeight + 1)),
                pField
        );
        canvas.drawRect(
                w2x(flipX((intFieldWidth / 2) - 1)),
                h2y(flipY(intFieldHeight)),
                w2x(flipX((intFieldWidth / 2) + 1)),
                h2y(flipY(intFieldHeight + 1)),
                pFieldBorder
        );

        // Dots on gates
        for (int x = (intFieldWidth / 2) - 1; x <= (intFieldWidth / 2) + 1; x++) {
            canvas.drawCircle(w2x(flipX(x)), h2y(flipY(-1)), dotSize, pDots);
            canvas.drawCircle(w2x(flipX(x)), h2y(flipY(intFieldHeight + 1)), dotSize, pDots);
        }

        // after you’ve calculated textSize and set it on pPlayer0 / pPlayer1 …
        float left  = w2x(flipX(intFieldWidth/2 - 1));
        float right = w2x(flipX(intFieldWidth/2 + 1));
        float gateWidthPx = Math.abs(right - left) * 0.9f;   // 10 % side padding

        String fitP1 = fitName(sPlayer1, pPlayer1, gateWidthPx);
        String fitP0 = fitName(sPlayer0, pPlayer0, gateWidthPx);

        canvas.drawText(fitP1,
                w2x(flipX(intFieldWidth / 2)),
                h2y(flipY(-1)) + (h2y(flipY(0)) - h2y(flipY(-1))) / 2 + pPlayer1.getTextSize() / 2,
                pPlayer1);

        canvas.drawText(fitP0,
                w2x(flipX(intFieldWidth / 2)),
                h2y(flipY(intFieldHeight)) + (h2y(flipY(intFieldHeight + 1)) - h2y(flipY(intFieldHeight))) / 2 + pPlayer0.getTextSize() / 2,
                pPlayer0);

        // Dots
        for (int x = 0; x <= intFieldWidth; x++) {
            for (int y = 0; y <= intFieldHeight; y++) {
                canvas.drawCircle(w2x(flipX(x)), h2y(flipY(y)), dotSize, pDots);
            }
        }

        // Moves
        oldx = Moves.get(0).X;
        oldy = Moves.get(0).Y;
        for (int i = 1; i < Moves.size(); i++) {
            int newX = Moves.get(i).X;
            int newY = Moves.get(i).Y;

            // ⛔ Skip artificial moves, eg. in case of forefeit
            if (newX == -1 && newY == -1) continue;

            Paint p = Moves.get(i - 1).P == 0 ? pPlayer0 : pPlayer1;
            canvas.drawLine(
                    w2x(flipX(oldx)), h2y(flipY(oldy)),
                    w2x(flipX(newX)), h2y(flipY(newY)),
                    p
            );
            oldx = newX;
            oldy = newY;
        }

        // Possible moves
        int currentTurn = Moves.get(Moves.size() - 1).P;
        Paint movePaint = currentTurn == 0 ? pPlayer0 : pPlayer1;
        // Pulsing animation for possible moves
        final float animationDuration = 2000f; // ms for a full grow+shrink cycle
        final float animationSizeIncreasePercent = 2.0f; // target: dotSize -> dotSize * percent -> dotSize
        float pulseDotSize = dotSize; // default when not animating
        boolean shouldPulse = false;
        // Game type logic (corrected)
        if (gameType == 1) {
            // Player vs Player: pulse for both players
            shouldPulse = true;
        } else if (gameType == 2) {
            // Player vs Android: pulse only for player
            shouldPulse = (currentTurn == 0);
        } else if (gameType == 3) {
            // Multiplayer: pulse only for current player and only on their screen
            shouldPulse = (currentTurn == (isFlipped ? 1 : 0));
        }

        if (shouldPulse && turnStartTime != null) {
            long now = System.currentTimeMillis();
            long elapsed = (now - turnStartTime) % (long) animationDuration;
            float progress = (float) elapsed / animationDuration; // [0..1)

            // Ease from 0 -> 1 -> 0 using a cosine wave mapped to [0,1]
            // This guarantees we always start at the base size on a new turn.
            float ease01 = (1f - (float) Math.cos(progress * 2f * (float) Math.PI)) / 2f; // [0..1]

            float minSize = dotSize;
            float maxSize = dotSize * animationSizeIncreasePercent;
            pulseDotSize = minSize + (maxSize - minSize) * ease01;

            // Safety: never shrink below base dot size
            if (pulseDotSize < dotSize) pulseDotSize = dotSize;
        }

        for (MoveTo pm : possibleMoves) {
            canvas.drawCircle(w2x(flipX(pm.X)), h2y(flipY(pm.Y)), pulseDotSize, movePaint);
        }

        // Ball
        MoveTo last = Moves.get(Moves.size() - 1);

        // Skip artificial moves, eg. in case of forefeit
        if (last.X == -1 && last.Y == -1) {
            last = Moves.get(Moves.size() - 2);
        }
        float ballCenterX = w2x(flipX(last.X));
        float ballCenterY = h2y(flipY(last.Y));
        float radius = dotSize * 4;
        // Background circle behind the ball
        float radiusBackground = (float) (radius  * 0.8);
        canvas.drawCircle(ballCenterX, ballCenterY, radiusBackground, movePaint);

        RectF dst = new RectF(ballCenterX - radius, ballCenterY - radius, ballCenterX + radius, ballCenterY + radius);
        canvas.drawBitmap(ballBitmap, null, dst, null);

        drawRunAnimation(canvas, radius);

        if (showIdlePlayerSprite && flSpriteSize > 0f) {
            int redFrameCount = idleRedPlayerFrames.length;
            int blueFrameCount = idleBluePlayerFrames.length;
            int maxFrameCount = Math.max(redFrameCount, blueFrameCount);

            if (maxFrameCount > 0) {
                long now = SystemClock.uptimeMillis();
                if (idlePlayerLastFrameTime == 0L) {
                    idlePlayerLastFrameTime = now;
                }
                if (runAnimationActive) {
                    idlePlayerLastFrameTime = now;
                } else {
                    long elapsed = now - idlePlayerLastFrameTime;
                    if (IdlePlayerSprite.FRAME_DURATION_MS > 0 && elapsed >= IdlePlayerSprite.FRAME_DURATION_MS) {
                        long framesToAdvance = elapsed / IdlePlayerSprite.FRAME_DURATION_MS;
                        idlePlayerFrameIndex = (int) ((idlePlayerFrameIndex + framesToAdvance) % maxFrameCount);
                        long remainder = elapsed % IdlePlayerSprite.FRAME_DURATION_MS;
                        idlePlayerLastFrameTime = now - remainder;
                    }
                }

                float spriteHeight = canvas.getHeight() * flSpriteSize;
                if (spriteHeight > 0f) {
                    // Blue player above the ball, bottom touching the ball's top edge
                    boolean blueShouldBeCloser = currentTurn == 1;
                    float ballTop = ballCenterY - radius;
                    if (blueFrameCount > 0 && !runAnimationActive) {
                        Bitmap spriteFrame = idleBluePlayerFrames[idlePlayerFrameIndex % blueFrameCount];
                        if (spriteFrame != null && !spriteFrame.isRecycled()) {
                            float spriteBottom = blueShouldBeCloser
                                    ? ballCenterY + spriteHeight * (1-ACTIVE_SPRITE_PROXIMITY_RATIO)
                                    : ballCenterY + 1f;
                            float spriteTop = spriteBottom - spriteHeight;
                            if (spriteTop < 0f) {
                                spriteTop = 0f;
                            }
                            float actualSpriteHeight = spriteBottom - spriteTop;
                            if (actualSpriteHeight > 0f) {
                                float spriteWidth = actualSpriteHeight * spriteFrame.getWidth() / (float) spriteFrame.getHeight();
                                float spriteLeft = ballCenterX - spriteWidth / 2f;
                                float spriteRight = ballCenterX + spriteWidth / 2f;
                                RectF spriteDst = new RectF(spriteLeft, spriteTop, spriteRight, spriteBottom);
                                canvas.drawBitmap(spriteFrame, null, spriteDst, null);
                            }
                        }
                    }

                    // Red player below the ball, top touching the ball's bottom edge
                    boolean redShouldBeCloser = currentTurn == 0;
                    if (redFrameCount > 0 && !runAnimationActive) {
                        Bitmap spriteFrame = idleRedPlayerFrames[idlePlayerFrameIndex % redFrameCount];
                        if (spriteFrame != null && !spriteFrame.isRecycled()) {
                            float spriteTop = redShouldBeCloser
                                    ? ballCenterY - spriteHeight * ACTIVE_SPRITE_PROXIMITY_RATIO
                                    : ballCenterY + 1f;
                            float spriteBottom = spriteTop + spriteHeight;
                            if (spriteBottom > canvas.getHeight()) {
                                spriteBottom = canvas.getHeight();
                            }
                            float actualSpriteHeight = spriteBottom - spriteTop;
                            if (actualSpriteHeight > 0f) {
                                float spriteWidth = actualSpriteHeight * spriteFrame.getWidth() / (float) spriteFrame.getHeight();
                                float spriteLeft = ballCenterX - spriteWidth / 2f;
                                float spriteRight = ballCenterX + spriteWidth / 2f;
                                RectF spriteDst = new RectF(spriteLeft, spriteTop, spriteRight, spriteBottom);
                                canvas.drawBitmap(spriteFrame, null, spriteDst, null);
                            }
                        }
                    }
                }
            }
        }


        // Turn indicator
        boolean isLocalTurn = currentTurn == (isFlipped ? 1 : 0);  // flipped view = player 1

        String textTop;
        String textBottom;
        String opponentName = isFlipped ? sPlayer0 : sPlayer1;
        String oponentTime = formatClockSeconds(isFlipped ? remainingTime0 : remainingTime1);
        String localName = isFlipped ? sPlayer1 : sPlayer0;
        String localTime = formatClockSeconds(isFlipped ? remainingTime1 : remainingTime0);

        float bottomHintY = h2y(intFieldHeight+1)+(canvas.getHeight()-h2y(intFieldHeight+1))*2/3;
        float topHintY = h2y(-1) *2 / 3;

        /*Log.d("TAG_Soccer", "Field.draw: remainingTime0="+remainingTime0
                + " remainingTime1="+remainingTime1
                + " turnStartTime="+((turnStartTime == null) ? "null" : String.valueOf(turnStartTime)));*/

        if (isLocalTurn) {
            if (gameType != 3) {
                textBottom = context.getString(R.string.field_your_move);
            } else {

                textBottom = fitNameInBanner(localName,
                        " " + SafeStringFormatter.safeGetString(context, R.string.field_move_tail, localTime),
                        pHintText, bannerWidthPx);
                textTop = fitNameInBanner(opponentName,
                        " " + SafeStringFormatter.safeGetString(context, R.string.field_hourglass_tail, oponentTime),
                        pHintText, bannerWidthPx);

                pHintText.getTextBounds(textTop, 0, textTop.length(), rText);
                canvas.drawText(textTop,
                        w2x(flipX(intFieldWidth / 2)),
                        topHintY - (float) rText.height() / 2,
                        pHintText);

                //Log.d("TAG_Soccer", "Field.draw: textTop: " + textTop);
            }
            pHintText.getTextBounds(textBottom, 0, textBottom.length(), rText);
            canvas.drawText(textBottom,
                    w2x(flipX(intFieldWidth / 2)),
                    bottomHintY - (float) rText.height() / 2,
                    pHintText);
            //Log.d("TAG_Soccer", "Field.draw: textBottom: " + textBottom);

        } else {
            if (gameType == 1) {
                textTop = context.getString(R.string.field_your_move_ellipsis);  // could be improved, but likely shared screen
            } else if (gameType == 2) {
                textTop = context.getString(R.string.field_thinking);
            } else  {
                // Multiplayer: determine which name is the opponent


                if (turnStartTime != null) {
                    textTop = fitNameInBanner(opponentName,
                            " " + SafeStringFormatter.safeGetString(context, R.string.field_move_ellipsis_tail, oponentTime),
                            pHintText, bannerWidthPx);
                } else {
                    textTop = fitNameInBanner(SafeStringFormatter.safeGetString(context, R.string.field_waiting_for, opponentName),
                            " " + SafeStringFormatter.safeGetString(context, R.string.field_to_start_tail, oponentTime),
                            pHintText, bannerWidthPx);
                }
                textBottom = fitNameInBanner(localName,
                        " " + SafeStringFormatter.safeGetString(context, R.string.field_hourglass_tail, localTime),
                        pHintText, bannerWidthPx);

                pHintText.getTextBounds(textBottom, 0, textBottom.length(), rText);
                canvas.drawText(textBottom,
                        w2x(flipX(intFieldWidth / 2)),
                        bottomHintY - (float) rText.height()/2,
                        pHintText);

                Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": textBottom: " + textBottom);

            }

            pHintText.getTextBounds(textTop, 0, textTop.length(), rText);

            canvas.drawText(textTop,
                    w2x(flipX(intFieldWidth / 2)),
                    topHintY - (float) rText.height() / 2,
                    pHintText);

            //Log.d("TAG_Soccer", "Field.draw: textTop: " + textTop);
        }
    }
    private String formatClockSeconds(long seconds) {
        if (seconds < 0) seconds = 0;
        long min = seconds / 60;
        long sec = seconds % 60;
        return String.format(Locale.US, "%02d:%02d", min, sec);
    }

    private String fitName(String name, Paint paint, float maxWidthPx) {
        // 1.  Make a working copy of the paint so we don’t mutate the original
        Paint p = new Paint(paint);

        // 2.  Try gradually reducing text size until it fits,
        //     but stop once we hit a sensible minimum (e.g.  8 sp)
        final float MIN_TEXT_SP = 8f;
        while (p.measureText(name) > maxWidthPx && p.getTextSize() > MIN_TEXT_SP) {
            p.setTextSize(p.getTextSize() * 0.9f);  // scale down 10 %
        }
        paint.setTextSize(p.getTextSize());         // keep the final size

        // 3.  If it still doesn’t fit, ellipsise the tail
        if (p.measureText(name) > maxWidthPx) {
            TextPaint tp = new TextPaint(p);
            CharSequence ellipsised = TextUtils.ellipsize(
                    name, tp, maxWidthPx, TextUtils.TruncateAt.END);
            return ellipsised.toString();
        }
        return name;
    }

    /**
     * Shrinks or ellipsises only the player's name so that
     * {@code name + tail} fits into {@code maxWidthPx}.
     * paint  – any paint that already has the target textSize set
     *          (we DON’T mutate it here).
     */
    private String fitNameInBanner(String name,
                                   String tail,
                                   Paint paint,
                                   float maxWidthPx) {

        // How wide is the non-variable part (« move… ⏳ 00:00 ») ?
        float tailWidth = paint.measureText(tail);

        // Leave at least 10 px padding on both sides
        float nameBudget = Math.max(0, maxWidthPx - tailWidth - 20);

        // If the name already fits – good, we’re done
        if (paint.measureText(name) <= nameBudget) {
            return name + tail;
        }

        // Otherwise ellipsise the name only
        TextPaint tp = new TextPaint(paint);
        CharSequence shortName = TextUtils.ellipsize(
                name, tp, nameBudget, TextUtils.TruncateAt.END);

        return shortName + tail;
    }

}
