package piotr_gorczynski.soccer2;

import static android.widget.Toast.LENGTH_SHORT;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.CountDownTimer;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class GameActivity extends AppCompatActivity {


    private ArrayList<MoveTo> Moves= new ArrayList<>();
    AlertDialog dialogWinner;
    int Winner=-1;
    int GameType=-1;
    GameView gameView;
    int androidLevel = 1;

    //Real-time Move Sync
    private String matchPath;
    private CollectionReference movesRef;

    private DocumentReference matchRef;

    private int localPlayerIndex;  // 0 or 1
    private FirebaseFirestore db;

    private String player0Name, player1Name;

    private String player0Uid, player1Uid;

    private boolean alertShown = false;

    private ListenerRegistration movesListener, clockListener;

    private boolean clockStartAttempted = false;

    private CountDownTimer turnTimer;

    private long remainingTime0, remainingTime1,previousRemainingTime0=-1, previousRemainingTime1=-1;
    private Long turnStartTime, previousTurnSTartTime= (long) -1;
    //private long turnStartTimeMs;

    Timestamp turnStartTimeTs;

    private volatile boolean gameEnded = false;

    /**
     * Return millis still left right now, given the seconds stored in
     * Firestore and the turnStartTime (ms since epoch) that came with it.
     */
    private long computeTimeLeft(long storedSeconds, @Nullable Long turnStartMs) {
        if (turnStartMs == null) return storedSeconds * 1000L;        // no clock yet
        long elapsed = System.currentTimeMillis() - turnStartMs;
        return Math.max(storedSeconds * 1000L - elapsed, 0L);
    }

    /**
     * Return the clock value (in whole seconds) that should be shown *now*
     * for the player whose turn is currently active.
     */
    private long computeTimeLeftSecs(long storedSecs, @Nullable Long turnStartMs) {
        if (turnStartMs == null) return storedSecs;          // clock not started
        long elapsed = (System.currentTimeMillis() - turnStartMs) / 1000;  // s
        return Math.max(storedSecs - elapsed, 0);
    }

    @SuppressLint("RedundantSuppression")
    @SuppressWarnings("deprecation")
    private boolean isLegacyMovesNotNull(Bundle savedInstanceState) {
        return savedInstanceState.getParcelableArrayList("Moves") != null;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // Don’t even replace the intent if it’s identical
        String newPath = intent.getStringExtra("matchPath");
        String currentPath = getIntent().getStringExtra("matchPath");

        // If nothing has changed, bail out
        if (newPath != null && newPath.equals(currentPath)) {
            Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                    + ": identical matchPath, skipping");
        } else {
            Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                    + ": ❌ CRITICAL ERROR: Different matchPath on 2nd call!");
            throw new IllegalStateException("❌ CRITICAL ERROR: Dirrenten match_id on 2nd call!");
        }
    }

    public static class ExceptionHandler implements Thread.UncaughtExceptionHandler {
        private final Thread.UncaughtExceptionHandler existingHandler;

        public ExceptionHandler() {
            this.existingHandler = Thread.getDefaultUncaughtExceptionHandler();
        }

        @Override
        public void uncaughtException(Thread thread, Throwable throwable) {
            // 🔹 Your custom logic here (e.g., logging to file)
            Log.e("ExceptionHandler", "Uncaught exception", throwable);

            // 🔹 Call the original handler (important!)
            if (existingHandler != null) {
                existingHandler.uncaughtException(thread, throwable);
            }
        }
    }


    @SuppressLint("ApplySharedPref")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
        //this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        if (savedInstanceState != null && (
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && savedInstanceState.getParcelableArrayList("Moves", MoveTo.class) != null) ||
                        isLegacyMovesNotNull(savedInstanceState)
        )) {
            Winner       = savedInstanceState.getInt("Winner", -1);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Moves = savedInstanceState.getParcelableArrayList("Moves", MoveTo.class);
            } else {
                @SuppressLint("RedundantSuppression")
                @SuppressWarnings("deprecation")
                ArrayList<MoveTo> legacyMoves = savedInstanceState.getParcelableArrayList("Moves");
                Moves = legacyMoves;
            }
        }
        else {
            // Default position in the middle of the field
            Moves.add( new MoveTo(getResources().getInteger(R.integer.intFieldHalfWidth),getResources().getInteger(R.integer.intFieldHalfHeight),0));

            //Ticket #6 studying MINMAX situation if Android should trunc the defeat branch
            //Moves.add( new MoveTo(3,2,1));

            //Ticket #5 studying MINMAX situation if Android should move into opposite direction
            /*Moves.add( new MoveTo(3,2,0));
            Moves.add( new MoveTo(4,3,0));
            Moves.add( new MoveTo(3,3,0));
            Moves.add( new MoveTo(4,4,0));
            Moves.add( new MoveTo(3,4,0));
            Moves.add( new MoveTo(4,5,0));
            Moves.add( new MoveTo(3,5,0));
            Moves.add( new MoveTo(3,6,0));
            Moves.add( new MoveTo(2,6,0));
            Moves.add( new MoveTo(1,7,0));
            Moves.add( new MoveTo(0,6,0));
            Moves.add( new MoveTo(0,5,1));*/

            //Ticket #4 studying MINMAX situation if Android win in this move (in the left corner)
            /*Moves.add( new MoveTo(2,7,0));
            Moves.add( new MoveTo(1,8,0));
            Moves.add( new MoveTo(0,7,0));
            Moves.add( new MoveTo(1,6,1));*/


            //Ticket #3 studying MINMAX situation if Android win in this move (on the corner of the Player's gate)
            //Moves.add( new MoveTo(2,8,1));

            //Ticket #2 studying MINMAX situation
            /*Moves.add( new MoveTo(2,0,0));
            Moves.add( new MoveTo(3,0,0));
            Moves.add( new MoveTo(3,1,0));
            Moves.add( new MoveTo(2,0,0));
            Moves.add( new MoveTo(2,1,0));
            Moves.add( new MoveTo(2,2,0));
            Moves.add( new MoveTo(3,1,0));
            Moves.add( new MoveTo(2,1,0));
            Moves.add( new MoveTo(3,0,0));
            Moves.add( new MoveTo(3,1,0));
            Moves.add( new MoveTo(4,1,0));
            Moves.add( new MoveTo(3,2,0));
            Moves.add( new MoveTo(3,1,0));
            Moves.add( new MoveTo(4,1,0));
            Moves.add( new MoveTo(5,0,0));*/


            //Ticket #1 situation for Android move (very long loop)
            /*Moves.add( new MoveTo(3,4,0));
            Moves.add( new MoveTo(3,3,1));
            Moves.add( new MoveTo(2,4,0));
            Moves.add( new MoveTo(1,3,1));
            Moves.add( new MoveTo(0,4,1));
            Moves.add( new MoveTo(1,5,0));
            Moves.add( new MoveTo(2,4,0));
            Moves.add( new MoveTo(2,3,1));
            Moves.add( new MoveTo(3,4,1));
            Moves.add( new MoveTo(2,4,1));
            Moves.add( new MoveTo(2,5,0));
            Moves.add( new MoveTo(3,4,0));
            Moves.add( new MoveTo(4,3,1));
            Moves.add( new MoveTo(3,3,1));
            Moves.add( new MoveTo(2,3,1));
            Moves.add( new MoveTo(1,3,1));
            Moves.add( new MoveTo(0,3,1));
            Moves.add( new MoveTo(1,4,0));
            Moves.add( new MoveTo(1,3,0));
            Moves.add( new MoveTo(0,2,0));
            Moves.add( new MoveTo(1,1,1));
            Moves.add( new MoveTo(0,1,1));
            Moves.add( new MoveTo(1,0,1));
            Moves.add( new MoveTo(1,1,1));
            Moves.add( new MoveTo(1,2,0));
            Moves.add( new MoveTo(2,1,1));
            Moves.add( new MoveTo(1,1,1));
            Moves.add( new MoveTo(2,0,1));
            Moves.add( new MoveTo(2,1,1));
            Moves.add( new MoveTo(2,2,0));
            Moves.add( new MoveTo(3,1,1));
            Moves.add( new MoveTo(2,1,1));
            Moves.add( new MoveTo(3,2,0));
            Moves.add( new MoveTo(3,1,0));
            Moves.add( new MoveTo(3,0,1));
            Moves.add( new MoveTo(2,0,1));
            Moves.add( new MoveTo(3,1,1));
            Moves.add( new MoveTo(4,2,0));
            Moves.add( new MoveTo(4,1,1));*/


        }

        GameType=getIntent().getIntExtra("GameType",0);
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Game Type entered: " + GameType);

        if (GameType == 3) {
            matchPath = getIntent().getStringExtra("matchPath");
            Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": matchPath=" + matchPath);
            String localNickname = getIntent().getStringExtra("localNickname");

            if (matchPath == null || localNickname == null) {
                Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Missing matchPath or localNickname");
                Toast.makeText(this, "Game launch failed.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": User not signed in");
                Toast.makeText(this, "Please log in to continue.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            String localUid = user.getUid();
            db = FirebaseFirestore.getInstance();   // assign to the field

            // 🔹 Show waiting screen immediately
            setContentView(R.layout.view_waiting_for_opponent);

            // grab the reference once
            matchRef = db.document(matchPath);

            matchRef.get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Match not found: " + matchPath);
                        Toast.makeText(this, "Match not found.", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    player0Uid = doc.getString("player0");
                    player1Uid = doc.getString("player1");


                    if (player0Uid == null || player1Uid == null) {
                        Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": player0 or player1 field is missing");
                        Toast.makeText(this, "Invalid match record.", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    // Determine who we are (0 or 1) and set up nicknames
                    localPlayerIndex = localUid.equals(player0Uid) ? 0 : 1;  // assign to the field
                    // Look up the remote player’s nickname
                    String remoteUid = localPlayerIndex == 0 ? player1Uid : player0Uid;

                    db.collection("users").document(remoteUid).get()
                            .addOnSuccessListener(remoteDoc -> {
                                String remoteNickname = remoteDoc.getString("nickname");
                                // after you fetch remoteNickname…
                                if (localPlayerIndex == 0) {
                                    player0Name = localNickname;
                                    player1Name = remoteNickname;
                                } else {
                                    player1Name = localNickname;
                                    player0Name = remoteNickname;
                                }

                                // grab initial clock values
                                // safely extract the two clocks:
                                Long rawT0 = doc.getLong("remainingTime0");
                                Long rawT1 = doc.getLong("remainingTime1");

                                if (rawT0 == null || rawT1 == null) {
                                    Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Clock fields missing on read!");
                                    throw new IllegalStateException("Missing remainingTime0/1 fields");
                                }
                                remainingTime0 = rawT0;
                                remainingTime1 = rawT1;

                                // Read turnStartTime as a Timestamp or null
                                turnStartTimeTs = doc.getTimestamp("turnStartTime");
                                turnStartTime = (turnStartTimeTs != null) ? turnStartTimeTs.toDate().getTime() : null;

                                // ── Adjust ONLY the active player's clock ──
                                int currentTurn =
                                        Objects.requireNonNull(doc.getLong("turn")).intValue();

                                if (turnStartTime != null) {          // clock already running
                                    if (currentTurn == 0) {
                                        remainingTime0 = computeTimeLeftSecs(remainingTime0,
                                                turnStartTime);
                                    } else {
                                        remainingTime1 = computeTimeLeftSecs(remainingTime1,
                                                turnStartTime);
                                    }
                                }

                                // CREATE your GameView exactly once
                                initGameView();

                                // ── Wire up real‐time “moves” listener ──
                                movesRef = matchRef.collection("moves");

                                // MOVE listener → only calls replaceMoves(...)
                                Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Attaching Firestore listener to: matches/" + matchPath + "/moves");
                                movesListener = movesRef
                                    .orderBy("createdAt")
                                    .addSnapshotListener(this::onMovesUpdate);

                                // CLOCK listener → only calls updateTimes(...)
                                clockListener = matchRef.addSnapshotListener(this::onClockUpdate);
                            })
                            .addOnFailureListener(e -> {
                                Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Failed to load opponent info.", e);
                                Toast.makeText(this, "Failed to load opponent.", Toast.LENGTH_LONG).show();
                                finish();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Failed to load match", e);
                    Toast.makeText(this, "Network error.", Toast.LENGTH_LONG).show();
                    finish();
                });

            return;
        }



        SharedPreferences sharedPreferences =
                getSharedPreferences(getPackageName() + "_preferences", Context.MODE_PRIVATE);

        if (sharedPreferences.contains("android_level")) {
            androidLevel = Integer.parseInt(sharedPreferences.getString("android_level", "1"));
            Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Preference android_level=" + androidLevel);
        }

        //Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": GameActivity.onCreate entered");
        if (GameType != 3) {
            gameView = new GameView(this, Moves, GameType, androidLevel);
            setContentView(gameView);

            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    new AlertDialog.Builder(GameActivity.this)
                            .setTitle("Leave game?")
                            .setMessage("Are you sure you want to exit?")
                            .setPositiveButton("Yes", (dialog, which) -> finish())
                            .setNegativeButton("Cancel", null)
                            .show();
                }
            });

        }

        if(savedInstanceState != null && savedInstanceState.getBoolean("alertShown")){
            Winner=savedInstanceState.getInt("Winner");
//            showWinner(Winner);
        }
    }

    private void startClock(int playerIndex, long remainingMillis) {
        if (gameEnded) return;              // ⬅️ hard gate
        // cancel any existing
        if (turnTimer != null) turnTimer.cancel();
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Started");
        turnTimer = new CountDownTimer(remainingMillis, 1000) {
            @Override public void onTick(long msUntilFinished) {
/*                long t0 = remainingTime0;
                long t1 = remainingTime1;
                if (playerIndex == 0)  t0 = msUntilFinished / 1000;
                else                   t1 = msUntilFinished / 1000;
                Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": t0=" + t0 + ", t1=" + t1);
                gameView.updateTimes(t0, t1, turnStartTime);*/
                if (playerIndex == 0)  remainingTime0 = msUntilFinished / 1000;
                else                   remainingTime1 = msUntilFinished / 1000;
                Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": remainingTime0=" + remainingTime0 + ", remainingTime1=" + remainingTime1);
                gameView.updateTimes(remainingTime0, remainingTime1, turnStartTime);

            }
            @Override public void onFinish() {
                // ⏱ local clock reached 0 – call helper
                handleTimeout(playerIndex);
            }
        }.start();
    }

    /**
     * Writes “timeout” result if the match is still active and no winner recorded.
     * @param timedOutPlayer 0 or 1
     */
    private void handleTimeout(int timedOutPlayer) {
        if (GameType != 3) return;          // PvP-online only

        final String winnerUid =
                timedOutPlayer == 0 ? player1Uid : player0Uid;

        db.runTransaction((Transaction.Function<Void>) txn -> {
                    DocumentSnapshot snap = txn.get(matchRef);

                    boolean stillActive   = "active".equals(snap.getString("status"));
                    boolean noWinnerYet   = snap.getString("winner") == null;
                    if (stillActive && noWinnerYet) {
                        Map<String, Object> update = new HashMap<>();
                        update.put("winner",   winnerUid);
                        update.put("status",   "completed");
                        update.put("reason",   "timeout");
                        update.put("remainingTime" + timedOutPlayer, 0);
                        update.put("updatedAt", FieldValue.serverTimestamp());
                        txn.update(matchRef, update);
                    }
                    return null;
                }).addOnSuccessListener(v -> {
                        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                                + ": 🏆 Timeout result recorded");
                        // ⏹️  stop the local countdown immediately
                        runOnUiThread(() -> {
                            if (turnTimer != null) {
                                Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                                        + ": Stopping clock...");
                                turnTimer.cancel();
                                turnTimer = null;      // hygiene – prevents reuse
                                Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                                        + ": Calling showWinner");
                                int winner = (timedOutPlayer == 0) ? 1 : 0;
                                showWinner(winner);           // dialog now sees reason="timeout"
                            }
                        });
                })
                .addOnFailureListener(e -> {
                        Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                                + ": ❌ Failed to record timeout", e);
                        stopClock();
                });
    }

    private void onClockUpdate(DocumentSnapshot snap, FirebaseFirestoreException e) {
        if (e != null || snap == null || !snap.exists()) return;
        if (gameEnded) return;   // board frozen
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Started");
        String serverWinner = snap.getString("winner");
        String reason       = snap.getString("reason");
        if (serverWinner != null && Winner == -1) {      // nobody has shown a dialog yet
            int winnerIdx = serverWinner.equals(player0Uid) ? 0 : 1;
            if ("timeout".equals(reason) || "abandon".equals(reason)) {              // optional filter
                runOnUiThread(() -> showWinner(winnerIdx));
                return;                                  // nothing else to update
            }
        }

        if (!"active".equals(snap.getString("status"))) {
            Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": ️ \uD83D\uDC80 Extra guard block was executed");
            return;                                  // match over → ignore update
        }

        Long rawT0 = snap.getLong("remainingTime0");
        Long rawT1 = snap.getLong("remainingTime1");
        // Read turnStartTime as a Timestamp or null
        turnStartTimeTs = snap.getTimestamp("turnStartTime");
        turnStartTime = (turnStartTimeTs != null) ? turnStartTimeTs.toDate().getTime() : null;

        Long turn = snap.getLong("turn");
        if (rawT0 == null || rawT1 == null || turn==null) {
            Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Clock fields missing on update!");
            throw new IllegalStateException("Missing remainingTime0/1 or turn fields");
        }
        remainingTime0 = rawT0;
        remainingTime1 = rawT1;
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                + ": remainingTime0="+remainingTime0
                + " remainingTime1="+remainingTime1
                + " turnStartTime="+((turnStartTime == null) ? "null" : String.valueOf(turnStartTime))
                + " turn="+turn
                + " clockStartAttempted="+clockStartAttempted);

        //Continue only if times changed...
        if(previousRemainingTime0==remainingTime0 && previousRemainingTime1==remainingTime1 && Objects.equals(previousTurnSTartTime, turnStartTime)) {
            Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                    + ": Times the same as previously, exiting");
            return;
        }

        previousRemainingTime0 = remainingTime0;
        previousRemainingTime1 = remainingTime1;
        previousTurnSTartTime = turnStartTime;

        gameView.updateTimes(remainingTime0, remainingTime1, turnStartTime);

        DocumentReference matchRefThisSnap = snap.getReference();

        //reseting the flag if the turn was nullified
        if (clockStartAttempted && turnStartTimeTs == null ) {
            clockStartAttempted=false;
            Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Flag clockStartAttempted=false");
            stopClock();
        }

        // Fix condition to use ts (which is now a Timestamp)
        if (!clockStartAttempted && turnStartTimeTs == null && turn.intValue() == localPlayerIndex) {

            db.runTransaction((Transaction.Function<Void>) txn -> {
                DocumentSnapshot ds = txn.get(matchRefThisSnap);     // ❷ read inside txn
                if (ds.getTimestamp("turnStartTime") == null) {
                    // Prepare updates for the match document
                    Map<String, Object> matchUpdates = new HashMap<>();
                    matchUpdates.put("turnStartTime", FieldValue.serverTimestamp());
                    matchUpdates.put("updatedAt", FieldValue.serverTimestamp());
                    txn.update(matchRef, matchUpdates);           // ❸ write once
                }
                return null;
            })
            .addOnSuccessListener(unused -> {
                clockStartAttempted = true; // Prevent repeat attempts!
                Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": turnStartTime updated by player " + localPlayerIndex);
                matchRefThisSnap.get()
                        .addOnSuccessListener(updatedSnap -> {
                            turnStartTimeTs = updatedSnap.getTimestamp("turnStartTime");
                            turnStartTime = (turnStartTimeTs != null) ? turnStartTimeTs.toDate().getTime() : null;
                            runOnUiThread(() -> gameView.updateTimes(remainingTime0, remainingTime1, turnStartTime));
                            Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Clock started for player " + localPlayerIndex);
                            //turnStartTimeMs = System.currentTimeMillis();
                            long remSecs = localPlayerIndex==0 ? remainingTime0 : remainingTime1;
                            startClock(localPlayerIndex, remSecs*1000);
                        })
                        .addOnFailureListener(err -> Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Failed to re-fetch turnStartTime and start the clock",err));
            })
            .addOnFailureListener(ex -> {
                clockStartAttempted = false; // Allow retry if failed
                Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": turnStartTime update failed", ex);
            });
        }
        /*  ⏱  If a valid turnStartTime exists and we haven’t started a
        countdown on _this_ device yet, start it now — no matter whose
        turn it is.  */
        if (!clockStartAttempted && turnStartTimeTs != null) {
            clockStartAttempted = true;        // prevent double-starts

            long remSecs = (turn.intValue() == 0) ? remainingTime0 : remainingTime1;
            long timeLeft  = computeTimeLeft(remSecs, turnStartTime);
            startClock(turn.intValue(), timeLeft);

            Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                            + ": Clock started locally for player " + turn);
        }

    }

    public void stopClock(){
        runOnUiThread(() -> {
            if(turnTimer!=null) {
                Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                        + ": Stopping clock...");
                turnTimer.cancel();
                turnTimer = null;
            }
        });
    }
    private void initGameView() {
        gameView = new GameView(
                this,
                Moves,
                GameType,
                player0Name,
                player1Name,
                localPlayerIndex,
                remainingTime0,
                remainingTime1,
                turnStartTime
        );
        gameView.setMoveCallback(this::sendMoveToFirestore);
        setContentView(gameView);

        // optional: back‐press handler, etc. as you had it

        getOnBackPressedDispatcher().addCallback(GameActivity.this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                new AlertDialog.Builder(GameActivity.this)
                        .setTitle("Leave game?")
                        .setMessage("Are you sure you want to exit? This will count as a forfeit.")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            if (matchPath != null) {
                                String loserUid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
                                String winnerUid = loserUid.equals(player0Uid) ? player1Uid : player0Uid;

                                Map<String, Object> update = new HashMap<>();
                                update.put("winner", winnerUid);
                                update.put("status", "completed");
                                update.put("reason", "abandon");

                                // 🔄 Update match document
                                matchRef.update(update)
                                        .addOnSuccessListener(unused -> Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                                                + ": 🏳️ Match marked as forfeited"))
                                        .addOnFailureListener(err -> Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                                                + ": ❌ Failed to update match", err));

                                // 📤 Add a dummy move to trigger opponent's listener
                                /*Map<String, Object> forfeitMove = new HashMap<>();
                                forfeitMove.put("x", -1);
                                forfeitMove.put("y", -1);
                                forfeitMove.put("p", localPlayerIndex);
                                forfeitMove.put("createdAt", FieldValue.serverTimestamp());

                                matchRef.collection("moves").add(forfeitMove)
                                        .addOnSuccessListener(unused -> Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": 📨 Forfeit move added"))
                                        .addOnFailureListener(e -> Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": ❌ Failed to send forfeit move", e));*/
                            }

                            finish();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

    }

    private void onMovesUpdate(QuerySnapshot snapshot, FirebaseFirestoreException e) {
        if (e != null) {
            Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Listen for moves failed", e);
            return;
        }
        if (gameEnded) return;   // board frozen
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Started. Snapshot Size: " + snapshot.size());

        ArrayList<MoveTo> newMoves = new ArrayList<>();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            int x = Objects.requireNonNull(doc.getLong("x")).intValue();
            int y = Objects.requireNonNull(doc.getLong("y")).intValue();
            int p = Objects.requireNonNull(doc.getLong("p")).intValue();
            newMoves.add(new MoveTo(x, y, p));
        }

        if (newMoves.isEmpty()) {
            Log.w("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": No moves found in Firestore yet — skipping view creation");
            return;
        }

        runOnUiThread(() -> {
            if (gameView != null) {
                Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Replacing moves: newMoves.size=" + newMoves.size());
                gameView.replaceMoves(newMoves);
                gameView.invalidate();

                int winner = gameView.checkWinnerFromMoves(newMoves);
                if (winner != -1 && this.Winner == -1) {
                    showWinner(winner);
                }
            }
        });
    }

    private void sendMoveToFirestore(int x, int y, int p) {
        // very first lines of sendMoveToFirestore(...)
        if (gameEnded) {
            Toast.makeText(this, "Game is over.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String,Object> moveData = new HashMap<>();
        moveData.put("x", x);
        moveData.put("y", y);
        moveData.put("p", p);
        moveData.put("createdAt", FieldValue.serverTimestamp());

        if(p!=this.localPlayerIndex){
            //Stop the clock
            stopClock();

/*            long prevRemainingSecs = localPlayerIndex == 0 ? remainingTime0 : remainingTime1;
            long prevRemainingMs = prevRemainingSecs * 1000L;
            long nowMs = System.currentTimeMillis();
            long elapsed = nowMs - turnStartTimeMs;
            long myNewRemainingMs = Math.max(prevRemainingMs - elapsed, 0L);

            // Prepare updates for the match document
            Map<String,Object> matchUpdates = new HashMap<>();
            matchUpdates.put("remainingTime" + localPlayerIndex,
                    myNewRemainingMs / 1000L);        // write back in seconds
            matchUpdates.put("turn", p);
            matchUpdates.put("turnStartTime", null); // next player will set it
            matchUpdates.put("updatedAt", FieldValue.serverTimestamp());*/

            // Send the move
            movesRef.add(moveData)
                    .addOnSuccessListener(docRef -> {
                        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Sent move (change turn). x="+x+" y="+y+" p="+p);
                        turnStartTime=null;
                        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Updating GameView");
                        gameView.updateTimes(remainingTime0,remainingTime1,null);
                    })

/*                    .addOnSuccessListener(docRef -> matchRef.update(matchUpdates)
                            .addOnSuccessListener(aVoid -> {
                                // Reset local turn/clock flags here if needed
                                Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Sent move an updated match");
                                turnStartTime=null;
                                gameView.updateTimes(remainingTime0,remainingTime1,null);
                                // clockStartAttempted = false; // So next time this device's turn comes, it'll try to start the clock
                                // Optionally reset local timing state
                            })
                            .addOnFailureListener(err -> Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Failed to update match")))*/
                    .addOnFailureListener(err -> {
                        Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Failed to send move");
                        Toast.makeText(this, "Failed to send move: "+err, LENGTH_SHORT).show();
                    });
        } else {
            // Send the move
            movesRef.add(moveData)
                    .addOnSuccessListener(docRef -> Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Sent move (bouncing). x="+x+" y="+y+" p="+p))
                    .addOnFailureListener(err -> {
                        Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Failed to send move");
                        Toast.makeText(this, "Failed to send move: "+err, LENGTH_SHORT).show();
                    });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!alertShown && Winner != -1) {
            showWinner(Winner);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        //---save whatever you need to persist—
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Started");
        //Moves=gameView.GetMoves();

        outState.putParcelableArrayList("Moves",Moves);

        if (Winner != -1) {
            outState.putInt("Winner", Winner);
        }

        if (dialogWinner != null) {          // was visible
            dialogWinner.dismiss();          // prevent leaked-window
            dialogWinner = null;
            alertShown   = false;            // <--- **allow it to be shown again**
        }

        super.onSaveInstanceState(outState);
    }

    public void showWinner(int Winner) {
        if (alertShown) return;
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Started. Winner = " + Winner);
        alertShown = true;
        gameEnded = true;
        if (movesListener != null) {  // extra hygiene
            movesListener.remove();
        }
        gameView.setInputEnabled(false);   // freeze the board
        final String sPlayer0, sPlayer1;
        this.Winner = Winner;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);

        // ❱❱ stop the clock as soon as we show the winner dialog
        stopClock();
        if (clockListener != null) {
            clockListener.remove();
        }

        switch (GameType) {
            case 2 -> {
                sPlayer0 = "Player";
                sPlayer1 = "Android";
            }
            case 3 -> {
                sPlayer0 = player0Name != null ? player0Name : "Player 0";
                sPlayer1 = player1Name != null ? player1Name : "Player 1";
            }
            default -> { //case 1 :-)
                sPlayer0 = "Player 1";
                sPlayer1 = "Player 2";
            }
        }

        if (GameType == 3) {
            final String sWinner = (Winner == 0 ? sPlayer0 : sPlayer1);
            final String sLooser= (Winner == 0 ? sPlayer1 : sPlayer0);

            if(matchPath == null) {
                Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Failed to load match reason");
                Toast.makeText(this, "Fatal error matchPath == null", Toast.LENGTH_LONG).show();
                throw new IllegalStateException("Fatal error matchPath == null");
            }
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference matchRef = this.matchRef;    // already points to the right doc

            db.runTransaction(transaction -> {
                        DocumentSnapshot snap = transaction.get(matchRef);
                        // did we write or not?
                        boolean didWrite = snap.exists() && snap.getString("winner") == null;
                        if (didWrite) {
                            String winnerUid = (Winner == 0 ? player0Uid : player1Uid);
                            Map<String,Object> update = new HashMap<>();
                            update.put("winner",  winnerUid);
                            update.put("status",  "completed");
                            update.put("reason",  "goal");
                            transaction.update(matchRef, update);
                        }
                        // return whether we actually wrote
                        return didWrite;
                    })
                    .addOnSuccessListener(didWrite -> {
                        if (didWrite) {
                            Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": 🏆 Match result recorded");
                        } else {
                            Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": 🏆 Match result already recorded by other client");
                        }
                    })
                    .addOnFailureListener(e ->
                            Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": ❌ Failed to record match result", e));

            matchRef.get()
                    .addOnSuccessListener(
                            GameActivity.this,      // ← executor tied to this Activity’s main looper
                            doc -> {
                                String reason = doc.getString("reason");
                                String msg="";
                                if ("timeout".equals(reason)) {
                                    msg = sLooser + " ran out of time. ";
                                } else if ("abandon".equals(reason)) {
                                    msg= sLooser + " forfeited the game. ";
                                }

                                msg=msg+"The winner is " + sWinner+"!";
                                builder.setMessage(msg);

                                builder.setPositiveButton("Close", (dialog, which) -> finish());
                                // make sure we’re still alive
                                if (!isFinishing() && !isDestroyed()) {
                                    dialogWinner = builder.create();
                                    Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": dialogWinner show: "+msg);
                                    dialogWinner.show();
                                } else {
                                    Log.w("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Tried to show dialog when activity is finishing or destroyed");
                                }

                            })
                    .addOnFailureListener(err -> {
                        Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Failed to load match reason", err);
                        Toast.makeText(this, "Failed to load match.", Toast.LENGTH_LONG).show();
                        // make sure we’re still alive, on the UI thread
                        /*runOnUiThread(() -> {
                            if (!isFinishing() && !isDestroyed()) {
                                dialogWinner = builder.create();
                                dialogWinner.show();
                            } else {
                                Log.w("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Tried to show dialog when activity is finishing or destroyed");
                            }
                            });*/
                        });
            return; // Avoid showing duplicate dialog below
        }

        // GameType 1 or 2 fallback
        builder.setMessage("The winner is " + (Winner == 0 ? sPlayer0 : sPlayer1));
        builder.setPositiveButton("Close", (dialog, which) -> finish());
        dialogWinner = builder.create();
        dialogWinner.show();
    }

    @Override
    protected void onDestroy() {
        if (movesListener != null) movesListener.remove();
        if (clockListener != null) clockListener.remove();
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // nothing to do – we stay portrait regardless
    }
}
