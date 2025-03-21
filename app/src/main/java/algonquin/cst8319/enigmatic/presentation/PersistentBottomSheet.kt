package algonquin.cst8319.enigmatic.presentation

import algonquin.cst8319.enigmatic.R
import android.app.Dialog
import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PersistentBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateDialog(
        savedInstanceState: Bundle?,
    ): Dialog {
        val bottomSheetDialog = BottomSheetDialog(requireContext())

        bottomSheetDialog.setContentView(R.layout.bottom_sheet)

        // Set behavior attributes here...

        return bottomSheetDialog
    }
}