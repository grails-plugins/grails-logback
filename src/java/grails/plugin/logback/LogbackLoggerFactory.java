package grails.plugin.logback;

import org.codehaus.groovy.grails.exceptions.StackTraceFilterer;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusListener;

/**
 * A Logback adapter that produces cleaner, more informative stack traces.
 * Based on org.slf4j.impl.GrailsLog4jLoggerAdapter.
 *
 * @author Graeme Rocher
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class LogbackLoggerFactory implements ILoggerFactory, StatusListener {

	protected LoggerContext loggerContext = new LoggerContext();
	protected StackTraceFilterer stackTraceFilterer = new LogbackStackTraceFilterer();

	public LogbackLoggerFactory() {
		loggerContext.setName(CoreConstants.DEFAULT_CONTEXT_NAME);
		loggerContext.getStatusManager().add(this);
	}

	public Logger getLogger(String name) {
		return new LogbackLoggerAdapter(loggerContext.getLogger(name));
	}

	public void reset() {
		loggerContext.reset();
		loggerContext.getStatusManager().add(this);
	}

	public LoggerContext getLoggerContext() {
		return loggerContext;
	}

	public void addStatusEvent(Status status) {
		if (status.getEffectiveLevel() >= Status.WARN) {
			System.err.println(status);
			if (status.getThrowable() != null) {
				stackTraceFilterer.filter(status.getThrowable()).printStackTrace(System.err);
			}
		}
	}
}
