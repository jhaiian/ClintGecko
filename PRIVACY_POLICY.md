# Privacy Policy for Clint Gecko

*Last updated: October 20, 2026*

## Overview

Clint Gecko is an open-source Android web browser built with privacy in mind. This policy explains what data is collected, what isn't, and how the app handles your information.

---

## Gecko Engine

Clint Gecko renders web pages with **GeckoView**, the engine from Mozilla that powers Firefox. GeckoView is bundled inside the app, so the engine is updated whenever Clint Gecko is updated.

GeckoView is made by Mozilla and is licensed under the Mozilla Public License 2.0. Clint Gecko is an independent project and is not affiliated with, endorsed by, or sponsored by Mozilla. "Firefox" is a trademark of the Mozilla Foundation.

Because GeckoView handles page rendering, JavaScript execution, and much of the networking at a level below the app itself, some behavior is part of the engine and not something Clint Gecko controls. What Clint Gecko does control on top of it:

- **Engine telemetry is turned off.** Clint Gecko switches off Gecko's telemetry, health report upload, usage data upload, ping reporting, and unsubmitted-crash-report prompts and auto-submission. The telemetry server address is replaced with an empty placeholder so nothing can be sent even if a setting is missed.
- **Mozilla studies and experiments are turned off.** Normandy, Shield studies, and the messaging experiment loader are disabled.
- **Network connectivity checks are turned off.** Gecko's captive portal detection and connectivity service are disabled, so the engine doesn't make background requests just to test your connection.
- **`about:config` is off by default, and remote debugging is disabled.** Nobody can attach a debugger from outside the app. You can choose to turn on `about:config` in Settings. While it's on, you can change any engine setting, including privacy-related ones such as telemetry, and a wrong change can cause severe crashes. Leave it off unless you know exactly what you're doing.
- **Incognito tabs use Gecko's private mode.** Cookies, cache, and site storage from incognito tabs aren't saved to disk.
- **Clint Gecko adds its own tracking protection settings on top of the engine.** These are described in the Tracking Protection section below. Clint Gecko adds no Safe Browsing controls of its own. Any such features that are part of the GeckoView version you have installed are governed by Mozilla's engine defaults, and may involve the engine contacting Mozilla or its partners' servers to update lists. See Mozilla's privacy notices for details.

---

## Data We Do Not Collect

Clint Gecko does not collect, store, transmit, or share any personal data. There are no analytics, no crash reporting services, no advertising SDKs, and no backend servers. The developer has no access to your browsing activity, search history, or any other information generated while using the app.

---

## Browsing Data

All browsing data, including history, cookies, cached content, and site storage, is stored locally on your device only. You can clear this data at any time through your device's app settings. Incognito tabs do not save cookies, cached content, or browsing history to your device.

**Tab groups.** Group names, colors, and which tabs belong to each group are stored locally on your device along with your open tabs. Incognito groups are kept in memory only and are gone when you close the app.

**Undo for closed tabs.** When you close tabs, Clint Gecko briefly keeps them in memory so you can tap Undo and bring them back. Nothing is written to storage for this. Incognito tabs are never kept for undo.

Clint Gecko includes an optional **Block third-party cookies** setting and an optional **Do Not Track** signal. These are requests and restrictions applied inside your browser. Websites are free to ignore a Do Not Track request. More protections are described in the Tracking Protection and Data and Device Protection sections below.

---

## Tracking Protection

Clint Gecko includes optional settings that limit how much websites can learn about you. Each one can be turned on or off in Settings under Privacy and Security.

- **Strip tracking parameters.** Removes tracking parameters such as `utm_` and `fbclid` from links before the page loads. This happens on your device. Nothing is sent anywhere to do it.
- **Fingerprinting protection.** Makes it harder for sites to identify you by your browser and device traits. Some sites may look or behave differently when it's on.
- **Send Global Privacy Control.** Sends a signal asking websites not to sell or share your data. Websites may choose to ignore it.
- **WebRTC leak protection.** Stops websites from finding your real IP address through video call features. Some calls may not connect when it's on.

