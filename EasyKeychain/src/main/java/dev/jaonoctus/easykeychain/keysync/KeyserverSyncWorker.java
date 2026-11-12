package dev.jaonoctus.easykeychain.keysync;


import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import dev.jaonoctus.easykeychain.daos.KeyWritableRepository;
import dev.jaonoctus.easykeychain.network.orbot.OrbotHelper;
import dev.jaonoctus.easykeychain.operations.KeySyncOperation;
import dev.jaonoctus.easykeychain.operations.KeySyncParcel;
import dev.jaonoctus.easykeychain.operations.results.ImportKeyResult;
import dev.jaonoctus.easykeychain.service.input.CryptoInputParcel;
import dev.jaonoctus.easykeychain.ui.OrbotRequiredDialogActivity;
import timber.log.Timber;


public class KeyserverSyncWorker extends Worker {
    private final AtomicBoolean cancellationSignal = new AtomicBoolean(false);

    public KeyserverSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        KeyWritableRepository keyWritableRepository =
                KeyWritableRepository.create(getApplicationContext());

        Timber.d("Starting key sync…");
        KeySyncOperation keySync =
                new KeySyncOperation(getApplicationContext(), keyWritableRepository, null,
                        cancellationSignal);
        ImportKeyResult result = keySync.execute(KeySyncParcel.createRefreshOutdated(),
                CryptoInputParcel.createCryptoInputParcel());
        return handleUpdateResult(result);
    }

    /**
     * Since we're returning START_REDELIVER_INTENT in onStartCommand, we need to remember to call
     * stopSelf(int) to prevent the Intent from being redelivered if our work is already done
     *
     * @param result
     *         result of keyserver sync
     */
    private Result handleUpdateResult(ImportKeyResult result) {
        if (result.isPending()) {
            Timber.d("Orbot required for sync but not running, attempting to start");
            // result is pending due to Orbot not being started
            // try to start it silently, if disabled show notifications
            new OrbotHelper.SilentStartManager() {
                @Override
                protected void onOrbotStarted() {
                }

                @Override
                protected void onSilentStartDisabled() {
                    OrbotRequiredDialogActivity.showOrbotRequiredNotification(
                            getApplicationContext());
                }
            }.startOrbotAndListen(getApplicationContext(), false);
            return Result.retry();
        } else if (isStopped()) {
            Timber.d("Keyserver sync cancelled");
            return Result.failure();
        } else {
            Timber.d("Keyserver sync completed: Updated: %d, Failed: %d", result.mUpdatedKeys,
                    result.mBadKeys);
            return Result.success();
        }
    }

    @Override
    public void onStopped() {
        super.onStopped();
        cancellationSignal.set(true);
    }
}
