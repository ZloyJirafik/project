# MyBody – Calorie Calculator (Android, Kotlin)

A native Android app for counting calories, written in **Kotlin** with
**Jetpack Compose**. It calculates your personal daily calorie target, lets you
log food from a built-in food database, search millions of packaged products
online, and scan product barcodes with the phone camera.

The interface is in **Russian** and uses the **Inter** font.

## Download

Ready-to-install APK for Android 8.0 or newer:
[`apk/MyBody-1.0.apk`](apk/MyBody-1.0.apk), also on the
[Releases page](https://github.com/ZloyJirafik/project/releases).
Copy it to the phone, open it and allow installing apps from this source.

Screenshots and presentation notes are in [`presentation/`](presentation/).

> The app was first called **CaloScan**. The code still uses that name (package
> `com.example.caloscan`, classes like `CaloScanViewModel`, the `CaloScanAndroid`
> folder), so phones that already have it installed update it and keep their data.
> Backups made under the old name can still be imported.

## Features

| Screen | What it does |
|---|---|
| **Дневник** (Diary) | Calories left today (progress ring), protein / carbs / fat bars, four meals. Browse other days with the arrows. |
| **Продукты** (Foods) | Search 149 common foods in 10 categories, your recent foods, your own foods, and Open Food Facts online. The scan button in the search box opens the scanner. |
| **Сканер** (Scan) | Scan a barcode or QR code with the camera (CameraX + ML Kit) or from a photo, or type the number. Opens the product card: photo, details, **Добавить** (adds it to «Мои продукты») and **Закрыть**. |
| **Прогресс** (Progress) | Calories chart for the last 7 / 30 days, daily average, days on target, streak, macro balance. |
| **Профиль** (Profile) | Calorie calculator (BMR → TDEE → goal), macro split, your own foods, backup export / import. |

Other details:
- Amounts in grams/ml or in servings ("1 шт.", "1 ломтик").
- Create your own foods, with values per 100 g or per serving (converted automatically).
- Build your own library in the store: scan a product, check its card and tap **Добавить**.
  It's saved to «Мои продукты» with its photo, and the message that follows has a
  **В дневник** button to log it right away.
- Unknown barcode? Add the product yourself and it's found instantly next time.
- Scanned products are saved on the phone, so they also work offline.
- Undo after removing a diary entry.
- Light and dark theme follow the phone's setting.
- All data stays on the phone (Room database). Only barcode and search
  requests go to Open Food Facts.

## Opening the project in Android Studio

You need **Android Studio Otter 3 Feature Drop (2025.2.3) or newer**. Any 2026
version works.

1. Start Android Studio → **File → Open** → select the `CaloScanAndroid` folder → **OK**.
2. Wait for **Gradle sync** to finish (progress bar at the bottom). The first time
   it downloads Gradle, the libraries and a JDK 17, which takes a few minutes.
   If Android Studio asks to install a missing SDK platform, click the link to install it.
3. Create a virtual phone: **Device Manager** (phone icon on the right) → **+** →
   **Create Virtual Device** → pick e.g. *Pixel 8* → choose a system image
   (Android 14 or newer) → **Finish**.
4. Press the green **Run ▶** button. The app opens in the emulator.

**On a real Android phone (Android 8.0 or newer):** turn on *Developer options*
(tap *Build number* 7 times in Settings → About phone), enable *USB debugging*,
connect the phone with a cable, pick it in the device list and press **Run ▶**.

**APK file:** **Build → Generate App Bundles or APKs → Generate APKs** creates
`app/build/outputs/apk/debug/app-debug.apk`, which you can copy to any Android phone.

> This is an Android app, so it doesn't run on an iPhone. Use the emulator in
> Android Studio or an Android phone. The web version in the folder above this one
> still works on iPhone.

### Trying the barcode scanner in the emulator

The emulator's camera shows a virtual room instead of a real camera image, so:
- **Ввести штрихкод** (type the barcode) on the Scan screen, or tap the Nutella example (`3017620422003`).
- **Scan from a photo:** drag a picture of a barcode onto the emulator window
  (it's saved to Downloads), then tap **Сканировать с фото** and pick it.
- On a real phone, the live camera just works: tap **Включить камеру** and allow camera access.

**Barcodes to try:** `3017620422003` (Nutella), `5449000000996` (Coca-Cola),
`2000000012348` (not in the database: shows «Товар не найден» → «Добавить товар»).

## Project structure

```
CaloScanAndroid/
├── build.gradle.kts, settings.gradle.kts   Gradle build setup
├── gradle/libs.versions.toml               All library versions in one place
└── app/
    ├── build.gradle.kts                    App settings and dependencies
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml         Permissions (camera, internet)
        │   ├── res/                        App icon, line icons, colors
        │   ├── res/font/                   Inter font (4 weights)
        │   └── kotlin/com/example/caloscan/
        │       ├── MainActivity.kt         Starts the app
        │       ├── CaloScanApplication.kt  Creates the database and repository
        │       ├── data/
        │       │   ├── model/Food.kt       Food, DiaryEntry, Meal, Nutrition
        │       │   ├── BuiltInFoods.kt     The 149-food database (Russian names)
        │       │   ├── FoodRepository.kt   Single source of data for the app
        │       │   ├── local/              Room database: tables, queries, backup
        │       │   └── remote/             Open Food Facts API + JSON parser
        │       ├── domain/                 Logic without Android code (unit tested)
        │       │   ├── CalorieCalculator.kt  BMR, TDEE, goal, macros, BMI
        │       │   ├── FoodSearch.kt         Search ranking
        │       │   ├── Portions.kt           Servings ↔ grams, +/− buttons
        │       │   ├── CustomFoodValidator.kt  Checks the "New food" form
        │       │   ├── ProductCode.kt          Finds the barcode number in a QR code
        │       │   └── ProgressCalculator.kt   Averages, streak, macro share
        │       ├── scan/BarcodeReader.kt   ML Kit barcode + QR reading (camera + photo)
        │       ├── ui/
        │       │   ├── CaloScanApp.kt      Top bar, bottom tabs, messages
        │       │   ├── CaloScanViewModel.kt  App state and actions
        │       │   ├── diary/ foods/ scan/ progress/ profile/   One folder per screen
        │       │   ├── sheets/             Bottom sheets (product card, add food, new food, lookup)
        │       │   ├── components/         Shared UI pieces (cards, ring, bars...)
        │       │   ├── theme/              Colors for light and dark mode
        │       │   └── preview/            Example data for the design previews
        │       └── util/Format.kt          Russian number/date formatting, plurals
        └── test/                           Unit tests (46 tests)
licenses/Inter-OFL.txt                      Font license
```

## How it works

### Architecture (MVVM)

```
Compose screens  →  CaloScanViewModel  →  FoodRepository  →  Room database (on the phone)
  (show state,        (state as Flows,        (one API for     →  BuiltInFoods (in the code)
   send actions)       handles actions)        all data)       →  Open Food Facts (internet)
```

- **Screens** are `@Composable` functions. Each has a `...Route` that reads state from
  the ViewModel, and a `...Screen` that only draws what it's given. That split makes
  the screens easy to preview in Android Studio's **Split / Design** view.
- **The ViewModel** exposes `StateFlow`s. Room queries return `Flow`s, so the screens
  update by themselves when data changes.
- **Room tables:** `foods` (your foods + scanned products), `diary_entries`,
  `recent_foods`, `profile`. Each diary entry keeps a copy of the food, so old days
  never change when a food is edited.

### Daily calorie target (`domain/CalorieCalculator.kt`)

1. **BMR** (calories burned at rest), Mifflin-St Jeor equation:
   - men: `10 × weight(kg) + 6.25 × height(cm) − 5 × age + 5`
   - women: `10 × weight(kg) + 6.25 × height(cm) − 5 × age − 161`
2. **TDEE** = BMR × activity factor (1.2 sedentary, 1.375, 1.55, 1.725, 1.9 extra active).
3. **Goal**: 1 kg of body fat ≈ 7,700 kcal, so −500 kcal/day ≈ −0.5 kg/week.
   The result never goes below 1,200 kcal (women) / 1,500 kcal (men).
4. **Macros**: the target is split by percentage (e.g. 20% protein, 50% carbs,
   30% fat); protein and carbs have 4 kcal per gram, fat 9 kcal per gram.

Example: man, 21 years, 180 cm, 75 kg, moderately active, losing 0.5 kg/week →
BMR 1,775 → TDEE 2,751 → target 2,250 kcal.

Calories of a food entry = `kcal per 100 g × amount in grams ÷ 100`.

### Russian language and the Inter font

- All texts are Russian strings in the Kotlin code. Numbers use Russian style
  (`1 234`, `0,4`), dates look like «Вс, 1 нояб.», and `plural()` in `util/Format.kt`
  picks the right word form («1 день», «3 дня», «5 дней»).
- Built-in foods have Russian names. Each one keeps its English name as its id
  (`b-banana`) and as an extra search word, so saved diary entries keep working and
  "banana" also finds «Банан». Search ignores case and «ё»/«е».
- Products from Open Food Facts use the Russian name when the product has one;
  units like "15 g" are shown as «15 г».
- The Inter font files are in `res/font/`, and `ui/theme/Theme.kt` sets Inter for every
  Material text style. Inter includes Cyrillic letters.

### Barcode scanning (`ui/scan/LiveBarcodeCamera.kt`, `scan/BarcodeReader.kt`)

1. **CameraX** shows the camera preview and sends frames (1280×720) to an analyzer.
2. **Google ML Kit** finds EAN-13, EAN-8, UPC-A and UPC-E barcodes, QR codes and
   DataMatrix codes in each frame. The model is bundled in the app, so it works
   offline. A code must be read in two frames in a row before it's used.
3. A QR or DataMatrix code is only used when the product's barcode number is in it
   (`domain/ProductCode.kt`), and its check digit must be right:
   - a GS1 Digital Link address: `https://id.gs1.org/01/03017620422003`
   - GS1 data, like the «Честный ЗНАК» codes: `0104601234567893215Abc…` (`01` + 14 digits)
   - the number alone

   A QR code without one (for example a link to the brand's website) shows
   «В этом QR-коде нет номера товара».
4. "Scan from a photo" opens the system photo picker and runs ML Kit on the picture.
5. The camera permission is asked the first time. If it was refused twice, the app
   offers a button that opens the app's settings.

### Product lookup and the product card (`ui/CaloScanViewModel.kt` → `lookupBarcode`)

Order: your own foods → products scanned before (saved on the phone) → Open Food Facts:

```
GET https://world.openfoodfacts.org/api/v2/product/{barcode}.json
```

A found product opens the **product card** (`ProductCardContent` in `ui/sheets/Sheets.kt`):
the photo (the 400 px version from Open Food Facts), name, brand, package size,
nutrition per 100 g, serving, barcode and source, then **Закрыть** and **Добавить**.

- **Добавить** saves a copy to «Мои продукты» (`FoodRepository.addToLibrary`), photo
  included. Next time the same barcode finds your copy first, and you can edit it.
  The message after it has a **В дневник** button.
- If Open Food Facts has no nutrition facts for the product, **Добавить** opens the
  "New product" form with the name, brand, photo and barcode filled in; you copy the
  numbers from the label.
- If the product is already in «Мои продукты», the card says so, and its main
  button is **В дневник**.
- Not found anywhere: «Товар не найден» → **Добавить товар** opens the same form.

Name search uses `https://world.openfoodfacts.org/cgi/search.pl`. That server is
sometimes busy (HTTP 503), so the app tries up to 3 times.

## Running the tests

The unit tests check the calorie formulas, food search, portions, the custom food
form, the Open Food Facts parser, backups, the progress statistics, reading barcode
numbers from QR codes, and the Russian number, date and plural formatting. They run
on the computer, no phone needed.

- In Android Studio: right-click the `app/src/test/kotlin` folder → **Run 'Tests in …'**.
- Or in the terminal (Windows): `gradlew.bat test`

## Libraries

| Library | Version | Used for |
|---|---|---|
| Android Gradle Plugin / Gradle | 9.0.1 / 9.1.0 | Building the app |
| Kotlin | 2.3.20 | Programming language |
| Jetpack Compose (BOM) + Material 3 | 2026.03.01 | User interface |
| Lifecycle / ViewModel | 2.10.0 | App state |
| Room | 2.8.4 | Database on the phone |
| CameraX | 1.5.3 | Camera preview |
| ML Kit Barcode Scanning | 17.3.0 | Reading barcodes |
| Coil | 3.4.0 | Product photos |
| Inter font | 4.1 | Text (bundled in `res/font`) |

minSdk 26 (Android 8.0), targetSdk / compileSdk 36. Android Studio may offer newer
versions; the project works without upgrading.

## Ideas for extending the project

- Water and exercise tracking (exercise adds to the daily budget)
- Weight log with a chart
- Favourite meals (save several foods as one item)
- Micronutrients (fiber, sugar, salt — already available from Open Food Facts)
- Accounts and cloud sync (e.g. Firebase)
- A home-screen widget showing calories left

## Credits

- Food values in the built-in database: rounded typical values based on
  [USDA FoodData Central](https://fdc.nal.usda.gov/).
- Product data: [Open Food Facts](https://world.openfoodfacts.org), available under
  the Open Database License (ODbL).
- Barcode reading: [Google ML Kit](https://developers.google.com/ml-kit/vision/barcode-scanning).
- Font: [Inter](https://rsms.me/inter/) by Rasmus Andersson, SIL Open Font License 1.1
  (see `licenses/Inter-OFL.txt`).

Calorie targets are estimates for healthy adults and are not medical advice.
