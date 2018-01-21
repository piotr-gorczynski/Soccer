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


/*

public class Student implements Parcelable{
        private String id;
        private String name;
        private String grade;

        // Constructor
        public Student(String id, String name, String grade){
            this.id = id;
            this.name = name;
            this.grade = grade;
       }
       // Getter and setter methods
       .........
       .........

       // Parcelling part
       public Student(Parcel in){
           String[] data = new String[3];

           in.readStringArray(data);
           // the order needs to be the same as in writeToParcel() method
           this.id = data[0];
           this.name = data[1];
           this.grade = data[2];
       }

       @Оverride
       public int describeContents(){
           return 0;
       }

       @Override
       public void writeToParcel(Parcel dest, int flags) {
           dest.writeStringArray(new String[] {this.id,
                                               this.name,
                                               this.grade});
       }
       public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
           public Student createFromParcel(Parcel in) {
               return new Student(in);
           }

           public Student[] newArray(int size) {
               return new Student[size];
           }
       };
   }
 */