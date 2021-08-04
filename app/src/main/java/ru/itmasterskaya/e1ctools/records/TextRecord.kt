package ru.itmasterskaya.e1ctools.records

import android.nfc.NdefRecord
import androidx.core.util.Preconditions
import java.io.UnsupportedEncodingException
import java.util.*
import kotlin.experimental.and


class TextRecord(languageCode: String?, text: String?) :
    ParsedNdefRecord {
    /**
     * Returns the ISO/IANA language code associated with this text element.
     */
    /** ISO/IANA language code  */
    val languageCode: String
    val text: String

    override fun str(): String? {
        return text
    }

    companion object {
        // TODO: deal with text fields which span multiple NdefRecords
        fun parse(record: NdefRecord): TextRecord {
            Preconditions.checkArgument(record.tnf == NdefRecord.TNF_WELL_KNOWN)
            Preconditions.checkArgument(Arrays.equals(record.type, NdefRecord.RTD_TEXT))
            return try {
                val payload = record.payload
                /*
                     * payload[0] contains the "Status Byte Encodings" field, per the
                     * NFC Forum "Text Record Type Definition" section 3.2.1.
                     *
                     * bit7 is the Text Encoding Field.
                     *
                     * if (Bit_7 == 0): The text is encoded in UTF-8 if (Bit_7 == 1):
                     * The text is encoded in UTF16
                     *
                     * Bit_6 is reserved for future use and must be set to zero.
                     *
                     * Bits 5 to 0 are the length of the IANA language code.
                     */
                val textEncoding =
                    if ((payload[0] and 128.toByte()).toInt() == 0) "UTF-8" else "UTF-16"
                val languageCodeLength: Int = (payload[0] and 63).toInt()
                val languageCode = String(payload, 1, languageCodeLength, charset("US-ASCII"))
                val text = String(
                    payload, languageCodeLength + 1,
                    payload.size - languageCodeLength - 1, charset(textEncoding)
                )
                TextRecord(languageCode, text)
            } catch (e: UnsupportedEncodingException) { // should never happen unless we get a malformed tag.
                throw IllegalArgumentException(e)
            }
        }

        fun isText(record: NdefRecord): Boolean {
            return try {
                parse(record)
                true
            } catch (e: IllegalArgumentException) {
                false
            }
        }
    }

    init {
        this.languageCode = Preconditions.checkNotNull(languageCode)
        this.text = Preconditions.checkNotNull(text)
    }
}