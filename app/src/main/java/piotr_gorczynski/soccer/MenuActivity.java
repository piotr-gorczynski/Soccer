package piotr_gorczynski.soccer;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
    }

    public void OpenGame(View view) {
        // Do something in response to button
        //Log.d("pgorczyn", "123456: MyActivity.OpenBall entered");
        Intent intent = new Intent(this, GameActivity.class);
        startActivity(intent);
    }
}
