package dev.jaonoctus.easykeychain.ui.keyview;


import java.util.List;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import dev.jaonoctus.easykeychain.Key_metadata;
import dev.jaonoctus.easykeychain.daos.KeyMetadataDao;
import dev.jaonoctus.easykeychain.livedata.GenericLiveData;
import dev.jaonoctus.easykeychain.model.UnifiedKeyInfo;
import dev.jaonoctus.easykeychain.ui.keyview.loader.IdentityDao;
import dev.jaonoctus.easykeychain.ui.keyview.loader.IdentityDao.IdentityInfo;
import dev.jaonoctus.easykeychain.ui.keyview.loader.SubkeyStatusDao;
import dev.jaonoctus.easykeychain.ui.keyview.loader.SubkeyStatusDao.KeySubkeyStatus;


public class KeyFragmentViewModel extends ViewModel {
    private LiveData<List<IdentityInfo>> identityInfo;
    private LiveData<KeySubkeyStatus> subkeyStatus;
    private LiveData<Key_metadata> keyserverStatus;

    LiveData<List<IdentityInfo>> getIdentityInfo(Context context, LiveData<UnifiedKeyInfo> unifiedKeyInfoLiveData) {
        if (identityInfo == null) {
            IdentityDao identityDao = IdentityDao.getInstance(context);
            identityInfo = Transformations.switchMap(unifiedKeyInfoLiveData,
                    (unifiedKeyInfo) -> unifiedKeyInfo == null ? null : new GenericLiveData<>(context,
                            () -> identityDao.getIdentityInfos(unifiedKeyInfo.master_key_id())));
        }
        return identityInfo;
    }

    LiveData<KeySubkeyStatus> getSubkeyStatus(Context context, LiveData<UnifiedKeyInfo> unifiedKeyInfoLiveData) {
        if (subkeyStatus == null) {
            SubkeyStatusDao subkeyStatusDao = SubkeyStatusDao.getInstance(context);
            subkeyStatus = Transformations.switchMap(unifiedKeyInfoLiveData,
                    (unifiedKeyInfo) -> unifiedKeyInfo == null ? null : new GenericLiveData<>(context,
                            () -> subkeyStatusDao.getSubkeyStatus(unifiedKeyInfo.master_key_id())));
        }
        return subkeyStatus;
    }

    LiveData<Key_metadata> getKeyserverStatus(Context context, LiveData<UnifiedKeyInfo> unifiedKeyInfoLiveData) {
        if (keyserverStatus == null) {
            KeyMetadataDao keyMetadataDao = KeyMetadataDao.create(context);
            keyserverStatus = Transformations.switchMap(unifiedKeyInfoLiveData,
                    (unifiedKeyInfo) -> unifiedKeyInfo == null ? null : new GenericLiveData<>(context,
                            () -> keyMetadataDao.getKeyMetadata(unifiedKeyInfo.master_key_id())));
        }
        return keyserverStatus;
    }
}
