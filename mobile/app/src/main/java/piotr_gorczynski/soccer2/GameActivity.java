package piotr_gorczynski.soccer2;

import static android.widget.Toast.LENGTH_SHORT;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

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
    private String matchId;
    private CollectionReference movesRef;
    private int localPlayerIndex;  // 0 or 1
    private FirebaseFirestore db;

    private boolean gameViewLaunched = false;

    private String player0Name, player1Name;

    private String player0Uid, player1Uid;

    @SuppressLint("RedundantSuppression")
    @SuppressWarnings("deprecation")
    private boolean isLegacyMovesNotNull(Bundle savedInstanceState) {
        return savedInstanceState.getParcelableArrayList("Moves") != null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
        //this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        if (savedInstanceState != null && (
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && savedInstanceState.getParcelableArrayList("Moves", MoveTo.class) != null) ||
                        isLegacyMovesNotNull(savedInstanceState)
        )) {
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

        if (GameType == 3) {
            matchId = getIntent().getStringExtra("matchId");
            Log.d("TAG_Soccer", "DEBUG matchId=" + matchId);
            String localNickname = getIntent().getStringExtra("localNickname");

            if (matchId == null || localNickname == null) {
                Log.e("TAG_Soccer", "Missing matchId or localNickname");
                Toast.makeText(this, "Game launch failed.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Log.e("TAG_Soccer", "User not signed in");
                Toast.makeText(this, "Please log in to continue.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            String localUid = user.getUid();
            db = FirebaseFirestore.getInstance();   // assign to the field

            // 🔹 Show waiting screen immediately
            setContentView(R.layout.view_waiting_for_opponent);

            db.collection("matches").document(matchId).get()
                    .addOnSuccessListener(doc -> {
                        if (!doc.exists()) {
                            Log.e("TAG_Soccer", "Match not found: " + matchId);
                            Toast.makeText(this, "Match not found.", Toast.LENGTH_LONG).show();
                            finish();
                            return;
                        }

                        String uid0 = doc.getString("player0");
                        String uid1 = doc.getString("player1");

                        if (uid0 == null || uid1 == null) {
                            Log.e("TAG_Soccer", "player0 or player1 field is missing");
                            Toast.makeText(this, "Invalid match record.", Toast.LENGTH_LONG).show();
                            finish();
                            return;
                        }

                        // Determine who we are (0 or 1) and set up nicknames
                        localPlayerIndex = localUid.equals(uid0) ? 0 : 1;  // assign to the field

                        // Look up the remote player’s nickname
                        String remoteUid = localPlayerIndex == 0 ? uid1 : uid0;
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
                                    player0Uid = doc.getString("player0");
                                    player1Uid = doc.getString("player1");

                                    // ── Wire up real‐time “moves” listener ──
                                    movesRef =
                                            db.collection("matches")
                                                    .document(this.matchId)
                                                    .collection("moves");

                                    Log.d("TAG_Soccer", "Attaching Firestore listener to: matches/" + matchId + "/moves");
                                    movesRef
                                            .orderBy("createdAt")
                                            .addSnapshotListener(this::onMovesUpdate);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("TAG_Soccer", "Failed to load opponent info.", e);
                                    Toast.makeText(this, "Failed to load opponent.", Toast.LENGTH_LONG).show();
                                    finish();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e("TAG_Soccer", "Failed to load match", e);
                        Toast.makeText(this, "Network error.", Toast.LENGTH_LONG).show();
                        finish();
                    });

            return;
        }

        if (GameType>0) {
            Log.d("TAG_Soccer", "123456: GameActivity.onCreate Game Type entered: " + GameType);
        }

        SharedPreferences sharedPreferences =
                getSharedPreferences(getPackageName() + "_preferences", Context.MODE_PRIVATE);


        if (sharedPreferences.contains("android_level")) {
            androidLevel = Integer.parseInt(sharedPreferences.getString("android_level", "1"));
            Log.e("TAG_Soccer", "Preference android_level=" + androidLevel);
        }

        //Log.d("TAG_Soccer", "123456: GameActivity.onCreate entered");
        if (GameType != 3) {
            gameView = new GameView(this, Moves, GameType, androidLevel);
            setContentView(gameView);

            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    new AlertDialog.Builder(GameActivity.this)
                            .setTitle("Leave game?")
                            .setMessage("Are you sure you want to exit?")
                            .setPositiveButton("Yes", (dialog, which) -> {
                                Intent intent = new Intent(GameActivity.this, MenuActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            })
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

    private void onMovesUpdate(QuerySnapshot snapshot, FirebaseFirestoreException e) {
        if (e != null) {
            Log.e("TAG_Soccer", "Listen for moves failed", e);
            return;
        }
        Log.d("TAG_Soccer", "onMovesUpdate triggered. Size: " + snapshot.size());
        Log.d("TAG_Soccer", "gameViewLaunched=" + gameViewLaunched +
                " player0Name=" + player0Name +
                " player1Name=" + player1Name);

        ArrayList<MoveTo> newMoves = new ArrayList<>();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            int x = Objects.requireNonNull(doc.getLong("x")).intValue();
            int y = Objects.requireNonNull(doc.getLong("y")).intValue();
            int p = Objects.requireNonNull(doc.getLong("p")).intValue();
            newMoves.add(new MoveTo(x, y, p));
        }

        if (newMoves.isEmpty()) {
            Log.w("TAG_Soccer", "No moves found in Firestore yet — skipping view creation");
            return;
        }

        if (!gameViewLaunched && player0Name != null && player1Name != null) {
            gameViewLaunched = true;
            Log.d("TAG_Soccer", "Creating GameView with newMoves.size=" + newMoves.size());
            gameView = new GameView(this, newMoves, GameType, player0Name, player1Name, localPlayerIndex);
            gameView.setMoveCallback(this::sendMoveToFirestore);
            runOnUiThread(() -> {
                setContentView(gameView);

                getOnBackPressedDispatcher().addCallback(GameActivity.this, new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        new AlertDialog.Builder(GameActivity.this)
                                .setTitle("Leave game?")
                                .setMessage("Are you sure you want to exit? This will count as a forfeit.")
                                .setPositiveButton("Yes", (dialog, which) -> {
                                    if (matchId != null) {
                                        String loserUid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
                                        String winnerUid = loserUid.equals(player0Uid) ? player1Uid : player0Uid;

                                        Map<String, Object> update = new HashMap<>();
                                        update.put("winner", winnerUid);
                                        update.put("status", "completed");
                                        update.put("reason", "abandon");

                                        FirebaseFirestore db = FirebaseFirestore.getInstance();

                                        // 🔄 Update match document
                                        db.collection("matches").document(matchId)
                                                .update(update)
                                                .addOnSuccessListener(unused -> Log.d("TAG_Soccer", "🏳️ Match marked as forfeited"))
                                                .addOnFailureListener(err -> Log.e("TAG_Soccer", "❌ Failed to update match", err));

                                        // 📤 Add a dummy move to trigger opponent's listener
                                        Map<String, Object> forfeitMove = new HashMap<>();
                                        forfeitMove.put("x", -1);
                                        forfeitMove.put("y", -1);
                                        forfeitMove.put("p", localPlayerIndex);  // who forfeited
                                        forfeitMove.put("createdAt", FieldValue.serverTimestamp());

                                        db.collection("matches").document(matchId)
                                                .collection("moves")
                                                .add(forfeitMove)
                                                .addOnSuccessListener(unused -> Log.d("TAG_Soccer", "📨 Forfeit move added"))
                                                .addOnFailureListener(e -> Log.e("TAG_Soccer", "❌ Failed to send forfeit move", e));
                                    }

                                    Intent intent = new Intent(GameActivity.this, MenuActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                    finish();
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    }
                });
            });

        } else {
            runOnUiThread(() -> {
                if (gameView != null) {
                    Log.d("TAG_Soccer", "Replacing moves: newMoves.size=" + newMoves.size());
                    gameView.replaceMoves(newMoves);
                    gameView.invalidate();

                    int winner = gameView.checkWinnerFromMoves(newMoves);
                    if (winner != -1 && this.Winner == -1) {
                        showWinner(winner);
                    }
                }
            });
        }
    }

    private void sendMoveToFirestore(int x, int y, int p) {
        Map<String,Object> m = new HashMap<>();
        m.put("x", x);
        m.put("y", y);
        m.put("p", p);
        m.put("createdAt", FieldValue.serverTimestamp());
        movesRef.add(m)
                .addOnFailureListener(err ->
                        Toast.makeText(this, "Failed to send move: "+err, LENGTH_SHORT).show()
                );
    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first
        if(Winner!=-1){
            showWinner(Winner);
        }
    }


    @Override
    public void onSaveInstanceState(Bundle outState)
    {
//---save whatever you need to persist—
        Log.d("TAG_Soccer", "123456: GameActivity.onSaveInstanceState entered");
        //Moves=gameView.GetMoves();
        outState.putParcelableArrayList("Moves",Moves);


        if(dialogWinner != null /*&& dialogWinner.isShowing()*/)
            // close dialog to prevent leaked window
            dialogWinner.dismiss();
        if(Winner!=-1){
            outState.putBoolean("alertShown", true);
            outState.putInt("Winner", Winner);
        }
        else
            outState.putBoolean("alertShown", false);

        super.onSaveInstanceState(outState);
    }

    public void showWinner(int Winner) {
        final String sPlayer0, sPlayer1;
        this.Winner = Winner;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);

        switch (GameType) {
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
            }
            default -> {
                sPlayer0 = "Player 0";
                sPlayer1 = "Player 1";
            }
        }

        if (GameType == 3 && matchId != null) {
            FirebaseFirestore.getInstance().collection("matches").document(matchId).get()
                    .addOnSuccessListener(doc -> {
                        String reason = doc.getString("reason");

                        if ("abandon".equals(reason)) {
                            builder.setMessage("Opponent forfeited the game.");
                        } else {
                            builder.setMessage("The winner is " + (Winner == 0 ? sPlayer0 : sPlayer1));
                        }

                        builder.setPositiveButton("Close", (dialog, which) -> {
                            Intent intent = new Intent(this, MenuActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        });

                        dialogWinner = builder.create();
                        dialogWinner.show();
                    })
                    .addOnFailureListener(err -> {
                        Log.e("TAG_Soccer", "Failed to load match reason", err);

                        builder.setMessage("The winner is " + (Winner == 0 ? sPlayer0 : sPlayer1));
                        builder.setPositiveButton("Close", (dialog, which) -> {
                            Intent intent = new Intent(this, MenuActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        });
                        dialogWinner = builder.create();
                        dialogWinner.show();
                    });

            return; // Avoid showing duplicate dialog below
        }

        // GameType 1 or 2 fallback
        builder.setMessage("The winner is " + (Winner == 0 ? sPlayer0 : sPlayer1));
        builder.setPositiveButton("Close", (dialog, which) -> {
            Intent intent = new Intent(this, MenuActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
        dialogWinner = builder.create();
        dialogWinner.show();
    }
}
