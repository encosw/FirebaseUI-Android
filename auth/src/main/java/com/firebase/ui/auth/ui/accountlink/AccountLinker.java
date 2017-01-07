package com.firebase.ui.auth.ui.accountlink;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.ResultCodes;
import com.firebase.ui.auth.ui.BaseHelper;
import com.firebase.ui.auth.ui.TaskFailureLogger;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AccountLinker implements OnSuccessListener<AuthResult>, OnFailureListener {
    private static final String TAG = "AccountLinker";

    private Activity mActivity;
    private BaseHelper mHelper;
    private IdpResponse mResponse;
    private AuthCredential mNewCredential;
    private AuthCredential mPrevCredential;

    private AccountLinker(Activity activity,
                          BaseHelper helper,
                          IdpResponse response,
                          AuthCredential newCredential,
                          AuthCredential prevCredential) {
        mActivity = activity;
        mHelper = helper;
        mResponse = response;
        mNewCredential = newCredential;
        mPrevCredential = prevCredential;
        start();
    }

    public static void link(Activity activity,
                            BaseHelper helper,
                            IdpResponse response,
                            @NonNull AuthCredential newCredential,
                            @Nullable AuthCredential prevCredential) {
        new AccountLinker(activity, helper, response, newCredential, prevCredential);
    }

    private void start() {
        FirebaseUser currentUser = mHelper.getCurrentUser();
        if (currentUser == null) {
            mHelper.getFirebaseAuth()
                    .signInWithCredential(mNewCredential)
                    .addOnSuccessListener(this)
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            finishWithError();
                        }
                    })
                    .addOnFailureListener(
                            new TaskFailureLogger(TAG, "Error signing in with new credential"));
        } else {
            currentUser
                    .linkWithCredential(mNewCredential)
                    .addOnSuccessListener(new FinishListener())
                    .addOnFailureListener(this)
                    .addOnFailureListener(
                            new TaskFailureLogger(TAG, "Error linking with credential"));
        }
    }

    @Override
    public void onSuccess(AuthResult result) {
        if (mPrevCredential != null) {
            result.getUser()
                    .linkWithCredential(mPrevCredential)
                    .addOnFailureListener(new TaskFailureLogger(
                            TAG, "Error signing in with previous credential"))
                    .addOnCompleteListener(new FinishListener());
        } else {
            mHelper.finishActivity(mActivity, ResultCodes.OK, IdpResponse.getIntent(mResponse));
        }
    }

    @Override
    public void onFailure(@NonNull Exception e) {
        if (e instanceof FirebaseAuthUserCollisionException && mHelper.canLinkAccounts()) {
            mResponse.setPrevUid(mHelper.getUidForAccountLinking());

            // Since we still want the user to be able to sign in even though
            // they have an existing account, we are going to save the uid of the
            // current user, log them out, and then sign in with the new credential.
            Task<AuthResult> signInTask = mHelper.getFirebaseAuth()
                    .signInWithCredential(mNewCredential)
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            finishWithError();
                        }
                    })
                    .addOnFailureListener(
                            new TaskFailureLogger(TAG, "Error linking with credential"));
            if (mPrevCredential == null) {
                signInTask.addOnSuccessListener(new FinishListener());
            } else {
                signInTask.addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult result) {
                        mHelper.getCurrentUser()
                                .linkWithCredential(mPrevCredential)
                                .addOnFailureListener(
                                        new TaskFailureLogger(TAG, "Error linking with credential"))
                                .addOnCompleteListener(new FinishListener());
                    }
                });
            }
        } else {
            finishWithError();
        }
    }

    private void finishWithError() {
        mHelper.finishActivity(
                mActivity,
                ResultCodes.CANCELED,
                IdpResponse.getErrorCodeIntent(ErrorCodes.UNKNOWN_ERROR));
    }

    private class FinishListener implements OnCompleteListener<AuthResult>, OnSuccessListener<AuthResult> {
        @Override
        public void onComplete(@NonNull Task task) {
            finishOk();
        }

        @Override
        public void onSuccess(AuthResult result) {
            finishOk();
        }

        private void finishOk() {
            mHelper.finishActivity(mActivity, ResultCodes.OK, IdpResponse.getIntent(mResponse));
        }
    }
}
