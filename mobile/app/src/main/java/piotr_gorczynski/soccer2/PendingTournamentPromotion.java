package piotr_gorczynski.soccer2;

import android.content.Context;
import android.content.SharedPreferences;

/** One explicit card selection, retained across login/Terms/nickname and process recreation. */
final class PendingTournamentPromotion {
    private static final String ID="promo_tournament", UID="promo_owner", TIME="promo_selected_at";
    private static final long MAX_AGE=24*60*60*1000L;
    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences(LanguageManager.PREFS_FILE,0); }
    static void select(Context c, String id, String uid, long now) {
        if (!TournamentPromotion.validId(id)) return;
        prefs(c).edit().putString(ID,id).putString(UID,uid).putLong(TIME,now).apply();
    }
    static String forUser(Context c, String uid, long now) {
        SharedPreferences p=prefs(c);
        String id=p.getString(ID,null);
        if (id==null) return null;
        long age=now-p.getLong(TIME,0);
        String owner=p.getString(UID,null);
        if (!TournamentPromotion.validId(id) || age<0 || age>MAX_AGE
                || (owner!=null && uid!=null && !owner.equals(uid))) {
            clear(c); return null;
        }
        if (uid==null) return null;
        if (owner==null) p.edit().putString(UID,uid).apply();
        return id;
    }
    static void clear(Context c) { prefs(c).edit().remove(ID).remove(UID).remove(TIME).apply(); }
}
