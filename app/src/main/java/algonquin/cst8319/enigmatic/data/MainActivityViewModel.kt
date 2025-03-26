/**
 * Copyright 2025 ENIGMatic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package algonquin.cst8319.enigmatic.data

import android.net.Uri
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainActivityViewModel : ViewModel() {
    val currentText: MutableLiveData<String> = MutableLiveData()
    val headerText: MutableLiveData<String> = MutableLiveData()
    val previewViewVisibility: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)
    val resultContainerVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val imageViewVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val scannedImage: MutableLiveData<Uri?> = MutableLiveData(null)

    /***
     * Update image and visibility.
     */
    fun setScannedImage(bitmap: Uri) {
        scannedImage.value = bitmap
        imageViewVisibility.value = View.VISIBLE
        resultContainerVisibility.value = View.VISIBLE
    }
}