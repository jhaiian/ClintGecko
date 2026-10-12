## Contributing

Contributions, bug reports, and feature requests are welcome.

1. [Open an issue](https://github.com/jhaiian/ClintGecko/issues) to report a bug or suggest a feature
2. Fork the repo and create a branch for your change
3. Submit a pull request with a clear description

To report a crash, use the built-in **Debug & Crash Reports** screen in Settings. It generates a pre-filled GitHub issue template with your device info and crash log.

### Translating the App

Want to help translate the app into your language? We’d love your help!

1. **Fork the project**  
   First, fork the project to your own GitHub account.

2. **Copy `strings.xml`**  
   Download or make a copy of this file:  
   `app/src/main/res/values/strings.xml`  
   You can edit it directly if you're using GitHub or another editor.  
   There are some placeholders and Unicode escape sequences in the file, so please be careful not to modify or remove them. Using an XML editor can help prevent formatting or syntax issues.

3. **Translate the strings**  
   Translate the text inside the `<string>` elements into your language.  
   Please keep the following unchanged:
   - String names/IDs
   - XML structure
   - Placeholders such as `%s`, `%1$d`, `{name}`, etc.
   - Unicode escape sequences such as `\uXXXX`
   - Comments and formatting, whenever possible

4. **Create your language folder**  
   Once you've finished or made enough progress, create a new folder inside:  
   `app/src/main/res/`  
   The folder should follow this format:  
   `values-XX`  
   Replace `XX` with your language code. For example:  
   - `values-fil` – Filipino  
   - `values-es` – Spanish  
   - `values-ja` – Japanese  
   - `values-fr` – French  
   - `values-de` – German  
   You can search Google for the appropriate ISO 639 language code for your language.

5. **Add your translated file**  
   Place your translated `strings.xml` inside your new language folder:  
   `app/src/main/res/values-XX/strings.xml`  
   For example, a Filipino translation would be:  
   `app/src/main/res/values-fil/strings.xml`

6. **Submit your contribution**  
   Commit your changes, push them to your fork, and create a Pull Request to the main project.

Thank you for helping make the app available in more languages!

---

## Building from Source

### Prerequisites
- Android Studio or JDK 17
- Android SDK (API 37)
- Gradle 9.8.0

### Steps

```bash
git clone https://github.com/jhaiian/ClintGecko.git
cd ClintGecko
```

Create a `local.properties` file in the root with your SDK path:

```properties
sdk.dir=/path/to/your/android/sdk
```

For a signed release build, also add:

```properties
signingConfig.storeFile=app/release_keystore.jks
signingConfig.storePassword=your_password
signingConfig.keyAlias=your_alias
signingConfig.keyPassword=your_password
```

Then build:

```bash
chmod +x gradlew
./gradlew assembleRelease
```

APKs will be output to `app/build/outputs/apk/release/`.

---

## CI/CD Secrets

In order to make the workflow work, you need the following secrets:

**Secret 1: `BASE_64_SIGNING_KEY`**

```bash
# Convert your keystore to base64
base64 -w 0 your_keystore.jks
# Copy the entire output as the secret value
```

**Secret 2: `LOCAL_PROPERTIES`**

```properties
signingConfig.storeFile=app/release_keystore.jks
signingConfig.storePassword=your_password
signingConfig.keyAlias=your_alias
signingConfig.keyPassword=your_password
```

To make your `release.yml` workflow work, set up the following **secrets** in your repository:

| Secret Name               | Purpose                                                        |
|----------------------------|----------------------------------------------------------------|
| `BASE_64_SIGNING_KEY`      | Encoded release keystore for signing APKs.                    |
| `LOCAL_PROPERTIES`         | Contents of your `local.properties` for SDK path and signing configs. |
| `GIT_USERNAME`             | Your GitHub username for automated commits.                   |
| `GIT_EMAIL`                | Your GitHub email for automated commits.                      |
| `PERSONAL_GITHUB_TOKEN`    | GitHub Personal Access Token (PAT) for pushing commits/tags.  |

---

## How to create a Personal Access Token (PAT)

Your workflow needs a GitHub token to push commits and tags. Follow these steps:

1. Go to **GitHub Settings → Developer settings → Personal Access Tokens → Tokens (classic)**.
2. Click **Generate new token → Generate new token (classic)**.
3. Give the token a name (e.g., `Clint Gecko CI`).
4. Set an expiration (recommended: 90 days or no expiration if you rotate it regularly).
5. Under **Scopes**, check:  
   - `repo` → Full control of private repositories
6. Click **Generate token**.
7. Copy the token immediately and add it as the secret `PERSONAL_GITHUB_TOKEN` in your repository.
