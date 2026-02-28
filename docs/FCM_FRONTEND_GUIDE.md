# FCM Integration Guide — Next.js Frontend

This guide covers everything needed to receive push notifications in the Luna Next.js app.

> **Important:** The Firebase config used in the frontend is NOT the service account JSON.
> It's the public web app config from the Firebase console (safe to expose in frontend code).

---

## Step 1 — Get Firebase Web Config

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Select your project
3. Click the gear icon → **Project Settings**
4. Scroll to **Your apps** → select your Web app (or create one if missing)
5. Copy the config object — it looks like this:

```js
const firebaseConfig = {
  apiKey: "AIza...",
  authDomain: "luna-xxx.firebaseapp.com",
  projectId: "luna-xxx",
  storageBucket: "luna-xxx.appspot.com",
  messagingSenderId: "123456789",
  appId: "1:123456789:web:abc123"
};
```

6. Get your **VAPID Key** (required for web push):
   - Firebase Console → Project Settings → **Cloud Messaging** tab
   - Scroll to **Web Push certificates**
   - Click **Generate key pair** if none exists
   - Copy the key string

---

## Step 2 — Install Firebase SDK

```bash
pnpm install firebase
# or
yarn add firebase
```

---

## Step 3 — Create Firebase Config File

Create `lib/firebase.ts`:

```ts
import { initializeApp, getApps } from 'firebase/app';
import { getMessaging, isSupported } from 'firebase/messaging';

const firebaseConfig = {
  apiKey: process.env.NEXT_PUBLIC_FIREBASE_API_KEY,
  authDomain: process.env.NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN,
  projectId: process.env.NEXT_PUBLIC_FIREBASE_PROJECT_ID,
  storageBucket: process.env.NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: process.env.NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID,
  appId: process.env.NEXT_PUBLIC_FIREBASE_APP_ID,
};

const app = getApps().length === 0 ? initializeApp(firebaseConfig) : getApps()[0];

export const getFirebaseMessaging = async () => {
  const supported = await isSupported();
  if (!supported) return null;
  return getMessaging(app);
};
```

Add to `.env.local`:

```
NEXT_PUBLIC_FIREBASE_API_KEY=AIza...
NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN=luna-xxx.firebaseapp.com
NEXT_PUBLIC_FIREBASE_PROJECT_ID=luna-xxx
NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET=luna-xxx.appspot.com
NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID=123456789
NEXT_PUBLIC_FIREBASE_APP_ID=1:123456789:web:abc123
NEXT_PUBLIC_FIREBASE_VAPID_KEY=your-vapid-key-here
```

---

## Step 4 — Create the Service Worker

FCM requires a service worker to receive **background notifications** (when the browser tab is not active).

Create `public/firebase-messaging-sw.js`:

```js
importScripts('https://www.gstatic.com/firebasejs/10.7.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/10.7.0/firebase-messaging-compat.js');

firebase.initializeApp({
  apiKey: "AIza...",                         // paste values directly here
  authDomain: "luna-xxx.firebaseapp.com",    // env vars don't work in service workers
  projectId: "luna-xxx",
  storageBucket: "luna-xxx.appspot.com",
  messagingSenderId: "123456789",
  appId: "1:123456789:web:abc123"
});

const messaging = firebase.messaging();

// Handle background notifications
messaging.onBackgroundMessage((payload) => {
  const { title, body } = payload.notification;
  self.registration.showNotification(title, {
    body,
    icon: '/icon.png',   // your app icon in /public
  });
});
```

> **Note:** Env vars (`process.env`) do not work inside service workers.
> Paste the Firebase config values directly in this file. These are public keys — safe to expose.

---

## Step 5 — Create useFcmToken Hook

Create `hooks/useFcmToken.ts`:

