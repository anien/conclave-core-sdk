package com.r3.conclave.common.internal.handler

/**
 * A utility String-based enclave host that records all calls into a list.
 */
class StringRecordingHandler : StringHandler() {
    var calls = ArrayList<String>()
    override fun onReceive(sender: StringSender, string: String) {
        calls.add(string)
    }
}