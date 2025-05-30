package piotr_gorczynski.soccer2;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class MoveTo implements Parcelable {
    public int X;
    public int Y;
    final public int P; //indicates the player, who should move in next turn!

    public MoveTo(int x, int y, int p) {
        X=x;
        Y=y;
        P=p;
    }

    @NonNull
    @Override
    public String toString() {
        return "n"+ X + Y + P;
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

    public static final Parcelable.Creator<MoveTo> CREATOR = new Parcelable.Creator<>() {
        public MoveTo createFromParcel(Parcel in) {
            return new MoveTo(in);
        }

        public MoveTo[] newArray(int size) {
            return new MoveTo[size];
        }
    };

}
