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