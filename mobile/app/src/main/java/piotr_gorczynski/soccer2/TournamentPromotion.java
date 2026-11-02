package piotr_gorczynski.soccer2;

import com.google.firebase.firestore.DocumentSnapshot;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/** Strict, small public offer; names/amounts never come from hard-coded campaign copy. */
final class TournamentPromotion {
    final String id, name, regulationId, currency;
    final long firstPrize, deadline, serverTime;
    final int minimumAge;

    private TournamentPromotion(String id, String name, String regulationId, String currency,
                                long prize, long deadline, long serverTime, int minimumAge) {
        this.id=id; this.name=name; this.regulationId=regulationId; this.currency=currency;
        this.firstPrize=prize; this.deadline=deadline; this.serverTime=serverTime; this.minimumAge=minimumAge;
    }

    static boolean validId(String id) { return id != null && id.matches("[A-Za-z0-9_-]{1,128}"); }

    static List<TournamentPromotion> parseAll(Object response) {
        if (!(response instanceof Map<?,?> envelope)) return List.of();
        if (!(envelope.get("tournaments") instanceof List<?> values)) {
            TournamentPromotion legacy=parse(response);
            return legacy==null ? List.of() : List.of(legacy);
        }
        Map<String,TournamentPromotion> unique=new LinkedHashMap<>();
        for (Object value:values) {
            Map<String,Object> item=new java.util.HashMap<>();
            item.put("tournament",value); item.put("serverTimeMillis",envelope.get("serverTimeMillis"));
            TournamentPromotion offer=parse(item);
            if (offer!=null) unique.putIfAbsent(offer.id,offer);
        }
        return new ArrayList<>(unique.values());
    }

    static TournamentPromotion parse(Object response) {
        if (!(response instanceof Map<?, ?> envelope) || !(envelope.get("tournament") instanceof Map<?, ?> m)) return null;
        String id=text(m.get("id")), name=text(m.get("name")), regulation=text(m.get("regulationId"));
        long prize=number(m.get("firstPrize")), deadline=number(m.get("registrationDeadlineMillis"));
        long now=number(envelope.get("serverTimeMillis")), age=number(m.get("minimumAge"));
        if (!validId(id) || !validId(regulation) || name == null || name.isBlank() || name.length()>200
                || !"BDT".equals(m.get("currency")) || prize<=0 || now<=0 || deadline<=now
                || age<18 || age>120) return null;
        return new TournamentPromotion(id,name,regulation,"BDT",prize,deadline,now,(int)age);
    }

    static boolean isOpen(DocumentSnapshot doc, long now) {
        if (doc==null || !doc.exists() || !validId(doc.getId()) || !"registering".equals(doc.getString("status"))) return false;
        Object markets=doc.get("visibleInFlavours");
        if (!(markets instanceof List<?> list) || !list.contains("bangladesh")) return false;
        var deadline=doc.getTimestamp("registrationDeadline");
        long max=number(doc.get("maxParticipants")), count=number(doc.get("participantsCount"));
        return deadline!=null && deadline.toDate().getTime()>now && max>0 && count>=0 && count<max
                && validId(doc.getString("regulation"));
    }

    private static String text(Object value) { return value instanceof String ? (String)value : null; }
    private static long number(Object value) {
        if (!(value instanceof Number n) || !Double.isFinite(n.doubleValue()) || n.doubleValue()!=n.longValue()) return -1;
        return n.longValue();
    }
}
