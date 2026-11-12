package piotr_gorczynski.soccer2;

import com.google.firebase.firestore.DocumentSnapshot;

/** Historical records retain UIDs; names always come from the current profile/tombstone. */
public final class UserDisplayName {
    public static final String REMOVED = "(Account removed)";
    private UserDisplayName() {}
    public static boolean isRemoved(DocumentSnapshot user) {
        return user == null || !user.exists() || Boolean.TRUE.equals(user.getBoolean("accountDeleted"));
    }
    public static String from(DocumentSnapshot user) {
        if (isRemoved(user)) return REMOVED;
        return user.getString("nickname");
    }
}
