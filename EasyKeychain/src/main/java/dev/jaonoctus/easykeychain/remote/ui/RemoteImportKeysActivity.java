/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package dev.jaonoctus.easykeychain.remote.ui;

import android.content.Intent;
import android.os.Bundle;

import dev.jaonoctus.easykeychain.operations.results.ImportKeyResult;
import dev.jaonoctus.easykeychain.ui.ImportKeysActivity;

public class RemoteImportKeysActivity extends ImportKeysActivity {

    public static final String EXTRA_DATA = "data";

    private Intent mPendingIntentData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPendingIntentData = getIntent().getParcelableExtra(EXTRA_DATA);
    }

    @Override
    public void handleResult(ImportKeyResult result, Integer position) {
        setResult(RESULT_OK, mPendingIntentData);
        finish();
    }

}
