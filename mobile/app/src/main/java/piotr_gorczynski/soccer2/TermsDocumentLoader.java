package piotr_gorczynski.soccer2;

import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

final class TermsDocumentLoader {
    static String verify(byte[] bytes, String expected) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) hex.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
        if (!hex.toString().equals(expected)) throw new IOException("Terms document hash mismatch");
        return new String(bytes, StandardCharsets.UTF_8);
    }
    static String load(TermsPolicy policy) throws Exception {
        HttpURLConnection c = (HttpURLConnection)new URL(policy.url).openConnection();
        c.setInstanceFollowRedirects(false); c.setConnectTimeout(15000); c.setReadTimeout(15000);
        try {
            if (c.getResponseCode()!=200) throw new IOException("Terms unavailable");
            try (InputStream in=c.getInputStream(); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
                byte[] buffer=new byte[8192]; int n;
                while ((n=in.read(buffer))!=-1) { out.write(buffer,0,n); if(out.size()>1024*1024) throw new IOException("Terms too large"); }
                return verify(out.toByteArray(),policy.sha256);
            }
        } finally { c.disconnect(); }
    }
}
