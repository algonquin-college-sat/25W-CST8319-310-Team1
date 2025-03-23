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
import android.media.AudioManager
import android.media.ToneGenerator

/**
 * used to validate and convert label data before processing.
 * It checks for missing or invalid fields such as toAddress, fromAddress, and barCode.
 * If validation fails, it triggers an error beep and returns an error message.
 */
class Validator {

    private var hasInvalidField: Boolean = false
    /**
     * Validates the fields of a [LabelJSON] object.
     *
     * Checks for the presence and minimum length of:
     * - toAddress (min 9 characters)
     * - fromAddress (min 11 characters)
     * - barCode (min 7 characters)
     *
     * If all fields are valid, returns the label as a JSON string.
     * If not, returns a formatted error message and plays an error tone.
     *
     * @param label The [LabelJSON] object to validate.
     * @return A JSON string if valid, or an error message if invalid.
     */
    fun validateAndConvert(label: LabelJSON): String {
        var errorMessage: String = ""
        val missingFields = mutableListOf<String>()
        val minToAddressLength = 9
        val minBarcodeLength = 7
        val minFromAddressLength = 11

        // Check for null/blank or too short values
        if (label.getToAddress().isNullOrBlank() || label.getToAddress().length < minToAddressLength ) {
            missingFields.add("toAddress")

            hasInvalidField = true
        }

        if (label.getFromAddress().isNullOrBlank() || label.getFromAddress().length < minFromAddressLength ) {
            missingFields.add("fromAddress")
            hasInvalidField = true
        }

        if (label.getBarCode().isNullOrBlank() || label.getBarCode().length < minBarcodeLength) {
            missingFields.add("barCode")
            hasInvalidField = true
        }

        return if (missingFields.isEmpty()) {
            label.toJson()
        } else {
            playErrorBeep()
            "Missing or invalid fields: ${missingFields.joinToString(", ")}\n\nplease rescan and try again"

        }
    }

    /**
     * Plays an error beep tone using the device’s alarm stream.
     * Called when invalid or missing fields are detected.
     */
    private fun playErrorBeep() {
         val toneGen1 = ToneGenerator(AudioManager.STREAM_ALARM, 100)
         toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 1000)
         toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 1000)
     }

}