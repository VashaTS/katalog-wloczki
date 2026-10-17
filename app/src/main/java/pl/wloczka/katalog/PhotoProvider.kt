package pl.wloczka.katalog
import android.content.*
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File

class PhotoProvider : ContentProvider() {
    override fun onCreate() = true
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val f = File(context!!.cacheDir, uri.lastPathSegment!!.replace("/", ""))
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.parseMode(mode))
    }
    override fun query(u: Uri, p: Array<String>?, s: String?, a: Array<String>?, o: String?): Cursor? = null
    override fun getType(uri: Uri) = "image/jpeg"
    override fun insert(uri: Uri, v: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, a: Array<String>?) = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<String>?) = 0
}
