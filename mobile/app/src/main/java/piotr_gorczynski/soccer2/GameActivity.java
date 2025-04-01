package piotr_gorczynski.soccer2;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;

import java.util.ArrayList;

public class GameActivity extends AppCompatActivity {


    private ArrayList<MoveTo> Moves= new ArrayList<>();
    AlertDialog dialogWinner;
    int Winner=-1;
    int GameType=-1;
    GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));
        //this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        if (savedInstanceState != null && savedInstanceState.getParcelableArrayList("Moves")!=null) {
            //intBallX = savedInstanceState.getInt("intBallX");
            //intBallY = savedInstanceState.getInt("intBallY");
            Moves = savedInstanceState.getParcelableArrayList("Moves");
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
        if (GameType>0) {
            Log.d("pgorczyn", "123456: GameActivity.onCreate Game Type entered: " + GameType);
        }

        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this /* Activity context */);

        int androidLevel=1;
        if (sharedPreferences.contains("android_level")) {
            androidLevel = Integer.parseInt(sharedPreferences.getString("android_level", "1"));
            Log.d("pgorczynMove", "Preference android_level=" + androidLevel);
        }



        //else
        //    throw(new Exception("No Game Type defined")) ;

        //Log.d("pgorczyn", "123456: GameActivity.onCreate entered");
        gameView = new GameView(this, Moves,GameType,androidLevel);
        setContentView(gameView);

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

/*    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
//---retrieve the information persisted earlier---
        //Log.d("pgorczyn", "123456: GameActivity.onRestoreInstanceState entered");
        Moves = savedInstanceState.getParcelableArrayList("Moves");
        if(savedInstanceState.getBoolean("alertShown")){
            Winner=savedInstanceState.getInt("Winner");
            showWinner(Winner);
        }
    }*/

    public void showWinner(int Winner) {

        String sPlayer0="",sPlayer1="";
        this.Winner=Winner;

        // setup the alert builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);

        switch(GameType) {
            case(1):
                sPlayer0="Player 1";
                sPlayer1="Player 2";
                break;
            case(2):
                sPlayer0="Player";
                sPlayer1="Android";
                break;
        }

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
