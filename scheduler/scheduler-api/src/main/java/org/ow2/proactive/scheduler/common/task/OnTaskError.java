/*
 *  *
 * ProActive Parallel Suite(TM): The Java(TM) library for
 *    Parallel, Distributed, Multi-Core Computing for
 *    Enterprise Grids & Clouds
 *
 * Copyright (C) 1997-2016 INRIA/University of
 *                 Nice-Sophia Antipolis/ActiveEon
 * Contact: proactive@ow2.org or contact@activeeon.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; version 3 of
 * the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://proactive.inria.fr/team_members.htm
 *  Contributor(s):
 *
 *  * $$ACTIVEEON_INITIAL_DEV$$
 */
package org.ow2.proactive.scheduler.common.task;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.objectweb.proactive.annotation.PublicAPI;

@PublicAPI
@XmlAccessorType(XmlAccessType.FIELD)
public class OnTaskError {

    // Members
    String descriptor;

    private final static String CANCEL_JOB_STRING = "cancelJob";
    private final static String PAUSE_TASK_STRING = "pauseTask";
    private final static String PAUSE_JOB_STRING = "pauseJob";
    private final static String CONTINUE_JOB_EXECUTION_STRING = "continueJobExecution";
    private final static String NOT_SET_STRING = "";

    public final static OnTaskError CANCEL_JOB = new OnTaskError(CANCEL_JOB_STRING);
    public final static OnTaskError PAUSE_TASK = new OnTaskError(PAUSE_TASK_STRING);
    public final static OnTaskError PAUSE_JOB = new OnTaskError(PAUSE_JOB_STRING);
    public final static OnTaskError CONTINUE_JOB_EXECUTION = new OnTaskError(CONTINUE_JOB_EXECUTION_STRING);
    public final static OnTaskError NOT_SET = new OnTaskError(NOT_SET_STRING);


    private OnTaskError(String descriptor) {
        this.descriptor = descriptor;
    }

    /**
     * Get a OnTaskError instance based on a descriptor string. If the descriptor string is not found,
     * 'not set' is returned.
     * @param descriptor Descriptor string.
     * @return OnTaskError instance or 'not set' if descriptor string is not recognized.
     */
    public static OnTaskError getInstance(String descriptor) {
        switch (descriptor) {
            case CANCEL_JOB_STRING:
                return CANCEL_JOB;
            case PAUSE_TASK_STRING:
                return PAUSE_TASK;
            case PAUSE_JOB_STRING:
                return PAUSE_JOB;
            case CONTINUE_JOB_EXECUTION_STRING:
                return CONTINUE_JOB_EXECUTION;
            default:
                return NOT_SET;
        }
    }

    String getDescriptor() {
        return this.descriptor;
    }

    public String toString() {
        return this.descriptor;
    }
}
