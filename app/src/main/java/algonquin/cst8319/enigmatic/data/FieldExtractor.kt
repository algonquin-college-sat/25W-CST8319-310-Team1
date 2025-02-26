package algonquin.cst8319.enigmatic.data

import android.util.Log
import com.google.mlkit.vision.text.Text.TextBlock
import java.security.KeyStore.TrustedCertificateEntry

class FieldExtractor(
    private var scannedTextBlocks: List<TextBlock>,

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
    private val toAddressHeaderRegex = Regex("TO.*[AÀ]", RegexOption.IGNORE_CASE)
    // note: added the letter 'O' to the matcher for digits, since OCR recognizer often reads a 0 as O
    private val postalCodeRegex = Regex("""[a-zA-Z][O0-9][a-zA-Z][\\ \\-]{0,1}[O0-9][a-zA-Z][O0-9]""")
    private val trackPinRegex = Regex("""\d\d\d\d\s\d\d\d\d\s\d\d\d\d\s\d\d\d\d""")
    private val fromAddressHeaderRegex = Regex("FROM.*DE", RegexOption.IGNORE_CASE)
    private val productDimensionRegex = Regex("""\d*x\d*x\d*cm""")
    private val productWeightRegex = Regex("KG")
    private val productInstructions = listOf("SIGNATURE", "18+ SIGNATURE", "19+ SIGNATURE", "21+ SIGNATURE", "CARD FOR PICKUP", "DELIVER TO PO", "LEAVE AT THE DOOR", "DO NOT SAFE DROP")
    private val referenceRegex = Regex("Ref.*R[eé]f", RegexOption.IGNORE_CASE)

    /**
     * Sole public function for this class, when called it sorts the instance's list of text
     * blocks and then calls each private function relevant to field extraction, finally it
     * returns a list of extracted fields.
     */
    fun extractAllFields() : MutableList<String> {
        if (scannedTextBlocks.isNotEmpty()) {
            cleanScannedText = sortScannedTextBlocks(scannedTextBlocks)

            productType = extractProductType()
            toAddress = extractToAddress()
            destPostalCode = extractDestPostalCode()
            trackPin = extractTrackPin()
            // extractFromAddress() needs to be fixed before enabling, to prevent app crashing
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

    private fun extractProductType() : String {
        var extractedProductType = ""

        for (block in cleanScannedText) {
            for (line in block) {
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

        var foundToAddressHeaderBlockIndex = -1
        var foundToAddressHeaderLineIndex = -1
        for (block in cleanScannedText) {
            for (line in block) {
                if (line.contains(toAddressHeaderRegex)) {
                    foundToAddressHeaderBlockIndex = cleanScannedText.indexOf(block)
                    foundToAddressHeaderLineIndex = block.indexOf(line)
                    Log.d("OCR", "foundToAddressHeaderLineIndex = ${foundToAddressHeaderLineIndex}")
                    break
                }
            }
        }

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
            while (!extractedToAddress.contains(postalCodeRegex)) {
                for (line in cleanScannedText[nextBlockIndex]) {
                    extractedToAddress += "${line}, "
                }
                nextBlockIndex += 1
            }

        }

        extractedFields.add("toAddress: $extractedToAddress")
        return extractedToAddress
    }


    private fun extractDestPostalCode(): String {
        var extractedDestPostalCode = ""

        for (block in cleanScannedText) {
            if (block.size == 1) {
                if (block[0].contains(postalCodeRegex))
                    extractedDestPostalCode = block[0]
            }
        }

        extractedFields.add("destPostalCode: $extractedDestPostalCode")
        return extractedDestPostalCode

    }

    private fun extractTrackPin(): String {
        var extractedTrackPin = ""

        for (block in cleanScannedText) {
            if (block.size == 1) {
                if (block[0].contains(trackPinRegex))
                    extractedTrackPin = block[0]
            }
        }

        extractedFields.add("trackPin: $extractedTrackPin")
        return extractedTrackPin

    }

    // TODO fix this function, makes the app crash (maybe out-of-bound index or infinite looping)
    private fun extractFromAddress() : String {
        var extractedFromAddress = ""

        var foundFromAddressHeaderBlockIndex = -1
        var foundFromAddressHeaderLineIndex = -1
        for (block in cleanScannedText) {
            for (line in block) {
                if (line.contains(fromAddressHeaderRegex)) {
                    foundFromAddressHeaderBlockIndex = cleanScannedText.indexOf(block)
                    foundFromAddressHeaderLineIndex = block.indexOf(line)
                    break
                }
            }
        }

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
                    if (line.contains(productDimensionRegex) || line.contains(productWeightRegex) || line.contains("MANIFEST", true)) {
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

        extractedFields.add("fromAddress: $extractedFromAddress")
        return extractedFromAddress
    }

    private fun extractProductDimension(): String {
        var extractedProductDimension = ""

        for (block in cleanScannedText) {
            if (block.size == 1) {
                if (block[0].contains(productDimensionRegex))
                    extractedProductDimension = block[0]
            }
        }

        extractedFields.add("productDimension: $extractedProductDimension")
        return extractedProductDimension
    }

    private fun extractProductWeight(): String {
        var extractedProductWeight = ""

        var foundProductWeightBlockIndex = -1
        for (block in cleanScannedText) {
            for (line in block) {
                if (line.contains(productWeightRegex)) {
                    foundProductWeightBlockIndex = cleanScannedText.indexOf(block)
                    break
                }
            }
        }

        if(foundProductWeightBlockIndex>=0) {
            // assuming weight value always the first line in that block
            extractedProductWeight = cleanScannedText[foundProductWeightBlockIndex][0]
        }

        extractedFields.add("productWeight: ${extractedProductWeight}kg")
        return extractedProductWeight
    }

    private fun extractProductInstruction(): String {
        var extractedProductInstruction = ""

        for (block in cleanScannedText) {
            for (line in block) {
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

        for (block in cleanScannedText) {
            for (line in block) {
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