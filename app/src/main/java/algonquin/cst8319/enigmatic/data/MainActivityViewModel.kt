/**
 *  Copyright 2025 ENIGMatic
 *
 *  Permission is hereby granted, free of charge, to any person obtaining
 *  a copy of this software and associated documentation files (the “Software”),
 *  to deal in the Software without restriction, including without limitation the
 *  rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 *  sell copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 *  INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 *  PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 *  HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 *  CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE
 *  OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
    val progressBarVisibility: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)

    /***
     * Update image and visibility.
     */
    fun setScannedImage(bitmap: Uri) {
        scannedImage.value = bitmap
        imageViewVisibility.value = View.VISIBLE
        resultContainerVisibility.value = View.VISIBLE
    }
}