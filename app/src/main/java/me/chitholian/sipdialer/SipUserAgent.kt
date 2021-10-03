package me.chitholian.sipdialer

import org.pjsip.pjsua2.Endpoint
import org.pjsip.pjsua2.EpConfig
import org.pjsip.pjsua2.TransportConfig
import org.pjsip.pjsua2.pjsip_transport_type_e.PJSIP_TRANSPORT_UDP

class SipUserAgent(val app: TheApp) {
    private lateinit var tpConfig: TransportConfig
    private lateinit var epConfig: EpConfig
    private lateinit var endpoint: Endpoint
    private var initialized = false

    fun initialize() {
        if (initialized) return
        endpoint = Endpoint()
        endpoint.libCreate()
        epConfig = EpConfig()
        endpoint.libInit(epConfig)

        tpConfig = TransportConfig()
        // TODO: Handle exception bellow
        endpoint.transportCreate(PJSIP_TRANSPORT_UDP, tpConfig)

        endpoint.libStart()
        initialized = true
    }

    fun destroy() {
        if (initialized) {
            endpoint.libDestroy()
            endpoint.delete()
            epConfig.delete()
            tpConfig.delete()
            initialized = false
        }
    }
}
