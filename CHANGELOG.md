# Changelog

All notable changes to Clint Gecko are documented here.

---

# v1.0.3

## What's New

### Tab Groups

You can finally organize your tabs into groups!

- Select some tabs and tap **Group**, then give the group a name and a color. If all the tabs come from the same site, Clint suggests a name for you.
- Each group sits inside a container tinted with its color. Tap the header to collapse or expand it. A collapsed group shows a small stack of its favicons.
- Drop a tab onto another tab to create a group with both of them, or drop it onto a grouped tab to add it to that group.
- Drag a group by its header to move it around. While you drag it, every group collapses so it moves as a single block.
- The header menu has New Tab in Group, Edit Name and Color, Select Tabs in Group, Ungroup, and Close Group.
- A row of group chips sits under the New Tab buttons. Tap a chip to jump straight to that group.
- New tabs opened from a grouped tab join its group. This includes "Open in new tab" and popups.
- When the current tab is in a group, a **Group Bar** shows the group's tabs as favicons so you can switch between them quickly. You can hide it from the tab menu with **Show Group Bar**.
- Groups and their colors are saved and restored after a restart. Incognito groups are never saved, and a group never mixes normal and incognito tabs.

> Tbh, I don't know if I did a good job. All I know is I've never used tab groups before, so I didn't really know what the convenient way to implement it was.

(Thanks to @BunCat09 for suggesting this [#11](https://github.com/jhaiian/ClintGecko/issues/11))

---

### Tab Strip

- Every tab shows its favicon, title, and a close button. Tabs share the available width and scroll sideways when they don't fit.
- Tap `+` at the end to open a new tab. The strip only shows tabs from your current mode, so incognito tabs never appear next to normal ones.
- Groups appear as colored chips before their tabs. Tap a chip to collapse or expand the group, or long-press it to open the tab menu on that group.
- Long-press a tab for Close Tab, Close Other Tabs, and group options.
- Tabs from a non-default profile show a small dot in the profile's color on their favicon.
- Turn it on or off in Settings › Look and feel › Layout › **Tab strip**. It's on by default on tablets and larger screens and off on phones.

(Thanks to @jamestong420 for suggesting this [#6](https://github.com/jhaiian/ClintGecko/issues/6))

---

### Tab Strip and Group Bar Position

Two new settings in Settings › Look and feel › Layout, **Tab Strip Position** and **Tab Group Bar Position**. Each one can follow the search bar (the default), or always stay at the top or bottom of the screen. Both bars can be shown at the same time, and changes apply right away without a restart.

---

### A Better Tab Menu

- Long-press anywhere on a tab card to drag it. You don't need to aim for the small handle anymore. Long-pressing without moving still starts selection mode.
- Swipe a tab sideways to close it.
- Close a tab, a group, a selection, or all your tabs, and an **Undo** button brings them back exactly where they were. Incognito tabs are never kept for undo, for privacy.
- New **search** button that filters your tabs by title or URL.
- New **Show as list** option in the menu that switches to compact rows with the favicon, title, site, and profile. On wide screens, the list uses multiple columns.

(Thanks to Daniel Rioja for suggesting this)

- New **Close duplicate tabs** option. It only shows up when you actually have duplicates, and it keeps the tab you're on.
- Swipe the address bar to open the tab menu, just like Firefox. Swipe down when the address bar is at the top, or up when it's at the bottom. Swiping sideways still switches tabs.

(Thanks to @akshaymaurya26 for suggesting this [#20](https://github.com/jhaiian/ClintGecko/issues/20))

- While dragging a tab, the tab menu now scrolls automatically when you reach the top or bottom.
- The tab menu is lighter and smoother, especially when you have a lot of tabs.

---

### Homepage Favorites

The Favorites section on the homepage is now yours to manage, like Firefox's shortcuts.

- Long-press a tile and drag it to rearrange your favorites.
- Drop a tile onto another tile to make a folder. You can rename a folder, ungroup it, or reorder the sites inside it.
- Long-press a tile for Open in new tab, Edit, Move to folder, and Remove.
- Long-press a site in Recently visited to add it to your favorites.
- Use the new **Add** tile to add any site by name and URL.
- Pick your own image for any tile, or switch back to the site's icon.
- Your favorites are included in settings backup and restore.

(Thanks to @xmiseggs for suggesting this [#4](https://github.com/jhaiian/ClintGecko/issues/4))

---

### Sharper Icons

Say goodbye to blurry little favicons! Icons across the app are now much sharper. That includes your tabs, the tab strip, the group bar, history, bookmarks, suggestions, and homepage tiles. Clint now looks for the best icon the site offers, including SVG icons. If a site doesn't have a good one, Clint uses a high-resolution icon service instead. That only sends the site's address, it's never used in Incognito, and turning off favicons in Data Saver blocks it completely.

"Add to home screen" shortcuts also use these sharper icons now.

(Thanks to @xmiseggs for suggesting this [#4](https://github.com/jhaiian/ClintGecko/issues/4))

---

### Custom Colors

Profiles now have a **Custom color** swatch next to the preset colors (tab groups get one too). Tap it to open the color picker and choose any color you want.

---

### Media Capture

Media Capture now finds every media link on the page, not just the one that's playing. If a site offers 360p, 480p, and 720p, you'll see all of them without switching the quality by hand.

It looks inside video and audio players, links, embedded players, page settings, and popular players like JW Player, Video.js, and Plyr. It also picks up subtitle tracks. Formats that used to show up as `OCTET-STREAM` now show their real name, like MP4, MKV, or MP3.

(Thanks to @34NANAKO34 for suggesting this [#5](https://github.com/jhaiian/ClintGecko/issues/5))

---

## Improvements

- Downloads from links like `video.php` now get their real file extension instead of being saved as `.php`. Clint checks the server's response and the first bytes of the file to work out what it really is.

(Thanks to @34NANAKO34 for reporting this [#5](https://github.com/jhaiian/ClintGecko/issues/5))

---

## Other Changes

- Updated Privacy Policy.

Since Clint now uses Google's icon service as a fallback for sharper icons, I've added it to the Privacy Policy. I also updated it to cover tab groups, homepage favorites, how downloads detect their file type, and how Media Capture scans the page. As always, everything stays on your device except what's needed to fetch an icon.

---

## Bug Fixes

- Fixed desktop mode getting stuck when Save State is enabled. Pages could keep reloading and never leave the desktop site, or two sites could keep bouncing between each other.
- Fixed turning off desktop mode sometimes breaking mobile layouts until the next reload.
- Fixed desktop mode from one tab carrying over to other tabs.
- Fixed turning off desktop mode on a saved site in Incognito being undone on the next page.
- Fixed media players that load videos from script links (like `video.php`) being ignored by Media Capture.
- Fixed closing a tab that came before the current tab sometimes switching you to the wrong tab.

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

## 🔥 Key Features

- 🦊 Firefox-based browsing with GeckoView
- 🧩 Browser extension support
- 🎵 Background playback
- 📥 Built-in Advanced Download Manager (ADM)
- 🎬 Built-in Media Capture

---

Clint is no longer limited to one browser engine. You can stick with the original WebView-based Clint Browser or choose Clint Gecko for a Firefox-powered experience with extension support.

And don't worry! The original Clint Browser will continue to be maintained as the lighter version, so you can choose whichever browser suits your needs best.

**Two engines. One Clint. 💜**

---
