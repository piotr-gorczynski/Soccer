package piotr_gorczynski.soccer2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

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
            String matchId = getIntent().getStringExtra("matchId");
            String localNickname = getIntent().getStringExtra("localNickname");

            if (matchId == null || localNickname == null) {
                Log.e("pgorczyn", "Missing matchId or localNickname");
                Toast.makeText(this, "Game launch failed.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Log.e("pgorczyn", "User not signed in");
                Toast.makeText(this, "Please log in to continue.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            String localUid = user.getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // 🔹 Show waiting screen immediately
            setContentView(R.layout.view_waiting_for_opponent);

            db.collection("matches").document(matchId).get()
                    .addOnSuccessListener(doc -> {
                        if (!doc.exists()) {
                            Log.e("pgorczyn", "Match not found: " + matchId);
                            Toast.makeText(this, "Match not found.", Toast.LENGTH_LONG).show();
                            finish();
                            return;
                        }

                        String uid0 = doc.getString("player0");
                        String uid1 = doc.getString("player1");

                        if (uid0 == null || uid1 == null) {
                            Log.e("pgorczyn", "player0 or player1 field is missing");
                            Toast.makeText(this, "Invalid match record.", Toast.LENGTH_LONG).show();
                            finish();
                            return;
                        }

                        String remoteUid = localUid.equals(uid0) ? uid1 : uid0;
                        final String[] nickname0 = { localUid.equals(uid0) ? localNickname : null };
                        final String[] nickname1 = { localUid.equals(uid1) ? localNickname : null };

                        db.collection("users").document(remoteUid).get()
                                .addOnSuccessListener(remoteDoc -> {
                                    String remoteNickname = remoteDoc.getString("nickname");
                                    if (localUid.equals(uid0)) {
                                        nickname1[0] = remoteNickname;
                                    } else {
                                        nickname0[0] = remoteNickname;
                                    }

                                    // 🔹 Listen for match activation
                                    db.collection("matches").document(matchId)
                                            .addSnapshotListener((snapshot, error) -> {
                                                if (error != null || snapshot == null || !snapshot.exists()) {
                                                    Log.e("pgorczyn", "Match snapshot error", error);
                                                    return;
                                                }

                                                String status = snapshot.getString("status");
                                                if ("active".equals(status)) {
                                                    Log.d("pgorczyn", "Match is active. Starting game...");

                                                    if (gameView == null) {
                                                        gameView = new GameView(this, Moves, GameType, nickname0[0], nickname1[0]);
                                                        setContentView(gameView);
                                                    }
                                                }
                                                // else: still waiting – UI already shows waiting screen
                                            });

                                })
                                .addOnFailureListener(e -> {
                                    Log.e("pgorczyn", "Failed to load remote nickname", e);
                                    Toast.makeText(this, "Failed to load opponent info.", Toast.LENGTH_LONG).show();
                                    finish();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e("pgorczyn", "Failed to load match", e);
                        Toast.makeText(this, "Network error.", Toast.LENGTH_LONG).show();
                        finish();
                    });

            return;
        }

        if (GameType>0) {
            Log.d("pgorczyn", "123456: GameActivity.onCreate Game Type entered: " + GameType);
        }

        SharedPreferences sharedPreferences =
                getSharedPreferences(getPackageName() + "_preferences", Context.MODE_PRIVATE);


        if (sharedPreferences.contains("android_level")) {
            androidLevel = Integer.parseInt(sharedPreferences.getString("android_level", "1"));
            Log.d("pgorczynMove", "Preference android_level=" + androidLevel);
        }

        //Log.d("pgorczyn", "123456: GameActivity.onCreate entered");
        if (GameType != 3) {
            gameView = new GameView(this, Moves, GameType, androidLevel);
            setContentView(gameView);
        }

        if(savedInstanceState != null && savedInstanceState.getBoolean("alertShown")){
            Winner=savedInstanceState.getInt("Winner");
//            showWinner(Winner);
        }
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
        Log.d("pgorczyn", "123456: GameActivity.onSaveInstanceState entered");
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

        String sPlayer0="",sPlayer1="";
        this.Winner=Winner;

        // setup the alert builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);

        sPlayer1 = switch (GameType) {
            case (1) -> {
                sPlayer0 = "Player 1";
                yield "Player 2";
            }
            case (2) -> {
                sPlayer0 = "Player";
                yield "Android";
            }
            default -> sPlayer1;
        };

        if(Winner==0)
            builder.setMessage("The winner is "+sPlayer0);
        else
            builder.setMessage("The winner is "+sPlayer1);

        // add a button
        builder.setPositiveButton("Close", (dialog, which) -> {

            // do something like...
            finish();
        });

        // create and show the alert dialog
        dialogWinner = builder.create();
        dialogWinner.show();
    }



}
