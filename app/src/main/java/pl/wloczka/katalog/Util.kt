package pl.wloczka.katalog
import android.graphics.*
import java.io.File

private val thumbCache = android.util.LruCache<String, Bitmap>(60)
fun cachedThumb(f: File, size: Int): Bitmap? {
    val k = "${f.path}#$size#${f.lastModified()}"
    thumbCache.get(k)?.let { return it }
    return loadThumb(f, size)?.also { thumbCache.put(k, it) }
}
fun loadThumb(f: File, size: Int): Bitmap? {
    if (!f.exists()) return null
    val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(f.path, o)
    var s = 1; while (o.outWidth / (s * 2) >= size && o.outHeight / (s * 2) >= size) s *= 2
    return BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = s })
}
/** Scales image bytes down to max 1600px, rotates by EXIF, writes JPEG. */
fun saveScaled(src: File, dst: File) {
    val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(src.path, o)
    var s = 1; while (maxOf(o.outWidth, o.outHeight) / (s * 2) >= 1600) s *= 2
    var b = BitmapFactory.decodeFile(src.path, BitmapFactory.Options().apply { inSampleSize = s }) ?: throw Exception("Nie można odczytać zdjęcia")
    val rot = try { when (android.media.ExifInterface(src.path).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1)) {
        6 -> 90f; 3 -> 180f; 8 -> 270f; else -> 0f } } catch (e: Exception) { 0f }
    if (rot != 0f) b = Bitmap.createBitmap(b, 0, 0, b.width, b.height, Matrix().apply { postRotate(rot) }, true)
    dst.outputStream().use { b.compress(Bitmap.CompressFormat.JPEG, 85, it) }
}

/** Edge-to-edge: transparent bars, draws behind them; insets (systemBars+cutout+IME) applied as padding. */
fun android.app.Activity.edgeToEdge(root: android.view.ViewGroup, toolbar: android.view.View, scroll: android.view.ViewGroup) {
    val w = window
    w.statusBarColor = android.graphics.Color.TRANSPARENT
    // API 24-25 nie ma jasnych ikon paska nawigacji, więc dajemy półprzezroczyste tło, żeby były widoczne.
    w.navigationBarColor = if (android.os.Build.VERSION.SDK_INT >= 26) android.graphics.Color.TRANSPARENT else 0x66000000
    if (android.os.Build.VERSION.SDK_INT >= 28) w.attributes = w.attributes.apply {
        layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES }
    if (android.os.Build.VERSION.SDK_INT >= 30) {
        w.setDecorFitsSystemWindows(false)
        w.insetsController?.setSystemBarsAppearance(android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
            android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS or android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
    } else {
        @Suppress("DEPRECATION")
        var f = android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        @Suppress("DEPRECATION")
        if (android.os.Build.VERSION.SDK_INT >= 26) f = f or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        @Suppress("DEPRECATION")
        w.decorView.systemUiVisibility = f
    }
    val tb = intArrayOf(toolbar.paddingLeft, toolbar.paddingTop, toolbar.paddingRight, toolbar.paddingBottom)
    val rb = intArrayOf(root.paddingLeft, root.paddingTop, root.paddingRight, root.paddingBottom)
    val sb = intArrayOf(scroll.paddingLeft, scroll.paddingTop, scroll.paddingRight, scroll.paddingBottom)
    scroll.clipToPadding = false
    root.setOnApplyWindowInsetsListener { _, ins ->
        var l: Int; var t: Int; var r: Int; var b: Int
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            val i = ins.getInsets(android.view.WindowInsets.Type.systemBars() or android.view.WindowInsets.Type.displayCutout() or android.view.WindowInsets.Type.ime())
            l = i.left; t = i.top; r = i.right; b = i.bottom
        } else {
            @Suppress("DEPRECATION") run { l = ins.systemWindowInsetLeft; t = ins.systemWindowInsetTop; r = ins.systemWindowInsetRight; b = ins.systemWindowInsetBottom }
            if (android.os.Build.VERSION.SDK_INT >= 28) ins.displayCutout?.let {
                l = maxOf(l, it.safeInsetLeft); t = maxOf(t, it.safeInsetTop); r = maxOf(r, it.safeInsetRight); b = maxOf(b, it.safeInsetBottom) }
        }
        toolbar.setPadding(tb[0], tb[1] + t, tb[2], tb[3])
        root.setPadding(rb[0] + l, rb[1], rb[2] + r, rb[3])
        scroll.setPadding(sb[0], sb[1], sb[2], sb[3] + b)
        ins
    }
    root.requestApplyInsets()
}
