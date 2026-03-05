# 📱 Folio — Enterprise PDF & Document Toolkit
### Full Android Development Prompt v2.0 | For VS Code Copilot (Claude Opus 4.6)

---

## 🎯 VISION & SOUL OF THIS APP

Build **Folio** — an enterprise-grade, offline-first document toolkit for Android. This is not a generic utility app. This is the app that makes people say *"I can't believe this is free."* Every screen, every animation, every interaction must feel considered, intentional, and buttery smooth. Think Notion meets Linear meets a Swiss design studio — but on Android.

**Core promise to the user:** Your documents never leave your phone. Everything is instant. Everything is beautiful.

---

## 🛠️ TECH STACK

| Layer | Technology | Reason |
|---|---|---|
| Language | Kotlin | Official, modern, concise |
| UI Framework | Jetpack Compose + Material 3 | Smooth, declarative UI |
| Architecture | MVVM + Clean Architecture (UseCase layer) | Enterprise-grade separation |
| PDF Engine | iText7 (primary) + Apache POI (Office conversions) | Industry standard |
| Office Conversion | Apache POI 5.x | .docx, .xlsx, .pptx read/write |
| Local Database | Room | History, recent files |
| Background Work | Kotlin Coroutines + Flow | Reactive, non-blocking |
| Ads | Google AdMob SDK | Monetization |
| IAP | Google Play Billing v6 | Remove Ads purchase |
| Image Loading | Coil 2 | Lightweight, Compose-native |
| Animations | Compose Animation APIs + Lottie | Meaningful motion |
| DI | Hilt | Scalable dependency injection |
| Min SDK | API 26 (Android 8.0) | Covers 95%+ of devices |
| Target SDK | API 35 (Android 15) | Latest features |

---

## 🎨 DESIGN SYSTEM — "Folio Pastel"

This is the most critical section. The design must feel human, warm, and premium — not AI-generated, not corporate, not default Material.

### Color Palette

Every color has a meaning. Every tool category has its own accent. Never use generic blue everywhere.

```kotlin
// Color.kt — Folio Design System

// Base surfaces — warm whites, never pure white
val Surface = Color(0xFFFAF9F7)           // Warm off-white, like paper
val SurfaceVariant = Color(0xFFF2F0EC)    // Slightly darker warm grey
val Background = Color(0xFFFAF9F7)

// Text
val OnSurface = Color(0xFF1C1B1A)         // Warm near-black (not pure #000)
val OnSurfaceVariant = Color(0xFF6B6860)  // Muted warm grey for secondary text

// Tool Category Accent Colors — pastel with meaning
val MergePastel = Color(0xFFD4E8FF)       // Soft sky blue  — "combining things"
val MergeAccent = Color(0xFF3B82C4)       // Deep sky — action color
val SplitPastel = Color(0xFFFFE4D4)       // Soft peach    — "dividing things"
val SplitAccent = Color(0xFFD4612A)

val CompressPastel = Color(0xFFD4F0E8)    // Soft mint      — "shrinking, efficient"
val CompressAccent = Color(0xFF2A9B72)

val SecurePastel = Color(0xFFEAD4FF)      // Soft lavender  — "protection, trust"
val SecureAccent = Color(0xFF7B4DB8)

val ConvertPastel = Color(0xFFFFF3D4)     // Soft amber     — "transformation"
val ConvertAccent = Color(0xFFB8860B)

val EditPastel = Color(0xFFFFD4E8)        // Soft rose      — "creative, active"
val EditAccent = Color(0xFFB8315A)

val SignPastel = Color(0xFFD4FFE4)        // Soft sage green — "confirmed, done"
val SignAccent = Color(0xFF2A8B4A)

val HealthPastel = Color(0xFFFFF8D4)      // Soft lemon     — "diagnostic, insight"
val HealthAccent = Color(0xFF8B7A00)

val WhatsAppPastel = Color(0xFFD4FFD4)    // Soft green     — "sharing, sending"
val WhatsAppAccent = Color(0xFF128C3E)

// Neutral card background
val CardSurface = Color(0xFFFFFFFF)       // Pure white cards on warm surface
val DividerColor = Color(0xFFE8E5E0)

// Dark mode equivalents
val SurfaceDark = Color(0xFF1A1917)       // Warm dark, not cold black
val SurfaceVariantDark = Color(0xFF242220)
val CardSurfaceDark = Color(0xFF2A2826)
val OnSurfaceDark = Color(0xFFF0EDE8)
```

### Typography

