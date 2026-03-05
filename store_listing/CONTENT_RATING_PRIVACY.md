# Folio — Content Rating & Privacy Questionnaire

## Google Play Content Rating (IARC)

### Violence
- No violence of any kind → **None**

### Sexual Content
- No sexual content → **None**

### Language
- No profanity → **None**

### Controlled Substances
- No references → **None**

### User Generated Content
- Users work with their own documents → **No UGC shared**

### Interactive Elements
- ✅ In-app purchases (Remove Ads $4.99)
- ✅ Ads (AdMob banners + interstitials)
- ❌ No user interaction / social features
- ❌ No location sharing
- ❌ No data sharing

### Expected Rating: **Everyone**

---

## Data Safety Section (Google Play)

### Data Collected
| Data Type | Collected? | Shared? | Purpose |
|-----------|-----------|---------|---------|
| Device identifiers | Yes (by AdMob) | Yes (AdMob) | Advertising |
| Purchase history | Yes (IAP) | No | App functionality |
| App interactions | No | No | — |
| Files / Documents | No | No | — |
| Location | No | No | — |
| Personal info | No | No | — |
| Contacts | No | No | — |

### Data Handling
- **Encryption in transit**: Yes (AdMob uses HTTPS)
- **Data deletion**: Users can clear app data from device Settings
- **Data retention**: No user data is retained on servers
- All document processing is 100% on-device

### Security Practices
- All file operations use `context.cacheDir` for temp storage
- Temp files deleted after every operation via `OperationCleanup`
- `FileProvider` used for all file sharing (Android security model)
- No network calls except AdMob ad loading and IAP verification
- No analytics SDKs, no crash reporting, no telemetry

---

## Privacy Policy Summary

The app includes a built-in Privacy Policy screen accessible from Settings.
Key points:
1. 100% on-device processing — no files uploaded
2. No cloud storage or servers
3. No personal data collection
4. Local storage only (Room DB for history metadata, DataStore for prefs)
5. Temp files auto-deleted after operations
6. AdMob collects device identifiers for ad serving (disclosed)
7. IAP purchase info handled by Google Play (not by the app)
8. Storage permissions requested only for file access

---

## Permissions Justification (for Play Store review)

| Permission | Reason |
|-----------|--------|
| `READ_MEDIA_IMAGES` (API 33+) | Required to select images for Image→PDF conversion |
| `READ_MEDIA_VIDEO` (API 33+) | Required for document file access via system picker |
| `READ_MEDIA_AUDIO` (API 33+) | Required for comprehensive file access |
| `READ_EXTERNAL_STORAGE` (API 26-32) | Required to read PDF/document files for all operations |
| `WRITE_EXTERNAL_STORAGE` (API 26-29) | Required to save processed files to Documents folder |
| `INTERNET` | Required for AdMob ad loading and IAP verification only |
| `ACCESS_NETWORK_STATE` | Required by AdMob SDK to check connectivity |
| `BILLING` | Required for Google Play in-app purchases |

No sensitive permissions (camera, microphone, contacts, location) are used.
