package pl.wloczka.katalog
import android.content.*
import android.database.sqlite.*
import java.io.File

data class Yarn(var id: Long = 0, var grubosc: String = "", var rodzaj: String = "", var kolor: String = "",
                var ilosc: Int = 1, var miejsce: String = "", var zdjecie: String? = null)

class Db(val ctx: Context) : SQLiteOpenHelper(ctx, "yarn.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE yarn(id INTEGER PRIMARY KEY AUTOINCREMENT, grubosc TEXT, rodzaj TEXT, kolor TEXT, ilosc INTEGER, miejsce TEXT, zdjecie TEXT)")
    }
    override fun onUpgrade(db: SQLiteDatabase, o: Int, n: Int) {}
    fun all(): List<Yarn> {
        val r = mutableListOf<Yarn>()
        readableDatabase.rawQuery("SELECT id,grubosc,rodzaj,kolor,ilosc,miejsce,zdjecie FROM yarn ORDER BY rodzaj COLLATE NOCASE, kolor COLLATE NOCASE", null).use { c ->
            while (c.moveToNext()) r += Yarn(c.getLong(0), c.getString(1) ?: "", c.getString(2) ?: "", c.getString(3) ?: "", c.getInt(4), c.getString(5) ?: "", c.getString(6))
        }
        return r
    }
    fun get(id: Long) = all().firstOrNull { it.id == id }
    private fun cv(y: Yarn) = ContentValues().apply {
        put("grubosc", y.grubosc); put("rodzaj", y.rodzaj); put("kolor", y.kolor); put("ilosc", y.ilosc); put("miejsce", y.miejsce); put("zdjecie", y.zdjecie)
    }
    fun save(y: Yarn) { if (y.id == 0L) y.id = writableDatabase.insert("yarn", null, cv(y)) else writableDatabase.update("yarn", cv(y), "id=?", arrayOf(y.id.toString())) }
    fun delete(y: Yarn) { writableDatabase.delete("yarn", "id=?", arrayOf(y.id.toString())); y.zdjecie?.let { photoFile(ctx, it).delete() } }
    fun clear() { writableDatabase.delete("yarn", null, null); photoDir(ctx).listFiles()?.forEach { it.delete() } }
}
fun photoDir(c: Context) = File(c.filesDir, "photos").apply { mkdirs() }
fun photoFile(c: Context, name: String) = File(photoDir(c), name)