```kotlin
// Type.kt — Use DM Sans (Google Font, small footprint, feels warm and modern)
// Import via downloadable fonts — does NOT increase APK size

val FolioTypography = Typography(
    displayLarge  = TextStyle(fontFamily = DmSans, fontWeight = W300, fontSize = 57.sp, letterSpacing = (-0.25).sp),
    headlineLarge = TextStyle(fontFamily = DmSans, fontWeight = W600, fontSize = 32.sp),
    headlineMedium= TextStyle(fontFamily = DmSans, fontWeight = W600, fontSize = 24.sp),
    titleLarge    = TextStyle(fontFamily = DmSans, fontWeight = W500, fontSize = 20.sp),
    titleMedium   = TextStyle(fontFamily = DmSans, fontWeight = W500, fontSize = 16.sp, letterSpacing = 0.1.sp),
    bodyLarge     = TextStyle(fontFamily = DmSans, fontWeight = W400, fontSize = 16.sp),
    bodyMedium    = TextStyle(fontFamily = DmSans, fontWeight = W400, fontSize = 14.sp),
    labelLarge    = TextStyle(fontFamily = DmSans, fontWeight = W500, fontSize = 14.sp, letterSpacing = 0.1.sp),
    labelSmall    = TextStyle(fontFamily = DmSans, fontWeight = W400, fontSize = 11.sp, letterSpacing = 0.5.sp),
)
```

### Spacing & Shape System

```kotlin
// Consistent spacing tokens — always use these, never hardcode random values
object Spacing {
    val xs  = 4.dp
    val sm  = 8.dp
    val md  = 16.dp
    val lg  = 24.dp
    val xl  = 32.dp
    val xxl = 48.dp
}

object Radius {
    val sm  = 8.dp
    val md  = 16.dp
    val lg  = 24.dp
    val xl  = 32.dp
    val pill = 100.dp  // for chips and badges
}
```

### Motion & Animation Principles

This is what makes the app feel "buttery smooth." Every transition must be animated. No jarring cuts.

```kotlin
// NavigationTransitions.kt
// Use these for ALL screen transitions — non-negotiable

fun enterTransition(): EnterTransition = slideInHorizontally(
    initialOffsetX = { fullWidth -> fullWidth },
    animationSpec = tween(durationMillis = 320, easing = EaseOutCubic)
) + fadeIn(animationSpec = tween(200))

fun exitTransition(): ExitTransition = slideOutHorizontally(
    targetOffsetX = { fullWidth -> -fullWidth / 4 },
    animationSpec = tween(320, easing = EaseInCubic)
) + fadeOut(animationSpec = tween(200))

fun popEnterTransition(): EnterTransition = slideInHorizontally(
    initialOffsetX = { fullWidth -> -fullWidth / 4 },
    animationSpec = tween(320, easing = EaseOutCubic)
) + fadeIn(animationSpec = tween(200))

fun popExitTransition(): ExitTransition = slideOutHorizontally(
    targetOffsetX = { fullWidth -> fullWidth },
    animationSpec = tween(320, easing = EaseInCubic)
) + fadeOut(animationSpec = tween(200))

// Card press animation — scale down on tap
fun Modifier.pressEffect(): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    this
        .scale(scale)
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    tryAwaitRelease()
                    isPressed = false
                }
            )
        }
}

// Progress bar — always use animated version
// Animate file size numbers counting up on success screen
// Stagger tool card appearance on home screen load
```

### Icon System

- Use **Lucide Icons** (Apache 2.0 license) — thin, modern, consistent stroke weight
- Every tool gets a unique icon — no reusing icons across different tools
- Icon size: `24.dp` in cards, `20.dp` in lists, `32.dp` in hero positions
- Icon tint: Always use the tool's Accent color — never grey icons

---

## 📁 COMPLETE PROJECT FOLDER STRUCTURE

