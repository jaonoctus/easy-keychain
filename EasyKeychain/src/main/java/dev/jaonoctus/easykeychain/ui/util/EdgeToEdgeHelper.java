package dev.jaonoctus.easykeychain.ui.util;


import android.app.Activity;
import android.app.Application;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import dev.jaonoctus.easykeychain.R;


/**
 * Keeps the layouts working now that Android draws every activity edge to edge.
 * <p>
 * Apps that target API 35 or newer are laid out behind the status and navigation bars, and from
 * API 36 on there is no way to opt out. The layouts of this app were written for a window that
 * ends at the system bars, and for a status bar that has the dark primary color. This restores
 * both for all activities at once:
 * <ul>
 * <li>the content view is padded by the system bars, the display cutout and the keyboard, and
 * <li>a strip in the dark primary color is drawn behind the status bar, whose icons are white.
 * </ul>
 * Layouts that handle window insets on their own (the navigation drawer of the main screen) are
 * not padded a second time.
 */
public class EdgeToEdgeHelper implements Application.ActivityLifecycleCallbacks {
    public static void register(Application application) {
        application.registerActivityLifecycleCallbacks(new EdgeToEdgeHelper());
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        // Not in onActivityCreated: that runs before the activity has set its content view.
        View decorView = activity.getWindow().getDecorView();
        if (decorView.getTag(R.id.edge_to_edge_applied) != null) {
            return;
        }
        decorView.setTag(R.id.edge_to_edge_applied, Boolean.TRUE);

        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null) {
            return;
        }

        if (!handlesInsetsItself(content)) {
            ViewCompat.setOnApplyWindowInsetsListener(content, (view, insets) -> {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                        | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return WindowInsetsCompat.CONSUMED;
            });
        }

        if (isOpaqueFullWindow(activity)) {
            StatusBarStripDrawable strip = new StatusBarStripDrawable(resolveStatusBarColor(activity));
            decorView.setForeground(strip);
            ViewCompat.setOnApplyWindowInsetsListener(decorView, (view, insets) -> {
                strip.setHeight(insets.getInsets(WindowInsetsCompat.Type.statusBars()).top);
                return ViewCompat.onApplyWindowInsets(view, insets);
            });
        }

        ViewCompat.requestApplyInsets(decorView);
    }

    private static boolean handlesInsetsItself(ViewGroup content) {
        for (int i = 0; i < content.getChildCount(); i++) {
            if (content.getChildAt(i).getFitsSystemWindows()) {
                return true;
            }
        }
        return false;
    }

    /** Dialog style and see-through activities must not get a colored strip painted over them. */
    private static boolean isOpaqueFullWindow(Activity activity) {
        TypedArray attributes = activity.getTheme().obtainStyledAttributes(new int[] {
                android.R.attr.windowIsFloating, android.R.attr.windowIsTranslucent });
        try {
            return !attributes.getBoolean(0, false) && !attributes.getBoolean(1, false);
        } finally {
            attributes.recycle();
        }
    }

    private static int resolveStatusBarColor(Activity activity) {
        TypedArray attributes = activity.getTheme().obtainStyledAttributes(new int[] { R.attr.colorPrimaryDark });
        try {
            return attributes.getColor(0, 0xff000000);
        } finally {
            attributes.recycle();
        }
    }

    private static class StatusBarStripDrawable extends Drawable {
        private final Paint paint = new Paint();
        private int height;

        StatusBarStripDrawable(int color) {
            paint.setColor(color);
        }

        void setHeight(int height) {
            if (this.height != height) {
                this.height = height;
                invalidateSelf();
            }
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            if (height > 0) {
                canvas.drawRect(0, 0, getBounds().width(), height, paint);
            }
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
    }
}
