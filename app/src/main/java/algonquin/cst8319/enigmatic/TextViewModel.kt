package algonquin.cst8319.enigmatic

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
class TextViewModel : ViewModel() {
    val currentText: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }
}