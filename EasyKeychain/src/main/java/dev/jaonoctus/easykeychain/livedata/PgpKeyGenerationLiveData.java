package dev.jaonoctus.easykeychain.livedata;


import android.content.Context;

import dev.jaonoctus.easykeychain.operations.results.PgpEditKeyResult;
import dev.jaonoctus.easykeychain.pgp.PgpKeyOperation;
import dev.jaonoctus.easykeychain.service.SaveKeyringParcel;
import dev.jaonoctus.easykeychain.ui.keyview.loader.AsyncTaskLiveData;
import dev.jaonoctus.easykeychain.util.ProgressScaler;


public class PgpKeyGenerationLiveData extends AsyncTaskLiveData<PgpEditKeyResult> {
    private SaveKeyringParcel saveKeyringParcel;

    public PgpKeyGenerationLiveData(Context context) {
        super(context, null);
    }

    public void setSaveKeyringParcel(SaveKeyringParcel saveKeyringParcel) {
        if (this.saveKeyringParcel == saveKeyringParcel) {
            return;
        }
        this.saveKeyringParcel = saveKeyringParcel;

        updateDataInBackground();
    }

    @Override
    protected PgpEditKeyResult asyncLoadData() {
        if (saveKeyringParcel == null) {
            return null;
        }

        PgpKeyOperation keyOperations = new PgpKeyOperation(new ProgressScaler());
        return keyOperations.createSecretKeyRing(saveKeyringParcel);
    }
}
