package com.example.service

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.data.repository.CallRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoSource
import org.webrtc.VideoTrack

enum class CallUiState {
    IDLE,
    RINGING_OUTGOING,
    RINGING_INCOMING,
    CONNECTED,
    ENDED
}

class WebRtcCallManager(
    private val context: Context,
    private val callRepository: CallRepository
) {
    private val scope = CoroutineScope(Dispatchers.Main)
    val rootEglBase: EglBase by lazy { EglBase.create() }

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null
    private var videoCapturer: CameraVideoCapturer? = null
    private var videoSource: VideoSource? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _callState = MutableStateFlow(CallUiState.IDLE)
    val callState: StateFlow<CallUiState> = _callState.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(false)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    private val _isCameraOn = MutableStateFlow(true)
    val isCameraOn: StateFlow<Boolean> = _isCameraOn.asStateFlow()

    private val _callDurationSeconds = MutableStateFlow(0)
    val callDurationSeconds: StateFlow<Int> = _callDurationSeconds.asStateFlow()

    private var durationTimerJob: Job? = null
    private var activeCallId: String? = null
    private var isCaller: Boolean = false

    init {
        initPeerConnectionFactory()
    }

    private fun initPeerConnectionFactory() {
        try {
            val options = PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
            PeerConnectionFactory.initialize(options)

            val encoderFactory = DefaultVideoEncoderFactory(rootEglBase.eglBaseContext, true, true)
            val decoderFactory = DefaultVideoDecoderFactory(rootEglBase.eglBaseContext)

            peerConnectionFactory = PeerConnectionFactory.builder()
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .setOptions(PeerConnectionFactory.Options())
                .createPeerConnectionFactory()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startOutgoingCall(callId: String, isVideo: Boolean) {
        activeCallId = callId
        this.isCaller = true
        _callState.value = CallUiState.RINGING_OUTGOING
        setupMediaAndPeer(callId, isVideo = isVideo, isCaller = true)
        listenToCallSession(callId)
    }

    fun acceptIncomingCall(callId: String, isVideo: Boolean) {
        activeCallId = callId
        this.isCaller = false
        callRepository.acceptCall(callId)
        _callState.value = CallUiState.CONNECTED
        setupMediaAndPeer(callId, isVideo = isVideo, isCaller = false)
        listenToCallSession(callId)
        startDurationTimer()
    }

    fun declineIncomingCall(callId: String) {
        callRepository.declineCall(callId)
        endCurrentCall()
    }

    fun triggerSingleVibrationPulse() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(400)
                }
            }
        } catch (_: Exception) {}
    }

    private fun setupMediaAndPeer(callId: String, isVideo: Boolean, isCaller: Boolean) {
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
        )

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        val factory = peerConnectionFactory ?: return

        // Audio Source & Track
        val audioConstraints = MediaConstraints()
        val audioSource = factory.createAudioSource(audioConstraints)
        localAudioTrack = factory.createAudioTrack("ARDAMSa0", audioSource)

        // Video Source & Track if video call
        if (isVideo) {
            setupVideoCapturer()
        }

        peerConnection = factory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                if (state == PeerConnection.IceConnectionState.CONNECTED) {
                    _callState.value = CallUiState.CONNECTED
                    startDurationTimer()
                } else if (state == PeerConnection.IceConnectionState.DISCONNECTED ||
                    state == PeerConnection.IceConnectionState.FAILED ||
                    state == PeerConnection.IceConnectionState.CLOSED
                ) {
                    endCurrentCall()
                }
            }
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidate(candidate: IceCandidate?) {
                if (candidate != null) {
                    val candidateJson = "${candidate.sdpMid}|${candidate.sdpMLineIndex}|${candidate.sdp}"
                    callRepository.sendIceCandidate(callId, isCaller, candidateJson)
                }
            }
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onAddStream(stream: org.webrtc.MediaStream?) {}
            override fun onRemoveStream(stream: org.webrtc.MediaStream?) {}
            override fun onDataChannel(dc: org.webrtc.DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out org.webrtc.MediaStream>?) {}
        })

        // Add local tracks
        localAudioTrack?.let { peerConnection?.addTrack(it) }
        localVideoTrack?.let { peerConnection?.addTrack(it) }

        // Start WebRTC SDP negotiation
        if (isCaller) {
            createOffer(callId)
        }

        // Listen for candidates from the other peer
        scope.launch {
            callRepository.observeRemoteCandidates(callId, isCaller).collect { candidateStr ->
                val parts = candidateStr.split("|")
                if (parts.size >= 3) {
                    val mid = parts[0]
                    val lineIndex = parts[1].toIntOrNull() ?: 0
                    val sdp = parts[2]
                    peerConnection?.addIceCandidate(IceCandidate(mid, lineIndex, sdp))
                }
            }
        }
    }

    private fun setupVideoCapturer() {
        val enumerator = Camera2Enumerator(context)
        val deviceNames = enumerator.deviceNames
        var frontCameraName: String? = null
        for (name in deviceNames) {
            if (enumerator.isFrontFacing(name)) {
                frontCameraName = name
                break
            }
        }
        val targetName = frontCameraName ?: deviceNames.firstOrNull() ?: return

        videoCapturer = enumerator.createCapturer(targetName, null)
        surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", rootEglBase.eglBaseContext)
        val factory = peerConnectionFactory ?: return
        videoSource = factory.createVideoSource(videoCapturer!!.isScreencast)
        videoCapturer!!.initialize(surfaceTextureHelper, context, videoSource!!.capturerObserver)
        videoCapturer!!.startCapture(640, 480, 30)

        localVideoTrack = factory.createVideoTrack("ARDAMSv0", videoSource)
    }

    private fun createOffer(callId: String) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }

        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                if (desc != null) {
                    peerConnection?.setLocalDescription(this, desc)
                    callRepository.sendSdpOffer(callId, desc.description)
                }
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(err: String?) {}
            override fun onSetFailure(err: String?) {}
        }, constraints)
    }

    private fun createAnswer(callId: String, offerSdp: String) {
        val offerDesc = SessionDescription(SessionDescription.Type.OFFER, offerSdp)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                val constraints = MediaConstraints()
                peerConnection?.createAnswer(object : SdpObserver {
                    override fun onCreateSuccess(desc: SessionDescription?) {
                        if (desc != null) {
                            peerConnection?.setLocalDescription(this, desc)
                            callRepository.sendSdpAnswer(callId, desc.description)
                        }
                    }
                    override fun onSetSuccess() {}
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(p0: String?) {}
                }, constraints)
            }
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {}
        }, offerDesc)
    }

    private fun listenToCallSession(callId: String) {
        scope.launch {
            callRepository.observeCallSession(callId).collect { session ->
                if (session == null) return@collect

                when (session.status) {
                    "accepted" -> {
                        if (_callState.value != CallUiState.CONNECTED) {
                            _callState.value = CallUiState.CONNECTED
                            startDurationTimer()
                        }
                        if (isCaller && session.sdpAnswer != null && peerConnection?.remoteDescription == null) {
                            val answerDesc = SessionDescription(SessionDescription.Type.ANSWER, session.sdpAnswer)
                            peerConnection?.setRemoteDescription(object : SdpObserver {
                                override fun onCreateSuccess(p0: SessionDescription?) {}
                                override fun onSetSuccess() {}
                                override fun onCreateFailure(p0: String?) {}
                                override fun onSetFailure(p0: String?) {}
                            }, answerDesc)
                        }
                    }
                    "ringing" -> {
                        if (!isCaller && session.sdpOffer != null && peerConnection?.remoteDescription == null) {
                            createAnswer(callId, session.sdpOffer)
                        }
                    }
                    "declined", "ended" -> {
                        endCurrentCall()
                    }
                }
            }
        }
    }

    private fun startDurationTimer() {
        durationTimerJob?.cancel()
        _callDurationSeconds.value = 0
        durationTimerJob = scope.launch {
            while (isActive) {
                delay(1000)
                _callDurationSeconds.value += 1
            }
        }
    }

    fun toggleMute() {
        val newMute = !_isMuted.value
        _isMuted.value = newMute
        localAudioTrack?.setEnabled(!newMute)
    }

    fun toggleSpeaker() {
        val newSpeaker = !_isSpeakerOn.value
        _isSpeakerOn.value = newSpeaker
        audioManager.isSpeakerphoneOn = newSpeaker
    }

    fun toggleCamera() {
        val newCam = !_isCameraOn.value
        _isCameraOn.value = newCam
        localVideoTrack?.setEnabled(newCam)
    }

    fun switchCamera() {
        videoCapturer?.switchCamera(null)
    }

    fun endCurrentCall() {
        durationTimerJob?.cancel()
        durationTimerJob = null
        activeCallId?.let { callRepository.endCall(it) }

        try {
            videoCapturer?.stopCapture()
            videoCapturer?.dispose()
        } catch (_: Exception) {}
        videoCapturer = null

        surfaceTextureHelper?.dispose()
        surfaceTextureHelper = null

        videoSource?.dispose()
        videoSource = null

        localAudioTrack?.dispose()
        localAudioTrack = null
        localVideoTrack?.dispose()
        localVideoTrack = null

        try {
            peerConnection?.close()
            peerConnection?.dispose()
        } catch (_: Exception) {}
        peerConnection = null

        _callState.value = CallUiState.ENDED
        activeCallId = null
    }

    fun resetState() {
        _callState.value = CallUiState.IDLE
        _callDurationSeconds.value = 0
        _isMuted.value = false
        _isSpeakerOn.value = false
        _isCameraOn.value = true
    }
}
