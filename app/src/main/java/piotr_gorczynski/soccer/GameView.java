package piotr_gorczynski.soccer;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

/**
 * Created by pgorczynski on 2016-05-13.
 */
public class GameView extends View {
    //private Ball ball;
  /*  private Ball ball2;*/
    private Field field;
    private GameActivity gameActivity;
    private int intBallX, intBallY,intFieldWidth,intFieldHeight;
    private ArrayList<MoveTo> possibleMoves= new ArrayList<MoveTo>();

    private ArrayList<MoveTo> Moves;//= new ArrayList<MoveTo>();

    /*private StatusMessage statusMsg;*/
//    private Paint Kolko;
    //   private RectF bounds;

    // For touch inputs - previous touch (x, y)
    // private float previousX;
    // private float previousY;


    // private MyBall mBall;
    // Constructor
//    public GameView(Context context, int ballX, int ballY,ArrayList<MoveTo> argMoves) {
    public GameView(Context context, ArrayList<MoveTo> argMoves) {
        super(context);
        gameActivity=(GameActivity)context;
        //intBallX=ballX;
        //intBallY=ballY;
        Resources res = context.getResources();
        intFieldWidth = res.getInteger(R.integer.intFieldHalfWidth)*2;
        intFieldHeight = res.getInteger(R.integer.intFieldHalfHeight)*2;

        //Moves.addAll(argMoves);
        Moves=argMoves;

        createPossibleMoves(Moves.get(Moves.size()-1).X, Moves.get(Moves.size()-1).Y);

        /*
        possibleMoves.add(new MoveTo(2,2,0));
        possibleMoves.add(new MoveTo(3,2,0));
        possibleMoves.add(new MoveTo(4,2,0));
        possibleMoves.add(new MoveTo(4,3,0));
*/

        //mBall=(MyBall)context;

        //      Kolko = new Paint();
        //      Kolko.setStyle(Paint.Style.STROKE);
        //      Kolko.setColor(Color.WHITE);
        //     bounds = new RectF();
        field = new Field(context, Moves, possibleMoves);  // ARGB
        ///ball = new Ball(Color.WHITE, 100,100);
        //ball2 = new Ball(Color.WHITE, -4);
        //statusMsg = new StatusMessage(Color.WHITE);

        // To enable keypad
        this.setFocusable(true);
        this.requestFocus();
        // To enable touch mode
        this.setFocusableInTouchMode(true);


    }

    ArrayList<MoveTo> GetMoves(){
        return Moves;
    }

    // Called back to draw the view. Also called after invalidate().
    @Override
    protected void onDraw(Canvas canvas) {
        // Draw the components
        field.draw(canvas);
        //ball.draw(canvas);
        //ball2.draw(canvas);
        //       bounds.set((int)(previousX)-50,(int)(previousY)-50,(int)(previousX)+50,(int)(previousY)+50);
        //       canvas.drawOval(bounds,Kolko);
        //statusMsg.draw(canvas);


        // Update the position of the ball, including collision detection and reaction.
        //ball.moveWithCollisionDetection(box);
        //ball2.moveWithCollisionDetection(box);
        //statusMsg.update(ball);

        // Delay
        //try {
        //     Thread.sleep(300);
        // } catch (InterruptedException e) { }

        //invalidate();  // Force a re-draw
    }

    // Called back when the view is first created or its size changes.
    @Override
    public void onSizeChanged(int w, int h, int oldW, int oldH) {
        // Set the movement bounds for the ball
        field.set(1, 1, w, h - 1);
    }