```
app/
├── src/main/
│   ├── java/com/folio/
│   │   ├── FolioApp.kt                    # Application class (Hilt)
│   │   ├── MainActivity.kt
│   │   ├── di/
│   │   │   ├── AppModule.kt
│   │   │   └── DatabaseModule.kt
│   │   ├── data/
│   │   │   ├── local/
│   │   │   │   ├── db/FolioDatabase.kt
│   │   │   │   ├── dao/HistoryDao.kt
│   │   │   │   ├── dao/RecentFileDao.kt
│   │   │   │   └── entity/
│   │   │   │       ├── HistoryEntity.kt
│   │   │   │       └── RecentFileEntity.kt
│   │   │   └── repository/
│   │   │       ├── DocumentRepository.kt
│   │   │       ├── HistoryRepository.kt
│   │   │       └── BillingRepository.kt
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   │   ├── DocumentFile.kt
│   │   │   │   ├── Operation.kt
│   │   │   │   └── HealthReport.kt
│   │   │   └── usecase/
│   │   │       ├── pdf/
│   │   │       │   ├── MergePdfUseCase.kt
│   │   │       │   ├── SplitPdfUseCase.kt
│   │   │       │   ├── CompressPdfUseCase.kt
│   │   │       │   ├── RotatePdfUseCase.kt
│   │   │       │   ├── ReorderPdfUseCase.kt
│   │   │       │   ├── ProtectPdfUseCase.kt
│   │   │       │   ├── UnlockPdfUseCase.kt
│   │   │       │   ├── WatermarkPdfUseCase.kt
│   │   │       │   ├── EditPdfUseCase.kt
│   │   │       │   └── ESignPdfUseCase.kt
│   │   │       ├── convert/
│   │   │       │   ├── ImageToPdfUseCase.kt
│   │   │       │   ├── PdfToImageUseCase.kt
│   │   │       │   ├── PdfToTextUseCase.kt
│   │   │       │   ├── WordToPdfUseCase.kt
│   │   │       │   ├── PdfToWordUseCase.kt
│   │   │       │   ├── PptToPdfUseCase.kt
│   │   │       │   ├── PdfToPptUseCase.kt
│   │   │       │   └── ExcelToPdfUseCase.kt
│   │   │       └── utility/
│   │   │           ├── HealthCheckUseCase.kt
│   │   │           └── WhatsAppShrinkUseCase.kt
│   │   ├── ui/
│   │   │   ├── theme/
│   │   │   │   ├── Color.kt
│   │   │   │   ├── Theme.kt
│   │   │   │   ├── Type.kt
│   │   │   │   └── Motion.kt
│   │   │   ├── navigation/
│   │   │   │   ├── AppNavGraph.kt
│   │   │   │   └── Transitions.kt
│   │   │   ├── screens/
│   │   │   │   ├── home/
│   │   │   │   │   ├── HomeScreen.kt
│   │   │   │   │   └── HomeViewModel.kt
│   │   │   │   ├── merge/
│   │   │   │   ├── split/
│   │   │   │   ├── compress/
│   │   │   │   ├── rotate/
│   │   │   │   ├── reorder/
│   │   │   │   ├── edit/
│   │   │   │   ├── esign/
│   │   │   │   ├── imagetopdf/
│   │   │   │   ├── pdftoimage/
│   │   │   │   ├── pdftotext/
│   │   │   │   ├── wordtopdf/
│   │   │   │   ├── pdftodocx/
│   │   │   │   ├── ppttopdf/
│   │   │   │   ├── pdftoppt/
│   │   │   │   ├── exceltopdf/
│   │   │   │   ├── protect/
│   │   │   │   ├── unlock/
│   │   │   │   ├── watermark/
│   │   │   │   ├── healthcheck/
│   │   │   │   ├── whatsapp/
│   │   │   │   ├── history/
│   │   │   │   └── settings/
│   │   │   └── components/
│   │   │       ├── FolioToolCard.kt       # The signature tool card component
│   │   │       ├── FolioTopBar.kt
│   │   │       ├── FolioButton.kt
│   │   │       ├── FolioProgressSheet.kt  # Bottom sheet during processing
│   │   │       ├── FolioSuccessScreen.kt  # Reusable success state
│   │   │       ├── FilePicker.kt
│   │   │       ├── AdBanner.kt
│   │   │       └── PageThumbnail.kt
│   │   └── util/
│   │       ├── FileUtil.kt
│   │       ├── PdfUtil.kt
│   │       ├── AdManager.kt
│   │       └── Extensions.kt
│   ├── res/
│   │   ├── drawable/                      # Lucide SVG icons + Lottie JSONs
│   │   ├── font/                          # DM Sans downloadable font config
│   │   ├── raw/                           # Lottie animation files
│   │   └── values/
│   └── AndroidManifest.xml
```

---

## 🏠 HOME SCREEN — "The Dashboard"

The home screen is the first impression. It must feel like a premium tool, not a grid of boring icons.

### Layout Structure (top to bottom):

```
┌─────────────────────────────────────────┐
│  "Good morning, Thejas" ← personalized  │  ← Greeting header (time-aware)
│  [Folio logo mark — small, tasteful]    │
├─────────────────────────────────────────┤
│  RECENT FILES  ───────────────────────  │  ← Horizontal scroll strip
│  [doc1.pdf] [report.docx] [slides.pptx] │    File type icons, name, date
├─────────────────────────────────────────┤
│  ┌──────────────────────────────────┐   │
│  │  ⚡ WhatsApp Ready Shrinker      │   │  ← Hero feature card (full width)
│  │  Make any file share-ready       │   │    MintPastel bg, large icon
│  │  instantly           [Use →]     │   │
│  └──────────────────────────────────┘   │
├─────────────────────────────────────────┤
│  PDF TOOLS  ────────────────────────    │  ← Section header: tiny caps label
│  ┌──────────┐  ┌──────────┐            │
│  │ 🔵 Merge │  │ 🟠 Split  │            │  ← 2-col staggered grid
│  └──────────┘  └──────────┘            │    Each card: pastel bg, icon,
│  ┌──────────┐  ┌──────────┐            │    name, 1-line description
│  │ 🟢 Compress│ │ 🔴 Rotate│           │
│  └──────────┘  └──────────┘            │
├─────────────────────────────────────────┤
│  CONVERT  ──────────────────────────   │
│  [Word→PDF] [PDF→Word] [PPT→PDF]...    │
├─────────────────────────────────────────┤
│  SECURITY  ─────────────────────────   │
│  [Protect] [Unlock] [Watermark] [Sign] │
├─────────────────────────────────────────┤
│  SMART TOOLS  ──────────────────────   │
│  [Health Check] [PDF Editor]           │
├─────────────────────────────────────────┤
│  [AdMob Banner — hidden for paid users]│
└─────────────────────────────────────────┘
```

