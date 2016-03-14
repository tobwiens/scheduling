/*
 * ################################################################
 *
 * ProActive Parallel Suite(TM): The Java(TM) library for
 *    Parallel, Distributed, Multi-Core Computing for
 *    Enterprise Grids & Clouds
 *
 * Copyright (C) 1997-2015 INRIA/University of
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
 * ################################################################
 * $$PROACTIVE_INITIAL_DEV$$
 */
package org.ow2.proactive.scheduler.common.job.factories;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.objectweb.proactive.extensions.dataspaces.vfs.selector.FileSelector;
import org.ow2.proactive.scheduler.common.job.TaskFlowJob;
import org.ow2.proactive.scheduler.common.task.*;
import org.ow2.proactive.scheduler.common.task.dataspaces.InputSelector;
import org.ow2.proactive.scheduler.common.task.dataspaces.OutputSelector;
import org.ow2.proactive.scheduler.common.task.flow.FlowActionType;
import org.ow2.proactive.scheduler.common.task.flow.FlowBlock;
import org.ow2.proactive.scheduler.common.task.flow.FlowScript;
import org.ow2.proactive.scheduler.core.properties.PASchedulerProperties;
import org.ow2.proactive.scripting.Script;
import org.ow2.proactive.scripting.SelectionScript;
import org.ow2.proactive.topology.descriptor.*;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * Helper class to transform a job into its xml representation.
 * <p>
 * The xml produced conforms to the definition in {@link Schemas}.
 * <p>
 * The order of elements is sensitive.
 *
 * @author esalagea
 */
public class Job2XMLTransformer {

    public static Logger logger = Logger.getLogger(Job2XMLTransformer.class);
    public static final String XSD_LOCATION = "urn:proactive:jobdescriptor:dev ../../src/scheduler/src/org/ow2/proactive/scheduler/common/xml/schemas/jobdescriptor/dev/schedulerjob.xsd";

    public Job2XMLTransformer() {

    }

    /**
     * Creates the xml representation of the job in argument
     *
     * @throws TransformerException
     * @throws ParserConfigurationException
     */
    public InputStream jobToxml(TaskFlowJob job) throws TransformerException, ParserConfigurationException {
        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
        Document doc = docBuilder.newDocument();
        doc.setXmlStandalone(true);

        // create the xml tree corresponding to this job
        Element rootJob = createRootJobElement(doc, job);
        doc.appendChild(rootJob);

        // set up a transformer
        TransformerFactory transfac = TransformerFactory.newInstance();
        Transformer trans = transfac.newTransformer();
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        trans.setOutputProperty(OutputKeys.INDENT, "yes");
        // If the encoding property is set on the client JVM, use it (it has to match the server-side encoding),
        // otherwise use UTF-8
        if (PASchedulerProperties.FILE_ENCODING.isSet()) {
            trans.setOutputProperty(OutputKeys.ENCODING, PASchedulerProperties.FILE_ENCODING.getValueAsString());
        } else {
            trans.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        }
        trans.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        // write the xml
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StreamResult result = new StreamResult(baos);
        DOMSource source = new DOMSource(doc);
        trans.transform(source, result);
        byte[] array = baos.toByteArray();
        return new ByteArrayInputStream(array);
    }

    /**
     * Creates the xml representation of the job in argument
     *
     * @throws TransformerException
     * @throws ParserConfigurationException
     */
    public String jobToxmlString(TaskFlowJob job) throws TransformerException, ParserConfigurationException, IOException {
        InputStream is = jobToxml(job);
        String answer =  IOUtils.toString(is, "UTF-8");
        return answer;
    }

    /**
     * Serializes the given job as xml and writes it to a file.
     *
     * @param job
     *            TaskFlowJob to be serialized
     * @param f
     *            The file where the xml will be written
     * @throws ParserConfigurationException
     * @throws TransformerException
     * @throws IOException
     */
    public void job2xmlFile(TaskFlowJob job, File f) throws ParserConfigurationException,
            TransformerException, IOException {
        String xmlString = jobToxmlString(job);
        FileWriter fw = new FileWriter(f);
        fw.write(xmlString);
        fw.close();
    }

