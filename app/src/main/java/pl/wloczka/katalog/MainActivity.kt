package pl.wloczka.katalog
import android.app.*
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.*
import android.view.*
import android.widget.*
import org.json.*
import java.util.zip.*

class MainActivity : Activity() {
    lateinit var db: Db
    lateinit var list: ListView
    lateinit var query: EditText
    lateinit var field: Spinner
    lateinit var count: TextView
    var items = listOf<Yarn>()
    val fields = arrayOf("Wszystkie pola", "Grubość", "Rodzaj", "Kolor", "Ilość", "Miejsce")
    val EXPORT = 1; val IMPORT = 2

    fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        db = Db(this)
        val outer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE) }
        val tbar = Toolbar(this).apply { title = "Katalog włóczki"; setTitleTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#6A1B9A")) }
        outer.addView(tbar, LinearLayout.LayoutParams(-1, -2)); setActionBar(tbar)
        tbar.overflowIcon?.setTint(Color.WHITE)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), dp(8), dp(8), 0) }
        outer.addView(root, LinearLayout.LayoutParams(-1, 0, 1f))
        field = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, fields) }
        query = EditText(this).apply { hint = "Szukaj…"; isSingleLine = true }
        val bar = LinearLayout(this)
        bar.addView(field, LinearLayout.LayoutParams(0, -2, 1f)); bar.addView(query, LinearLayout.LayoutParams(0, -2, 1.3f))
        root.addView(bar)
        val row2 = LinearLayout(this)
        count = TextView(this).apply { gravity = Gravity.CENTER_VERTICAL }
        row2.addView(count, LinearLayout.LayoutParams(0, -1, 1f))
        row2.addView(Button(this).apply { text = "Pokaż wszystkie"; setOnClickListener { query.setText(""); field.setSelection(0) } })
        row2.addView(Button(this).apply { text = "+ Dodaj"; setOnClickListener { startActivity(Intent(this@MainActivity, EditActivity::class.java)) } })
        root.addView(row2)
        list = ListView(this)
        root.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(outer)
        edgeToEdge(outer, tbar, list)
        query.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(e: Editable?) = refresh()
            override fun beforeTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
            override fun onTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
        })
        field.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) = refresh()
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        list.adapter = adapter
        list.setOnItemClickListener { _, _, pos, _ -> edit(items[pos]) }
    }
    override fun onResume() { super.onResume(); refresh() }

    fun edit(y: Yarn) = startActivity(Intent(this, EditActivity::class.java).putExtra("id", y.id))

    fun refresh() {
        val q = query.text.toString().trim().lowercase()
        items = db.all().filter { y ->
            if (q.isEmpty()) true else {
                val vals = listOf(y.grubosc, y.rodzaj, y.kolor, y.ilosc.toString(), y.miejsce)
                when (val f = field.selectedItemPosition) {
                    0 -> vals.any { it.lowercase().contains(q) }
                    4 -> y.ilosc.toString() == q
                    else -> vals[f - 1].lowercase().contains(q)
                }
            }
        }
        count.text = "Pozycji: ${items.size}, motków: ${items.sumOf { it.ilosc }}"
        adapter.notifyDataSetChanged()
    }

    val adapter = object : BaseAdapter() {
        override fun getCount() = items.size
        override fun getItem(p: Int) = items[p]
        override fun getItemId(p: Int) = items[p].id
        override fun getView(p: Int, cv: View?, parent: ViewGroup): View {
            val y = items[p]
            val row = LinearLayout(this@MainActivity).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(6), 0, dp(6)) }
            val img = ImageView(this@MainActivity).apply { scaleType = ImageView.ScaleType.CENTER_CROP; setBackgroundColor(Color.LTGRAY) }
            y.zdjecie?.let { cachedThumb(photoFile(this@MainActivity, it), dp(64))?.let { b -> img.setImageBitmap(b) } }
            row.addView(img, LinearLayout.LayoutParams(dp(64), dp(64)))
            val t = TextView(this@MainActivity).apply {
                setPadding(dp(8), 0, dp(4), 0)
                text = "${y.rodzaj.ifEmpty { "(bez rodzaju)" }} – ${y.kolor}\nGrubość: ${y.grubosc}\nMiejsce: ${y.miejsce}"
            }
            row.addView(t, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(Button(this@MainActivity).apply { text = "−"; textSize = 20f; isFocusable = false; setOnClickListener { change(y, -1) } }, LinearLayout.LayoutParams(dp(48), dp(48)))
            row.addView(TextView(this@MainActivity).apply { text = y.ilosc.toString(); textSize = 18f; gravity = Gravity.CENTER }, LinearLayout.LayoutParams(dp(40), -2))
            row.addView(Button(this@MainActivity).apply { text = "+"; textSize = 20f; isFocusable = false; setOnClickListener { change(y, 1) } }, LinearLayout.LayoutParams(dp(48), dp(48)))
            return row
        }
    }

    fun change(y: Yarn, d: Int) {
        if (y.ilosc + d <= 0) {
            AlertDialog.Builder(this).setTitle("Usunąć pozycję?")
                .setMessage("Ilość spadnie do 0. Czy usunąć „${y.rodzaj} – ${y.kolor}” z katalogu?")
                .setPositiveButton("Usuń") { _, _ -> db.delete(y); refresh() }
                .setNegativeButton("Anuluj", null).show()
        } else { y.ilosc += d; db.save(y); refresh() }
    }

    override fun onCreateOptionsMenu(m: Menu): Boolean {
        m.add(0, 1, 0, "Eksportuj do pliku"); m.add(0, 2, 0, "Importuj z pliku"); return true
    }
    override fun onOptionsItemSelected(i: MenuItem): Boolean {
        when (i.itemId) {
            1 -> startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/zip")
                .putExtra(Intent.EXTRA_TITLE, "katalog-wloczki.zip"), EXPORT)
            2 -> startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*"), IMPORT)
        }
        return true
    }
    override fun onActivityResult(rq: Int, rc: Int, data: Intent?) {
        val uri = data?.data ?: return
        if (rc != RESULT_OK) return
        if (rq == EXPORT) try { export(uri); toast("Wyeksportowano ${db.all().size} pozycji") } catch (e: Exception) { toast("Błąd eksportu: ${e.message}") }
        if (rq == IMPORT) AlertDialog.Builder(this).setTitle("Import")
            .setMessage("Zastąpić obecny katalog danymi z pliku, czy dodać je do obecnych?")
            .setPositiveButton("Zastąp") { _, _ -> doImport(uri, true) }
            .setNeutralButton("Dodaj") { _, _ -> doImport(uri, false) }
            .setNegativeButton("Anuluj", null).show()
    }
    fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()

    fun export(uri: Uri) {
        val arr = JSONArray()
        val all = db.all()
        contentResolver.openOutputStream(uri, "w")!!.use { os ->
            ZipOutputStream(os).use { z ->
                for (y in all) {
                    arr.put(JSONObject().put("grubosc", y.grubosc).put("rodzaj", y.rodzaj).put("kolor", y.kolor)
                        .put("ilosc", y.ilosc).put("miejsce", y.miejsce).put("zdjecie", y.zdjecie ?: JSONObject.NULL))
                    val f = y.zdjecie?.let { photoFile(this, it) }
                    if (f != null && f.exists()) { z.putNextEntry(ZipEntry("photos/${f.name}")); f.inputStream().use { it.copyTo(z) }; z.closeEntry() }
                }
                z.putNextEntry(ZipEntry("katalog.json"))
                z.write(JSONObject().put("wersja", 1).put("wloczki", arr).toString(2).toByteArray()); z.closeEntry()
            }
        }
    }

    fun doImport(uri: Uri, replace: Boolean) {
        try {
            var json: String? = null
            val photos = mutableMapOf<String, ByteArray>()
            contentResolver.openInputStream(uri)!!.use { ZipInputStream(it).use { z ->
                var e = z.nextEntry
                while (e != null) {
                    if (e.name == "katalog.json") json = z.readBytes().toString(Charsets.UTF_8)
                    else if (e.name.startsWith("photos/") && !e.isDirectory) photos[e.name.substringAfterLast('/')] = z.readBytes()
                    e = z.nextEntry
                }
            } }
            val arr = JSONObject(json ?: throw Exception("brak katalog.json w pliku")).getJSONArray("wloczki")
            if (replace) db.clear()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                var photo: String? = null
                if (!o.isNull("zdjecie")) photos[o.getString("zdjecie")]?.let { b ->
                    photo = "img_${System.currentTimeMillis()}_$i.jpg"; photoFile(this, photo!!).writeBytes(b)
                }
                db.save(Yarn(0, o.optString("grubosc"), o.optString("rodzaj"), o.optString("kolor"), o.optInt("ilosc", 1), o.optString("miejsce"), photo))
            }
            toast("Zaimportowano ${arr.length()} pozycji"); refresh()
        } catch (e: Exception) { toast("Błąd importu: ${e.message}") }
    }
}
