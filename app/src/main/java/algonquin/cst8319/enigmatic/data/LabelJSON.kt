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

