# Changelog

All notable changes to Clint Gecko are documented here.

---

# v1.0.2

## What's New

### A Brand-New Download Manager

The Download Manager has been completely redesigned with its own identity.

Before anything else, I want to answer one question:

**Why am I doing this?**

Let's be real. The previous download UI was heavily inspired by 1DM. Don't get me wrong, I love 1DM, but I don't want Clint to be known as a copy of another app. Even though Clint uses a different download engine, most people don't know that, and I don't want its identity to be defined by its similarities to another app.

Clint is growing, and we now have two versions. I want to give the project its own identity, its own design language, and a UI that people can recognize as Clint.

I want people to use Clint because of what Clint offers, not because it reminds them of another app.

I want Clint to be Clint. This rework is another step toward making that happen.

But don't worry, I'm not Supercell. I'm not going to give you a depressing new UI and call it a day. All of my friends like the new UI, so that's a good sign, but I can't speak for everyone. Your feedback is always welcome, and I'm open to suggestions on how to make it even better.

Now, let's talk about what's changed.

The new Download Manager is built around two sections, **Queue** and **Library**, with a cleaner, more modern interface designed to be comfortable to use on any screen size, from small phones to tablets.

---

### Queue

- Live dashboard at the top showing your total download speed, a speed graph for the last minute, peak speed, how many downloads are active and queued, how much is left, an estimate of when everything will finish, and your free storage. Pause All and Resume All are one tap away.
- New download cards with a progress ring around the file's thumbnail. File names wrap instead of getting cut off, and details like speed, time left, running time, and whether a download can be resumed are shown as tags that wrap onto the next line, so everything stays visible.
- Parts map showing which parts of a file are done, downloading, or waiting. For stream downloads, it shows every individual segment along with the exact segment count (for example, "Segments: 775/1032").
- Tap a download to expand it and see its own speed graph with average and peak speed, the full parts or segments map, the source website, quick settings (speed limit, Wi-Fi only, connections), and large Pause and Cancel buttons.
- Up next lists queued downloads in order. Use the arrows to reorder them, or choose Download next to move one to the front.
- Paused and failed downloads have their own groups. Error messages are shown in full.

---

### Library

- Finished downloads are grouped by date: Today, Yesterday, Earlier this week, Earlier this month, and Older.
- Gallery view for photos and videos with real thumbnails and video lengths, or switch to a list view in one tap.
- Real previews throughout the app:
  - Image thumbnails, including SVG
  - Video frames with resolution and duration
  - Album artwork with song title, artist, and duration
  - The first page of PDFs and their page count
  - App icons, app names, and versions for APK files
- Filter by type: Videos, Audio, Images, Documents, Archives, Apps, and Other.

---

### File Details Sheet

The long dropdown menu has been replaced with a bottom sheet. It shows a large preview, the full file name, file details, where the file came from, and how long it took to download. It has big Open and Share buttons and a grid of every other action, including Show in Folder, Rename, Redownload, Copy Link, Copy Name, Copy Path, and Properties. Unfinished downloads also get Settings, New Link, Find Link in Browser, and Download Next.

---

### Gestures and Selection

- Swipe right on a download to pause, resume, retry, or share it. Swipe left to delete it.
- Selection mode now has a bottom action bar for Redownload, Copy Link, Copy Name, Delete, and more.

---

### Download Log

A new **Download Log** setting (Download Settings › General), off by default. When turned on, Clint keeps a detailed log for each download plus a global log covering all downloads, to help diagnose problems. Logs include download settings, server responses, parts and segments with their download URLs, retries, errors, progress, and a summary when a download finishes. Cookies are never recorded.

Open the global log from the Downloads menu, or a single download's log from its details sheet. You can filter to warnings and errors, share the log, or clear it. When the setting is off, nothing is logged, so there is no extra performance or storage cost.

---

### Total Speed Limit

A new **Total Speed Limit** setting (Download Settings › Speed Limit) caps the combined speed of all downloads, on top of each download's own speed limit.

---

## Improvements

### Faster Downloads

- Smarter multi-connection downloads. When a connection finishes early, it now takes over half of the largest remaining part instead of sitting idle, so one slow part no longer holds up the end of a download.
- Truly parallel connections. Multi-connection downloads now open real separate connections instead of sharing a single one on servers that support HTTP/2.
- Lower memory use for stream downloads. Segments are written straight to storage instead of being loaded into memory first, which helps a lot on low-end devices.
- Faster writes with larger buffers.
- Smoother, lighter progress updates, even with a large download history.
- A per-website connection limit to reduce rate limiting when several downloads come from the same site.

### More Reliable Downloads

- A single failing part no longer fails the whole download. Failed parts are retried with gradually longer waits, and a download only gives up after a long period with no progress.
- Safer resuming. Clint now checks that the file on the server hasn't changed before resuming. If it has, the download restarts cleanly instead of producing a corrupted file.
- Size verification. Every finished download is checked against its expected size. If a server cuts the connection early, the download retries instead of being marked complete.
- Downloads continue after the app is closed or killed. Interrupted downloads are queued again and resume automatically instead of being marked as failed.
- No more limit on the number of parts that can be resumed.
- Free space check before starting. If a file won't fit on your device, you'll get a clear message right away instead of a failure partway through.
- Better support for long downloads on Android 14 and newer, using the system's dedicated method for user-started data transfers when possible.

---

## Bug Fixes

- Fixed downloads that were decrypting, merging, or converting not appearing in the Downloading tab.
- Fixed a case where a multi-connection download that switched to a single connection after rate limiting could resume from the wrong position and corrupt the file.
- Fixed downloads that a server cut off early being marked as complete with a truncated file.
- Removed Trim Referer. It caused broken sites.
- Fixed Dimensions Detector not working on DOM players.

(Thanks to Daniel Rioja for reporting this.)

- Fixed Upload and Download not working on extensions.

(Thanks to @phanimitravadlamani for reporting this [#15](https://github.com/jhaiian/ClintGecko/issues/15))

- Fixed Upload not working on certain devices.

(Thanks to @TheWeaver786 for reporting this [#13](https://github.com/jhaiian/ClintGecko/issues/13))

- Fixed system navigation covering the pages.

(Thanks to @deniganda for reporting this [#87](https://github.com/jhaiian/ClintBrowser/issues/87))

---

## Donation

Thank you to **Daniel Rioja** for donating another $13.48! Thank you so much, sir. This covers two weeks of my expenses going to college. This definitely helps a lot.

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
