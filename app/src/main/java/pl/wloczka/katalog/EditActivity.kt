package pl.wloczka.katalog
import android.app.*
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.*
import android.widget.*
import java.io.File

class EditActivity : Activity() {
    lateinit var db: Db
    lateinit var item: Yarn
    lateinit var img: ImageView
    var newPhoto: String? = null
    var photoChanged = false
    val CAM = 1; val GAL = 2
    val camFile get() = File(cacheDir, "camera.jpg")
    fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        db = Db(this)
        val id = intent.getLongExtra("id", 0)
        item = if (id != 0L) db.get(id) ?: Yarn() else Yarn()
        newPhoto = s?.getString("photo") ?: item.zdjecie
        photoChanged = s?.getBoolean("changed") ?: false
        title = if (id != 0L) "Edycja włóczki" else "Nowa włóczka"
        val l = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(16)) }
        fun field(label: String, v: String, num: Boolean = false): EditText {
            l.addView(TextView(this).apply { text = label; setPadding(0, dp(8), 0, 0) })
            return EditText(this).apply { setText(v); isSingleLine = true
                if (num) inputType = InputType.TYPE_CLASS_NUMBER }.also { l.addView(it) }
        }
        val fG = field("Grubość", item.grubosc); val fR = field("Rodzaj", item.rodzaj); val fK = field("Kolor", item.kolor)
        val fI = field("Ilość", item.ilosc.toString(), true); val fM = field("Miejsce składowania", item.miejsce)
        l.addView(TextView(this).apply { text = "Zdjęcie"; setPadding(0, dp(8), 0, dp(4)) })
        img = ImageView(this).apply { setBackgroundColor(Color.LTGRAY); scaleType = ImageView.ScaleType.FIT_CENTER; adjustViewBounds = true }
        l.addView(img, LinearLayout.LayoutParams(-1, dp(240)))
        val pb = LinearLayout(this)
        pb.addView(Button(this).apply { text = "Aparat"; setOnClickListener { camera() } }, LinearLayout.LayoutParams(0, -2, 1f))
        pb.addView(Button(this).apply { text = "Galeria"; setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE), GAL) } }, LinearLayout.LayoutParams(0, -2, 1f))
        pb.addView(Button(this).apply { text = "Usuń zdjęcie"; setOnClickListener { newPhoto = null; photoChanged = true; showPhoto() } }, LinearLayout.LayoutParams(0, -2, 1f))
        l.addView(pb)
        l.addView(Button(this).apply { text = "Zapisz"; setOnClickListener {
            val q = fI.text.toString().toIntOrNull()
            if (q == null || q < 1) { Toast.makeText(this@EditActivity, "Ilość musi być liczbą ≥ 1", Toast.LENGTH_LONG).show(); return@setOnClickListener }
            val old = item.zdjecie
            item.grubosc = fG.text.toString().trim(); item.rodzaj = fR.text.toString().trim(); item.kolor = fK.text.toString().trim()
            item.ilosc = q; item.miejsce = fM.text.toString().trim(); item.zdjecie = newPhoto
            db.save(item)
            if (old != null && old != newPhoto) photoFile(this@EditActivity, old).delete()
            finish()
        } }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) })
        val outer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE) }
        val tbar = Toolbar(this).apply { title = this@EditActivity.title; setTitleTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#6A1B9A")) }
        tbar.setNavigationIcon(android.R.drawable.ic_menu_close_clear_cancel)
        tbar.navigationIcon?.setTint(Color.WHITE)
        tbar.navigationContentDescription = "Anuluj"
        tbar.setNavigationOnClickListener { finish() }
        outer.addView(tbar, LinearLayout.LayoutParams(-1, -2))
        val sv = ScrollView(this).apply { addView(l) }
        outer.addView(sv, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(outer)
        edgeToEdge(outer, tbar, sv)
        showPhoto()
    }
    override fun onSaveInstanceState(o: Bundle) { super.onSaveInstanceState(o); o.putString("photo", newPhoto); o.putBoolean("changed", photoChanged) }

    fun showPhoto() { val b = newPhoto?.let { loadThumb(photoFile(this, it), dp(400)) }; if (b != null) img.setImageBitmap(b) else img.setImageDrawable(null) }

    fun camera() {
        camFile.delete()
        val uri = Uri.parse("content://pl.wloczka.katalog.photos/camera.jpg")
        val i = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).putExtra(android.provider.MediaStore.EXTRA_OUTPUT, uri)
            .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        i.clipData = android.content.ClipData.newRawUri("", uri)
        try { startActivityForResult(i, CAM) } catch (e: Exception) { Toast.makeText(this, "Brak aplikacji aparatu", Toast.LENGTH_LONG).show() }
    }
    override fun onActivityResult(rq: Int, rc: Int, data: Intent?) {
        if (rc != RESULT_OK) return
        try {
            val src = if (rq == GAL) { val u = data?.data ?: return
                File(cacheDir, "pick.tmp").also { f -> contentResolver.openInputStream(u)!!.use { i -> f.outputStream().use { i.copyTo(it) } } }
            } else camFile
            val name = "img_${System.currentTimeMillis()}.jpg"
            saveScaled(src, photoFile(this, name))
            src.delete()
            // remove previous unsaved new photo
            if (newPhoto != null && newPhoto != item.zdjecie) photoFile(this, newPhoto!!).delete()
            newPhoto = name; photoChanged = true; showPhoto()
        } catch (e: Exception) { Toast.makeText(this, "Błąd zdjęcia: ${e.message}", Toast.LENGTH_LONG).show() }
    }
}
