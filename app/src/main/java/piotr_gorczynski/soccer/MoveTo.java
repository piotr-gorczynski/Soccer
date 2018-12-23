package piotr_gorczynski.soccer;

import android.os.Parcel;
import android.os.Parcelable;

public class MoveTo implements Parcelable {
    public int X;
    public int Y;
    public int P;

    public MoveTo(int x, int y, int p) {
        X=x;
        Y=y;
        P=p;
    }

    @Override
    public String toString() {
        return "n"+ Integer.toString(X)+ Integer.toString(Y)+Integer.toString(P);
    }

    // Parcelling part
    public MoveTo(Parcel in){
        int[] data = new int[3];

        in.readIntArray(data);
        // the order needs to be the same as in writeToParcel() method
        this.X = data[0];
        this.Y = data[1];
        this.P = data[2];
    }

    @Override
    public int describeContents(){
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeIntArray(new int[] {
                this.X,
                this.Y,
                this.P});
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public MoveTo createFromParcel(Parcel in) {
            return new MoveTo(in);
        }

        public MoveTo[] newArray(int size) {
            return new MoveTo[size];
        }
    };

}