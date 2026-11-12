# Documentation for ProGuard:
#   http://developer.android.com/guide/developing/tools/proguard.html
#   http://proguard.sourceforge.net/

-dontoptimize
-dontpreverify

-dontnote **

-keepattributes **

-keep class dev.jaonoctus.easykeychain.** { *; }

# fix bug, see https://github.com/BelooS/ChipsLayoutManager/issues/31
-dontwarn com.beloo.widget.chipslayoutmanager.Orientation