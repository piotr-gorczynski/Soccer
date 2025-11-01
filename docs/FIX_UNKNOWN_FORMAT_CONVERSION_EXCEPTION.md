# Fix for UnknownFormatConversionException Crash

## Issue
Crashlytics reported crashes with `java.util.UnknownFormatConversionException: Conversion = ' '` occurring in production. This happens when localized string resources contain malformed format specifiers (e.g., `"%1 $s"` with a space instead of `"%1$s"`).

## Root Cause
The crash occurs when `Context.getString()` is called with format arguments on string resources that have malformed format specifiers in translations. The stacktrace showed:

```
Fatal Exception: java.util.UnknownFormatConversionException: Conversion = ' '
       at java.util.Formatter$FormatSpecifier.conversion(Formatter.java:3013)
       at android.content.res.Resources.getString(Resources.java:590)
       at android.content.Context.getString(Context.java:967)
```

## Solution
Replaced all vulnerable `context.getString(R.string.xxx, args)` calls with `SafeStringFormatter.safeGetString(context, R.string.xxx, args)` in the following files:

### Files Modified

1. **FriendAdapter.java** (6 locations)
   - Line 282: `invite_stats_format` - formatting invite statistics
   - Line 292: `invite_stats_format` - error fallback for invite stats
   - Line 379: `match_stats_format` - formatting match statistics (no tournaments)
   - Line 423: `match_stats_format` - formatting match statistics (with tournaments)
   - Line 433: `match_stats_format` - error fallback for tournament stats
   - Line 443: `match_stats_format` - error fallback for match stats

2. **PastInviteAdapter.java** (7 locations)
   - Line 111: `invite_from_format` - displaying invite sender nickname
   - Line 168: `invite_received_and_status` - displaying invite time and status
   - Line 170: `invite_received_format` - displaying invite time only
   - Line 201: `invite_from_format` - displaying cached nickname
   - Line 248: `presence_user_online` - displaying online status
   - Line 253: `presence_user_last_seen` - displaying last seen time
   - Line 257: `presence_user_offline` - displaying offline status

3. **PendingInviteAdapter.java** (5 locations)
   - Line 110: `invite_from_format` - displaying invite sender nickname
   - Line 165: `invite_from_format` - displaying cached nickname
   - Line 217: `presence_user_online` - displaying online status
   - Line 222: `presence_user_last_seen` - displaying last seen time
   - Line 226: `presence_user_offline` - displaying offline status

## SafeStringFormatter Behavior
The `SafeStringFormatter.safeGetString()` method:
1. Attempts to format the string normally
2. If `UnknownFormatConversionException` occurs, it catches it and creates a fallback
3. Uses `Resources.getText()` instead of `getString()` to get raw string without format processing
4. Appends the arguments in parentheses as a fallback: `"raw string (arg1, arg2)"`
5. Has multiple layers of exception handling to ensure it never crashes

## Testing
- All vulnerable getString calls with format arguments have been replaced
- No remaining vulnerable calls found in the codebase
- Changes are minimal and surgical, only replacing method calls

## Impact
- Prevents crashes from malformed format specifiers in any localized string resources
- Maintains app stability even when translations contain formatting errors
- Provides meaningful fallback messages instead of crashes
- Consistent with existing SafeStringFormatter usage throughout the codebase

## Related
- Previous fix: PR #588 improved SafeStringFormatter to handle edge cases in the fallback handler
- Existing implementation: `SafeStringFormatter.java` already in use for other sensitive string formatting operations