These are requests and restrictions applied inside your browser. They reduce tracking but cannot guarantee that you won't be tracked, and websites may use other techniques that these settings don't cover.

---

## Data and Device Protection

These optional settings help protect your data on your device. All of them are local features, and none of them send anything to the developer or to any server.

- **App lock.** Asks for your fingerprint, face, or screen lock before the app opens, and again after the delay you choose (immediately, 1, 5, or 15 minutes). Android performs the check on your device and only tells Clint Gecko whether it succeeded. Clint Gecko never receives or stores your fingerprint, face data, PIN, pattern, or password. App lock controls access to the app. It does not encrypt the data stored on your device.
- **Block screenshots.** Prevents screenshots and screen recording inside the app and hides the app preview in recent apps. It can't stop someone from photographing your screen with another device.
- **Incognito keyboard.** Asks your keyboard not to learn from what you type in incognito tabs. Your keyboard app decides whether to honor this request, and Clint Gecko has no control over what it does.
- **Auto-clear clipboard.** Clears the clipboard after the delay you choose (30 seconds, 1 minute, or 5 minutes). Clipboard contents are not sent anywhere.
- **Clear data on exit.** Deletes the data you select when you close the app. You can choose any of browsing history, cookies and site data, and cache. Deleted data can't be recovered.
- **Strict certificate checking.** Never lets you continue to a site with an invalid security certificate.

---

## Secure DNS

Before a website loads, your device has to look up its address using DNS. Normally this lookup goes to your network's default resolver, which is usually run by your internet provider. Secure DNS lets the Gecko engine send these lookups over HTTPS (DNS over HTTPS, or DoH) to a provider you choose instead.

You can choose one of three modes in Settings:

- **Off.** Uses your network's default DNS resolver.
- **Increased protection.** Uses DoH when available and falls back to regular DNS if the provider is unavailable. When it falls back, your lookups go to your network's default resolver.
- **Maximum protection.** Uses DoH exclusively. Websites won't load if the DNS provider is unavailable.

**DNS provider.** You can choose Cloudflare, Google, Quad9, or a custom provider. With a custom provider, you enter the address of a DNS-over-HTTPS endpoint yourself.

**What this means for your privacy:**
- When Secure DNS is on, the domain names of the sites you visit are sent to the DNS provider you selected, along with your IP address, the same as any request over the internet. Clint Gecko includes no accounts or personal identifiers in these requests.
- DoH hides your lookups from your network and internet provider, but not from the DNS provider you pick. Choose a provider you trust.
- If you configure a custom provider, Clint Gecko has no way to know what that service does with your lookups. Review its privacy policy before using it.
- Secure DNS applies to lookups made by the Gecko engine. Other requests made by the app itself, such as update checks and Website Blocker list downloads, may use your device's regular DNS resolver.
- Your choices are stored locally on your device.

You can review Cloudflare's privacy information at https://developers.cloudflare.com/1.1.1.1/privacy/public-dns-resolver/, Google's at https://developers.google.com/speed/public-dns/privacy, and Quad9's at https://quad9.net/privacy/policy/

---

---

## Profiles

Profiles let you keep separate browsing identities, each with its own cookies and site storage. A profile's name and color are stored locally in a SQLite database on your device. Profiles are never uploaded or synced anywhere. Deleting a profile removes the data kept for it inside the app.

---

## Search History

When you perform searches in the address or search bar, Clint Gecko saves your search history locally to improve your experience. **All search history data is stored exclusively on your local device using SQLite**, a lightweight file-based database. This data never leaves your device. It is not uploaded to any server, shared with any third party, or transmitted anywhere else.

You can clear your search history at any time through the app's settings or by clearing the app's storage from your device settings.

---

## Search Engine

You can choose which search engine handles the searches you type in the address or search bar, in Settings:

- **DuckDuckGo** (default)
- **Brave Search**
- **Ecosia**
- **Google**
- **Custom**, a URL template you enter yourself

When you search, your query is sent as part of the request URL to whichever engine you've selected. If you configure a custom engine, your query is sent to whatever URL you entered, since the app has no way to know what that service does with it. Review that service's own privacy policy before using it.

