package piotr_gorczynski.soccer2;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;

public class TermsPolicyTest {
    @Test public void allGlobalLanguagesRequireCurrentTermsForNewUsers() {
        String[] languages={"am","ar","bn","my","en","fr","de","hi","km","lo","mg","mn","ne","fa","pl","si","so","es","sw","ur"};
        for(String language:languages) {
            TermsPolicy p=TermsPolicy.required(false,language);
            assertEquals(language,p.language);
            assertEquals("https://piotr-gorczynski.com/terms/"+TermsPolicy.GLOBAL_VERSION+"/"+language+".html",p.url);
            assertFalse(p.accepts(null,null));
            assertFalse(p.accepts(Collections.emptyMap(),null));
            assertTrue(p.accepts(Collections.emptyMap(),saved(p)));
            assertTrue(p.matches(saved(TermsPolicy.required(false,"en"))));
            Map<String,Object> old=saved(p);old.put("version","GLOBAL-terms-old");
            assertFalse(p.accepts(Collections.emptyMap(),old));
        }
        assertEquals("en",TermsPolicy.required(false,"unsupported").language);
    }
    private Map<String,Object> saved(TermsPolicy p) {
        Map<String,Object> r=p.newRecord(); r.put("acceptedAt",new Timestamp(100,0)); return r;
    }
    @Test public void bangladeshNeverUsesLegacyOrGlobal() {
        TermsPolicy bd=TermsPolicy.required(true,"pl");
        assertFalse(bd.accepts(Collections.singletonMap("termsAccepted",true),null));
        assertFalse(bd.matches(saved(TermsPolicy.required(false,"en"))));
        assertTrue(bd.matches(saved(bd)));
        Map<String,Object> old=saved(bd); old.put("version","BD-terms-2025-01-01"); assertFalse(bd.matches(old));
    }
    @Test public void globalLegacyAndScopeAreIndependent() {
        TermsPolicy global=TermsPolicy.required(false,"pl");
        assertFalse(global.accepts(Collections.singletonMap("termsAccepted",true),null));
        assertFalse(global.matches(saved(TermsPolicy.required(true,"en"))));
        Map<String,Object> newBd=new HashMap<>();newBd.put("termsAccepted",true);newBd.put("termsAcceptanceModel",2);
        assertFalse(global.accepts(newBd,null));
        newBd.put("legacyGlobalTermsAccepted",true);assertFalse(global.accepts(newBd,null));
    }
    @Test public void futureGlobalVersionCannotUseLegacy() {
        TermsPolicy future=new TermsPolicy("global","GLOBAL-terms-2027-01-15","en","unused","unused");
        assertFalse(future.accepts(Collections.singletonMap("termsAccepted",true),null));
        assertFalse(future.accepts(Collections.singletonMap("legacyGlobalTermsAccepted",true),null));
    }
    @Test public void exactMetadataAndServerTimestamp() {
        TermsPolicy p=TermsPolicy.required(false,"pl");Map<String,Object> r=p.newRecord();
        assertEquals("terms_global__GLOBAL-terms-2026-10-13",p.id());
        assertEquals("global",r.get("scope"));assertEquals(p.version,r.get("version"));
        assertEquals("pl",r.get("language"));assertEquals(p.url,r.get("documentUrl"));
        assertEquals(p.sha256,r.get("documentSha256"));assertEquals(FieldValue.serverTimestamp(),r.get("acceptedAt"));
        assertEquals(10,r.size());assertFalse(p.matches(r));
        r.put("acceptedAt",new Timestamp(100,0));assertTrue(p.matches(r));
        r.put("documentSha256","wrong");assertFalse(p.matches(r));
    }
    @Test public void languageRoutingAndAcceptanceAcrossLanguages() {
        TermsPolicy bn=TermsPolicy.required(true,"bn"), en=TermsPolicy.required(true,"pl");
        assertEquals("BD-terms-2026-10-14",TermsPolicy.BD_VERSION);
        assertEquals(en.version,bn.version);
        assertEquals(en.id(),bn.id());
        for(String language:new String[]{"am","ar","my","en","fr","de","hi","km","lo","mg","mn","ne","fa","pl","si","so","es","sw","ur","unsupported"}) {
            TermsPolicy routed=TermsPolicy.required(true,language);
            assertEquals(en.url,routed.url);assertEquals(en.sha256,routed.sha256);
        }
        assertEquals("bn",bn.language);
        assertTrue(bn.url.endsWith("/bangladesh/terms/BD-terms-2026-10-14-bn.html"));
        assertEquals("en",en.language);
        assertTrue(en.url.endsWith("/bangladesh/terms/BD-terms-2026-10-14.html"));
        assertTrue(en.matches(saved(bn))); assertTrue(bn.matches(saved(en)));
        assertTrue(TermsPolicy.required(false,"bn").url.endsWith("/terms/GLOBAL-terms-2026-10-13/bn.html"));
        assertTrue(TermsPolicy.required(false,"pl").url.endsWith("/pl.html"));
        assertTrue(PrivacyPolicyLinks.bangladeshUrl("bn").endsWith("/privacy-bn.html"));
        assertTrue(PrivacyPolicyLinks.bangladeshUrl("pl").endsWith("/privacy-en.html"));
    }
    @Test public void historicalBangladeshAcceptanceDoesNotSatisfyCurrentPolicy() {
        String version="BD-terms-2026-10-06";
        for(String language:new String[]{"en","bn"}) {
            TermsPolicy old=new TermsPolicy("bangladesh",version,language,
                "https://piotr-gorczynski.com/bangladesh/terms/"+version+(language.equals("bn")?"-bn":"")+".html",
                language.equals("bn")?"7e35810d68cdd8a0115bb526e867a7bd0b584c6b4e37756b47faeddf88caff3d":"e77d67d705baec188e277fe8c1cfdcbbad4d061e0581a64e6c884ede8c76ebcc");
            assertFalse(TermsPolicy.required(true,"en").accepts(Collections.emptyMap(),saved(old)));
            assertFalse(TermsPolicy.required(true,"bn").accepts(Collections.emptyMap(),saved(old)));
        }
    }
    @Test public void hashRejectsChangedHtml() throws Exception {
        byte[] bytes="hello".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        assertEquals("hello",TermsDocumentLoader.verify(bytes,"2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"));
        try { TermsDocumentLoader.verify(bytes,"wrong");fail(); }catch(java.io.IOException expected){}
    }
}
