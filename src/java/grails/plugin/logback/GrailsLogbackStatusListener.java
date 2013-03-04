package grails.plugin.logback;

import org.codehaus.groovy.grails.exceptions.StackTraceFilterer;

import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusListener;

/**
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class GrailsLogbackStatusListener implements StatusListener {

	protected StackTraceFilterer stackTraceFilterer = new LogbackStackTraceFilterer();

	public void addStatusEvent(Status status) {
		if (status.getEffectiveLevel() >= Status.WARN) {
			System.err.println(status);
			if (status.getThrowable() != null) {
				stackTraceFilterer.filter(status.getThrowable()).printStackTrace(System.err);
			}
		}
	}
}