```ts
import { useEffect } from 'react';
import { getToken } from 'firebase/messaging';
import { getFirebaseMessaging } from '@/lib/firebase';

const VAPID_KEY = process.env.NEXT_PUBLIC_FIREBASE_VAPID_KEY;

export function useFcmToken(authToken: string | null) {
  useEffect(() => {
    if (!authToken) return;

    const registerToken = async () => {
      try {
        // Ask for notification permission
        const permission = await Notification.requestPermission();
        if (permission !== 'granted') {
          console.log('Notification permission denied');
          return;
        }

        const messaging = await getFirebaseMessaging();
        if (!messaging) return;

        const fcmToken = await getToken(messaging, { vapidKey: VAPID_KEY });
        if (!fcmToken) return;

        // Register token with Luna backend
        await fetch('/api/users/me/fcm-tokens', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${authToken}`,
          },
          body: JSON.stringify({
            fcmToken,
            platform: 'WEB',
            deviceName: navigator.userAgent.slice(0, 100),
          }),
        });

        console.log('FCM token registered');
      } catch (error) {
        console.error('FCM registration failed:', error);
      }
    };

    registerToken();
  }, [authToken]);
}
```

---

## Step 6 — Use the Hook After Login

In your root layout or auth provider, call the hook once the user is logged in:

```ts
// app/layout.tsx or components/AuthProvider.tsx

import { useFcmToken } from '@/hooks/useFcmToken';

export function AuthProvider({ children }) {
  const { token } = useAuth(); // your existing auth hook

  useFcmToken(token);           // registers FCM token when user is logged in

  return <>{children}</>;
}
```

---

## Step 7 — Unregister Token on Logout

When the user logs out, delete the token from the backend:

```ts
import { getToken, deleteToken } from 'firebase/messaging';
import { getFirebaseMessaging } from '@/lib/firebase';

export async function logout(authToken: string) {
  try {
    const messaging = await getFirebaseMessaging();
    if (messaging) {
      const fcmToken = await getToken(messaging, {
        vapidKey: process.env.NEXT_PUBLIC_FIREBASE_VAPID_KEY
      });

      if (fcmToken) {
        // Remove from backend
        await fetch(`/api/users/me/fcm-tokens/${encodeURIComponent(fcmToken)}`, {
          method: 'DELETE',
          headers: { 'Authorization': `Bearer ${authToken}` },
        });

        // Remove from Firebase
        await deleteToken(messaging);
      }
    }
  } catch (error) {
    console.error('FCM cleanup on logout failed:', error);
  }

  // ... rest of your logout logic
}
```

---

## Step 8 — Handle Foreground Notifications (Optional)

When the app tab is active, Firebase does not show a notification automatically.
You can listen and show a toast instead:

```ts
import { onMessage } from 'firebase/messaging';
import { getFirebaseMessaging } from '@/lib/firebase';

// In your AuthProvider or a NotificationListener component
useEffect(() => {
  let unsubscribe: (() => void) | undefined;

  getFirebaseMessaging().then((messaging) => {
    if (!messaging) return;

    unsubscribe = onMessage(messaging, (payload) => {
      const { title, body } = payload.notification ?? {};
      const { type, userId } = payload.data ?? {};

      // Show a toast, update a notification badge, etc.
      console.log('Foreground notification:', title, body);
      // e.g. toast.info(`${title}`)
    });
  });

  return () => unsubscribe?.();
}, []);
```

---

## Testing Checklist

- [ ] User B logs in → browser asks for notification permission → allow it
- [ ] FCM token appears in `user_fcm_tokens` table in the DB
- [ ] User A follows User B → User B receives a browser notification
- [ ] User A unfollows User B, then follows again within 1 hour → no second notification
- [ ] User B logs out → token is deleted from the DB

---

## Troubleshooting

| Problem | Likely cause |
|---|---|
| Permission prompt never appears | `Notification.requestPermission()` must be called from a user gesture (button click) in some browsers |
| Token is null | VAPID key is wrong or service worker failed to register |
| Background notifications not showing | `firebase-messaging-sw.js` not in `/public` or has a JS error |
| Foreground notifications not showing | Expected — handle with `onMessage` and show a toast manually |
| Backend returns 401 | JWT token not passed in Authorization header |