### FolioToolCard Component (build this perfectly — it's used 20+ times):

```kotlin
@Composable
fun FolioToolCard(
    title: String,
    description: String,
    icon: ImageVector,
    pastelColor: Color,       // Card background
    accentColor: Color,       // Icon tint + subtle details
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .pressEffect()    // Scale animation on tap
            .clickable { onClick() },
        shape = RoundedCornerShape(Radius.lg),   // 24.dp
        colors = CardDefaults.cardColors(containerColor = pastelColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)  // Flat — no shadow
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),  // 16.dp
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            // Icon in a slightly darker tinted circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(accentColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
            }
            Text(title, style = MaterialTheme.typography.titleMedium, color = OnSurface)
            Text(description, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant, maxLines = 2)
        }
    }
}
```

### Home Screen Stagger Animation (cards must not all appear at once):

```kotlin
// Stagger tool cards on first load
LaunchedEffect(Unit) {
    tools.forEachIndexed { index, _ ->
        delay(index * 40L)   // 40ms between each card
        visibleCards.add(index)
    }
}

// Each card uses AnimatedVisibility with a slide + fade
AnimatedVisibility(
    visible = index in visibleCards,
    enter = slideInVertically(initialOffsetY = { it / 3 }) + fadeIn()
)
```

---

## 📋 COMPLETE FEATURE LIST (32 Features — All Free)

**Zero PRO gates. Everything is available to every user.**
The only monetization is: Ads (free users) and Remove Ads ($4.99 one-time).

---

### 🔵 PDF TOOLS (10 features)

#### 1. PDF Merger
- Pick 2–15 files (PDF + DOCX + PPTX + images all accepted and auto-converted before merging)
- Draggable reorderable file list with drag handles
- Show file type badge, name, size, page count for each
- Background merge via Coroutine on Dispatchers.IO
- **Progress: animated indeterminate → switches to determinate once file size is known**
- Success screen: file name, size saved, confetti Lottie animation, Share + Open + Done buttons

#### 2. PDF Splitter
- Pick one PDF, show page thumbnails (PdfRenderer)
- Three modes:
  - **By page range**: "1-3, 5, 8-12" — validate input in real time
  - **Every N pages**: split into chunks
  - **Individual pages**: one PDF per page
- Preview affected pages highlighted before splitting

#### 3. PDF Compressor (with Smart Preview)
- Pick one or multiple PDFs
- **Instant size analysis** — before user taps anything, show:
  ```
  Original: 8.4 MB
  ─────────────────────────────
  🟢 Light    → ~6.2 MB  (-26%)  [Tap to select]
  🟡 Balanced → ~3.1 MB  (-63%)  [Tap to select] ← Default
  🔴 Maximum  → ~1.0 MB  (-88%)  [Tap to select]
  ```
- After compression: animated before/after size bar comparison
- Batch mode: compress multiple files at once with per-file progress

#### 4. PDF Page Rotator
- Thumbnail grid of all pages (3-column)
- Tap to select, long-press to enter multi-select
- "Select All" chip at top
- Rotate buttons: 90° CW / 90° CCW / 180°
- **Live preview**: selected pages show rotation immediately in grid (optimistic UI)

#### 5. PDF Page Reorder
- Full-screen draggable thumbnail grid
- Long-press + drag to reorder
- Page number badge on each thumbnail
- Undo last move (shake gesture or undo button)

#### 6. PDF Editor ⭐ (New — Enterprise Feature)
- Open any PDF and tap to edit existing text
- Add new text boxes anywhere on the page
- Add images (from gallery or camera)
- Delete pages
- Draw / annotate with a pen tool (freehand)
- Highlight text (finger select + color picker)
- Add sticky note comments
- Uses Canvas-based rendering — all local, no WebView
- Save as new file (non-destructive by default)

#### 7. PDF Password Protector
- 128-bit AES encryption
- Enter + confirm password with visibility toggle
- Live password strength indicator (Weak / Fair / Strong / Very Strong)
- Option to set permissions: allow printing, allow copying text
- Output: `Protected_[filename].pdf`

#### 8. PDF Password Remover
- Enter password, validate in real time
- Clear, kind error message if wrong (not a crash, not a generic error)
- Show "Forgot your password? We can't recover it — but here's why" — educational tooltip

