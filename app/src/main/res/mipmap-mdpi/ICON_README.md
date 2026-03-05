# Folio App Icon — Density Buckets

For pre-API-26 devices, you'll need PNG icons at these sizes:

| Density  | Folder           | Size     |
|----------|------------------|----------|
| mdpi     | mipmap-mdpi      | 48×48    |
| hdpi     | mipmap-hdpi      | 72×72    |
| xhdpi    | mipmap-xhdpi     | 96×96    |
| xxhdpi   | mipmap-xxhdpi    | 144×144  |
| xxxhdpi  | mipmap-xxxhdpi   | 192×192  |

## Play Store Icon
- **512×512 PNG** — no transparency, no rounded corners
- Play Store automatically rounds corners

## Design
The adaptive icon uses:
- **Background**: Warm off-white (#FFF8F5)
- **Foreground**: Document shape with "F" letter in Folio MergeAccent (#5B6ABF)

## How to Generate
1. Open Android Studio → Right-click `res` → New → Image Asset
2. Select the foreground/background vectors already created
3. This auto-generates all density PNGs

The XML adaptive icons in `mipmap-anydpi-v26/` are already set up
for API 26+ devices (Android 8.0+). Since minSdk=26, the PNGs
are only needed as fallback for edge cases.
