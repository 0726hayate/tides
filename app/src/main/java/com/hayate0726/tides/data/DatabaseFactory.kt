package com.hayate0726.tides.data

import android.content.Context
import androidx.room.Room
import com.hayate0726.tides.crypto.DbKey
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File

/**
 * Opens a Room database backed by SQLCipher, using the given DbKey to
 * encrypt the on-disk file. The factory copies the key bytes into a
 * passphrase array handed to SQLCipher and does not retain a reference.
 * Once open() returns, the caller may zero() the original key.
 *
 * The caller is responsible for closing the database when the app locks.
 */
object DatabaseFactory {

    private val nativeLibLoaded = java.util.concurrent.atomic.AtomicBoolean(false)

    fun open(ctx: Context, file: File, key: DbKey): TidesDatabase {
        if (nativeLibLoaded.compareAndSet(false, true)) {
            System.loadLibrary("sqlcipher")
        }
        val passphrase = key.bytes.copyOf()
        val factory = SupportOpenHelperFactory(passphrase, null, false)
        return Room.databaseBuilder(
            ctx.applicationContext,
            TidesDatabase::class.java,
            file.absolutePath,
        )
            .openHelperFactory(factory)
            .build()
    }
}
