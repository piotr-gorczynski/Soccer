package piotr_gorczynski.soccer2;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Objects;

import android.util.AttributeSet;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;


//Added for compatibility

/**
 * Created by pgorczynski on 2016-05-13.
 */
public class GameView extends View {

    private final MyHandler mHandler;
    private final Field field;
    private final GameActivity gameActivity;
    private final int intFieldWidth;
    private final int intFieldHeight;
    //private ArrayList<MoveTo> possibleMoves= new ArrayList<MoveTo>();
    private final ArrayList<MoveTo> realMoves;//= new ArrayList<MoveTo>();
    final ArrayList<MoveTo> possibleMovesForDrawing = new ArrayList<>();
    ArrayList<MoveTo> androidMoves = new ArrayList<>();
    private final int GameType;
    private final int androidLevel;
    private MoveCallback moveCallback;

    private int localPlayerIndex=0;

    private long remTime0, remTime1;

    private Long turnStartsTime=null;

    private boolean inputEnabled = true;
    /** Disable or re-enable all touches on the board */
    public void setInputEnabled(boolean enabled) {
        this.inputEnabled = enabled;
    }

    @SuppressWarnings("unused")
    public interface MoveCallback {
        void onLocalMove(int x, int y, int p);
    }

    public void setMoveCallback(MoveCallback cb) {
        this.moveCallback = cb;
    }

    public void replaceMoves(ArrayList<MoveTo> newMoves) {
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Started. newMoves.size=" + newMoves.size());
        this.realMoves.clear();
        this.realMoves.addAll(newMoves);
    }

    public static class MyHandler extends Handler {
        private final GameView gameView;

        public MyHandler(GameView gameView) {
            super(Looper.getMainLooper());
            this.gameView = gameView;
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Started");
            gameView.androidMove();
        }
    }

    public static class NextMoveFound {
        public boolean found;
        public boolean defeat;
        public int bouncingLevel;
        public boolean victory;

        public NextMoveFound(boolean found, int bouncingLevel, boolean defeat, boolean victory) {
            this.found = found;
            this.bouncingLevel = bouncingLevel;
            this.defeat = defeat;
            this.victory=victory;
        }
    }

    public GameView(Context context) {
        super(context);
        throw new UnsupportedOperationException("GameView must be created programmatically with all required parameters.");
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        throw new UnsupportedOperationException("GameView must be created programmatically with all required parameters.");
    }

