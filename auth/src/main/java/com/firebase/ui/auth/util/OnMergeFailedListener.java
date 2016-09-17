package com.firebase.ui.auth.util;

public interface OnMergeFailedListener {
    /**
     * In the event that the account the user is trying to sign into already exists,
     * you will need to manually merge the data from the user's previous account into the new one.
     *
     * @param prevUid The user id to transfer data from
     */
    void onMergeFailed(String prevUid);
}
