# Terms of Service for Clint Gecko

*Last updated: October 25, 2026*

## 1. What These Terms Cover

These Terms of Service ("Terms") describe the legal relationship between you and the developer of Clint Gecko ("we", "us", or "the developer"). By using Clint Gecko, you agree to these Terms.

If you do not agree, simply uninstall the app.

## 2. What Clint Gecko Is

Clint Gecko is a **free, open‑source Android browser** built around privacy, powered by Mozilla's GeckoView engine. Key facts:

- No backend servers, everything runs on your device.
- Install and manage browser extensions, including from Mozilla's Add-ons site.
- Keep audio and video playing in the background with media controls.
- Block entire categories of sites at the domain level with Website Blocker.
- Translate web pages on your device with the translation engine built into GeckoView.
- Set your own search engine and search suggestions provider, including fully custom ones you point at any URL.
- Incognito sessions are completely isolated.
- You can view, modify, or share the source code under the GPL‑v3 license.

Your browsing data belongs to you. The developer never sees it.

## 3. Open Source License (GPL‑v3)

The source code of Clint Gecko is licensed under the **GNU General Public License version 3**. You are free to:

- Use the app for any purpose.
- Study how it works.
- Modify the code.
- Share copies with anyone.
- Distribute your modified versions (as long as you also share your changes under GPL‑v3).

A copy of the license is available at [gnu.org/licenses/gpl-3.0.html](https://www.gnu.org/licenses/gpl-3.0.html) and inside the app's repository.

Third-party components, including GeckoView, keep their own licenses. They are listed in `Attribution.md` in the app's repository.

## 4. Trademarks and Branding (Not Open Source)

The name **"Clint Gecko"**, the word "Clint", the logo, icons, screenshots, and all related branding assets are **trademarks and proprietary assets** of the developer. They are **not** covered by the GPL.

You may **not** use, copy, modify, or redistribute these assets without explicit written permission. For example, you cannot create a modified version of Clint Gecko and still call it "Clint Gecko" or use the original logo. Contact `jhaiian@proton.me` for permission.

## 5. Privacy: No Data Collection

Clint Gecko does **not** collect any personal information, browsing history, usage statistics, crash reports, or telemetry of any kind. The app has no analytics, no tracking pixels, no backend servers. Telemetry built into the Gecko engine is switched off.

However, websites you visit may set cookies or collect data according to their own privacy policies, and extensions you install may collect data according to their own developers' policies. Those websites and extensions are not under our control. The Privacy Policy explains in detail what the app does and doesn't send.

## 6. Backups Are Your Responsibility

Clint Gecko lets you create backup files of your browsing data, optionally protected with a password you choose. Because we never see or store that password, we have no way to recover it if you forget it. If that happens, the backup cannot be unlocked by us or by anyone else. Please keep your password somewhere safe.

Backup files are saved wherever you choose to save them, using your device's own file picker. A backup can include things like cookies, history, and installed extensions with their data. Once a backup file exists, keeping it safe (backing it up further, not losing the storage device it's on, not sharing it carelessly) is up to you. We're not responsible for backup files that are lost, deleted, corrupted, or accessed by someone else after they leave the app.

Before you can back up or restore, the app will also ask you to unlock it with your fingerprint, face unlock, or your device's own PIN/pattern/password. This check happens entirely on your device through Android's own security system; we never see or receive it. It exists so that someone who picks up your unlocked phone can't export or overwrite your data without your say-so. If your device has no lock screen or biometrics set up at all, Backup & Restore won't be available until you set one up in your device settings, since there's simply nothing for Android to check against.

## 7. Custom Search Engines and Search Suggestions

Clint Gecko lets you configure your own custom search engine and your own custom search suggestions provider, each pointed at any URL you enter.

These features send your data (search queries or suggestion text) to whatever destination you configure, whether that's a built in provider or one you typed in yourself. The developer does not run, host, review, or vet any of these destinations. You are solely responsible for what you point them at.

## 8. Extensions

Clint Gecko lets you install browser extensions, from Mozilla's Add-ons site, from an `.xpi` link on a website, or from a file on your device. Extensions are **third-party software**. The developer does not write, host, review, test, vet, or endorse them, and does not control what they do or what they collect.

By installing an extension, you understand and agree that:

- Extensions can be given permission to read and change the pages you visit, see your tabs and browsing activity, and send information over the internet. Clint Gecko shows you the permissions an extension asks for before you approve it, but it can't guarantee that an extension uses them honestly or safely.
- You are solely responsible for deciding which extensions to install, which permissions to grant, and whether to allow an extension in incognito tabs.
- An extension's own developer is responsible for that extension, including its privacy practices, its terms, and any harm it causes. This includes extensions that are malicious, buggy, outdated, or that stop working.
- Extension support is provided by the Gecko engine. Clint Gecko does not support every extension or every extension feature, and an extension that works in Firefox may not work, or may work differently, in Clint Gecko. Mozilla does not provide support for Clint Gecko.
- Extensions can slow the browser down, use more battery or data, conflict with Clint Gecko's own features, or cause crashes. If something goes wrong, turn the extension off or remove it.
- Removing an extension deletes the data it stored in the app, unless you've included it in a backup you made.