---

## Search Suggestions

When you type in the address or search bar, Clint Gecko shows real-time search suggestions to help you find what you're looking for faster.

You can choose which provider these suggestions come from in Settings:

- **DuckDuckGo** (default), fetched from `https://duckduckgo.com/ac/?q=`
- **Google**, fetched from `https://www.google.com/complete/search?client=firefox&q=`
- **Custom**, a URL template you enter yourself

**What this means for your privacy:**
- The text you type is sent only to whichever suggestion provider you've selected, and only to retrieve suggestion results
- No search history, cookies, or personal identifiers from Clint Gecko are included
- Neither provider receives requests associated with your Clint Gecko identity, since the app has none to send
- If you configure a custom suggestions provider, each keystroke you type may be sent to that URL as you type, so only point this at a service you trust

Your saved search history is completely separate and never sent to DuckDuckGo, Google, or any other service.

You can review DuckDuckGo's privacy policy at https://duckduckgo.com/privacy and Google's privacy policy at https://policies.google.com/privacy

---

## Favicons

Website icons (favicons) are loaded to help identify sites in tabs, history, and bookmarks.

- First, Clint Gecko tries to load the favicon directly from the website
- If unavailable, it falls back to DuckDuckGo's favicon service

When using the fallback service, only the domain name is sent to retrieve the icon. No personal data or browsing history is included.

**High-resolution icons.** Favicons in tabs, the tab strip, history, bookmarks, and suggestions, as well as homepage favorites and home screen shortcuts, use larger icons when available. Incognito tabs never look up high-resolution icons. To find one, Clint Gecko first checks the website itself for its larger icons and web app manifest. If the site doesn't offer a large enough icon, Clint Gecko asks Google's favicon service (`t3.gstatic.com`) and then DuckDuckGo's favicon service. Only the site's address without any page path (for example, `https://example.com`) is sent to Google, and only the domain name is sent to DuckDuckGo. Found icons are cached on your device.

If you turn off favicons in Data Saver, none of these requests are made.

You can review DuckDuckGo's privacy policy at https://duckduckgo.com/privacy and Google's privacy policy at https://policies.google.com/privacy

---

## Extensions

Clint Gecko supports browser extensions through Gecko's WebExtensions system. Extensions are optional. None are installed for you, and the app works the same way without them.

**Where extensions come from.**
- You can browse and install extensions from Mozilla's Add-ons site (`https://addons.mozilla.org/android/`), by opening an `.xpi` link on a website, or by installing an `.xpi` file from your device.
- Clint Gecko does not host, review, vet, or endorse any extension. Extensions are written by third parties.

**What you see before installing.**
- Before an extension is installed, Clint Gecko shows the permissions it requests, including which websites it can access and any data collection it declares, and asks you to approve or cancel. Extensions can also ask for extra permissions later, and you'll be asked again.
- You can turn an extension off, remove it, or change its optional permissions at any time from the Extensions screen.
- Gecko may periodically contact Mozilla's Add-ons servers to check for updates to extensions you've installed from there. When it does, your IP address is visible to Mozilla for that request, the same as visiting any website.

**Incognito.** Extensions do not run in incognito tabs unless you allow it for that specific extension. You can change this at any time.

**What extensions can do with your data.** Depending on the permissions you grant, an extension can read or change the pages you visit, see your tabs and browsing activity, and send that information over the internet to its own servers. Once you grant a permission, what that extension does with the data is governed by that extension's developer and their privacy policy, not by Clint Gecko and not by this policy. Only install extensions you trust, and only grant permissions you're comfortable with.

**Where extension data lives.** Installed extensions and the data they store (such as their settings) are kept locally on your device. Clint Gecko does not upload or sync them.

**Clint Engine Bridge.** Clint Gecko includes one built-in extension, the Clint Engine Bridge, that is part of the app itself. It connects the Gecko engine to Clint Gecko's own features, including Media Capture, Website Blocker, Data Saver, Do Not Track, and popup handling. Because those features need to see page requests, it has broad access to the pages you load, but it only communicates with the app on your device. It does not send anything to the developer or to any server.

---

