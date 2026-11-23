# Changelog

All notable changes to Clint Gecko are documented here.

---

# v1.0.6

## Announcement

Today, November 24, 2026, marks the anniversary of the premiere of Hawkeye, my favorite show of all time. To celebrate, I'm introducing all of you to:

- **[Kate](https://github.com/jhaiian/Kate)** – An open-source local music player with synced lyrics and customizable themes.
- **[Lucky](https://github.com/jhaiian/Lucky)** – An Android video player built with Rust, featuring motion interpolation.

I engineered both of them, and both support Clint themes!

Lucky is still in early development, but frick it, I'm releasing it today as well.

As for Clint, read the full changelog below!

---

## What's New

### Video Player

Clint now has its own video player! It works with videos on almost any website.

- Tap a website's fullscreen button and the video opens in Clint's player instead. If you'd rather use the site's own player, turn off **Use for website fullscreen**.
- A small floating bar shows up over videos with **Pop-up player**, **Fullscreen**, and a close button. You can use it to open any video in Clint's player, even when the site's fullscreen button doesn't work.
- The player has a seek bar, play and pause, skip back and forward, playback speed from 0.25x to 3x, screen fit (Fit, Stretch, Zoom, Original, 16:9, and 4:3), lock controls, rotate screen, pop-up player, and play in background. Live streams show a **LIVE** badge.
- Swipe the left side to change the brightness, the right side to change the volume, or across the screen to seek.
- Double-tap the left or right side to skip back or forward, and press and hold to play at 2x until you let go.
- If Media Capture found other qualities or audio tracks for the video, you can switch between them right from the player. Subtitles that come with the video can be turned on and off too.
- Clint remembers the screen fit you picked.
- The screen stays on while a video is playing.
- **Auto picture-in-picture** keeps a fullscreen video playing in a small window when you leave the app. The window has skip back, play and pause, and skip forward buttons, and the video fills the whole window.
- Switching to another tab closes the player.
- The Media Capture video preview and the new **Play** button in the download dialog use the same player.

#### Video Player Settings

Everything is in the new **Video player** page in Settings › Browser.

- Turn the player, **Use for website fullscreen**, **Floating video button**, and **Auto picture-in-picture** on or off.
- Turn **Swipe gestures**, **Double-tap to skip**, and **Hold to speed up** on or off.
- **Remember brightness and volume** brings back the levels you last set with swipe gestures when a video opens. It's off by default.
- **Skip interval** can be 5, 10, or 30 seconds.
- **Player buttons** lets you choose which buttons show in the player.
- **Progress bar style** has Slider, Thin line, Thick bar, Squiggle, and Dots.
- **Hide controls after** 2, 4, 6, or 10 seconds.
- **Theme-colored icons** tints the player icons with your accent color instead of white.

(Thanks to @Woallin, @xmiseggs, @meishah, and @gigisforza70 for suggesting this [#14](https://github.com/jhaiian/ClintGecko/issues/14) [#38](https://github.com/jhaiian/ClintGecko/issues/38) [#85](https://github.com/jhaiian/ClintGecko/issues/85) [#72](https://github.com/jhaiian/ClintBrowser/issues/72))

---

### Profiles

Profiles are now fully separate from each other. Each one has its own tabs, cookies, site data, bookmarks, history, downloads, site permissions, and extensions, so you can keep things like Work and Personal apart.

- Tap a profile to switch to it, or use **Switch profile** in the browser menu or the tab manager's ⋮ menu.
- Tabs from every profile stay in your tab manager. Opening a tab from another profile switches you to that profile.
- Each profile can have its own extensions turned on.
- New **Temporary profile** option. Its tabs, data, and bookmarks are erased when its last tab closes or when you close the app.
- New **Profile switcher** button for the address bar. Tap it to pick a profile, or long press it to switch to the next one. It's hidden by default, so turn it on in Customize Toolbar if you want it.
- Each profile can also have its own homepage and favorites.
- Deleting a profile now also deletes its bookmarks, history, and site permissions. Its downloads move to the Default profile.

(Thanks to @Subbarao6338 for suggesting this [#71](https://github.com/jhaiian/ClintGecko/issues/71))

---

### A New Browser Menu

The browser menu got a full redesign.

- Items are now grouped into **Quick actions** (a grid of icon tiles), **Toggles** (on and off chips), and a **List** of full-width rows.
- A page header at the top shows the site name and whether the connection is secure.
- A navigation bar with Back, Forward, Reload, Bookmark, and Share. You can swap any of them for Home or New tab, or hide the bar completely.
- New **Create Favorite** item. It opens the Add site dialog with the current page already filled in.
- New **Find in page** item.
- The extensions list in the menu can now be sorted by **Enabled first**, **Name (A–Z)**, **Recently used**, or **Custom order**. You set the custom order by dragging.
- A **Customize** shortcut at the bottom of the menu.

#### Customize Menu

- Drag items between sections, or tap the section button on an item to move it. Items in **Hidden** won't show up in the menu.
- New **Grid columns** (3, 4, or 5), **Density** (Comfortable or Compact), **Page header**, **Labels under icons**, and **Show navigation bar** options.
- Tap a navigation bar button to swap it for another one.
- A live preview of your menu at the top. Tap the arrows to see it fullscreen.
- Your old order and hidden items carry over.

(Thanks to @Subbarao6338 and @super585 for suggesting this [#69](https://github.com/jhaiian/ClintGecko/issues/69) [#81](https://github.com/jhaiian/ClintBrowser/issues/81))

---

### Customize Toolbar

New **Customize Toolbar** page in Settings › Look and feel.

- Reorder the buttons next to the address bar, or turn off the ones you don't use. That's Extensions, Media Capture, Profile switcher, and the tab counter. The menu button is always shown.
- Pick 3 to 6 actions for the bottom bar and drag to reorder them. You can choose from Back, Forward, Home, Reload, Bookmark, Share, New tab, Tabs, Downloads, History, Find in page, and Menu.
- Every bottom bar button can have its own long-press action. Tap the hand icon to pick one.
(Thanks to @akshaymaurya26 for suggesting this [#27](https://github.com/jhaiian/ClintGecko/issues/27))
- **Compact bars** makes the address bar and bottom bar shorter.
(Thanks to @wibucter for suggesting this [#44](https://github.com/jhaiian/ClintGecko/issues/44))
- **Button labels** shows a label under each bottom bar button.
- **Also show at the bottom** adds the bottom bar buttons under the address bar when the address bar is at the bottom.
- New **Progress bar** option for the page loading line. Keep **Classic** or switch to the new Material 3 **Squiggle**.
(Thanks to @RENBOWS for suggesting this [#77](https://github.com/jhaiian/ClintGecko/issues/77))
- **Search bar shape** moved here. It's used by the address bar, the search screen, and the homepage.

Compact bars, button labels, and the bottom row need a restart. Clint asks first, and if you pick **Later** it restarts when you leave the page.

---

### Pinned Extensions

- Pin an extension from the extensions panel in the menu or from its details page. Its icon and badge show up next to the address bar. Tap it to use the extension, or long press it to open its settings.
- You can also pin an **Extensions** button. It opens the extensions panel as a popup.
- How many fit depends on your screen. Phones show 2, medium screens show 4, and large screens show 6. Extra pinned extensions go to the top of the Extensions popup, and the Extensions button shows up on its own when there are extras.

---

### Homepage Customization

Homepage settings now have their own page in Settings › Browser › Homepage, with a **Live Preview** at the top. Tap the arrows to see it fullscreen.

#### Header

- **Header Style** shows the logo and title, only the logo, only the title, or nothing.
- **Custom Title** replaces the name on your homepage. Leave it empty to use the default.
- **Show Greeting** adds a greeting above the title based on the time of day.

#### Content

- **Quick Action Buttons** lets you pick which buttons show. You can choose from Bookmarks, History, Downloads, Incognito switch, Settings, Extensions, and Profiles.
- **Section Order** lets you rearrange the search bar, quick actions, favorites, recent sites, and widgets.
- New widgets for a **Clock**, **Continue where you left off**, and **Active downloads**.
- **Recent Sites Shown** from 1 to 10.
- **Voice Search Button** to show or hide the microphone in the search bar.

#### Tiles and Cards

- **Favorites Columns** can be Auto or 3 to 6.
- **Tile Shape** (rounded square, circle, or square), **Tile Size**, and **Tile Labels**.
- **Folder Icon** can show a preview grid of the sites inside or a plain folder.
- **Card Opacity** and **Card Corner Radius**.

#### Background

- New **Solid Color** design with any color you want.
- New **Custom Gradient** design that blends two colors at any angle.
- New **Image Dimming**, **Image Blur**, and **Parallax Scrolling** options for your own photo.
- **Separate Dark Theme Image** lets you use a different photo while the dark theme is on.

#### Profiles

- **Customize For** lets each profile have its own homepage.
- **Separate Favorites** gives a profile its own favorites instead of sharing the default ones.
- **Reset to Default** removes a profile's own homepage settings.

---

### Search Suggestions

Search suggestions are cleaner now and a lot more customizable.

- New **Compact** style, now the default. It drops the boxed icons, card backgrounds, and dividers. The old style is still there as **Cards**.
- Repeated history entries are merged, and history that just repeats what you typed is hidden.
- New **Open tabs** section. Tap **Switch to tab** to jump to a tab you already have open.
- Your homepage favorites show up when the search bar is empty.
- A **Paste & Go** button opens a copied link or text right away.
- Swipe a history suggestion to delete it. Long press a suggestion to fill it into the search bar or remove it.
- The part of a search suggestion you haven't typed yet is now in bold.
- New engine button inside the search field. Tap it to search once with a different engine without changing your settings. Web addresses still open directly.
- The back arrow in the search field is gone, since the back gesture already closes the search screen.

#### Suggestions Settings

Suggestions now have their own page in Settings › Browser › Suggestions.

- **Suggestion style**, **Section order** (Bookmarks first, History first, Web suggestions first, or Mixed), and **Text size**.
- **Web addresses** shows only the domain, the full address, or nothing.
- **Show section titles** and **Show fill arrow**.
- **Bookmark suggestions shown** and **History suggestions shown**.
- Switches for open tabs, favorites, Paste & Go, and the quick engine switch.

#### Custom Search Engines

- You can now add as many custom search engines and suggestion APIs as you want, instead of just one. They work for both normal browsing and Incognito.
- Each one has a name, an optional description, and its URL. The description shows under the name in the pickers.
- Search engines and suggestion APIs now have their own editors, and the `{query}` hint is only shown once.
- Custom engines also show up in the one-time engine picker.
- Your old custom search engine and suggestions API are moved into the list automatically.

(Thanks to @xmiseggs for suggesting this [#62](https://github.com/jhaiian/ClintGecko/issues/62))

---

### Find in Page

Clint now has Find in page!

- Open it from the menu, the text selection menu, or the bottom bar if you add it there. When you type in the address bar, Clint also offers to find that text on the current page.
- Every match is highlighted. The current one stands out, the others are amber, and small markers on the right edge show where the matches are on the page.
- Search options for **Match case**, **Whole word**, **Links only**, and **Regex**.
- **Results** shows every match in a list with the text around it. Tap one to jump to it.
- Your recent searches show up when the bar is empty. You can also paste or search by voice.
- With a keyboard, Ctrl+F opens it, F3 or Ctrl+G goes to the next match (add Shift to go back), and Esc closes it.

#### Find in Page Settings

New **Find in page** page in Settings › Browser.

- **Show bar at the top** puts the find bar under the top toolbar instead of above the keyboard.
- **Dim the page** darkens everything except the matches.
- **Match markers on the edge**, **Remember search options**, and **Suggest in the address bar**.
- **Save recent searches** and **Clear recent searches**. Nothing is saved in private tabs.

(Thanks to @NielSync and @rayfonz for suggesting this [#90](https://github.com/jhaiian/ClintBrowser/issues/90) [#113](https://github.com/jhaiian/ClintBrowser/issues/113))

(Thanks to @NielSync for the PR that started this [#103](https://github.com/jhaiian/ClintBrowser/pull/103))

---

### Tabs

#### Pinned Tabs

You can now pin tabs from the tab's menu in the tab manager or the tab strip.

- Pinned tabs stay at the start of your tabs and show as a small icon in the tab strip.
- **Close all tabs**, **Close other tabs**, closing duplicates, and auto-closing inactive tabs all skip pinned tabs.
- Pinned tabs are kept loaded in the background longer than other tabs.
- A pinned tab can't be in a group.

#### Sort Tabs and Group by Site

- New **Sort tabs** option in the tab manager's ⋮ menu. Sort by **Last used**, **Site**, or **Title**. Pinned tabs stay first and groups stay together.
- New **Group by site** option. It puts every site that has 2 or more ungrouped tabs into its own group, with a name and color picked for you.

#### Recently Closed

New **Recently closed** list in the tab manager's ⋮ menu.

- It keeps your last 25 closed tabs. Incognito tabs are never saved.
- Tap one to reopen it, and it goes back into its group.
- Remove tabs one by one or tap **Clear all**.
- The list is cleared when you delete your browsing history or remove a profile.

#### Close Tabs with Back

New **Close Tabs with Back** setting in Settings › Look and feel.

- **Tabs opened from links** (default) closes a tab opened from a link and takes you back to the tab that opened it.
- **Any tab** closes the current tab and switches to the last tab you used. Pinned tabs stay open.
- **Never** only goes back to the previous page.

(Thanks to @Zarif321 and @Iconicspectre99 for suggesting this [#122](https://github.com/jhaiian/ClintBrowser/issues/122) [#114](https://github.com/jhaiian/ClintBrowser/issues/114))

---

### Custom Tabs

Clint now supports Custom Tabs, so apps that open links in an in-app browser can use Clint for it.

- Clint shows up as a Custom Tabs browser for apps that look for one.
- Links open in a simple Clint window with a close button, the page title, and the site address.
- The ⋮ menu has **Share**, **Copy link**, and **Open in Clint**.

---

### Download Manager

#### View Modes

The library now has 10 view modes to choose from, **Grid**, **Small grid**, **Extra small grid**, **Big list**, **Large list**, **List**, **Small list**, **Extra small list**, **Compact list**, and **List without images**.

Pinch to zoom in or out to switch between them. The items morph smoothly from one layout to the next.

(Inspired by Kate)

#### Customize Downloads

New **Customize downloads** page in Settings › Downloads, with a live preview of a download and its notification.

- **Progress style** (Ring or Bar) and **Density** (Compact, Comfortable, or Spacious).
- Show or hide **thumbnails**, **speed**, **time remaining**, **source site**, and the **parts bar**.
- Show or hide the **live speed card**, the **download graph**, and the **total speed graph**. **Pause all** stays in the menu when the speed card is hidden.
- **Default sorting** for the library.
- **Tapping a finished download** can open the file, show its details, or share it.
- **Details menu actions** lets you choose which actions show up in a download's details.
- Notification options for **Action buttons**, **Speed and time left**, **Progress bar**, and **Quiet completion**.

(Thanks to @iHarryPotter178 for suggesting this [#88](https://github.com/jhaiian/ClintBrowser/issues/88))

#### Live Updates

On Android 16 and newer, active downloads now show as a live update on the status bar, the lock screen, or the island on supported phones. It shows the percentage in the status bar and has Pause and Cancel buttons. It's on by default, and you can turn it off with **Live updates** in Customize downloads. If Android is blocking it for Clint, a shortcut takes you to the right system setting.

#### Saved Locations

You can now save several download folders in Settings › Downloads › **Saved locations** and pick where each download goes right from the download dialog. This works for links, manual downloads, and MEGA.

#### Use Any Download Manager

The **Download manager** setting has a new **Custom app** option, so you can send downloads to any download manager you have installed, like AB Download Manager.

Third-party download managers now get the site's cookies, User-Agent, and referrer, so downloads that need your login keep working. Cookies can give full access to your account on that site, so Clint warns you about this at the top of the dialog. Only pick a download manager you trust.

(Thanks to @TivyxLab and @pal246 for suggesting this [#18](https://github.com/jhaiian/ClintGecko/issues/18) [#80](https://github.com/jhaiian/ClintGecko/issues/80))

#### Links From Other Apps

When another app sends a link to Clint's download manager, only the download dialog shows up now, right on top of that app. Before, the whole Downloads screen opened. Clint also uses the cookies, User-Agent, referrer, and file name the other app sends with the link.

(Thanks to @Subbarao6338 for reporting this [#67](https://github.com/jhaiian/ClintGecko/issues/67))

#### Play Before You Download

Video downloads now have a **Play** button in the download dialog. It opens the video in Clint's player with the same User-Agent, referrer, and cookies as the download, so you can check it before saving.

#### More Reliable Downloads

- Downloads that can't be resumed now wait up to an hour for a stalled connection to come back. Before, they failed after 60 seconds and started over from zero.
- On Android 8 to 13, Clint keeps Wi-Fi awake while downloading so it doesn't go into power saving with the screen off.
- If Android stops a download job, downloads that can't be resumed move to the foreground service instead of being stopped.
- **Pause all**, bulk pause, and the notification's Pause button no longer pause a download that can't be resumed once it has started, since that would throw away its progress.
- If a server starts refusing extra connections with a 403 error, Clint now uses fewer connections instead of failing, just like it already did for 429 errors. It also remembers the lower number for that server.
- A connection that gets no data for 15 seconds now reconnects on its own.
- Pausing now stops a download right away.
- Changing the number of concurrent downloads now applies right away.
- Queued downloads show their place in the queue and their details.

---

### Media Capture

Media Capture got a redesign.

- Media info now wraps onto more lines, so the size, codec, bitrate, and duration are never cut off.
(Thanks to @anonymous-shanks for suggesting this [#40](https://github.com/jhaiian/ClintGecko/issues/40))
- New **Select multiple** mode with **Select all** and **Deselect all**. The header shows how many you picked and their total size. Download them all at once from one dialog, or add them to Downloads paused.
(Thanks to @anonymous-shanks for suggesting this [#39](https://github.com/jhaiian/ClintGecko/issues/39))
- New **Hide small media** setting in the Media Capture settings. It hides anything smaller than the size you pick, in KB or MB, with a few presets. Subtitles and media with an unknown size always stay visible. A banner shows when the filter is on, with a **Show all** button and a "Showing X of Y" count.
(Thanks to @anurag565 for suggesting this [#104](https://github.com/jhaiian/ClintBrowser/issues/104))
- The sheet no longer goes under the navigation bar or the keyboard.

---

### Pop-ups and Sign-ins

#### Let Extensions Handle Pop-ups

New **Let extensions handle popups** setting in Settings › Look and feel. It's on by default.

When you have a content blocker like uBlock Origin installed, it now decides which pop-ups to block. Blocked pop-ups disappear quietly, and the rest open without asking, just like in Firefox. Without a content blocker, you still get the popup alert like before. Turn it off if you want every pop-up to another site to ask first.

(Thanks to @anonymous-shanks for reporting this [#79](https://github.com/jhaiian/ClintGecko/issues/79))

#### Sign In With Google and Others

Signing in with Google and other accounts that use a pop-up now works. Before, you'd pick your account and get stuck on a blank white page. Pop-ups now open in the window the website created, so the site that opened it can still talk to it.

- If you decline a pop-up, the website now knows it was blocked.
- When a pop-up closes itself, you go back to the tab that opened it.

(Thanks to @fajarrasyid for reporting this [#84](https://github.com/jhaiian/ClintGecko/issues/84))

---

### Parental Controls

New **Parental controls** page in Settings, under Protection.

#### Parental Password

- Set a password that only you know. It's separate from your phone's screen lock and needs at least 4 characters.
- Once it's set, changes that could weaken your protection ask for it first.
- The password is never saved as plain text.
- After too many wrong tries, it locks you out for a while. Each lockout lasts longer, up to 30 minutes.
- If you forget it, it can't be recovered.

#### What It Protects

You can turn protection on or off for each area. Turning one off needs the password.

- **Website Blocker** asks for it before you turn the blocker off or delete its lists and websites.
- **Extensions** asks for it before you open, turn off, or remove an extension, take away its private browsing or site access, or open extension pages like `about:addons`. Unlocking a page only works for that tab and that page.
- **History** asks for it before you delete history entries, clear all history, remove search suggestions, delete browsing data that includes history, or turn on clearing history on exit.
- **Backup restore** asks for it before you restore a backup.

#### Harder to Turn Off Website Blocker

Even without a password, Website Blocker is now harder to turn off on a whim. Turning off the blocker, an active category, or Additional Websites, or resetting or deleting protected lists, now makes you wait 30 seconds and then type a random 6-character code. The toggle in the browser menu no longer turns it off in one tap either. If you set a parental password, it replaces the wait and the code.

(Thanks to UnbeatenPlayzYT from Discord for suggesting this)

---

### Performance Boost

New **Performance boost** page in Settings, under Advanced. Turn these on if pages lag or Clint crashes on heavy sites. Each switch tells you what you gain and what you give up.

- **Reduce blur effects** skips the frosted glass blur behind headers, menus, and cards.
- **Turn off WebGL** stops sites from using 3D graphics, which saves a lot of GPU memory.
- **Limit to 30 frames per second** makes the GPU and battery work less.
- **Reduce animations** asks sites to skip transitions and other moving effects.
- **Stop animated images** shows GIFs as still pictures.

Changes apply to pages you open next, so reload your open tabs to feel the difference.

---

### Website Colors

New **Website colors** option in Settings › Accessibility.

- **Original colors** shows websites the way they are.
- **Grayscale** replaces all colors with shades of gray.
- **Sepia** gives pages warm brown tones, like an old photo.
- **Black and white** uses only black and white for the strongest contrast.
- **Monochrome** uses shades of a single color that matches your theme.

It applies to all your open tabs right away, no reload needed.

(Thanks to UnbeatenPlayzYT from Discord for suggesting this)

---

### A New Setup

The first-time setup has been redesigned from scratch, with an animated picture for every step and a step counter at the top.

- The welcome screen shows what Clint can do before you agree to the terms.
- **Look** has your theme, accent color, and the pure background option. Only a few accent colors show at first. Tap **Show all colors** to see the rest.
- **Layout** has the address bar position, menu style, and **Hide toolbars while scrolling**.
- **Search** lets you pick your search engine, now with each engine's icon and a preview.
- **Privacy** has Enhanced Tracking Protection, cookie protection, fingerprinting protection, removing tracking parameters, Global Privacy Control, and Do Not Track.
- **Security** has HTTPS-Only Mode, Secure DNS, WebRTC protection, clear data on exit, and screenshot blocking.
- **Add superpowers** recommends uBlock Origin, Dark Reader, Bitwarden, SponsorBlock, I still don't care about cookies, and Violentmonkey, and you can add any of them in one tap. They're only downloaded from addons.mozilla.org if you tap **Add**.
- **Notifications** (Android 13 and newer) and **Default browser** steps, then you're all set.

---

### Faster Startup

- Fixed the first page sometimes staying blank for up to 8 seconds after opening the app. Clint and GeckoView kept waiting on each other at startup.
- Your tabs now start loading right away instead of waiting for Clint's built-in extension to be ready.
- Clint now saves its built-in scripts, so they're ready the moment GeckoView starts.
- GeckoView now starts as soon as you open the app, while the window is still being created, just like Firefox does.
- The address bar no longer goes blank while Clint is starting.
- Coming back to Clint is faster. The tab you were on stays active while the app is in the background, so the page shows right away instead of being redrawn.
- Fixed taps and scrolls not working right after coming back to Clint and then being applied all at once.
(Thanks to @xmiseggs for reporting this [#37](https://github.com/jhaiian/ClintGecko/issues/37))

---

### New Languages

Clint is now available in:

- **Simplified Chinese** (简体中文)
- **Vietnamese** (Tiếng Việt)
- **French** (Français)
- **German** (Deutsch)
- **Arabic** (العربية)

(Thanks to @Linyesantan for the Simplified Chinese translation [#116](https://github.com/jhaiian/ClintBrowser/pull/116))

---

## Improvements

- New **Strip HTTP/HTTPS/WWW from URLs** setting in Settings › Look and feel. It hides `http://`, `https://`, and `www.` in the address bar and the tab tray. It's on by default.
- Hiding the toolbars while scrolling no longer resizes the page, so the page doesn't jump around or reflow.
(Thanks to Wibu CTer from Discord for suggesting this)
- The toolbars now follow your finger when they hide on scroll and settle when you let go. Before, they snapped after you scrolled a certain distance, which made them jump back and forth on slow scrolls.
- Long pressing an image now finds it even when it's under another layer. It also works with background images, `<picture>` images, lazy loaded images, SVGs, and canvases, and picks the largest version of the image.
(Thanks to @HatoGreyrat for suggesting this [#74](https://github.com/jhaiian/ClintBrowser/issues/74))
- Long pressing a linked image now shows the image actions too, like **Download image**, **Preview image**, **Open image in new tab**, **Copy image**, and **Share image**.
- Opening a MEGA link now shows a floating **Download from MEGA** button instead of opening the download dialog right away.
(Thanks to @Zarif321 for suggesting this [#109](https://github.com/jhaiian/ClintBrowser/issues/109))
- History now keeps everything until you clear it. Before, only your last 50 sites were kept.
(Thanks to @Iconicspectre99 for reporting this [#114](https://github.com/jhaiian/ClintBrowser/issues/114))
- You can now swipe snackbars away instead of waiting for them to disappear.
(Thanks to @anonymous-shanks and @NielSync for suggesting this [#78](https://github.com/jhaiian/ClintGecko/issues/78) [#90](https://github.com/jhaiian/ClintBrowser/issues/90), and to @NielSync for the PR that started this [#101](https://github.com/jhaiian/ClintBrowser/pull/101))
- The notification icon is bigger now, so it's the same size as other apps.
(Thanks to @RENBOWS for suggesting this [#76](https://github.com/jhaiian/ClintGecko/issues/76))
- Every finished download now shows up in your gallery and other apps right away, including images saved from blob and data links and Media Capture streams.
(Thanks to @Linyesantan for the fix [#117](https://github.com/jhaiian/ClintBrowser/pull/117))
- New **Bookmarks** entry in Settings › Browser.
- About now has a **More apps by the developer** section with Kate, Lucky, and Clint Browser.

---

## Other Changes

- Updated Privacy Policy and Terms of Service.

I added profiles, parental controls, the video player, Custom Tabs, pop-ups, recently closed tabs, Find in page, and the new download options to the Privacy Policy. The big one is third-party download managers. They now get the site's cookies so downloads that need your login keep working, so I explained what that means and why you should only pick one you trust. I also added that finished downloads now show up in your gallery, that the add-on icons in setup are loaded from Mozilla, and the new live update permission. The Terms of Service now has sections for the video player, third-party download managers, and parental controls. As always, nothing leaves your device except what each feature needs to work.

- The store listings are now translated into Vietnamese, French, German, and Arabic too.

---

## Dependency Updates

- Bumped GeckoView from `159.0.20261022150525` to `160.0.20261116130455`.
- Bumped Android Gradle Plugin from `9.5.0` to `9.5.1`.
- Bumped Gradle from `9.8.2` to `9.9.1`.
- Bumped BouncyCastle from `1.86` to `1.87`.
- Added AndroidX Browser `1.10.0` for Custom Tabs.

---

## Bug Fixes

- Fixed links opened in a new tab from an Incognito tab opening in a normal tab. They now stay in Incognito, and so do images opened in a new tab.
(Thanks to @NielSync for reporting and fixing this [#90](https://github.com/jhaiian/ClintBrowser/issues/90) [#100](https://github.com/jhaiian/ClintBrowser/pull/100))
- Fixed resuming a download after its link expired marking it as complete. Some servers send a playlist or a page instead of the file, and Clint deleted the part you already had and saved that instead. Now it keeps your progress and tells you the link expired.
(Thanks to @DarkHorse313 for reporting this [#86](https://github.com/jhaiian/ClintGecko/issues/86))
- Fixed downloads doing nothing on sites that remove the file link right after you tap Download, like the **Download** button for raw files on GitHub.
(Thanks to @cabinz3q for reporting this [#48](https://github.com/jhaiian/ClintGecko/issues/48))
- Fixed pausing and quickly resuming a multi-part download starting every part over from the beginning.
- Fixed the link long-press sheet showing "null" instead of the link's text, and showing a different address than the real link.
- Fixed bottom sheets closing on their own after a quick scroll.
- Fixed the browser menu flickering when dragged to the top of the screen.

---

## Donation

Thank you to **Stefan Zipperer** for donating $5.28 through PayPal!

Every bit of support helps me keep working on Clint. Thank you so much!

---

# v1.0.5

## What's New

### MEGA Downloads

You can now download straight from MEGA links!

- Open a MEGA file or folder link and Clint offers to download it for you.
- Folder links open a picker where you can select all, keep the folder structure, skip files you already added, and download on Wi-Fi only.
- MEGA downloads can be paused, resumed, and split into multiple parts like any other download.
- Files are decrypted on your device while they download. The key in the link never leaves your phone.
- Clint gets a fresh download link from MEGA every time a download starts or retries, so old links don't break your downloads.
- Quota limits and broken links now show a clear message instead of a vague error.
- Don't want it? Turn off **Detect MEGA links** in Settings › Downloads.

---

### Download Manager

#### Add Without Starting

New **Add** button in the bottom left of every download dialog. It puts the download in your list paused so you can start it later.

- It works for links, streams and Media Capture, blobs, MEGA files and folders, redownloads, and manual downloads.
- It skips the mobile data and battery prompts, since nothing is downloading yet.
- It's turned off while you have a scheduled start set.
- Paused blob downloads are kept safe in app storage, so they survive a restart.

(Thanks to @anonymous-shanks for suggesting this [#32](https://github.com/jhaiian/ClintGecko/issues/32))

#### Refreshing Links

- Each download now remembers the page it came from, separately from the Referer header. Refreshing a link takes you back to the right page instead of just the site's home page or the video player's site. The Properties dialog shows both.
- While you're refreshing a link, picking a video in Media Capture offers to update the paused download right away.
- Updating a link now sends the same User-Agent, Referer, and cookies as the download, skips the check when the link didn't change, ignores error pages, and tells you when a link opens a web page instead of a file.

(Thanks to @DarkHorse313 for reporting this [#16](https://github.com/jhaiian/ClintGecko/issues/16))

#### Download History

New **Save download history** and **Save Incognito download history** switches in Settings › Downloads. Turn one off and finished downloads from that mode won't stay in your Downloads list. The file and the completion notification stay either way. Unfinished, paused, and failed downloads are always kept so you can resume or retry them.

(Thanks to @anonymous-shanks for suggesting this [#32](https://github.com/jhaiian/ClintGecko/issues/32))

#### Duplicate File Handling

New **Duplicate file handling** setting in Settings › Downloads.

- **Always ask** lets you choose what to do each time.
- **Add a duplicate** keeps both files and adds a number to the new one.
- **Overwrite existing file** replaces the old file.

The automatic options only kick in when the existing download is already finished.

#### Other Download Changes

- The Downloads library remembers whether you used grid or list view.

(Thanks to @anonymous-shanks for suggesting this [#32](https://github.com/jhaiian/ClintGecko/issues/32))

---

### Faster and Smoother GeckoView

This update makes GeckoView a lot lighter on your phone, so the whole browser should feel faster.

#### Tab Switching

- Switching tabs is faster. Before, every switch made GeckoView re-save and clean up every tab running in the background. Now each background tab is only put to sleep once.
- The tab you're looking at now gets top priority, so Android is less likely to kill it when your phone is low on memory.
- Clint keeps fewer tabs active in the background on phones with less RAM. It keeps 1 on low-RAM phones, 2 under 3 GB, 4 under 5 GB, and 6 on everything else.
- Rotating the screen, folding or unfolding your phone, resizing the window, and plugging in a keyboard no longer reload all your tabs.

#### Startup and Page Loads

- Clint now loads its built-in scripts once and keeps them in memory. Before, it read them from the app file every time you opened a tab, loaded a page, or came back to the app.
- Some of the work GeckoView needs is now done in the background while it starts up, so the first page loads sooner.
- Clint makes fewer system checks on every page you open.

#### Memory and Battery

- On Android 14 and newer, GeckoView's image cache is cleared when you leave the app. Android stopped sending the low memory signal Clint used for this.
- Tab thumbnails are now resized in the background instead of slowing down the screen.
- Fixed a memory leak where a closed browser window could stay in memory.

---

### Media Capture

#### Better Detection

Media Capture now finds streams on a lot more sites.

- It catches playlists served without a file extension or with an unusual file type.
- It recognizes DASH manifests by their content.
- It no longer misses media that loads before a tab is fully set up, or media loaded by a service worker.
- It skips links it already listed.
- It keeps your results when a page only changes the part after `#` in its address.

(Thanks to @Ahmedal2022 for the patch that started this [#36](https://github.com/jhaiian/ClintGecko/issues/36))

#### YouTube

YouTube protects its videos in a way Media Capture can't save.

- If YTDLnis is installed, opening Media Capture on YouTube sends the page straight to YTDLnis.
- If it isn't, Media Capture explains why YouTube can't be captured and shows a **Get YTDLnis** button that opens its latest release.

---

### Incognito

- New **Incognito search engine** and **Incognito search suggestions** settings. Incognito can now use a different search engine and suggestions provider than normal browsing, and each one can have its own custom entry. Both start as **Same as normal**, so nothing changes until you pick something. Searches, suggestions, the search hint, and the search engine homepage all follow the mode of the tab you're in.
- A silent notification now shows while Incognito tabs are open. Tap it to close all of them at once. It works right from the notification, so you stay in whatever app you're using.
- New **Remember browsing mode** option. If you were last using Incognito, Clint opens in a fresh Incognito tab on the homepage. Your Incognito tabs are still never saved. It's off by default.

(Thanks to @anonymous-shanks for suggesting the notification and Remember browsing mode [#32](https://github.com/jhaiian/ClintGecko/issues/32))

---

### Accessibility

New **Accessibility** page in Settings.

- **Automatic font sizing** keeps website text in sync with your Android font size.
- Turn it off to pick your own **Font size** for websites, anywhere from 50% to 200%, with a live preview.
- **Zoom on all websites** lets you pinch to zoom even on sites that try to block it.

(Thanks to @fajarrasyid and @xmiseggs for suggesting this [#24](https://github.com/jhaiian/ClintGecko/issues/24) [#35](https://github.com/jhaiian/ClintGecko/issues/35))

---

### Unverified Extensions

You can now install extensions that Mozilla hasn't signed. Before, these just failed with an error. Now Clint warns you first and installs only if you tap **Install anyway**.

- These extensions don't update on their own.
- Clint loads them again every time it starts.
- Removing one also deletes the file Clint kept for it.

Only install ones you trust.

---

## Improvements

- **Delete browsing data** now actually follows the time range you pick. Before, cookies, site data, cache, site settings, and tabs were always deleted for all time. Now only cookies from that range, data from sites you visited in that range, site settings you changed in that range, and tabs you used in that range are removed.
- **Tabs start at bottom** now really reverses the tab order, with your first tab at the bottom and newer tabs above it. Groups and drag and drop follow the new order too. An incomplete row now sits at the top of the grid instead of the bottom.
- Long press the address bar to **Copy** the link, **Paste**, or **Paste & Go**.
- The toolbars now hide and show when you scroll, even while a page is still loading.
- New **Full Brightness Image** option in Homepage settings when you use a custom image. It shows your photo as it is, without the dark overlay.
(Thanks to @sankhababu21 for suggesting this [#19](https://github.com/jhaiian/ClintGecko/issues/19))
- Long press the tab counter to open a new tab.
(Thanks to @akshaymaurya26 for suggesting this [#27](https://github.com/jhaiian/ClintGecko/issues/27))

---

## Other Changes

- Updated Privacy Policy and Terms of Service.

I added MEGA downloads, the YTDLnis handoff, unverified extensions, the new Incognito settings, address bar paste, and the download history switches to the Privacy Policy. I also fixed the Delete browsing data part, since it now follows your time range. The Terms of Service now has a MEGA section and covers unverified extensions and YTDLnis. As always, nothing leaves your device except what each feature needs to work.

- Dropped the 32-bit x86 build. GeckoView stopped making x86 builds after version 144, so that APK couldn't run anyway. ARM phones aren't affected.
- The store listings now have screenshots with translated captions for every language, plus a new banner.

---

## Dependency Updates

- Bumped GeckoView from `158.0.20261021001945` to `159.0.20261022150525`.
- Bumped Android Gradle Plugin from `9.4.1` to `9.5.0`.
- Bumped OkHttp from `5.5.0` to `5.5.1`.

---

## Bug Fixes

- Fixed home screen shortcuts restarting the whole browser every time you tapped them, which could crash the app. Shortcuts you already added are updated automatically.
- Fixed the app freezing for several seconds and then showing a black screen when switching between the browser and a frameless shortcut.
- Fixed your open tabs sometimes being restored from an older save after a restart.
- Fixed pages going blank after switching your phone between light and dark mode.
- Fixed the number in the tab counter sitting low and off-center, especially with a larger system font size. With big fonts the counter box now grows to fit the number instead of cutting it off, and the custom theme preview matches.
- Fixed old tab thumbnails sometimes sticking around after a tab was closed.

---

## Donation

Thank you to an anonymous supporter for donating $20.44 through PayPal!

Thank you to **Sharashka USA** for donating $5 through Ko-fi!

Thank you to **Gianluca Acuña** for becoming a $1 member on Patreon!

Every bit of support helps me keep working on Clint. Thank you all so much!

---

# v1.0.4

## What's New

### A Rebuilt Download Engine

Most of this update is under the hood. The download engine got a full review and a lot of fixes, so downloads should be faster, lighter, and much more reliable.

#### Auto Mode

The **Split download parts**, **Max simultaneous parts**, and **Concurrent segments** sliders now have an **Auto** option. Slide all the way to the right to use it.

With Auto, Clint picks the number of connections and adjusts it while downloading. It only adds a connection if that actually makes the download faster. It uses fewer connections on mobile data, in battery saver, or when your phone is running hot. It also remembers what worked best for each server, so the next download from that server starts at the right number.

Auto is the new default. If you already changed these sliders, your settings stay the same.

#### More Reliable

- Retries now wait longer each time instead of retrying every 5 seconds forever. The default is 10 retries, and you can still set 0 for unlimited.
- A failed attempt keeps the progress it already made instead of starting over.
- If copying a finished download to your chosen folder fails, only the copy is retried. Before, the file was deleted and downloaded again, forever.
- Downloads can continue in the background even when Android won't let Clint start a foreground service.
- Downloads paused because of no network, Wi-Fi only, or a schedule now resume correctly after a restart.
- Unfinished files now end in `.part` and get their real name once they're done.
- Leftover temporary files are cleaned up.

#### Faster and Lighter

- Downloads to a folder you chose on your device or SD card are now written straight there, instead of being downloaded first and then copied at 100%.
- Stream downloads use a lot less storage. A 4 GB video with separate audio used to need about 12 GB written. Now it's about 8 GB.
- Much fewer disk writes, which means less work for your phone during fast downloads.
- Downloads no longer compete with the rest of the app for resources.
- Fewer notification updates, so notifications don't get stuck or skipped.

#### Streams and Live Recordings

- Pausing, retrying, or restarting the app no longer deletes a live recording. It picks up where it left off.
- A live recording that keeps failing now saves what it already has instead of failing completely.
- Fixed many DASH streams that didn't download at all.
- Streams with unsupported encryption now show a clear message right away, instead of failing only after everything was downloaded.

---

### Page Translation

You can now translate pages right inside Clint! It uses Firefox's own translation engine, so everything happens **on your device**. The page text never leaves your phone. The only thing downloaded is the language files, and they come from Mozilla.

- A translate bar slides in under the address bar. Pick the source and target languages, then tap **Translate**. Tap **Show original** to switch back.
- The bar shows up when Clint detects a page in another language, from the new **Translate page** item in the menu, or from the translate button in the address bar.
- The bar's menu has **Always translate**, **Never translate** for a language, and **Never translate this site**.
- The bar hides by itself after a few seconds. You can also swipe it up or close it.
- Clint remembers the last language you translated to.
- New **Translations** page in Settings › Browser › Tools. From there you can turn the offer and the address bar button on or off, set Offer / Always / Never for each language, manage sites that are never translated, and download or delete language files.

---

### Enhanced Tracking Protection

New **Enhanced Tracking Protection** setting in Settings › Privacy & Security, with three levels:

- **Standard** (default): Blocks cryptominers and social media tracking cookies. In private tabs it also blocks tracking content and email trackers.
- **Strict**: Everything in Standard, plus tracking content and social media trackers in **every** tab. Some sites might break.
- **Off**: No tracker blocking.

If a site breaks, you can turn protection off for that site from the site info sheet.

---

### Cookie Protection

The **Block third-party cookies** switch is now a **Cookie protection** setting with three options:

- **Allow all cookies**
- **Total Cookie Protection** (default): Every site gets its own separate cookie jar, so trackers can't follow you from site to site.
- **Block third-party cookies**

If you had turned off third-party cookie blocking before, you'll be on **Allow all cookies**. If you had changed it and then turned it back on, you'll be on **Block third-party cookies**. Everyone else gets Total Cookie Protection.

---

### Site Information

Tap the lock icon in the address bar to see information about the site you're on, just like Chrome and Firefox.

- Whether the connection is secure, plus the site's certificate details (who it was issued to, who issued it, and how long it's valid).
- A warning if you chose to continue past a certificate error.
- How many trackers were blocked on the page. Tap the number to see them grouped by type: social media trackers, cross-site cookies, tracking content, fingerprinters, and cryptominers.
- Every permission you've saved for the site (camera, microphone, location, notifications, clipboard). Tap one to change it, or reset them all at once.
- Shortcuts to Cookies Control and Site settings.

---

### New Look for Settings

All the settings screens have been redesigned.

- A **Search settings** bar at the top of the main settings screen.
- **Support Clint** now has its own card at the top.
- Categories are grouped into **General**, **Protection**, **Downloads & data**, and **Advanced**, with **About** at the end.
- The Browser, Look and feel, and Downloads screens have been reorganized so related settings sit together. The big "Layout" card in Look and feel has been split up, and **Language** is now at the top.
- Updated descriptions on the main settings screen so they match what each screen actually has now.
- On tablets and in landscape, the selected category is highlighted.

---

### Delete Browsing Data

A new **Delete browsing data** screen, similar to Chrome's. Open it from History › ⋮, or from Settings › Privacy & Security › History.

- Pick a time range: Last hour, Last 24 hours, Last 7 days, Last 4 weeks, or All time.
- Choose what to delete: browsing history, cookies and site data, cached images and files, open tabs, and site settings. Each one shows a summary of what it will clear.
- Closed tabs can be restored with **Undo**.
- Your choices are remembered for next time.

---

### New Languages

Clint is now available in:

- **Hindi** (हिन्दी)
- **Indonesian** (Bahasa Indonesia)
- **Brazilian Portuguese** (Português (Brasil))

---

### A Better Error Page

When a page fails to load, you now get a proper error page that matches your theme, including custom themes.

- Each error gets its own message: no internet, site not found, connection refused, timed out, certificate problems, dangerous sites, and many more. Network errors also list a few things to try.
- **Try again** and **Go back** buttons, and a **Details** section with the error code and full URL.
- **Try again** works even when JavaScript is turned off.

---

### New Look for History

- History is grouped into **Today**, **Yesterday**, **Earlier this week**, **Earlier this month**, and **Older**, or by letter when sorted by title.
- Each entry shows the site and the time you visited.
- Tap ✕ to remove an entry, with **Undo** if you change your mind.
- A cleaner selection mode with a **Delete** button.

---

### New Look for Search Suggestions

- Suggestions are grouped into **Bookmarks**, **History**, and **Search suggestions** cards.
- The part of a suggestion that matches what you typed is highlighted.
- URLs are easier to read, without `https://www.` in front.
- To remove a history entry, long-press it.
- A back button in the search field, and the keyboard hides when you scroll the suggestions.
- New switches in Settings › Browser › Search: **Show history in suggestions** and **Show bookmarks in suggestions**.

---

### Website Isolation

New **Website isolation** switch in Settings › Privacy & Security. This is the same feature as Firefox's "Website isolation" in Labs. It runs each site in its own process for better security. It's off by default, and Clint needs to restart after you change it.

---

### Long Shot

No internet? Play a game! Long Shot is a small archery game, like Chrome's dino game.

- It shows up on the offline page, or you can play it anytime by typing `clint://longshot` in the address bar. Typing `clint:` also suggests it.
- Pull back and let go to shoot. Three misses and it's game over.
- After each hit the target moves and gets smaller, and the wind gets stronger. Hit enough and the target starts swinging.
- Hit bullseyes in a row for up to a ×3 multiplier.
- Your best score is saved.

> I don't know how fun it is lol, but give it a try when your internet goes down.

---

## Improvements

- The browser menu now opens instantly. On some phones (like Xiaomi/HyperOS), it used to take a moment to open, and tapping again could close it.
- Pull to refresh is harder to trigger by accident. It no longer starts when you scroll up to the top of a page, and it's blocked on Instagram Reels, TikTok, Facebook Reels, and YouTube Shorts.
- New **Tab groups** switch in Settings › Look and feel. Turn it off if you don't use tab groups. Your tabs stay open.
- New **Buttons at bottom** option in the tab menu's ⋮ menu. It moves the tab count, search, New Tab buttons, and group chips to the bottom so they're easier to reach with one hand.
- New **Open tabs** option in Settings › Privacy & Security › Clear data on exit › Data to clear. It's off by default.
- Pop-ups from the same site now open without asking. Pop-ups to other sites still ask.
- Location requests from websites now have the **Don't ask me again** option, like camera, microphone, and notifications. You also no longer get two dialogs in a row.
- You can now drag the text cursor past the edge of a text field. That means you can reach the end of a long URL in the address bar.
- The segment map now shows one cell per segment again, and the expanded grid scrolls when a stream has a lot of segments.

---

## Other Changes

- Updated Privacy Policy and Terms of Service.

Translation downloads its language files from Mozilla, so I've added it to the Privacy Policy, along with tracking protection, cookie protection, site information, delete browsing data, and the new open tabs option for clear data on exit. I also added a note to the Terms of Service that machine translations can be wrong. As always, everything stays on your device except what's needed to download those files.

---

## Dependency Updates

- Bumped GeckoView from `158.0.20261007201254` to `158.0.20261021001945`.
- Bumped Gradle from `9.8.1` to `9.8.2`.

---

## Bug Fixes

- Fixed the Downloads screen being empty after the app was killed, and new downloads not being saved in that state.
- Fixed pausing and quickly resuming a download sometimes starting it twice.
- Fixed changes to a download's link, cookies, or settings getting reverted while it was downloading.
- Fixed some servers making a download restart over and over.
- Fixed slow or speed-limited downloads pausing and resuming every 30 seconds.
- Fixed being rate limited throwing away most of a download's progress.
- Fixed downloads failing with "Server error 416" when resuming.
- Fixed some HLS streams downloading the whole video for every segment.
- Fixed text fields at the bottom of a page (like Google AI Mode's "Ask anything" box) being covered by the keyboard.
- Fixed the page jumping and a black band showing up when opening or closing the keyboard.
- Fixed fullscreen not working on some embedded video players (like the Dailymotion trailers on FilmAffinity). The screen would flicker and drop out of fullscreen right away.
- Fixed the buttons on the error page not working.
- Fixed a failed download's notification getting stuck when push notifications are off.
- Fixed cancelling a stream conversion leaving an empty file in Downloads.

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

- Fixed Upload and Download not working on extensions.

(Thanks to @phanimitravadlamani for reporting this [#15](https://github.com/jhaiian/ClintGecko/issues/15))

- Fixed Upload not working on certain devices.

(Thanks to @TheWeaver786 for reporting this [#13](https://github.com/jhaiian/ClintGecko/issues/13))

- Fixed system navigation covering the pages.

(Thanks to @deniganda for reporting this [#87](https://github.com/jhaiian/ClintBrowser/issues/87))

---

# v1.0.1

## What's New

### Custom Theme

Added a custom theme editor that allows users to individually customize all colors used throughout the app. Include a real-time preview that updates instantly as colors are changed, so users can see the results before applying the theme. Allow users to save, rename, edit, apply, and reuse their custom themes.

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

- Added ability to hide quick actions on the homepage.

- Added ability to add a custom URL for the homepage.

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
