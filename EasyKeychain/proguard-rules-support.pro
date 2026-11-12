# Obfuscate android.support.v7.view.menu.** to fix Samsung Android 4.2 bug
# https://code.google.com/p/android/issues/detail?id=78377
-keepnames class !android.support.v7.view.menu.**, ** { *; }

-keep class android.support.v7.widget.SearchView { *; }

# Room creates the generated *_Impl database classes by reflection through their default
# constructor. The rule shipped with the Room version WorkManager pulls in only keeps the class,
# and R8 in full mode (the default) then removes the constructor it sees no caller for:
#   java.lang.RuntimeException: Failed to create an instance of class androidx.work.impl.WorkDatabase
# which crashed the release build on startup, in WorkManager's initializer.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
