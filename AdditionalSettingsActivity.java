package org.telegram.ui;

import android.view.View;

import org.telegram.messenger.R;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

public class AdditionalSettingsActivity extends UniversalFragment {

    @Override
    protected boolean useSimpleActionBar() {
        return true;
    }

    @Override
    protected CharSequence getTitle() {
        return "إعدادات إضافية";
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader("الإعدادات الإضافية"));
        items.add(UItem.asShadow("هنا راح نضيف ميزاتنا الخاصة بالتدريج."));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}