#### 9. PDF Watermark Adder
- Watermark text input
- Controls: Font size, opacity (10–90%), angle (45° default, adjustable), color picker (8 presets + custom HEX)
- **Live preview** on first page thumbnail — updates as user adjusts sliders
- Option: text watermark OR image watermark (upload any PNG)

#### 10. PDF Page Reorder (already listed above as #5)

---

### 🟡 CONVERT TOOLS (10 features)

All conversions are fully on-device using Apache POI + iText7. No internet required.

#### 11. Image → PDF
- Pick 1–30 images (JPG, PNG, WEBP, HEIC, BMP)
- Reorderable list
- Page size: A4, A3, Letter, Fit to Image, Square
- Margin: None, Small, Medium, Large
- Output quality slider for images embedded in PDF

#### 12. PDF → Images
- Output: JPG or PNG
- Quality: 72dpi (screen), 150dpi (standard), 300dpi (print)
- Each page → one image, saved in a folder named after the PDF

#### 13. PDF → Text
- Extract all selectable text from a text-based PDF
- Show in scrollable text view with search-within (Ctrl+F style)
- Copy All button, Save as .txt button
- Friendly error if scanned (image-based) PDF detected: "This PDF contains scanned images. Use an OCR tool to extract text."

#### 14. Word (DOCX) → PDF ⭐
- Support: `.doc`, `.docx`
- Preserve: fonts, tables, images, headers/footers, numbered lists, styles
- Using Apache POI → iText7 rendering pipeline
- Show page count and preview of first page before saving

#### 15. PDF → Word (DOCX) ⭐
- Extract text + layout and rebuild as editable .docx
- Show a "layout accuracy" notice: "Complex PDFs may lose some formatting — this is normal"
- Output: clean, editable Word document

#### 16. PowerPoint (PPTX) → PDF ⭐
- Support: `.ppt`, `.pptx`
- Each slide → one PDF page
- Preserve text, images, shapes, backgrounds
- Show slide count and thumbnail strip preview

#### 17. PDF → PowerPoint (PPTX) ⭐
- Each PDF page → one slide
- Text extraction per page placed in slide text boxes
- Images preserved
- Output: editable .pptx

#### 18. Excel (XLSX) → PDF ⭐
- Support: `.xls`, `.xlsx`
- Fit to page width automatically
- Option: all sheets or selected sheets
- Grid lines: show/hide toggle

#### 19. Word / PPT / Excel → PDF (Universal Converter)
- A single drop zone screen
- User drops ANY supported file — app detects format automatically and routes to correct converter
- Supported: `.pdf`, `.docx`, `.doc`, `.pptx`, `.ppt`, `.xlsx`, `.xls`, `.txt`, `.jpg`, `.png`, `.webp`, `.heic`, `.bmp`
- This is the **"just works"** screen — power user shortcut on home screen

#### 20. PDF Splitter (batch export as images already in #12)

---

### 🟣 SECURITY TOOLS (4 features)

*(Protect, Unlock, Watermark already detailed above)*

#### 21. E-Sign PDF ⭐ (Enterprise-grade)
- Open any PDF for signing
- Three signature input methods:
  - **Draw**: finger-draw your signature on a canvas (smooth Bezier curves, pressure-simulated stroke width)
  - **Type**: type your name, choose from 5 signature-style fonts
  - **Image**: upload a photo of your handwritten signature