## Custom Themes

The custom theme editor lets you change the colors used throughout the app. Your themes, including their names and colors, are stored locally on your device. The live preview is drawn on your device as you change colors. Themes are never uploaded, synced, or shared by Clint Gecko. Deleting a theme removes it from the app.

---

## Homepage

You can set a custom URL as your homepage and choose whether to show quick actions on it. These choices are stored locally on your device.

**Favorites.** The tiles and folders in the homepage's Favorites section, their order, and any custom images you pick for them are stored locally on your device. When you pick a custom image, Android's photo picker gives Clint Gecko only the image you chose, and a copy is saved inside the app. Favorites are never uploaded or synced, and they are only included in a backup if you create one yourself. How tile icons are fetched is described in the Favicons section above.

When you open the homepage, Clint Gecko connects to the address you entered the same way visiting any website would, so that site can see your IP address and may use its own cookies and tracking. Only use a homepage you trust.

---

---

## Bookmarks

Bookmarks are stored locally on your device only using SQLite. They are never synced, uploaded, or shared. You can delete them at any time through the app or by clearing app storage.

---

## Backup & Restore

Clint Gecko lets you back up your data into a single file you keep for yourself. You choose which categories to include. Depending on what you select, a backup can contain settings, open tabs and tab groups, homepage favorites and their custom images, downloads, Website Blocker data, cookies, profiles, bookmarks, search history, site permissions, shortcuts, installed extensions along with the data they store, and update settings. Here's how it actually works, in plain terms:

- **You choose where it goes.** When you create a backup, Android's own file picker opens and you pick the folder: your device storage, an SD card, wherever. Clint Gecko doesn't send the file anywhere on its own, and there's no cloud sync tied to it.
- **Password protection is optional, and it's real encryption.** If you set a password, the backup is locked with AES‑256‑GCM, the same kind of encryption used by banks and messaging apps. Your password itself is never used directly. It's run through Argon2id first, which is specifically designed to make password-guessing slow and expensive, even for someone with serious hardware. If you leave the password blank, the backup is saved without encryption, which is convenient but means anyone with the file can read it, so treat it like any other unprotected file.
- **We can't help you recover a lost password.** Because the encryption key comes entirely from your password, and we never store or see that password, there's no "forgot password" option and no backdoor. If it's lost, the backup is gone. Please choose something you'll remember, or write it down somewhere safe.
- **Restoring works the same way in reverse.** You pick a backup file, enter the password if it has one, and everything gets read and restored locally on your device. Nothing is uploaded to check the password or verify the file. It either decrypts correctly or it doesn't.

In short: your backups never leave your control unless you move them somewhere yourself.

**Why we ask for your phone's password or biometric unlock first.** Before you can create or restore a backup, Clint Gecko asks you to confirm it's really you, using your fingerprint, face unlock, or your device PIN/pattern/password, whichever you already use to unlock your phone. This isn't sent anywhere or stored by us; Android checks it locally and just tells the app "yes, this is the device owner." We added this step because a backup can contain sensitive stuff, like saved history and cookies that keep you logged into sites, and we didn't want someone who picks up your unlocked phone to be able to quietly export all of that or overwrite it with a restore in a few taps. It's an extra lock on top of the backup password, not a replacement for it.

---

## Downloads

Files you download are saved directly to your device. Clint Gecko does not upload or transmit downloaded files.

To give a file the right name and extension, Clint Gecko may read the server's response headers and the first few bytes of the file. This check happens on your device and nothing is sent anywhere.

The download manager can split a download into segments, such as the pieces of a streaming video, and fetch them from the website. The website's server sees those requests and your IP address, the same as any download. The progress, segment counts, and average segment size shown in a download's Properties are kept on your device, and so is the download history. When a download needs to be combined into one file, this is done locally on your device, and its progress is tracked on your device.

---

## Media Capture