    public GameView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        throw new UnsupportedOperationException("GameView must be created programmatically with all required parameters.");
    }


    // Constructor

    public GameView(Context context, ArrayList<MoveTo> argMoves, int argGameType, String player0Name, String player1Name, int localPlayerIndex, long time0, long time1, Long turnStartTime) {
        super(context);
        // simpler log—no reflection, no nulls
        Log.d("TAG_Soccer",getClass().getSimpleName() + ".<init>: Started, received argMoves.size=" + argMoves.size());
        this.localPlayerIndex = localPlayerIndex;
        mHandler = new MyHandler(this);
        GameType = argGameType;
        androidLevel = 0;  // unused for GameType 3
        gameActivity = (GameActivity) context;
        setBackgroundColor(ContextCompat.getColor(context, R.color.colorGreenDark));

        Resources res = context.getResources();
        intFieldWidth = res.getInteger(R.integer.intFieldHalfWidth) * 2;
        intFieldHeight = res.getInteger(R.integer.intFieldHalfHeight) * 2;

        realMoves = argMoves;

        this.remTime0 = time0;
        this.remTime1 = time1;

        // construct Field with custom nicknames
        field = new Field(context, realMoves, possibleMovesForDrawing, GameType, player0Name, player1Name, localPlayerIndex);

        this.setFocusable(true);
        this.requestFocus();
        this.setFocusableInTouchMode(true);

        // pass the clock values into the Field
        field.setRemainingTimes(remTime0, remTime1, turnStartTime);
        // No Android move logic here, because GameType 3 is human vs. human
    }

    // allow later updates
    public void updateTimes(long time0, long time1, Long ts) {
        remTime0 = time0;
        remTime1 = time1;
        turnStartsTime = ts;
        field.setRemainingTimes(time0, time1, ts);
        invalidate();
    }

    public GameView(Context context, ArrayList<MoveTo> argMoves, int argGameType,int androidLevel) {
        super(context);

        mHandler = new MyHandler(this);
        GameType = argGameType;
        this.androidLevel=androidLevel;
        gameActivity = (GameActivity) context;
        setBackgroundColor(ContextCompat.getColor(context, R.color.colorGreenDark));


        Resources res = context.getResources();
        intFieldWidth = res.getInteger(R.integer.intFieldHalfWidth) * 2;
        intFieldHeight = res.getInteger(R.integer.intFieldHalfHeight) * 2;

        //Moves.addAll(argMoves);
        realMoves = argMoves;



        /* This can be used for debugging
        possibleMoves.add(new MoveTo(2,2,0));
        possibleMoves.add(new MoveTo(3,2,0));
        possibleMoves.add(new MoveTo(4,2,0));
        possibleMoves.add(new MoveTo(4,3,0));
        */


        field = new Field(context, realMoves, possibleMovesForDrawing, GameType, "Player 1", "Player 2",0);

        // To enable keypad
        this.setFocusable(true);
        this.requestFocus();
        // To enable touch mode
        this.setFocusableInTouchMode(true);

        //if plays with Android and it is Android move and there are possible MOves
        if ((GameType == 2) && (realMoves.get(realMoves.size() - 1).P == 1))
            //Send message for Android to move
            mHandler.sendEmptyMessage(1);
    }

    @Override
    public boolean performClick() {
        // Call the super method
        super.performClick();
        // Handle the click action
        // You can put your custom click logic here
        return true;
    }

    // Called back to draw the view. Also called after invalidate().
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Started");

        if (realMoves.isEmpty()) {
            Log.w("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Skipped: realMoves is empty");
            return;
        }

        createPossibleMoves(possibleMovesForDrawing, realMoves);
        field.setRemainingTimes(remTime0, remTime1,turnStartsTime);
        field.draw(canvas);
    }


    // Called back when the view is first created or its size changes.
    @Override
    public void onSizeChanged(int w, int h, int oldW, int oldH) {
        // Set the movement bounds for the ball
        field.set(1, 1, w, h - 1);
    }

    private void createPossibleMoves(ArrayList<MoveTo> possibleMoves, ArrayList<MoveTo> Moves) {
        possibleMoves.clear();
        int xPos = Moves.get(Moves.size() - 1).X;
        int yPos = Moves.get(Moves.size() - 1).Y;
        int x = xPos;

        for (int y = yPos + 1; y >= yPos - 1; y--)
            for (int i = 0; i <= 2; i++) {
                x = switch (i) {
                    case 0 -> xPos; //redundant, but I left it on purpose
                    case 1 -> xPos - 1;
                    case 2 -> xPos + 1;
                    default -> x;
                };

                if (
                    //left border
                        (((x >= 0) && (xPos > 0)) || ((x > 0) && (xPos == 0)))
                                //right border
                                && (((x <= intFieldWidth) && (xPos < intFieldWidth)) || ((x < intFieldWidth) && (xPos == intFieldWidth)))
                                && (
                                //upper line below
                                ((y >= 0) && (yPos > 0))
                                        //upper line left side
                                        || ((xPos < intFieldWidth / 2 - 1) && (yPos == 0) && (y > 0))
                                        //upper line left gate
                                        || ((xPos == intFieldWidth / 2 - 1) && (yPos == 0) && ((x == intFieldWidth / 2) || (y > 0)))
                                        //upper line middle
                                        || ((xPos == intFieldWidth / 2) && (yPos == 0))
                                        //upper line right gate
                                        || ((xPos == intFieldWidth / 2 + 1) && (yPos == 0) && ((x == intFieldWidth / 2) || (y > 0)))
                                        //upper line right side
                                        || ((xPos > intFieldWidth / 2 + 1) && (yPos == 0) && (y > 0))
                        )
                                && (
                                //bottom line above
                                ((y <= intFieldHeight) && (yPos < intFieldHeight))
                                        //bottom line left side
                                        || ((xPos < intFieldWidth / 2 - 1) && (yPos == intFieldHeight) && (y < intFieldHeight))
                                        //bottom line left gate
                                        || ((xPos == intFieldWidth / 2 - 1) && (yPos == intFieldHeight) && ((x == intFieldWidth / 2) || (y < intFieldHeight)))
                                        //bottom line middle
                                        || ((xPos == intFieldWidth / 2) && (yPos == intFieldHeight))
                                        //bottom line right gate
                                        || ((xPos == intFieldWidth / 2 + 1) && (yPos == intFieldHeight) && ((x == intFieldWidth / 2) || (y < intFieldHeight)))
                                        //bottom line right side
                                        || ((xPos > intFieldWidth / 2 + 1) && (yPos == intFieldHeight) && (y < intFieldHeight))
                        )
                                && thereIsNoLine(xPos, yPos, x, y, Moves)
                                //and we are not checking the same point
                                && !((xPos == x) && (yPos == y))
                        )
                    possibleMoves.add(new MoveTo(x, y, -1));

            }

    }

    private boolean thereIsNoLine(int x1,int y1, int x2, int y2, ArrayList<MoveTo> Moves){
        int xS,yS,xE,yE;
        xS=Moves.get(0).X;
        yS=Moves.get(0).Y;

        for(int i=1;i<Moves.size();i++) {
            xE=Moves.get(i).X;
            yE=Moves.get(i).Y;
            if( ( (x1==xS)&&(y1==yS)&&(x2==xE)&&(y2==yE) ) || ((x1==xE)&&(y1==yE)&&(x2==xS)&&(y2==yS) ) )
                return false;
            xS=xE;
            yS=yE;
        }
        return true;
    }

    private boolean isBouncing(int x, int y,ArrayList<MoveTo> Moves) {
        return ((x == 0) && (y >= 0) && (y <= intFieldHeight))
                || ((x == intFieldWidth) && (y >= 0) && (y <= intFieldHeight))
                || ((y == 0) && (x < intFieldWidth / 2) && (x > 0))
                || ((y == 0) && (x > intFieldWidth / 2) && (x < intFieldWidth))
                || ((y == intFieldHeight) && (x < intFieldWidth / 2) && (x > 0))
                || ((y == intFieldHeight) && (x > intFieldWidth / 2) && (x < intFieldWidth))
                || wasBallThere(x, y, Moves);
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

    public int checkWinnerFromMoves(ArrayList<MoveTo> moves) {
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Started");
        MoveTo last = moves.get(moves.size() - 1);
        return checkWinner(last.X, last.Y, new ArrayList<>(moves));
    }

    public boolean MakeMove(int x, int y,ArrayList<MoveTo> Moves){
        ArrayList<MoveTo> possibleMoves= new ArrayList<>();
        boolean bouncing=isBouncing(x,y,Moves);

        if(bouncing)
            Moves.add(new MoveTo(x,y,Moves.get(Moves.size()-1).P));
        else
            Moves.add(new MoveTo(x,y,pOpponent(Moves.get(Moves.size()-1).P)));

        if (GameType == 3) {

            int lastP = Moves.get(Moves.size() - 1).P;
            if (localPlayerIndex != lastP) {
                Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Catching early change player turn");
                // pass the clock values into the Field
                turnStartsTime=null;
                this.gameActivity.stopClock();
                field.setRemainingTimes(remTime0, remTime1,null);
            }
            invalidate();

            // Send to Firestore if still active
            if (moveCallback != null) {
                moveCallback.onLocalMove(x, y, Moves.get(Moves.size() - 1).P);
            }
        }

        createPossibleMoves(possibleMoves, Moves);

        if(possibleMoves.isEmpty()) {
            if(y==-1)
                gameActivity.showWinner(0);
            else {
                if(y==intFieldHeight+1)
                    gameActivity.showWinner(1);
                else {
                    if(bouncing){
                        gameActivity.showWinner(pOpponent(Moves.get(Moves.size()-1).P));
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
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Started");
        boolean bouncing=isBouncing(x,y,Moves);
        ArrayList<MoveTo> nextMoves = new ArrayList<>(Moves);
        if(bouncing)
            nextMoves.add(new MoveTo(x,y,Moves.get(Moves.size()-1).P));
        else
            nextMoves.add(new MoveTo(x,y,pOpponent(Moves.get(Moves.size()-1).P)));

        ArrayList<MoveTo> possibleMoves= new ArrayList<>();

        createPossibleMoves(possibleMoves, nextMoves);
        //createPossibleMoves(possibleMoves, Moves);

        if(possibleMoves.isEmpty()) {
            if(y==-1)
                return 0;
            else {
                if(y==intFieldHeight+1)
                    return 1;
                else {
                    if(bouncing){
                        return pOpponent(nextMoves.get(nextMoves.size()-1).P);
                    }
                    else
                        return (nextMoves.get(nextMoves.size()-1).P);
                }
            }
        }
        return -1;
    }

    public int pOpponent(int p){
        if(p==0)
            return 1;
        else
            return 0;
    }

    public boolean androidNextMove_v2(ArrayList<MoveTo> Moves,MoveTo masterMinMoveTo, int bouncingLevel, NextMoveFound masterNextMoveFound, int treeDepthLevel, long startThinkingTime) {
        String stringMove=Moves.get(Moves.size()-1).toString();
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": <"+stringMove+" bouncinglevel='"+ bouncingLevel +"' treedepthlevel='"+ treeDepthLevel +"'>");
        logMoves(Moves);
        long difference;
        long startTime = System.currentTimeMillis();
        NextMoveFound nextMoveFound = new NextMoveFound(masterNextMoveFound.found, 0, false, false);
        NextMoveFound tempNextMoveFound = new NextMoveFound(false, 0, false, false);
        boolean localNextMoveFound=false;
        boolean tempFound;
        ArrayList<MoveTo> possibleMoves= new ArrayList<>();
        createPossibleMoves(possibleMoves,Moves);
        if(!possibleMoves.isEmpty()) {
            MoveTo minMoveTo = new MoveTo(masterMinMoveTo.X, masterMinMoveTo.Y, -1);
            MoveTo tempMinMoveTo = new MoveTo(0, 0, -1);
            ArrayList<MoveTo> bestMoves = new ArrayList<>(Moves);
            ArrayList<MoveTo> tempMoves = new ArrayList<>(Moves);
            int movesSize = Moves.size();

            for (MoveTo i : possibleMoves) {

                //if victory then do not search any more
                if(nextMoveFound.victory)
                    break;

                //if too many bouncing levels
                int gameBouncingLevel = 50;
                if((nextMoveFound.found) && (nextMoveFound.bouncingLevel> gameBouncingLevel)) {
                    Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": <gameBouncingLevelReached>" + gameBouncingLevel + "</gameBouncingLevelReached>");
                    break;
                }

                difference = (System.currentTimeMillis() - startThinkingTime)/1000;
                if((nextMoveFound.found) && (difference>androidLevel)) {
                    Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": <timeLimitReached>" + difference + "</timeLimitReached>");
                    break;
                }

                //if already landed in the gate
                if((checkWinner(i.X, i.Y, Moves) == 1) && (Moves.get(Moves.size() - 1).P==1)){
                    minMoveTo.X = i.X;
                    minMoveTo.Y = i.Y;
                    while (bestMoves.size() > movesSize)
                        bestMoves.remove(bestMoves.size() - 1);
                    bestMoves.add(new MoveTo(minMoveTo.X, minMoveTo.Y, Moves.get(Moves.size() - 1).P));
                    Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": <minimumfound1>" + minMoveTo + "</minimumfound1>");
                    nextMoveFound.found = true;
                    nextMoveFound.bouncingLevel = bouncingLevel;
                    nextMoveFound.victory=true;
                    localNextMoveFound = true;
                    break;
                }

                int gameTreeDepthLevel = 1;
                if (!(isBouncing(i.X, i.Y, Moves) || treeDepthLevel< gameTreeDepthLevel))
                {
                    if (((MINMAX(i.X, i.Y, Moves.get(Moves.size()-1).P) < MINMAX(minMoveTo.X, minMoveTo.Y, Moves.get(Moves.size()-1).P)) ) || (!nextMoveFound.found )) {
                        minMoveTo.X = i.X;
                        minMoveTo.Y = i.Y;
                        while (bestMoves.size() > movesSize)
                            bestMoves.remove(bestMoves.size() - 1);
                        bestMoves.add(new MoveTo(minMoveTo.X, minMoveTo.Y, Moves.get(Moves.size() - 1).P));
                        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": <minimumfound player='"+ Moves.get(Moves.size() - 1).P +"' MINMAX='" + MINMAX(minMoveTo.X, minMoveTo.Y, Moves.get(Moves.size() - 1).P) +"'>" + minMoveTo + "</minimumfound>");
                        nextMoveFound.found = true;
                        nextMoveFound.bouncingLevel = bouncingLevel;
                        localNextMoveFound = true;
                        //if already landed in the gate
                        if( (checkWinner(i.X, i.Y, Moves) == 1) && (Moves.get(Moves.size() - 1).P==1) ) {
                            nextMoveFound.victory = true;
                            break;
                        }
                        if(checkWinner(i.X, i.Y, Moves) == 0) {
                            Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": <defeat>" + stringMove + "</defeat>");
                            nextMoveFound.defeat=true;
                            break;
                        }
                    }
                }
                else {
                    if( isBouncing(i.X, i.Y, Moves) ) {
                        tempMoves.add(new MoveTo(i.X, i.Y, Moves.get(Moves.size() - 1).P));
                        tempMinMoveTo.X=minMoveTo.X;
                        tempMinMoveTo.Y=minMoveTo.Y;
                        tempNextMoveFound.found=nextMoveFound.found;
                        tempNextMoveFound.bouncingLevel=nextMoveFound.bouncingLevel;
                        tempNextMoveFound.defeat=false;
                        tempNextMoveFound.victory=false;
                        tempFound=androidNextMove_v2(tempMoves, tempMinMoveTo,bouncingLevel+1,tempNextMoveFound, treeDepthLevel,startThinkingTime);
                    }
                    else {
                        tempMoves.add(new MoveTo(i.X, i.Y, pOpponent(Moves.get(Moves.size() - 1).P)));
                        tempMinMoveTo.X=0;
                        tempMinMoveTo.Y=0;
                        tempNextMoveFound.found=false;
                        tempNextMoveFound.bouncingLevel=0;
                        tempNextMoveFound.defeat=false;
                        tempNextMoveFound.victory=false;
                        tempFound=androidNextMove_v2(tempMoves, tempMinMoveTo,bouncingLevel,tempNextMoveFound, treeDepthLevel+1,startThinkingTime);
                    }
                    if ( tempFound ) {
                        if (((MINMAX(tempMinMoveTo.X, tempMinMoveTo.Y, Moves.get(Moves.size()-1).P) < MINMAX(minMoveTo.X, minMoveTo.Y, Moves.get(Moves.size()-1).P)) ) || (!nextMoveFound.found )) {
                            minMoveTo.X = tempMinMoveTo.X;
                            minMoveTo.Y = tempMinMoveTo.Y;
                            while (bestMoves.size() > movesSize)
                                bestMoves.remove(bestMoves.size() - 1);
                            for (int j = bestMoves.size(); j < tempMoves.size(); j++)
                                bestMoves.add(new MoveTo(tempMoves.get(j).X, tempMoves.get(j).Y, tempMoves.get(j).P));
                            Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": <minimumfound player='"+ Moves.get(Moves.size() - 1).P +"' MINMAX='"+ MINMAX(minMoveTo.X, minMoveTo.Y, Moves.get(Moves.size() - 1).P) +"'>" + minMoveTo + "</minimumfound>");
                            localNextMoveFound=true;
                            nextMoveFound.found = true;
                            nextMoveFound.bouncingLevel = tempNextMoveFound.bouncingLevel;
                            nextMoveFound.victory=tempNextMoveFound.victory;
                        }
                    }
                    if(tempNextMoveFound.defeat && Moves.get(Moves.size() - 1).P==0) {
                        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": <defeat>" + stringMove + "</defeat>");
                        nextMoveFound.defeat=true;
                        masterNextMoveFound.defeat= true;
                        break;
                    }
                    while (tempMoves.size() > movesSize)
                        tempMoves.remove(tempMoves.size() - 1);
                    if(nextMoveFound.victory)
                        break;
                }
            }
            if (localNextMoveFound ) {
                if(Moves.get(Moves.size() - 1).P==0)
                    masterNextMoveFound.defeat=nextMoveFound.defeat;
                for (int j = Moves.size(); j < bestMoves.size(); j++)
                    Moves.add(new MoveTo(bestMoves.get(j).X, bestMoves.get(j).Y, bestMoves.get(j).P));
                masterNextMoveFound.found=true;
                masterNextMoveFound.bouncingLevel=nextMoveFound.bouncingLevel;
                masterNextMoveFound.victory=nextMoveFound.victory;
                masterMinMoveTo.X=minMoveTo.X;
                masterMinMoveTo.Y=minMoveTo.Y;
            }
        }
        difference = System.currentTimeMillis() - startTime;
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": <secondselapsed>"+ difference / 1000 +"</secondselapsed>");
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": </"+stringMove+">");
        return localNextMoveFound;
    }


    public void androidMove() {

        if (GameType != 2)
            return; //throw error to be added
        if (realMoves.get(realMoves.size() - 1).P != 1)
            return; //throw error to be added
        ArrayList<MoveTo> possibleMoves = new ArrayList<>();
        createPossibleMoves(possibleMoves, realMoves);
        if (possibleMoves.isEmpty()) return; //throw error to be added
        //called 1-st time
        if (androidMoves.size() <= realMoves.size()) {
            Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": In androidMove 1-st time");
            androidMoves = new ArrayList<>(realMoves);
            //assigning first form the list as MIN
            MoveTo minMoveTo = new MoveTo(possibleMoves.get(0).X, possibleMoves.get(0).Y, 1);
            NextMoveFound nextMoveFound = new NextMoveFound(false, 0, false, false);
            Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": <?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            androidNextMove_v2(androidMoves, minMoveTo,0,nextMoveFound, 0, System.currentTimeMillis());
            if ( nextMoveFound.found ) {
                Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Sending message for androidMove 1-st time");
                mHandler.sendEmptyMessage(1);
            }
            else {
                gameActivity.showWinner(0);
            }
        } else {
            //called n-th time
            Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": In androidMove n-th time");
            SystemClock.sleep(1000);
            boolean nextMovePossible = MakeMove(androidMoves.get(realMoves.size()).X, androidMoves.get(realMoves.size()).Y, realMoves);
            invalidate();
            if (nextMovePossible)
                if (realMoves.get(realMoves.size() - 1).P == 1) {
                    Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Sending message for androidMove n-th time");
                    mHandler.sendEmptyMessage(1);
                }
                else
                    androidMoves.clear();
        }
    }

    public void logMoves(ArrayList<MoveTo> Moves){
        StringBuilder str= new StringBuilder("<moves>");
        for(MoveTo i: Moves)
            str.append(i.toString()).append(";");
        str.append("</moves>");
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": " + str);
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
        if (!inputEnabled) return true;          // if not input enabled, exit

        // 1) if android's move then ignore onTouchEvent
        if((GameType ==2) && (realMoves.get(realMoves.size()-1).P==1))
            return true;

        // 2) Only handle ACTION_UP (finger lifted)
        if (event.getAction()==MotionEvent.ACTION_UP)  {
            // 2a) Map screen coords → field coords
            int x = field.x2w(event.getX());
            int y = field.y2h(event.getY());

            if (field.isFlipped()) {
                x = field.getFieldWidth() - x;
                y = field.getFieldHeight() - y;
            }

            // 2b) Compute legal moves
            ArrayList<MoveTo> possibleMoves= new ArrayList<>();
            createPossibleMoves(possibleMoves,realMoves);

            // 2c) If this tap is a valid move…
            if(isMoveValid(x,y,possibleMoves)) {
                if (GameType == 3) {
                    int lastP = realMoves.get(realMoves.size() - 1).P;

                    if (localPlayerIndex != lastP) {
                        // 🔒 Not your turn — show feedback and ignore the touch
                        Toast.makeText(
                                getContext(),                     // a View always has a Context
                                R.string.toast_not_your_turn,     // put the text in strings.xml
                                Toast.LENGTH_SHORT
                        ).show();

                        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                                + ": 🔒 Not your turn — ignoring touch");
                        return true;
                    }

                    // Apply move locally (updates UI, checks victory)
                    MakeMove(x, y, realMoves);
                }
                else {
                    // ── GameType 1 & 2: use your existing local/AI move logic
                    MakeMove(x, y, realMoves);
                    Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                            + ": Before invalidate, last P=" + realMoves.get(realMoves.size() - 1).P);
                    invalidate();
                    Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                            + ": After invalidate");

                    // ── If now it's Android’s turn and there _are_ moves available, queue AI
                    if ((GameType == 2) && (realMoves.get(realMoves.size() - 1).P == 1) && (!possibleMoves.isEmpty()))
                        //Send message for Android to move
                        mHandler.sendEmptyMessage(1);
                }
                // 2d) Accessibility / click feedback
                performClick();
            }
        }
        return true;  // Event handled
    }
}
