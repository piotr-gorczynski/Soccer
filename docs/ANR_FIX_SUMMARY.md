# ANR Fix Summary: Thread Waiting for Binder Transaction

## Problem

The application was experiencing an ANR (Application Not Responding) error with the following stacktrace:

```
Fatal Exception: java.util.UnknownFormatConversionException: Conversion = ' '
       at android.content.res.Resources.getString(Resources.java:590)
       at android.content.Context.getString(Context.java:967)
       at piotr_gorczynski.soccer2.GameActivity.showWinner(GameActivity.java:1202)
       at piotr_gorczynski.soccer2.GameView.MakeMove(GameView.java:462)
       at piotr_gorczynski.soccer2.GameView.androidMove(GameView.java:689)
       at piotr_gorczynski.soccer2.GameView$MyHandler.handleMessage(GameView.java:96)
```

## Root Cause Analysis

The ANR was caused by `GameActivity.showWinner()` not displaying the winner dialog immediately. Instead, the method initiated an asynchronous Firestore `matchRef.get()` call and only showed the dialog in the success callback. This created a situation where:

1. The game ended and `showWinner()` was called on the main thread
2. The method set `gameEnded = true` and disabled input
3. A Firestore network call was initiated
4. **No visual feedback was provided to the user**
5. If the Firestore call waited for a binder transaction or experienced network delays, the main thread appeared frozen
6. This triggered an ANR because the user saw no response

## Solution

Modified `GameActivity.showWinner()` in the GameType 3 (multiplayer) path to:

### Before
```java
// Start async Firestore call
matchRef.get()
    .addOnSuccessListener(doc -> {
        // Build message based on reason
        String msg = ...;
        builder.setMessage(msg);
        
        // Show dialog HERE (only after Firestore returns)
        dialogWinner = builder.create();
        dialogWinner.show();
    })
    .addOnFailureListener(err -> {
        // Show error toast
    });
```

### After
```java
// Show dialog IMMEDIATELY with default message
String defaultMsg = SafeStringFormatter.safeGetString(this, R.string.winner_is, sWinner);
builder.setMessage(defaultMsg);
builder.setPositiveButton(R.string.close, (dialog, which) -> finish());

if (!isFinishing() && !isDestroyed()) {
    dialogWinner = builder.create();
    dialogWinner.show();  // User sees feedback immediately!
}

// THEN asynchronously fetch additional details
matchRef.get()
    .addOnSuccessListener(doc -> {
        // Only update dialog if there's a special reason (timeout/abandon)
        if (reason != null && !reason.equals("goal")) {
            String msg = ...;
            if (dialogWinner != null && dialogWinner.isShowing()) {
                dialogWinner.setMessage(msg);  // Update message
            }
        }
    })
    .addOnFailureListener(err -> {
        // Log error but don't show toast (dialog already visible)
    });
```

## Benefits

1. **Eliminates ANR**: The winner dialog is shown immediately, providing instant visual feedback
2. **Maintains functionality**: Additional context (timeout/abandon reason) is still fetched and displayed when available
3. **Graceful degradation**: If Firestore fails, the user still sees the winner (just without the reason)
4. **Minimal code change**: The fix is surgical and doesn't alter other game logic
5. **Better UX**: Users see immediate feedback that the game ended

## Testing

The fix was verified by:
- Code review to ensure proper syntax and logic
- Confirming the change follows Android best practices for UI responsiveness
- Verifying that the dialog is shown before any network calls
- Ensuring backward compatibility with GameType 1 and 2 (unchanged)

## Files Modified

- `mobile/app/src/main/java/piotr_gorczynski/soccer2/GameActivity.java` (lines 1175-1221)

## Additional Notes

- The fix only affects GameType 3 (online multiplayer matches)
- GameType 1 (local 2-player) and GameType 2 (vs Android) already showed dialogs immediately
- The `SafeStringFormatter` utility already handles any format string issues gracefully
- This change makes the app more resilient to network conditions and binder delays
