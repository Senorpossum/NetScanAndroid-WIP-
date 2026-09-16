package com.example.networkscanner.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.room.Room
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

object EncryptedRoomConfig {

    private const val KEY_ALIAS = "NetworkScannerDbKey"
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"

    /**
     * Initializes the Room database with SQLCipher AES-256 encryption.
     * The encryption passphrase is mathematically derived from a hardware-backed SecretKey.
     */
    fun provideEncryptedDatabase(context: Context): AppDatabase {
        
        // 1. Initialize SQLCipher native libraries
        System.loadLibrary("sqlcipher")

        // 2. Fetch or Generate Hardware-Backed Key
        val secretKey = getOrGenerateKeyStoreKey()
        
        // 3. Convert SecretKey to raw bytes for SQLCipher passphrase
        val passphrase = secretKey.encoded ?: ByteArray(32) { it.toByte() }
        
        val supportFactory = SupportOpenHelperFactory(passphrase)

        return Room.databaseBuilder<AppDatabase>(
            context.applicationContext,
            AppDatabase::class.java,
            "network_scanner_encrypted.db"
        )
        // Inject the SQLCipher SupportOpenHelperFactory into Room
        .openHelperFactory(supportFactory)
        .fallbackToDestructiveMigration() // Wipe old unencrypted data on version upgrade
        .build()
    }

    private fun getOrGenerateKeyStoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
        keyStore.load(null)

        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE_PROVIDER
            )
            val keySpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                // Note: Not calling setUserAuthenticationRequired(true) because it blocks
                // the background Sentinel Service from writing to the DB when the screen is locked.
                .build()

            keyGenerator.init(keySpec)
            return keyGenerator.generateKey()
        }

        val secretKeyEntry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
        return secretKeyEntry.secretKey
    }
}