    /**
     * Creates the "job" element <define name="job">
     */
    private Element createRootJobElement(Document doc, TaskFlowJob job) {
        Element rootJob = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, "job");

        // ********** attributes ***********
        rootJob.setAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "xsi:schemaLocation",
                XSD_LOCATION);
        setAttribute(rootJob, XMLAttributes.JOB_PROJECT_NAME, job.getProjectName(), true);
        setAttribute(rootJob, XMLAttributes.JOB_PRIORITY, job.getPriority().toString());
        if (job.getCancelJobOnErrorProperty().isSet()) {
            setAttribute(rootJob, XMLAttributes.COMMON_CANCELJOB_ON_ERROR, Boolean.toString(job
                    .getCancelJobOnErrorProperty().getValue().getBooleanValue()));
        }
        //if (job.getOnTaskError().isSet()) {
            setAttribute(rootJob, XMLAttributes.COMMON_ON_TASK_ERROR,job
                    .getOnTaskError().getValue().toString());
        //}
        if (job.getMaxNumberOfExecutionProperty().isSet()) {
            setAttribute(rootJob, XMLAttributes.COMMON_MAX_NUMBER_OF_EXECUTION, Integer.toString(job
                    .getMaxNumberOfExecution()));
        }
        setAttribute(rootJob, XMLAttributes.COMMON_NAME, job.getName(), true);
        if (job.getRestartTaskOnErrorProperty().isSet()) {
            setAttribute(rootJob, XMLAttributes.COMMON_RESTART_TASK_ON_ERROR, job.getRestartTaskOnError()
                    .toString());
        }

        // *** elements ***

        // <ref name="variables"/>
        if (job.getVariables() != null && !job.getVariables().isEmpty()) {
            Element variablesE = createVariablesElement(doc, job.getVariables());
            rootJob.appendChild(variablesE);
        }

        // <ref name="jobDescription"/>
        if (job.getDescription() != null) {
            Element descrNode = createElement(doc, XMLTags.COMMON_DESCRIPTION.getXMLName(), job
                    .getDescription());
            rootJob.appendChild(descrNode);
        }

        // <ref name="genericInformation"/>
        if ((job.getGenericInformation() != null) && (job.getGenericInformation().size() > 0)) {
            Element genericInfo = createGenericInformation(doc, job.getGenericInformation());
            rootJob.appendChild(genericInfo);
        }

        // <ref name="inputSpace"/>
        if (job.getInputSpace() != null) {
            Element inputspace = createElement(doc, XMLTags.DS_INPUT_SPACE.getXMLName(), null, new Attribute(
                    XMLAttributes.DS_URL.getXMLName(), job.getInputSpace()));
            rootJob.appendChild(inputspace);
        }

        // <ref name="outputSpace"/>
        if (job.getOutputSpace() != null) {
            Element outputSpace = createElement(doc, XMLTags.DS_OUTPUT_SPACE.getXMLName(), null,
                    new Attribute(XMLAttributes.DS_URL.getXMLName(), job.getOutputSpace()));
            rootJob.appendChild(outputSpace);
        }

        // <ref name="globalSpace"/>
        if (job.getGlobalSpace() != null) {
            Element globalSpace = createElement(doc, XMLTags.DS_GLOBAL_SPACE.getXMLName(), null,
                    new Attribute(XMLAttributes.DS_URL.getXMLName(), job.getGlobalSpace()));
            rootJob.appendChild(globalSpace);
        }

        // <ref name="userSpace"/>
        if (job.getUserSpace() != null) {
            Element userSpace = createElement(doc, XMLTags.DS_USER_SPACE.getXMLName(), null, new Attribute(
                    XMLAttributes.DS_URL.getXMLName(), job.getUserSpace()));
            rootJob.appendChild(userSpace);
        }

        // <ref name="taskFlow"/>
        Element taskFlow = createTaskFlowElement(doc, job);
        rootJob.appendChild(taskFlow);

        return rootJob;
    }

    /**
     * Creates an element and set the value of its attributes
     *
     */
    private Element createElement(Document doc, String tagName, String elementText, Attribute... attribs) {
        Element el = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, tagName);
        for (Attribute a : attribs) {
            el.setAttribute(a.getName(), a.getValue());
        }
        if (elementText != null) {
            Text text = doc.createTextNode(elementText);
            el.appendChild(text);
        }
        return el;
    }

    /*
     * Creates the variables element
     */
    private Element createVariablesElement(Document doc, Map<String, String> variables) {
        if (variables == null) {
            return null;
        }
        Element variablesE = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.VARIABLES.getXMLName());
        for (String name : variables.keySet()) {
            Element variableE = createElement(doc, XMLTags.VARIABLE.getXMLName(), null, new Attribute(
                    XMLAttributes.COMMON_NAME.getXMLName(), name), new Attribute(XMLAttributes.COMMON_VALUE
                    .getXMLName(), variables.get(name)));
            variablesE.appendChild(variableE);
        }
        return variablesE;
    }

    /**
     * Creates the generic information element corresponding to <define
     * name="genericInformation">
     *
     */
    private Element createGenericInformation(Document doc, Map<String, String> info) {
        if (info == null)
            return null;

        Element el = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.COMMON_GENERIC_INFORMATION.getXMLName());

        // <oneOrMore>
        // <ref name="info"/>
        // </oneOrMore>
        for (String name : info.keySet()) {
            Element infoElement = createElement(doc, XMLTags.COMMON_INFO.getXMLName(), null, new Attribute(
                    XMLAttributes.COMMON_NAME.getXMLName(), name), new Attribute(XMLAttributes.COMMON_VALUE
                    .getXMLName(), info.get(name)));
            el.appendChild(infoElement);
        }
        return el;
    }

    /**
     * Sets the value of the given attribute to lowerCase, if the value is not
     * null, otherwise it doesn't do anything
     *
     * @param e
     *            element to set the attribute value for
     */
    private void setAttribute(Element e, XMLAttributes attrib, String attribVal) {
        if (attribVal == null)
            return;
        e.setAttribute(attrib.getXMLName(), attribVal.toLowerCase());
    }

    /**
     * Sets the value of the given attribute.
     *
     * @param caseSensitive
     *            if true, the attribVal case is kept, if false, the value is
     *            set as lowercase
     */
    private void setAttribute(Element e, XMLAttributes attrib, String attribVal, boolean caseSensitive) {
        if (attribVal == null)
            return;

        if (caseSensitive) {
            e.setAttribute(attrib.getXMLName(), attribVal);
        } else
            setAttribute(e, attrib, attribVal);
    }

    /**
     * Creates the taskflow element, corresponding to <define name="taskFlow">
     *
     */
    private Element createTaskFlowElement(Document doc, TaskFlowJob job) {
        Element taskFlowElement = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.TASK_FLOW.getXMLName());

        ArrayList<Task> tasks = job.getTasks();

        // <oneOrMore>
        // <ref name="task"/>
        // </oneOrMore>
        for (Task task : tasks) {
            Element taskE = createTaskElement(doc, task);
            taskFlowElement.appendChild(taskE);
        }
        return taskFlowElement;
    }

    /**
     * Creates the task element, corressponding to <define name="task">
     *
     */
    private Element createTaskElement(Document doc, Task task) {
        Element taskE = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.TASK.getXMLName());

        // **** attributes *****
        // **** common attributes ***

        if (task.getCancelJobOnErrorProperty().isSet()) {
            setAttribute(taskE, XMLAttributes.COMMON_CANCELJOB_ON_ERROR, Boolean.toString(task
                    .isCancelJobOnError()));
        }
        //if (job.getOnTaskError().isSet()) {
        setAttribute(taskE, XMLAttributes.COMMON_ON_TASK_ERROR,
                task.getOnTaskError().getValue().toString());
        //}
        if (task.getMaxNumberOfExecutionProperty().isSet()) {
            setAttribute(taskE, XMLAttributes.COMMON_MAX_NUMBER_OF_EXECUTION, Integer.toString(task
                    .getMaxNumberOfExecution()));
        }
        setAttribute(taskE, XMLAttributes.COMMON_NAME, task.getName(), true);
        if (task.getRestartTaskOnErrorProperty().isSet()) {
            setAttribute(taskE, XMLAttributes.COMMON_RESTART_TASK_ON_ERROR, task.getRestartTaskOnError()
                    .toString());
        }

        // *** task attributes ***
        if (task.getWallTime() != 0) {
            setAttribute(taskE, XMLAttributes.TASK_WALLTIME, formatDate(task.getWallTime()));
        }

        if (task.isRunAsMe()) {
            setAttribute(taskE, XMLAttributes.TASK_RUN_AS_ME, "true");
        }

        if (task.isPreciousResult()) {
            setAttribute(taskE, XMLAttributes.TASK_PRECIOUS_RESULT, "true");
        }

        if (task.isPreciousLogs()) {
            setAttribute(taskE, XMLAttributes.TASK_PRECIOUS_LOGS, "true");
        }

        // *** elements ****

        // <ref name="taskDescription"/>
        if (task.getDescription() != null) {
            Element descrNode = createElement(doc, XMLTags.COMMON_DESCRIPTION.getXMLName(), task
                    .getDescription());
            taskE.appendChild(descrNode);
        }

        // <ref name="genericInformation"/>
        if ((task.getGenericInformation() != null) && (task.getGenericInformation().size() > 0)) {
            Element genericInfoE = createGenericInformation(doc, task.getGenericInformation());
            taskE.appendChild(genericInfoE);
        }

        // <ref name="depends"/>
        List<Task> dependencies = task.getDependencesList();
        if ((dependencies != null) && (dependencies.size() > 0)) {
            Element dependsE = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.TASK_DEPENDENCES.getXMLName());
            for (Task dep : dependencies) {
                Element dependsTask = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.TASK_DEPENDENCES_TASK.getXMLName());
                setAttribute(dependsTask, XMLAttributes.TASK_DEPENDS_REF, dep.getName(), true);
                dependsE.appendChild(dependsTask);
            }
            taskE.appendChild(dependsE);
        }// if has dependencies

        // <ref name="inputFiles"/>
        List<InputSelector> inputFiles = task.getInputFilesList();
        if (inputFiles != null) {
            Element inputFilesE = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.DS_INPUT_FILES.getXMLName());
            for (InputSelector inputSelector : inputFiles) {
                FileSelector fs = inputSelector.getInputFiles();
                Element filesE = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.DS_FILES.getXMLName());
                // the xml only supports one value for the includes/excludes
                // pattern
                if (!fs.getIncludes().isEmpty())
                    setAttribute(filesE, XMLAttributes.DS_INCLUDES, fs.getIncludes().iterator().next(), true);
                if (!fs.getExcludes().isEmpty())
                    setAttribute(filesE, XMLAttributes.DS_EXCLUDES, fs.getExcludes().iterator().next(), true);
                if (inputSelector.getMode() != null) {
                    setAttribute(filesE, XMLAttributes.DS_ACCESS_MODE, inputSelector.getMode().toString(),
                            true);
                }
                inputFilesE.appendChild(filesE);
            }
            taskE.appendChild(inputFilesE);
        }

        // <ref name="parallel"/>
        Element parallelEnvE = createParallelEnvironment(doc, task);
        if (parallelEnvE != null)
            taskE.appendChild(parallelEnvE);

        // <ref name="selection"/>
        List<SelectionScript> selectionScripts = task.getSelectionScripts();
        if (selectionScripts != null && selectionScripts.size() > 0) {
            Element selectionE = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.SCRIPT_SELECTION.getXMLName());
            for (SelectionScript selectionScript : selectionScripts) {
                Element scriptE = createScriptElement(doc, selectionScript);
                selectionE.appendChild(scriptE);
            }
            taskE.appendChild(selectionE);
        }


        // <ref name="forkEnvironment"/>
        if (task.getForkEnvironment() != null) {
            Element forkEnvE = createForkEnvironmentElement(doc, task.getForkEnvironment());
            taskE.appendChild(forkEnvE);
        }

        // <ref name="pre"/>
        Script preScript = task.getPreScript();
        if (preScript != null) {
            Element preE = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.SCRIPT_PRE.getXMLName());
            Element scriptE = createScriptElement(doc, preScript);
            preE.appendChild(scriptE);
            taskE.appendChild(preE);
        }

        // <ref name="executable"/>
        Element executableE = null;
        if (task instanceof JavaTask) {
            executableE = createJavaExecutableElement(doc, (JavaTask) task);
        } else if (task instanceof NativeTask) {
            executableE = createNativeExecutableElement(doc, (NativeTask) task);
        } else if (task instanceof ScriptTask) {
            executableE = createScriptExecutableElement(doc, (ScriptTask) task);
        }
        taskE.appendChild(executableE);

        // <ref name="flow"/>
        Element controlFlowE = createFlowControlElement(doc, task);
        if (controlFlowE != null)
            taskE.appendChild(controlFlowE);

        // <ref name="post"/>
        Script postScript = task.getPostScript();
        if (postScript != null) {
            Element postE = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.SCRIPT_POST.getXMLName());
            Element scriptE = createScriptElement(doc, postScript);
            postE.appendChild(scriptE);
            taskE.appendChild(postE);
        }

        // <ref name="cleaning"/>
        Script cleanScript = task.getCleaningScript();
        if (cleanScript != null) {
            Element cleanE = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.SCRIPT_CLEANING.getXMLName());
            Element scriptE = createScriptElement(doc, cleanScript);
            cleanE.appendChild(scriptE);
            taskE.appendChild(cleanE);
        }

        // <ref name="outputFiles"/>
        List<OutputSelector> outputFiles = task.getOutputFilesList();
        if (outputFiles != null) {
            Element outputFilesE = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.DS_OUTPUT_FILES.getXMLName());
            for (OutputSelector outputSelector : outputFiles) {
                FileSelector fs = outputSelector.getOutputFiles();
                Element filesE = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.DS_FILES.getXMLName());
                // the xml only supports one value for the includes/excludes
                // pattern
                if (!fs.getIncludes().isEmpty())
                    setAttribute(filesE, XMLAttributes.DS_INCLUDES, fs.getIncludes().iterator().next(), true);
                if (!fs.getExcludes().isEmpty())
                    setAttribute(filesE, XMLAttributes.DS_EXCLUDES, fs.getExcludes().iterator().next(), true);
                if (outputSelector.getMode() != null) {
                    setAttribute(filesE, XMLAttributes.DS_ACCESS_MODE, outputSelector.getMode().toString(),
                            true);
                }
                outputFilesE.appendChild(filesE);
            }
            taskE.appendChild(outputFilesE);
        }
        return taskE;
    }

    /**
     * Creates the parallel environment element for the given task. Corresponds
     * to <define name="parallel">
     *
     * @return the {@link XMLTags#PARALLEL_ENV} element if the task has a
     *         parallel environment, null otherwise
     */
    private Element createParallelEnvironment(Document doc, Task task) {
        ParallelEnvironment penv = task.getParallelEnvironment();
        if (penv == null)
            return null;

        Element parallelEnvE = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.PARALLEL_ENV.getXMLName());
        setAttribute(parallelEnvE, XMLAttributes.TASK_NB_NODES, Integer.toString(penv.getNodesNumber()));

        // <ref name="topology"/>
        TopologyDescriptor topologyDescr = penv.getTopologyDescriptor();
        if (topologyDescr != null) {

            // <choice>
            // <ref name="arbitrary"/>
            // <ref name="bestProximity"/>
            // <ref name="thresholdProximity"/>
            // <ref name="singleHost"/>
            // <ref name="singleHostExclusive"/>
            // <ref name="multipleHostsExclusive"/>
            // <ref name="differentHostsExclusive"/>
            // </choice>

            Element topologyE = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.TOPOLOGY.getXMLName());
            Element topologyDescrE = null;

            if (topologyDescr instanceof ArbitraryTopologyDescriptor) {
                topologyDescrE = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.TOPOLOGY_ARBITRARY.getXMLName());
            } else if (topologyDescr instanceof ThresholdProximityDescriptor) {
                topologyDescrE = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.TOPOLOGY_THRESHOLD_PROXIMITY.getXMLName());
                long threshold = ((ThresholdProximityDescriptor) topologyDescr).getThreshold();
                topologyDescrE.setAttribute(XMLAttributes.TOPOLOGY_THRESHOLD.getXMLName(), Long
                        .toString(threshold));
            } else if (topologyDescr instanceof BestProximityDescriptor) {
                topologyDescrE = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.TOPOLOGY_BEST_PROXIMITY.getXMLName());

            } else if (topologyDescr instanceof SingleHostExclusiveDescriptor) {
                topologyDescrE = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.TOPOLOGY_SINGLE_HOST_EXCLUSIVE.getXMLName());
            } else if (topologyDescr instanceof SingleHostDescriptor) {
                topologyDescrE = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.TOPOLOGY_SINGLE_HOST.getXMLName());
            } else if (topologyDescr instanceof MultipleHostsExclusiveDescriptor) {
                topologyDescrE = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.TOPOLOGY_MULTIPLE_HOSTS_EXCLUSIVE.getXMLName());
            }
            if (topologyDescr instanceof DifferentHostsExclusiveDescriptor) {
                topologyDescrE = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.TOPOLOGY_DIFFERENT_HOSTS_EXCLUSIVE.getXMLName());
            }

            if (topologyDescrE != null) {
                topologyE.appendChild(topologyDescrE);
            }

            parallelEnvE.appendChild(topologyE);
        }
        return parallelEnvE;
    }

    /**
     * Creates a flow control element for the given task <element
     * name="controlFlow">
     *
     * @return the xml Element corresponding to the flow control, if the task
     *         contains a flow control, null otherwise
     */
    private Element createFlowControlElement(Document doc, Task task) {
        Element controlFlowE = null;

        // <ref name="block"/>
        if (task.getFlowBlock() != FlowBlock.NONE) {
            controlFlowE = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.FLOW.getXMLName());
            setAttribute(controlFlowE, XMLAttributes.FLOW_BLOCK, task.getFlowBlock().toString());
        }

        FlowScript flowScript = task.getFlowScript();
        if (flowScript != null) {

            Element flowActionE = null;

            // flowActionE can be if, loop, replicate or null.
            // if not null, it contains a script element

            // <ref name="actionIf"/>
            // <ref name="actionReplicate"/>
            // <ref name="actionLoop"/>
            // </choice>

            // *** if ***
            // <element name="if">
            if (flowScript.getActionType().equals(FlowActionType.IF.toString())) {
                flowActionE = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.FLOW_IF.getXMLName());
                setAttribute(flowActionE, XMLAttributes.FLOW_TARGET, flowScript.getActionTarget(), true);
                setAttribute(flowActionE, XMLAttributes.FLOW_ELSE, flowScript.getActionTargetElse(), true);
                setAttribute(flowActionE, XMLAttributes.FLOW_CONTINUATION,
                        flowScript.getActionContinuation(), true);
            }

            // *** loop ***
            // <element name="loop">
            if (flowScript.getActionType().equals(FlowActionType.LOOP.toString())) {
                flowActionE = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.FLOW_LOOP.getXMLName());
                setAttribute(flowActionE, XMLAttributes.FLOW_TARGET, flowScript.getActionTarget(), true);
            }

            // *** replicate ***
            // <element name="replicate">
            if (flowScript.getActionType().equals(FlowActionType.REPLICATE.toString())) {
                flowActionE = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.FLOW_REPLICATE.getXMLName());
            }

            if (flowActionE != null) {
                if (controlFlowE == null) {
                    controlFlowE = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.FLOW.getXMLName());
                }
                Element scriptE = createScriptElement(doc, flowScript);
                flowActionE.appendChild(scriptE);
                controlFlowE.appendChild(flowActionE);
            }
        }// flowScript !=null

        return controlFlowE;

    }

    /**
     * Corresponds to <element name="script">
     *
     * The schema allows the specification of a script either by writing the
     * script code either by providing a file with arguments. Both will result
     * in the same {@link org.ow2.proactive.scripting.Script} object. In the
     * current translation we will always translate a Script object by inlining
     * the script code using a "codeScript"element (first option).
     *
     * The xml specification does not allow addding arguments to a script
     * defined by its code. Therefore, when we translate the script object to
     * xml, if we encounter arguments, we will insert their value directly in
     * the script's code by inserting a line like:
     * <p/>
     * var args = ["argument_1",...,"argument_n"];
     *
     */
    private Element createScriptElement(Document doc, Script script) {
        Element scriptElement = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.SCRIPT_SCRIPT.getXMLName());
        Element codeE = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.SCRIPT_CODE.getXMLName());
        setAttribute(codeE, XMLAttributes.LANGUAGE, script.getEngineName(), true);
        String scriptText = script.getScript();
        Serializable[] params = script.getParameters();
        if (params != null) {

            scriptText = inlineScriptParametersInText(scriptText, params);
        }

        CDATASection scriptTextCDATA = doc.createCDATASection(scriptText);
        codeE.appendChild(scriptTextCDATA);

        scriptElement.appendChild(codeE);
        return scriptElement;
    }

    /**
     * Inserts a line, at the beginning of the script code with the text: var
     * args = ["argument_1",...,"argument_n"];
     *
     * where "argument_1",...,"argument_n" are the elements of the input array
     * "params"
     *
     */
    public static String inlineScriptParametersInText(String scriptText, Serializable[] params) {
        String paramsLine = "var args=[";
        for (Serializable param : params) {
            paramsLine += "\"" + param + "\",";
        }
        paramsLine = paramsLine.substring(0, paramsLine.length() - 1) + "];";

        return paramsLine + "\n" + scriptText;
    }

    /**
     * Corresponds to <element name="javaExecutable">
     *
     */
    private Element createJavaExecutableElement(Document doc, JavaTask t) {
        Element executableE = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.JAVA_EXECUTABLE.getXMLName());
        setAttribute(executableE, XMLAttributes.TASK_CLASS_NAME, t.getExecutableClassName(), true);

        // <ref name="javaParameters"/>
        try {
            Map<String, Serializable> args = t.getArguments();
            if ((args != null) && (args.size() > 0)) {
                // <element name="parameter">
                Element paramsE = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.TASK_PARAMETERS.getXMLName());
                for (String name : args.keySet()) {
                    Serializable val = args.get(name);
                    Element paramE = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.TASK_PARAMETER.getXMLName());
                    setAttribute(paramE, XMLAttributes.COMMON_NAME, name, true);
                    setAttribute(paramE, XMLAttributes.COMMON_VALUE, val.toString(), true);
                    paramsE.appendChild(paramE);
                }
                executableE.appendChild(paramsE);
            }
        } catch (Exception e) {
            logger.error("Could not add arguments for Java Executable element of task " + t.getName(), e);
        }
        return executableE;
    }

    /**
     * Corresponds to <element name="forkEnvironment">
     *
     */
    private Element createForkEnvironmentElement(Document doc, ForkEnvironment fe) {
        Element forkEnvE = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.FORK_ENVIRONMENT.getXMLName());
        // attributes
        setAttribute(forkEnvE, XMLAttributes.TASK_WORKDING_DIR, fe.getWorkingDir(), true);
        setAttribute(forkEnvE, XMLAttributes.FORK_JAVA_HOME, fe.getJavaHome(), true);

        // <ref name="sysProps"/>
        if ((fe.getSystemEnvironment() != null) && (fe.getSystemEnvironment().keySet().size() > 0)) {
            // <element name="SystemEnvironment">
            Element sysEnvE = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.FORK_SYSTEM_PROPERTIES.getXMLName());
            if (fe.getSystemEnvironment() != null) {

                // <oneOrMore>
                // <ref name="sysProp"/>
                // </oneOrMore>
                for (Map.Entry<String, String> entry : fe.getSystemEnvironment().entrySet()) {
                    Element variableE = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.VARIABLE.getXMLName());
                    setAttribute(variableE, XMLAttributes.COMMON_NAME, entry.getKey());
                    setAttribute(variableE, XMLAttributes.COMMON_VALUE, entry.getValue());

                    sysEnvE.appendChild(variableE);
                }
            }
            forkEnvE.appendChild(sysEnvE);
        }

        // <ref name="jvmArgs"/>
        List<String> args = fe.getJVMArguments();
        if (args != null && (args.size() > 0)) {
            Element jvmArgsE = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.FORK_JVM_ARGS.getXMLName());
            for (String arg : args) {
                Element argE = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.FORK_JVM_ARG.getXMLName());
                setAttribute(argE, XMLAttributes.COMMON_VALUE, arg, true);
                jvmArgsE.appendChild(argE);
            }
            forkEnvE.appendChild(jvmArgsE);
        }

        // <ref name="additionalClasspath"/>
        List<String> additionalCP = fe.getAdditionalClasspath();
        if ((additionalCP != null) && (additionalCP.size() > 0)) {
            Element additionalCPE = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.FORK_ADDITIONAL_CLASSPATH.getXMLName());
            for (String pathElement : additionalCP) {
                Element pathE = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.FORK_PATH_ELEMENT.getXMLName());
                setAttribute(pathE, XMLAttributes.PATH, pathElement, true);
                additionalCPE.appendChild(pathE);
            }

            forkEnvE.appendChild(additionalCPE);
        }

        // <ref name="envScript"/>
        if (fe.getEnvScript() != null) {
            Element envScriptE = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.SCRIPT_ENV.getXMLName());
            Element scriptElement = createScriptElement(doc, fe.getEnvScript());
            envScriptE.appendChild(scriptElement);
            forkEnvE.appendChild(envScriptE);
        }
        return forkEnvE;
    }

    /**
     * Corresponds to <element name="nativeExecutable">
     */
    private Element createNativeExecutableElement(Document doc, NativeTask t) {
        Element nativeExecE = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.NATIVE_EXECUTABLE.getXMLName());

        // <choice>
        // <ref name="staticCommand"/>
        // <ref name="dynamicCommand"/>
        // </choice>
        String[] cmd = t.getCommandLine();
        if (cmd != null && cmd.length > 0) {
            // <element name="staticCommand">
            Element staticCmdE = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.NATIVE_TASK_STATIC_COMMAND.getXMLName());
            setAttribute(staticCmdE, XMLAttributes.TASK_COMMAND_VALUE, cmd[0], true);

            // <ref name="commandArguments"/>
            if (cmd.length > 1) {
                // <element name="arguments">
                Element argsE = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.NATIVE_TASK_ARGUMENTS.getXMLName());
                for (int i = 1; i < cmd.length; i++) {
                    // <element name="argument">
                    Element argE = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.NATIVE_TASK_ARGUMENT.getXMLName());
                    setAttribute(argE, XMLAttributes.COMMON_VALUE, cmd[i], true);
                    argsE.appendChild(argE);
                }
                staticCmdE.appendChild(argsE);
            }
            nativeExecE.appendChild(staticCmdE);
        } else {
            logger.error("The task " + t.getName() + " does not define a command");
        }

        return nativeExecE;
    }

    private Element createScriptExecutableElement(Document doc, ScriptTask t) {
        Element scriptExecE = doc.createElementNS(Schemas.SCHEMA_LATEST.namespace, XMLTags.SCRIPT_EXECUTABLE.getXMLName());
        Element scriptE = createScriptElement(doc, t.getScript());
        scriptExecE.appendChild(scriptE);
        return scriptExecE;
    }

    private static String formatDate(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        String formatted = String.format("%02d:%02d:%02d", hours,
                minutes, seconds);
        // replace heading 00: as it's not accepted by the schema validation
        return (formatted.replaceFirst("^(00:)+", ""));
    }
}

class Attribute {
    private String name;
    private String value;

    public Attribute(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}