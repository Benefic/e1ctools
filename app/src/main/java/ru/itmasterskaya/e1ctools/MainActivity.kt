package ru.itmasterskaya.e1ctools

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_main.*
import ru.itmasterskaya.e1ctools.records.NdefMessageParser.parse


class MainActivity : AppCompatActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var pendingIntent: PendingIntent

    @SuppressLint("UnspecifiedImmutableFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        if (nfcAdapter == null) {
            Toast.makeText(this, "Не удалось подключить адаптер NFC", Toast.LENGTH_SHORT).show()
            setResult(NFC_STOP_READ)
            finish()
            return
        }
        pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, this.javaClass)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0
        )
        Glide.with(this).load(R.drawable.loading).into(image)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) {
            setIntent(intent)
            resolveIntent(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        enableForegroundDispatchSystem()
    }

    override fun onPause() {
        super.onPause()
        disableForegroundDispatchSystem()
    }

    private fun enableForegroundDispatchSystem() {
        if (nfcAdapter != null) {
            if (!nfcAdapter!!.isEnabled)
                showWirelessSettings()
            nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
        }
    }

    private fun disableForegroundDispatchSystem() {
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onBackPressed() {
        setResult(NFC_STOP_READ)
        finish()
    }

    private fun showWirelessSettings() {
        Toast.makeText(this, "Необходимо включить NFC", Toast.LENGTH_SHORT).show()
        val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
        startActivity(intent)
    }

    private fun resolveIntent(intent: Intent) {
        val action = intent.action
        if (NfcAdapter.ACTION_TAG_DISCOVERED == action || NfcAdapter.ACTION_TECH_DISCOVERED == action || NfcAdapter.ACTION_NDEF_DISCOVERED == action) {
            val rawMsgs =
                intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            val msgs: Array<NdefMessage?>
            val resultIntent = Intent()
            setResult(NFC_READ_RESULT, resultIntent)
            if (rawMsgs != null) {
                msgs = arrayOfNulls(rawMsgs.size)
                for (i in rawMsgs.indices) {
                    msgs[i] = rawMsgs[i] as NdefMessage
                }
                resultIntent.putExtra(NFC_EXTRA_MESSAGES_KEY, parseMessages(msgs))
            }
            val keyID = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID)?.let { parseID(it) }
            resultIntent.putExtra(
                NFC_EXTRA_KEY_ID,
                keyID
            )
            finish()
        }
    }

    private fun parseID(inArray: ByteArray): String {
        var i: Int
        var input: Int
        val hex = arrayOf(
            "0",
            "1",
            "2",
            "3",
            "4",
            "5",
            "6",
            "7",
            "8",
            "9",
            "A",
            "B",
            "C",
            "D",
            "E",
            "F"
        )
        var out = ""


        var j = 0
        while (j < inArray.size) {
            input = inArray[j].toInt() and 0xff
            i = input shr 4 and 0x0f
            out += hex[i]
            i = input and 0x0f
            out += hex[i]
            ++j
        }

        return out
    }

    private fun parseMessages(msgs: Array<NdefMessage?>?): String {
        if (msgs == null || msgs.isEmpty()) return ""
        val builder = java.lang.StringBuilder()
        val records =
            parse(msgs[0]!!)
        val size = records.size
        for (i in 0 until size) {
            val record = records[i]
            val str = record.str()
            builder.append(str).append("\n")
        }
        return builder.toString()
    }

    companion object {
        private const val NFC_READ_RESULT = 23
        private const val NFC_STOP_READ = 24
        private const val NFC_EXTRA_MESSAGES_KEY = "nfc_extra_messages_key"
        private const val NFC_EXTRA_KEY_ID = "nfc_extra_key_id"
    }
}

