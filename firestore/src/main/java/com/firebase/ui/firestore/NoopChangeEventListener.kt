package com.firebase.ui.firestore

import com.firebase.ui.common.ChangeEventType
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException

object NoopChangeEventListener : ChangeEventListener {
    override fun onChildChanged(
            type: ChangeEventType,
            snapshot: DocumentSnapshot,
            newIndex: Int,
            oldIndex: Int
    ) = Unit

    override fun onDataChanged() = Unit

    override fun onError(e: FirebaseFirestoreException) = Unit
}
