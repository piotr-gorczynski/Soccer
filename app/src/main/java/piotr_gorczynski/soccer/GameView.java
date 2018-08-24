package piotr_gorczynski.soccer;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;

/**
 * Created by pgorczynski on 2016-05-13.
 */
public class GameView extends View {

    private MyHandler mHandler;
    private Field field;
    private GameActivity gameActivity;
    private int intFieldWidth,intFieldHeight;
    //private ArrayList<MoveTo> possibleMoves= new ArrayList<MoveTo>();
    private TreeMoveTo androidTreeMoveTo;
    private ArrayList<MoveTo> realMoves;//= new ArrayList<MoveTo>();
    ArrayList<MoveTo> possibleMovesForDrawing= new ArrayList<MoveTo>();
    private int GameType;



    public class MyHandler extends Handler
    {
        private GameView gameView;

         public MyHandler(GameView gameView)
        {
            this.gameView = gameView;
        }

        @Override
        public void handleMessage(Message msg)
        {
            super.handleMessage(msg);
            Log.d("pgorczyn", "123456: In handle message");
            gameView.androidMove( );
        }
    }


    // Constructor
    public GameView(Context context, ArrayList<MoveTo> argMoves, int argGameType) {
        super(context);

        mHandler = new MyHandler(this);
        GameType=argGameType;
        gameActivity=(GameActivity)context;
        setBackgroundColor(ContextCompat.getColor(context, R.color.colorGreenDark));



        Resources res = context.getResources();
        intFieldWidth = res.getInteger(R.integer.intFieldHalfWidth)*2;
        intFieldHeight = res.getInteger(R.integer.intFieldHalfHeight)*2;

        //Moves.addAll(argMoves);
        realMoves=argMoves;



        /*
        possibleMoves.add(new MoveTo(2,2,0));
        possibleMoves.add(new MoveTo(3,2,0));
        possibleMoves.add(new MoveTo(4,2,0));
        possibleMoves.add(new MoveTo(4,3,0));
*/


        field = new Field(context, realMoves, possibleMovesForDrawing, GameType);  // ARGB


        // To enable keypad
        this.setFocusable(true);
        this.requestFocus();
        // To enable touch mode
        this.setFocusableInTouchMode(true);


    }

