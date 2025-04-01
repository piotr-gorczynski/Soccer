package piotr_gorczynski.soccer2;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import androidx.core.content.ContextCompat;
import android.util.Log;

import java.util.ArrayList;

public class Field {

    private int intFieldWidth,intFieldHeight;//,intBallX, intBallY;
    private float flFieldMargin,flDots,flText,flLinesWidth;
    private Paint pField,pFieldBorder,pDots,pPlayer0,pPlayer1,pHintText,pHintBalloon;  // paint style and color
    private Rect rField,rText;
    private int GameType;
    private String sPlayer0,sPlayer1,sPlayer1YourMoveorThinking;

    ArrayList<MoveTo> possibleMoves;//= new ArrayList<MoveTo>();
    ArrayList<MoveTo> Moves;//= new ArrayList<MoveTo>();

    public Field(Context current, ArrayList<MoveTo> argMoves, ArrayList<MoveTo> argpossibleMoves, int argGameType) {

        GameType=argGameType;
        switch(GameType) {
            case(1):
                sPlayer0="Player 1";
                sPlayer1="Player 2";
                sPlayer1YourMoveorThinking="Your move!...";
                break;
            case(2):
                sPlayer0="Player";
                sPlayer1="Android";
                sPlayer1YourMoveorThinking="Thinking...";
                break;
        }



        pField = new Paint();
        pFieldBorder= new Paint();
        pDots= new Paint();

        pField.setColor(ContextCompat.getColor(current, R.color.colorGreen));

        pFieldBorder.setStyle(Paint.Style.STROKE);
        pFieldBorder.setColor(Color.WHITE);

        pDots.setStyle(Paint.Style.FILL);
        pDots.setColor(Color.WHITE);

        rField = new Rect();
        rText = new Rect();

        Resources res = current.getResources();
        flFieldMargin = res.getFraction(R.fraction.flFieldMargin,1,1);
        flLinesWidth = res.getFraction(R.fraction.flLinesWidth,1,1);
        flDots= res.getFraction(R.fraction.flDots,1,1);
        flText = res.getFraction(R.fraction.flText,1,1);
        intFieldWidth = res.getInteger(R.integer.intFieldHalfWidth)*2;
        intFieldHeight = res.getInteger(R.integer.intFieldHalfHeight)*2;

        pPlayer0=new Paint();
        pPlayer0.setStyle(Paint.Style.FILL);
        pPlayer0.setColor(ContextCompat.getColor(current, R.color.colorPlayer0));
        pPlayer0.setTextAlign(Paint.Align.CENTER);


        pPlayer1=new Paint();
        pPlayer1.setStyle(Paint.Style.FILL);
        pPlayer1.setColor(ContextCompat.getColor(current, R.color.colorPlayer1));
        pPlayer1.setTextAlign(Paint.Align.CENTER);

        pHintText=new Paint();
        pHintText.setStyle(Paint.Style.FILL);
        pHintText.setColor(Color.WHITE);
        pHintText.setTextAlign(Paint.Align.CENTER);

        pHintBalloon=new Paint();
        pHintBalloon.setStyle(Paint.Style.FILL);
        pHintBalloon.setColor(Color.YELLOW);



        //intBallX=ballX;
        //intBallY=ballY;
        //Log.d("pgorczyn", "123456: Box getString(R.string.app_name):" + res.getString(R.string.app_name));
        //Log.d("pgorczyn", "123456: Box getInteger(R.integer.intFieldMargin):" + res.getInteger(R.integer.intFieldMargin));

        /*Moves.addAll(argMoves);
        possibleMoves.addAll(argpossibleMoves);*/
        Moves=argMoves;
        possibleMoves=argpossibleMoves;

    }


  /*  public void moveBall(ArrayList<MoveTo> argMoves, ArrayList<MoveTo> argpossibleMoves){
        //intBallX=ballX;
        //intBallY=ballY;

        Moves.clear();
        Moves.addAll(argMoves);

        possibleMoves.clear();
        possibleMoves.addAll(argpossibleMoves);

    }*/
    public int x2w(float x) {


        if(rField.height()>rField.width() ) {
            //portrait
            return Math.round((x-rField.left)*intFieldWidth/rField.width());
        } else {
            //landscape
            return Math.round((x-rField.left)*intFieldHeight/rField.width());
        }
    }

