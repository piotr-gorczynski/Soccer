package piotr_gorczynski.soccer2;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
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
import androidx.preference.PreferenceManager;

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

    private volatile boolean spritesLoaded;
    private MoveTo pendingRunPrevious;
    private MoveTo pendingRunNext;

    private volatile Bitmap[] idleRedPlayerFrames;
    private volatile Bitmap[] runRedPlayerWestFrames;
    private volatile Bitmap[] runRedPlayerWestNorthFrames;
    private volatile Bitmap[] runRedPlayerNorthFrames;
    private volatile Bitmap[] runRedPlayerEastNorthFrames;
    private volatile Bitmap[] runRedPlayerEastFrames;
    private volatile Bitmap[] runRedPlayerEastSouthFrames;
    private volatile Bitmap[] runRedPlayerSouthFrames;
    private volatile Bitmap[] runRedPlayerSouthWestFrames;
    private volatile Bitmap[] runBluePlayerWestFrames;
    private volatile Bitmap[] runBluePlayerWestNorthFrames;
    private volatile Bitmap[] runBluePlayerNorthFrames;
    private volatile Bitmap[] runBluePlayerEastNorthFrames;
    private volatile Bitmap[] runBluePlayerEastFrames;
    private volatile Bitmap[] runBluePlayerEastSouthFrames;
    private volatile Bitmap[] runBluePlayerSouthFrames;
    private volatile Bitmap[] runBluePlayerSouthWestFrames;
    private volatile Bitmap[] idleBluePlayerFrames;
    private volatile Bitmap[] kickRedPlayerWestFrames;
    private volatile Bitmap[] kickRedPlayerWestNorthFrames;
    private volatile Bitmap[] kickRedPlayerNorthFrames;
    private volatile Bitmap[] kickRedPlayerEastNorthFrames;
    private volatile Bitmap[] kickRedPlayerEastFrames;
    private volatile Bitmap[] kickRedPlayerEastSouthFrames;
    private volatile Bitmap[] kickRedPlayerSouthFrames;
    private volatile Bitmap[] kickRedPlayerSouthWestFrames;
    private volatile Bitmap[] kickBluePlayerWestFrames;
    private volatile Bitmap[] kickBluePlayerWestNorthFrames;
    private volatile Bitmap[] kickBluePlayerNorthFrames;
    private volatile Bitmap[] kickBluePlayerEastNorthFrames;
    private volatile Bitmap[] kickBluePlayerEastFrames;
    private volatile Bitmap[] kickBluePlayerEastSouthFrames;
    private volatile Bitmap[] kickBluePlayerSouthFrames;
    private volatile Bitmap[] kickBluePlayerSouthWestFrames;
    private Bitmap[] activeRunRedPlayerFrames;
    private Bitmap[] activeRunBluePlayerFrames;
    private Bitmap[] activeKickRedPlayerFrames;
    private Bitmap[] activeKickBluePlayerFrames;
    private final String sPlayer0;
    private final String sPlayer1;
    private final int gameType;
    private final Context context;
    final ArrayList<MoveTo> possibleMoves;//= new ArrayList<MoveTo>();
    final ArrayList<MoveTo> Moves;//= new ArrayList<MoveTo>();
    private boolean isFlipped=false;

    private long remainingTime0, remainingTime1;

    private Long turnStartTime;
    private final boolean animationsEnabled;
    private final boolean showIdlePlayerSprite;
    private final boolean handTutorialAllowed;
    private boolean handTutorialPending = false;
    private SpriteLoadListener spriteLoadListener;
    private int idlePlayerFrameIndex = 0;
    private long idlePlayerLastFrameTime = 0L;
    private boolean runAnimationActive = false;
    private int runPlayerFrameIndex = 0;
    private long runPlayerLastFrameTime = 0L;
    private float runStartGridX = 0f;
    private float runStartGridY = 0f;
    private float runDirectionX = 0f;
    private float runDirectionY = 0f;
    private float runTotalDistance = 0f;
    private int runFrameLimit = RunPlayerSprite.FRAME_COUNT;
    private int runBaseFrameLimit = RunPlayerSprite.FRAME_COUNT;
    private boolean runStartRedCloser = false;
    private boolean runTargetRedCloser = false;
    private boolean runStartBlueCloser = false;
    private boolean runTargetBlueCloser = false;
    private int runMovingPlayer = -1;
    private int runRedDelayFrames = 0;
    private int runBlueDelayFrames = 0;
    private boolean runRedCompleted = false;
    private boolean runBlueCompleted = false;
    private float lastRunSpriteHeight = 0f;
    private float lastRunBallRadius = 0f;
    private boolean runRedFrameVisible = false;
    private boolean runBlueFrameVisible = false;
    private boolean kickAnimationActive = false;
    private int kickPlayerFrameIndex = 0;
    private long kickPlayerLastFrameTime = 0L;
    private long kickAnimationStartTime = 0L;
    private int kickFrameLimit = KickPlayerSprite.FRAME_COUNT;
    private boolean kickRedCompleted = false;
    private boolean kickBlueCompleted = false;
    private boolean kickRedFrameVisible = false;
    private boolean kickBlueFrameVisible = false;
    private boolean kickCompletedThisFrame = false;
    private float runFinalRedGridX = Float.NaN;
    private float runFinalRedGridY = Float.NaN;
    private float runFinalBlueGridX = Float.NaN;
    private float runFinalBlueGridY = Float.NaN;

    private boolean ballAnimationActive = false;
    private long ballAnimationStartTime = 0L;
    private float ballStartGridX = 0f;
    private float ballStartGridY = 0f;
    private float ballTargetGridX = 0f;
    private float ballTargetGridY = 0f;
    private float ballDirectionX = 0f;
    private float ballDirectionY = 0f;
    private float ballTotalDistance = 0f;
    private long ballAnimationDurationMs = 0L;
    private int ballKickDelayFrames = 0;

    private boolean waitForKickToStartOpponentRun = false;
    private int delayedOpponentPlayer = -1;

    private static final int RUN_FRAME_COUNT = RunPlayerSprite.FRAME_COUNT;
    private static final float RUN_FRAME_STEP_DISTANCE = RUN_FRAME_COUNT > 0
            ? (float) (Math.sqrt(2.0) / RUN_FRAME_COUNT)
            : 0f;
    private static final float RUN_DESTINATION_EPSILON = 0.001f;
    private static final float ACTIVE_SPRITE_PROXIMITY_RATIO = 0.7f;
    private static final int RUN_DELAY_CYCLES_FROM_KICK = 6;
    private static final int RUN_DELAY_CYCLES_FROM_RUN = 2;
    private static final float SPRITE_DIRECTION_EPSILON = 0.0001f;
    private static final long BALL_DEFAULT_DURATION_MS = 10
            * RunPlayerSprite.FRAME_DURATION_MS;
    private static final int BALL_DELAY_FROM_KICK_START = 4;

    // Hand tutorial constants
    private static final int DURATION_SHOWING_HAND = 1000; // milliseconds
    private static final int INITIAL_MOVE_COUNT = -1; // Sentinel value for first tutorial display
    private static final String PREF_HAND_TUTORIAL_ENABLED = "show_hand_tutorial";
    private static final String PREF_HAND_TUTORIAL_CYCLE_COUNT = "hand_tutorial_cycle_count";
    private static final String PREF_HAND_TUTORIAL_NEXT_THRESHOLD = "hand_tutorial_next_threshold";
    private static final int THRESHOLD_FIRST = 3;
    private static final int THRESHOLD_SECOND = 10;
    private static final int THRESHOLD_THIRD = 20;
    
    // Hand tutorial state
    private final Bitmap handBitmap;
    private final SharedPreferences prefs;
    private boolean showHandTutorial = false;
    private int handTutorialCycle = 0;
    private int handTutorialPositionIndex = 0;
    private long handTutorialLastUpdateTime = 0L;
    private int handTutorialLastMoveCount = INITIAL_MOVE_COUNT; // Track the number of moves when tutorial was last shown
    private HandTutorialDialogCallback dialogCallback = null;
    
    // Callback interface for requesting dialog
    public interface HandTutorialDialogCallback {
        void onRequestHandTutorialDialog();
    }

    public interface SpriteLoadListener {
        void onSpritesLoaded();
    }
    
    public void setHandTutorialDialogCallback(HandTutorialDialogCallback callback) {
        this.dialogCallback = callback;
    }

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

        this.animationsEnabled = animationsEnabled;
        showIdlePlayerSprite = animationsEnabled && (gameType == 1 || gameType == 2);
        spritesLoaded = !showIdlePlayerSprite;
        
        // Initialize all sprite arrays with empty bitmaps to avoid null pointer exceptions
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
        runBluePlayerWestFrames = EMPTY_BITMAP_ARRAY;
        runBluePlayerWestNorthFrames = EMPTY_BITMAP_ARRAY;
        runBluePlayerNorthFrames = EMPTY_BITMAP_ARRAY;
        runBluePlayerEastNorthFrames = EMPTY_BITMAP_ARRAY;
        runBluePlayerEastFrames = EMPTY_BITMAP_ARRAY;
        runBluePlayerEastSouthFrames = EMPTY_BITMAP_ARRAY;
        runBluePlayerSouthFrames = EMPTY_BITMAP_ARRAY;
        runBluePlayerSouthWestFrames = EMPTY_BITMAP_ARRAY;
        kickRedPlayerWestFrames = EMPTY_BITMAP_ARRAY;
        kickRedPlayerWestNorthFrames = EMPTY_BITMAP_ARRAY;
        kickRedPlayerNorthFrames = EMPTY_BITMAP_ARRAY;
        kickRedPlayerEastNorthFrames = EMPTY_BITMAP_ARRAY;
        kickRedPlayerEastFrames = EMPTY_BITMAP_ARRAY;
        kickRedPlayerEastSouthFrames = EMPTY_BITMAP_ARRAY;
        kickRedPlayerSouthFrames = EMPTY_BITMAP_ARRAY;
        kickRedPlayerSouthWestFrames = EMPTY_BITMAP_ARRAY;
        kickBluePlayerWestFrames = EMPTY_BITMAP_ARRAY;
        kickBluePlayerWestNorthFrames = EMPTY_BITMAP_ARRAY;
        kickBluePlayerNorthFrames = EMPTY_BITMAP_ARRAY;
        kickBluePlayerEastNorthFrames = EMPTY_BITMAP_ARRAY;
        kickBluePlayerEastFrames = EMPTY_BITMAP_ARRAY;
        kickBluePlayerEastSouthFrames = EMPTY_BITMAP_ARRAY;
        kickBluePlayerSouthFrames = EMPTY_BITMAP_ARRAY;
        kickBluePlayerSouthWestFrames = EMPTY_BITMAP_ARRAY;
        activeRunRedPlayerFrames = EMPTY_BITMAP_ARRAY;
        activeRunBluePlayerFrames = EMPTY_BITMAP_ARRAY;
        activeKickRedPlayerFrames = EMPTY_BITMAP_ARRAY;
        activeKickBluePlayerFrames = EMPTY_BITMAP_ARRAY;
        
        if (showIdlePlayerSprite) {
            // Move heavy bitmap operations to background thread to prevent ANR
            // This follows the same pattern as MenuActivity ANR fix (MENUACTIVITY_ANR_FIX.md)
            Thread spriteLoaderThread = new Thread(() -> {
                try {
                    // Load all sprite sheets in background
                    Bitmap[] loadedIdleRedPlayerFrames = IdleRedPlayerSprite.getFrames(current);
                    Bitmap[] loadedIdleBluePlayerFrames = IdleBluePlayerSprite.getFrames(current);
                    Bitmap[] loadedRunRedPlayerWestFrames = RunRedPlayerSprite.getWestFrames(current);
                    Bitmap[] loadedRunRedPlayerWestNorthFrames = RunRedPlayerSprite.getWestNorthFrames(current);
                    Bitmap[] loadedRunRedPlayerNorthFrames = RunRedPlayerSprite.getNorthFrames(current);
                    Bitmap[] loadedRunRedPlayerEastNorthFrames = RunRedPlayerSprite.getEastNorthFrames(current);
                    Bitmap[] loadedRunRedPlayerEastFrames = RunRedPlayerSprite.getEastFrames(current);
                    Bitmap[] loadedRunRedPlayerEastSouthFrames = RunRedPlayerSprite.getEastSouthFrames(current);
                    Bitmap[] loadedRunRedPlayerSouthFrames = RunRedPlayerSprite.getSouthFrames(current);
                    Bitmap[] loadedRunRedPlayerSouthWestFrames = RunRedPlayerSprite.getSouthWestFrames(current);
                    Bitmap[] loadedRunBluePlayerWestFrames = RunBluePlayerSprite.getWestFrames(current);
                    Bitmap[] loadedRunBluePlayerWestNorthFrames = RunBluePlayerSprite.getWestNorthFrames(current);
                    Bitmap[] loadedRunBluePlayerNorthFrames = RunBluePlayerSprite.getNorthFrames(current);
                    Bitmap[] loadedRunBluePlayerEastNorthFrames = RunBluePlayerSprite.getEastNorthFrames(current);
                    Bitmap[] loadedRunBluePlayerEastFrames = RunBluePlayerSprite.getEastFrames(current);
                    Bitmap[] loadedRunBluePlayerEastSouthFrames = RunBluePlayerSprite.getEastSouthFrames(current);
                    Bitmap[] loadedRunBluePlayerSouthFrames = RunBluePlayerSprite.getSouthFrames(current);
                    Bitmap[] loadedRunBluePlayerSouthWestFrames = RunBluePlayerSprite.getSouthWestFrames(current);
                    Bitmap[] loadedKickRedPlayerWestFrames = KickRedPlayerSprite.getWestFrames(current);
                    Bitmap[] loadedKickRedPlayerWestNorthFrames = KickRedPlayerSprite.getWestNorthFrames(current);
                    Bitmap[] loadedKickRedPlayerNorthFrames = KickRedPlayerSprite.getNorthFrames(current);
                    Bitmap[] loadedKickRedPlayerEastNorthFrames = KickRedPlayerSprite.getEastNorthFrames(current);
                    Bitmap[] loadedKickRedPlayerEastFrames = KickRedPlayerSprite.getEastFrames(current);
                    Bitmap[] loadedKickRedPlayerEastSouthFrames = KickRedPlayerSprite.getEastSouthFrames(current);
                    Bitmap[] loadedKickRedPlayerSouthFrames = KickRedPlayerSprite.getSouthFrames(current);
                    Bitmap[] loadedKickRedPlayerSouthWestFrames = KickRedPlayerSprite.getSouthWestFrames(current);
                    Bitmap[] loadedKickBluePlayerWestFrames = KickBluePlayerSprite.getWestFrames(current);
                    Bitmap[] loadedKickBluePlayerWestNorthFrames = KickBluePlayerSprite.getWestNorthFrames(current);
                    Bitmap[] loadedKickBluePlayerNorthFrames = KickBluePlayerSprite.getNorthFrames(current);
                    Bitmap[] loadedKickBluePlayerEastNorthFrames = KickBluePlayerSprite.getEastNorthFrames(current);
                    Bitmap[] loadedKickBluePlayerEastFrames = KickBluePlayerSprite.getEastFrames(current);
                    Bitmap[] loadedKickBluePlayerEastSouthFrames = KickBluePlayerSprite.getEastSouthFrames(current);
                    Bitmap[] loadedKickBluePlayerSouthFrames = KickBluePlayerSprite.getSouthFrames(current);
                    Bitmap[] loadedKickBluePlayerSouthWestFrames = KickBluePlayerSprite.getSouthWestFrames(current);
                    
                    // Update sprite arrays on main thread
                    // Check if activity is still valid before updating UI
                    if (current instanceof Activity) {
                        Activity activity = (Activity) current;
                        if (!activity.isFinishing() && !activity.isDestroyed()) {
                            activity.runOnUiThread(() -> {
                                idleRedPlayerFrames = loadedIdleRedPlayerFrames;
                                idleBluePlayerFrames = loadedIdleBluePlayerFrames;
                                runRedPlayerWestFrames = loadedRunRedPlayerWestFrames;
                                runRedPlayerWestNorthFrames = loadedRunRedPlayerWestNorthFrames;
                                runRedPlayerNorthFrames = loadedRunRedPlayerNorthFrames;
                                runRedPlayerEastNorthFrames = loadedRunRedPlayerEastNorthFrames;
                                runRedPlayerEastFrames = loadedRunRedPlayerEastFrames;
                                runRedPlayerEastSouthFrames = loadedRunRedPlayerEastSouthFrames;
                                runRedPlayerSouthFrames = loadedRunRedPlayerSouthFrames;
                                runRedPlayerSouthWestFrames = loadedRunRedPlayerSouthWestFrames;
                                runBluePlayerWestFrames = loadedRunBluePlayerWestFrames;
                                runBluePlayerWestNorthFrames = loadedRunBluePlayerWestNorthFrames;
                                runBluePlayerNorthFrames = loadedRunBluePlayerNorthFrames;
                                runBluePlayerEastNorthFrames = loadedRunBluePlayerEastNorthFrames;
                                runBluePlayerEastFrames = loadedRunBluePlayerEastFrames;
                                runBluePlayerEastSouthFrames = loadedRunBluePlayerEastSouthFrames;
                                runBluePlayerSouthFrames = loadedRunBluePlayerSouthFrames;
                                runBluePlayerSouthWestFrames = loadedRunBluePlayerSouthWestFrames;
                                kickRedPlayerWestFrames = loadedKickRedPlayerWestFrames;
                                kickRedPlayerWestNorthFrames = loadedKickRedPlayerWestNorthFrames;
                                kickRedPlayerNorthFrames = loadedKickRedPlayerNorthFrames;
                                kickRedPlayerEastNorthFrames = loadedKickRedPlayerEastNorthFrames;
                                kickRedPlayerEastFrames = loadedKickRedPlayerEastFrames;
                                kickRedPlayerEastSouthFrames = loadedKickRedPlayerEastSouthFrames;
                                kickRedPlayerSouthFrames = loadedKickRedPlayerSouthFrames;
                                kickRedPlayerSouthWestFrames = loadedKickRedPlayerSouthWestFrames;
                                kickBluePlayerWestFrames = loadedKickBluePlayerWestFrames;
                                kickBluePlayerWestNorthFrames = loadedKickBluePlayerWestNorthFrames;
                                kickBluePlayerNorthFrames = loadedKickBluePlayerNorthFrames;
                                kickBluePlayerEastNorthFrames = loadedKickBluePlayerEastNorthFrames;
                                kickBluePlayerEastFrames = loadedKickBluePlayerEastFrames;
                                kickBluePlayerEastSouthFrames = loadedKickBluePlayerEastSouthFrames;
                                kickBluePlayerSouthFrames = loadedKickBluePlayerSouthFrames;
                                kickBluePlayerSouthWestFrames = loadedKickBluePlayerSouthWestFrames;
                                activeRunRedPlayerFrames = loadedRunRedPlayerNorthFrames;
                                activeRunBluePlayerFrames = loadedRunBluePlayerNorthFrames;
                                activeKickRedPlayerFrames = loadedKickRedPlayerNorthFrames;
                                activeKickBluePlayerFrames = loadedKickBluePlayerNorthFrames;
                                idlePlayerLastFrameTime = SystemClock.uptimeMillis();

                                spritesLoaded = true;
                                notifySpriteLoadComplete();
                                startPendingRunAnimationIfReady();
                                enableHandTutorialIfReady();

                                Log.d("TAG_Soccer", getClass().getSimpleName() + ": Sprite sheets loaded successfully in background");
                            });
                        } else {
                            Log.d("TAG_Soccer", getClass().getSimpleName() + ": Activity finishing/destroyed, skipping sprite update");
                        }
                    }
                } catch (Exception e) {
                    Log.e("TAG_Soccer", getClass().getSimpleName() + ": Error loading sprites in background thread", e);
                    spritesLoaded = true; // prevent UI from getting stuck behind overlay if loading fails
                    notifySpriteLoadComplete();
                }
            }, "Field-SpriteLoader");
            spriteLoaderThread.start();
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

        // Load hand bitmap for tutorial
        Bitmap tempHandBitmap;
        try {
            tempHandBitmap = BitmapFactory.decodeResource(current.getResources(), R.drawable.hand);
            if (tempHandBitmap == null) {
                throw new RuntimeException("Hand bitmap resource returned null");
            }
        } catch (Exception e) {
            Log.e("TAG_Soccer", getClass().getSimpleName() + ".<init>: Failed to load hand bitmap", e);
            throw new RuntimeException("Failed to load hand bitmap resource", e);
        }
        handBitmap = tempHandBitmap;

        // Initialize preferences
        prefs = PreferenceManager.getDefaultSharedPreferences(current);

        // Check if hand tutorial is enabled in settings (default is true)
        boolean handTutorialEnabled = prefs.getBoolean(PREF_HAND_TUTORIAL_ENABLED, true);
        handTutorialAllowed = handTutorialEnabled && (argGameType == 1 || argGameType == 2);
        if (handTutorialAllowed) {
            // Load the current cycle count from preferences
            handTutorialCycle = prefs.getInt(PREF_HAND_TUTORIAL_CYCLE_COUNT, 0);
            handTutorialPending = true;
            enableHandTutorialIfReady();
            Log.d("TAG_Soccer", getClass().getSimpleName() + ".<init>: Hand tutorial enabled, current cycle count: " + handTutorialCycle);
        } else {
            Log.d("TAG_Soccer", getClass().getSimpleName() + ".<init>: Hand tutorial disabled or wrong game type");
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
        if (!areSpritesReadyForAnimations()) {
            pendingRunPrevious = previous;
            pendingRunNext = next;
            return;
        }

        startRunAnimationInternal(previous, next);
    }

    private void startPendingRunAnimationIfReady() {
        if (pendingRunPrevious == null || pendingRunNext == null) {
            return;
        }

        if (!areSpritesReadyForAnimations()) {
            return;
        }

        startRunAnimationInternal(pendingRunPrevious, pendingRunNext);
        pendingRunPrevious = null;
        pendingRunNext = null;
    }

    private void startRunAnimationInternal(MoveTo previous, MoveTo next) {
        if (previous == null || next == null) {
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
        float totalDistance = (float) Math.hypot(totalDeltaX, totalDeltaY);

        int movingPlayer = (previous.P == 0 || previous.P == 1) ? previous.P : -1;

        float spriteDeltaX = totalDeltaX;
        float spriteDeltaY = totalDeltaY;
        if (movingPlayer == 0 || movingPlayer == 1) {
            float spriteHeight = lastRunSpriteHeight;
            if (spriteHeight > 0f) {
                float startCenterX = w2x(flippedStartX);
                float startCenterY = h2y(flippedStartY);
                float endCenterX = w2x(flippedTargetX);
                float endCenterY = h2y(flippedTargetY);
                float ballRadius = lastRunBallRadius;

                if (movingPlayer == 0) {
                    float startProximity = 1f;
                    float endProximity = next.P == 0 ? 1f : 0f;

                    float startFarTop = startCenterY + ballRadius;
                    float startCloseTop = startCenterY - spriteHeight * ACTIVE_SPRITE_PROXIMITY_RATIO;
                    float startTop = lerp(startFarTop, startCloseTop, startProximity);
                    float startBottom = startTop + spriteHeight;

                    float endFarTop = endCenterY + ballRadius;
                    float endCloseTop = endCenterY - spriteHeight * ACTIVE_SPRITE_PROXIMITY_RATIO;
                    float endTop = lerp(endFarTop, endCloseTop, endProximity);
                    float endBottom = endTop + spriteHeight;

                    spriteDeltaX = endCenterX - startCenterX;
                    spriteDeltaY = endBottom - startBottom;
                } else {
                    float startProximity = 1f;
                    float endProximity = next.P == 1 ? 1f : 0f;

                    float startFarBottom = startCenterY - ballRadius;
                    float startCloseBottom = startCenterY + spriteHeight * (1f - ACTIVE_SPRITE_PROXIMITY_RATIO);
                    float startBottom = lerp(startFarBottom, startCloseBottom, startProximity);

                    float endFarBottom = endCenterY - ballRadius;
                    float endCloseBottom = endCenterY + spriteHeight * (1f - ACTIVE_SPRITE_PROXIMITY_RATIO);
                    float endBottom = lerp(endFarBottom, endCloseBottom, endProximity);

                    spriteDeltaX = endCenterX - startCenterX;
                    spriteDeltaY = endBottom - startBottom;
                }
            }
        }

        int movementFrameCount = RunPlayerSprite.FRAME_COUNT;
        boolean canStartRun = showIdlePlayerSprite && RUN_FRAME_COUNT > 0;
        boolean canStartKick = showIdlePlayerSprite && KickPlayerSprite.FRAME_COUNT > 0;

        if (canStartRun) {
            RunAnimationFrameSet frameSet = selectRunAnimationFrames(totalDeltaX, totalDeltaY, spriteDeltaX, spriteDeltaY);
            KickAnimationFrameSet kickFrameSet = canStartKick ? selectKickAnimationFrames(totalDeltaX, totalDeltaY, spriteDeltaX, spriteDeltaY) : KickAnimationFrameSet.EMPTY;
            if (!frameSet.isEmpty()) {
                activeRunRedPlayerFrames = frameSet.redFrames;
                activeRunBluePlayerFrames = frameSet.blueFrames;
                activeKickRedPlayerFrames = kickFrameSet.redFrames;
                activeKickBluePlayerFrames = kickFrameSet.blueFrames;

                int availableRedFrames = Math.min(RUN_FRAME_COUNT, frameSet.redFrames.length);
                int availableBlueFrames = Math.min(RUN_FRAME_COUNT, frameSet.blueFrames.length);
                int availableFrames = Math.max(availableRedFrames, availableBlueFrames);

                int availableKickRedFrames = !kickFrameSet.isEmpty() ? Math.min(KickPlayerSprite.FRAME_COUNT, kickFrameSet.redFrames.length) : 0;
                int availableKickBlueFrames = !kickFrameSet.isEmpty() ? Math.min(KickPlayerSprite.FRAME_COUNT, kickFrameSet.blueFrames.length) : 0;

                if (availableFrames > 0) {
                    int frameLimit = availableFrames;
                    if (totalDistance > 0f && RUN_FRAME_STEP_DISTANCE > 0f) {
                        float framesForDistance = totalDistance / RUN_FRAME_STEP_DISTANCE;
                        frameLimit = Math.max(1, Math.min(availableFrames, (int) Math.ceil(framesForDistance)));
                    }

                    movementFrameCount = frameLimit;

                    runMovingPlayer = movingPlayer;
                    boolean nextMoveSamePlayer = next.P == movingPlayer;
                    runRedDelayFrames = 0;
                    runBlueDelayFrames = 0;
                    delayedOpponentPlayer = -1;
                    waitForKickToStartOpponentRun = false;

                    if (runMovingPlayer == 1 && frameSet.redFrames.length > 0) {
                        if (!nextMoveSamePlayer) {
                            runRedDelayFrames = RUN_DELAY_CYCLES_FROM_KICK;
                            delayedOpponentPlayer = 0;
                            waitForKickToStartOpponentRun = true;
                        } else {
                            runRedDelayFrames = RUN_DELAY_CYCLES_FROM_RUN;
                        }
                    }

                    if (runMovingPlayer == 0 && frameSet.blueFrames.length > 0) {
                        if (!nextMoveSamePlayer) {
                            runBlueDelayFrames = RUN_DELAY_CYCLES_FROM_KICK;
                            delayedOpponentPlayer = 1;
                            waitForKickToStartOpponentRun = true;
                        } else {
                            runBlueDelayFrames = RUN_DELAY_CYCLES_FROM_RUN;
                        }
                    }

                    runBaseFrameLimit = frameLimit;
                    runFrameLimit = frameLimit + Math.max(runRedDelayFrames, runBlueDelayFrames);
                    runStartRedCloser = previous.P == 0;
                    runTargetRedCloser = next.P == 0;
                    runStartBlueCloser = previous.P == 1;
                    runTargetBlueCloser = next.P == 1;
                    runStartGridX = flippedStartX;
                    runStartGridY = flippedStartY;
                    runTotalDistance = totalDistance;
                    if (totalDistance > 0f) {
                        runDirectionX = totalDeltaX / totalDistance;
                        runDirectionY = totalDeltaY / totalDistance;
                    } else {
                        runDirectionX = 0f;
                        runDirectionY = 0f;
                    }
                    runPlayerFrameIndex = 0;
                    runPlayerLastFrameTime = 0L;  // Don't start time yet if we have kick animation
                    runRedCompleted = false;
                    runBlueCompleted = false;

                    // Initialize kick animation for the moving player
                    if (canStartKick && !kickFrameSet.isEmpty() && (movingPlayer == 0 || movingPlayer == 1)) {
                        kickAnimationActive = true;
                        kickPlayerFrameIndex = 0;
                        kickAnimationStartTime = SystemClock.uptimeMillis();
                        kickPlayerLastFrameTime = kickAnimationStartTime;
                        kickFrameLimit = movingPlayer == 0 ? availableKickRedFrames : availableKickBlueFrames;
                        kickRedCompleted = movingPlayer != 0;
                        kickBlueCompleted = movingPlayer != 1;

                        // Start counting run delay from the kick start
                        runAnimationActive = true;
                        runPlayerFrameIndex = 0;
                        runPlayerLastFrameTime = kickAnimationStartTime;
                    } else {
                        kickAnimationActive = false;
                        kickPlayerFrameIndex = 0;
                        kickPlayerLastFrameTime = 0L;
                        kickAnimationStartTime = 0L;
                        kickFrameLimit = KickPlayerSprite.FRAME_COUNT;
                        kickRedCompleted = false;
                        kickBlueCompleted = false;
                        // No kick animation, start run immediately
                        runAnimationActive = true;
                        runPlayerLastFrameTime = SystemClock.uptimeMillis();
                    }
                } else {
                    canStartRun = false;
                }
            } else {
                canStartRun = false;
            }
        }

        if (!canStartRun) {
            runAnimationActive = false;
            runPlayerFrameIndex = 0;
            runPlayerLastFrameTime = 0L;
            runRedDelayFrames = 0;
            runBlueDelayFrames = 0;
            runRedCompleted = false;
            runBlueCompleted = false;
            runBaseFrameLimit = RUN_FRAME_COUNT;
            runFrameLimit = RUN_FRAME_COUNT;
            runStartRedCloser = false;
            runTargetRedCloser = false;
            runStartBlueCloser = false;
            runTargetBlueCloser = false;
            runDirectionX = 0f;
            runDirectionY = 0f;
            runTotalDistance = 0f;
            waitForKickToStartOpponentRun = false;
            delayedOpponentPlayer = -1;
            resetRunFinalPositions();

            kickAnimationActive = false;
            kickPlayerFrameIndex = 0;
            kickPlayerLastFrameTime = 0L;
            kickAnimationStartTime = 0L;
            kickFrameLimit = KickPlayerSprite.FRAME_COUNT;
            kickRedCompleted = false;
            kickBlueCompleted = false;
        }

        startBallAnimation(flippedStartX, flippedStartY, flippedTargetX, flippedTargetY, totalDistance, movementFrameCount);
    }

    private boolean areSpritesReadyForAnimations() {
        return !showIdlePlayerSprite || spritesLoaded;
    }

    public boolean areSpritesLoaded() {
        return spritesLoaded;
    }

    public void setSpriteLoadListener(SpriteLoadListener listener) {
        spriteLoadListener = listener;
        notifySpriteLoadComplete();
    }

    private void notifySpriteLoadComplete() {
        if (spriteLoadListener != null && spritesLoaded) {
            spriteLoadListener.onSpritesLoaded();
        }
    }

    private void enableHandTutorialIfReady() {
        if (!handTutorialPending) {
            return;
        }

        if (!spritesLoaded) {
            return;
        }

        showHandTutorial = handTutorialAllowed;
        handTutorialPending = false;
        handTutorialLastUpdateTime = SystemClock.uptimeMillis();
    }

    public boolean isRunAnimationActive() {
        return runAnimationActive;
    }

    public boolean isBallAnimationActive() {
        return ballAnimationActive;
    }

    public boolean isHandTutorialActive() {
        return showHandTutorial && spritesLoaded;
    }

    private void resetRunFinalPositions() {
        runFinalRedGridX = Float.NaN;
        runFinalRedGridY = Float.NaN;
        runFinalBlueGridX = Float.NaN;
        runFinalBlueGridY = Float.NaN;
    }

    private void stopRunAnimation(long referenceTime) {
        runAnimationActive = false;
        runPlayerFrameIndex = 0;
        runPlayerLastFrameTime = 0L;
        idlePlayerLastFrameTime = referenceTime;
        runBaseFrameLimit = RUN_FRAME_COUNT;
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
        runRedCompleted = false;
        runBlueCompleted = false;
        waitForKickToStartOpponentRun = false;
        delayedOpponentPlayer = -1;
        completeBallAnimation();
    }

    private void completeBallAnimation() {
        ballAnimationActive = false;
        ballAnimationStartTime = 0L;
        ballStartGridX = ballTargetGridX;
        ballStartGridY = ballTargetGridY;
        ballDirectionX = 0f;
        ballDirectionY = 0f;
        ballTotalDistance = 0f;
        ballAnimationDurationMs = 0L;
        ballKickDelayFrames = 0;
    }

    private void startBallAnimation(float startGridX, float startGridY,
                                    float targetGridX, float targetGridY,
                                    float totalDistance, int frameCount) {
        ballTargetGridX = targetGridX;
        ballTargetGridY = targetGridY;

        boolean shouldAnimateBall = animationsEnabled && gameType != 3;

        if (!shouldAnimateBall) {
            ballStartGridX = targetGridX;
            ballStartGridY = targetGridY;
            ballDirectionX = 0f;
            ballDirectionY = 0f;
            ballTotalDistance = 0f;
            ballAnimationDurationMs = 0L;
            ballAnimationStartTime = 0L;
            ballAnimationActive = false;
            ballKickDelayFrames = 0;
            return;
        }

        if (totalDistance <= 0f) {
            ballStartGridX = targetGridX;
            ballStartGridY = targetGridY;
            completeBallAnimation();
            return;
        }

        ballStartGridX = startGridX;
        ballStartGridY = startGridY;
        ballTotalDistance = totalDistance;

        float deltaX = targetGridX - startGridX;
        float deltaY = targetGridY - startGridY;
        if (totalDistance > 0f) {
            ballDirectionX = deltaX / totalDistance;
            ballDirectionY = deltaY / totalDistance;
        } else {
            ballDirectionX = 0f;
            ballDirectionY = 0f;
        }

        ballAnimationDurationMs = frameCount > 0
                ? frameCount * RunPlayerSprite.FRAME_DURATION_MS
                : BALL_DEFAULT_DURATION_MS;
        boolean kickDelayNeeded = kickAnimationActive;
        ballKickDelayFrames = kickDelayNeeded ? BALL_DELAY_FROM_KICK_START : 0;
        ballAnimationStartTime = kickDelayNeeded ? 0L : SystemClock.uptimeMillis();
        ballAnimationActive = true;
    }

    private float computeBallDistance(long elapsedMs, long durationMs, float totalDistance) {
        if (durationMs <= 0L) {
            return totalDistance;
        }

        long clampedElapsed = Math.max(0L, Math.min(elapsedMs, durationMs));
        float normalized = (float) clampedElapsed / (float) durationMs;
        float progress = (2f * normalized) - (normalized * normalized);
        float distance = totalDistance * progress;
        return clamp(distance, totalDistance);
    }

    private void releaseOpponentRunAfterKickFrame() {
        if (!waitForKickToStartOpponentRun) {
            return;
        }

        if (kickPlayerFrameIndex < RUN_DELAY_CYCLES_FROM_KICK) {
            return;
        }

        int currentRunFrame = Math.max(0, runPlayerFrameIndex);
        if (delayedOpponentPlayer == 0) {
            runRedDelayFrames = currentRunFrame;
        } else if (delayedOpponentPlayer == 1) {
            runBlueDelayFrames = currentRunFrame;
        } else {
            waitForKickToStartOpponentRun = false;
            return;
        }

        runFrameLimit = runBaseFrameLimit + Math.max(runRedDelayFrames, runBlueDelayFrames);
        waitForKickToStartOpponentRun = false;
    }

    private void drawKickAnimation(Canvas canvas, float ballRadius) {
        if (!kickAnimationActive) {
            return;
        }
        
        Bitmap[] redFrames = activeKickRedPlayerFrames != null ? activeKickRedPlayerFrames : EMPTY_BITMAP_ARRAY;
        Bitmap[] blueFrames = activeKickBluePlayerFrames != null ? activeKickBluePlayerFrames : EMPTY_BITMAP_ARRAY;
        int redFrameCount = redFrames.length;
        int blueFrameCount = blueFrames.length;
        
        if (kickFrameLimit <= 0) {
            stopKickAnimation(SystemClock.uptimeMillis());
            return;
        }

        long now = SystemClock.uptimeMillis();
        if (kickPlayerLastFrameTime == 0L) {
            kickPlayerLastFrameTime = now;
        }

        long elapsed = now - kickPlayerLastFrameTime;
        if (KickPlayerSprite.FRAME_DURATION_MS > 0 && elapsed >= KickPlayerSprite.FRAME_DURATION_MS) {
            long framesToAdvance = elapsed / KickPlayerSprite.FRAME_DURATION_MS;
            kickPlayerFrameIndex += (int) framesToAdvance;
            if (kickPlayerFrameIndex >= kickFrameLimit) {
                stopKickAnimation(now);
                return;
            }
            long remainder = elapsed % KickPlayerSprite.FRAME_DURATION_MS;
            kickPlayerLastFrameTime = now - remainder;
        }

        releaseOpponentRunAfterKickFrame();

        if (!kickAnimationActive) {
            return;
        }

        // Only the moving player shows kick animation
        Bitmap redFrame = null;
        Bitmap blueFrame = null;
        
        if (runMovingPlayer == 0 && redFrameCount > 0 && !kickRedCompleted) {
            redFrame = getKickFrame(redFrames, kickPlayerFrameIndex, redFrameCount);
            if (kickPlayerFrameIndex >= redFrameCount - 1) {
                kickRedCompleted = true;
            }
        }
        
        if (runMovingPlayer == 1 && blueFrameCount > 0 && !kickBlueCompleted) {
            blueFrame = getKickFrame(blueFrames, kickPlayerFrameIndex, blueFrameCount);
            if (kickPlayerFrameIndex >= blueFrameCount - 1) {
                kickBlueCompleted = true;
            }
        }

        float spriteHeight = canvas.getHeight() * flSpriteSize;
        if (spriteHeight <= 0f) {
            stopKickAnimation(now);
            return;
        }
        lastRunSpriteHeight = spriteHeight;

        float ballCenterX = w2x(runStartGridX);
        float ballCenterY = h2y(runStartGridY);

        kickRedFrameVisible = false;
        kickBlueFrameVisible = false;

        if (blueFrame != null && !blueFrame.isRecycled()) {
            float blueProximity = runStartBlueCloser ? 1f : 0f;
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
                kickBlueFrameVisible = true;
                Log.d("TAG_Soccer", getClass().getSimpleName() + ".drawKickAnimation: "
                        + "blueSprite left=" + blueLeft + " top=" + blueTop + " right=" + blueRight + " bottom=" + blueBottom + ", "
                        + "frameIndex=" + kickPlayerFrameIndex);
            }
        }

        if (redFrame != null && !redFrame.isRecycled()) {
            float redProximity = runStartRedCloser ? 1f : 0f;
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
                kickRedFrameVisible = true;
                Log.d("TAG_Soccer", getClass().getSimpleName() + ".drawKickAnimation: "
                        + "redSprite left=" + redLeft + " top=" + redTop + " right=" + redRight + " bottom=" + redBottom + ", "
                        + "frameIndex=" + kickPlayerFrameIndex);
            }
        }

        // Check if kick animation is complete
        if ((runMovingPlayer == 0 && kickRedCompleted) || (runMovingPlayer == 1 && kickBlueCompleted)) {
            stopKickAnimation(now);
        }
    }

    private Bitmap getKickFrame(Bitmap[] frames, int frameIndex, int frameCount) {
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

    private void stopKickAnimation(long referenceTime) {
        long elapsedSinceKickStart = kickAnimationStartTime > 0L
                ? Math.max(0L, referenceTime - kickAnimationStartTime)
                : 0L;

        kickAnimationActive = false;
        kickPlayerFrameIndex = 0;
        kickPlayerLastFrameTime = 0L;
        kickFrameLimit = KickPlayerSprite.FRAME_COUNT;
        kickRedCompleted = false;
        kickBlueCompleted = false;
        activeKickRedPlayerFrames = EMPTY_BITMAP_ARRAY;
        activeKickBluePlayerFrames = EMPTY_BITMAP_ARRAY;

        if (waitForKickToStartOpponentRun) {
            releaseOpponentRunAfterKickFrame();
            if (waitForKickToStartOpponentRun) {
                int currentRunFrame = Math.max(0, runPlayerFrameIndex);
                if (delayedOpponentPlayer == 0) {
                    runRedDelayFrames = currentRunFrame;
                } else if (delayedOpponentPlayer == 1) {
                    runBlueDelayFrames = currentRunFrame;
                }
                runFrameLimit = runBaseFrameLimit + Math.max(runRedDelayFrames, runBlueDelayFrames);
                waitForKickToStartOpponentRun = false;
            }
        }

        // Start (or resync) run animation after the kick completes
        if (!runAnimationActive) {
            if (RunPlayerSprite.FRAME_DURATION_MS > 0 && elapsedSinceKickStart > 0L) {
                int elapsedRunFrames = (int) (elapsedSinceKickStart / RunPlayerSprite.FRAME_DURATION_MS);
                runRedDelayFrames = Math.max(0, runRedDelayFrames - elapsedRunFrames);
                runBlueDelayFrames = Math.max(0, runBlueDelayFrames - elapsedRunFrames);
            }

            int updatedDelay = Math.max(runRedDelayFrames, runBlueDelayFrames);
            runFrameLimit = runBaseFrameLimit + updatedDelay;
            runAnimationActive = true;
            runPlayerFrameIndex = 0;
            runPlayerLastFrameTime = referenceTime;
        } else {
            // Kick and run were started together; ensure run timing begins after kick
            // but avoid resetting an already running timer, which can stall frame
            // advancement if the kick finishes mid-run.
            if (runPlayerLastFrameTime == 0L) {
                runPlayerLastFrameTime = referenceTime;
            }
        }

        kickAnimationStartTime = 0L;
        kickCompletedThisFrame = true;
    }

    private void drawRunAnimation(Canvas canvas, float ballRadius) {
        if (!runAnimationActive) {
            return;
        }
        Bitmap[] redFrames = activeRunRedPlayerFrames != null ? activeRunRedPlayerFrames : EMPTY_BITMAP_ARRAY;
        Bitmap[] blueFrames = activeRunBluePlayerFrames != null ? activeRunBluePlayerFrames : EMPTY_BITMAP_ARRAY;
        int redFrameCount = redFrames.length;
        int blueFrameCount = blueFrames.length;
        int frameCount = Math.max(redFrameCount, blueFrameCount);
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

        if (waitForKickToStartOpponentRun) {
            if (delayedOpponentPlayer == 1) {
                blueFrameIndex = -1;
            } else if (delayedOpponentPlayer == 0) {
                redFrameIndex = -1;
            }
        }

        Bitmap redFrame = redFrameIndex >= 0
                ? getRunFrame(redFrames, redFrameIndex, redFrameCount)
                : null;
        Bitmap blueFrame = blueFrameIndex >= 0
                ? getRunFrame(blueFrames, blueFrameIndex, blueFrameCount)
                : null;

        // Avoid drawing the kicking player's run frames while the kick animation is active
        // or on the same frame when the kick finishes.
        if (kickAnimationActive || kickCompletedThisFrame) {
            if (runMovingPlayer == 0) {
                redFrame = null;
            } else if (runMovingPlayer == 1) {
                blueFrame = null;
            }
        }

        float spriteHeight = canvas.getHeight() * flSpriteSize;
        if (spriteHeight <= 0f) {
            stopRunAnimation(now);
            return;
        }
        lastRunSpriteHeight = spriteHeight;

        float redDistanceTraveled = computeDistanceForFrameIndex(redFrameIndex, redFrameCount, runTotalDistance, true);
        float blueDistanceTraveled = computeDistanceForFrameIndex(blueFrameIndex, blueFrameCount, runTotalDistance, false);

        boolean redHasFrames = redFrameCount > 0;
        boolean blueHasFrames = blueFrameCount > 0;

        if (!redHasFrames) {
            runRedCompleted = true;
        }
        if (!blueHasFrames) {
            runBlueCompleted = true;
        }

        if (redHasFrames && !runRedCompleted) {
            boolean redReached = runTotalDistance > 0f
                    ? redDistanceTraveled >= runTotalDistance - RUN_DESTINATION_EPSILON
                    : redFrameIndex >= redFrameCount - 1;
            if (redReached) {
                runRedCompleted = true;
                redFrame = null;
            }
        } else {
            redFrame = null;
        }

        if (blueHasFrames && !runBlueCompleted) {
            boolean blueReached = runTotalDistance > 0f
                    ? blueDistanceTraveled >= runTotalDistance - RUN_DESTINATION_EPSILON
                    : blueFrameIndex >= blueFrameCount - 1;
            if (blueReached) {
                runBlueCompleted = true;
                blueFrame = null;
            }
        } else {
            blueFrame = null;
        }

        float redAnimationProgress = computeAnimationProgress(redFrameIndex, redFrameCount, redDistanceTraveled);
        float blueAnimationProgress = computeAnimationProgress(blueFrameIndex, blueFrameCount, blueDistanceTraveled);

        float blueStartProximity = runStartBlueCloser ? 1f : 0f;
        float blueEndProximity = runTargetBlueCloser ? 1f : 0f;
        float blueProximity = clamp(lerp(blueStartProximity, blueEndProximity, blueAnimationProgress), 1f);

        float redStartProximity = runStartRedCloser ? 1f : 0f;
        float redEndProximity = runTargetRedCloser ? 1f : 0f;
        float redProximity = clamp(lerp(redStartProximity, redEndProximity, redAnimationProgress), 1f);

        float redGridX = runStartGridX + runDirectionX * redDistanceTraveled;
        float redGridY = runStartGridY + runDirectionY * redDistanceTraveled;
        float redCenterX = w2x(redGridX);
        float redCenterY = h2y(redGridY);

        float blueGridX = runStartGridX + runDirectionX * blueDistanceTraveled;
        float blueGridY = runStartGridY + runDirectionY * blueDistanceTraveled;
        float blueCenterX = w2x(blueGridX);
        float blueCenterY = h2y(blueGridY);

        // Save current sprite positions for use in idle animation.
        // Only update the player that is actually moving so the idle animation for
        // the stationary opponent keeps using their previous position.
        if (runMovingPlayer == 0) {
            runFinalRedGridX = redGridX;
            runFinalRedGridY = redGridY;
        }
        if (runMovingPlayer == 1) {
            runFinalBlueGridX = blueGridX;
            runFinalBlueGridY = blueGridY;
        }

        // Calculate ball position
        float ballGridX = runStartGridX;
        float ballGridY = runStartGridY;
        if (ballAnimationActive) {
            long ballElapsed = now - ballAnimationStartTime;
            long duration = ballAnimationDurationMs > 0L ? ballAnimationDurationMs : BALL_DEFAULT_DURATION_MS;
            if (ballElapsed < duration && ballTotalDistance > 0f) {
                float traveled = computeBallDistance(ballElapsed, duration, ballTotalDistance);
                ballGridX = ballStartGridX + ballDirectionX * traveled;
                ballGridY = ballStartGridY + ballDirectionY * traveled;
            } else {
                ballGridX = ballTargetGridX;
                ballGridY = ballTargetGridY;
            }
        }
        float ballCenterX = w2x(ballGridX);
        float ballCenterY = h2y(ballGridY);

        runBlueFrameVisible = false;
        runRedFrameVisible = false;

        if (blueFrame != null && !blueFrame.isRecycled()) {
            float blueFarBottom = blueCenterY - ballRadius;
            float blueCloseBottom = blueCenterY + spriteHeight * (1f - ACTIVE_SPRITE_PROXIMITY_RATIO);
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
                float blueLeft = blueCenterX - blueWidth / 2f;
                float blueRight = blueCenterX + blueWidth / 2f;
                RectF blueDst = new RectF(blueLeft, blueTop, blueRight, blueBottom);
                canvas.drawBitmap(blueFrame, null, blueDst, null);
                runBlueFrameVisible = true;
                Log.d("TAG_Soccer", getClass().getSimpleName() + ".drawRunAnimation: "
                        + "blueSprite left=" + blueLeft + " top=" + blueTop + " right=" + blueRight + " bottom=" + blueBottom + ", "
                        + "frameIndex=" + runPlayerFrameIndex);
            }
        }

        if (redFrame != null && !redFrame.isRecycled()) {
            float redFarTop = redCenterY + ballRadius;
            float redCloseTop = redCenterY - spriteHeight * ACTIVE_SPRITE_PROXIMITY_RATIO;
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
                float redLeft = redCenterX - redWidth / 2f;
                float redRight = redCenterX + redWidth / 2f;
                RectF redDst = new RectF(redLeft, redTop, redRight, redBottom);
                canvas.drawBitmap(redFrame, null, redDst, null);
                runRedFrameVisible = true;
                Log.d("TAG_Soccer", getClass().getSimpleName() + ".drawRunAnimation: "
                        + "redSprite left=" + redLeft + " top=" + redTop + " right=" + redRight + " bottom=" + redBottom + ", "
                        + "frameIndex=" + runPlayerFrameIndex);
            }
        }

        if ((!redHasFrames || runRedCompleted) && (!blueHasFrames || runBlueCompleted)) {
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

    private static float clamp(float value, float max) {
        return Math.max((float) 0.0, Math.min(max, value));
    }

    private float computeDistanceForFrameIndex(int frameIndex, int frameCount, float totalDistance, boolean advanceImmediately) {
        if (frameIndex < 0 || frameCount <= 0) {
            return 0f;
        }
        int stepOffset = advanceImmediately ? 1 : 0;
        int clampedStep = Math.min(frameIndex + stepOffset, frameCount);
        float distance = RUN_FRAME_STEP_DISTANCE * clampedStep;
        if (totalDistance > 0f && distance > totalDistance) {
            distance = totalDistance;
        }
        return distance;
    }

    private float computeAnimationProgress(int frameIndex, int frameCount, float distanceTraveled) {
        if (frameIndex < 0) {
            return 0f;
        }
        if (runTotalDistance > 0f) {
            return clamp(distanceTraveled / runTotalDistance, 1f);
        }
        if (frameCount > 1) {
            int clampedIndex = Math.min(frameIndex, frameCount - 1);
            return clamp((float) clampedIndex / (float) (frameCount - 1), 1f);
        }
        return 1f;
    }

    private RunAnimationFrameSet selectRunAnimationFrames(float deltaX, float deltaY, float spriteDeltaX, float spriteDeltaY) {
        if (deltaX == 0f && deltaY == 0f) {
            return RunAnimationFrameSet.EMPTY;
        }

        float orientationX = spriteDeltaX;
        float orientationY = spriteDeltaY;

        double orientationMagnitude = Math.hypot(orientationX, orientationY);
        if (orientationMagnitude < SPRITE_DIRECTION_EPSILON) {
            orientationX = deltaX;
            orientationY = deltaY;
        }

        double angle = Math.atan2(-orientationY, orientationX);
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

    private KickAnimationFrameSet selectKickAnimationFrames(float deltaX, float deltaY, float spriteDeltaX, float spriteDeltaY) {
        if (deltaX == 0f && deltaY == 0f) {
            return KickAnimationFrameSet.EMPTY;
        }

        float orientationX = spriteDeltaX;
        float orientationY = spriteDeltaY;

        double orientationMagnitude = Math.hypot(orientationX, orientationY);
        if (orientationMagnitude < SPRITE_DIRECTION_EPSILON) {
            orientationX = deltaX;
            orientationY = deltaY;
        }

        double angle = Math.atan2(-orientationY, orientationX);
        double degrees = Math.toDegrees(angle);
        if (degrees < 0) {
            degrees += 360.0;
        }

        Bitmap[] redFrames;
        Bitmap[] blueFrames;
        if (degrees >= 157.5 && degrees < 202.5) {
            redFrames = kickRedPlayerWestFrames;
            blueFrames = kickBluePlayerWestFrames;
        } else if (degrees >= 112.5 && degrees < 157.5) {
            redFrames = kickRedPlayerWestNorthFrames;
            blueFrames = kickBluePlayerWestNorthFrames;
        } else if (degrees >= 67.5 && degrees < 112.5) {
            redFrames = kickRedPlayerNorthFrames;
            blueFrames = kickBluePlayerNorthFrames;
        } else if (degrees >= 22.5 && degrees < 67.5) {
            redFrames = kickRedPlayerEastNorthFrames;
            blueFrames = kickBluePlayerEastNorthFrames;
        } else if (degrees >= 337.5 || degrees < 22.5) {
            redFrames = kickRedPlayerEastFrames;
            blueFrames = kickBluePlayerEastFrames;
        } else if (degrees >= 292.5 && degrees < 337.5) {
            redFrames = kickRedPlayerEastSouthFrames;
            blueFrames = kickBluePlayerEastSouthFrames;
        } else if (degrees >= 247.5 && degrees < 292.5) {
            redFrames = kickRedPlayerSouthFrames;
            blueFrames = kickBluePlayerSouthFrames;
        } else {
            redFrames = kickRedPlayerSouthWestFrames;
            blueFrames = kickBluePlayerSouthWestFrames;
        }

        if ((redFrames == null || redFrames.length == 0)
                && (blueFrames == null || blueFrames.length == 0)) {
            return KickAnimationFrameSet.EMPTY;
        }

        return new KickAnimationFrameSet(redFrames, blueFrames);
    }

    private static final class KickAnimationFrameSet {
        static final KickAnimationFrameSet EMPTY = new KickAnimationFrameSet(EMPTY_BITMAP_ARRAY, EMPTY_BITMAP_ARRAY);

        final Bitmap[] redFrames;
        final Bitmap[] blueFrames;

        KickAnimationFrameSet(Bitmap[] redFrames, Bitmap[] blueFrames) {
            this.redFrames = redFrames != null ? redFrames : EMPTY_BITMAP_ARRAY;
            this.blueFrames = blueFrames != null ? blueFrames : EMPTY_BITMAP_ARRAY;
        }

        boolean isEmpty() {
            return redFrames.length == 0 && blueFrames.length == 0;
        }
    }

    private static final class BallState {
        final float centerX;
        final float centerY;
        final float radius;

        BallState(float centerX, float centerY, float radius) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.radius = radius;
        }
    }

    private int flipX(int x) {
        return isFlipped ? intFieldWidth - x : x;
    }

    private int flipY(int y) {
        return isFlipped ? intFieldHeight - y : y;
    }

    private void drawFieldAndGates(Canvas canvas, float dotSize) {
        canvas.drawRect(rField, pField);
        canvas.drawRect(rField, pFieldBorder);

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

        for (int x = (intFieldWidth / 2) - 1; x <= (intFieldWidth / 2) + 1; x++) {
            canvas.drawCircle(w2x(flipX(x)), h2y(flipY(-1)), dotSize, pDots);
            canvas.drawCircle(w2x(flipX(x)), h2y(flipY(intFieldHeight + 1)), dotSize, pDots);
        }

        float left = w2x(flipX(intFieldWidth / 2 - 1));
        float right = w2x(flipX(intFieldWidth / 2 + 1));
        float gateWidthPx = Math.abs(right - left) * 0.9f;

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
    }

    private float computePulseDotSize(float dotSize, int currentTurn) {
        final float animationDuration = 2000f;
        final float animationSizeIncreasePercent = 2.0f;

        float pulseDotSize = dotSize;
        boolean shouldPulse = false;

        if (gameType == 1) {
            shouldPulse = true;
        } else if (gameType == 2) {
            shouldPulse = (currentTurn == 0);
        } else if (gameType == 3) {
            shouldPulse = (currentTurn == (isFlipped ? 1 : 0));
        }

        if (shouldPulse && turnStartTime != null) {
            long now = System.currentTimeMillis();
            long elapsed = (now - turnStartTime) % (long) animationDuration;
            float progress = (float) elapsed / animationDuration;

            float ease01 = (1f - (float) Math.cos(progress * 2f * (float) Math.PI)) / 2f;

            float maxSize = dotSize * animationSizeIncreasePercent;
            pulseDotSize = dotSize + (maxSize - dotSize) * ease01;

            if (pulseDotSize < dotSize) pulseDotSize = dotSize;
        }

        return pulseDotSize;
    }

    private BallState drawBall(Canvas canvas, float dotSize, Paint movePaint) {
        MoveTo last = Moves.get(Moves.size() - 1);
        if (last.X == -1 && last.Y == -1) {
            last = Moves.get(Moves.size() - 2);
        }

        float targetGridX = flipX(last.X);
        float targetGridY = flipY(last.Y);

        float ballGridX = targetGridX;
        float ballGridY = targetGridY;

        if (ballAnimationActive) {
            long now = SystemClock.uptimeMillis();
            boolean kickDelayActive = ballKickDelayFrames > 0
                    && kickAnimationActive
                    && kickPlayerFrameIndex < ballKickDelayFrames;

            if (kickDelayActive) {
                ballGridX = ballStartGridX;
                ballGridY = ballStartGridY;
            } else {
                if (ballKickDelayFrames > 0 && ballAnimationStartTime == 0L) {
                    ballAnimationStartTime = now;
                }
                ballKickDelayFrames = 0;

                long elapsed = ballAnimationStartTime == 0L ? 0L : now - ballAnimationStartTime;
                long duration = ballAnimationDurationMs > 0L ? ballAnimationDurationMs : BALL_DEFAULT_DURATION_MS;

                if (elapsed >= duration || ballTotalDistance <= 0f) {
                    ballGridX = ballTargetGridX;
                    ballGridY = ballTargetGridY;
                    completeBallAnimation();
                } else {
                    float traveled = computeBallDistance(elapsed, duration, ballTotalDistance);
                    ballGridX = ballStartGridX + ballDirectionX * traveled;
                    ballGridY = ballStartGridY + ballDirectionY * traveled;
                }
            }
        } else {
            ballStartGridX = targetGridX;
            ballStartGridY = targetGridY;
            ballTargetGridX = targetGridX;
            ballTargetGridY = targetGridY;
        }

        float ballCenterX = w2x(ballGridX);
        float ballCenterY = h2y(ballGridY);
        float radius = dotSize * 4;
        float radiusBackground = radius * 0.8f;

        lastRunBallRadius = radius;

        canvas.drawCircle(ballCenterX, ballCenterY, radiusBackground, movePaint);

        RectF dst = new RectF(ballCenterX - radius, ballCenterY - radius, ballCenterX + radius, ballCenterY + radius);
        canvas.drawBitmap(ballBitmap, null, dst, null);

        Log.d("TAG_Soccer", getClass().getSimpleName() + ".drawBall: "
                + "ballCenterX=" + ballCenterX + " ballCenterY=" + ballCenterY);

        return new BallState(ballCenterX, ballCenterY, radius);
    }

    private MoveTo findLastMoveByPlayer(int playerIndex) {
        if (Moves == null || Moves.isEmpty()) {
            return null;
        }

        int turnAfterPlayerMove = playerIndex == 0 ? 1 : 0;
        for (int i = Moves.size() - 1; i >= 0; i--) {
            MoveTo move = Moves.get(i);
            if (move.X == -1 && move.Y == -1) {
                continue; // Skip artificial moves (e.g., forfeits)
            }
            if (move.P == turnAfterPlayerMove) {
                return move;
            }
        }
        return null;
    }

    private void drawIdlePlayers(Canvas canvas, BallState ballState, int currentTurn) {
        if (!showIdlePlayerSprite || flSpriteSize <= 0f) {
            return;
        }

        MoveTo lastRedMove = findLastMoveByPlayer(0);
        MoveTo lastBlueMove = findLastMoveByPlayer(1);

        int redFrameCount = idleRedPlayerFrames.length;
        int blueFrameCount = idleBluePlayerFrames.length;
        int maxFrameCount = Math.max(redFrameCount, blueFrameCount);

        if (maxFrameCount == 0) {
            return;
        }

        long now = SystemClock.uptimeMillis();
        if (idlePlayerLastFrameTime == 0L) {
            idlePlayerLastFrameTime = now;
        }

        boolean runActive = runAnimationActive;
        boolean blueDelayActive = runActive && runBlueDelayFrames > 0
                && runPlayerFrameIndex < runBlueDelayFrames;
        boolean redDelayActive = runActive && runRedDelayFrames > 0
                && runPlayerFrameIndex < runRedDelayFrames;

        boolean hasRedRunFrames = activeRunRedPlayerFrames != null
                && activeRunRedPlayerFrames.length > 0;
        boolean hasBlueRunFrames = activeRunBluePlayerFrames != null
                && activeRunBluePlayerFrames.length > 0;

        boolean isRedMoving = runActive && hasRedRunFrames && !runRedCompleted
                && runPlayerFrameIndex >= runRedDelayFrames;
        boolean isBlueMoving = runActive && hasBlueRunFrames && !runBlueCompleted
                && runPlayerFrameIndex >= runBlueDelayFrames;

        boolean shouldDrawIdleBlue = !runBlueFrameVisible && !kickBlueFrameVisible && (!runActive
                || runBlueCompleted
                || (isRedMoving && blueDelayActive)
                || (!isRedMoving && !isBlueMoving));
        boolean shouldDrawIdleRed = !runRedFrameVisible && !kickRedFrameVisible && (!runActive
                || runRedCompleted
                || (isBlueMoving && redDelayActive)
                || (!isBlueMoving && !isRedMoving));
        boolean shouldAdvanceIdle = shouldDrawIdleBlue || shouldDrawIdleRed;

        if (shouldAdvanceIdle) {
            long elapsed = now - idlePlayerLastFrameTime;
            if (IdlePlayerSprite.FRAME_DURATION_MS > 0 && elapsed >= IdlePlayerSprite.FRAME_DURATION_MS) {
                long framesToAdvance = elapsed / IdlePlayerSprite.FRAME_DURATION_MS;
                idlePlayerFrameIndex = (int) ((idlePlayerFrameIndex + framesToAdvance) % maxFrameCount);
                long remainder = elapsed % IdlePlayerSprite.FRAME_DURATION_MS;
                idlePlayerLastFrameTime = now - remainder;
            }
        } else {
            idlePlayerLastFrameTime = now;
        }

        float spriteHeight = canvas.getHeight() * flSpriteSize;
        if (spriteHeight <= 0f) {
            return;
        }

        lastRunSpriteHeight = spriteHeight;

        float ballCenterX = ballState.centerX;
        float ballCenterY = ballState.centerY;

        boolean blueShouldBeCloser = currentTurn == 1;
        if (runActive && runMovingPlayer == 0 && blueDelayActive) {
            blueShouldBeCloser = false;
        }
        if (kickAnimationActive && runMovingPlayer == 0) {
            blueShouldBeCloser = false;
        }
        float idleBlueCenterX;
        float idleBlueCenterY;
        if (!Float.isNaN(runFinalBlueGridX) && !Float.isNaN(runFinalBlueGridY)) {
            // Use the final position from the last run animation
            idleBlueCenterX = w2x(runFinalBlueGridX);
            idleBlueCenterY = h2y(runFinalBlueGridY);
        } else if (lastBlueMove != null) {
            idleBlueCenterX = w2x(flipX(lastBlueMove.X));
            idleBlueCenterY = h2y(flipY(lastBlueMove.Y));
        } else {
            idleBlueCenterX = ballCenterX;
            idleBlueCenterY = ballCenterY;
        }

        // Align the idle sprite horizontally with the ball so its center matches the ball's
        if (blueShouldBeCloser) {
            idleBlueCenterX = ballCenterX;
        }

        if (blueFrameCount > 0 && shouldDrawIdleBlue) {
            Bitmap spriteFrame = idleBluePlayerFrames[idlePlayerFrameIndex % blueFrameCount];
            if (spriteFrame != null && !spriteFrame.isRecycled()) {
                float spriteBottom = blueShouldBeCloser
                        ? idleBlueCenterY + spriteHeight * (1 - ACTIVE_SPRITE_PROXIMITY_RATIO)
                        : idleBlueCenterY + 1f;
                float spriteTop = spriteBottom - spriteHeight;
                if (spriteTop < 0f) {
                    spriteTop = 0f;
                }
                float actualSpriteHeight = spriteBottom - spriteTop;
                if (actualSpriteHeight > 0f) {
                    float spriteWidth = actualSpriteHeight * spriteFrame.getWidth() / (float) spriteFrame.getHeight();
                    float spriteLeft = idleBlueCenterX - spriteWidth / 2f;
                    float spriteRight = idleBlueCenterX + spriteWidth / 2f;
                    RectF spriteDst = new RectF(spriteLeft, spriteTop, spriteRight, spriteBottom);
                    canvas.drawBitmap(spriteFrame, null, spriteDst, null);
                    Log.d("TAG_Soccer", getClass().getSimpleName() + ".drawIdlePlayers: "
                            + "blueSprite left=" + spriteLeft + " top=" + spriteTop + " right=" + spriteRight + " bottom=" + spriteBottom + ", "
                            + "frameIndex=" + idlePlayerFrameIndex);
                }
            }
        }

        boolean redShouldBeCloser = currentTurn == 0;
        if (runActive && runMovingPlayer == 1 && redDelayActive) {
            redShouldBeCloser = false;
        }
        if (kickAnimationActive && runMovingPlayer == 1) {
            redShouldBeCloser = false;
        }
        float idleRedCenterX;
        float idleRedCenterY;
        if (!Float.isNaN(runFinalRedGridX) && !Float.isNaN(runFinalRedGridY)) {
            // Use the final position from the last run animation
            idleRedCenterX = w2x(runFinalRedGridX);
            idleRedCenterY = h2y(runFinalRedGridY);
        } else if (lastRedMove != null) {
            idleRedCenterX = w2x(flipX(lastRedMove.X));
            idleRedCenterY = h2y(flipY(lastRedMove.Y));
        } else {
            idleRedCenterX = ballCenterX;
            idleRedCenterY = ballCenterY;
        }

        // Align the idle sprite horizontally with the ball so its center matches the ball's
        if (redShouldBeCloser) {
            idleRedCenterX = ballCenterX;
        }

        if (redFrameCount > 0 && shouldDrawIdleRed) {
            Bitmap spriteFrame = idleRedPlayerFrames[idlePlayerFrameIndex % redFrameCount];
            if (spriteFrame != null && !spriteFrame.isRecycled()) {
                float spriteTop = redShouldBeCloser
                        ? idleRedCenterY - spriteHeight * ACTIVE_SPRITE_PROXIMITY_RATIO
                        : idleRedCenterY + 1f;
                float spriteBottom = spriteTop + spriteHeight;
                if (spriteBottom > canvas.getHeight()) {
                    spriteBottom = canvas.getHeight();
                }
                float actualSpriteHeight = spriteBottom - spriteTop;
                if (actualSpriteHeight > 0f) {
                    float spriteWidth = actualSpriteHeight * spriteFrame.getWidth() / (float) spriteFrame.getHeight();
                    float spriteLeft = idleRedCenterX - spriteWidth / 2f;
                    float spriteRight = idleRedCenterX + spriteWidth / 2f;
                    RectF spriteDst = new RectF(spriteLeft, spriteTop, spriteRight, spriteBottom);
                    canvas.drawBitmap(spriteFrame, null, spriteDst, null);
                    Log.d("TAG_Soccer", getClass().getSimpleName() + ".drawIdlePlayers: "
                            + "redSprite left=" + spriteLeft + " top=" + spriteTop + " right=" + spriteRight + " bottom=" + spriteBottom + ", "
                            + "frameIndex=" + idlePlayerFrameIndex);
                }
            }
        }
    }

    public void draw(Canvas canvas) {
        //Log.d("TAG_Soccer", "Field.draw: Started");

        kickCompletedThisFrame = false;

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

        // 1) Field
        drawFieldAndGates(canvas, dotSize);

        // 2) Dots and lines
        for (int x = 0; x <= intFieldWidth; x++) {
            for (int y = 0; y <= intFieldHeight; y++) {
                canvas.drawCircle(w2x(flipX(x)), h2y(flipY(y)), dotSize, pDots);
            }
        }

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

        int currentTurn = Moves.get(Moves.size() - 1).P;
        Paint movePaint = currentTurn == 0 ? pPlayer0 : pPlayer1;
        float pulseDotSize = computePulseDotSize(dotSize, currentTurn);
        for (MoveTo pm : possibleMoves) {
            canvas.drawCircle(w2x(flipX(pm.X)), h2y(flipY(pm.Y)), pulseDotSize, movePaint);
        }

        // 3) Ball
        BallState ballState = drawBall(canvas, dotSize, movePaint);

        // 4-5) Sprites
        runRedFrameVisible = false;
        runBlueFrameVisible = false;
        kickRedFrameVisible = false;
        kickBlueFrameVisible = false;
        
        // Draw kick animation if active, but continue advancing/drawing run animation
        // so opponent movement can start mid-kick after the configured delay.
        if (kickAnimationActive) {
            drawKickAnimation(canvas, ballState.radius);
            drawRunAnimation(canvas, ballState.radius);
        } else {
            drawRunAnimation(canvas, ballState.radius);
        }
        
        drawIdlePlayers(canvas, ballState, currentTurn);


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

        // 6) Hand tutorial - show only for player turns (not Android turn)
        drawHandTutorial(canvas, currentTurn);
    }

    private void drawHandTutorial(Canvas canvas, int currentTurn) {
        /* Only show tutorial if enabled */
        if (!showHandTutorial) {
            return;
        }

        // Don't show during Android's turn (gameType 2, currentTurn 1)
        if (gameType == 2 && currentTurn == 1) {
            return;
        }

        // Don't show if there are no possible moves
        if (possibleMoves == null || possibleMoves.isEmpty()) {
            showHandTutorial = false;
            return;
        }

        int currentMoveCount = Moves.size();
        
        // Check if a new move has been made - if so, start a new cycle
        if (handTutorialLastMoveCount != currentMoveCount) {
            // New move detected - check if we should continue or stop
            if (handTutorialLastMoveCount != INITIAL_MOVE_COUNT) {
                // Not the first move - increment cycle counter
                handTutorialCycle++;
                
                // Save the updated cycle count to preferences
                prefs.edit().putInt(PREF_HAND_TUTORIAL_CYCLE_COUNT, handTutorialCycle).apply();
                
                // Get the next threshold to check
                int nextThreshold = prefs.getInt(PREF_HAND_TUTORIAL_NEXT_THRESHOLD, THRESHOLD_FIRST);
                
                // Check if we've reached a threshold where we should ask the user
                if (handTutorialCycle >= nextThreshold) {
                    // Stop showing tutorial temporarily and request dialog
                    showHandTutorial = false;
                    
                    // Request dialog from GameActivity
                    if (dialogCallback != null) {
                        dialogCallback.onRequestHandTutorialDialog();
                    }
                    
                    Log.d("TAG_Soccer", getClass().getSimpleName() + ".drawHandTutorial: Reached threshold " 
                        + nextThreshold + " cycles, requesting dialog");
                    return;
                }
            }
            
            // Reset for the new move/cycle
            handTutorialLastMoveCount = currentMoveCount;
            handTutorialPositionIndex = 0;
            handTutorialLastUpdateTime = SystemClock.uptimeMillis();
            Log.d("TAG_Soccer", getClass().getSimpleName() + ".drawHandTutorial: Starting cycle " 
                + (handTutorialCycle + 1) + " for move " + currentMoveCount 
                + " (player " + currentTurn + ")");
        }

        long currentTime = SystemClock.uptimeMillis();
        long elapsed = currentTime - handTutorialLastUpdateTime;

        // Check if it's time to move to the next position
        if (elapsed >= DURATION_SHOWING_HAND) {
            handTutorialLastUpdateTime = currentTime;
            handTutorialPositionIndex++;

            // Check if we've shown all positions in this cycle
            if (handTutorialPositionIndex >= possibleMoves.size()) {
                // Cycle complete, wait for next move to start new cycle
                // Position index will remain >= size, so the conditional check at the end of this method
                // will skip rendering until handTutorialPositionIndex is reset to 0 when a new move is detected
                /*Log.d("TAG_Soccer", getClass().getSimpleName() + ".drawHandTutorial: Cycle "
                    + (handTutorialCycle + 1) + " completed, waiting for next move");*/
                return;
            }
        }

        // Draw the hand at the current position
        if (handTutorialPositionIndex < possibleMoves.size()) {
            MoveTo currentMove = possibleMoves.get(handTutorialPositionIndex);
            
            // Calculate hand position - top of hand should touch the center of the possible move circle
            float circleCenterX = w2x(flipX(currentMove.X));
            float circleCenterY = h2y(flipY(currentMove.Y));
            
            // Hand size should match sprite size
            float handHeight = canvas.getHeight() * flSpriteSize;
            float handWidth = handHeight * handBitmap.getWidth() / (float) handBitmap.getHeight();
            
            // Position hand so its top touches the center of the circle
            float handLeft = circleCenterX - handWidth / 2f;
            float handRight = handLeft + handWidth;
            float handBottom = circleCenterY + handHeight;
            
            // Draw the hand
            RectF handDst = new RectF(handLeft, circleCenterY, handRight, handBottom);
            canvas.drawBitmap(handBitmap, null, handDst, null);
            
            /*Log.d("TAG_Soccer", getClass().getSimpleName() + ".drawHandTutorial: Drawing hand at position "
                + handTutorialPositionIndex + "/" + possibleMoves.size() 
                + ", cycle " + (handTutorialCycle + 1));*/
        }
    }
    
    // Method to be called when user responds to dialog
    public void onHandTutorialDialogResponse(boolean continueShowing) {
        if (continueShowing) {
            // User wants to continue - update threshold and re-enable tutorial
            int currentThreshold = prefs.getInt(PREF_HAND_TUTORIAL_NEXT_THRESHOLD, THRESHOLD_FIRST);
            int newThreshold;
            
            if (currentThreshold == THRESHOLD_FIRST) {
                newThreshold = THRESHOLD_SECOND;
            } else if (currentThreshold == THRESHOLD_SECOND) {
                newThreshold = THRESHOLD_THIRD;
            } else {
                // After third threshold, set a very high value (effectively infinite)
                newThreshold = Integer.MAX_VALUE;
            }
            
            prefs.edit().putInt(PREF_HAND_TUTORIAL_NEXT_THRESHOLD, newThreshold).apply();
            showHandTutorial = true;
            handTutorialLastUpdateTime = SystemClock.uptimeMillis();
            
            Log.d("TAG_Soccer", getClass().getSimpleName() + ".onHandTutorialDialogResponse: User chose to continue, next threshold: " + newThreshold);
        } else {
            // User wants to turn it off - disable in settings
            prefs.edit().putBoolean(PREF_HAND_TUTORIAL_ENABLED, false).apply();
            showHandTutorial = false;
            
            Log.d("TAG_Soccer", getClass().getSimpleName() + ".onHandTutorialDialogResponse: User chose to disable hand tutorial");
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