Media Capture detects media resources used by websites, such as video, audio, subtitles, and streaming formats like HLS/M3U8 and DASH, and lets you download them directly. For example, when a page plays a video, Media Capture inspects the page and its network requests to find available media streams and lists them as downloadable options. When Media Capture is turned on, it also reads the page's content, such as media players, links, and player settings, to find every available quality and subtitle, not just the one that is playing. Clint Gecko may contact the website to check a found link's size and format, the same way the page itself would. Some sites serve video and audio as separate streams, and where supported, Media Capture can combine a video that has no sound with a separate audio track into a single playable file. Combining happens entirely on your device.

All of this detection happens locally on your device. Clint Gecko does not intentionally upload captured media, browsing data, or any information about detected media to an external server.

That said, this privacy statement is not an absolute guarantee of privacy or security. Media Capture works by interacting with webpage content, network requests, Android, the Gecko engine, and other components outside Clint Gecko's control. Websites may use their own tracking, DRM, authentication, or other technologies that affect how Media Capture behaves. Media Capture is provided as-is, and Clint Gecko does not guarantee that it will detect every type of media, prevent websites from tracking you, or provide complete privacy or security while using the feature.

---

## Background Playback

Background playback keeps audio and video playing when you leave Clint Gecko, switch to another app, or turn off the screen. You can turn it on or off in Settings.

- **A media notification appears while something plays.** It shows playback controls and, when the page provides them, details about what's playing, such as the title, artist, album, and artwork. In incognito tabs, the notification shows a generic title instead of what you're playing.
- **Those details are shared with Android only.** They are passed to Android's media system so your notification shade, lock screen, headphones, car, or watch can show and control playback. Depending on your device, other apps or connected devices that Android allows to read media sessions may be able to see them too. Clint Gecko doesn't send them anywhere else.
- **Nothing is recorded or uploaded.** Clint Gecko doesn't capture, store, or transmit the audio or video being played. The page streams it to your device the same way it would with the app open.
- **It runs as a foreground service.** This is how Android allows media to keep playing while the app isn't on screen. Websites keep loading and may keep communicating with their own servers while media plays in the background, exactly as they would in the foreground.

---

## Background Behavior and Memory Management

To save battery and memory, Clint Gecko pauses pages when you switch to another app or turn off the screen, and resumes them when you return. Tabs that are playing audio or video stay active so playback continues, and they pause again when the media finishes.

- **Tabs may be hibernated.** When your device is low on memory, background tabs, and in severe cases the active tab while the app is in the background, are put into hibernation to free memory. A hibernated tab reloads when you return to it, so the website may be contacted again at that point.
- **Tabs load when you open them.** Tabs you haven't opened since the app started don't load in the background at startup. No requests are made for them until you open them.
- **Cached images may be cleared.** When memory becomes critically low, cached images are cleared.
- **All of this happens on your device.** Nothing about your tabs or memory use is sent to the developer or to any server.

---

---

## Website Blocker

Website Blocker lets you block entire categories of sites (ads, malware, piracy, tracking, and more) at the domain level.

- **Downloading category lists requires network access.** When you enable a category, Clint Gecko downloads that category's host list directly from its source on GitHub (either `blocklistproject.github.io` or `raw.githubusercontent.com`, depending on the category). This means your device's IP address is visible to GitHub for that request, the same as visiting any website. No personal data, browsing history, or identifiers from Clint Gecko are sent along with it.
- **Everything else happens locally.** Once downloaded, host lists are compiled into a local blocking engine entirely on your device. Checking whether a site should be blocked never involves a network request or a third-party server.
- **Custom websites you add yourself** are stored locally on your device and are never uploaded anywhere.
- You can review exactly which sources are used in `Attribution.md` in the app's repository.

Website Blocker works at the domain level. It is not a full ad and tracker filtering system, and it doesn't inspect page content or hide page elements.

---

## Update Checks

The GitHub build of Clint Gecko can check for new versions. By default it checks each time the app starts, and you can turn that off or skip the check on mobile data in Settings.

- **What's requested.** The app downloads a small file from `raw.githubusercontent.com` that lists the latest stable or beta version. Your IP address is visible to GitHub for that request, the same as visiting any website. No personal data, browsing history, or identifiers from Clint Gecko are sent along with it.
- **Updates are your choice.** If a new version is found, you decide whether to download it. Installing it uses Android's own installer.
- **The FOSS build has no update checker** and makes no update requests.