## 9. Background Playback

Clint Gecko can keep audio and video playing when you leave the app or turn off the screen, and shows a media notification with controls while it does. You can turn this on or off in Settings.

Background playback relies on Android's foreground service and media session features, and on what each website allows. Some websites, devices, or battery settings may stop or limit playback, and the developer does not guarantee that playback will continue without interruption. Playback may use data, battery, and a notification slot on your device. You are responsible for the content you play and for making sure your use follows the terms of the websites you visit and any applicable laws.

## 10. Media Capture

Clint Gecko includes Media Capture, a feature that detects media resources used by websites, such as video, audio, subtitles, and streaming formats like HLS/M3U8 and DASH, and lets you download them directly. Where a website provides video and audio as separate streams, Media Capture can, where supported, combine them.

Media Capture works by inspecting webpage content and network requests, and by interacting with Android, the Gecko engine, and other components outside the developer's control. Websites may use their own tracking, DRM, authentication, or other technologies that affect how Media Capture behaves. Media Capture is provided **"as is"**, and the developer does not guarantee that it will detect every type of media, prevent websites from tracking you, or provide complete privacy or security while using the feature.

You are responsible for making sure your use of Media Capture, including any content you download with it, complies with the terms of the websites you visit and any applicable copyright or other laws.

## 11. Website Blocker

Website Blocker blocks categories of websites at the domain level, using lists the app downloads from third-party sources on GitHub, plus any sites you add yourself. These lists are maintained by others. The developer does not guarantee that they are complete, accurate, or up to date, that a site in a category really belongs there, or that any site you want blocked will be blocked. Website Blocker is not a replacement for parental controls, security software, or other protection.

## 12. Translations

Clint Gecko can translate web pages using the translation engine built into GeckoView. Translations are made by a machine and can be wrong, incomplete, or miss the meaning of the original. Don't rely on a translation for anything important, such as legal, medical, financial, or safety information. Check the original page or ask a qualified translator instead.

The translation engine and its language files are made by Mozilla. Not every language or page is supported, and the developer doesn't guarantee that translation will work on your device or that language files will always be available to download.

## 13. Privacy and Security Features

Clint Gecko includes features such as Enhanced Tracking Protection, Cookie protection, fingerprinting protection, Secure DNS, and Website isolation. They make tracking and attacks harder, but they can't stop all of them. Tracker lists are maintained by others, so they may miss some trackers or block things that aren't trackers. Stricter settings can make some websites stop working. If that happens, you can turn protection off for that site or change the setting.

Website isolation is a Gecko engine feature that is off by default. Turning it on may use more memory and may make the app less stable on some devices.

Delete browsing data and Clear data on exit permanently delete the data you choose. Make sure you've picked the right data before you use them.

## 14. Gecko Engine and Third-Party Components

Clint Gecko renders web pages with GeckoView, which is made by Mozilla and licensed under the Mozilla Public License 2.0, and relies on other third-party components. Clint Gecko is an independent project. It is not affiliated with, endorsed by, or sponsored by Mozilla. "Mozilla" and "Firefox" are trademarks of the Mozilla Foundation, and are used here only to describe the engine Clint Gecko is built on. The developer doesn't control how they work, and they may contain bugs or security flaws. Browser engines need frequent security updates. We aim to update GeckoView regularly, but we can't promise how quickly a fix will reach you, and you may be exposed to known issues until you update. Keep Clint Gecko up to date.

## 15. No Warranty

Clint Gecko is provided **"as is"** and **"as available"**. To the maximum extent permitted by law, the developer disclaims all warranties, express or implied, including:

- Merchantability
- Fitness for a particular purpose
- Non‑infringement
- Uninterrupted or error‑free operation

You use the app entirely at your own risk.

## 16. Limitation of Liability

In no event shall the developer be liable for any damages arising from your use of Clint Gecko, including but not limited to:

- Data loss or corruption
- Device damage
- Security breaches caused by websites you visit
- Harm caused by extensions you install
- Mistakes in machine translations
- Issues with third‑party services (such as search engines, search suggestions providers, or block list sources)
- Any indirect, incidental, or consequential damages

Some jurisdictions do not allow certain liability exclusions. In those cases, this limitation applies to the fullest extent permitted by law.

## 17. Third‑Party Content

Clint Gecko is a **browser**, it renders content from websites, search engines, extensions, and online services that are completely outside the developer's control. We do not:

- Endorse or guarantee the accuracy, legality, or safety of any website or extension you use
- Take responsibility for what you find on the internet

You are responsible for your own browsing choices.

## 18. Changes to These Terms

These Terms may be updated occasionally. Changes will be posted on the GitHub repository. The app will always link to the latest version. If you continue using Clint Gecko after an update, you accept the new Terms.

## 19. Governing Law

These Terms are governed by the laws of the Philippines. If you live elsewhere, your local consumer rights may also apply.

## 20. Contact

For questions or permission requests:

- Email: `jhaiian@proton.me`
- Discord: [discord.gg/4kUe4yPQ32](https://discord.gg/4kUe4yPQ32)
