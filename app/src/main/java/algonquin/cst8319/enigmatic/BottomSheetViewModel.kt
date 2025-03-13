package algonquin.cst8319.enigmatic

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
class BottomSheetViewModel : ViewModel() {
    val currentText: MutableLiveData<String> = MutableLiveData()
    val headerText: MutableLiveData<String> = MutableLiveData()
}