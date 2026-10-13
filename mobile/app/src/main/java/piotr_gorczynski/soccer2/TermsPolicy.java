package piotr_gorczynski.soccer2;

import java.util.*;
import com.google.firebase.firestore.FieldValue;

/** Required legal documents. Never edit bytes at a published versioned URL. */
public final class TermsPolicy {
    public static final String GLOBAL_VERSION = "GLOBAL-terms-2025-07-30";
    public static final String BD_VERSION = "BD-terms-2026-10-06";
    // Fixed migration boundary: changing GLOBAL_VERSION disables legacy grandfathering.
    private static final String LEGACY_GLOBAL_BOUNDARY = "GLOBAL-terms-2025-07-30";
    private static final Map<String, String> GLOBAL_HASHES = new HashMap<>();
    static {
        GLOBAL_HASHES.put("sw", "c12355475a714d4437facb1c5730df7d22c1b5dabb8391881a3ed7292d19216b");
        GLOBAL_HASHES.put("so", "9ffc0fb10b72d3a2092cb6b7ed8071d56e3e04c6982a6a3d3686c7bbc86b581a");
        GLOBAL_HASHES.put("si", "a7b0854fc56167449984bf95e0ed7c228ebb5631ceec0a33663f6cb36856d600");
        GLOBAL_HASHES.put("my", "41261796572b3b909a57bdbc0f70420f5a223cabf6d23fbb42f9ea24a33cf5ac");
        GLOBAL_HASHES.put("mn", "d2412e2add43ff23b344769d76d30b93d99eee222d6cde34c146b7c732eb8ef5");
        GLOBAL_HASHES.put("mg", "44b4747c1b720a45ec086898a87539eb04b82c1934c13370be2599cf7632d868");
        GLOBAL_HASHES.put("lo", "3ed6ecb70995b4e56a92c83898a9137d94a2bb5494508603bae8019070f0ff55");
        GLOBAL_HASHES.put("km", "2486c36d8d8cd613273d38c34230739da205c39f216108bdcd2c830e2ab88169");
        GLOBAL_HASHES.put("fa", "fc5d1ee479140778776ca0e82cd52d57277a1b0cb5182e7fbbbb95121ef93536");
        GLOBAL_HASHES.put("ar", "4f4fce9b97a5715959a0427edad03452aba270e0f7e18ef1060052372bab5ac2");
        GLOBAL_HASHES.put("am", "0bed30cad106fe1455d62ed6aacba84891cd95409bd7ee87925d79fa60a08b41");
        GLOBAL_HASHES.put("bn", "c37145edf58e83edf1e2c2a487ab41f5712bb0c6f68e240882649edf9a419491");
        GLOBAL_HASHES.put("de", "3f0447b1b9269f69ba3b46d7af2484d9d611e5d030b845a98bb53812f705d958");
        GLOBAL_HASHES.put("en", "940cb471414a4d0a5e57844e7593cd3b2e61ce78a5169575f3726b09e6d99961");
        GLOBAL_HASHES.put("es", "fc6101ee6d363dcec0c06b7bb435ee1cad54385f3e0f169893a21534625ad02e");
        GLOBAL_HASHES.put("fr", "45d94dd921a0df17c8dcee865b194dc3c2582ddd0c1f12dff4bdbe3b85eba495");
        GLOBAL_HASHES.put("hi", "abdecb0a2dcfd45280ac2c3eebaf8b7caaae0a95286866f328f9e4474dc60bd9");
        GLOBAL_HASHES.put("ne", "6366c7ff60038c609c2c891a57f6e115844d1c1fb17c5ef35e4e25f24c51fd16");
        GLOBAL_HASHES.put("pl", "3f2c0e60ef535e5783ac221eb9b4f3fc10788d1905fb02036f8a90f19a29409b");
        GLOBAL_HASHES.put("ur", "99c895f3584bb19aedaccbac5ffddb9cb7ca54c4f3363697e2adc8ad85539289");
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