Similarly, the app's pages that show this policy, the Terms of Service, the changelog, supporters, and attributions load the latest copy of those documents from the project's GitHub repository when you open them.

---

## Shortcuts

Shortcuts let you add a website to your home screen. Here's how they handle your data:

- **Shortcut data is local.** A shortcut's name, URL, icon, frameless setting, and the profile it opens in are stored on your device. They are never uploaded or synced anywhere by Clint Gecko.
- **Shortcut icons may be fetched in high resolution.** When you add a shortcut, Clint Gecko looks for a sharper icon as described in the Favicons section above.
- **The home screen icon belongs to your launcher.** Clint Gecko asks Android to place the icon, and your launcher handles it from there.
- **Opening a shortcut loads the website.** It connects to the site the same way visiting the page normally would.
- **Deleting a shortcut removes its data from the app.** Its icon may stay on your home screen until you remove it yourself.

---

## Autofill

Clint Gecko can work with the Android autofill service you've chosen on your device, such as a password manager, to fill in forms. Any saved passwords or form data live in that autofill service, not in Clint Gecko. Clint Gecko does not store, read, or transmit them, and what that service does with your data is covered by its own privacy policy. You can turn autofill off in Settings.

---

## Website Permissions

Clint Gecko allows websites to request access to certain device features. These permissions are fully controlled by you and can be configured in **Site Settings**.

When a website requests access, you can choose how the browser handles it:

- **Ask first** — The browser will show a prompt every time a website requests access (Default)
- **Always deny** — All website requests for that permission will be blocked automatically without prompting
- **Always allow** — All website requests for that permission will be granted automatically without prompting

### Available Website Permissions

Websites may request access to:

- **Camera** — Used for taking photos or recording video directly from websites
- **Microphone** — Used for voice input, calls, or audio recording features on websites
- **Location** — Used for location-based services such as maps or nearby results
- **Notifications** — Used by websites to send push notifications if you allow them
- **Clipboard** — Used when a website wants to read text from your clipboard, for example to let you paste into a form with one tap

### Site Exceptions

You can define site-specific exceptions that override the default behavior.

- Each website can have its own permission rule
- These overrides take priority over global settings
- If you select "Don't ask again" when responding to a prompt, the website is automatically added to Site Exceptions

You can manage or remove these exceptions anytime from Site Settings.

---

## App Permissions

Clint Gecko requests the following permissions:

## Internet & Network
- **INTERNET** – Lets the app load websites, download files, install extensions, and check for updates.
- **ACCESS_NETWORK_STATE** – Checks if you're connected to the internet (Wi‑Fi or mobile data) so downloads can pause when offline and resume when reconnected.

## Storage & Downloads
- **WRITE_EXTERNAL_STORAGE** – Saves downloaded files to your Downloads folder. *(Android 9 and below only)*
- **POST_NOTIFICATIONS** – Shows download progress, completion, and failure alerts, and the media controls for background playback, in your notification bar.
- **REQUEST_INSTALL_PACKAGES** – Installs APK files downloaded from the download screen and installing updates from GitHub.
- **SCHEDULE_EXACT_ALARM** – Download Scheduler
- **MANAGE_EXTERNAL_STORAGE** – Allow the download manager to write directly to a custom location without requiring SAF (exclusively available in the GitHub Release).

## Camera & Audio
- **CAMERA** – Used when a website asks you to upload a photo and you choose to take one with your camera, or when a website requests camera access for video calls.
- **RECORD_AUDIO** – Used for the voice search button in the browser, and when a website requests microphone access for voice or video calls.
- **MODIFY_AUDIO_SETTINGS** – Required for WebRTC voice/video calls on websites. The engine needs this to manage audio routing during a call.

## Location
- **ACCESS_FINE_LOCATION** – Used when a website asks for your precise location, e.g., to show nearby places or get directions.
- **ACCESS_COARSE_LOCATION** – Same as above, but used as a fallback when only approximate location is available.

