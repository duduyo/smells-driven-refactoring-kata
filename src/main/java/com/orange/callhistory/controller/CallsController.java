package com.orange.callhistory.controller;

import static com.orange.callhistory.service.CallEventStatus.valueOf;

import java.util.Optional;
import java.util.UUID;

import com.orange.callhistory.controller.dto.CallDtoR;
import com.orange.callhistory.controller.dto.CallDtoW;
import com.orange.callhistory.controller.dto.CallEventDto;
import com.orange.callhistory.service.Call;
import com.orange.callhistory.service.CallEvent;
import com.orange.callhistory.service.CallService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CallsController implements CallsApi {

    private CallService callService;

    @Autowired
    public CallsController(CallService callService) {
        this.callService = callService;
    }

    @Override
    public ResponseEntity<Void> putCalls(@PathVariable String callId, @RequestBody CallDtoW callDtoW) {
        Call call = new Call(callId, callDtoW.getParticipantTelNumber(), callDtoW.getParticipantAnnouncement(), callDtoW.getParticipantRingingTimeout());

// TODO Smell : look like a business rule, in the controller
        Optional<Call> existingCall = callService.findCall(callId);
        if (existingCall.isPresent()) {
            throw new CallException("You cannot create a call with this callId : a call with id=" + callId + " already exists");
        }
        else {
            callService.save(call);
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    public ResponseEntity<Void> postCallEvents(@PathVariable("callId") String callId, @RequestBody CallEventDto callEventDto) {
        CallEvent callEvent = new CallEvent(UUID.randomUUID().toString(), valueOf(callEventDto.getStatus().toString()), callEventDto.getTimestamp());

// TODO Smell : look like a business rule, in a controller
        Optional<Call> call = callService.findCall(callId);
        if (call.isPresent()) {
            call.get().addEvent(callEvent);
            callService.save(call.get());
        }
        else {
            throw new CallException("Cannot add event to call with callId " + callId + " : it does not exist");
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    public ResponseEntity<CallDtoR> getCall(@PathVariable("callId") String callId) {

        Optional<Call> call = callService.findCall(callId);
        CallDtoR callDtoR = call.map(this::mapCallToCallDto).orElse(null);
        return new ResponseEntity<>(callDtoR, HttpStatus.OK);
    }


    private CallDtoR mapCallToCallDto(Call call) {
        CallDtoR callDtoR = new CallDtoR();
        callDtoR.setCallId(call.getCallId());
        callDtoR.setParticipantTelNumber(call.getParticipantTelNumber());
        callDtoR.setParticipantAnnouncement(call.getParticipantAnnouncement());
        callDtoR.setParticipantRingingTimeout(call.getParticipantRingingTimeout());
// TODO Smell : business rule in the middle of mapping logic
        // the geozone is calculated with the phonenumber prefix
        String participantGeoZone;
        if (call.getParticipantTelNumber().startsWith("+33")) {
            participantGeoZone = "FR";
        }
        else if (call.getParticipantTelNumber().startsWith("+34")) {
            participantGeoZone = "SP";
        }
        else {
            participantGeoZone = "OTHER_COUNTRY";
        }
        callDtoR.setParticipantGeoZone(CallDtoR.ParticipantGeoZoneEnum.fromValue(participantGeoZone));
        callDtoR.setConnectionDate(call.getConnectionDate());
        callDtoR.setTerminationDate(call.getTerminationDate());
        return callDtoR;
    }

}