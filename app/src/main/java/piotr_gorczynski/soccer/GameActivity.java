package piotr_gorczynski.soccer;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

public class GameActivity extends AppCompatActivity {

    private GameView gameView;
    private ArrayList<MoveTo> Moves=new ArrayList<MoveTo>();
    AlertDialog dialogWinner;
    int Winner=-1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        if (savedInstanceState != null) {
            //intBallX = savedInstanceState.getInt("intBallX");
            //intBallY = savedInstanceState.getInt("intBallY");
            Moves = savedInstanceState.getParcelableArrayList("Moves");
        }
        else {
            Moves.add( new MoveTo(getResources().getInteger(R.integer.intFieldHalfWidth),getResources().getInteger(R.integer.intFieldHalfHeight),0));
        }

        //Set initial Ball position in the Middle of the Field
        //intBallX=getResources().getInteger(R.integer.intFieldHalfWidth);
        //intBallY=getResources().getInteger(R.integer.intFieldHalfHeight);


        //Log.d("pgorczyn", "123456: GameActivity.onCreate entered");
        //Log.d("pgorczyn", "123456: intBallX=" + intBallX);
        //gameView = new GameView(this,intBallX,intBallY,Moves);
        gameView = new GameView(this, Moves);
        setContentView(gameView);
        gameView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorGreenDark));

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
        //intBallX=gameView.getBallX();
        //intBallY=gameView.getBallY();
        //Moves=gameView.GetMoves();
        //Log.d("pgorczyn", "123456: intBallX=" + intBallX);
        //outState.putInt("intBallX", intBallX);
        //outState.putInt("intBallY", intBallY);
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
        //intBallX = savedInstanceState.getInt("intBallX");
        //intBallY = savedInstanceState.getInt("intBallY");
        //Log.d("pgorczyn", "123456: intBallX=" + intBallX);
        Moves = savedInstanceState.getParcelableArrayList("Moves");
        if(savedInstanceState.getBoolean("alertShown")){
            Winner=savedInstanceState.getInt("Winner");
            showWinner(Winner);
        }
    }*/

    public void showWinner(int Winner) {

        this.Winner=Winner;
        // setup the alert builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        if(Winner==0)
            builder.setMessage("The winner is Player 1");
        else
            builder.setMessage("The winner is Player 2");

        // add a button
        builder.setPositiveButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                // do something like...
                finish();
            }
        });

        // create and show the alert dialog
        dialogWinner = builder.create();
        dialogWinner.show();
    }



}