    public int y2h(float y) {
        if(rField.height()>rField.width() ) {
            //portrait
            return Math.round((y-rField.top)*intFieldHeight/rField.height());
        } else {
            //landscape
            return Math.round((rField.bottom-y)*intFieldWidth/rField.height());
        }
    }

    private float w2x(int w) {

        if(rField.height()>rField.width() ) {
            //portrait
            return rField.left + w * rField.width() / intFieldWidth;
        } else {
            //landscape
            return rField.left + w * rField.width() / intFieldHeight;
        }

    }

    private float h2y(int h) {
        if(rField.height()>rField.width() ) {
            //portrait
            return rField.top+h*rField.height()/intFieldHeight;
        } else {
            //landscape
            return rField.bottom-h*rField.height()/intFieldWidth;
        }

    }



    public void set(int x, int y, int width, int height) {
        int xMin, xMax, yMin, yMax;
        xMin = x+(int)(width*flFieldMargin);
        xMax = x + width -(int)(width*flFieldMargin)- 1;
        yMin = y+(int)(height*flFieldMargin);
        yMax = y + height - (int)(height*flFieldMargin)- 1;
        // The box's rField do not change unless the view's size changes
        rField.set(xMin, yMin, xMax, yMax);
    }

    public void draw(Canvas canvas) {
        Log.d("pgorczyn", "123456: Field.draw");
        int oldx, oldy;



        if(rField.height()>rField.width() ) {
            //portrait
            pPlayer0.setTextSize(rField.height() *flText);
            pPlayer0.setStrokeWidth(rField.height() * flLinesWidth);
            pPlayer1.setTextSize(rField.height() *flText);
            pPlayer1.setStrokeWidth(rField.height() * flLinesWidth);
            pFieldBorder.setStrokeWidth(rField.height() * flLinesWidth);
            pHintText.setTextSize(rField.height() *flText);

            //Field
            canvas.drawRect(rField, pField);
            canvas.drawRect(rField, pFieldBorder);

            //Top gate
            canvas.drawRect(w2x((intFieldWidth / 2) - 1),
                    h2y(-1),
                    w2x((intFieldWidth / 2) + 1),
                    h2y(0),
                    pField);
            canvas.drawRect(w2x((intFieldWidth / 2) - 1),
                    h2y(-1),
                    w2x((intFieldWidth / 2) + 1),
                    h2y(0),
                    pFieldBorder);

            canvas.drawText(sPlayer1,w2x(intFieldWidth/2),h2y(-1)+(h2y(0)-h2y(-1))/2+pPlayer1.getTextSize()/2,pPlayer1);

            //canvas.drawCircle(w2x(intFieldWidth/2), h2y(-1)+(h2y(0)-h2y(-1))/2, rField.height() * flDots, pDots);

            //Bottom gate
            canvas.drawRect(w2x((intFieldWidth / 2) - 1),
                    h2y(intFieldHeight),
                    w2x((intFieldWidth / 2) + 1),
                    h2y(intFieldHeight + 1),
                    pField);
            canvas.drawRect(w2x((intFieldWidth / 2) - 1),
                    h2y(intFieldHeight),
                    w2x((intFieldWidth / 2) + 1),
                    h2y(intFieldHeight + 1),
                    pFieldBorder);

            canvas.drawText(sPlayer0,w2x(intFieldWidth/2),h2y(intFieldHeight)+(h2y(intFieldHeight+1)-h2y(intFieldHeight))/2+pPlayer0.getTextSize()/2,pPlayer0);
            //canvas.drawCircle(w2x(intFieldWidth/2), h2y(intFieldHeight)+(h2y(intFieldHeight+1)-h2y(intFieldHeight))/2, rField.height() * flDots, pDots);

            //Dots on the field
            for (int x = 0; x <= intFieldWidth; x++) {
                for (int y = 0; y <= intFieldHeight; y++) {
                    canvas.drawCircle(w2x(x), h2y(y), rField.height() * flDots, pDots);
                }
            }
            //Dots on top gate & bootom gate
            for (int x = ((intFieldWidth / 2) - 1); x <= ((intFieldWidth / 2) + 1); x++) {
                canvas.drawCircle(w2x(x), h2y(-1), rField.height() * flDots, pDots);
                canvas.drawCircle(w2x(x), h2y(intFieldHeight + 1), rField.height() * flDots, pDots);
            }

            //Moves
            oldx=Moves.get(0).X;
            oldy=Moves.get(0).Y;
            for(int i=1;i<Moves.size();i++) {
                if(Moves.get(i-1).P==0)
                    canvas.drawLine(w2x(oldx),h2y(oldy),w2x(Moves.get(i).X),h2y(Moves.get(i).Y),pPlayer0);
                else
                    canvas.drawLine(w2x(oldx),h2y(oldy),w2x(Moves.get(i).X),h2y(Moves.get(i).Y),pPlayer1);

                oldx=Moves.get(i).X;
                oldy=Moves.get(i).Y;
            }

            //PossibleMoves
            if(Moves.get(Moves.size()-1).P==0)
                for(MoveTo pm : possibleMoves)

                    canvas.drawCircle( w2x(pm.X), h2y(pm.Y),rField.height() * flDots,pPlayer0 );
            else
                for(MoveTo pm : possibleMoves)
                    canvas.drawCircle( w2x(pm.X), h2y(pm.Y),rField.height() * flDots,pPlayer1 );

            //Ball
            //canvas.drawCircle(xPos(0),yPos(0),50,pDots );
            //canvas.drawCircle( ballX, ballY,5,pDots );
            //canvas.drawText(Float.toString(x2w(ballX)), 100, 100, pText);
            //canvas.drawText(Float.toString(y2h(ballY)), 100, 150, pText);



            canvas.drawCircle( w2x(Moves.get(Moves.size()-1).X), h2y(Moves.get(Moves.size()-1).Y),rField.height() * flDots*2,pDots );

            if(Moves.get(Moves.size()-1).P==0) {
                pHintText.getTextBounds("Your move!", 0, 10, rText);
                canvas.drawText("Your move!", w2x(intFieldWidth / 2), h2y(intFieldHeight + 1) + (canvas.getHeight() - h2y(intFieldHeight + 1)) / 2 + rText.height() / 2, pHintText);
            }
            else{
                pHintText.getTextBounds(sPlayer1YourMoveorThinking, 0, sPlayer1YourMoveorThinking.length(), rText);
                canvas.drawText(sPlayer1YourMoveorThinking,w2x(intFieldWidth/2), h2y(-1)/2+rText.height()/2,pHintText);
            }

        } else {
            //landscape
            pPlayer0.setTextSize(rField.width() *flText);
            pPlayer0.setStrokeWidth(rField.width() * flLinesWidth);
            pPlayer1.setTextSize(rField.width() *flText);
            pPlayer1.setStrokeWidth(rField.width() * flLinesWidth);
            pFieldBorder.setStrokeWidth(rField.width() * flLinesWidth);
            pHintText.setTextSize(rField.width() *flText);

            //Field
            canvas.drawRect(rField, pField);
            canvas.drawRect(rField, pFieldBorder);

            //Top gate

            canvas.drawRect(w2x( - 1),
                    h2y((intFieldWidth / 2) + 1),
                    w2x(0),
                    h2y((intFieldWidth / 2) - 1),
                    pField);
            canvas.drawRect(w2x( - 1),
                    h2y((intFieldWidth / 2) + 1),
                    w2x(0),
                    h2y((intFieldWidth / 2) - 1),
                    pFieldBorder);


            pPlayer0.getTextBounds(sPlayer0, 0, sPlayer0.length(), rText);

            /*canvas.drawCircle(w2x(-1)+(w2x(0)-w2x(-1))/2, h2y(intFieldWidth/2),5, pDots);

            canvas.drawRect(w2x(-1)+(w2x(0)-w2x(-1))/2 - rText.height()/2,
                    h2y(intFieldWidth/2) - rText.width()/2,
                    w2x(-1)+(w2x(0)-w2x(-1))/2 + rText.height()/2,
                    h2y(intFieldWidth/2) + rText.width()/2,
                    pFieldBorder);*/

            // rotate the canvas on center of the text to draw
            canvas.rotate(-90, w2x(-1)+(w2x(0)-w2x(-1))/2,h2y(intFieldWidth/2));
            // draw the rotated text
            canvas.drawText(sPlayer0, w2x(-1)+(w2x(0)-w2x(-1))/2, h2y(intFieldWidth/2)+ rText.height()/2, pPlayer0);
            //undo the translation and rotation
            canvas.rotate(+90, w2x(-1)+(w2x(0)-w2x(-1))/2,h2y(intFieldWidth/2));


            //Bottom gate
            canvas.drawRect(w2x( intFieldHeight),
                    h2y((intFieldWidth / 2) + 1),
                    w2x(intFieldHeight+1),
                    h2y((intFieldWidth / 2) - 1),
                    pField);
            canvas.drawRect(w2x( intFieldHeight),
                    h2y((intFieldWidth / 2) + 1),
                    w2x(intFieldHeight+1),
                    h2y((intFieldWidth / 2) - 1),
                    pFieldBorder);

            pPlayer1.getTextBounds(sPlayer1, 0, sPlayer1.length(), rText);

            /*canvas.drawCircle(w2x(intFieldHeight)+(w2x(intFieldHeight+1)-w2x(intFieldHeight))/2, h2y(intFieldWidth/2),5, pDots);

            canvas.drawRect(w2x(intFieldHeight)+(w2x(intFieldHeight+1)-w2x(intFieldHeight))/2 - rText.height()/2,
                    h2y(intFieldWidth/2) - rText.width()/2,
                    w2x(intFieldHeight)+(w2x(intFieldHeight+1)-w2x(intFieldHeight))/2 + rText.height()/2,
                    h2y(intFieldWidth/2) + rText.width()/2,
                    pFieldBorder);*/
            // rotate the canvas on center of the text to draw
            canvas.rotate(+90, w2x(intFieldHeight)+(w2x(intFieldHeight+1)-w2x(intFieldHeight))/2,h2y(intFieldWidth/2));
            // draw the rotated text
            canvas.drawText(sPlayer1, w2x(intFieldHeight)+(w2x(intFieldHeight+1)-w2x(intFieldHeight))/2, h2y(intFieldWidth/2)+ rText.height()/2, pPlayer1);
            //undo the translation and rotation
            canvas.rotate(-90, w2x(intFieldHeight)+(w2x(intFieldHeight+1)-w2x(intFieldHeight))/2,h2y(intFieldWidth/2));


            //Dots on the field
            for (int x = 0; x <= intFieldHeight; x++) {
                for (int y = 0; y <= intFieldWidth; y++) {
                    canvas.drawCircle(w2x(x), h2y(y), rField.width() * flDots, pDots);
                }
            }
            //Dots on top gate & bootom gate
            for (int y = ((intFieldWidth / 2) - 1); y <= ((intFieldWidth / 2) + 1); y++) {
                canvas.drawCircle(w2x(-1), h2y(y), rField.width() * flDots, pDots);
                canvas.drawCircle(w2x(intFieldHeight + 1), h2y(y), rField.width() * flDots, pDots);
            }

            //Moves
            oldx=Moves.get(0).X;
            oldy=Moves.get(0).Y;
            for(int i=1;i<Moves.size();i++) {
                if(Moves.get(i-1).P==0)
                    canvas.drawLine(w2x(intFieldHeight-oldy),h2y(intFieldWidth-oldx),w2x(intFieldHeight-Moves.get(i).Y),h2y(intFieldWidth-Moves.get(i).X),pPlayer0);
                else
                    canvas.drawLine(w2x(intFieldHeight-oldy),h2y(intFieldWidth-oldx),w2x(intFieldHeight-Moves.get(i).Y),h2y(intFieldWidth-Moves.get(i).X),pPlayer1);                                  
                oldx=Moves.get(i).X;
                oldy=Moves.get(i).Y;
            }


            //PossibleMoves
            if(Moves.get(Moves.size()-1).P==0)
                for(MoveTo pm : possibleMoves)
                    canvas.drawCircle( w2x(intFieldHeight-pm.Y), h2y(intFieldWidth-pm.X),rField.width() * flDots,pPlayer0 );
            else
                for(MoveTo pm : possibleMoves)
                    canvas.drawCircle( w2x(intFieldHeight-pm.Y), h2y(intFieldWidth-pm.X),rField.width() * flDots,pPlayer1 );



            //Ball
            //canvas.drawCircle(xPos(0),yPos(0),50,pDots );
            //canvas.drawCircle( ballX, ballY,5,pDots );
            //canvas.drawText(Float.toString(x2w(ballX)), 100, 100, pText);
            //canvas.drawText(Float.toString(y2h(ballY)), 100, 150, pText);


            canvas.drawCircle( w2x(intFieldHeight-Moves.get(Moves.size()-1).Y), h2y(intFieldWidth-Moves.get(Moves.size()-1).X),rField.width() * flDots*2,pDots );        }

    }

}
