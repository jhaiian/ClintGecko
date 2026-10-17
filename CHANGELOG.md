# Changelog

All notable changes to Clint Gecko are documented here.

---

# v1.0.1

## What's New

### Custom Theme

Added a custom theme editor that allows users to individually customize all colors used throughout the app. Include a real-time preview that updates instantly as colors are changed, so users can see the results before applying the theme. Allow users to save, rename, edit, apply, and reuse their custom themes.

Thank you to Daniel Rioja for suggesting this.

---

### Media Capture

Added support for combining muted video streams with separate audio tracks in Media Capture, allowing users to merge them into a single playable video with audio.

If the video has no audio or playback is glitchy, try switching to the software codec. I've received a lot of reports about these issues, and in most cases, I've had to tell users to switch to the software codec because their device's hardware may not properly support the specific codec.

Thank you to @reviewlord for suggesting this [#71](https://github.com/jhaiian/ClintBrowser/issues/71)

---

### Advanced Download Manager

- Handling a foreground timeout properly.
- The Properties dialog will now display the current downloaded segment / total download segments.
- Also the segment average size.
- Made muxing have a real progress percentage tracker instead of being indeterminate.

---

## GeckoView Engine

### Performance & Stability

**Improved battery life and background efficiency**

- Pages now pause when you switch to another app or turn off your screen, then resume when you return.
- Tabs playing audio or video remain active in the background, so your music and videos continue playing.
- When background media playback finishes, the tab automatically pauses to avoid unnecessary resource usage.

**Smarter memory management**

- The browser now responds progressively to memory pressure before performance degrades.
- Background tabs are put into hibernation to free up memory and reload when you return to them.
- Under severe memory pressure, even the currently active tab can hibernate while the app is in the background.
- Cached images are cleared when memory becomes critically low.

**Faster startup with lower memory usage**

- Tabs that haven't been opened no longer load in the background during startup. They load only when you open them.
- Starting the browser with many saved tabs now requires less memory.

**Reduced engine memory overhead**

- GeckoView helper processes no longer perform unnecessary app initialization, reducing memory overhead.

**Bug fixes and reliability**

- Fixed an issue where memory was not properly released after closing tabs.
- Fixed internal lists that could grow indefinitely over time.
- Closed tabs now properly detach from the app, preventing them from unnecessarily retaining memory.
- Improved browser extension tab communication to reduce the chance of messages being lost.

> I'm still finding more ways to improve GeckoView performance.

Also, crashes produced by the Gecko engine can now be seen in Debug & Crash Reports, so you can report them to me and I'll have a solution to fix them. Don't worry, all crash logs are local. Like I said, Clint doesn't have a backend, your data stays on your device.

Bumped GeckoView version from `157.0.20261005135250` to `158.0.20261007201254`.

---

## Privacy And Security

### Tracking protection

- **Strip tracking parameters** (on): Removes trackers like `utm_` and `fbclid` from links before they load. On or off.
- **Fingerprinting protection** (on): Makes it harder for sites to identify you by your browser and device traits. On or off.
- **Send Global Privacy Control** (on): Asks websites not to sell or share your data. Websites may choose to ignore it. On or off.
- **Trim referrer** (on): Shares only the site address, not the full page link, when you follow a link. On or off.
- **WebRTC leak protection** (on in Gecko, off in Browser): Stops websites from finding your real IP address through video call features. Browser adds "Some calls may not connect". On or off.

### Data and device protection

- **App lock** (off): Require your fingerprint, face, or screen lock to open the app. When on, a **Lock after** row appears with these options:
  - Immediately
  - 1 minute (default)
  - 5 minutes
  - 15 minutes
- **Block screenshots** (off): Prevents screenshots and screen recording, and hides the app preview in recent apps. On or off.
- **Incognito keyboard** (on): Asks your keyboard not to learn what you type in Incognito Mode. On or off.
- **Auto-clear clipboard** (off): Opens a picker with these options:
  - Off
  - After 30 seconds
  - After 1 minute
  - After 5 minutes
- **Clear data on exit** (off): Deletes the selected data when you close the app. When on, a **Data to clear** row opens a checklist, with all three ticked by default:
  - Browsing history
  - Cookies and site data
  - Cache

(Thanks to @super585 for suggesting this [#81](https://github.com/jhaiian/ClintBrowser/issues/81))

- **Strict certificate checking** (off), in the first card: Never allow continuing to sites with invalid security certificates. On or off.

---

## Secure DNS

- **Off** — Use your network's default DNS resolver.
- **Increased protection** — Use DNS over HTTPS (DoH) when available. Fall back to regular DNS if the provider is unavailable.
- **Maximum protection** — Use DNS over HTTPS exclusively. Websites won't load if the DNS provider is unavailable.

### DNS Provider

Choose your preferred DNS provider.

- Cloudflare
- Google
- Quad9
- Custom DNS provider

### Custom DNS

Enter a custom DNS-over-HTTPS endpoint URL.

(Thanks to @xmiseggs for suggesting this [#4](https://github.com/jhaiian/ClintGecko/issues/4))

---

## Other Changes

- Added ability to enable `about:config`.

Don't play around on this because it can cause a severe crash that can break your device.

Thank you to Daniel Rioja for suggesting this.

- Added ability to hide quick actions on the homepage.

Thank you to Daniel Rioja for suggesting this.

- Added ability to add a custom URL for the homepage.

Thank you to Daniel Rioja for suggesting this.

- Improved nested scroll.
- Increased all bottom sheet sizes to max 80%. I can't set this to 100 percent because there's a glitch when you scroll too fast, it will glitch.
- The Swipe Refresh Layout will now be disabled on the extensions homepage to fix the bug where scrolling up on the extensions page refreshes it. This is because we can't really inject a JS script inside that, and the refresh layout uses JS to detect if the thing is at the top.
- Updated Privacy Policy.

---

## Bug Fixes

- Fixed Android 10 storage permission.
- Fixed Clipboard permission not working.
- Fixed Autofill not working.
- Fixed notification bug. (Thanks to @Woallin [#9](https://github.com/jhaiian/ClintGecko/issues/9))

---

## Donation

Thank you to **Daniel Rioja** for donating $6.67! 

---

> There are a lot of requests right now, but I'll get to them eventually. I've just prioritized the donor's request for now because their support helps me personally and supports the project as well. Don't worry, guys! Your issues will be addressed and fixed soon.

---

# v1.0.0

## Introducing Clint Gecko!

Clint Browser is getting a new sibling! Unlike the original Clint Browser, which runs on Android System WebView, Clint Gecko is powered by Firefox's GeckoView engine.

And because of that, Clint Gecko brings extension support and background playback to the Clint experience!

But that's not all. Since Clint Gecko is an official fork of Clint Browser, with the browser engine being the main change, it also retains the built-in ADM (Advanced Download Manager) and Media Capture features, making it easier to download files and capture online media directly from your browser.

---

## ðŸ”¥ Key Features

- ðŸ¦Š Firefox-based browsing with GeckoView
- ðŸ§© Browser extension support
- ðŸŽµ Background playback
- ðŸ“¥ Built-in Advanced Download Manager (ADM)
- ðŸŽ¬ Built-in Media Capture

---

Clint is no longer limited to one browser engine. You can stick with the original WebView-based Clint Browser or choose Clint Gecko for a Firefox-powered experience with extension support.

And don't worry! The original Clint Browser will continue to be maintained as the lighter version, so you can choose whichever browser suits your needs best.

**Two engines. One Clint. ðŸ’œ**

---
