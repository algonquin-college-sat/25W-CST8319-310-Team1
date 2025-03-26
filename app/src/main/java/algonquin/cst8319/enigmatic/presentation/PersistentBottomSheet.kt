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