# Custom Firebase Auth Domain

Sometimes Firebase Authentication uses a default domain like `PROJECT_ID.firebaseapp.com` for email link actions and OAuth flows. To use your own domain, configure the custom domain in Firebase Console and then instruct the Android client to use it.

## 1. Configure the domain in Firebase

Add `piotr-gorczynski.com` (or your domain) under **Authentication ➜ Sign-in method ➜ Authorized domains**.

## 2. Override the handler URL for email actions

When sending email verification or password reset links, pass a custom URL to `ActionCodeSettings`:

```java
ActionCodeSettings settings = ActionCodeSettings.newBuilder()
    .setUrl("https://piotr-gorczynski.com/__/auth/handler")
    .setHandleCodeInApp(true)
    .setAndroidPackageName("piotr_gorczynski.soccer2", true, null)
    .build();
```

## 3. Specify the authDomain in Firebase initialization

If using `FirebaseOptions` directly, set the `authDomain` to your custom hostname so Google sign-in redirects correctly:

```java
FirebaseOptions options = new FirebaseOptions.Builder()
    .setApiKey("AIzaSy...")
    .setApplicationId("1:690882718361:android:...")
    .setProjectId("soccer-dev-1744877837")
    .setAuthDomain("piotr-gorczynski.com")
    .build();

FirebaseApp.initializeApp(context, options, "custom");
```

If you rely on `google-services.json`, you can set the `auth_domain` field there instead.
