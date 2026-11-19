package piotr_gorczynski.soccer2;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;

/** One explicit card selection, retained across login/Terms/nickname and process recreation. */
final class PendingTournamentPromotion {
    private static final String ID="promo_tournament", UID="promo_owner", TIME="promo_selected_at";
    private static final long MAX_AGE=24*60*60*1000L;
    private static final String LOGIN_ENTRY="tournament_promotion_login";
    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences(LanguageManager.PREFS_FILE,0); }
    static void select(Context c, String id, String uid, long now) {
        if (!TournamentPromotion.validId(id)) return;
        prefs(c).edit().putString(ID,id).putString(UID,uid).putLong(TIME,now).apply();
    }
    /** Launch metadata only: the selected tournament remains solely in the existing preferences. */
    static Intent loginIntent(Context c) {
        return new Intent(c,UniversalLoginActivity.class).putExtra(LOGIN_ENTRY,true);
    }
    static boolean isLoginEntry(Context c,Intent intent,long now) {
        if(intent==null || !intent.getBooleanExtra(LOGIN_ENTRY,false)) return false;
        SharedPreferences p=prefs(c);
        long age=now-p.getLong(TIME,0);
        return TournamentPromotion.validId(p.getString(ID,null)) && age>=0 && age<=MAX_AGE
                && p.getString(UID,null)==null;
    }
    /** Read-only context for presentation/diagnostics; does not bind or clear a selection. */
    static boolean hasSelectionForUser(Context c,String uid,long now) {
        if(uid==null)return false;
        SharedPreferences p=prefs(c);
        long age=now-p.getLong(TIME,0);
        String owner=p.getString(UID,null);
        return TournamentPromotion.validId(p.getString(ID,null)) && age>=0 && age<=MAX_AGE
                && (owner==null || owner.equals(uid));
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
