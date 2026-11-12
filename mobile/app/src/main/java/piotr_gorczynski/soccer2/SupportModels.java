package piotr_gorczynski.soccer2;

import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import android.content.Context;

final class SupportModels {
    static Map<?, ?> map(Object data) {
        if (!(data instanceof Map)) throw new IllegalArgumentException("Invalid support response");
        return (Map<?, ?>) data;
    }
    static String text(Map<?, ?> data, String key) {
        Object value = data.get(key);
        return value instanceof String ? (String) value : "";
    }
    static String cursor(Map<?, ?> data) {
        String value = text(data, "nextCursor");
        return value.isEmpty() ? null : value;
    }
    static long number(Map<?, ?> data, String key) {
        Object value = data.get(key);
        return value instanceof Number ? Math.max(0, ((Number) value).longValue()) : 0;
    }
    static boolean validId(String id) { return id != null && id.matches("[A-Za-z0-9_-]{1,128}"); }
    static long time(String value) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = format.parse(value, new ParsePosition(0));
        return date == null ? 0 : date.getTime();
    }
    static String date(Context context, String value) {
        long time = time(value);
        if (time == 0) return context.getString(R.string.help_unknown_date);
        Locale locale = context.getResources().getConfiguration().getLocales().get(0);
        return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale).format(new Date(time));
    }
    static int status(String status) {
        switch (status) {
            case "open": return R.string.help_wait_support;
            case "waiting_for_user": return R.string.help_wait_user;
            case "resolved": return R.string.payment_support_status_resolved;
            default: return R.string.payment_support_status_closed;
        }
    }
    static int topic(String category) {
        if ("player_report".equals(category)) return R.string.help_topic_player;
        if (java.util.Arrays.asList("validation_rejected", "payout_method_unavailable", "payment_not_received").contains(category))
            return R.string.help_topic_payment;
        return R.string.help_topic_other;
    }
    static final class Ticket {
        final String id, reference, category, status, updatedAt, latestReply, latestReplyAt;
        final long version, unread;
        final boolean canReply;
        Ticket(Object raw) {
            Map<?, ?> data = map(raw);
            id = text(data, "ticketId");
            if (!validId(id)) throw new IllegalArgumentException("Invalid conversation ID");
            reference = text(data, "reference"); category = text(data, "category"); status = text(data, "status");
            updatedAt = text(data, "updatedAt"); latestReply = text(data, "latestSupportReply");
            latestReplyAt = text(data, "latestSupportReplyAt");
            version = number(data, "supportReplyVersion"); unread = Math.min(version, number(data, "unreadCount"));
            canReply = Boolean.TRUE.equals(data.get("canReply"));
        }
    }
    static final class Message {
        final String id, author, body, createdAt;
        Message(Object raw) {
            Map<?, ?> data = map(raw);
            id = text(data, "messageId"); author = text(data, "authorType");
            body = text(data, "message"); createdAt = text(data, "createdAt");
            if (!validId(id) || !("user".equals(author) || "support".equals(author)))
                throw new IllegalArgumentException("Invalid conversation message");
        }
    }
    static final class Inbox {
        final List<Ticket> tickets = new ArrayList<>();
        final long unread;
        final String next;
        Inbox(Object raw) {
            Map<?, ?> data = map(raw); next = cursor(data); unread = number(data, "unreadCount");
            if (!(data.get("conversations") instanceof List)) throw new IllegalArgumentException("Invalid inbox");
            for (Object ticket : (List<?>) data.get("conversations")) tickets.add(new Ticket(ticket));
        }
    }
    static final class ThreadPage {
        final Ticket ticket;
        final List<Message> messages = new ArrayList<>();
        final Message initial;
        final String next;
        ThreadPage(Object raw) {
            Map<?, ?> data = map(raw); ticket = new Ticket(data.get("conversation")); next = cursor(data);
            if (!(data.get("messages") instanceof List)) throw new IllegalArgumentException("Invalid conversation");
            for (Object message : (List<?>) data.get("messages")) messages.add(new Message(message));
            initial = data.get("initialMessage") instanceof Map ? new Message(data.get("initialMessage")) : null;
        }
    }
}
