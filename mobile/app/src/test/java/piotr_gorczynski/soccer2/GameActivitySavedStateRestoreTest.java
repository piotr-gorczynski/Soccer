package piotr_gorczynski.soccer2;

import android.os.Bundle;
import android.os.Parcel;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Method;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 34)
public class GameActivitySavedStateRestoreTest {

    @Test
    public void restoreMoves_restoresParcelledMovesWithoutCrashing() throws Exception {
        Bundle savedState = new Bundle();
        ArrayList<MoveTo> expectedMoves = new ArrayList<>();
        expectedMoves.add(new MoveTo(1, 2, 0));
        expectedMoves.add(new MoveTo(3, 4, 1));
        savedState.putParcelableArrayList("Moves", expectedMoves);

        Bundle restoredState = roundTrip(savedState);
        ArrayList<MoveTo> restoredMoves = invokeRestoreMoves(restoredState);

        assertNotNull("Moves should be restored after bundle parcel round-trip", restoredMoves);
        assertEquals(2, restoredMoves.size());
        assertEquals(1, restoredMoves.get(0).X);
        assertEquals(2, restoredMoves.get(0).Y);
        assertEquals(0, restoredMoves.get(0).P);
        assertEquals(3, restoredMoves.get(1).X);
        assertEquals(4, restoredMoves.get(1).Y);
        assertEquals(1, restoredMoves.get(1).P);
    }

    @Test
    public void restoreMoves_returnsNullWhenMovesAreMissing() throws Exception {
        ArrayList<MoveTo> restoredMoves = invokeRestoreMoves(new Bundle());

        assertNull("Missing saved moves should return null", restoredMoves);
    }

    @SuppressWarnings("unchecked")
    private ArrayList<MoveTo> invokeRestoreMoves(Bundle savedState) throws Exception {
        GameActivity activity = new GameActivity();
        Method restoreMoves = GameActivity.class.getDeclaredMethod("restoreMoves", Bundle.class);
        restoreMoves.setAccessible(true);
        return (ArrayList<MoveTo>) restoreMoves.invoke(activity, savedState);
    }

    private Bundle roundTrip(Bundle source) {
        Parcel parcel = Parcel.obtain();
        source.writeToParcel(parcel, 0);
        byte[] bytes = parcel.marshall();
        parcel.recycle();

        Parcel restoredParcel = Parcel.obtain();
        restoredParcel.unmarshall(bytes, 0, bytes.length);
        restoredParcel.setDataPosition(0);
        Bundle restored = restoredParcel.readBundle(null);
        restoredParcel.recycle();
        return restored;
    }
}
