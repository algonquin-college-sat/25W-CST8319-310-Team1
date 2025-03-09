package algonquin.cst8319.enigmatic
import kotlinx.serialization.Serializable

@Serializable
data class LabelJSON(
    private var productType : String,
    private var toAddress : String,
    private var destPostalCode : String,
    private var trackPin : String,
    private var barCode : String,
    private var fromAddress : String,
    private var productDimension : String,
    private var productWeight : String,
    private var productInstruction : String,
    private var reference : String
) {
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

    override fun toString() : String {
        var string = ""
        string += "{\n"
        string += "\tproductType: ${productType}\n"
        string += "\ttoAddress: ${toAddress}\n"
        string += "\tdestPostalCode: ${destPostalCode}\n"
        string += "\ttrackPin: ${trackPin}\n"
        string += "\tbarCode: ${barCode}\n"
        string += "\tfromAddress: ${fromAddress}\n"
        string += "\tproductDimension: ${productDimension}\n"
        string += "\tproductWeight: ${productWeight}kg\n"
        string += "\tproductInstruction: ${productInstruction}\n"
        string += "\treference: ${reference}\n"
        string += "}"

        return string
    }

}
