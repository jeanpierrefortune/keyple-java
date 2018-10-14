/*
 * Copyright (c) 2018 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License version 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 */

package org.eclipse.keyple.calypso.command.po;


/**
 * Interface for commands that can modify the PO memory content
 */
public interface PoModificationCommand {
    /**
     * Gets the number of bytes in the session modifications buffer required to execute this
     * command.
     * 
     * @return a number in bytes.
     */
    int getModificationsBufferBytesUsage();
}