     // Called back to draw the view. Also called after invalidate().
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.d("pgorczyn", "123456: onDraw");
        createPossibleMoves(possibleMovesForDrawing,realMoves);
        field.draw(canvas);
    }

    // Called back when the view is first created or its size changes.
    @Override
    public void onSizeChanged(int w, int h, int oldW, int oldH) {
        // Set the movement bounds for the ball
        field.set(1, 1, w, h - 1);
    }

    private void createPossibleMoves(ArrayList<MoveTo> possibleMoves, ArrayList<MoveTo> Moves  ){
        possibleMoves.clear();
        int xPos=Moves.get(Moves.size()-1).X;
        int yPos=Moves.get(Moves.size()-1).Y;
        for(int x=xPos-1;x<=xPos+1;x++)
            for(int y=yPos-1;y<=yPos+1;y++){
                if(
                    //left border
                    (((x>=0) && (xPos>0)) || ((x>0) && (xPos==0)))
                    //right border
                    && (((x<=intFieldWidth) && (xPos<intFieldWidth)) || ((x<intFieldWidth) && (xPos==intFieldWidth)))
                    && (
                        //upper line below
                        ((y>=0) && (yPos>0))
                        //upper line left side
                        || ((xPos<intFieldWidth/2-1) && (yPos==0) && (y>0))
                        //upper line left gate
                        || ((xPos==intFieldWidth/2-1) && (yPos==0) && ((x==intFieldWidth/2) || (y>0)))
                        //upper line middle
                        || ((xPos==intFieldWidth/2) && (yPos==0))
                        //upper line right gate
                        || ((xPos==intFieldWidth/2+1) && (yPos==0) && ((x==intFieldWidth/2) || (y>0)))
                        //upper line right side
                        || ((xPos>intFieldWidth/2+1) && (yPos==0) && (y>0))
                        )
                    && (
                        //bottom line above
                        ((y<=intFieldHeight) && (yPos<intFieldHeight))
                        //bottom line left side
                        || ((xPos<intFieldWidth/2-1) && (yPos==intFieldHeight) && (y<intFieldHeight))
                        //bottom line left gate
                        || ((xPos==intFieldWidth/2-1) && (yPos==intFieldHeight) && ((x==intFieldWidth/2) || (y<intFieldHeight)))
                        //bottom line middle
                        || ((xPos==intFieldWidth/2) && (yPos==intFieldHeight))
                        //bottom line right gate
                        || ((xPos==intFieldWidth/2+1) && (yPos==intFieldHeight) && ((x==intFieldWidth/2) || (y<intFieldHeight)))
                        //bottom line right side
                        || ((xPos>intFieldWidth/2+1) && (yPos==intFieldHeight) && (y<intFieldHeight))
                        )
                    && thereIsNoLine(xPos,yPos,x,y, Moves)
                    //and we are not checking the same point
                    && !((xPos==x) && (yPos==y))
                    )
                    possibleMoves.add(new MoveTo(x,y,-1));
            }
    }

    private boolean thereIsNoLine(int x1,int y1, int x2, int y2, ArrayList<MoveTo> Moves){
        int xS,yS,xE,yE;
        xS=Moves.get(0).X;
        yS=Moves.get(0).Y;
 /*       if((x1==3)&&(y1==3)&&(x2==3)&&(y2==4))
            xS=Moves.get(0).X;*/

        for(int i=1;i<Moves.size();i++) {
            xE=Moves.get(i).X;
            yE=Moves.get(i).Y;
            if((x1==xS)&&(y1==yS)&&(x2==xE)&&(y2==yE))
                return false;
            xS=xE;
            yS=yE;
        }
        xS=Moves.get(0).X;
        yS=Moves.get(0).Y;
        for(int i=1;i<Moves.size();i++) {
            xE=Moves.get(i).X;
            yE=Moves.get(i).Y;
            if((x1==xE)&&(y1==yE)&&(x2==xS)&&(y2==yS))
                return false;
            xS=xE;
            yS=yE;
        }
        return true;
    }

    private boolean isBouncing(int x, int y,ArrayList<MoveTo> Moves) {
        if(
            ((x==0) && (y>0) && (y<intFieldHeight))
            ||((x==intFieldWidth) && (y>0) && (y<intFieldHeight))
            ||((y==0)&&(x<intFieldWidth/2)&&(x>0))
            ||((y==0)&&(x>intFieldWidth/2)&&(x<intFieldWidth))
            ||((y==intFieldHeight)&&(x<intFieldWidth/2)&&(x>0))
            ||((y==intFieldHeight)&&(x>intFieldWidth/2)&&(x<intFieldWidth))
            ||wasBallThere(x,y,Moves))
            return true;
        else
            return false;
    }

    private boolean wasBallThere(int x, int y, ArrayList<MoveTo> Moves){
        for(MoveTo i: Moves) {
            if((x==i.X) && (y==i.Y)) return true;
        }
        return false;
    }

    private boolean isMoveValid(int x, int y, ArrayList<MoveTo> possibleMoves){
        for(MoveTo i: possibleMoves) {
            if((x==i.X) && (y==i.Y)) return true;
        }
        return false;
    }

    public boolean MakeMove(int x, int y,ArrayList<MoveTo> Moves){
        ArrayList<MoveTo> possibleMoves= new ArrayList<MoveTo>();
        boolean bouncing=isBouncing(x,y,Moves);

        if(bouncing)
            Moves.add(new MoveTo(x,y,Moves.get(Moves.size()-1).P));
        else
            if(Moves.get(Moves.size()-1).P==0)
                Moves.add(new MoveTo(x,y,1));
            else
                Moves.add(new MoveTo(x,y,0));

        createPossibleMoves(possibleMoves, Moves);

        if(possibleMoves.size()==0) {
            if(y==-1)
                gameActivity.showWinner(0);
            else {
                if(y==intFieldHeight+1)
                    gameActivity.showWinner(1);
                else {
                    if(bouncing){
                        if(Moves.get(Moves.size()-1).P==0)
                            gameActivity.showWinner(1);
                        else
                            gameActivity.showWinner(0);
                    }
                    else
                        gameActivity.showWinner((Moves.get(Moves.size()-1).P));
                }
            }
            return false;
        }
        return true;
    }

    public int checkWinner(int x, int y,ArrayList<MoveTo> Moves){
        ArrayList<MoveTo> nextMoves= (ArrayList<MoveTo>) Moves.clone();
        boolean bouncing=isBouncing(x,y,Moves);

        if(bouncing)
            nextMoves.add(new MoveTo(x,y,Moves.get(Moves.size()-1).P));
        else
        if(nextMoves.get(Moves.size()-1).P==0)
            nextMoves.add(new MoveTo(x,y,1));
        else
            nextMoves.add(new MoveTo(x,y,0));

        ArrayList<MoveTo> possibleMoves= new ArrayList<MoveTo>();
        createPossibleMoves(possibleMoves, nextMoves);

        if(possibleMoves.size()==0) {
            if(y==-1)
                return 0;
            else {
                if(y==intFieldHeight+1)
                    return 1;
                else {
                    if(bouncing){
                        if(Moves.get(Moves.size()-1).P==0)
                            return 1;
                        else
                            return 0;
                    }
                    else
                        return (Moves.get(Moves.size()-1).P);
                }
            }
        }
        return -1;
    }


    public boolean androidNextMove(ArrayList<MoveTo> Moves,MoveTo minMoveTo) {
        Log.d("pgorczyn", "1234567: Start possible moves evaluation");
        ArrayList<MoveTo> possibleMoves= new ArrayList<MoveTo>();
        createPossibleMoves(possibleMoves,Moves);
        if(possibleMoves.size()==0) return false;
        ArrayList<MoveTo> bestMoves= (ArrayList<MoveTo>) Moves.clone();
        ArrayList<MoveTo> tempMoves= (ArrayList<MoveTo>) Moves.clone();
        int movesSize = Moves.size();
        boolean nextMoveFound=false;
        for(MoveTo i: possibleMoves) {
            if (isBouncing(i.X, i.Y, Moves)) {
                tempMoves.add(new MoveTo(i.X,i.Y,1));
                if(androidNextMove(tempMoves, minMoveTo)==true) {
                    while (bestMoves.size()>movesSize) bestMoves.remove(bestMoves.size()-1);
                    for(int j=bestMoves.size();j<tempMoves.size();j++)
                        bestMoves.add(new MoveTo(tempMoves.get(j).X,tempMoves.get(j).Y,tempMoves.get(j).P));
                    nextMoveFound=true;
                }
                while (tempMoves.size()>movesSize) tempMoves.remove(tempMoves.size()-1);
            } else {
                if ((MINMAX(i.X, i.Y, 1) < MINMAX(minMoveTo.X, minMoveTo.Y, 1)) || (checkWinner(minMoveTo.X, minMoveTo.Y,Moves)==0)) {
                    minMoveTo.X = i.X;
                    minMoveTo.Y = i.Y;
                    while (bestMoves.size()>movesSize) bestMoves.remove(bestMoves.size()-1);
                    bestMoves.add(new MoveTo(minMoveTo.X,minMoveTo.Y,1));
                    nextMoveFound = true;
                }
            }
        }
        if(nextMoveFound==true)
            for(int j=Moves.size();j<bestMoves.size();j++)
                Moves.add(new MoveTo(bestMoves.get(j).X,bestMoves.get(j).Y,bestMoves.get(j).P));
        Log.d("pgorczyn", "1234567: After moves evaluation");
        return nextMoveFound;
    }


    public void androidMove() {
        ArrayList<MoveTo> possibleMoves= new ArrayList<MoveTo>();
        createPossibleMoves(possibleMoves,realMoves);
        if((GameType==2) && (realMoves.get(realMoves.size()-1).P==1) && (possibleMoves.size()>0)) {
            Log.d("pgorczyn", "1234567: In androidMove");
            ArrayList<MoveTo> androidMoves= (ArrayList<MoveTo>) realMoves.clone();
            //assigning first form the list as MIN
            MoveTo minMoveTo =  new MoveTo(possibleMoves.get(0).X,possibleMoves.get(0).Y,1);
            if(androidNextMove(androidMoves,minMoveTo)==true)
                if(androidMoves.size()>realMoves.size()) {//just to be 100% sure
                    Log.d("pgorczyn", "1234567: Making move");
                    SystemClock.sleep(1000);
                    boolean nextMovePossible = MakeMove(androidMoves.get(realMoves.size()).X, androidMoves.get(realMoves.size()).Y, realMoves);

                    invalidate();
                    if ( nextMovePossible == true) {
                        if (realMoves.get(realMoves.size() - 1).P == 1) {
                            Log.d("pgorczyn", "1234567: Before sleep");
                            //SystemClock.sleep(1000);
                            Log.d("pgorczyn", "1234567: After sleep");
                            Log.d("pgorczyn", "1234567: Sending message for androidMove");
                            mHandler.sendEmptyMessageDelayed(1,1000);
                        }
                    }
                }
        }
    }

    //MINMAX Evaluation function
    public int MINMAX(int x, int y, int p) {
        //for p=0 we are returning distance from 1's gate
        if(p==0){
            return Math.max(Math.abs(-1-y),Math.abs(intFieldWidth/2-x));
        }
        else
            return Math.max(Math.abs(intFieldHeight+1-y),Math.abs(intFieldWidth/2-x));
    }


    // Touch-input handler
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x,y;
        boolean bouncing;
        ArrayList<MoveTo> possibleMoves= new ArrayList<MoveTo>();
        //if ((event.getAction()==MotionEvent.ACTION_DOWN) || (event.getAction()==MotionEvent.ACTION_MOVE) || (event.getAction()==MotionEvent.ACTION_UP) ) {
        if (event.getAction()==MotionEvent.ACTION_UP)  {
            if(getHeight()>getWidth()){
                x=field.x2w(event.getX());
                y=field.y2h(event.getY());
            } else {
                y=intFieldHeight-field.x2w(event.getX());
                x=intFieldWidth-field.y2h(event.getY());
            }
            createPossibleMoves(possibleMoves,realMoves);
            if(isMoveValid(x,y,possibleMoves)) {
                MakeMove(x,y,realMoves);
                Log.d("pgorczyn", "123456: Before invalidate");
                Log.d("pgorczyn", "MINMAX:" + Integer.toString(MINMAX(x, y, realMoves.get(realMoves.size() - 1).P)));

                invalidate();
                Log.d("pgorczyn", "123456: After invalidate");
                //if plays with Android and it is Android move and there are possible MOves
                if((GameType ==2) && (realMoves.get(realMoves.size()-1).P==1) && (possibleMoves.size()>0))
                    //Send message for Android to move
                    mHandler.sendEmptyMessage(1);
            }
        }
        return true;  // Event handled
    }
}