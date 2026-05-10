# ColorLens

Android-App, die Farben über die Handykamera erfasst und als Palette exportiert. **Portfolio-Demo** — gebaut, um Jetpack Compose und CameraX praktisch durchzuspielen, nicht als produktives Tool gedacht.

## Funktionen

- Live-Farb-Picker (zentrales Pixel im Kamerabild)
- HEX, RGB, HSL gleichzeitig
- Palette speichern, als JSON oder CSS-Variablen exportieren

## Stack

Kotlin · Jetpack Compose (BOM 2024.12, Material 3) · CameraX 1.4.1 · `minSdk` 26, `targetSdk` 35

## Build

```
./gradlew assembleDebug
```

Kein Release-APK — wer bauen will, hat den Source.

## Hinweis

Eine von mehreren kleinen Apps in diesem Repo-Bereich, die als Übungsstrecken für Android-Themen entstanden sind. Funktioniert, ist aber kein Versuch, einen weiteren Color-Picker zu lancieren.

## Lizenz

MIT.
