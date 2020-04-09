/********************************************************************************
 * Copyright (c) 2019 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information regarding copyright
 * ownership.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.keyple.calypso.command.po.parser.security;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.keyple.calypso.command.po.AbstractPoResponseParser;
import org.eclipse.keyple.calypso.command.po.builder.security.ChangeKeyCmdBuild;
import org.eclipse.keyple.calypso.command.po.exception.*;
import org.eclipse.keyple.core.command.AbstractApduResponseParser;
import org.eclipse.keyple.core.seproxy.message.ApduResponse;

public class ChangeKeyRespPars extends AbstractPoResponseParser {
    private static final Map<Integer, StatusProperties> STATUS_TABLE;

    static {
        Map<Integer, StatusProperties> m =
                new HashMap<Integer, StatusProperties>(AbstractApduResponseParser.STATUS_TABLE);
        m.put(0x6700, new StatusProperties(false,
                "Lc value not supported (not 04h, 10h, 18h, 20h).", KeyplePoIllegalParameterException.class));
        m.put(0x6900, new StatusProperties(false, "Transaction Counter is 0.", KeyplePoTransactionsOverflowException.class));
        m.put(0x6982, new StatusProperties(false,
                "Security conditions not fulfilled (Get Challenge not done: challenge unavailable).", KeyplePoSecurityContextException.class));
        m.put(0x6985, new StatusProperties(false,
                "Access forbidden (a session is open or DF is invalidated).", KeyplePoAccessForbiddenException.class));
        m.put(0x6988, new StatusProperties(false, "Incorrect Cryptogram.", KeyplePoSecurityDataException.class));
        m.put(0x6A80, new StatusProperties(false,
                "Decrypted message incorrect (key algorithm not supported, incorrect padding, etc.).", KeyplePoSecurityDataException.class));
        m.put(0x6A87, new StatusProperties(false, "Lc not compatible with P2.", KeyplePoIllegalParameterException.class));
        m.put(0x6B00, new StatusProperties(false, "Incorrect P1, P2.", KeyplePoIllegalParameterException.class));
        STATUS_TABLE = m;
    }

    /**
     * Instantiates a new ChangeKeyRespPars
     * 
     * @param response the response from the PO
     * @param builderReference the reference to the builder that created this parser
     */
    public ChangeKeyRespPars(ApduResponse response, ChangeKeyCmdBuild builderReference) {
        super(response, builderReference);
    }

    @Override
    protected Map<Integer, StatusProperties> getStatusTable() {
        return STATUS_TABLE;
    }
}