    // Key-up event handler
  /*  @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_RIGHT: // Increase rightward speed
                ball.speedX++;
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:  // Increase leftward speed
                ball.speedX--;
                break;
            case KeyEvent.KEYCODE_DPAD_UP:    // Increase upward speed
                ball.speedY--;
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:  // Increase downward speed
                ball.speedY++;
                break;
            case KeyEvent.KEYCODE_DPAD_CENTER: // Stop
                ball.speedX = 0;
                ball.speedY = 0;
                break;
            case KeyEvent.KEYCODE_A:    // Zoom in
                // Max radius is about 90% of half of the smaller dimension
                float maxRadius = (box.xMax > box.yMax) ? box.yMax / 2 * 0.9f  : box.xMax / 2 * 0.9f;
                if (ball.radius < maxRadius) {
                    ball.radius *= 1.05;   // Increase radius by 5%
                }
                break;
            case KeyEvent.KEYCODE_Z:    // Zoom out
                if (ball.radius > 20) {  // Minimum radius
                    ball.radius *= 0.95;  // Decrease radius by 5%
                }
                break;
            default:
                mBall.onKeyUp(keyCode,event);
        }
        return true;  // Event handled
    }
*/

    private void createPossibleMoves(int xPos, int yPos  ){
        possibleMoves.clear();
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
                    && thereIsNoLine(xPos,yPos,x,y)
                    )
                    possibleMoves.add(new MoveTo(x,y,-1));
            }
    }

    private boolean thereIsNoLine(int x1,int y1, int x2, int y2){
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

    private boolean isBouncing(int x, int y) {
        if(
            ((x==0) && (y>0) && (y<intFieldHeight))
            ||((x==intFieldWidth) && (y>0) && (y<intFieldHeight))
            ||((y==0)&&(x<intFieldWidth/2)&&(x>0))
            ||((y==0)&&(x>intFieldWidth/2)&&(x<intFieldWidth))
            ||((y==intFieldHeight)&&(x<intFieldWidth/2)&&(x>0))
            ||((y==intFieldHeight)&&(x>intFieldWidth/2)&&(x<intFieldWidth))
            ||wasBallThere(x,y))
            return true;
        else
            return false;
    }

    private boolean wasBallThere(int x, int y){
        for(MoveTo pm: Moves) {
            if((x==pm.X) && (y==pm.Y)) return true;
        }
        return false;
    }

    private boolean isMoveValid(int x, int y){
        for(MoveTo pm: possibleMoves) {
            if((x==pm.X) && (y==pm.Y)) return true;
        }
        return false;
    }

    // Touch-input handler
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x,y;
        boolean bouncing;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            {
                if(getHeight()>getWidth()){
                    x=field.x2w(event.getX());
                    y=field.y2h(event.getY());
                } else {
                    y=intFieldHeight-field.x2w(event.getX());
                    x=intFieldWidth-field.y2h(event.getY());
                }

                if(isMoveValid(x,y)) {
                    bouncing=isBouncing(x,y);
                    if(bouncing)
                        Moves.add(new MoveTo(x,y,Moves.get(Moves.size()-1).P));
                    else
                        if(Moves.get(Moves.size()-1).P==0)
                            Moves.add(new MoveTo(x,y,1));
                        else
                            Moves.add(new MoveTo(x,y,0));

                    //***

                    createPossibleMoves(x,y);

//                    field.moveBall(Moves,possibleMoves);
                    /*ballX=event.getX();
                    ballY=event.getY();*/
                    invalidate();  // Force a re-draw
                    //***
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
                    }

                }

            }
        }
        /*
        float currentX = event.getX();
        float currentY = event.getY();
        float deltaX, deltaY;
        float scalingFactor = 5.0f / ((box.xMax > box.yMax) ? box.yMax : box.xMax);
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                // Modify rotational angles according to movement
                deltaX = currentX - previousX;
                deltaY = currentY - previousY;
                ball.speedX += deltaX * scalingFactor;
                ball.speedY += deltaY * scalingFactor;
        }
        // Save current x, y
        previousX = currentX;
        previousY = currentY;*/
        return true;  // Event handled
    }

/*    public int getBallX(){
        return intBallX;
    }

    public int getBallY(){
        return intBallY;
    }*/



}