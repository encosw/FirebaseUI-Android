/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
