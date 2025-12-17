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

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Log;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
    private final Paint pTutorialBalloon;
    private final Paint pTutorialBalloonBorder;
    private final Paint pTutorialBalloonText;
    private final Rect rField;
    private final Rect rText;
    private final Bitmap ballBitmap;
    private static final Bitmap[] EMPTY_BITMAP_ARRAY = new Bitmap[0];

    private volatile boolean spritesLoaded;
    private MoveTo pendingRunPrevious;
    private MoveTo pendingRunNext;

    private static final boolean SHOW_ANIMATION_LOG = false;

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
    private int numberMovesAnalyzed = 0;
    private final boolean animationsEnabled;
    private final boolean showIdlePlayerSprite;
    private final boolean handTutorialAllowed;
    private boolean handTutorialPending = false;
    private SpriteLoadListener spriteLoadListener;
    private int idlePlayerFrameIndex = 0;
    private long idlePlayerLastFrameTime = 0L;
    private boolean runAnimationActive = false;
    private boolean runAnimationStarting = false;
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
    private int runKickPausedFrames = 0;
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

    // Cached run animation state for separate blue/red rendering
    private Bitmap cachedRunRedFrame = null;
    private Bitmap cachedRunBlueFrame = null;
    private float cachedRedCenterX = 0f;
    private float cachedRedCenterY = 0f;
    private float cachedBlueCenterX = 0f;
    private float cachedBlueCenterY = 0f;
    private float cachedRedProximity = 0f;
    private float cachedBlueProximity = 0f;
    private float cachedRunSpriteHeight = 0f;
    private String activeRunDirectionLabel = "";
    private String cachedRedDirectionLabel = "";
    private String cachedBlueDirectionLabel = "";
    private int cachedRedFrameIndex = -1;
    private int cachedBlueFrameIndex = -1;

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

    // Parabolic trajectory state variables for Case 1
    // (north move where current and next move are for the same player)
    private boolean useParabolicTrajectory = false;
    private float runTargetGridX = 0f;
    private float runTargetGridY = 0f;
    private float parabolicSpikeGridX = 0f;
    // Previous grid positions for direction tracking during parabolic trajectory
    private float parabolicPrevRedGridX = Float.NaN;
    private float parabolicPrevRedGridY = Float.NaN;
    private float parabolicPrevBlueGridX = Float.NaN;
    private float parabolicPrevBlueGridY = Float.NaN;
    // Track previous frame indices to only update parabolic positions when frame advances
    private int parabolicPrevRedFrameIndex = -1;
    private int parabolicPrevBlueFrameIndex = -1;
    // Cached parabolic direction frame selections to prevent oscillation between redraws
    private Bitmap parabolicCachedRedFrame = null;
    private String parabolicCachedRedDirectionLabel = "";
    private Bitmap parabolicCachedBlueFrame = null;
    private String parabolicCachedBlueDirectionLabel = "";

    private static final int RUN_FRAME_COUNT = RunPlayerSprite.FRAME_COUNT;
    private static final float RUN_FRAME_STEP_DISTANCE = RUN_FRAME_COUNT > 0
            ? (float) (Math.sqrt(2.0) / RUN_FRAME_COUNT)
            : 0f;
    private static final float RUN_DESTINATION_EPSILON = 0.001f;
    private static final float ACTIVE_SPRITE_PROXIMITY_RATIO = 0.7f;
    private static final float PASSIVE_SPRITE_PROXIMITY_RATIO = 0.1f;
    private static final int RUN_DELAY_CYCLES_FROM_KICK = 5;
    private static final int RUN_DELAY_CYCLES_FROM_RUN = 2;
    private static final float SPRITE_DIRECTION_EPSILON = 0.0001f;
    private static final long BALL_DEFAULT_DURATION_MS = 10
            * RunPlayerSprite.FRAME_DURATION_MS;
    private static final int BALL_DELAY_FROM_KICK_START = 3;

    // Hand tutorial constants
    private static final int DURATION_SHOWING_HAND = 1000; // milliseconds
    private static final int INITIAL_MOVE_COUNT = -1; // Sentinel value for first tutorial display
    private static final String PREF_HAND_TUTORIAL_ENABLED = "show_hand_tutorial";
    private static final String PREF_HAND_TUTORIAL_CYCLE_COUNT = "hand_tutorial_cycle_count";
    private static final String PREF_HAND_TUTORIAL_NEXT_THRESHOLD = "hand_tutorial_next_threshold";
    private static final int THRESHOLD_FIRST = 3;
    private static final int THRESHOLD_SECOND = 10;
    private static final int THRESHOLD_THIRD = 20;
    
    // Tutorial balloon constants
    private static final float BALLOON_TEXT_SIZE_RATIO = 0.8f; // Ratio of field text size
    private static final float BALLOON_PADDING_RATIO = 0.5f; // Padding as ratio of text size
    private static final float BALLOON_CORNER_RATIO = 0.3f; // Corner radius as ratio of text size
    private static final float BALLOON_MARGIN_RATIO = 0.05f; // Margin from field edge as ratio
    private static final float BALLOON_MOVE_CLEARANCE_RATIO = 2.5f; // Clearance around possible moves
    private static final float BALLOON_MAX_WIDTH_RATIO = 0.9f; // Maximum balloon width as ratio of field width
    private static final float BALLOON_LINE_SPACING_RATIO = 0.2f; // Line spacing as ratio of line height
    
    // Tutorial message types
    public enum TutorialMessageType {
        INITIAL,              // Before first move
        BOUNCE_BORDER,        // Ball bounced on border
        BOUNCE_VISITED,       // Ball bounced on visited point
        NO_MOVES,             // No possible moves (loss)
        GOAL,                 // Scored in opponent's goal
        OWN_GOAL              // Scored in own goal
    }
    
    // Hand tutorial state
    private final Bitmap handBitmap;
    private final SharedPreferences prefs;
    private boolean showHandTutorial = false;
    private int handTutorialCycle = 0;
    private int handTutorialPositionIndex = 0;
    private long handTutorialLastUpdateTime = 0L;
    private int handTutorialLastMoveCount = INITIAL_MOVE_COUNT; // Track the number of moves when tutorial was last shown
    private HandTutorialDialogCallback dialogCallback = null;
    private TutorialMessageType currentTutorialMessage = TutorialMessageType.INITIAL;
    
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

    private void logAnimation(String message) {
        if (SHOW_ANIMATION_LOG) {
            Log.d("TAG_Soccer", message);
        }
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
                    if (current instanceof Activity activity) {
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

        // Tutorial balloon paints
        pTutorialBalloon = new Paint();
        pTutorialBalloon.setStyle(Paint.Style.FILL);
        pTutorialBalloon.setColor(Color.WHITE);
        pTutorialBalloon.setAntiAlias(true);

        pTutorialBalloonBorder = new Paint();
        pTutorialBalloonBorder.setStyle(Paint.Style.STROKE);
        pTutorialBalloonBorder.setColor(Color.BLACK);
        pTutorialBalloonBorder.setStrokeWidth(2f);
        pTutorialBalloonBorder.setAntiAlias(true);

        pTutorialBalloonText = new Paint();
        pTutorialBalloonText.setStyle(Paint.Style.FILL);
        pTutorialBalloonText.setColor(Color.BLACK);
        pTutorialBalloonText.setTextAlign(Paint.Align.CENTER);
        pTutorialBalloonText.setAntiAlias(true);

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

    // called from GameView
    public void setNumberMovesAnalyzed(int count) {
        numberMovesAnalyzed = count;
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
        if (spritesNotReadyForAnimations()) {
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

        if (spritesNotReadyForAnimations()) {
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

        Log.d("TAG_Soccer", getClass().getSimpleName() + ".startRunAnimationInternal: "
                + "previous.X=" + previous.X + ", previous.Y=" + previous.Y + ", previous.P=" + previous.P
                + ", next.X=" + next.X + ", next.Y=" + next.Y + ", next.P=" + next.P);

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

        if (canStartRun) {
            RunAnimationFrameSet frameSet = selectRunAnimationFrames(totalDeltaX, totalDeltaY, spriteDeltaX, spriteDeltaY);
            KickAnimationFrameSet kickFrameSet = selectKickAnimationFrames(totalDeltaX, totalDeltaY, spriteDeltaX, spriteDeltaY);
            if (!frameSet.isEmpty()) {
                activeRunRedPlayerFrames = frameSet.redFrames;
                activeRunBluePlayerFrames = frameSet.blueFrames;
                activeKickRedPlayerFrames = kickFrameSet.redFrames;
                activeKickBluePlayerFrames = kickFrameSet.blueFrames;
                activeRunDirectionLabel = frameSet.directionLabel;

                int availableRedFrames = Math.min(RUN_FRAME_COUNT, frameSet.redFrames.length);
                int availableBlueFrames = Math.min(RUN_FRAME_COUNT, frameSet.blueFrames.length);
                int availableFrames = Math.max(availableRedFrames, availableBlueFrames);

                int availableKickRedFrames = kickFrameSet.isNotEmpty() ? Math.min(KickPlayerSprite.FRAME_COUNT, kickFrameSet.redFrames.length) : 0;
                int availableKickBlueFrames = kickFrameSet.isNotEmpty() ? Math.min(KickPlayerSprite.FRAME_COUNT, kickFrameSet.blueFrames.length) : 0;

                if (availableFrames > 0) {
                    int frameLimit = availableFrames;
                    if (totalDistance > 0f && RUN_FRAME_STEP_DISTANCE > 0f) {
                        float framesForDistance = totalDistance / RUN_FRAME_STEP_DISTANCE;
                        frameLimit = Math.max(1, (int) Math.ceil(framesForDistance));
                    }

                    movementFrameCount = frameLimit;

                    runMovingPlayer = movingPlayer;
                    boolean nextMoveSamePlayer = next.P == movingPlayer;
                    runRedDelayFrames = 0;
                    runBlueDelayFrames = 0;
                    delayedOpponentPlayer = -1;
                    waitForKickToStartOpponentRun = false;

                    // Reset opponent's position to the start (ball) position when starting a new animation.
                    // This handles the case when user makes a move before the previous animation finishes.
                    if (movingPlayer == 0) {
                        // Red is kicking, reset blue's position to the ball position
                        runFinalBlueGridX = flippedStartX;
                        runFinalBlueGridY = flippedStartY;
                    } else if (movingPlayer == 1) {
                        // Blue is kicking, reset red's position to the ball position
                        runFinalRedGridX = flippedStartX;
                        runFinalRedGridY = flippedStartY;
                    }

                    if (runMovingPlayer == 1 && frameSet.redFrames.length > 0) {
                        if (!nextMoveSamePlayer) {
                            runRedDelayFrames = RUN_DELAY_CYCLES_FROM_KICK;
                        } else {
                            runRedDelayFrames = RUN_DELAY_CYCLES_FROM_RUN;
                        }
                        delayedOpponentPlayer = 0;
                        waitForKickToStartOpponentRun = true;
                    }

                    if (runMovingPlayer == 0 && frameSet.blueFrames.length > 0) {
                        if (!nextMoveSamePlayer) {
                            runBlueDelayFrames = RUN_DELAY_CYCLES_FROM_KICK;
                        } else {
                            runBlueDelayFrames = RUN_DELAY_CYCLES_FROM_RUN;
                        }
                        delayedOpponentPlayer = 1;
                        waitForKickToStartOpponentRun = true;
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

                // Detect conditions for parabolic trajectory:
                // 1. North move (y decreasing, x unchanged) AND next move is for player 0
                // 2. South move (y increasing, x unchanged) AND next move is for player 1
                int originalStartX = previous.X;
                int originalStartY = previous.Y;
                int originalTargetX = next.X;
                int originalTargetY = next.Y;
                boolean isNorthMove = (originalTargetX == originalStartX) && (originalTargetY < originalStartY);
                boolean isSouthMove = (originalTargetX == originalStartX) && (originalTargetY > originalStartY);
                boolean useParabolicForNorth = isNorthMove && (next.P == 0);
                boolean useParabolicForSouth = isSouthMove && (next.P == 1);

                if (useParabolicForNorth || useParabolicForSouth) {
                    useParabolicTrajectory = true;
                    // Store target grid positions for parabolic trajectory calculation
                    runTargetGridX = flippedTargetX;
                    runTargetGridY = flippedTargetY;
                    // Calculate x_s (spike x position in grid coordinates)
                    // x_s = x0 - 1 if on left side of field (curve left), x0 + 1 if on right side (curve right)
                    // This keeps the parabolic arc within the field bounds
                    // Use flippedStartX for the comparison to ensure consistent visual behavior after flipping
                    float halfFieldWidth = intFieldWidth / 2.0f;
                    if (flippedStartX < halfFieldWidth) {
                        // Left side of field: curve left by grid unit
                        parabolicSpikeGridX = flippedStartX - 0.4f;
                    } else {
                        // Right side of field: curve right by grid unit
                        parabolicSpikeGridX = flippedStartX + 0.4f;
                    }
                    // Initialize previous position tracking for direction-based frame selection
                    parabolicPrevRedGridX = flippedStartX;
                    parabolicPrevRedGridY = flippedStartY;
                    parabolicPrevBlueGridX = flippedStartX;
                    parabolicPrevBlueGridY = flippedStartY;
                    // Initialize previous frame indices for parabolic tracking
                    parabolicPrevRedFrameIndex = -1;
                    parabolicPrevBlueFrameIndex = -1;
                    // Reset cached parabolic frames
                    parabolicCachedRedFrame = null;
                    parabolicCachedRedDirectionLabel = "";
                    parabolicCachedBlueFrame = null;
                    parabolicCachedBlueDirectionLabel = "";
                    Log.d("TAG_Soccer", getClass().getSimpleName() + ".startRunAnimationInternal: "
                            + "Parabolic trajectory enabled: isNorthMove=" + isNorthMove
                            + ", isSouthMove=" + isSouthMove + ", nextPlayer=" + next.P
                            + ", spikeGridX=" + parabolicSpikeGridX
                            + ", startX=" + originalStartX + ", startY=" + originalStartY
                            + ", targetX=" + originalTargetX + ", targetY=" + originalTargetY);
                } else {
                    useParabolicTrajectory = false;
                    runTargetGridX = 0f;
                    runTargetGridY = 0f;
                    parabolicSpikeGridX = 0f;
                    parabolicPrevRedGridX = Float.NaN;
                    parabolicPrevRedGridY = Float.NaN;
                    parabolicPrevBlueGridX = Float.NaN;
                    parabolicPrevBlueGridY = Float.NaN;
                    parabolicPrevRedFrameIndex = -1;
                    parabolicPrevBlueFrameIndex = -1;
                    // Reset cached parabolic frames
                    parabolicCachedRedFrame = null;
                    parabolicCachedRedDirectionLabel = "";
                    parabolicCachedBlueFrame = null;
                    parabolicCachedBlueDirectionLabel = "";
                    Log.d("TAG_Soccer", getClass().getSimpleName() + ".startRunAnimationInternal: "
                            + "Parabolic trajectory NOT enabled: isNorthMove=" + isNorthMove
                            + ", isSouthMove=" + isSouthMove + ", nextPlayer=" + next.P
                            + ", startX=" + originalStartX + ", startY=" + originalStartY
                            + ", targetX=" + originalTargetX + ", targetY=" + originalTargetY);
                }
                runPlayerFrameIndex = 0;
                runPlayerLastFrameTime = 0L;  // Don't start time yet if we have kick animation
                idlePlayerFrameIndex = 0;
                runRedCompleted = false;
                runBlueCompleted = false;
                runKickPausedFrames = 0;

                    // Initialize kick animation for the moving player
                    if (kickFrameSet.isNotEmpty() && (movingPlayer == 0 || movingPlayer == 1)) {
                        kickAnimationActive = true;
                        kickPlayerFrameIndex = 0;
                        kickAnimationStartTime = SystemClock.uptimeMillis();
                        kickPlayerLastFrameTime = kickAnimationStartTime;
                        kickFrameLimit = movingPlayer == 0 ? availableKickRedFrames : availableKickBlueFrames;
                        kickRedCompleted = movingPlayer != 0;
                        kickBlueCompleted = movingPlayer != 1;
                        idlePlayerFrameIndex = 0;

                        // Start counting run delay from the kick start
                        runAnimationActive = true;
                        runAnimationStarting = true;
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
                        runAnimationStarting = true;
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
            runKickPausedFrames = 0;
            waitForKickToStartOpponentRun = false;
            delayedOpponentPlayer = -1;
            useParabolicTrajectory = false;
            runTargetGridX = 0f;
            runTargetGridY = 0f;
            parabolicSpikeGridX = 0f;
            parabolicPrevRedGridX = Float.NaN;
            parabolicPrevRedGridY = Float.NaN;
            parabolicPrevBlueGridX = Float.NaN;
            parabolicPrevBlueGridY = Float.NaN;
            parabolicPrevRedFrameIndex = -1;
            parabolicPrevBlueFrameIndex = -1;
            // Reset cached parabolic frames
            parabolicCachedRedFrame = null;
            parabolicCachedRedDirectionLabel = "";
            parabolicCachedBlueFrame = null;
            parabolicCachedBlueDirectionLabel = "";
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

    private boolean spritesNotReadyForAnimations() {
        return showIdlePlayerSprite && !spritesLoaded;
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
        Log.d("TAG_Soccer", getClass().getSimpleName() + ".stopRunAnimation: stopping run"
                + ", runMovingPlayer=" + runMovingPlayer
                + ", runPlayerFrameIndex=" + runPlayerFrameIndex
                + ", runRedCompleted=" + runRedCompleted
                + ", runBlueCompleted=" + runBlueCompleted
                + ", runFinalRedGridX=" + runFinalRedGridX
                + ", runFinalRedGridY=" + runFinalRedGridY
                + ", runFinalBlueGridX=" + runFinalBlueGridX
                + ", runFinalBlueGridY=" + runFinalBlueGridY);
        runAnimationActive = false;
        runAnimationStarting = false;
        runPlayerFrameIndex = 0;
        runPlayerLastFrameTime = 0L;
        idlePlayerLastFrameTime = referenceTime;
        idlePlayerFrameIndex = 0;
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
        runKickPausedFrames = 0;
        waitForKickToStartOpponentRun = false;
        delayedOpponentPlayer = -1;
        useParabolicTrajectory = false;
        runTargetGridX = 0f;
        runTargetGridY = 0f;
        parabolicSpikeGridX = 0f;
        parabolicPrevRedGridX = Float.NaN;
        parabolicPrevRedGridY = Float.NaN;
        parabolicPrevBlueGridX = Float.NaN;
        parabolicPrevBlueGridY = Float.NaN;
        parabolicPrevRedFrameIndex = -1;
        parabolicPrevBlueFrameIndex = -1;
        // Reset cached parabolic frames
        parabolicCachedRedFrame = null;
        parabolicCachedRedDirectionLabel = "";
        parabolicCachedBlueFrame = null;
        parabolicCachedBlueDirectionLabel = "";
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

        if (kickPlayerFrameIndex <= RUN_DELAY_CYCLES_FROM_KICK) {
            return;
        }

        // When nextMoveSamePlayer == true, the opponent's delay is RUN_DELAY_CYCLES_FROM_RUN.
        // In this case, don't release during kick - wait for the kick to end and let
        // stopKickAnimation handle the delay so it's counted from the kicking player's
        // run animation start.
        int opponentDelay = delayedOpponentPlayer == 0 ? runRedDelayFrames : runBlueDelayFrames;
        if (opponentDelay == RUN_DELAY_CYCLES_FROM_RUN) {
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

    private void drawKickAnimationBlue(Canvas canvas, float ballRadius) {
        if (!kickAnimationActive) {
            return;
        }
        
        Bitmap[] blueFrames = activeKickBluePlayerFrames != null ? activeKickBluePlayerFrames : EMPTY_BITMAP_ARRAY;
        int blueFrameCount = blueFrames.length;
        
        if (kickFrameLimit <= 0) {
            return;
        }

        Bitmap blueFrame = null;
        
        if (runMovingPlayer == 1 && blueFrameCount > 0 && !kickBlueCompleted) {
            blueFrame = getKickFrame(blueFrames, kickPlayerFrameIndex, blueFrameCount);
        }

        float spriteHeight = canvas.getHeight() * flSpriteSize;
        if (spriteHeight <= 0f) {
            return;
        }

        float ballCenterX = w2x(runStartGridX);
        float ballCenterY = h2y(runStartGridY);

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
                logAnimation(getClass().getSimpleName() + ".drawKickAnimationBlue: "
                        + "blueSprite left=" + blueLeft + " top=" + blueTop + " right=" + blueRight + " bottom=" + blueBottom + ", "
                        + "frameIndex=" + kickPlayerFrameIndex);
            }
        }
    }

    private void drawKickAnimationRed(Canvas canvas, float ballRadius) {
        if (!kickAnimationActive) {
            return;
        }
        
        Bitmap[] redFrames = activeKickRedPlayerFrames != null ? activeKickRedPlayerFrames : EMPTY_BITMAP_ARRAY;
        int redFrameCount = redFrames.length;
        
        if (kickFrameLimit <= 0) {
            return;
        }

        Bitmap redFrame = null;
        
        if (runMovingPlayer == 0 && redFrameCount > 0 && !kickRedCompleted) {
            redFrame = getKickFrame(redFrames, kickPlayerFrameIndex, redFrameCount);
        }

        float spriteHeight = canvas.getHeight() * flSpriteSize;
        if (spriteHeight <= 0f) {
            return;
        }

        float ballCenterX = w2x(runStartGridX);
        float ballCenterY = h2y(runStartGridY);

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
                logAnimation(getClass().getSimpleName() + ".drawKickAnimationRed: "
                        + "redSprite left=" + redLeft + " top=" + redTop + " right=" + redRight + " bottom=" + redBottom + ", "
                        + "frameIndex=" + kickPlayerFrameIndex);
            }
        }
    }

    private void updateKickAnimationState(Canvas canvas) {
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
            // First time drawing kick animation - reset frame index to start from 0
            kickPlayerFrameIndex = 0;
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

        // Update completion status for both players
        if (runMovingPlayer == 0 && redFrameCount > 0 && !kickRedCompleted) {
            if (kickPlayerFrameIndex >= redFrameCount - 1) {
                kickRedCompleted = true;
            }
        }
        
        if (runMovingPlayer == 1 && blueFrameCount > 0 && !kickBlueCompleted) {
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

        // Set visibility flags based on whether frames will be drawn
        // This must be done before updateIdlePlayersState checks these flags
        if (runMovingPlayer == 1 && blueFrameCount > 0 && !kickBlueCompleted) {
            Bitmap blueFrame = getKickFrame(blueFrames, kickPlayerFrameIndex, blueFrameCount);
            if (blueFrame != null && !blueFrame.isRecycled()) {
                kickBlueFrameVisible = true;
            }
        }
        if (runMovingPlayer == 0 && redFrameCount > 0 && !kickRedCompleted) {
            Bitmap redFrame = getKickFrame(redFrames, kickPlayerFrameIndex, redFrameCount);
            if (redFrame != null && !redFrame.isRecycled()) {
                kickRedFrameVisible = true;
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
                // nextMoveSamePlayer == true: opponent should start RUN_DELAY_CYCLES_FROM_RUN
                // frames after the kicking player's run animation starts. Since we're about to
                // reset the kicking player to start at frame 0, set the opponent's delay to
                // currentRunFrame + RUN_DELAY_CYCLES_FROM_RUN so they start at the right time.
                int currentRunFrame = Math.max(0, runPlayerFrameIndex);
                int opponentDelay = currentRunFrame + RUN_DELAY_CYCLES_FROM_RUN;
                if (delayedOpponentPlayer == 0) {
                    runRedDelayFrames = opponentDelay;
                } else if (delayedOpponentPlayer == 1) {
                    runBlueDelayFrames = opponentDelay;
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
            runAnimationStarting = true;
            runPlayerFrameIndex = 0;
            runPlayerLastFrameTime = referenceTime;
        } else {
            // Kick and run were started together. The opponent has been running during
            // the kick, so we must NOT reset runPlayerFrameIndex or we'll lose their
            // progress. Instead, adjust the kicking player's delay so they start from
            // frame 0 while the opponent continues from their current frame.
            int currentRunFrame = Math.max(0, runPlayerFrameIndex);
            if (runMovingPlayer == 0) {
                runRedDelayFrames = currentRunFrame;
            } else if (runMovingPlayer == 1) {
                runBlueDelayFrames = currentRunFrame;
            }
            runFrameLimit = runBaseFrameLimit + Math.max(runRedDelayFrames, runBlueDelayFrames);
        }
        runKickPausedFrames = 0;
        idlePlayerFrameIndex = 0;

        kickAnimationStartTime = 0L;
        kickCompletedThisFrame = true;
    }

    private void updateRunAnimationState(Canvas canvas) {
        // Reset cached state
        cachedRunRedFrame = null;
        cachedRunBlueFrame = null;
        cachedRedDirectionLabel = "";
        cachedBlueDirectionLabel = "";
        cachedRedFrameIndex = -1;
        cachedBlueFrameIndex = -1;

        if (!runAnimationActive) {
            return;
        }
        Bitmap[] redFrames = activeRunRedPlayerFrames != null ? activeRunRedPlayerFrames : EMPTY_BITMAP_ARRAY;
        Bitmap[] blueFrames = activeRunBluePlayerFrames != null ? activeRunBluePlayerFrames : EMPTY_BITMAP_ARRAY;
        int redFrameCount = redFrames.length;
        int blueFrameCount = blueFrames.length;
        int frameCount = Math.max(redFrameCount, blueFrameCount);
        int maxDelay = Math.max(runRedDelayFrames, runBlueDelayFrames);
        int frameLimit = Math.max(runFrameLimit, frameCount + maxDelay);
        if (frameLimit <= 0) {
            stopRunAnimation(SystemClock.uptimeMillis());
            return;
        }

        long now = SystemClock.uptimeMillis();
        
        // Reset frame index to start from 0 when animation starts (runAnimationStarting)
        // or on first draw after initialization (runPlayerLastFrameTime == 0L)
        if (runAnimationStarting || runPlayerLastFrameTime == 0L) {
            runPlayerFrameIndex = 0;
            runPlayerLastFrameTime = now;
            runAnimationStarting = false;
        }

        long elapsed = now - runPlayerLastFrameTime;
        if (RunPlayerSprite.FRAME_DURATION_MS > 0 && elapsed >= RunPlayerSprite.FRAME_DURATION_MS) {
            long framesToAdvance = elapsed / RunPlayerSprite.FRAME_DURATION_MS;
            runPlayerFrameIndex += (int) framesToAdvance;
            if (kickAnimationActive && runMovingPlayer >= 0) {
                runKickPausedFrames += (int) framesToAdvance;
            }

            int adjustedFrameLimit = frameLimit + runKickPausedFrames;
            if (runPlayerFrameIndex >= adjustedFrameLimit) {
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

        if (runKickPausedFrames > 0) {
            if (runMovingPlayer == 0) {
                redFrameIndex -= runKickPausedFrames;
            } else if (runMovingPlayer == 1) {
                blueFrameIndex -= runKickPausedFrames;
            }
        }

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
        String redDirectionLabel = redFrame != null ? activeRunDirectionLabel : "";
        String blueDirectionLabel = blueFrame != null ? activeRunDirectionLabel : "";

        // Avoid drawing the kicking player's run frames while the kick animation is active.
        // When kickCompletedThisFrame is true, the kick animation has ended and we should
        // start showing the run animation immediately (handled by setting runXxxFrameVisible
        // flags below to prevent idle animation from showing).
        if (kickAnimationActive) {
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
        cachedRunSpriteHeight = spriteHeight;

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
                if (runTotalDistance > 0f) {
                    if (useParabolicTrajectory) {
                        // For parabolic trajectory, endpoint is (x_0, y_1)
                        runFinalRedGridX = runStartGridX;
                        runFinalRedGridY = runTargetGridY;
                    } else {
                        runFinalRedGridX = runStartGridX + runDirectionX * runTotalDistance;
                        runFinalRedGridY = runStartGridY + runDirectionY * runTotalDistance;
                    }
                }
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
                if (runTotalDistance > 0f) {
                    if (useParabolicTrajectory) {
                        // For parabolic trajectory, endpoint is (x_0, y_1)
                        runFinalBlueGridX = runStartGridX;
                        runFinalBlueGridY = runTargetGridY;
                    } else {
                        runFinalBlueGridX = runStartGridX + runDirectionX * runTotalDistance;
                        runFinalBlueGridY = runStartGridY + runDirectionY * runTotalDistance;
                    }
                }
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

        cachedRedProximity = clamp(lerp(redStartProximity, redEndProximity, redAnimationProgress), 1f);
        cachedBlueProximity = blueProximity;

        float redGridX;
        float redGridY;
        float blueGridX;
        float blueGridY;

        Log.d("TAG_Soccer", getClass().getSimpleName() + ".updateRunAnimationState: "
                + "useParabolicTrajectory=" + useParabolicTrajectory
                + ", runStartGridX=" + runStartGridX + ", runStartGridY=" + runStartGridY
                + ", runTargetGridX=" + runTargetGridX + ", runTargetGridY=" + runTargetGridY);

        if (useParabolicTrajectory) {
            // Parabolic trajectory for north move where same player continues
            // Formula: x(y) = x_s + 4*(x_0 - x_s)/((y_1 - y_0)^2) * (y - (y_0 + y_1)/2)^2
            // This creates a sideways parabola passing through (x_0, y_0) and (x_0, y_1)
            // with its spike at (x_s, (y_0 + y_1)/2)

            float y0 = runStartGridY;
            float y1 = runTargetGridY;
            float x0 = runStartGridX;
            float xs = parabolicSpikeGridX;
            float yDelta = y1 - y0;
            float yMid = (y0 + y1) / 2.0f;

            // Red sprite position - use redAnimationProgress to interpolate Y
            redGridY = lerp(y0, y1, redAnimationProgress);
            float v = 4.0f * (x0 - xs) / (yDelta * yDelta);
            if (Math.abs(yDelta) > RUN_DESTINATION_EPSILON) {
                float yOffset = redGridY - yMid;
                redGridX = xs + v * (yOffset * yOffset);
            } else {
                redGridX = x0;
            }

            // Blue sprite position - use blueAnimationProgress to interpolate Y
            blueGridY = lerp(y0, y1, blueAnimationProgress);
            if (Math.abs(yDelta) > RUN_DESTINATION_EPSILON) {
                float yOffset = blueGridY - yMid;
                blueGridX = xs + v * (yOffset * yOffset);
            } else {
                blueGridX = x0;
            }
            Log.d("TAG_Soccer", getClass().getSimpleName() + ".updateRunAnimationState: "
                    + "PARABOLIC trajectory active: y0=" + y0 + ", y1=" + y1 + ", x0=" + x0 + ", xs=" + xs
                    + ", redProgress=" + redAnimationProgress + ", blueProgress=" + blueAnimationProgress
                    + ", redGridX=" + redGridX + ", redGridY=" + redGridY
                    + ", blueGridX=" + blueGridX + ", blueGridY=" + blueGridY);

            // For parabolic trajectory, dynamically select animation frames based on
            // the instantaneous direction of movement at each epsilon step.
            // Only recompute when the frame index advances to prevent oscillation
            // between redraws (screen refresh rate is faster than animation frame rate).
            if (redFrameIndex != parabolicPrevRedFrameIndex) {
                RunFrameSelection redParabolicFrame = getParabolicDirectionFrame(
                        redGridX, redGridY, parabolicPrevRedGridX, parabolicPrevRedGridY,
                        redFrameIndex, true);
                if (redParabolicFrame.frame != null) {
                    parabolicCachedRedFrame = redParabolicFrame.frame;
                    parabolicCachedRedDirectionLabel = redParabolicFrame.directionLabel;
                }
                parabolicPrevRedGridX = redGridX;
                parabolicPrevRedGridY = redGridY;
                parabolicPrevRedFrameIndex = redFrameIndex;
            }
            // Use cached values (only update redFrame/redDirectionLabel if we have valid cached data)
            // But respect the kick animation check that may have nullified redFrame earlier
            if (parabolicCachedRedFrame != null && redFrame != null) {
                redFrame = parabolicCachedRedFrame;
                redDirectionLabel = parabolicCachedRedDirectionLabel;
            }

            if (blueFrameIndex != parabolicPrevBlueFrameIndex) {
                RunFrameSelection blueParabolicFrame = getParabolicDirectionFrame(
                        blueGridX, blueGridY, parabolicPrevBlueGridX, parabolicPrevBlueGridY,
                        blueFrameIndex, false);
                if (blueParabolicFrame.frame != null) {
                    parabolicCachedBlueFrame = blueParabolicFrame.frame;
                    parabolicCachedBlueDirectionLabel = blueParabolicFrame.directionLabel;
                }
                parabolicPrevBlueGridX = blueGridX;
                parabolicPrevBlueGridY = blueGridY;
                parabolicPrevBlueFrameIndex = blueFrameIndex;
            }
            // Use cached values (only update blueFrame/blueDirectionLabel if we have valid cached data)
            // But respect the kick animation check that may have nullified blueFrame earlier
            if (parabolicCachedBlueFrame != null && blueFrame != null) {
                blueFrame = parabolicCachedBlueFrame;
                blueDirectionLabel = parabolicCachedBlueDirectionLabel;
            }
        } else {
            // Linear trajectory (original behavior)
            redGridX = runStartGridX + runDirectionX * redDistanceTraveled;
            redGridY = runStartGridY + runDirectionY * redDistanceTraveled;

            blueGridX = runStartGridX + runDirectionX * blueDistanceTraveled;
            blueGridY = runStartGridY + runDirectionY * blueDistanceTraveled;
        }

        if (redFrame == null) {
            redDirectionLabel = "";
            cachedRedFrameIndex = -1;
        } else {
            cachedRedFrameIndex = redFrameIndex;
        }

        if (blueFrame == null) {
            blueDirectionLabel = "";
            cachedBlueFrameIndex = -1;
        } else {
            cachedBlueFrameIndex = blueFrameIndex;
        }

        float redCenterX = w2x(redGridX);
        float redCenterY = h2y(redGridY);
        float blueCenterX = w2x(blueGridX);
        float blueCenterY = h2y(blueGridY);

        // Cache positions for separate drawing methods
        cachedRedCenterX = redCenterX;
        cachedRedCenterY = redCenterY;
        cachedBlueCenterX = blueCenterX;
        cachedBlueCenterY = blueCenterY;
        cachedRunRedFrame = redFrame;
        cachedRunBlueFrame = blueFrame;
        cachedRedDirectionLabel = redDirectionLabel;
        cachedBlueDirectionLabel = blueDirectionLabel;

        // Set visibility flags based on whether frames will be drawn
        // This must be done before updateIdlePlayersState checks these flags
        if (blueFrame != null && !blueFrame.isRecycled()) {
            runBlueFrameVisible = true;
        }
        if (redFrame != null && !redFrame.isRecycled()) {
            runRedFrameVisible = true;
        }
        // When kick just completed, the kicking player's run frame was nullified to avoid
        // visual overlap, but we should still mark the run as visible to prevent showing
        // the idle animation. The kicking player should start running immediately.
        if (kickCompletedThisFrame) {
            if (runMovingPlayer == 1 && blueHasFrames && !runBlueCompleted) {
                runBlueFrameVisible = true;
            }
            if (runMovingPlayer == 0 && redHasFrames && !runRedCompleted) {
                runRedFrameVisible = true;
            }
        }

        // Save current sprite positions for use in idle animation.
        // Only update the player that is actually moving so the idle animation for
        // the stationary opponent keeps using their previous position.
        if (redFrameIndex >= 0) {
            runFinalRedGridX = redGridX;
            runFinalRedGridY = redGridY;
        }
        if (blueFrameIndex >= 0) {
            runFinalBlueGridX = blueGridX;
            runFinalBlueGridY = blueGridY;
            Log.d("TAG_Soccer", getClass().getSimpleName() + ".updateRunAnimationState: "
                    + "updated runFinalBlueGridX=" + runFinalBlueGridX
                    + ", runFinalBlueGridY=" + runFinalBlueGridY
                    + ", blueFrameIndex=" + blueFrameIndex
                    + ", blueDistanceTraveled=" + blueDistanceTraveled
                    + ", runTotalDistance=" + runTotalDistance);
        }

        if ((!redHasFrames || runRedCompleted) && (!blueHasFrames || runBlueCompleted)) {
            stopRunAnimation(now);
        }
    }

    private void drawRunAnimationBlue(Canvas canvas, float ballRadius) {
        if (!runAnimationActive || runBlueCompleted) {
            return;
        }

        Bitmap blueFrame = cachedRunBlueFrame;
        float spriteHeight = cachedRunSpriteHeight;
        float blueCenterX = cachedBlueCenterX;
        float blueCenterY = cachedBlueCenterY;
        float blueProximity = cachedBlueProximity;

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
                logAnimation(getClass().getSimpleName() + ".drawRunAnimationBlue: "
                        + "blueSprite left=" + blueLeft + " top=" + blueTop + " right=" + blueRight + " bottom=" + blueBottom
                        + ", frameIndex=" + cachedBlueFrameIndex
                        + ", direction=" + cachedBlueDirectionLabel);
            }
        }
    }

    private void drawRunAnimationRed(Canvas canvas) {
        if (!runAnimationActive || runRedCompleted) {
            return;
        }

        Bitmap redFrame = cachedRunRedFrame;
        float spriteHeight = cachedRunSpriteHeight;
        float redCenterX = cachedRedCenterX;
        float redCenterY = cachedRedCenterY;
        float redProximity = cachedRedProximity;

        if (redFrame != null && !redFrame.isRecycled()) {
            float redCloseTop = redCenterY - spriteHeight * ACTIVE_SPRITE_PROXIMITY_RATIO;
            float redTop = lerp(redCenterY, redCloseTop, redProximity);
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
                logAnimation(getClass().getSimpleName() + ".drawRunAnimationRed: "
                        + "redSprite left=" + redLeft + " top=" + redTop + " right=" + redRight + " bottom=" + redBottom
                        + ", frameIndex=" + cachedRedFrameIndex
                        + ", direction=" + cachedRedDirectionLabel);
            }
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
        int steps = Math.max(0, frameIndex + stepOffset);
        float distance = RUN_FRAME_STEP_DISTANCE * steps;
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
        String directionLabel;
        if (degrees >= 157.5 && degrees < 202.5) {
            redFrames = runRedPlayerWestFrames;
            blueFrames = runBluePlayerWestFrames;
            directionLabel = "WEST";
        } else if (degrees >= 112.5 && degrees < 157.5) {
            redFrames = runRedPlayerWestNorthFrames;
            blueFrames = runBluePlayerWestNorthFrames;
            directionLabel = "WEST_NORTH";
        } else if (degrees >= 67.5 && degrees < 112.5) {
            redFrames = runRedPlayerNorthFrames;
            blueFrames = runBluePlayerNorthFrames;
            directionLabel = "NORTH";
        } else if (degrees >= 22.5 && degrees < 67.5) {
            redFrames = runRedPlayerEastNorthFrames;
            blueFrames = runBluePlayerEastNorthFrames;
            directionLabel = "EAST_NORTH";
        } else if (degrees >= 337.5 || degrees < 22.5) {
            redFrames = runRedPlayerEastFrames;
            blueFrames = runBluePlayerEastFrames;
            directionLabel = "EAST";
        } else if (degrees >= 292.5 && degrees < 337.5) {
            redFrames = runRedPlayerEastSouthFrames;
            blueFrames = runBluePlayerEastSouthFrames;
            directionLabel = "EAST_SOUTH";
        } else if (degrees >= 247.5 && degrees < 292.5) {
            redFrames = runRedPlayerSouthFrames;
            blueFrames = runBluePlayerSouthFrames;
            directionLabel = "SOUTH";
        } else {
            redFrames = runRedPlayerSouthWestFrames;
            blueFrames = runBluePlayerSouthWestFrames;
            directionLabel = "SOUTH_WEST";
        }

        if ((redFrames == null || redFrames.length == 0)
                && (blueFrames == null || blueFrames.length == 0)) {
            return RunAnimationFrameSet.EMPTY;
        }

        return new RunAnimationFrameSet(redFrames, blueFrames, directionLabel);
    }

    private Bitmap getRunFrame(Bitmap[] frames, int frameIndex, int frameCount) {
        if (frames == null || frames.length == 0 || frameCount <= 0) {
            return null;
        }

        int cycleLength = Math.min(frameCount, frames.length);
        int safeIndex = ((frameIndex % cycleLength) + cycleLength) % cycleLength;
        if (safeIndex < 0 || safeIndex >= frames.length) {
            return null;
        }

        return frames[safeIndex];
    }

    /**
     * Gets the appropriate run animation frame for a player during parabolic trajectory,
     * based on the instantaneous direction of movement.
     *
     * @param currentX current grid X position
     * @param currentY current grid Y position
     * @param prevX previous grid X position
     * @param prevY previous grid Y position
     * @param frameIndex current animation frame index
     * @param isRedPlayer true for red player, false for blue player
     * @return the appropriate frame selection, or {@link RunFrameSelection#EMPTY} if no frame should be displayed
     */
    private RunFrameSelection getParabolicDirectionFrame(float currentX, float currentY,
                                                         float prevX, float prevY,
                                                         int frameIndex, boolean isRedPlayer) {
        if (frameIndex < 0 || Float.isNaN(prevX) || Float.isNaN(prevY)) {
            return RunFrameSelection.EMPTY;
        }

        float deltaX = currentX - prevX;
        float deltaY = currentY - prevY;

        if (Math.abs(deltaX) <= SPRITE_DIRECTION_EPSILON && Math.abs(deltaY) <= SPRITE_DIRECTION_EPSILON) {
            return RunFrameSelection.EMPTY;
        }

        RunAnimationFrameSet frameSet = selectRunAnimationFrames(deltaX, deltaY, deltaX, deltaY);
        Bitmap[] frames = isRedPlayer ? frameSet.redFrames : frameSet.blueFrames;

        if (frameSet.isEmpty() || frames.length == 0) {
            return RunFrameSelection.EMPTY;
        }

        Bitmap frame = getRunFrame(frames, frameIndex, frames.length);
        if (frame == null) {
            return RunFrameSelection.EMPTY;
        }

        return new RunFrameSelection(frame, frameSet.directionLabel);
    }

    private record RunAnimationFrameSet(Bitmap[] redFrames, Bitmap[] blueFrames,
                                        String directionLabel) {
            static final RunAnimationFrameSet EMPTY = new RunAnimationFrameSet(EMPTY_BITMAP_ARRAY, EMPTY_BITMAP_ARRAY, "");

            private RunAnimationFrameSet(Bitmap[] redFrames, Bitmap[] blueFrames, String directionLabel) {
                this.redFrames = redFrames != null ? redFrames : EMPTY_BITMAP_ARRAY;
                this.blueFrames = blueFrames != null ? blueFrames : EMPTY_BITMAP_ARRAY;
                this.directionLabel = directionLabel != null ? directionLabel : "";
            }

            boolean isEmpty() {
                return redFrames.length == 0 && blueFrames.length == 0;
            }
        }

    private record RunFrameSelection(Bitmap frame, String directionLabel) {
            static final RunFrameSelection EMPTY = new RunFrameSelection(null, "");

            private RunFrameSelection(Bitmap frame, String directionLabel) {
                this.frame = frame;
                this.directionLabel = directionLabel != null ? directionLabel : "";
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

    private record KickAnimationFrameSet(Bitmap[] redFrames, Bitmap[] blueFrames) {
            static final KickAnimationFrameSet EMPTY = new KickAnimationFrameSet(EMPTY_BITMAP_ARRAY, EMPTY_BITMAP_ARRAY);

            private KickAnimationFrameSet(Bitmap[] redFrames, Bitmap[] blueFrames) {
                this.redFrames = redFrames != null ? redFrames : EMPTY_BITMAP_ARRAY;
                this.blueFrames = blueFrames != null ? blueFrames : EMPTY_BITMAP_ARRAY;
            }

            boolean isNotEmpty() {
                return redFrames.length != 0 || blueFrames.length != 0;
            }
        }

    private record BallState(float centerX, float centerY, float radius) {
    }

    /**
         * Represents a drawable element with its bottom position for sorting.
         * Elements are sorted by bottom value - lower values are drawn first (appear behind).
         */
        private record DrawableElement(int type, float bottom) implements Comparable<DrawableElement> {
            static final int TYPE_BALL = 0;
            static final int TYPE_BLUE = 1;
            static final int TYPE_RED = 2;

        @Override
            public int compareTo(DrawableElement other) {
                return Float.compare(this.bottom, other.bottom);
            }
        }

    /**
     * Computes the bottom value for the blue sprite based on the current animation state.
     * @param canvas The canvas being drawn to
     * @param ballRadius The ball radius
     * @return The bottom coordinate of the blue sprite, or Float.MIN_VALUE if not visible
     */
    private float computeBlueBottom(Canvas canvas, float ballRadius) {
        float spriteHeight = canvas.getHeight() * flSpriteSize;
        if (spriteHeight <= 0f) {
            return Float.MIN_VALUE;
        }

        // Check kick animation first
        if (kickAnimationActive && runMovingPlayer == 1 && !kickBlueCompleted) {
            Bitmap[] blueFrames = activeKickBluePlayerFrames != null ? activeKickBluePlayerFrames : EMPTY_BITMAP_ARRAY;
            if (blueFrames.length > 0 && kickFrameLimit > 0) {
                Bitmap blueFrame = getKickFrame(blueFrames, kickPlayerFrameIndex, blueFrames.length);
                if (blueFrame != null && !blueFrame.isRecycled()) {
                    float ballCenterY = h2y(runStartGridY);
                    float blueProximity = runStartBlueCloser ? 1f : 0f;
                    float blueFarBottom = ballCenterY - ballRadius;
                    float blueCloseBottom = ballCenterY + spriteHeight * (1f - ACTIVE_SPRITE_PROXIMITY_RATIO);
                    float blueBottom = lerp(blueFarBottom, blueCloseBottom, blueProximity);
                    if (blueBottom > canvas.getHeight()) {
                        blueBottom = canvas.getHeight();
                    }
                    return blueBottom;
                }
            }
        }

        // Check run animation
        if (runAnimationActive && cachedRunBlueFrame != null && !cachedRunBlueFrame.isRecycled()) {
            float blueCenterY = cachedBlueCenterY;
            float blueProximity = cachedBlueProximity;
            float blueFarBottom = blueCenterY - ballRadius;
            float blueCloseBottom = blueCenterY + spriteHeight * (1f - ACTIVE_SPRITE_PROXIMITY_RATIO);
            float blueBottom = lerp(blueFarBottom, blueCloseBottom, blueProximity);
            if (blueBottom > canvas.getHeight()) {
                blueBottom = canvas.getHeight();
            }
            return blueBottom;
        }

        // Check idle animation
        if (cachedShouldDrawIdleBlue && idleBluePlayerFrames.length > 0) {
            Bitmap spriteFrame = idleBluePlayerFrames[idlePlayerFrameIndex % idleBluePlayerFrames.length];
            if (spriteFrame != null && !spriteFrame.isRecycled()) {
                float idleBlueCenterY = cachedIdleBlueCenterY;
                boolean blueShouldBeCloser = cachedBlueShouldBeCloser;
                return blueShouldBeCloser
                        ? idleBlueCenterY + spriteHeight * (1 - ACTIVE_SPRITE_PROXIMITY_RATIO)
                        : idleBlueCenterY + PASSIVE_SPRITE_PROXIMITY_RATIO * spriteHeight - cachedIdleBallRadius;
            }
        }

        return Float.MIN_VALUE;
    }

    /**
     * Computes the bottom value for the red sprite based on the current animation state.
     * @param canvas The canvas being drawn to
     * @param ballRadius The ball radius
     * @return The bottom coordinate of the red sprite, or Float.MIN_VALUE if not visible
     */
    private float computeRedBottom(Canvas canvas, float ballRadius) {
        float spriteHeight = canvas.getHeight() * flSpriteSize;
        if (spriteHeight <= 0f) {
            return Float.MIN_VALUE;
        }

        // Check kick animation first
        if (kickAnimationActive && runMovingPlayer == 0 && !kickRedCompleted) {
            Bitmap[] redFrames = activeKickRedPlayerFrames != null ? activeKickRedPlayerFrames : EMPTY_BITMAP_ARRAY;
            if (redFrames.length > 0 && kickFrameLimit > 0) {
                Bitmap redFrame = getKickFrame(redFrames, kickPlayerFrameIndex, redFrames.length);
                if (redFrame != null && !redFrame.isRecycled()) {
                    float ballCenterY = h2y(runStartGridY);
                    float redProximity = runStartRedCloser ? 1f : 0f;
                    float redFarTop = ballCenterY + ballRadius;
                    float redCloseTop = ballCenterY - spriteHeight * ACTIVE_SPRITE_PROXIMITY_RATIO;
                    float redTop = lerp(redFarTop, redCloseTop, redProximity);
                    float redBottom = redTop + spriteHeight;
                    if (redBottom > canvas.getHeight()) {
                        redBottom = canvas.getHeight();
                    }
                    return redBottom;
                }
            }
        }

        // Check run animation
        if (runAnimationActive && cachedRunRedFrame != null && !cachedRunRedFrame.isRecycled()) {
            float redFarTop = cachedRedCenterY;
            float redProximity = cachedRedProximity;
            float redCloseTop = redFarTop - spriteHeight * ACTIVE_SPRITE_PROXIMITY_RATIO;
            float redTop = lerp(redFarTop, redCloseTop, redProximity);
            float redBottom = redTop + spriteHeight;
            if (redBottom > canvas.getHeight()) {
                redBottom = canvas.getHeight();
            }
            return redBottom;
        }

        // Check idle animation
        if (cachedShouldDrawIdleRed && idleRedPlayerFrames.length > 0) {
            Bitmap spriteFrame = idleRedPlayerFrames[idlePlayerFrameIndex % idleRedPlayerFrames.length];
            if (spriteFrame != null && !spriteFrame.isRecycled()) {
                float idleRedCenterY = cachedIdleRedCenterY;
                boolean redShouldBeCloser = cachedRedShouldBeCloser;
                float spriteTop = redShouldBeCloser
                        ? idleRedCenterY - spriteHeight * ACTIVE_SPRITE_PROXIMITY_RATIO
                        : idleRedCenterY;
                float spriteBottom = spriteTop + spriteHeight;
                if (spriteBottom > canvas.getHeight()) {
                    spriteBottom = canvas.getHeight();
                }
                return spriteBottom;
            }
        }

        return Float.MIN_VALUE;
    }

    /**
     * Draws the blue sprite (kick, run, or idle animation based on current state).
     * Note: Each individual draw method has early-return checks, so calling all three
     * is efficient and only the appropriate animation will actually render.
     */
    private void drawBlueSprite(Canvas canvas, float ballRadius) {
        drawKickAnimationBlue(canvas, ballRadius);
        drawRunAnimationBlue(canvas, ballRadius);
        drawIdleBluePlayer(canvas);
    }

    /**
     * Draws the red sprite (kick, run, or idle animation based on current state).
     * Note: Each individual draw method has early-return checks, so calling all three
     * is efficient and only the appropriate animation will actually render.
     */
    private void drawRedSprite(Canvas canvas, float ballRadius) {
        drawKickAnimationRed(canvas, ballRadius);
        drawRunAnimationRed(canvas);
        drawIdleRedPlayer(canvas);
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

    private BallState computeBallState(float dotSize) {
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
                    && kickPlayerFrameIndex <= ballKickDelayFrames;

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

        lastRunBallRadius = radius;

        return new BallState(ballCenterX, ballCenterY, radius);
    }

    private void renderBall(Canvas canvas, BallState ballState, Paint movePaint) {
        float ballCenterX = ballState.centerX;
        float ballCenterY = ballState.centerY;
        float radius = ballState.radius;
        float radiusBackground = radius * 0.8f;

        canvas.drawCircle(ballCenterX, ballCenterY, radiusBackground, movePaint);

        RectF dst = new RectF(ballCenterX - radius, ballCenterY - radius, ballCenterX + radius, ballCenterY + radius);
        canvas.drawBitmap(ballBitmap, null, dst, null);

        logAnimation(getClass().getSimpleName() + ".renderBall: "
                + "ballCenterX=" + ballCenterX + " ballCenterY=" + ballCenterY);
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

    // Cached idle player state for separate blue/red rendering
    private boolean cachedShouldDrawIdleBlue = false;
    private boolean cachedShouldDrawIdleRed = false;
    private float cachedIdleBlueCenterX = 0f;
    private float cachedIdleBlueCenterY = 0f;
    private float cachedIdleRedCenterX = 0f;
    private float cachedIdleRedCenterY = 0f;
    private boolean cachedBlueShouldBeCloser = false;
    private boolean cachedRedShouldBeCloser = false;
    private float cachedIdleSpriteHeight = 0f;
    private float cachedIdleBallRadius = 0f;

    private void updateIdlePlayersState(Canvas canvas, BallState ballState, int currentTurn) {
        // Reset cached state
        cachedShouldDrawIdleBlue = false;
        cachedShouldDrawIdleRed = false;

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
            // First time drawing idle animation - reset frame index to start from 0
            idlePlayerFrameIndex = 0;
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

        boolean shouldDrawIdleBlue = !kickBlueFrameVisible && (!runActive
                || runBlueCompleted
                || (isRedMoving && blueDelayActive)
                || (!isRedMoving && !isBlueMoving)
                || (isBlueMoving && !runBlueFrameVisible));
        boolean shouldDrawIdleRed = !kickRedFrameVisible && (!runActive
                || runRedCompleted
                || (isBlueMoving && redDelayActive)
                || (!isBlueMoving && !isRedMoving)
                || (isRedMoving && !runRedFrameVisible));
        boolean shouldAdvanceIdle = shouldDrawIdleBlue || shouldDrawIdleRed;

        if (!shouldDrawIdleBlue && !runBlueFrameVisible && !kickBlueFrameVisible) {
            Log.d("TAG_Soccer", getClass().getSimpleName() + ".updateIdlePlayersState: "
                    + "Skipping idle blue; "
                    + ", runPlayerFrameIndex=" + runPlayerFrameIndex
                    + ", runFrameLimit=" + runFrameLimit);
        }

        if (!shouldDrawIdleRed && !runRedFrameVisible && !kickRedFrameVisible) {
            Log.d("TAG_Soccer", getClass().getSimpleName() + ".updateIdlePlayersState: "
                    + "Skipping idle red; runActive=" + runActive
                    + ", isRedMoving=" + isRedMoving
                    + ", isBlueMoving=" + isBlueMoving
                    + ", redDelayActive=" + redDelayActive
                    + ", runPlayerFrameIndex=" + runPlayerFrameIndex
                    + ", runFrameLimit=" + runFrameLimit);
        }

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
        cachedIdleSpriteHeight = spriteHeight;
        cachedIdleBallRadius = ballState.radius;

        float ballCenterX = ballState.centerX;
        float ballCenterY = ballState.centerY;

        // Compute blue idle state
        boolean blueShouldBeCloser = currentTurn == 1;
        if (runActive && runMovingPlayer == 0 && blueDelayActive) {
            blueShouldBeCloser = false;
        }
        if (kickAnimationActive && runMovingPlayer == 0) {
            blueShouldBeCloser = false;
        }
        // When blue is the moving player (kicker) and run animation is active, blue should stay closer
        if (runActive && runMovingPlayer == 1) {
            blueShouldBeCloser = true;
        }
        float idleBlueCenterX;
        float idleBlueCenterY;
        if (!Float.isNaN(runFinalBlueGridX) && !Float.isNaN(runFinalBlueGridY)) {
            idleBlueCenterX = w2x(runFinalBlueGridX);
            idleBlueCenterY = h2y(runFinalBlueGridY);
        } else if (lastBlueMove != null) {
            idleBlueCenterX = w2x(flipX(lastBlueMove.X));
            idleBlueCenterY = h2y(flipY(lastBlueMove.Y));
        } else {
            idleBlueCenterX = ballCenterX;
            idleBlueCenterY = ballCenterY;
        }

        if (blueShouldBeCloser) {
            idleBlueCenterX = ballCenterX;
        }

        // Compute red idle state
        boolean redShouldBeCloser = currentTurn == 0;
        if (runActive && runMovingPlayer == 1 && redDelayActive) {
            redShouldBeCloser = false;
        }
        if (kickAnimationActive && runMovingPlayer == 1) {
            redShouldBeCloser = false;
        }
        // When red is the moving player (kicker) and run animation is active, red should stay closer
        if (runActive && runMovingPlayer == 0) {
            redShouldBeCloser = true;
        }
        float idleRedCenterX;
        float idleRedCenterY;
        if (!Float.isNaN(runFinalRedGridX) && !Float.isNaN(runFinalRedGridY)) {
            idleRedCenterX = w2x(runFinalRedGridX);
            idleRedCenterY = h2y(runFinalRedGridY);
        } else if (lastRedMove != null) {
            idleRedCenterX = w2x(flipX(lastRedMove.X));
            idleRedCenterY = h2y(flipY(lastRedMove.Y));
        } else {
            idleRedCenterX = ballCenterX;
            idleRedCenterY = ballCenterY;
        }

        if (redShouldBeCloser) {
            idleRedCenterX = ballCenterX;
        }

        // Cache all state
        cachedShouldDrawIdleBlue = shouldDrawIdleBlue && blueFrameCount > 0;
        cachedShouldDrawIdleRed = shouldDrawIdleRed && redFrameCount > 0;
        cachedIdleBlueCenterX = idleBlueCenterX;
        cachedIdleBlueCenterY = idleBlueCenterY;
        cachedIdleRedCenterX = idleRedCenterX;
        cachedIdleRedCenterY = idleRedCenterY;
        cachedBlueShouldBeCloser = blueShouldBeCloser;
        cachedRedShouldBeCloser = redShouldBeCloser;
    }

    private void drawIdleBluePlayer(Canvas canvas) {
        if (!cachedShouldDrawIdleBlue) {
            return;
        }

        int blueFrameCount = idleBluePlayerFrames.length;
        if (blueFrameCount == 0) {
            return;
        }

        Bitmap spriteFrame = idleBluePlayerFrames[idlePlayerFrameIndex % blueFrameCount];
        if (spriteFrame == null || spriteFrame.isRecycled()) {
            return;
        }

        float spriteHeight = cachedIdleSpriteHeight;
        float idleBlueCenterX = cachedIdleBlueCenterX;
        float idleBlueCenterY = cachedIdleBlueCenterY;
        boolean blueShouldBeCloser = cachedBlueShouldBeCloser;

        float spriteBottom = blueShouldBeCloser
                ? idleBlueCenterY + spriteHeight * (1 - ACTIVE_SPRITE_PROXIMITY_RATIO)
                : idleBlueCenterY + PASSIVE_SPRITE_PROXIMITY_RATIO * spriteHeight - cachedIdleBallRadius;
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
            logAnimation(getClass().getSimpleName() + ".drawIdleBluePlayer: "
                    + "blueSprite left=" + spriteLeft + " top=" + spriteTop + " right=" + spriteRight + " bottom=" + spriteBottom + ", "
                    + "frameIndex=" + idlePlayerFrameIndex);
            logAnimation(getClass().getSimpleName() + ".drawIdleBluePlayer: "
                    + "idleBlueCenterX=" + idleBlueCenterX
                    + ", idleBlueCenterY=" + idleBlueCenterY
                    + ", runFinalBlueGridX=" + runFinalBlueGridX
                    + ", runFinalBlueGridY=" + runFinalBlueGridY
                    + ", blueShouldBeCloser=" + blueShouldBeCloser
                    + ", runAnimationActive=" + runAnimationActive
                    + ", runMovingPlayer=" + runMovingPlayer);
        }
    }

    private void drawIdleRedPlayer(Canvas canvas) {
        if (!cachedShouldDrawIdleRed) {
            return;
        }

        int redFrameCount = idleRedPlayerFrames.length;
        if (redFrameCount == 0) {
            return;
        }

        Bitmap spriteFrame = idleRedPlayerFrames[idlePlayerFrameIndex % redFrameCount];
        if (spriteFrame == null || spriteFrame.isRecycled()) {
            return;
        }

        float spriteHeight = cachedIdleSpriteHeight;
        float idleRedCenterX = cachedIdleRedCenterX;
        float idleRedCenterY = cachedIdleRedCenterY;
        boolean redShouldBeCloser = cachedRedShouldBeCloser;

        float spriteTop = redShouldBeCloser
                ? idleRedCenterY - spriteHeight * ACTIVE_SPRITE_PROXIMITY_RATIO
                : idleRedCenterY;
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
            logAnimation(getClass().getSimpleName() + ".drawIdleRedPlayer: "
                    + "redSprite left=" + spriteLeft + " top=" + spriteTop + " right=" + spriteRight + " bottom=" + spriteBottom + ", "
                    + "frameIndex=" + idlePlayerFrameIndex);
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

        // Drawing order is now dynamic based on bottom values.
        // Elements with lower bottom values are drawn first (appear behind).
        
        // Reset visibility flags
        runRedFrameVisible = false;
        runBlueFrameVisible = false;
        kickRedFrameVisible = false;
        kickBlueFrameVisible = false;
        
        // First, compute ball state (needed for sprite positioning)
        BallState ballState = computeBallState(dotSize);
        
        // Update all animation states with correct ball position
        if (kickAnimationActive) {
            updateKickAnimationState(canvas);
        }
        updateRunAnimationState(canvas);
        updateIdlePlayersState(canvas, ballState, currentTurn);
        
        // Compute bottom values for all drawable elements
        float ballBottom = ballState.centerY + ballState.radius;
        float blueBottom = computeBlueBottom(canvas, ballState.radius);
        float redBottom = computeRedBottom(canvas, ballState.radius);
        
        // Cache bottom values for external access if needed
        // Cached bottom values for drawing order calculation

        // Create array of drawable elements and sort by bottom value
        // Elements with lower bottom values are drawn first (appear behind)
        DrawableElement[] elements = new DrawableElement[3];
        elements[0] = new DrawableElement(DrawableElement.TYPE_BALL, ballBottom);
        elements[1] = new DrawableElement(DrawableElement.TYPE_BLUE, blueBottom);
        elements[2] = new DrawableElement(DrawableElement.TYPE_RED, redBottom);
        Arrays.sort(elements);
        
        // Draw elements in sorted order (lower bottom values first)
        for (DrawableElement element : elements) {
            switch (element.type) {
                case DrawableElement.TYPE_BALL:
                    renderBall(canvas, ballState, movePaint);
                    break;
                case DrawableElement.TYPE_BLUE:
                    drawBlueSprite(canvas, ballState.radius);
                    break;
                case DrawableElement.TYPE_RED:
                    drawRedSprite(canvas, ballState.radius);
                    break;
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
                String formattedNumber = NumberFormat.getInstance(Locale.getDefault()).format(numberMovesAnalyzed);
                textTop = context.getString(R.string.field_thinking, formattedNumber);
                Log.d("TAG_Soccer", "Field.draw: Updated thinking text with numberMovesAnalyzed=" + numberMovesAnalyzed);
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

                logAnimation(getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": textBottom: " + textBottom);

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

                    logAnimation(getClass().getSimpleName() + ".drawHandTutorial: Reached threshold "
                        + nextThreshold + " cycles, requesting dialog");
                    return;
                }
            }
            
            // Reset for the new move/cycle
            handTutorialLastMoveCount = currentMoveCount;
            handTutorialPositionIndex = 0;
            handTutorialLastUpdateTime = SystemClock.uptimeMillis();
            logAnimation(getClass().getSimpleName() + ".drawHandTutorial: Starting cycle "
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
                // Animation cycle complete - hand stops drawing but balloon remains visible until user moves
                /*Log.d("TAG_Soccer", getClass().getSimpleName() + ".drawHandTutorial: Cycle "
                    + (handTutorialCycle + 1) + " completed, balloon continues to show");*/
            }
        }

        // Draw the hand at the current position
        if (handTutorialPositionIndex < possibleMoves.size()) {
            MoveTo currentMove = possibleMoves.get(handTutorialPositionIndex);
            
            // Calculate hand position - top of hand should touch the center of the possible move circle
            float circleCenterX = w2x(flipX(currentMove.X));
            float circleCenterY = h2y(flipY(currentMove.Y));
            
            // Hand size should match sprite size
            RectF handDst = getRectF(canvas, circleCenterX, circleCenterY);
            canvas.drawBitmap(handBitmap, null, handDst, null);
            
            /*Log.d("TAG_Soccer", getClass().getSimpleName() + ".drawHandTutorial: Drawing hand at position "
                + handTutorialPositionIndex + "/" + possibleMoves.size() 
                + ", cycle " + (handTutorialCycle + 1));*/
        }
        
        // Draw the tutorial balloon message
        drawTutorialBalloon(canvas);
    }

    @NonNull
    private RectF getRectF(Canvas canvas, float circleCenterX, float circleCenterY) {
        float handHeight = canvas.getHeight() * flSpriteSize;
        float handWidth = handHeight * handBitmap.getWidth() / (float) handBitmap.getHeight();

        // Position hand so its top touches the center of the circle
        float handLeft = circleCenterX - handWidth / 2f;
        float handRight = handLeft + handWidth;
        float handBottom = circleCenterY + handHeight;

        // Draw the hand
        return new RectF(handLeft, circleCenterY, handRight, handBottom);
    }

    /**
     * Draws a white balloon with tutorial message positioned to avoid covering possible moves
     */
    private void drawTutorialBalloon(Canvas canvas) {
        if (possibleMoves == null || possibleMoves.isEmpty()) {
            return;
        }

        // Get the tutorial message from resources based on current message type
        String message = getTutorialMessage();
        
        // Set text size based on canvas size
        boolean isPortrait = rField.height() > rField.width();
        float textSize = isPortrait ? rField.height() * flText * BALLOON_TEXT_SIZE_RATIO : rField.width() * flText * BALLOON_TEXT_SIZE_RATIO;
        pTutorialBalloonText.setTextSize(textSize);
        
        // Calculate padding
        float padding = textSize * BALLOON_PADDING_RATIO;
        
        // Calculate maximum balloon width (90% of field width to ensure it fits on screen)
        float fieldWidth = rField.width();
        float maxBalloonWidth = fieldWidth * BALLOON_MAX_WIDTH_RATIO;
        
        // Wrap text into multiple lines if needed
        ArrayList<String> wrappedLines = wrapText(message, pTutorialBalloonText, maxBalloonWidth - padding * 2);
        
        // Calculate line height once (all lines use the same paint)
        Rect textBounds = new Rect();
        pTutorialBalloonText.getTextBounds("M", 0, 1, textBounds);
        float lineHeight = textBounds.height();
        
        // Calculate balloon width based on the longest line
        float balloonWidth = 0;
        for (String line : wrappedLines) {
            pTutorialBalloonText.getTextBounds(line, 0, line.length(), textBounds);
            float lineWidth = textBounds.width();
            if (lineWidth > balloonWidth) {
                balloonWidth = lineWidth;
            }
        }
        
        // Add padding to balloon width
        balloonWidth += padding * 2;
        
        // Calculate balloon height for multiple lines (add spacing between lines)
        float lineSpacing = lineHeight * BALLOON_LINE_SPACING_RATIO;
        float balloonHeight = wrappedLines.size() * lineHeight + (wrappedLines.size() - 1) * lineSpacing + padding * 2;
        float cornerRadius = textSize * BALLOON_CORNER_RATIO;
        
        // Calculate dot size for collision detection
        float dotSize = isPortrait ? rField.height() * flDots : rField.width() * flDots;
        float pulseDotSize = dotSize * BALLOON_MOVE_CLEARANCE_RATIO;
        
        // Find the best position for the balloon that doesn't cover possible moves
        RectF balloonRect = findBalloonPosition(canvas, balloonWidth, balloonHeight, pulseDotSize);
        
        // Draw the white balloon background with rounded corners
        canvas.drawRoundRect(balloonRect, cornerRadius, cornerRadius, pTutorialBalloon);
        
        // Draw the balloon border
        canvas.drawRoundRect(balloonRect, cornerRadius, cornerRadius, pTutorialBalloonBorder);
        
        // Draw each line of text centered in the balloon
        float textX = balloonRect.centerX();
        float totalTextHeight = wrappedLines.size() * lineHeight + (wrappedLines.size() - 1) * lineSpacing;
        float startY = balloonRect.centerY() - totalTextHeight / 2f + lineHeight;
        
        for (int i = 0; i < wrappedLines.size(); i++) {
            String line = wrappedLines.get(i);
            float textY = startY + i * (lineHeight + lineSpacing);
            canvas.drawText(line, textX, textY, pTutorialBalloonText);
        }
    }

    /**
     * Wraps text into multiple lines to fit within the specified width
     */
    private ArrayList<String> wrapText(String text, Paint paint, float maxWidth) {
        ArrayList<String> lines = new ArrayList<>();
        
        // If the entire text fits in one line, return it as is
        if (paint.measureText(text) <= maxWidth) {
            lines.add(text);
            return lines;
        }
        
        // Split text into words
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        
        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            float testWidth = paint.measureText(testLine);
            
            if (testWidth <= maxWidth) {
                // Word fits in current line
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            } else {
                // Word doesn't fit, start a new line
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    // Single word is too long, add it anyway
                    lines.add(word);
                }
            }
        }
        
        // Add the last line if there's any content
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        
        return lines;
    }

    /**
     * Gets the tutorial message string based on current tutorial message type
     */
    private String getTutorialMessage() {
        switch (currentTutorialMessage) {
            case BOUNCE_BORDER:
                return context.getString(R.string.field_hand_tutorial_bounce_border);
            case BOUNCE_VISITED:
                return context.getString(R.string.field_hand_tutorial_bounce_visited);
            case NO_MOVES:
                return context.getString(R.string.field_hand_tutorial_no_moves);
            case GOAL:
                return context.getString(R.string.field_hand_tutorial_goal);
            case OWN_GOAL:
                return context.getString(R.string.field_hand_tutorial_own_goal);
            case INITIAL:
            default:
                return context.getString(R.string.field_hand_tutorial_message);
        }
    }

    /**
     * Sets the tutorial message type to be displayed
     */
    public void setTutorialMessageType(TutorialMessageType messageType) {
        this.currentTutorialMessage = messageType;
    }

    /**
     * Checks if the balloon rectangle overlaps with any possible move points
     */
    private boolean checkBalloonOverlap(RectF balloonRect, float pulseDotSize) {
        for (MoveTo move : possibleMoves) {
            float moveX = w2x(flipX(move.X));
            float moveY = h2y(flipY(move.Y));
            
            // Check if the move point is within the balloon area (with some margin)
            if (moveX >= balloonRect.left - pulseDotSize && 
                moveX <= balloonRect.right + pulseDotSize &&
                moveY >= balloonRect.top - pulseDotSize && 
                moveY <= balloonRect.bottom + pulseDotSize) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds the best position for the tutorial balloon that doesn't cover possible moves
     */
    private RectF findBalloonPosition(Canvas canvas, float balloonWidth, float balloonHeight, float pulseDotSize) {
        // Calculate field bounds in pixels
        float fieldLeft = rField.left;
        float fieldRight = rField.right;
        float fieldTop = rField.top;
        float fieldBottom = rField.bottom;
        
        // Try to position the balloon at the top center of the field
        float balloonCenterX = (fieldLeft + fieldRight) / 2f;
        float balloonTop = fieldTop + (fieldBottom - fieldTop) * BALLOON_MARGIN_RATIO;
        
        RectF balloonRect = new RectF(
            balloonCenterX - balloonWidth / 2f,
            balloonTop,
            balloonCenterX + balloonWidth / 2f,
            balloonTop + balloonHeight
        );
        
        // Check if this position overlaps with any possible moves
        boolean overlaps = checkBalloonOverlap(balloonRect, pulseDotSize);
        
        // If top position overlaps, try bottom of field
        if (overlaps) {
            balloonTop = fieldBottom - balloonHeight - (fieldBottom - fieldTop) * BALLOON_MARGIN_RATIO;
            balloonRect = new RectF(
                balloonCenterX - balloonWidth / 2f,
                balloonTop,
                balloonCenterX + balloonWidth / 2f,
                balloonTop + balloonHeight
            );
            
            // Check again for overlaps at bottom position
            overlaps = checkBalloonOverlap(balloonRect, pulseDotSize);
            
            // If bottom also overlaps, try left side
            if (overlaps) {
                float balloonLeft = fieldLeft + (fieldRight - fieldLeft) * BALLOON_MARGIN_RATIO;
                float balloonCenterY = (fieldTop + fieldBottom) / 2f;
                balloonRect = new RectF(
                    balloonLeft,
                    balloonCenterY - balloonHeight / 2f,
                    balloonLeft + balloonWidth,
                    balloonCenterY + balloonHeight / 2f
                );
            }
        }
        
        // Ensure balloon stays within field bounds
        if (balloonRect.left < fieldLeft) {
            balloonRect.offset(fieldLeft - balloonRect.left, 0);
        }
        if (balloonRect.right > fieldRight) {
            balloonRect.offset(fieldRight - balloonRect.right, 0);
        }
        if (balloonRect.top < fieldTop) {
            balloonRect.offset(0, fieldTop - balloonRect.top);
        }
        if (balloonRect.bottom > fieldBottom) {
            balloonRect.offset(0, fieldBottom - balloonRect.bottom);
        }
        
        return balloonRect;
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
