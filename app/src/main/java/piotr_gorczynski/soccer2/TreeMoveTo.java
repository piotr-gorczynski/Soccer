package piotr_gorczynski.soccer2;

import java.util.ArrayList;

public class TreeMoveTo {
    public int X;
    public int Y;
    public int P;
    boolean bounce;
    ArrayList<TreeMoveTo> NextMoves = new ArrayList<TreeMoveTo>();

    public TreeMoveTo(int x, int y, int p) {
        X=x;
        Y=y;
        P=p;
    }
}
