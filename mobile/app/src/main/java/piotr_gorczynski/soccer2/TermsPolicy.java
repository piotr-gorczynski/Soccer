package piotr_gorczynski.soccer2;

import java.util.*;
import com.google.firebase.firestore.FieldValue;

/** Required legal documents. Never edit bytes at a published versioned URL. */
public final class TermsPolicy {
    public static final String GLOBAL_VERSION = "GLOBAL-terms-2026-10-13";
    public static final String BD_VERSION = "BD-terms-2026-10-06";
    // Fixed migration boundary: changing GLOBAL_VERSION disables legacy grandfathering.
    private static final String LEGACY_GLOBAL_BOUNDARY = "GLOBAL-terms-2025-07-30";
    private static final Map<String, String> GLOBAL_HASHES = new HashMap<>();
    static {
        GLOBAL_HASHES.put("sw", "28e4ce1915f56d915141cc4bd5a1a53c8b778a1c4563b1e913cd36bf99d87911");
        GLOBAL_HASHES.put("so", "1b64b4b1d4c6e719fe6b6af2d730ce496ab36f2bd3a79c5466dc47f19a36f4e9");
        GLOBAL_HASHES.put("si", "7a1f3fa4ae94d93e6abba92e9f70571cc341cd589a78d7987f4b89012691de08");
        GLOBAL_HASHES.put("my", "cabea8e8fe3da7badb5f64e256e35dbc2d0e14c90649a937563beacd75b33a94");
        GLOBAL_HASHES.put("mn", "dae202cac4f190f513937de3eef984a62ab3f980af853ea9337ac271baa77445");
        GLOBAL_HASHES.put("mg", "4ee7bc7118925ee89cea9304745ba331e84dec98e2a29482d86b078042d9b4bb");
        GLOBAL_HASHES.put("lo", "757aa960aab745a64e1ea0503fdaa51f6692114d7a59b380970801701141b2e3");
        GLOBAL_HASHES.put("km", "fea35e844d5c87d0770b7761fa8d8493e1a3a24e000aaac95981cbfb6f2ec25a");
        GLOBAL_HASHES.put("fa", "914e1dbfa9d59d7f6b8a5eec83a73a31474e769fce5dc06c19b3b82be8af8f11");
        GLOBAL_HASHES.put("ar", "ee0f6587db4e5058efdb4861eb26072fa05100b34184ce3196a250f3493a3c6d");
        GLOBAL_HASHES.put("am", "4acecd36d517bf9e1d72332c73cd199add69e17c8f4bba1b67d7691b9c7b64b6");
        GLOBAL_HASHES.put("bn", "70f0a8955890324c375111ed62ff6305b7c534c8067725b5dad4a2e1a7757189");
        GLOBAL_HASHES.put("de", "0473ff6a4e8fe0215975b2dfe5a716aea91bf349205c5f8f6dbf8f2ff3ca291b");
        GLOBAL_HASHES.put("en", "50c67e0ef90a613ab8b3bc02ec92bede977762169c2efdfdece6526a5a00ec73");
        GLOBAL_HASHES.put("es", "07f268b06d91834167512e7059daf60db7a2b57615a5bf64556740d6905b4fd1");
        GLOBAL_HASHES.put("fr", "c82e9784f25f851d81931685eeb861056348f8aa2369377358eaf4f47df98d42");
        GLOBAL_HASHES.put("hi", "17de2970b534321dd0d979daf23e52ae89c6ae66b2642c5d5e615a10ca8cb61d");
        GLOBAL_HASHES.put("ne", "e20232619fb921d1e017e22d621571b32438ac9a765ded3c258d5329c893788e");
        GLOBAL_HASHES.put("pl", "b4054f58584ccf732681d45ddc46bfdeb31f1a1cf0dfd6dd99e3925fad7216d7");
        GLOBAL_HASHES.put("ur", "70a7e680ba517b25a237f1256c82843cd6a73387bbfa324e65bcd9c930829d3d");
    }
    public final String scope, version, language, url, sha256;
    TermsPolicy(String scope, String version, String language, String url, String sha256) {
        this.scope=scope; this.version=version; this.language=language; this.url=url; this.sha256=sha256;
    }
    public static TermsPolicy required(boolean bd, String language) {
        if (bd && "bn".equals(language)) return new TermsPolicy("bangladesh", BD_VERSION, "bn",
            "https://piotr-gorczynski.com/bangladesh/terms/" + BD_VERSION + "-bn.html", "7e35810d68cdd8a0115bb526e867a7bd0b584c6b4e37756b47faeddf88caff3d");
        if (bd) return new TermsPolicy("bangladesh", BD_VERSION, "en",
            "https://piotr-gorczynski.com/bangladesh/terms/" + BD_VERSION + ".html", "e77d67d705baec188e277fe8c1cfdcbbad4d061e0581a64e6c884ede8c76ebcc");
        String lang = GLOBAL_HASHES.containsKey(language) ? language : "en";
        return new TermsPolicy("global", GLOBAL_VERSION, lang,
            "https://piotr-gorczynski.com/terms/" + GLOBAL_VERSION + "/" + lang + ".html", GLOBAL_HASHES.get(lang));
    }
    public String id() { return "terms_" + scope + "__" + version; }
    public boolean matches(Map<String,Object> record) {
        if (record == null || !"terms".equals(record.get("documentType"))
            || !scope.equals(record.get("scope")) || !version.equals(record.get("version"))
            || !(record.get("acceptedAt") instanceof com.google.firebase.Timestamp)) return false;
        TermsPolicy accepted = required("bangladesh".equals(scope), String.valueOf(record.get("language")));
        return accepted.language.equals(record.get("language")) && accepted.url.equals(record.get("documentUrl"))
            && accepted.sha256.equals(record.get("documentSha256"));
    }
    public boolean accepts(Map<String,Object> profile, Map<String,Object> record) {
        if (matches(record)) return true;
        return "global".equals(scope) && version.equals(LEGACY_GLOBAL_BOUNDARY)
            && profile != null && (Boolean.TRUE.equals(profile.get("legacyGlobalTermsAccepted"))
            || (!profile.containsKey("termsAcceptanceModel") && Boolean.TRUE.equals(profile.get("termsAccepted"))));
    }
    public Map<String,Object> newRecord() {
        Map<String,Object> r = new HashMap<>();
        r.put("documentType","terms"); r.put("scope",scope); r.put("version",version);
        r.put("language",language); r.put("documentUrl",url); r.put("documentSha256",sha256);
        r.put("acceptedAt",FieldValue.serverTimestamp()); r.put("appVersionCode",BuildConfig.VERSION_CODE);
        r.put("appVersionName",BuildConfig.VERSION_NAME); r.put("flavor",BuildConfig.FLAVOR);
        return r;
    }
}
