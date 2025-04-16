package piotr_gorczynski.soccer2;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.content.SharedPreferences;
import android.widget.TextView;


public class MenuActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String nickname = prefs.getString("nickname", null);

        TextView nicknameLabel = findViewById(R.id.nicknameLabel);
        if (nickname != null && !nickname.isEmpty()) {
            nicknameLabel.setText(getString(R.string.hello_comma) + nickname + getString(R.string.exclemation));
        } else {
            nicknameLabel.setText(getString(R.string.welcome_to_soccer));
        }
    }


    public void OpenGamePlayerVsPlayer(View view) {
        // Do something in response to button
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("GameType",1);
        startActivity(intent);
    }

    public void OpenGamePlayerVsAndroid(View view) {
        // Do something in response to button
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("GameType",2);
        startActivity(intent);
    }

    public void OpenSettings(View view) {
        // Do something in response to button
        Intent intent = new Intent(this, SettingsActivity.class);
  //      intent.putExtra( SettingsActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.LevelPreferenceFragment.class.getName() );
    //    intent.putExtra( SettingsActivity.EXTRA_NO_HEADERS, true );
        startActivity(intent);
    }

    public void OpenAccount(View view) {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
//---save whatever you need to persist—
        Log.d("pgorczyn", "123456: MenuActivity.onSaveInstanceState entered");
        //intBallX=gameView.getBallX();
        //intBallY=gameView.getBallY();
        //Moves=gameView.GetMoves();
        //Log.d("pgorczyn", "123456: intBallX=" + intBallX);
        //outState.putInt("intBallX", intBallX);
        //outState.putInt("intBallY", intBallY);
/*        outState.putParcelableArrayList("Moves",Moves);



        if(dialogWinner != null )
            // close dialog to prevent leaked window
            dialogWinner.dismiss();
        if(Winner!=-1){
            outState.putBoolean("alertShown", true);
            outState.putInt("Winner", Winner);
        }
        else
            outState.putBoolean("alertShown", false);
*/

        super.onSaveInstanceState(outState);
    }



}
