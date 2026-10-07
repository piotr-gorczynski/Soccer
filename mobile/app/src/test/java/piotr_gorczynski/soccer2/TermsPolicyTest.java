package piotr_gorczynski.soccer2;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;

public class TermsPolicyTest {
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
        assertTrue(global.accepts(Collections.singletonMap("termsAccepted",true),null));
        assertFalse(global.matches(saved(TermsPolicy.required(true,"en"))));
        Map<String,Object> newBd=new HashMap<>();newBd.put("termsAccepted",true);newBd.put("termsAcceptanceModel",2);
        assertFalse(global.accepts(newBd,null));
        newBd.put("legacyGlobalTermsAccepted",true);assertTrue(global.accepts(newBd,null));
    }
    @Test public void futureGlobalVersionCannotUseLegacy() {
        TermsPolicy future=new TermsPolicy("global","GLOBAL-terms-2027-01-15","en","unused","unused");
        assertFalse(future.accepts(Collections.singletonMap("termsAccepted",true),null));
        assertFalse(future.accepts(Collections.singletonMap("legacyGlobalTermsAccepted",true),null));
    }
    @Test public void exactMetadataAndServerTimestamp() {
        TermsPolicy p=TermsPolicy.required(false,"pl");Map<String,Object> r=p.newRecord();
        assertEquals("terms_global__GLOBAL-terms-2025-07-30",p.id());
        assertEquals("global",r.get("scope"));assertEquals(p.version,r.get("version"));
        assertEquals("pl",r.get("language"));assertEquals(p.url,r.get("documentUrl"));
        assertEquals(p.sha256,r.get("documentSha256"));assertEquals(FieldValue.serverTimestamp(),r.get("acceptedAt"));
        assertEquals(10,r.size());assertFalse(p.matches(r));
        r.put("acceptedAt",new Timestamp(100,0));assertTrue(p.matches(r));
        r.put("documentSha256","wrong");assertFalse(p.matches(r));
    }
    @Test public void hashRejectsChangedHtml() throws Exception {
        byte[] bytes="hello".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        assertEquals("hello",TermsDocumentLoader.verify(bytes,"2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"));
        try { TermsDocumentLoader.verify(bytes,"wrong");fail(); }catch(java.io.IOException expected){}
    }
}