- Place signature anywhere: drag-and-drop the signature stamp onto the page
- Resize + rotate the placed signature
- Add date stamp (optional, auto-fills today's date)
- Add initials stamp (separate from signature, for multi-page documents)
- Save signed PDF — original is untouched (save as copy)
- The signature canvas must feel like real ink: use Compose Canvas with cubic bezier smoothing

```kotlin
// Smooth signature drawing — implement this exactly
class SignatureState {
    private val path = Path()
    private var lastX = 0f
    private var lastY = 0f

    fun onDown(x: Float, y: Float) {
        path.moveTo(x, y)
        lastX = x; lastY = y
    }

    fun onMove(x: Float, y: Float) {
        // Smooth with quadratic bezier
        val midX = (lastX + x) / 2f
        val midY = (lastY + y) / 2f
        path.quadraticBezierTo(lastX, lastY, midX, midY)
        lastX = x; lastY = y
    }

    fun render(canvas: Canvas) {
        canvas.drawPath(path, signaturePaint)
    }
}
```

---

### 🟠 SMART / UNIQUE TOOLS (4 features)

#### 22. PDF Health Checker
- Instant scan: integrity, password status, page count, file size, PDF version, content type (text/scanned/mixed), embedded fonts, broken references
- Results shown as a **beautiful card report** — green ✅ for good, amber ⚠️ for warnings, red ❌ for issues
- "Share Report" exports as formatted text

#### 23. WhatsApp Ready Shrinker
- One-tap smart compress targeting:
  - WhatsApp (under 100MB, ideal under 16MB)
  - Email (under 10MB)
  - Gmail (under 25MB)
  - Custom target (user types MB)
- App auto-picks compression level to hit target
- If target is unreachable: "Best we can do: X MB — proceed?"
- Success screen: animated checkmark, file size, "Share on WhatsApp" deep link button

#### 24. PDF Health Checker already listed above

#### 25. Recent Files Dashboard (integrated into home)
- Last 20 files touched by the app (opened, created, modified)
- File type icon, name, operation performed, time ago
- Tap to re-open in correct tool
- Swipe to remove from recents

---

### ⚙️ UTILITY (6 features)

#### 26. History Log
- Room-backed log of every operation: tool, input file, output file, timestamp, size
- Group by date (Today, Yesterday, This Week, Older)
- Tap to re-open output file (with graceful handling if file is gone)
- Swipe to delete entries

#### 27. Dark Mode
- Full warm dark theme (not cold blue-grey — use `#1A1917` surfaces)
- Follow system, or manual override in Settings

#### 28. Default Save Location
- User sets once in Settings — all tools respect it

#### 29. Remove Ads ($4.99 one-time purchase)
- Google Play Billing v6
- After purchase: all banner + interstitial ads gone permanently
- Restore purchase option
- Show a subtle persistent "✨ Remove Ads — $4.99" row in Settings
- On home screen: tiny non-intrusive "Go ad-free" text chip near top — not a banner

#### 30. Privacy Policy Screen
- In-app, always accessible from Settings
- State clearly: fully offline, no data collected, no file uploads, ads by Google AdMob

#### 31. Settings Screen
- Appearance: Auto / Light / Dark
- Default save folder
- Remove Ads / Restore Purchase
- Privacy Policy
- App version + build number
- "Rate Folio" (opens Play Store)
- "Share Folio" (system share sheet)

#### 32. Onboarding (first launch only — 3 screens max)
- Screen 1: "Everything offline. Your files stay on your phone."
- Screen 2: "32 powerful tools. One beautiful app."
- Screen 3: "No account. No cloud. Just you and your documents."
- Skip button always visible
- Illustrated with simple SVG illustrations — warm, minimal, human

---

## 🔄 PROCESSING EXPERIENCE (Critical for Enterprise Feel)

Every tool that processes a file must follow this exact UX pattern — no exceptions:

### Bottom Sheet Progress (not a blocking dialog)

```kotlin
// FolioProgressSheet.kt
// Show as ModalBottomSheet — user can still see what's below

@Composable
fun FolioProgressSheet(
    fileName: String,
    operationLabel: String,     // e.g. "Compressing", "Merging", "Converting"
    progress: Float?,           // null = indeterminate, 0f-1f = determinate
    onCancel: (() -> Unit)? = null
) {
    // Layout:
    // [Animated tool icon — subtle pulse animation]
    // [operationLabel] [fileName]
    // [LinearProgressIndicator — animated, rounded]
    // "This may take a few seconds..."
    // [Cancel button — only shown if cancellable]
}
```

### Success State

```kotlin
// FolioSuccessScreen.kt — shared across ALL tools

@Composable
fun FolioSuccessScreen(
    outputFileName: String,
    originalSize: Long,
    outputSize: Long,
    operationLabel: String,     // e.g. "Compressed", "Merged", "Converted"
    onShare: () -> Unit,
    onOpen: () -> Unit,
    onDone: () -> Unit
) {
    // Layout:
    // Lottie checkmark animation (plays once, loops idle)
    // "Done! [operationLabel] successfully."
    // Animated size counter (numbers count up like a slot machine)
    // If compression: "Saved X MB (Y%)" in a mint pastel pill
    // [Share] [Open] buttons (primary actions)
    // [Done] text button (secondary)
}
```

---

## 💰 ADMOB INTEGRATION RULES

### Placement Rules:
- **Banner ad**: bottom of every screen — hidden permanently after Remove Ads purchase
- **Interstitial ad**: ONLY shown after a successful operation completes — never mid-task
- Minimum cooldown between interstitials: **4 minutes**
- **Never** show interstitials on History or Settings screens
- **Never** interrupt an operation with an ad

### AdManager Implementation:

```kotlin
object AdManager {
    private var lastInterstitialShownAt = 0L
    private val COOLDOWN_MS = 4 * 60 * 1000L

    fun shouldShowInterstitial(adsRemoved: Boolean): Boolean {
        if (adsRemoved) return false
        return (System.currentTimeMillis() - lastInterstitialShownAt) > COOLDOWN_MS
    }

    fun recordInterstitialShown() {
        lastInterstitialShownAt = System.currentTimeMillis()
    }
}
```

### Test Ad Unit IDs (development only):

```
Banner:       ca-app-pub-3940256099942544/6300978111
Interstitial: ca-app-pub-3940256099942544/1033173712
```

Replace with real IDs before Play Store submission.

---

## 🔒 SECURITY & PRIVACY

- 100% on-device processing — nothing uploaded, ever
- `FileProvider` for all file sharing (Android security model)
- All temp files in `context.cacheDir` — auto-deleted after each operation
- Correct permission requests by API level:
  - API 33+: `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO`
  - API 26–32: `READ_EXTERNAL_STORAGE`
- Never store the actual content of user files in Room — only metadata (name, path, size, timestamp)

```kotlin
fun clearOperationTempFiles(context: Context) {
    context.cacheDir.walkTopDown()
        .filter { it.isFile && it.extension in listOf("pdf", "jpg", "png", "docx", "pptx", "xlsx") }
        .forEach { it.delete() }
}
```

---

## ⚡ PERFORMANCE TARGETS (Enterprise Standard)

| Metric | Target |
|---|---|
| App cold start | < 1.5 seconds |
| Home screen load | < 400ms |
| Screen transition | 320ms (buttery, no stutter) |
| Merge 5 PDFs (10MB each) | < 8 seconds |
| Compress 1 PDF (10MB) | < 5 seconds |
| Word → PDF (20 pages) | < 10 seconds |
| APK size | < 20MB (Office libs add size — acceptable) |

### Performance Rules:
- All file I/O and PDF operations run on `Dispatchers.IO` — **never** main thread
- Use `PdfRenderer` (built-in Android) for thumbnails — not a library
- Thumbnail cache: keep last 50 thumbnails in LRU memory cache (use Coil's built-in cache)
- Lazy load thumbnails in grids — never load all at once
- Enable R8 full mode + ProGuard:

```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

---

## 📐 NAVIGATION STRUCTURE

```
Home (Dashboard)
├── [PDF Tools]
│   ├── Merge
│   ├── Split
│   ├── Compress
│   ├── Rotate
│   ├── Reorder
│   ├── Edit PDF          ← New
│   ├── Protect
│   ├── Unlock
│   └── Watermark
├── [Convert]
│   ├── Universal Converter (any file → PDF or PDF → any)   ← Featured
│   ├── Image → PDF
│   ├── PDF → Image
│   ├── PDF → Text
│   ├── Word ↔ PDF
│   ├── PPT ↔ PDF
│   └── Excel → PDF
├── [Security]
│   ├── E-Sign            ← New
│   ├── Protect
│   ├── Unlock
│   └── Watermark
├── [Smart Tools]
│   ├── Health Checker
│   └── WhatsApp Shrinker
├── History
└── Settings
    └── Privacy Policy

// All transitions use the slide+fade transitions defined in Motion.kt
```

---

## 📦 KEY DEPENDENCIES (build.gradle.kts)

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
}

dependencies {
    // Compose BOM — pins all Compose versions
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.foundation:foundation")   // drag & drop

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // ViewModel + Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")

    // Hilt DI
    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-android-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Room (History)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // PDF Engine
    implementation("com.itextpdf:itext7-core:7.2.5")

    // Office File Support (Word, Excel, PowerPoint)
    implementation("org.apache.poi:poi:5.2.5")
    implementation("org.apache.poi:poi-ooxml:5.2.5")

    // AdMob
    implementation("com.google.android.gms:play-services-ads:23.1.0")

    // Google Play Billing (IAP)
    implementation("com.android.billingclient:billing-ktx:6.2.1")

    // Coil (image loading + caching)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Lottie (success animations, onboarding illustrations)
    implementation("com.airbnb.android:lottie-compose:6.4.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // DataStore (for preferences: dark mode, save location, ads removed flag)
    implementation("androidx.datastore:datastore-preferences:1.1.1")
}
```

---

## 🚨 ERROR HANDLING — The Enterprise Standard

Every error must be caught, named, and communicated warmly. No "An error occurred." ever.

| Error | User-Facing Message |
|---|---|
| File not found | "We couldn't find this file. It may have been moved or deleted." |
| File too large | "This file is too large to process on your device right now." |
| Storage full | "Your device is running out of space. Free up some storage and try again." |
| Wrong password | "That password didn't work. Double-check and try again." |
| Corrupted PDF | "This file appears to be damaged. Try downloading or obtaining it again." |
| Permission denied | "Folio needs storage access to work. Tap here to grant it in Settings." |
| Unsupported format | "This file type isn't supported yet. We're always adding more!" |
| Conversion failed | "Something went wrong with the conversion. The file format may not be fully supported." |

Show errors using:
- **Snackbar** for non-critical, recoverable errors
- **Full-screen error state** (with illustration + retry button) for operation failures
- **Inline validation** on input fields (passwords, page ranges) — real-time, not on submit

---

## 🔨 BUILD PHASES (Follow In Order)

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
PHASE 1 — Foundation (Day 1–2)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
□ Android project setup with Hilt
□ All dependencies added to build.gradle.kts
□ Folio design system: Color.kt, Type.kt, Theme.kt, Motion.kt
□ FolioToolCard, FolioTopBar, FolioButton, FolioProgressSheet, FolioSuccessScreen components
□ Navigation graph skeleton (all routes defined, screens are empty placeholders)
□ Home screen UI — full layout, no functionality (use fake data)
□ Stagger animation on home screen card grid
□ AdMob banner integration

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
PHASE 2 — Core PDF Tools (Day 3–7)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
□ PDF Merger (with draggable file list, progress sheet, success screen)
□ PDF Compressor + Smart Preview Panel
□ PDF Splitter
□ PDF Page Rotator (with thumbnail grid + PdfRenderer)
□ PDF Page Reorder
□ History screen + Room database

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
PHASE 3 — Conversion Tools (Day 8–12)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
□ Image → PDF
□ PDF → Images
□ Word (DOCX) → PDF
□ PDF → Word (DOCX)
□ PPT → PDF
□ PDF → PPT
□ Excel → PDF
□ PDF → Text
□ Universal Converter screen

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
PHASE 4 — Security & Smart Tools (Day 13–16)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
□ PDF Protect (128-bit AES)
□ PDF Unlock
□ Watermark Adder (with live preview)
□ E-Sign (draw, type, image — all 3 modes)
□ PDF Editor (text edit, image add, draw, highlight)
□ PDF Health Checker
□ WhatsApp Ready Shrinker

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
PHASE 5 — Polish & Monetization (Day 17–19)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
□ Settings screen
□ Remove Ads IAP ($4.99) — Google Play Billing v6
□ AdMob interstitials with 4-minute cooldown
□ Onboarding screens (first launch)
□ Privacy Policy screen
□ DataStore migration (replace SharedPreferences with DataStore)
□ Lottie animations on success screens
□ Dark mode thorough testing (every screen)
□ Transition animations QA pass (every screen-to-screen transition)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
PHASE 6 — Pre-Launch (Day 20–21)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
□ ProGuard + R8 full mode enabled
□ APK size check (target < 20MB)
□ Test all 32 features on physical device (not just emulator)
□ Test on Android 8, 10, 12, 14
□ Replace test AdMob IDs with real IDs
□ Generate signed release AAB (not APK — Play Store prefers AAB)
□ Play Store listing: icon, screenshots, feature graphic, description, keywords
□ Content rating questionnaire
□ Internal testing → closed testing → production rollout
```

---

## ✅ PRE-LAUNCH CHECKLIST

- [ ] All 32 features work offline (disable Wi-Fi, test everything)
- [ ] Ads appear correctly, interstitial cooldown works
- [ ] Remove Ads ($4.99) purchase removes all ads permanently
- [ ] Restore Purchase works correctly
- [ ] Dark mode looks correct on every single screen
- [ ] All screen transitions are smooth (no janky frame drops)
- [ ] All temp files deleted after each operation (check cacheDir is empty)
- [ ] APK / AAB size checked
- [ ] Tested on real device — not just emulator
- [ ] App icon: 512×512 PNG (no transparency, no rounded corners — Play Store adds them)
- [ ] Screenshots: 4+ phone screenshots, 1 10-inch tablet screenshot
- [ ] Feature graphic: 1024×500 PNG
- [ ] Privacy Policy screen accessible from Settings
- [ ] Onboarding shows only on first launch (DataStore flag)
- [ ] No hardcoded test Ad IDs in release build
- [ ] Signed with production keystore (keep keystore file safe — losing it = can't update app ever)

---

## 🎯 FIRST COMMANDS FOR COPILOT

Paste this entire file to Copilot, then say:

> **"You are building Folio — an enterprise-grade offline document toolkit for Android. Read the entire prompt above carefully. Start with Phase 1, Step 1: Create a new Android project named 'Folio', configure Hilt, add all dependencies from the prompt to build.gradle.kts, then implement the complete design system exactly as specified in the COLOR PALETTE and TYPOGRAPHY sections. Create Color.kt, Type.kt, Theme.kt, and Motion.kt. Do not build any screens yet."**

After Phase 1 is complete:
> **"Phase 1 done. Now Phase 2, Step 1: Build the FolioToolCard component exactly as specified, the FolioProgressSheet bottom sheet, and the FolioSuccessScreen. Then build the Home screen layout with all sections and stagger animation — use placeholder data for now."**

Continue phase by phase. Never ask Copilot to build more than one Phase at once.

---

*App Name: Folio | Built for: Thejas | Version: 1.0.0*
*Stack: Kotlin + Jetpack Compose + Material 3 + iText7 + Apache POI*
*Philosophy: The greatest document app ever built for Android.*
