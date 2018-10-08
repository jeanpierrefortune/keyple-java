/*
 * Copyright (c) 2018 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License version 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 */

package org.eclipse.keyple.plugin.remote_se.nse;

import java.util.ArrayList;
import java.util.Map;
import org.eclipse.keyple.plugin.remote_se.transport.*;
import org.eclipse.keyple.plugin.remote_se.transport.json.JsonParser;
import org.eclipse.keyple.seproxy.*;
import org.eclipse.keyple.seproxy.event.ReaderEvent;
import org.eclipse.keyple.seproxy.exception.KeypleReaderException;
import org.eclipse.keyple.seproxy.exception.KeypleReaderNotFoundException;
import org.eclipse.keyple.seproxy.plugin.AbstractObservableReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class NativeSeRemoteService implements NseAPI, RseClient, DtoDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(NativeSeRemoteService.class);

    private TransportNode transportNode;
    private final SeProxyService seProxyService;
    private final NseSessionManager nseSessionManager;

    public NativeSeRemoteService() {
        this.seProxyService = SeProxyService.getInstance();
        this.nseSessionManager = new NseSessionManager();
    }

    @Override
    public TransportDTO onDTO(TransportDTO message) {

        KeypleDTO msg = message.getKeypleDTO();
        TransportDTO out = null;


        logger.debug("onDto {}", KeypleDTOHelper.toJson(msg));

        // receive a response to a reader_connect
        if (msg.getAction().equals(KeypleDTOHelper.READER_CONNECT) && !msg.isRequest()) {
            logger.info("**** RESPONSE - READER_CONNECT ****");
            // parse response
            JsonObject body = JsonParser.getGson().fromJson(msg.getBody(), JsonObject.class);
            String sessionId = msg.getSessionId();
            Integer statusCode = body.get("statusCode").getAsInt();
            String nativeReaderName = body.get("nativeReaderName").getAsString();

            // reader connection was a success
            if (statusCode == 0) {
                try {
                    // observe reader
                    ProxyReader localReader = this.findLocalReader(nativeReaderName);
                    if (localReader instanceof AbstractObservableReader) {
                        logger.debug(
                                "Add NativeSeRemoteService as an observer for native reader {}",
                                localReader.getName());
                        ((AbstractObservableReader) localReader).addObserver(this);
                    }
                    // store sessionId
                    nseSessionManager.addNewSession(sessionId, localReader.getName());
                } catch (KeypleReaderNotFoundException e) {
                    logger.warn(
                            "While receiving a confirmation of Rse connection, local reader was not found");
                }
            } else {
                logger.warn("Receive a error statusCode {} {}", statusCode,
                        KeypleDTOHelper.toJson(msg));
            }

            out =  message.nextTransportDTO(KeypleDTOHelper.NoResponse());

        } else if (msg.getAction().equals(KeypleDTOHelper.READER_TRANSMIT)) {
            logger.info("**** ACTION - READER_TRANSMIT ****");

            SeRequestSet seRequestSet =
                    JsonParser.getGson().fromJson(msg.getBody(), SeRequestSet.class);

            SeResponseSet seResponseSet = null;
            try {
                // execute transmit
                seResponseSet = this.onTransmit(msg.getSessionId(), seRequestSet);
            } catch (KeypleReaderException e) {
                e.printStackTrace();
            }
            // prepare response
            String parseBody = JsonParser.getGson().toJson(seResponseSet, SeResponseSet.class);
            out=  message.nextTransportDTO(
                    new KeypleDTO(msg.getAction(), parseBody, false, msg.getSessionId()));


        } else {
            logger.warn("**** ACTION - UNRECOGNIZED ****");
            logger.warn("Receive uncoregnized message action", msg.getAction());
            out = message.nextTransportDTO(KeypleDTOHelper.NoResponse());
        }

        logger.debug("onDto response to be sent {}", KeypleDTOHelper.toJson(out.getKeypleDTO()));
        return out;


    }

    // RseClient
    @Override
    public void update(ReaderEvent event) {
        logger.info("update Reader Event {}", event.getEventType());

        // retrieve last sessionId known for this reader
        String sessionId = nseSessionManager.getLastSession(event.getReaderName());

        // construct json data
        String data = JsonParser.getGson().toJson(event);

        transportNode.sendDTO(new KeypleDTO(KeypleDTOHelper.READER_EVENT, data, true, sessionId));

    }

    // RseClient
    @Override
    public void connectReader(ProxyReader localReader, Map<String, Object> options) {
        logger.info("connectReader {} {}", localReader.getName(), options);

        JsonObject jsonObject = new JsonObject();
        jsonObject.add("nativeReaderName", new JsonPrimitive(localReader.getName()));

        Boolean isAsync = (Boolean) options.get("isAsync");
        if(isAsync!=null){
            jsonObject.add("isAsync", new JsonPrimitive(isAsync));

        }
        //String transmitUrl = (String) options.get("transmitUrl");

        String data = jsonObject.toString();

        transportNode.sendDTO(new KeypleDTO(KeypleDTOHelper.READER_CONNECT, data, true));

    }


    // NseAPI
    @Override
    public SeResponseSet onTransmit(String sessionId, SeRequestSet req)
            throws KeypleReaderException {
        try {
            ProxyReader reader =
                    findLocalReader(nseSessionManager.findReaderNameBySession(sessionId));
            return reader.transmit(req);
        } catch (KeypleReaderNotFoundException e) {
            e.printStackTrace();
            return new SeResponseSet(new ArrayList<SeResponse>());// todo
        }
    }

    public void bind(TransportNode node) {
        this.transportNode = node;
        node.setDtoDispatcher(this);
    }

    public ProxyReader findLocalReader(String readerName) throws KeypleReaderNotFoundException {
        logger.debug("Find local reader by name {} in {} plugin(s)", readerName,
                seProxyService.getPlugins().size());
        for (ReaderPlugin plugin : seProxyService.getPlugins()) {
            try {
                return plugin.getReader(readerName);
            } catch (KeypleReaderNotFoundException e) {
                // continue
            }
        }
        throw new KeypleReaderNotFoundException("Local Reader not found");
    }


}
