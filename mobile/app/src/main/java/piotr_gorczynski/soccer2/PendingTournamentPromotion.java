package piotr_gorczynski.soccer2;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;

/** One explicit promotion selection, retained across login/Terms/nickname and process recreation. */
final class PendingTournamentPromotion {
    private static final String ID="promo_tournament", UID="promo_owner", TIME="promo_selected_at";
    private static final long MAX_AGE=24*60*60*1000L;
    private static final String LOGIN_ENTRY="tournament_promotion_login";
    private static final String SOURCE="promo_source", ATTR_ID="promo_join_attribution_id",
            ATTR_UID="promo_join_attribution_owner", ATTR_TIME="promo_join_attribution_at";
    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences(LanguageManager.PREFS_FILE,0); }
    static void select(Context c, String id, String uid, long now) {
        select(c,id,uid,now,"menu");
    }
    static void select(Context c, String id, String uid, long now, String source) {
        if (!TournamentPromotion.validId(id)) return;
        prefs(c).edit().putString(ID,id).putString(UID,uid).putLong(TIME,now)
                .putString(SOURCE,"first_win".equals(source)?"first_win":"menu")
                .putString(ATTR_ID,id).putString(ATTR_UID,uid).putLong(ATTR_TIME,now).apply();
    }
    /** Only a live first-win selection may initiate the guest Menu-to-login handoff. */
    static boolean isFirstWinSelection(Context c,String uid,long now) {
        SharedPreferences p=prefs(c);
        long age=now-p.getLong(TIME,0);
        String owner=p.getString(UID,null);
        return "first_win".equals(p.getString(SOURCE,null)) && TournamentPromotion.validId(p.getString(ID,null))
                && age>=0 && age<=MAX_AGE && (owner==null || owner.equals(uid));
    }
    /** Analytics only: survives selection consumption, bounded by owner, tournament and 24-hour age. */
    static String joinSource(Context c,String id,String uid,long now) {
        if(c==null || uid==null)return null;
        SharedPreferences p=prefs(c);
        long age=now-p.getLong(ATTR_TIME,0);
        return id!=null && id.equals(p.getString(ATTR_ID,null)) && uid.equals(p.getString(ATTR_UID,null))
                && age>=0 && age<=MAX_AGE && "first_win".equals(p.getString(SOURCE,null)) ? "first_win" : null;
    }
    static boolean hasFirstWinAttribution(Context c,String id,long now) {
        SharedPreferences p=prefs(c);
        return joinSource(c,id,p.getString(ATTR_UID,null),now)!=null;
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
        if (owner==null) p.edit().putString(UID,uid).putString(ATTR_UID,uid).apply();
        return id;
    }
    static void clear(Context c) { prefs(c).edit().remove(ID).remove(UID).remove(TIME).apply(); }
}
