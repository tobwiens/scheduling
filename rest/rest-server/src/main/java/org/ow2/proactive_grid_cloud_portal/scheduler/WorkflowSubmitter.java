/*
 * ProActive Parallel Suite(TM):
 * The Open Source library for parallel and distributed
 * Workflows & Scheduling, Orchestration, Cloud Automation
 * and Big Data Analysis on Enterprise Grids & Clouds.
 *
 * Copyright (c) 2007 - 2017 ActiveEon
 * Contact: contact@activeeon.com
 *
 * This library is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation: version 3 of
 * the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 */
package org.ow2.proactive_grid_cloud_portal.scheduler;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.objectweb.proactive.core.util.log.ProActiveLogger;
import org.ow2.proactive.scheduler.common.Scheduler;
import org.ow2.proactive.scheduler.common.exception.JobCreationException;
import org.ow2.proactive.scheduler.common.exception.NotConnectedException;
import org.ow2.proactive.scheduler.common.exception.PermissionException;
import org.ow2.proactive.scheduler.common.exception.SubmissionClosedException;
import org.ow2.proactive.scheduler.common.job.Job;
import org.ow2.proactive.scheduler.common.job.JobId;
import org.ow2.proactive.scheduler.common.job.factories.JobFactory;
import org.ow2.proactive.scheduler.core.properties.PASchedulerProperties;
import org.ow2.proactive_grid_cloud_portal.scheduler.exception.JobCreationRestException;
import org.ow2.proactive_grid_cloud_portal.scheduler.exception.NotConnectedRestException;
import org.ow2.proactive_grid_cloud_portal.scheduler.exception.PermissionRestException;
import org.ow2.proactive_grid_cloud_portal.scheduler.exception.SubmissionClosedRestException;
import org.ow2.proactive_grid_cloud_portal.webapp.PortalConfiguration;


public class WorkflowSubmitter {

    private static final Logger logger = ProActiveLogger.getLogger(WorkflowSubmitter.class);

    private static final String FILE_ENCODING = PASchedulerProperties.FILE_ENCODING.getValueAsString();

    private Scheduler scheduler;

    public WorkflowSubmitter(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * Submits a workflow to the scheduler (XML or archive).
     * It also creates a job visualization HTML file.
     *
     * @param workflowFile a workflow file (XML or archive)
     * @param variables    variables to be replaced on submission
     * @return job ID of the job created for the specified workflow and associated variables.
     * @throws JobCreationRestException
     * @throws NotConnectedRestException
     * @throws PermissionRestException
     * @throws SubmissionClosedRestException
     */
    public JobId submit(File workflowFile, Map<String, String> variables) throws NotConnectedRestException,
            PermissionRestException, SubmissionClosedRestException, JobCreationRestException {
        try {
            Job job = createJobObject(workflowFile, variables);
            JobId jobId = scheduler.submit(job);
            // Create Job's SVG visualization file
            String visualization = job.getVisualization();
            if (visualization != null && !visualization.isEmpty()) {
                File visualizationFile = new File(PortalConfiguration.jobIdToPath(jobId.value()) + ".html");
                Files.deleteIfExists(visualizationFile.toPath());
                FileUtils.write(new File(visualizationFile.getAbsolutePath()),
                                job.getVisualization(),
                                Charset.forName(FILE_ENCODING));
            }
            return jobId;
        } catch (NotConnectedException e) {
            throw new NotConnectedRestException(e);
        } catch (PermissionException e) {
            throw new PermissionRestException(e);
        } catch (SubmissionClosedException e) {
            throw new SubmissionClosedRestException(e);
        } catch (JobCreationException e) {
            throw new JobCreationRestException(e);
        } catch (Exception e) {
            logger.warn("Unexpected error when submitting job", e);
            throw new JobCreationRestException(e);
        }
    }

    private Job createJobObject(File jobFile, Map<String, String> jobVariables) throws JobCreationException {
        return JobFactory.getFactory().createJob(jobFile.getAbsolutePath(), jobVariables);
    }

}
