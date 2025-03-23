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
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


@Serializable
data class LabelJSON(
    private var productType: String,
    private var toAddress: String,
    private var destPostalCode: String,
    private var trackPin: String,
    private var barCode: String,
    private var fromAddress: String,
    private var productDimension: String,
    private var productWeight: String,
    private var productInstruction: String,
    private var reference: String
)

{
    fun getProductType() = this.productType

    fun setProductType(productType: String) {
        this.productType = productType
    }

    fun getToAddress() = this.toAddress

    fun setToAddress(toAddress: String) {
        this.toAddress = toAddress
    }

    fun getDestPostalCode() = this.destPostalCode

    fun setDestPostalCode(destPostalCode: String) {
        this.destPostalCode = destPostalCode
    }

    fun getTrackPin() = this.trackPin

    fun setTrackPin(trackPin: String) {
        this.trackPin = trackPin
    }

    fun getBarCode() = this.barCode

    fun setBarCode(barCode: String) {
        this.barCode = barCode
    }

    fun getFromAddress() = this.fromAddress

    fun setFromAddress(fromAddress: String) {
        this.fromAddress = fromAddress
    }

    fun getProductDimension() = this.productDimension

    fun setProductDimension(productDimension: String) {
        this.productDimension = productDimension
    }

    fun getProductWeight() = this.productWeight

    fun setProductWeight(productWeight: String) {
        this.productWeight = productWeight
    }

    fun getProductInstruction() = this.productInstruction

    fun setProductInstruction(productInstruction: String) {
        this.productInstruction = productInstruction
    }

    fun getReference() = this.reference

    fun setReference(reference: String) {
        this.reference = reference
    }

    /**
     * Converts the object into a properly formatted JSON string.
     * Uses Kotlinx Serialization for accurate JSON representation.
     */
    fun toJson(): String {
        return Json { prettyPrint = true }.encodeToString(this)
    }
}

