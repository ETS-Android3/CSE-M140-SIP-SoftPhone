package me.chitholian.sipdialer

import org.pjsip.pjsua2.*
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
        Runtime.getRuntime().gc()
        if (initialized) {
            // TODO: Handle runtime.
            endpoint.libDestroy()
            endpoint.delete()
            epConfig.delete()
            tpConfig.delete()
            initialized = false
        }
    }

    fun startAudio(audioMedia: AudioMedia) {
        if (initialized) {
            // TODO: Handle exception.
            endpoint.audDevManager().captureDevMedia.startTransmit(audioMedia)
            audioMedia.startTransmit(endpoint.audDevManager().playbackDevMedia)
        }
    }

    fun stopAudio(audioMedia: AudioMedia) {
        if (initialized) {
            // TODO: Handle exception.
            audioMedia.stopTransmit(endpoint.audDevManager().playbackDevMedia)
            endpoint.audDevManager().captureDevMedia.stopTransmit(audioMedia)
        }
    }

    fun setSpeakerMode(turnOn: Boolean) {
        if (initialized) {
            if (turnOn) {
                endpoint.audDevManager()
                    .setOutputRoute(pjmedia_aud_dev_route.PJMEDIA_AUD_DEV_ROUTE_LOUDSPEAKER, false)
                endpoint.audDevManager().refreshDevs()
            } else {
                endpoint.audDevManager()
                    .setOutputRoute(pjmedia_aud_dev_route.PJMEDIA_AUD_DEV_ROUTE_DEFAULT, true)
            }
        }
    }
}
