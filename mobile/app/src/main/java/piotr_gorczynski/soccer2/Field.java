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
    private final Bitmap[] idleBluePlayerFrames;
    private Bitmap[] activeRunRedPlayerFrames;
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
    private float runDeltaGridX = 0f;
    private float runDeltaGridY = 0f;

    private static final int RUN_FRAME_COUNT = RunPlayerSprite.FRAME_COUNT;

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
            activeRunRedPlayerFrames = runRedPlayerNorthFrames;
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

        Bitmap[] selectedFrames = selectRunAnimationFrames(totalDeltaX, totalDeltaY);
        if (selectedFrames.length == 0) {
            return;
        }

        activeRunRedPlayerFrames = selectedFrames;
        runStartGridX = flippedStartX;
        runStartGridY = flippedStartY;
        runDeltaGridX = totalDeltaX / RUN_FRAME_COUNT;
        runDeltaGridY = totalDeltaY / RUN_FRAME_COUNT;
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
    }

    private void drawRunAnimation(Canvas canvas, float ballRadius) {
        if (!runAnimationActive) {
            return;
        }
        Bitmap[] frames = activeRunRedPlayerFrames != null ? activeRunRedPlayerFrames : EMPTY_BITMAP_ARRAY;
        int frameCount = frames.length;
        if (frameCount == 0 || RUN_FRAME_COUNT <= 0) {
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
            if (runPlayerFrameIndex >= frameCount) {
                stopRunAnimation(now);
                return;
            }
            long remainder = elapsed % RunPlayerSprite.FRAME_DURATION_MS;
            runPlayerLastFrameTime = now - remainder;
        }

        if (!runAnimationActive) {
            return;
        }

        Bitmap spriteFrame = frames[Math.min(runPlayerFrameIndex, frameCount - 1)];
        if (spriteFrame == null || spriteFrame.isRecycled()) {
            stopRunAnimation(now);
            return;
        }

        float spriteHeight = canvas.getHeight() * flSpriteSize;
        if (spriteHeight <= 0f) {
            stopRunAnimation(now);
            return;
        }

        float stepIndex = Math.min(runPlayerFrameIndex + 1, RUN_FRAME_COUNT);
        float currentGridX = runStartGridX + runDeltaGridX * stepIndex;
        float currentGridY = runStartGridY + runDeltaGridY * stepIndex;

        float spriteTop = h2y(currentGridY) + ballRadius;
        float spriteBottom = spriteTop + spriteHeight;
        if (spriteBottom > canvas.getHeight()) {
            spriteBottom = canvas.getHeight();
        }
        if (spriteTop < 0f) {
            spriteTop = 0f;
        }

        float actualSpriteHeight = spriteBottom - spriteTop;
        if (actualSpriteHeight <= 0f) {
            stopRunAnimation(now);
            return;
        }

        float spriteWidth = actualSpriteHeight * spriteFrame.getWidth() / (float) spriteFrame.getHeight();
        float spriteCenterX = w2x(currentGridX);
        float spriteLeft = spriteCenterX - spriteWidth / 2f;
        float spriteRight = spriteCenterX + spriteWidth / 2f;

        RectF spriteDst = new RectF(spriteLeft, spriteTop, spriteRight, spriteBottom);
        canvas.drawBitmap(spriteFrame, null, spriteDst, null);

        if (runPlayerFrameIndex >= frameCount - 1) {
            stopRunAnimation(now);
        }
    }

    private Bitmap[] selectRunAnimationFrames(float deltaX, float deltaY) {
        if (deltaX == 0f && deltaY == 0f) {
            return EMPTY_BITMAP_ARRAY;
        }

        double angle = Math.atan2(-deltaY, deltaX);
        double degrees = Math.toDegrees(angle);
        if (degrees < 0) {
            degrees += 360.0;
        }

        Bitmap[] frames;
        if (degrees >= 157.5 && degrees < 202.5) {
            frames = runRedPlayerWestFrames;
        } else if (degrees >= 112.5 && degrees < 157.5) {
            frames = runRedPlayerWestNorthFrames;
        } else if (degrees >= 67.5 && degrees < 112.5) {
            frames = runRedPlayerNorthFrames;
        } else if (degrees >= 22.5 && degrees < 67.5) {
            frames = runRedPlayerEastNorthFrames;
        } else if (degrees >= 337.5 || degrees < 22.5) {
            frames = runRedPlayerEastFrames;
        } else if (degrees >= 292.5 && degrees < 337.5) {
            frames = runRedPlayerEastSouthFrames;
        } else if (degrees >= 247.5 && degrees < 292.5) {
            frames = runRedPlayerSouthFrames;
        } else {
            frames = runRedPlayerSouthWestFrames;
        }

        if (frames == null || frames.length == 0) {
            return EMPTY_BITMAP_ARRAY;
        }

        return frames;
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
        float cx = w2x(flipX(last.X));
        float cy = h2y(flipY(last.Y));
        float radius = dotSize * 4;
        // Background circle behind the ball
        float radiusBackground = (float) (radius  * 0.8);
        canvas.drawCircle(cx, cy, radiusBackground, movePaint);

        RectF dst = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);
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
                    if (blueFrameCount > 0) {
                        Bitmap spriteFrame = idleBluePlayerFrames[idlePlayerFrameIndex % blueFrameCount];
                        if (spriteFrame != null && !spriteFrame.isRecycled()) {
                            float spriteBottom = cy - radius;
                            float spriteTop = spriteBottom - spriteHeight;
                            if (spriteTop < 0f) {
                                spriteTop = 0f;
                            }
                            float actualSpriteHeight = spriteBottom - spriteTop;
                            if (actualSpriteHeight > 0f) {
                                float spriteWidth = actualSpriteHeight * spriteFrame.getWidth() / (float) spriteFrame.getHeight();
                                float spriteLeft = cx - spriteWidth / 2f;
                                float spriteRight = cx + spriteWidth / 2f;
                                RectF spriteDst = new RectF(spriteLeft, spriteTop, spriteRight, spriteBottom);
                                canvas.drawBitmap(spriteFrame, null, spriteDst, null);
                            }
                        }
                    }

                    // Red player below the ball, top touching the ball's bottom edge
                    if (redFrameCount > 0 && !runAnimationActive) {
                        Bitmap spriteFrame = idleRedPlayerFrames[idlePlayerFrameIndex % redFrameCount];
                        if (spriteFrame != null && !spriteFrame.isRecycled()) {
                            float spriteTop = cy + radius;
                            float spriteBottom = spriteTop + spriteHeight;
                            if (spriteBottom > canvas.getHeight()) {
                                spriteBottom = canvas.getHeight();
                            }
                            float actualSpriteHeight = spriteBottom - spriteTop;
                            if (actualSpriteHeight > 0f) {
                                float spriteWidth = actualSpriteHeight * spriteFrame.getWidth() / (float) spriteFrame.getHeight();
                                float spriteLeft = cx - spriteWidth / 2f;
                                float spriteRight = cx + spriteWidth / 2f;
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

                Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": textTop: " + textTop);
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

            Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": textTop: " + textTop);
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
