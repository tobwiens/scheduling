/*
 * ################################################################
 *
 * ProActive Parallel Suite(TM): The Java(TM) library for
 *    Parallel, Distributed, Multi-Core Computing for
 *    Enterprise Grids & Clouds
 *
 * Copyright (C) 1997-2011 INRIA/University of
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
 *  Contributor(s): ActiveEon Team - http://www.activeeon.com
 *
 * ################################################################
 * $$ACTIVEEON_CONTRIBUTOR$$
 */
package org.ow2.proactive.scheduler.task.utils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Manage substitutions for variables and more.
 */
public class Substitutor {

    public static Map<String, String> buildSubstitutes(Map<String, Serializable> variables) {
        Map<String, String> replacements = new HashMap<>();

        if (variables != null) {
            for (Map.Entry<String, Serializable> variable : variables.entrySet()) {
                replacements.put("$" + variable.getKey(), variable.getValue().toString());
                replacements.put("${" + variable.getKey() + "}", variable.getValue().toString());
            }
        }

        return replacements;
    }

    public static String replace(String input, Map<String, String> substitutes) {
        String output = input;

        for (Map.Entry<String, String> replacement : substitutes.entrySet()) {
            output = output.replace(replacement.getKey(), replacement.getValue());
        }

        return output;
    }

}