## App & System Integration
- **QUERY_ALL_PACKAGES** – Detects which apps on your device can handle special links, like opening a phone number in your dialer or a store link in the Play Store.
- **FOREGROUND_SERVICE** – Keeps downloading files and playing media even when you switch to another app.
- **FOREGROUND_SERVICE_DATA_SYNC** – Works with `FOREGROUND_SERVICE` to tell Android the background activity is a file download.
- **FOREGROUND_SERVICE_MEDIA_PROCESSING** – Works with `FOREGROUND_SERVICE` to tell Android the background activity is processing a downloaded video stream, such as combining its segments into a single file.
- **FOREGROUND_SERVICE_MEDIA_PLAYBACK** – Works with `FOREGROUND_SERVICE` to tell Android the background activity is audio or video playback, so background playback keeps running with the app closed or the screen off.

## Security
- **USE_BIOMETRIC** – Lets App lock and the backup confirmation ask Android to verify you with your fingerprint, face, or screen lock. Android does the check, and Clint Gecko only receives a yes or no.

## Power & Background
- **REQUEST_IGNORE_BATTERY_OPTIMIZATIONS** – Prevents Android from pausing downloads when the device is in battery saver or Doze mode.
- **WAKE_LOCK** – Prevents your device from sleeping while a download is in progress so files don't get stuck halfway.
- **RECEIVE_BOOT_COMPLETED** – Checks for unfinished downloads after your device restarts and resumes them automatically.

No permission is used for tracking or data collection.

---

## Crash Logs

If the app crashes, Clint Gecko saves a crash log on your device so you can look at it in Debug & Crash Reports. This includes crashes caused by the Gecko engine. Engine crashes are saved locally in the same way, and the engine's own crash reporting to Mozilla stays turned off. These logs stay on your device. Nothing is sent automatically. If you want to report a bug, you choose whether to copy a log and share it on GitHub or with the developer. Crash logs can include technical details about your device and the app's state, so look through a log before sharing it. You can delete saved logs at any time from Settings.

---

## Data Retention and Deletion

Since no personal data is collected by the developer, there is nothing to delete on our servers. All local data (history, bookmarks, downloads, extensions and their data, etc.) is stored on your device. This includes your custom themes, homepage choices, and privacy settings. You can delete any or all of it at any time through the app's settings, with Clear data on exit, or by clearing the app's storage in your device settings.

---

## Children's Privacy

Clint Gecko does not knowingly collect any personal information from anyone, including children under the age of 13. The app has no accounts, no sign‑ins, and no data transmission to the developer. If you are a parent or guardian and believe your child has used the app in a way that concerns you, you may contact us, but note that no data has been collected by us.

---

## Third-Party Services

Clint Gecko does not include analytics, advertising, or tracking SDKs.

However, the search engine or services you choose (such as DuckDuckGo, Brave Search, Ecosia, Google, or a custom one you configure) may collect data according to their own privacy policies. Clint Gecko has no control over those services. Additionally:

- The search suggestions feature uses DuckDuckGo's or Google's public suggestion API, or a custom one you configure, depending on which provider you've selected.
- The favicon fallback feature uses DuckDuckGo's public API, and high-resolution icons may also use Google's favicon service, as described above.
- If you turn on Secure DNS, the domain names of the sites you visit are sent to the DNS provider you choose (Cloudflare, Google, Quad9, or a custom one), as described above.
- Website Blocker list downloads and update checks are fetched from GitHub, as described above.
- Extensions you install, and Mozilla's Add-ons site when you browse it, are run by third parties with their own privacy policies.
- The GeckoView engine is developed by Mozilla and may contact Mozilla's servers for engine features Clint Gecko doesn't disable, as described above.

---

## Changes to This Policy

Any updates to this privacy policy will be reflected in this document on the GitHub repository. The app always links to the latest version. Your continued use of the app after changes means you accept the updated policy.

---

## Open Source

Clint Gecko is fully open source. You can review the source code to verify how the app works at:
https://github.com/jhaiian/ClintGecko

---

## Contact

If you have questions about this privacy policy, you can reach the developer at `jhaiian@proton.me` or through the community Discord at https://discord.gg/4kUe4yPQ32
