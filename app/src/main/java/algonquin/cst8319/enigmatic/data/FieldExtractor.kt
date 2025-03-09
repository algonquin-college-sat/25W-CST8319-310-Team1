package algonquin.cst8319.enigmatic.data

import android.util.Log
import com.google.mlkit.vision.text.Text.TextBlock

class FieldExtractor(
    private var scannedTextBlocks: List<TextBlock>
) {

    // String variables to store the extract field, matching JSON field requirements
    private lateinit var productType: String
    private lateinit var toAddress: String
    private lateinit var destPostalCode: String
    private lateinit var trackPin: String
    private lateinit var fromAddress: String
    private lateinit var productDimension: String
    private lateinit var productWeight: String
    private lateinit var productInstruction: String
    private lateinit var reference: String

    // lists to store sorted & extracted Strings from instance's list of text blocks
    private var cleanScannedText = mutableListOf<MutableList<String>>()
    private var extractedFields = mutableListOf<String>()

    // lists of keywords or Regex useful to find required fields when parsing the scanned label text
    // these can be tweaked as required to improve field retrieval
    private val productTypes = listOf("Priority", "Regular Parcel", "Xpresspost", "Expedited Parcel")
    private val toAddressHeaderRegex = Regex("TO.*[AÀÅ]", RegexOption.IGNORE_CASE)
    // note: added the letter 'O' to the matcher for digits, since OCR recognizer often reads a 0 as O
    private val postalCodeRegex = Regex("""[a-zA-Z][O0-9][a-zA-Z][\\ \\-]{0,1}[O0-9][a-zA-Z][O0-9]""")
    private val trackPinRegex = Regex("""\d\d\d\d\s\d\d\d\d\s\d\d\d\d\s\d\d\d\d""")
    private val fromAddressHeaderRegex = Regex("FROM.*DE", RegexOption.IGNORE_CASE)
    private val productDimensionRegex = Regex("""\d*x\d*x\d*cm""")
    private val productWeightRegex = Regex("KG")
    private val productWeightValueRegex = Regex("""\d*[.]\d\d\d""")
    private val productInstructions = listOf("SIGNATURE", "18+ SIGNATURE", "19+ SIGNATURE", "21+ SIGNATURE", "CARD FOR PICKUP", "DELIVER TO PO", "LEAVE AT THE DOOR", "DO NOT SAFE DROP")
    private val referenceRegex = Regex("Ref.*R[eé]f", RegexOption.IGNORE_CASE)

    // variable holding the index of the found fields
    private var foundProductTypeIndex = -1
    private var foundToAddressHeaderBlockIndex = -1
    private var foundToAddressHeaderLineIndex = -1
    private var foundPostalCodeIndex = -1
    private var foundTrackPinIndex = -1
    private var foundFromAddressHeaderBlockIndex = -1
    private var foundFromAddressHeaderLineIndex = -1
    private var foundProductDimensionIndex = -1
    private var foundProductWeightIndex = -1
    private var foundProductInstructionIndex = -1
    private var foundReferenceIndex = -1


    // All getters for private fields
    fun getProductType(): String {return productType}
    fun getToAddress(): String {return toAddress}
    fun getDestPostalCode(): String {return destPostalCode}
    fun getTrackPin(): String {return trackPin}
    fun getFromAddress(): String {return fromAddress}
    fun getProductDimension(): String {return productDimension}
    fun getProductWeight(): String {return productWeight}
    fun getProductInstruction(): String {return productInstruction}
    fun getReference(): String {return reference}


    /**
     * Sole public function for this class, when called it sorts the instance's list of text
     * blocks and then calls each private function relevant to field extraction, finally it
     * returns a list of extracted fields.
     */
    fun extractAllFields() : MutableList<String> {
        if (scannedTextBlocks.isNotEmpty()) {
            // sort all text blocks
            cleanScannedText = sortScannedTextBlocks(scannedTextBlocks)

            // parse all text blocks to find each field reference index
            findAllFieldPositions()

            // extract each field using their found reference position
            productType = extractProductType()
            toAddress = extractToAddress()
            destPostalCode = extractDestPostalCode()
            trackPin = extractTrackPin()
            fromAddress = extractFromAddress()
            productDimension = extractProductDimension()
            productWeight = extractProductWeight()
            productInstruction = extractProductInstruction()
            reference = extractReference()

        }

        return extractedFields
    }

    private fun sortScannedTextBlocks(scannedTextBlocks: List<TextBlock>) : MutableList<MutableList<String>> {
        val sortedScannedTextStrings = mutableListOf<MutableList<String>>()

        // sorting all text blocks by their vertical position, i.e. boundingBox.top value
        val sortedTextBlocks = scannedTextBlocks.sortedWith(compareBy { it.boundingBox?.top })

        // iterating through each block and reordering lines by their vertical position
        for (block in sortedTextBlocks) {
            val boundingBox = block.boundingBox

            val sortedBlock = sortBlockLines(block)

            sortedScannedTextStrings.add(sortedBlock)

            // useful logging for debugging only
            if (boundingBox != null) {
                Log.d(
                    "OCR", "Sorted text block: ${sortedBlock}\n" +
                            "bounding box: Left:${boundingBox.left}, Top:${boundingBox.top}, " +
                            "Right: ${boundingBox.right}, Bottom: ${boundingBox.bottom}"
                )
            }
        }

        return sortedScannedTextStrings
    }


    private fun sortBlockLines(textBlock: TextBlock) : MutableList<String> {
        val sortedBlock = mutableListOf<String>()

        // sorting lines
        val sortedTextLines = textBlock.lines.sortedBy { it.boundingBox?.top }

        for (line in sortedTextLines) {
            sortedBlock.add(line.text)
        }

        return sortedBlock
    }

    private fun findAllFieldPositions() {
        // parsing through all text blocks to find positions of header or actual field
        // instead of parsing through all blocks in each field extraction methods
        for (block in cleanScannedText) {
            for (line in block) {
                // some fields are expected to be in single-line blocks
                if (block.size == 1) {
                    if (foundPostalCodeIndex < 0 && line.contains(postalCodeRegex)) {
                        foundPostalCodeIndex = cleanScannedText.indexOf(block)
                    }
                    else if (foundTrackPinIndex < 0 && line.contains(trackPinRegex)) {
                        foundTrackPinIndex = cleanScannedText.indexOf(block)
                    }
                    else if (foundProductDimensionIndex < 0 && line.contains(productDimensionRegex)) {
                        foundProductDimensionIndex = cleanScannedText.indexOf(block)
                    }
                }

                if (foundProductTypeIndex < 0) {
                    for (productType in productTypes) {
                        if (line.contains(productType)) {
                            foundProductTypeIndex = cleanScannedText.indexOf(block)
                            break
                        }
                    }
                }

                if (foundToAddressHeaderBlockIndex < 0 && line.contains(toAddressHeaderRegex)) {
                    foundToAddressHeaderBlockIndex = cleanScannedText.indexOf(block)
                    foundToAddressHeaderLineIndex = block.indexOf(line)
                }

                if (foundFromAddressHeaderBlockIndex < 0 && line.contains(fromAddressHeaderRegex)) {
                    foundFromAddressHeaderBlockIndex = cleanScannedText.indexOf(block)
                    foundFromAddressHeaderLineIndex = block.indexOf(line)
                }

                if (foundProductWeightIndex < 0 && line.contains(productWeightRegex)) {
                    foundProductWeightIndex = cleanScannedText.indexOf(block)
                }

                if (foundProductInstructionIndex < 0) {
                    for (instruction in productInstructions) {
                        if (line.equals(instruction, true)) {
                            foundProductInstructionIndex = cleanScannedText.indexOf(block)
                            break
                        }
                    }
                }

                if (foundReferenceIndex < 0 && line.contains(referenceRegex)) {
                    foundReferenceIndex = cleanScannedText.indexOf(block)
                }
            }
        }
    }

    private fun extractProductType() : String {
        var extractedProductType = ""

        if (foundProductTypeIndex >= 0) {
            for (line in cleanScannedText[foundProductTypeIndex]) {
                for (productType in productTypes) {
                    if (line.contains(productType)) {
                        extractedProductType = productType
                        break
                    }
                }
            }
        }

        extractedFields.add("productType: $extractedProductType")
        return extractedProductType
    }

    private fun extractToAddress() : String {
        var extractedToAddress = ""

        if(foundToAddressHeaderBlockIndex>=0) {
            // making sure we include 'to address' details if embedded in same block as
            // the header, by extracting any remaining lines the header block
            if ((foundToAddressHeaderLineIndex + 1) < cleanScannedText[foundToAddressHeaderBlockIndex].size) {
                for (lineIndex in (foundToAddressHeaderLineIndex + 1)..<cleanScannedText[foundToAddressHeaderBlockIndex].size) {
                    extractedToAddress += "${cleanScannedText[foundToAddressHeaderBlockIndex][lineIndex]}, "
                }
            }

            var nextBlockIndex = foundToAddressHeaderBlockIndex + 1

            // continue until postal code was found so we get complete address
            var blockIsAddressRelated = true
            while (nextBlockIndex < cleanScannedText.size &&
                    !extractedToAddress.contains(postalCodeRegex) &&
                    blockIsAddressRelated) {
                for (line in cleanScannedText[nextBlockIndex]) {
                    // sometimes postal code in address is not recognized properly,
                    // i.e. digit read as character, so loop continues through next blocks
                    // so adding check to make sure the product instruction (ie '18+ SIGNATURE')
                    // which is the next block after 'to address' does not slip into the extracted address
                    for (instruction in productInstructions) {
                        if (line.equals(instruction, true)) {
                            blockIsAddressRelated = false
                            break
                        }
                    }
                    if (blockIsAddressRelated) {
                        extractedToAddress += "${line}, "
                    }
                    else {
                        break
                    }
                }
                nextBlockIndex += 1
            }

        }

        // clean up by removing last comma
        if (extractedToAddress.endsWith(", ")) {
            extractedToAddress = extractedToAddress.substringBeforeLast(",")
        }
        extractedFields.add("toAddress: $extractedToAddress")
        return extractedToAddress
    }


    private fun extractDestPostalCode(): String {
        var extractedDestPostalCode = ""

        if (foundPostalCodeIndex >= 0) {
            extractedDestPostalCode = cleanScannedText[foundPostalCodeIndex][0]
        }

        extractedFields.add("destPostalCode: $extractedDestPostalCode")
        return extractedDestPostalCode

    }

    private fun extractTrackPin(): String {
        var extractedTrackPin = ""

        if (foundTrackPinIndex >= 0) {
            val trackPinBlock = cleanScannedText[foundTrackPinIndex]
            // sometimes found block will be the 'PIN/NIP:' near bottom of label
            if (trackPinBlock[0].contains(":")) {
                extractedTrackPin = trackPinBlock[0].substringAfterLast(":")
            }
            else {
                extractedTrackPin = trackPinBlock[0]
            }
        }

        extractedFields.add("trackPin: $extractedTrackPin")
        return extractedTrackPin

    }

    private fun extractFromAddress() : String {
        var extractedFromAddress = ""

        if(foundFromAddressHeaderBlockIndex>=0) {
            // making sure we include 'from address' details if embedded in same block as
            // the header, by extracting any remaining lines the header block
            if ((foundFromAddressHeaderBlockIndex + 1) < cleanScannedText[foundFromAddressHeaderBlockIndex].size) {
                for (lineIndex in (foundFromAddressHeaderLineIndex + 1)..<(cleanScannedText[foundFromAddressHeaderBlockIndex].size)) {
                    extractedFromAddress += "${cleanScannedText[foundFromAddressHeaderBlockIndex][lineIndex]}, "
                }
            }

            var nextBlockIndex = foundFromAddressHeaderBlockIndex + 1

            // continue until postal code was found so we get complete address
            while (nextBlockIndex < cleanScannedText.size && !extractedFromAddress.contains(postalCodeRegex)) {
                // using a hack here: there is often other text blocks in between 'from' header and
                // the 'address' text block, i.e. dimension or weight or MANIFEST, so skipping those blocks
                // is required
                var isAddressRelated = true
                for (line in cleanScannedText[nextBlockIndex]) {
                    if (line.contains(productDimensionRegex) ||
                        line.contains(productWeightRegex) ||
                        line.contains(productWeightValueRegex) ||
                        line.contains("MANIFEST", true)) {
                        isAddressRelated = false
                        break
                    }
                }
                if (isAddressRelated) {
                    for (line in cleanScannedText[nextBlockIndex]) {
                        extractedFromAddress += "${line}, "
                    }
                }

                nextBlockIndex += 1
            }

        }

        // clean up by removing last comma
        if (extractedFromAddress.endsWith(", ")) {
            extractedFromAddress = extractedFromAddress.substringBeforeLast(",")
        }
        extractedFields.add("fromAddress: $extractedFromAddress")
        return extractedFromAddress
    }

    private fun extractProductDimension(): String {
        var extractedProductDimension = ""

        if (foundProductDimensionIndex >= 0) {
            extractedProductDimension = cleanScannedText[foundProductDimensionIndex][0]
        }

        extractedFields.add("productDimension: $extractedProductDimension")
        return extractedProductDimension
    }

    private fun extractProductWeight(): String {
        var extractedProductWeight = ""

        if(foundProductWeightIndex>=0) {
            val productWeightBlock = cleanScannedText[foundProductWeightIndex]
            // first line of block is usually weight value, but sometimes the value is
            // in a previous block, so checking that first line does not contain 'kg'
            if (!productWeightBlock[0].contains(productWeightRegex)) {
                extractedProductWeight = productWeightBlock[0]
            }
            // check previous block and make sure it's not the product dimensions or 'from/to' header
            else if (!cleanScannedText[foundProductWeightIndex - 1][0].contains(productDimensionRegex) &&
                        !cleanScannedText[foundProductWeightIndex - 1][0].contains(fromAddressHeaderRegex)){
                extractedProductWeight = cleanScannedText[foundProductWeightIndex - 1][0]

            }
            // check 2nd previous block and make sure it's not the product dimensions or 'from/to' header
            else if (!cleanScannedText[foundProductWeightIndex - 2][0].contains(productDimensionRegex) &&
                !cleanScannedText[foundProductWeightIndex - 2][0].contains(fromAddressHeaderRegex)){
                extractedProductWeight = cleanScannedText[foundProductWeightIndex - 2][0]

            }
            // lastly, assume that the product weight value is the 3rd previous block
            else {
                extractedProductWeight = cleanScannedText[foundProductWeightIndex - 3][0]
            }
        }

        extractedFields.add("productWeight: ${extractedProductWeight}kg")
        return extractedProductWeight
    }

    private fun extractProductInstruction(): String {
        var extractedProductInstruction = ""

        if (foundProductInstructionIndex >= 0) {
            for (line in cleanScannedText[foundProductInstructionIndex]) {
                for (instruction in productInstructions) {
                    if (line.equals(instruction, true)) {
                        extractedProductInstruction = instruction
                        break
                    }
                }
            }
        }

        extractedFields.add("productInstruction: ${extractedProductInstruction}")
        return extractedProductInstruction
    }

    private fun extractReference(): String {
        var extractedReference = ""

        if (foundReferenceIndex >= 0) {
            for (line in cleanScannedText[foundReferenceIndex]) {
                if (line.contains(referenceRegex)) {
                    extractedReference = line.substringAfterLast(":")
                    break
                }
            }
        }

        extractedFields.add("reference: ${extractedReference}")
        return extractedReference
    }

}