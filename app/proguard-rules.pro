# --- Networking & Discovery APIs ---
-keep class java.nio.channels.SocketChannel { *; }
-keep class android.net.nsd.NsdManager { *; }

# --- Native UI & PDF APIs ---
-keep class android.graphics.pdf.PdfDocument { *; }

# --- Room Database & Serialization ---
# Keep all Room entities and DTOs intact so reflection-based instantiation doesn't fail after obfuscation
-keep class com.example.networkscanner.data.local.** { *; }
-keep class com.example.networkscanner.domain.model.** { *; }
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keep @androidx.room.Database class *

# --- SQLCipher ---
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }

# --- Security: Strip all Logging from Release Builds ---
# R8 will completely remove these calls from the bytecode, ensuring 
# sensitive IP/MAC addresses or stack traces aren't leaked to Logcat.
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}
